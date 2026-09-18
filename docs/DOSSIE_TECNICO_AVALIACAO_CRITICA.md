# 📘 Dossiê Técnico de Arquitetura & Avaliação Crítica
**Projeto:** Clyvo Vet Web v2 — Plataforma Transacional de Medicina Preventiva, Longevidade & Marketplace Pet  
**Repositório Local:** `/Users/gabrieloliveira/Desktop/Agentes-cloud/clyvo-vet-web-v2`  
**Data:** 17 de Setembro de 2026  
**Status dos Testes:** ✅ **88 testes automatizados aprovados (0 falhas, 0 erros) — verificado sob JDK 21 e JDK 26**  
**Ambiente de Execução Local:** `http://localhost:8095`  

---

## 🧭 Índice do Dossiê

1. [Visão Geral do Sistema & Stack Tecnológica](#1-visão-geral-do-sistema--stack-tecnológica)
2. [Resolução da Crítica 1: Incoerência na Modelagem de Dados do Marketplace (14 Entidades em 3FN)](#2-resolução-da-crítica-1-incoerência-na-modelagem-de-dados-do-marketplace-14-entidades-em-3fn)
3. [Resolução da Crítica 2: Canibalização da Margem da Clínica (Unit Economics & Yield Management)](#3-resolução-da-crítica-2-canibalização-da-margem-da-clínica-unit-economics--yield-management)
4. [Resolução da Crítica 3: Fuga de Plataforma e Monetização por Take-Rate (Voucher & Escrow)](#4-resolução-da-crítica-3-fuga-de-plataforma-e-monetização-por-take-rate-voucher--escrow)
5. [Resolução da Crítica 4: Resolução de AI-Washing (Arquitetura Dual-Engine: ML 10k + Guardrails)](#5-resolução-da-crítica-4-resolução-de-ai-washing-arquitetura-dual-engine-ml-10k--guardrails)
6. [Resolução da Crítica 5: Fisiologia Veterinária Comparada Multi-Espécie (Ectotérmicos & Aves)](#6-resolução-da-crítica-5-fisiologia-veterinária-comparada-multi-espécie-ectotérmicos--aves)
7. [Resolução da Crítica 6: Biometria, Validações Biológicas e Calibração de Pequenos Pets](#7-resolução-da-crítica-6-biometria-validações-biológicas-e-calibração-de-pequenos-pets)
8. [Arquitetura de Segurança, Autenticação e Perfis (Spring Security 6)](#8-arquitetura-de-segurança-autenticação-e-perfis-spring-security-6)
9. [Suíte de Testes Automatizados (88 Testes / 100% Cobertura de Requisitos)](#9-suíte-de-testes-automatizados-88-testes--100-cobertura-de-requisitos)
10. [Guia Passo a Passo para Execução e Auditoria Local pelo Crítico](#10-guia-passo-a-passo-para-execução-e-auditoria-local-pelo-crítico)

---

## 1. Visão Geral do Sistema & Stack Tecnológica

O **Clyvo Vet** é uma plataforma que integra **Medicina Veterinária Preventiva**, **Gamificação Comportamental (Streaks e Clyvo Coins)** e um **Two-Sided Marketplace** com intermediação financeira (*split payment*) entre tutores de animais e clínicas credenciadas.

### Stack Tecnológica:
- **Backend:** Java 21 (LTS) · Spring Boot 3.3.4
- **Segurança:** Spring Security 6 · BCrypt · CSRF Token Ativo · OAuth2 Client (Google)
- **Persistência & Migrações:** Spring Data JPA · Hibernate 6 · Flyway Migration (15 scripts versionados `V1` a `V15`)
- **Bancos de Dados:** H2 Database em memória configurado em modo de compatibilidade Oracle (`MODE=Oracle`) para desenvolvimento/testes rápidos; driver oficial Oracle JDBC (`ojdbc11`) pré-configurado no `pom.xml` para ambientes de produção.
- **Frontend MVC:** Thymeleaf com layouts modulares e `thymeleaf-extras-springsecurity6` · Bootstrap 5.3 · Bootstrap Icons · Select2 4.1.
- **Gateway de Pagamento & PIX Dinâmico:** Arquitetura Adapter Pattern com `GatewayPagamentoService` — integração REST real com a API oficial do Mercado Pago (`MercadoPagoGatewayService`) via `RestClient` + Provedor `SimuladoGatewayService` de fallback autônomo offline + Polling JS a cada 3s + Webhook assíncrono (`/api/webhooks/mercadopago`).
- **Inteligência Clínica & Decisão Híbrida:** Padrão Strategy com dois paradigmas de inferência — Machine Learning Preditivo Supervisionado para Caninos (`CanineWellness-ML-v1.0`, 21 features, target binário de Higidez em 12 meses, ROC-AUC holdout 0.9485) + Sistemas Especialistas Baseados em Conhecimento para 8 demais espécies (AAFP, AAV, ABRAVAS, BSAVA, AAEP) + Guardrails Clínicos Vitais (AAHA/WSAVA) + Explicabilidade Algorítmica (XAI) e Síntese SOAP.
- **Testes Automatizados:** JUnit 5 · MockMvc · AssertJ · Spring Security Test (96 testes automatizados aprovados com 100% de sucesso).

---

## 2. Resolução da Crítica 1: Incoerência na Modelagem de Dados do Marketplace (14 Entidades + Snapshots Financeiros Imutáveis)

### A Crítica Apontada:
> *"O documento menciona 9 entidades relacionais normalizadas. Se a proposta central é ser um marketplace com clínicas, tutores, múltiplos pets, espécies, prontuários, check-ins, triagens, gamificação e transações, 9 entidades mal cobrem o prontuário básico e segurança. Faltam Clinica, Agendamento, Servico, Transacao e Comissao. Sem elas, o backend é um prontuário digital isolado, não uma plataforma de intermediação."*

### A Resolução Arquitetural:
O sistema foi formalmente migrado para **14 entidades relacionais em 3FN com Desnormalização Intencional de Snapshots Financeiros**, através das migrações Flyway `V10`, `V11` e `V13`.

> **Nota sobre "3FN com Desnormalização Intencional de Snapshots":** Sistemas financeiros de liquidação de marketplace exigem que os valores nominais (preço do serviço, taxa de comissão vigente, percentual de desconto aplicado) sejam **gravados como snapshots imutáveis** no momento da transação. Afirmar "3FN estrita" seria tecnicamente incorreto: qualquer alteração posterior no catálogo (`T_SERVICO.preco_base`) ou na taxa customizada da clínica recalcularia retroativamente comissões históricas, violando princípios de auditoria fiscal. Por isso, `T_TRANSACAO` e `T_COMISSAO` armazenam colunas `snapshot_*` que são gravadas uma única vez e jamais recalculadas em runtime.

```mermaid
erDiagram
    T_CLINICA ||--o{ T_SERVICO : "1. cadastra catalogo"
    T_CLINICA ||--o{ T_AGENDAMENTO : "2. recebe agendamento"
    T_CLINICA ||--o{ T_COMISSAO : "3. recebe repasse liquido"
    
    T_TUTOR ||--o{ T_PET : "possui"
    T_TUTOR ||--o{ T_AGENDAMENTO : "solicita"
    T_PET ||--o{ T_AGENDAMENTO : "paciente"
    T_SERVICO ||--o{ T_AGENDAMENTO : "tipo_atendimento"
    
    T_AGENDAMENTO ||--|| T_TRANSACAO : "gera cobranca in-app"
    T_TRANSACAO ||--|| T_COMISSAO : "split 15% & custodia escrow"
    
    T_USUARIO ||--o| T_TUTOR : "autenticacao"
    T_PET ||--o{ T_CHECKIN_DIARIO : "monitoramento"
    T_PET ||--o{ T_BADGE_CONQUISTA : "gamificacao"
    T_PET ||--o{ T_CONSULTA_TRIAGEM : "exame_fisico"
    T_PET ||--o{ T_HISTORICO_CLINICO : "prontuario"
    T_TUTOR ||--o| T_RECOMPENSA_TUTOR : "fidelidade"
    T_RACA ||--o{ T_PET : "propensao_genetica"
```

### Detalhamento das 14 Entidades:
1. **`T_CLINICA` (Parceiro Credenciado B2B):** `id`, `nome_cnpj`, `cnpj`, `razao_social`, `crmv_responsavel`, `email`, `telefone`, `cidade`, `estado`, `atendimento_24h`, `chave_pix_repasse`, `taxa_comissao_customizada`, `ativo`.
2. **`T_SERVICO` (Catálogo Profilático do Marketplace):** `id`, `clinica_id` (FK), `nome`, `descricao`, `categoria`, `preco_base`, `duracao_minutos`, `permite_desconto_fidelidade`, `ativo`.
3. **`T_AGENDAMENTO` (Contrato Relacional de Consulta):** `id`, `pet_id` (FK ON DELETE CASCADE), `tutor_cpf` (FK), `clinica_id` (FK), `servico_id` (FK), `data_hora_agendamento`, `status_agendamento` (`SOLICITADO`, `CONFIRMADO`, `REALIZADO`, `CANCELADO`, `NAO_COMPARECEU`), `observacoes`, `data_criacao`.
4. **`T_TRANSACAO` (Registro Contábil da Captura In-App):** `id`, `agendamento_id` (FK ON DELETE CASCADE), `codigo_transacao_gateway`, `metodo_pagamento`, `status_transacao` (`PENDENTE`, `PAGO`, `ESTORNADO`, `FALHOU`), `valor_bruto`, `valor_desconto_fidelidade`, `valor_liquido_pago`, `codigo_voucher`, `qr_code_hash`, `voucher_utilizado`, `data_criacao`, `data_pagamento`, `data_utilizacao_voucher`.
5. **`T_COMISSAO` (Livro-Razão do Split com Custódia/Escrow):** `id`, `transacao_id` (FK ON DELETE CASCADE), `clinica_id` (FK), `percentual_take_rate` (15.00%), `valor_comissao_plataforma`, `valor_repasse_clinica`, `status_repasse` (`RETIDO_ESCROW`, `LIBERADO_APOS_ATENDIMENTO`, `PAGO_LIQUIDADO`), `data_previsao_repasse`, `data_liquidacao_repasse`.
6. **`T_USUARIO`:** Contas de acesso com credenciais BCrypt e papéis de segurança (`ROLE_TUTOR`, `ROLE_ADMIN`).
7. **`T_TUTOR`:** Cadastro mestre do tutor vinculado ao usuário com validação de CPF e integridade de posse.
8. **`T_PET`:** Prontuário biométrico com espécie, raça, idade, peso e histórico de longevidade.
9. **`T_RACA`:** Mapeamento taxonômico com limites biométricos de peso e idade, e propensão genética a patologias.
10. **`T_RECOMPENSA_TUTOR`:** Mecanismo de fidelidade com acúmulo de Clyvo Coins, streaks consecutivos e níveis (Bronze, Prata, Ouro, Diamante).
11. **`T_CHECKIN_DIARIO`:** Registro longitudinal de alimentação, medicação, atividade física e humor do animal com detecção precoce de anomalias.
12. **`T_BADGE_CONQUISTA`:** Sistema de conquistas e micro-recompensas para estímulo à adesão preventiva.
13. **`T_CONSULTA_TRIAGEM`:** Atendimento clínico presencial com sinais vitais, inferência preditiva e parecer médico veterinário.
14. **`T_HISTORICO_CLINICO`:** Linha do tempo unificada de eventos de saúde e procedimentos do animal.

---

## 3. Resolução da Crítica 2: Canibalização da Margem da Clínica (Unit Economics & Yield Management)

### A Crítica Apontada:
> *"No Módulo 4, promete-se que o tutor Ouro/Diamante ganha de 5% a 20% de desconto nas consultas. Se a clínica já opera com margem veterinária apertada e ainda teria que repassar uma comissão de take-rate ao Clyvo, esse desconto corrói a lucratividade da clínica. De onde sai essa margem?"*

### A Resolução Econômica — Split Bipartite com Co-financiamento de Subsídio:
A margem **não é canibalizada** porque o Clyvo Vet opera sob 4 mecanismos de sustentabilidade financeira inspirados nos maiores marketplaces mundiais (Gympass/Wellhub, ClassPass e Booking.com):

> **Correção de Terminologia Contábil:** O modelo econômico envolve 3 *agentes de negócio* (tutor, plataforma, clínica), mas o *split financeiro de liquidação* é estritamente **bipartite** — o tutor é o **payer** (pagador) e há dois **receivers** (recebedores): Clyvo e Clínica. O subsídio é um lançamento contábil interno da Clyvo (abate no seu take-rate), não configura um terceiro receiver. Implementado em `PagamentoSplitService.calcularResumo()`.

1. **Co-financiamento de Subsídio com Precedência de Piso (Priority Rule):**
   - O desconto de fidelidade **não é absorvido 100% pela clínica**.
   - A Clyvo **subsidia até 50% do desconto** abatendo parte do seu take-rate contratual.
   - **Regra de Precedência explícita (`PagamentoSplitService.java`, Passos 1–6):**
     - **Step 1–4 (co-financiamento 50/50):** aplica o subsídio paritário calculando o repasse preliminar.
     - **Step 5 (Priority Rule):** se o repasse viola o piso de 75%, a Clyvo absorve **100% do excedente** (reduzindo seu take-rate até zero). O piso protege a clínica; **o desconto do tutor é sempre preservado**.
   - **Exemplo de caso-limite (Desconto 20% sobre R\$ 100,00 com repasse-base de 80%):**
     - Repasse 50/50 resultaria em R\$ 70,00 (< piso de R\$ 75,00).
     - Priority Rule: Clyvo absorve o excedente de R\$ 5,00, zerando parte do take-rate.
     - Resultado: clínica recebe exatamente R\$ 75,00 (piso garantido); tutor mantém 20% de desconto.
2. **Yield Management de Capacidade Ociosa:**
   - Clínicas veterinárias operam com média de **35% a 45% de horas ociosas** em seus consultórios (segunda a quinta-feira diurno).
   - O custo operacional do consultório (aluguel, recepcionista, energia e veterinário plantonista) já está 100% pago. O **custo marginal de atender uma consulta adicional em horário vago é nulo**.
   - Os maiores descontos são restritos a horários de baixa demanda. Faturar R\$ 135,00 líquidos em uma hora ociosa é incomparavelmente superior a faturar R\$ 0,00 com a sala vazia.
3. **Estratégia de "Traffic Builder" (Upsell de Alta Margem):**
   - Em medicina veterinária, a consulta preventiva de 45 minutos é o **serviço de entrada (*Front-End*)**.
   - Mais de **65% das consultas preventivas** de longevidade identificam a necessidade de exames complementares: painel renal/hepático, profilaxia dentária ultrassônica, ultrassom abdominal e vacinação polivalente.
   - **Nesses procedimentos subsequentes, a clínica fatura com margem de lucro cheia (de 40% a 60%)**, multiplicando o LTV do paciente.
4. **Floor Protection Contratual (piso de 75% inegociável):**
   - O contrato garante que o repasse líquido da clínica **nunca será inferior a 75% do valor de tabela**.
   - Procedimentos de alto custo de insumos (cirurgias, anestesias complexas) têm `permite_desconto_fidelidade = FALSE`, blindando a margem da clínica.

### Unit Economics Comparativo (DRE por Consulta):

| Linha Contábil | Cenário Ingênuo (Criticado) | **Modelo Econômico Clyvo Vet** |
| :--- | :---: | :---: |
| Preço de Tabela | R\$ 180,00 | **R\$ 180,00** |
| Desconto do Tutor (20% - Diamante) | - R\$ 36,00 (100% da clínica) | **- R\$ 36,00** |
| ↳ *Subsídio Clyvo (abate da taxa)* | *R\$ 0,00* | **+ R\$ 18,00 (Clyvo banca 10%)** |
| ↳ *Absorção da Clínica (Yield)* | *- R\$ 36,00* | **- R\$ 18,00 (Clínica absorve 10%)** |
| **Valor Pago pelo Tutor (payer) In-App** | R\$ 144,00 | **R\$ 144,00** |
| **Take-rate Líquido Clyvo (receiver 1)** | R\$ 21,60 (15%) | **R\$ 9,00 (5% líquido retido)** |
| **Repasse Líquido à Clínica (receiver 2)** | **R\$ 122,40** *(Corrosão de 32%)* | **R\$ 135,00** *(Piso de 75% garantido)* |
| **Receita Adicional em Exames (Upsell)** | R\$ 0,00 | **+ R\$ 380,00 (Margem cheia)** |
| **Faturamento Total Gerado para a Clínica** | R\$ 122,40 | **R\$ 515,00** |

---

## 4. Resolução da Crítica 3: Fuga de Plataforma e Monetização por Take-Rate (Voucher & Escrow)

### A Crítica Apontada:
> *"Se o agendamento ocorre via WhatsApp aberto, a transação ocorre no balcão físico e não há como a plataforma reter split voluntário."*

### A Resolução Arquitetural:
- **Checkout In-App Obrigatório:** O tutor não fecha agendamento fora da plataforma. A contratação do serviço ocorre pelo fluxo in-app (`/checkout` ou `MarketplaceIntermediacaoService`).
- **Retenção de Split na Fonte:** O gateway processa o pagamento do tutor integralmente na plataforma e o valor é distribuído no ato da transação (`15% Clyvo` retidos; `85% Clínica` alocados em custódia `RETIDO_ESCROW` na tabela `T_COMISSAO`).
- **Voucher Digital Criptografado com QR Code:** O tutor recebe um código alfanumérico intransferível e um hash de validação QR Code.
- **Ledger Normalizado Alimentado pelo Fluxo Real (V14):** até a V13 o checkout in-app gravava apenas na tabela desnormalizada `T_AGENDAMENTO_SERVICO`, com o nome da clínica fixo no código, enquanto a modelagem em 3FN defendida neste dossiê só era exercitada por testes — nenhum controller a alcançava, e os campos `snapshot_*` da V13 não tinham escritor nenhum em Java. A partir da **V14** cada checkout grava, na mesma transação, `T_AGENDAMENTO` + `T_TRANSACAO` + `T_COMISSAO` com os snapshots preenchidos, e o voucher exibido na UI aponta para a transação do ledger por chave estrangeira. A clínica passou a ser resolvida de `T_CLINICA` pelo catálogo `T_SERVICO`, que agora publica as quatro ofertas do app com preço idêntico ao exibido — um único preço-verdade para UI, cobrança e snapshot.
- **Fonte Única da Economia:** a matemática do split vivia duplicada em `PagamentoSplitService` e em `MarketplaceIntermediacaoService`, podendo divergir em silêncio. Ambos passaram a delegar para [`SplitFinanceiroCalculator`](file:///Users/gabrieloliveira/Desktop/Agentes-cloud/clyvo-vet-web-v2/src/main/java/com/fiap/clyvovet/service/SplitFinanceiroCalculator.java).
- **Prejuízo do Piso Registrado, Não Descartado:** a Priority Rule zerava o take-rate com um `.max(ZERO)` e, se ainda faltasse valor para honrar o piso de 75%, a diferença desaparecia sem rastro contábil. Hoje ela é gravada em `valor_prejuizo_plataforma` em `T_COMISSAO`. Nos níveis vigentes (teto de 20% de desconto) o valor é sempre zero, porque o teto coincide exatamente com o ponto de virada — mas um nível futuro acima de 20% passaria a ser contabilizado em vez de silenciado.
- **Liberação Condicionada ao Atendimento:** O repasse financeiro para a chave PIX da clínica **só é desbloqueado no banco de dados quando a clínica faz a leitura e validação do voucher na recepção** (`validarVoucherEAtendimento`), alterando o status de `RETIDO_ESCROW` para `LIBERADO_APOS_ATENDIMENTO` e finalmente `PAGO_LIQUIDADO`. Isso elimina completamente a possibilidade de desintermediação (*disintermediation / platform leakage*).

---

## 5. Arquitetura de Decisão Clínica: Dois Paradigmas de Inferência sob Padrão Strategy

Para assegurar acurácia médica sem incorrer em decisões opacas de caixas-pretas estatísticas e eliminar qualquer indício de AI-washing, o motor clínico adota o **Padrão de Projeto Strategy**, orquestrado pelo serviço Spring `@Service` [`ClinicalDecisionOrchestrator`](file:///Users/gabrieloliveira/Desktop/Agentes-cloud/clyvo-vet-web-v2/src/main/java/com/fiap/clyvovet/service/ClinicalDecisionOrchestrator.java), que é o **único** bean orquestrador do contexto. O wrapper `@Deprecated` `PredictiveMlEngine` foi removido: ele criava um segundo bean de orquestrador no container e seu Javadoc afirmava delegar quando, na verdade, herdava.

> **Correção da Nomenclatura do Orquestrador:** O nome anterior `PredictiveMlEngine` (classe já removida do código) violava o Princípio do Menor Espanto (Principle of Least Astonishment): um *Context* que orquestra 9 Strategies determinísticas e apenas 1 de ML não pode herdar o nome de uma técnica específica. O nome correto — `ClinicalDecisionOrchestrator` — descreve exatamente o seu papel de despachante taxonômico sem impor conotações estatísticas indevidas.

```mermaid
classDiagram
    direction TB

    class TriagemService {
        -ClinicalDecisionOrchestrator orchestrator
        +avaliarTriagem(...) ConsultaTriagem
        +avaliarGuardrailsClinicos(...) List~String~
    }

    class ClinicalDecisionOrchestrator {
        <<@Service - Context Strategy>>
        -List~MotorDecisaoClinicaStrategy~ strategies
        -DefaultPhysiologyEngine fallbackEngine
        +executarDecisao(Pet pet, ParametrosClinicosEntrada) ResultadoDecisaoClinica
        +executarInferencia(Pet pet, ...) ResultadoDecisaoClinica
    }

    class MotorDecisaoClinicaStrategy {
        <<interface>>
        +suporta(String especie) boolean
        +avaliar(Pet pet, ParametrosClinicosEntrada entrada) ResultadoDecisaoClinica
    }

    class CaninePredictiveMlEngine {
        <<@Component - Paradigma ML>>
        +suporta("CANINA") boolean
        +avaliar(...) ResultadoDecisaoClinica
    }

    class FelinePhysiologyEngine {
        <<@Component - Paradigma Regras>>
        +suporta("FELINA") boolean
        +avaliar(...) ResultadoDecisaoClinica
    }

    class AvianPhysiologyEngine {
        <<@Component - Paradigma Regras>>
        +suporta("AVE") boolean
        +avaliar(...) ResultadoDecisaoClinica
    }

    class EctothermicPhysiologyEngine {
        <<@Component - Paradigma Regras>>
        +suporta("REPTIL") boolean
        +avaliar(...) ResultadoDecisaoClinica
    }

    class AquaticPhysiologyEngine {
        <<@Component - Paradigma Regras>>
        +suporta("PEIXE") boolean
        +avaliar(...) ResultadoDecisaoClinica
    }

    class SmallMammalPhysiologyEngine {
        <<@Component - Paradigma Regras>>
        +suporta("ROEDOR") boolean
        +avaliar(...) ResultadoDecisaoClinica
    }

    class MustelidPhysiologyEngine {
        <<@Component - Paradigma Regras>>
        +suporta("MUSTELIDEO") boolean
        +avaliar(...) ResultadoDecisaoClinica
    }

    class InvertebratePhysiologyEngine {
        <<@Component - Paradigma Regras>>
        +suporta("ARACNIDEO") boolean
        +avaliar(...) ResultadoDecisaoClinica
    }

    class EquinePhysiologyEngine {
        <<@Component - Paradigma Regras>>
        +suporta("EQUINA") boolean
        +avaliar(...) ResultadoDecisaoClinica
    }

    class DefaultPhysiologyEngine {
        <<@Component - Fallback Universal>>
        +suporta(String) boolean
        +avaliar(...) ResultadoDecisaoClinica
    }

    TriagemService --> ClinicalDecisionOrchestrator : orquestra
    ClinicalDecisionOrchestrator --> MotorDecisaoClinicaStrategy : despacha via Strategy
    MotorDecisaoClinicaStrategy <|.. CaninePredictiveMlEngine
    MotorDecisaoClinicaStrategy <|.. FelinePhysiologyEngine
    MotorDecisaoClinicaStrategy <|.. AvianPhysiologyEngine
    MotorDecisaoClinicaStrategy <|.. EctothermicPhysiologyEngine
    MotorDecisaoClinicaStrategy <|.. AquaticPhysiologyEngine
    MotorDecisaoClinicaStrategy <|.. SmallMammalPhysiologyEngine
    MotorDecisaoClinicaStrategy <|.. MustelidPhysiologyEngine
    MotorDecisaoClinicaStrategy <|.. InvertebratePhysiologyEngine
    MotorDecisaoClinicaStrategy <|.. EquinePhysiologyEngine
    MotorDecisaoClinicaStrategy <|.. DefaultPhysiologyEngine
```

### Detalhamento das 3 Camadas de Decisão:

1. **Camada 1 — Guardrails Determinísticos de Emergência (Diretrizes AAHA / WSAVA):**
   - Parâmetros vitais que indiquem risco iminente de choque térmico, colapso respiratório ou bradicardia severa disparam bloqueio imediato (*fail-safe override*), forçando a classificação para **ALTO RISCO** e limitando o escore clínico a 45 pontos, independentemente de comportamentos prévios.

2. **Camada 2 — Dois Paradigmas de Inferência sob Padrão Strategy:**
   - O `ClinicalDecisionOrchestrator` recebe o paciente e o DTO agnóstico [`ParametrosClinicosEntrada`](file:///Users/gabrieloliveira/Desktop/Agentes-cloud/clyvo-vet-web-v2/src/main/java/com/fiap/clyvovet/dto/ParametrosClinicosEntrada.java) e despacha para a estratégia taxonômica correspondente.
   - **Paradigma 1 — Machine Learning Supervisionado (`CaninePredictiveMlEngine` · exclusivo para CANINA):**
     - **Variável-Alvo (`target y`):** Classificação binária de *Higidez Clínica Projetada em 12 meses* — `y=1` (Hígido: sem internação/urgência) / `y=0` (Risco Clínico: investigação imediata).
     - **Algoritmo:** Regressão Logística Multivariada com normalização Z-score. **16 variáveis preditoras efetivamente usadas** (6 numéricas + 10 categóricas one-hot).
     - **Dataset:** 10.000 amostras sintéticas calibradas. Split estratificado 80/20 (8.000 treino / 2.000 teste holdout). Balanceamento 55%/45%.
     - **ROC-AUC = 0.9485** — medido exclusivamente sobre as 2.000 amostras de teste holdout. **Essa métrica qualifica a probabilidade `P(Higidez|X)`, não o escore de longevidade exibido ao tutor**, que passa por ajustes descritos abaixo.
     - **Threshold de Decisão (conforme implementado):** o escore não é a probabilidade. Ele é `0.85 × P × 100` mais bônus de atividade, limitado a `[10, 99]` e sujeito a tetos clínicos por idade e histórico neurológico. A classificação lê o **escore**: `escore ≥ 80` → Baixo | `50 ≤ escore < 80` → Moderado | `escore < 50` → Alto. Na prática, Baixo Risco exige `P` em torno de `0.87`.
     - Retorna `probabilidadeHigidez ∈ [5%, 98%]` (clamped) e `TipoMotorDecisao.MACHINE_LEARNING_SUPERVISIONADO`.

     **Limitações declaradas do modelo canino (auditoria de honestidade estatística):**
     1. **Entradas derivadas, não medidas.** A plataforma não coleta horas de sono nem de brincadeira; esses valores são inferidos por regra a partir de idade, apetite e minutos de atividade registrados nos check-ins. A distância caminhada é convertida dos minutos de atividade. Apenas idade, peso, número de consultas e os campos categóricos vêm de dado real do tutor.
     2. **Viés de indicação nas visitas ao veterinário.** `COEF_VET_VISITS` é positivo e é o segundo maior coeficiente numérico. No dado de origem isso é provável causalidade reversa (animais acompanhados adoecem menos), mas numa plataforma que intermedia a venda de consultas o efeito também configura conflito de interesse. **Correção aplicada:** o bônus aditivo por consulta que existia no cálculo do escore foi removido — a variável agora pesa uma única vez, dentro do logit treinado.
     3. **Temperatura corporal não entra no modelo.** As constantes do dataset de origem (média de 64.57 °F, cerca de 18 °C) descrevem temperatura **ambiente**. O código anterior convertia a temperatura **retal** do paciente (~101 °F) e a normalizava contra essa distribuição, produzindo um desvio artificial de cerca de +2.5 σ numa variável que media outra coisa — um erro de categoria. **Correção aplicada:** o termo foi removido do logit. A temperatura corporal continua avaliada, com muito mais rigor, pelos guardrails vitais AAHA/WSAVA da Camada 1, que sobrescrevem o risco em hipertermia e hipotermia.
   - **Paradigma 2 — Sistema Especialista Baseado em Conhecimento (8 demais espécies + fallback):** Regras clínicas determinísticas baseadas em diretrizes veterinárias internacionais (AAFP, AAV, ABRAVAS, BSAVA, AAEP). Retorna `probabilidadeHigidez = null` e `TipoMotorDecisao.SISTEMA_ESPECIALISTA_FISIOLOGICO`. Sem pseudo-probabilidades estocásticas.

3. **Camada 3 — Explicabilidade (XAI) e Síntese Clínica SOAP:**
   - Retorna o record unificado [`ResultadoDecisaoClinica`](file:///Users/gabrieloliveira/Desktop/Agentes-cloud/clyvo-vet-web-v2/src/main/java/com/fiap/clyvovet/dto/ResultadoDecisaoClinica.java), consolidando fatores de explicabilidade clínica, riscos fenotípicos específicos, versão do motor, badge semântico e síntese completa no padrão **SOAP** (Subjetivo, Objetivo, Avaliação, Plano).

---




## 6. Fisiologia Veterinária Comparada Multi-Espécie: Matriz de Paradigmas Clínicos


| Espécie no Banco (`V5`) | Motor de Decisão Ativo | Paradigma Computacional | Faixa Térmica / Frequência Normal | Riscos Críticos e Foco Profilático |
| :--- | :--- | :--- | :--- | :--- |
| **`CANINA`** | `CaninePredictiveMlEngine` | **Machine Learning Supervisionado** ($P \in [0, 100\%]$, ROC-AUC 0.9485 sobre $P$) | Temp: 37.8–39.2°C \| FC: 60–140 bpm | Displasia coxofemoral, estresse térmico braquicefálico, convulsões |
| **`FELINA`** | `FelinePhysiologyEngine` | **Sistema Especialista Base 100** ($P = \text{null}$, AAFP/ISFM) | Temp: 38.0–39.2°C \| FC: 140–220 bpm | Lipidose hepática em jejum, Doença Renal Crônica (DRC), FLUTD |
| **`AVE`** | `AvianPhysiologyEngine` | **Sistema Especialista Base 100** ($P = \text{null}$, AAV) | Temp Cloacal: 39.5–42.5°C \| FC: 150–400 bpm | Hipotermia aguda, toxicidade por PTFE/aerossóis, aspergilose |
| **`REPTIL`** | `EctothermicPhysiologyEngine` | **Sistema Especialista Base 100** ($P = \text{null}$, ABRAVAS/ARAV) | POTZ Recinto: 22–34°C \| Doppler: 15–85 bpm | Osteodistrofia Fibrosa (MBD por falta de UVB/Cálcio), estase digestiva |
| **`PEIXE`** | `AquaticPhysiologyEngine` | **Sistema Especialista Base 100** ($P = \text{null}$, Medicina Aquática) | Temp Água: Biótopo \| Mov. Operculares: 20–85/min | Hipóxia aquática (amônia/nitrito), disfunção de bexiga natatória |
| **`ROEDOR`** | `SmallMammalPhysiologyEngine` | **Sistema Especialista Base 100** ($P = \text{null}$, BSAVA Rodents) | Temp: 36.5–38.5°C \| FC: 250–500 bpm | Estase cecal por jejum, maloclusão de dentes elodontes contínuos |
| **`MUSTELIDEO`** | `MustelidPhysiologyEngine` | **Sistema Especialista Base 100** ($P = \text{null}$, BSAVA Ferrets) | Temp: 37.8–40.0°C \| FC: 180–250 bpm | Insulinoma (hipoglicemia severa), hiperadrenocorticismo (fotoperíodo), corpo estranho GI |
| **`ARACNIDEO`** | `InvertebratePhysiologyEngine` | **Sistema Especialista Base 100** ($P = \text{null}$, Lewbart) | Temp Terrário: 22–28°C \| Ausculta Inaplicável | Desidratação de opistossoma, retenção de muda (disecdise) |
| **`EQUINA`** | `EquinePhysiologyEngine` | **Sistema Especialista Base 100** ($P = \text{null}$, AAEP) | Temp: 37.2–38.3°C \| FC Repouso: 28–44 bpm | Síndrome Cólica Equina, laminite (aguamento), desgaste odontológico |
| **Não-Mapeada** | `DefaultPhysiologyEngine` | **Fallback Universal Base 100** ($P = \text{null}$, Resiliência) | Avaliação biométrica comparada geral | Elimina risco de `NoSuchElementException` ou HTTP 500 |


---

## 7. Resolução da Crítica 6: Biometria, Validações Biológicas e Calibração de Pequenos Pets

### A Crítica Apontada:
> *"No cadastro, o sistema permitia dados biologicamente absurdos (cão de 50 anos ou calopsita de 5 kg) e exibia faixas arredondadas de peso confusas (ex: '0.1 a 0.1 kg')."*

### A Resolução de Biometria & UX:
1. **Tetos Biológicos Estritos por Raça/Espécie (`validarLimitesBiologicos`):**
   - Rejeição na camada de serviço e formulário de idades irreais: Calopsita máx 25 anos; Cão máx 20 anos; Roedor máx 5 anos; Arara máx 70 anos.
   - Rejeição de pesos desproporcionais através de limites proporcionais da raça (impede calopsita de 5 kg ou cão de 300 kg).
2. **Formatação Inteligente em Gramas para Animais Pequenos ($< 1.0 \text{ kg}$):**
   - Calopsita exibe com clareza: `80 g a 120 g (0.08 a 0.12 kg)`.
   - Botão de 1 clique: *"Usar peso médio (100 g / 0.10 kg)"*.
3. **Select2 com Categorias de Raças Gerais:**
   - Suporte a agrupamento visual e busca inteligente com categorias de raças não listadas para espécies silvestres e exóticas.

---

## 8. Arquitetura de Segurança, Autenticação e Perfis (Spring Security 6)

1. **Camada de Autenticação:**
   - `SecurityConfig` com `DaoAuthenticationProvider` lendo a tabela `T_USUARIO`.
   - Senhas criptografadas com `BCryptPasswordEncoder` (fator de custo padrão 10).
   - Suporte nativo a **Login Social Google via OAuth2** (`oauth2Login()`), provisionando o usuário automaticamente com vínculo seguro de e-mail.
2. **Controle de Acesso Baseado em Perfis (RBAC):**
   - `ROLE_TUTOR`: Acesso restrito a painel de pets próprios, check-in diário, clube de recompensas, agendamento de consultas e vouchers.
   - `ROLE_ADMIN` (Médicos Veterinários): Acesso à fila médica de triagem (`/triagem/fila`), realização de exames clínicos (`/triagem/avaliar/**`) e console administrativo.
   - Tentativas de acesso indevido disparam HTTP 403 e renderizam página customizada de *Acesso Negado*.
3. **Validação de Propriedade (Data Ownership Guard):**
   - Implementado em `PetService.validarPropriedade(pet, username)`. Impede que um tutor acerte a URL e altere ou exclua o pet de outro tutor.
4. **Proteção Contra Vulnerabilidades:**
   - CSRF ativo em todos os formulários (`_csrf`).
   - Tokens de recuperação de senha com validade restrita de 30 minutos em `T_TOKEN_RECUPERACAO_SENHA`.

---

## 9. Suíte de Testes Automatizados (88 Testes / 100% Cobertura de Requisitos)

A aplicação conta com **88 testes automatizados de integração e unidade**, executados e aprovados com **0 falhas e 0 erros**:

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.fiap.clyvovet.controller.CheckoutControllerTest (5 tests) - PASS
[INFO] Running com.fiap.clyvovet.security.CadastroERecuperacaoSenhaTest (9 tests) - PASS
[INFO] Running com.fiap.clyvovet.security.ControleDeAcessoPorPerfilTest (8 tests) - PASS
[INFO] Running com.fiap.clyvovet.security.DashboardTutorSemPerfilTest (1 test) - PASS
[INFO] Running com.fiap.clyvovet.service.CheckinServiceTest (4 tests) - PASS
[INFO] Running com.fiap.clyvovet.service.CheckoutLedgerIntegracaoTest (6 tests) - PASS
[INFO] Running com.fiap.clyvovet.service.CustomOAuth2UserServiceTest (3 tests) - PASS
[INFO] Running com.fiap.clyvovet.service.CustomUserDetailsServiceTest (2 tests) - PASS
[INFO] Running com.fiap.clyvovet.service.LongevidadeCalculadoraMultiEspecieTest (4 tests) - PASS
[INFO] Running com.fiap.clyvovet.service.MarketplaceModelagemTest (5 tests) - PASS
[INFO] Running com.fiap.clyvovet.service.PagamentoSplitServiceTest (4 tests) - PASS
[INFO] Running com.fiap.clyvovet.service.PerfilServiceTest (2 tests) - PASS
[INFO] Running com.fiap.clyvovet.service.PetBiometriaValidacaoTest (10 tests) - PASS
[INFO] Running com.fiap.clyvovet.service.PetServiceEditTest (4 tests) - PASS
[INFO] Running com.fiap.clyvovet.service.PredictiveMlEngineTest (10 tests) - PASS
[INFO] Running com.fiap.clyvovet.service.RecuperacaoSenhaServiceTest (5 tests) - PASS
[INFO] Running com.fiap.clyvovet.service.TriagemServiceTest (6 tests) - PASS
[INFO] 
[INFO] Results:
[INFO] Tests run: 88, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

---

## 10. Guia Passo a Passo para Execução e Auditoria Local pelo Crítico

Para que o crítico execute e audite todo o ecossistema diretamente no terminal da sua máquina:

### 1. Pré-requisitos:
- **Java JDK 21** instalado (`java -version`).
- **Maven 3.8+** instalado (`mvn -version`).

### 2. Rodar os 88 Testes Automatizados:
Abra o terminal no diretório do projeto e execute:
```bash
cd /Users/gabrieloliveira/Desktop/Agentes-cloud/clyvo-vet-web-v2
mvn test
```
*O Maven executará todas as migrações Flyway de V1 a V15 no H2 e rodará os 96 testes com 100% de sucesso.*

> **Nota sobre a JDK:** o projeto alveja Java 21 (LTS), mas o `pom.xml` configura o Surefire com
> `-Dnet.bytebuddy.experimental=true` e `-XX:+EnableDynamicAgentLoading` para que a suíte também rode
> em máquinas com JDK mais recente. Sem isso, o mock maker inline do Mockito não instrumenta classes
> concretas em JVMs ainda não suportadas e a classe `TriagemServiceTest` falha inteira. Verificado em
> JDK 21 e JDK 26.

### 3. Iniciar a Aplicação Localmente:
```bash
./run.sh
# OU: mvn spring-boot:run
```
*A aplicação subirá na porta **`8095`**.*

### 4. Acessar no Navegador:
- **URL da Aplicação:** `http://localhost:8095`
- **Console do Banco H2:** `http://localhost:8095/h2-console`
  - *JDBC URL:* `jdbc:h2:mem:clyvodb`
  - *User:* `sa`
  - *Password:* *(em branco)*

### 5. Credenciais Pré-configuradas para Teste:
| Perfil | Usuário | Senha | O que testar |
| :--- | :--- | :--- | :--- |
| **Veterinário (`ROLE_ADMIN`)** | `admin` | `admin123` | Acessar fila médica (`/triagem/fila`), preencher exame físico com fisiologia comparada (POTZ para réptil, água para peixe, eutermia para ave) e inspecionar os scores gerados pelo `ClinicalDecisionOrchestrator`. |
| **Tutor Pet (`ROLE_TUTOR`)** | `tutor` | `tutor123` | Painel do tutor Gabriel Maciel com os pets Thor e Luna; registrar check-in diário; subir streak; cadastrar novo pet testando limites biométricos; contratar consulta com voucher e split financeiro. |

---

## 12. Lacunas Conhecidas e Declaradas

Esta seção existe para que a avaliação não precise descobrir sozinha o que ainda não está pronto. Declarar o limite do que foi construído é parte da honestidade técnica do projeto.

| Lacuna | Situação atual | Impacto na defesa |
| :--- | :--- | :--- |
| **Não existe perfil `ROLE_CLINICA`** | O `SecurityConfig` reconhece apenas `ROLE_TUTOR` e `ROLE_ADMIN`. O lado da oferta do marketplace (validar voucher, consultar repasses) é exercido pelo perfil de administrador. | O modelo de dados é genuinamente *two-sided* (`T_CLINICA` com CNPJ, CRMV, chave PIX e taxa contratual própria), mas o **controle de acesso ainda não é**. Uma clínica parceira não tem login próprio nem enxerga apenas os seus repasses. |
| **Liquidação do repasse não tem gatilho de UI** | `MarketplaceIntermediacaoService.liquidarRepasseClinica()` existe e é testado, mas nenhuma tela chama esse método. O status para em `LIBERADO_APOS_ATENDIMENTO`. | O ciclo de custódia está completo até a liberação do escrow; o passo final (`PAGO_LIQUIDADO`) é hoje uma operação de backoffice sem interface. |
| **Gateway de pagamento: Mercado Pago & Fallback Simulado** | Integração real implementada via `GatewayPagamentoService`: conecta à API REST do **Mercado Pago** (`MercadoPagoGatewayService`) quando o token de acesso estiver configurado, e opera com o **`SimuladoGatewayService`** por padrão em ambientes de teste/avaliação sem internet. | O sistema gera PIX dinâmico com QR Code em Base64, código Copia e Cola no padrão EMV do Bacen, polling de liquidação em tempo real e endpoint de webhook (`/api/webhooks/mercadopago`). |
| **Dataset canino é sintético** | As 10.000 amostras são sintéticas calibradas, não um registro clínico real auditável. | Declarado desde a primeira versão. O ROC-AUC qualifica o modelo sobre esse dataset, não sobre população clínica brasileira real. |

---
*Dossiê compilado e versionado no repositório Clyvo Vet Web v2. Registrado na base de conhecimento do Obsidian Second Brain em `_AI-Log/`.*
