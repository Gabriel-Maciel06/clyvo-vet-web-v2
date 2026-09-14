package com.fiap.clyvovet.service;

import com.fiap.clyvovet.model.Usuario;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * O Google exige o escopo "openid" (login social baseado em OIDC, não OAuth2
 * puro). Quando esse escopo está presente, o Spring Security usa
 * {@code OidcUserService} para carregar o usuário — e IGNORA o
 * {@link CustomOAuth2UserService} configurado via
 * {@code userInfoEndpoint().userService(...)}, que só vale para provedores
 * OAuth2 sem OpenID Connect.
 * <p>
 * Por isso esta classe existe: reaproveita a mesma lógica de provisionamento/
 * vínculo de conta ({@link CustomOAuth2UserService#provisionarOuVincular}),
 * só que a partir dos dados devolvidos pelo fluxo OIDC.
 */
@Service
public class CustomOidcUserService extends OidcUserService {

    private final CustomOAuth2UserService customOAuth2UserService;

    public CustomOidcUserService(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();
        String nome = oidcUser.getFullName();
        String googleId = oidcUser.getSubject();
        String foto = oidcUser.getPicture();

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("A conta Google não retornou um e-mail. Verifique as permissões concedidas.");
        }

        Usuario usuario = customOAuth2UserService.provisionarOuVincular(email, nome, googleId, foto);

        return new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority(usuario.getRole().name())),
                userRequest.getIdToken(),
                oidcUser.getUserInfo(),
                "email"
        );
    }
}
