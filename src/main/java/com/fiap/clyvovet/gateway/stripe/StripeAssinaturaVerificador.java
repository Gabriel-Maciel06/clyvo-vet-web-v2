package com.fiap.clyvovet.gateway.stripe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Verificador da assinatura HMAC que a Stripe envia no cabeçalho {@code Stripe-Signature}.
 *
 * <p>Sem esta checagem o endpoint de webhook e apenas um formulario publico: qualquer
 * pessoa poderia enviar um JSON dizendo {@code "payment_status": "paid"} e liberar um
 * voucher sem ter pago nada. Era exatamente esse o estado anterior.</p>
 *
 * <h3>Esquema da Stripe</h3>
 * <p>O cabecalho tem a forma {@code t=<timestamp>,v1=<hex>,v1=<hex>...}. A carga assinada
 * e a concatenacao {@code <timestamp>.<corpo bruto>}, e a assinatura esperada e o
 * HMAC-SHA256 dessa carga usando o segredo do endpoint ({@code whsec_...}) como chave.
 * Pode haver mais de um {@code v1} durante rotacao de segredo, entao basta que um
 * confira.</p>
 *
 * <p>A comparacao usa {@link MessageDigest#isEqual} (tempo constante) para nao vazar
 * informacao por tempo de resposta, e o timestamp e validado contra uma tolerancia para
 * que uma requisicao legitima capturada nao possa ser reenviada indefinidamente.</p>
 */
@Component
public class StripeAssinaturaVerificador {

    private static final Logger log = LoggerFactory.getLogger(StripeAssinaturaVerificador.class);

    /** Mesma tolerancia padrao das bibliotecas oficiais da Stripe. */
    private static final long TOLERANCIA_SEGUNDOS = 300;

    private final String webhookSecret;

    public StripeAssinaturaVerificador(
            @Value("${clyvo.gateway.stripe.webhook-secret:${CLYVO_STRIPE_WEBHOOK_SECRET:${STRIPE_WEBHOOK_SECRET:}}}")
            String webhookSecret) {
        this.webhookSecret = (webhookSecret != null) ? webhookSecret.trim() : "";
    }

    /** True quando ha um segredo configurado, ou seja, quando o webhook pode ser aceito. */
    public boolean estaConfigurado() {
        return !webhookSecret.isBlank();
    }

    /**
     * Valida a assinatura de uma notificacao.
     *
     * <p>Se nao houver segredo configurado o resultado e sempre falso. Isso e deliberado:
     * um ambiente sem segredo nao tem como distinguir a Stripe de um impostor, entao a
     * postura segura e recusar em vez de aceitar. O fluxo de demonstracao offline nao
     * depende deste endpoint — ele usa o botao de simulacao, que exige sessao do tutor
     * dono do agendamento.</p>
     *
     * @param payload         corpo bruto exatamente como recebido (qualquer reserializacao invalida a assinatura)
     * @param cabecalhoStripe conteudo do cabecalho {@code Stripe-Signature}
     */
    public boolean assinaturaValida(String payload, String cabecalhoStripe) {
        if (!estaConfigurado()) {
            log.warn("[StripeWebhook] Segredo de webhook nao configurado: notificacao recusada.");
            return false;
        }
        if (payload == null || cabecalhoStripe == null || cabecalhoStripe.isBlank()) {
            log.warn("[StripeWebhook] Notificacao sem corpo ou sem cabecalho Stripe-Signature.");
            return false;
        }

        String timestamp = null;
        List<String> assinaturas = new ArrayList<>();
        for (String parte : cabecalhoStripe.split(",")) {
            String item = parte.trim();
            int igual = item.indexOf('=');
            if (igual <= 0) continue;
            String chave = item.substring(0, igual);
            String valor = item.substring(igual + 1);
            if ("t".equals(chave)) {
                timestamp = valor;
            } else if ("v1".equals(chave)) {
                assinaturas.add(valor);
            }
        }

        if (timestamp == null || assinaturas.isEmpty()) {
            log.warn("[StripeWebhook] Cabecalho Stripe-Signature malformado.");
            return false;
        }

        long enviadoEm;
        try {
            enviadoEm = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            log.warn("[StripeWebhook] Timestamp invalido no cabecalho Stripe-Signature.");
            return false;
        }

        long diferenca = Math.abs(Instant.now().getEpochSecond() - enviadoEm);
        if (diferenca > TOLERANCIA_SEGUNDOS) {
            log.warn("[StripeWebhook] Notificacao fora da janela de tolerancia ({}s): possivel replay.", diferenca);
            return false;
        }

        String esperada = calcularHmacHex(timestamp + "." + payload);
        if (esperada == null) {
            return false;
        }

        byte[] esperadaBytes = esperada.getBytes(StandardCharsets.UTF_8);
        for (String recebida : assinaturas) {
            if (MessageDigest.isEqual(esperadaBytes, recebida.getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
        }

        log.warn("[StripeWebhook] Assinatura invalida: notificacao recusada.");
        return false;
    }

    private String calcularHmacHex(String cargaAssinada) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(cargaAssinada.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            log.error("[StripeWebhook] Falha ao calcular HMAC da assinatura: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Gera um cabecalho {@code Stripe-Signature} valido para um payload. Existe para que os
     * testes automatizados possam exercitar o caminho legitimo sem depender da Stripe.
     */
    public String gerarCabecalhoParaTeste(String payload, long epochSegundos) {
        String assinatura = calcularHmacHex(epochSegundos + "." + payload);
        return "t=" + epochSegundos + ",v1=" + assinatura;
    }
}
