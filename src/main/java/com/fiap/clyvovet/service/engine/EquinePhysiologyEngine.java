package com.fiap.clyvovet.service.engine;

import com.fiap.clyvovet.dto.FatorExplicabilidade;
import com.fiap.clyvovet.dto.ParametrosClinicosEntrada;
import com.fiap.clyvovet.dto.ResultadoDecisaoClinica;
import com.fiap.clyvovet.dto.RiscoFenotipico;
import com.fiap.clyvovet.model.AlimentacaoStatus;
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
 * Sistema Especialista em Medicina e Sanidade Equina (Grandes Animais - AAEP Guidelines).
 * Avaliação determinística por Conformidade Fisiológica Base 100.
 * Considera os parâmetros vitais e patologias críticas de equinos:
 * - Eutermia retal: 37.2°C a 38.3°C
 * - FC de repouso: 28 a 44 bpm (frequências > 50 bpm indicam dor visceral aguda / cólica)
 * - Monitoramento profilático de Síndrome Cólica, Laminite e Odontologia Hipsodonte.
 */
@Component
public class EquinePhysiologyEngine implements MotorDecisaoClinicaStrategy {

    public static final String VERSAO_MOTOR = "Equine-Physiology-Rules-v1.0";

    @Override
    public boolean suporta(String especie) {
        if (especie == null) return false;
        String e = especie.trim().toUpperCase();
        return e.contains("EQUIN") || e.contains("CAVALO") || e.contains("EGUA") || e.contains("ASININ");
    }

