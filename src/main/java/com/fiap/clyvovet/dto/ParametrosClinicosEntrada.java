package com.fiap.clyvovet.dto;

import com.fiap.clyvovet.model.CheckinDiario;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO Agnóstico e Neutro de Entrada para Avaliação Clínica Profilática.
 * Substitui parâmetros soltos e desacopla a entrada dos motores clínicos de pressupostos mamíferos rígidos,
 * viabilizando a representação adequada de endotérmicos, ectotérmicos, animais aquáticos e artrópodes.
 */
public record ParametrosClinicosEntrada(
        BigDecimal pesoAferido,
        BigDecimal temperaturaAferida,       // Retal/cloacal (endotérmicos) ou POTZ/ambiente/aquário (ectotérmicos/artrópodes)
        Integer frequenciaMensurada,         // Ausculta cardíaca bpm (endotérmicos), Doppler (répteis), Mov. Operculares/min (peixes) ou null (invertebrados)
        List<CheckinDiario> checkinsRecentes,
        int totalConsultasHistorico,
        String queixaPrincipal
) {
    public ParametrosClinicosEntrada {
        checkinsRecentes = (checkinsRecentes != null) ? List.copyOf(checkinsRecentes) : List.of();
    }

    public static ParametrosClinicosEntrada of(BigDecimal pesoAferido,
                                               BigDecimal temperaturaAferida,
                                               Integer frequenciaMensurada,
                                               List<CheckinDiario> checkinsRecentes,
                                               int totalConsultasHistorico,
                                               String queixaPrincipal) {
        return new ParametrosClinicosEntrada(pesoAferido, temperaturaAferida, frequenciaMensurada,
                checkinsRecentes, totalConsultasHistorico, queixaPrincipal);
    }
}
