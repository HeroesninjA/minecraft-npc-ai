# MCP Service — Plan de Îmbunătățire

Actualizat: 2026-06-28

Punctul de intrare recomandat: `spring-ai-mcp-serviciu-intern.md` → `mcp-serviciu-imbunatatiri.md`.

## Starea Actuală

`ainpc-mcp-service` este un sidecar Spring Boot + Spring AI MCP cu 5 clase de tool-uri, toate **statice**:

| Clasă | Tool-uri | Problemă |
|---|---|---|
| `AinpcPingTools` | `ainpc.ping` | Trivial, mereu "ok". |
| `AinpcDebugHealthTools` | `ainpc.debug.health` | Hardcoded `status=UP` fără verificare reală. |
| `AinpcFeatureStateTools` | `ainpc.feature.state` | Listă hardcodată; `runtimeBridge.enabled=false` peste tot. |
| `AinpcSemanticContextTools` | 11 tool-uri | Expun **schemele** contextului, nu date reale. |
| `AdminMcpGui` (core plugin) | — | GUI care arată MCP config, dar nu poate returna date live. |

Pluginul are clientul (`HttpMcpRuntimeClient`) și fallback (`NoopMcpRuntimeClient`), dar **niciun tool MCP nu returnează date reale din runtime Paper**.

## Gapuri Majore

1. **Fără pod runtime (plugin ↔ MCP)** — tool-urile nu pot citi NPC-uri, questuri, mapping, story, reputație
2. **Tool-urile Faza 3 (doc original) neimplementate** — `ainpc.server.snapshot`, `ainpc.npc.list`, `ainpc.npc.context`, `ainpc.dialog.context`, `ainpc.quest.summary`, `ainpc.world.mapping.summary`
3. **Health indicator fals** — `ainpc.debug.health` raportează `UP` fără verificare bridge
4. **11 tool-uri redundante** — LLM-ul extern nu poate alege bine între variante pereche
5. **Fără redactare / audit** — payload-urile pot conține date sensibile
6. **Fără cache / TTL** — când bridge-ul va exista, fiecare tool call ar citi date proaspete fără raționalizare

## Pași de Îmbunătățire

### Pasul 1 — Pod runtime cu snapshot file (prioritar)

Pluginul scrie periodic (fiecare 5–10s, pe scheduler Paper main thread) un fișier `data/mcp-runtime-snapshot.json` compact cu date redactate: NPC-uri, regiuni, quest-uri active, story state, reputații sumar.

MCP service citește fișierul la cerere, cu cache în memorie (TTL 2s). Nu implică rețea între procese. Vezi `mcp-runtime-bridge-design.md`.

### Pasul 2 — Implementează tool-urile Faza 3 (read-only)

Tool-urile astea folosesc snapshot-ul din Pasul 1:

| Tool | Conținut |
|---|---|
| `ainpc.server.snapshot` | plugin version, NPC count, addon count, mapping counts |
| `ainpc.npc.list` | listă limitată NPC-uri cu filtre (regiune, profesie) |
| `ainpc.npc.context` | identitate, profesie, rutină, relații pentru 1 NPC |
| `ainpc.quest.summary` | progres curent per player / template selector |
| `ainpc.world.mapping.summary` | region/place/node counts, semantic index sumar |
| `ainpc.dialog.context` | snapshot dialog NPC ↔ player (cel mai valoros pentru AI) |

Tipurile din `ainpc-api` (`WorldRegionInfo`, `ProgressionEventPayload`, etc.) sunt sursa de schemă.

### Pasul 3 — Strat de redactare + audit

`RedactingSnapshotFilter` strip-uiește câmpuri din blocklist (`token`, `apiKey`, `password`, `OPENAI_API_KEY`, `mcp.token`) înainte de serializare.

Audit compact (tool, timestamp, payload size) append-uieste în `data/mcp-write-audit.json`.

### Pasul 4 — Health indicator real

Un Spring `HealthIndicator` verifică:
- există snapshot file?
- e fresh (< 60s)?
- conține schemaVersion așteptat?

`ainpc.debug.health` și Actuator `/actuator/health` includ acest status. Când bridge-ul e stale, tool-urile returnează `available=false, status="bridge_stale"`.

### Pasul 5 — Consolidare tool-uri

Un tool `ainpc.semantic.context(domain, summary?)` unifică cele 11 tool-uri existente. Tool-urile individuale rămân ca alias pentru backward-compat cu testele existente.

### Pasul 6 — Spring profiles pentru bridge

| Profile | Comportament |
|---|---|
| `local-static` (default) | Tool-urile cu date statice (cum e acum) |
| `local-bridge` | Citește snapshot file live |
| `offline` | Refuză tool-uri de date; permite doar `ainpc.ping` și `ainpc.feature.state` |

### Pasul 7 — Cache + timeout

`SnapshotReader` cu `AtomicReference<Snapshot>` + `lastRead: Instant`. TTL configurabil prin `mcp.snapshot.cache.ttl.seconds` (default 2s). Fiecare tool call timeout asincron 10s.

### Pasul 8 — Integrare cu `AIOrchestrationService` (Faza 4 doc)

Când `features.ai=true && features.mcp=true`, `AIOrchestrationService` consumă JSON-ul din `ainpc.dialog.context` în prompt NPC. `ai.orchestration.enabled` rămâne default false până la validare smoke test.

### Pasul 9 — Teste

- Unitare: fiecare tool returnează JSON cu schemaVersion, fără blocklist fields
- Smoke test: Spring Boot profile `local-bridge` cu snapshot fixture
- Contract: output parsează prin `ainpc-api` types
- Integrare Paper: `features.mcp=false` → comportament identic cu înainte

### Pasul 10 — Documentație + smoke script

Update `spring-ai-mcp-serviciu-intern.md` cu status implementat pe Faza 3–4.
Script `scripts/smoke-mcp-service.ps1` care pornește sidecar, apelează toate tool-urile, validează JSON.

## Ordinea Recomandată

```
Pasul 1 → Pasul 2 → Pasul 3 → Pasul 4 → Pasul 5 → Pasul 6 → Pasul 7 → Pasul 8 → Pasul 9 → Pasul 10
```

Fiecare pas trebuie să păstreze backward-compat: `features.mcp=false` (config implicit) funcționează exact ca înainte.

## Referințe

- `docs/spring-ai-mcp-serviciu-intern.md` — design original sidecar MCP
- `docs/ai-orchestrare-mcp-stack.md` — stack complet AI + MCP
- `docs/mcp-runtime-bridge-design.md` — design pod runtime (snapshot file)
- `docs/mcp-tools-catalog.md` — catalog tool-uri MCP actuale + propuse
- `.ai/mcp-plan-status.json` — tracking progres implementare
- `ainpc-mcp-service/src/main/java/ro/ainpc/mcp/tools/` — tool-urile existente
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/mcp/` — clientul MCP din plugin
