package com.fiap.clyvovet.gateway;

import com.fiap.clyvovet.gateway.dto.CobrancaGeradaDto;
import com.fiap.clyvovet.gateway.dto.RequisicaoCobrancaDto;
import com.fiap.clyvovet.gateway.dto.StatusCobrancaDto;

/**
 * Contrato da Camada de Gateway de Pagamento (Adapter Pattern).
 *
 * <p>Permite alternar de forma transparente entre chamadas HTTP reais à API do
 * Mercado Pago e um fallback de simulação autônomo (para testes automatizados e
 * execuções sem credenciais externas configuradas).</p>
 */
public interface GatewayPagamentoService {

    /**
     * Gera uma cobrança no gateway (PIX com QR Code dinâmico ou sessão de checkout).
     */
    CobrancaGeradaDto criarCobranca(RequisicaoCobrancaDto requisicao);

    /**
     * Consulta o status atual de uma cobrança no gateway.
     */
    StatusCobrancaDto consultarStatus(String idTransacaoGateway);

    /**
     * Simula a liquidação imediata da cobrança (exclusivo para ambiente de teste/sandbox).
     */
    StatusCobrancaDto simularPagamento(String idTransacaoGateway);

    /**
     * Indica se a implementação ativa está operando em modo simulado.
     */
    boolean isModoSimulado();

    /**
     * Nome identificador do provedor ativo (ex: "MERCADO_PAGO", "SIMULADO").
     */
    String getNomeProvedor();
}
