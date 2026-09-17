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

/**
 * Prova que o checkout in-app alimenta o ledger normalizado em 3FN.
 *
 * <p>Antes da V14 o fluxo vivo gravava so em T_AGENDAMENTO_SERVICO e as tabelas
 * T_AGENDAMENTO/T_TRANSACAO/T_COMISSAO eram schema morto, exercitado apenas por
 * testes que nunca passavam por um controller. Os snapshots imutaveis da V13
 * tambem nao tinham escritor nenhum em Java.</p>
 */
@SpringBootTest
@Transactional
class CheckoutLedgerIntegracaoTest {

    @Autowired private PagamentoSplitService pagamentoSplitService;
    @Autowired private SplitFinanceiroCalculator splitCalculator;
    @Autowired private TutorRepository tutorRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private RecompensaTutorRepository recompensaTutorRepository;
    @Autowired private TransacaoRepository transacaoRepository;
    @Autowired private ComissaoRepository comissaoRepository;
    @Autowired private ServicoRepository servicoRepository;

    private Tutor tutor;
    private Pet pet;

    @BeforeEach
    void setUp() {
        tutor = tutorRepository.findByUsuarioUsername("tutor").orElseThrow();
        pet = petRepository.findByTutorCpfOrderByIdAsc(tutor.getCpf()).stream().findFirst().orElseThrow();
    }

    private void definirFidelidade(int desconto, String nivel) {
        RecompensaTutor r = recompensaTutorRepository.findByTutorCpf(tutor.getCpf()).orElseGet(() -> {
            RecompensaTutor nova = new RecompensaTutor();
            nova.setTutorCpf(tutor.getCpf());
            return nova;
        });
        r.setDescontoPercentual(desconto);
        r.setNivelFidelidade(nivel);
        recompensaTutorRepository.save(r);
    }

    @Test
    @DisplayName("Todo serviço do app tem linha correspondente no catálogo T_SERVICO com o mesmo preço")
    void catalogoCobreTodosOsServicosDoApp() {
        for (TipoServicoPreventivo tipo : TipoServicoPreventivo.values()) {
            Servico catalogo = servicoRepository.findFirstByCodigoServicoAppAndAtivoTrue(tipo.name())
                    .orElseThrow(() -> new AssertionError("Sem linha de catálogo para " + tipo.name()));

            assertEquals(0, tipo.getValorBase().compareTo(catalogo.getPrecoBase()),
                    "Preço do app e do catálogo devem ser o mesmo número para " + tipo.name()
                            + " (senão o snapshot_preco_catalogo registra um valor que o tutor nunca pagou)");
            assertNotNull(catalogo.getClinica(), "O serviço precisa de uma clínica ofertante real");
        }
    }

    @Test
    @DisplayName("Checkout grava agendamento, transação e comissão no ledger normalizado")
    void checkoutAlimentaLedgerNormalizado() {
        definirFidelidade(10, "PRATA");

        CheckoutRequestDto dto = new CheckoutRequestDto();
        dto.setPetId(pet.getId());
        dto.setTipoServico(TipoServicoPreventivo.CONSULTA_PREVENTIVA);
        dto.setMetodoPagamento("PIX");

        AgendamentoServico voucher = pagamentoSplitService.processarCheckout(dto, "tutor");

        Transacao transacao = voucher.getTransacao();
        assertNotNull(transacao, "O voucher precisa apontar para a transação do ledger");
        assertEquals(StatusTransacao.PAGO, transacao.getStatusTransacao());
        assertEquals(voucher.getCodigoVoucher(), transacao.getCodigoVoucher());

        Agendamento agendamentoLedger = transacao.getAgendamento();
        assertNotNull(agendamentoLedger, "A transação precisa estar ligada a um agendamento normalizado");
        assertEquals(StatusAgendamento.CONFIRMADO, agendamentoLedger.getStatusAgendamento());
        assertNotNull(agendamentoLedger.getClinica(), "A clínica agora vem de T_CLINICA, não de texto fixo");
        assertNotNull(agendamentoLedger.getServico(), "O serviço agora vem do catálogo T_SERVICO");

        Comissao comissao = comissaoRepository.findByTransacaoId(transacao.getId()).orElseThrow();
        assertEquals(StatusRepasseComissao.RETIDO_ESCROW, comissao.getStatusRepasse());
        assertEquals(0, voucher.getValorRepasseClinica().compareTo(comissao.getValorRepasseClinica()),
                "Projeção de leitura e ledger não podem divergir no valor do repasse");
    }

