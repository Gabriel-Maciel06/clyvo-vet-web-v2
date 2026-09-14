package com.fiap.clyvovet.service;

import com.fiap.clyvovet.model.ProviderAutenticacao;
import com.fiap.clyvovet.model.TokenRecuperacaoSenha;
import com.fiap.clyvovet.model.Usuario;
import com.fiap.clyvovet.repository.TokenRecuperacaoSenhaRepository;
import com.fiap.clyvovet.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fluxo de "esqueci minha senha": gera um token de uso único válido por 30
 * minutos, envia por e-mail o link de redefinição e, ao ser confirmado,
 * troca a senha do usuário (sempre com BCrypt).
 */
@Service
public class RecuperacaoSenhaService {

    private static final int VALIDADE_MINUTOS = 30;

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacaoSenhaRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificacaoEmailService notificacaoEmailService;
    private final String urlBase;

    public RecuperacaoSenhaService(UsuarioRepository usuarioRepository,
                                    TokenRecuperacaoSenhaRepository tokenRepository,
                                    PasswordEncoder passwordEncoder,
                                    NotificacaoEmailService notificacaoEmailService,
                                    org.springframework.core.env.Environment env) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificacaoEmailService = notificacaoEmailService;
        this.urlBase = env.getProperty("app.url-base", "http://localhost:8095");
    }

    /**
     * Sempre retorna normalmente (sem indicar se o e-mail existe ou não, para
     * não expor quais e-mails estão cadastrados). Contas Google não recebem
     * token, pois não têm senha local a redefinir.
     */
    @Transactional
    public void solicitar(String email) {
        usuarioRepository.findByEmail(email)
                .filter(usuario -> usuario.getProvider() == ProviderAutenticacao.LOCAL)
                .ifPresent(this::gerarEEnviarToken);
    }

    private void gerarEEnviarToken(Usuario usuario) {
        String token = UUID.randomUUID().toString();
        LocalDateTime agora = LocalDateTime.now();
        TokenRecuperacaoSenha entidade = new TokenRecuperacaoSenha(
                usuario, token, agora, agora.plusMinutes(VALIDADE_MINUTOS));
        tokenRepository.save(entidade);

        String link = urlBase + "/redefinir-senha?token=" + token;
        String corpo = "Olá, " + usuario.getNomeCompleto() + "!\n\n"
                + "Recebemos um pedido para redefinir sua senha na Clyvo Vet.\n"
                + "Clique no link abaixo para escolher uma nova senha (válido por "
                + VALIDADE_MINUTOS + " minutos):\n\n" + link + "\n\n"
                + "Se você não solicitou isso, ignore este e-mail.";
        notificacaoEmailService.enviar(usuario.getEmail(), "Clyvo Vet - Redefinição de senha", corpo);
    }

    public boolean tokenValido(String token) {
        return tokenRepository.findByToken(token)
                .map(TokenRecuperacaoSenha::isValido)
                .orElse(false);
    }

    @Transactional
    public void redefinir(String token, String novaSenha) {
        TokenRecuperacaoSenha entidade = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Link de redefinição inválido."));
        if (!entidade.isValido()) {
            throw new IllegalArgumentException("Este link de redefinição expirou ou já foi utilizado.");
        }

        Usuario usuario = entidade.getUsuario();
        usuario.setPassword(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);

        entidade.setUsado(true);
        tokenRepository.save(entidade);
    }
}
