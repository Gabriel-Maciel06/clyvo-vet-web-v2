package com.fiap.clyvovet.controller;

import com.fiap.clyvovet.model.CheckinDiario;
import com.fiap.clyvovet.model.ConsultaTriagem;
import com.fiap.clyvovet.model.Pet;
import com.fiap.clyvovet.model.RecompensaTutor;
import com.fiap.clyvovet.model.StatusConsulta;
import com.fiap.clyvovet.model.Tutor;
import com.fiap.clyvovet.repository.TutorRepository;
import com.fiap.clyvovet.service.CheckinService;
import com.fiap.clyvovet.service.PerfilService;
import com.fiap.clyvovet.service.PetService;
import com.fiap.clyvovet.service.TriagemService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

    private final PetService petService;
    private final CheckinService checkinService;
    private final TriagemService triagemService;
    private final TutorRepository tutorRepository;
    private final PerfilService perfilService;

    public HomeController(PetService petService,
                          CheckinService checkinService,
                          TriagemService triagemService,
                          TutorRepository tutorRepository,
                          PerfilService perfilService) {
        this.petService = petService;
        this.checkinService = checkinService;
        this.triagemService = triagemService;
        this.tutorRepository = tutorRepository;
        this.perfilService = perfilService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));

        model.addAttribute("username", username);
        model.addAttribute("isAdmin", isAdmin);

        if (isAdmin) {
            // Dashboard do Veterinário / Administrador
            List<ConsultaTriagem> triagensPendentes = triagemService.listarPorStatus(StatusConsulta.SOLICITADA);
            List<ConsultaTriagem> todasTriagens = triagemService.listarTodas();
            List<CheckinDiario> alertasSaude = checkinService.listarAlertasAtivos();
            List<Pet> todosPets = petService.listarTodos();

            model.addAttribute("triagensPendentes", triagensPendentes);
            model.addAttribute("todasTriagens", todasTriagens);
            model.addAttribute("alertasSaude", alertasSaude);
            model.addAttribute("totalPets", todosPets.size());
            model.addAttribute("pets", todosPets);
            return "dashboard-admin";
        } else {
            // Dashboard do Tutor
            Optional<Tutor> tutorOpt = tutorRepository.findByUsuarioUsername(username);
            // Sempre define este atributo (nunca nulo no model): usado com th:unless no
            // template, e sem Tutor associado (não deveria acontecer, mas alguém autenticado
            // como ROLE_TUTOR sem linha em T_TUTOR é tratado como "cadastro incompleto" em
            // vez de quebrar a tela.
            boolean precisaCompletar = tutorOpt.isEmpty() || perfilService.precisaCompletarCadastro(tutorOpt.get());
            model.addAttribute("precisaCompletarCadastro", precisaCompletar);

            if (tutorOpt.isPresent()) {
                Tutor tutor = tutorOpt.get();
                model.addAttribute("tutor", tutor);

                if (!precisaCompletar) {
                    List<Pet> pets = petService.listarPorTutor(username);
                    model.addAttribute("pets", pets);

                    RecompensaTutor recompensa = checkinService.obterOuCriarRecompensa(tutor.getCpf());
                    model.addAttribute("recompensa", recompensa);
                }
            }
            return "dashboard-tutor";
        }
    }
}
