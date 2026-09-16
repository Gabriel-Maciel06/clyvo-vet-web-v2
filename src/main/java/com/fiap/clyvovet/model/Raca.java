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

    /**
     * Retorna a faixa média formatada inteligentemente.
     * Para animais menores que 1 kg (como aves, roedores e peixes), exibe primariamente
     * em gramas (g) com o equivalente em kg para garantir clareza médica (ex: "80 a 120 g (0.08 a 0.12 kg)").
     */
    public String getPesoMedioFormatado() {
        if (pesoMedioMin != null && pesoMedioMax != null) {
            double min = pesoMedioMin.doubleValue();
            double max = pesoMedioMax.doubleValue();
            if (max < 1.0) {
                long minG = Math.round(min * 1000);
                long maxG = Math.round(max * 1000);
                return String.format(java.util.Locale.US, "%d g a %d g (%.2f a %.2f kg)", minG, maxG, min, max);
            }
            return String.format(java.util.Locale.US, "%.1f a %.1f kg", min, max);
        }
        return "Consulte faixa típica da espécie";
    }

    /** Retorna um valor médio sugerido de 1 clique para pré-preenchimento no formulário. */
    public BigDecimal getPesoMedioSugerido() {
        if (pesoMedioMin != null && pesoMedioMax != null) {
            return pesoMedioMin.add(pesoMedioMax).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }
        if ("CANINA".equalsIgnoreCase(especie)) return new BigDecimal("15.00");
        if ("FELINA".equalsIgnoreCase(especie)) return new BigDecimal("4.50");
        return new BigDecimal("5.00");
    }

    /** Retorna o limite biológico máximo plausível de idade em anos calibrado por raça/espécie. */
    public int getLimiteMaximoIdadeAnos() {
        if (nome != null) {
            String n = nome.toLowerCase();
            if (n.contains("calopsita")) return 25; // Calopsitas vivem 12-18 anos, máx ~25-30
            if (n.contains("periquito")) return 18;
            if (n.contains("papagaio") || n.contains("arara")) return 70;
            if (n.contains("twister") || n.contains("rato") || n.contains("hamster")) return 5;
            if (n.contains("furão") || n.contains("ferret")) return 12;
            if (n.contains("betta")) return 5;
            if (n.contains("kinguio")) return 20;
            if (n.contains("cobra") || n.contains("snake")) return 25;
            if (n.contains("jabuti")) return 100;
            if (n.contains("tartaruga")) return 50;
        }
        if (especie == null) return 30;
        return switch (especie.trim().toUpperCase()) {
            case "CANINA" -> 30;     // Recorde mundial canino: ~31 anos
            case "FELINA" -> 30;     // Recorde mundial felino: ~38 anos
            case "ROEDOR" -> 6;      // Pequenos roedores
            case "MUSTELIDEO" -> 14; // Furões
            case "AVE" -> 35;        // Média geral para aves
            case "REPTIL" -> 80;     // Répteis
            case "PEIXE" -> 20;      // Peixes ornamentais
            case "ARACNIDEO" -> 25;  // Tarântulas
            case "EQUINA" -> 45;     // Cavalos
            default -> 30;
        };
    }

    /** Retorna o peso biológico máximo plausível em kg para a espécie/raça. */
    public double getLimiteMaximoPesoKg() {
        if (pesoMedioMax != null && pesoMedioMax.doubleValue() < 1.0) {
            // Para animais de pequeno porte (< 1 kg: aves como calopsita, roedores, peixes),
            // tolerar até 3.5x o peso máximo da raça, garantindo bloqueio de absurdos (ex: 5 kg para calopsita)
            return Math.max(0.35, Math.round(pesoMedioMax.doubleValue() * 3.5 * 100.0) / 100.0);
        }
        if (especie == null) return 160.0;
        return switch (especie.trim().toUpperCase()) {
            case "CANINA" -> 160.0;   // Maior mastiff registrado pesava ~155 kg
            case "FELINA" -> 25.0;    // Obesidade extrema felina atinge ~20-22 kg
            case "ROEDOR" -> 5.0;     // Capivara doméstica ou roedor grande
            case "MUSTELIDEO" -> 10.0;
            case "AVE" -> 15.0;       // Aves de grande porte
            case "REPTIL" -> 300.0;   // Tartarugas gigantes
            case "PEIXE" -> 50.0;
            case "ARACNIDEO" -> 1.0;
            case "EQUINA" -> 1200.0;  // Cavalos pesados de tração
            default -> 200.0;
        };
    }

    /** Retorna o peso biológico mínimo viável em kg para a espécie. */
    public double getLimiteMinimoPesoKg() {
        if (pesoMedioMin != null && pesoMedioMin.doubleValue() < 1.0) {
            return Math.max(0.001, Math.round(pesoMedioMin.doubleValue() * 0.20 * 1000.0) / 1000.0);
        }
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
