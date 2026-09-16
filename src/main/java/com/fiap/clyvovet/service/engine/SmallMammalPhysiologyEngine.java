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
 * Sistema Especialista em Pequenos Mamíferos Não-Convencionais (Roedores e Lagomorfos: Hamster, Coelho, Porquinho-da-Índia).
 * Avaliação determinística por Conformidade Fisiológica Base 100.
 * Monitoramento de crescimento dentário contínuo (elodonte), cecotrofia e estase gastrointestinal.
 */
@Component
public class SmallMammalPhysiologyEngine implements MotorDecisaoClinicaStrategy {

    public static final String VERSAO_MOTOR = "SmallMammal-Physiology-Rules-v1.0";

    @Override
    public boolean suporta(String especie) {
        if (especie == null) return false;
        String e = especie.trim().toUpperCase();
        return e.contains("ROEDOR") || e.contains("COELHO") || e.contains("HAMSTER")
                || e.contains("PORQUINHO") || e.contains("CHINCHILA") || e.contains("RATO")
                || e.contains("GERBIL");
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
                ? Period.between(pet.getDataNascimento(), LocalDate.now()).getYears() : 1;
        double pesoKg = (pesoAferido != null)
                ? pesoAferido.doubleValue() : (pet.getPeso() != null ? pet.getPeso().doubleValue() : 0.5);
        double tempVal = (temperatura != null) ? temperatura.doubleValue() : 38.5;

        int escoreFisiologico = 100;
        List<FatorExplicabilidade> fatores = new ArrayList<>();
        List<RiscoFenotipico> riscos = new ArrayList<>();

        // 1. Termorregulação (Normal: 37.5°C a 39.5°C)
        if (tempVal < 36.5) {
            escoreFisiologico -= 30;
            fatores.add(new FatorExplicabilidade("Hipotermia em Pequeno Mamífero (< 36.5°C)", "-30 pts", "critico",
                    "Risco crítico de choque hipotérmico e parada do trânsito gastrointestinal (estase cecal)."));
        } else if (tempVal > 40.0) {
            escoreFisiologico -= 25;
            fatores.add(new FatorExplicabilidade("Hipertermia / Estresse Térmico (> 40.0°C)", "-25 pts", "critico",
                    "Pequenos roedores e coelhos não transpiram; superaquecimento acima de 28°C no ambiente é rapidamente letal."));
        } else {
            fatores.add(new FatorExplicabilidade("Eutermia Basal Estável (" + String.format(java.util.Locale.US, "%.1f", tempVal) + "°C)", "+0 pts", "positivo",
                    "Temperatura corporal mantida dentro da faixa fisiológica esperada para pequenos mamíferos."));
        }

        // 2. Frequência Cardíaca (Normal: 180 a 450 bpm)
        if (freqCardiopulmonar != null) {
            if (freqCardiopulmonar < 140) {
                escoreFisiologico -= 25;
                fatores.add(new FatorExplicabilidade("Bradicardia Severa (" + freqCardiopulmonar + " bpm)", "-25 pts", "alerta",
                        "Ritmo cardíaco deprimido sugere colapso metabólico."));
            } else {
                fatores.add(new FatorExplicabilidade("Frequência Cardíaca Fisiológica (" + freqCardiopulmonar + " bpm)", "+0 pts", "positivo",
                        "Ritmo metabólico acelerado mantido sem sopros auscultáveis."));
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
            escoreFisiologico -= 25;
            fatores.add(new FatorExplicabilidade("Inapetência / Hipomotilidade Cecal", "-25 pts", "critico",
                    "Em roedores/coelhos, jejum > 12h causa estase cecal aguda e desequilíbrio da microbiota fermentativa."));
        }

        // Riscos Fenotípicos Gerais de Pequenos Mamíferos
        riscos.add(new RiscoFenotipico("Odontológico / Dentição Elodonte", "Inspeção Contínua", "Maloclusão",
                "Dentes de crescimento contínuo. Exige feno de boa qualidade à vontade para desgaste mastigatório correto."));

        escoreFisiologico = Math.max(10, Math.min(100, escoreFisiologico));
        ClassificacaoRisco risco = (escoreFisiologico >= 80) ? ClassificacaoRisco.BAIXO
                : (escoreFisiologico >= 50 ? ClassificacaoRisco.MODERADO : ClassificacaoRisco.ALTO);

        String soap = String.format(
                "SOAP CLÍNICO PEQUENOS MAMÍFEROS (Clyvo Vet Small Mammal Expert System)\n" +
                "[S - Subjetivo]: Paciente %s (%s, %d anos). Queixa: \"%s\".\n" +
                "[O - Objetivo]: Peso aferido: %.3f kg (%.0f g). Temp Corpórea: %.1f°C. FC: %s bpm.\n" +
                "[A - Avaliação Clínica]: Índice de Conformidade Fisiológica = %d/100 (Risco %s). XAI: %s.\n" +
                "[P - Plano Profilático]: Feno de capim livre, controle de substrato sem poeira e inspeção odontológica trimestral.",
                pet.getNome(), (pet.getRaca() != null ? pet.getRaca().getNome() : "Pequeno Mamífero"), idadeAnos,
                (queixaPrincipal != null ? queixaPrincipal : "Rotina profilática"),
                pesoKg, pesoKg * 1000.0, tempVal, (freqCardiopulmonar != null ? freqCardiopulmonar.toString() : "Ausculta regular"),
                escoreFisiologico, risco.name(),
                fatores.isEmpty() ? "Parâmetros basais regulares" : fatores.get(0).fator() + " (" + fatores.get(0).impacto() + ")"
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
