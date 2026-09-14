package com.fiap.clyvovet.service;

import com.fiap.clyvovet.dto.CheckinDto;
import com.fiap.clyvovet.model.AlimentacaoStatus;
import com.fiap.clyvovet.model.CheckinDiario;
import com.fiap.clyvovet.model.HumorPet;
import com.fiap.clyvovet.model.RecompensaTutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes do FLUXO 1 (check-in diário + gamificação) sobre os dados iniciais do Flyway (V3):
 * tutor 'tutor' (CPF 123.456.789-00) com streak 5, 230 pts (PRATA), último check-in ontem; pets Thor (1) e Luna (2).
 */
@SpringBootTest
@Transactional
class CheckinServiceTest {

    private static final String TUTOR_CPF = "123.456.789-00";

    @Autowired
    private CheckinService checkinService;

    private CheckinDto checkinSaudavel(Long petId, int minutos, boolean remedio) {
        CheckinDto dto = new CheckinDto();
        dto.setPetId(petId);
        dto.setAlimentacaoStatus(AlimentacaoStatus.RECOMENDADA);
        dto.setHumorPet(HumorPet.ENERGICO);
        dto.setMinutosAtividade(minutos);
        dto.setRemedioAdministrado(remedio);
        return dto;
    }

    @Test
    @DisplayName("Check-in saudável com 45 min e remédio: 20 pontos, streak 5 -> 6 e sobe de PRATA para OURO")
    void checkinPontuaEIncrementaStreak() {
        CheckinDiario salvo = checkinService.registrarCheckin(checkinSaudavel(1L, 45, true), "tutor");

        assertEquals(20, salvo.getPontosGanhos());
        assertFalse(salvo.getAlertaGerado());

        RecompensaTutor recompensa = checkinService.obterOuCriarRecompensa(TUTOR_CPF);
        assertEquals(6, recompensa.getStreakDias());
        assertEquals(250, recompensa.getPontosAcumulados()); // 230 + 20 -> sobe de PRATA para OURO
        assertEquals("OURO", recompensa.getNivelFidelidade());
        assertEquals(15, recompensa.getDescontoPercentual());
    }

    @Test
    @DisplayName("Sintomas relatados geram alerta clínico para a central médica")
    void checkinComSintomasGeraAlerta() {
        CheckinDto dto = checkinSaudavel(2L, 10, false);
        dto.setHumorPet(HumorPet.APATICO);
        dto.setSintomasObservados("Respiração ofegante e recusa de ração");

        CheckinDiario salvo = checkinService.registrarCheckin(dto, "tutor");

        assertTrue(salvo.getAlertaGerado());
        assertTrue(checkinService.listarAlertasAtivos().stream().anyMatch(c -> c.getId().equals(salvo.getId())));
    }

    @Test
    @DisplayName("Não permite dois check-ins do mesmo pet no mesmo dia")
    void naoPermiteCheckinDuplicadoNoDia() {
        checkinService.registrarCheckin(checkinSaudavel(1L, 30, false), "tutor");
        assertThrows(IllegalStateException.class,
                () -> checkinService.registrarCheckin(checkinSaudavel(1L, 30, false), "tutor"));
    }

    @Test
    @DisplayName("Usuário sem vínculo com o pet não consegue registrar check-in")
    void usuarioSemPropriedadeNaoRegistra() {
        assertThrows(AccessDeniedException.class,
                () -> checkinService.registrarCheckin(checkinSaudavel(1L, 30, false), "admin"));
    }
}
