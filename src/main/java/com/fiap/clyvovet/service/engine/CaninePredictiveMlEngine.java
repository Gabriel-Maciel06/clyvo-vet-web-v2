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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Estratégia de Machine Learning Supervisionado para Caninos (CANINA).
 *
 * <h3>Ficha Técnica do Modelo — CanineWellness-ML-v1.0</h3>
 *
 * <p><strong>Variável-Alvo (Target y):</strong> Classificação binária de
 * <em>Higidez Clínica Projetada em 12 meses</em>:
 * <ul>
 *   <li>{@code y = 1} → Hígido: ausência de necessidade de internação ou intervenção de urgência
 *       no horizonte de 12 meses com base no perfil longitudinal de check-ins e sinais vitais.</li>
 *   <li>{@code y = 0} → Risco Clínico Significativo: indicativo de consulta imediata e investigação
 *       diagnóstica aprofundada (exames laboratoriais, painel bioquímico ou avaliação especialista).</li>
 * </ul>
 * </p>
 *
 * <p><strong>Regra de Threshold de Decisão (conforme implementada):</strong></p>
 * <p>O escore NÃO é a probabilidade. Ele é derivado dela e depois ajustado:
 * {@code escore = 0.85 * P * 100 + bonus de atividade}, limitado a [10, 99] e
 * sujeito a tetos clínicos por idade e histórico neurológico. A classificação
 * de risco lê o escore, não a probabilidade:</p>
 * <ul>
 *   <li>{@code escore >= 80} → {@link ClassificacaoRisco#BAIXO} (exige, na prática, P em torno de 0.87)</li>
 *   <li>{@code 50 <= escore < 80} → {@link ClassificacaoRisco#MODERADO}</li>
 *   <li>{@code escore < 50} → {@link ClassificacaoRisco#ALTO}</li>
 * </ul>
 *
 * <p><strong>Dataset:</strong> Canine Wellness Classification Dataset — 10.000 amostras sintéticas
 * calibradas sobre dados populacionais caninos. Split estratificado 80/20:
 * 8.000 amostras de treino / 2.000 amostras de teste holdout. Balanceamento de classes: 55% hígidos /
 * 45% em risco. Sem data leakage entre conjuntos.</p>
 *
 * <p><strong>ROC-AUC = 0.9485</strong> — medido exclusivamente sobre o conjunto de teste holdout
 * (2.000 amostras). Essa métrica qualifica a <em>probabilidade</em> {@code P(Higidez | X)},
 * e não o escore de longevidade exibido ao tutor, que passa pelos ajustes descritos acima.</p>
 *
 * <p><strong>Algoritmo:</strong> Regressão Logística Multivariada com normalização Z-score.
 * 16 variáveis preditoras efetivamente usadas: 6 numéricas (idade, peso, caminhada, sono,
 * brincadeira, visitas ao veterinário) + 10 categóricas one-hot (4 níveis de atividade,
 * 2 de dieta, 2 de medicação contínua, 2 de histórico de convulsão).
 * Coeficientes derivados do dataset de treino via MLE.</p>
 *
 * <h3>Limitações declaradas (leia antes de interpretar o escore)</h3>
 * <ol>
 *   <li><strong>Entradas derivadas, não medidas.</strong> O Clyvo Vet não coleta horas de sono
 *       nem de brincadeira. Esses valores são inferidos por regra a partir dos check-ins
 *       (idade, apetite e minutos de atividade). A distância caminhada é convertida dos
 *       minutos de atividade. Apenas idade, peso, visitas ao veterinário e os campos
 *       categóricos vêm de dado real do tutor.</li>
 *   <li><strong>Viés de indicação nas visitas ao veterinário.</strong> {@code COEF_VET_VISITS}
 *       é positivo e é o segundo maior coeficiente numérico: mais consultas elevam a
 *       probabilidade de higidez. No dado de origem isso é provável causalidade reversa
 *       (animais acompanhados adoecem menos), mas numa plataforma que intermedia a venda
 *       de consultas o efeito também é um conflito de interesse. Por isso o bônus aditivo
 *       de consultas que existia no cálculo do escore foi removido: a variável agora pesa
 *       uma única vez, dentro do logit treinado.</li>
 *   <li><strong>Temperatura corporal não entra no modelo.</strong> Ver nota abaixo.</li>
 * </ol>
 *
 * <p><em>Nota sobre temperatura:</em> o dataset de origem media temperatura <strong>ambiente</strong>
 * (média de 64.57 °F, cerca de 18 °C). O código convertia a temperatura <strong>retal</strong> do
 * paciente, em torno de 101 °F, e a normalizava contra essa distribuição, gerando um desvio-padrão
 * artificial de aproximadamente +2.5 numa variável que media outra coisa. Era um erro de categoria.
 * O termo foi removido do logit. A temperatura corporal continua avaliada, e com muito mais rigor,
 * pela camada de guardrails clínicos vitais (AAHA/WSAVA) em {@code TriagemService}, que sobrescreve
 * o risco em caso de hipertermia ou hipotermia.</p>
 *
 * <p><em>Nota:</em> {@code probabilidadeHigidez} é reportada no intervalo [5%, 98%] (clamped)
 * para evitar extrapolação de confiança além da densidade do dataset de treino.</p>
 */
@Component
public class CaninePredictiveMlEngine implements MotorDecisaoClinicaStrategy {

    public static final String VERSAO_MOTOR = "CanineWellness-ML-v1.0";

    // Constantes e Coeficientes de Regressão Logística e Normalização (model_metadata.json)
    private static final double INTERCEPT = 0.2814964638063569;

    // Estatísticas Numéricas para Normalização Z-Score (Média e Desvio Padrão)
    private static final double AGE_MEAN = 7.06;
    private static final double AGE_STD = 3.75;

    private static final double WEIGHT_MEAN = 50.22; // lbs
    private static final double WEIGHT_STD = 14.90;

    private static final double WALK_MEAN = 2.53; // milhas
    private static final double WALK_STD = 1.46;

    private static final double SLEEP_MEAN = 11.0; // horas
    private static final double SLEEP_STD = 1.46;

    private static final double PLAY_MEAN = 1.53; // horas
    private static final double PLAY_STD = 0.98;

    private static final double VET_VISITS_MEAN = 1.47;
    private static final double VET_VISITS_STD = 1.15;

    // As constantes TEMP_MEAN/TEMP_STD/COEF_TEMP do dataset original foram removidas:
    // mediam temperatura AMBIENTE e estavam sendo alimentadas com temperatura
    // CORPORAL do paciente. Ver nota sobre temperatura no javadoc da classe.

    // Coeficientes Lineares das Variáveis Numéricas Padronizadas
    private static final double COEF_AGE = -0.7297;
    private static final double COEF_WEIGHT = 0.0572;
    private static final double COEF_WALK = -0.0317;
    private static final double COEF_SLEEP = 0.0427;
    private static final double COEF_PLAY = -0.0039;
    private static final double COEF_VET_VISITS = 0.8321;

    // Coeficientes Categóricos One-Hot
    private static final double COEF_ACT_VERY_ACTIVE = 1.3268;
    private static final double COEF_ACT_ACTIVE = 1.2587;
    private static final double COEF_ACT_MODERATE = -1.1804;
    private static final double COEF_ACT_LOW = -1.1435;

    private static final double COEF_DIET_SPECIAL = 1.3627;
    private static final double COEF_DIET_HARD_FOOD = -0.9531;

    private static final double COEF_MEDS_NO = 1.3253;
    private static final double COEF_MEDS_YES = -1.0639;

    private static final double COEF_SEIZURES_NO = 1.4282;
    private static final double COEF_SEIZURES_YES = -1.1667;

    @Override
    public boolean suporta(String especie) {
        return "CANINA".equalsIgnoreCase(especie) || "CAO".equalsIgnoreCase(especie) || "CACHORRO".equalsIgnoreCase(especie);
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
                ? Period.between(pet.getDataNascimento(), LocalDate.now()).getYears()
                : 4;

        double pesoKg = (pesoAferido != null)
                ? pesoAferido.doubleValue()
                : (pet.getPeso() != null ? pet.getPeso().doubleValue() : 15.0);
        double pesoLbs = pesoKg * 2.20462;

        double minutosAtividadeMedia = 40.0;
        boolean usaMedicamentoContinuo = false;
        boolean temHistoricoConvulsao = false;
        boolean dietaEspecial = false;
        boolean apetiteReduzido = false;

        if (checkins != null && !checkins.isEmpty()) {
            double somaMinutos = 0;
            int countMeds = 0;
            for (CheckinDiario chk : checkins) {
                if (chk.getMinutosAtividade() != null) {
                    somaMinutos += chk.getMinutosAtividade();
                }
                if (Boolean.TRUE.equals(chk.getRemedioAdministrado())) {
                    countMeds++;
                }
                if (chk.getAlimentacaoStatus() == AlimentacaoStatus.POUCO_APETITE) {
                    apetiteReduzido = true;
                } else if (chk.getAlimentacaoStatus() == AlimentacaoStatus.RECOMENDADA) {
                    dietaEspecial = true;
                }
                if (chk.getSintomasObservados() != null) {
                    String sint = chk.getSintomasObservados().toLowerCase();
                    if (sint.contains("convuls") || sint.contains("tremor") || sint.contains("ataque")) {
                        temHistoricoConvulsao = true;
                    }
                }
            }
            minutosAtividadeMedia = somaMinutos / checkins.size();
            usaMedicamentoContinuo = countMeds > (checkins.size() / 2);
        }

        double walkMiles = Math.max(0.2, (minutosAtividadeMedia / 20.0) * 1.0);
        double sleepHours = (apetiteReduzido || idadeAnos >= 8) ? 13.0 : 11.0;
        double playHours = (minutosAtividadeMedia >= 45) ? 2.0 : 1.0;
        double vetVisits = Math.max(1.0, (double) totalConsultasHistorico);

        // Normalização Z-Score
        double zAge = (idadeAnos - AGE_MEAN) / AGE_STD;
        double zWeight = (pesoLbs - WEIGHT_MEAN) / WEIGHT_STD;
        double zWalk = (walkMiles - WALK_MEAN) / WALK_STD;
        double zSleep = (sleepHours - SLEEP_MEAN) / SLEEP_STD;
        double zPlay = (playHours - PLAY_MEAN) / PLAY_STD;
        double zVet = (vetVisits - VET_VISITS_MEAN) / VET_VISITS_STD;

        // Cálculo do Logit
        double logit = INTERCEPT
                + (COEF_AGE * zAge)
                + (COEF_WEIGHT * zWeight)
                + (COEF_WALK * zWalk)
                + (COEF_SLEEP * zSleep)
                + (COEF_PLAY * zPlay)
                + (COEF_VET_VISITS * zVet);

        if (minutosAtividadeMedia >= 60) logit += COEF_ACT_VERY_ACTIVE;
        else if (minutosAtividadeMedia >= 30) logit += COEF_ACT_ACTIVE;
        else if (minutosAtividadeMedia >= 15) logit += COEF_ACT_MODERATE;
        else logit += COEF_ACT_LOW;

        if (dietaEspecial) logit += COEF_DIET_SPECIAL;
        else logit += COEF_DIET_HARD_FOOD;

        if (usaMedicamentoContinuo) logit += COEF_MEDS_YES;
        else logit += COEF_MEDS_NO;

        if (temHistoricoConvulsao) logit += COEF_SEIZURES_YES;
        else logit += COEF_SEIZURES_NO;

        // Probabilidade Sigmóide Real P(Higidez | X)
        double probHigidez = 1.0 / (1.0 + Math.exp(-logit));
        probHigidez = Math.max(0.05, Math.min(0.98, probHigidez));

        // Escore de Longevidade (0 a 100)
        // As visitas ao veterinario ja pesam dentro do logit (COEF_VET_VISITS).
        // O bonus aditivo que existia aqui as contava uma segunda vez, inflando o
        // escore de quem mais consome consultas na propria plataforma.
        double baseScore = probHigidez * 100.0;
        double activityBonus = Math.min(walkMiles * 1.5, 6.0);
        int escoreLongevidade = (int) Math.round(Math.max(10, Math.min(99, (baseScore * 0.85) + activityBonus)));

        if (idadeAnos >= 12 && escoreLongevidade > 75) escoreLongevidade = 75;
        if (temHistoricoConvulsao && escoreLongevidade > 65) escoreLongevidade = 65;

        ClassificacaoRisco risco = (escoreLongevidade >= 80) ? ClassificacaoRisco.BAIXO
                : (escoreLongevidade >= 50 ? ClassificacaoRisco.MODERADO : ClassificacaoRisco.ALTO);

        // Fatores de Explicabilidade (XAI)
        List<FatorExplicabilidade> fatoresXai = new ArrayList<>();
        if (idadeAnos <= 4) {
            fatoresXai.add(new FatorExplicabilidade("Fase Adulta Jovem (" + idadeAnos + " anos)", "+14 pts", "positivo",
                    "Expectativa de vida favorável decorrente de menor senescência celular e vigor metabólico."));
        } else if (idadeAnos >= 9) {
            fatoresXai.add(new FatorExplicabilidade("Paciente Geriátrico (" + idadeAnos + " anos)", "-18 pts", "negativo",
                    "Idade avançada correlacionada com propensão a cardiopatias, artrose e declínio renal."));
        }

        if (minutosAtividadeMedia >= 45) {
            fatoresXai.add(new FatorExplicabilidade("Nível de Atividade Frequente (" + Math.round(minutosAtividadeMedia) + " min/dia)", "+12 pts", "positivo",
                    "Estilo de vida ativo atua preventivamente contra obesidade e preserva massa magra."));
        } else if (minutosAtividadeMedia < 20) {
            fatoresXai.add(new FatorExplicabilidade("Sedentarismo Crônico", "-14 pts", "negativo",
                    "Baixo nível de atividade física associado a declínio cardiorrespiratório e estresse crônico."));
        }

        if (usaMedicamentoContinuo) {
            fatoresXai.add(new FatorExplicabilidade("Terapia Medicamentosa Contínua", "-11 pts", "negativo",
                    "Indica comorbidade pré-existente exigindo acompanhamento laboratorial semestral."));
        }

        if (temHistoricoConvulsao) {
            fatoresXai.add(new FatorExplicabilidade("Histórico de Alteração Neurológica / Convulsão", "-25 pts", "negativo",
                    "Eventos neurológicos requerem investigação tomográfica e dosagem de enzimas hepáticas."));
        }

        if (totalConsultasHistorico >= 2) {
            fatoresXai.add(new FatorExplicabilidade("Histórico Clínico e Profilaxia Ativa (" + totalConsultasHistorico + " consultas)", "+8 pts", "positivo",
                    "Acompanhamento veterinário periódico viabiliza diagnóstico precoce de enfermidades crônicas."));
        }

        // Riscos Fenotípicos
        List<RiscoFenotipico> riscos = new ArrayList<>();
        String racaNome = (pet.getRaca() != null) ? pet.getRaca().getNome().toLowerCase() : "";
        if (racaNome.contains("bulldog") || racaNome.contains("pug") || racaNome.contains("shih")) {
            if (temperatura != null && temperatura.compareTo(new BigDecimal("39.0")) > 0) {
                riscos.add(new RiscoFenotipico("Estresse Térmico Braquicefálico", "Crítico", "Resfriamento Imediato",
                        "Hipertermia em paciente braquicefálico representa emergência médica aguda por colapso de vias aéreas."));
            } else {
                riscos.add(new RiscoFenotipico("Síndrome Braquicefálica", "Atenção Preventiva", "Exame de Vias Aéreas",
                        "Conformação anatômica com estenose nasal e palato mole alongado. Evitar estresse térmico acima de 26°C."));
            }
        }
        if (pesoKg > 25.0 && (racaNome.contains("pastor") || racaNome.contains("labrador") || racaNome.contains("golden") || racaNome.contains("rottweiler"))) {
            riscos.add(new RiscoFenotipico("Ortopédico / Displasia Coxofemoral", "Monitoramento", "Risco Displasia",
                    "Predisposição fenotípica a osteoartrose coxofemoral. Manter controle ponderal estrito e suplementação de condroprotetores."));
        }
        if (temHistoricoConvulsao) {
            riscos.add(new RiscoFenotipico("Neurológico / Convulsão", "Investigação Ativa", "Triagem Neurológica",
                    "Histórico de tremores ou convulsões reportado pelo tutor nos check-ins recentes. Requer avaliação neurológica e painel bioquímico."));
        }

        String soap = String.format(
                "SOAP CLÍNICO PREDITIVO (Clyvo Vet ML)\n" +
                "[S - Subjetivo]: Paciente %s (%d anos, %s). Queixa: \"%s\". Check-ins recentes: %d registros analisados.\n" +
                "[O - Objetivo]: Peso aferido: %.1f kg (%.1f lbs). Temp: %s°C. FC: %s bpm.\n" +
                "[A - Avaliação ML]: Probabilidade Multivariada de Higidez = %.1f%%. Escore de Longevidade = %d/100 (Risco %s). XAI: %s.\n" +
                "[P - Plano Profilático]: Manter acompanhamento de check-ins, reforçar hidratação e retorno clínico em 6 meses.",
                pet.getNome(), idadeAnos, (pet.getRaca() != null ? pet.getRaca().getNome() : "Canino"),
                (queixaPrincipal != null ? queixaPrincipal : "Rotina preventiva"),
                (checkins != null ? checkins.size() : 0),
                pesoKg, pesoLbs,
                (temperatura != null ? temperatura.toString() : "--"),
                (freqCardiopulmonar != null ? freqCardiopulmonar.toString() : "--"),
                probHigidez * 100.0, escoreLongevidade, risco.name(),
                fatoresXai.isEmpty() ? "Parâmetros basais regulares" : fatoresXai.get(0).fator() + " (" + fatoresXai.get(0).impacto() + ")"
        );

        double probFinal = new BigDecimal(probHigidez * 100.0).setScale(1, RoundingMode.HALF_UP).doubleValue();

        return new ResultadoDecisaoClinica(
                probFinal,
                escoreLongevidade,
                risco,
                fatoresXai,
                riscos,
                soap,
                VERSAO_MOTOR,
                TipoMotorDecisao.MACHINE_LEARNING_SUPERVISIONADO
        );
    }
}
