package com.fiap.clyvovet.model;

import jakarta.persistence.*;

@Entity
@Table(name = "T_CLINICA")
public class Clinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOME_CNPJ", nullable = false, length = 150)
    private String nomeCnpj;

    @Column(name = "TELEFONE", length = 20)
    private String telefone;

    @Column(name = "CIDADE", nullable = false, length = 100)
    private String cidade;

    @Column(name = "ESTADO", nullable = false, length = 2)
    private String estado;

    @Column(name = "ATENDIMENTO_24H")
    private Boolean atendimento24h;

    public Clinica() {}

    public Clinica(Long id, String nomeCnpj, String telefone, String cidade, String estado, Boolean atendimento24h) {
        this.id = id;
        this.nomeCnpj = nomeCnpj;
        this.telefone = telefone;
        this.cidade = cidade;
        this.estado = estado;
        this.atendimento24h = atendimento24h;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNomeCnpj() { return nomeCnpj; }
    public void setNomeCnpj(String nomeCnpj) { this.nomeCnpj = nomeCnpj; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Boolean getAtendimento24h() { return atendimento24h; }
    public void setAtendimento24h(Boolean atendimento24h) { this.atendimento24h = atendimento24h; }
}
