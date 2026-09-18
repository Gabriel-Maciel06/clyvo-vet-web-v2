package com.fiap.clyvovet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.clyvovet.gateway.stripe.StripeAssinaturaVerificador;
import com.fiap.clyvovet.service.PagamentoSplitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * Recepcao dos webhooks assincronos da Stripe.
 *
 * <p>Endpoint publico e isento de CSRF, porque quem chama e o servidor da Stripe e nao
 * um navegador com sessao. Ser publico torna as duas verificacoes abaixo obrigatorias:</p>
 *
 * <ol>
 *   <li><strong>Assinatura.</strong> O cabecalho {@code Stripe-Signature} e validado por
 *       HMAC-SHA256 contra o segredo do endpoint. Sem isso o endpoint aceitaria qualquer
 *       JSON vindo da internet dizendo que uma cobranca foi paga.</li>
 *   <li><strong>Reconsulta.</strong> Mesmo com assinatura valida, o corpo nunca e usado
 *       como prova de pagamento: quem confirma e
 *       {@link PagamentoSplitService#confirmarPagamentoPorCodigoGateway(String)}, que
 *       pergunta o status a propria Stripe antes de liberar o voucher.</li>
 * </ol>
 *
 * <p>Os eventos tratados cobrem o ciclo do PIX, que e assincrono: a sessao completa antes
 * de o dinheiro chegar, e a confirmacao vem depois em
 * {@code checkout.session.async_payment_succeeded}.</p>
 */
@RestController
@RequestMapping("/api/webhooks")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private static final Set<String> EVENTOS_DE_LIQUIDACAO = Set.of(
            "checkout.session.completed",
            "checkout.session.async_payment_succeeded"
    );

    private static final Set<String> EVENTOS_DE_FALHA = Set.of(
            "checkout.session.async_payment_failed",
            "checkout.session.expired"
    );

    private final PagamentoSplitService pagamentoSplitService;
    private final StripeAssinaturaVerificador assinaturaVerificador;
    private final ObjectMapper objectMapper;

    public StripeWebhookController(PagamentoSplitService pagamentoSplitService,
                                   StripeAssinaturaVerificador assinaturaVerificador) {
        this.pagamentoSplitService = pagamentoSplitService;
        this.assinaturaVerificador = assinaturaVerificador;
        this.objectMapper = new ObjectMapper();
    }

    @PostMapping("/stripe")
    public ResponseEntity<Map<String, Object>> receberNotificacaoStripe(
            @RequestBody(required = false) String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String assinatura) {

        if (!assinaturaVerificador.assinaturaValida(payload, assinatura)) {
            // 400 e o que a Stripe espera quando a assinatura nao confere; ela reenvia
            // os eventos legitimos, entao recusar aqui nao perde notificacao real.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("received", false, "error", "assinatura_invalida"));
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.path("type").asText();
            JsonNode sessao = root.path("data").path("object");
            String sessionId = sessao.path("id").asText();

            if (sessionId.isBlank()) {
                return ResponseEntity.ok(Map.of("received", true, "status", "sem_sessao"));
            }

            String codigoGateway = sessionId.startsWith("STRIPE-") ? sessionId : "STRIPE-" + sessionId;

            if (EVENTOS_DE_FALHA.contains(eventType)) {
                log.info("[StripeWebhook] Evento {} para {}: cobrança não liquidada, nada a confirmar.",
                        eventType, codigoGateway);
                return ResponseEntity.ok(Map.of("received", true, "status", "nao_pago"));
            }

            if (!EVENTOS_DE_LIQUIDACAO.contains(eventType)) {
                log.debug("[StripeWebhook] Evento {} ignorado.", eventType);
                return ResponseEntity.ok(Map.of("received", true, "status", "ignorado"));
            }

            try {
                // O corpo diz que pagou; a confirmação pergunta à Stripe se é verdade.
                pagamentoSplitService.confirmarPagamentoPorCodigoGateway(codigoGateway);
                log.info("[StripeWebhook] Transação {} confirmada após verificação junto à Stripe.", codigoGateway);
                return ResponseEntity.ok(Map.of("received", true, "status", "confirmado"));
            } catch (PagamentoSplitService.PagamentoNaoConfirmadoException e) {
                // Caminho normal do PIX: a sessão completa antes de o dinheiro chegar.
                // O evento async_payment_succeeded chega depois e liquida.
                log.info("[StripeWebhook] {} ainda não liquidada junto à Stripe: {}", codigoGateway, e.getMessage());
                return ResponseEntity.ok(Map.of("received", true, "status", "aguardando_liquidacao"));
            } catch (IllegalArgumentException e) {
                log.warn("[StripeWebhook] Nenhuma transação local para {}: {}", codigoGateway, e.getMessage());
                return ResponseEntity.ok(Map.of("received", true, "status", "transacao_desconhecida"));
            }

        } catch (Exception e) {
            log.error("[StripeWebhook] Erro ao processar webhook da Stripe: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("received", true, "status", "erro"));
        }
    }
}
