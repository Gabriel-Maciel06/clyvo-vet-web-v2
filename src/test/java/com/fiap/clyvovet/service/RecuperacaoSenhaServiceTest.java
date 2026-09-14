package com.fiap.clyvovet.service;

import com.fiap.clyvovet.model.TokenRecuperacaoSenha;
import com.fiap.clyvovet.model.Usuario;
import com.fiap.clyvovet.repository.TokenRecuperacaoSenhaRepository;
import com.fiap.clyvovet.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes do fluxo de "esqueci minha senha" sobre o tutor do seed (V3):
 * username "tutor", e-mail gabriel.tutor@gmail.com, senha original "tutor123".
 */
@SpringBootTest
@Transactional
class RecuperacaoSenhaServiceTest {

    @Autowired
    private RecuperacaoSenhaService recuperacaoSenhaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenRecuperacaoSenhaRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Solicitar recuperação para e-mail existente gera um token válido")
    void solicitarGeraTokenValido() {
        recuperacaoSenhaService.solicitar("gabriel.tutor@gmail.com");

        Usuario usuario = usuarioRepository.findByEmail("gabriel.tutor@gmail.com").orElseThrow();
        assertEquals(1, tokenRepository.findAll().stream()
                .filter(t -> t.getUsuario().getId().equals(usuario.getId())).count());
    }

    @Test
    @DisplayName("Solicitar recuperação para e-mail inexistente não gera token nem lança erro")
    void solicitarParaEmailInexistenteNaoFalha() {
        assertDoesNotThrow(() -> recuperacaoSenhaService.solicitar("ninguem@teste.com"));
        assertTrue(tokenRepository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Token válido redefine a senha e passa a autenticar com BCrypt")
    void redefinirComTokenValidoTrocaSenha() {
        Usuario usuario = usuarioRepository.findByUsername("tutor").orElseThrow();
        LocalDateTime agora = LocalDateTime.now();
        TokenRecuperacaoSenha token = tokenRepository.save(
                new TokenRecuperacaoSenha(usuario, "token-teste-valido", agora, agora.plusMinutes(30)));

        recuperacaoSenhaService.redefinir(token.getToken(), "novaSenha123");

        Usuario atualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("novaSenha123", atualizado.getPassword()));
        assertTrue(tokenRepository.findByToken(token.getToken()).orElseThrow().isUsado());
    }

    @Test
    @DisplayName("Token expirado não pode ser usado")
    void tokenExpiradoEhRejeitado() {
        Usuario usuario = usuarioRepository.findByUsername("tutor").orElseThrow();
        LocalDateTime passado = LocalDateTime.now().minusHours(1);
        TokenRecuperacaoSenha token = tokenRepository.save(
                new TokenRecuperacaoSenha(usuario, "token-expirado", passado.minusMinutes(30), passado));

        assertFalse(recuperacaoSenhaService.tokenValido(token.getToken()));
        assertThrows(IllegalArgumentException.class,
                () -> recuperacaoSenhaService.redefinir(token.getToken(), "novaSenha123"));
    }

    @Test
    @DisplayName("Token já usado não pode ser reaproveitado")
    void tokenUsadoNaoPodeSerReaproveitado() {
        Usuario usuario = usuarioRepository.findByUsername("tutor").orElseThrow();
        LocalDateTime agora = LocalDateTime.now();
        TokenRecuperacaoSenha token = tokenRepository.save(
                new TokenRecuperacaoSenha(usuario, "token-uso-unico", agora, agora.plusMinutes(30)));

        recuperacaoSenhaService.redefinir(token.getToken(), "primeiraTroca123");

        assertThrows(IllegalArgumentException.class,
                () -> recuperacaoSenhaService.redefinir(token.getToken(), "segundaTroca456"));
    }
}
