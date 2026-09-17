package com.fiap.clyvovet.service;

import com.fiap.clyvovet.service.engine.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @deprecated Orquestrador legado. Migrar para {@link ClinicalDecisionOrchestrator}.
 *
 * <p>Esta classe é mantida exclusivamente como wrapper retrocompativel para nao quebrar
 * dependencias de injecao existentes em {@link TriagemService} e nos testes unitarios.
 * Ela herda todo o comportamento de {@link ClinicalDecisionOrchestrator} sem adicionar
 * logica propria - qualquer nova funcionalidade deve ser implementada no orquestrador.</p>
 *
 * <p><strong>Motivo da Deprecacao:</strong> O nome {@code PredictiveMlEngine} viola o
 * Principio do Menor Espanto (Principle of Least Astonishment) ao nomear como "ML Engine"
 * um Context que orquestra 9 estrategias determinísticas e apenas 1 de ML. O nome correto
 * do orquestrador é {@link ClinicalDecisionOrchestrator}.</p>
 */
@Deprecated
@Service
public class PredictiveMlEngine extends ClinicalDecisionOrchestrator {

    // Aliases retrocompativeis - mantidos para nao quebrar referencias em testes e servicos legados
    public static final String MODEL_VERSION_CANINE       = ClinicalDecisionOrchestrator.MODEL_VERSION_CANINE;
    public static final String MODEL_VERSION              = MODEL_VERSION_CANINE;
    public static final String MODEL_VERSION_ECTOTHERMIC  = ClinicalDecisionOrchestrator.ENGINE_VERSION_ECTOTHERMIC;
    public static final String MODEL_VERSION_AQUATIC      = ClinicalDecisionOrchestrator.ENGINE_VERSION_AQUATIC;
    public static final String MODEL_VERSION_AVIAN        = ClinicalDecisionOrchestrator.ENGINE_VERSION_AVIAN;
    public static final String MODEL_VERSION_INVERTEBRATE = ClinicalDecisionOrchestrator.ENGINE_VERSION_INVERTEBRATE;

    public PredictiveMlEngine(List<MotorDecisaoClinicaStrategy> strategies,
                               DefaultPhysiologyEngine fallbackEngine) {
        super(strategies, fallbackEngine);
    }
}
