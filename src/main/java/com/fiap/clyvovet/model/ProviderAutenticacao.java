package com.fiap.clyvovet.model;

/**
 * Origem da autenticação de um {@link Usuario}: conta local (usuário/senha
 * cadastrados na plataforma) ou conta provisionada via login social.
 */
public enum ProviderAutenticacao {
    LOCAL("Cadastro local"),
    GOOGLE("Google");

    private final String descricao;

    ProviderAutenticacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
