# 🧠 ERP IA-First

**Enterprise Resource Planning 100% IA-First** — a novel ERP architecture where AI is the _brain_ (decision-making) and the ERP is the _body_ (transaction execution).

> Unlike traditional ERPs that bolt AI onto existing screens, this system was designed from scratch with AI agents as the core paradigm. No screens, no rigid workflows — just natural language, structured decisions, and auditable actions.

## Architecture

```
Natural Language → Agent Layer → Context Assembly → LLM → Policy Engine → Execution
                     ↗ Plan                                        ↘
                   Orchestrator ←───── Evidence ←───── Tools       Audit Log
                     ↘ Synthesize                                  ↗
                       Response + ActionPlan ──────→ Approve → Execute
```

### 9 Layers

| Layer | Package | Description |
|-------|---------|-------------|
| **Cognitive Interface** | `web` | REST endpoints (`/agent`, `/actions/{id}/approve`, `/actions/{id}/execute`) |
| **Agent Layer** | `agent` | Plan → Synthesize lifecycle with AgentRegistry for intent routing |
| **Context Assembly** | `context` | Evidence collection via typed tools |
| **Prompt Engine** | `prompt` | Versioned prompts with lifecycle (DRAFT → ACTIVE → DEPRECATED) |
| **LLM Provider** | `llm` | Abstracted LlmPort with Cloud (timeout/retry/circuit-breaker) and Ollama providers |
| **Policy/Guardrails** | `policy` | PolicyEngine validates ActionPlans before execution |
| **Execution Engine** | `execution` | Idempotent action execution with transactional guarantees |
| **Transactional Core** | `core` | JPA entities, repositories, deterministic business logic |
| **Event Bus** | `event` | Typed domain events (InMemory, Kafka-ready) |
| **Governance** | `audit` | Full decision audit trail with structured tool calls and policy results |

## Tech Stack

- **Java 21** (LTS) + **Spring Boot 3.4.2**
- **PostgreSQL** (prod) / **H2** (dev/test)
- **Flyway** migrations
- **Spring Security** (JWT/RBAC-ready, toggleable)
- **Swagger/OpenAPI** via Springdoc
- **JUnit 5** + **Mockito** + **Testcontainers** (ready)

## Quick Start

```bash
# Clone and build
mvn clean compile

# Run in dev mode (H2 + Ollama)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests
mvn test

# Access Swagger UI
open http://localhost:8080/swagger-ui.html
```

## API

### POST `/api/v1/agent` — Process Intent

```json
{
  "intent": "Verificar estoque de itens abaixo do mínimo",
  "tenantId": "default",
  "user": "admin"
}
```

**Response:**
```json
{
  "response": "Analisei os níveis de estoque...",
  "actionPlan": {
    "summary": "...",
    "actions": [{
      "type": "DRAFT_PURCHASE_ORDER",
      "risk": "MEDIUM",
      "requiresApproval": true,
      "idempotencyKey": "uuid..."
    }]
  },
  "auditId": "decision-uuid"
}
```

### POST `/api/v1/actions/{auditId}/approve`

```json
{ "approvedBy": "admin" }
```

### POST `/api/v1/actions/{auditId}/execute`

```json
{ "executedBy": "admin" }
```

## LLM Provider Switching

```yaml
# application-dev.yml → Ollama (local)
llm:
  provider: ollama
  ollama:
    url: http://localhost:11434/api/generate
    model: llama3

# application.yml → Cloud (OpenAI-compatible)
llm:
  provider: cloud
  cloud:
    url: https://api.openai.com/v1/chat/completions
    api-key: ${OPENAI_API_KEY}
    model: gpt-4
```

## Adding a New Agent

1. Create a class implementing `AgentDefinition`
2. Annotate with `@Component`
3. Return supported intents in `getSupportedIntents()`
4. Implement `plan()` (which tools to call) and `synthesize()` (produce response + ActionPlan)
5. The agent is auto-discovered by `AgentRegistry`

## Adding a New Tool

1. Create a class implementing `AgentTool<Input, Output>`
2. Annotate with `@Component`
3. Define typed Input/Output DTOs
4. The tool is auto-discovered by `ToolRegistry`

## Prompt Versioning

```java
// Create v1
promptService.createVersion("inventory-analysis", "Analyze {{productName}}...", null, "Initial", "default");

// Create v2 (auto-deprecates v1)
promptService.createVersion("inventory-analysis", "Improved: {{productName}}...", null, "Better prompt", "default");

// Rollback to v1
promptService.rollback("inventory-analysis", 1, "default");
```

## Test Coverage

- **42 tests** (38 unit + 3 integration + 1 LLM validator)
- Covers: agent routing, plan/synthesize lifecycle, policy engine, prompt templates, event bus, core models, action plans, LLM output validation, context snapshots, and full lifecycle integration (suggest → approve)
