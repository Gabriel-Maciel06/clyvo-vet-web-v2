package com.fiap.clyvovet.controller;

import com.fiap.clyvovet.dto.CheckoutRequestDto;
import com.fiap.clyvovet.gateway.GatewayPagamentoService;
import com.fiap.clyvovet.model.*;
import com.fiap.clyvovet.repository.PetRepository;
import com.fiap.clyvovet.repository.TransacaoRepository;
import com.fiap.clyvovet.repository.TutorRepository;
import com.fiap.clyvovet.service.PagamentoSplitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Segurança do retorno do Stripe Checkout (`GET /servicos/stripe/retorno`).
 *
 * <p>Era a falha mais fácil de explorar de todo o fluxo: o {@code session_id} vem da barra
 * de endereços, e o endpoint confirmava o pagamento sem perguntar nada à Stripe. Bastava
 * ao tutor iniciar um checkout, abandonar na tela da Stripe e abrir a URL de retorno à mão
 * para receber o voucher sem ter pago.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RetornoStripeSegurancaTest {

    // Um id por execução: o gateway simulado guarda status num mapa em memória que o
    // rollback transacional não desfaz, então ids fixos vazariam estado entre os testes.
    private String sessionId;
    private String codigoGateway;

    @Autowired private MockMvc mockMvc;
    @Autowired private PagamentoSplitService pagamentoSplitService;
    @Autowired private TutorRepository tutorRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private TransacaoRepository transacaoRepository;
    @Autowired private GatewayPagamentoService gatewayPagamentoService;

    private AgendamentoServico agendamentoPendente;

    @BeforeEach
    void setUp() {
        Tutor tutor = tutorRepository.findByUsuarioUsername("tutor").orElseThrow();
        Pet pet = petRepository.findByTutorCpfOrderByIdAsc(tutor.getCpf()).stream().findFirst().orElseThrow();

        CheckoutRequestDto dto = new CheckoutRequestDto();
        dto.setPetId(pet.getId());
        dto.setTipoServico(TipoServicoPreventivo.CONSULTA_PREVENTIVA);
        dto.setMetodoPagamento("PIX");

        sessionId = "cs_test_retorno_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        codigoGateway = "STRIPE-" + sessionId;

        agendamentoPendente = pagamentoSplitService.iniciarCheckout(dto, "tutor");
        agendamentoPendente.getTransacao().setCodigoTransacaoGateway(codigoGateway);
        transacaoRepository.save(agendamentoPendente.getTransacao());
    }

    private StatusPagamento statusAtual() {
        return pagamentoSplitService.buscarPorId(agendamentoPendente.getId()).getStatusPagamento();
    }

    @Test
    @DisplayName("Voltar da Stripe sem ter pago não libera o voucher")
    @WithMockUser(username = "tutor", roles = {"TUTOR"})
    void retornoSemPagamentoNaoLiberaVoucher() throws Exception {
        mockMvc.perform(get("/servicos/stripe/retorno").param("session_id", sessionId))
                .andExpect(status().is3xxRedirection());

        assertNotEquals(StatusPagamento.PAGO_CONFIRMADO, statusAtual(),
                "Abrir a URL de retorno não pode substituir o pagamento");
    }

    @Test
    @DisplayName("Retorno libera o voucher quando o gateway confirma o pagamento")
    @WithMockUser(username = "tutor", roles = {"TUTOR"})
    void retornoComPagamentoConfirmadoLiberaVoucher() throws Exception {
        gatewayPagamentoService.simularPagamento(codigoGateway);

        mockMvc.perform(get("/servicos/stripe/retorno").param("session_id", sessionId))
                .andExpect(status().is3xxRedirection());

        assertEquals(StatusPagamento.PAGO_CONFIRMADO, statusAtual());
    }

    @Test
    @DisplayName("Tutor não confirma pagamento de agendamento que não é dele")
    @WithMockUser(username = "admin", roles = {"TUTOR"})
    void retornoDeOutroTutorEhNegado() throws Exception {
        gatewayPagamentoService.simularPagamento(codigoGateway);

        mockMvc.perform(get("/servicos/stripe/retorno").param("session_id", sessionId))
                .andExpect(status().isForbidden());

        assertNotEquals(StatusPagamento.PAGO_CONFIRMADO, statusAtual());
    }
}
