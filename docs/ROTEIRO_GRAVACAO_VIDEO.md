# 🎬 Roteiro Oficial de Gravação do Vídeo (Até 15 Minutos) — Clyvo Vet Web v2
### **Entrega Final — 4ª Sprint de Java Advanced (FIAP)**

🎥 **Vídeo Gravado da Apresentação (YouTube):** [https://youtu.be/Z_FFclzDsFU](https://youtu.be/Z_FFclzDsFU)  
**Duração Máxima Permitida:** 15 minutos (Alvo recomendado: **12 a 14 minutos** para margem de segurança)  
**Formato Exigido pela Rubrica FIAP:** Todos os integrantes do grupo devem participar com fala técnica clara, objetiva e domínio sobre o módulo demonstrado.

---

## 📊 Mapeamento da Rubrica Oficial da 4ª Sprint (100 Pontos)

| Critério da Rubrica FIAP | Peso | Onde é coberto neste Roteiro |
| :--- | :---: | :--- |
| **1. Demonstração Técnica da Solução** (Deploy online, navegação nos fluxos principais, conceitos Java contextualizados, boa UI/UX) | **40 pts** | Blocos 4, 5, 6 e 7 (05:30 – 13:30) |
| **2. Narrativa da Solução** (Proposta clara, decisões de design, escolhas tecnológicas justificadas, originalidade e criatividade) | **20 pts** | Blocos 1 e 2 (00:00 – 03:15) |
| **3. Integração Multidisciplinar** (Explicitação de como as demais disciplinas do semestre foram aplicadas + evidências SQL, DevOps, IA, UX, QA) | **20 pts** | Bloco 3 (03:15 – 05:30) |
| **4. Apresentação Oral e Comunicação em Equipe** (Participação de todos os membros, clareza, objetividade e domínio técnico) | **10 pts** | Divisão equilibrada entre os integrantes ao longo dos 8 blocos |
| **5. Organização da Entrega e Documentação** (Repositório organizado, `README.md` completo, link de deploy e vídeo de até 15 min) | **10 pts** | Bloco 1 e Bloco 8 (Fechamento no repositório GitHub) |

---

## 👥 Sugestão de Divisão de Falas por Papel na Equipe
*(Adapte os nomes conforme os integrantes do grupo — o roteiro está estruturado em 5 papéis alinhados à arquitetura do projeto)*

1. **Integrante 1 (Líder Técnico / Produto & Narrativa):** Abertura, Problema de Mercado, Proposta de Valor e Fechamento (Blocos 1 e 8).
2. **Integrante 2 (Arquiteto Back-End Java & IA):** Escolhas Tecnológicas, Arquitetura Spring Boot 3.3.4, Security OAuth2 e Motor Dual-Engine CDSS com Strategy Pattern (Bloco 2 e Bloco 6).
3. **Integrante 3 (Engenheiro de Banco de Dados & DevOps):** Integração Multidisciplinar, Modelagem 3FN (15 tabelas), Flyway (`V1` a `V15`), Deploy Docker/Render e Script SQL Consolidado (Bloco 3).
4. **Integrante 4 (Especialista Front-End, UI/UX & Gamificação):** Design System Clínico ("Ficha Clínica"), Acessibilidade e Demonstração ao Vivo do Fluxo do Tutor — Cadastro Pet, Protocolo de Longevidade, Check-in Diário e Clyvo Rewards (Bloco 4).
5. **Integrante 5 (Engenheiro FinTech, Marketplace & QA Playwright):** Demonstração do Marketplace In-App com Split Bipartite, Subsídio Paritário, Pagamento PIX Sandbox, Emissão/Validação de Voucher em Escrow e Testes Automatizados JUnit + Playwright (Blocos 5 e 7).

---

## ⏱️ Cronograma Detalhado Passo a Passo (00:00 – 14:00)

### 🔹 BLOCO 1: Abertura & Narrativa da Solução (00:00 – 01:45) — *[20 pts: Narrativa]*
**Responsável:** Integrante 1  
**Tela:** Repositório GitHub (`README.md`) + Aplicação Online aberta na tela de Login (`/login`).

**Fala sugerida:**
> *"Olá professores e banca avaliadora da FIAP! Apresentamos a entrega final da 4ª Sprint do **Clyvo Vet Web v2**, uma HealthTech e Marketplace B2B2C de Medicina Veterinária Preventiva e Longevidade Pet desenvolvida em Java 21 e Spring Boot 3.3.4.*  
> *Qual é a dor real que resolvemos? No mercado pet brasileiro, mais de 70% das visitas veterinárias ainda são puramente reativas — o tutor só leva o animal à clínica quando o sintoma já é agudo, encarecendo o tratamento e reduzindo a expectativa de vida do pet. Ao mesmo tempo, plataformas que apenas listam clínicas sofrem com a fuga da transação (desintermediação no balcão).*  
> *Nossa originalidade está em unir três pilares em um único ecossistema:*  
> *1) **Gamificação Preventiva Diária (Clyvo Rewards):** o tutor realiza micro-check-ins diários de saúde, acumula streaks e converte pontos em descontos reais de até 20%;*  
> *2) **Suporte à Decisão Clínica Multi-Espécie (Dual-Engine CDSS):** combina Machine Learning supervisionado para cães e sistemas especialistas fisiológicos para 9 classes biológicas com guardrails vitais da AAHA/WSAVA;*  
> *3) **Marketplace com Split Financeiro e Custódia Escrow:** o tutor contrata procedimentos in-app via PIX ou Stripe com co-financiamento paritário do desconto e Voucher Digital anti-fuga."*