    @Test
    @DisplayName("Snapshots imutáveis da V13 são gravados pelo checkout, não apenas pelo seed")
    void snapshotsImutaveisSaoGravadosNoCheckout() {
        definirFidelidade(15, "OURO");

        CheckoutRequestDto dto = new CheckoutRequestDto();
        dto.setPetId(pet.getId());
        dto.setTipoServico(TipoServicoPreventivo.CHECKUP_LONGEVIDADE_SENIOR);
        dto.setMetodoPagamento("CARTAO_CREDITO");

        AgendamentoServico voucher = pagamentoSplitService.processarCheckout(dto, "tutor");
        Transacao transacao = voucher.getTransacao();

        assertEquals(0, new BigDecimal("290.00").compareTo(transacao.getSnapshotPrecoCatalogo()),
                "snapshot_preco_catalogo deve registrar o preço vigente no ato da captura");
        assertEquals(0, new BigDecimal("15.00").compareTo(transacao.getSnapshotTaxaDescontoPct()));
        assertEquals("OURO", transacao.getSnapshotNivelFidelidade());

        Comissao comissao = comissaoRepository.findByTransacaoId(transacao.getId()).orElseThrow();
        assertNotNull(comissao.getSnapshotTaxaComissaoVigente(), "A taxa contratual vigente precisa ficar congelada");
        assertEquals(0, new BigDecimal("75.00").compareTo(comissao.getSnapshotPisoRepassePct()));
    }

    @Test
    @DisplayName("Baixa do voucher propaga para o ledger e libera a comissão do escrow")
    void baixaDoVoucherPropagaParaOLedger() {
        definirFidelidade(5, "BRONZE");

        CheckoutRequestDto dto = new CheckoutRequestDto();
        dto.setPetId(pet.getId());
        dto.setTipoServico(TipoServicoPreventivo.EXAMES_LABORATORIAIS_PREVENTIVOS);
        dto.setMetodoPagamento("PIX");

        AgendamentoServico voucher = pagamentoSplitService.processarCheckout(dto, "tutor");
        Long transacaoId = voucher.getTransacao().getId();

        pagamentoSplitService.validarEBaixarVoucher(voucher.getCodigoVoucher(), "admin");

        Transacao transacao = transacaoRepository.findById(transacaoId).orElseThrow();
        assertTrue(transacao.getVoucherUtilizado(), "O ledger não pode ficar para trás da UI");
        assertNotNull(transacao.getDataUtilizacaoVoucher());
        assertEquals(StatusAgendamento.REALIZADO, transacao.getAgendamento().getStatusAgendamento());

        Comissao comissao = comissaoRepository.findByTransacaoId(transacaoId).orElseThrow();
        assertEquals(StatusRepasseComissao.LIBERADO_APOS_ATENDIMENTO, comissao.getStatusRepasse(),
                "A comissão precisa sair do escrow quando o atendimento acontece");
    }

    @Test
    @DisplayName("Prejuízo da plataforma ao honrar o piso é registrado, não descartado em silêncio")
    void prejuizoAoHonrarOPisoEhRegistrado() {
        // Desconto de 30% ultrapassa o ponto em que a comissão da Clyvo zera:
        // repasse preliminar = 85% - 15% = 70% do valor de tabela, abaixo do piso de 75%.
        SplitFinanceiroCalculator.Resultado r = splitCalculator.calcular(new BigDecimal("100.00"), 30);

        assertTrue(r.pisoProtegidoAplicado(), "O piso deve ter sido acionado");
        assertEquals(0, BigDecimal.ZERO.compareTo(r.valorComissaoClyvo()), "A Clyvo zera o take-rate antes de tudo");
        assertEquals(0, new BigDecimal("75.00").compareTo(r.valorRepasseClinica()), "A clínica recebe o piso integral");
        assertEquals(0, new BigDecimal("70.00").compareTo(r.valorFinal()), "O desconto do tutor é preservado");
        assertEquals(0, new BigDecimal("5.00").compareTo(r.valorPrejuizoPlataforma()),
                "Os R$ 5,00 que a plataforma paga do próprio caixa precisam aparecer no livro-razão");
    }

    @Test
    @DisplayName("Nos níveis vigentes (até 20%) a plataforma nunca opera no prejuízo")
    void niveisVigentesNaoGeramPrejuizo() {
        for (int desconto : new int[]{0, 5, 10, 15, 20}) {
            SplitFinanceiroCalculator.Resultado r = splitCalculator.calcular(new BigDecimal("150.00"), desconto);

            assertEquals(0, BigDecimal.ZERO.compareTo(r.valorPrejuizoPlataforma()),
                    "Desconto de " + desconto + "% não deveria gerar prejuízo");
            assertTrue(r.valorComissaoClyvo().signum() >= 0, "Take-rate nunca pode ficar negativo");
            assertEquals(0, r.valorFinal().compareTo(r.valorComissaoClyvo().add(r.valorRepasseClinica())),
                    "Comissão + repasse devem fechar exatamente o valor pago pelo tutor");
        }
    }
}
