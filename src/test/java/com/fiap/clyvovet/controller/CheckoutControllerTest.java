package com.fiap.clyvovet.controller;

import com.fiap.clyvovet.model.Pet;
import com.fiap.clyvovet.model.TipoServicoPreventivo;
import com.fiap.clyvovet.model.Tutor;
import com.fiap.clyvovet.repository.PetRepository;
import com.fiap.clyvovet.repository.TutorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private PetRepository petRepository;

    private Pet pet;

    @BeforeEach
    void setUp() {
        Tutor tutor = tutorRepository.findByUsuarioUsername("tutor").orElseThrow();
        pet = petRepository.findByTutorCpfOrderByIdAsc(tutor.getCpf()).stream().findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Acesso ao catálogo de serviços sem login é redirecionado para /login")
    void catalogoSemLoginRedireciona() throws Exception {
        mockMvc.perform(get("/servicos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "tutor", roles = "TUTOR")
    @DisplayName("Tutor autenticado acessa catálogo de serviços preventivos")
    void catalogoComTutorAutenticado() throws Exception {
        mockMvc.perform(get("/servicos"))
                .andExpect(status().isOk())
                .andExpect(view().name("servicos/catalogo"))
                .andExpect(model().attributeExists("servicos", "pets"));
    }

    @Test
    @WithMockUser(username = "tutor", roles = "TUTOR")
    @DisplayName("Tela de checkout exibe resumo de split e pet selecionado")
    void telaCheckoutExibeSplit() throws Exception {
        mockMvc.perform(get("/servicos/checkout")
                        .param("petId", pet.getId().toString())
                        .param("tipoServico", "CONSULTA_PREVENTIVA"))
                .andExpect(status().isOk())
                .andExpect(view().name("servicos/checkout"))
                .andExpect(model().attributeExists("split", "checkoutDto", "pets"));
    }

    @Test
    @WithMockUser(username = "tutor", roles = "TUTOR")
    @DisplayName("Submissão do checkout in-app gera cobrança no gateway e redireciona para a tela de pagamento Pix")
    void submissaoCheckoutInApp() throws Exception {
        mockMvc.perform(post("/servicos/checkout").with(csrf())
                        .param("petId", pet.getId().toString())
                        .param("tipoServico", TipoServicoPreventivo.PACOTE_VACINAL_COMPLETO.name())
                        .param("metodoPagamento", "PIX")
                        .param("observacoes", "Teste de split in-app"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/servicos/pagamento/*"));
    }

    @Test
    @WithMockUser(username = "tutor", roles = "TUTOR")
    @DisplayName("Simulação de confirmação de pagamento Pix redireciona para o voucher ativado")
    void confirmacaoPagamentoSandboxRedirecionaParaVoucher() throws Exception {
        var mvcResult = mockMvc.perform(post("/servicos/checkout").with(csrf())
                        .param("petId", pet.getId().toString())
                        .param("tipoServico", TipoServicoPreventivo.CONSULTA_PREVENTIVA.name())
                        .param("metodoPagamento", "PIX"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirectedUrl = mvcResult.getResponse().getRedirectedUrl();
        assert redirectedUrl != null;
        String agendamentoId = redirectedUrl.substring(redirectedUrl.lastIndexOf('/') + 1);

        mockMvc.perform(post("/servicos/pagamento/" + agendamentoId + "/simular").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/servicos/voucher/" + agendamentoId));
    }

    @Test
    @WithMockUser(username = "tutor", roles = "TUTOR")
    @DisplayName("Tutor comum não pode validar voucher (rota exclusiva ADMIN)")
    void tutorNaoPodeValidarVoucher() throws Exception {
        mockMvc.perform(post("/servicos/validar-voucher").with(csrf())
                        .param("codigoVoucher", "CLYVO-TESTE-123"))
                .andExpect(status().isForbidden());
    }
}
