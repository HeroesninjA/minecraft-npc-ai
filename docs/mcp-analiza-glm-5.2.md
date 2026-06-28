# Analiză GLM 5.2 — Îmbunătățire Serviciu MCP

> Salvat din analiza GLM, 2026-06-28. Conținut original, păstrat exact.
> Acesta este documentul sursă care a generat planul din `docs/mcp-serviciu-imbunatatiri.md`.

---

## Cum poate fi îmbunătățit serviciul MCP al pluginului

### Starea actuală (ainpc-mcp-service)

Serviciul MCP este un sidecar Spring Boot + Spring AI MCP cu 5 clase de tool-uri, toate statice:

- `AinpcPingTools` (trivial ping)
- `AinpcDebugHealthTools` (raportează hardcoded `status=UP`, fără să verifice ceva real)
- `AinpcFeatureStateTools` (expune o listă de "feature flags" hardcodate)
- `AinpcSemanticContextTools` (11 tool-uri care expun schemele contextului, nu date reale)
- `runtimeBridge.enabled = false` în toate răspunsurile

Pluginul (`ainpc-core-plugin`) are deja clientul (`HttpMcpRuntimeClient`) și fallback-ul (`NoopMcpRuntimeClient`), dar **niciun tool MCP nu returnează date reale din runtime-ul Paper**. Toate Map-urile sunt literale în cod.

### Gapuri majore identificate

1. **Fără pod runtime (plugin ↔ MCP)**: tool-urile nu pot citi NPC-uri, questuri, mapping, story, reputație — chiar dacă API-urile publice există deja în `ainpc-api` (`WorldAdminApi`, `ReputationApi`, `ProgressionEventPayload`, `AddonRegistryApi`, etc.)
2. **Tool-urile Faza 3 din `docs/spring-ai-mcp-serviciu-intern.md` NU sunt implementate**: `ainpc.server.snapshot`, `ainpc.npc.list`, `ainpc.npc.context`, `ainpc.dialog.context`, `ainpc.quest.summary`, `ainpc.world.mapping.summary`
3. **`ainpc.debug.health` mint** — raportează constant UP fără să verifice bridge-ul
4. **Informație redundată**: 11 tool-uri cu scheme redundante; un LLM extern nu poate alege bine între `ainpc.semantic.context` și `ainpc.semantic.context.summary`
5. **Fără redactare/audit**: docul cere "redactare și limite payload" — nu există încă niciun strat de redactare sau audit al output-urilor
6. **Fără cache/TTL**: odată legat la runtime, fiecare apel de tool ar citi date proaspete fără raționalizare

### Plan de îmbunătățire (ordonat incremental)

#### Pasul 1 — Pod runtime cu snapshot file (cea mai sigură abordare)

Pluginul scrie periodic (la 5–10s, pe scheduler-ul Paper pe main thread) un **snapshot JSON redactat** în `data/mcp-runtime-snapshot.json` continând: număr NPC, regiuni/place-uri, quest-uri active, story state sumar, reputații pe top. MCP service folosește un `SnapshotReader` cu cache în memorie (TTL 2s). Nu implică socket între procese, nu atinge bukkit async, poate rula chiar și la restart plugin.

#### Pasul 2 — Implementează tool-urile Faza 3 cu date reale (read-only)

Pe baza snapshot-ului:
- `ainpc.server.snapshot`: plugin version, NPC count, addon count, world mapping counts (din `WorldAdminApi`)
- `ainpc.npc.list`: listă limitată (max 20) cu filtre după regiune/profesie
- `ainpc.npc.context`: identitate, profesie, rutină, relații — pentru un NPC (limitat la 1 NPC/apel)
- `ainpc.quest.summary`: progres curent pentru un selector player/template
- `ainpc.world.mapping.summary`: region/place/node counts + semantic index summary
- `ainpc.dialog.context`: snapshot contextual pentru un dialog NPC ↔ player (cel mai valoros pentru AIOrchestrationService)

Tool-urile folosesc tipurile deja existente din `ainpc-api` ca sursă de schema (`WorldRegionInfo`, `NpcLifecycleEvents`, `ProgressionEventPayload`, etc.) — nu se inventează DTO-uri noi.

#### Pasul 3 — Strat de redactare + audit

Componenta `RedactingToolOutputFilter` strip-uiește câmpuri din blocklist (`token`, `OPENAI_API_KEY`, `password`, `mcp.token`, `apiKey`) înainte de serializare. Audit compact (sursă tool, timestamp, dimensiune payload, N rows) se append-uiește în `data/mcp-write-audit.json` (pattern-ul deja folosit de serverul MCP de context). Toate tool-urile read-only rămân `readOnlyHint=true, destructiveHint=false`.

