package com.fiap.clyvovet.controller;

import com.fiap.clyvovet.dto.CheckoutRequestDto;
import com.fiap.clyvovet.model.*;
import com.fiap.clyvovet.repository.RecompensaTutorRepository;
import com.fiap.clyvovet.repository.TutorRepository;
import com.fiap.clyvovet.service.PagamentoSplitService;
import com.fiap.clyvovet.service.PetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/servicos")
public class CheckoutController {

    private final PagamentoSplitService pagamentoSplitService;
    private final PetService petService;
    private final TutorRepository tutorRepository;
    private final RecompensaTutorRepository recompensaTutorRepository;

    public CheckoutController(PagamentoSplitService pagamentoSplitService,
                              PetService petService,
                              TutorRepository tutorRepository,
                              RecompensaTutorRepository recompensaTutorRepository) {
        this.pagamentoSplitService = pagamentoSplitService;
        this.petService = petService;
        this.tutorRepository = tutorRepository;
        this.recompensaTutorRepository = recompensaTutorRepository;
    }

    @PreAuthorize("hasRole('TUTOR')")
    @GetMapping
    public String catalogoServicos(Authentication auth, Model model) {
        List<Pet> pets = petService.listarPorTutor(auth.getName());
        Tutor tutor = tutorRepository.findByUsuarioUsername(auth.getName()).orElse(null);
        RecompensaTutor recompensa = (tutor != null)
                ? recompensaTutorRepository.findByTutorCpf(tutor.getCpf()).orElse(null)
                : null;

        model.addAttribute("servicos", TipoServicoPreventivo.values());
        model.addAttribute("pets", pets);
        model.addAttribute("recompensa", recompensa);
        return "servicos/catalogo";
    }

