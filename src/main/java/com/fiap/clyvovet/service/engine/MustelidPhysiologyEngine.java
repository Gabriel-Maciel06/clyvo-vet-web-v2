package com.fiap.clyvovet.service.engine;

import com.fiap.clyvovet.dto.FatorExplicabilidade;
import com.fiap.clyvovet.dto.ParametrosClinicosEntrada;
import com.fiap.clyvovet.dto.ResultadoDecisaoClinica;
import com.fiap.clyvovet.dto.RiscoFenotipico;
import com.fiap.clyvovet.model.AlimentacaoStatus;
import com.fiap.clyvovet.model.CheckinDiario;
import com.fiap.clyvovet.model.ClassificacaoRisco;
import com.fiap.clyvovet.model.Pet;
import com.fiap.clyvovet.model.TipoMotorDecisao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Sistema Especialista em Medicina de Mustelídeos (Furões / Ferrets - Mustela putorius furo).
 * Avaliação determinística por Conformidade Fisiológica Base 100 (BSAVA Ferrets Guidelines).
 * Considera as peculiaridades metabólicas de carnívoros estritos de trânsito gastrointestinal ultra-rápido (3-4 horas):
 * - Eutermia: 37.8°C a 40.0°C
 * - Frequência cardíaca basal: 180 a 250 bpm
 * - Prevenção ativa de Insulinoma, Hiperadrenocorticismo e Obstrução por Corpo Estranho.
 */
@Component
public class MustelidPhysiologyEngine implements MotorDecisaoClinicaStrategy {

    public static final String VERSAO_MOTOR = "Mustelid-Physiology-Rules-v1.0";

    @Override
    public boolean suporta(String especie) {
        if (especie == null) return false;
        String e = especie.trim().toUpperCase();
        return e.contains("MUSTELIDEO") || e.contains("MUSTELÍDEO") || e.contains("FURÃO")
                || e.contains("FURAO") || e.contains("FERRET");
    }

