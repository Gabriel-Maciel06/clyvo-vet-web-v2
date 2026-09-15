package com.fiap.clyvovet.service;

import com.fiap.clyvovet.dto.ProtocoloLongevidadeDto;
import com.fiap.clyvovet.model.Pet;
import com.fiap.clyvovet.model.Raca;
import com.fiap.clyvovet.repository.RacaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class LongevidadeCalculadoraMultiEspecieTest {

    @Autowired
    private LongevidadeCalculadoraService calculadoraService;

    @Autowired
    private PetService petService;

    @Autowired
    private RacaRepository racaRepository;

    @Test
    @DisplayName("Emoji deve refletir a espécie e categoria biológica de forma padronizada")
    void deveRetornarEmojiPorEspecie() {
        assertEquals("🐶", new Raca(1L, "Golden Retriever", "CANINA", null, 12, null).getEmoji());
        assertEquals("🐶", new Raca(2L, "SRD (Vira-lata)", "CANINA", null, 14, null).getEmoji());
        assertEquals("🐱", new Raca(3L, "Siamês", "FELINA", null, 16, null).getEmoji());
        assertEquals("🦜", new Raca(4L, "Calopsita", "AVE", null, 15, null).getEmoji());
        assertEquals("🦜", new Raca(5L, "Papagaio-verdadeiro", "AVE", null, 50, null).getEmoji());
        assertEquals("🐢", new Raca(6L, "Jabuti-piranga", "REPTIL", null, 60, null).getEmoji());
        assertEquals("🐍", new Raca(7L, "Corn Snake (Cobra-do-milho)", "REPTIL", null, 16, null).getEmoji());
        assertEquals("🐹", new Raca(8L, "Rato Twister", "ROEDOR", null, 3, null).getEmoji());
        assertEquals("🦦", new Raca(9L, "Furão (Ferret)", "MUSTELIDEO", null, 8, null).getEmoji());
        assertEquals("🐠", new Raca(10L, "Peixe Betta", "PEIXE", null, 3, null).getEmoji());
        assertEquals("🕷️", new Raca(11L, "Tarântula (Caranguejeira)", "ARACNIDEO", null, 15, null).getEmoji());
        assertEquals("🐴", new Raca(12L, "Cavalo", "EQUINA", null, 28, null).getEmoji());
        assertEquals("🐾", new Raca(13L, "Desconhecido", "OUTRO", null, 10, null).getEmoji());
    }

    @Test
    @DisplayName("Protocolo de longevidade deve adaptar estágio de vida e cuidados para espécies exóticas")
    void deveCalcularProtocoloParaAvesERepteis() {
        // Jabuti com 15 anos (expectativa 60 anos) -> 25% de vida percorrida -> JOVEM ADULTO
        Raca jabuti = new Raca(100L, "Jabuti-piranga", "REPTIL", "Piramidismo", 60, "Lâmpada UVB");
        Pet petJabuti = new Pet();
        petJabuti.setNome("Donatello");
        petJabuti.setRaca(jabuti);
        petJabuti.setDataNascimento(LocalDate.now().minusYears(15));
        petJabuti.setPeso(new BigDecimal("4.5"));

        ProtocoloLongevidadeDto protoJabuti = calculadoraService.calcularProtocolo(petJabuti);
        assertEquals("JOVEM ADULTO", protoJabuti.getEstagioVida());
        assertEquals(25, protoJabuti.getPercentualExpectativaVida());
        assertTrue(protoJabuti.getVacinasRecomendadas().stream().anyMatch(v -> v.contains("UVB")));
        assertTrue(protoJabuti.getExamesPreventivos().stream().anyMatch(e -> e.contains("carapaça") || e.contains("osteodistrofia")));

        // Rato Twister com 2 anos (expectativa 3 anos) -> 67% de vida percorrida -> ADULTO MADURO
        Raca rato = new Raca(101L, "Rato Twister", "ROEDOR", "Micoplasma", 3, "Substrato sem pó");
        Pet petRato = new Pet();
        petRato.setNome("Splinter");
        petRato.setRaca(rato);
        petRato.setDataNascimento(LocalDate.now().minusYears(2));
        petRato.setPeso(new BigDecimal("0.4"));

        ProtocoloLongevidadeDto protoRato = calculadoraService.calcularProtocolo(petRato);
        assertEquals(67, protoRato.getPercentualExpectativaVida());
        assertTrue(protoRato.getExamesPreventivos().stream().anyMatch(e -> e.contains("Mycoplasma") || e.contains("respiratório")));
    }

    @Test
    @DisplayName("PetService deve agrupar raças por espécie corretamente")
    void deveAgruparRacasPorEspecie() {
        Map<String, List<Raca>> agrupadas = petService.listarRacasAgrupadasPorEspecie();
        assertNotNull(agrupadas);
        assertFalse(agrupadas.isEmpty());
        assertTrue(agrupadas.containsKey("CANINA"));
        assertTrue(agrupadas.containsKey("FELINA"));
        assertTrue(agrupadas.containsKey("AVE"));
        assertTrue(agrupadas.containsKey("REPTIL"));
    }

    @Test
    @DisplayName("PetService deve posicionar a opção geral/srd no índice 0 de cada grupo de espécie")
    void devePosicionarOpcaoGeralNoInicioDoGrupo() {
        Map<String, List<Raca>> agrupadas = petService.listarRacasAgrupadasPorEspecie();

        // Verifica que AVE tem opção geral no índice 0
        List<Raca> aves = agrupadas.get("AVE");
        assertNotNull(aves);
        assertTrue(aves.get(0).getNome().contains("Geral"));

        // Verifica que FELINA tem opção geral no índice 0
        List<Raca> felinos = agrupadas.get("FELINA");
        assertNotNull(felinos);
        assertTrue(felinos.get(0).getNome().contains("Geral"));

        // Verifica que CANINA tem opção geral no índice 0
        List<Raca> caninos = agrupadas.get("CANINA");
        assertNotNull(caninos);
        assertTrue(caninos.get(0).getNome().contains("Geral"));

        // Verifica que categoria OUTRO está cadastrada e disponível
        assertTrue(agrupadas.containsKey("OUTRO"));
        assertEquals("Outro Animal (Espécie Não Listada)", agrupadas.get("OUTRO").get(0).getNome());
    }
}