    @Override
    public ResultadoDecisaoClinica avaliar(Pet pet, ParametrosClinicosEntrada entrada) {
        BigDecimal pesoAferido = (entrada != null) ? entrada.pesoAferido() : null;
        BigDecimal temperatura = (entrada != null) ? entrada.temperaturaAferida() : null;
        Integer freqCardiopulmonar = (entrada != null) ? entrada.frequenciaMensurada() : null;
        List<CheckinDiario> checkins = (entrada != null) ? entrada.checkinsRecentes() : List.of();
        int totalConsultasHistorico = (entrada != null) ? entrada.totalConsultasHistorico() : 1;
        String queixaPrincipal = (entrada != null) ? entrada.queixaPrincipal() : "Rotina";

        int idadeAnos = (pet.getDataNascimento() != null)
                ? Period.between(pet.getDataNascimento(), LocalDate.now()).getYears() : 6;
        int expectativa = (pet.getRaca() != null && pet.getRaca().getExpectativaVida() != null)
                ? pet.getRaca().getExpectativaVida() : 28;
        double pesoKg = (pesoAferido != null)
                ? pesoAferido.doubleValue() : (pet.getPeso() != null ? pet.getPeso().doubleValue() : 450.0);
        double tempVal = (temperatura != null) ? temperatura.doubleValue() : 37.8;

        int escoreFisiologico = 100;
        List<FatorExplicabilidade> fatores = new ArrayList<>();
        List<RiscoFenotipico> riscos = new ArrayList<>();

        // 1. Termorregulação (Normal Equino: 37.2°C a 38.3°C)
        if (tempVal < 36.8) {
            escoreFisiologico -= 30;
            fatores.add(new FatorExplicabilidade("Hipotermia Equina (< 36.8°C)", "-30 pts", "critico",
                    "Indicativo grave de choque circulatório, endotoxemia por cólica estrangulada ou exaustão física."));
        } else if (tempVal > 38.5) {
            escoreFisiologico -= 25;
            fatores.add(new FatorExplicabilidade("Hipertermia Equina (> 38.5°C)", "-25 pts", "critico",
                    "Febre infecciosa (garrotilho, influenza equina) ou golpe de calor por esforço em pista."));
        } else {
            fatores.add(new FatorExplicabilidade("Eutermia Basal Equina (" + String.format(java.util.Locale.US, "%.1f", tempVal) + "°C)", "+0 pts", "positivo",
                    "Temperatura retal dentro da faixa fisiológica ideal de repouso."));
        }

        // 2. Frequência Cardíaca de Repouso (Normal: 28 a 44 bpm)
        if (freqCardiopulmonar != null) {
            if (freqCardiopulmonar < 24) {
                escoreFisiologico -= 25;
                fatores.add(new FatorExplicabilidade("Bradicardia Severa para Equino (< 24 bpm)", "-25 pts", "alerta",
                        "Sugere bloqueio de condução cardíaca ou hipotermia."));
            } else if (freqCardiopulmonar > 60) {
                escoreFisiologico -= 35;
                fatores.add(new FatorExplicabilidade("Taquicardia Severa em Equino (> 60 bpm)", "-35 pts", "critico",
                        "Em equinos, FC acima de 50-60 bpm é o principal indicador clínico de dor visceral intensa (Síndrome Cólica)."));
                riscos.add(new RiscoFenotipico("Gastrointestinal / Emergência", "Crítico", "Suspeita de Cólica Aguda",
                        "Taquicardia pronunciada associada a desconforto visceral requer sondagem nasogástrica e palpação retal imediatas."));
            } else if (freqCardiopulmonar > 44) {
                escoreFisiologico -= 15;
                fatores.add(new FatorExplicabilidade("Taquicardia Moderada (" + freqCardiopulmonar + " bpm)", "-15 pts", "alerta",
                        "Elevação discreta de FC; monitorar motilidade cecal (borborigmos) e grau de hidratação."));
            } else {
                fatores.add(new FatorExplicabilidade("FC de Repouso Fisiológica (" + freqCardiopulmonar + " bpm)", "+0 pts", "positivo",
                        "Ausculta cardíaca basal regular sem arritmias."));
            }
        }

        // 3. Apetite e Alimentação
        boolean apetiteReduzido = false;
        if (checkins != null && !checkins.isEmpty()) {
            for (CheckinDiario chk : checkins) {
                if (chk.getAlimentacaoStatus() == AlimentacaoStatus.POUCO_APETITE) apetiteReduzido = true;
            }
        }
        if (apetiteReduzido) {
            escoreFisiologico -= 20;
            fatores.add(new FatorExplicabilidade("Inapetência / Recusa Alimentar", "-20 pts", "alerta",
                    "Em equinos, inapetência súbita é sinal precoce de compactação intestinal ou dor bucal severa."));
        }

        // Riscos Fenotípicos Específicos de Grandes Animais
        riscos.add(new RiscoFenotipico("Gastrointestinal / Cólica", "Vigilância Diária", "Síndrome Cólica",
                "Manter oferta de água limpa irrestrita, pasto/volumoso de boa qualidade e evitar excesso de concentrado rico em amido."));
        riscos.add(new RiscoFenotipico("Locomotor / Podologia", "Manejo Preventivo", "Laminite e Ferrageamento",
                "Ferrageamento balanceado a cada 6-8 semanas. Prevenção de laminite por sobrecarga glicêmica ou concussão mecânica."));
        riscos.add(new RiscoFenotipico("Odontológico / Hipsodonte", "Revisão Anual", "Pontas de Esmalte",
                "Dentição de erupção contínua. Desgaste odontológico preventivo (grosagem de pontas) para evitar úlceras orais."));

        escoreFisiologico = Math.max(10, Math.min(100, escoreFisiologico));
        ClassificacaoRisco risco = (escoreFisiologico >= 80) ? ClassificacaoRisco.BAIXO
                : (escoreFisiologico >= 50 ? ClassificacaoRisco.MODERADO : ClassificacaoRisco.ALTO);

        String soap = String.format(
                "SOAP CLÍNICO EQUINOS (Clyvo Vet Equine Expert System)\n" +
                "[S - Subjetivo]: Paciente equino %s (%s, %d anos - expectativa: %d anos). Queixa: \"%s\".\n" +
                "[O - Objetivo]: Peso aferido: %.1f kg. Temp Retal: %.1f°C. FC Repouso: %s bpm.\n" +
                "[A - Avaliação Equina]: Índice de Conformidade Fisiológica = %d/100 (Risco %s). Fatores: %s.\n" +
                "[P - Plano Profilático]: Dieta volumosa (feno/pasto à vontade), casqueamento e ferrageamento regular, profilaxia dentária anual e vacinação contra tétano/encefalomielite.",
                pet.getNome(), (pet.getRaca() != null ? pet.getRaca().getNome() : "Cavalo"), idadeAnos, expectativa,
                (queixaPrincipal != null ? queixaPrincipal : "Rotina profilática"),
                pesoKg, tempVal, (freqCardiopulmonar != null ? freqCardiopulmonar.toString() : "Ausculta regular"),
                escoreFisiologico, risco.name(),
                fatores.isEmpty() ? "Sinais vitais de repouso conformes" : fatores.get(0).fator() + " (" + fatores.get(0).impacto() + ")"
        );

        return new ResultadoDecisaoClinica(
                null,
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
