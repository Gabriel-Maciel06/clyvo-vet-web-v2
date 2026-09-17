package com.fiap.clyvovet.service;

import com.fiap.clyvovet.dto.CheckoutRequestDto;
import com.fiap.clyvovet.model.*;
import com.fiap.clyvovet.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Checkout in-app de servicos preventivos.
 *
 * <p>A partir da V14 este fluxo deixou de ser um silo desnormalizado. Cada
 * checkout agora grava em duas camadas com papeis distintos:</p>
 * <ul>
 *   <li><strong>T_AGENDAMENTO_SERVICO</strong> — projecao de leitura que a UI do
 *       tutor consome (voucher, QR code, status de uso).</li>
 *   <li><strong>T_AGENDAMENTO / T_TRANSACAO / T_COMISSAO</strong> — ledger
 *       normalizado em 3FN com os snapshots imutaveis da V13, que e a fonte
 *       auditavel do dinheiro.</li>
 * </ul>
 *
 * <p>As duas ficam ligadas pelo FK {@code AgendamentoServico.transacao}. A
 * matematica do split nao vive mais aqui: foi extraida para
 * {@link SplitFinanceiroCalculator}, compartilhada com
 * {@link MarketplaceIntermediacaoService}.</p>
 */
@Service
public class PagamentoSplitService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoSplitService.class);

    public static final BigDecimal TAXA_TAKE_RATE_PADRAO = SplitFinanceiroCalculator.TAXA_TAKE_RATE_PADRAO;
    public static final BigDecimal PISO_REPASSE_CLINICA_PERCENTUAL = SplitFinanceiroCalculator.PISO_REPASSE_CLINICA_PERCENTUAL;
    public static final BigDecimal PARIDADE_SUBSIDIO_PLATAFORMA = SplitFinanceiroCalculator.PARIDADE_SUBSIDIO_PLATAFORMA;

    private final AgendamentoServicoRepository agendamentoRepository;
    private final PetRepository petRepository;
    private final TutorRepository tutorRepository;
    private final RecompensaTutorRepository recompensaTutorRepository;
    private final HistoricoClinicoRepository historicoClinicoRepository;
    private final PetService petService;
    private final SplitFinanceiroCalculator splitCalculator;
    private final ServicoRepository servicoRepository;
    private final AgendamentoRepository agendamentoLedgerRepository;
    private final TransacaoRepository transacaoRepository;
    private final ComissaoRepository comissaoRepository;

    public PagamentoSplitService(AgendamentoServicoRepository agendamentoRepository,
                                 PetRepository petRepository,
                                 TutorRepository tutorRepository,
                                 RecompensaTutorRepository recompensaTutorRepository,
                                 HistoricoClinicoRepository historicoClinicoRepository,
                                 PetService petService,
                                 SplitFinanceiroCalculator splitCalculator,
                                 ServicoRepository servicoRepository,
                                 AgendamentoRepository agendamentoLedgerRepository,
                                 TransacaoRepository transacaoRepository,
                                 ComissaoRepository comissaoRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.petRepository = petRepository;
        this.tutorRepository = tutorRepository;
        this.recompensaTutorRepository = recompensaTutorRepository;
        this.historicoClinicoRepository = historicoClinicoRepository;
        this.petService = petService;
        this.splitCalculator = splitCalculator;
        this.servicoRepository = servicoRepository;
        this.agendamentoLedgerRepository = agendamentoLedgerRepository;
        this.transacaoRepository = transacaoRepository;
        this.comissaoRepository = comissaoRepository;
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
            BigDecimal valorPrejuizoPlataforma,
            boolean pisoProtegidoAplicado,
            String nivelFidelidade
    ) {}

    /**
     * Calcula o split financeiro bipartite entre Clyvo (plataforma) e Clinica (receiver).
     * A regra economica esta em {@link SplitFinanceiroCalculator}; aqui so resolvemos
     * o nivel de fidelidade do tutor e o preco de tabela do servico.
     */
    public ResumoSplit calcularResumo(String tutorCpf, TipoServicoPreventivo servico) {
        RecompensaTutor recompensa = recompensaTutorRepository.findByTutorCpf(tutorCpf).orElse(null);
        int descontoPercentual = (recompensa != null && recompensa.getDescontoPercentual() != null)
                ? recompensa.getDescontoPercentual() : 0;
        String nivel = (recompensa != null && recompensa.getNivelFidelidade() != null)
                ? recompensa.getNivelFidelidade() : "BRONZE";

        SplitFinanceiroCalculator.Resultado r = splitCalculator.calcular(servico.getValorBase(), descontoPercentual);

        return new ResumoSplit(
                r.valorOriginal(),
                r.descontoPercentual(),
                r.valorDesconto(),
                r.valorFinal(),
                r.taxaClyvoPercentual(),
                r.taxaEfetivaPercentual(),
                r.valorSubsidioClyvo(),
                r.valorDescontoClinica(),
                r.valorComissaoClyvo(),
                r.valorRepasseClinica(),
                r.valorPrejuizoPlataforma(),
                r.pisoProtegidoAplicado(),
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

        // Catalogo real: resolve a clinica ofertante em vez de gravar um nome fixo.
        Servico servicoCatalogo = servicoRepository
                .findFirstByCodigoServicoAppAndAtivoTrue(servico.name())
                .orElse(null);
        Clinica clinica = (servicoCatalogo != null) ? servicoCatalogo.getClinica() : null;

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
        agendamento.setValorPrejuizoPlataforma(split.valorPrejuizoPlataforma());
        agendamento.setStatusPagamento(StatusPagamento.PAGO_CONFIRMADO);
        agendamento.setMetodoPagamento(dto.getMetodoPagamento());
        agendamento.setCodigoVoucher(codigoVoucher);
        agendamento.setQrCodeHash(qrHash);
        if (clinica != null && clinica.getNomeCnpj() != null) {
            agendamento.setClinicaParceira(clinica.getNomeCnpj());
        }
        agendamento.setDataCriacao(LocalDateTime.now());
        agendamento.setDataPagamento(LocalDateTime.now());
        agendamento.setObservacoes(dto.getObservacoes());

        // Espelha a operacao no ledger normalizado 3FN com os snapshots da V13.
        Transacao transacao = registrarNoLedger(pet, tutor, servicoCatalogo, clinica, split, dto, codigoVoucher, qrHash);
        agendamento.setTransacao(transacao);

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

    /**
     * Grava a operacao no livro-razao normalizado. Os campos {@code snapshot_*}
     * registram os valores vigentes no ato da captura e nunca sao recalculados:
     * se o preco do catalogo ou a taxa da clinica mudarem amanha, o historico
     * ja liquidado permanece auditavel como estava hoje.
     *
     * <p>Se o catalogo ainda nao tiver a linha correspondente ao servico, o
     * checkout do tutor nao pode falhar por isso — o voucher e emitido e o
     * registro no ledger e apenas omitido, com log de alerta.</p>
     */
    private Transacao registrarNoLedger(Pet pet,
                                        Tutor tutor,
                                        Servico servicoCatalogo,
                                        Clinica clinica,
                                        ResumoSplit split,
                                        CheckoutRequestDto dto,
                                        String codigoVoucher,
                                        String qrHash) {
        if (servicoCatalogo == null || clinica == null) {
            log.warn("Serviço '{}' sem linha correspondente em T_SERVICO: voucher {} emitido sem registro no ledger normalizado.",
                    dto.getTipoServico(), codigoVoucher);
            return null;
        }

        Agendamento agendamentoLedger = new Agendamento();
        agendamentoLedger.setPet(pet);
        agendamentoLedger.setTutor(tutor);
        agendamentoLedger.setClinica(clinica);
        agendamentoLedger.setServico(servicoCatalogo);
        agendamentoLedger.setDataHoraAgendamento(LocalDateTime.now().plusDays(2));
        agendamentoLedger.setStatusAgendamento(StatusAgendamento.CONFIRMADO);
        agendamentoLedger.setObservacoes(dto.getObservacoes());
        agendamentoLedger.setDataCriacao(LocalDateTime.now());
        agendamentoLedger = agendamentoLedgerRepository.save(agendamentoLedger);

        Transacao transacao = new Transacao();
        transacao.setAgendamento(agendamentoLedger);
        transacao.setCodigoTransacaoGateway("GW-CHECKOUT-" + UUID.randomUUID());
        transacao.setMetodoPagamento(dto.getMetodoPagamento() != null ? dto.getMetodoPagamento() : "PIX");
        transacao.setStatusTransacao(StatusTransacao.PAGO);
        transacao.setValorBruto(split.valorOriginal());
        transacao.setValorDescontoFidelidade(split.valorDesconto());
        transacao.setValorLiquidoPago(split.valorFinal());
        transacao.setCodigoVoucher(codigoVoucher);
        transacao.setQrCodeHash(qrHash);
        transacao.setVoucherUtilizado(false);
        transacao.setDataCriacao(LocalDateTime.now());
        transacao.setDataPagamento(LocalDateTime.now());
        transacao.setSnapshotPrecoCatalogo(servicoCatalogo.getPrecoBase());
        transacao.setSnapshotTaxaDescontoPct(BigDecimal.valueOf(split.descontoPercentual()).setScale(2));
        transacao.setSnapshotNivelFidelidade(split.nivelFidelidade());
        transacao = transacaoRepository.save(transacao);

        Comissao comissao = new Comissao();
        comissao.setTransacao(transacao);
        comissao.setClinica(clinica);
        comissao.setPercentualTakeRate(split.taxaClyvoPercentual());
        comissao.setValorSubsidioPlataforma(split.valorSubsidioClyvo());
        comissao.setTaxaEfetivaPercentual(split.taxaEfetivaPercentual());
        comissao.setValorComissaoPlataforma(split.valorComissaoClyvo());
        comissao.setValorRepasseClinica(split.valorRepasseClinica());
        comissao.setValorPrejuizoPlataforma(split.valorPrejuizoPlataforma());
        comissao.setPisoProtegidoAplicado(split.pisoProtegidoAplicado());
        comissao.setStatusRepasse(StatusRepasseComissao.RETIDO_ESCROW);
        comissao.setDataPrevisaoRepasse(LocalDate.now().plusDays(3));
        comissao.setSnapshotTaxaComissaoVigente(
                clinica.getTaxaComissaoCustomizada() != null
                        ? clinica.getTaxaComissaoCustomizada()
                        : SplitFinanceiroCalculator.TAXA_TAKE_RATE_PADRAO);
        comissao.setSnapshotPisoRepassePct(SplitFinanceiroCalculator.PISO_REPASSE_CLINICA_PERCENTUAL);
        comissaoRepository.save(comissao);

        return transacao;
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

        baixarNoLedger(agendamento);

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

    /**
     * Propaga a baixa do voucher para o ledger: marca a transacao como utilizada,
     * conclui o agendamento e libera a comissao retida em escrow para repasse.
     * Sem isso o livro-razao ficaria eternamente em RETIDO_ESCROW enquanto a UI
     * ja mostra o atendimento como concluido.
     */
    private void baixarNoLedger(AgendamentoServico agendamento) {
        Transacao transacao = agendamento.getTransacao();
        if (transacao == null) {
            return; // voucher legado, anterior a V14
        }

        transacao.setVoucherUtilizado(true);
        transacao.setDataUtilizacaoVoucher(LocalDateTime.now());
        transacaoRepository.save(transacao);

        Agendamento agendamentoLedger = transacao.getAgendamento();
        if (agendamentoLedger != null) {
            agendamentoLedger.setStatusAgendamento(StatusAgendamento.REALIZADO);
            agendamentoLedgerRepository.save(agendamentoLedger);
        }

        comissaoRepository.findByTransacaoId(transacao.getId()).ifPresent(comissao -> {
            comissao.setStatusRepasse(StatusRepasseComissao.LIBERADO_APOS_ATENDIMENTO);
            comissaoRepository.save(comissao);
        });
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
