package com.fiap.clyvovet.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração do Spring Security: autenticação por formulário e
 * proteção de rotas por perfil (ROLE_TUTOR x ROLE_ADMIN).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ControleDeAcessoPorPerfilTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Página de login é pública")
    void loginEhPublico() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Rota protegida sem autenticação redireciona para /login")
    void rotaProtegidaExigeLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Login com credenciais válidas (BCrypt no banco via Flyway) autentica")
    void loginValidoAutentica() throws Exception {
        mockMvc.perform(formLogin("/login").user("tutor").password("tutor123"))
                .andExpect(authenticated().withRoles("TUTOR"))
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("Login com senha errada não autentica")
    void loginInvalidoFalha() throws Exception {
        mockMvc.perform(formLogin("/login").user("tutor").password("senha-errada"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @WithMockUser(username = "tutor", roles = "TUTOR")
    @DisplayName("Tutor NÃO acessa a fila de triagem (rota do veterinário) -> 403")
    void tutorNaoAcessaFilaDeTriagem() throws Exception {
        mockMvc.perform(get("/triagem/fila")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("Veterinário acessa a fila de triagem -> 200")
    void adminAcessaFilaDeTriagem() throws Exception {
        mockMvc.perform(get("/triagem/fila")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("Veterinário NÃO acessa o check-in diário (rota do tutor) -> 403")
    void adminNaoAcessaCheckin() throws Exception {
        mockMvc.perform(get("/checkin/novo")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "tutor", roles = "TUTOR")
    @DisplayName("Tutor acessa o próprio check-in diário -> 200")
    void tutorAcessaCheckin() throws Exception {
        mockMvc.perform(get("/checkin/novo")).andExpect(status().isOk());
    }
}
