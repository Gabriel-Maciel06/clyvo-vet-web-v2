-- ===================================================================
-- V7: Categorias e Raças Gerais (Fallback com Dados Científicos Médios)
-- ===================================================================
-- Permite que o tutor selecione a categoria/título da espécie quando
-- o animal for de outra raça, sem raça definida (SRD) ou espécie não listada.
-- ===================================================================

INSERT INTO T_RACA (nome, especie, propensao_doenca, expectativa_vida, cuidados_especiais) VALUES
-- 1. CANINA (Geral / SRD / Outra Raça)
('Cão (Geral / SRD / Outra Raça)', 'CANINA', 
 'Predisposições patológicas dependem do porte (pequeno, médio ou grande). Avaliação fenotípica preventiva recomendada.', 
 13, 
 'Vacinação anual polivalente (V10) e antirrábica, vermifugação semestral, controle de ectoparasitas e exames anuais.'),

-- 2. FELINA (Geral / SRD / Outra Raça)
('Gato (Geral / SRD / Outra Raça)', 'FELINA', 
 'Doença renal crônica felina, gengivoestomatite, cardiopatia hipertrófica e propensão a ganho de peso pós-castração.', 
 15, 
 'Estímulo contínuo à ingestão hídrica com fontes de água, ração úmida de qualidade, arranhadores verticais e ultrassom renal.'),

-- 3. AVE (Geral / Outra Espécie)
('Ave (Geral / Outra Espécie)', 'AVE', 
 'Hipovitaminose A por dieta exclusiva de sementes, afecções respiratórias por aerossóis/teflon aquecido e retenção de ovos.', 
 15, 
 'Dieta extrusada própria para a espécie, banhos de aspersão, exposição solar matinal direta (sem vidro) e recinto telado seguro.'),

-- 4. RÉPTIL (Geral / Outra Espécie)
('Réptil (Geral / Outra Espécie)', 'REPTIL', 
 'Doença osteometabólica nutricional (falta de UVB/cálcio), estomatite infecciosa e pneumonias por temperatura inadequada.', 
 25, 
 'Gradiente térmico controlado por termostato dia/noite, fonte emissora de radiação UVB calibrada e alimentação balanceada.'),

-- 5. ROEDOR (Geral / Hamster / Chinchila / Outro)
('Roedor (Geral / Hamster / Chinchila / Outro)', 'ROEDOR', 
 'Infecções respiratórias de vias aéreas superiores, má oclusão dentária dos incisivos e neoplasias mamárias.', 
 4, 
 'Substrato hipoalergênico livre de pó (evitar serragem de pinus), feno fresco à vontade para desgaste dentário e recinto ventilado.'),

-- 6. MUSTELÍDEO (Geral / Outro)
('Mustelídeo (Geral / Outro)', 'MUSTELIDEO', 
 'Neoplasia de células beta pancreáticas (insulinoma), hiperadrenocorticismo e ingestão de corpos estranhos de borracha/plástico.', 
 8, 
 'Dieta carnívora estrita hiperproteica livre de carboidratos, fotoperíodo controlado com escuridão noturna e vacina de cinomose.'),

-- 7. PEIXE (Geral / Ornamental)
('Peixe (Geral / Ornamental)', 'PEIXE', 
 'Intoxicação por compostos nitrogenados (amônia tóxica e nitrito), ictiofiriase parasitária e infecções de nadadeira.', 
 5, 
 'Aquário maturado com filtragem biológica eficiente, trocas parciais de água (TPA) semanais com anticloro e temperatura estável.'),

-- 8. ARACNÍDEO (Geral / Outro)
('Aracnídeo (Geral / Outro)', 'ARACNIDEO', 
 'Desidratação fatal do cefalotórax, traumas e ruptura por queda de altura e complicações durante a ecdise (troca de pele).', 
 10, 
 'Terrário horizontal sem risco de quedas, substrato de fibra de coco com umidade controlada e bebedouro raso com água fresca.'),

-- 9. EQUINO (Geral / Outra Raça / Mestiço)
('Equino (Geral / Outra Raça / Mestiço)', 'EQUINA', 
 'Síndrome cólica por alteração dietética, laminite (aguamento), pontas cortantes de esmalte dentário e osteoartrite.', 
 25, 
 'Fornecimento contínuo de volumoso de boa qualidade, água limpa à vontade, casqueamento a cada 6 semanas e odontologia anual.'),

-- 10. OUTRO (Espécie Não Listada / Silvestre / Exótico)
('Outro Animal (Espécie Não Listada)', 'OUTRO', 
 'Predisposições clínicas variam conforme a biologia da espécie. Necessita de anamnese profilática detalhada de manejo.', 
 10, 
 'Consulta e protocolo de longevidade individualizado com médico veterinário especialista em animais silvestres e exóticos.');
