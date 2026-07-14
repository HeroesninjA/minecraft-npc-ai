# Feature Flags Lifecycle

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Regula de lifecycle pentru caracteristicile majore AINPC.

## Regula

- fiecare feature major are flag, runtime state si fallback;
- comanda publica arata mesaj clar cand feature-ul este blocat;
- listener-ele si scheduler-ele verifica starea rezolvata;
- serviciile trec prin disabled sau no-op inainte de oprire completa.

## Niveluri

- feature resolution;
- command gate;
- runtime gate;
- service lifecycle.

## Legaturi

- `canonical/constitutie-proiect.md`
- `reference/documentatie-lipsa.md`
