package com.fiap.clyvovet.controller;

import com.fiap.clyvovet.dto.CheckoutRequestDto;
import com.fiap.clyvovet.gateway.GatewayPagamentoService;
import com.fiap.clyvovet.gateway.stripe.StripeAssinaturaVerificador;
import com.fiap.clyvovet.model.*;
import com.fiap.clyvovet.repository.PetRepository;
import com.fiap.clyvovet.repository.TutorRepository;
import com.fiap.clyvovet.service.PagamentoSplitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Segurança do webhook da Stripe.
 *
 * <p>Este endpoint é público e isento de CSRF, então é alcançável por qualquer pessoa na
 * internet. Os testes abaixo fixam as duas defesas que o tornam seguro: a validação da
 * assinatura HMAC e a reconsulta ao gateway antes de liberar qualquer voucher.</p>
 */
@SpringBootTest(properties = "clyvo.gateway.stripe.webhook-secret=whsec_segredo_de_teste_clyvo")
@AutoConfigureMockMvc
@Transactional
class StripeWebhookControllerTest {

    private static final String SESSION_ID = "cs_test_session_123";
    private static final String CODIGO_GATEWAY = "STRIPE-" + SESSION_ID;

    @Autowired private MockMvc mockMvc;
    @Autowired private PagamentoSplitService pagamentoSplitService;
    @Autowired private TutorRepository tutorRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private com.fiap.clyvovet.repository.TransacaoRepository transacaoRepository;
    @Autowired private StripeAssinaturaVerificador assinaturaVerificador;
    @Autowired private GatewayPagamentoService gatewayPagamentoService;

    private AgendamentoServico agendamentoPendente;

    @BeforeEach
    void setUp() {
        Tutor tutor = tutorRepository.findByUsuarioUsername("tutor").orElseThrow();
        Pet pet = petRepository.findByTutorCpfOrderByIdAsc(tutor.getCpf()).stream().findFirst().orElseThrow();

        CheckoutRequestDto dto = new CheckoutRequestDto();
        dto.setPetId(pet.getId());
        dto.setTipoServico(TipoServicoPreventivo.CONSULTA_PREVENTIVA);
        dto.setMetodoPagamento("CARTAO_CREDITO");

        agendamentoPendente = pagamentoSplitService.iniciarCheckout(dto, "tutor");
        agendamentoPendente.getTransacao().setCodigoTransacaoGateway(CODIGO_GATEWAY);
        transacaoRepository.save(agendamentoPendente.getTransacao());
    }

    private String eventoJson(String tipo) {
        return """
                {
                    "id": "evt_test_12345",
                    "type": "%s",
                    "data": { "object": { "id": "%s", "payment_status": "paid" } }
                }
                """.formatted(tipo, SESSION_ID);
    }

    private StatusPagamento statusAtual() {
        return pagamentoSplitService.buscarPorId(agendamentoPendente.getId()).getStatusPagamento();
    }

    @Test
    @DisplayName("Webhook forjado sem assinatura é recusado e não libera voucher")
    void webhookSemAssinaturaEhRecusado() throws Exception {
        // Regressão da falha original: este mesmo JSON, sem assinatura nenhuma, confirmava
        // o pagamento. Qualquer pessoa na internet conseguia atendimento sem pagar.
        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventoJson("checkout.session.completed")))
                .andExpect(status().isBadRequest());

        assertNotEquals(StatusPagamento.PAGO_CONFIRMADO, statusAtual(),
                "Um webhook sem assinatura jamais pode liquidar uma cobrança");
    }

    @Test
    @DisplayName("Webhook com assinatura inválida é recusado")
    void webhookComAssinaturaInvalidaEhRecusado() throws Exception {
        mockMvc.perform(post("/api/webhooks/stripe")
                        .header("Stripe-Signature", "t=" + Instant.now().getEpochSecond() + ",v1=deadbeef")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventoJson("checkout.session.completed")))
                .andExpect(status().isBadRequest());

        assertNotEquals(StatusPagamento.PAGO_CONFIRMADO, statusAtual());
    }

    @Test
    @DisplayName("Webhook assinado com timestamp antigo é recusado (proteção contra replay)")
    void webhookComTimestampAntigoEhRecusado() throws Exception {
        String payload = eventoJson("checkout.session.completed");
        long antigo = Instant.now().getEpochSecond() - 3600;

        mockMvc.perform(post("/api/webhooks/stripe")
                        .header("Stripe-Signature", assinaturaVerificador.gerarCabecalhoParaTeste(payload, antigo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        assertNotEquals(StatusPagamento.PAGO_CONFIRMADO, statusAtual());
    }

    @Test
    @DisplayName("Webhook assinado não confirma enquanto o gateway não reconhecer o pagamento")
    void webhookAssinadoNaoConfirmaSemPagamentoNoGateway() throws Exception {
        // Assinatura legítima, corpo dizendo "paid", mas o gateway ainda não recebeu nada.
        // É exatamente o ciclo normal do PIX: a sessão completa antes de o dinheiro cair.
        String payload = eventoJson("checkout.session.completed");

        mockMvc.perform(post("/api/webhooks/stripe")
                        .header("Stripe-Signature",
                                assinaturaVerificador.gerarCabecalhoParaTeste(payload, Instant.now().getEpochSecond()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        assertNotEquals(StatusPagamento.PAGO_CONFIRMADO, statusAtual(),
                "O corpo do webhook não é prova de pagamento; quem decide é o gateway");
    }

    @Test
    @DisplayName("Webhook assinado confirma quando o gateway reconhece o pagamento")
    void webhookAssinadoConfirmaComPagamentoNoGateway() throws Exception {
        gatewayPagamentoService.simularPagamento(CODIGO_GATEWAY);

        String payload = eventoJson("checkout.session.async_payment_succeeded");
        mockMvc.perform(post("/api/webhooks/stripe")
                        .header("Stripe-Signature",
                                assinaturaVerificador.gerarCabecalhoParaTeste(payload, Instant.now().getEpochSecond()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        assertEquals(StatusPagamento.PAGO_CONFIRMADO, statusAtual());
    }

    @Test
    @DisplayName("Evento de falha do PIX não liquida a cobrança")
    void eventoDeFalhaNaoLiquida() throws Exception {
        gatewayPagamentoService.simularPagamento(CODIGO_GATEWAY);

        String payload = eventoJson("checkout.session.async_payment_failed");
        mockMvc.perform(post("/api/webhooks/stripe")
                        .header("Stripe-Signature",
                                assinaturaVerificador.gerarCabecalhoParaTeste(payload, Instant.now().getEpochSecond()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        assertNotEquals(StatusPagamento.PAGO_CONFIRMADO, statusAtual(),
                "async_payment_failed significa que o PIX não foi pago");
    }
}
