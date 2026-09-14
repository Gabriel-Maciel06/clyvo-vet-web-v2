-- ===================================================================
-- V3: Inserção de Dados Iniciais (Clyvo Vet - Gamificação & Clínico)
-- ===================================================================

-- 1. Usuários do Sistema
-- admin (Veterinário): admin123
-- tutor (Tutor Pet): tutor123
INSERT INTO T_USUARIO (username, password, role, nome_completo, email) VALUES
('admin', '$2a$10$juVqTpq9GgTfJ1E1GQnFme6SjpavbXh/0gOPdRuZ0rp3nSD/mFXWO', 'ROLE_ADMIN', 'Dr. Carlos Mendes (Médico Veterinário)', 'carlos.vet@clyvovet.com.br'),
('tutor', '$2a$10$AMufPFMEx1iLUlUDXsao2Oq55rJT0ZLQDnRubtq35Vhw9S4r1D5q.', 'ROLE_TUTOR', 'Gabriel Maciel (Tutor Responsável)', 'gabriel.tutor@gmail.com');

-- 2. Raças de Pets com Propensão Genética
INSERT INTO T_RACA (nome, especie, propensao_doenca, expectativa_vida, cuidados_especiais) VALUES
('Golden Retriever', 'CANINA', 'Displasia coxofemoral, cardiopatias e obesidade', 12, 'Controle rigoroso de peso, atividade física moderada e exames cardiológicos anuais'),
('Buldogue Francês', 'CANINA', 'Síndrome braquiocefálica, dermatites e problemas de coluna', 11, 'Evitar calor excessivo, limpeza diária de dobras e controle respiratório'),
('Poodle Médio', 'CANINA', 'Problemas oftalmológicos (catarata) e tártaro dentário', 15, 'Higiene bucal diária e avaliação oftalmológica preventiva'),
('Labrador Retriever', 'CANINA', 'Artrose, displasia e propensão a ganho de peso', 13, 'Suplementação articular preventiva e dieta balanceada com baixo teor calórico'),
('SRD (Vira-lata)', 'CANINA', 'Geralmente resistente; propensões dependem do porte', 14, 'Vacinação em dia, vermifugação e check-ups preventivos regulares'),
('Siamês', 'FELINA', 'Insuficiência renal crônica e problemas respiratórios', 16, 'Estímulo constante à ingestão hídrica (fontes de água) e exames renais precoces'),
('Persa', 'FELINA', 'Doença renal policística e problemas respiratórios/oculares', 14, 'Limpeza facial frequente, escovação diária e ultrassom renal anual');

-- 3. Tutor Cadastrado (Vinculado ao usuário 'tutor')
INSERT INTO T_TUTOR (cpf, nome, telefone, email, usuario_id) VALUES
('123.456.789-00', 'Gabriel Maciel', '(11) 98765-4321', 'gabriel.tutor@gmail.com', 2);

-- 4. Recompensa Inicial do Tutor (Gamificação & Fidelidade)
INSERT INTO T_RECOMPENSA_TUTOR (tutor_cpf, pontos_acumulados, streak_dias, ultimo_checkin, desconto_percentual, nivel_fidelidade) VALUES
('123.456.789-00', 230, 5, CURRENT_DATE - 1, 10, 'PRATA'); -- PRATA (>=100 pts); último check-in ontem: o próximo check-in de 20 pts sobe para OURO (250 pts) e streak 6

-- 5. Clínicas Parceiras
INSERT INTO T_CLINICA (nome_cnpj, telefone, cidade, estado, atendimento_24h) VALUES
('Hospital Veterinário Clyvo Central', '(11) 3456-7890', 'São Paulo', 'SP', TRUE),
('Clínica Preventiva Pet Care Jardins', '(11) 3012-4567', 'São Paulo', 'SP', FALSE);

-- 6. Pets do Tutor
INSERT INTO T_PET (nome, data_nascimento, peso, status_longevidade, escore_saude, raca_id, tutor_cpf) VALUES
('Thor', '2021-05-15', 32.50, 'Escore de longevidade excelente (92/100). Articulações saudáveis e rotina diária em dia.', 92, 1, '123.456.789-00'),
('Luna', '2022-09-20', 12.80, 'Atenção preventiva ao trato respiratório e controle de peso (82/100).', 82, 2, '123.456.789-00');

