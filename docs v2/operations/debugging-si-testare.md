# Debugging si Testare

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Acesta este ghidul operational pentru debugging si testare.

## Flux recomandat

1. reproduce problema;
2. verifica logurile;
3. ruleaza testele relevante;
4. izoleaza modulul;
5. adauga un test daca bug-ul este reproductibil;
6. modifica implementarea doar dupa aceea.

## Include

- comenzi Gradle utile;
- scripturi PowerShell;
- diagnostic pentru OpenAI;
- diagnostic pentru NPC-uri si World Admin;
- smoke test pe Paper;
- reguli pentru testele noi.

## Reguli

- nu incepe direct cu modificari in cod;
- debug dump-urile trebuie sa ajute, nu sa aglomereze;
- testele noi trebuie legate de bug-uri reale sau riscuri reale.

## Legaturi

- `operations/server-admin-runbook.md`
- `operations/release-checklist.md`
- `architecture/harta-clase-debug.md`
