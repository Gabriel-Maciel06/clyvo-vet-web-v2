package com.fiap.clyvovet.model;

import jakarta.persistence.*;

@Entity
@Table(name = "T_TUTOR")
public class Tutor {

    @Id
    @Column(name = "CPF", length = 14)
    private String cpf;

    @Column(name = "NOME", nullable = false, length = 100)
    private String nome;

    @Column(name = "TELEFONE", length = 20)
    private String telefone;

    @Column(name = "EMAIL", length = 100)
    private String email;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USUARIO_ID")
    private Usuario usuario;

    public Tutor() {}

    public Tutor(String cpf, String nome, String telefone, String email, Usuario usuario) {
        this.cpf = cpf;
        this.nome = nome;
        this.telefone = telefone;
        this.email = email;
        this.usuario = usuario;
    }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
