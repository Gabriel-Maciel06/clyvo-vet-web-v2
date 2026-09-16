package com.fiap.clyvovet.service.engine;

import com.fiap.clyvovet.dto.FatorExplicabilidade;
import com.fiap.clyvovet.dto.ParametrosClinicosEntrada;
import com.fiap.clyvovet.dto.ResultadoDecisaoClinica;
import com.fiap.clyvovet.dto.RiscoFenotipico;
import com.fiap.clyvovet.model.CheckinDiario;
import com.fiap.clyvovet.model.ClassificacaoRisco;
import com.fiap.clyvovet.model.Pet;
import com.fiap.clyvovet.model.TipoMotorDecisao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Estratégia de Fallback Universal e Resiliência Operacional (Padrão Strategy).
 * Garante que nenhuma espécie exótica, atípica ou não categorizada resulte em
 * NoSuchElementException, NullPointerException ou HTTP 500 no sistema.
 * Aplica avaliação determinística de sanidade biológica geral Base 100.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DefaultPhysiologyEngine implements MotorDecisaoClinicaStrategy {

    private static final Logger log = LoggerFactory.getLogger(DefaultPhysiologyEngine.class);
    public static final String VERSAO_MOTOR = "Generic-Physiology-Rules-v1.0";

    @Override
    public boolean suporta(String especie) {
        // Atua como fallback genérico para qualquer espécie não mapeada
        return true;
    }

    @Override
    public ResultadoDecisaoClinica avaliar(Pet pet, ParametrosClinicosEntrada entrada) {
        String especieNome = (pet != null && pet.getRaca() != null && pet.getRaca().getEspecie() != null)
                ? pet.getRaca().getEspecie() : "Não Especificada";
        log.warn("Acionando Fallback Universal ({}) para espécie: {}", VERSAO_MOTOR, especieNome);

        BigDecimal pesoAferido = (entrada != null) ? entrada.pesoAferido() : null;
        BigDecimal temperatura = (entrada != null) ? entrada.temperaturaAferida() : null;
        Integer freqCardiopulmonar = (entrada != null) ? entrada.frequenciaMensurada() : null;
        List<CheckinDiario> checkins = (entrada != null) ? entrada.checkinsRecentes() : List.of();
        int totalConsultasHistorico = (entrada != null) ? entrada.totalConsultasHistorico() : 1;
        String queixaPrincipal = (entrada != null) ? entrada.queixaPrincipal() : "Rotina";

        int idadeAnos = (pet != null && pet.getDataNascimento() != null)
                ? Period.between(pet.getDataNascimento(), LocalDate.now()).getYears() : 3;
        int expectativa = (pet != null && pet.getRaca() != null && pet.getRaca().getExpectativaVida() != null)
                ? pet.getRaca().getExpectativaVida() : 12;
        double pesoKg = (pesoAferido != null)
                ? pesoAferido.doubleValue() : (pet != null && pet.getPeso() != null ? pet.getPeso().doubleValue() : 5.0);

        int escoreFisiologico = 90; // Escore basal de segurança
        List<FatorExplicabilidade> fatores = new ArrayList<>();
        List<RiscoFenotipico> riscos = new ArrayList<>();

        fatores.add(new FatorExplicabilidade(
                "Fallback Fisiológico Geral Ativado (" + especieNome + ")",
                "+0 pts", "alerta",
                "Espécie sem motor supervisionado dedicado. Aplicadas diretrizes de higidez biométrica e segurança comparada."
        ));

        // Avaliação de Sinais Térmicos Genéricos
        if (temperatura != null) {
            double t = temperatura.doubleValue();
            if (t < 35.0 || t > 41.5) {
                escoreFisiologico -= 30;
                fatores.add(new FatorExplicabilidade("Temperatura em Faixa Crítica (" + t + "°C)", "-30 pts", "critico",
                        "Desvio térmico acentuado para a maioria dos vertebrados endotérmicos e ectotérmicos."));
            } else {
                fatores.add(new FatorExplicabilidade("Temperatura Compatível (" + t + "°C)", "+0 pts", "positivo",
                        "Temperatura aferida em faixa de estabilidade fisiológica."));
            }
        }

        // Avaliação de Idade vs Expectativa
        double percVida = (double) idadeAnos / Math.max(1, expectativa);
        if (percVida > 0.75) {
            escoreFisiologico -= 10;
            fatores.add(new FatorExplicabilidade("Fase Sênior (" + idadeAnos + " de " + expectativa + " anos)", "-10 pts", "alerta",
                    "Paciente em estágio geriátrico; recomenda-se triagem laboratorial ampla."));
        }

        // Histórico de Check-ins
        if (!checkins.isEmpty()) {
            fatores.add(new FatorExplicabilidade("Acompanhamento Tutor Ativo (" + checkins.size() + " check-ins)", "+5 pts", "positivo",
                    "Engajamento contínuo do tutor no monitoramento de sinais vitais."));
            escoreFisiologico += 5;
        }

        riscos.add(new RiscoFenotipico("Taxonomia / Protocolo Geral", "Auditoria", "Diretrizes Gerais",
                "Recomenda-se exame clínico detalhado e avaliação por médico veterinário especialista em animais selvagens/exóticos."));

        escoreFisiologico = Math.max(10, Math.min(100, escoreFisiologico));
        ClassificacaoRisco risco = (escoreFisiologico >= 80) ? ClassificacaoRisco.BAIXO
                : (escoreFisiologico >= 50 ? ClassificacaoRisco.MODERADO : ClassificacaoRisco.ALTO);

        String soap = String.format(
                "SOAP CLÍNICO COMPARADO (Clyvo Vet Universal Fallback Expert System)\n" +
                "[S - Subjetivo]: Paciente %s (Espécie: %s, %d anos). Queixa: \"%s\".\n" +
                "[O - Objetivo]: Peso aferido: %.2f kg. Temp: %s. Frequência: %s.\n" +
                "[A - Avaliação Fisiológica Comparada]: Índice de Conformidade = %d/100 (Risco %s). XAI: %s.\n" +
                "[P - Plano Profilático]: Exame físico minucioso e adaptação do protocolo às particularidades etológicas da espécie.",
                (pet != null ? pet.getNome() : "Paciente"), especieNome, idadeAnos,
                (queixaPrincipal != null ? queixaPrincipal : "Rotina profilática"),
                pesoKg,
                (temperatura != null ? temperatura + "°C" : "Não informada"),
                (freqCardiopulmonar != null ? freqCardiopulmonar + " mensurada" : "Não informada"),
                escoreFisiologico, risco.name(),
                fatores.get(0).fator()
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
