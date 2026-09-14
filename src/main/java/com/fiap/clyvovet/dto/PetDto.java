package com.fiap.clyvovet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PetDto {

    private Long id;

    @NotBlank(message = "O nome do pet é obrigatório")
    private String nome;

    @NotNull(message = "A raça deve ser informada")
    private Long racaId;

    @NotNull(message = "A data de nascimento é obrigatória")
    private LocalDate dataNascimento;

    @NotNull(message = "O peso é obrigatório")
    @DecimalMin(value = "0.1", message = "O peso deve ser maior que zero")
    private BigDecimal peso;

    public PetDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Long getRacaId() { return racaId; }
    public void setRacaId(Long racaId) { this.racaId = racaId; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public BigDecimal getPeso() { return peso; }
    public void setPeso(BigDecimal peso) { this.peso = peso; }
}
