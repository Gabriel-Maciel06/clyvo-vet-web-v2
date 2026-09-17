package com.fiap.clyvovet.service;

import com.fiap.clyvovet.model.*;
import com.fiap.clyvovet.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MarketplaceIntermediacaoService {

    private final ServicoRepository servicoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final TransacaoRepository transacaoRepository;
    private final ComissaoRepository comissaoRepository;
    private final PetRepository petRepository;
    private final TutorRepository tutorRepository;
    private final RecompensaTutorRepository recompensaTutorRepository;
    private final PetService petService;
    private final SplitFinanceiroCalculator splitCalculator;

    public MarketplaceIntermediacaoService(ServicoRepository servicoRepository,
                                           AgendamentoRepository agendamentoRepository,
                                           TransacaoRepository transacaoRepository,
                                           ComissaoRepository comissaoRepository,
                                           PetRepository petRepository,
                                           TutorRepository tutorRepository,
                                           RecompensaTutorRepository recompensaTutorRepository,
                                           PetService petService,
                                           SplitFinanceiroCalculator splitCalculator) {
        this.servicoRepository = servicoRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.transacaoRepository = transacaoRepository;
        this.comissaoRepository = comissaoRepository;
        this.petRepository = petRepository;
        this.tutorRepository = tutorRepository;
        this.recompensaTutorRepository = recompensaTutorRepository;
        this.petService = petService;
        this.splitCalculator = splitCalculator;
    }

    public record ContratoIntermediacaoDto(
            Agendamento agendamento,
            Transacao transacao,
            Comissao comissao
    ) {}

    @Transactional
    public ContratoIntermediacaoDto contratarServicoNoMarketplace(
            Long petId,
            Long servicoId,
            LocalDateTime dataHoraAgendamento,
            String metodoPagamento,
            String observacoes,
            String usernameTutor) {

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("Pet não encontrado com ID: " + petId));
        petService.validarPropriedade(pet, usernameTutor);

        Tutor tutor = tutorRepository.findByUsuarioUsername(usernameTutor)
                .orElseThrow(() -> new IllegalArgumentException("Tutor não encontrado para usuário: " + usernameTutor));

        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado com ID: " + servicoId));

        Clinica clinica = servico.getClinica();
        if (clinica == null || Boolean.FALSE.equals(clinica.getAtivo())) {
            throw new IllegalStateException("Clínica indisponível para contratação no marketplace.");
        }

        // 1. Desconto de fidelidade aplicavel a este servico
        int percentualDesconto = 0;
        String nivelFidelidade = "BRONZE";
        if (Boolean.TRUE.equals(servico.getPermiteDescontoFidelidade())) {
            RecompensaTutor recompensa = recompensaTutorRepository.findByTutorCpf(tutor.getCpf()).orElse(null);
            if (recompensa != null) {
                if (recompensa.getDescontoPercentual() != null) {
                    percentualDesconto = recompensa.getDescontoPercentual();
                }
                if (recompensa.getNivelFidelidade() != null) {
                    nivelFidelidade = recompensa.getNivelFidelidade();
                }
            }
        }

        // 2. Split: a economia vive num unico lugar (SplitFinanceiroCalculator),
        //    compartilhado com o checkout in-app. Antes esta conta estava duplicada
        //    aqui e em PagamentoSplitService, com risco de divergirem em silencio.
        BigDecimal taxaContratual = clinica.getTaxaComissaoCustomizada() != null
                ? clinica.getTaxaComissaoCustomizada()
                : SplitFinanceiroCalculator.TAXA_TAKE_RATE_PADRAO;

        SplitFinanceiroCalculator.Resultado split =
                splitCalculator.calcular(servico.getPrecoBase(), percentualDesconto, taxaContratual);

        BigDecimal valorBruto = split.valorOriginal();
        BigDecimal valorDesconto = split.valorDesconto();
        BigDecimal valorLiquidoPago = split.valorFinal();
        BigDecimal valorSubsidioClyvo = split.valorSubsidioClyvo();
        BigDecimal valorComissaoClyvo = split.valorComissaoClyvo();
        BigDecimal valorRepasseClinica = split.valorRepasseClinica();
        BigDecimal taxaEfetiva = split.taxaEfetivaPercentual();
        boolean pisoAplicado = split.pisoProtegidoAplicado();

        // 8. Criação e Persistência do Agendamento
        Agendamento agendamento = new Agendamento();
        agendamento.setPet(pet);
        agendamento.setTutor(tutor);
        agendamento.setClinica(clinica);
        agendamento.setServico(servico);
        agendamento.setDataHoraAgendamento(dataHoraAgendamento);
        agendamento.setStatusAgendamento(StatusAgendamento.CONFIRMADO);
        agendamento.setObservacoes(observacoes);
        agendamento.setDataCriacao(LocalDateTime.now());
        agendamento = agendamentoRepository.save(agendamento);

        // 9. Criação da Transação Financeira In-App (Captura do Gateway)
        String codigoVoucher = "VCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String qrCodeHash = "QR-MKT-" + agendamento.getId() + "-" + System.currentTimeMillis();
        String codigoGateway = "GW-TRANS-" + UUID.randomUUID();

        Transacao transacao = new Transacao();
        transacao.setAgendamento(agendamento);
        transacao.setCodigoTransacaoGateway(codigoGateway);
        transacao.setMetodoPagamento(metodoPagamento != null ? metodoPagamento : "PIX");
        transacao.setStatusTransacao(StatusTransacao.PAGO);
        transacao.setValorBruto(valorBruto);
        transacao.setValorDescontoFidelidade(valorDesconto);
        transacao.setValorLiquidoPago(valorLiquidoPago);
        transacao.setCodigoVoucher(codigoVoucher);
        transacao.setQrCodeHash(qrCodeHash);
        transacao.setVoucherUtilizado(false);
        transacao.setDataCriacao(LocalDateTime.now());
        transacao.setDataPagamento(LocalDateTime.now());
        transacao.setSnapshotPrecoCatalogo(servico.getPrecoBase());
        transacao.setSnapshotTaxaDescontoPct(BigDecimal.valueOf(percentualDesconto).setScale(2));
        transacao.setSnapshotNivelFidelidade(nivelFidelidade);
        transacao = transacaoRepository.save(transacao);

        // 10. Criação da Comissão / Split em Custódia (Escrow)
        Comissao comissao = new Comissao();
        comissao.setTransacao(transacao);
        comissao.setClinica(clinica);
        comissao.setPercentualTakeRate(taxaContratual);
        comissao.setValorSubsidioPlataforma(valorSubsidioClyvo);
        comissao.setTaxaEfetivaPercentual(taxaEfetiva);
        comissao.setValorComissaoPlataforma(valorComissaoClyvo);
        comissao.setValorRepasseClinica(valorRepasseClinica);
        comissao.setPisoProtegidoAplicado(pisoAplicado);
        comissao.setStatusRepasse(StatusRepasseComissao.RETIDO_ESCROW);
        comissao.setDataPrevisaoRepasse(LocalDate.now().plusDays(3));
        comissao.setValorPrejuizoPlataforma(split.valorPrejuizoPlataforma());
        comissao.setSnapshotTaxaComissaoVigente(taxaContratual);
        comissao.setSnapshotPisoRepassePct(SplitFinanceiroCalculator.PISO_REPASSE_CLINICA_PERCENTUAL);
        comissao = comissaoRepository.save(comissao);

        return new ContratoIntermediacaoDto(agendamento, transacao, comissao);
    }

    @Transactional
    public Transacao validarVoucherEAtendimento(String codigoVoucher) {
        Transacao transacao = transacaoRepository.findByCodigoVoucher(codigoVoucher)
                .orElseThrow(() -> new IllegalArgumentException("Voucher digital não encontrado: " + codigoVoucher));

        if (Boolean.TRUE.equals(transacao.getVoucherUtilizado())) {
            throw new IllegalStateException("Voucher já foi utilizado anteriormente em: " + transacao.getDataUtilizacaoVoucher());
        }

        transacao.setVoucherUtilizado(true);
        transacao.setDataUtilizacaoVoucher(LocalDateTime.now());
        transacao = transacaoRepository.save(transacao);

        // Atualiza Agendamento para REALIZADO
        Agendamento agendamento = transacao.getAgendamento();
        if (agendamento != null) {
            agendamento.setStatusAgendamento(StatusAgendamento.REALIZADO);
            agendamentoRepository.save(agendamento);
        }

        // Libera a comissão retida em Escrow para repasse à clínica
        Comissao comissao = comissaoRepository.findByTransacaoId(transacao.getId()).orElse(null);
        if (comissao != null) {
            comissao.setStatusRepasse(StatusRepasseComissao.LIBERADO_APOS_ATENDIMENTO);
            comissaoRepository.save(comissao);
        }

        return transacao;
    }

    @Transactional
    public Comissao liquidarRepasseClinica(Long comissaoId) {
        Comissao comissao = comissaoRepository.findById(comissaoId)
                .orElseThrow(() -> new IllegalArgumentException("Comissão não encontrada com ID: " + comissaoId));

        if (comissao.getStatusRepasse() != StatusRepasseComissao.LIBERADO_APOS_ATENDIMENTO) {
            throw new IllegalStateException("Apenas repasses liberados após atendimento podem ser liquidados. Status atual: " + comissao.getStatusRepasse());
        }

        comissao.setStatusRepasse(StatusRepasseComissao.PAGO_LIQUIDADO);
        comissao.setDataLiquidacaoRepasse(LocalDateTime.now());
        return comissaoRepository.save(comissao);
    }
}
