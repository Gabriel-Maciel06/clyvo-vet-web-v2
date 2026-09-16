package com.fiap.clyvovet.service;

import com.fiap.clyvovet.model.*;
import com.fiap.clyvovet.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    public MarketplaceIntermediacaoService(ServicoRepository servicoRepository,
                                           AgendamentoRepository agendamentoRepository,
                                           TransacaoRepository transacaoRepository,
                                           ComissaoRepository comissaoRepository,
                                           PetRepository petRepository,
                                           TutorRepository tutorRepository,
                                           RecompensaTutorRepository recompensaTutorRepository,
                                           PetService petService) {
        this.servicoRepository = servicoRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.transacaoRepository = transacaoRepository;
        this.comissaoRepository = comissaoRepository;
        this.petRepository = petRepository;
        this.tutorRepository = tutorRepository;
        this.recompensaTutorRepository = recompensaTutorRepository;
        this.petService = petService;
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

        // 1. Cálculo de Desconto de Fidelidade e Valores
        int percentualDesconto = 0;
        if (Boolean.TRUE.equals(servico.getPermiteDescontoFidelidade())) {
            RecompensaTutor recompensa = recompensaTutorRepository.findByTutorCpf(tutor.getCpf()).orElse(null);
            if (recompensa != null && recompensa.getDescontoPercentual() != null) {
                percentualDesconto = recompensa.getDescontoPercentual();
            }
        }

        BigDecimal valorBruto = servico.getPrecoBase();
        BigDecimal valorDesconto = valorBruto.multiply(BigDecimal.valueOf(percentualDesconto))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal valorLiquidoPago = valorBruto.subtract(valorDesconto);

        // 2. Cálculo do Split Contábil (Take-Rate retido na fonte)
        BigDecimal taxaTakeRate = clinica.getTaxaComissaoCustomizada() != null 
                ? clinica.getTaxaComissaoCustomizada() 
                : new BigDecimal("15.00");

        BigDecimal valorComissaoClyvo = valorLiquidoPago.multiply(taxaTakeRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal valorRepasseClinica = valorLiquidoPago.subtract(valorComissaoClyvo);

        // 3. Criação e Persistência do Agendamento
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

        // 4. Criação da Transação Financeira In-App (Captura do Gateway)
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
        transacao = transacaoRepository.save(transacao);

        // 5. Criação da Comissão / Split em Custódia (Escrow)
        Comissao comissao = new Comissao();
        comissao.setTransacao(transacao);
        comissao.setClinica(clinica);
        comissao.setPercentualTakeRate(taxaTakeRate);
        comissao.setValorComissaoPlataforma(valorComissaoClyvo);
        comissao.setValorRepasseClinica(valorRepasseClinica);
        comissao.setStatusRepasse(StatusRepasseComissao.RETIDO_ESCROW);
        comissao.setDataPrevisaoRepasse(LocalDate.now().plusDays(3));
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
