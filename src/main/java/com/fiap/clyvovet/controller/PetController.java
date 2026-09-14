package com.fiap.clyvovet.controller;

import com.fiap.clyvovet.dto.BadgeItemDto;
import com.fiap.clyvovet.dto.PetDto;
import com.fiap.clyvovet.model.BadgeConquista;
import com.fiap.clyvovet.model.CheckinDiario;
import com.fiap.clyvovet.model.ConsultaTriagem;
import com.fiap.clyvovet.model.HistoricoClinico;
import com.fiap.clyvovet.model.Pet;
import com.fiap.clyvovet.repository.HistoricoClinicoRepository;
import com.fiap.clyvovet.service.CheckinService;
import com.fiap.clyvovet.service.PetService;
import com.fiap.clyvovet.service.TriagemService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/pets")
public class PetController {

    private final PetService petService;
    private final CheckinService checkinService;
    private final TriagemService triagemService;
    private final HistoricoClinicoRepository historicoClinicoRepository;
    private final com.fiap.clyvovet.service.LongevidadeCalculadoraService longevidadeService;

    public PetController(PetService petService,
                         CheckinService checkinService,
                         TriagemService triagemService,
                         HistoricoClinicoRepository historicoClinicoRepository,
                         com.fiap.clyvovet.service.LongevidadeCalculadoraService longevidadeService) {
        this.petService = petService;
        this.checkinService = checkinService;
        this.triagemService = triagemService;
        this.historicoClinicoRepository = historicoClinicoRepository;
        this.longevidadeService = longevidadeService;
    }

    @GetMapping
    public String listarPets(Authentication auth, Model model) {
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        List<Pet> pets = isAdmin ? petService.listarTodos() : petService.listarPorTutor(auth.getName());
        model.addAttribute("pets", pets);
        model.addAttribute("isAdmin", isAdmin);
        return "pets/lista";
    }

    @GetMapping("/novo")
    public String formNovoPet(Model model) {
        model.addAttribute("petDto", new PetDto());
        model.addAttribute("racas", petService.listarRacas());
        return "pets/form";
    }

    @PostMapping("/salvar")
    public String salvarPet(@Valid @ModelAttribute("petDto") PetDto petDto,
                            BindingResult bindingResult,
                            Authentication auth,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("racas", petService.listarRacas());
            return "pets/form";
        }

        try {
            petService.salvar(petDto, auth.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Pet salvo com sucesso na Clyvo Vet!");
            return "redirect:/pets";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erro ao salvar pet: " + e.getMessage());
            model.addAttribute("racas", petService.listarRacas());
            return "pets/form";
        }
    }

    /** Atalho da sidebar "Protocolo de Longevidade": abre o primeiro pet do tutor logado. */
    @GetMapping("/protocolo")
    public String protocoloLongevidade(Authentication auth) {
        List<Pet> pets = petService.listarPorTutor(auth.getName());
        return pets.isEmpty() ? "redirect:/pets/novo" : "redirect:/pets/" + pets.get(0).getId();
    }

    @GetMapping("/{id}")
    public String detalhesPet(@PathVariable("id") Long petId, Authentication auth, Model model) {
        Pet pet = petService.buscarPorIdAutorizado(petId, auth); // tutor só vê os próprios pets
        List<CheckinDiario> checkins = checkinService.listarHistoricoPorPet(petId);
        List<BadgeConquista> badges = checkinService.listarBadgesPorPet(petId);
        List<ConsultaTriagem> triagens = triagemService.listarPorPet(petId);
        List<HistoricoClinico> timeline = historicoClinicoRepository.findByPetIdOrderByDataRegistroDesc(petId);
        com.fiap.clyvovet.dto.ProtocoloLongevidadeDto protocolo = longevidadeService.calcularProtocolo(pet);
        List<BadgeItemDto> galeriaBadges = checkinService.obterGaleriaDeBadgesCompletas(pet);

        model.addAttribute("pet", pet);
        model.addAttribute("checkins", checkins);
        model.addAttribute("badges", badges);
        model.addAttribute("galeriaBadges", galeriaBadges);
        model.addAttribute("triagens", triagens);
        model.addAttribute("timeline", timeline);
        model.addAttribute("protocolo", protocolo);

        return "pets/detalhes";
    }
}
