package com.fiap.clyvovet.gateway.simulado;

import com.fiap.clyvovet.gateway.GatewayPagamentoService;
import com.fiap.clyvovet.gateway.dto.CobrancaGeradaDto;
import com.fiap.clyvovet.gateway.dto.RequisicaoCobrancaDto;
import com.fiap.clyvovet.gateway.dto.StatusCobrancaDto;
import com.fiap.clyvovet.model.StatusTransacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provedor Simulado Autônomo de Pagamentos (Fallback Resiliente).
 *
 * <p>Gera payloads PIX válidos no formato EMV (BR Code) e SVG de QR Code para visualização
 * imediata, permitindo que a aplicação funcione em 100% dos seus fluxos mesmo sem acesso
 * à internet ou credenciais de produção.</p>
 */
@Service
public class SimuladoGatewayService implements GatewayPagamentoService {

    private static final Logger log = LoggerFactory.getLogger(SimuladoGatewayService.class);

    private final Map<String, StatusTransacao> transacoesEmMemoria = new ConcurrentHashMap<>();

    @Override
    public CobrancaGeradaDto criarCobranca(RequisicaoCobrancaDto requisicao) {
        String idTransacao = "SIM-GW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        transacoesEmMemoria.put(idTransacao, StatusTransacao.PENDENTE);

        String valorFormatado = requisicao.valor() != null ? requisicao.valor().toPlainString() : "0.00";
        String payloadPix = "00020126580014br.gov.bcb.pix0136clyvo-pix-" + idTransacao.toLowerCase()
                + "520400005303986540" + valorFormatado.length() + valorFormatado
                + "5802BR5916CLYVO VET PLAT6009SAO PAULO62070503***6304ABCD";

        String svgQr = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 200' width='200' height='200'>"
                + "<rect width='200' height='200' fill='#ffffff'/>"
                + "<rect x='20' y='20' width='50' height='50' fill='#0f172a'/>"
                + "<rect x='30' y='30' width='30' height='30' fill='#ffffff'/>"
                + "<rect x='37' y='37' width='16' height='16' fill='#0f172a'/>"
                + "<rect x='130' y='20' width='50' height='50' fill='#0f172a'/>"
                + "<rect x='140' y='30' width='30' height='30' fill='#ffffff'/>"
                + "<rect x='147' y='37' width='16' height='16' fill='#0f172a'/>"
                + "<rect x='20' y='130' width='50' height='50' fill='#0f172a'/>"
                + "<rect x='30' y='140' width='30' height='30' fill='#ffffff'/>"
                + "<rect x='37' y='147' width='16' height='16' fill='#0f172a'/>"
                + "<circle cx='100' cy='100' r='18' fill='#10b981'/>"
                + "<text x='100' y='105' font-size='12' font-family='sans-serif' font-weight='bold' fill='#ffffff' text-anchor='middle'>PIX</text>"
                + "</svg>";

        String qrCodeBase64 = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svgQr.getBytes(StandardCharsets.UTF_8));

        log.info("[SimuladoGateway] Cobrança PIX gerada: ID={}, Valor=R$ {}", idTransacao, valorFormatado);

        return new CobrancaGeradaDto(
                idTransacao,
                StatusTransacao.PENDENTE,
                payloadPix,
                qrCodeBase64,
                null,
                LocalDateTime.now().plusMinutes(15),
                "Cobrança PIX simulada gerada com sucesso",
                true
        );
    }

    @Override
    public StatusCobrancaDto consultarStatus(String idTransacaoGateway) {
        StatusTransacao status = transacoesEmMemoria.getOrDefault(idTransacaoGateway, StatusTransacao.PENDENTE);
        boolean pago = status == StatusTransacao.PAGO;
        return new StatusCobrancaDto(
                idTransacaoGateway,
                status,
                pago,
                pago ? LocalDateTime.now() : null,
                "Status obtido via Gateway Simulado"
        );
    }

    @Override
    public StatusCobrancaDto simularPagamento(String idTransacaoGateway) {
        transacoesEmMemoria.put(idTransacaoGateway, StatusTransacao.PAGO);
        log.info("[SimuladoGateway] Pagamento liquidado via simulação para ID={}", idTransacaoGateway);
        return new StatusCobrancaDto(
                idTransacaoGateway,
                StatusTransacao.PAGO,
                true,
                LocalDateTime.now(),
                "Pagamento simulado com sucesso no sandbox"
        );
    }

    @Override
    public boolean isModoSimulado() {
        return true;
    }

    @Override
    public String getNomeProvedor() {
        return "SIMULADO_OFFLINE";
    }
}
