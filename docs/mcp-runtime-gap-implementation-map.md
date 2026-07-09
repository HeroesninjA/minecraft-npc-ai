# MCP Runtime Gap Implementation Map

Actualizat: 2026-07-09

Acesta este pasul urmator dupa checklist: leaga fiecare gap de fisierele reale si de ordinea de lucru.

Status pe faze: `mcp-runtime-gap-phase-status.md`

## Faza 1 — Bridge runtime read-only

- Pluginul scrie snapshot periodic
  - `ainpc-core-plugin/src/main/kotlin/ro/ainpc/mcp/bridge/RuntimeSnapshot.kt`
  - `ainpc-core-plugin/src/main/kotlin/ro/ainpc/mcp/bridge/RuntimeSnapshotProducer.kt`
- MCP service citeste snapshot-ul cu TTL
  - `ainpc-mcp-service/src/main/java/ro/ainpc/mcp/bridge/RuntimeSnapshot.java`
  - `ainpc-mcp-service/src/main/java/ro/ainpc/mcp/bridge/SnapshotReader.java`

## Faza 2 — Tool-uri reale read-only

- Snapshot server
  - `ainpc-mcp-service/src/main/java/ro/ainpc/mcp/tools/AinpcServerSnapshotTools.java`
- NPC tools
  - `ainpc-mcp-service/src/main/java/ro/ainpc/mcp/tools/AinpcNpcTools.java`
- World mapping tools
  - `ainpc-mcp-service/src/main/java/ro/ainpc/mcp/tools/AinpcWorldMappingTools.java`
- Quest snapshot tools
  - `ainpc-mcp-service/src/main/java/ro/ainpc/mcp/tools/AinpcQuestSnapshotTools.java`

## Faza 3 — Redactare si audit

- Redactare payload
  - `ainpc-mcp-service/src/main/java/ro/ainpc/mcp/bridge/RedactingSnapshotFilter.java`
- Audit compact
  - `ainpc-mcp-service/src/main/java/ro/ainpc/mcp/bridge/McpAuditLogger.java`
- Snapshot orchestration
  - `ainpc-mcp-service/src/main/java/ro/ainpc/mcp/bridge/McpSnapshotService.java`

## Faza 4 — Health real

- Bridge health indicator
  - `ainpc-mcp-service/src/main/java/ro/ainpc/mcp/bridge/McpRuntimeBridgeHealthIndicator.java`

## Faza 5 — Consolidare si integrare

- Unified context tool
  - `ainpc-mcp-service/src/main/java/ro/ainpc/mcp/tools/AinpcSemanticContextTools.java`
- NPC dialog context provider
  - `ainpc-core-plugin/src/main/kotlin/ro/ainpc/mcp/bridge/McpDialogContextProvider.kt`

## Faza 6 — Profiles si smoke

- Spring profiles
  - `ainpc-mcp-service/src/main/resources/application-local-bridge.yml`
  - `ainpc-mcp-service/src/main/resources/application-offline.yml`
  - `ainpc-mcp-service/src/main/resources/application-static.yml`
- Smoke script
  - `scripts/smoke-mcp-service.ps1`

## Faza 7 — Teste

- Snapshot reader test
  - `ainpc-mcp-service/src/test/java/ro/ainpc/mcp/bridge/SnapshotReaderTest.java`
- Redactare test
  - `ainpc-mcp-service/src/test/java/ro/ainpc/mcp/bridge/RedactingSnapshotFilterTest.java`
- Health indicator test
  - `ainpc-mcp-service/src/test/java/ro/ainpc/mcp/bridge/McpRuntimeBridgeHealthIndicatorTest.java`
- Tool snapshot test
  - `ainpc-mcp-service/src/test/java/ro/ainpc/mcp/tools/AinpcServerSnapshotToolsTest.java`

## Ordinea practica de lucru

1. Verifica daca snapshot-ul runtime exista si e fresh.
2. Verifica daca tool-urile read-only returneaza date reale.
3. Verifica redactarea si auditul.
4. Verifica health-ul bridge-ului.
5. Verifica smoke-ul si testele de contract.
6. Abia dupa aceea reevalueaza daca mai trebuie un serviciu nou.
