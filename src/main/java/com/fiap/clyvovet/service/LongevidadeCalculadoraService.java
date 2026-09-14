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

        boolean isFelino = pet.getRaca() != null && "FELINA".equalsIgnoreCase(pet.getRaca().getEspecie());
        BigDecimal peso = pet.getPeso() != null ? pet.getPeso() : new BigDecimal("15.0");

        // 1. Cálculo Científico de Idade Humana Equivalente
        int idadeHumana = calcularIdadeHumana(anos, meses, isFelino, peso);

        // 2. Classificação do Estágio de Vida
        String estagio;
        String badgeClass;
        String descricaoEstagio;

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

        // 3. Percentual de Expectativa de Vida Percorrida
        int expectativa = (pet.getRaca() != null && pet.getRaca().getExpectativaVida() != null)
                ? pet.getRaca().getExpectativaVida()
                : 13;
        int percentualExpectativa = Math.min(100, (int) Math.round(((double) anos / expectativa) * 100));

        // 4. Vacinas Recomendadas por Idade
        List<String> vacinas = new ArrayList<>();
        if (anos < 1) {
            if (isFelino) {
                vacinas.add("Vacina Quádrupla/Quíntupla Felina (V4/V5) - 3 doses essenciais");
                vacinas.add("Vacina Antirrábica Felina (a partir de 4 meses)");
                vacinas.add("Teste de FIV/FeLV antes da imunização");
            } else {
                vacinas.add("Vacina Polivalente Canina (V10 ou V8) - 3 doses de filhote");
                vacinas.add("Vacina Antirrábica (Dose única aos 4 meses)");
                vacinas.add("Vacina contra Gripe Canina / Tosse dos Canis (Bronchiguard)");
                vacinas.add("Vacina contra Giardíase Canina (2 doses)");
            }
        } else {
            if (isFelino) {
                vacinas.add("Reforço Anual Quádrupla/Quíntupla Felina (V4/V5)");
                vacinas.add("Reforço Anual Antirrábica");
                if (anos >= 10) {
                    vacinas.add("Avaliação de Titulação de Anticorpos (evita sobrecarga vacinal em felinos idosos)");
                }
            } else {
                vacinas.add("Reforço Anual Polivalente V10 (Proteção contra 10 cepas e leptospirose)");
                vacinas.add("Reforço Anual Antirrábica Obrigatória");
                vacinas.add("Reforço Anual Gripe Canina / Tosse dos Canis");
                if (anos >= 8) {
                    vacinas.add("Titulação Sorológica de Anticorpos recomendada antes da revacinação em sêniores");
                }
            }
        }

        // 5. Exames Preventivos Recomendados para a Faixa Etária
        List<String> exames = new ArrayList<>();
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

        // 6. Nutrição e Suplementos de Longevidade
        List<String> nutricao = new ArrayList<>();
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

        // 7. Atividade Física
        String atividade;
        if (anos < 1) {
            atividade = "Sessões curtas de brincadeiras (15-20 min), sem saltos de altura para preservar cartilagens de crescimento.";
        } else if (anos <= 6) {
            atividade = "40 a 60 minutos diários de caminhadas ativas, natação ou enriquecimento ambiental moderado.";
        } else if (anos <= 10) {
            atividade = "25 a 35 minutos diários de caminhadas de baixo impacto em piso regular, evitando sol quente.";
        } else {
            atividade = "Passeios contemplativos leves de 15 a 20 minutos, priorizando estímulo olfativo e conforto articular.";
        }

        // 8. Mensagem Formatada para WhatsApp da Clínica
        String tutorNome = pet.getTutor() != null ? pet.getTutor().getNome() : "Tutor";
        String textoZap = String.format(
                "Olá, equipe Clyvo Vet! Sou %s, tutor(a) do pet %s (%s, %d anos - idade humana ~%d anos). " +
                "Gostaria de agendar o check-up preventivo recomendado pelo protocolo de longevidade (Vacinas e Exames da fase %s).",
                tutorNome, pet.getNome(), pet.getRaca() != null ? pet.getRaca().getNome() : "Pet", anos, idadeHumana, estagio
        );
        String zapEncoded = URLEncoder.encode(textoZap, StandardCharsets.UTF_8);

        return new ProtocoloLongevidadeDto(
                anos, meses, idadeHumana, estagio, badgeClass, descricaoEstagio,
                percentualExpectativa, vacinas, exames, nutricao, atividade, zapEncoded
        );
    }

    private int calcularIdadeHumana(int anos, int meses, boolean isFelino, BigDecimal peso) {
        double anosCompletos = anos + (meses / 12.0);

        if (isFelino) {
            // Felinos: 1º ano = 15 anos; 2º ano = 24 anos; cada ano subsequente = +4 anos humanos
            if (anosCompletos <= 1.0) {
                return (int) Math.round(anosCompletos * 15.0);
            } else if (anosCompletos <= 2.0) {
                return (int) Math.round(15.0 + (anosCompletos - 1.0) * 9.0);
            } else {
                return (int) Math.round(24.0 + (anosCompletos - 2.0) * 4.0);
            }
        }

        // Caninos: ponderação por porte com base no peso atual
        double pesoKg = peso.doubleValue();
        if (pesoKg < 10.0) {
            // Porte Pequeno (envelhece mais devagar no sênior)
            if (anosCompletos <= 1.0) return (int) Math.round(anosCompletos * 15.0);
            if (anosCompletos <= 2.0) return (int) Math.round(15.0 + (anosCompletos - 1.0) * 9.0);
            return (int) Math.round(24.0 + (anosCompletos - 2.0) * 4.0);
        } else if (pesoKg <= 25.0) {
            // Porte Médio
            if (anosCompletos <= 1.0) return (int) Math.round(anosCompletos * 15.0);
            if (anosCompletos <= 2.0) return (int) Math.round(15.0 + (anosCompletos - 1.0) * 9.0);
            return (int) Math.round(24.0 + (anosCompletos - 2.0) * 5.0);
        } else {
            // Porte Grande / Gigante (como Golden Retriever, Pastor, Rottweiler - envelhece mais rápido a partir dos 4 anos)
            if (anosCompletos <= 1.0) return (int) Math.round(anosCompletos * 15.0);
            if (anosCompletos <= 2.0) return (int) Math.round(15.0 + (anosCompletos - 1.0) * 9.0);
            return (int) Math.round(24.0 + (anosCompletos - 2.0) * 6.5);
        }
    }
}
