-- ===================================================================
-- V12: Adicionar tipo_motor para suporte ao Padrão Strategy e
-- segregação formal de Machine Learning vs Sistema Especialista
-- ===================================================================

ALTER TABLE T_CONSULTA_TRIAGEM ADD COLUMN tipo_motor VARCHAR(50) DEFAULT 'MACHINE_LEARNING_SUPERVISIONADO';

COMMENT ON COLUMN T_CONSULTA_TRIAGEM.tipo_motor IS 'Identificador do paradigma de inferência aplicado (MACHINE_LEARNING_SUPERVISIONADO ou SISTEMA_ESPECIALISTA_FISIOLOGICO).';
