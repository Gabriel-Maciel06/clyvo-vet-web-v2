package com.fiap.clyvovet.service;

import com.fiap.clyvovet.dto.AvaliacaoTriagemDto;
import com.fiap.clyvovet.dto.SolicitacaoTriagemDto;
import com.fiap.clyvovet.model.*;
import com.fiap.clyvovet.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

@Service
public class TriagemService {

    private final ConsultaTriagemRepository triagemRepository;
    private final PetRepository petRepository;
    private final CheckinDiarioRepository checkinRepository;
    private final HistoricoClinicoRepository historicoClinicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PetService petService;

    public TriagemService(ConsultaTriagemRepository triagemRepository,
                          PetRepository petRepository,
                          CheckinDiarioRepository checkinRepository,
                          HistoricoClinicoRepository historicoClinicoRepository,
                          UsuarioRepository usuarioRepository,
                          PetService petService) {
        this.triagemRepository = triagemRepository;
        this.petRepository = petRepository;
        this.checkinRepository = checkinRepository;
        this.historicoClinicoRepository = historicoClinicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.petService = petService;
    }

    public List<ConsultaTriagem> listarTodas() {
        return triagemRepository.findAllByOrderByDataSolicitacaoDesc();
    }

    public List<ConsultaTriagem> listarPorPet(Long petId) {
        return triagemRepository.findByPetIdOrderByDataSolicitacaoDesc(petId);
    }

    public List<ConsultaTriagem> listarPorStatus(StatusConsulta status) {
        return triagemRepository.findByStatusOrderByDataSolicitacaoDesc(status);
    }

    public ConsultaTriagem buscarPorId(Long id) {
        return triagemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Triagem não encontrada com ID: " + id));
    }

    /** FLUXO 2 (etapa do tutor): abre a solicitação de triagem para um pet do próprio tutor. */
    @Transactional
    public ConsultaTriagem solicitarTriagem(SolicitacaoTriagemDto dto, String usernameTutor) {
        Pet pet = petRepository.findById(dto.getPetId())
                .orElseThrow(() -> new IllegalArgumentException("Pet não encontrado: " + dto.getPetId()));
        petService.validarPropriedade(pet, usernameTutor);

        ConsultaTriagem triagem = new ConsultaTriagem();
        triagem.setPet(pet);
        triagem.setDataSolicitacao(LocalDateTime.now());
        triagem.setStatus(StatusConsulta.SOLICITADA);
        triagem.setQueixaPrincipal(dto.getQueixaPrincipal());

        ConsultaTriagem salva = triagemRepository.save(triagem);

        HistoricoClinico hist = new HistoricoClinico(
                null, pet, LocalDateTime.now(),
                "TRIAGEM_PREVENTIVA",
                "Solicitação de triagem preventiva aberta: " + dto.getQueixaPrincipal(),
                "Aguardando avaliação clínica e cálculo preditivo de longevidade."
        );
        historicoClinicoRepository.save(hist);

        return salva;
    }

    @Transactional
    public ConsultaTriagem avaliarTriagem(AvaliacaoTriagemDto dto, String usernameVeterinario) {
        ConsultaTriagem triagem = buscarPorId(dto.getTriagemId());
        Usuario vet = usuarioRepository.findByUsername(usernameVeterinario)
                .orElseThrow(() -> new IllegalArgumentException("Veterinário não encontrado: " + usernameVeterinario));

        Pet pet = triagem.getPet();

        // 1. Atualizar dados físicos aferidos
        triagem.setPesoAferido(dto.getPesoAferido());
        triagem.setTemperatura(dto.getTemperatura());
        triagem.setFrequenciaCardiaca(dto.getFrequenciaCardiaca());
        triagem.setParecerVeterinario(dto.getParecerVeterinario());
        triagem.setVeterinario(vet);
        triagem.setDataConsulta(LocalDateTime.now());
        triagem.setStatus(StatusConsulta.CONCLUIDA);

        // Atualizar peso no perfil do Pet
        pet.setPeso(dto.getPesoAferido());
        petRepository.save(pet);

        // 2. MOTOR DE IA PREDITIVA E ESCORE DE LONGEVIDADE (0 a 100)
        ResultadoCalculoEscore resultado = calcularEscoreLongevidadeEInsights(pet, dto.getPesoAferido(), dto.getTemperatura(), dto.getFrequenciaCardiaca());
        triagem.setEscoreLongevidade(resultado.escore());
        triagem.setClassificacaoRisco(resultado.risco());
        triagem.setInsightIa(resultado.insights());

        // Atualiza escore no Pet
        pet.setEscoreSaude(resultado.escore());
        pet.setStatusLongevidade("Escore: " + resultado.escore() + "/100 - Risco " + resultado.risco().name());
        petRepository.save(pet);

        ConsultaTriagem salva = triagemRepository.save(triagem);

        // 3. Registrar na Linha do Tempo Clínica
        HistoricoClinico hist = new HistoricoClinico(
                null, pet, LocalDateTime.now(),
                "TRIAGEM_PREVENTIVA",
                String.format("Triagem Concluída por Dr(a). %s. Escore Longevidade: %d/100 (Risco %s).",
                        vet.getNomeCompleto(), resultado.escore(), resultado.risco().name()),
                "Insights da IA: " + resultado.insights() + " | Parecer: " + dto.getParecerVeterinario()
        );
        historicoClinicoRepository.save(hist);

        return salva;
    }

