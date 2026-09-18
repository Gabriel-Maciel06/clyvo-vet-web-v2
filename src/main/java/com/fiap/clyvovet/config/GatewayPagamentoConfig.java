package com.fiap.clyvovet.config;

import com.fiap.clyvovet.gateway.GatewayPagamentoService;
import com.fiap.clyvovet.gateway.mercadopago.MercadoPagoGatewayService;
import com.fiap.clyvovet.gateway.simulado.SimuladoGatewayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuração e Injeção do Provedor de Gateway de Pagamento.
 *
 * <p>Seleciona dinamicamente a implementação apropriada com base na presença do
 * token de acesso do Mercado Pago (via application.properties ou variável de ambiente
 * {@code CLYVO_GATEWAY_ACCESS_TOKEN}). Se nenhum token for fornecido, ativa
 * automaticamente o {@link SimuladoGatewayService}, garantindo resiliência offline.</p>
 */
@Configuration
public class GatewayPagamentoConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewayPagamentoConfig.class);

    @Value("${clyvo.gateway.access-token:${CLYVO_GATEWAY_ACCESS_TOKEN:}}")
    private String accessToken;

    @Value("${clyvo.gateway.provider:auto}")
    private String provider;

    @Bean
    @Primary
    public GatewayPagamentoService gatewayPagamentoService(SimuladoGatewayService fallbackService) {
        boolean usarMercadoPago = !"simulado".equalsIgnoreCase(provider)
                && accessToken != null
                && !accessToken.trim().isBlank();

        if (usarMercadoPago) {
            log.info("Inicializando Gateway de Pagamento REAL: Mercado Pago (Token configurado: {}...)",
                    accessToken.substring(0, Math.min(10, accessToken.length())));
            return new MercadoPagoGatewayService(accessToken, fallbackService);
        } else {
            log.info("Inicializando Gateway de Pagamento SIMULADO (Fallback autônomo offline ativo).");
            return fallbackService;
        }
    }
}
