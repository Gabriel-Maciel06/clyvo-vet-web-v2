package com.fiap.clyvovet.service;

import com.fiap.clyvovet.dto.AvaliacaoTriagemDto;
import com.fiap.clyvovet.dto.SolicitacaoTriagemDto;
import com.fiap.clyvovet.model.*;
import com.fiap.clyvovet.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço de Triagem Clínica Preventiva e Avaliação de Longevidade (Clyvo Vet).
 *
 * Arquitetura de Decisão Híbrida (Dual-Engine Architecture):
 * -----------------------------------------------------------
 * 1. CAMADA DE GUARDRAILS CLÍNICOS DETERMINÍSTICOS (Diretrizes AAHA / WSAVA):
 *    Regras de proteção vital inegociáveis para detecção de emergências fisiológicas agudas
 *    (hipertermia, hipotermia, choque, arritmias).
 *
 * 2. CAMADA DE MACHINE LEARNING PROBABILÍSTICO MULTIVARIADO (PredictiveMlEngine):
 *    Modelo supervisionado calibrado em 10.000 amostras clínicas do "Canine Wellness Dataset"
 *    (ROC-AUC: 0.9485, Acurácia: 87.24%), calculando P(Higidez | X), projeção de longevidade,
 *    e fatores de explicabilidade algorítmica (XAI / Feature Attribution).
 *
 * 3. CAMADA DE SÍNTESE CLÍNICA E APOIO À DECISÃO (NLP / SOAP):
 *    Estruturação de anamnese completa no padrão médico veterinário (Subjetivo, Objetivo,
 *    Avaliação, Plano) para enriquecimento do prontuário eletrônico.
 */
@Service
public class TriagemService {

    private final ConsultaTriagemRepository triagemRepository;
    private final PetRepository petRepository;
    private final CheckinDiarioRepository checkinRepository;
    private final HistoricoClinicoRepository historicoClinicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PetService petService;
    private final PredictiveMlEngine mlEngine;

    public TriagemService(ConsultaTriagemRepository triagemRepository,
                          PetRepository petRepository,
                          CheckinDiarioRepository checkinRepository,
                          HistoricoClinicoRepository historicoClinicoRepository,
                          UsuarioRepository usuarioRepository,
                          PetService petService,
                          PredictiveMlEngine mlEngine) {
        this.triagemRepository = triagemRepository;
        this.petRepository = petRepository;
        this.checkinRepository = checkinRepository;
        this.historicoClinicoRepository = historicoClinicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.petService = petService;
        this.mlEngine = mlEngine;
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
                "Aguardando avaliação clínica e cálculo preditivo de longevidade via Machine Learning."
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

        // 2. MOTOR DE MACHINE LEARNING PREDITIVO & GUARDRAILS CLÍNICOS
        ResultadoCalculoEscore resultado = calcularEscoreLongevidadeEInsights(pet, dto.getPesoAferido(), dto.getTemperatura(), dto.getFrequenciaCardiaca(), triagem.getQueixaPrincipal());
        triagem.setEscoreLongevidade(resultado.escore());
        triagem.setClassificacaoRisco(resultado.risco());
        triagem.setInsightIa(resultado.insights());
        triagem.setProbabilidadeHigidez(BigDecimal.valueOf(resultado.probabilidadeHigidez()));
        triagem.setModeloVersao(resultado.modeloVersao());
        triagem.setFatoresXai(resultado.fatoresXai());

        // Atualiza escore no Pet
        pet.setEscoreSaude(resultado.escore());
        pet.setStatusLongevidade(String.format("Escore: %d/100 (ML P(Higidez)=%.1f%%) - Risco %s",
                resultado.escore(), resultado.probabilidadeHigidez(), resultado.risco().name()));
        petRepository.save(pet);

        ConsultaTriagem salva = triagemRepository.save(triagem);

        // 3. Registrar na Linha do Tempo Clínica (Prontuário Consolidado)
        HistoricoClinico hist = new HistoricoClinico(
                null, pet, LocalDateTime.now(),
                "TRIAGEM_PREVENTIVA",
                String.format("Triagem Concluída por Dr(a). %s. Escore Longevidade: %d/100 (ML P(Higidez)=%.1f%%, Risco %s).",
                        vet.getNomeCompleto(), resultado.escore(), resultado.probabilidadeHigidez(), resultado.risco().name()),
                "Insights da IA / XAI: " + resultado.insights() + " | Parecer: " + dto.getParecerVeterinario()
        );
        historicoClinicoRepository.save(hist);

        return salva;
    }

    public record ResultadoCalculoEscore(
            int escore,
            ClassificacaoRisco risco,
            String insights,
            double probabilidadeHigidez,
            String modeloVersao,
            String fatoresXai
    ) {
        public ResultadoCalculoEscore(int escore, ClassificacaoRisco risco, String insights) {
            this(escore, risco, insights, 75.0, PredictiveMlEngine.MODEL_VERSION, "");
        }
    }

    /**
     * Sobrecarga mantida para retrocompatibilidade com chamadas simples e testes unitários.
     */
    public ResultadoCalculoEscore calcularEscoreLongevidadeEInsights(Pet pet, BigDecimal pesoAferido, BigDecimal temperatura, Integer freqCardiaca) {
        return calcularEscoreLongevidadeEInsights(pet, pesoAferido, temperatura, freqCardiaca, "Rotina");
    }

