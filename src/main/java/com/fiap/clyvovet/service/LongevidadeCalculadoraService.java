package com.fiap.clyvovet.service;

import com.fiap.clyvovet.dto.ProtocoloLongevidadeDto;
import com.fiap.clyvovet.model.Pet;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Service
public class LongevidadeCalculadoraService {

    public ProtocoloLongevidadeDto calcularProtocolo(Pet pet) {
        LocalDate hoje = LocalDate.now();
        Period periodo = Period.between(pet.getDataNascimento(), hoje);
        int anos = periodo.getYears();
        int meses = periodo.getMonths();

        String especie = (pet.getRaca() != null && pet.getRaca().getEspecie() != null)
                ? pet.getRaca().getEspecie().trim().toUpperCase()
                : "CANINA";

        int expectativa = (pet.getRaca() != null && pet.getRaca().getExpectativaVida() != null)
                ? pet.getRaca().getExpectativaVida()
                : 13;

        BigDecimal peso = pet.getPeso() != null ? pet.getPeso() : new BigDecimal("15.0");

        // 1. Cálculo Científico de Idade Humana Equivalente adaptado por espécie
        int idadeHumana = calcularIdadeHumana(anos, meses, especie, peso, expectativa);

        // 2. Percentual de Expectativa de Vida Percorrida
        int percentualExpectativa = Math.min(100, (int) Math.round(((double) anos / Math.max(1, expectativa)) * 100));

        // 3. Classificação do Estágio de Vida
        String estagio;
        String badgeClass;
        String descricaoEstagio;

        if ("CANINA".equals(especie) || "FELINA".equals(especie)) {
            if (anos < 1) {
                estagio = "FILHOTE / JÚNIOR";
                badgeClass = "bg-success";
                descricaoEstagio = "Fase crucial de imunização primária, crescimento esquelético e socialização positiva.";
            } else if (anos <= 3) {
                estagio = "JOVEM ADULTO";
                badgeClass = "bg-info";
                descricaoEstagio = "Pico de energia, massa muscular ativa e imunidade consolidada. Prevenção comportamental.";
            } else if (anos <= 6) {
                estagio = "ADULTO MADURO";
                badgeClass = "bg-primary";
                descricaoEstagio = "Fase adulta madura. Manter peso ideal e iniciar monitoramento precoce articular e cardíaco.";
            } else if (anos <= 10) {
                estagio = "SÊNIOR";
                badgeClass = "bg-warning text-dark";
                descricaoEstagio = "Início do envelhecimento celular. Requer check-ups semestrais, exames renais e suporte de articulações.";
            } else {
                estagio = "GERIÁTRICO";
                badgeClass = "bg-danger";
                descricaoEstagio = "Longevidade avançada. Foco total em conforto, preservação cognitiva, função renal e analgesia.";
            }
        } else {
            // Animais exóticos, silvestres e equinos: classificação proporcional à expectativa biológica da espécie
            if (percentualExpectativa < 15) {
                estagio = "FILHOTE / JÚNIOR";
                badgeClass = "bg-success";
                descricaoEstagio = "Fase inicial de desenvolvimento, adaptação ao recinto, manejo ambiental e imunidade primária.";
            } else if (percentualExpectativa < 40) {
                estagio = "JOVEM ADULTO";
                badgeClass = "bg-info";
                descricaoEstagio = "Pico de vitalidade e maturidade fisiológica. Prevenção ativa de manejo e estabilidade de recinto.";
            } else if (percentualExpectativa < 70) {
                estagio = "ADULTO MADURO";
                badgeClass = "bg-primary";
                descricaoEstagio = "Fase adulta consolidada. Monitoramento contínuo de nutrição, escore corporal e enriquecimento ambiental.";
            } else if (percentualExpectativa < 90) {
                estagio = "SÊNIOR";
                badgeClass = "bg-warning text-dark";
                descricaoEstagio = "Maturidade avançada. Requer triagens semestrais direcionadas às predisposições patológicas da espécie.";
            } else {
                estagio = "GERIÁTRICO";
                badgeClass = "bg-danger";
                descricaoEstagio = "Longevidade excepcional. Foco em suporte analgésico, preservação metabólica e conforto do habitat.";
            }
        }

        // 4. Vacinas e Profilaxias Recomendadas
        List<String> vacinas = obterVacinasEProfilaxias(especie, anos);

        // 5. Exames Preventivos Recomendados
        List<String> exames = obterExamesPreventivos(especie, anos);

        // 6. Nutrição e Suplementação Específica
        List<String> nutricao = obterDiretrizesNutricionais(especie, anos);

        // 7. Atividade e Enriquecimento Ambiental
        String atividade = obterDiretrizesAtividade(especie, anos);

        // 8. Mensagem Formatada para WhatsApp da Clínica
        String tutorNome = pet.getTutor() != null ? pet.getTutor().getNome() : "Tutor";
        String textoZap = String.format(
                "Olá, equipe Clyvo Vet! Sou %s, tutor(a) do pet %s (%s, %d anos - idade humana ~%d anos). " +
                "Gostaria de agendar o check-up preventivo recomendado pelo protocolo de longevidade (Protocolo da fase %s).",
                tutorNome, pet.getNome(), pet.getRaca() != null ? pet.getRaca().getNome() : "Pet", anos, idadeHumana, estagio
        );
        String zapEncoded = URLEncoder.encode(textoZap, StandardCharsets.UTF_8);

        return new ProtocoloLongevidadeDto(
                anos, meses, idadeHumana, estagio, badgeClass, descricaoEstagio,
                percentualExpectativa, vacinas, exames, nutricao, atividade, zapEncoded
        );
    }

