package com.fiap.clyvovet.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;

/**
 * Tratamento global de erros das telas Thymeleaf: substitui a "Whitelabel Error Page"
 * por páginas amigáveis e com o status HTTP correto.
 *  - IllegalArgumentException (recurso não encontrado / dado inválido) -> 404
 *  - AccessDeniedException (pet de outro tutor, etc.) -> é repassada ao Spring Security (403 / access-denied)
 *  - Qualquer outra exceção -> 500 com página de erro genérica
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String recursoNaoEncontrado(IllegalArgumentException ex, Model model) {
        model.addAttribute("titulo", "Registro não encontrado");
        model.addAttribute("mensagem", ex.getMessage());
        model.addAttribute("status", 404);
        return "erro";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public void acessoNegado(AccessDeniedException ex) {
        // Relança para o ExceptionTranslationFilter do Spring Security renderizar /access-denied (403)
        throw ex;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public String metodoNaoSuportado(HttpRequestMethodNotSupportedException ex, Model model) {
        model.addAttribute("titulo", "Método não permitido");
        model.addAttribute("mensagem", ex.getMessage());
        model.addAttribute("status", 405);
        return "erro";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String erroInesperado(Exception ex, Model model) {
        log.error("Erro inesperado ao processar a requisição", ex);
        model.addAttribute("titulo", "Ocorreu um erro inesperado");
        model.addAttribute("mensagem", ex.getMessage());
        model.addAttribute("status", 500);
        return "erro";
    }
}
