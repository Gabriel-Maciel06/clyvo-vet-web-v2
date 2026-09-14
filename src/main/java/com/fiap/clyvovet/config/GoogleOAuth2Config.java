package com.fiap.clyvovet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * Registra o cliente OAuth2 do Google só quando as credenciais existem no
 * ambiente ({@code GOOGLE_CLIENT_ID} e {@code GOOGLE_CLIENT_SECRET}).
 * <p>
 * Sem essas variáveis, este bean simplesmente não é criado: a aplicação sobe
 * normalmente com login local apenas, e {@link SecurityConfig} usa a ausência
 * do bean para decidir se habilita {@code .oauth2Login(...)} e se mostra o
 * botão "Continuar com Google" na tela de login (ver {@link OAuth2FeatureFlags}).
 * Evita, de propósito, que a aplicação falhe ao subir por falta de credenciais
 * do Google — que só o próprio time consegue gerar no Google Cloud Console.
 */
@Configuration
public class GoogleOAuth2Config {

    @Bean
    @ConditionalOnProperty(name = {"GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_SECRET"})
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${GOOGLE_CLIENT_ID}") String clientId,
            @Value("${GOOGLE_CLIENT_SECRET}") String clientSecret) {

        ClientRegistration google = ClientRegistration.withRegistrationId("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .clientName("Google")
                .build();

        return new InMemoryClientRegistrationRepository(google);
    }
}
