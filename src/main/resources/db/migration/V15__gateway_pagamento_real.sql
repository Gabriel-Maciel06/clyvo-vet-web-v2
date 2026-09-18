-- ===================================================================
-- V15: Suporte a Gateway Real de Pagamento (Mercado Pago / PIX Dinâmico)
--
-- Armazena os dados de cobrança gerados pela API de pagamento:
--  - String Copia e Cola do PIX (EMV payload)
--  - Imagem do QR Code em Base64 para renderização direta na interface
--  - Link de checkout externo (quando aplicável)
--  - Timestamp de expiração da cobrança
-- ===================================================================

-- 1. Campos de Cobrança PIX e Checkout na Transação do Ledger (T_TRANSACAO)
ALTER TABLE T_TRANSACAO ADD qr_code_pix_copia_cola VARCHAR(1000);
ALTER TABLE T_TRANSACAO ADD qr_code_pix_base64 CLOB;
ALTER TABLE T_TRANSACAO ADD link_pagamento_checkout VARCHAR(500);
ALTER TABLE T_TRANSACAO ADD data_expiracao_pagamento TIMESTAMP;

-- 2. Campos de Exibição no Agendamento In-App (T_AGENDAMENTO_SERVICO)
ALTER TABLE T_AGENDAMENTO_SERVICO ADD qr_code_pix_copia_cola VARCHAR(1000);
ALTER TABLE T_AGENDAMENTO_SERVICO ADD qr_code_pix_base64 CLOB;
ALTER TABLE T_AGENDAMENTO_SERVICO ADD data_expiracao_pagamento TIMESTAMP;
