package com.fiap.clyvovet.gateway.dto;

import com.fiap.clyvovet.model.StatusTransacao;
import java.time.LocalDateTime;

public record StatusCobrancaDto(
        String idTransacaoGateway,
        StatusTransacao status,
        boolean pago,
        LocalDateTime dataPagamento,
        String detalhes
) {}
