package com.fiap.clyvovet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AvaliacaoTriagemDto {

    @NotNull(message = "O ID da triagem é obrigatório")
    private Long triagemId;

    @NotNull(message = "O peso aferido é obrigatório")
    @DecimalMin(value = "0.1", message = "O peso deve ser maior que zero")
    private BigDecimal pesoAferido;

    @NotNull(message = "A temperatura é obrigatória")
    private BigDecimal temperatura;

    @NotNull(message = "A frequência cardíaca é obrigatória")
    private Integer frequenciaCardiaca;

    @NotBlank(message = "O parecer veterinário é obrigatório")
    private String parecerVeterinario;

    public AvaliacaoTriagemDto() {}

    public Long getTriagemId() { return triagemId; }
    public void setTriagemId(Long triagemId) { this.triagemId = triagemId; }
    public BigDecimal getPesoAferido() { return pesoAferido; }
    public void setPesoAferido(BigDecimal pesoAferido) { this.pesoAferido = pesoAferido; }
    public BigDecimal getTemperatura() { return temperatura; }
    public void setTemperatura(BigDecimal temperatura) { this.temperatura = temperatura; }
    public Integer getFrequenciaCardiaca() { return frequenciaCardiaca; }
    public void setFrequenciaCardiaca(Integer frequenciaCardiaca) { this.frequenciaCardiaca = frequenciaCardiaca; }
    public String getParecerVeterinario() { return parecerVeterinario; }
    public void setParecerVeterinario(String parecerVeterinario) { this.parecerVeterinario = parecerVeterinario; }
}
