-- ===================================================================
-- V8: Adicionar métricas de Machine Learning e Explicabilidade (XAI)
-- na Triagem Clínica (Clyvo Vet)
-- ===================================================================

ALTER TABLE T_CONSULTA_TRIAGEM ADD COLUMN probabilidade_higidez NUMERIC(5,2);
ALTER TABLE T_CONSULTA_TRIAGEM ADD COLUMN modelo_versao VARCHAR(50) DEFAULT 'CanineWellness-ML-v1.0';
ALTER TABLE T_CONSULTA_TRIAGEM ADD COLUMN fatores_xai VARCHAR(2000);

COMMENT ON COLUMN T_CONSULTA_TRIAGEM.probabilidade_higidez IS 'Probabilidade multivariada de higidez biológica calculada pelo modelo de Machine Learning (0 a 100%).';
COMMENT ON COLUMN T_CONSULTA_TRIAGEM.modelo_versao IS 'Versão do modelo estatístico/ML calibrado em execução (ex: CanineWellness-ML-v1.0).';
COMMENT ON COLUMN T_CONSULTA_TRIAGEM.fatores_xai IS 'Fatores de explicabilidade algorítmica (XAI / SHAP-like feature attribution) que influenciaram o score.';
