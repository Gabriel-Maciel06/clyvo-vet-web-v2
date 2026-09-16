package com.fiap.clyvovet.model;

public enum StatusTransacao {
    PENDENTE("Aguardando Liquidação Gateway"),
    PAGO("Capturado e Confirmado In-App"),
    ESTORNADO("Estornado / Reembolsado"),
    FALHOU("Falha na Transação");

    private final String descricao;

    StatusTransacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
