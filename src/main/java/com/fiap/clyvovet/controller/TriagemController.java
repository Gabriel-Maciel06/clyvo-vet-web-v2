package com.fiap.clyvovet.controller;

import com.fiap.clyvovet.dto.AvaliacaoTriagemDto;
import com.fiap.clyvovet.dto.SolicitacaoTriagemDto;
import com.fiap.clyvovet.model.ConsultaTriagem;
import com.fiap.clyvovet.model.Pet;
import com.fiap.clyvovet.model.StatusConsulta;
import com.fiap.clyvovet.service.CheckinService;
import com.fiap.clyvovet.service.PetService;
import com.fiap.clyvovet.service.TriagemService;
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
@RequestMapping("/triagem")
public class TriagemController {

    private final TriagemService triagemService;
    private final PetService petService;
    private final CheckinService checkinService;

    public TriagemController(TriagemService triagemService,
                             PetService petService,
                             CheckinService checkinService) {
        this.triagemService = triagemService;
        this.petService = petService;
        this.checkinService = checkinService;
    }

    // --- VISÃO DO TUTOR: SOLICITAR AVALIAÇÃO PREVENTIVA ---
    @GetMapping("/solicitar")
    public String formSolicitacao(@RequestParam(value = "petId", required = false) Long petId,
                                  Authentication auth,
                                  Model model) {
        List<Pet> pets = petService.listarPorTutor(auth.getName());
        SolicitacaoTriagemDto dto = new SolicitacaoTriagemDto();
        if (petId != null) {
            dto.setPetId(petId);
        } else if (!pets.isEmpty()) {
            dto.setPetId(pets.get(0).getId());
        }

        model.addAttribute("solicitacaoDto", dto);
        model.addAttribute("pets", pets);
        return "triagem/solicitar";
    }

    @PostMapping("/solicitar")
    public String enviarSolicitacao(@Valid @ModelAttribute("solicitacaoDto") SolicitacaoTriagemDto dto,
                                    BindingResult bindingResult,
                                    Authentication auth,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pets", petService.listarPorTutor(auth.getName()));
            return "triagem/solicitar";
        }

        try {
            ConsultaTriagem triagem = triagemService.solicitarTriagem(dto, auth.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Solicitação de triagem preventiva aberta com sucesso para " + triagem.getPet().getNome() +
                    "! O corpo clínico Clyvo Vet foi notificado.");
            return "redirect:/pets/" + triagem.getPet().getId();
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erro ao solicitar triagem: " + e.getMessage());
            model.addAttribute("pets", petService.listarPorTutor(auth.getName()));
            return "triagem/solicitar";
        }
    }

    // --- VISÃO DO VETERINÁRIO (ROLE_ADMIN): FILA E AVALIAÇÃO ---
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/fila")
    public String filaTriagem(Model model) {
        List<ConsultaTriagem> pendentes = triagemService.listarPorStatus(StatusConsulta.SOLICITADA);
        List<ConsultaTriagem> concluidas = triagemService.listarPorStatus(StatusConsulta.CONCLUIDA);

        model.addAttribute("pendentes", pendentes);
        model.addAttribute("concluidas", concluidas);
        return "triagem/fila";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/avaliar/{id}")
    public String formAvaliacao(@PathVariable("id") Long id, Model model) {
        ConsultaTriagem triagem = triagemService.buscarPorId(id);
        AvaliacaoTriagemDto dto = new AvaliacaoTriagemDto();
        dto.setTriagemId(triagem.getId());
        dto.setPesoAferido(triagem.getPet().getPeso());

        model.addAttribute("triagem", triagem);
        model.addAttribute("avaliacaoDto", dto);
        model.addAttribute("checkins", checkinService.listarHistoricoPorPet(triagem.getPet().getId()));
        return "triagem/avaliar";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/avaliar")
    public String salvarAvaliacao(@Valid @ModelAttribute("avaliacaoDto") AvaliacaoTriagemDto dto,
                                  BindingResult bindingResult,
                                  Authentication auth,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            ConsultaTriagem triagem = triagemService.buscarPorId(dto.getTriagemId());
            model.addAttribute("triagem", triagem);
            model.addAttribute("checkins", checkinService.listarHistoricoPorPet(triagem.getPet().getId()));
            return "triagem/avaliar";
        }

        try {
            ConsultaTriagem concluida = triagemService.avaliarTriagem(dto, auth.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Triagem concluída com sucesso! Escore de longevidade calculado: " +
                    concluida.getEscoreLongevidade() + "/100 (Risco " + concluida.getClassificacaoRisco().name() + ").");
            return "redirect:/triagem/fila";
        } catch (Exception e) {
            ConsultaTriagem triagem = triagemService.buscarPorId(dto.getTriagemId());
            model.addAttribute("triagem", triagem);
            model.addAttribute("checkins", checkinService.listarHistoricoPorPet(triagem.getPet().getId()));
            model.addAttribute("errorMessage", "Erro ao concluir triagem: " + e.getMessage());
            return "triagem/avaliar";
        }
    }
}
