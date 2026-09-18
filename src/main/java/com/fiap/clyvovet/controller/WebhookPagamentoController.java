package com.fiap.clyvovet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.clyvovet.gateway.GatewayPagamentoService;
import com.fiap.clyvovet.gateway.dto.StatusCobrancaDto;
import com.fiap.clyvovet.service.PagamentoSplitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller de Recepção de Webhooks Assíncronos de Pagamento.
 *
 * <p>Endpoint público sem autenticação de sessão ou CSRF, chamado
 * diretamente pelos servidores do Mercado Pago para notificação de
 * alteração de status de pagamentos (PIX / Cartão).</p>
 */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookPagamentoController {

    private static final Logger log = LoggerFactory.getLogger(WebhookPagamentoController.class);

    private final PagamentoSplitService pagamentoSplitService;
    private final GatewayPagamentoService gatewayPagamentoService;
    private final ObjectMapper objectMapper;

    public WebhookPagamentoController(PagamentoSplitService pagamentoSplitService,
                                      GatewayPagamentoService gatewayPagamentoService) {
        this.pagamentoSplitService = pagamentoSplitService;
        this.gatewayPagamentoService = gatewayPagamentoService;
        this.objectMapper = new ObjectMapper();
    }

    @PostMapping("/mercadopago")
    public ResponseEntity<Map<String, String>> receberNotificacaoMercadoPago(
            @RequestBody(required = false) String payload,
            @RequestParam(value = "id", required = false) String paramId,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "type", required = false) String type) {

        log.info("[Webhook] Notificação recebida do Mercado Pago. Topic={}, Type={}, ParamId={}", topic, type, paramId);

        String paymentId = paramId;

        if (payload != null && !payload.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(payload);
                if (root.has("data") && root.path("data").has("id")) {
                    paymentId = root.path("data").path("id").asText();
                } else if (root.has("id")) {
                    paymentId = root.path("id").asText();
                }
            } catch (Exception e) {
                log.warn("[Webhook] Falha ao processar payload JSON do webhook: {}", e.getMessage());
            }
        }

        if (paymentId == null || paymentId.isBlank()) {
            log.warn("[Webhook] Nenhum ID de pagamento identificado na requisição.");
            return ResponseEntity.ok(Map.of("status", "ignored", "reason", "no_payment_id"));
        }

        String codigoGateway = paymentId.startsWith("MP-") ? paymentId : "MP-" + paymentId;
        log.info("[Webhook] Processando atualização para transação gateway: {}", codigoGateway);

        try {
            StatusCobrancaDto statusAtual = gatewayPagamentoService.consultarStatus(codigoGateway);
            if (statusAtual.pago()) {
                pagamentoSplitService.confirmarPagamentoPorCodigoGateway(codigoGateway);
                log.info("[Webhook] Transação {} confirmada e liquidada com sucesso via webhook.", codigoGateway);
            } else {
                log.info("[Webhook] Transação {} recebida mas ainda com status={}", codigoGateway, statusAtual.status());
            }
            return ResponseEntity.ok(Map.of("status", "processed", "paymentId", paymentId));
        } catch (Exception e) {
            log.error("[Webhook] Erro ao processar confirmação para {}: {}", codigoGateway, e.getMessage());
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
