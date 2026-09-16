package com.fiap.clyvovet.service;

import com.fiap.clyvovet.model.*;
import com.fiap.clyvovet.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("Testes do TriagemService com Arquitetura Dual-Engine (ML + Guardrails)")
class TriagemServiceTest {

    private ConsultaTriagemRepository triagemRepository;
    private PetRepository petRepository;
    private CheckinDiarioRepository checkinRepository;
    private HistoricoClinicoRepository historicoClinicoRepository;
    private UsuarioRepository usuarioRepository;
    private PetService petService;
    private PredictiveMlEngine mlEngine;
    private TriagemService triagemService;

    @BeforeEach
    void setUp() {
        triagemRepository = Mockito.mock(ConsultaTriagemRepository.class);
        petRepository = Mockito.mock(PetRepository.class);
        checkinRepository = Mockito.mock(CheckinDiarioRepository.class);
        historicoClinicoRepository = Mockito.mock(HistoricoClinicoRepository.class);
        usuarioRepository = Mockito.mock(UsuarioRepository.class);
        petService = Mockito.mock(PetService.class);
        mlEngine = new PredictiveMlEngine(); // Usa a instância real do motor de ML para validação fim-a-fim

        triagemService = new TriagemService(
                triagemRepository,
                petRepository,
                checkinRepository,
                historicoClinicoRepository,
                usuarioRepository,
                petService,
                mlEngine
        );
    }

    private Pet criarPetMock(String nome, int idadeAnos, double pesoKg) {
        Raca raca = new Raca(1L, "Labrador Retriever", "CANINA", "Displasia", 12, "Exercício moderado");
        return new Pet(10L, nome, LocalDate.now().minusYears(idadeAnos), BigDecimal.valueOf(pesoKg), "Ativo", 80, raca, null);
    }

    @Test
    @DisplayName("Cálculo de Escore Híbrido deve incorporar P(Higidez), XAI e Guardrails")
    void deveCalcularEscoreHibridoComMlEGuardrails() {
        Pet pet = criarPetMock("Marley", 3, 28.0);
        when(checkinRepository.findByPetIdOrderByDataCheckinDesc(10L)).thenReturn(Collections.emptyList());
        when(triagemRepository.findByPetIdOrderByDataSolicitacaoDesc(10L)).thenReturn(Collections.emptyList());

        TriagemService.ResultadoCalculoEscore resultado = triagemService.calcularEscoreLongevidadeEInsights(
                pet,
                new BigDecimal("28.5"),
                new BigDecimal("38.5"), // Eutérmico
                90, // Frequência normal
                "Consulta periódica preventiva"
        );

        assertNotNull(resultado);
        assertTrue(resultado.escore() >= 80, "Escore de pet jovem eutérmico deve ser >= 80");
        assertEquals(ClassificacaoRisco.BAIXO, resultado.risco());
        assertTrue(resultado.probabilidadeHigidez() > 70.0, "P(Higidez) deve ser estatisticamente calculada");
        assertTrue(resultado.insights().contains("ML Preditivo"), "Insights devem explicitar inferência de ML");
        assertTrue(resultado.insights().contains("XAI"), "Insights devem incluir explicabilidade algorítmica");
    }

    @Test
    @DisplayName("Fail-Safe: Hipertermia Crítica deve Acionar Sobrescrita de Risco Clínico")
    void deveAcionarFailSafeQuandoTemperaturaForFebreSevera() {
        Pet pet = criarPetMock("Toby", 2, 20.0);
        when(checkinRepository.findByPetIdOrderByDataCheckinDesc(10L)).thenReturn(Collections.emptyList());
        when(triagemRepository.findByPetIdOrderByDataSolicitacaoDesc(10L)).thenReturn(Collections.emptyList());

        // Temperatura em 39.8°C (Hipertermia severa / febre aguda)
        TriagemService.ResultadoCalculoEscore resultado = triagemService.calcularEscoreLongevidadeEInsights(
                pet,
                new BigDecimal("20.0"),
                new BigDecimal("39.8"),
                130,
                "Pet prostrado e muito quente"
        );

        assertNotNull(resultado);
        // Mesmo sendo um pet jovem onde o ML estatístico daria alta higidez,
        // o Guardrail Clínico de Emergência (AAHA/WSAVA) força o risco ALTO e penaliza o score.
        assertEquals(ClassificacaoRisco.ALTO, resultado.risco(), "Febre grave deve acionar Fail-Safe para risco ALTO");
        assertTrue(resultado.escore() <= 50, "Escore deve ser contido devido à emergência clínica");
        assertTrue(resultado.insights().contains("Hipertermia/Febre Vital"), "Insight deve registrar o alerta do guardrail");
    }