    @Override
    public ResultadoDecisaoClinica avaliar(Pet pet, ParametrosClinicosEntrada entrada) {
        BigDecimal pesoAferido = (entrada != null) ? entrada.pesoAferido() : null;
        BigDecimal temperatura = (entrada != null) ? entrada.temperaturaAferida() : null;
        Integer freqCardiopulmonar = (entrada != null) ? entrada.frequenciaMensurada() : null;
        List<CheckinDiario> checkins = (entrada != null) ? entrada.checkinsRecentes() : List.of();
        int totalConsultasHistorico = (entrada != null) ? entrada.totalConsultasHistorico() : 1;
        String queixaPrincipal = (entrada != null) ? entrada.queixaPrincipal() : "Rotina";

        int idadeAnos = (pet.getDataNascimento() != null)
                ? Period.between(pet.getDataNascimento(), LocalDate.now()).getYears() : 2;
        int expectativa = (pet.getRaca() != null && pet.getRaca().getExpectativaVida() != null)
                ? pet.getRaca().getExpectativaVida() : 8;
        double pesoKg = (pesoAferido != null)
                ? pesoAferido.doubleValue() : (pet.getPeso() != null ? pet.getPeso().doubleValue() : 1.2);
        double tempVal = (temperatura != null) ? temperatura.doubleValue() : 38.8;

        int escoreFisiologico = 100;
        List<FatorExplicabilidade> fatores = new ArrayList<>();
        List<RiscoFenotipico> riscos = new ArrayList<>();

        // 1. Termorregulação (Normal em Furão: 37.8°C a 40.0°C)
        if (tempVal < 37.5) {
            escoreFisiologico -= 30;
            fatores.add(new FatorExplicabilidade("Hipotermia em Furão (< 37.5°C)", "-30 pts", "critico",
                    "Furões em colapso glicêmico por insulinoma ou choque séptico evoluem rapidamente com hipotermia severa."));
            riscos.add(new RiscoFenotipico("Termorregulação / Choque", "Emergência", "Hipotermia Aguda",
                    "Risco vital iminente. Aquecimento assistido e aferição glicêmica capilar imediata."));
        } else if (tempVal > 40.2) {
            escoreFisiologico -= 25;
            fatores.add(new FatorExplicabilidade("Hipertermia em Furão (> 40.2°C)", "-25 pts", "critico",
                    "Suspeita de choque térmico (insolação) ou processo infeccioso agudo (ex: cinomose, pneumonia)."));
        } else {
            fatores.add(new FatorExplicabilidade("Eutermia Basal em Furão (" + String.format(java.util.Locale.US, "%.1f", tempVal) + "°C)", "+0 pts", "positivo",
                    "Temperatura corpórea central em perfeita conformidade com os padrões biológicos da espécie."));
        }

        // 2. Frequência Cardíaca (Normal: 180 a 250 bpm)
        if (freqCardiopulmonar != null) {
            if (freqCardiopulmonar < 150) {
                escoreFisiologico -= 25;
                fatores.add(new FatorExplicabilidade("Bradicardia Severa para Furão (< 150 bpm)", "-25 pts", "alerta",
                        "Ritmo cardíaco incompatível com o metabolismo mustelídeo. Sugere exaustão metabólica."));
            } else if (freqCardiopulmonar > 300) {
                escoreFisiologico -= 15;
                fatores.add(new FatorExplicabilidade("Taquicardia Severa (> 300 bpm)", "-15 pts", "alerta",
                        "Estresse agudo, dor abdominal ou crise adrenal."));
            } else {
                fatores.add(new FatorExplicabilidade("Frequência Cardíaca Fisiológica (" + freqCardiopulmonar + " bpm)", "+0 pts", "positivo",
                    "Ritmo basal regular auscultado."));
            }
        }

        // 3. Apetite e Trânsito Digestivo
        boolean inapetente = false;
        if (checkins != null && !checkins.isEmpty()) {
            for (CheckinDiario chk : checkins) {
                if (chk.getAlimentacaoStatus() == AlimentacaoStatus.POUCO_APETITE) inapetente = true;
            }
        }
        if (inapetente) {
            escoreFisiologico -= 20;
            fatores.add(new FatorExplicabilidade("Inapetência / Risco de Obstrução Gastrointestinal", "-20 pts", "critico",
                    "Furões ingerem compulsivamente itens de borracha/silicone. Inapetência > 8h exige ultrassonografia abdominal urgente."));
        }

        // Riscos Fenotípicos Específicos de Furões
        if (idadeAnos >= 3) {
            riscos.add(new RiscoFenotipico("Endócrino Pancreático", "Triagem Ativa", "Suspeita de Insulinoma",
                    "Idade > 3 anos: alta prevalência de insulinoma (adenoma/carcinoma de ilhotas). Monitorar glicemia em jejum de 4h."));
            riscos.add(new RiscoFenotipico("Endócrino Adrenal", "Prevenção Fotoperíodo", "Doença Adrenal (Hiperadrenocorticismo)",
                    "Controle rigoroso de escuridão absoluta (12h de sono no escuro diário) para prevenir hiperplasia do córtex adrenal."));
        } else {
            riscos.add(new RiscoFenotipico("Gastrointestinal / Comportamento", "Manejo Preventivo", "Corpo Estranho",
                    "Furões jovens têm alta incidência de ingestão de brinquedos de borracha e calçados. Enriquecimento ambiental supervisionado."));
        }

        escoreFisiologico = Math.max(10, Math.min(100, escoreFisiologico));
        ClassificacaoRisco risco = (escoreFisiologico >= 80) ? ClassificacaoRisco.BAIXO
                : (escoreFisiologico >= 50 ? ClassificacaoRisco.MODERADO : ClassificacaoRisco.ALTO);

        String soap = String.format(
                "SOAP CLÍNICO MUSTELÍDEOS (Clyvo Vet Ferret Expert System)\n" +
                "[S - Subjetivo]: Paciente %s (%s, %d anos - expectativa: %d anos). Queixa: \"%s\".\n" +
                "[O - Objetivo]: Peso aferido: %.2f kg. Temp central: %.1f°C. FC: %s bpm.\n" +
                "[A - Avaliação Mustelídeo]: Índice de Conformidade Fisiológica = %d/100 (Risco %s). Fatores: %s.\n" +
                "[P - Plano Profilático]: Dieta carnívora hiperproteica livre de carboidratos, fotoperíodo controlado (12h escuro) e triagem periódica de glicemia.",
                pet.getNome(), (pet.getRaca() != null ? pet.getRaca().getNome() : "Furão (Ferret)"), idadeAnos, expectativa,
                (queixaPrincipal != null ? queixaPrincipal : "Rotina profilática"),
                pesoKg, tempVal, (freqCardiopulmonar != null ? freqCardiopulmonar.toString() : "Ausculta regular"),
                escoreFisiologico, risco.name(),
                fatores.isEmpty() ? "Parâmetros basais conformes" : fatores.get(0).fator() + " (" + fatores.get(0).impacto() + ")"
        );

        return new ResultadoDecisaoClinica(
                null,
                escoreFisiologico,
                risco,
                fatores,
                riscos,
                soap,
                VERSAO_MOTOR,
                TipoMotorDecisao.SISTEMA_ESPECIALISTA_FISIOLOGICO
        );
    }
}
