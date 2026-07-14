# Harta claselor pentru spawn

Status: canonical in `docs v2`.
Actualizat: 2026-06-21.

Aceasta harta urmareste subsistemul spawn si legatura lui cu rutina.

## Noduri principale

- `NpcSpawnOrchestrator`;
- `HouseAllocationPlanner`;
- `HouseAllocationValidator`;
- `HouseholdPersistenceService`;
- `SpawnBatchTracker`;
- `NpcSpawnPlan`;
- `FamilyBindingPlan`;
- `RoutineService`;
- `RoutineEngine`.

## Flux

- planner-ul calculeaza alocarea;
- validatorul verifica semantica;
- orchestratorul executa spawn-ul si rollback-ul;
- persistenta salveaza household-urile;
- rutina muta NPC-ul in starea zilnica.

## Reguli

- spawn-ul trebuie sa fie sigur si verificabil;
- rollback-ul trebuie sa fie clar;
- persistenta si rutina nu se amesteca cu logica de selectie;
- batch tracking-ul ajuta la compensare si debug.

## Legaturi

- `architecture/harta-clase-world.md`
- `canonical/implementat-deja.md`
- `reference/harta-clase-index.md`
