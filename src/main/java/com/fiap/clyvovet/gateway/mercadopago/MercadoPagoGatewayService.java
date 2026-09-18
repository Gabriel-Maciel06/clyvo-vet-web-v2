package com.fiap.clyvovet.gateway.mercadopago;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.clyvovet.gateway.GatewayPagamentoService;
import com.fiap.clyvovet.gateway.dto.CobrancaGeradaDto;
import com.fiap.clyvovet.gateway.dto.RequisicaoCobrancaDto;
import com.fiap.clyvovet.gateway.dto.StatusCobrancaDto;
import com.fiap.clyvovet.gateway.simulado.SimuladoGatewayService;
import com.fiap.clyvovet.model.StatusTransacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Integração Real com a API REST do Mercado Pago (PIX Transparente e Checkout).
 *
 * <p>Utiliza o {@link RestClient} nativo do Spring Framework 6 para geração
 * de cobranças PIX dinâmicas e consulta de status via webhooks/polling.</p>
 */
public class MercadoPagoGatewayService implements GatewayPagamentoService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoGatewayService.class);
    private static final String MP_API_URL = "https://api.mercadopago.com";

    private final String accessToken;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final SimuladoGatewayService fallbackService;

    public MercadoPagoGatewayService(String accessToken, SimuladoGatewayService fallbackService) {
        this.accessToken = accessToken != null ? accessToken.trim() : "";
        this.fallbackService = fallbackService;
        this.objectMapper = new ObjectMapper();
        this.restClient = RestClient.builder()
                .baseUrl(MP_API_URL)
                .build();
    }

    @Override
    public CobrancaGeradaDto criarCobranca(RequisicaoCobrancaDto requisicao) {
        if (accessToken.isBlank()) {
            log.warn("[MercadoPagoGateway] Access Token ausente. Redirecionando para Gateway Simulado.");
            return fallbackService.criarCobranca(requisicao);
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("transaction_amount", requisicao.valor());
            body.put("description", requisicao.descricao() != null ? requisicao.descricao() : "Atendimento Clyvo Vet");
            body.put("payment_method_id", "pix");

            Map<String, Object> payer = new HashMap<>();
            payer.put("email", (requisicao.pagadorEmail() != null && !requisicao.pagadorEmail().isBlank())
                    ? requisicao.pagadorEmail() : "tutor@clyvovet.com.br");
            payer.put("first_name", (requisicao.pagadorNome() != null && !requisicao.pagadorNome().isBlank())
                    ? requisicao.pagadorNome() : "Tutor Clyvo");

            if (requisicao.pagadorCpf() != null && !requisicao.pagadorCpf().isBlank()) {
                String cpfNumeros = requisicao.pagadorCpf().replaceAll("\\D", "");
                if (cpfNumeros.length() == 11) {
                    Map<String, String> identification = new HashMap<>();
                    identification.put("type", "CPF");
                    identification.put("number", cpfNumeros);
                    payer.put("identification", identification);
                }
            }
            body.put("payer", payer);

            String idempotencyKey = UUID.randomUUID().toString();

            String responseBody = restClient.post()
                    .uri("/v1/payments")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            String idTransacao = "MP-" + root.path("id").asText();
            String statusMp = root.path("status").asText();

            StatusTransacao status = "approved".equalsIgnoreCase(statusMp) ? StatusTransacao.PAGO : StatusTransacao.PENDENTE;

            JsonNode transactionData = root.path("point_of_interaction").path("transaction_data");
            String qrCodeCopiaCola = transactionData.path("qr_code").asText("");
            String qrCodeBase64Raw = transactionData.path("qr_code_base64").asText("");
            String ticketUrl = transactionData.path("ticket_url").asText(null);

            String qrCodeBase64 = !qrCodeBase64Raw.isBlank()
                    ? "data:image/png;base64," + qrCodeBase64Raw
                    : null;

            LocalDateTime expiracao = LocalDateTime.now().plusMinutes(30);
            if (root.has("date_of_expiration")) {
                try {
                    expiracao = OffsetDateTime.parse(root.path("date_of_expiration").asText()).toLocalDateTime();
                } catch (Exception ignored) {}
            }

            log.info("[MercadoPagoGateway] Cobrança criada na API Mercado Pago: ID={}, Status={}", idTransacao, statusMp);

            return new CobrancaGeradaDto(
                    idTransacao,
                    status,
                    qrCodeCopiaCola,
                    qrCodeBase64,
                    ticketUrl,
                    expiracao,
                    "Cobrança PIX gerada com sucesso no Mercado Pago",
                    false
            );

        } catch (Exception e) {
            log.error("[MercadoPagoGateway] Erro ao chamar API Mercado Pago: {}. Acionando fallback simulado.", e.getMessage());
            return fallbackService.criarCobranca(requisicao);
        }
    }

    @Override
    public StatusCobrancaDto consultarStatus(String idTransacaoGateway) {
        if (idTransacaoGateway == null || idTransacaoGateway.startsWith("SIM-") || accessToken.isBlank()) {
            return fallbackService.consultarStatus(idTransacaoGateway);
        }

        String mpPaymentId = idTransacaoGateway.replace("MP-", "");
        try {
            String responseBody = restClient.get()
                    .uri("/v1/payments/" + mpPaymentId)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            String statusMp = root.path("status").asText();

            boolean aprovado = "approved".equalsIgnoreCase(statusMp);
            StatusTransacao status = aprovado ? StatusTransacao.PAGO
                    : ("rejected".equalsIgnoreCase(statusMp) || "cancelled".equalsIgnoreCase(statusMp))
                    ? StatusTransacao.FALHOU : StatusTransacao.PENDENTE;

            LocalDateTime dataPagamento = null;
            if (root.has("date_approved") && !root.path("date_approved").isNull()) {
                try {
                    dataPagamento = OffsetDateTime.parse(root.path("date_approved").asText()).toLocalDateTime();
                } catch (Exception ignored) {
                    dataPagamento = LocalDateTime.now();
                }
            }

            return new StatusCobrancaDto(
                    idTransacaoGateway,
                    status,
                    aprovado,
                    dataPagamento,
                    "Status retornado pela API Mercado Pago: " + statusMp
            );

        } catch (Exception e) {
            log.error("[MercadoPagoGateway] Erro ao consultar pagamento {}: {}", mpPaymentId, e.getMessage());
            return fallbackService.consultarStatus(idTransacaoGateway);
        }
    }

    @Override
    public StatusCobrancaDto simularPagamento(String idTransacaoGateway) {
        return fallbackService.simularPagamento(idTransacaoGateway);
    }

    @Override
    public boolean isModoSimulado() {
        return false;
    }

    @Override
    public String getNomeProvedor() {
        return "MERCADO_PAGO";
    }
}
