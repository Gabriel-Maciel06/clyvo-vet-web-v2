-- ===================================================================
-- V5: Ampliação Multi-espécie do Catálogo de Raças e Cuidados Clínicos
-- ===================================================================
-- Baseado nos compêndios e bases científicas de longevidade e medicina:
-- 1. AnAge Database (Animal Ageing and Longevity Database)
-- 2. Merck Veterinary Manual (Exotic, Avian and Large Animal Medicine)
-- 3. BSAVA Manuals of Exotic Pets, Rodents, Ferrets and Reptiles
-- ===================================================================

-- 1. Ampliar tamanho da coluna propensao_doenca para dados clínicos detalhados
ALTER TABLE T_RACA ALTER COLUMN propensao_doenca VARCHAR(500);

-- 2. Inserção de Novas Espécies e Raças
INSERT INTO T_RACA (nome, especie, propensao_doenca, expectativa_vida, cuidados_especiais) VALUES
-- AVES (especie: 'AVE')
('Calopsita', 'AVE', 'Hipovitaminose A, clamidiose (psitacose), retenção de ovos e intoxicação por vapores de teflon aquecido', 15, 'Dieta extrusada balanceada, exposição solar matinal e corte seguro de penas de voo'),
('Papagaio-verdadeiro', 'AVE', 'Lipidose hepática por excesso de sementes gordurosas, aspergilose respiratória e automutilação de penas', 50, 'Enriquecimento ambiental cognitivo, banhos regulares de aspersão e dieta rica em vegetais frescos'),
('Periquito-australiano', 'AVE', 'Neoplasias renais e gonadais, sarna cnemidocóptica (ácaro de bico) e deficiência de iodo (bócio)', 9, 'Higiene frequente de poleiros, suplementação mineral e quarentena preventiva para novas aves'),

-- RÉPTEIS (especie: 'REPTIL')
('Jabuti-piranga', 'REPTIL', 'Doença osteometabólica e piramidismo de carapaça (falta de UVB/cálcio), estomatite e pneumonia', 60, 'Lâmpada emissora de UVB 10.0, gradiente térmico de 26 a 32°C e substrato úmido de terra vegetal'),
('Tartaruga-tigre-d''água', 'REPTIL', 'Blefarite por hipovitaminose A (olhos edemaciados), podridão ulcerativa de carapaça e pneumonia', 30, 'Área seca aquecida com UVB, filtragem mecânico-biológica potente e termostato na água (26-28°C)'),
('Corn Snake (Cobra-do-milho)', 'REPTIL', 'Disecdise (retenção de muda de pele), estomatite infecciosa (podridão de boca) e criptosporidiose', 16, 'Toca úmida com esfagno para ecdise, placa térmica regulada por termostato e presas descongeladas'),

-- ROEDORES (especie: 'ROEDOR')
('Rato Twister', 'ROEDOR', 'Complexo respiratório murino (Mycoplasma pulmonis), fibroadenoma mamário e pododermatite', 3, 'Substrato hipoalergênico sem poeira (evitar serragem de pinus), gaiola ventilada e estímulo diário'),

-- MUSTELÍDEOS (especie: 'MUSTELIDEO')
('Furão (Ferret)', 'MUSTELIDEO', 'Insulinoma (tumor de células beta pancreáticas), hiperadrenocorticismo e corpo estranho gastrointestinal', 8, 'Controle rigoroso de fotoperíodo (escuridão noturna), dieta hiperproteica livre de amido e vacina de cinomose'),

-- PEIXES (especie: 'PEIXE')
('Peixe Betta', 'PEIXE', 'Apodrecimento bacteriano de nadadeiras, ictiofiriase (doença dos pontos brancos) e hidropisia', 3, 'Aquário de no mínimo 15L ciclado com aquecedor/termostato (26-28°C) e trocas parciais semanais'),
('Kinguio (Goldfish)', 'PEIXE', 'Disfunção da bexiga natatória, intoxicação por amônia/nitrito e parasitismo por vermes de âncora', 12, 'Aquário amplo com filtragem biológica superdimensionada, alimentação com grânulos que afundam e vegetais'),

-- ARACNÍDEOS (especie: 'ARACNIDEO')
('Tarântula (Caranguejeira)', 'ARACNIDEO', 'Desidratação, ruptura do opistossoma por quedas de altura e complicações durante a ecdise', 15, 'Terrário horizontal sem risco de queda, bebedouro raso com água limpa e substrato úmido de fibra de coco'),

-- EQUINOS (especie: 'EQUINA')
('Cavalo (Hipismo / Trabalho)', 'EQUINA', 'Síndrome cólica equina, laminite (aguamento), osteoartrite articular e pontas dentárias cortantes', 28, 'Dieta volumosa de boa qualidade à vontade, odontologia equina anual e ferrageamento a cada 6 semanas');