    @Test
    @DisplayName("Fisiologia Comparada: Réptil em temperatura ambiente de 26°C não deve sofrer penalidade de hipotermia mamífera")
    void deveAvaliarReptilSemPenalidadeDeHipotermiaMamifera() {
        Raca racaJabuti = new Raca(3L, "Jabuti-piranga", "REPTIL", "Osteometabólica", 60, "UVB",
                new BigDecimal("4.0"), new BigDecimal("12.0"));
        Pet jabuti = new Pet(20L, "Cascalho", LocalDate.now().minusYears(10), new BigDecimal("6.5"), "Ativo", 85, racaJabuti, null);

        when(checkinRepository.findByPetIdOrderByDataCheckinDesc(20L)).thenReturn(Collections.emptyList());
        when(triagemRepository.findByPetIdOrderByDataSolicitacaoDesc(20L)).thenReturn(Collections.emptyList());

        // Jabuti em recinto a 26.0°C (POTZ ideal) e 28 bpm (Doppler normal para réptil)
        TriagemService.ResultadoCalculoEscore resultado = triagemService.calcularEscoreLongevidadeEInsights(
                jabuti,
                new BigDecimal("6.5"),
                new BigDecimal("26.0"),
                28,
                "Consulta anual de rotina"
        );

        assertNotNull(resultado);
        assertFalse(resultado.insights().contains("Hipotermia Vital"),
                "Não deve disparar alerta de hipotermia mamífera para réptil em temperatura adequada de 26°C");
        assertFalse(resultado.insights().contains("Bradicardia Severa"),
                "Não deve disparar bradicardia canina para réptil com 28 bpm");
        assertEquals(ClassificacaoRisco.BAIXO, resultado.risco(),
                "Réptil saudável com recinto em 26°C deve ter risco BAIXO");
        assertTrue(resultado.escore() >= 80,
                "Escore de réptil com manejo térmico correto deve ser alto (>= 80)");
        assertEquals("Ectothermic-Wellness-v1.0", resultado.modeloVersao(),
                "Deve utilizar o motor de ML especializado para ectotérmicos");
    }

    @Test
    @DisplayName("Fisiologia Comparada: Peixe em água a 26°C sem ausculta cardíaca não deve ser penalizado por hipotermia nem bradicardia")
    void deveAvaliarPeixeComTemperaturaDeAguaSemPenalidadeMamifera() {
        Raca racaBetta = new Raca(5L, "Peixe Betta", "PEIXE", "Ictio", 4, "Água limpa",
                new BigDecimal("0.003"), new BigDecimal("0.005"));
        Pet betta = new Pet(30L, "Azulão", LocalDate.now().minusYears(1), new BigDecimal("0.004"), "Ativo", 90, racaBetta, null);

        when(checkinRepository.findByPetIdOrderByDataCheckinDesc(30L)).thenReturn(Collections.emptyList());
        when(triagemRepository.findByPetIdOrderByDataSolicitacaoDesc(30L)).thenReturn(Collections.emptyList());

        // Água do aquário a 26.0°C e sem ausculta cardíaca (FC nula / NA)
        TriagemService.ResultadoCalculoEscore resultado = triagemService.calcularEscoreLongevidadeEInsights(
                betta,
                new BigDecimal("0.004"),
                new BigDecimal("26.0"),
                null,
                "Acompanhamento de biótopo"
        );

        assertNotNull(resultado);
        assertFalse(resultado.insights().contains("Hipotermia"), "Não deve penalizar peixe tropical com água a 26°C");
        assertEquals(ClassificacaoRisco.BAIXO, resultado.risco());
        assertTrue(resultado.escore() >= 80);
        assertEquals("Aquatic-Wellness-v1.0", resultado.modeloVersao());
    }

    @Test
    @DisplayName("Fisiologia Comparada: Ave com temperatura cloacal de 41.5°C não deve ser rotulada com febre de mamífero")
    void deveAvaliarAveComTemperaturaCloacalNormalSemFalsoAlertaDeFebre() {
        Raca racaCalopsita = new Raca(8L, "Calopsita", "AVE", "Clamidiose", 15, "Gaiola ampla",
                new BigDecimal("0.08"), new BigDecimal("0.12"));
        Pet calopsita = new Pet(40L, "Piu", LocalDate.now().minusYears(2), new BigDecimal("0.095"), "Ativo", 88, racaCalopsita, null);

        when(checkinRepository.findByPetIdOrderByDataCheckinDesc(40L)).thenReturn(Collections.emptyList());
        when(triagemRepository.findByPetIdOrderByDataSolicitacaoDesc(40L)).thenReturn(Collections.emptyList());

        // Temperatura cloacal de 41.5°C (perfeita para aves) e FC 280 bpm (normal para calopsita)
        TriagemService.ResultadoCalculoEscore resultado = triagemService.calcularEscoreLongevidadeEInsights(
                calopsita,
                new BigDecimal("0.095"),
                new BigDecimal("41.5"),
                280,
                "Rotina"
        );

        assertNotNull(resultado);
        assertFalse(resultado.insights().contains("Hipertermia/Febre Vital"),
                "Temperatura cloacal de 41.5°C é fisiológica para aves e não deve receber alerta de febre");
        assertEquals(ClassificacaoRisco.BAIXO, resultado.risco());
        assertTrue(resultado.escore() >= 80);
        assertEquals("Avian-Wellness-v1.0", resultado.modeloVersao());
    }
}
