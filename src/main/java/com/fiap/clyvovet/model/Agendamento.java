package com.fiap.clyvovet.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "T_AGENDAMENTO")
public class Agendamento {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLINICA_ID", nullable = false)
    private Clinica clinica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SERVICO_ID", nullable = false)
    private Servico servico;

    @Column(name = "DATA_HORA_AGENDAMENTO", nullable = false)
    private LocalDateTime dataHoraAgendamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS_AGENDAMENTO", nullable = false, length = 30)
    private StatusAgendamento statusAgendamento = StatusAgendamento.SOLICITADO;

    @Column(name = "OBSERVACOES", length = 500)
    private String observacoes;

    @Column(name = "DATA_CRIACAO", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    public Agendamento() {}

    public Agendamento(Long id, Pet pet, Tutor tutor, Clinica clinica, Servico servico, LocalDateTime dataHoraAgendamento, StatusAgendamento statusAgendamento, String observacoes, LocalDateTime dataCriacao) {
        this.id = id;
        this.pet = pet;
        this.tutor = tutor;
        this.clinica = clinica;
        this.servico = servico;
        this.dataHoraAgendamento = dataHoraAgendamento;
        this.statusAgendamento = statusAgendamento;
        this.observacoes = observacoes;
        this.dataCriacao = dataCriacao;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Pet getPet() { return pet; }
    public void setPet(Pet pet) { this.pet = pet; }

    public Tutor getTutor() { return tutor; }
    public void setTutor(Tutor tutor) { this.tutor = tutor; }

    public Clinica getClinica() { return clinica; }
    public void setClinica(Clinica clinica) { this.clinica = clinica; }

    public Servico getServico() { return servico; }
    public void setServico(Servico servico) { this.servico = servico; }

    public LocalDateTime getDataHoraAgendamento() { return dataHoraAgendamento; }
    public void setDataHoraAgendamento(LocalDateTime dataHoraAgendamento) { this.dataHoraAgendamento = dataHoraAgendamento; }

    public StatusAgendamento getStatusAgendamento() { return statusAgendamento; }
    public void setStatusAgendamento(StatusAgendamento statusAgendamento) { this.statusAgendamento = statusAgendamento; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
}
