package com.fiap.clyvovet.service.engine;

import com.fiap.clyvovet.dto.FatorExplicabilidade;
import com.fiap.clyvovet.dto.ParametrosClinicosEntrada;
import com.fiap.clyvovet.dto.ResultadoDecisaoClinica;
import com.fiap.clyvovet.dto.RiscoFenotipico;
import com.fiap.clyvovet.model.CheckinDiario;
import com.fiap.clyvovet.model.ClassificacaoRisco;
import com.fiap.clyvovet.model.Pet;
import com.fiap.clyvovet.model.TipoMotorDecisao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Sistema Especialista em Medicina Aviária (Psitaciformes, Passeriformes).
 * Avaliação determinística por Conformidade Fisiológica Base 100 (AAV Guidelines).
 * Considera o altíssimo metabolismo basal aviário:
 * - Eutermia cloacal (39.5°C a 42.5°C)
 * - Taquicardia fisiológica (150 a 400 bpm)
 * - Sensibilidade extrema do trato respiratório (sacos aéreos / vapores de PTFE).
 */
@Component
public class AvianPhysiologyEngine implements MotorDecisaoClinicaStrategy {

    public static final String VERSAO_MOTOR = "Avian-Physiology-Rules-v1.0";

    @Override
    public boolean suporta(String especie) {
        return "AVE".equalsIgnoreCase(especie) || "AVES".equalsIgnoreCase(especie)
                || "CALOPSITA".equalsIgnoreCase(especie) || "PAPAGAIO".equalsIgnoreCase(especie)
                || "ARARA".equalsIgnoreCase(especie) || "PERIQUITO".equalsIgnoreCase(especie)
                || "CANARIO".equalsIgnoreCase(especie);
    }

