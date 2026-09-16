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
}
