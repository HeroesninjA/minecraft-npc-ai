# MCP Runtime Gap Checklist

Actualizat: 2026-07-09

Checklist executabil pentru inchiderea gap-urilor MCP runtime. Ordinea este importanta.

Tracker automat: `.\scripts\mcp-runtime-gap-status.ps1 -ProjectRoot "."`
Mapa de implementare: `mcp-runtime-gap-implementation-map.md`
Status pe faze: `mcp-runtime-gap-phase-status.md`

## Checklist

- [ ] Pluginul scrie periodic `data/mcp-runtime-snapshot.json`
- [ ] Snapshot-ul include `schemaVersion`, `lastUpdated`, `available` si `fresh`
- [ ] Snapshot-ul include rezumat pentru NPC, quest, mapping si dialog
- [ ] MCP service citeste snapshot-ul cu TTL scurt si cache local
- [ ] `ainpc.server.snapshot` raporteaza starea snapshot-ului
- [ ] `ainpc.npc.list` returneaza NPC-uri reale din snapshot
- [ ] `ainpc.npc.context` returneaza context real pentru un NPC
- [ ] `ainpc.quest.summary` returneaza progres real de quest
- [ ] `ainpc.world.mapping.summary` returneaza rezumat de mapping
- [ ] `ainpc.dialog.context` returneaza context real pentru dialog
- [ ] Redactarea elimina token, apiKey, password si alte secrete
- [ ] Auditul MCP append-uieste doar metadata compacta
- [ ] Health indicator marcheaza `bridge_stale` cand snapshot-ul e vechi
- [ ] Health indicator marcheaza `UP` doar cand snapshot-ul este fresh
- [ ] Tool-urile redundante primesc aliasuri compatibile, nu logica duplicata
- [ ] Exista smoke test pentru profile-ul `local-bridge`
- [ ] Exista test de contract pentru schema snapshot-ului

## Gate-uri de acceptare

1. Pluginul si MCP service pot rula separat.
2. Datele runtime sunt read-only in MCP service.
3. Un snapshot vechi nu este raportat ca sanatos.
4. Tool-urile noi nu rup compatibilitatea cu aliasurile vechi.

## Dupa ce checklist-ul este complet

- reevalueaza daca mai ai nevoie de un serviciu nou;
- daca nu exista boundary clar de securitate sau scaling, continua doar cu hardening pe serviciile existente.