    private int calcularIdadeHumana(int anos, int meses, String especie, BigDecimal peso, int expectativa) {
        double anosCompletos = anos + (meses / 12.0);

        return switch (especie) {
            case "FELINA" -> {
                if (anosCompletos <= 1.0) yield (int) Math.round(anosCompletos * 15.0);
                if (anosCompletos <= 2.0) yield (int) Math.round(15.0 + (anosCompletos - 1.0) * 9.0);
                yield (int) Math.round(24.0 + (anosCompletos - 2.0) * 4.0);
            }
            case "CANINA" -> {
                double pesoKg = peso.doubleValue();
                if (pesoKg < 10.0) {
                    if (anosCompletos <= 1.0) yield (int) Math.round(anosCompletos * 15.0);
                    if (anosCompletos <= 2.0) yield (int) Math.round(15.0 + (anosCompletos - 1.0) * 9.0);
                    yield (int) Math.round(24.0 + (anosCompletos - 2.0) * 4.0);
                } else if (pesoKg <= 25.0) {
                    if (anosCompletos <= 1.0) yield (int) Math.round(anosCompletos * 15.0);
                    if (anosCompletos <= 2.0) yield (int) Math.round(15.0 + (anosCompletos - 1.0) * 9.0);
                    yield (int) Math.round(24.0 + (anosCompletos - 2.0) * 5.0);
                } else {
                    if (anosCompletos <= 1.0) yield (int) Math.round(anosCompletos * 15.0);
                    if (anosCompletos <= 2.0) yield (int) Math.round(15.0 + (anosCompletos - 1.0) * 9.0);
                    yield (int) Math.round(24.0 + (anosCompletos - 2.0) * 6.5);
                }
            }
            case "ROEDOR" -> (int) Math.round(anosCompletos * 28.0);
            case "MUSTELIDEO" -> {
                if (anosCompletos <= 1.0) yield (int) Math.round(anosCompletos * 14.0);
                yield (int) Math.round(14.0 + (anosCompletos - 1.0) * 9.0);
            }
            case "EQUINA" -> {
                if (anosCompletos <= 1.0) yield (int) Math.round(anosCompletos * 6.5);
                if (anosCompletos <= 3.0) yield (int) Math.round(6.5 + (anosCompletos - 1.0) * 5.5);
                yield (int) Math.round(18.0 + (anosCompletos - 3.0) * 2.5);
            }
            default -> (int) Math.round(anosCompletos * (80.0 / Math.max(1, expectativa)));
        };
    }

