package com.fiap.clyvovet.gateway.dto;

import com.fiap.clyvovet.model.StatusTransacao;
import java.time.LocalDateTime;

public record CobrancaGeradaDto(
        String idTransacaoGateway,
        StatusTransacao status,
        String qrCodePixCopiaCola,
        String qrCodePixBase64,
        String linkCheckout,
        LocalDateTime dataExpiracao,
        String mensagem,
        boolean simulado
) {}
