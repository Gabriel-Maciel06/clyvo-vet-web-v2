package com.fiap.clyvovet.dto;

import com.fiap.clyvovet.model.AlimentacaoStatus;
import com.fiap.clyvovet.model.HumorPet;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CheckinDto {

    @NotNull(message = "O Pet deve ser informado")
    private Long petId;

    @NotNull(message = "O status de alimentação é obrigatório")
    private AlimentacaoStatus alimentacaoStatus;

    private Boolean remedioAdministrado = false;

    private Integer minutosAtividade = 0;

    @NotNull(message = "O humor do pet é obrigatório")
    private HumorPet humorPet;

    @Size(max = 500, message = "Os sintomas devem ter no máximo 500 caracteres")
    private String sintomasObservados;

    public CheckinDto() {}

    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }
    public AlimentacaoStatus getAlimentacaoStatus() { return alimentacaoStatus; }
    public void setAlimentacaoStatus(AlimentacaoStatus alimentacaoStatus) { this.alimentacaoStatus = alimentacaoStatus; }
    public Boolean getRemedioAdministrado() { return remedioAdministrado; }
    public void setRemedioAdministrado(Boolean remedioAdministrado) { this.remedioAdministrado = remedioAdministrado; }
    public Integer getMinutosAtividade() { return minutosAtividade; }
    public void setMinutosAtividade(Integer minutosAtividade) { this.minutosAtividade = minutosAtividade; }
    public HumorPet getHumorPet() { return humorPet; }
    public void setHumorPet(HumorPet humorPet) { this.humorPet = humorPet; }
    public String getSintomasObservados() { return sintomasObservados; }
    public void setSintomasObservados(String sintomasObservados) { this.sintomasObservados = sintomasObservados; }
}