    private List<String> obterVacinasEProfilaxias(String especie, int anos) {
        List<String> lista = new ArrayList<>();
        switch (especie) {
            case "CANINA" -> {
                if (anos < 1) {
                    lista.add("Vacina Polivalente Canina (V10 ou V8) - 3 doses de filhote");
                    lista.add("Vacina Antirrábica (Dose única aos 4 meses)");
                    lista.add("Vacina contra Gripe Canina / Tosse dos Canis (Bronchiguard)");
                    lista.add("Vacina contra Giardíase Canina (2 doses)");
                } else {
                    lista.add("Reforço Anual Polivalente V10 (Proteção contra 10 cepas e leptospirose)");
                    lista.add("Reforço Anual Antirrábica Obrigatória");
                    lista.add("Reforço Anual Gripe Canina / Tosse dos Canis");
                    if (anos >= 8) {
                        lista.add("Titulação Sorológica de Anticorpos recomendada antes da revacinação em sêniores");
                    }
                }
            }
            case "FELINA" -> {
                if (anos < 1) {
                    lista.add("Vacina Quádrupla/Quíntupla Felina (V4/V5) - 3 doses essenciais");
                    lista.add("Vacina Antirrábica Felina (a partir de 4 meses)");
                    lista.add("Teste de FIV/FeLV antes da imunização");
                } else {
                    lista.add("Reforço Anual Quádrupla/Quíntupla Felina (V4/V5)");
                    lista.add("Reforço Anual Antirrábica");
                    if (anos >= 10) {
                        lista.add("Avaliação de Titulação de Anticorpos (evita sobrecarga vacinal em felinos idosos)");
                    }
                }
            }
            case "AVE" -> {
                lista.add("Exame Coproparasitológico e Pesquisa de Coccídeos / Giárdia");
                lista.add("Painel Molecular (PCR) para Clamidiose (Psitacose) e Poliomavírus");
                lista.add("Exame de Fezes para pesquisa de Megabactéria (Macrorhabdus)");
                lista.add("Desparasitação preventiva anual sob orientação de veterinário de silvestres");
            }
            case "REPTIL" -> {
                lista.add("Verificação semestral da emissão de radiação da lâmpada UVB (solarium)");
                lista.add("Exame Coproparasitológico seriado para nematódeos e protozoários");
                lista.add("Banho morno de hidratação e estímulo à motilidade cloacal");
                lista.add("Controle rigoroso de gradiente térmico no recinto (dia e noite)");
            }
            case "ROEDOR" -> {
                lista.add("Avaliação preventiva do trato respiratório e ausculta pulmonar especializada");
                lista.add("Exame parasitológico para ácaros de pelo e ectoparasitas");
                lista.add("Inspeção e desgaste dos dentes incisivos (crescimento contínuo)");
            }
            case "MUSTELIDEO" -> {
                lista.add("Vacina contra Cinomose Canina (cepa segura indicada para furões)");
                lista.add("Vacina Antirrábica Anual");
                lista.add("Curva glicêmica preventiva semestral (rastreio precoce de insulinoma)");
            }
            case "PEIXE" -> {
                lista.add("Monitoramento diário de temperatura e semanal de Amônia tóxica / Nitrito / pH");
                lista.add("Quarentena rigorosa de 30 dias para introdução de novos indivíduos ou plantas");
                lista.add("Troca parcial de água (TPA) de 20% a 30% com condicionador anticloro e biologia ativa");
            }
            case "ARACNIDEO" -> {
                lista.add("Monitoramento de umidade do substrato (higrômetro no terrário)");
                lista.add("Inspeção pós-ecdise para checagem de integridade das quelíceras e exoesqueleto");
                lista.add("Higienização e desinfecção periódica do recinto contra ácaros predadores");
            }
            case "EQUINA" -> {
                lista.add("Vacina contra Tétano e Raiva Equina (anual)");
                lista.add("Vacina contra Encefalomielite e Influenza Equina");
                lista.add("Exame de OPG (Ovos por Grama de Fezes) para vermifugação estratégica direcionada");
            }
            default -> lista.add("Consulta preventiva semestral com médico veterinário especialista");
        }
        return lista;
    }

