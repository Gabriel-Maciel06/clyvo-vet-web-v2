package com.fiap.clyvovet.gateway;

import com.fiap.clyvovet.dto.CheckoutRequestDto;
import com.fiap.clyvovet.gateway.dto.CobrancaGeradaDto;
import com.fiap.clyvovet.gateway.dto.RequisicaoCobrancaDto;
import com.fiap.clyvovet.gateway.dto.StatusCobrancaDto;
import com.fiap.clyvovet.gateway.mercadopago.MercadoPagoGatewayService;
import com.fiap.clyvovet.gateway.simulado.SimuladoGatewayService;
import com.fiap.clyvovet.model.*;
import com.fiap.clyvovet.repository.AgendamentoServicoRepository;
import com.fiap.clyvovet.repository.ComissaoRepository;
import com.fiap.clyvovet.repository.PetRepository;
import com.fiap.clyvovet.repository.TutorRepository;
import com.fiap.clyvovet.service.PagamentoSplitService;
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
class GatewayPagamentoIntegracaoTest {

    @Autowired
    private GatewayPagamentoService gatewayPagamentoService;

    @Autowired
    private SimuladoGatewayService simuladoGatewayService;

    @Autowired
    private PagamentoSplitService pagamentoSplitService;

    @Autowired
    private AgendamentoServicoRepository agendamentoRepository;

    @Autowired
    private ComissaoRepository comissaoRepository;

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private PetRepository petRepository;

    private Tutor tutor;
    private Pet pet;

    @BeforeEach
    void setUp() {
        tutor = tutorRepository.findByUsuarioUsername("tutor").orElseThrow();
        pet = petRepository.findByTutorCpfOrderByIdAsc(tutor.getCpf()).stream().findFirst().orElseThrow();
    }

    @Test
    @DisplayName("SimuladoGatewayService gera cobrança PIX com payload EMV e QR Code em Base64")
    void simuladoGeraCobrancaPixValida() {
        RequisicaoCobrancaDto req = new RequisicaoCobrancaDto(
                "CLYVO-TESTE-01",
                new BigDecimal("127.50"),
                "Consulta Preventiva de Longevidade",
                "PIX",
                "Gabriel Oliveira",
                "gabriel@email.com",
                "12345678901"
        );

        CobrancaGeradaDto cobranca = simuladoGatewayService.criarCobranca(req);

        assertNotNull(cobranca.idTransacaoGateway(), "ID da transação deve ser gerado");
        assertTrue(cobranca.idTransacaoGateway().startsWith("SIM-GW-"));
        assertEquals(StatusTransacao.PENDENTE, cobranca.status());
        assertNotNull(cobranca.qrCodePixCopiaCola(), "Payload Copia e Cola deve ser gerado");
        assertTrue(cobranca.qrCodePixCopiaCola().startsWith("000201"));
        assertNotNull(cobranca.qrCodePixBase64(), "QR Code Base64 deve ser gerado");
        assertTrue(cobranca.qrCodePixBase64().startsWith("data:image/svg+xml;base64,"));
        assertNotNull(cobranca.dataExpiracao());

        // Consulta de status inicial
        StatusCobrancaDto statusInicial = simuladoGatewayService.consultarStatus(cobranca.idTransacaoGateway());
        assertFalse(statusInicial.pago());
        assertEquals(StatusTransacao.PENDENTE, statusInicial.status());

        // Simulação de liquidação
        StatusCobrancaDto statusLiquidado = simuladoGatewayService.simularPagamento(cobranca.idTransacaoGateway());
        assertTrue(statusLiquidado.pago());
        assertEquals(StatusTransacao.PAGO, statusLiquidado.status());
    }

    @Test
    @DisplayName("MercadoPagoGatewayService sem token cai com segurança no fallback simulado")
    void mercadoPagoSemTokenUsaFallback() {
        MercadoPagoGatewayService mpService = new MercadoPagoGatewayService("", simuladoGatewayService);

        RequisicaoCobrancaDto req = new RequisicaoCobrancaDto(
                "CLYVO-TESTE-02",
                new BigDecimal("150.00"),
                "Pacote Vacinal",
                "PIX",
                "Tutor Teste",
                "tutor@teste.com",
                "98765432100"
        );

        CobrancaGeradaDto cobranca = mpService.criarCobranca(req);
        assertNotNull(cobranca);
        assertTrue(cobranca.idTransacaoGateway().startsWith("SIM-GW-"), "Deve usar fallback quando token for vazio");
    }

    @Test
    @DisplayName("Ciclo completo de pagamento: Iniciação PENDENTE -> Confirmação -> PAGO & Escrow retido")
    void cicloCompletoPagamentoInApp() {
        CheckoutRequestDto dto = new CheckoutRequestDto();
        dto.setPetId(pet.getId());
        dto.setTipoServico(TipoServicoPreventivo.CONSULTA_PREVENTIVA);
        dto.setMetodoPagamento("PIX");
        dto.setObservacoes("Teste de ciclo de pagamento e webhook");

        // 1. Iniciação do Checkout
        AgendamentoServico agendamentoPendente = pagamentoSplitService.iniciarCheckout(dto, "tutor");

        assertNotNull(agendamentoPendente.getId());
        assertEquals(StatusPagamento.PENDENTE_PAGAMENTO, agendamentoPendente.getStatusPagamento());
        assertNotNull(agendamentoPendente.getQrCodePixCopiaCola());
        assertNotNull(agendamentoPendente.getQrCodePixBase64());
        assertNotNull(agendamentoPendente.getTransacao());
        assertEquals(StatusTransacao.PENDENTE, agendamentoPendente.getTransacao().getStatusTransacao());

        // 2. Confirmação do Pagamento (simulando retorno do Gateway / Webhook)
        AgendamentoServico agendamentoConfirmado = pagamentoSplitService.confirmarPagamento(agendamentoPendente.getId());

        assertEquals(StatusPagamento.PAGO_CONFIRMADO, agendamentoConfirmado.getStatusPagamento());
        assertNotNull(agendamentoConfirmado.getDataPagamento());
        assertEquals(StatusTransacao.PAGO, agendamentoConfirmado.getTransacao().getStatusTransacao());

        // 3. Verificação de Escrow em T_COMISSAO
        Comissao comissao = comissaoRepository.findByTransacaoId(agendamentoConfirmado.getTransacao().getId()).orElseThrow();
        assertEquals(StatusRepasseComissao.RETIDO_ESCROW, comissao.getStatusRepasse());
        assertTrue(comissao.getValorRepasseClinica().compareTo(BigDecimal.ZERO) > 0);
    }
}
