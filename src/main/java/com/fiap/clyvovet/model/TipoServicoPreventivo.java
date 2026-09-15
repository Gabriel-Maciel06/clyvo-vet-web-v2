package com.fiap.clyvovet.model;

import java.math.BigDecimal;

public enum TipoServicoPreventivo {
    CONSULTA_PREVENTIVA(
            "Consulta Preventiva de Longevidade",
            "Avaliação clínica completa com cálculo de escore, anamnese de longevidade e exame físico.",
            new BigDecimal("150.00"),
            "bi-heart-pulse-fill",
            "#0d9488"
    ),
    PACOTE_VACINAL_COMPLETO(
            "Pacote Vacinal Preventivo Anual",
            "Imunização completa conforme diretrizes da espécie (Polivalente + Raiva + reforços sazonais).",
            new BigDecimal("180.00"),
            "bi-shield-fill-check",
            "#0284c7"
    ),
    CHECKUP_LONGEVIDADE_SENIOR(
            "Check-up Completo Sênior / Especializado",
            "Painel diagnóstico com perfil renal, hepático, glicêmico, eletrocardiograma e pressão arterial.",
            new BigDecimal("290.00"),
            "bi-activity",
            "#d97706"
    ),
    EXAMES_LABORATORIAIS_PREVENTIVOS(
            "Painel de Exames Laboratoriais Preventivos",
            "Hemograma completo, bioquímica sérica e urinálise para detecção precoce assintomática.",
            new BigDecimal("140.00"),
            "bi-clipboard2-pulse-fill",
            "#7c3aed"
    );

    private final String titulo;
    private final String descricao;
    private final BigDecimal valorBase;
    private final String icone;
    private final String corBadge;

    TipoServicoPreventivo(String titulo, String descricao, BigDecimal valorBase, String icone, String corBadge) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.valorBase = valorBase;
        this.icone = icone;
        this.corBadge = corBadge;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValorBase() {
        return valorBase;
    }

    public String getIcone() {
        return icone;
    }

    public String getCorBadge() {
        return corBadge;
    }
}
