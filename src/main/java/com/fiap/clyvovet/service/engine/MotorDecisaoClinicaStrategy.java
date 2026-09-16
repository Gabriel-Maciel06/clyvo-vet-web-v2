package com.fiap.clyvovet.service.engine;

import com.fiap.clyvovet.dto.ParametrosClinicosEntrada;
import com.fiap.clyvovet.dto.ResultadoDecisaoClinica;
import com.fiap.clyvovet.model.CheckinDiario;
import com.fiap.clyvovet.model.Pet;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface do Padrão Strategy para Motores de Decisão Clínica.
 * Cada implementação encapsula as regras biológicas e o paradigma computacional
 * específico de uma classe taxonômica (Caninos, Felinos, Aves, Répteis, Peixes, Roedores, Mustelídeos, Invertebrados, Equinos).
 */
public interface MotorDecisaoClinicaStrategy {

    /**
     * Avalia se a estratégia atende à espécie informada.
     */
    boolean suporta(String especie);

    /**
     * Executa a avaliação clínica especializada a partir do DTO neutro de entrada.
     */
    ResultadoDecisaoClinica avaliar(Pet pet, ParametrosClinicosEntrada entrada);

    /**
     * Sobrecarga de conveniência com parâmetros desacoplados para interoperabilidade.
     */
    default ResultadoDecisaoClinica avaliar(Pet pet,
                                           BigDecimal pesoAferido,
                                           BigDecimal temperatura,
                                           Integer freqCardiopulmonar,
                                           List<CheckinDiario> checkins,
                                           int totalConsultasHistorico,
                                           String queixaPrincipal) {
        return avaliar(pet, new ParametrosClinicosEntrada(
                pesoAferido, temperatura, freqCardiopulmonar, checkins, totalConsultasHistorico, queixaPrincipal
        ));
    }
}
