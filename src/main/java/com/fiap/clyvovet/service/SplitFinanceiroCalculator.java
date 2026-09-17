package com.fiap.clyvovet.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fonte unica da matematica do split financeiro do marketplace.
 *
 * <p>Antes desta classe a mesma economia estava implementada duas vezes, em
 * {@code PagamentoSplitService} e em {@code MarketplaceIntermediacaoService}.
 * Duas copias da regra de dinheiro significam que uma pode divergir da outra
 * sem que nenhum teste perceba, e a banca nao tem como saber qual delas e a
 * oficial. Ambos os servicos agora delegam aqui.</p>
 *
 * <h3>Modelo: Split Bipartite com Co-financiamento de Subsidio</h3>
 * <p>O tutor e o <em>payer</em>. Ha dois <em>receivers</em>: Clyvo e Clinica.
 * O subsidio nao e um terceiro recebedor, e um abate contabil interno no
 * take-rate da Clyvo.</p>
 *
 * <h3>Priority Rule do piso de 75%</h3>
 * <p>Se o repasse preliminar violar o piso contratual, a Clyvo absorve o
 * excedente reduzindo o proprio take-rate ate zero. O desconto do tutor e
 * sempre preservado. Se ainda assim faltar valor, a diferenca e registrada
 * explicitamente em {@link Resultado#valorPrejuizoPlataforma()} em vez de ser
 * descartada — e dinheiro que a plataforma paga do proprio caixa e precisa
 * aparecer no livro-razao.</p>
 */
@Component
public class SplitFinanceiroCalculator {

    public static final BigDecimal TAXA_TAKE_RATE_PADRAO = new BigDecimal("15.00");
    public static final BigDecimal PISO_REPASSE_CLINICA_PERCENTUAL = new BigDecimal("75.00");
    public static final BigDecimal PARIDADE_SUBSIDIO_PLATAFORMA = new BigDecimal("0.50");

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    public record Resultado(
            BigDecimal valorOriginal,
            int descontoPercentual,
            BigDecimal valorDesconto,
            BigDecimal valorFinal,
            BigDecimal taxaClyvoPercentual,
            BigDecimal taxaEfetivaPercentual,
            BigDecimal valorSubsidioClyvo,
            BigDecimal valorDescontoClinica,
            BigDecimal valorComissaoClyvo,
            BigDecimal valorRepasseClinica,
            BigDecimal valorPrejuizoPlataforma,
            boolean pisoProtegidoAplicado
    ) {}

    public Resultado calcular(BigDecimal valorOriginal, int descontoPercentual) {
        return calcular(valorOriginal, descontoPercentual, TAXA_TAKE_RATE_PADRAO);
    }

    public Resultado calcular(BigDecimal valorOriginal, int descontoPercentual, BigDecimal taxaContratual) {
        BigDecimal taxa = (taxaContratual != null) ? taxaContratual : TAXA_TAKE_RATE_PADRAO;

        BigDecimal valorDesconto = valorOriginal.multiply(BigDecimal.valueOf(descontoPercentual))
                .divide(CEM, 2, RoundingMode.HALF_UP);
        BigDecimal valorFinal = valorOriginal.subtract(valorDesconto);

        // Co-financiamento: a Clyvo banca 50% do desconto, a clinica absorve o resto.
        BigDecimal valorSubsidioClyvo = valorDesconto.multiply(PARIDADE_SUBSIDIO_PLATAFORMA)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorDescontoClinica = valorDesconto.subtract(valorSubsidioClyvo);

        // Comissao contratual sobre o valor de tabela, abatida do subsidio concedido.
        BigDecimal comissaoBase = valorOriginal.multiply(taxa).divide(CEM, 2, RoundingMode.HALF_UP);
        BigDecimal valorComissaoClyvo = comissaoBase.subtract(valorSubsidioClyvo);
        BigDecimal valorRepasseClinica = valorFinal.subtract(valorComissaoClyvo);

        // Priority Rule: o piso limita a margem da Clyvo, nunca o desconto do tutor.
        BigDecimal pisoMinimo = valorOriginal.multiply(PISO_REPASSE_CLINICA_PERCENTUAL)
                .divide(CEM, 2, RoundingMode.HALF_UP);
        BigDecimal valorPrejuizoPlataforma = BigDecimal.ZERO.setScale(2);
        boolean pisoAplicado = false;

        if (valorRepasseClinica.compareTo(pisoMinimo) < 0) {
            BigDecimal excedente = pisoMinimo.subtract(valorRepasseClinica);
            valorRepasseClinica = pisoMinimo;
            pisoAplicado = true;

            BigDecimal comissaoAposAbate = valorComissaoClyvo.subtract(excedente);
            if (comissaoAposAbate.signum() < 0) {
                // A comissao zerou e ainda falta cobrir o piso: a plataforma paga a
                // diferenca do proprio caixa. Registrado, nunca descartado.
                valorPrejuizoPlataforma = comissaoAposAbate.negate();
                valorComissaoClyvo = BigDecimal.ZERO.setScale(2);
                valorSubsidioClyvo = valorSubsidioClyvo.add(excedente).subtract(valorPrejuizoPlataforma);
            } else {
                valorComissaoClyvo = comissaoAposAbate;
                valorSubsidioClyvo = valorSubsidioClyvo.add(excedente);
            }
        }

        BigDecimal taxaEfetiva = (valorOriginal.signum() > 0)
                ? valorComissaoClyvo.multiply(CEM).divide(valorOriginal, 2, RoundingMode.HALF_UP)
                : taxa;

        return new Resultado(
                valorOriginal,
                descontoPercentual,
                valorDesconto,
                valorFinal,
                taxa,
                taxaEfetiva,
                valorSubsidioClyvo,
                valorDescontoClinica,
                valorComissaoClyvo,
                valorRepasseClinica,
                valorPrejuizoPlataforma,
                pisoAplicado
        );
    }
}
