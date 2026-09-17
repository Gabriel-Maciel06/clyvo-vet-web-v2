package com.fiap.clyvovet.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "T_TRANSACAO")
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AGENDAMENTO_ID", nullable = false, unique = true)
    private Agendamento agendamento;

    @Column(name = "CODIGO_TRANSACAO_GATEWAY", nullable = false, unique = true, length = 100)
    private String codigoTransacaoGateway;

    @Column(name = "METODO_PAGAMENTO", nullable = false, length = 30)
    private String metodoPagamento = "PIX";

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS_TRANSACAO", nullable = false, length = 30)
    private StatusTransacao statusTransacao = StatusTransacao.PENDENTE;

    @Column(name = "VALOR_BRUTO", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorBruto;

    @Column(name = "VALOR_DESCONTO_FIDELIDADE", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorDescontoFidelidade = BigDecimal.ZERO;

    @Column(name = "VALOR_LIQUIDO_PAGO", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorLiquidoPago;

    @Column(name = "CODIGO_VOUCHER", nullable = false, unique = true, length = 50)
    private String codigoVoucher;

    @Column(name = "QR_CODE_HASH", nullable = false, length = 255)
    private String qrCodeHash;

    @Column(name = "VOUCHER_UTILIZADO", nullable = false)
    private Boolean voucherUtilizado = false;

    @Column(name = "DATA_CRIACAO", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Column(name = "DATA_PAGAMENTO")
    private LocalDateTime dataPagamento;

    @Column(name = "DATA_UTILIZACAO_VOUCHER")
    private LocalDateTime dataUtilizacaoVoucher;

    // --- Snapshots imutaveis (V13): valores VIGENTES no ato da captura. ---
    // Gravados uma unica vez no checkout e nunca recalculados, para que alteracoes
    // posteriores em T_SERVICO.preco_base nao reescrevam o historico ja liquidado.

    @Column(name = "SNAPSHOT_PRECO_CATALOGO", precision = 10, scale = 2)
    private BigDecimal snapshotPrecoCatalogo;

    @Column(name = "SNAPSHOT_TAXA_DESCONTO_PCT", precision = 5, scale = 2)
    private BigDecimal snapshotTaxaDescontoPct;

    @Column(name = "SNAPSHOT_NIVEL_FIDELIDADE", length = 20)
    private String snapshotNivelFidelidade;

    public Transacao() {}

    public Transacao(Long id, Agendamento agendamento, String codigoTransacaoGateway, String metodoPagamento, StatusTransacao statusTransacao, BigDecimal valorBruto, BigDecimal valorDescontoFidelidade, BigDecimal valorLiquidoPago, String codigoVoucher, String qrCodeHash, Boolean voucherUtilizado, LocalDateTime dataCriacao, LocalDateTime dataPagamento, LocalDateTime dataUtilizacaoVoucher) {
        this.id = id;
        this.agendamento = agendamento;
        this.codigoTransacaoGateway = codigoTransacaoGateway;
        this.metodoPagamento = metodoPagamento;
        this.statusTransacao = statusTransacao;
        this.valorBruto = valorBruto;
        this.valorDescontoFidelidade = valorDescontoFidelidade;
        this.valorLiquidoPago = valorLiquidoPago;
        this.codigoVoucher = codigoVoucher;
        this.qrCodeHash = qrCodeHash;
        this.voucherUtilizado = voucherUtilizado;
        this.dataCriacao = dataCriacao;
        this.dataPagamento = dataPagamento;
        this.dataUtilizacaoVoucher = dataUtilizacaoVoucher;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Agendamento getAgendamento() { return agendamento; }
    public void setAgendamento(Agendamento agendamento) { this.agendamento = agendamento; }

    public String getCodigoTransacaoGateway() { return codigoTransacaoGateway; }
    public void setCodigoTransacaoGateway(String codigoTransacaoGateway) { this.codigoTransacaoGateway = codigoTransacaoGateway; }

    public String getMetodoPagamento() { return metodoPagamento; }
    public void setMetodoPagamento(String metodoPagamento) { this.metodoPagamento = metodoPagamento; }

    public StatusTransacao getStatusTransacao() { return statusTransacao; }
    public void setStatusTransacao(StatusTransacao statusTransacao) { this.statusTransacao = statusTransacao; }

    public BigDecimal getValorBruto() { return valorBruto; }
    public void setValorBruto(BigDecimal valorBruto) { this.valorBruto = valorBruto; }

    public BigDecimal getValorDescontoFidelidade() { return valorDescontoFidelidade; }
    public void setValorDescontoFidelidade(BigDecimal valorDescontoFidelidade) { this.valorDescontoFidelidade = valorDescontoFidelidade; }

    public BigDecimal getValorLiquidoPago() { return valorLiquidoPago; }
    public void setValorLiquidoPago(BigDecimal valorLiquidoPago) { this.valorLiquidoPago = valorLiquidoPago; }

    public String getCodigoVoucher() { return codigoVoucher; }
    public void setCodigoVoucher(String codigoVoucher) { this.codigoVoucher = codigoVoucher; }

    public String getQrCodeHash() { return qrCodeHash; }
    public void setQrCodeHash(String qrCodeHash) { this.qrCodeHash = qrCodeHash; }

    public Boolean getVoucherUtilizado() { return voucherUtilizado; }
    public void setVoucherUtilizado(Boolean voucherUtilizado) { this.voucherUtilizado = voucherUtilizado; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    public LocalDateTime getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDateTime dataPagamento) { this.dataPagamento = dataPagamento; }

    public LocalDateTime getDataUtilizacaoVoucher() { return dataUtilizacaoVoucher; }
    public void setDataUtilizacaoVoucher(LocalDateTime dataUtilizacaoVoucher) { this.dataUtilizacaoVoucher = dataUtilizacaoVoucher; }

    public BigDecimal getSnapshotPrecoCatalogo() { return snapshotPrecoCatalogo; }
    public void setSnapshotPrecoCatalogo(BigDecimal snapshotPrecoCatalogo) { this.snapshotPrecoCatalogo = snapshotPrecoCatalogo; }

    public BigDecimal getSnapshotTaxaDescontoPct() { return snapshotTaxaDescontoPct; }
    public void setSnapshotTaxaDescontoPct(BigDecimal snapshotTaxaDescontoPct) { this.snapshotTaxaDescontoPct = snapshotTaxaDescontoPct; }

    public String getSnapshotNivelFidelidade() { return snapshotNivelFidelidade; }
    public void setSnapshotNivelFidelidade(String snapshotNivelFidelidade) { this.snapshotNivelFidelidade = snapshotNivelFidelidade; }
}
