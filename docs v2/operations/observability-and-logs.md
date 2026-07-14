# Observability and Logs

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Acesta este rezumatul pentru loguri, debugdump si observabilitate.

## Reguli

- logurile trebuie sa fie actionabile;
- debugdump-ul trebuie sa ajute la diagnostic;
- datele sensibile nu se emit in clar;
- auditul si observabilitatea trebuie sa spuna acelasi lucru despre stare.

## Include

- raportare de erori si warnings;
- status pentru startup si shutdown;
- summary-uri pentru mapping, quest, story si NPC;
- ferestre de evenimente recente.

## Legaturi

- `operations/audit.md`
- `operations/debugging-si-testare.md`
- `architecture/harta-clase-debug.md`
