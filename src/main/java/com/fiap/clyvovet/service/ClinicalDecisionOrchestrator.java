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
 * Orquestrador Central de Decisão Clínica Profilática (Clyvo Vet CDSS).
 *
 * <p>Implementa o <strong>Context</strong> do Padrão de Projeto Strategy para roteamento
 * taxonômico de decisão clínica. Recebe por injeção de dependência do Spring Container
 * a lista completa de {@link MotorDecisaoClinicaStrategy} disponíveis e, em tempo de
 * execução, despacha cada requisição para a estratégia especializada que declara suporte
 * à espécie do paciente.</p>
 *
 * <p><strong>Dois Paradigmas de Inferência sob Padrão Strategy:</strong></p>
 * <ol>
 *   <li><strong>Machine Learning Supervisionado</strong> ({@link CaninePredictiveMlEngine}):
 *       Exclusivo para caninos. Regressão logística multivariada treinada sobre 10.000 amostras
 *       calibradas. Retorna {@code P(Higidez | X) in [0, 1]} e escore probabilístico real.</li>
 *   <li><strong>Sistema Especialista Baseado em Conhecimento</strong> (todos os demais engines):
 *       Regras clínicas determinísticas fundamentadas em diretrizes veterinárias internacionais
 *       (AAFP, AAV, ABRAVAS, BSAVA, AAEP). Retorna {@code probabilidadeHigidez = null} e
 *       escore de conformidade em Base 100. Sem pseudo-probabilidades estocásticas.</li>
 * </ol>
 *
 * <p><strong>Fallback Universal:</strong> {@link DefaultPhysiologyEngine} garante que nenhuma
 * espécie — mesmo não mapeada nas estratégias especializadas — cause {@code NoSuchElementException}
 * ou HTTP 500. A aplicação opera com resiliência total.</p>
 */
@Service
public class ClinicalDecisionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ClinicalDecisionOrchestrator.class);

    public static final String MODEL_VERSION_CANINE       = CaninePredictiveMlEngine.VERSAO_MOTOR;
    public static final String ENGINE_VERSION_FELINE      = FelinePhysiologyEngine.VERSAO_MOTOR;
    public static final String ENGINE_VERSION_AVIAN       = AvianPhysiologyEngine.VERSAO_MOTOR;
    public static final String ENGINE_VERSION_ECTOTHERMIC = EctothermicPhysiologyEngine.VERSAO_MOTOR;
    public static final String ENGINE_VERSION_AQUATIC     = AquaticPhysiologyEngine.VERSAO_MOTOR;
    public static final String ENGINE_VERSION_SMALL_MAMMAL= SmallMammalPhysiologyEngine.VERSAO_MOTOR;
    public static final String ENGINE_VERSION_MUSTELID    = MustelidPhysiologyEngine.VERSAO_MOTOR;
    public static final String ENGINE_VERSION_INVERTEBRATE= InvertebratePhysiologyEngine.VERSAO_MOTOR;
    public static final String ENGINE_VERSION_EQUINE      = EquinePhysiologyEngine.VERSAO_MOTOR;
    public static final String ENGINE_VERSION_GENERIC     = DefaultPhysiologyEngine.VERSAO_MOTOR;

    /** @deprecated Use {@link #MODEL_VERSION_CANINE} */
    @Deprecated
    public static final String MODEL_VERSION = MODEL_VERSION_CANINE;

    private final List<MotorDecisaoClinicaStrategy> strategies;
    private final DefaultPhysiologyEngine fallbackEngine;

    public ClinicalDecisionOrchestrator(List<MotorDecisaoClinicaStrategy> strategies,
                                        DefaultPhysiologyEngine fallbackEngine) {
        this.strategies = strategies;
        this.fallbackEngine = fallbackEngine;
        log.info("ClinicalDecisionOrchestrator inicializado com {} estrategias clinicas especializadas e fallback universal.",
                strategies.size());
    }

    /**
     * Ponto central de decisão clínica profilática.
     * Localiza a estratégia taxonômica adequada via suporta() e delega a avaliação.
     * Em espécie não mapeada, aciona o DefaultPhysiologyEngine como fallback garantido.
     */
    public ResultadoDecisaoClinica executarDecisao(Pet pet, ParametrosClinicosEntrada entrada) {
        String especie = (pet != null && pet.getRaca() != null && pet.getRaca().getEspecie() != null)
                ? pet.getRaca().getEspecie().trim().toUpperCase()
                : "CANINA";

        MotorDecisaoClinicaStrategy strategy = strategies.stream()
                .filter(s -> !(s instanceof DefaultPhysiologyEngine))
                .filter(s -> s.suporta(especie))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("Nenhuma estrategia clinica especializada suporta a especie '{}'. Ativando fallback universal.", especie);
                    return fallbackEngine;
                });

        log.debug("Executando decisao clinica para pet '{}' (Especie: {}) via estrategia '{}'.",
                (pet != null ? pet.getNome() : "Desconhecido"), especie, strategy.getClass().getSimpleName());

        return strategy.avaliar(pet, entrada);
    }

    /**
     * Sobrecarga de interoperabilidade que encapsula parâmetros avulsos em ParametrosClinicosEntrada.
     * A frequenciaMensurada representa FC cardíaca, Doppler ou batimentos operculares conforme a espécie.
     */
    public ResultadoDecisaoClinica executarInferencia(Pet pet,
                                                      BigDecimal pesoAferido,
                                                      BigDecimal temperaturaCorporal,
                                                      Integer freqMensurada,
                                                      List<CheckinDiario> checkinsRecentes,
                                                      int totalConsultasHistorico,
                                                      String queixaPrincipal) {
        ParametrosClinicosEntrada entrada = new ParametrosClinicosEntrada(
                pesoAferido,
                temperaturaCorporal,
                freqMensurada,
                checkinsRecentes,
                totalConsultasHistorico,
                queixaPrincipal
        );
        return executarDecisao(pet, entrada);
    }
}
