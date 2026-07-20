# Sidecar-ul runtime Spring AI MCP

Status: contract canonic verificat in cod; integrarea runtime este partiala.
Actualizat: 2026-07-18.

`ainpc-mcp-service` este modulul Spring Boot al produsului AINPC. Asculta implicit pe `127.0.0.1:39841`, expune MCP Streamable HTTP si actuator health.

## Nu este MCP-ul de context al proiectului

- sidecar runtime Spring: `ainpc-mcp-service`, port `39841`, date Paper si tool-uri AINPC;
- server context proiect: serviciul Node local, port `3000`, Chroma/memory/rules/workspace;
- Serena: port `9121`, navigare simbolica;
- cele trei procese au contracte si cataloage diferite.

## Moduri

- profilul implicit este `static`;
- in profilul `static`, `mcp.snapshot.path` este gol si tool-urile bazate pe snapshot nu au date runtime;
- profilul `local-bridge` configureaza snapshot-ul, TTL-ul, pragul stale, coada de comenzi si auditul;
- profilul `offline` dezactiveaza citirea snapshot-ului;
- tool-urile semantice statice raman contracte/schema si nu demonstreaza acces la runtime.

## Responsabilitate implementata

- inregistreaza tool-uri MCP prin `@McpTool`;
- citeste un snapshot JSON prin `SnapshotReader`;
- expune health pentru proces si bridge, plus health-ul bounded al runtime-ului din snapshot v3;
- aplica un filtru de redactare pe proiectia snapshot cunoscuta;
- scrie audit compact pentru apelurile care trec prin `McpSnapshotService`;
- poate crea fisiere de comanda pentru coada Paper.

## Ce nu face

- nu orchestreaza provideri AI si nu apeleaza modele;
- nu detine logica de quest, story, NPC sau world;
- nu modifica direct DB sau lumea;
- nu valideaza efectul final executat de plugin;
- nu este proxy automat pentru serverul MCP Node de pe portul `3000`.

## Acces si scriere

- bind-ul implicit pe `127.0.0.1` limiteaza expunerea la host;
- modulul nu contine Spring Security sau un filtru care sa valideze `mcp.token`;
- clientul Paper poate trimite Bearer token, dar sidecar-ul verificat nu il impune;
- `AinpcWriteTools` este inregistrat indiferent de `mcp.write_tools_enabled`;
- flag-ul din plugin opreste doar polling-ul cozii, nu publicarea tool-urilor de catre sidecar;
- ambele procese trebuie sa indice acelasi director de comenzi, preferabil prin cale absoluta.

## Blocaje curente

- smoke-ul end-to-end cu un snapshot produs de plugin nu este inca gate obligatoriu de release;
- profilul implicit nu activeaza bridge-ul local;
- tool-ul `ainpc.feature.state` raporteaza bridge `not_configured` si valori hardcodate;
- doua metode sunt adnotate cu acelasi nume `ainpc.semantic.context`;
- autentificarea si gate-ul de publicare pentru write tools nu sunt implementate.

## Surse in cod

- `ainpc-mcp-service/src/main/resources/application.yml`
- `ainpc-mcp-service/src/main/resources/application-local-bridge.yml`
- `ainpc-mcp-service/src/main/java/ro/ainpc/mcp/bridge/SnapshotReader.java`
- `ainpc-mcp-service/src/main/java/ro/ainpc/mcp/tools/AinpcWriteTools.java`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/mcp/McpRuntimeConfig.kt`

## Legaturi

- `architecture/mcp-runtime-bridge-design.md`
- `reference/mcp-tools-catalog.md`
- `reference/mcp-runtime-gap-checklist.md`
