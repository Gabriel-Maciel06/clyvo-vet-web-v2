package com.fiap.clyvovet.dto;

import java.time.LocalDate;

public class BadgeItemDto {

    private String codigo;
    private String nome;
    private String icone;
    private String descricao;
    private boolean desbloqueado;
    private LocalDate dataConquista;
    private int progressoAtual;
    private int progressoTotal;
    private String dicaProgresso;

    public BadgeItemDto() {}

    public BadgeItemDto(String codigo, String nome, String icone, String descricao, boolean desbloqueado, LocalDate dataConquista, int progressoAtual, int progressoTotal, String dicaProgresso) {
        this.codigo = codigo;
        this.nome = nome;
        this.icone = icone;
        this.descricao = descricao;
        this.desbloqueado = desbloqueado;
        this.dataConquista = dataConquista;
        this.progressoAtual = progressoAtual;
        this.progressoTotal = progressoTotal;
        this.dicaProgresso = dicaProgresso;
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getIcone() { return icone; }
    public void setIcone(String icone) { this.icone = icone; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public boolean isDesbloqueado() { return desbloqueado; }
    public void setDesbloqueado(boolean desbloqueado) { this.desbloqueado = desbloqueado; }
    public LocalDate getDataConquista() { return dataConquista; }
    public void setDataConquista(LocalDate dataConquista) { this.dataConquista = dataConquista; }
    public int getProgressoAtual() { return progressoAtual; }
    public void setProgressoAtual(int progressoAtual) { this.progressoAtual = progressoAtual; }
    public int getProgressoTotal() { return progressoTotal; }
    public void setProgressoTotal(int progressoTotal) { this.progressoTotal = progressoTotal; }
    public String getDicaProgresso() { return dicaProgresso; }
    public void setDicaProgresso(String dicaProgresso) { this.dicaProgresso = dicaProgresso; }

    public int getPercentual() {
        if (progressoTotal <= 0) return 0;
        return Math.min(100, (int) Math.round(((double) progressoAtual / progressoTotal) * 100));
    }
}
