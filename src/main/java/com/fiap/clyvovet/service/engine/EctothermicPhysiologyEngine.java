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
 * Sistema Especialista em Fisiologia Ectotérmica / Répteis (Quelônios, Sáurios, Ofídios).
 * Avaliação determinística por Conformidade Fisiológica Base 100 (ABRAVAS / ARAV Guidelines).
 * Não utiliza pseudo-sigmoides nem forja probabilidades estocásticas.
 * Foca em termorregulação exógena (POTZ), Doppler cervicobraquial e prevenção de Osteodistrofia Fibrosa (MBD).
 */
@Component
public class EctothermicPhysiologyEngine implements MotorDecisaoClinicaStrategy {

    public static final String VERSAO_MOTOR = "Ectothermic-Physiology-Rules-v1.0";

    @Override
    public boolean suporta(String especie) {
        return "REPTIL".equalsIgnoreCase(especie) || "JABUTI".equalsIgnoreCase(especie)
                || "TARTARUGA".equalsIgnoreCase(especie) || "LAGARTO".equalsIgnoreCase(especie)
                || "SERPENTE".equalsIgnoreCase(especie) || "COBRA".equalsIgnoreCase(especie);
    }

    @Override
    public ResultadoDecisaoClinica avaliar(Pet pet, ParametrosClinicosEntrada entrada) {
        BigDecimal pesoAferido = (entrada != null) ? entrada.pesoAferido() : null;
        BigDecimal tempRecinto = (entrada != null) ? entrada.temperaturaAferida() : null;
        Integer freqDoppler = (entrada != null) ? entrada.frequenciaMensurada() : null;
        List<CheckinDiario> checkins = (entrada != null) ? entrada.checkinsRecentes() : List.of();
        int totalConsultasHistorico = (entrada != null) ? entrada.totalConsultasHistorico() : 1;
        String queixaPrincipal = (entrada != null) ? entrada.queixaPrincipal() : "Rotina";

        int expectativa = (pet.getRaca() != null && pet.getRaca().getExpectativaVida() != null)
                ? pet.getRaca().getExpectativaVida() : 50;
        int idadeAnos = (pet.getDataNascimento() != null)
                ? Period.between(pet.getDataNascimento(), LocalDate.now()).getYears() : 10;
        double pesoKg = (pesoAferido != null)
                ? pesoAferido.doubleValue() : (pet.getPeso() != null ? pet.getPeso().doubleValue() : 5.0);
        double tempVal = (tempRecinto != null) ? tempRecinto.doubleValue() : 27.0;

        int escoreFisiologico = 100;
        List<FatorExplicabilidade> fatores = new ArrayList<>();
        List<RiscoFenotipico> riscos = new ArrayList<>();

        // 1. Avaliação Térmica de Recinto (POTZ: 24°C a 32°C com basking de 34°C)
        if (tempVal < 20.0) {
            escoreFisiologico -= 35;
            fatores.add(new FatorExplicabilidade("Recinto Hipotérmico Crítico (< 20°C)", "-35 pts", "critico",
                    "Risco iminente de estase digestiva, bloqueio metabólico, imunossupressão severa e fermentação cecal estagnada."));
            riscos.add(new RiscoFenotipico("Manejo Térmico / POTZ", "Crítico", "Estase Fria",
                    "Temperatura de " + tempVal + "°C incompatível com a fisiologia de répteis. Acionar aquecimento de emergência."));
        } else if (tempVal > 35.0) {
            escoreFisiologico -= 35;
            fatores.add(new FatorExplicabilidade("Superaquecimento de Recinto (> 35°C)", "-35 pts", "critico",
                    "Excesso térmico sem gradiente de fuga causa desidratação aguda, hipertermia fatal e choque térmico."));
            riscos.add(new RiscoFenotipico("Manejo Térmico / POTZ", "Crítico", "Hipertermia Recinto",
                    "Temperatura de " + tempVal + "°C excede limite superior tolerado. Criar área de escape resfriada."));
        } else if (tempVal >= 24.0 && tempVal <= 32.5) {
            fatores.add(new FatorExplicabilidade("Faixa Térmica Ótima (POTZ " + String.format(java.util.Locale.US, "%.1f", tempVal) + "°C)", "+0 pts", "positivo",
                    "Temperatura do terrário em perfeita harmonia com a zona de preferência fisiológica de répteis hígidos."));
        } else {
            escoreFisiologico -= 10;
            fatores.add(new FatorExplicabilidade("Temperatura de Recinto Marginal (" + String.format(java.util.Locale.US, "%.1f", tempVal) + "°C)", "-10 pts", "alerta",
                    "Recinto próximo aos limites limiares; recomenda-se verificar lâmpada de cerâmica e termostato."));
        }

        // 2. Frequência Cardíaca por Doppler (Opcional - sem fonendoscópio torácico em quelônios)
        if (freqDoppler != null) {
            if (freqDoppler < 15) {
                escoreFisiologico -= 20;
                fatores.add(new FatorExplicabilidade("Bradicardia Severa ao Doppler (" + freqDoppler + " bpm)", "-20 pts", "alerta",
                        "Batimentos cardíacos em ritmo bradicárdico excessivo sugerem hipotermia profunda ou depressão metabólica."));
            } else if (freqDoppler > 85) {
                escoreFisiologico -= 20;
                fatores.add(new FatorExplicabilidade("Taquicardia ao Doppler (" + freqDoppler + " bpm)", "-20 pts", "alerta",
                        "Frequência cardíaca elevada sugere dor, estresse agudo de contenção ou sobrecarga térmica sistêmica."));
            } else {
                fatores.add(new FatorExplicabilidade("Ritmo Basal Ectotérmico Doppler (" + freqDoppler + " bpm)", "+0 pts", "positivo",
                        "Frequência cardíaca Doppler em perfeita sincronia com a temperatura ambiente do recinto."));
            }
        }

        // 3. Check-ins de Tutor (Inapetência / Recusa Alimentar)
        boolean apetiteReduzido = false;
        if (checkins != null && !checkins.isEmpty()) {
            for (CheckinDiario chk : checkins) {
                if (chk.getAlimentacaoStatus() == AlimentacaoStatus.POUCO_APETITE) {
                    apetiteReduzido = true;
                }
            }
        }
        if (apetiteReduzido) {
            escoreFisiologico -= 15;
            fatores.add(new FatorExplicabilidade("Inapetência / Hiporexia Prolongada", "-15 pts", "negativo",
                    "Recusa alimentar em répteis exige investigação imediata de parasitas gastrointestinais e suficiência de radiação UVB."));
        }

        // 4. Idade em Relação à Expectativa Zoológica
        double percVida = (double) idadeAnos / Math.max(1, expectativa);
        if (percVida > 0.80) {
            escoreFisiologico -= 10;
            fatores.add(new FatorExplicabilidade("Réptil Geriátrico (" + idadeAnos + " de " + expectativa + " anos)", "-10 pts", "alerta",
                    "Avanço senil requer avaliação semestral de carapaça, rigidez articular e função renal."));
        }

        // Riscos Fenotípicos Gerais de Répteis
        riscos.add(new RiscoFenotipico("Nutricional & Esquelético (MBD)", "Monitoramento Ativo", "Exigência UVB",
                "Predisposição à Doença Osteometabólica (MBD). Garantir fonte de radiação UVB (5.0/10.0) e suplemento de Cálcio sem fósforo + D3."));

        // Limites Estritos de Pontuação
        escoreFisiologico = Math.max(10, Math.min(100, escoreFisiologico));
        ClassificacaoRisco risco = (escoreFisiologico >= 80) ? ClassificacaoRisco.BAIXO
                : (escoreFisiologico >= 50 ? ClassificacaoRisco.MODERADO : ClassificacaoRisco.ALTO);

        String soap = String.format(
                "SOAP CLÍNICO ECTOTÉRMICO (Clyvo Vet Ectothermic Expert System - Fisiologia Comparada)\n" +
                "[S - Subjetivo]: Paciente ectotérmico %s (%s, %d anos - expectativa: %d anos). Queixa: \"%s\".\n" +
                "[O - Objetivo]: Peso aferido: %.2f kg. Temp Recinto (POTZ): %.1f°C. FC Doppler: %s.\n" +
                "[A - Avaliação Ectotérmica]: Índice de Conformidade Fisiológica = %d/100 (Risco %s). XAI: %s.\n" +
                "[P - Plano Profilático]: Manter zona térmica ótima (POTZ 24-32°C com basking spot a 34°C), iluminação UVB ativa e reposição de cálcio com D3.",
                pet.getNome(), (pet.getRaca() != null ? pet.getRaca().getNome() : "Réptil"), idadeAnos, expectativa,
                (queixaPrincipal != null ? queixaPrincipal : "Rotina preventiva"),
                pesoKg, tempVal, (freqDoppler != null ? freqDoppler + " bpm" : "Não aplicável / Doppler opcional"),
                escoreFisiologico, risco.name(),
                fatores.isEmpty() ? "Parâmetros de recinto estáveis" : fatores.get(0).fator() + " (" + fatores.get(0).impacto() + ")"
        );

        return new ResultadoDecisaoClinica(
                null, // Sem probabilidade pseudo-estatística (Sistema Especialista Determinístico)
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
