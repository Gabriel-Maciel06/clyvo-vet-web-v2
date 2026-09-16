package com.fiap.clyvovet.service;

import com.fiap.clyvovet.dto.ResultadoDecisaoClinica;
import com.fiap.clyvovet.model.*;
import com.fiap.clyvovet.service.engine.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do Motor Orquestrador de Decisão Clínica (PredictiveMlEngine com Padrão Strategy)")
class PredictiveMlEngineTest {

    private PredictiveMlEngine mlEngine;

    @BeforeEach
    void setUp() {
        DefaultPhysiologyEngine fallback = new DefaultPhysiologyEngine();
        List<MotorDecisaoClinicaStrategy> strategies = List.of(
                new CaninePredictiveMlEngine(),
                new FelinePhysiologyEngine(),
                new AvianPhysiologyEngine(),
                new EctothermicPhysiologyEngine(),
                new AquaticPhysiologyEngine(),
                new SmallMammalPhysiologyEngine(),
                new MustelidPhysiologyEngine(),
                new InvertebratePhysiologyEngine(),
                new EquinePhysiologyEngine(),
                fallback
        );
        mlEngine = new PredictiveMlEngine(strategies, fallback);
    }

    private Pet criarPetMock(String nome, String racaNome, String especie, int idadeAnos, double pesoKg, String propensao) {
        Raca raca = new Raca(1L, racaNome, especie, propensao, 13, "Cuidados preventivos gerais");
        return new Pet(1L, nome, LocalDate.now().minusYears(idadeAnos), BigDecimal.valueOf(pesoKg), "Ativo", 85, raca, null);
    }

    @Test
    @DisplayName("Cão Jovem e Ativo deve receber Alta Probabilidade de Higidez e Baixo Risco via ML Supervisionado")
    void devePredizerAltaHigidezParaCaoJovemEAtivo() {
        Pet thor = criarPetMock("Thor", "Golden Retriever", "CANINA", 3, 30.0, "Displasia Coxofemoral");

        List<CheckinDiario> checkins = new ArrayList<>();
        checkins.add(new CheckinDiario(1L, thor, LocalDate.now(), AlimentacaoStatus.RECOMENDADA, true, 45, HumorPet.ENERGICO, null, 10, false));
        checkins.add(new CheckinDiario(2L, thor, LocalDate.now().minusDays(1), AlimentacaoStatus.RECOMENDADA, true, 50, HumorPet.ENERGICO, null, 10, false));

        ResultadoDecisaoClinica resultado = mlEngine.executarInferencia(
                thor,
                new BigDecimal("30.0"),
                new BigDecimal("38.4"),
                95,
                checkins,
                3,
                "Check-up preventivo semestral"
        );

        assertNotNull(resultado);
        assertNotNull(resultado.probabilidadeHigidez(), "Para caninos sob ML, probabilidade deve ser preenchida");
        assertTrue(resultado.probabilidadeHigidez() >= 70.0, "Probabilidade de higidez deve ser alta (>70%)");
        assertTrue(resultado.escoreLongevidade() >= 80, "Escore de longevidade deve ser >= 80");
        assertEquals(ClassificacaoRisco.BAIXO, resultado.classificacaoRisco());
        assertEquals(CaninePredictiveMlEngine.VERSAO_MOTOR, resultado.versaoModelo());
        assertEquals(TipoMotorDecisao.MACHINE_LEARNING_SUPERVISIONADO, resultado.tipoMotor(),
                "Para caninos, o motor deve ser Machine Learning Supervisionado");

        // Validação da Explicabilidade (XAI)
        assertFalse(resultado.fatoresXai().isEmpty(), "Deve conter fatores de explicabilidade algorítmica");
        assertTrue(resultado.fatoresXai().stream().anyMatch(f -> f.fator().contains("Fase Adulta Jovem")),
                "XAI deve identificar fase adulta jovem como fator positivo");
        assertTrue(resultado.resumoFormatadoXai().contains("P(Higidez)="), "Resumo XAI deve conter probabilidade e fatores");
        assertTrue(resultado.sinteseSoap().contains("SOAP CLÍNICO PREDITIVO"), "Deve gerar síntese SOAP estruturada");
    }

