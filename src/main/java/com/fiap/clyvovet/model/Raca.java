package com.fiap.clyvovet.model;

import jakarta.persistence.*;

@Entity
@Table(name = "T_RACA")
public class Raca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOME", nullable = false, unique = true, length = 100)
    private String nome;

    @Column(name = "ESPECIE", nullable = false, length = 20)
    private String especie;

    @Column(name = "PROPENSAO_DOENCA", length = 255)
    private String propensaoDoenca;

    @Column(name = "EXPECTATIVA_VIDA", nullable = false)
    private Integer expectativaVida;

    @Column(name = "CUIDADOS_ESPECIAIS", length = 500)
    private String cuidadosEspeciais;

    public Raca() {}

    public Raca(Long id, String nome, String especie, String propensaoDoenca, Integer expectativaVida, String cuidadosEspeciais) {
        this.id = id;
        this.nome = nome;
        this.especie = especie;
        this.propensaoDoenca = propensaoDoenca;
        this.expectativaVida = expectativaVida;
        this.cuidadosEspeciais = cuidadosEspeciais;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }
    public String getPropensaoDoenca() { return propensaoDoenca; }
    public void setPropensaoDoenca(String propensaoDoenca) { this.propensaoDoenca = propensaoDoenca; }
    public Integer getExpectativaVida() { return expectativaVida; }
    public void setExpectativaVida(Integer expectativaVida) { this.expectativaVida = expectativaVida; }
    public String getCuidadosEspeciais() { return cuidadosEspeciais; }
    public void setCuidadosEspeciais(String cuidadosEspeciais) { this.cuidadosEspeciais = cuidadosEspeciais; }
}
