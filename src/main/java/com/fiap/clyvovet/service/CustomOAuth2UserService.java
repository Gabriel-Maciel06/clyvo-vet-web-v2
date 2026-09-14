package com.fiap.clyvovet.service;

import com.fiap.clyvovet.model.ProviderAutenticacao;
import com.fiap.clyvovet.model.RoleUsuario;
import com.fiap.clyvovet.model.Tutor;
import com.fiap.clyvovet.model.Usuario;
import com.fiap.clyvovet.repository.TutorRepository;
import com.fiap.clyvovet.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Provisiona (ou vincula) automaticamente uma conta local quando alguém entra
 * pela primeira vez com "Continuar com Google". Login social sempre cria um
 * ROLE_TUTOR — contas de veterinário/administrador continuam exclusivamente
 * locais, por segurança.
 * <p>
 * Como {@code T_TUTOR.cpf} é a chave primária e o Google não fornece CPF, um
 * CPF provisório é atribuído; o tutor completa o cadastro no primeiro acesso
 * (ver {@link PerfilService}).
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;

    public CustomOAuth2UserService(UsuarioRepository usuarioRepository, TutorRepository tutorRepository) {
        this.usuarioRepository = usuarioRepository;
        this.tutorRepository = tutorRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String nome = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");
        String foto = oAuth2User.getAttribute("picture");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("A conta Google não retornou um e-mail. Verifique as permissões concedidas.");
        }

        Usuario usuario = provisionarOuVincular(email, nome, googleId, foto);

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority(usuario.getRole().name())),
                oAuth2User.getAttributes(),
                "email"
        );
    }

    @Transactional
    protected Usuario provisionarOuVincular(String email, String nome, String googleId, String foto) {
        return usuarioRepository.findByEmail(email)
                .map(existente -> vincularContaExistente(existente, googleId, foto))
                .orElseGet(() -> provisionarNovoTutor(email, nome, googleId, foto));
    }

    private Usuario vincularContaExistente(Usuario usuario, String googleId, String foto) {
        // Uma conta local com o mesmo e-mail passa a também aceitar login via Google.
        usuario.setProvider(ProviderAutenticacao.GOOGLE);
        usuario.setProviderId(googleId);
        usuario.setAvatarUrl(foto);
        return usuarioRepository.save(usuario);
    }

    private Usuario provisionarNovoTutor(String email, String nome, String googleId, String foto) {
        Usuario usuario = new Usuario();
        usuario.setUsername(email);
        usuario.setEmail(email);
        usuario.setNomeCompleto(nome != null && !nome.isBlank() ? nome : email);
        usuario.setRole(RoleUsuario.ROLE_TUTOR);
        usuario.setProvider(ProviderAutenticacao.GOOGLE);
        usuario.setProviderId(googleId);
        usuario.setAvatarUrl(foto);
        usuario.setPassword(null);
        Usuario salvo = usuarioRepository.save(usuario);

        Tutor tutor = new Tutor();
        tutor.setCpf(PerfilService.PREFIXO_CPF_PROVISORIO + String.format("%08d", salvo.getId()));
        tutor.setNome(salvo.getNomeCompleto());
        tutor.setEmail(email);
        tutor.setUsuario(salvo);
        tutorRepository.save(tutor);

        return salvo;
    }
}
