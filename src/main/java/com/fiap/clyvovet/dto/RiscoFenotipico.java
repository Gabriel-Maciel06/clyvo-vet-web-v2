package com.fiap.clyvovet.dto;

/**
 * Mapeamento de predisposições raciais e fenotípicas da espécie para direcionamento preventivo.
 */
public record RiscoFenotipico(
        String categoria,
        String status,
        String badge,
        String detalhes
) {}
