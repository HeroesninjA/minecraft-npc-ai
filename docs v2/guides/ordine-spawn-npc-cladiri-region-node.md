# Ordine spawn NPC, cladiri, regiuni si node-uri

Status: canonical in `docs v2`.
Actualizat: 2026-07-11.

Versiunea de lucru pentru ordinea de spawn si controlul de rollback.

## Ce acopera

- mapping demo si settlement planner;
- HouseAllocation si NpcSpawnPlan;
- binding-uri persistente;
- family bind;
- audit si rollback.

## Regula

- plan -> constructie/import -> region -> place -> node -> HouseAllocation -> spawn;
- nu sari peste place, node sau alocare.

## Legaturi

- `planning/npc-population-world-stack.md`
- `architecture/households-persistente.md`
