package com.fiap.clyvovet.controller;

import com.fiap.clyvovet.dto.CheckoutRequestDto;
import com.fiap.clyvovet.model.*;
import com.fiap.clyvovet.repository.RecompensaTutorRepository;
import com.fiap.clyvovet.repository.TutorRepository;
import com.fiap.clyvovet.service.PagamentoSplitService;
import com.fiap.clyvovet.service.PetService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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
            AgendamentoServico agendamento = pagamentoSplitService.processarCheckout(dto, auth.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Pagamento in-app confirmado e voucher emitido com sucesso! O valor foi liquidado com split automático via gateway.");
            return "redirect:/servicos/voucher/" + agendamento.getId();
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