    private List<String> obterExamesPreventivos(String especie, int anos) {
        List<String> exames = new ArrayList<>();
        switch (especie) {
            case "CANINA", "FELINA" -> {
                if (anos < 1) {
                    exames.add("Exame Coproparasitológico de fezes (eliminação de parasitas)");
                    exames.add("Hemograma completo de base para início de vida");
                    exames.add("Avaliação ortopédica e dentição de filhote");
                } else if (anos <= 6) {
                    exames.add("Hemograma completo e plaquetas anual");
                    exames.add("Perfil Bioquímico Renal e Hepático (Ureia, Creatinina, ALT, FA)");
                    exames.add("Avaliação odontológica para profilaxia de tártaro e gengivite");
                    exames.add("Exame de Urina Tipo 1 e densidade urinária");
                } else if (anos <= 10) {
                    exames.add("Perfil Renal Avançado: Creatinina + Biomarcador Precoce SDMA");
                    exames.add("Ultrassonografia Abdominal Preventiva Semestral");
                    exames.add("Ecocardiograma e Eletrocardiograma anual");
                    exames.add("Raio-X de Coluna e Articulações (quadril/joelho)");
                    exames.add("Aferição de Pressão Arterial Sistêmica");
                } else {
                    exames.add("Check-up Geriátrico Semestral Completo (Sangue, Urina e SDMA)");
                    exames.add("Avaliação Neurológica e Cognitiva (Síndrome da Disfunção Cognitiva)");
                    exames.add("Ultrassonografia Abdominal e Ecocardiograma com Doppler");
                    exames.add("Aferição de Pressão Arterial e Fundo de Olho");
                    exames.add("Controle rigoroso de dor osteoarticular crônica");
                }
            }
            case "AVE" -> {
                exames.add("Hemograma aviário completo (contagem de heterófilos)");
                exames.add("Radiografia de corpo total para avaliação de fígado, sacos aéreos e ossos");
                exames.add("Bioquímica sérica (Ácido Úrico, AST e Cálcio iônico)");
                exames.add("Avaliação física de simetria do bico e desgaste de unhas");
            }
            case "REPTIL" -> {
                exames.add("Radiografia preventiva para detecção de osteodistrofia fibrosa e retenção de ovos");
                exames.add("Bioquímica sanguínea: Relação Cálcio/Fósforo e Ácido Úrico");
                exames.add("Inspeção da cavidade oral para diagnóstico precoce de estomatite");
                exames.add("Avaliação estrutural da carapaça e plastron (firmeza e ausência de úlceras)");
            }
            case "ROEDOR" -> {
                exames.add("Palpação abdominal e torácica para detecção precoce de nódulos mamários");
                exames.add("Inspeção otoscópica e dos dentes molares com afastador bucal");
                exames.add("Ausculta torácica para rastreio de infecções subclínicas por Mycoplasma");
            }
            case "MUSTELIDEO" -> {
                exames.add("Glicemia de jejum preventiva (valores <60 mg/dL indicam suspeita de insulinoma)");
                exames.add("Ultrassonografia abdominal com ênfase em glândulas adrenais e baço");
                exames.add("Hemograma completo e painel hepato-renal");
            }
            case "PEIXE" -> {
                exames.add("Biometria periódica (peso corporal e comprimento total)");
                exames.add("Raspado cutâneo de muco e biópsia de brânquia (em caso de prurido ou manchas)");
                exames.add("Painel químico da água: Amônia, Nitrito, Nitrato, Dureza (GH/KH) e Temperatura");
            }
            case "ARACNIDEO" -> {
                exames.add("Exame visual detalhado do abdômen (opistossoma) para checagem de turgidez");
                exames.add("Inspeção de quelíceras, pedipalpos e fiandeiras");
                exames.add("Avaliação de mobilidade e reflexos posturais");
            }
            case "EQUINA" -> {
                exames.add("Exame odontológico completo com grosa para remoção de pontas dentárias cortantes");
                exames.add("Exame físico locomotor e teste de flexão para detecção precoce de claudicação");
                exames.add("Hemograma completo, Fibrinogênio plasmático e enzimas musculares (CK e AST)");
                exames.add("Ausculta gastrointestinal nos 4 quadrantes para monitoramento de motilidade cecal");
            }
            default -> exames.add("Avaliação clínica e física especializada anual");
        }
        return exames;
    }

