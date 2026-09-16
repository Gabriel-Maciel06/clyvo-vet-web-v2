package com.fiap.clyvovet.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "T_RACA")
public class Raca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOME", nullable = false, unique = true, length = 100)
    private String nome;

    @Column(name = "ESPECIE", nullable = false, length = 20)
    private String especie;

    @Column(name = "PROPENSAO_DOENCA", length = 500)
    private String propensaoDoenca;

    @Column(name = "EXPECTATIVA_VIDA", nullable = false)
    private Integer expectativaVida;

    @Column(name = "CUIDADOS_ESPECIAIS", length = 500)
    private String cuidadosEspeciais;

    @Column(name = "PESO_MEDIO_MIN", precision = 5, scale = 2)
    private BigDecimal pesoMedioMin;

    @Column(name = "PESO_MEDIO_MAX", precision = 5, scale = 2)
    private BigDecimal pesoMedioMax;

    public Raca() {}

    public Raca(Long id, String nome, String especie, String propensaoDoenca, Integer expectativaVida, String cuidadosEspeciais) {
        this.id = id;
        this.nome = nome;
        this.especie = especie;
        this.propensaoDoenca = propensaoDoenca;
        this.expectativaVida = expectativaVida;
        this.cuidadosEspeciais = cuidadosEspeciais;
    }

    public Raca(Long id, String nome, String especie, String propensaoDoenca, Integer expectativaVida, String cuidadosEspeciais, BigDecimal pesoMedioMin, BigDecimal pesoMedioMax) {
        this.id = id;
        this.nome = nome;
        this.especie = especie;
        this.propensaoDoenca = propensaoDoenca;
        this.expectativaVida = expectativaVida;
        this.cuidadosEspeciais = cuidadosEspeciais;
        this.pesoMedioMin = pesoMedioMin;
        this.pesoMedioMax = pesoMedioMax;
    }

    /**
     * Imagem (emoji) padronizada por espécie / categoria biológica do paciente.
     * Traz clareza imediata na UI: cães (🐶), gatos (🐱), aves (🦜), répteis (🐢/🐍),
     * roedores (🐹), furões (🦦), peixes (🐠), aracnídeos (🕷️) e equinos (🐴).
     */
    public String getEmoji() {
        if (especie == null) return "🐾";
        if (nome != null && (nome.contains("Cobra") || nome.contains("Snake"))) {
            return "🐍";
        }
        return switch (especie.trim().toUpperCase()) {
            case "CANINA" -> "🐶";
            case "FELINA" -> "🐱";
            case "AVE" -> "🦜";
            case "REPTIL" -> "🐢";
            case "ROEDOR" -> "🐹";
            case "MUSTELIDEO" -> "🦦";
            case "PEIXE" -> "🐠";
            case "ARACNIDEO" -> "🕷️";
            case "EQUINA" -> "🐴";
            default -> "🐾";
        };
    }

    /** Retorna a faixa média formatada (ex: "27.0 a 36.0 kg" ou padrão por espécie). */
    public String getPesoMedioFormatado() {
        if (pesoMedioMin != null && pesoMedioMax != null) {
            return pesoMedioMin + " a " + pesoMedioMax + " kg";
        }
        return "Consulte faixa típica da espécie";
    }

    /** Retorna um valor médio sugerido de 1 clique para pré-preenchimento no formulário. */
    public BigDecimal getPesoMedioSugerido() {
        if (pesoMedioMin != null && pesoMedioMax != null) {
            return pesoMedioMin.add(pesoMedioMax).divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_UP);
        }
        if ("CANINA".equalsIgnoreCase(especie)) return new BigDecimal("15.0");
        if ("FELINA".equalsIgnoreCase(especie)) return new BigDecimal("4.5");
        return new BigDecimal("5.0");
    }

    /** Retorna o limite biológico máximo plausível de idade em anos para a espécie. */
    public int getLimiteMaximoIdadeAnos() {
        if (especie == null) return 30;
        return switch (especie.trim().toUpperCase()) {
            case "CANINA" -> 30;     // Recorde histórico mundial: ~31 anos
            case "FELINA" -> 30;     // Recorde histórico mundial: ~38 anos
            case "ROEDOR" -> 6;      // Ratos/hamsters raramente passam de 3 a 5 anos
            case "MUSTELIDEO" -> 14; // Furões vivem até 8-12 anos
            case "AVE" -> 80;        // Grandes psitacídeos vivem até 60-80 anos
            case "REPTIL" -> 120;    // Jabutis e tartarugas gigantes
            case "PEIXE" -> 25;      // Kinguios vivem até 15-20 anos
            case "ARACNIDEO" -> 30;  // Fêmeas de tarântulas até 25-30 anos
            case "EQUINA" -> 45;     // Cavalos vivem até 30-40 anos
            default -> 35;
        };
    }

    /** Retorna o peso biológico máximo plausível em kg para a espécie. */
    public double getLimiteMaximoPesoKg() {
        if (especie == null) return 160.0;
        return switch (especie.trim().toUpperCase()) {
            case "CANINA" -> 160.0;   // Maior mastiff registrado pesava ~155 kg
            case "FELINA" -> 25.0;    // Obesidade extrema felina atinge ~20-22 kg
            case "ROEDOR" -> 5.0;     // Capivara doméstica ou rato grande
            case "MUSTELIDEO" -> 10.0;
            case "AVE" -> 15.0;
            case "REPTIL" -> 300.0;   // Tartarugas gigantes
            case "PEIXE" -> 50.0;
            case "ARACNIDEO" -> 1.0;
            case "EQUINA" -> 1200.0;  // Cavalos pesados de tração
            default -> 200.0;
        };
    }

    /** Retorna o peso biológico mínimo viável em kg para a espécie. */
    public double getLimiteMinimoPesoKg() {
        if (especie == null) return 0.2;
        return switch (especie.trim().toUpperCase()) {
            case "CANINA" -> 0.20;   // Filhote/miniatura (ex: Chihuahua)
            case "FELINA" -> 0.15;
            case "ROEDOR" -> 0.01;
            case "MUSTELIDEO" -> 0.10;
            case "AVE" -> 0.01;
            case "REPTIL" -> 0.01;
            case "PEIXE" -> 0.001;
            case "ARACNIDEO" -> 0.001;
            case "EQUINA" -> 15.0;
            default -> 0.05;
        };
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }
    public String getPropensaoDoenca() { return propensaoDoenca; }
    public void setPropensaoDoenca(String propensaoDoenca) { this.propensaoDoenca = propensaoDoenca; }
    public Integer getExpectativaVida() { return expectativaVida; }
    public void setExpectativaVida(Integer expectativaVida) { this.expectativaVida = expectativaVida; }
    public String getCuidadosEspeciais() { return cuidadosEspeciais; }
    public void setCuidadosEspeciais(String cuidadosEspeciais) { this.cuidadosEspeciais = cuidadosEspeciais; }
    public BigDecimal getPesoMedioMin() { return pesoMedioMin; }
    public void setPesoMedioMin(BigDecimal pesoMedioMin) { this.pesoMedioMin = pesoMedioMin; }
    public BigDecimal getPesoMedioMax() { return pesoMedioMax; }
    public void setPesoMedioMax(BigDecimal pesoMedioMax) { this.pesoMedioMax = pesoMedioMax; }
}
