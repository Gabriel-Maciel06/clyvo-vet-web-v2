package com.fiap.clyvovet.controller;

import com.fiap.clyvovet.dto.CheckinDto;
import com.fiap.clyvovet.model.*;
import com.fiap.clyvovet.repository.TutorRepository;
import com.fiap.clyvovet.service.CheckinService;
import com.fiap.clyvovet.service.PetService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/checkin")
public class CheckinController {

    private final CheckinService checkinService;
    private final PetService petService;
    private final TutorRepository tutorRepository;

    public CheckinController(CheckinService checkinService,
                             PetService petService,
                             TutorRepository tutorRepository) {
        this.checkinService = checkinService;
        this.petService = petService;
        this.tutorRepository = tutorRepository;
    }

    @GetMapping("/novo")
    public String formCheckin(@RequestParam(value = "petId", required = false) Long petId,
                              Authentication auth,
                              Model model) {
        List<Pet> pets = petService.listarPorTutor(auth.getName());
        CheckinDto dto = new CheckinDto();
        if (petId != null) {
            dto.setPetId(petId);
        } else if (!pets.isEmpty()) {
            dto.setPetId(pets.get(0).getId());
        }

        model.addAttribute("checkinDto", dto);
        model.addAttribute("pets", pets);
        model.addAttribute("humores", HumorPet.values());
        model.addAttribute("alimentacoes", AlimentacaoStatus.values());
        return "checkin/form";
    }

    @PostMapping("/salvar")
    public String registrarCheckin(@Valid @ModelAttribute("checkinDto") CheckinDto checkinDto,
                                   BindingResult bindingResult,
                                   Authentication auth,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pets", petService.listarPorTutor(auth.getName()));
            model.addAttribute("humores", HumorPet.values());
            model.addAttribute("alimentacoes", AlimentacaoStatus.values());
            return "checkin/form";
        }

        try {
            CheckinDiario salvo = checkinService.registrarCheckin(checkinDto, auth.getName());
            if (Boolean.TRUE.equals(salvo.getAlertaGerado())) {
                redirectAttributes.addFlashAttribute("warningMessage",
                        "Check-in registrado! Atenção: Detectamos sintomas ou apatia em " + salvo.getPet().getNome() +
                        ". Um alerta preventivo foi gerado para a equipe médica Clyvo Vet.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Check-in realizado com sucesso! Você ganhou " + salvo.getPontosGanhos() +
                        " Clyvo Coins e aumentou seu streak de cuidados!");
            }
            return "redirect:/pets/" + salvo.getPet().getId();
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/dashboard";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erro ao registrar check-in: " + e.getMessage());
            model.addAttribute("pets", petService.listarPorTutor(auth.getName()));
            model.addAttribute("humores", HumorPet.values());
            model.addAttribute("alimentacoes", AlimentacaoStatus.values());
            return "checkin/form";
        }
    }

    @GetMapping("/recompensas")
    public String recompensas(Authentication auth, Model model) {
        Tutor tutor = tutorRepository.findByUsuarioUsername(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Tutor não encontrado"));

        RecompensaTutor recompensa = checkinService.obterOuCriarRecompensa(tutor.getCpf());
        List<Pet> pets = petService.listarPorTutor(auth.getName());

        model.addAttribute("tutor", tutor);
        model.addAttribute("recompensa", recompensa);
        model.addAttribute("pets", pets);
        return "checkin/recompensas";
    }
}
