-- ===================================================================
-- V9: Adicionar Faixas de Peso Médio e Parâmetros Biométricos em T_RACA
-- ===================================================================

ALTER TABLE T_RACA ADD COLUMN peso_medio_min NUMERIC(5,2);
ALTER TABLE T_RACA ADD COLUMN peso_medio_max NUMERIC(5,2);

COMMENT ON COLUMN T_RACA.peso_medio_min IS 'Peso corporal mínimo típico para animais adultos saudáveis da raça (em kg).';
COMMENT ON COLUMN T_RACA.peso_medio_max IS 'Peso corporal máximo típico para animais adultos saudáveis da raça (em kg).';

-- 1. Caninos (V3 e V7)
UPDATE T_RACA SET peso_medio_min = 27.00, peso_medio_max = 36.00 WHERE nome LIKE '%Golden Retriever%';
UPDATE T_RACA SET peso_medio_min = 8.00,  peso_medio_max = 14.00 WHERE nome LIKE '%Buldogue Francês%';
UPDATE T_RACA SET peso_medio_min = 9.00,  peso_medio_max = 15.00 WHERE nome LIKE '%Poodle Médio%';
UPDATE T_RACA SET peso_medio_min = 25.00, peso_medio_max = 36.00 WHERE nome LIKE '%Labrador Retriever%';
UPDATE T_RACA SET peso_medio_min = 10.00, peso_medio_max = 25.00 WHERE nome LIKE '%SRD (Vira-lata)%';
UPDATE T_RACA SET peso_medio_min = 10.00, peso_medio_max = 25.00 WHERE nome LIKE '%Cão (Geral%';

-- 2. Felinos (V3 e V7)
UPDATE T_RACA SET peso_medio_min = 3.50,  peso_medio_max = 5.50  WHERE nome LIKE '%Siamês%';
UPDATE T_RACA SET peso_medio_min = 3.50,  peso_medio_max = 6.00  WHERE nome LIKE '%Persa%';
UPDATE T_RACA SET peso_medio_min = 3.50,  peso_medio_max = 5.50  WHERE nome LIKE '%Gato (Geral%';

-- 3. Aves (V5 e V7)
UPDATE T_RACA SET peso_medio_min = 0.08,  peso_medio_max = 0.12  WHERE nome LIKE '%Calopsita%';
UPDATE T_RACA SET peso_medio_min = 0.38,  peso_medio_max = 0.45  WHERE nome LIKE '%Papagaio-verdadeiro%';
UPDATE T_RACA SET peso_medio_min = 0.03,  peso_medio_max = 0.05  WHERE nome LIKE '%Periquito-australiano%';
UPDATE T_RACA SET peso_medio_min = 0.08,  peso_medio_max = 0.45  WHERE nome LIKE '%Ave (Geral%';

-- 4. Répteis (V5 e V7)
UPDATE T_RACA SET peso_medio_min = 5.00,  peso_medio_max = 12.00 WHERE nome LIKE '%Jabuti-piranga%';
UPDATE T_RACA SET peso_medio_min = 1.00,  peso_medio_max = 2.50  WHERE nome LIKE '%Tartaruga-tigre%';
UPDATE T_RACA SET peso_medio_min = 0.40,  peso_medio_max = 0.90  WHERE nome LIKE '%Corn Snake%';
UPDATE T_RACA SET peso_medio_min = 1.00,  peso_medio_max = 10.00 WHERE nome LIKE '%Réptil (Geral%';

-- 5. Roedores (V5 e V7)
UPDATE T_RACA SET peso_medio_min = 0.25,  peso_medio_max = 0.55  WHERE nome LIKE '%Rato Twister%';
UPDATE T_RACA SET peso_medio_min = 0.10,  peso_medio_max = 0.50  WHERE nome LIKE '%Roedor (Geral%';

-- 6. Mustelídeos (V5 e V7)
UPDATE T_RACA SET peso_medio_min = 0.70,  peso_medio_max = 2.00  WHERE nome LIKE '%Furão%';
UPDATE T_RACA SET peso_medio_min = 0.70,  peso_medio_max = 2.00  WHERE nome LIKE '%Mustelídeo (Geral%';

-- 7. Peixes (V5 e V7)
UPDATE T_RACA SET peso_medio_min = 0.005, peso_medio_max = 0.015 WHERE nome LIKE '%Betta%';
UPDATE T_RACA SET peso_medio_min = 0.05,  peso_medio_max = 0.25  WHERE nome LIKE '%Kinguio%';
UPDATE T_RACA SET peso_medio_min = 0.01,  peso_medio_max = 0.20  WHERE nome LIKE '%Peixe (Geral%';

-- 8. Aracnídeos (V5 e V7)
UPDATE T_RACA SET peso_medio_min = 0.02,  peso_medio_max = 0.08  WHERE nome LIKE '%Tarântula%';
UPDATE T_RACA SET peso_medio_min = 0.02,  peso_medio_max = 0.08  WHERE nome LIKE '%Aracnídeo (Geral%';

-- 9. Equinos (V5 e V7)
UPDATE T_RACA SET peso_medio_min = 400.00, peso_medio_max = 600.00 WHERE nome LIKE '%Cavalo%';
UPDATE T_RACA SET peso_medio_min = 400.00, peso_medio_max = 600.00 WHERE nome LIKE '%Equino (Geral%';
