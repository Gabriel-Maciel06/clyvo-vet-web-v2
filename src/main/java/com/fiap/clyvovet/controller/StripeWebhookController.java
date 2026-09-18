package com.fiap.clyvovet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.clyvovet.service.PagamentoSplitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller de Recepção de Webhooks Assíncronos da Stripe.
 *
 * <p>Endpoint público e isento de CSRF chamado pelos servidores da Stripe Cloud
 * (https://stripe.com) quando eventos de pagamento são disparados
 * (ex: {@code checkout.session.completed} ou {@code payment_intent.succeeded}).</p>
 */
@RestController
@RequestMapping("/api/webhooks")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final PagamentoSplitService pagamentoSplitService;
    private final ObjectMapper objectMapper;

    public StripeWebhookController(PagamentoSplitService pagamentoSplitService) {
        this.pagamentoSplitService = pagamentoSplitService;
        this.objectMapper = new ObjectMapper();
    }

    @PostMapping("/stripe")
    public ResponseEntity<Map<String, Object>> receberNotificacaoStripe(@RequestBody(required = false) String payload) {
        log.info("[StripeWebhook] Notificação recebida da Stripe Cloud.");

        if (payload == null || payload.isBlank()) {
            log.warn("[StripeWebhook] Payload vazio recebido.");
            return ResponseEntity.ok(Map.of("received", true, "status", "empty_payload"));
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.path("type").asText();
            log.info("[StripeWebhook] Evento identificado: {}", eventType);

            if ("checkout.session.completed".equalsIgnoreCase(eventType)) {
                JsonNode sessionObj = root.path("data").path("object");
                String sessionId = sessionObj.path("id").asText();
                String paymentStatus = sessionObj.path("payment_status").asText();
                String clientRef = sessionObj.path("client_reference_id").asText(null);

                if ("paid".equalsIgnoreCase(paymentStatus)) {
                    String codigoGateway = sessionId.startsWith("STRIPE-") ? sessionId : "STRIPE-" + sessionId;
                    log.info("[StripeWebhook] Confirmando pagamento para sessão: {} (client_ref: {})", codigoGateway, clientRef);
                    try {
                        try {
                            pagamentoSplitService.confirmarPagamentoPorCodigoGateway(codigoGateway);
                        } catch (IllegalArgumentException e) {
                            if (clientRef != null && !clientRef.isBlank()) {
                                var ag = pagamentoSplitService.buscarPorCodigoVoucher(clientRef);
                                pagamentoSplitService.confirmarPagamento(ag.getId());
                            } else {
                                throw e;
                            }
                        }
                        log.info("[StripeWebhook] Transação {} confirmada com sucesso via webhook.", codigoGateway);
                    } catch (Exception e) {
                        log.warn("[StripeWebhook] Aviso ao confirmar transação {}: {}", codigoGateway, e.getMessage());
                    }
                }
            }

            return ResponseEntity.ok(Map.of("received", true));
        } catch (Exception e) {
            log.error("[StripeWebhook] Erro ao processar webhook da Stripe: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("received", true, "error", e.getMessage()));
        }
    }
}
