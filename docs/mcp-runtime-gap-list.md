# MCP Runtime Gap List

Actualizat: 2026-07-09

Concluzie: continua cu imbunatatirea serviciilor existente. Nu adauga un serviciu nou pana nu se inchid gap-urile de mai jos.

Checklist executabil: `mcp-runtime-gap-checklist.md`
Tracker: `.\scripts\mcp-runtime-gap-status.ps1 -ProjectRoot "."`
Mapa de implementare: `mcp-runtime-gap-implementation-map.md`
Status pe faze: `mcp-runtime-gap-phase-status.md`
Raport de lucru ramas: `mcp-runtime-remaining-work.md`

## 1) Gap-ul principal

`ainpc-mcp-service` nu citeste inca date reale din runtime-ul Paper. Asta este blocajul principal, nu numarul de servicii.

## 2) Ordinea corecta

1. **Bridge runtime read-only**
   - snapshot JSON periodic din plugin;
   - citire cu TTL scurt in MCP service;
   - `available`, `fresh`, `schemaVersion`, `lastUpdated`.
2. **Tool-uri reale read-only**
   - `ainpc.server.snapshot`;
   - `ainpc.npc.list`;
   - `ainpc.npc.context`;
   - `ainpc.quest.summary`;
   - `ainpc.world.mapping.summary`;
   - `ainpc.dialog.context`.
3. **Redactare si audit**
   - blocklist pentru token/apiKey/password;
   - audit compact pentru fiecare call.
4. **Health real**
   - `bridge_stale` cand snapshot-ul e vechi;
   - `UP` doar cand snapshot-ul e fresh si schema e valida.
5. **Consolidare tool-uri**
   - unificare treptata a tool-urilor redundante;
   - alias doar pentru compatibilitate.

## 3) Ce ramane de facut in serverul MCP Docker

- continue cache-urile si top-k ranking;
- keep `context_pack` compact;
- mentine sync/drift checks pentru `opencode.json` si `.codex/config.toml`;
- evita sa adaugi alte servicii pana cand bridge-ul runtime nu este stabil.

## 4) Cand ar merita un serviciu nou

Adauga un serviciu nou doar daca apare una dintre conditiile:

- boundary clar de securitate;
- scaling independent real;
- contract separat care nu incape in serviciile actuale;
- reducere demonstrabila a coupling-ului.

## 5) Recomandare

Pentru moment, nu adauga alt serviciu. Inchide bridge-ul runtime, apoi reevalueaza.
