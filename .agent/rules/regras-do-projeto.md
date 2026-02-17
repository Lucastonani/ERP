---
trigger: always_on
---

# ERP IA-FIRST — Project Rules (Java 21 / Spring Boot 3)
**Verdade Única do Projeto.**  
A IA deve ler e obedecer este arquivo antes de sugerir código, mudanças ou arquitetura.

---

## 0) Definições (Glossário)
- **IA-first**: IA é o cérebro (decide), core é o corpo (executa).
- **Intent**: objetivo em linguagem natural ("simule pedido conservador").
- **Agent**: módulo cognitivo especializado que planeja e sintetiza decisões.
- **Tool**: função tipada que expõe dados/ações permitidas do core para agentes (read-only ou pre-exec).
- **Evidence**: dados coletados por tools (com fonte, query, timestamp, payload).
- **ActionPlan**: plano estruturado de ações sugeridas (não executadas).
- **PolicyEngine**: guardrails determinísticos que validam ActionPlan antes de qualquer execução.
- **DecisionLog**: auditoria absoluta (proveniência completa).
- **ActionExecutor**: executa ações aprovadas no core via ActionHandlers, com idempotência.
- **Approve**: ato de autorizar uma decisão (muda status e registra aprovador).
- **Execute**: ato de efetivar transações determinísticas no core.

---

## 1) Princípios IA-FIRST (Não Negociáveis)
1. **IA é o Cérebro, ERP é o Corpo**  
   - Agentes decidem e explicam.  
   - Core transacional é determinístico, auditável e “sem inteligência”.

2. **Suggest vs Execute (separação absoluta)**
   - Agentes **NUNCA** executam transações críticas diretamente.
   - Agentes retornam **somente** `ActionPlan` (sugestão).
   - Execução real só ocorre via **ActionExecutor + ActionHandlers** após aprovação.

3. **Auditoria Absoluta**
   Toda decisão deve gerar `DecisionLog` com:
   - intent, agent, correlationId, tenantId/storeId
   - `prompt_name` + `prompt_version`
   - `evidences` (ou toolCalls estruturados) com timestamps
   - `llm_request` / `llm_response` (redigidos quando necessário)
   - `policy_results` (pass/block + motivos)
   - status: `SUGGESTED | APPROVED | EXECUTED | REJECTED | OUTPUT_INVALID`

4. **Prompts são Código**
   - Prompts versionados no banco (`prompt_templates`), com rollback.
   - Prompts não podem ficar como “strings mágicas” no código.
   - Mudança de prompt é mudança de lógica — deve ser auditável e testável.

5. **Explicabilidade obrigatória**
   - Todo `AgentResponse` deve vir com: “o que fez”, “por que fez”, “quais dados usou”.
   - Usuário pode perguntar: “por que 12 e não 8?” e o sistema deve responder com evidências.

---

## 2) Stack Tecnológica (Não Negociável)
- **Java:** 21 (LTS)
- **Spring Boot:** 3.x
- **Build:** Maven
- **DB:** PostgreSQL (produção)  
- **Dev/Test rápido:** H2 permitido (apenas para unit tests e smoke tests)
- **Migrações:** Flyway (SQL). **PROIBIDO** `ddl-auto` / `hbm2ddl`
- **Observabilidade:** logs estruturados + correlationId; OpenTelemetry (quando aplicável)
- **API Docs:** OpenAPI/Swagger
- **Testes:** JUnit 5 + Mockito
- **Integração REAL:** Testcontainers com PostgreSQL (obrigatório para testes de integração críticos)
- **Segurança:** Spring Security + JWT + RBAC (produção). Dev pode permissivo via flag.

---

## 3) Arquitetura (Camadas + Limites)
A comunicação flui por interfaces/portas. Nenhuma camada “pula” camadas.

### 3.1 Camada Cognitiva (Web/API) — `com.erp.ia.web`
- Endpoints REST (ex.: `/api/v1/agent`, `/api/v1/actions/{id}/approve`, `/api/v1/actions/{id}/execute`)
- **Nunca** contém lógica de decisão.
- Tradução de entrada/saída + autenticação + validação de DTO.

### 3.2 Agentes — `com.erp.ia.agent`
- `AgentDefinition`: `plan()` + `synthesize()`
- `AgentOrchestrator` chama:
  - tools via `ContextAssembler/ToolExecutor`
  - `LlmPort` (interface)
  - `PolicyEngine`
  - `DecisionLogService`
- **PROIBIDO** agentes acessarem JPA repositories/services do core diretamente.

### 3.3 Contexto & Tooling — `com.erp.ia.context`, `com.erp.ia.tool`
- `AgentTool<I,O>` tipado + Bean Validation.
- `ToolExecutor`: valida input, aplica timeout, registra toolCall.
- `Evidence`: `source`, `query`, `timestamp`, `payload` (tipado/serializável).
- Tools são o **único** canal oficial para agentes obterem dados do core.

### 3.4 Policy Engine (Guardrails) — `com.erp.ia.policy`
- Valida `ActionPlan` de forma determinística.
- Decisões **bloqueadas são logadas** (`REJECTED`) com motivos.
- Se policy bloquear:
  - **não retornar ActionPlan executável** ao usuário (ver regra 5.2).

### 3.5 Core Transacional — `com.erp.ia.core`
- Entidades JPA + serviços determinísticos.
- Não conhece agentes, LLM, prompts, policies.
- Apenas executa transações (system of record).

