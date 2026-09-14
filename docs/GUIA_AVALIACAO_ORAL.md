# Guia de Preparação para Avaliação Oral (Banca FIAP)
**Projeto:** Clyvo Vet - Plataforma Web de Longevidade & Gamificação Pet
**Tema:** Java Advanced - 3º Sprint

---

## 💡 1. Pitch do Projeto (30 a 60 segundos)
> "O Clyvo Vet é uma plataforma de medicina preventiva e longevidade pet desenvolvida em Spring Boot. O mercado pet hoje sofre com consultas tardias e episódicas, que encarecem tratamentos e reduzem a sobrevida dos animais. Nossa solução usa gamificação (check-ins diários, streaks e Clyvo Coins com descontos reais em consultas) para criar recorrência e fidelização do tutor, alimentando uma central médica com algoritmo preditivo que antecipa riscos genéticos e comportamentais antes que se tornem emergências graves."

---

## ❓ 2. Perguntas Técnicas Prováveis & Respostas Perfeitas

### P1: Como o Flyway foi configurado e por que ele é crucial?
- **Resposta:** "Utilizamos o Flyway para garantir versionamento declarativo e reprodutibilidade total do banco de dados em qualquer ambiente. As migrações residem em `src/main/resources/db/migration/` com prefixos ordenados (`V1`, `V2`, `V3`). O `V1` estrutura as tabelas mestras, o `V2` define as tabelas de gamificação e triagem clínica, e o `V3` executa o seed de dados com senhas criptografadas em BCrypt. Desabilitamos a criação mágica do Hibernate (`ddl-auto=none`) para que o banco seja estritamente gerido pelas migrações SQL do Flyway."

### P2: Como o Spring Security trata a autenticação e autorização por perfis?
- **Resposta:** "Implementamos um `SecurityConfig` declarativo com `DaoAuthenticationProvider` e `CustomUserDetailsService`. As senhas são protegidas com o algoritmo de hashing `BCryptPasswordEncoder`. Criamos duas roles principais: `ROLE_TUTOR` e `ROLE_ADMIN` (corpo clínico). Rotas médicas como `/triagem/fila` e `/triagem/avaliar/**` são estritamente restritas a administradores (`hasRole('ADMIN')`). Se um tutor tenta acessá-las diretamente, o Spring Security intercepta e retorna HTTP 403 / Access Denied. Além disso, a proteção CSRF está ativa em todos os formulários via Thymeleaf."

### P3: Quais são os dois fluxos completos de negócio (não-CRUD)?
- **Resposta:** 
  1. **Fluxo 1 (Gamificação & Check-in Diário de Cuidado):** O tutor registra dados diários de alimentação, medicação, minutos de exercício e humor do animal. O sistema calcula a pontuação, avalia a sequência consecutiva (`streak`), ajusta o nível de fidelidade (Bronze a Diamante) e percentual de desconto em consultas, avalia regras automáticas para desbloqueio de badges de longevidade e, caso detecte anomalias (como dor ou recusa de apetite), despacha um alerta clínico para a equipe médica.
  2. **Fluxo 2 (Triagem Preventiva e Escore de Longevidade):** O veterinário atende à fila de triagem e preenche os dados do exame físico. O motor da aplicação processa esses dados cruzando com a predisposição genética da raça e com o histórico dos check-ins diários, gerando um **Escore de Longevidade (0 a 100)** e classificação de risco (**BAIXO**, **MODERADO**, **ALTO**), consolidando o prontuário na linha do tempo do pet.

### P4: Como foram evitadas as penalidades de código (SOLID, Clean Code)?
- **Resposta:** "Seguimos rigorosamente os princípios de Clean Code: injeção de dependências estritamente por construtor, separação clara em camadas (Controller, Service, Repository, DTO, Model), isolamento das regras de negócio dentro dos Services (sem lógica pesada em controllers), uso de Bean Validation nos DTOs (`@NotNull`, `@Size`, `@DecimalMin`), sem 'God methods' e sem acoplamento indevido."

