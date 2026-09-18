package com.fiap.clyvovet.gateway;

import com.fiap.clyvovet.gateway.dto.CobrancaGeradaDto;
import com.fiap.clyvovet.gateway.dto.RequisicaoCobrancaDto;
import com.fiap.clyvovet.gateway.dto.StatusCobrancaDto;
import com.fiap.clyvovet.gateway.simulado.SimuladoGatewayService;
import com.fiap.clyvovet.gateway.stripe.StripeGatewayService;
import com.fiap.clyvovet.model.StatusTransacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class StripeGatewayServiceTest {

    private SimuladoGatewayService simuladoFallback;

    @BeforeEach
    void setUp() {
        simuladoFallback = new SimuladoGatewayService();
    }

    @Test
    @DisplayName("StripeGatewayService sem chave secreta cai silenciosamente no fallback simulado")
    void stripeSemChaveUsaFallback() {
        StripeGatewayService service = new StripeGatewayService("", "http://localhost:8095", simuladoFallback);

        RequisicaoCobrancaDto req = new RequisicaoCobrancaDto(
                "CLYVO-TESTE-STRIPE",
                new BigDecimal("127.50"),
                "Consulta Preventiva",
                "CARTAO_CREDITO",
                "Gabriel Oliveira",
                "gabriel@email.com",
                "12345678901"
        );

        CobrancaGeradaDto cobranca = service.criarCobranca(req);
        assertNotNull(cobranca);
        assertTrue(cobranca.simulado());
        assertTrue(cobranca.idTransacaoGateway().startsWith("SIM-GW-"));
        assertEquals(StatusTransacao.PENDENTE, cobranca.status());
        assertTrue(service.isModoSimulado());
        assertEquals("STRIPE", service.getNomeProvedor());
    }

    @Test
    @DisplayName("StripeGatewayService com chave cria Sessão Checkout e retorna link hosted")
    void stripeCriaSessaoCheckoutComSucesso() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.stripe.com");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        StripeGatewayService service = new StripeGatewayService("sk_test_fake_123", "http://localhost:8095", simuladoFallback, restClient);

        String stripeJsonResponse = """
                {
                  "id": "cs_test_a1b2c3d4e5",
                  "object": "checkout.session",
                  "url": "https://checkout.stripe.com/c/pay/cs_test_a1b2c3d4e5",
                  "payment_status": "unpaid",
                  "status": "open",
                  "expires_at": 1758153600
                }
                """;

        mockServer.expect(requestTo("https://api.stripe.com/v1/checkout/sessions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk_test_fake_123"))
                .andRespond(withSuccess(stripeJsonResponse, MediaType.APPLICATION_JSON));

        RequisicaoCobrancaDto req = new RequisicaoCobrancaDto(
                "CLYVO-STRIPE-001",
                new BigDecimal("127.50"),
                "Consulta Longevidade Canina",
                "CARTAO_CREDITO",
                "Tutor Demo",
                "tutor@clyvo.com",
                "12345678901"
        );

        CobrancaGeradaDto cobranca = service.criarCobranca(req);

        mockServer.verify();
        assertNotNull(cobranca);
        assertEquals("STRIPE-cs_test_a1b2c3d4e5", cobranca.idTransacaoGateway());
        assertEquals(StatusTransacao.PENDENTE, cobranca.status());
        assertEquals("https://checkout.stripe.com/c/pay/cs_test_a1b2c3d4e5", cobranca.linkCheckout());
        assertFalse(cobranca.simulado());
        assertNotNull(cobranca.dataExpiracao());
    }

    @Test
    @DisplayName("StripeGatewayService consulta status de sessão liquidada na Stripe")
    void stripeConsultaStatusPago() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.stripe.com");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        StripeGatewayService service = new StripeGatewayService("sk_test_fake_123", "http://localhost:8095", simuladoFallback, restClient);

        String stripeSessionStatus = """
                {
                  "id": "cs_test_liquidada",
                  "object": "checkout.session",
                  "payment_status": "paid",
                  "status": "complete"
                }
                """;

        mockServer.expect(requestTo("https://api.stripe.com/v1/checkout/sessions/cs_test_liquidada"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer sk_test_fake_123"))
                .andRespond(withSuccess(stripeSessionStatus, MediaType.APPLICATION_JSON));

        StatusCobrancaDto status = service.consultarStatus("STRIPE-cs_test_liquidada");

        mockServer.verify();
        assertNotNull(status);
        assertTrue(status.pago());
        assertEquals(StatusTransacao.PAGO, status.status());
        assertNotNull(status.dataPagamento());
    }
}