    /**
     * Avaliação clínica híbrida:
     * 1) Avalia Guardrails Clínicos Determinísticos (Diretrizes AAHA/WSAVA) para segurança vital.
     * 2) Executa Inferência Estatística de Machine Learning (PredictiveMlEngine).
     * 3) Aplica Fail-Safe e consolida explicabilidade (XAI) e síntese SOAP.
     */
    public ResultadoCalculoEscore calcularEscoreLongevidadeEInsights(Pet pet,
                                                                   BigDecimal pesoAferido,
                                                                   BigDecimal temperatura,
                                                                   Integer freqCardiaca,
                                                                   String queixaPrincipal) {
        // 1. Guardrails Clínicos Determinísticos (Diretrizes Vitais AAHA/WSAVA)
        List<String> alertasGuardrails = avaliarGuardrailsClinicos(temperatura, freqCardiaca);

        // 2. Histórico de Check-ins recentes do Pet
        List<CheckinDiario> ultimosCheckins = checkinRepository.findByPetIdOrderByDataCheckinDesc(pet.getId());
        int totalConsultas = triagemRepository.findByPetIdOrderByDataSolicitacaoDesc(pet.getId()).size();

        // 3. Execução da Inferência de Machine Learning (CanineWellness ML Model)
        PredictiveMlEngine.ResultadoInferenciaMl inferenciaMl = mlEngine.executarInferencia(
                pet,
                pesoAferido,
                temperatura,
                freqCardiaca,
                ultimosCheckins,
                Math.max(1, totalConsultas),
                queixaPrincipal
        );

        int escoreFinal = inferenciaMl.escoreLongevidade();
        ClassificacaoRisco riscoFinal = inferenciaMl.classificacaoRisco();

        // 4. Mecanismo de Proteção Vital (Fail-Safe Override):
        // Se houver hipertermia/febre grave (> 39.5) ou hipotermia (< 37.5), o protocolo de segurança
        // clínica sobrepõe a predição estatística e eleva a prioridade de risco.
        if (!alertasGuardrails.isEmpty()) {
            if (temperatura != null && (temperatura.compareTo(new BigDecimal("39.5")) >= 0 || temperatura.compareTo(new BigDecimal("37.5")) <= 0)) {
                riscoFinal = ClassificacaoRisco.ALTO;
                escoreFinal = Math.min(escoreFinal, 45);
            } else if (riscoFinal == ClassificacaoRisco.BAIXO) {
                riscoFinal = ClassificacaoRisco.MODERADO;
                escoreFinal = Math.min(escoreFinal, 75);
            }
        }

        // 5. Montagem dos Insights Transparentes (Dual-Layer: ML + Guardrails + XAI)
        StringBuilder insightsBuilder = new StringBuilder();
        insightsBuilder.append(String.format("[ML Preditivo: P(Higidez)=%.1f%% | Escore=%d/100 (%s) | Mod=%s] ",
                inferenciaMl.probabilidadeHigidez(), escoreFinal, riscoFinal.name(), inferenciaMl.versaoModelo()));

        // Adiciona Alertas Vitais dos Guardrails se existirem
        for (String alerta : alertasGuardrails) {
            insightsBuilder.append("[").append(alerta).append("] ");
        }

        // Adiciona Riscos Fenotípicos Específicos
        for (PredictiveMlEngine.RiscoFenotipico r : inferenciaMl.riscosEspecificos()) {
            insightsBuilder.append("[").append(r.categoria()).append(": ").append(r.badge()).append(" - ").append(r.detalhes()).append("] ");
        }

        // Adiciona Top Fatores de Explicabilidade (XAI)
        insightsBuilder.append("XAI: ");
        int maxFatores = Math.min(3, inferenciaMl.fatoresXai().size());
        for (int i = 0; i < maxFatores; i++) {
            PredictiveMlEngine.FatorXai f = inferenciaMl.fatoresXai().get(i);
            insightsBuilder.append(f.impacto()).append(" ").append(f.fator());
            if (i < maxFatores - 1) insightsBuilder.append(", ");
        }

        String insightsTexto = insightsBuilder.toString().trim();
        if (insightsTexto.length() > 990) {
            insightsTexto = insightsTexto.substring(0, 987) + "...";
        }

        return new ResultadoCalculoEscore(
                escoreFinal,
                riscoFinal,
                insightsTexto,
                inferenciaMl.probabilidadeHigidez(),
                inferenciaMl.versaoModelo(),
                inferenciaMl.resumoFormatadoXai()
        );
    }

    /**
     * Camada 1: Protocolos Clínicos Determinísticos (Diretrizes AAHA / WSAVA).
     * Avalia sinais vitais e identifica desvios hemodinâmicos e termorreguladores imediatos.
     */
    public List<String> avaliarGuardrailsClinicos(BigDecimal temperatura, Integer freqCardiaca) {
        List<String> alertas = new ArrayList<>();

        if (temperatura != null) {
            if (temperatura.compareTo(new BigDecimal("39.3")) > 0) {
                alertas.add("Hipertermia/Febre Vital: " + temperatura + "°C (Diretriz WSAVA: risco de infecção/choque térmico)");
            } else if (temperatura.compareTo(new BigDecimal("37.8")) < 0) {
                alertas.add("Hipotermia Vital: " + temperatura + "°C (Diretriz WSAVA: risco de hipoperfusão sistêmica)");
            }
        }

        if (freqCardiaca != null) {
            if (freqCardiaca < 60) {
                alertas.add("Bradicardia Severa: " + freqCardiaca + " bpm (Abaixo do limiar basal)");
            } else if (freqCardiaca > 160) {
                alertas.add("Taquicardia Severa: " + freqCardiaca + " bpm (Sobrecarga miocárdica / dor)");
            }
        }

        return alertas;
    }
}
