# Planurile de settlement

Status: contract de delimitare verificat in cod.
Actualizat: 2026-07-18.

Nu exista un singur `SettlementPlan` care conduce importul, constructia si spawn-ul. Runtime-ul curent foloseste modele separate, cu tranzactii si persistenta diferite.

## 1. DTO-urile publice neconectate

`ainpc-api` declara `SettlementPlan`, `SettlementGenerationPlan`, `BuildingPlacementPlan` si `RoadPlacementPlan`.

- sunt DTO-uri si au teste proprii;
- nu au consumer in codul de productie din core sau addon;
- statusurile `DRAFT`, `VALIDATED` si `COMMITTED` nu formeaza un workflow runtime;
- nu exista executor, repository, comanda de commit, discard, export sau rollback pentru ele.

Aceste tipuri sunt scaffold API, nu sursa de adevar pentru spawn sau world mapping.

Decizia de compatibilitate este explicita:

- agregatul `SettlementPlan` ramane in API pentru compatibilitate sursa si binara, dar este marcat `@Deprecated` la nivel `WARNING` ca scaffold neexecutabil;
- nu exista termen de eliminare si nu se declara `ReplaceWith`, deoarece API-ul public nu ofera un executor echivalent;
- submodelele existente raman containere de date compatibile, fara a dobandi implicit lifecycle runtime;
- `HouseAllocation` ramane un model intern core pentru spawn si nu este prezentat addonurilor drept inlocuitor public.

## 2. Planul activ de spawn

Fluxul folosit de `/ainpc world settlement plan|spawn` este:

`WorldAdminService -> HouseAllocationPlanner -> HouseAllocation -> NpcSpawnOrchestrator`

- mapping-ul existent furnizeaza regiunea, casele si node-urile;
- `plan` executa validarea si dry-run-ul orchestratorului;
- `spawn` creeaza sau reutilizeaza NPC-uri, persista household-uri si apoi incearca binding-ul best-effort in mapping si `npc_world_bindings`;
- `PostSpawnBindingResult` agrega separat progresul si erorile binding-ului; o stare incompleta este afisata ca succes partial, cu NPC-urile ramase spawnate si fara rollback post-spawn;
- persistenta mapping-ului ramane separata si cere `/ainpc world save`.

Acest flux nu foloseste DTO-ul public `SettlementPlan`.

## 3. Planul de patch semantic

Fluxul pentru `/ainpc patch ...` este:

`VillageGapAnalyzer -> GapReport -> VillagePatchPlanner -> PatchPlan -> VillagePatchApplier`

- analiza si planificarea citesc mapping-ul;
- capabilitatea implicita este numai `semantic-place-mapping`;
- patch-urile de cladiri sunt blocate implicit deoarece cer `native-block-build`;
- patch-urile aplicabile creeaza mapping semantic, nu blocuri Minecraft;
- nu exista rollback tranzactional pentru elementele de mapping deja create.

## 4. Preview-ul narativ

`NarrativeGenerator` produce un `PopulationPlan` separat. `PopulationPlanRepository` il persista ca JSON versionat, `list` si `inspect <planId>` il regasesc exact, iar `select <planId>` pastreaza o selectie per regiune. Conversia conserva metadata narativa in `HouseAllocation` si `NpcSpawnPlan`, pregatita pentru persistenta in profilul JSON, dar inspectia o foloseste numai pentru afisare; comanda de spawn regenereaza alte `HouseAllocation` prin `HouseAllocationPlanner` si nu executa preview-ul selectat.

## Regula de vocabular

- `SettlementPlan` inseamna DTO API deprecated, neconectat si neexecutabil;
- `HouseAllocation` inseamna intrarea runtime pentru spawn;
- `PopulationPlan` inseamna preview narativ persistent si selectabil, dar neexecutat;
- `PatchPlan` inseamna completare de mapping semantic;
- niciunul nu implica automat constructie fizica, commit atomic sau rollback global.

## Surse in cod

- `ainpc-api/src/main/kotlin/ro/ainpc/api/settlement/SettlementPlan.kt`
- `ainpc-api/src/main/kotlin/ro/ainpc/api/settlement/SettlementGenerationPlan.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/HouseAllocationPlanner.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/NpcSpawnOrchestrator.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/PostSpawnBindingResult.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/PopulationPlanRepository.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/patch/VillagePatchPlanner.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/NarrativeGenerator.kt`

## Legaturi

- `guides/ordine-spawn-npc-cladiri-region-node.md`
- `architecture/generare-populatie-narativa.md`
- `planning/patch-planner.md`
