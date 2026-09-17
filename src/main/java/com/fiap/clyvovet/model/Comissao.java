package com.fiap.clyvovet.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "T_COMISSAO")
public class Comissao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACAO_ID", nullable = false, unique = true)
    private Transacao transacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLINICA_ID", nullable = false)
    private Clinica clinica;

    @Column(name = "PERCENTUAL_TAKE_RATE", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualTakeRate = new BigDecimal("15.00");

    @Column(name = "VALOR_SUBSIDIO_PLATAFORMA", precision = 10, scale = 2)
    private BigDecimal valorSubsidioPlataforma = BigDecimal.ZERO;

    @Column(name = "TAXA_EFETIVA_PERCENTUAL", precision = 5, scale = 2)
    private BigDecimal taxaEfetivaPercentual = new BigDecimal("15.00");

    @Column(name = "VALOR_COMISSAO_PLATAFORMA", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorComissaoPlataforma;

    @Column(name = "VALOR_REPASSE_CLINICA", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorRepasseClinica;

    @Column(name = "PISO_PROTEGIDO_APLICADO")
    private Boolean pisoProtegidoAplicado = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS_REPASSE", nullable = false, length = 35)
    private StatusRepasseComissao statusRepasse = StatusRepasseComissao.RETIDO_ESCROW;

    @Column(name = "DATA_PREVISAO_REPASSE", nullable = false)
    private LocalDate dataPrevisaoRepasse;

    @Column(name = "DATA_LIQUIDACAO_REPASSE")
    private LocalDateTime dataLiquidacaoRepasse;

    // --- Snapshots imutaveis (V13) da taxa e do piso vigentes no ato do split. ---
    @Column(name = "SNAPSHOT_TAXA_COMISSAO_VIGENTE", precision = 5, scale = 2)
    private BigDecimal snapshotTaxaComissaoVigente;

    @Column(name = "SNAPSHOT_PISO_REPASSE_PCT", precision = 5, scale = 2)
    private BigDecimal snapshotPisoRepassePct;

    /** Valor que a plataforma pagou do proprio caixa para honrar o piso da clinica. */
    @Column(name = "VALOR_PREJUIZO_PLATAFORMA", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorPrejuizoPlataforma = BigDecimal.ZERO;

    public Comissao() {}

    public Comissao(Long id, Transacao transacao, Clinica clinica, BigDecimal percentualTakeRate, BigDecimal valorComissaoPlataforma, BigDecimal valorRepasseClinica, StatusRepasseComissao statusRepasse, LocalDate dataPrevisaoRepasse, LocalDateTime dataLiquidacaoRepasse) {
        this.id = id;
        this.transacao = transacao;
        this.clinica = clinica;
        this.percentualTakeRate = percentualTakeRate;
        this.valorComissaoPlataforma = valorComissaoPlataforma;
        this.valorRepasseClinica = valorRepasseClinica;
        this.statusRepasse = statusRepasse;
        this.dataPrevisaoRepasse = dataPrevisaoRepasse;
        this.dataLiquidacaoRepasse = dataLiquidacaoRepasse;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Transacao getTransacao() { return transacao; }
    public void setTransacao(Transacao transacao) { this.transacao = transacao; }

    public Clinica getClinica() { return clinica; }
    public void setClinica(Clinica clinica) { this.clinica = clinica; }

    public BigDecimal getPercentualTakeRate() { return percentualTakeRate; }
    public void setPercentualTakeRate(BigDecimal percentualTakeRate) { this.percentualTakeRate = percentualTakeRate; }

    public BigDecimal getValorSubsidioPlataforma() { return valorSubsidioPlataforma; }
    public void setValorSubsidioPlataforma(BigDecimal valorSubsidioPlataforma) { this.valorSubsidioPlataforma = valorSubsidioPlataforma; }

    public BigDecimal getTaxaEfetivaPercentual() { return taxaEfetivaPercentual; }
    public void setTaxaEfetivaPercentual(BigDecimal taxaEfetivaPercentual) { this.taxaEfetivaPercentual = taxaEfetivaPercentual; }

    public BigDecimal getValorComissaoPlataforma() { return valorComissaoPlataforma; }
    public void setValorComissaoPlataforma(BigDecimal valorComissaoPlataforma) { this.valorComissaoPlataforma = valorComissaoPlataforma; }

    public BigDecimal getValorRepasseClinica() { return valorRepasseClinica; }
    public void setValorRepasseClinica(BigDecimal valorRepasseClinica) { this.valorRepasseClinica = valorRepasseClinica; }

    public Boolean getPisoProtegidoAplicado() { return pisoProtegidoAplicado; }
    public void setPisoProtegidoAplicado(Boolean pisoProtegidoAplicado) { this.pisoProtegidoAplicado = pisoProtegidoAplicado; }

    public StatusRepasseComissao getStatusRepasse() { return statusRepasse; }
    public void setStatusRepasse(StatusRepasseComissao statusRepasse) { this.statusRepasse = statusRepasse; }

    public LocalDate getDataPrevisaoRepasse() { return dataPrevisaoRepasse; }
    public void setDataPrevisaoRepasse(LocalDate dataPrevisaoRepasse) { this.dataPrevisaoRepasse = dataPrevisaoRepasse; }

    public LocalDateTime getDataLiquidacaoRepasse() { return dataLiquidacaoRepasse; }
    public void setDataLiquidacaoRepasse(LocalDateTime dataLiquidacaoRepasse) { this.dataLiquidacaoRepasse = dataLiquidacaoRepasse; }

    public BigDecimal getSnapshotTaxaComissaoVigente() { return snapshotTaxaComissaoVigente; }
    public void setSnapshotTaxaComissaoVigente(BigDecimal snapshotTaxaComissaoVigente) { this.snapshotTaxaComissaoVigente = snapshotTaxaComissaoVigente; }

    public BigDecimal getSnapshotPisoRepassePct() { return snapshotPisoRepassePct; }
    public void setSnapshotPisoRepassePct(BigDecimal snapshotPisoRepassePct) { this.snapshotPisoRepassePct = snapshotPisoRepassePct; }

    public BigDecimal getValorPrejuizoPlataforma() { return valorPrejuizoPlataforma; }
    public void setValorPrejuizoPlataforma(BigDecimal valorPrejuizoPlataforma) { this.valorPrejuizoPlataforma = valorPrejuizoPlataforma; }
}
