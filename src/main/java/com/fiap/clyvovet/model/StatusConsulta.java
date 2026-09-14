package com.fiap.clyvovet.model;

public enum StatusConsulta {
    SOLICITADA("Aguardando Triagem", "badge bg-warning text-dark"),
    AGENDADA("Consulta Agendada", "badge bg-info text-dark"),
    CONCLUIDA("Triagem Concluída", "badge bg-success"),
    CANCELADA("Cancelada", "badge bg-secondary");

    private final String descricao;
    private final String badgeClass;

    StatusConsulta(String descricao, String badgeClass) {
        this.descricao = descricao;
        this.badgeClass = badgeClass;
    }

    public String getDescricao() { return descricao; }
    public String getBadgeClass() { return badgeClass; }
}
