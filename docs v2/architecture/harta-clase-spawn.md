# Harta claselor pentru spawn

Status: harta verificata in cod.
Actualizat: 2026-07-18.

Aceasta harta urmareste subsistemul spawn si legatura lui cu rutina.

## Noduri principale

- `NpcSpawnOrchestrator`;
- `NpcPopulationService`;
- `HouseAllocationPlanner`;
- `HouseAllocationValidator`;
- `HouseholdPersistenceService`;
- `SpawnBatchTracker`;
- `NpcSpawnPlan`;
- `FamilyBindingPlan`;
- `PostSpawnBindingResult` ca rezultat command-layer pentru binding-ul ulterior spawn-ului;
- `NarrativeGenerator`, `PopulationPlan` si `PopulationPlanRepository` ca ramura persistenta de preview si selectie; conversia conserva metadata narativa pana in `NpcSpawnPlan` si profilul JSON;
- `RoutineService`;
- `RoutineEngine`.

## Flux

- planner-ul calculeaza alocarea;
- validatorul verifica semantica;
- orchestratorul executa spawn-ul si rollback-ul;
- persistenta salveaza household-urile;
- household-ul si rezidentii sunt persistati dupa rezultatele reusite;
- comenzile aplica apoi best-effort metadata de mapping si `npc_world_bindings`, agregand rezultatul ca `COMPLETE`, `PARTIAL`, `FAILED` sau `NOT_APPLICABLE`;
- un binding incomplet nu modifica succesul deja raportat de orchestrator si nu declanseaza rollback; mesajul final il declara explicit drept succes partial;
- generatorul narativ produce un plan in memorie, dar comanda de spawn nu il consuma;
- rutina ramane un scheduler separat si poate folosi ulterior ancorele NPC.

## Reguli

- spawn-ul trebuie sa fie sigur si verificabil;
- rollback-ul trebuie sa fie clar;
- persistenta si rutina nu se amesteca cu logica de selectie;
- batch tracking-ul ajuta la compensare si debug;
- `NpcPopulationService` calculeaza doar statistici; repopularea vanilla este in `NPCManager` si este dezactivata implicit;
- `PopulationPlan` nu este echivalent cu `HouseAllocation` executat;
- household-ul persistent nu este citit de `RoutineEngine`.

## Legaturi

- `architecture/harta-clase-world.md`
- `canonical/implementat-deja.md`
- `reference/harta-clase-index.md`
- `architecture/households-persistente.md`
- `architecture/simulare-sat-si-lume.md`