### 3.6 Execução Determinística — `com.erp.ia.execution`
- `ActionExecutor` recebe decisão aprovada e executa via `ActionHandlers` por `ActionType`.
- **Idempotência obrigatória** via `idempotencyKey` por ação.
- Atualiza `DecisionLog` para `EXECUTED` e registra resultados.

### 3.7 Infra — `com.erp.ia.llm`, `com.erp.ia.audit`, `com.erp.ia.event`
- **LLM**: `LlmPort` + providers (cloud/local), com timeout/retry/circuit breaker.
- **Audit**: persistência do `DecisionLog` + filhos (toolCalls/policyResults).
- **EventBus**: eventos tipados (dev in-memory, prod Kafka).

---

## 4) Regras do Fluxo do Agente (Contrato Operacional)
### 4.1 Happy Path
1) `POST /api/v1/agent` com intent + contexto mínimo  
2) Orchestrator: route agent  
3) `agent.plan()` define quais tools rodar  
4) `ToolExecutor` roda tools → gera `Evidence`  
5) `agent.synthesize()` produz:
   - resposta natural
   - `ActionPlan` (tipado/estruturado)
   - evidências resumidas (opcional)  
6) `PolicyEngine.validate(ActionPlan, request)`
7) `DecisionLog` é persistido (pai + filhos) com status:
   - `SUGGESTED` se pass
   - `REJECTED` se blocked
   - `OUTPUT_INVALID` se output inválido
8) Resposta retorna: `auditId` + resposta natural (+ ActionPlan somente se permitido)

### 4.2 Contrato de validação de saída da LLM (Obrigatório)
- Qualquer JSON vindo de LLM que será usado como ActionPlan deve passar por:
  - parsing tipado + Bean Validation (`LlmOutputValidator`)
- Se inválido:
  - `DecisionLog.status = OUTPUT_INVALID`
  - **ActionPlan retornado deve ser nulo/vazio**
  - resposta deve pedir correção/retry (sem executar nada)

---

## 5) Regras de Segurança e Governança (Hard Rules)
### 5.1 Approve ≠ Execute
- `/approve` apenas muda status para `APPROVED` e registra:
  - approvedBy + approvedAt
- Execução real ocorre em:
  - `/execute` (manual) **ou**
  - worker/assinatura de evento (automação), mas isso deve ser explícito por policy/config.

### 5.2 Proibição de “vazar plano bloqueado”
Se `PolicyEngine` bloquear:
- A API **não deve** retornar um ActionPlan executável.
- Deve retornar mensagem + motivos + `auditId`.
- O `DecisionLog` deve persistir policyResults.

### 5.3 Persistência correta do Audit (transação)
- Ao criar `DecisionLog` e adicionar `toolCalls/policyResults`, deve existir garantia de persistência:
  - `@Transactional` no fluxo, **e/ou**
  - `DecisionLogService.save(decisionLog)` após adicionar filhos
- `findById()` **nunca** é mecanismo de “trigger save”.

### 5.4 RBAC + Permissões por Ação
- Cada `ActionType` tem permissão exigida.
- `PolicyEngine` valida:
  - `requiresApproval`
  - limites de gasto/risco
  - permissões do usuário

---

## 6) Prompts (Governança + Testes)
- Prompts em `prompt_templates` (DB), com:
  - `name`, `version`, `content`, `variables`, `active`
- Toda chamada de agente deve registrar:
  - prompt_name + prompt_version usados
- **Golden Prompt Tests** obrigatórios:
  - contexto fixo + prompt versionado → output validado (schema/DTO)
  - evita regressão silenciosa de lógica.

---

## 7) Padrões de Código (Práticos e Seguros)
1) **Entidades JPA**
   - Evitar Lombok `@Data` em entidades (risco com equals/hashCode/toString).
   - Preferir `@Getter/@Setter` e equals/hashcode controlados (ou nenhum).
2) **IDs (Padrão único)**
   - Core: `Long` (auto-inc) **ou** UUID — escolha um e padronize.
   - Audit/Execution: UUID (recomendado).
3) **DTOs**
   - `record` para DTOs imutáveis.
4) **DI**
   - via construtor (sem field injection).
5) **Erros**
   - `GlobalExceptionHandler` padroniza respostas.
   - Não engolir exceções; logar com correlationId.
6) **JSON**
   - Jackson.
   - Saída de LLM → sempre `LlmOutputValidator` (hard rule).
7) **Logs**
   - correlationId obrigatório em toda request e em todo DecisionLog.

---

## 8) Testes (Obrigatório)
- Unit tests: agentes, policy, tools, prompt registry, audit.
- Integration tests:
  - **PostgreSQL via Testcontainers** (obrigatório para fluxo end-to-end e migrations).
- H2:
  - permitido para unit tests e smoke test rápido,
  - não deve ser o único “teste de verdade”.

---

## 9) Regras de Evolução (para não virar ERP tradicional)
- Nunca priorizar CRUD/telas como “produto”.
- A UI é conversacional + aprovações + auditoria.
- Novo módulo = novo agente + novas tools + novas policies + prompts versionados.
- Se uma feature exigir “hardcode” no core, primeiro perguntar:
  - isso é regra determinística legal/contábil? (core)
  - ou é heurística/decisão? (agente/prompt/policy)

---

## 10) Comandos Úteis
- Testes: `mvn test`
- App: `mvn spring-boot:run`
- Integração real (quando configurado): `mvn verify -Pintegration-test`
- H2 console (somente dev): `/h2-console`

---

## Regra Final (Sempre)
Se um agente estiver gravando no banco diretamente, chamando JPA repository, ou executando transação sem ActionExecutor/Approval → **ESTÁ ERRADO**.