### P5: Como vocês impedem que um tutor acesse ou altere o pet de outro tutor?
- **Resposta:** "Além da proteção de rota por perfil, fazemos **validação de propriedade na camada de serviço**: `PetService.validarPropriedade(pet, username)` compara o CPF do tutor dono do pet com o tutor vinculado ao usuário logado. Ela é chamada em `PetController` (detalhes do pet, via `buscarPorIdAutorizado`), em `CheckinService.registrarCheckin` e em `TriagemService.solicitarTriagem`. Se não bater, lançamos `AccessDeniedException`, que o Spring Security converte em 403 e na página *Acesso Negado*. O veterinário (`ROLE_ADMIN`) é a exceção: pode ver qualquer pet."

### P6: Onde fica a regra de níveis e descontos? Por que na entidade?
- **Resposta:** "Em `RecompensaTutor.atualizarNivelEDesconto()` e `registrarCheckinNaData()`. Antes existia lógica duplicada no service e na entidade com limites diferentes; centralizamos na entidade (modelo rico) para haver **uma única fonte da regra**: Prata com 7 dias ou 100 pontos (10%), Ouro com 14 dias ou 250 pontos (15%), Diamante com 30 dias ou 500 pontos (20%). O `CheckinService` só orquestra: chama `registrarCheckinNaData`, `adicionarPontos` e salva."

### P7: Como os erros são tratados?
- **Resposta:** "Um `@ControllerAdvice` (`GlobalExceptionHandler`) converte `IllegalArgumentException` (registro inexistente) em página 404 amigável e qualquer erro inesperado em 500 com a mesma página `erro.html`; `AccessDeniedException` é relançada para o Spring Security renderizar o 403. Nos formulários, `BindingResult` devolve a própria tela com as mensagens do Bean Validation."

### P8: Quais testes automatizados existem?
- **Resposta:** "Doze testes de integração com `@SpringBootTest`. `ControleDeAcessoPorPerfilTest` usa MockMvc e `spring-security-test`: login público, redirecionamento para `/login`, autenticação real com BCrypt via `formLogin`, senha errada, 403 do tutor na fila médica e do veterinário no check-in. `CheckinServiceTest` cobre o Fluxo 1 sobre os dados do Flyway: 20 pontos com atividade e medicação, streak 5 para 6 e subida de Prata para Ouro, alerta clínico por sintomas, bloqueio de check-in duplicado e bloqueio de usuário sem vínculo com o pet. Os testes rodam com `mvn test` e usam o mesmo H2 + Flyway da aplicação."

### P9: Por que H2 e não Oracle? Dá para trocar?
- **Resposta:** "Para a disciplina o foco é frontend, Flyway e Security, então usamos H2 em memória em modo Oracle para o avaliador rodar com um único comando. O driver `ojdbc11` já está no `pom.xml`: basta trocar `spring.datasource.*` e o dialeto no `application.properties`; as migrações do Flyway usam SQL compatível."

### P10: Como a IA foi usada no processo?
- **Resposta (adapte à sua realidade):** "Usei IA como par de programação: para revisar o código em busca de falhas (foi assim que identificamos a falta de validação de propriedade do pet e a regra de níveis duplicada), gerar o esqueleto dos testes com MockMvc e revisar o README. Toda sugestão foi lida, entendida e testada antes de entrar no projeto, e as decisões de arquitetura (Flyway como fonte única do esquema, regra de fidelidade na entidade, validação no service) são nossas."

---

## 🔎 3. Trechos que a banca pode pedir para explicar (saiba localizar rápido)
| Arquivo | O que explicar |
| :--- | :--- |
| `config/SecurityConfig.java` | `filterChain`: ordem dos `requestMatchers`, `hasRole` x `ROLE_` prefixo, `formLogin`, `logout`, CSRF (exceção só para o console H2). |
| `service/CustomUserDetailsService.java` | Converte `T_USUARIO` em `UserDetails`; a `SimpleGrantedAuthority` recebe `ROLE_TUTOR`/`ROLE_ADMIN`. |
| `service/CheckinService.registrarCheckin` | Passo a passo do Fluxo 1 (propriedade, duplicidade, alerta, pontos, streak, badges, timeline). |
| `model/RecompensaTutor.java` | Regra de níveis/desconto e do streak. |
| `service/TriagemService.calcularEscoreLongevidadeEInsights` | Como o escore parte de 100 e cada fator desconta; limiares de risco. |
| `db/migration/V1..V3` | Ordem das migrações, FKs, por que `ddl-auto=none`. |
| `templates/fragments/layout.html` | `sec:authorize` na sidebar; fragmento `appShell` reutilizado por todas as telas. |
| `src/test/...` | O que cada teste prova. |
