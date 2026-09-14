package com.fiap.clyvovet.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "T_BADGE_CONQUISTA")
public class BadgeConquista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PET_ID", nullable = false)
    private Pet pet;

    @Column(name = "CODIGO_BADGE", nullable = false, length = 50)
    private String codigoBadge;

    @Column(name = "NOME_BADGE", nullable = false, length = 100)
    private String nomeBadge;

    @Column(name = "ICONE", nullable = false, length = 50)
    private String icone;

    @Column(name = "DESCRICAO", nullable = false, length = 255)
    private String descricao;

    @Column(name = "DATA_CONQUISTA", nullable = false)
    private LocalDate dataConquista;

    public BadgeConquista() {}

    public BadgeConquista(Long id, Pet pet, String codigoBadge, String nomeBadge, String icone, String descricao, LocalDate dataConquista) {
        this.id = id;
        this.pet = pet;
        this.codigoBadge = codigoBadge;
        this.nomeBadge = nomeBadge;
        this.icone = icone;
        this.descricao = descricao;
        this.dataConquista = dataConquista;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Pet getPet() { return pet; }
    public void setPet(Pet pet) { this.pet = pet; }
    public String getCodigoBadge() { return codigoBadge; }
    public void setCodigoBadge(String codigoBadge) { this.codigoBadge = codigoBadge; }
    public String getNomeBadge() { return nomeBadge; }
    public void setNomeBadge(String nomeBadge) { this.nomeBadge = nomeBadge; }
    public String getIcone() { return icone; }
    public void setIcone(String icone) { this.icone = icone; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public LocalDate getDataConquista() { return dataConquista; }
    public void setDataConquista(LocalDate dataConquista) { this.dataConquista = dataConquista; }
}
