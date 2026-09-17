-- ===================================================================
-- V13: Imutabilidade Contabil de Snapshots Financeiros
--
-- Resolve a critica sobre incompatibilidade entre "3FN Estrita" e
-- auditoria fiscal em sistemas de liquidacao de marketplace.
--
-- PRINCIPIO: Tabelas financeiras de liquidacao exigem desnormalizacao
-- intencional de snapshots de valor nominal vigente no momento da
-- transacao. Qualquer alteracao posterior no catalogo (T_SERVICO.preco_base)
-- ou na taxa customizada da clinica (T_CLINICA.taxa_comissao_customizada)
-- NAO pode recalcular retroativamente valores de comissoes e repassses ja
-- liquidados — isso violaria principios de auditoria fiscal e contabil.
--
-- MODELO: "3FN com Desnormalizacao Intencional de Snapshots Financeiros"
-- ===================================================================

-- 1. Snapshots de Preco e Taxa Vigentes no Momento da Transacao (Imutaveis)
-- Estes campos registram o valor e a taxa VIGENTES no ato da captura.
-- Sao gravados uma unica vez e NUNCA devem ser atualizados por triggers ou
-- re-calculo em runtime. Constituem o livro-razao auditavel da plataforma.
ALTER TABLE T_TRANSACAO
    ADD snapshot_preco_catalogo     NUMERIC(10,2);   -- T_SERVICO.preco_base vigente no dia da transacao
ALTER TABLE T_TRANSACAO
    ADD snapshot_taxa_desconto_pct  NUMERIC(5,2);    -- Percentual de desconto de fidelidade aplicado
ALTER TABLE T_TRANSACAO
    ADD snapshot_nivel_fidelidade   VARCHAR(20);     -- Nivel do tutor (BRONZE, PRATA, OURO, DIAMANTE)

-- 2. Snapshot da Taxa de Comissao Contratual Vigente no Momento do Split
ALTER TABLE T_COMISSAO
    ADD snapshot_taxa_comissao_vigente  NUMERIC(5,2); -- T_CLINICA.taxa_comissao_customizada no dia
ALTER TABLE T_COMISSAO
    ADD snapshot_piso_repasse_pct       NUMERIC(5,2) DEFAULT 75.00; -- Piso contratual vigente

-- 3. Atualizacao do seed com os snapshots do agendamento de exemplo
-- (Consulta de R$ 180,00 - 10% PRATA = R$ 162,00 via PIX)
UPDATE T_TRANSACAO SET
    snapshot_preco_catalogo    = 180.00,
    snapshot_taxa_desconto_pct = 10.00,
    snapshot_nivel_fidelidade  = 'PRATA'
WHERE id = 1;

UPDATE T_COMISSAO SET
    snapshot_taxa_comissao_vigente = 15.00,
    snapshot_piso_repasse_pct      = 75.00
WHERE id = 1;

-- ===================================================================
-- COMENTARIO TECNICO DE CONFORMIDADE:
-- Os campos snapshot_* sao gravados pelo PagamentoSplitService no
-- momento do processarCheckout() e NUNCA recalculados. Garantem que
-- o historico financeiro seja auditavel mesmo apos alteracoes de preco
-- no catalogo T_SERVICO ou da taxa T_CLINICA.taxa_comissao_customizada.
-- ===================================================================
