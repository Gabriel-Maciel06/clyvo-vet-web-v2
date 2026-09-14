package com.fiap.clyvovet.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

/**
 * Expõe se o login com Google está disponível neste ambiente (credenciais
 * configuradas), para a tela de login decidir se mostra o botão.
 */
@Component
public class OAuth2FeatureFlags {

    private final boolean googleHabilitado;

    public OAuth2FeatureFlags(ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider) {
        this.googleHabilitado = clientRegistrationRepositoryProvider.getIfAvailable() != null;
    }

    public boolean isGoogleHabilitado() {
        return googleHabilitado;
    }
}
