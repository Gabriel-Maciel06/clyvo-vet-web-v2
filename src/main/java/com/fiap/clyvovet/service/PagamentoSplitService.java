package com.fiap.clyvovet.service;

import com.fiap.clyvovet.dto.CheckoutRequestDto;
import com.fiap.clyvovet.model.*;
import com.fiap.clyvovet.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PagamentoSplitService {

    public static final BigDecimal TAXA_TAKE_RATE_PADRAO = new BigDecimal("15.00");
    public static final BigDecimal PISO_REPASSE_CLINICA_PERCENTUAL = new BigDecimal("75.00");
    public static final BigDecimal PARIDADE_SUBSIDIO_PLATAFORMA = new BigDecimal("0.50");

    private final AgendamentoServicoRepository agendamentoRepository;
    private final PetRepository petRepository;
    private final TutorRepository tutorRepository;
    private final RecompensaTutorRepository recompensaTutorRepository;
    private final HistoricoClinicoRepository historicoClinicoRepository;
    private final PetService petService;

    public PagamentoSplitService(AgendamentoServicoRepository agendamentoRepository,
                                 PetRepository petRepository,
                                 TutorRepository tutorRepository,
                                 RecompensaTutorRepository recompensaTutorRepository,
                                 HistoricoClinicoRepository historicoClinicoRepository,
                                 PetService petService) {
        this.agendamentoRepository = agendamentoRepository;
        this.petRepository = petRepository;
        this.tutorRepository = tutorRepository;
        this.recompensaTutorRepository = recompensaTutorRepository;
        this.historicoClinicoRepository = historicoClinicoRepository;
        this.petService = petService;
    }

    public record ResumoSplit(
            BigDecimal valorOriginal,
            int descontoPercentual,
            BigDecimal valorDesconto,
            BigDecimal valorFinal,
            BigDecimal taxaClyvoPercentual,
            BigDecimal taxaEfetivaPercentual,
            BigDecimal valorSubsidioClyvo,
            BigDecimal valorDescontoClinica,
            BigDecimal valorComissaoClyvo,
            BigDecimal valorRepasseClinica,
            boolean pisoProtegidoAplicado,
            String nivelFidelidade
    ) {}

    /**
     * Calcula o split financeiro bipartite entre Clyvo (plataforma) e Clínica (receiver).
     *
     * <p><strong>Modelo: Split Bipartite com Co-financiamento de Subsídio</strong><br>
     * O tutor é o <em>payer</em> (pagador). Os <em>receivers</em> são dois: Clyvo e Clínica.
     * O subsídio é um lançamento contábil interno da Clyvo (abate no seu take-rate contratual),
     * não configura um terceiro recebedor — o split permanece estritamente bipartite.</p>
     *
     * <p><strong>Regra de Precedência do Piso (Priority Rule — piso limita a Clyvo, nunca o tutor):</strong></p>
     * <ol>
     *   <li>Calcular repasse preliminar = valorPagoTutor - (comissaoBase - subsidioClyvo).</li>
     *   <li>Verificar o piso contratual: repasse >= 75% * valorOriginal?</li>
     *   <li><em>Se sim:</em> modelo co-financiado 50/50 é aplicado na íntegra.</li>
     *   <li><em>Se não (colisão):</em> a Clyvo absorve 100% do excedente restante,
     *       reduzindo seu take-rate retido até zero. O desconto integral do tutor é preservado.
     *       A clínica recebe exatamente o piso de 75% do valor de tabela.
     *       O piso limita a margem da <strong>Clyvo</strong>, nunca o desconto do tutor.</li>
     * </ol>
     *
     * @param tutorCpf CPF do tutor para busca do nível de fidelidade e desconto aplicável.
     * @param servico  Enum do serviço preventivo com o valor de tabela (valorBase).
     * @return {@link ResumoSplit} imutável com todos os valores nominais calculados.
     */
    public ResumoSplit calcularResumo(String tutorCpf, TipoServicoPreventivo servico) {
        RecompensaTutor recompensa = recompensaTutorRepository.findByTutorCpf(tutorCpf).orElse(null);
        int descontoPercentual = (recompensa != null && recompensa.getDescontoPercentual() != null)
                ? recompensa.getDescontoPercentual() : 0;
        String nivel = (recompensa != null && recompensa.getNivelFidelidade() != null)
                ? recompensa.getNivelFidelidade() : "BRONZE";

        BigDecimal valorOriginal = servico.getValorBase();
        BigDecimal valorDesconto = valorOriginal.multiply(BigDecimal.valueOf(descontoPercentual))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal valorFinal = valorOriginal.subtract(valorDesconto);

        // PASSO 1 — Co-financiamento de Subsídio: Clyvo banca 50% do desconto (lançamento interno de abate de take-rate)
        // A clínica absorve os outros 50%, restrito ao Yield Management de capacidade ociosa.
        BigDecimal valorSubsidioClyvo = valorDesconto.multiply(PARIDADE_SUBSIDIO_PLATAFORMA)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorDescontoClinica = valorDesconto.subtract(valorSubsidioClyvo);

        // PASSO 2 — Comissão Contratual Base da Clyvo: 15% sobre o valor de tabela (snapshot imutável)
        BigDecimal comissaoBase = valorOriginal.multiply(TAXA_TAKE_RATE_PADRAO)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // PASSO 3 — Take-rate Líquido: comissão base menos o subsídio interno já concedido
        BigDecimal valorComissaoClyvo = comissaoBase.subtract(valorSubsidioClyvo);

        // PASSO 4 — Repasse Bipartite Preliminar: o payer (tutor) paga valorFinal;
        //           a Clyvo retém valorComissaoClyvo; o restante vai para a Clínica.
        BigDecimal valorRepasseClinica = valorFinal.subtract(valorComissaoClyvo);

        // PASSO 5 — Priority Rule (Piso de 75%): Se o repasse preliminar viola o piso contratual,
        //           a Clyvo absorve 100% do excedente (reduz seu take-rate até zero).
        //           O desconto do tutor é SEMPRE preservado integralmente.
        BigDecimal pisoMinimo = valorOriginal.multiply(PISO_REPASSE_CLINICA_PERCENTUAL)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        boolean pisoAplicado = false;
        if (valorRepasseClinica.compareTo(pisoMinimo) < 0) {
            BigDecimal excedente = pisoMinimo.subtract(valorRepasseClinica);
            valorRepasseClinica = pisoMinimo;
            valorComissaoClyvo = valorComissaoClyvo.subtract(excedente).max(BigDecimal.ZERO);
            valorSubsidioClyvo = valorSubsidioClyvo.add(excedente); // Clyvo absorve a diferença integralmente
            pisoAplicado = true;
        }

        // PASSO 6 — Taxa Efetiva: percentual real retido pela Clyvo após subsidio e priority rule
        BigDecimal taxaEfetiva = valorOriginal.compareTo(BigDecimal.ZERO) > 0
                ? valorComissaoClyvo.multiply(BigDecimal.valueOf(100)).divide(valorOriginal, 2, RoundingMode.HALF_UP)
                : TAXA_TAKE_RATE_PADRAO;

        return new ResumoSplit(
                valorOriginal,
                descontoPercentual,
                valorDesconto,
                valorFinal,
                TAXA_TAKE_RATE_PADRAO,
                taxaEfetiva,
                valorSubsidioClyvo,
                valorDescontoClinica,
                valorComissaoClyvo,
                valorRepasseClinica,
                pisoAplicado,
                nivel
        );
    }

    @Transactional
    public AgendamentoServico processarCheckout(CheckoutRequestDto dto, String usernameTutor) {
        Pet pet = petRepository.findById(dto.getPetId())
                .orElseThrow(() -> new IllegalArgumentException("Pet não encontrado: " + dto.getPetId()));
        petService.validarPropriedade(pet, usernameTutor);

        Tutor tutor = tutorRepository.findByUsuarioUsername(usernameTutor)
                .orElseThrow(() -> new IllegalArgumentException("Tutor não encontrado para o usuário: " + usernameTutor));

        TipoServicoPreventivo servico = dto.getTipoServico();
        ResumoSplit split = calcularResumo(tutor.getCpf(), servico);

        String codigoVoucher = "CLYVO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String qrHash = "QR-CLYVO-" + pet.getId() + "-" + System.currentTimeMillis();

        AgendamentoServico agendamento = new AgendamentoServico();
        agendamento.setPet(pet);
        agendamento.setTutor(tutor);
        agendamento.setTipoServico(servico);
        agendamento.setDescricaoServico(servico.getTitulo());
        agendamento.setValorOriginal(split.valorOriginal());
        agendamento.setDescontoFidelidade(split.valorDesconto());
        agendamento.setValorFinal(split.valorFinal());
        agendamento.setTaxaClyvoPercentual(split.taxaClyvoPercentual());
        agendamento.setValorComissaoClyvo(split.valorComissaoClyvo());
        agendamento.setValorSubsidioClyvo(split.valorSubsidioClyvo());
        agendamento.setValorDescontoClinica(split.valorDescontoClinica());
        agendamento.setTaxaEfetivaPercentual(split.taxaEfetivaPercentual());
        agendamento.setValorRepasseClinica(split.valorRepasseClinica());
        agendamento.setStatusPagamento(StatusPagamento.PAGO_CONFIRMADO);
        agendamento.setMetodoPagamento(dto.getMetodoPagamento());
        agendamento.setCodigoVoucher(codigoVoucher);
        agendamento.setQrCodeHash(qrHash);
        agendamento.setClinicaParceira("Hospital Veterinário Central Parceiro Clyvo");
        agendamento.setDataCriacao(LocalDateTime.now());
        agendamento.setDataPagamento(LocalDateTime.now());
        agendamento.setObservacoes(dto.getObservacoes());

        AgendamentoServico salvo = agendamentoRepository.save(agendamento);

        // Bonificação de fidelidade (+50 pontos no Clyvo Rewards pela contratação preventiva)
        RecompensaTutor recompensa = recompensaTutorRepository.findByTutorCpf(tutor.getCpf()).orElse(null);
        if (recompensa != null) {
            recompensa.adicionarPontos(50);
            recompensaTutorRepository.save(recompensa);
        }

        // Registro imediato na Linha do Tempo Clínica do Pet
        HistoricoClinico evento = new HistoricoClinico(
                null, pet, LocalDateTime.now(),
                "VOUCHER_PREVENTIVO",
                String.format("Voucher Emitido: %s (Cód: %s)", servico.getTitulo(), codigoVoucher),
                String.format("Pago in-app via %s. Valor: R$ %s (Desconto Fidelidade: R$ %s). Split Clyvo Take-rate: R$ %s retido no gateway. Apresente o QR Code no hospital parceiro.",
                        dto.getMetodoPagamento(), split.valorFinal(), split.valorDesconto(), split.valorComissaoClyvo())
        );
        historicoClinicoRepository.save(evento);

        return salvo;
    }

    @Transactional
    public AgendamentoServico validarEBaixarVoucher(String codigoVoucher, String usernameVeterinario) {
        AgendamentoServico agendamento = agendamentoRepository.findByCodigoVoucher(codigoVoucher.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Voucher não localizado com o código: " + codigoVoucher));

        if (agendamento.getStatusPagamento() == StatusPagamento.UTILIZADO_NA_CLINICA) {
            throw new IllegalStateException("Este voucher já foi utilizado e liquidado na clínica.");
        }
        if (agendamento.getStatusPagamento() == StatusPagamento.CANCELADO) {
            throw new IllegalStateException("Este voucher foi cancelado e não é válido para atendimento.");
        }

        agendamento.setStatusPagamento(StatusPagamento.UTILIZADO_NA_CLINICA);
        agendamento.setDataUtilizacao(LocalDateTime.now());
        AgendamentoServico atualizado = agendamentoRepository.save(agendamento);

        // Registro de comparecimento e liquidação na timeline
        HistoricoClinico evento = new HistoricoClinico(
                null, agendamento.getPet(), LocalDateTime.now(),
                "ATENDIMENTO_CONCLUIDO",
                String.format("Atendimento Realizado via Voucher %s", agendamento.getCodigoVoucher()),
                String.format("Procedimento '%s' realizado com sucesso pela clínica parceira. Validação efetuada pelo profissional: %s.",
                        agendamento.getDescricaoServico(), usernameVeterinario)
        );
        historicoClinicoRepository.save(evento);

        return atualizado;
    }

    public AgendamentoServico buscarPorId(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado: " + id));
    }

    public AgendamentoServico buscarPorCodigoVoucher(String codigoVoucher) {
        return agendamentoRepository.findByCodigoVoucher(codigoVoucher.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Voucher não encontrado com código: " + codigoVoucher));
    }

    public List<AgendamentoServico> listarPorTutor(String usernameTutor) {
        Tutor tutor = tutorRepository.findByUsuarioUsername(usernameTutor)
                .orElseThrow(() -> new IllegalArgumentException("Tutor não encontrado: " + usernameTutor));
        return agendamentoRepository.findByTutorCpfOrderByDataCriacaoDesc(tutor.getCpf());
    }

    public List<AgendamentoServico> listarPorPet(Long petId) {
        return agendamentoRepository.findByPetIdOrderByDataCriacaoDesc(petId);
    }
}
