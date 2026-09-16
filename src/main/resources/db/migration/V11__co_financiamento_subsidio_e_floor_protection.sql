-- ===================================================================
-- V11: Co-financiamento Paritário do Desconto e Garantia de Piso (Floor Protection)
-- Implementa no banco de dados os campos contábeis que sustentam o modelo:
-- 1. Subsídio de Take-Rate da Clyvo (abate da taxa da plataforma)
-- 2. Desconto absorvido pela Clínica (restrito ao Yield de horários ociosos)
-- 3. Taxa Efetiva Retida e Flag de Piso Protegido (mínimo de 75% da tabela)
-- ===================================================================

ALTER TABLE T_COMISSAO ADD valor_subsidio_plataforma NUMERIC(10,2) DEFAULT 0.00 NOT NULL;
ALTER TABLE T_COMISSAO ADD taxa_efetiva_percentual NUMERIC(5,2) DEFAULT 15.00 NOT NULL;
ALTER TABLE T_COMISSAO ADD piso_protegido_aplicado BOOLEAN DEFAULT FALSE NOT NULL;

ALTER TABLE T_AGENDAMENTO_SERVICO ADD valor_subsidio_clyvo NUMERIC(10,2) DEFAULT 0.00 NOT NULL;
ALTER TABLE T_AGENDAMENTO_SERVICO ADD valor_desconto_clinica NUMERIC(10,2) DEFAULT 0.00 NOT NULL;
ALTER TABLE T_AGENDAMENTO_SERVICO ADD taxa_efetiva_percentual NUMERIC(5,2) DEFAULT 15.00 NOT NULL;

-- Atualização dos registros de seed com o modelo de co-financiamento paritário:
-- Consulta de R$ 180,00 com 10% de desconto (R$ 18,00):
-- Subsídio Clyvo (50%): R$ 9,00
-- Desconto Clínica (50%): R$ 9,00
-- Comissão Base (15%): R$ 27,00 -> Comissão Líquida Clyvo: R$ 18,00 (Taxa efetiva: 10,00%)
-- Repasse Líquido Clínica: R$ 144,00 (80,00% da tabela, acima do piso de 75%)
UPDATE T_COMISSAO SET 
    valor_subsidio_plataforma = 9.00,
    taxa_efetiva_percentual = 10.00,
    valor_comissao_plataforma = 18.00,
    valor_repasse_clinica = 144.00,
    piso_protegido_aplicado = FALSE
WHERE id = 1;
