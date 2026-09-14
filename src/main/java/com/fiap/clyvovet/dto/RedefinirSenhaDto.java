package com.fiap.clyvovet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RedefinirSenhaDto {

    @NotBlank
    private String token;

    @NotBlank(message = "A nova senha é obrigatória")
    @Size(min = 6, max = 100, message = "A senha deve ter pelo menos 6 caracteres")
    private String novaSenha;

    @NotBlank(message = "Confirme a nova senha")
    private String confirmarSenha;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getNovaSenha() { return novaSenha; }
    public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }
    public String getConfirmarSenha() { return confirmarSenha; }
    public void setConfirmarSenha(String confirmarSenha) { this.confirmarSenha = confirmarSenha; }
}
