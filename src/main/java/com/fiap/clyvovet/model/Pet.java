package com.fiap.clyvovet.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

@Entity
@Table(name = "T_PET")
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOME", nullable = false, length = 100)
    private String nome;

    @Column(name = "DATA_NASCIMENTO", nullable = false)
    private LocalDate dataNascimento;

    @Column(name = "PESO", nullable = false, precision = 5, scale = 2)
    private BigDecimal peso;

    @Column(name = "STATUS_LONGEVIDADE", length = 500)
    private String statusLongevidade;

    @Column(name = "ESCORE_SAUDE")
    private Integer escoreSaude;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "RACA_ID", nullable = false)
    private Raca raca;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TUTOR_CPF", nullable = false)
    private Tutor tutor;

    public Pet() {}

    public Pet(Long id, String nome, LocalDate dataNascimento, BigDecimal peso, String statusLongevidade, Integer escoreSaude, Raca raca, Tutor tutor) {
        this.id = id;
        this.nome = nome;
        this.dataNascimento = dataNascimento;
        this.peso = peso;
        this.statusLongevidade = statusLongevidade;
        this.escoreSaude = escoreSaude;
        this.raca = raca;
        this.tutor = tutor;
    }

    public int getIdadeAnos() {
        if (dataNascimento == null) return 0;
        return Period.between(dataNascimento, LocalDate.now()).getYears();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public BigDecimal getPeso() { return peso; }
    public void setPeso(BigDecimal peso) { this.peso = peso; }
    public String getStatusLongevidade() { return statusLongevidade; }
    public void setStatusLongevidade(String statusLongevidade) { this.statusLongevidade = statusLongevidade; }
    public Integer getEscoreSaude() { return escoreSaude; }
    public void setEscoreSaude(Integer escoreSaude) { this.escoreSaude = escoreSaude; }
    public Raca getRaca() { return raca; }
    public void setRaca(Raca raca) { this.raca = raca; }
    public Tutor getTutor() { return tutor; }
    public void setTutor(Tutor tutor) { this.tutor = tutor; }
}
