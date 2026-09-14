package com.fiap.clyvovet.security;

import com.fiap.clyvovet.model.RoleUsuario;
import com.fiap.clyvovet.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reproduz o bug encontrado ao testar o login com Google de verdade: o Google
 * exige o escopo "openid", o que faz o Spring Security usar OidcUserService em
 * vez do CustomOAuth2UserService configurado — sem a correção em
 * SecurityConfig (oidcUserService(...)), a conta era autenticada mas nunca
 * criada no banco, e /dashboard tentava renderizar sem nenhum Tutor associado.
 * Este teste garante que esse cenário (ROLE_TUTOR autenticado sem linha em
 * T_TUTOR) nunca mais devolva 500 / página em branco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardTutorSemPerfilTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Test
    @DisplayName("ROLE_TUTOR autenticado sem Tutor no banco: dashboard responde 200 em vez de quebrar")
    void dashboardNaoQuebraSemTutorAssociado() throws Exception {
        // Cria só o Usuario (nível baixo), sem passar pelo autocadastro nem pelo login Google —
        // simula exatamente o estado que o bug do OIDC deixava no banco.
        usuarioService.cadastrarUsuario("Sem Perfil", "sem.perfil@teste.com", "sem.perfil", "senha123", RoleUsuario.ROLE_TUTOR);

        mockMvc.perform(get("/dashboard").with(user("sem.perfil").roles("TUTOR")))
                .andExpect(status().isOk());
    }
}
