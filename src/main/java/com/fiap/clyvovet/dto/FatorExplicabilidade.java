package com.fiap.clyvovet.dto;

/**
 * Vetor de explicabilidade clínica (XAI) indicando fator analisado, impacto na pontuação e justificativa clínica.
 */
public record FatorExplicabilidade(
        String fator,
        String impacto,
        String tipo,
        String descricao
) {}
