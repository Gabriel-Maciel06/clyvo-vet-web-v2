package com.fiap.clyvovet.gateway.stripe;

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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Integração Oficial com a API REST da Stripe (Stripe Checkout Hosted com Cartão e PIX).
 *
 * <p>Implementa o {@link GatewayPagamentoService} (Adapter Pattern) utilizando o
 * {@link RestClient} nativo do Spring 6. Comunica-se com a API da Stripe
 * (https://api.stripe.com/v1) através de requisições autenticadas via Bearer Token.</p>
 *
 * <p>Conta com fallback automático para o {@link SimuladoGatewayService} quando a chave
 * secreta estiver ausente ou houver indisponibilidade temporária de rede.</p>
 */
public class StripeGatewayService implements GatewayPagamentoService {

    private static final Logger log = LoggerFactory.getLogger(StripeGatewayService.class);
    private static final String STRIPE_API_URL = "https://api.stripe.com";

    private final String secretKey;
    private final String appUrlBase;
    private final SimuladoGatewayService fallbackService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public StripeGatewayService(String secretKey, String appUrlBase, SimuladoGatewayService fallbackService) {
        this(secretKey, appUrlBase, fallbackService, RestClient.builder().baseUrl(STRIPE_API_URL).build());
    }

    public StripeGatewayService(String secretKey, String appUrlBase, SimuladoGatewayService fallbackService, RestClient restClient) {
        this.secretKey = secretKey != null ? secretKey.trim() : "";
        this.appUrlBase = (appUrlBase != null && !appUrlBase.isBlank())
                ? appUrlBase.replaceAll("/+$", "")
                : "http://localhost:8095";
        this.fallbackService = fallbackService;
        this.restClient = restClient;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public CobrancaGeradaDto criarCobranca(RequisicaoCobrancaDto requisicao) {
        if (secretKey.isBlank()) {
            log.warn("[StripeGateway] Chave secreta da Stripe ausente. Redirecionando para Gateway Simulado.");
            return fallbackService.criarCobranca(requisicao);
        }

        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("mode", "payment");
            formData.add("payment_method_types[0]", "card");
            formData.add("payment_method_types[1]", "pix");

            long centavos = 0L;
            if (requisicao.valor() != null) {
                centavos = requisicao.valor().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
            }
            if (centavos < 50) {
                centavos = 50; // Valor mínimo suportado pela Stripe em BRL
            }

            formData.add("line_items[0][price_data][currency]", "brl");
            formData.add("line_items[0][price_data][unit_amount]", String.valueOf(centavos));
            formData.add("line_items[0][price_data][product_data][name]",
                    requisicao.descricao() != null ? requisicao.descricao() : "Atendimento Veterinário Clyvo");
            formData.add("line_items[0][quantity]", "1");

            if (requisicao.pagadorEmail() != null && !requisicao.pagadorEmail().isBlank()) {
                formData.add("customer_email", requisicao.pagadorEmail());
            }

            if (requisicao.identificadorPedido() != null) {
                formData.add("client_reference_id", requisicao.identificadorPedido());
                formData.add("metadata[codigo_voucher]", requisicao.identificadorPedido());
            }
            if (requisicao.pagadorCpf() != null && !requisicao.pagadorCpf().isBlank()) {
                formData.add("metadata[pagador_cpf]", requisicao.pagadorCpf());
            }
            if (requisicao.pagadorNome() != null && !requisicao.pagadorNome().isBlank()) {
                formData.add("metadata[pagador_nome]", requisicao.pagadorNome());
            }

            formData.add("success_url", appUrlBase + "/servicos/stripe/retorno?session_id={CHECKOUT_SESSION_ID}");
            formData.add("cancel_url", appUrlBase + "/servicos/checkout");

            String responseBody;
            try {
                responseBody = executarPostSessao(formData);
            } catch (Exception e) {
                // Caso a conta Stripe não esteja com o método PIX habilitado nas configurações, tenta criar somente com Cartão
                if (e.getMessage() != null && e.getMessage().toLowerCase().contains("pix")) {
                    log.warn("[StripeGateway] PIX indisponível na conta Stripe, tentando sessão apenas com Cartão...");
                    formData.remove("payment_method_types[1]");
                    responseBody = executarPostSessao(formData);
                } else {
                    throw e;
                }
            }

            JsonNode root = objectMapper.readTree(responseBody);
            String rawSessionId = root.path("id").asText();
            String idTransacao = "STRIPE-" + rawSessionId;
            String checkoutUrl = root.path("url").asText();
            String paymentStatus = root.path("payment_status").asText();

            StatusTransacao status = "paid".equalsIgnoreCase(paymentStatus) ? StatusTransacao.PAGO : StatusTransacao.PENDENTE;

            LocalDateTime expiracao = LocalDateTime.now().plusHours(24);
            if (root.has("expires_at") && !root.path("expires_at").isNull()) {
                long epoch = root.path("expires_at").asLong();
                if (epoch > 0) {
                    expiracao = LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault());
                }
            }

            log.info("[StripeGateway] Sessão Stripe Checkout criada: ID={}, Status={}, URL={}", idTransacao, paymentStatus, checkoutUrl);

            return new CobrancaGeradaDto(
                    idTransacao,
                    status,
                    null,
                    null,
                    checkoutUrl,
                    expiracao,
                    "Sessão de pagamento Stripe Checkout criada com sucesso",
                    false
            );

        } catch (Exception e) {
            log.error("[StripeGateway] Erro ao chamar API Stripe: {}. Acionando fallback simulado.", e.getMessage());
            return fallbackService.criarCobranca(requisicao);
        }
    }

    private String executarPostSessao(MultiValueMap<String, String> formData) {
        return restClient.post()
                .uri("/v1/checkout/sessions")
                .header("Authorization", "Bearer " + secretKey)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(String.class);
    }

    @Override
    public StatusCobrancaDto consultarStatus(String idTransacaoGateway) {
        if (idTransacaoGateway == null || idTransacaoGateway.startsWith("SIM-") || secretKey.isBlank()) {
            return fallbackService.consultarStatus(idTransacaoGateway);
        }

        String sessionId = idTransacaoGateway.replace("STRIPE-", "");
        try {
            String responseBody = restClient.get()
                    .uri("/v1/checkout/sessions/" + sessionId)
                    .header("Authorization", "Bearer " + secretKey)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            String paymentStatus = root.path("payment_status").asText();
            String sessionStatus = root.path("status").asText();

            // So "payment_status: paid" significa dinheiro recebido.
            //
            // A condicao anterior aceitava tambem "status: complete", o que e incorreto
            // para PIX: o PIX e assincrono, entao a sessao fica "complete" assim que o
            // cliente termina o fluxo na tela da Stripe, enquanto o pagamento ainda esta
            // pendente e pode simplesmente expirar sem nunca ser pago (o padrao e 4h, e
            // a confirmacao chega depois, por evento). Aceitar "complete" liberava o
            // voucher de um PIX que nunca foi pago.
            boolean pago = "paid".equalsIgnoreCase(paymentStatus);
            StatusTransacao status;
            if (pago) {
                status = StatusTransacao.PAGO;
            } else if ("expired".equalsIgnoreCase(sessionStatus)
                    || "canceled".equalsIgnoreCase(sessionStatus)
                    || "no_payment_required".equalsIgnoreCase(paymentStatus)) {
                status = StatusTransacao.FALHOU;
            } else {
                status = StatusTransacao.PENDENTE;
            }

            LocalDateTime dataPagamento = pago ? LocalDateTime.now() : null;

            return new StatusCobrancaDto(
                    idTransacaoGateway,
                    status,
                    pago,
                    dataPagamento,
                    "Status retornado pela Stripe Checkout Session: " + paymentStatus + " (" + sessionStatus + ")"
            );
        } catch (Exception e) {
            log.error("[StripeGateway] Erro ao consultar sessão Stripe {}: {}", sessionId, e.getMessage());
            return fallbackService.consultarStatus(idTransacaoGateway);
        }
    }

    @Override
    public StatusCobrancaDto simularPagamento(String idTransacaoGateway) {
        return fallbackService.simularPagamento(idTransacaoGateway);
    }

    @Override
    public boolean isModoSimulado() {
        return secretKey.isBlank();
    }

    @Override
    public String getNomeProvedor() {
        return "STRIPE";
    }
}
