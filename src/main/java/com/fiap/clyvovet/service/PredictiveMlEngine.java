package com.fiap.clyvovet.service;

import com.fiap.clyvovet.dto.ParametrosClinicosEntrada;
import com.fiap.clyvovet.dto.ResultadoDecisaoClinica;
import com.fiap.clyvovet.model.CheckinDiario;
import com.fiap.clyvovet.model.Pet;
import com.fiap.clyvovet.service.engine.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Motor Orquestrador de Decisão Clínica Profilática (Clyvo Vet CDSS Engine).
 *
 * Implementa o Padrão Strategy para roteamento taxonômico estrito:
 * 1. Caninos: Machine Learning Supervisionado (Random Forest + Regressão Logística, ROC-AUC 0.9485).
 * 2. Felinos: Sistema Especialista Baseado em Diretrizes Fisiológicas (AAFP / ISFM, Base 100).
 * 3. Aves: Sistema Especialista Aviário (AAV Guidelines, Base 100).
 * 4. Répteis: Sistema Especialista Ectotérmico (ABRAVAS / ARAV, Base 100).
 * 5. Peixes: Sistema Especialista Aquático (Medicina de Teleósteos / Biótopo, Base 100).
 * 6. Roedores: Sistema Especialista em Pequenos Mamíferos (BSAVA Rodents, Base 100).
 * 7. Mustelídeos: Sistema Especialista em Furões/Ferrets (BSAVA Ferrets, Base 100).
 * 8. Invertebrados: Sistema Especialista em Aracnídeos (Invertebrate Medicine, Base 100).
 * 9. Equinos: Sistema Especialista em Grandes Animais (AAEP Guidelines, Base 100).
 * 10. Fallback Universal: Resiliência operacional contra espécies não mapeadas.
 */
@Service
public class PredictiveMlEngine {

    private static final Logger log = LoggerFactory.getLogger(PredictiveMlEngine.class);

    // Versões Oficiais dos Motores
    public static final String MODEL_VERSION_CANINE = CaninePredictiveMlEngine.VERSAO_MOTOR;
    public static final String MODEL_VERSION = MODEL_VERSION_CANINE;
    public static final String ENGINE_VERSION_FELINE = FelinePhysiologyEngine.VERSAO_MOTOR;
    public static final String ENGINE_VERSION_AVIAN = AvianPhysiologyEngine.VERSAO_MOTOR;
    public static final String ENGINE_VERSION_ECTOTHERMIC = EctothermicPhysiologyEngine.VERSAO_MOTOR;
    public static final String ENGINE_VERSION_AQUATIC = AquaticPhysiologyEngine.VERSAO_MOTOR;
    public static final String ENGINE_VERSION_SMALL_MAMMAL = SmallMammalPhysiologyEngine.VERSAO_MOTOR;
    public static final String ENGINE_VERSION_MUSTELID = MustelidPhysiologyEngine.VERSAO_MOTOR;
    public static final String ENGINE_VERSION_INVERTEBRATE = InvertebratePhysiologyEngine.VERSAO_MOTOR;
    public static final String ENGINE_VERSION_EQUINE = EquinePhysiologyEngine.VERSAO_MOTOR;
    public static final String ENGINE_VERSION_GENERIC = DefaultPhysiologyEngine.VERSAO_MOTOR;

    // Aliases retrocompatíveis
    public static final String MODEL_VERSION_ECTOTHERMIC = ENGINE_VERSION_ECTOTHERMIC;
    public static final String MODEL_VERSION_AQUATIC = ENGINE_VERSION_AQUATIC;
    public static final String MODEL_VERSION_AVIAN = ENGINE_VERSION_AVIAN;
    public static final String MODEL_VERSION_INVERTEBRATE = ENGINE_VERSION_INVERTEBRATE;

    private final List<MotorDecisaoClinicaStrategy> strategies;
    private final DefaultPhysiologyEngine fallbackEngine;

    public PredictiveMlEngine(List<MotorDecisaoClinicaStrategy> strategies,
                                DefaultPhysiologyEngine fallbackEngine) {
        this.strategies = strategies;
        this.fallbackEngine = fallbackEngine;
        log.info("PredictiveMlEngine inicializado com {} estratégias clínicas especializadas e fallback universal.",
                strategies.size());
    }

    /**
     * Ponto central de decisão clínica profilática:
     * Recebe o paciente e o DTO neutro de entrada, identifica a estratégia taxonômica
     * adequada e executa a avaliação clínica determinística ou preditiva.
     */
    public ResultadoDecisaoClinica executarDecisao(Pet pet, ParametrosClinicosEntrada entrada) {
        String especie = (pet != null && pet.getRaca() != null && pet.getRaca().getEspecie() != null)
                ? pet.getRaca().getEspecie().trim().toUpperCase()
                : "CANINA";

        // Localiza a primeira estratégia especializada que atenda à espécie (ignorando o fallback nesta etapa)
        MotorDecisaoClinicaStrategy strategy = strategies.stream()
                .filter(s -> !(s instanceof DefaultPhysiologyEngine))
                .filter(s -> s.suporta(especie))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("Nenhuma estratégia clínica especializada suporta a espécie '{}'. Ativando fallback universal.", especie);
                    return fallbackEngine;
                });

        log.debug("Executando decisão clínica para pet '{}' (Espécie: {}) via estratégia '{}'.",
                (pet != null ? pet.getNome() : "Desconhecido"), especie, strategy.getClass().getSimpleName());

        return strategy.avaliar(pet, entrada);
    }

    /**
     * Sobrecarga de interoperabilidade que encapsula os parâmetros soltos em ParametrosClinicosEntrada.
     */
    public ResultadoDecisaoClinica executarInferencia(Pet pet,
                                                      BigDecimal pesoAferido,
                                                      BigDecimal temperaturaCorporal,
                                                      Integer freqCardiaca,
                                                      List<CheckinDiario> checkinsRecentes,
                                                      int totalConsultasHistorico,
                                                      String queixaPrincipal) {
        ParametrosClinicosEntrada entrada = new ParametrosClinicosEntrada(
                pesoAferido,
                temperaturaCorporal,
                freqCardiaca,
                checkinsRecentes,
                totalConsultasHistorico,
                queixaPrincipal
        );
        return executarDecisao(pet, entrada);
    }
}
