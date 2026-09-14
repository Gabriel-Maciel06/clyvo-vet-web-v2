package com.fiap.clyvovet.model;

public enum AlimentacaoStatus {
    RECOMENDADA("Alimentação recomendada / Balanceada", "text-success"),
    PETISCOS_MODERADOS("Ração + Petiscos moderados", "text-primary"),
    EXCESSO("Excesso de petiscos / Dieta desregulada", "text-warning"),
    POUCO_APETITE("Pouco apetite / Comeu menos que o normal", "text-danger");

    private final String descricao;
    private final String colorClass;

    AlimentacaoStatus(String descricao, String colorClass) {
        this.descricao = descricao;
        this.colorClass = colorClass;
    }

    public String getDescricao() { return descricao; }
    public String getColorClass() { return colorClass; }
}
