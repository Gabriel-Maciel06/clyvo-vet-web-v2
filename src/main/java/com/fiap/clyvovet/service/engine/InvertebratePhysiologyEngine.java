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
 * Sistema Especialista em Invertebrados e Aracnídeos Exóticos (Tarântulas / Artrópodes).
 * Avaliação determinística por Conformidade Fisiológica Base 100 (Invertebrate Medicine Guidelines - Lewbart).
 * Reconhece a biologia de exoesqueleto de quitina e circulação aberta de hemolinfa:
 * - Ausculta torácica não aplicável.
 * - Monitoramento térmico de terrário (22.0°C a 28.0°C).
 * - Prevenção de disecdise (muda incompleta), desidratação e trauma de opistossoma.
 */
@Component
public class InvertebratePhysiologyEngine implements MotorDecisaoClinicaStrategy {

    public static final String VERSAO_MOTOR = "Invertebrate-Physiology-Rules-v1.0";

    @Override
    public boolean suporta(String especie) {
        if (especie == null) return false;
        String e = especie.trim().toUpperCase();
        return e.contains("ARACNIDEO") || e.contains("ARACNÍDEO") || e.contains("TARANTULA")
                || e.contains("TARÂNTULA") || e.contains("ARANHA") || e.contains("ESCORPIAO")
                || e.contains("INVERTEBRADO");
    }

    @Override
    public ResultadoDecisaoClinica avaliar(Pet pet, ParametrosClinicosEntrada entrada) {
        BigDecimal pesoAferido = (entrada != null) ? entrada.pesoAferido() : null;
        BigDecimal tempTerrario = (entrada != null) ? entrada.temperaturaAferida() : null;
        List<CheckinDiario> checkins = (entrada != null) ? entrada.checkinsRecentes() : List.of();
        int totalConsultasHistorico = (entrada != null) ? entrada.totalConsultasHistorico() : 1;
        String queixaPrincipal = (entrada != null) ? entrada.queixaPrincipal() : "Rotina";

        int idadeAnos = (pet.getDataNascimento() != null)
                ? Period.between(pet.getDataNascimento(), LocalDate.now()).getYears() : 2;
        int expectativa = (pet.getRaca() != null && pet.getRaca().getExpectativaVida() != null)
                ? pet.getRaca().getExpectativaVida() : 15;
        double pesoKg = (pesoAferido != null)
                ? pesoAferido.doubleValue() : (pet.getPeso() != null ? pet.getPeso().doubleValue() : 0.03);
        double tempVal = (tempTerrario != null) ? tempTerrario.doubleValue() : 25.0;

        int escoreFisiologico = 100;
        List<FatorExplicabilidade> fatores = new ArrayList<>();
        List<RiscoFenotipico> riscos = new ArrayList<>();

        // 1. Temperatura do Terrário (Faixa segura: 22.0°C a 28.0°C)
        if (tempVal < 20.0) {
            escoreFisiologico -= 35;
            fatores.add(new FatorExplicabilidade("Terrário Frio para Aracnídeo (< 20.0°C)", "-35 pts", "critico",
                    "Temperatura baixa deprime o metabolismo, impede a síntese enzimática digestiva e bloqueia o ciclo de ecdise."));
            riscos.add(new RiscoFenotipico("Termorregulação / Manejo", "Alerta Térmico", "Hipotermia Ambiental",
                    "Placa térmica com termostato obrigatória para elevar o recinto à faixa de 24°C a 26°C."));
        } else if (tempVal > 30.0) {
            escoreFisiologico -= 30;
            fatores.add(new FatorExplicabilidade("Superaquecimento do Recinto (> 30.0°C)", "-30 pts", "critico",
                    "Risco iminente de desidratação aguda fatal por evaporação da hemolinfa."));
        } else {
            fatores.add(new FatorExplicabilidade("Gradiente Térmico Adequado (" + String.format(java.util.Locale.US, "%.1f", tempVal) + "°C)", "+0 pts", "positivo",
                    "Microambiente térmico dentro da zona ótima para manutenção metabólica e muda de exoesqueleto."));
        }

        // Riscos Fenotípicos de Invertebrados
        riscos.add(new RiscoFenotipico("Tegumento / Ecdise", "Acompanhamento de Ciclo", "Troca de Exoesqueleto",
                "Manter umidade do substrato (fibra de coco ou esfagno) para evitar retenção de muda (disecdise) e perda de apêndices locomotores."));
        riscos.add(new RiscoFenotipico("Trauma / Arquitetura do Recinto", "Prevenção Mecânica", "Risco de Queda",
                "Terrário horizontal com altura livre de queda inferior a 1,5 vezes a envergadura do paciente para proteger o opistossoma."));

        escoreFisiologico = Math.max(10, Math.min(100, escoreFisiologico));
        ClassificacaoRisco risco = (escoreFisiologico >= 80) ? ClassificacaoRisco.BAIXO
                : (escoreFisiologico >= 50 ? ClassificacaoRisco.MODERADO : ClassificacaoRisco.ALTO);

        String soap = String.format(
                "SOAP CLÍNICO INVERTEBRADOS (Clyvo Vet Invertebrate Expert System)\n" +
                "[S - Subjetivo]: Paciente %s (%s, %d anos - expectativa: %d anos). Queixa: \"%s\".\n" +
                "[O - Objetivo]: Peso aferido: %.3f kg (%.0f g). Temp Terrário: %.1f°C. Ausculta cardíaca torácica: Inaplicável (Circulação de hemolinfa).\n" +
                "[A - Avaliação Invertebrado]: Índice de Conformidade Fisiológica = %d/100 (Risco %s). Fatores: %s.\n" +
                "[P - Plano Profilático]: Umidade do substrato ajustada, bebedouro raso com água limpa, terrário horizontal anti-queda e presas vivas adequadas.",
                pet.getNome(), (pet.getRaca() != null ? pet.getRaca().getNome() : "Aracnídeo Exótico"), idadeAnos, expectativa,
                (queixaPrincipal != null ? queixaPrincipal : "Rotina profilática"),
                pesoKg, pesoKg * 1000.0, tempVal,
                escoreFisiologico, risco.name(),
                fatores.isEmpty() ? "Manejo e recinto conformes" : fatores.get(0).fator() + " (" + fatores.get(0).impacto() + ")"
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
