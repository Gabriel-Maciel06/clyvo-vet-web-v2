package com.fiap.clyvovet.gateway.dto;

import java.math.BigDecimal;

public record RequisicaoCobrancaDto(
        String identificadorPedido,
        BigDecimal valor,
        String descricao,
        String metodoPagamento,
        String pagadorNome,
        String pagadorEmail,
        String pagadorCpf
) {}
