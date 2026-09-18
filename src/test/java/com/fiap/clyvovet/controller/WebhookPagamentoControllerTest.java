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
class WebhookPagamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PagamentoSplitService pagamentoSplitService;

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private PetRepository petRepository;

    private AgendamentoServico agendamentoPendente;

    @BeforeEach
    void setUp() {
        Tutor tutor = tutorRepository.findByUsuarioUsername("tutor").orElseThrow();
        Pet pet = petRepository.findByTutorCpfOrderByIdAsc(tutor.getCpf()).stream().findFirst().orElseThrow();

        CheckoutRequestDto dto = new CheckoutRequestDto();
        dto.setPetId(pet.getId());
        dto.setTipoServico(TipoServicoPreventivo.PACOTE_VACINAL_COMPLETO);
        dto.setMetodoPagamento("PIX");

        agendamentoPendente = pagamentoSplitService.iniciarCheckout(dto, "tutor");
    }

    @Test
    @DisplayName("Webhook do Mercado Pago é acessível publicamente sem CSRF e processa notificação")
    void webhookPublicoSemCsrf() throws Exception {
        String codigoGateway = agendamentoPendente.getTransacao().getCodigoTransacaoGateway();

        // Simula primeiro a liquidação no gateway
        pagamentoSplitService.simularConfirmacaoPagamento(agendamentoPendente.getId());

        String jsonWebhook = """
                {
                    "action": "payment.updated",
                    "data": {
                        "id": "%s"
                    },
                    "type": "payment"
                }
                """.formatted(codigoGateway);

        mockMvc.perform(post("/api/webhooks/mercadopago")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWebhook))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("processed"));

        AgendamentoServico atualizado = pagamentoSplitService.buscarPorId(agendamentoPendente.getId());
        assertEquals(StatusPagamento.PAGO_CONFIRMADO, atualizado.getStatusPagamento());
    }

    @Test
    @DisplayName("Webhook sem ID de pagamento é ignorado graciosamente sem erro 500")
    void webhookSemIdIgnorado() throws Exception {
        mockMvc.perform(post("/api/webhooks/mercadopago")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ignored"));
    }
}
