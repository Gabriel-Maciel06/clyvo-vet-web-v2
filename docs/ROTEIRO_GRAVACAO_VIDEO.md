# 🎬 Roteiro de Gravação do Vídeo Demonstrativo — Clyvo Vet (Java Advanced, 3ª Sprint)
**Duração máxima:** 10 minutos (alvo: 7 a 8 min)
**Objetivo:** mostrar a aplicação funcionando e evidenciar os 4 itens da rubrica: Frontend, Flyway, Spring Security e os 2 fluxos completos.

## ✅ Antes de gravar
- [ ] JDK 21 ativo no terminal: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` (macOS).
- [ ] Projeto clonado numa pasta limpa: `git clone https://github.com/Gabriel-Maciel06/clyvo-vet-web.git && cd clyvo-vet-web`.
- [ ] Aplicação **ainda parada** (o vídeo mostra o Flyway subindo).
- [ ] Navegador em janela anônima (sem sessão antiga) e zoom em 100%.
- [ ] IDE aberta no projeto com `SecurityConfig.java` e a pasta `db/migration` visíveis.
- [ ] Gravador configurado em 720p ou mais, microfone testado.

---

## 1. 🎬 Abertura (0:00 – 0:45)
**Tela:** README no GitHub.
**Fala:** *"Olá professor! Este é o Clyvo Vet, nossa aplicação web em Spring Boot para a 3ª Sprint de Java Advanced. O problema que atacamos é o cuidado veterinário reativo: o tutor só procura a clínica quando o sintoma já está grave. A plataforma cria uma rotina de medicina preventiva gamificada para o tutor e uma central médica preditiva para o veterinário. Vou mostrar os quatro pontos da rubrica: frontend, Flyway, Spring Security e os dois fluxos completos."*

## 2. 🗄️ Flyway e banco (0:45 – 2:00)
**Tela:** IDE em `src/main/resources/db/migration/`.
- Abra `V1__criar_tabelas_base.sql` e `V2__criar_tabelas_fluxos_clinicos.sql` rapidamente; abra `V3__inserir_dados_iniciais.sql` e aponte as senhas em **BCrypt**.
- Abra `application.properties` e aponte `spring.jpa.hibernate.ddl-auto=none`.

**Fala:** *"O esquema do banco é 100% controlado pelo Flyway: V1 cria as tabelas base, V2 as tabelas dos fluxos clínicos e de gamificação, V3 faz a carga inicial. O Hibernate está com ddl-auto none, então o Flyway é a única fonte da verdade."*

**Terminal:**
```bash
mvn spring-boot:run
```
Enquanto sobe, aponte no log as linhas do Flyway: `Migrating schema "PUBLIC" to version "1 - criar tabelas base"`, `"2 - ..."`, `"3 - ..."` e `Successfully applied 3 migrations`.

(Opcional, 15 s) Abra `http://localhost:8095/h2-console`, conecte com `jdbc:h2:mem:clyvodb` / `sa` e mostre as tabelas `T_*` e a `flyway_schema_history`.

## 3. 🔐 Spring Security e perfis (2:00 – 3:15)
**Tela:** IDE em `SecurityConfig.java`.
**Fala:** *"Autenticação por formulário com DaoAuthenticationProvider lendo a tabela T_USUARIO, senhas BCrypt, CSRF ativo. Dois perfis: ROLE_TUTOR e ROLE_ADMIN, o veterinário. As rotas são protegidas aqui no filterChain e também com @PreAuthorize nos controllers."*

**Navegador:**
1. Abra `http://localhost:8095` → redireciona para `/login`. Mostre a tela.
2. Tente logar com senha errada → mensagem *"Usuário ou senha inválidos"*.
3. Logue com **`tutor` / `tutor123`** → painel do tutor (Thor e Luna, streak 5, nível PRATA, 10% OFF).
4. Digite na URL `http://localhost:8095/triagem/fila` → página **Acesso Negado (403)**.
   **Fala:** *"O tutor não enxerga a fila médica: proteção de rota por perfil."*
