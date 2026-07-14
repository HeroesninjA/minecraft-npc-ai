# Story State Service

Status: contract canonic pentru starea narativa persistenta.
Actualizat: 2026-07-14.

Serviciul detine scrierile validate pentru story state si evenimente narative.

## Responsabilitate

- pastreaza stare la nivel de regiune, place si story global;
- inregistreaza evenimente narative auditable;
- valideaza tranzitiile inainte de persistenta;
- expune snapshot-uri pentru audit si proiectii read-only.

## Limite

- nu construieste prompturi AI;
- nu formuleaza dialog;
- nu permite scrieri directe din GUI, context sau model;
- nu inlocuieste serviciile de quest si progression.

## Legaturi

- `architecture/story-context-service.md`
- `architecture/progression-service.md`
- `planning/questuri-avansate-v2.md`
