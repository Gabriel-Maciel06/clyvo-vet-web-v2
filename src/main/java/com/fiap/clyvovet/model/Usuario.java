package com.fiap.clyvovet.model;

import jakarta.persistence.*;

@Entity
@Table(name = "T_USUARIO")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USERNAME", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "PASSWORD", nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 20)
    private RoleUsuario role;

    @Column(name = "NOME_COMPLETO", nullable = false, length = 100)
    private String nomeCompleto;

    @Column(name = "EMAIL", nullable = false, length = 100)
    private String email;

    public Usuario() {}

    public Usuario(Long id, String username, String password, RoleUsuario role, String nomeCompleto, String email) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.nomeCompleto = nomeCompleto;
        this.email = email;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public RoleUsuario getRole() { return role; }
    public void setRole(RoleUsuario role) { this.role = role; }
    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
