# MCP de context al proiectului

Status: document operational pentru infrastructura locala de dezvoltare; nu descrie sidecar-ul runtime.
Actualizat: 2026-07-15.

## Scop

Stack-ul local din `mcp-ai-server/mcp` ofera context de proiect pentru Codex, JetBrains si OpenCode:

- MCP Node pe `http://127.0.0.1:3000/mcp`;
- Serena pe `http://127.0.0.1:9121/mcp`;
- Chroma si Postgres pe loopback;
- persistenta, indexare, memory, rules, changelog, backup si restore;
- dashboard read-only la `http://127.0.0.1:3000/admin`.

## Limita fata de produs

- nu este modulul `ainpc-mcp-service`;
- nu foloseste Spring AI;
- nu este endpoint-ul implicit al `McpRuntimeClient` din plugin;
- tool-urile sale de workspace/context nu devin automat tool-uri runtime AINPC;
- cele trei tool-uri `ainpc.build.mode.*` citite din snapshot sunt o extensie separata si nu unifica cele doua servere.

## Operare

Regulile, comenzile de doctor, backup, audit, restore si registration sunt mentinute in `../../AGENTS.md`. Acest document nu le dubleaza pentru a evita drift-ul.

## Criteriu de sanatate

- `/health` si `/mcp` raspund;
- statusul raporteaza watcher-ele, Chroma si config-ul clientilor;
- backup-ul si test-restore-ul sunt verificabile;
- serviciile raman legate la `127.0.0.1`.

## Legaturi

- `operations/server-admin-runbook.md`
- `architecture/spring-ai-mcp-serviciu-intern.md`
- `architecture/mcp-runtime-bridge-design.md`
