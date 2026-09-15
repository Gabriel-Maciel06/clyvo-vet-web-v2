package com.fiap.clyvovet.model;

public enum StatusPagamento {
    PENDENTE_PAGAMENTO("Aguardando Pagamento", "warning"),
    PAGO_CONFIRMADO("Pago & Voucher Ativo", "success"),
    UTILIZADO_NA_CLINICA("Utilizado na Clínica", "secondary"),
    CANCELADO("Cancelado", "danger");

    private final String descricao;
    private final String badgeColor;

    StatusPagamento(String descricao, String badgeColor) {
        this.descricao = descricao;
        this.badgeColor = badgeColor;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getBadgeColor() {
        return badgeColor;
    }
}