---

### 🔹 BLOCO 2: Decisões de Design e Arquitetura Java Advanced (01:45 – 03:15) — *[20 pts: Narrativa & Conceitos]*
**Responsável:** Integrante 2  
**Tela:** IDE aberta mostrando a estrutura de pacotes `com.fiap.clyvovet` (`SecurityConfig.java`, `MotorDecisaoClinicaStrategy.java`, `CheckoutController.java`).

**Fala sugerida:**
> *"Em Java Advanced, projetamos a aplicação seguindo estritamente os princípios SOLID, Clean Code e DRY:*  
> *- **Segurança Híbrida (`SecurityConfig`):** implementamos autenticação dupla com formulário local (`DaoAuthenticationProvider` + `BCryptPasswordEncoder`), recuperação de senha por token único e Login Social com **Google OAuth2 / OpenID Connect** (`CustomOAuth2UserService` e `CustomOidcUserService`). Também separamos os perfis `dev` e `prod`: em produção, o console H2 e credenciais de teste são desativados automaticamente.*  
> *- **Padrão Strategy no Motor Clínico (`MotorDecisaoClinicaStrategy`):** para evitar condicionais monolíticas e respeitar o princípio Aberto-Fechado (OCP), o Spring injeta todas as estratégias fisiológicas (`CaninePredictiveMlEngine`, `FelinePhysiologyEngine`, `EctothermicPhysiologyEngine`, `AvianPhysiologyEngine`, etc.) e roteia dinamicamente conforme a espécie do paciente.*  
> *- **Padrão Strategy no Gateway de Pagamento (`GatewayPagamentoService`):** desacoplamos a orquestração contábil dos provedores externos (`StripeGatewayService`, `MercadoPagoGatewayService` e `SimuladoGatewayService`), permitindo testar todo o ciclo de PIX e Webhook em sandbox ou produção."*

---

### 🔹 BLOCO 3: Integração Multidisciplinar e Evidências (03:15 – 05:30) — *[20 pts: Integração Multidisciplinar]*
**Responsável:** Integrante 3  
**Tela:** Mostrar `src/main/resources/db/migration` (`V1` a `V15`), `docs/sql/SCHEMA_E_SEED_CONSOLIDADO_SPRINT4.sql`, `Dockerfile` e `render.yaml`.

