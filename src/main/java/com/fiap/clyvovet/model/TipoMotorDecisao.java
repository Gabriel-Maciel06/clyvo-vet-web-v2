package com.fiap.clyvovet.model;

/**
 * Declaração explícita do paradigma computacional e clínico empregado na tomada de decisão.
 * Elimina maquiagem tecnológica (AI-washing) distinguindo formalmente inferência estatística empírica
 * de sistemas especialistas baseados em consensos veterinários.
 */
public enum TipoMotorDecisao {
    MACHINE_LEARNING_SUPERVISIONADO(
            "Machine Learning Preditivo",
            "bg-success",
            "Modelo probabilístico multivariado treinado com dados empíricos (Canine Wellness Dataset 10k amostras, ROC-AUC 0.9485)."
    ),
    SISTEMA_ESPECIALISTA_FISIOLOGICO(
            "Sistema Especialista Baseado em Diretrizes",
            "bg-info text-dark",
            "Motor determinístico fundamentado em consensos de Medicina Veterinária Zoológica e Fisiologia Comparada (Conformidade Base 100)."
    );

    private final String descricao;
    private final String badgeClass;
    private final String explicacao;

    TipoMotorDecisao(String descricao, String badgeClass, String explicacao) {
        this.descricao = descricao;
        this.badgeClass = badgeClass;
        this.explicacao = explicacao;
    }

    public String getDescricao() { return descricao; }
    public String getBadgeClass() { return badgeClass; }
    public String getExplicacao() { return explicacao; }
}
