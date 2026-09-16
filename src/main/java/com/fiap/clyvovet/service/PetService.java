package com.fiap.clyvovet.service;

import com.fiap.clyvovet.dto.PetDto;
import com.fiap.clyvovet.model.*;
import com.fiap.clyvovet.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PetService {

    private final PetRepository petRepository;
    private final TutorRepository tutorRepository;
    private final RacaRepository racaRepository;
    private final HistoricoClinicoRepository historicoClinicoRepository;
    private final CheckinDiarioRepository checkinDiarioRepository;
    private final BadgeConquistaRepository badgeConquistaRepository;
    private final ConsultaTriagemRepository consultaTriagemRepository;

    public PetService(PetRepository petRepository,
                      TutorRepository tutorRepository,
                      RacaRepository racaRepository,
                      HistoricoClinicoRepository historicoClinicoRepository,
                      CheckinDiarioRepository checkinDiarioRepository,
                      BadgeConquistaRepository badgeConquistaRepository,
                      ConsultaTriagemRepository consultaTriagemRepository) {
        this.petRepository = petRepository;
        this.tutorRepository = tutorRepository;
        this.racaRepository = racaRepository;
        this.historicoClinicoRepository = historicoClinicoRepository;
        this.checkinDiarioRepository = checkinDiarioRepository;
        this.badgeConquistaRepository = badgeConquistaRepository;
        this.consultaTriagemRepository = consultaTriagemRepository;
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
        return racaRepository.findAllByOrderByEspecieAscNomeAsc();
    }

    public Map<String, List<Raca>> listarRacasAgrupadasPorEspecie() {
        return racaRepository.findAll().stream()
                .sorted(java.util.Comparator
                        .comparing(Raca::getEspecie)
                        .thenComparing((Raca r) -> isRacaGeral(r) ? 0 : 1)
                        .thenComparing(Raca::getNome))
                .collect(Collectors.groupingBy(Raca::getEspecie, LinkedHashMap::new, Collectors.toList()));
    }

    private boolean isRacaGeral(Raca r) {
        if (r == null || r.getNome() == null) return false;
        String n = r.getNome().toLowerCase();
        return n.contains("geral") || n.contains("não listada");
    }

    @Transactional
    public Pet salvar(PetDto dto, String username) {
        Tutor tutor = tutorRepository.findByUsuarioUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Tutor não encontrado para o usuário: " + username));

        Raca raca = racaRepository.findById(dto.getRacaId())
                .orElseThrow(() -> new IllegalArgumentException("Raça inválida com ID: " + dto.getRacaId()));

        // Validação de consistência biológica (impede idades e pesos irreais)
        validarLimitesBiologicos(dto, raca);

        Pet pet;
        boolean novo = false;
        if (dto.getId() != null) {
            pet = buscarPorId(dto.getId());
            validarPropriedade(pet, username);

            BigDecimal pesoAnterior = pet.getPeso();
            pet.setPeso(dto.getPeso());
            if (dto.getNome() != null && !dto.getNome().isBlank()) {
                pet.setNome(dto.getNome());
            }
            if (dto.getDataNascimento() != null) {
                pet.setDataNascimento(dto.getDataNascimento());
            }
            if (raca != null) {
                pet.setRaca(raca);
            }

            if (pesoAnterior != null && !pesoAnterior.equals(dto.getPeso())) {
                HistoricoClinico histPeso = new HistoricoClinico(
                        null, pet, java.time.LocalDateTime.now(),
                        "PESAGEM_CLINICA",
                        "Peso aferido atualizado de " + pesoAnterior + " kg para " + dto.getPeso() + " kg.",
                        "Recálculo do índice de longevidade e acompanhamento metabólico."
                );
                historicoClinicoRepository.save(histPeso);
            }
        } else {
            if (tutor.getCpf().startsWith(PerfilService.PREFIXO_CPF_PROVISORIO)) {
                throw new IllegalStateException(
                        "Complete seu cadastro (CPF e telefone) antes de adicionar um pet.");
            }
            pet = new Pet();
            pet.setTutor(tutor);
            pet.setStatusLongevidade("Acompanhamento preventivo ativo");
            pet.setEscoreSaude(85);
            pet.setNome(dto.getNome());
            pet.setRaca(raca);
            pet.setDataNascimento(dto.getDataNascimento());
            pet.setPeso(dto.getPeso());
            novo = true;
        }

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

    @Transactional
    public void excluir(Long petId, Authentication auth) {
        Pet pet = buscarPorIdAutorizado(petId, auth);
        removerDadosAssociadosEPet(pet);
    }

    @Transactional
    public void excluir(Long petId, String username) {
        Pet pet = buscarPorId(petId);
        validarPropriedade(pet, username);
        removerDadosAssociadosEPet(pet);
    }

    private void removerDadosAssociadosEPet(Pet pet) {
        Long petId = pet.getId();
        historicoClinicoRepository.deleteByPetId(petId);
        badgeConquistaRepository.deleteByPetId(petId);
        checkinDiarioRepository.deleteByPetId(petId);
        consultaTriagemRepository.deleteByPetId(petId);
        petRepository.delete(pet);
    }

    /**
     * Valida os limites biométricos de idade e peso para impedir inconsistências
     * biológicas impossíveis no prontuário (ex.: cão de 50 anos ou peso de 500 kg).
     */
    public void validarLimitesBiologicos(PetDto dto, Raca raca) {
        if (dto.getDataNascimento() != null) {
            if (dto.getDataNascimento().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("A data de nascimento não pode estar no futuro.");
            }
            int idadeAnos = Period.between(dto.getDataNascimento(), LocalDate.now()).getYears();
            int maxIdade = raca.getLimiteMaximoIdadeAnos();
            if (idadeAnos > maxIdade) {
                throw new IllegalArgumentException(String.format(
                        "Idade biologicamente incompatível: um animal da espécie %s não atinge %d anos de idade (limite biológico aceitável: até %d anos). Por favor, corrija a data de nascimento.",
                        raca.getEspecie(), idadeAnos, maxIdade
                ));
            }
        }

        if (dto.getPeso() != null) {
            double peso = dto.getPeso().doubleValue();
            double minViavel = raca.getLimiteMinimoPesoKg();
            double maxViavel = raca.getLimiteMaximoPesoKg();
            if (peso < minViavel || peso > maxViavel) {
                throw new IllegalArgumentException(String.format(
                        "Peso biologicamente impossível: %.2f kg para a espécie %s (faixa viável: %.2f kg a %.2f kg). Verifique o valor digitado.",
                        peso, raca.getEspecie(), minViavel, maxViavel
                ));
            }
        }
    }
}