    @Override
    public ResultadoDecisaoClinica avaliar(Pet pet, ParametrosClinicosEntrada entrada) {
        BigDecimal pesoAferido = (entrada != null) ? entrada.pesoAferido() : null;
        BigDecimal tempCloacal = (entrada != null) ? entrada.temperaturaAferida() : null;
        Integer freqCardiaca = (entrada != null) ? entrada.frequenciaMensurada() : null;
        List<CheckinDiario> checkins = (entrada != null) ? entrada.checkinsRecentes() : List.of();
        int totalConsultasHistorico = (entrada != null) ? entrada.totalConsultasHistorico() : 1;
        String queixaPrincipal = (entrada != null) ? entrada.queixaPrincipal() : "Rotina";

        int expectativa = (pet.getRaca() != null && pet.getRaca().getExpectativaVida() != null)
                ? pet.getRaca().getExpectativaVida() : 15;
        int idadeAnos = (pet.getDataNascimento() != null)
                ? Period.between(pet.getDataNascimento(), LocalDate.now()).getYears() : 2;

        double pesoKg = (pesoAferido != null)
                ? pesoAferido.doubleValue() : (pet.getPeso() != null ? pet.getPeso().doubleValue() : 0.10);
        double tempVal = (tempCloacal != null) ? tempCloacal.doubleValue() : 41.2;

        int escoreFisiologico = 100;
        List<FatorExplicabilidade> fatores = new ArrayList<>();
        List<RiscoFenotipico> riscos = new ArrayList<>();

        // 1. Termorregulação Aviária (Normal: 39.5°C a 42.5°C)
        if (tempVal < 38.5) {
            escoreFisiologico -= 35;
            fatores.add(new FatorExplicabilidade("Hipotermia Aviária Severa (< 38.5°C)", "-35 pts", "critico",
                    "Em aves, temperatura < 38.5°C indica colapso metabólico, exaustão glicêmica ou choque endotóxico. Requer UTI aquecida imediata."));
            riscos.add(new RiscoFenotipico("Termorregulação Aviária", "Emergência", "Hipotermia Aguda",
                    "Temperatura cloacal de " + tempVal + "°C requer hospitalização em câmara aquecida a 30°C."));
        } else if (tempVal > 43.0) {
            escoreFisiologico -= 35;
            fatores.add(new FatorExplicabilidade("Hipertermia Severa em Ave (> 43.0°C)", "-35 pts", "critico",
                    "Risco iminente de colapso cardiorrespiratório e edema pulmonar por estresse térmico severo."));
        } else if (tempVal >= 39.5 && tempVal <= 42.5) {
            fatores.add(new FatorExplicabilidade("Eutermia Aviária Cloacal (" + String.format(java.util.Locale.US, "%.1f", tempVal) + "°C)", "+0 pts", "positivo",
                    "Temperatura cloacal em perfeita consonância com o metabolismo acelerado característico de aves hígidas."));
        } else {
            escoreFisiologico -= 10;
        }

        // 2. Frequência Cardíaca Aviária (Normal: 150 a 400 bpm)
        int fc = (freqCardiaca != null) ? freqCardiaca : 250;
        if (fc < 120) {
            escoreFisiologico -= 25;
            fatores.add(new FatorExplicabilidade("Bradicardia Severa para Ave (< 120 bpm)", "-25 pts", "critico",
                    "Queda da frequência em aves reflete hipotermia profunda, depressão neurológica ou toxemia."));
        } else if (fc >= 150 && fc <= 400) {
            fatores.add(new FatorExplicabilidade("Ritmo Cardíaco Basal Aviário (" + fc + " bpm)", "+0 pts", "positivo",
                    "Frequência cardíaca rápida fisiológica mantida sem arritmias audíveis."));
        }

        // 3. Avaliação Ponderal Proporcional
        if (pesoKg < 0.05) {
            escoreFisiologico -= 10;
        }

        // Riscos Fenotípicos Gerais de Aves
        riscos.add(new RiscoFenotipico("Respiratório / Sacos Aéreos", "Prevenção Ativa", "Sensibilidade Aérea",
                "Aves possuem fluxo aéreo unidirecional de altíssima absorção. Proteger estritamente contra aerossóis domésticos, incensos e panelas de teflon (PTFE) aquecidas."));

        // Limites Estritos
        escoreFisiologico = Math.max(10, Math.min(100, escoreFisiologico));
        ClassificacaoRisco risco = (escoreFisiologico >= 80) ? ClassificacaoRisco.BAIXO
                : (escoreFisiologico >= 50 ? ClassificacaoRisco.MODERADO : ClassificacaoRisco.ALTO);

        String soap = String.format(
                "SOAP CLÍNICO AVIÁRIO (Clyvo Vet Avian Expert System - Fisiologia Comparada)\n" +
                "[S - Subjetivo]: Paciente aviário %s (%s, %d anos - expectativa: %d anos). Queixa: \"%s\".\n" +
                "[O - Objetivo]: Peso aferido: %.3f kg (%.0f g). Temp Cloacal: %.1f°C. FC: %d bpm.\n" +
                "[A - Avaliação Aviária]: Índice de Conformidade Fisiológica = %d/100 (Risco %s). XAI: %s.\n" +
                "[P - Plano Profilático]: Dieta com ração extrusada especializada, suplementação vitamínica em trocas de pena e enriquecimento com poleiros de diâmetros variados.",
                pet.getNome(), (pet.getRaca() != null ? pet.getRaca().getNome() : "Ave"), idadeAnos, expectativa,
                (queixaPrincipal != null ? queixaPrincipal : "Rotina profilática"),
                pesoKg, (pesoKg * 1000.0), tempVal, fc,
                escoreFisiologico, risco.name(),
                fatores.isEmpty() ? "Eutermia aviária confirmada" : fatores.get(0).fator() + " (" + fatores.get(0).impacto() + ")"
        );

        return new ResultadoDecisaoClinica(
                null, // Sem probabilidade pseudo-estatística
                escoreFisiologico,
                risco,
                fatores,
                riscos,
                soap,
                VERSAO_MOTOR,
                TipoMotorDecisao.SISTEMA_ESPECIALISTA_FISIOLOGICO
        );
    }
}