    @Test
    @DisplayName("Cão Sênior com Tremores deve ter Risco Elevado e Fatores Negativos")
    void devePredizerRiscoElevadoParaCaoSeniorComSintomas() {
        Pet bob = criarPetMock("Bob", "Beagle", "CANINA", 11, 16.0, "Obesidade");

        List<CheckinDiario> checkins = new ArrayList<>();
        checkins.add(new CheckinDiario(1L, bob, LocalDate.now(), AlimentacaoStatus.POUCO_APETITE, false, 10, HumorPet.APATICO, "Episódio de tremor muscular", 10, true));

        ResultadoDecisaoClinica resultado = mlEngine.executarInferencia(
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
    @DisplayName("Felino (Gato) deve ser avaliado por Sistema Especialista ISFM/AAFP com Probabilidade Nula e Base 100")
    void deveAvaliarFelinoCorretamenteSemPontoCego() {
        Pet mimi = criarPetMock("Mimi", "Siamês", "FELINA", 4, 4.0, "Doença Renal Crônica");

        ResultadoDecisaoClinica resultado = mlEngine.executarInferencia(
                mimi,
                new BigDecimal("4.0"),
                new BigDecimal("38.6"), // Temperatura normal felina (38.0 a 39.2°C)
                170,                   // FC normal felina (140 a 220 bpm)
                List.of(),
                2,
                "Avaliação de rotina e saúde renal"
        );

        assertNotNull(resultado);
        assertNull(resultado.probabilidadeHigidez(), "Para Sistema Especialista Felino, probabilidade estocástica deve ser rigorosamente nula");
        assertEquals(TipoMotorDecisao.SISTEMA_ESPECIALISTA_FISIOLOGICO, resultado.tipoMotor());
        assertEquals(FelinePhysiologyEngine.VERSAO_MOTOR, resultado.versaoModelo());
        assertEquals(ClassificacaoRisco.BAIXO, resultado.classificacaoRisco());
        assertTrue(resultado.escoreLongevidade() >= 80, "Gato eutérmico e saudável deve ter escore alto na base 100");
        assertTrue(resultado.riscosEspecificos().stream().anyMatch(r -> r.categoria().contains("Renal")),
                "Deve monitorar predisposição à Doença Renal Crônica (DRC)");
        assertTrue(resultado.sinteseSoap().contains("SOAP CLÍNICO FELINO"));
    }

    @Test
    @DisplayName("Motor Ectotérmico para Répteis deve operar em Base 100 com Probabilidade Nula")
    void deveExecutarInferenciaEctotermicaParaReptil() {
        Pet jabuti = criarPetMock("Matusalém", "Jabuti-piranga", "REPTIL", 12, 7.0, "Osteometabólica / UVB");

        ResultadoDecisaoClinica resultado = mlEngine.executarInferencia(
                jabuti,
                new BigDecimal("7.0"),
                new BigDecimal("27.5"), // Temperatura de Recinto / POTZ
                32,                     // Frequência cardíaca Doppler
                List.of(),
                2,
                "Check-up do terrário e casca"
        );

        assertNotNull(resultado);
        assertNull(resultado.probabilidadeHigidez(), "Répteis não usam pseudo-sigmoide; probabilidade deve ser nula");
        assertEquals(EctothermicPhysiologyEngine.VERSAO_MOTOR, resultado.versaoModelo());
        assertEquals(TipoMotorDecisao.SISTEMA_ESPECIALISTA_FISIOLOGICO, resultado.tipoMotor());
        assertEquals(ClassificacaoRisco.BAIXO, resultado.classificacaoRisco());
        assertTrue(resultado.escoreLongevidade() >= 80);
        assertTrue(resultado.fatoresXai().stream().anyMatch(f -> f.fator().contains("POTZ")),
                "XAI deve conter fator de adequação de POTZ");
        assertTrue(resultado.sinteseSoap().contains("SOAP CLÍNICO ECTOTÉRMICO"));
    }

    @Test
    @DisplayName("Mustelídeo (Furão) deve ser avaliado por Sistema Especialista com Alerta de Insulinoma")
    void deveAvaliarMustelideoComRegrasEspecializadas() {
        Pet furao = criarPetMock("Bandit", "Furão (Ferret)", "MUSTELIDEO", 3, 1.2, "Insulinoma");

        ResultadoDecisaoClinica resultado = mlEngine.executarInferencia(
                furao,
                new BigDecimal("1.2"),
                new BigDecimal("38.8"), // Temp normal (37.8 a 40.0°C)
                210,                   // FC normal (180 a 250 bpm)
                List.of(),
                1,
                "Rotina semestral"
        );

        assertNotNull(resultado);
        assertNull(resultado.probabilidadeHigidez());
        assertEquals(MustelidPhysiologyEngine.VERSAO_MOTOR, resultado.versaoModelo());
        assertEquals(TipoMotorDecisao.SISTEMA_ESPECIALISTA_FISIOLOGICO, resultado.tipoMotor());
        assertTrue(resultado.riscosEspecificos().stream().anyMatch(r -> r.categoria().contains("Insulinoma") || r.detalhes().contains("insulinoma")),
                "Deve alertar propensão a insulinoma e fotoperíodo em furões");
    }

    @Test
    @DisplayName("Invertebrado (Tarântula) não deve exigir ausculta cardíaca e deve monitorar Ecdise")
    void deveAvaliarInvertebradoComSucesso() {
        Pet aranha = criarPetMock("Viúva", "Tarântula (Caranguejeira)", "ARACNIDEO", 2, 0.03, "Complicações de muda");

        ResultadoDecisaoClinica resultado = mlEngine.executarInferencia(
                aranha,
                new BigDecimal("0.03"),
                new BigDecimal("25.0"), // Temp terrário
                null,                   // Sem ausculta torácica
                List.of(),
                1,
                "Rotina de terrário"
        );

        assertNotNull(resultado);
        assertNull(resultado.probabilidadeHigidez());
        assertEquals(InvertebratePhysiologyEngine.VERSAO_MOTOR, resultado.versaoModelo());
        assertEquals(TipoMotorDecisao.SISTEMA_ESPECIALISTA_FISIOLOGICO, resultado.tipoMotor());
        assertEquals(ClassificacaoRisco.BAIXO, resultado.classificacaoRisco());
        assertTrue(resultado.riscosEspecificos().stream().anyMatch(r -> r.categoria().contains("Ecdise")));
    }

    @Test
    @DisplayName("Equino deve ser avaliado com FC de Repouso Baixa (28-44 bpm) e Alerta de Cólica")
    void deveAvaliarEquinoComRegrasDeGrandesAnimais() {
        Pet cavalo = criarPetMock("Trovão", "Cavalo (Hipismo / Trabalho)", "EQUINA", 7, 480.0, "Síndrome Cólica");

        ResultadoDecisaoClinica resultado = mlEngine.executarInferencia(
                cavalo,
                new BigDecimal("480.0"),
                new BigDecimal("37.8"), // Temp retal normal
                36,                     // FC repouso normal (28 a 44 bpm)
                List.of(),
                2,
                "Avaliação pré-prova"
        );

        assertNotNull(resultado);
        assertNull(resultado.probabilidadeHigidez());
        assertEquals(EquinePhysiologyEngine.VERSAO_MOTOR, resultado.versaoModelo());
        assertEquals(TipoMotorDecisao.SISTEMA_ESPECIALISTA_FISIOLOGICO, resultado.tipoMotor());
        assertEquals(ClassificacaoRisco.BAIXO, resultado.classificacaoRisco());
        assertTrue(resultado.riscosEspecificos().stream().anyMatch(r -> r.categoria().contains("Cólica")));
    }

    @Test
    @DisplayName("Espécie Não-Mapeada deve acionar Fallback Universal sem Lançar Exceção")
    void deveAcionarFallbackUniversalParaEspecieDesconhecida() {
        Pet marsupial = criarPetMock("Jubileu", "Quokka", "MARSUPIAL_EXOTICO", 3, 4.0, "Silvestre");

        ResultadoDecisaoClinica resultado = mlEngine.executarInferencia(
                marsupial,
                new BigDecimal("4.0"),
                new BigDecimal("38.0"),
                80,
                List.of(),
                1,
                "Avaliação de resgate"
        );

        assertNotNull(resultado, "Fallback jamais deve devolver nulo");
        assertNull(resultado.probabilidadeHigidez(), "Fallback determinístico opera em base 100");
        assertEquals(DefaultPhysiologyEngine.VERSAO_MOTOR, resultado.versaoModelo());
        assertEquals(TipoMotorDecisao.SISTEMA_ESPECIALISTA_FISIOLOGICO, resultado.tipoMotor());
        assertTrue(resultado.fatoresXai().stream().anyMatch(f -> f.fator().contains("Fallback")),
                "XAI deve auditar o acionamento do fallback");
    }
}
