package com.fiap.clyvovet.config;

import com.fiap.clyvovet.gateway.GatewayPagamentoService;
import com.fiap.clyvovet.gateway.mercadopago.MercadoPagoGatewayService;
import com.fiap.clyvovet.gateway.simulado.SimuladoGatewayService;
import com.fiap.clyvovet.gateway.stripe.StripeGatewayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuração e Injeção do Provedor de Gateway de Pagamento.
 *
 * <p>Seleciona dinamicamente a implementação apropriada com base na presença das
 * credenciais da Stripe ({@code CLYVO_STRIPE_SECRET_KEY} ou {@code STRIPE_SECRET_KEY})
 * ou do Mercado Pago ({@code CLYVO_GATEWAY_ACCESS_TOKEN}). Se nenhuma credencial
 * for fornecida ou o modo for configurado como simulado, ativa automaticamente
 * o {@link SimuladoGatewayService}, garantindo resiliência offline total.</p>
 */
@Configuration
public class GatewayPagamentoConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewayPagamentoConfig.class);

    @Value("${clyvo.gateway.access-token:${CLYVO_GATEWAY_ACCESS_TOKEN:}}")
    private String mpAccessToken;

    @Value("${clyvo.gateway.stripe.secret-key:${CLYVO_STRIPE_SECRET_KEY:${STRIPE_SECRET_KEY:}}}")
    private String stripeSecretKey;

    @Value("${app.url-base:${APP_URL_BASE:http://localhost:8095}}")
    private String appUrlBase;

    @Value("${clyvo.gateway.provider:auto}")
    private String provider;

    @Bean
    @Primary
    public GatewayPagamentoService gatewayPagamentoService(SimuladoGatewayService fallbackService) {
        String prov = (provider != null) ? provider.trim().toLowerCase() : "auto";

        boolean hasStripeKey = stripeSecretKey != null && !stripeSecretKey.trim().isBlank();
        boolean hasMpKey = mpAccessToken != null && !mpAccessToken.trim().isBlank();

        if ("stripe".equals(prov) || ("auto".equals(prov) && hasStripeKey)) {
            log.info("Inicializando Gateway de Pagamento REAL: STRIPE (Chave configurada: {}...)",
                    hasStripeKey ? stripeSecretKey.substring(0, Math.min(10, stripeSecretKey.length())) : "fallback");
            return new StripeGatewayService(stripeSecretKey, appUrlBase, fallbackService);
        }

        if ("mercadopago".equals(prov) || ("auto".equals(prov) && hasMpKey)) {
            log.info("Inicializando Gateway de Pagamento REAL: Mercado Pago (Token configurado: {}...)",
                    hasMpKey ? mpAccessToken.substring(0, Math.min(10, mpAccessToken.length())) : "fallback");
            return new MercadoPagoGatewayService(mpAccessToken, fallbackService);
        }

        log.info("Inicializando Gateway de Pagamento SIMULADO (Fallback autônomo offline ativo).");
        return fallbackService;
    }
}
