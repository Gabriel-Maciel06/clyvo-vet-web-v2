package com.fiap.clyvovet.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração (MockMvc) do autocadastro público de tutores e do
 * fluxo de recuperação de senha.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CadastroERecuperacaoSenhaTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Tela de cadastro é pública")
    void cadastroEhPublico() throws Exception {
        mockMvc.perform(get("/cadastro")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Cadastro válido cria a conta e redireciona para o login")
    void cadastroValidoCriaConta() throws Exception {
        mockMvc.perform(post("/cadastro").with(csrf())
                        .param("nomeCompleto", "Maria da Silva")
                        .param("cpf", "98765432100")
                        .param("telefone", "(11) 91111-2222")
                        .param("email", "maria.silva@teste.com")
                        .param("username", "maria.silva")
                        .param("senha", "senha123")
                        .param("confirmarSenha", "senha123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?cadastro=sucesso"));
    }

    @Test
    @DisplayName("Cadastro com senhas diferentes é rejeitado e permanece na tela")
    void cadastroComSenhasDiferentesFalha() throws Exception {
        mockMvc.perform(post("/cadastro").with(csrf())
                        .param("nomeCompleto", "João Pereira")
                        .param("cpf", "11122233344")
                        .param("telefone", "(11) 93333-4444")
                        .param("email", "joao.pereira@teste.com")
                        .param("username", "joao.pereira")
                        .param("senha", "senha123")
                        .param("confirmarSenha", "outrasenha"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro"));
    }

    @Test
    @DisplayName("Cadastro com username já existente (seed 'tutor') é rejeitado com mensagem")
    void cadastroComUsernameDuplicadoFalha() throws Exception {
        mockMvc.perform(post("/cadastro").with(csrf())
                        .param("nomeCompleto", "Outro Tutor")
                        .param("cpf", "55544433322")
                        .param("telefone", "(11) 95555-6666")
                        .param("email", "outro.tutor@teste.com")
                        .param("username", "tutor") // já existe no seed V3
                        .param("senha", "senha123")
                        .param("confirmarSenha", "senha123"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("Tela de recuperação de senha é pública")
    void recuperarSenhaEhPublica() throws Exception {
        mockMvc.perform(get("/recuperar-senha")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Solicitar recuperação sempre mostra a mesma mensagem, exista ou não o e-mail")
    void solicitarRecuperacaoNaoRevelaExistenciaDoEmail() throws Exception {
        mockMvc.perform(post("/recuperar-senha").with(csrf()).param("email", "nao.existe@teste.com"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("successMessage"));

        mockMvc.perform(post("/recuperar-senha").with(csrf()).param("email", "gabriel.tutor@gmail.com")) // seed V3
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("Link de redefinição com token inválido é rejeitado")
    void redefinirSenhaComTokenInvalido() throws Exception {
        mockMvc.perform(get("/redefinir-senha").param("token", "token-que-nao-existe"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("tokenInvalido", true));
    }

    @Test
    @DisplayName("Tela de login sempre exibe o botão Continuar com Google")
    void loginExibeBotaoGoogle() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Continuar com Google")));
    }

    @Test
    @DisplayName("Login Google Demo é público, provisiona usuário e redireciona para completar cadastro")
    void loginGoogleDemoAutenticaERedireciona() throws Exception {
        mockMvc.perform(get("/login/google-demo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/perfil/completar-cadastro"));
    }
}
