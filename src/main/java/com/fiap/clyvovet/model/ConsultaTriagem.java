package com.fiap.clyvovet.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "T_CONSULTA_TRIAGEM")
public class ConsultaTriagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PET_ID", nullable = false)
    private Pet pet;

    @Column(name = "DATA_SOLICITACAO", nullable = false)
    private LocalDateTime dataSolicitacao = LocalDateTime.now();

    @Column(name = "DATA_CONSULTA")
    private LocalDateTime dataConsulta;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 30)
    private StatusConsulta status = StatusConsulta.SOLICITADA;

    @Column(name = "QUEIXA_PRINCIPAL", nullable = false, length = 500)
    private String queixaPrincipal;

    @Column(name = "PESO_AFERIDO", precision = 5, scale = 2)
    private BigDecimal pesoAferido;

    @Column(name = "TEMPERATURA", precision = 4, scale = 1)
    private BigDecimal temperatura;

    @Column(name = "FREQUENCIA_CARDIACA")
    private Integer frequenciaCardiaca;

    @Column(name = "ESCORE_LONGEVIDADE")
    private Integer escoreLongevidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "CLASSIFICACAO_RISCO", length = 20)
    private ClassificacaoRisco classificacaoRisco;

    @Column(name = "INSIGHT_IA", length = 1000)
    private String insightIa;

    @Column(name = "PARECER_VETERINARIO", length = 1000)
    private String parecerVeterinario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "VETERINARIO_ID")
    private Usuario veterinario;

    public ConsultaTriagem() {}

    public ConsultaTriagem(Long id, Pet pet, LocalDateTime dataSolicitacao, LocalDateTime dataConsulta, StatusConsulta status, String queixaPrincipal, BigDecimal pesoAferido, BigDecimal temperatura, Integer frequenciaCardiaca, Integer escoreLongevidade, ClassificacaoRisco classificacaoRisco, String insightIa, String parecerVeterinario, Usuario veterinario) {
        this.id = id;
        this.pet = pet;
        this.dataSolicitacao = dataSolicitacao;
        this.dataConsulta = dataConsulta;
        this.status = status;
        this.queixaPrincipal = queixaPrincipal;
        this.pesoAferido = pesoAferido;
        this.temperatura = temperatura;
        this.frequenciaCardiaca = frequenciaCardiaca;
        this.escoreLongevidade = escoreLongevidade;
        this.classificacaoRisco = classificacaoRisco;
        this.insightIa = insightIa;
        this.parecerVeterinario = parecerVeterinario;
        this.veterinario = veterinario;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Pet getPet() { return pet; }
    public void setPet(Pet pet) { this.pet = pet; }
    public LocalDateTime getDataSolicitacao() { return dataSolicitacao; }
    public void setDataSolicitacao(LocalDateTime dataSolicitacao) { this.dataSolicitacao = dataSolicitacao; }
    public LocalDateTime getDataConsulta() { return dataConsulta; }
    public void setDataConsulta(LocalDateTime dataConsulta) { this.dataConsulta = dataConsulta; }
    public StatusConsulta getStatus() { return status; }
    public void setStatus(StatusConsulta status) { this.status = status; }
    public String getQueixaPrincipal() { return queixaPrincipal; }
    public void setQueixaPrincipal(String queixaPrincipal) { this.queixaPrincipal = queixaPrincipal; }
    public BigDecimal getPesoAferido() { return pesoAferido; }
    public void setPesoAferido(BigDecimal pesoAferido) { this.pesoAferido = pesoAferido; }
    public BigDecimal getTemperatura() { return temperatura; }
    public void setTemperatura(BigDecimal temperatura) { this.temperatura = temperatura; }
    public Integer getFrequenciaCardiaca() { return frequenciaCardiaca; }
    public void setFrequenciaCardiaca(Integer frequenciaCardiaca) { this.frequenciaCardiaca = frequenciaCardiaca; }
    public Integer getEscoreLongevidade() { return escoreLongevidade; }
    public void setEscoreLongevidade(Integer escoreLongevidade) { this.escoreLongevidade = escoreLongevidade; }
    public ClassificacaoRisco getClassificacaoRisco() { return classificacaoRisco; }
    public void setClassificacaoRisco(ClassificacaoRisco classificacaoRisco) { this.classificacaoRisco = classificacaoRisco; }
    public String getInsightIa() { return insightIa; }
    public void setInsightIa(String insightIa) { this.insightIa = insightIa; }
    public String getParecerVeterinario() { return parecerVeterinario; }
    public void setParecerVeterinario(String parecerVeterinario) { this.parecerVeterinario = parecerVeterinario; }
    public Usuario getVeterinario() { return veterinario; }
    public void setVeterinario(Usuario veterinario) { this.veterinario = veterinario; }
}
