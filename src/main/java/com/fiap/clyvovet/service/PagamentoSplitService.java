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

        // 1. Modelo Tripartite: Co-Financiamento Paritário do Desconto (50% Clyvo / 50% Clínica)
        BigDecimal valorSubsidioClyvo = valorDesconto.multiply(PARIDADE_SUBSIDIO_PLATAFORMA)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorDescontoClinica = valorDesconto.subtract(valorSubsidioClyvo);

        // 2. Comissão Contratual Base da Clyvo (15% sobre o valor de tabela)
        BigDecimal comissaoBase = valorOriginal.multiply(TAXA_TAKE_RATE_PADRAO)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // 3. Aplicação do Subsídio no Take-Rate da Clyvo
        BigDecimal valorComissaoClyvo = comissaoBase.subtract(valorSubsidioClyvo);

        // 4. Repasse Líquido à Clínica: Tutor paga valorFinal; Clyvo retém valorComissaoClyvo
        BigDecimal valorRepasseClinica = valorFinal.subtract(valorComissaoClyvo);

        // 5. Floor Protection: Garantia de repasse mínimo de 75% da tabela
        BigDecimal pisoMinimo = valorOriginal.multiply(PISO_REPASSE_CLINICA_PERCENTUAL)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        boolean pisoAplicado = false;
        if (valorRepasseClinica.compareTo(pisoMinimo) < 0) {
            BigDecimal diferencaPiso = pisoMinimo.subtract(valorRepasseClinica);
            valorRepasseClinica = pisoMinimo;
            valorComissaoClyvo = valorComissaoClyvo.subtract(diferencaPiso).max(BigDecimal.ZERO);
            valorSubsidioClyvo = valorSubsidioClyvo.add(diferencaPiso);
            pisoAplicado = true;
        }

        // 6. Taxa Efetiva Retida pela Clyvo (comissao retida / valor original)
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
