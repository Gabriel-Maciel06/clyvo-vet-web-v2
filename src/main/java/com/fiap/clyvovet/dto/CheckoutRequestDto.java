package com.fiap.clyvovet.dto;

import com.fiap.clyvovet.model.TipoServicoPreventivo;
import jakarta.validation.constraints.NotNull;

public class CheckoutRequestDto {

    @NotNull(message = "Selecione o pet para o atendimento")
    private Long petId;

    @NotNull(message = "Selecione o procedimento preventivo")
    private TipoServicoPreventivo tipoServico;

    @NotNull(message = "Selecione a forma de pagamento")
    private String metodoPagamento = "PIX";

    private String observacoes;

    public CheckoutRequestDto() {}

    public Long getPetId() {
        return petId;
    }

    public void setPetId(Long petId) {
        this.petId = petId;
    }

    public TipoServicoPreventivo getTipoServico() {
        return tipoServico;
    }

    public void setTipoServico(TipoServicoPreventivo tipoServico) {
        this.tipoServico = tipoServico;
    }

    public String getMetodoPagamento() {
        return metodoPagamento;
    }

    public void setMetodoPagamento(String metodoPagamento) {
        this.metodoPagamento = metodoPagamento;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
