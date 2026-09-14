package com.fiap.clyvovet.model;

public enum ClassificacaoRisco {
    BAIXO("Baixo Risco / Excelente Longevidade", "badge-baixo-risco", "text-success"),
    MODERADO("Risco Moderado / Atenção Preventiva", "badge-moderado-risco", "text-warning"),
    ALTO("Alto Risco / Necessita Intervenção Clínica Imediata", "badge-alto-risco", "text-danger");

    private final String descricao;
    private final String badgeClass;
    private final String textClass;

    ClassificacaoRisco(String descricao, String badgeClass, String textClass) {
        this.descricao = descricao;
        this.badgeClass = badgeClass;
        this.textClass = textClass;
    }

    public String getDescricao() { return descricao; }
    public String getBadgeClass() { return badgeClass; }
    public String getTextClass() { return textClass; }
}
