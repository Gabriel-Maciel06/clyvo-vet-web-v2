package com.fiap.clyvovet.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CustomUserDetailsServiceTest {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    @DisplayName("Conta local (seed 'tutor') carrega normalmente")
    void carregaContaLocal() {
        assertNotNull(customUserDetailsService.loadUserByUsername("tutor").getPassword());
    }

    @Test
    @DisplayName("Conta provisionada via Google (sem senha) não quebra o login local: é tratada como 'não encontrada'")
    void contaGoogleNaoQuebraLoginLocal() {
        customOAuth2UserService.provisionarOuVincular(
                "google.puro@gmail.com", "Google Puro", "sub-puro", null);

        // Antes da correção, isso lançava NullPointerException/IllegalArgumentException
        // (o construtor de User exige senha não nula) em vez de uma falha de autenticação tratável.
        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("google.puro@gmail.com"));
    }
}
