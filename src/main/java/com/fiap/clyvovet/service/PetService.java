package com.fiap.clyvovet.service;

import com.fiap.clyvovet.dto.PetDto;
import com.fiap.clyvovet.model.*;
import com.fiap.clyvovet.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PetService {

    private final PetRepository petRepository;
    private final TutorRepository tutorRepository;
    private final RacaRepository racaRepository;
    private final HistoricoClinicoRepository historicoClinicoRepository;

    public PetService(PetRepository petRepository,
                      TutorRepository tutorRepository,
                      RacaRepository racaRepository,
                      HistoricoClinicoRepository historicoClinicoRepository) {
        this.petRepository = petRepository;
        this.tutorRepository = tutorRepository;
        this.racaRepository = racaRepository;
        this.historicoClinicoRepository = historicoClinicoRepository;
    }

    public List<Pet> listarPorTutor(String username) {
        Tutor tutor = tutorRepository.findByUsuarioUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Tutor não encontrado para o usuário: " + username));
        return petRepository.findByTutorCpfOrderByIdAsc(tutor.getCpf());
    }

    public List<Pet> listarTodos() {
        return petRepository.findAll();
    }

    public Pet buscarPorId(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("Pet não encontrado com ID: " + petId));
    }

    /**
     * Garante que o pet pertence ao tutor logado. O veterinário (ROLE_ADMIN) pode acessar qualquer pet.
     * Evita que um tutor acesse ou manipule pets de outros tutores alterando o ID na URL/formulário.
     */
    public Pet buscarPorIdAutorizado(Long petId, Authentication auth) {
        Pet pet = buscarPorId(petId);
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            validarPropriedade(pet, auth.getName());
        }
        return pet;
    }

    public void validarPropriedade(Pet pet, String username) {
        Tutor tutor = tutorRepository.findByUsuarioUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Usuário sem cadastro de tutor: " + username));
        if (!pet.getTutor().getCpf().equals(tutor.getCpf())) {
            throw new AccessDeniedException("O pet " + pet.getNome() + " não pertence ao tutor logado.");
        }
    }

    public List<Raca> listarRacas() {
        return racaRepository.findAll();
    }

    @Transactional
    public Pet salvar(PetDto dto, String username) {
        Tutor tutor = tutorRepository.findByUsuarioUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Tutor não encontrado para o usuário: " + username));

        Raca raca = racaRepository.findById(dto.getRacaId())
                .orElseThrow(() -> new IllegalArgumentException("Raça inválida com ID: " + dto.getRacaId()));

        Pet pet;
        boolean novo = false;
        if (dto.getId() != null) {
            pet = buscarPorId(dto.getId());
            validarPropriedade(pet, username);
        } else {
            pet = new Pet();
            pet.setTutor(tutor);
            pet.setStatusLongevidade("Acompanhamento preventivo ativo");
            pet.setEscoreSaude(85);
            novo = true;
        }

        pet.setNome(dto.getNome());
        pet.setRaca(raca);
        pet.setDataNascimento(dto.getDataNascimento());
        pet.setPeso(dto.getPeso());

        Pet salvo = petRepository.save(pet);

        if (novo) {
            HistoricoClinico hist = new HistoricoClinico(
                    null, salvo, java.time.LocalDateTime.now(),
                    "CADASTRO_PET",
                    "Pet " + salvo.getNome() + " registrado na plataforma Clyvo Vet.",
                    "Início do acompanhamento preventivo e de longevidade."
            );
            historicoClinicoRepository.save(hist);
        }

        return salvo;
    }
}
