package com.fiap.clyvovet.service;

import com.fiap.clyvovet.dto.CompletarCadastroDto;
import com.fiap.clyvovet.model.Tutor;
import com.fiap.clyvovet.repository.TutorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PerfilServiceTest {

    @Autowired
    private PerfilService perfilService;

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private TutorRepository tutorRepository;

    @Test
    @DisplayName("Tutor provisionado via Google completa o cadastro trocando o CPF provisório pelo real")
    void completaCadastroTrocandoCpf() {
        customOAuth2UserService.provisionarOuVincular("teste.perfil@gmail.com", "Teste Perfil", "sub-perfil", null);
        Tutor antes = tutorRepository.findByUsuarioUsername("teste.perfil@gmail.com").orElseThrow();
        assertTrue(perfilService.precisaCompletarCadastro(antes));

        CompletarCadastroDto dto = new CompletarCadastroDto();
        dto.setCpf("321.654.987-00");
        dto.setTelefone("(11) 90000-1111");
        perfilService.completarCadastro("teste.perfil@gmail.com", dto);

        Tutor depois = tutorRepository.findByUsuarioUsername("teste.perfil@gmail.com").orElseThrow();
        assertEquals("32165498700", depois.getCpf());
        assertEquals("(11) 90000-1111", depois.getTelefone());
        assertFalse(perfilService.precisaCompletarCadastro(depois));
        assertFalse(tutorRepository.existsById(antes.getCpf()), "o CPF provisório antigo não deve mais existir");
    }

    @Test
    @DisplayName("Tutor com cadastro já completo não pode repetir a troca de CPF")
    void naoPermiteCompletarDuasVezes() {
        Tutor tutorSeed = tutorRepository.findByUsuarioUsername("tutor").orElseThrow(); // seed V3, CPF real

        CompletarCadastroDto dto = new CompletarCadastroDto();
        dto.setCpf("999.999.999-99");
        dto.setTelefone("(11) 99999-9999");

        assertThrows(IllegalStateException.class, () -> perfilService.completarCadastro("tutor", dto));
    }
}
