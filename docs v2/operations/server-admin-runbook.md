# Server Admin Runbook

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Acesta este ghidul operational pentru instalare, verificare si depanare pe server Paper.

## Cand il folosesti

- instalare pe server local sau de test;
- verificare startup;
- configurare OpenAI si fallback;
- verificare mapping;
- colectare debug dump;
- cleanup dupa incident.

## Reguli

- runbook-ul este operational, nu de design;
- nu edita baza de date cu serverul pornit;
- backup-ul vine inainte de operatii riscante;
- auditul si debugdump-ul trebuie sa fie disponibile.

## Flux minim

1. instalare curata;
2. configuratie minima;
3. verificare dupa pornire;
4. smoke mapping;
5. smoke NPC;
6. smoke household si settlement;
7. smoke quest;
8. audit si debug dump;
9. backup rapid;
10. upgrade controlat;
11. troubleshooting.

## Legaturi

- `operations/release-checklist.md`
- `operations/debugging-si-testare.md`
- `operations/audit.md`
