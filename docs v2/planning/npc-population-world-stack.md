# NPC Population World Stack

Status: index derivat de planning.
Actualizat: 2026-07-15.

Traseu de lectura pentru legarea populatiei NPC de lumea semantica. Pagina nu defineste contracte si nu transforma preview-urile in functionalitati active.

## Ordine

1. `architecture/mapping.md`
2. `architecture/settlement-plan.md`
3. `guides/ordine-spawn-npc-cladiri-region-node.md`
4. `architecture/harta-clase-spawn.md`
5. `architecture/npc-world-bindings.md`
6. `architecture/households-persistente.md`
7. `architecture/generare-populatie-narativa.md`
8. `architecture/comportament-natural-npc-rutine-alocari.md`
9. `architecture/simulation-service.md`
10. `architecture/npc-uri-temporare-si-episodice.md`
11. `planning/rutine-npc-si-timeline.md`
12. `reference/prevenire-duplicare-npc.md`

## Regula

- `NpcSpawnOrchestrator` executa `HouseAllocation`; `NpcPopulationService` raporteaza statistici;
- `PopulationPlan` ramane preview persistent si selectabil dupa `planId`, dar nu este executat de settlement spawn;
- repopularea pe paturi ramane separata in `NPCManager` si este dezactivata implicit;
- household-ul persistent nu este consumat de rutina;
- starea confirmata ramane in `canonical/implementat-deja.md`.
