package com.fiap.clyvovet.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Usuário ou senha inválidos. Tente novamente.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "Você foi desconectado com sucesso.");
        }
        return "login";
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
