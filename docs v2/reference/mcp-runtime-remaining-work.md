# MCP Runtime Remaining Work

Status: canonical in `docs v2`.
Actualizat: 2026-07-09.

Raportul curent spune sa nu adaugi inca un serviciu nou.

## Ce ramane

- hardening pe serviciile existente;
- drift de config;
- stabilitatea rapoartelor de stare;
- continuitate pentru snapshot read-only.

## Ce nu merita acum

- servicii noi fara boundary clar;
- expunere pe LAN/internet;
- hop nou de retea intre plugin si MCP;
- optimizari care reduc calitatea doar pentru token count.

## Legaturi

- `reference/mcp-runtime-gap-phase-status.md`
- `operations/observability-and-logs.md`
- `operations/debugging-si-testare.md`
