package com.fiap.clyvovet.model;

public enum RoleUsuario {
    ROLE_ADMIN("Veterinário / Administrador"),
    ROLE_TUTOR("Tutor de Pet");

    private final String descricao;

    RoleUsuario(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
