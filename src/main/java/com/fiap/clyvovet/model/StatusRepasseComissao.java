package com.fiap.clyvovet.model;

public enum StatusRepasseComissao {
    RETIDO_ESCROW("Retido em Custódia (Escrow)"),
    LIBERADO_APOS_ATENDIMENTO("Liberado após Check-in/Voucher"),
    PAGO_LIQUIDADO("Repasse Liquidado via PIX");

    private final String descricao;

    StatusRepasseComissao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
