package com.fiap.clyvovet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Preenchido pelo tutor provisionado via Google na primeira vez que acessa o
 * sistema, para trocar o CPF provisório (gerado no momento do login social)
 * pelo CPF real — necessário porque T_TUTOR usa o CPF como chave primária.
 */
public class CompletarCadastroDto {

    @NotBlank(message = "O CPF é obrigatório")
    @Pattern(regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}", message = "Informe um CPF válido (11 dígitos)")
    private String cpf;

    @NotBlank(message = "O telefone é obrigatório")
    private String telefone;

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
}
