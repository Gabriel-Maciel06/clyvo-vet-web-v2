package com.fiap.clyvovet.model;

public enum HumorPet {
    ENERGICO("Muito enérgico e alegre", "text-success", "bi-emoji-laughing-fill"),
    TRANQUILO("Calmo e tranquilo", "text-primary", "bi-emoji-smile-fill"),
    APATICO("Apático / Desanimado", "text-warning", "bi-emoji-neutral-fill"),
    COCEIRA_DESCONFORTO("Com coceira ou incômodo", "text-warning", "bi-exclamation-circle-fill"),
    DOR("Demonstrando dor ou queixa", "text-danger", "bi-emoji-frown-fill");

    private final String descricao;
    private final String colorClass;
    private final String iconClass;

    HumorPet(String descricao, String colorClass, String iconClass) {
        this.descricao = descricao;
        this.colorClass = colorClass;
        this.iconClass = iconClass;
    }

    public String getDescricao() { return descricao; }
    public String getColorClass() { return colorClass; }
    public String getIconClass() { return iconClass; }
}
