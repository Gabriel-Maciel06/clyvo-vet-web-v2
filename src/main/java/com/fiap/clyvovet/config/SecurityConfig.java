package com.fiap.clyvovet.config;

import com.fiap.clyvovet.service.CustomOAuth2UserService;
import com.fiap.clyvovet.service.CustomOidcUserService;
import com.fiap.clyvovet.service.CustomUserDetailsService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

    /**
     * Só existe em ambientes de desenvolvimento (perfil "prod" o define como
     * false em application-prod.properties). Controla, ao mesmo tempo, a
     * liberação do console H2 e a exceção de CSRF que ele exige — em produção
     * nenhum dos dois fica ativo.
     */
    @Value("${app.h2-console.permitir:true}")
    private boolean h2ConsolePermitido;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                           CustomOAuth2UserService customOAuth2UserService,
                           CustomOidcUserService customOidcUserService,
                           ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider) {
        this.userDetailsService = userDetailsService;
        this.customOAuth2UserService = customOAuth2UserService;
        this.customOidcUserService = customOidcUserService;
        this.clientRegistrationRepositoryProvider = clientRegistrationRepositoryProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean googleLoginHabilitado = clientRegistrationRepositoryProvider.getIfAvailable() != null;

        http
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> {
                // Arquivos estáticos
                auth.requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll();
                if (h2ConsolePermitido) {
                    auth.requestMatchers("/h2-console/**").permitAll();
                }
                // Rotas públicas de autenticação e autocadastro
                auth.requestMatchers("/login", "/erro", "/access-denied",
                        "/cadastro", "/recuperar-senha", "/redefinir-senha", "/login/google-demo").permitAll();
                // Conclusão de cadastro (CPF real) para tutores criados via login social
                auth.requestMatchers("/perfil/**").hasRole("TUTOR");
                // Rotas exclusivas do Veterinário (ROLE_ADMIN): fila e avaliação clínica
                auth.requestMatchers("/triagem/fila", "/triagem/avaliar/**").hasRole("ADMIN");
                // Rotas exclusivas do Tutor (ROLE_TUTOR): cadastro de pets, check-in, recompensas e solicitação de triagem
                auth.requestMatchers("/pets/novo", "/pets/salvar", "/pets/protocolo", "/checkin/**", "/triagem/solicitar").hasRole("TUTOR");
                // Qualquer outra rota autenticada
                auth.anyRequest().authenticated();
            })
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            );

        if (googleLoginHabilitado) {
            http.oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)   // provedores OAuth2 puro (sem OpenID Connect)
                    .oidcUserService(customOidcUserService)) // Google usa OIDC (escopo "openid") -> este é o que roda de fato
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=google")
            );
        }

        // O console H2 só existe (e só precisa dessa exceção de CSRF/frame) fora de produção.
        if (h2ConsolePermitido) {
            http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        }

        return http.build();
    }
}
