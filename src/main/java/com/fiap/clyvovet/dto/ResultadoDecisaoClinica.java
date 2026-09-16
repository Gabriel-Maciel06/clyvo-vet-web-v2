package com.fiap.clyvovet.dto;

import com.fiap.clyvovet.model.ClassificacaoRisco;
import com.fiap.clyvovet.model.TipoMotorDecisao;

import java.util.List;

/**
 * DTO Unificado e Coerente para representação de decisões clínicas profiláticas.
 * Segrega formalmente probabilidade estocástica (exclusiva de Machine Learning)
 * de conformidade fisiológica determinística (Sistemas Especialistas Base 100).
 */
public record ResultadoDecisaoClinica(
        Double probabilidadeHigidez,            // null quando for Sistema Especialista determinístico
        int escoreSaudeLongevidade,              // 0 a 100 universal para todas as espécies
        ClassificacaoRisco classificacaoRisco,
        List<FatorExplicabilidade> fatoresExplicabilidade,
        List<RiscoFenotipico> riscosEspecificos,
        String sinteseSoap,
        String versaoMotor,
        TipoMotorDecisao tipoMotor
) {
    public boolean isMachineLearning() {
        return tipoMotor == TipoMotorDecisao.MACHINE_LEARNING_SUPERVISIONADO;
    }

    // Métodos de interoperabilidade
    public int escoreLongevidade() { return escoreSaudeLongevidade; }
    public String versaoModelo() { return versaoMotor; }
    public List<FatorExplicabilidade> fatoresXai() { return fatoresExplicabilidade; }

    public String resumoFormatadoXai() {
        StringBuilder sb = new StringBuilder();
        if (isMachineLearning()) {
            sb.append(String.format("P(Higidez)=%.1f%% | Modelo: %s\nFatores XAI: ",
                    probabilidadeHigidez != null ? probabilidadeHigidez : 0.0, versaoMotor));
        } else {
            sb.append(String.format("Conformidade Fisiológica=%d/100 | Motor: %s\nFatores Clínicos: ",
                    escoreSaudeLongevidade, versaoMotor));
        }
        if (fatoresExplicabilidade != null) {
            for (int i = 0; i < fatoresExplicabilidade.size(); i++) {
                FatorExplicabilidade f = fatoresExplicabilidade.get(i);
                sb.append("[").append(f.impacto()).append(" ").append(f.fator()).append("]");
                if (i < fatoresExplicabilidade.size() - 1) sb.append(" ");
            }
        }
        return sb.toString();
    }
}
