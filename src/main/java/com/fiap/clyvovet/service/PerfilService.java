package com.fiap.clyvovet.service;

import com.fiap.clyvovet.dto.CompletarCadastroDto;
import com.fiap.clyvovet.model.Tutor;
import com.fiap.clyvovet.repository.PetRepository;
import com.fiap.clyvovet.repository.TutorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Prefixo do CPF provisório atribuído a tutores criados via login social (ver {@link CustomOAuth2UserService}). */
@Service
public class PerfilService {

    public static final String PREFIXO_CPF_PROVISORIO = "GOOGLE";

    private final TutorRepository tutorRepository;
    private final PetRepository petRepository;

    public PerfilService(TutorRepository tutorRepository, PetRepository petRepository) {
        this.tutorRepository = tutorRepository;
        this.petRepository = petRepository;
    }

    public boolean precisaCompletarCadastro(Tutor tutor) {
        return tutor != null && tutor.getCpf() != null && tutor.getCpf().startsWith(PREFIXO_CPF_PROVISORIO);
    }

    /**
     * Troca o CPF provisório (atribuído no login social) pelo CPF real informado
     * pelo tutor. Como o CPF é a chave primária de T_TUTOR, a troca é feita
     * removendo o registro antigo e recriando com a nova chave — só é permitida
     * enquanto o tutor ainda não tem nenhum pet cadastrado, evitando qualquer
     * inconsistência com chaves estrangeiras já existentes.
     */
    @Transactional
    public void completarCadastro(String username, CompletarCadastroDto dto) {
        Tutor tutorAtual = tutorRepository.findByUsuarioUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Tutor não encontrado para o usuário: " + username));

        if (!precisaCompletarCadastro(tutorAtual)) {
            throw new IllegalStateException("Este cadastro já está completo.");
        }

        String novoCpf = dto.getCpf().replaceAll("\\D", "");
        if (tutorRepository.existsById(novoCpf)) {
            throw new IllegalArgumentException("Já existe um tutor cadastrado com este CPF.");
        }
        if (petRepository.existsByTutorCpf(tutorAtual.getCpf())) {
            // Salvaguarda: não deveria acontecer, pois o CPF provisório é bloqueado no cadastro de pet.
            throw new IllegalStateException("Não é possível trocar o CPF: já existem pets vinculados.");
        }

        Tutor novoTutor = new Tutor();
        novoTutor.setCpf(novoCpf);
        novoTutor.setNome(tutorAtual.getNome());
        novoTutor.setEmail(tutorAtual.getEmail());
        novoTutor.setTelefone(dto.getTelefone());
        novoTutor.setUsuario(tutorAtual.getUsuario());

        tutorRepository.delete(tutorAtual);
        tutorRepository.flush();
        tutorRepository.save(novoTutor);
    }
}
