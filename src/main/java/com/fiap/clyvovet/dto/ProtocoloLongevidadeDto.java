package com.fiap.clyvovet.dto;

import java.util.List;

public class ProtocoloLongevidadeDto {

    private int idadeAnos;
    private int idadeMesesRestantes;
    private int idadeHumanaEquivalente;
    private String estagioVida; // FILHOTE, JOVEM_ADULTO, ADULTO_MADURO, SENIOR, GERIATRICO
    private String estagioBadgeClass; // success, info, primary, warning, danger
    private String descricaoEstagio;
    private int percentualExpectativaVida;
    private List<String> vacinasRecomendadas;
    private List<String> examesPreventivos;
    private List<String> nutricaoESuplementos;
    private String recomendacaoAtividade;
    private String mensagemWhatsApp;

    public ProtocoloLongevidadeDto() {}

    public ProtocoloLongevidadeDto(int idadeAnos, int idadeMesesRestantes, int idadeHumanaEquivalente, String estagioVida, String estagioBadgeClass, String descricaoEstagio, int percentualExpectativaVida, List<String> vacinasRecomendadas, List<String> examesPreventivos, List<String> nutricaoESuplementos, String recomendacaoAtividade, String mensagemWhatsApp) {
        this.idadeAnos = idadeAnos;
        this.idadeMesesRestantes = idadeMesesRestantes;
        this.idadeHumanaEquivalente = idadeHumanaEquivalente;
        this.estagioVida = estagioVida;
        this.estagioBadgeClass = estagioBadgeClass;
        this.descricaoEstagio = descricaoEstagio;
        this.percentualExpectativaVida = percentualExpectativaVida;
        this.vacinasRecomendadas = vacinasRecomendadas;
        this.examesPreventivos = examesPreventivos;
        this.nutricaoESuplementos = nutricaoESuplementos;
        this.recomendacaoAtividade = recomendacaoAtividade;
        this.mensagemWhatsApp = mensagemWhatsApp;
    }

    public int getIdadeAnos() { return idadeAnos; }
    public void setIdadeAnos(int idadeAnos) { this.idadeAnos = idadeAnos; }
    public int getIdadeMesesRestantes() { return idadeMesesRestantes; }
    public void setIdadeMesesRestantes(int idadeMesesRestantes) { this.idadeMesesRestantes = idadeMesesRestantes; }
    public int getIdadeHumanaEquivalente() { return idadeHumanaEquivalente; }
    public void setIdadeHumanaEquivalente(int idadeHumanaEquivalente) { this.idadeHumanaEquivalente = idadeHumanaEquivalente; }
    public String getEstagioVida() { return estagioVida; }
    public void setEstagioVida(String estagioVida) { this.estagioVida = estagioVida; }
    public String getEstagioBadgeClass() { return estagioBadgeClass; }
    public void setEstagioBadgeClass(String estagioBadgeClass) { this.estagioBadgeClass = estagioBadgeClass; }
    public String getDescricaoEstagio() { return descricaoEstagio; }
    public void setDescricaoEstagio(String descricaoEstagio) { this.descricaoEstagio = descricaoEstagio; }
    public int getPercentualExpectativaVida() { return percentualExpectativaVida; }
    public void setPercentualExpectativaVida(int percentualExpectativaVida) { this.percentualExpectativaVida = percentualExpectativaVida; }
    public List<String> getVacinasRecomendadas() { return vacinasRecomendadas; }
    public void setVacinasRecomendadas(List<String> vacinasRecomendadas) { this.vacinasRecomendadas = vacinasRecomendadas; }
    public List<String> getExamesPreventivos() { return examesPreventivos; }
    public void setExamesPreventivos(List<String> examesPreventivos) { this.examesPreventivos = examesPreventivos; }
    public List<String> getNutricaoESuplementos() { return nutricaoESuplementos; }
    public void setNutricaoESuplementos(List<String> nutricaoESuplementos) { this.nutricaoESuplementos = nutricaoESuplementos; }
    public String getRecomendacaoAtividade() { return recomendacaoAtividade; }
    public void setRecomendacaoAtividade(String recomendacaoAtividade) { this.recomendacaoAtividade = recomendacaoAtividade; }
    public String getMensagemWhatsApp() { return mensagemWhatsApp; }
    public void setMensagemWhatsApp(String mensagemWhatsApp) { this.mensagemWhatsApp = mensagemWhatsApp; }
}
