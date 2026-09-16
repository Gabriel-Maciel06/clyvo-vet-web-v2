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
 * Sistema Especialista em Fisiologia e Medicina Felina (Consensos AAFP / ISFM).
 * Avaliação determinística por Conformidade Fisiológica Base 100.
 * Elimina o "ponto cego dos felinos", calculando escore com base em parâmetros fisiológicos estritos:
 * - Peso típico (3.5 - 5.5 kg)
 * - Eutermia central felina (38.0°C a 39.2°C)
 * - Frequência cardíaca basal (140 a 220 bpm)
 * - Prevenção de Lipidose Hepática, FLUTD e Doença Renal Crônica (DRC).
 */
@Component
public class FelinePhysiologyEngine implements MotorDecisaoClinicaStrategy {

    public static final String VERSAO_MOTOR = "Feline-Physiology-Rules-v1.0";

    @Override
    public boolean suporta(String especie) {
        return "FELINA".equalsIgnoreCase(especie) || "GATO".equalsIgnoreCase(especie) || "FELINO".equalsIgnoreCase(especie);
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
                ? Period.between(pet.getDataNascimento(), LocalDate.now()).getYears() : 3;
        double pesoKg = (pesoAferido != null)
                ? pesoAferido.doubleValue() : (pet.getPeso() != null ? pet.getPeso().doubleValue() : 4.2);
        double tempVal = (temperatura != null) ? temperatura.doubleValue() : 38.5;

        int escoreFisiologico = 100;
        List<FatorExplicabilidade> fatores = new ArrayList<>();
        List<RiscoFenotipico> riscos = new ArrayList<>();

        // 1. Termorregulação Felina (Normal: 38.0°C a 39.2°C)
        if (tempVal < 37.5) {
            escoreFisiologico -= 30;
            fatores.add(new FatorExplicabilidade("Hipotermia Felina Crítica (< 37.5°C)", "-30 pts", "critico",
                    "Gatos em choque séptico, uremico ou dor severa apresentam hipotermia paradoxal precoce. Aquecimento assistido urgente."));
        } else if (tempVal > 39.5) {
            escoreFisiologico -= 25;
            fatores.add(new FatorExplicabilidade("Hipertermia / Febre Felina (> 39.5°C)", "-25 pts", "negativo",
                    "Elevação térmica sinaliza processo infeccioso sistêmico, PIF, virose ou bacteremia."));
        } else {
            fatores.add(new FatorExplicabilidade("Eutermia Felina Estável (" + String.format(java.util.Locale.US, "%.1f", tempVal) + "°C)", "+0 pts", "positivo",
                    "Temperatura corpórea central em perfeita faixa homeostática felina (38.0°C a 39.2°C)."));
        }

        // 2. Frequência Cardíaca Felina (Normal: 140 a 220 bpm)
        if (freqCardiopulmonar != null) {
            if (freqCardiopulmonar < 140) {
                escoreFisiologico -= 30;
                fatores.add(new FatorExplicabilidade("Bradicardia Severa em Felino (" + freqCardiopulmonar + " bpm)", "-30 pts", "critico",
                    "Em felinos, FC < 140 bpm indica hipotermia severa, hipercalemia por obstrução uretral ou colapso circulatório."));
            } else if (freqCardiopulmonar > 225) {
                escoreFisiologico -= 15;
                fatores.add(new FatorExplicabilidade("Taquicardia Fisiológica / Estresse (" + freqCardiopulmonar + " bpm)", "-15 pts", "alerta",
                    "FC elevada associada a estresse em consultório, dor aguda ou cardiopatia oculta (CMH)."));
            } else {
                fatores.add(new FatorExplicabilidade("Frequência Cardíaca Basal Felina (" + freqCardiopulmonar + " bpm)", "+0 pts", "positivo",
                    "Ritmo cardíaco em consonância com as diretrizes AAFP para felinos em repouso."));
            }
        }

        // 3. Avaliação Ponderal (Porte Felino Médio)
        if (pesoKg < 2.8) {
            escoreFisiologico -= 15;
            fatores.add(new FatorExplicabilidade("Baixo Escore Corporal Felino (ECC < 3/9)", "-15 pts", "negativo",
                    "Peso abaixo de 2.8 kg sugere perda de massa muscular, caquexia renal ou má-absorção gastrointestinal."));
        } else if (pesoKg > 6.8) {
            escoreFisiologico -= 15;
            fatores.add(new FatorExplicabilidade("Sobrepeso / Obesidade Felina (ECC > 7/9)", "-15 pts", "negativo",
                    "Obesidade felina é o principal fator de risco para Diabetes Mellitus tipo 2 e sobrecarga articular."));
        }

        // 4. Check-ins de Tutor e Comportamento
        boolean hiporexiaObservada = false;
        boolean alteracaoUrinaria = false;
        if (checkins != null && !checkins.isEmpty()) {
            for (CheckinDiario chk : checkins) {
                if (chk.getAlimentacaoStatus() == AlimentacaoStatus.POUCO_APETITE) {
                    hiporexiaObservada = true;
                }
                if (chk.getSintomasObservados() != null) {
                    String s = chk.getSintomasObservados().toLowerCase();
                    if (s.contains("xixi") || s.contains("areia") || s.contains("urina") || s.contains("dor")) {
                        alteracaoUrinaria = true;
                    }
                }
            }
        }

        if (hiporexiaObservada) {
            escoreFisiologico -= 20;
            fatores.add(new FatorExplicabilidade("Hiporexia / Anorexia Observada", "-20 pts", "critico",
                    "Em felinos, jejum superior a 48-72h induz mobilização lipídica excessiva para o fígado com risco de Lipidose Hepática fatal."));
        }

        if (alteracaoUrinaria) {
            escoreFisiologico -= 25;
            fatores.add(new FatorExplicabilidade("Alerta de Trato Urinário Inferior (FLUTD)", "-25 pts", "critico",
                    "Alteração de micção observada na caixa de areia. Risco de obstrução uretral aguda por tampão mucoso em machos."));
            riscos.add(new RiscoFenotipico("Nefrologia / FLUTD", "Atenção Imediata", "Sedimento Urinário",
                    "Investigação imediata de hematúria, cristalúria de estruvita e desobstrução preventiva."));
        }

        // 5. Profilaxia Renal e Faixa Etária
        if (pet.getRaca() != null && pet.getRaca().getPropensaoDoenca() != null &&
                (pet.getRaca().getPropensaoDoenca().toUpperCase().contains("RENAL") || pet.getRaca().getPropensaoDoenca().toUpperCase().contains("DRC"))) {
            riscos.add(new RiscoFenotipico("Nefrologia / Renal", "Vigilância Preventiva", "Predisposição DRC",
                    "Predisposição racial a Doença Renal Crônica (DRC). Acompanhamento de SDMA e densidade urinária anual recomendado."));
        } else if (idadeAnos >= 8) {
            escoreFisiologico -= 10;
            fatores.add(new FatorExplicabilidade("Felino Sênior / Geriátrico (" + idadeAnos + " anos)", "-10 pts", "alerta",
                    "Avanço etário exige triagem semestral de creatinina, SDMA, urinálise e aferição de pressão arterial sistêmica (PA)."));
            riscos.add(new RiscoFenotipico("Renal / Geriátrico", "Prevenção Ativa", "SDMA Semestral",
                    "Doença Renal Crônica (DRC) afeta >30% dos gatos sêniores. Estimular ingestão hídrica úmida e fontes ativas."));
        }

        // Riscos Fenotípicos Gerais de Felinos
        riscos.add(new RiscoFenotipico("Hidratação & Manejo Ambiental", "Rotina Ativa", "Consumo Hídrico",
                "Felinos evoluíram no deserto com baixa sensação de sede espontânea. Oferecer ração úmida sachê diária e fontes de água corrente."));

        // Limites Estritos
        escoreFisiologico = Math.max(10, Math.min(100, escoreFisiologico));
        ClassificacaoRisco risco = (escoreFisiologico >= 80) ? ClassificacaoRisco.BAIXO
                : (escoreFisiologico >= 50 ? ClassificacaoRisco.MODERADO : ClassificacaoRisco.ALTO);

        String soap = String.format(
                "SOAP CLÍNICO FELINO (Clyvo Vet Feline Expert System - AAFP/ISFM Guidelines)\n" +
                "[S - Subjetivo]: Paciente felino %s (%s, %d anos). Queixa: \"%s\".\n" +
                "[O - Objetivo]: Peso aferido: %.2f kg. Temp Corpórea: %.1f°C. FC: %s bpm.\n" +
                "[A - Avaliação Felina]: Índice de Conformidade Fisiológica = %d/100 (Risco %s). XAI: %s.\n" +
                "[P - Plano Profilático]: Manter enriquecimento ambiental vertical, caixas de areia limpas (regra N+1), estímulo hídrico e monitoramento de FLUTD.",
                pet.getNome(), (pet.getRaca() != null ? pet.getRaca().getNome() : "Felino"), idadeAnos,
                (queixaPrincipal != null ? queixaPrincipal : "Rotina preventiva"),
                pesoKg, tempVal, (freqCardiopulmonar != null ? freqCardiopulmonar.toString() : "Ausculta basal"),
                escoreFisiologico, risco.name(),
                fatores.isEmpty() ? "Parâmetros basais conformes" : fatores.get(0).fator() + " (" + fatores.get(0).impacto() + ")"
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
