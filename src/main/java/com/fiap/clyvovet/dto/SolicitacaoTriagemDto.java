package com.fiap.clyvovet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SolicitacaoTriagemDto {

    @NotNull(message = "O Pet deve ser informado")
    private Long petId;

    @NotBlank(message = "A queixa principal ou motivo da avaliação é obrigatório")
    @Size(min = 5, max = 500, message = "A queixa deve ter entre 5 e 500 caracteres")
    private String queixaPrincipal;

    public SolicitacaoTriagemDto() {}

    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }
    public String getQueixaPrincipal() { return queixaPrincipal; }
    public void setQueixaPrincipal(String queixaPrincipal) { this.queixaPrincipal = queixaPrincipal; }
}