-- 7. Badges Conquistados pelo Pet Thor
INSERT INTO T_BADGE_CONQUISTA (pet_id, codigo_badge, nome_badge, icone, descricao, data_conquista) VALUES
(1, 'STREAK_5', 'Guardião Fiel - 5 Dias', 'bi-award-fill', 'Completou 5 dias consecutivos de check-in de cuidados.', CURRENT_DATE - 1),
(1, 'VIDA_ATIVA', 'Atleta Canino', 'bi-lightning-charge-fill', 'Mais de 45 minutos diários de caminhadas e atividades físicas.', CURRENT_DATE - 3);

-- 8. Check-in Diário de Cuidado Recente (Linha do Tempo)
INSERT INTO T_CHECKIN_DIARIO (pet_id, data_checkin, alimentacao_status, remedio_administrado, minutos_atividade, humor_pet, sintomas_observados, pontos_ganhos, alerta_gerado) VALUES
(1, CURRENT_DATE - 1, 'RECOMENDADA', TRUE, 50, 'ENERGICO', 'Animal muito bem disposto, sem claudicação.', 20, FALSE),
(2, CURRENT_DATE - 1, 'PETISCOS_MODERADOS', FALSE, 20, 'TRANQUILO', 'Apresentou leve respiração ofegante ao subir escadas.', 15, TRUE);

-- 9. Consulta e Triagem Preventiva (Fluxo 2)
INSERT INTO T_CONSULTA_TRIAGEM (pet_id, data_solicitacao, data_consulta, status, queixa_principal, peso_aferido, temperatura, frequencia_cardiaca, escore_longevidade, classificacao_risco, insight_ia, parecer_veterinario, veterinario_id) VALUES
(1, CURRENT_TIMESTAMP - INTERVAL '10' DAY, CURRENT_TIMESTAMP - INTERVAL '8' DAY, 'CONCLUIDA', 'Avaliação preventiva anual de longevidade e articulações', 32.50, 38.5, 95, 92, 'BAIXO', 'Golden Retriever de 5 anos com excelente adesão ao check-in diário. Risco osteoarticular baixo.', 'Animal em excelente estado nutricional e osteoarticular. Manter suplementação ômega-3.', 1),
(2, CURRENT_TIMESTAMP - INTERVAL '1' DAY, NULL, 'SOLICITADA', 'Dificuldade para respirar após passeios e roncos noturnos', NULL, NULL, NULL, NULL, NULL, 'Alerta preditivo: Buldogue Francês com queixa respiratória. Indicada triagem do palato mole e estenose.', NULL, NULL);

-- 10. Histórico Clínico Consolidado
INSERT INTO T_HISTORICO_CLINICO (pet_id, data_registro, tipo_evento, descricao, conduta_adotada) VALUES
(1, CURRENT_TIMESTAMP - INTERVAL '1' DAY, 'CHECKIN_DIARIO', 'Check-in diário realizado: Humor Enérgico, Dieta Recomendada, Atividade 50 min.', 'Parâmetros diários dentro da normalidade de prevenção e longevidade. +20 pts de fidelidade acumulados.'),
(1, CURRENT_TIMESTAMP - INTERVAL '8' DAY, 'TRIAGEM_PREVENTIVA', 'Triagem de longevidade concluída com escore 92/100.', 'Prescrito protocolo de suplementação ômega-3 e natação preventiva pelo Dr. Carlos Mendes.'),
(1, CURRENT_TIMESTAMP - INTERVAL '30' DAY, 'CADASTRO_PET', 'Pet cadastrado no ecossistema Clyvo Vet.', 'Início do plano contínuo de longevidade e monitoramento de saúde.'),
(2, CURRENT_TIMESTAMP - INTERVAL '1' DAY, 'ALERTA_SAUDE', 'Alerta gerado no check-in diário: respiração ofegante ao subir escadas.', 'ALERTA CLÍNICO: Notificação automática emitida ao Hospital Veterinário Clyvo Central. Indicada triagem respiratória emergencial.'),
(2, CURRENT_TIMESTAMP - INTERVAL '2' DAY, 'CHECKIN_DIARIO', 'Check-in diário realizado: Humor Tranquilo, Petiscos moderados, Atividade 20 min.', 'Parâmetros diários estáveis. Tutor orientado sobre controle de calor e hidratação.'),
(2, CURRENT_TIMESTAMP - INTERVAL '20' DAY, 'CADASTRO_PET', 'Pet cadastrado no ecossistema Clyvo Vet.', 'Início do acompanhamento profilático para Buldogue Francês.');

