# AINPC Audit

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Acesta este rezumatul auditului runtime si al backlog-ului de securitate.

## Verdict

- auditul runtime este read-only;
- nu modifica NPC-uri, DB, config sau lume;
- este pentru diagnostic rapid in joc;
- security review-ul ramane backlog separat.

## Acopera

- `/ainpc audit npc`;
- `/ainpc audit world`;
- `/ainpc audit db`;
- `/ainpc audit spawn`;
- exporturile de debug pentru inspectie.

## Nu face

- nu repara automat datele;
- nu executa actiuni destructive;
- nu inlocuieste testarea completa de securitate;
- nu amesteca audit operational cu securitate.

## Legaturi

- `operations/debugging-si-testare.md`
- `operations/release-checklist.md`
- `canonical/implementat-deja.md`
