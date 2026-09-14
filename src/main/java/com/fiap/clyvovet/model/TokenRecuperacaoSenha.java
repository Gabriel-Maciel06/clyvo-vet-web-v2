package com.fiap.clyvovet.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Token de uso único enviado por e-mail para o fluxo de "esqueci minha senha".
 * Expira em {@link #dataExpiracao} e não pode ser reaproveitado após {@link #usado}.
 */
@Entity
@Table(name = "T_TOKEN_RECUPERACAO_SENHA")
public class TokenRecuperacaoSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "USUARIO_ID", nullable = false)
    private Usuario usuario;

    @Column(name = "TOKEN", nullable = false, unique = true, length = 100)
    private String token;

    @Column(name = "DATA_CRIACAO", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "DATA_EXPIRACAO", nullable = false)
    private LocalDateTime dataExpiracao;

    @Column(name = "USADO", nullable = false)
    private boolean usado = false;

    public TokenRecuperacaoSenha() {}

    public TokenRecuperacaoSenha(Usuario usuario, String token, LocalDateTime dataCriacao, LocalDateTime dataExpiracao) {
        this.usuario = usuario;
        this.token = token;
        this.dataCriacao = dataCriacao;
        this.dataExpiracao = dataExpiracao;
    }

    public boolean isValido() {
        return !usado && LocalDateTime.now().isBefore(dataExpiracao);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
    public LocalDateTime getDataExpiracao() { return dataExpiracao; }
    public void setDataExpiracao(LocalDateTime dataExpiracao) { this.dataExpiracao = dataExpiracao; }
    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }
}
