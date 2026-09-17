package com.fiap.clyvovet.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "T_SERVICO")
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLINICA_ID", nullable = false)
    private Clinica clinica;

    @Column(name = "NOME", nullable = false, length = 150)
    private String nome;

    @Column(name = "DESCRICAO", nullable = false, length = 500)
    private String descricao;

    @Column(name = "CATEGORIA", nullable = false, length = 50)
    private String categoria;

    @Column(name = "PRECO_BASE", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoBase;

    @Column(name = "DURACAO_MINUTOS", nullable = false)
    private Integer duracaoMinutos = 45;

    @Column(name = "PERMITE_DESCONTO_FIDELIDADE", nullable = false)
    private Boolean permiteDescontoFidelidade = true;

    @Column(name = "ATIVO", nullable = false)
    private Boolean ativo = true;

    /** Chave que liga esta linha de catalogo ao enum TipoServicoPreventivo exibido no app. */
    @Column(name = "CODIGO_SERVICO_APP", length = 50)
    private String codigoServicoApp;

    public Servico() {}

    public Servico(Long id, Clinica clinica, String nome, String descricao, String categoria, BigDecimal precoBase, Integer duracaoMinutos, Boolean permiteDescontoFidelidade, Boolean ativo) {
        this.id = id;
        this.clinica = clinica;
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.precoBase = precoBase;
        this.duracaoMinutos = duracaoMinutos;
        this.permiteDescontoFidelidade = permiteDescontoFidelidade;
        this.ativo = ativo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Clinica getClinica() { return clinica; }
    public void setClinica(Clinica clinica) { this.clinica = clinica; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public BigDecimal getPrecoBase() { return precoBase; }
    public void setPrecoBase(BigDecimal precoBase) { this.precoBase = precoBase; }

    public Integer getDuracaoMinutos() { return duracaoMinutos; }
    public void setDuracaoMinutos(Integer duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }

    public Boolean getPermiteDescontoFidelidade() { return permiteDescontoFidelidade; }
    public void setPermiteDescontoFidelidade(Boolean permiteDescontoFidelidade) { this.permiteDescontoFidelidade = permiteDescontoFidelidade; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public String getCodigoServicoApp() { return codigoServicoApp; }
    public void setCodigoServicoApp(String codigoServicoApp) { this.codigoServicoApp = codigoServicoApp; }
}
