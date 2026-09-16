package com.fiap.clyvovet.service;

import com.fiap.clyvovet.dto.PetDto;
import com.fiap.clyvovet.model.Raca;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Validação Biológica e Prevenção de Dados Impossíveis (PetService)")
class PetBiometriaValidacaoTest {

    private PetService petService;
    private Raca racaCanina;
    private Raca racaRoedor;
    private Raca racaReptil;

    @BeforeEach
    void setUp() {
        // PetService puro para teste unitário do método de validação biológica
        petService = new PetService(null, null, null, null, null, null, null);

        racaCanina = new Raca(1L, "Golden Retriever", "CANINA", "Displasia", 12, "Exercício",
                new BigDecimal("27.0"), new BigDecimal("36.0"));

        racaRoedor = new Raca(2L, "Rato Twister", "ROEDOR", "Respiratório", 3, "Substrato sem pó",
                new BigDecimal("0.25"), new BigDecimal("0.55"));

        racaReptil = new Raca(3L, "Jabuti-piranga", "REPTIL", "Osteometabólica", 60, "UVB",
                new BigDecimal("5.0"), new BigDecimal("12.0"));
    }

    @Test
    @DisplayName("Deve rejeitar data de nascimento que resulte em um cão com 50 anos de idade")
    void deveRejeitarCaoCom50AnosDeIdade() {
        PetDto dto = new PetDto();
        dto.setNome("Thor Imortal");
        dto.setRacaId(racaCanina.getId());
        dto.setDataNascimento(LocalDate.now().minusYears(50)); // 50 anos!
        dto.setPeso(new BigDecimal("32.0"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                petService.validarLimitesBiologicos(dto, racaCanina)
        );

        assertTrue(ex.getMessage().contains("Idade biologicamente incompatível"),
                "A mensagem deve informar incompatibilidade biológica de idade");
        assertTrue(ex.getMessage().contains("50 anos"),
                "A mensagem deve citar os 50 anos informados");
        assertTrue(ex.getMessage().contains("30 anos"),
                "A mensagem deve citar o limite máximo biológico de 30 anos para caninos");
    }

    @Test
    @DisplayName("Deve rejeitar data de nascimento no futuro")
    void deveRejeitarDataNascimentoNoFuturo() {
        PetDto dto = new PetDto();
        dto.setNome("Pet do Futuro");
        dto.setRacaId(racaCanina.getId());
        dto.setDataNascimento(LocalDate.now().plusDays(5));
        dto.setPeso(new BigDecimal("15.0"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                petService.validarLimitesBiologicos(dto, racaCanina)
        );

        assertTrue(ex.getMessage().contains("futuro"),
                "A mensagem deve informar que data de nascimento não pode estar no futuro");
    }

    @Test
    @DisplayName("Deve rejeitar peso biologicamente impossível para cão (ex: 500 kg)")
    void deveRejeitarPesoImpossivelParaCao() {
        PetDto dto = new PetDto();
        dto.setNome("Hulk Canino");
        dto.setRacaId(racaCanina.getId());
        dto.setDataNascimento(LocalDate.now().minusYears(3));
        dto.setPeso(new BigDecimal("500.0")); // 500 kg para um cão!

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                petService.validarLimitesBiologicos(dto, racaCanina)
        );

        assertTrue(ex.getMessage().contains("Peso biologicamente impossível"),
                "A mensagem deve alertar peso biologicamente impossível");
        assertTrue(ex.getMessage().contains("160"),
                "A mensagem deve indicar o limite fisiológico máximo tolerado");
    }

    @Test
    @DisplayName("Deve rejeitar peso incompatível para roedor (ex: 20 kg para um rato)")
    void deveRejeitarPesoIncompativelParaRoedor() {
        PetDto dto = new PetDto();
        dto.setNome("Splinter Gigante");
        dto.setRacaId(racaRoedor.getId());
        dto.setDataNascimento(LocalDate.now().minusYears(1));
        dto.setPeso(new BigDecimal("20.0")); // 20 kg para um rato!

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                petService.validarLimitesBiologicos(dto, racaRoedor)
        );

        assertTrue(ex.getMessage().contains("Peso biologicamente impossível"),
                "A mensagem deve alertar peso excessivo para roedor");
    }

