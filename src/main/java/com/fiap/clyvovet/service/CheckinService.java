package com.fiap.clyvovet.service;

import com.fiap.clyvovet.dto.CheckinDto;
import com.fiap.clyvovet.model.*;
import com.fiap.clyvovet.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CheckinService {

    private final CheckinDiarioRepository checkinRepository;
    private final PetRepository petRepository;
    private final RecompensaTutorRepository recompensaRepository;
    private final BadgeConquistaRepository badgeRepository;
    private final HistoricoClinicoRepository historicoClinicoRepository;
    private final PetService petService;

    public CheckinService(CheckinDiarioRepository checkinRepository,
                          PetRepository petRepository,
                          RecompensaTutorRepository recompensaRepository,
                          BadgeConquistaRepository badgeRepository,
                          HistoricoClinicoRepository historicoClinicoRepository,
                          PetService petService) {
        this.checkinRepository = checkinRepository;
        this.petRepository = petRepository;
        this.recompensaRepository = recompensaRepository;
        this.badgeRepository = badgeRepository;
        this.historicoClinicoRepository = historicoClinicoRepository;
        this.petService = petService;
    }

    public List<CheckinDiario> listarHistoricoPorPet(Long petId) {
        return checkinRepository.findByPetIdOrderByDataCheckinDesc(petId);
    }

    public List<CheckinDiario> listarAlertasAtivos() {
        return checkinRepository.findByAlertaGeradoTrueOrderByDataCheckinDesc();
    }

    public RecompensaTutor obterOuCriarRecompensa(String tutorCpf) {
        return recompensaRepository.findByTutorCpf(tutorCpf)
                .orElseGet(() -> {
                    RecompensaTutor nova = new RecompensaTutor(null, tutorCpf, 0, 0, null, 0, "BRONZE");
                    return recompensaRepository.save(nova);
                });
    }

    public List<BadgeConquista> listarBadgesPorPet(Long petId) {
        return badgeRepository.findByPetIdOrderByDataConquistaDesc(petId);
    }

    public List<com.fiap.clyvovet.dto.BadgeItemDto> obterGaleriaDeBadgesCompletas(Pet pet) {
        List<BadgeConquista> conquistadas = badgeRepository.findByPetIdOrderByDataConquistaDesc(pet.getId());
        long totalCheckins = checkinRepository.countByPetId(pet.getId());
        RecompensaTutor recompensa = obterOuCriarRecompensa(pet.getTutor().getCpf());
        int streakAtual = recompensa.getStreakDias();

        List<com.fiap.clyvovet.dto.BadgeItemDto> galeria = new java.util.ArrayList<>();

        // 1. Primeiro Passo
        boolean has1 = conquistadas.stream().anyMatch(b -> b.getCodigoBadge().equalsIgnoreCase("PRIMEIRO_PASSO") || b.getCodigoBadge().equalsIgnoreCase("STREAK_5"));
        LocalDate data1 = conquistadas.stream().filter(b -> b.getCodigoBadge().equalsIgnoreCase("PRIMEIRO_PASSO") || b.getCodigoBadge().equalsIgnoreCase("STREAK_5")).findFirst().map(BadgeConquista::getDataConquista).orElse(null);
        galeria.add(new com.fiap.clyvovet.dto.BadgeItemDto(
                "PRIMEIRO_PASSO", "Primeiro Passo", "bi-award-fill",
                "Completou o 1º check-in de saúde e longevidade na plataforma Clyvo Vet.",
                totalCheckins >= 1 || has1, data1 != null ? data1 : LocalDate.now(),
                (int) Math.min(1, Math.max(1, totalCheckins)), 1, "Complete 1 check-in diário"
        ));

        // 2. Guardião Fiel (Streak 5 dias)
        boolean hasStreak5 = conquistadas.stream().anyMatch(b -> b.getCodigoBadge().equalsIgnoreCase("STREAK_5") || b.getCodigoBadge().equalsIgnoreCase("TUTOR_DEDICADO"));
        LocalDate dataStreak5 = conquistadas.stream().filter(b -> b.getCodigoBadge().equalsIgnoreCase("STREAK_5")).findFirst().map(BadgeConquista::getDataConquista).orElse(null);
        galeria.add(new com.fiap.clyvovet.dto.BadgeItemDto(
                "STREAK_5", "Guardião Fiel (5 Dias)", "bi-fire",
                "Manteve uma sequência consecutiva de 5 dias de check-in de rotina.",
                streakAtual >= 5 || hasStreak5, dataStreak5 != null ? dataStreak5 : LocalDate.now().minusDays(1),
                Math.min(5, Math.max(streakAtual, hasStreak5 ? 5 : 0)), 5, "Mantenha 5 dias consecutivos de check-in"
        ));

        // 3. Atleta Canino (Vida Ativa)
        boolean hasAtleta = conquistadas.stream().anyMatch(b -> b.getCodigoBadge().equalsIgnoreCase("VIDA_ATIVA"));
        LocalDate dataAtleta = conquistadas.stream().filter(b -> b.getCodigoBadge().equalsIgnoreCase("VIDA_ATIVA")).findFirst().map(BadgeConquista::getDataConquista).orElse(null);
        galeria.add(new com.fiap.clyvovet.dto.BadgeItemDto(
                "VIDA_ATIVA", "Atleta Canino", "bi-lightning-charge-fill",
                "Acumulou mais de 45 minutos diários de caminhadas e exercícios ativos.",
                hasAtleta, dataAtleta,
                hasAtleta ? 45 : 30, 45, "Registre 45 min de atividade física em um check-in"
        ));

        // 4. Semana de Ouro (7 dias de streak)
        boolean hasSemana7 = streakAtual >= 7;
        galeria.add(new com.fiap.clyvovet.dto.BadgeItemDto(
                "SEMANA_OURO", "Semana de Ouro", "bi-trophy-fill",
                "Completou 7 dias seguidos monitorando dieta, humor e medicação.",
                hasSemana7, null,
                Math.min(7, streakAtual), 7, "Atingir 7 dias seguidos de streak (Atualmente: " + streakAtual + "/7)"
        ));

        // 5. Mestre da Nutrição (10 check-ins com alimentação balanceada)
        boolean hasNutri = totalCheckins >= 10;
        galeria.add(new com.fiap.clyvovet.dto.BadgeItemDto(
                "MESTRE_NUTRICAO", "Mestre da Nutrição", "bi-heart-pulse-fill",
                "Acompanhou com rigor a alimentação recomendada por 10 dias.",
                hasNutri, null,
                (int) Math.min(10, totalCheckins), 10, "Registrar 10 check-ins de alimentação saudável (Atualmente: " + totalCheckins + "/10)"
        ));

        // 6. Guardião da Longevidade (Check-up preventivo com escore > 90)
        boolean hasEscore90 = pet.getEscoreSaude() != null && pet.getEscoreSaude() >= 90;
        galeria.add(new com.fiap.clyvovet.dto.BadgeItemDto(
                "GUARDIAO_LONGEVIDADE", "Guardião da Longevidade", "bi-shield-check",
                "Concluiu a triagem clínica com Escore de Longevidade superior a 90 pontos!",
                hasEscore90, LocalDate.now().minusDays(8),
                pet.getEscoreSaude() != null ? pet.getEscoreSaude() : 85, 90, "Obtenha nota clínica superior a 90/100"
        ));

        return galeria;
    }

    /**
     * FLUXO 1 - Check-in diário: valida propriedade do pet, evita duplicidade no dia,
     * detecta alerta clínico, pontua, atualiza streak/nível do tutor, desbloqueia badges
     * e registra o evento na linha do tempo clínica.
     */
    @Transactional
    public CheckinDiario registrarCheckin(CheckinDto dto, String usernameTutor) {
        Pet pet = petRepository.findById(dto.getPetId())
                .orElseThrow(() -> new IllegalArgumentException("Pet não encontrado: " + dto.getPetId()));
        petService.validarPropriedade(pet, usernameTutor);

        LocalDate hoje = LocalDate.now();

        // 1. Evitar check-in duplicado no mesmo dia
        Optional<CheckinDiario> existente = checkinRepository.findByPetIdAndDataCheckin(pet.getId(), hoje);
        if (existente.isPresent()) {
            throw new IllegalStateException("O check-in diário para " + pet.getNome() + " já foi realizado hoje!");
        }

        // 2. Análise de Sintomas / Alerta de Saúde
        boolean geraAlerta = false;
        if (dto.getHumorPet() == HumorPet.APATICO || dto.getHumorPet() == HumorPet.DOR ||
            dto.getAlimentacaoStatus() == AlimentacaoStatus.POUCO_APETITE ||
            (dto.getSintomasObservados() != null && !dto.getSintomasObservados().isBlank())) {
            geraAlerta = true;
        }

        // 3. Salva Check-in
        int pontosGanhos = 10;
        if (dto.getMinutosAtividade() != null && dto.getMinutosAtividade() >= 30) {
            pontosGanhos += 5; // Bônus atividade
        }
        if (Boolean.TRUE.equals(dto.getRemedioAdministrado())) {
            pontosGanhos += 5; // Bônus medicação rigorosa
        }

        CheckinDiario checkin = new CheckinDiario(
                null, pet, hoje, dto.getAlimentacaoStatus(),
                dto.getRemedioAdministrado(), dto.getMinutosAtividade(),
                dto.getHumorPet(), dto.getSintomasObservados(),
                pontosGanhos, geraAlerta
        );
        CheckinDiario salvo = checkinRepository.save(checkin);

        // 4. Atualizar Gamificação do Tutor (Streak, Pontos, Desconto, Nível)
        atualizarGamificacaoTutor(pet.getTutor().getCpf(), hoje, pontosGanhos);

        // 5. Avaliar e Destravar Badges para o Pet
        avaliarBadges(pet, dto.getMinutosAtividade());

        // 6. Registrar Evento na Linha do Tempo Clínica
        String desc = String.format("Check-in diário realizado: Humor %s, Dieta %s, Atividade %d min.",
                dto.getHumorPet().getDescricao(), dto.getAlimentacaoStatus().getDescricao(), dto.getMinutosAtividade());
        String conduta = geraAlerta
                ? "ALERTA CLÍNICO: Sintomas ou apatia relatados pelo tutor. Notificação enviada à clínica e veterinário responsável."
                : "Parâmetros diários dentro da normalidade de prevenção e bem-estar.";

        HistoricoClinico hist = new HistoricoClinico(
                null, pet, LocalDateTime.now(),
                geraAlerta ? "ALERTA_SAUDE" : "CHECKIN_DIARIO",
                desc, conduta
        );
        historicoClinicoRepository.save(hist);

        return salvo;
    }

    private void atualizarGamificacaoTutor(String tutorCpf, LocalDate hoje, int pontosGanhos) {
        RecompensaTutor recompensa = obterOuCriarRecompensa(tutorCpf);
        recompensa.registrarCheckinNaData(hoje);   // streak
        recompensa.adicionarPontos(pontosGanhos);  // pontos + nível + desconto (regra na entidade)
        recompensaRepository.save(recompensa);
    }

    private void avaliarBadges(Pet pet, Integer minutosAtividade) {
        long totalCheckins = checkinRepository.countByPetId(pet.getId());

        // Badge 0: Atleta (45+ minutos de atividade em um único check-in)
        if (minutosAtividade != null && minutosAtividade >= 45 && !badgeRepository.existsByPetIdAndCodigoBadge(pet.getId(), "VIDA_ATIVA")) {
            badgeRepository.save(new BadgeConquista(
                    null, pet, "VIDA_ATIVA", "Atleta Canino", "bi-lightning-charge-fill",
                    "Mais de 45 minutos diários de caminhadas e atividades físicas.", LocalDate.now()
            ));
        }

        // Badge 1: Primeiro Passo
        if (totalCheckins >= 1 && !badgeRepository.existsByPetIdAndCodigoBadge(pet.getId(), "PRIMEIRO_PASSO")) {
            badgeRepository.save(new BadgeConquista(
                    null, pet, "PRIMEIRO_PASSO", "Primeiro Passo", "bi-award",
                    "Completou o 1º check-in de saúde e longevidade na plataforma!", LocalDate.now()
            ));
        }

        // Badge 2: Tutor Dedicado (3 check-ins)
        if (totalCheckins >= 3 && !badgeRepository.existsByPetIdAndCodigoBadge(pet.getId(), "TUTOR_DEDICADO")) {
            badgeRepository.save(new BadgeConquista(
                    null, pet, "TUTOR_DEDICADO", "Tutor Dedicado", "bi-heart-pulse",
                    "Registrou 3 ou mais check-ins, garantindo histórico contínuo.", LocalDate.now()
            ));
        }

        // Badge 3: Guardião da Longevidade (7 check-ins)
        if (totalCheckins >= 7 && !badgeRepository.existsByPetIdAndCodigoBadge(pet.getId(), "GUARDIAO_LONGEVIDADE")) {
            badgeRepository.save(new BadgeConquista(
                    null, pet, "GUARDIAO_LONGEVIDADE", "Guardião da Longevidade", "bi-shield-check",
                    "1 semana completa de dados biométricos e hábitos saudáveis monitorados!", LocalDate.now()
            ));
        }
    }
}
