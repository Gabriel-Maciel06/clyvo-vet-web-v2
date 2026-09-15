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
        model.addAttribute("racasPorEspecie", petService.listarRacasAgrupadasPorEspecie());
        return "pets/form";
    }

    @PostMapping("/salvar")
    public String salvarPet(@Valid @ModelAttribute("petDto") PetDto petDto,
                            BindingResult bindingResult,
                            Authentication auth,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            if (petDto.getId() != null) {
                String msg = bindingResult.getAllErrors().get(0).getDefaultMessage();
                redirectAttributes.addFlashAttribute("errorMessage", msg);
                return "redirect:/pets/" + petDto.getId() + "?tab=editar";
            }
            model.addAttribute("racas", petService.listarRacas());
            model.addAttribute("racasPorEspecie", petService.listarRacasAgrupadasPorEspecie());
            return "pets/form";
        }

        try {
            boolean eraNovo = (petDto.getId() == null);
            Pet salvo = petService.salvar(petDto, auth.getName());
            if (eraNovo) {
                redirectAttributes.addFlashAttribute("successMessage", "Pet cadastrado com sucesso na Clyvo Vet!");
                return "redirect:/pets";
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Dados de " + salvo.getNome() + " atualizados com sucesso!");
                return "redirect:/pets/" + salvo.getId();
            }
        } catch (Exception e) {
            if (petDto.getId() != null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Erro ao atualizar pet: " + e.getMessage());
                return "redirect:/pets/" + petDto.getId() + "?tab=editar";
            }
            model.addAttribute("errorMessage", "Erro ao salvar pet: " + e.getMessage());
            model.addAttribute("racas", petService.listarRacas());
            model.addAttribute("racasPorEspecie", petService.listarRacasAgrupadasPorEspecie());
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

        PetDto petDto = new PetDto();
        petDto.setId(pet.getId());
        petDto.setNome(pet.getNome());
        petDto.setRacaId(pet.getRaca() != null ? pet.getRaca().getId() : null);
        petDto.setDataNascimento(pet.getDataNascimento());
        petDto.setPeso(pet.getPeso());

        model.addAttribute("pet", pet);
        model.addAttribute("petDto", petDto);
        model.addAttribute("racas", petService.listarRacas());
        model.addAttribute("racasPorEspecie", petService.listarRacasAgrupadasPorEspecie());
        model.addAttribute("checkins", checkins);
        model.addAttribute("badges", badges);
        model.addAttribute("galeriaBadges", galeriaBadges);
        model.addAttribute("triagens", triagens);
        model.addAttribute("timeline", timeline);
        model.addAttribute("protocolo", protocolo);

        return "pets/detalhes";
    }

    @GetMapping("/{id}/editar")
    public String editarPet(@PathVariable("id") Long petId, Authentication auth) {
        petService.buscarPorIdAutorizado(petId, auth);
        return "redirect:/pets/" + petId + "?tab=editar";
    }
}
