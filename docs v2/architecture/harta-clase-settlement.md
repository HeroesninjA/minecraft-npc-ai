# Harta claselor pentru settlement

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Harta scurta pentru subsistemul settlement si template-urile de cladiri.

## Noduri principale

- `SettlementConfigLoader`;
- `BuildingTemplateRegistry`.

## Flux

- incarca configuratia de asezari;
- inregistreaza template-urile de cladiri;
- alimenteaza planificarea si spawn-ul de asezari.

## Legaturi

- `architecture/generare-sate-fara-worldedit.md`
- `architecture/generare-sate-worldedit-si-npc.md`
