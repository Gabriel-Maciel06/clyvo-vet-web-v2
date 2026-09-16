package com.fiap.clyvovet.service;

import com.fiap.clyvovet.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do Motor de Machine Learning Preditivo e XAI (PredictiveMlEngine)")
class PredictiveMlEngineTest {

    private PredictiveMlEngine mlEngine;

    @BeforeEach
    void setUp() {
        mlEngine = new PredictiveMlEngine();
    }

    private Pet criarPetMock(String nome, String racaNome, String especie, int idadeAnos, double pesoKg, String propensao) {
        Raca raca = new Raca(1L, racaNome, especie, propensao, 13, "Cuidados preventivos gerais");
        return new Pet(1L, nome, LocalDate.now().minusYears(idadeAnos), BigDecimal.valueOf(pesoKg), "Ativo", 85, raca, null);
    }

    @Test
    @DisplayName("Cão Jovem e Ativo deve receber Alta Probabilidade de Higidez e Baixo Risco")
    void devePredizerAltaHigidezParaCaoJovemEAtivo() {
        Pet thor = criarPetMock("Thor", "Golden Retriever", "CANINA", 3, 30.0, "Displasia Coxofemoral");

        List<CheckinDiario> checkins = new ArrayList<>();
        checkins.add(new CheckinDiario(1L, thor, LocalDate.now(), AlimentacaoStatus.RECOMENDADA, true, 45, HumorPet.ENERGICO, null, 10, false));
        checkins.add(new CheckinDiario(2L, thor, LocalDate.now().minusDays(1), AlimentacaoStatus.RECOMENDADA, true, 50, HumorPet.ENERGICO, null, 10, false));

        PredictiveMlEngine.ResultadoInferenciaMl resultado = mlEngine.executarInferencia(
                thor,
                new BigDecimal("30.0"),
                new BigDecimal("38.4"),
                95,
                checkins,
                3,
                "Check-up preventivo semestral"
        );

        assertNotNull(resultado);
        assertTrue(resultado.probabilidadeHigidez() >= 70.0, "Probabilidade de higidez deve ser alta (>70%)");
        assertTrue(resultado.escoreLongevidade() >= 80, "Escore de longevidade deve ser >= 80");
        assertEquals(ClassificacaoRisco.BAIXO, resultado.classificacaoRisco());
        assertEquals("CanineWellness-ML-v1.0", resultado.versaoModelo());

        // Validação da Explicabilidade (XAI)
        assertFalse(resultado.fatoresXai().isEmpty(), "Deve conter fatores de explicabilidade algorítmica");
        assertTrue(resultado.fatoresXai().stream().anyMatch(f -> f.fator().contains("Fase Adulta Jovem")),
                "XAI deve identificar fase adulta jovem como fator positivo");
        assertTrue(resultado.resumoFormatadoXai().contains("P(Higidez)="), "Resumo XAI deve conter probabilidade e fatores");
        assertTrue(resultado.sinteseSoap().contains("SOAP CLÍNICO PREDITIVO"), "Deve gerar síntese SOAP estruturada");
    }

    @Test
    @DisplayName("Cão Sênior Sedentário com Convulsões deve ter Risco Elevado e Fatores XAI Negativos")
    void devePredizerRiscoElevadoParaCaoSeniorComSintomas() {
        Pet bob = criarPetMock("Bob", "Beagle", "CANINA", 11, 16.0, "Obesidade");

        List<CheckinDiario> checkins = new ArrayList<>();
        checkins.add(new CheckinDiario(1L, bob, LocalDate.now(), AlimentacaoStatus.POUCO_APETITE, false, 10, HumorPet.APATICO, "Episódio de tremor muscular", 10, true));

        PredictiveMlEngine.ResultadoInferenciaMl resultado = mlEngine.executarInferencia(
                bob,
                new BigDecimal("16.5"),
                new BigDecimal("38.9"),
                110,
                checkins,
                1,
                "Tremores e desânimo"
        );

        assertNotNull(resultado);
        assertTrue(resultado.escoreLongevidade() < 75, "Escore deve refletir declínio etário e sinais clínicos");
        assertTrue(resultado.fatoresXai().stream().anyMatch(f -> f.tipo().equals("negativo")),
                "Deve registrar fatores negativos de envelhecimento e tremores");
        assertTrue(resultado.riscosEspecificos().stream().anyMatch(r -> r.categoria().contains("Neurológico")),
                "Deve alertar risco neurológico devido a histórico de tremor");
    }

    @Test
    @DisplayName("Braquicefálico com Temperatura Elevada deve Disparar Alerta Crítico de Estresse Térmico")
    void deveDispararAlertaTermicoParaBraquicefalico() {
        Pet luna = criarPetMock("Luna", "Bulldog Francês", "CANINA", 4, 12.0, "Problemas respiratórios");

        PredictiveMlEngine.ResultadoInferenciaMl resultado = mlEngine.executarInferencia(
                luna,
                new BigDecimal("12.0"),
                new BigDecimal("39.2"), // Temperatura elevada
                125,
                List.of(),
                2,
                "Ofegante após passeio no calor"
        );

        assertTrue(resultado.riscosEspecificos().stream().anyMatch(r ->
                r.categoria().contains("Estresse Térmico") && r.status().equals("Crítico")),
                "Deve gerar alerta de risco térmico crítico para raça braquicefálica com hipertermia");
    }

    @Test
    @DisplayName("Porte Grande sem Condroprotetor deve Disparar Risco Articular/Displasia")
    void deveDispararRiscoArticularParaGrandePorteSemMedicamento() {
        Pet max = criarPetMock("Max", "Pastor Alemão", "CANINA", 5, 36.0, "Displasia Coxofemoral");

        // Sem check-ins ou medicamentos
        PredictiveMlEngine.ResultadoInferenciaMl resultado = mlEngine.executarInferencia(
                max,
                new BigDecimal("36.0"),
                new BigDecimal("38.5"),
                90,
                List.of(),
                1,
                "Rotina"
        );

        assertTrue(resultado.riscosEspecificos().stream().anyMatch(r ->
                r.categoria().contains("Ortopédico") && r.badge().contains("Risco Displasia")),
                "Deve sinalizar risco de displasia para cão de grande porte sem medicação contínua");
    }

    @Test
    @DisplayName("Escore de Longevidade deve Respeitar Limiares de Segurança [10, 99]")
    void deveLimitarEscoreEntre10E99() {
        Pet petExtremo = criarPetMock("Extremo", "Chihuahua", "CANINA", 16, 2.5, "Cardiopatia");

        PredictiveMlEngine.ResultadoInferenciaMl resultado = mlEngine.executarInferencia(
                petExtremo,
                new BigDecimal("2.5"),
                new BigDecimal("37.2"),
                50,
                List.of(),
                0,
                "Emergência"
        );

        assertTrue(resultado.escoreLongevidade() >= 10, "Escore não pode ser inferior a 10");
        assertTrue(resultado.escoreLongevidade() <= 99, "Escore não pode ser superior a 99");
    }
}
