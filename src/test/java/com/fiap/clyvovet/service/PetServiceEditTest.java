package com.fiap.clyvovet.service;

import com.fiap.clyvovet.dto.PetDto;
import com.fiap.clyvovet.model.Pet;
import com.fiap.clyvovet.model.RoleUsuario;
import com.fiap.clyvovet.model.Tutor;
import com.fiap.clyvovet.model.Usuario;
import com.fiap.clyvovet.repository.PetRepository;
import com.fiap.clyvovet.repository.TutorRepository;
import com.fiap.clyvovet.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PetServiceEditTest {

    @Autowired
    private PetService petService;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TutorRepository tutorRepository;

    @Test
    @DisplayName("Edição de pet atualiza nome, peso e dados cadastrais com sucesso pelo tutor proprietário")
    void deveEditarPetComSucesso() {
        Pet petOriginal = petService.listarPorTutor("tutor").get(0);
        Long petId = petOriginal.getId();

        PetDto editDto = new PetDto();
        editDto.setId(petId);
        editDto.setNome("Nome Editado Teste");
        editDto.setPeso(new BigDecimal("28.4"));
        editDto.setRacaId(petOriginal.getRaca().getId());
        editDto.setDataNascimento(LocalDate.of(2021, 5, 10));

        Pet petAtualizado = petService.salvar(editDto, "tutor");

        assertEquals(petId, petAtualizado.getId());
        assertEquals("Nome Editado Teste", petAtualizado.getNome());
        assertEquals(new BigDecimal("28.4"), petAtualizado.getPeso());
        assertEquals(LocalDate.of(2021, 5, 10), petAtualizado.getDataNascimento());

        Pet petDoBanco = petRepository.findById(petId).orElseThrow();
        assertEquals("Nome Editado Teste", petDoBanco.getNome());
    }

    @Test
    @DisplayName("Não deve permitir que outro tutor edite pet que não lhe pertence")
    void deveBloquearEdicaoPorOutroTutor() {
        // Cria um segundo tutor
        Usuario u2 = new Usuario(null, "outro_tutor", "senha", RoleUsuario.ROLE_TUTOR, "Outro Tutor", "outro@gmail.com");
        usuarioRepository.save(u2);
        Tutor t2 = new Tutor("999.888.777-66", "Outro Tutor", "(11) 91111-2222", "outro@gmail.com", u2);
        tutorRepository.save(t2);

        Pet petDoTutor1 = petService.listarPorTutor("tutor").get(0);

        PetDto editDto = new PetDto();
        editDto.setId(petDoTutor1.getId());
        editDto.setNome("Tentativa Invasao");
        editDto.setPeso(new BigDecimal("10.0"));
        editDto.setRacaId(petDoTutor1.getRaca().getId());
        editDto.setDataNascimento(petDoTutor1.getDataNascimento());

        assertThrows(AccessDeniedException.class, () -> petService.salvar(editDto, "outro_tutor"));
    }
}