    @Test
    @DisplayName("Deve aceitar réptil com idade avançada plausível (ex: Jabuti com 45 anos)")
    void deveAceitarReptilComIdadeCompativel() {
        PetDto dto = new PetDto();
        dto.setNome("Matusalém");
        dto.setRacaId(racaReptil.getId());
        dto.setDataNascimento(LocalDate.now().minusYears(45)); // 45 anos é viável para jabuti
        dto.setPeso(new BigDecimal("8.5"));

        assertDoesNotThrow(() -> petService.validarLimitesBiologicos(dto, racaReptil),
                "Jabuti de 45 anos com 8.5 kg deve ser aceito sem exceção");
    }

    @Test
    @DisplayName("Deve aceitar cão saudável com parâmetros normais de idade e peso")
    void deveAceitarCaoSaudavelComParametrosNormais() {
        PetDto dto = new PetDto();
        dto.setNome("Thor");
        dto.setRacaId(racaCanina.getId());
        dto.setDataNascimento(LocalDate.now().minusYears(4));
        dto.setPeso(new BigDecimal("32.5"));

        assertDoesNotThrow(() -> petService.validarLimitesBiologicos(dto, racaCanina),
                "Cão Golden de 4 anos e 32.5 kg deve ser aceito sem exceção");
    }

    @Test
    @DisplayName("Deve formatar peso de Calopsita em gramas e kg (80 g a 120 g (0.08 a 0.12 kg))")
    void deveFormatarPesoCalopsitaEmGramasEKg() {
        Raca calopsita = new Raca(4L, "Calopsita", "AVE", "Clamidiose", 15, "Gaiola ampla",
                new BigDecimal("0.08"), new BigDecimal("0.12"));

        assertEquals("80 g a 120 g (0.08 a 0.12 kg)", calopsita.getPesoMedioFormatado());
        assertEquals(25, calopsita.getLimiteMaximoIdadeAnos());
        assertEquals(new BigDecimal("0.10"), calopsita.getPesoMedioSugerido());
    }

    @Test
    @DisplayName("Deve rejeitar peso biologicamente impossível para Calopsita (ex: 5 kg)")
    void deveRejeitarPesoImpossivelParaCalopsita() {
        Raca calopsita = new Raca(4L, "Calopsita", "AVE", "Clamidiose", 15, "Gaiola ampla",
                new BigDecimal("0.08"), new BigDecimal("0.12"));

        PetDto dto = new PetDto();
        dto.setNome("Piu-Piu");
        dto.setRacaId(calopsita.getId());
        dto.setDataNascimento(LocalDate.now().minusYears(2));
        dto.setPeso(new BigDecimal("5.0")); // 5 kg para uma calopsita!

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                petService.validarLimitesBiologicos(dto, calopsita)
        );

        assertTrue(ex.getMessage().contains("Peso biologicamente impossível"));
    }

    @Test
    @DisplayName("Deve rejeitar calopsita com idade superior ao teto biológico (ex: 40 anos)")
    void deveRejeitarCalopsitaComIdadeSuperiorAoTeto() {
        Raca calopsita = new Raca(4L, "Calopsita", "AVE", "Clamidiose", 15, "Gaiola ampla",
                new BigDecimal("0.08"), new BigDecimal("0.12"));

        PetDto dto = new PetDto();
        dto.setNome("Lili");
        dto.setRacaId(calopsita.getId());
        dto.setDataNascimento(LocalDate.now().minusYears(40)); // 40 anos para calopsita!
        dto.setPeso(new BigDecimal("0.10"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                petService.validarLimitesBiologicos(dto, calopsita)
        );

        assertTrue(ex.getMessage().contains("Idade biologicamente incompatível"));
        assertTrue(ex.getMessage().contains("25 anos"));
    }

    @Test
    @DisplayName("Deve aceitar calopsita saudável com peso de 90g (0.09 kg) e 3 anos")
    void deveAceitarCalopsitaSaudavel() {
        Raca calopsita = new Raca(4L, "Calopsita", "AVE", "Clamidiose", 15, "Gaiola ampla",
                new BigDecimal("0.08"), new BigDecimal("0.12"));

        PetDto dto = new PetDto();
        dto.setNome("Lili");
        dto.setRacaId(calopsita.getId());
        dto.setDataNascimento(LocalDate.now().minusYears(3));
        dto.setPeso(new BigDecimal("0.09")); // 90 gramas

        assertDoesNotThrow(() -> petService.validarLimitesBiologicos(dto, calopsita));
    }
}
