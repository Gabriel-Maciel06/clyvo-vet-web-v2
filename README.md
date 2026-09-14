<div align="center">

# 🐾 Clyvo Vet
### **Plataforma Web de Medicina Preventiva, Longevidade & Gamificação Pet**
*Entrega 3ª Sprint — Java Advanced (FIAP)*

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-Role--Based-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![Flyway](https://img.shields.io/badge/Flyway-Database%20Migrations-CC0200?style=for-the-badge&logo=flyway&logoColor=white)](https://flywaydb.org/)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-Frontend%20MVC-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)](https://www.thymeleaf.org/)
[![Bootstrap](https://img.shields.io/badge/Bootstrap-5.3-7952B3?style=for-the-badge&logo=bootstrap&logoColor=white)](https://getbootstrap.com/)

<p align="center">
  <a href="#-sobre-o-projeto">Sobre</a> •
  <a href="#-como-executar">Como Executar</a> •
  <a href="#-credenciais-de-acesso">Acesso</a> •
  <a href="#-rubrica-da-sprint">Rubrica</a> •
  <a href="#-fluxos-de-negócio-completos">Fluxos</a> •
  <a href="#-testes-automatizados">Testes</a> •
  <a href="#-estrutura-do-projeto">Estrutura</a>
</p>

</div>

---

## 🎥 Vídeo Demonstrativo
▶️ https://youtu.be/MC7T2CgAiig

---

## 👥 Integrantes
- **Vitória Rodrigues Martins** - RM565160
- **Augusto Bonomo Júnior** - RM565155
- **Thomas Fontes** - RM562254
- **Gabriel Maciel** - RM562795
- **Matheus Pereira Molina** - RM563399

---

## 📖 Sobre o Projeto

O **Clyvo Vet** combate o **cuidado veterinário reativo**: a maioria dos tutores só procura a clínica quando os sintomas já estão graves, o que encarece o tratamento e reduz a longevidade do pet.

A plataforma aplica **medicina preventiva contínua** em dois papéis:
- **Tutor (`ROLE_TUTOR`):** faz um **check-in diário** dos hábitos do pet (alimentação, humor, atividade física, medicação e sintomas). Cada check-in rende **Clyvo Coins**, mantém a **sequência de dias (streak)**, sobe o **nível de fidelidade** (Bronze → Prata → Ouro → Diamante) com **desconto real em consultas (5% a 20%)** e desbloqueia **badges**. Sintomas relatados geram um **alerta clínico** automático.
- **Veterinário (`ROLE_ADMIN`):** recebe os alertas e a **fila de triagem preventiva**, registra o exame físico e o sistema calcula um **Escore de Longevidade (0 a 100)** cruzando idade, predisposição genética da raça, sinais vitais e o histórico de check-ins, classificando o risco em **BAIXO, MODERADO ou ALTO** e consolidando tudo na **linha do tempo clínica** do pet.

---

## 🚀 Como Executar

### Pré-requisitos
| Ferramenta | Versão |
| :--- | :--- |
| **JDK** | 21 (o projeto compila com `--release 21`) |
| **Maven** | 3.8+ |
| **Git** | qualquer |

Não é preciso instalar banco de dados: a aplicação sobe um **H2 em memória (modo Oracle)** e o **Flyway** cria e popula o esquema automaticamente.

### Passo a passo
```bash
# 1. Clone o repositório
git clone https://github.com/Gabriel-Maciel06/clyvo-vet-web.git
cd clyvo-vet-web

# 2. (Opcional) Rode os testes automatizados
mvn test

# 3. Execute a aplicação (opção A: direto pelo Maven)
mvn spring-boot:run

#    ou (opção B: empacotar e rodar o jar)
mvn clean package -DskipTests
java -jar target/clyvo-vet-web-1.0.0.jar
```

> Se você tiver mais de um JDK instalado, aponte o Maven para o 21 antes de rodar:
> `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` (macOS) ou defina `JAVA_HOME` no Windows/Linux.

### Acesso
| O quê | Endereço |
| :--- | :--- |
| **Aplicação Web** | http://localhost:8095 |
| **Console H2** (ver as tabelas criadas pelo Flyway) | http://localhost:8095/h2-console — JDBC URL `jdbc:h2:mem:clyvodb`, usuário `sa`, senha em branco |

A porta pode ser alterada com a variável `PORT` (ex.: `PORT=8080 mvn spring-boot:run`).

---

## 👥 Credenciais de Acesso

Os usuários são criados pela migração `V3__inserir_dados_iniciais.sql` com senha em **BCrypt**:

| Perfil | Usuário | Senha | O que acessa |
| :--- | :--- | :--- | :--- |
| **Veterinário / Clínica** (`ROLE_ADMIN`) | `admin` | `admin123` | Central Médica (`/dashboard`), Alertas de Saúde, Fila de Triagem (`/triagem/fila`), Avaliação Clínica (`/triagem/avaliar/{id}`), prontuário de todos os pets |
| **Tutor de Pet** (`ROLE_TUTOR`) | `tutor` | `tutor123` | Painel do Tutor (`/dashboard`), Cadastro de Pets (`/pets/novo`), Check-in Diário (`/checkin/novo`), Clube de Recompensas (`/checkin/recompensas`), Solicitação de Triagem (`/triagem/solicitar`) |

> 🔒 Um tutor que tentar abrir `/triagem/fila`, ou um veterinário que tentar abrir `/checkin/novo`, recebe **HTTP 403** e a página *Acesso Negado*. Um tutor também não consegue ver ou manipular pets de outro tutor trocando o ID na URL ou no formulário.

---

## 🎯 Rubrica da Sprint

| Requisito | Pontos | Como foi atendido |
| :--- | :---: | :--- |
| **1. Frontend** | 30 | 13 telas em **Thymeleaf + Bootstrap 5.3** com layout compartilhado (`fragments/layout.html`), sidebar por perfil (`sec:authorize`), dashboards distintos para tutor e veterinário, formulários com mensagens de validação (`th:errors`) e responsividade mobile. |
| **2. Flyway** | 20 | `V1__criar_tabelas_base.sql` (usuários, raças, tutores, clínicas, pets), `V2__criar_tabelas_fluxos_clinicos.sql` (check-ins, recompensas, badges, triagem, histórico) e `V3__inserir_dados_iniciais.sql` (carga). `spring.jpa.hibernate.ddl-auto=none`: o Flyway é a única fonte do esquema. |
| **3. Spring Security** | 30 | Login por formulário, `DaoAuthenticationProvider` + `CustomUserDetailsService` lendo `T_USUARIO`, senhas **BCrypt**, CSRF ativo, dois perfis (`ROLE_TUTOR`, `ROLE_ADMIN`), rotas protegidas em `SecurityConfig` e `@PreAuthorize`, página de acesso negado e **validação de propriedade do pet** na camada de serviço. |
| **4. Funcionalidades completas** | 20 | Dois fluxos não-CRUD ponta a ponta (abaixo) com **Bean Validation** nos DTOs (`@NotNull`, `@NotBlank`, `@Size`, `@DecimalMin`) e regras de negócio nos *services*. |

---

## 🔄 Fluxos de Negócio Completos

### 🎮 Fluxo 1 — Check-in Diário, Streak e Recompensas (Tutor)
`CheckinController` → `CheckinService.registrarCheckin()`

1. Valida que o pet pertence ao tutor logado e que ainda não houve check-in hoje.
2. Detecta **alerta clínico** (humor apático/dor, pouco apetite ou sintomas descritos) e o publica na Central Médica.
3. Pontua: 10 Clyvo Coins base, +5 com 30 min ou mais de atividade, +5 com medicação administrada.
4. Atualiza a **recompensa do tutor** (`RecompensaTutor`): streak de dias consecutivos, pontos, nível e desconto (regra única na entidade).
5. Desbloqueia **badges** (Primeiro Passo, Tutor Dedicado, Atleta Canino, Guardião da Longevidade).
6. Registra o evento na **linha do tempo clínica** do pet.

| Nível | Critério (streak **ou** pontos) | Desconto |
| :--- | :--- | :---: |
| Bronze | padrão | 5% |
| Prata | 7 dias ou 100 pts | 10% |
| Ouro | 14 dias ou 250 pts | 15% |
| Diamante | 30 dias ou 500 pts | 20% |

### 🩺 Fluxo 2 — Triagem Preventiva e Escore de Longevidade (Tutor → Veterinário)
`TriagemController` → `TriagemService`

1. O tutor abre uma solicitação informando a queixa (`/triagem/solicitar`).
2. A solicitação entra na **fila médica** (`/triagem/fila`, somente `ROLE_ADMIN`).
3. O veterinário registra peso, temperatura, frequência cardíaca e parecer (`/triagem/avaliar/{id}`).
4. `calcularEscoreLongevidadeEInsights()` parte de 100 e desconta por idade sênior, predisposição genética da raça, febre/hipotermia, frequência cardíaca fora da faixa e alertas recentes nos check-ins.
5. O resultado (escore, risco e insights) é gravado na triagem, atualiza o perfil do pet e entra na linha do tempo clínica.

```mermaid
graph LR
    A[Tutor: solicita triagem] --> B[Fila médica ROLE_ADMIN]
    B --> C[Veterinário: exame físico]
    C --> D[Motor de escore 0-100]
    D --> E[Risco BAIXO / MODERADO / ALTO]
    E --> F[Prontuário e linha do tempo do pet]
```

---

## 🧪 Testes Automatizados

```bash
mvn test
```
| Classe | O que cobre |
| :--- | :--- |
| `ControleDeAcessoPorPerfilTest` | Login público, redirecionamento para `/login`, autenticação com BCrypt, senha inválida, `403` de tutor na fila médica e de veterinário no check-in, `200` nas rotas do próprio perfil. |
| `CheckinServiceTest` | Pontuação e streak (5 → 6, Prata → Ouro), alerta clínico por sintomas, bloqueio de check-in duplicado no dia e bloqueio de usuário sem vínculo com o pet. |

---

## 💻 Tecnologias
- **Java 21** · **Spring Boot 3.3.4** (Web MVC, Validation, Data JPA)
- **Spring Security 6** (form login, BCrypt, CSRF, roles, `@EnableMethodSecurity`)
- **Flyway** (migrações SQL versionadas) · **H2** em memória, modo Oracle (driver Oracle `ojdbc11` incluído para troca de banco)
- **Thymeleaf** + `thymeleaf-extras-springsecurity6` · **Bootstrap 5.3** + Bootstrap Icons
- **JUnit 5** + `spring-security-test` (MockMvc)

---

## 📁 Estrutura do Projeto
```
clyvo-vet-web/
├── docs/
│   ├── GUIA_AVALIACAO_ORAL.md          # Perguntas prováveis da banca e respostas
│   └── ROTEIRO_GRAVACAO_VIDEO.md       # Roteiro do vídeo (até 10 min)
├── src/main/java/com/fiap/clyvovet/
│   ├── config/SecurityConfig.java      # Autenticação, perfis e rotas protegidas
│   ├── controller/                     # Auth, Home (dashboard por perfil), Pet, Checkin (Fluxo 1), Triagem (Fluxo 2)
│   ├── dto/                            # DTOs com Bean Validation
│   ├── model/                          # Entidades JPA e enums (mapeadas ao esquema do Flyway)
│   ├── repository/                     # Spring Data JPA
│   └── service/                        # Regras de negócio, gamificação e motor de escore
├── src/main/resources/
│   ├── application.properties
│   ├── db/migration/V1..V3__*.sql      # Flyway
│   └── templates/                      # Telas Thymeleaf (login, dashboards, pets, checkin, triagem)
├── src/test/java/com/fiap/clyvovet/    # Testes de segurança e do fluxo de check-in
└── pom.xml
```

---

<div align="center">
  <sub>Desenvolvido para a 3ª Sprint de Java Advanced — FIAP.</sub>
</div>
