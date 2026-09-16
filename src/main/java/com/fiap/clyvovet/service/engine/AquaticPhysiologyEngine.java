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
import java.util.ArrayList;
import java.util.List;

/**
 * Sistema Especialista em Medicina e Manejo de Peixes Ornamentais (Teleósteos / Biótopo Aquático).
 * Avaliação determinística por Conformidade Fisiológica Base 100.
 * Focado na estabilidade térmica da água, respiração opercular e integridade da bexiga natatória.
 */
@Component
public class AquaticPhysiologyEngine implements MotorDecisaoClinicaStrategy {

    public static final String VERSAO_MOTOR = "Aquatic-Physiology-Rules-v1.0";

    @Override
    public boolean suporta(String especie) {
        return "PEIXE".equalsIgnoreCase(especie) || "PEIXES".equalsIgnoreCase(especie)
                || "AQUATICO".equalsIgnoreCase(especie) || "BETTA".equalsIgnoreCase(especie)
                || "KINGUIO".equalsIgnoreCase(especie);
    }

    @Override
    public ResultadoDecisaoClinica avaliar(Pet pet, ParametrosClinicosEntrada entrada) {
        BigDecimal pesoAferido = (entrada != null) ? entrada.pesoAferido() : null;
        BigDecimal tempAgua = (entrada != null) ? entrada.temperaturaAferida() : null;
        Integer freqOpercular = (entrada != null) ? entrada.frequenciaMensurada() : null;
        List<CheckinDiario> checkins = (entrada != null) ? entrada.checkinsRecentes() : List.of();
        int totalConsultasHistorico = (entrada != null) ? entrada.totalConsultasHistorico() : 1;
        String queixaPrincipal = (entrada != null) ? entrada.queixaPrincipal() : "Rotina";

        String racaNome = (pet.getRaca() != null) ? pet.getRaca().getNome().toLowerCase() : "";
        double pesoKg = (pesoAferido != null)
                ? pesoAferido.doubleValue() : (pet.getPeso() != null ? pet.getPeso().doubleValue() : 0.05);
        double tempVal = (tempAgua != null) ? tempAgua.doubleValue() : 25.0;

        int escoreFisiologico = 100;
        List<FatorExplicabilidade> fatores = new ArrayList<>();
        List<RiscoFenotipico> riscos = new ArrayList<>();

        // 1. Adequação da Temperatura da Água ao Biótopo da Espécie
        boolean isAguaFria = racaNome.contains("kinguio") || racaNome.contains("goldfish") || racaNome.contains("fria");
        if (isAguaFria) {
            if (tempVal >= 18.0 && tempVal <= 22.5) {
                fatores.add(new FatorExplicabilidade("Temperatura Ideal de Água Fria (" + String.format(java.util.Locale.US, "%.1f", tempVal) + "°C)", "+0 pts", "positivo",
                        "Água temperada ideal para Kinguios, preservando alta taxa de oxigênio dissolvido e equilíbrio osmótico."));
            } else if (tempVal > 25.0) {
                escoreFisiologico -= 25;
                fatores.add(new FatorExplicabilidade("Água Excessivamente Quente para Kinguio (> 25°C)", "-25 pts", "negativo",
                        "Água aquecida reduz oxigênio dissolvido e acelera metabolismo do Kinguio, propiciando estresse e poluição orgânica."));
                riscos.add(new RiscoFenotipico("Qualidade de Água", "Atenção", "Temperatura Elevada",
                        "Temperatura de " + tempVal + "°C incompatível com espécies de água fria. Aumentar aeração e refrigerar."));
            } else {
                escoreFisiologico -= 10;
            }
        } else {
            // Espécies tropicais (Betta, Tetras, Ciclídeos)
            if (tempVal >= 24.0 && tempVal <= 28.5) {
                fatores.add(new FatorExplicabilidade("Temperatura Tropical Ideal (" + String.format(java.util.Locale.US, "%.1f", tempVal) + "°C)", "+0 pts", "positivo",
                        "Termostato e água estabilizados na faixa biológica ótima de peixes tropicais e labirintídeos."));
            } else if (tempVal < 22.0) {
                escoreFisiologico -= 30;
                fatores.add(new FatorExplicabilidade("Água Fria para Peixe Tropical (< 22°C)", "-30 pts", "critico",
                        "Água fria causa paralisia digestiva, inativação do sistema imune e surtos de Ictiofitiríase (íctio)."));
                riscos.add(new RiscoFenotipico("Manejo de Aquário", "Crítico", "Hipotermia Aquática",
                        "Água a " + tempVal + "°C predispõe a ictio e choque osmótico. Instalar termostato com aquecedor imediatamente."));
            } else if (tempVal > 30.0) {
                escoreFisiologico -= 30;
                fatores.add(new FatorExplicabilidade("Superaquecimento Aquático (> 30°C)", "-30 pts", "critico",
                        "Temperatura crítica com queda drástica de oxigênio dissolvido, forçando respiração superficial na lâmina d'água."));
            } else {
                escoreFisiologico -= 10;
            }
        }

        // 2. Frequência Opercular (Movimentos branquiais por minuto - ausculta cardíaca inaplicável)
        if (freqOpercular != null) {
            if (freqOpercular >= 35 && freqOpercular <= 85) {
                fatores.add(new FatorExplicabilidade("Frequência Opercular Regular (" + freqOpercular + " mov/min)", "+0 pts", "positivo",
                        "Movimentos branquiais rítmicos sem sinais de ofegação ou hipóxia aquática."));
            } else if (freqOpercular > 100) {
                escoreFisiologico -= 25;
                fatores.add(new FatorExplicabilidade("Hiperventilação Opercular Severa (" + freqOpercular + " mov/min)", "-25 pts", "alerta",
                        "Batimentos branquiais acelerados sugerem pico tóxico de amônia/nitrito ou déficit agudo de aeração."));
                riscos.add(new RiscoFenotipico("Qualidade / Respiratório", "Alerta Crítico", "Hipóxia Aquática",
                        "Hiperventilação branquial requer teste imediato de Amônia Tóxica e Nitrito no aquário."));
            }
        }

        // 3. Sintomas e Natação
        boolean disturbeNatatorio = false;
        boolean apetiteReduzido = false;
        if (checkins != null && !checkins.isEmpty()) {
            for (CheckinDiario chk : checkins) {
                if (chk.getAlimentacaoStatus() == AlimentacaoStatus.POUCO_APETITE) {
                    apetiteReduzido = true;
                }
                if (chk.getSintomasObservados() != null) {
                    String s = chk.getSintomasObservados().toLowerCase();
                    if (s.contains("boia") || s.contains("fundo") || s.contains("lado") || s.contains("nadadeira") || s.contains("bexiga")) {
                        disturbeNatatorio = true;
                    }
                }
            }
        }

        if (disturbeNatatorio) {
            escoreFisiologico -= 30;
            fatores.add(new FatorExplicabilidade("Distúrbio Natatório / Bexiga Natatória", "-30 pts", "critico",
                    "Alteração de flutuabilidade observada. Requer jejum terapêutico, dieta descompressiva e ajuste da qualidade da água."));
            riscos.add(new RiscoFenotipico("Gastrointestinal / Flutuabilidade", "Atenção", "Bexiga Natatória",
                    "Evitar alimentos secos que fermentam no trato digestivo. Oferecer artêmia e ervilha descascada."));
        }

        if (apetiteReduzido) {
            escoreFisiologico -= 15;
            fatores.add(new FatorExplicabilidade("Inapetência Aquática", "-15 pts", "negativo",
                    "Recusa de ração em grânulos ou flocos; verificar acúmulo de matéria orgânica no substrato."));
        }

        // Riscos Fenotípicos Gerais de Peixes
        riscos.add(new RiscoFenotipico("Ciclo do Nitrogênio & Biótopo", "Manutenção Periódica", "TPA Semanal",
                "Peixes excretam amônia diretamente pelas brânquias. Realizar trocas parciais de água (TPA 20-30%% semanal) com desclorificante."));

        // Limites Estritos
        escoreFisiologico = Math.max(10, Math.min(100, escoreFisiologico));
        ClassificacaoRisco risco = (escoreFisiologico >= 80) ? ClassificacaoRisco.BAIXO
                : (escoreFisiologico >= 50 ? ClassificacaoRisco.MODERADO : ClassificacaoRisco.ALTO);

        String soap = String.format(
                "SOAP CLÍNICO AQUÁTICO (Clyvo Vet Aquatic Expert System - Fisiologia Comparada)\n" +
                "[S - Subjetivo]: Paciente pecilotérmico aquático %s (%s). Queixa: \"%s\".\n" +
                "[O - Objetivo]: Peso aproximado: %.3f kg. Temp da Água: %.1f°C. Freq. Opercular: %s.\n" +
                "[A - Avaliação Aquática]: Índice de Conformidade de Biótopo = %d/100 (Risco %s). XAI: %s.\n" +
                "[P - Plano Profilático]: Manter trocas parciais de água (TPA 20%% semanais com condicionador de cloro), teste de pH/Amônia e aeração biológica.",
                pet.getNome(), (pet.getRaca() != null ? pet.getRaca().getNome() : "Peixe"),
                (queixaPrincipal != null ? queixaPrincipal : "Rotina de biótopo"),
                pesoKg, tempVal, (freqOpercular != null ? freqOpercular + " mov/min" : "Regular / Não aferido"),
                escoreFisiologico, risco.name(),
                fatores.isEmpty() ? "Parâmetros do biótopo aquático regulares" : fatores.get(0).fator() + " (" + fatores.get(0).impacto() + ")"
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
