package com.fiap.clyvovet.controller;

import com.fiap.clyvovet.dto.CheckoutRequestDto;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StripeWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PagamentoSplitService pagamentoSplitService;

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private com.fiap.clyvovet.repository.TransacaoRepository transacaoRepository;

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
        agendamentoPendente.getTransacao().setCodigoTransacaoGateway("STRIPE-cs_test_session_123");
        transacaoRepository.save(agendamentoPendente.getTransacao());
    }

    @Test
    @DisplayName("Webhook da Stripe é público, sem CSRF e confirma evento checkout.session.completed")
    void webhookStripePublicoProcessaEvento() throws Exception {
        String rawSessionId = "cs_test_session_123";

        String jsonStripeWebhook = """
                {
                    "id": "evt_test_12345",
                    "type": "checkout.session.completed",
                    "data": {
                        "object": {
                            "id": "%s",
                            "payment_status": "paid",
                            "client_reference_id": "%s"
                        }
                    }
                }
                """.formatted(rawSessionId, agendamentoPendente.getCodigoVoucher());

        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonStripeWebhook))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true));

        AgendamentoServico atualizado = pagamentoSplitService.buscarPorId(agendamentoPendente.getId());
        assertEquals(StatusPagamento.PAGO_CONFIRMADO, atualizado.getStatusPagamento());
    }

    @Test
    @DisplayName("Webhook da Stripe com payload vazio ou evento irrelevante retorna 200 OK sem falhar")
    void webhookStripePayloadVazioRetornaOk() throws Exception {
        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true));

        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"customer.created\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true));
    }
}