    public record ResultadoCalculoEscore(int escore, ClassificacaoRisco risco, String insights) {}

    public ResultadoCalculoEscore calcularEscoreLongevidadeEInsights(Pet pet, BigDecimal pesoAferido, BigDecimal temperatura, Integer freqCardiaca) {
        int escore = 100;
        StringBuilder insights = new StringBuilder();

        // Idade
        int idadeAnos = Period.between(pet.getDataNascimento(), LocalDate.now()).getYears();
        if (idadeAnos >= 8) {
            escore -= 20;
            insights.append("[Idade Sênior: ").append(idadeAnos).append(" anos. Maior risco articular e renal] ");
        } else if (idadeAnos >= 5) {
            escore -= 10;
            insights.append("[Fase Adulta Madura: ").append(idadeAnos).append(" anos] ");
        } else {
            insights.append("[Jovem/Adulto Saudável: ").append(idadeAnos).append(" anos] ");
        }

        // Raça & Propensão Genética
        Raca raca = pet.getRaca();
        if (raca != null && raca.getPropensaoDoenca() != null) {
            escore -= 15;
            insights.append("[Genética da Raça: Alerta de predisposição para ").append(raca.getPropensaoDoenca()).append("] ");
        }

        // Temperatura e Sinais Vitais
        if (temperatura != null) {
            if (temperatura.compareTo(new BigDecimal("39.3")) > 0) {
                escore -= 15;
                insights.append("[Hipertermia/Febre detectada: ").append(temperatura).append("°C] ");
            } else if (temperatura.compareTo(new BigDecimal("37.8")) < 0) {
                escore -= 15;
                insights.append("[Hipotermia detectada: ").append(temperatura).append("°C] ");
            }
        }

        if (freqCardiaca != null && (freqCardiaca < 60 || freqCardiaca > 160)) {
            escore -= 10;
            insights.append("[Frequência cardíaca fora do padrão ótimo: ").append(freqCardiaca).append(" bpm] ");
        }

        // Histórico de Check-ins (Alimentação e Sintomas)
        List<CheckinDiario> ultimosCheckins = checkinRepository.findByPetIdOrderByDataCheckinDesc(pet.getId());
        long checkinsComAlerta = ultimosCheckins.stream().filter(CheckinDiario::getAlertaGerado).count();
        if (checkinsComAlerta >= 2) {
            escore -= 15;
            insights.append("[Histórico Recente: ").append(checkinsComAlerta).append(" alertas de apatia ou recusa alimentar reportados nos check-ins do tutor] ");
        }

        // Clamping escore 0 a 100
        escore = Math.max(10, Math.min(100, escore));

        ClassificacaoRisco risco;
        if (escore >= 80) {
            risco = ClassificacaoRisco.BAIXO;
        } else if (escore >= 50) {
            risco = ClassificacaoRisco.MODERADO;
        } else {
            risco = ClassificacaoRisco.ALTO;
        }

        return new ResultadoCalculoEscore(escore, risco, insights.toString().trim());
    }
}