    @PreAuthorize("hasRole('TUTOR')")
    @GetMapping("/checkout")
    public String telaCheckout(@RequestParam(value = "petId", required = false) Long petId,
                               @RequestParam(value = "tipoServico", required = false) TipoServicoPreventivo tipoServico,
                               Authentication auth,
                               Model model) {
        List<Pet> pets = petService.listarPorTutor(auth.getName());
        Tutor tutor = tutorRepository.findByUsuarioUsername(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Tutor não encontrado: " + auth.getName()));

        CheckoutRequestDto dto = new CheckoutRequestDto();
        if (petId != null) {
            dto.setPetId(petId);
        } else if (!pets.isEmpty()) {
            dto.setPetId(pets.get(0).getId());
        }

        if (tipoServico != null) {
            dto.setTipoServico(tipoServico);
        } else {
            dto.setTipoServico(TipoServicoPreventivo.CONSULTA_PREVENTIVA);
        }

        PagamentoSplitService.ResumoSplit split = pagamentoSplitService.calcularResumo(tutor.getCpf(), dto.getTipoServico());

        model.addAttribute("checkoutDto", dto);
        model.addAttribute("pets", pets);
        model.addAttribute("servicos", TipoServicoPreventivo.values());
        model.addAttribute("split", split);
        model.addAttribute("tutor", tutor);
        return "servicos/checkout";
    }

    @PreAuthorize("hasRole('TUTOR')")
    @PostMapping("/checkout")
    public String processarCheckout(@Valid @ModelAttribute("checkoutDto") CheckoutRequestDto dto,
                                    BindingResult bindingResult,
                                    Authentication auth,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            List<Pet> pets = petService.listarPorTutor(auth.getName());
            Tutor tutor = tutorRepository.findByUsuarioUsername(auth.getName()).orElse(null);
            PagamentoSplitService.ResumoSplit split = (tutor != null && dto.getTipoServico() != null)
                    ? pagamentoSplitService.calcularResumo(tutor.getCpf(), dto.getTipoServico())
                    : null;

            model.addAttribute("pets", pets);
            model.addAttribute("servicos", TipoServicoPreventivo.values());
            model.addAttribute("split", split);
            model.addAttribute("tutor", tutor);
            return "servicos/checkout";
        }

        try {
            AgendamentoServico agendamento = pagamentoSplitService.iniciarCheckout(dto, auth.getName());
            if (agendamento.getStatusPagamento() == StatusPagamento.PAGO_CONFIRMADO) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Pagamento in-app confirmado e voucher emitido com sucesso! O valor foi liquidado com split automático via gateway.");
                return "redirect:/servicos/voucher/" + agendamento.getId();
            }
            return "redirect:/servicos/pagamento/" + agendamento.getId();
        } catch (Exception e) {
            List<Pet> pets = petService.listarPorTutor(auth.getName());
            Tutor tutor = tutorRepository.findByUsuarioUsername(auth.getName()).orElse(null);
            model.addAttribute("errorMessage", "Erro ao processar pagamento: " + e.getMessage());
            model.addAttribute("pets", pets);
            model.addAttribute("servicos", TipoServicoPreventivo.values());
            model.addAttribute("split", (tutor != null && dto.getTipoServico() != null) ? pagamentoSplitService.calcularResumo(tutor.getCpf(), dto.getTipoServico()) : null);
            model.addAttribute("tutor", tutor);
            return "servicos/checkout";
        }
    }

    @PreAuthorize("hasRole('TUTOR')")
    @GetMapping("/pagamento/{id}")
    public String telaPagamento(@PathVariable("id") Long id, Authentication auth, Model model) {
        AgendamentoServico agendamento = pagamentoSplitService.buscarPorId(id);
        Tutor tutor = tutorRepository.findByUsuarioUsername(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Tutor não encontrado: " + auth.getName()));

        if (!agendamento.getTutor().getCpf().equals(tutor.getCpf())) {
            throw new org.springframework.security.access.AccessDeniedException("Acesso negado a este pagamento.");
        }

        if (agendamento.getStatusPagamento() == StatusPagamento.PAGO_CONFIRMADO
                || agendamento.getStatusPagamento() == StatusPagamento.UTILIZADO_NA_CLINICA) {
            return "redirect:/servicos/voucher/" + agendamento.getId();
        }

        model.addAttribute("agendamento", agendamento);
        model.addAttribute("transacao", agendamento.getTransacao());
        model.addAttribute("split", pagamentoSplitService.calcularResumo(tutor.getCpf(), agendamento.getTipoServico()));
        return "servicos/pagamento";
    }

    @PreAuthorize("hasRole('TUTOR')")
    @GetMapping("/pagamento/{id}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checarStatusPagamento(@PathVariable("id") Long id, Authentication auth) {
        AgendamentoServico agendamento = pagamentoSplitService.consultarEAtualizarStatus(id);
        boolean pago = (agendamento.getStatusPagamento() == StatusPagamento.PAGO_CONFIRMADO
                || agendamento.getStatusPagamento() == StatusPagamento.UTILIZADO_NA_CLINICA);

        Map<String, Object> response = new HashMap<>();
        response.put("pago", pago);
        response.put("status", agendamento.getStatusPagamento().name());
        response.put("statusDescricao", agendamento.getStatusPagamento().getDescricao());
        response.put("redirectUrl", "/servicos/voucher/" + id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('TUTOR')")
    @PostMapping("/pagamento/{id}/simular")
    public String simularPagamento(@PathVariable("id") Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        AgendamentoServico agendamento = pagamentoSplitService.buscarPorId(id);
        Tutor tutor = tutorRepository.findByUsuarioUsername(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Tutor não encontrado: " + auth.getName()));

        if (!agendamento.getTutor().getCpf().equals(tutor.getCpf())) {
            throw new org.springframework.security.access.AccessDeniedException("Acesso negado a este pagamento.");
        }

        pagamentoSplitService.simularConfirmacaoPagamento(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Pagamento PIX liquidado com sucesso via Sandbox! O voucher foi ativado e a comissão retida em Escrow.");
        return "redirect:/servicos/voucher/" + id;
    }

    @PreAuthorize("hasRole('TUTOR')")
    @GetMapping("/voucher/{id}")
    public String verVoucher(@PathVariable("id") Long id, Authentication auth, Model model) {
        AgendamentoServico agendamento = pagamentoSplitService.buscarPorId(id);
        Tutor tutor = tutorRepository.findByUsuarioUsername(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Tutor não encontrado"));

        // Garante que o voucher pertence ao tutor logado
        if (!agendamento.getTutor().getCpf().equals(tutor.getCpf())) {
            throw new org.springframework.security.access.AccessDeniedException("Acesso negado a este voucher");
        }

        model.addAttribute("voucher", agendamento);
        return "servicos/voucher";
    }

    @PreAuthorize("hasRole('TUTOR')")
    @GetMapping("/meus-vouchers")
    public String meusVouchers(Authentication auth, Model model) {
        List<AgendamentoServico> vouchers = pagamentoSplitService.listarPorTutor(auth.getName());
        model.addAttribute("vouchers", vouchers);
        return "servicos/meus-vouchers";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/validar-voucher")
    public String validarVoucherNaClinica(@RequestParam("codigoVoucher") String codigoVoucher,
                                         Authentication auth,
                                         RedirectAttributes redirectAttributes) {
        try {
            AgendamentoServico agendamento = pagamentoSplitService.validarEBaixarVoucher(codigoVoucher, auth.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Voucher " + agendamento.getCodigoVoucher() + " validado com sucesso! Atendimento de '" +
                    agendamento.getDescricaoServico() + "' para o pet " + agendamento.getPet().getNome() +
                    " registrado como concluído.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro na validação do voucher: " + e.getMessage());
        }
        return "redirect:/triagem/fila";
    }
}
