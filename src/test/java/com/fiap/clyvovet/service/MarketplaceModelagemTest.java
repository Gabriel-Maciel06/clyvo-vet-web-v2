package com.fiap.clyvovet.service;

import com.fiap.clyvovet.model.*;
import com.fiap.clyvovet.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MarketplaceModelagemTest {

    @Autowired
    private MarketplaceIntermediacaoService marketplaceService;

    @Autowired
    private ClinicaRepository clinicaRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private ComissaoRepository comissaoRepository;

    @Autowired
    private RecompensaTutorRepository recompensaTutorRepository;

    @Test
    @DisplayName("Deve verificar que o seed Flyway V10 carregou clínicas, catálogo de serviços e agendamento transacionado")
    void deveVerificarSeedFlywayMarketplace() {
        List<Clinica> clinicas = clinicaRepository.findAll();
        assertFalse(clinicas.isEmpty(), "Deveria haver clínicas cadastradas no seed");

        Clinica central = clinicas.stream().filter(c -> c.getId().equals(1L)).findFirst().orElse(null);
        assertNotNull(central);
        assertEquals("12.345.678/0001-90", central.getCnpj());
        assertEquals("financeiro@clyvocentral.com.br", central.getChavePixRepasse());
        assertTrue(central.getAtivo());

        List<Servico> servicosCentral = servicoRepository.findByClinicaIdAndAtivoTrue(1L);
        assertFalse(servicosCentral.isEmpty(), "Central deveria ter catálogo de serviços");
        assertTrue(servicosCentral.stream().anyMatch(s -> s.getNome().contains("Longevidade")));

        List<Agendamento> agendamentos = agendamentoRepository.findAll();
        assertFalse(agendamentos.isEmpty(), "Deveria haver agendamento de seed");

        List<Transacao> transacoes = transacaoRepository.findAll();
        assertFalse(transacoes.isEmpty(), "Deveria haver transação financeira de seed");

        List<Comissao> comissoes = comissaoRepository.findAll();
        assertFalse(comissoes.isEmpty(), "Deveria haver comissão contábil de seed");
        assertEquals(StatusRepasseComissao.RETIDO_ESCROW, comissoes.get(0).getStatusRepasse());
    }

    @Test
    @DisplayName("Deve contratar serviço no marketplace com cálculo exato de split (15%) e retenção em custódia/escrow")
    void deveContratarServicoComSplitEEscrow() {
        LocalDateTime dataAgendada = LocalDateTime.now().plusDays(5);
        MarketplaceIntermediacaoService.ContratoIntermediacaoDto contrato = marketplaceService.contratarServicoNoMarketplace(
                1L, // Thor
                1L, // Consulta Preventiva (R$ 180,00)
                dataAgendada,
                "PIX",
                "Consulta de rotina com exame cardiológico",
                "tutor"
        );

        assertNotNull(contrato);
        assertNotNull(contrato.agendamento().getId());
        assertNotNull(contrato.transacao().getId());
        assertNotNull(contrato.comissao().getId());

        // Verificação dos valores com Co-Financiamento Paritário e Floor Protection:
        // Preço base de tabela = R$ 180.00
        // Desconto tutor Gabriel (Nível PRATA = 10%) = R$ 18.00
        // Valor líquido pago in-app = R$ 162.00
        assertEquals(new BigDecimal("180.00"), contrato.transacao().getValorBruto());
        assertEquals(new BigDecimal("18.00"), contrato.transacao().getValorDescontoFidelidade());
        assertEquals(new BigDecimal("162.00"), contrato.transacao().getValorLiquidoPago());

        // Modelo Tripartite:
        // Subsídio da Clyvo (50% do desconto) = R$ 9.00
        assertEquals(new BigDecimal("9.00"), contrato.comissao().getValorSubsidioPlataforma());
        // Comissão base (15% de 180) = R$ 27.00
        // Comissão líquida da Clyvo (27 - 9) = R$ 18.00
        assertEquals(new BigDecimal("18.00"), contrato.comissao().getValorComissaoPlataforma());
        // Taxa efetiva retida pela Clyvo: 18 / 180 * 100 = 10.00%
        assertEquals(new BigDecimal("10.00"), contrato.comissao().getTaxaEfetivaPercentual());

        // Repasse líquido clínica = 162.00 - 18.00 = R$ 144.00 (80.00% da tabela, acima do piso de 75%)
        assertEquals(new BigDecimal("144.00"), contrato.comissao().getValorRepasseClinica());
        assertFalse(contrato.comissao().getPisoProtegidoAplicado());

        // Fechamento contábil perfeito centavo a centavo: R$ 144 + R$ 18 = R$ 162
        assertEquals(contrato.transacao().getValorLiquidoPago(),
                contrato.comissao().getValorComissaoPlataforma().add(contrato.comissao().getValorRepasseClinica()));

        assertEquals(StatusRepasseComissao.RETIDO_ESCROW, contrato.comissao().getStatusRepasse());
        assertEquals(StatusAgendamento.CONFIRMADO, contrato.agendamento().getStatusAgendamento());
        assertEquals(StatusTransacao.PAGO, contrato.transacao().getStatusTransacao());
    }

    @Test
    @DisplayName("Cenário Crítico do Dossiê: Tutor DIAMANTE (20% de desconto) prova no código a taxa efetiva de 5% e o repasse de R$ 135 (piso 75%)")
    void deveContratarServicoComDesconto20PorCentoEProvarTaxa5PorCentoEPiso75PorCento() {
        // Eleva o tutor para DIAMANTE (20% de desconto)
        RecompensaTutor recompensa = recompensaTutorRepository.findByTutorCpf("123.456.789-00").orElseThrow();
        recompensa.setDescontoPercentual(20);
        recompensa.setNivelFidelidade("DIAMANTE");
        recompensaTutorRepository.save(recompensa);

        MarketplaceIntermediacaoService.ContratoIntermediacaoDto contrato = marketplaceService.contratarServicoNoMarketplace(
                1L, // Thor
                1L, // Consulta Preventiva (R$ 180,00 de tabela)
                LocalDateTime.now().plusDays(4),
                "PIX",
                "Consulta anual Diamante",
                "tutor"
        );

        // 1. Tutor paga R$ 180 - 20% (R$ 36) = R$ 144.00
        assertEquals(new BigDecimal("180.00"), contrato.transacao().getValorBruto());
        assertEquals(new BigDecimal("36.00"), contrato.transacao().getValorDescontoFidelidade());
        assertEquals(new BigDecimal("144.00"), contrato.transacao().getValorLiquidoPago());

        // 2. Co-financiamento do desconto:
        // A Clyvo subsidia 50% do desconto = R$ 18.00 abatidos do take-rate
        assertEquals(new BigDecimal("18.00"), contrato.comissao().getValorSubsidioPlataforma());

        // 3. Take-rate da Clyvo cai de 15% (R$ 27,00) para 5% (R$ 9,00):
        // Comissão Líquida Clyvo: 27.00 - 18.00 = R$ 9.00
        assertEquals(new BigDecimal("9.00"), contrato.comissao().getValorComissaoPlataforma());
        // Taxa Efetiva Retida pela Clyvo: 9.00 / 180.00 * 100 = 5.00%
        assertEquals(new BigDecimal("5.00"), contrato.comissao().getTaxaEfetivaPercentual());

        // 4. Repasse Líquido à Clínica: 144.00 - 9.00 = R$ 135.00 (Exatamente 75% da tabela!)
        assertEquals(new BigDecimal("135.00"), contrato.comissao().getValorRepasseClinica());

        // 5. Verificação da soma contábil do split: R$ 9,00 + R$ 135,00 = R$ 144,00
        BigDecimal somaSplit = contrato.comissao().getValorComissaoPlataforma().add(contrato.comissao().getValorRepasseClinica());
        assertEquals(new BigDecimal("144.00"), somaSplit);
    }

    @Test
    @DisplayName("Deve validar voucher digital na clínica, atualizar agendamento para REALIZADO e liberar repasse de comissão")
    void deveValidarVoucherELiberarComissao() {
        MarketplaceIntermediacaoService.ContratoIntermediacaoDto contrato = marketplaceService.contratarServicoNoMarketplace(
                1L, 1L, LocalDateTime.now().plusDays(3), "PIX", "Check-up", "tutor"
        );

        String voucher = contrato.transacao().getCodigoVoucher();
        assertNotNull(voucher);

        // Clínica valida o voucher na recepção
        Transacao transacaoValidada = marketplaceService.validarVoucherEAtendimento(voucher);

        assertTrue(transacaoValidada.getVoucherUtilizado());
        assertNotNull(transacaoValidada.getDataUtilizacaoVoucher());

        // Confirma agendamento atualizado
        Agendamento agendamento = agendamentoRepository.findById(contrato.agendamento().getId()).orElseThrow();
        assertEquals(StatusAgendamento.REALIZADO, agendamento.getStatusAgendamento());

        // Confirma comissão liberada
        Comissao comissao = comissaoRepository.findByTransacaoId(transacaoValidada.getId()).orElseThrow();
        assertEquals(StatusRepasseComissao.LIBERADO_APOS_ATENDIMENTO, comissao.getStatusRepasse());

        // Liquidação final do repasse para chave PIX da clínica
        Comissao liquidada = marketplaceService.liquidarRepasseClinica(comissao.getId());
        assertEquals(StatusRepasseComissao.PAGO_LIQUIDADO, liquidada.getStatusRepasse());
        assertNotNull(liquidada.getDataLiquidacaoRepasse());
    }

    @Test
    @DisplayName("Deve rejeitar tentativa de reutilização de voucher digital já consumido")
    void deveRejeitarReutilizacaoDeVoucher() {
        MarketplaceIntermediacaoService.ContratoIntermediacaoDto contrato = marketplaceService.contratarServicoNoMarketplace(
                1L, 1L, LocalDateTime.now().plusDays(2), "PIX", "Teste", "tutor"
        );

        String voucher = contrato.transacao().getCodigoVoucher();
        marketplaceService.validarVoucherEAtendimento(voucher);

        // Segunda tentativa com o mesmo voucher
        assertThrows(IllegalStateException.class, () -> marketplaceService.validarVoucherEAtendimento(voucher));
    }
}
