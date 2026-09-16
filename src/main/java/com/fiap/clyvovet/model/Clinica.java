package com.fiap.clyvovet.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "T_CLINICA")
public class Clinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOME_CNPJ", nullable = false, length = 150)
    private String nomeCnpj;

    @Column(name = "CNPJ", length = 18)
    private String cnpj;

    @Column(name = "RAZAO_SOCIAL", length = 150)
    private String razaoSocial;

    @Column(name = "CRMV_RESPONSAVEL", length = 30)
    private String crmvResponsavel;

    @Column(name = "EMAIL", length = 100)
    private String email;

    @Column(name = "TELEFONE", length = 20)
    private String telefone;

    @Column(name = "CIDADE", nullable = false, length = 100)
    private String cidade;

    @Column(name = "ESTADO", nullable = false, length = 2)
    private String estado;

    @Column(name = "ATENDIMENTO_24H")
    private Boolean atendimento24h = false;

    @Column(name = "CHAVE_PIX_REPASSE", length = 100)
    private String chavePixRepasse;

    @Column(name = "TAXA_COMISSAO_CUSTOMIZADA", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxaComissaoCustomizada = new BigDecimal("15.00");

    @Column(name = "ATIVO", nullable = false)
    private Boolean ativo = true;

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

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }

    public String getCrmvResponsavel() { return crmvResponsavel; }
    public void setCrmvResponsavel(String crmvResponsavel) { this.crmvResponsavel = crmvResponsavel; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Boolean getAtendimento24h() { return atendimento24h; }
    public void setAtendimento24h(Boolean atendimento24h) { this.atendimento24h = atendimento24h; }

    public String getChavePixRepasse() { return chavePixRepasse; }
    public void setChavePixRepasse(String chavePixRepasse) { this.chavePixRepasse = chavePixRepasse; }

    public BigDecimal getTaxaComissaoCustomizada() { return taxaComissaoCustomizada; }
    public void setTaxaComissaoCustomizada(BigDecimal taxaComissaoCustomizada) { this.taxaComissaoCustomizada = taxaComissaoCustomizada; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
}
