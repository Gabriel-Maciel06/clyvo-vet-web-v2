package com.fiap.clyvovet.model;

public enum StatusAgendamento {
    SOLICITADO("Solicitado pelo Tutor"),
    CONFIRMADO("Confirmado pela Clínica"),
    REALIZADO("Atendimento Realizado"),
    CANCELADO("Cancelado"),
    NAO_COMPARECEU("Não Compareceu");

    private final String descricao;

    StatusAgendamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
