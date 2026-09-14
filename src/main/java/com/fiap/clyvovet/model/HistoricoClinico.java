package com.fiap.clyvovet.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "T_HISTORICO_CLINICO")
public class HistoricoClinico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PET_ID", nullable = false)
    private Pet pet;

    @Column(name = "DATA_REGISTRO", nullable = false)
    private LocalDateTime dataRegistro = LocalDateTime.now();

    @Column(name = "TIPO_EVENTO", nullable = false, length = 50)
    private String tipoEvento; // CHECKIN_DIARIO, TRIAGEM_PREVENTIVA, VACINACAO, ALERTA_SAUDE

    @Column(name = "DESCRICAO", nullable = false, length = 500)
    private String descricao;

    @Column(name = "CONDUTA_ADOTADA", length = 1000)
    private String condutaAdotada;

    public HistoricoClinico() {}

    public HistoricoClinico(Long id, Pet pet, LocalDateTime dataRegistro, String tipoEvento, String descricao, String condutaAdotada) {
        this.id = id;
        this.pet = pet;
        this.dataRegistro = dataRegistro;
        this.tipoEvento = tipoEvento;
        this.descricao = descricao;
        this.condutaAdotada = condutaAdotada;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Pet getPet() { return pet; }
    public void setPet(Pet pet) { this.pet = pet; }
    public LocalDateTime getDataRegistro() { return dataRegistro; }
    public void setDataRegistro(LocalDateTime dataRegistro) { this.dataRegistro = dataRegistro; }
    public String getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getCondutaAdotada() { return condutaAdotada; }
    public void setCondutaAdotada(String condutaAdotada) { this.condutaAdotada = condutaAdotada; }
}
