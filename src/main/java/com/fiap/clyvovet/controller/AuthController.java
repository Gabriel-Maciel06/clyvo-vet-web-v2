package com.fiap.clyvovet.controller;

import com.fiap.clyvovet.config.OAuth2FeatureFlags;
import com.fiap.clyvovet.dto.CadastroUsuarioDto;
import com.fiap.clyvovet.dto.RecuperarSenhaDto;
import com.fiap.clyvovet.dto.RedefinirSenhaDto;
import com.fiap.clyvovet.service.RecuperacaoSenhaService;
import com.fiap.clyvovet.service.UsuarioService;
import com.fiap.clyvovet.model.Tutor;
import com.fiap.clyvovet.model.Usuario;
import com.fiap.clyvovet.repository.TutorRepository;
import com.fiap.clyvovet.service.CustomOAuth2UserService;
import com.fiap.clyvovet.service.PerfilService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;

@Controller
public class AuthController {

    private final UsuarioService usuarioService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;
    private final OAuth2FeatureFlags oAuth2FeatureFlags;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final TutorRepository tutorRepository;
    private final PerfilService perfilService;

    @Value("${app.mostrar-credenciais-teste:false}")
    private boolean mostrarCredenciaisTeste;

    public AuthController(UsuarioService usuarioService,
                           RecuperacaoSenhaService recuperacaoSenhaService,
                           OAuth2FeatureFlags oAuth2FeatureFlags,
                           CustomOAuth2UserService customOAuth2UserService,
                           TutorRepository tutorRepository,
                           PerfilService perfilService) {
        this.usuarioService = usuarioService;
        this.recuperacaoSenhaService = recuperacaoSenhaService;
        this.oAuth2FeatureFlags = oAuth2FeatureFlags;
        this.customOAuth2UserService = customOAuth2UserService;
        this.tutorRepository = tutorRepository;
        this.perfilService = perfilService;
    }

    @ModelAttribute("googleLoginEnabled")
    public boolean googleLoginEnabled() {
        return oAuth2FeatureFlags.isGoogleHabilitado();
    }

    @ModelAttribute("mostrarCredenciaisTeste")
    public boolean mostrarCredenciaisTeste() {
        return mostrarCredenciaisTeste;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "cadastro", required = false) String cadastro,
                            @RequestParam(value = "senhaRedefinida", required = false) String senhaRedefinida,
                            Model model) {
        if ("google".equals(error)) {
            model.addAttribute("errorMessage", "Não foi possível concluir o login com Google. Tente novamente.");
        } else if (error != null) {
            model.addAttribute("errorMessage", "Usuário ou senha inválidos. Tente novamente.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "Você foi desconectado com sucesso.");
        }
        if (cadastro != null) {
            model.addAttribute("successMessage", "Cadastro realizado com sucesso! Faça login para continuar.");
        }
        if (senhaRedefinida != null) {
            model.addAttribute("successMessage", "Senha redefinida com sucesso! Faça login com a nova senha.");
        }
        return "login";
    }

    @GetMapping("/cadastro")
    public String cadastroForm(Model model) {
        model.addAttribute("cadastroDto", new CadastroUsuarioDto());
        return "cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@Valid @ModelAttribute("cadastroDto") CadastroUsuarioDto dto,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (!dto.getSenha().equals(dto.getConfirmarSenha())) {
            bindingResult.rejectValue("confirmarSenha", "senha.diferente", "As senhas não coincidem");
        }
        if (bindingResult.hasErrors()) {
            return "cadastro";
        }

        try {
            usuarioService.cadastrarTutor(dto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "cadastro";
        }

        return "redirect:/login?cadastro=sucesso";
    }

    @GetMapping("/recuperar-senha")
    public String recuperarSenhaForm(Model model) {
        model.addAttribute("recuperarSenhaDto", new RecuperarSenhaDto());
        return "recuperar-senha";
    }

    @PostMapping("/recuperar-senha")
    public String recuperarSenha(@Valid @ModelAttribute("recuperarSenhaDto") RecuperarSenhaDto dto,
                                  BindingResult bindingResult,
                                  Model model) {
        if (bindingResult.hasErrors()) {
            return "recuperar-senha";
        }
        // Sempre a mesma mensagem, exista ou não o e-mail: evita confirmar quais contas existem.
        recuperacaoSenhaService.solicitar(dto.getEmail());
        model.addAttribute("successMessage",
                "Se este e-mail estiver cadastrado, enviamos um link de redefinição de senha para ele.");
        return "recuperar-senha";
    }

    @GetMapping("/redefinir-senha")
    public String redefinirSenhaForm(@RequestParam("token") String token, Model model) {
        if (!recuperacaoSenhaService.tokenValido(token)) {
            model.addAttribute("tokenInvalido", true);
            return "redefinir-senha";
        }
        RedefinirSenhaDto dto = new RedefinirSenhaDto();
        dto.setToken(token);
        model.addAttribute("redefinirSenhaDto", dto);
        model.addAttribute("tokenInvalido", false);
        return "redefinir-senha";
    }

    @PostMapping("/redefinir-senha")
    public String redefinirSenha(@Valid @ModelAttribute("redefinirSenhaDto") RedefinirSenhaDto dto,
                                  BindingResult bindingResult,
                                  Model model) {
        if (!dto.getNovaSenha().equals(dto.getConfirmarSenha())) {
            bindingResult.rejectValue("confirmarSenha", "senha.diferente", "As senhas não coincidem");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("tokenInvalido", false);
            return "redefinir-senha";
        }

        try {
            recuperacaoSenhaService.redefinir(dto.getToken(), dto.getNovaSenha());
        } catch (IllegalArgumentException e) {
            model.addAttribute("tokenInvalido", true);
            return "redefinir-senha";
        }

        return "redirect:/login?senhaRedefinida=true";
    }

    /**
     * Rota de demonstração do login com Google quando executando em ambiente local
     * sem credenciais GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET configuradas.
     * Provisiona um usuário social (ROLE_TUTOR com CPF provisório) e autentica na sessão.
     */
    @GetMapping("/login/google-demo")
    public String loginGoogleDemo(HttpServletRequest request) {
        String emailDemo = "gabriel.google@clyvovet.com";
        String nomeDemo = "Gabriel Maciel (Google)";
        String googleIdDemo = "google-demo-sub-998877";
        String fotoDemo = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150";

        Usuario usuario = customOAuth2UserService.provisionarOuVincular(emailDemo, nomeDemo, googleIdDemo, fotoDemo);

        UserDetails userDetails = new User(
                usuario.getUsername(),
                "",
                Collections.singletonList(new SimpleGrantedAuthority(usuario.getRole().name()))
        );

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        request.getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );

        Tutor tutor = tutorRepository.findByUsuarioUsername(usuario.getUsername()).orElse(null);
        if (tutor != null && perfilService.precisaCompletarCadastro(tutor)) {
            return "redirect:/perfil/completar-cadastro";
        }
        return "redirect:/dashboard";
    }

    /**
     * Aceita qualquer método HTTP: o Spring Security faz FORWARD para esta rota mantendo o
     * método original (ex.: POST bloqueado por perfil ou CSRF), e um @GetMapping responderia 405.
     */
    @RequestMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