    private List<String> obterDiretrizesNutricionais(String especie, int anos) {
        List<String> nutricao = new ArrayList<>();
        switch (especie) {
            case "CANINA", "FELINA" -> {
                if (anos < 1) {
                    nutricao.add("Ração Super Premium Filhotes com alta densidade de DHA para desenvolvimento cerebral");
                    nutricao.add("Fracionamento alimentar em 3 a 4 refeições diárias");
                } else if (anos <= 6) {
                    nutricao.add("Ração Super Premium Adulto específica por porte com controle calórico rigoroso");
                    nutricao.add("Suplementação preventiva com Ômega-3 (EPA/DHA) para proteção cardiovascular");
                    nutricao.add("Petiscos funcionais com ação antitártaro e probióticos");
                } else {
                    nutricao.add("Ração Super Premium Sênior / Geriátrica com fósforo reduzido para proteção renal");
                    nutricao.add("Suplementação com Condroitina, Glicosamina e Colágeno Tipo II (UC-II)");
                    nutricao.add("Dose terapêutica de Ômega-3 de alta concentração (neuroproteção e anti-inflamatório)");
                    nutricao.add("Antioxidantes celulares: Coenzima Q10, Vitamina E e Extrato de Chá Verde");
                }
            }
            case "AVE" -> {
                nutricao.add("Ração extrusada balanceada para psitacídeos como base (70% da dieta diária)");
                nutricao.add("Vegetais frescos verde-escuros (couve, rúcula, brócolis) e cenoura rica em Vitamina A");
                nutricao.add("Proibição estrita: sementes de girassol exclusivas, abacate, chocolate e sal");
            }
            case "REPTIL" -> {
                nutricao.add("Quelônios terrestres: 85% de folhas verdes escuras, 10% legumes e 5% frutas/flores");
                nutricao.add("Suplementação de carbonato de cálcio puro polvilhado sobre o alimento 2 a 3x por semana");
                nutricao.add("Serpentes: presas pré-abatidas descongeladas em temperatura morna, respeitando o intervalo digestivo");
            }
            case "ROEDOR" -> {
                nutricao.add("Ração extrusada específica para ratos com teor controlado de proteína vegetal");
                nutricao.add("Complementação diária com legumes cozidos (abóbora, cenoura) e sementes selecionadas");
                nutricao.add("Água filtrada sempre fresca em bebedouro do tipo bico de bilha");
            }
            case "MUSTELIDEO" -> {
                nutricao.add("Dieta carnívora estrita: ração Super Premium para furões com mínimo 35-40% de proteína animal");
                nutricao.add("Gordura animal de alta digestibilidade (mínimo 20%) e teor zero de fibras ou carboidratos");
                nutricao.add("Proibição total de açúcares, frutas ou petiscos vegetais (prevenção de insulinoma)");
            }
            case "PEIXE" -> {
                nutricao.add("Ração em grânulos de afundamento lento de alta absorção biológica");
                nutricao.add("Alimento vivo ou liofilizado como complemento proteico (artêmia salina / dáfnias)");
                nutricao.add("Jejum de 1 dia por semana para descanso do trato gastrointestinal e prevenção de hidropisia");
            }
            case "ARACNIDEO" -> {
                nutricao.add("Insetos vivos criados sob controle sanitário (baratas cinéreas, grilos e tenébrios)");
                nutricao.add("Alimentação semanal para jovens e quinzenal para adultos estabilizados");
                nutricao.add("Nunca oferecer presas durante a muda (ecdise) ou até 7 dias após o término");
            }
            case "EQUINA" -> {
                nutricao.add("Volumoso de excelência (feno de capim de alta digestibilidade) na proporção de 1,5 a 2% do peso vivo");
                nutricao.add("Ração concentrada fracionada em pelo menos 2 a 3 refeições diárias, sem excessos de amido");
                nutricao.add("Sal mineral e água potável fresca à vontade (consumo de 30 a 50 litros/dia)");
            }
            default -> nutricao.add("Alimentação balanceada específica recomendada por médico veterinário");
        }
        return nutricao;
    }

    private String obterDiretrizesAtividade(String especie, int anos) {
        return switch (especie) {
            case "CANINA" -> {
                if (anos < 1) yield "Sessões curtas de brincadeiras (15-20 min), sem saltos de altura para preservar cartilagens de crescimento.";
                if (anos <= 6) yield "40 a 60 minutos diários de caminhadas ativas, natação ou enriquecimento ambiental moderado.";
                if (anos <= 10) yield "25 a 35 minutos diários de caminhadas de baixo impacto em piso regular, evitando sol quente.";
                yield "Passeios contemplativos leves de 15 a 20 minutos, priorizando estímulo olfativo e conforto articular.";
            }
            case "FELINA" -> {
                if (anos < 1) yield "Estímulo a perseguição de brinquedos tipo varinha, arranhadores em níveis e socialização exploratória.";
                if (anos <= 6) yield "Sessões diárias de 20 a 30 minutos de caça simulada, prateleiras suspensas e rotatividade de brinquedos.";
                yield "Estimulação suave em solo, rampas de acesso fácil a locais altos e comedouros interativos lentos.";
            }
            case "AVE" -> "Voos livres diários em cômodo seguro telado, poleiros de galhos naturais de diâmetros variados e brinquedos de forrageamento.";
            case "REPTIL" -> "Acesso supervisionado a sol natural matinal direto (sem vidro) em gramado seguro, tocas térmicas e substrato para escavação.";
            case "ROEDOR" -> "Soltura supervisionada de no mínimo 1 hora diária em espaço seguro, túneis, redes de pano e exercícios de cognição.";
            case "MUSTELIDEO" -> "Mínimo de 3 a 4 horas de soltura fora da gaiola em ambiente à prova de furões (ferret-proof), túneis sanfonados e caixas de escavação.";
            case "PEIXE" -> "Enriquecimento ambiental estático com plantas naturais (Anubias, Valisnérias) e fluxo hidrodinâmico calibrado para a natação da espécie.";
            case "ARACNIDEO" -> "Terrário estático e protegido de vibrações mecânicas intensas, com substrato profundo para escavação e tocas seguras.";
            case "EQUINA" -> "Trabalho montado ou guia regular com aquecimento e desaquecimento adequados, intercalado com piquete diário para pastejo livre.";
            default -> "Estímulo físico e enriquecimento ambiental compatível com as necessidades biológicas do animal.";
        };
    }
}
