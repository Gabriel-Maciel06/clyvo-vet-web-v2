package com.fiap.clyvovet.controller;

import com.fiap.clyvovet.dto.CompletarCadastroDto;
import com.fiap.clyvovet.model.Tutor;
import com.fiap.clyvovet.repository.TutorRepository;
import com.fiap.clyvovet.service.PerfilService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Conclusão de cadastro para tutores provisionados via login social (ver
 * {@link com.fiap.clyvovet.service.CustomOAuth2UserService}), que entram com
 * um CPF provisório até informarem o CPF real.
 */
@Controller
public class PerfilController {

    private final PerfilService perfilService;
    private final TutorRepository tutorRepository;

    public PerfilController(PerfilService perfilService, TutorRepository tutorRepository) {
        this.perfilService = perfilService;
        this.tutorRepository = tutorRepository;
    }

    @GetMapping("/perfil/completar-cadastro")
    public String formulario(Authentication auth, Model model) {
        Tutor tutor = tutorRepository.findByUsuarioUsername(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Tutor não encontrado"));

        if (!perfilService.precisaCompletarCadastro(tutor)) {
            return "redirect:/dashboard";
        }

        model.addAttribute("completarCadastroDto", new CompletarCadastroDto());
        return "completar-cadastro";
    }

    @PostMapping("/perfil/completar-cadastro")
    public String salvar(@Valid @ModelAttribute("completarCadastroDto") CompletarCadastroDto dto,
                          BindingResult bindingResult,
                          Authentication auth,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "completar-cadastro";
        }

        try {
            perfilService.completarCadastro(auth.getName(), dto);
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "completar-cadastro";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Cadastro completo! Agora você já pode cadastrar seus pets.");
        return "redirect:/dashboard";
    }
}