**Fala sugerida:**
> *"Nossa solução integra diretamente as demais disciplinas do semestre com evidências concretas no repositório:*  
> *1. **Mastering Relational and Non-Relational Database:** modelamos 15 tabelas em 3ª Forma Normal (`T_USUARIO`, `T_TUTOR`, `T_RACA`, `T_PET`, `T_CHECKIN_DIARIO`, `T_CONSULTA_TRIAGEM`, `T_CLINICA`, `T_SERVICO`, `T_AGENDAMENTO_SERVICO`, `T_TRANSACAO`, `T_COMISSAO`, etc.). O versionamento é 100% controlado pelo **Flyway** em 15 migrações evolutivas (`V1` a `V15`) com `ddl-auto=none`, além do script consolidado em `docs/sql/SCHEMA_E_SEED_CONSOLIDADO_SPRINT4.sql`.*  
> *2. **DevOps Tools & Cloud Computing:** empacotamos a aplicação em um `Dockerfile` multi-stage otimizado com Eclipse Temurin JDK/JRE 21 Alpine e configuramos o Infrastructure-as-Code no `render.yaml`, provisionando o serviço web conectado ao PostgreSQL gerenciado em nuvem.*  
> *3. **Disruptive Architectures / IA & Machine Learning:** implementamos o motor de inferência logística calibrado sobre 10.000 amostras clínicas (ROC-AUC 0.9485) com explicabilidade XAI e síntese clínica no padrão médico SOAP.*  
> *4. **UX/UI Design & Front-End:** adotamos um Design System autoral inspirado em prontuário médico real — tipografia Bricolage Grotesque e IBM Plex Sans com `tabular-nums`, superfícies chapadas sem poluição visual e fragmentos Thymeleaf reutilizáveis.*  
> *5. **Compliance & Quality Assurance:** construímos uma suíte com **113 testes automatizados JUnit 5 / MockMvc** e validação E2E no navegador via **Playwright**."*

---

### 🔹 BLOCO 4: Demonstração Técnica — Autenticação, UI/UX e Jornada do Tutor (05:30 – 08:30) — *[40 pts: Demo Técnica]*
**Responsável:** Integrante 4  
**Tela:** Navegador na aplicação em execução.

**Roteiro de cliques ao vivo:**
1. **Tela de Login (`/login`):**
   - Mostrar o design limpo da folha de autenticação, o link de **Criar Conta (`/cadastro`)**, **Esqueci minha senha (`/recuperar-senha`)** e o botão **Continuar com Google** (abrir o modal explicativo de OAuth2 Demo).
   - Fazer login como Tutor: `tutor` / `tutor123`.
2. **Dashboard do Tutor (`/dashboard`):**
   - Destacar os indicadores numéricos alinhados (`tabular-nums`), o nível de fidelidade atual (**PRATA — 10% OFF**, streak de 5 dias, 230 Clyvo Coins) e os cards dos pets (**Thor** — Golden Retriever e **Luna** — Bulldog Francês).
3. **Prontuário Digital e Protocolo de Longevidade (`/pets/1`):**
   - Clicar em **Protocolo de Longevidade** do **Thor**.
   - Mostrar o **Equivalente em Idade Humana**, o **Calendário Preventivo Personalizado**, a **Galeria de Badges**, a **Linha do Tempo Clínica** (filtrável por tipo de evento) e a aba **Atualizar Peso**.
4. **Cadastro de Novo Pet com Biometria Inteligente (`/pets/novo`):**
   - Abrir `/pets/novo`, selecionar no `Select2` uma espécie exótica ou canina (ex.: **Dragão Barbudo** ou **Pastor Alemão**) e mostrar o card dinâmico que exibe instantaneamente a faixa de peso saudável, expectativa de vida e botão *"Usar peso médio"*.
5. **Check-in Diário e Evolução de Nível (`/checkin/novo`):**
   - Selecionar **Thor**, marcar **Alimentação Recomendada**, humor **Enérgico**, **45 minutos** de atividade e **Remédios administrados**.
   - Clicar em **Concluir Check-in (+10 Coins)**.
   - Mostrar o alerta verde de sucesso (+20 Clyvo Coins: 10 base + 5 exercício + 5 medicação) e abrir **Clyvo Rewards (`/checkin/recompensas`)** mostrando que o tutor subiu de **230 para 250 pontos**, evoluindo automaticamente do nível **PRATA (10% OFF)** para **OURO (15% OFF)**!

---

### 🔹 BLOCO 5: Demonstração Técnica — Marketplace In-App, Split Paritário e PIX Sandbox (08:30 – 10:45) — *[40 pts: Demo Técnica]*
**Responsável:** Integrante 5  
**Tela:** Navegador logado como Tutor (`tutor`).

**Roteiro de cliques ao vivo:**
1. **Catálogo de Procedimentos (`/servicos`):**
   - Mostrar que o desconto de **15% OFF (Nível OURO)** já aparece aplicado nos procedimentos preventivos da rede credenciada.
   - Clicar em **Contratar Procedimento In-App** no **Check-up Longevidade Sênior (R$ 280,00)** ou **Consulta Preventiva (R$ 150,00)**.
