package com.fiap.clyvovet.service;

import com.fiap.clyvovet.dto.CheckoutRequestDto;
import com.fiap.clyvovet.model.*;
import com.fiap.clyvovet.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PagamentoSplitServiceTest {

    @Autowired
    private PagamentoSplitService pagamentoSplitService;

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private RecompensaTutorRepository recompensaTutorRepository;

    @Autowired
    private AgendamentoServicoRepository agendamentoRepository;

    @Autowired
    private HistoricoClinicoRepository historicoClinicoRepository;

    private Tutor tutor;
    private Pet pet;

    @BeforeEach
    void setUp() {
        // Usuário 'tutor' já existe nas migrações Flyway (V3) com CPF 123.456.789-00 ou similar
        tutor = tutorRepository.findByUsuarioUsername("tutor").orElseThrow();
        pet = petRepository.findByTutorCpfOrderByIdAsc(tutor.getCpf()).stream().findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Cálculo de split financeiro com take-rate de 15% e desconto de fidelidade")
    void calcularResumoComSplit() {
        // Garante que o tutor tem 15% de desconto (OURO)
        RecompensaTutor recompensa = recompensaTutorRepository.findByTutorCpf(tutor.getCpf()).orElseGet(() -> {
            RecompensaTutor r = new RecompensaTutor();
            r.setTutorCpf(tutor.getCpf());
            return r;
        });
        recompensa.setDescontoPercentual(15);
        recompensa.setNivelFidelidade("OURO");
        recompensaTutorRepository.save(recompensa);

        PagamentoSplitService.ResumoSplit split = pagamentoSplitService.calcularResumo(
                tutor.getCpf(),
                TipoServicoPreventivo.CONSULTA_PREVENTIVA
        );

        // CONSULTA_PREVENTIVA custa R$ 150.00 base
        assertEquals(new BigDecimal("150.00"), split.valorOriginal());
        assertEquals(15, split.descontoPercentual());
        // 15% de 150 = 22.50
        assertEquals(new BigDecimal("22.50"), split.valorDesconto());
        // Valor final = 127.50
        assertEquals(new BigDecimal("127.50"), split.valorFinal());

        // Take-rate do Clyvo = 15% de 127.50 = 19.13 (ou 19.12 arredondado)
        // Repasse clínica = 127.50 - 19.13 = 108.37 (ou 108.38)
        BigDecimal somaSplit = split.valorComissaoClyvo().add(split.valorRepasseClinica());
        assertEquals(split.valorFinal(), somaSplit, "A soma do split da clínica + comissão Clyvo deve ser exatamente o valor final pago");
    }

    @Test
    @DisplayName("Processamento de checkout in-app emite voucher com status PAGO e grava na timeline")
    void processarCheckoutInApp() {
        CheckoutRequestDto dto = new CheckoutRequestDto();
        dto.setPetId(pet.getId());
        dto.setTipoServico(TipoServicoPreventivo.PACOTE_VACINAL_COMPLETO);
        dto.setMetodoPagamento("PIX");
        dto.setObservacoes("Pet calmo para vacina");

        AgendamentoServico agendamento = pagamentoSplitService.processarCheckout(dto, "tutor");

        assertNotNull(agendamento.getId());
        assertEquals(StatusPagamento.PAGO_CONFIRMADO, agendamento.getStatusPagamento());
        assertTrue(agendamento.getCodigoVoucher().startsWith("CLYVO-"));
        assertEquals("PIX", agendamento.getMetodoPagamento());

        // Verifica se foi registrado na linha do tempo
        boolean temEventoTimeline = historicoClinicoRepository.findByPetIdOrderByDataRegistroDesc(pet.getId())
                .stream()
                .anyMatch(h -> "VOUCHER_PREVENTIVO".equals(h.getTipoEvento()) && h.getDescricao().contains(agendamento.getCodigoVoucher()));
        assertTrue(temEventoTimeline, "Deve registrar emissão do voucher na timeline clínica");
    }

    @Test
    @DisplayName("Clínica valida e baixa voucher utilizado com sucesso e impede reuso")
    void validarEBaixarVoucher() {
        CheckoutRequestDto dto = new CheckoutRequestDto();
        dto.setPetId(pet.getId());
        dto.setTipoServico(TipoServicoPreventivo.CONSULTA_PREVENTIVA);
        dto.setMetodoPagamento("CARTAO_CREDITO");

        AgendamentoServico agendamento = pagamentoSplitService.processarCheckout(dto, "tutor");

        // Veterinário valida o voucher na clínica
        AgendamentoServico validado = pagamentoSplitService.validarEBaixarVoucher(agendamento.getCodigoVoucher(), "admin");
        assertEquals(StatusPagamento.UTILIZADO_NA_CLINICA, validado.getStatusPagamento());
        assertNotNull(validado.getDataUtilizacao());

        // Tentar validar de novo deve lançar exceção (anti-fraude)
        assertThrows(IllegalStateException.class, () ->
                pagamentoSplitService.validarEBaixarVoucher(agendamento.getCodigoVoucher(), "admin")
        );
    }
}
