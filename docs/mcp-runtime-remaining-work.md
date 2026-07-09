# MCP Runtime — Remaining Work Report

Actualizat: 2026-07-09

## Verdict

Nu adăuga încă un serviciu nou.

- Fazele de bridge runtime sunt închise în `mcp-runtime-gap-phase-status.md`.
- Riscul rămas este operațional: hardening pe serviciile existente, drift de config, cost de tokeni și stabilitatea rapoartelor de stare.
- Dacă ai nevoie de date runtime din Paper, ține soluția read-only și bazată pe snapshot; nu introduce o cale de rețea nouă.

## Ce mai rămâne

| Zonă | De ce contează | Pas următor |
|---|---|---|
| MCP Docker server | Aici sunt costurile reale de latență și blocaj | Continuă tuning pe `server.js`, cache-uri, top-k și smoke tests |
| OpenCode / Codex config | Drift-ul rupe integrarea și introduce regresii | Rulează verificările de sync/drift și păstrează `opencode.json` aliniat |
| Runtime bridge | E deja proiectat; mai are sens doar ca hardening read-only | Păstrează snapshot + redactare + health real + audit |
| Status / observabilitate | Fără status clar, regresiile reapar tăcut | Păstrează `ctx:status`, audit și rapoartele `.ai/` |

## Ce nu merită acum

- un serviciu nou doar pentru a împărți responsabilități încă neclare;
- expunere pe LAN / internet;
- încă un hop de rețea între plugin și MCP;
- optimizări care reduc calitatea doar ca să scadă artificial tokenii.

## Ordinea recomandată

1. Termină hardening-ul serviciilor existente.
2. Ține config-ul Codex / OpenCode în sync.
3. Folosește snapshot read-only pentru runtime data, dacă mai ai nevoie.
4. Adaugă serviciu nou doar când apare un boundary clar de securitate sau scaling.

## Referințe

- `docs/mcp-docker-server-mvp-si-faze.md`
- `docs/mcp-runtime-gap-phase-status.md`
- `docs/mcp-runtime-gap-checklist.md`
- `docs/mcp-runtime-gap-implementation-map.md`
- `docs/mcp-serviciu-imbunatatiri.md`