2. **Checkout Seguro com Split Transparente (`/servicos/checkout`):**
   - Apontar o quadro **Split Contábil & Co-financiamento**: mostrar o Valor Tabela, o Desconto Fidelidade de 15%, o Repasse Líquido protegido da Clínica Parceira (**Floor Protection de 75%**) e o Subsídio Paritário da Clyvo Vet.
   - Selecionar **Pix Instantâneo** e clicar em **Prosseguir para Pagamento Seguro**.
3. **Tela de Pagamento PIX (`/servicos/pagamento/{id}`):**
   - Mostrar o QR Code gerado, o código **Pix Copia e Cola** (clicar em *Copiar*), o timer regressivo de 15 minutos e o polling assíncrono em tempo real (`/servicos/pagamento/{id}/status`).
   - Clicar no botão **Simular Confirmação Instantânea** (Sandbox).
4. **Voucher Digital Emitido (`/servicos/voucher/{id}`):**
   - Mostrar o **Voucher Oficial** com status **PAGO & ATIVO**, o código único (`CLYVO-...`) — copiar esse código para usar no próximo bloco! — e o aviso de custódia Escrow.

---

### 🔹 BLOCO 6: Demonstração Técnica — Visão do Veterinário, Triagem IA Dual-Engine e Baixa do Voucher (10:45 – 12:45) — *[40 pts: Demo Técnica]*
**Responsável:** Integrante 2  
**Tela:** Navegador.

**Roteiro de cliques ao vivo:**
1. **Controle de Acesso (RBAC):**
   - Ainda como tutor, tentar acessar `/triagem/fila` pela barra de endereços $\rightarrow$ mostrar a tela **Acesso Restrito (HTTP 403)**.
   - Fazer logout na sidebar e entrar como Veterinário: `admin` / `admin123`.
2. **Central Médica (`/dashboard` e `/triagem/fila`):**
   - Mostrar o painel administrativo com alertas clínicos ativos e acessar a **Fila de Triagem & Longevidade (`/triagem/fila`)**.
3. **Validação e Liquidação do Voucher em Escrow (`/servicos/validar-voucher`):**
   - No topo da fila clínica, colar o código do voucher (`CLYVO-...`) recém-emitido pelo tutor e clicar em **Validar**.
   - Mostrar a confirmação verde: o voucher muda para `UTILIZADO_NA_CLINICA`, o evento `ATENDIMENTO_CONCLUIDO` entra no prontuário do pet e o repasse em custódia Escrow é liberado para a clínica!
4. **Avaliação Clínica Preditiva Dual-Engine (`/triagem/avaliar/{id}`):**
   - Clicar em **Realizar Triagem & IA** na solicitação pendente da **Luna**.
   - Mostrar à esquerda os últimos check-ins do tutor (IA Feed) e à direita o banner do **Motor de Decisão Clínica Ativo (`CanineWellness-ML-v1.0`)**.
   - Preencher sinais vitais com alerta (ex.: Peso `13.2` kg, Temperatura `39.5` °C, FC `165` bpm) e parecer clínico. Clicar em **Concluir Triagem & Gerar Insights IA**.
   - Mostrar na tabela de histórico o **Escore de Longevidade**, a probabilidade **`P(Higidez)`**, a classificação de risco e a síntese clínica **SOAP + XAI**.

---

### 🔹 BLOCO 7: Qualidade de Código, Testes JUnit e E2E Playwright (12:45 – 13:30) — *[Evidência de Engenharia]*
**Responsável:** Integrante 5  
**Tela:** Terminal / IDE.

**Fala sugerida:**
> *"Para garantir zero regressão e evitar qualquer penalidade de código ou usabilidade, mantemos **113 testes automatizados** (`mvn test`) cobrindo segurança RBAC, anti-IDOR no polling de pagamentos, cálculos de co-financiamento paritário, biometria multi-espécie e inferência de Machine Learning, além da bateria E2E automatizada no navegador real com Playwright."*

---

### 🔹 BLOCO 8: Encerramento (13:30 – 14:00) — *[10 pts: Organização & Comunicação]*
**Responsável:** Integrante 1  
**Tela:** `README.md` do repositório GitHub.

**Fala sugerida:**
> *"Todos os artefatos exigidos na 4ª Sprint — código-fonte refatorado em Java 21 e Spring Boot 3.3.4, 15 migrações Flyway, script SQL consolidado, configuração de deploy em nuvem (`Dockerfile` e `render.yaml`), documentação multidisciplinar e link de acesso online — estão organizados e documentados no `README.md` do nosso repositório. Muito obrigado!"*
