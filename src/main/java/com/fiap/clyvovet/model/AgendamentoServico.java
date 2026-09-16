package com.fiap.clyvovet.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "T_AGENDAMENTO_SERVICO")
public class AgendamentoServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PET_ID", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TUTOR_CPF", nullable = false)
    private Tutor tutor;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_SERVICO", nullable = false, length = 50)
    private TipoServicoPreventivo tipoServico;

    @Column(name = "DESCRICAO_SERVICO", nullable = false, length = 255)
    private String descricaoServico;

    @Column(name = "VALOR_ORIGINAL", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorOriginal;

    @Column(name = "DESCONTO_FIDELIDADE", nullable = false, precision = 10, scale = 2)
    private BigDecimal descontoFidelidade = BigDecimal.ZERO;

    @Column(name = "VALOR_FINAL", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorFinal;

    @Column(name = "TAXA_CLYVO_PERCENTUAL", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxaClyvoPercentual = new BigDecimal("15.00");

    @Column(name = "VALOR_COMISSAO_CLYVO", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorComissaoClyvo;

    @Column(name = "VALOR_SUBSIDIO_CLYVO", precision = 10, scale = 2)
    private BigDecimal valorSubsidioClyvo = BigDecimal.ZERO;

    @Column(name = "VALOR_DESCONTO_CLINICA", precision = 10, scale = 2)
    private BigDecimal valorDescontoClinica = BigDecimal.ZERO;

    @Column(name = "TAXA_EFETIVA_PERCENTUAL", precision = 5, scale = 2)
    private BigDecimal taxaEfetivaPercentual = new BigDecimal("15.00");

    @Column(name = "VALOR_REPASSE_CLINICA", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorRepasseClinica;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS_PAGAMENTO", nullable = false, length = 30)
    private StatusPagamento statusPagamento;

    @Column(name = "METODO_PAGAMENTO", nullable = false, length = 30)
    private String metodoPagamento;

    @Column(name = "CODIGO_VOUCHER", nullable = false, unique = true, length = 50)
    private String codigoVoucher;

    @Column(name = "QR_CODE_HASH", nullable = false, length = 255)
    private String qrCodeHash;

    @Column(name = "CLINICA_PARCEIRA", nullable = false, length = 150)
    private String clinicaParceira = "Hospital Veterinário Central Parceiro Clyvo";

    @Column(name = "DATA_CRIACAO", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "DATA_PAGAMENTO")
    private LocalDateTime dataPagamento;

    @Column(name = "DATA_UTILIZACAO")
    private LocalDateTime dataUtilizacao;

    @Column(name = "OBSERVACOES", length = 500)
    private String observacoes;

    public AgendamentoServico() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Pet getPet() {
        return pet;
    }

    public void setPet(Pet pet) {
        this.pet = pet;
    }

    public Tutor getTutor() {
        return tutor;
    }

    public void setTutor(Tutor tutor) {
        this.tutor = tutor;
    }

    public TipoServicoPreventivo getTipoServico() {
        return tipoServico;
    }

    public void setTipoServico(TipoServicoPreventivo tipoServico) {
        this.tipoServico = tipoServico;
    }

    public String getDescricaoServico() {
        return descricaoServico;
    }

    public void setDescricaoServico(String descricaoServico) {
        this.descricaoServico = descricaoServico;
    }

    public BigDecimal getValorOriginal() {
        return valorOriginal;
    }

    public void setValorOriginal(BigDecimal valorOriginal) {
        this.valorOriginal = valorOriginal;
    }

    public BigDecimal getDescontoFidelidade() {
        return descontoFidelidade;
    }

    public void setDescontoFidelidade(BigDecimal descontoFidelidade) {
        this.descontoFidelidade = descontoFidelidade;
    }

    public BigDecimal getValorFinal() {
        return valorFinal;
    }

    public void setValorFinal(BigDecimal valorFinal) {
        this.valorFinal = valorFinal;
    }

    public BigDecimal getTaxaClyvoPercentual() {
        return taxaClyvoPercentual;
    }

    public void setTaxaClyvoPercentual(BigDecimal taxaClyvoPercentual) {
        this.taxaClyvoPercentual = taxaClyvoPercentual;
    }

    public BigDecimal getValorComissaoClyvo() {
        return valorComissaoClyvo;
    }

    public void setValorComissaoClyvo(BigDecimal valorComissaoClyvo) {
        this.valorComissaoClyvo = valorComissaoClyvo;
    }

    public BigDecimal getValorRepasseClinica() {
        return valorRepasseClinica;
    }

    public void setValorRepasseClinica(BigDecimal valorRepasseClinica) {
        this.valorRepasseClinica = valorRepasseClinica;
    }

    public StatusPagamento getStatusPagamento() {
        return statusPagamento;
    }

    public void setStatusPagamento(StatusPagamento statusPagamento) {
        this.statusPagamento = statusPagamento;
    }

    public String getMetodoPagamento() {
        return metodoPagamento;
    }

    public void setMetodoPagamento(String metodoPagamento) {
        this.metodoPagamento = metodoPagamento;
    }

    public String getCodigoVoucher() {
        return codigoVoucher;
    }

    public void setCodigoVoucher(String codigoVoucher) {
        this.codigoVoucher = codigoVoucher;
    }

    public String getQrCodeHash() {
        return qrCodeHash;
    }

    public void setQrCodeHash(String qrCodeHash) {
        this.qrCodeHash = qrCodeHash;
    }

    public String getClinicaParceira() {
        return clinicaParceira;
    }

    public void setClinicaParceira(String clinicaParceira) {
        this.clinicaParceira = clinicaParceira;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(LocalDateTime dataPagamento) {
        this.dataPagamento = dataPagamento;
    }

    public LocalDateTime getDataUtilizacao() {
        return dataUtilizacao;
    }

    public void setDataUtilizacao(LocalDateTime dataUtilizacao) {
        this.dataUtilizacao = dataUtilizacao;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public BigDecimal getValorSubsidioClyvo() {
        return valorSubsidioClyvo;
    }

    public void setValorSubsidioClyvo(BigDecimal valorSubsidioClyvo) {
        this.valorSubsidioClyvo = valorSubsidioClyvo;
    }

    public BigDecimal getValorDescontoClinica() {
        return valorDescontoClinica;
    }

    public void setValorDescontoClinica(BigDecimal valorDescontoClinica) {
        this.valorDescontoClinica = valorDescontoClinica;
    }

    public BigDecimal getTaxaEfetivaPercentual() {
        return taxaEfetivaPercentual;
    }

    public void setTaxaEfetivaPercentual(BigDecimal taxaEfetivaPercentual) {
        this.taxaEfetivaPercentual = taxaEfetivaPercentual;
    }
}