#### Pasul 4 — Health indicator real

`McpRuntimeBridgeHealthIndicator` (Spring Actuator) verifică: există snapshot file? e fresh (< 60s)? conține schemaVersion așteptat? `ainpc.debug.health` raportează asta în loc de hardcoded `UP`. Când bridge-ul e down, tool-urile de date returnează explicit `available=false, status="bridge_stale"` în loc să returneze `Map` gol.

#### Pasul 5 — Consolidare tool-uri pentru a reduce overhead model

Schematic, tool-urile context pot fi fuzionate într-un singur tool cu argumentul `domain=world|story|mapping|quest|routing`:

```
ainpc.semantic.context(domain, summary?=false)
```

Păstrează tool-urile individuale ca alias pentru backward-compat cu teste existente. Reduce 11 tool-uri la 2 în lista văzută de model, mult mai ușor de rutat.

#### Pasul 6 — Spring profiles pentru runtime bridge

- `local-static` (implicit, pentru dezvoltare izolată — totul static cum e acum)
- `local-bridge` (citește snapshot file)
- `offline` (refuză tool-uri de date cu 503; permite doar `ainpc.ping` și `ainpc.feature.state`)

Profile ales prin `SPRING_PROFILES_ACTIVE` sau `application.yml`, testat în `AinpcMcpServiceApplicationTests`.

#### Pasul 7 — Cache + timeout pe tool-uri

`SnapshotReader` cu TTL configurabil (`mcp.snapshot.cache.ttl.seconds=2`, default). Fiecare tool are timeout asincron (10s). Se adaugă `AtomicReference+Instant` simplu, fără dependență nouă.

#### Pasul 8 — Integrare cu `AIOrchestrationService` (Faza 4 doc)

Când `features.ai=true && features.mcp=true`, `AIOrchestrationService` consumă JSON-ul returnat de `ainpc.dialog.context` și îl folosește în promptul NPC. Asta declanșează "ai propune, MCP structurează, core validează" întreg ciclul. `ai.orchestration.enabled` rămâne default false până când Pasul 2 e validat cu smoke test Paper.

#### Pasul 9 — Teste

- **Teste unitare**: fiecare tool returnează JSON cu schemaVersion corect, câmpuri așteptate, fără câmpuri din blocklist
- **Smoke test**: pornire Spring Boot profile `local-bridge` cu snapshot file de test fixture
- **Test contract**: output-ul tool-urilor parsează prin `ainpc-api` types (dacă există)
- **Integrare Paper**: cu `features.mcp=false` pluginul se comportă identic cu fără MCP (gate de acceptare din doc)

#### Pasul 10 — Documentație + smoke script

Update `docs/spring-ai-mcp-serviciu-intern.md` cu status implementat pe Faza 3 și Faza 4; adaugă un `scripts/smoke-mcp-service.ps1` care pornește sidecar-ul, apelează toate tool-urile prin curl/MCP și validează JSON-urile. Marchează în `TODO.md` că Faza 3 a început.

### Întrebări (înainte de implementare)

1. **Vrei să merg cu podul bazat pe *snapshot file* (Pasul 1)** — cel mai simplu și fără rețea — sau preferi un mic endpoint REST direct în plugin (mai live, dar necesită scheduler Paper și mai multă izolare)?
2. **Capacitatea de redactare/audit (Pasul 3)** — vrei ca audit-ul să meargă și în serverul MCP de context existent (`data/write-audit.json` din `mcp-ai-server`) sau într-un fișier separat al sidecar-ului Spring (`ainpc-mcp-service/data/mcp-write-audit.json`)?
3. **Consolidarea tool-urilor (Pasul 5)** — vrei să păstrez tool-urile individuale cu schemele statice ca alias pentru backward-compat cu testele existente, sau prefer varianta curată cu 2 tool-uri total?
4. **Prioritatea**: Încep cu **Pasul 1 + 2** (pod runtime + tool-uri Faza 3 read-only cu date reale) sau vrei mai întâi **Pasul 4 + 8** (health real + integrare cu `AIOrchestrationService`), care necesită plugin live?
5. **Programul curent `TODO.md`** pune accent pe *Pass Paper pe playable village* și smoke test-uri. Vrei ca îmbunătățirea MCP să meargă în paralel (nu accelerează demo-ul) sau să așteptăm până demo-ul e cap-coadă?