5. Volte ao dashboard.

## 4. 🎮 Fluxo 1 — Check-in diário e recompensas (3:15 – 5:15)
Como **tutor**:
1. Clique em **Fazer Check-in Diário** (`/checkin/novo`).
2. Clique em **Concluir Check-in** sem marcar alimentação e humor → as mensagens de validação aparecem em vermelho.
   **Fala:** *"Bean Validation nos DTOs, refletida no formulário."*
3. Preencha: pet **Thor**, alimentação **Recomendada**, humor **Enérgico**, atividade **45** minutos, marque **Remédios administrados**. Concluir.
4. Mostre a mensagem verde: **+20 Clyvo Coins** e streak. Abra o **dashboard**: streak **5 → 6**, nível **PRATA → OURO**, desconto **10% → 15%**.
   **Fala:** *"10 coins base, +5 pela atividade, +5 pela medicação. O tutor passou de 230 para 250 pontos e subiu de Prata para Ouro."*
5. Abra **Clube de Recompensas** (`/checkin/recompensas`) e mostre a tabela de níveis.
6. Faça um segundo check-in para **Luna** com humor **Apático** e sintoma *"respiração ofegante e recusa de ração"* → mensagem amarela de **alerta clínico gerado**.
7. Abra o perfil da Luna (`/pets/2`) e mostre a **linha do tempo** com o alerta e as **badges**.

## 5. 🩺 Fluxo 2 — Triagem preventiva com escore de longevidade (5:15 – 7:30)
1. Ainda como tutor: **Solicitar Triagem** para a **Luna**, queixa *"ronco noturno e cansaço após passeios"*. Enviar → mensagem de sucesso.
2. **Sair** e logar como **`admin` / `admin123`**.
3. Central Médica: mostre o card de **Alertas de Saúde** (o alerta da Luna que o tutor acabou de gerar) e a **Fila de Triagem**.
4. Abra **Fila de Triagem** e clique em **Avaliar** na solicitação da Luna.
5. Mostre à esquerda o **feed de check-ins** do tutor que o algoritmo usa. Preencha: peso **13.2**, temperatura **39.6**, frequência **170**, parecer *"Suspeita de síndrome braquicefálica; solicitar raio-X de tórax e controle de peso."* Concluir.
   **Fala:** *"O motor parte de 100 pontos e desconta por idade, predisposição genética da raça, febre, frequência cardíaca fora da faixa e alertas recentes nos check-ins."*
6. Mostre a mensagem com o **escore** e o **risco** (com esses valores: risco **ALTO**). Abra o perfil da Luna e mostre o escore atualizado, o insight da IA e o evento na linha do tempo.
7. (10 s) Como admin, tente `http://localhost:8095/checkin/novo` → **403**. *"O veterinário não faz check-in: cada perfil tem suas rotas."*

## 6. 🧪 Testes e encerramento (7:30 – 8:30)
**Terminal:**
```bash
mvn test
```
Mostre `Tests run: 12, Failures: 0` e cite: *"Testes de integração com MockMvc cobrem login, 403 por perfil e o fluxo de check-in: pontuação, streak, alerta e bloqueio de duplicidade."*

**Fala final:** *"Entregamos frontend em Thymeleaf e Bootstrap, versionamento de banco com Flyway, autenticação e autorização por perfil com Spring Security, e dois fluxos completos com validações. Obrigado!"*

---

## ⚠️ Se algo der errado na hora
| Sintoma | Solução |
| :--- | :--- |
| `release version 21 not supported` | JDK antigo no PATH: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` |
| Porta 8095 ocupada | `PORT=8096 mvn spring-boot:run` e use a nova porta |
| "Check-in já foi realizado hoje" | Reinicie a aplicação (H2 em memória recria o banco limpo) |
| Login não entra | Confira caps lock; usuários: `tutor/tutor123`, `admin/admin123` |
