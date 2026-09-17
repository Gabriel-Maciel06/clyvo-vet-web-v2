-- ===================================================================
-- V14: Liga o Checkout In-App ao Ledger Normalizado (3FN)
--
-- PROBLEMA RESOLVIDO: ate a V13 existiam dois subsistemas financeiros
-- paralelos que nao se conversavam:
--
--   (a) Fluxo VIVO  : CheckoutController -> PagamentoSplitService
--                     grava apenas em T_AGENDAMENTO_SERVICO (desnormalizada,
--                     com o nome da clinica gravado como texto fixo no codigo).
--   (b) Fluxo DOCUMENTADO: MarketplaceIntermediacaoService
--                     grava em T_CLINICA/T_SERVICO/T_AGENDAMENTO/T_TRANSACAO/
--                     T_COMISSAO (3FN), mas nao era alcancavel por nenhum
--                     controller — so os testes o exercitavam.
--
-- Consequencia: os campos snapshot_* criados na V13 nunca eram gravados por
-- codigo Java; so existiam na linha de seed. O "livro-razao auditavel" era
-- schema morto.
--
-- ESTA MIGRACAO estabelece o vinculo que faltava para que o checkout real
-- passe a alimentar o ledger normalizado.
-- ===================================================================

-- 1. Chave de mapeamento deterministico entre o enum TipoServicoPreventivo
--    (catalogo comercial exibido no app) e a linha real de T_SERVICO.
--    Sem isso nao ha como o checkout resolver um FK de servico/clinica.
ALTER TABLE T_SERVICO ADD codigo_servico_app VARCHAR(50);

-- 2. Catalogo in-app publicado pelo Hospital Veterinario Clyvo Central (id 1).
--    Os precos sao IDENTICOS aos declarados em TipoServicoPreventivo para que
--    exista um unico preco-verdade: o que a UI mostra, o que o tutor paga e o
--    que o snapshot_preco_catalogo registra sao o mesmo numero.
INSERT INTO T_SERVICO (clinica_id, nome, descricao, categoria, preco_base, duracao_minutos, permite_desconto_fidelidade, ativo, codigo_servico_app) VALUES
(1, 'Consulta Preventiva de Longevidade', 'Avaliação clínica completa com cálculo de escore, anamnese de longevidade e exame físico.', 'CONSULTA_PREVENTIVA', 150.00, 45, TRUE, TRUE, 'CONSULTA_PREVENTIVA'),
(1, 'Pacote Vacinal Preventivo Anual', 'Imunização completa conforme diretrizes da espécie (Polivalente + Raiva + reforços sazonais).', 'VACINACAO_ANUAL', 180.00, 30, TRUE, TRUE, 'PACOTE_VACINAL_COMPLETO'),
(1, 'Check-up Completo Sênior / Especializado', 'Painel diagnóstico com perfil renal, hepático, glicêmico, eletrocardiograma e pressão arterial.', 'CHECKUP_GERIATRICO', 290.00, 60, TRUE, TRUE, 'CHECKUP_LONGEVIDADE_SENIOR'),
(1, 'Painel de Exames Laboratoriais Preventivos', 'Hemograma completo, bioquímica sérica e urinálise para detecção precoce assintomática.', 'EXAMES_LABORATORIAIS', 140.00, 45, TRUE, TRUE, 'EXAMES_LABORATORIAIS_PREVENTIVOS');

-- 3. Vinculo do voucher exibido na UI com a transacao do ledger.
--    Nulo nas linhas legadas (emitidas antes desta versao); preenchido dali
--    em diante por PagamentoSplitService.processarCheckout().
ALTER TABLE T_AGENDAMENTO_SERVICO ADD transacao_id BIGINT;
ALTER TABLE T_AGENDAMENTO_SERVICO ADD CONSTRAINT fk_agserv_transacao
    FOREIGN KEY (transacao_id) REFERENCES T_TRANSACAO(id);
CREATE INDEX idx_agserv_transacao ON T_AGENDAMENTO_SERVICO(transacao_id);

-- 4. Registro contabil do prejuizo absorvido pela plataforma quando a Priority
--    Rule do piso de 75% consome a comissao inteira e ainda falta valor.
--    Antes esse excedente era silenciosamente descartado por um .max(ZERO)
--    no calculo, sem deixar rastro no ledger.
ALTER TABLE T_COMISSAO ADD valor_prejuizo_plataforma NUMERIC(10,2) DEFAULT 0.00 NOT NULL;
ALTER TABLE T_AGENDAMENTO_SERVICO ADD valor_prejuizo_plataforma NUMERIC(10,2) DEFAULT 0.00 NOT NULL;
