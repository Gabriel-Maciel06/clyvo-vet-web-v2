package com.fiap.clyvovet.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "T_CHECKIN_DIARIO")
public class CheckinDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PET_ID", nullable = false)
    private Pet pet;

    @Column(name = "DATA_CHECKIN", nullable = false)
    private LocalDate dataCheckin;

    @Enumerated(EnumType.STRING)
    @Column(name = "ALIMENTACAO_STATUS", nullable = false, length = 30)
    private AlimentacaoStatus alimentacaoStatus;

    @Column(name = "REMEDIO_ADMINISTRADO")
    private Boolean remedioAdministrado = false;

    @Column(name = "MINUTOS_ATIVIDADE")
    private Integer minutosAtividade = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "HUMOR_PET", nullable = false, length = 30)
    private HumorPet humorPet;

    @Column(name = "SINTOMAS_OBSERVADOS", length = 500)
    private String sintomasObservados;

    @Column(name = "PONTOS_GANHOS")
    private Integer pontosGanhos = 10;

    @Column(name = "ALERTA_GERADO")
    private Boolean alertaGerado = false;

    public CheckinDiario() {}

    public CheckinDiario(Long id, Pet pet, LocalDate dataCheckin, AlimentacaoStatus alimentacaoStatus, Boolean remedioAdministrado, Integer minutosAtividade, HumorPet humorPet, String sintomasObservados, Integer pontosGanhos, Boolean alertaGerado) {
        this.id = id;
        this.pet = pet;
        this.dataCheckin = dataCheckin;
        this.alimentacaoStatus = alimentacaoStatus;
        this.remedioAdministrado = remedioAdministrado;
        this.minutosAtividade = minutosAtividade;
        this.humorPet = humorPet;
        this.sintomasObservados = sintomasObservados;
        this.pontosGanhos = pontosGanhos;
        this.alertaGerado = alertaGerado;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Pet getPet() { return pet; }
    public void setPet(Pet pet) { this.pet = pet; }
    public LocalDate getDataCheckin() { return dataCheckin; }
    public void setDataCheckin(LocalDate dataCheckin) { this.dataCheckin = dataCheckin; }
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
    public Integer getPontosGanhos() { return pontosGanhos; }
    public void setPontosGanhos(Integer pontosGanhos) { this.pontosGanhos = pontosGanhos; }
    public Boolean getAlertaGerado() { return alertaGerado; }
    public void setAlertaGerado(Boolean alertaGerado) { this.alertaGerado = alertaGerado; }
}
