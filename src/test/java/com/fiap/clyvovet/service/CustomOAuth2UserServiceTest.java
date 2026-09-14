package com.fiap.clyvovet.service;

import com.fiap.clyvovet.model.ProviderAutenticacao;
import com.fiap.clyvovet.model.RoleUsuario;
import com.fiap.clyvovet.model.Tutor;
import com.fiap.clyvovet.model.Usuario;
import com.fiap.clyvovet.repository.TutorRepository;
import com.fiap.clyvovet.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa a lógica de provisionamento/vínculo de conta do login com Google
 * diretamente (sem simular a chamada HTTP ao Google), chamando o método
 * protegido {@code provisionarOuVincular} — acessível por estar no mesmo
 * pacote de teste.
 */
@SpringBootTest
@Transactional
class CustomOAuth2UserServiceTest {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TutorRepository tutorRepository;

    @Test
    @DisplayName("E-mail novo: cria Usuario (ROLE_TUTOR, provider GOOGLE) e Tutor com CPF provisório")
    void provisionaNovoTutorViaGoogle() {
        Usuario usuario = customOAuth2UserService.provisionarOuVincular(
                "novo.tutor@gmail.com", "Novo Tutor", "google-sub-123", "https://foto.exemplo/1.png");

        assertEquals(RoleUsuario.ROLE_TUTOR, usuario.getRole());
        assertEquals(ProviderAutenticacao.GOOGLE, usuario.getProvider());
        assertEquals("google-sub-123", usuario.getProviderId());
        assertNull(usuario.getPassword());

        Tutor tutor = tutorRepository.findByUsuarioUsername("novo.tutor@gmail.com").orElseThrow();
        assertTrue(tutor.getCpf().startsWith(PerfilService.PREFIXO_CPF_PROVISORIO));
    }

    @Test
    @DisplayName("E-mail já cadastrado localmente (seed 'tutor'): apenas vincula o Google, sem duplicar conta")
    void vinculaContaLocalExistenteAoGoogle() {
        long totalAntes = usuarioRepository.count();

        Usuario usuario = customOAuth2UserService.provisionarOuVincular(
                "gabriel.tutor@gmail.com", "Gabriel Maciel", "google-sub-999", null);

        assertEquals(totalAntes, usuarioRepository.count(), "não deve criar uma segunda conta");
        assertEquals(ProviderAutenticacao.GOOGLE, usuario.getProvider());
        assertEquals("google-sub-999", usuario.getProviderId());
        assertEquals("tutor", usuario.getUsername(), "o username local original é preservado");
    }

    @Test
    @DisplayName("Login repetido pelo mesmo e-mail Google não cria uma segunda conta")
    void loginRepetidoNaoDuplicaConta() {
        customOAuth2UserService.provisionarOuVincular("repetido@gmail.com", "Repetido", "sub-1", null);
        long totalDepoisDoPrimeiro = usuarioRepository.count();

        customOAuth2UserService.provisionarOuVincular("repetido@gmail.com", "Repetido", "sub-1", null);

        assertEquals(totalDepoisDoPrimeiro, usuarioRepository.count());
    }
}
