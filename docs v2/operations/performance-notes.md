# Performance Notes

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Acesta este rezumatul notelor de performanta pentru servere mari.

## Zone de risc

- indexarea semantica;
- task-uri periodice;
- audit si debugdump;
- generarea asistata;
- operatiile de persistence si reload.

## Reguli

- masura inainte de optimizare;
- nu muta lucru greu in thread-ul principal;
- pastreaza fallback si degradare sigura;
- documenteaza orice schimbare care atinge tick-ul.

## Legaturi

- `operations/debugging-si-testare.md`
- `architecture/simulation-service.md`
- `architecture/harta-clase-world.md`
