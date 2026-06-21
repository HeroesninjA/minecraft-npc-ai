# Harta claselor pentru spawn

Actualizat: 2026-06-21

Acest document este doar documentatie. Nu schimba runtime-ul si nu modifica ordinea de executie.

Pentru orientare rapida, citeste mai intai [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md), apoi [harta claselor de cod](./harta-clase-cod.md).

## Scop

Aceasta harta urmareste subsistemul `spawn` si legatura lui cu `routine`: planificarea household-urilor, validarea alocarii, persistenta household-urilor, batch tracking si miscarea la rutina.

Nu este un inventar complet al tuturor claselor din spawn. Este o harta de lucru pentru nodurile care decid cum apare un NPC in lume si cum este legat de casa si rutina lui.

## Noduri principale

- `NpcSpawnOrchestrator` -> orchestrarea spawn-ului, bind de familie si rollback
- `HouseAllocationPlanner` -> planifica alocari pe regiuni si case
- `HouseAllocationValidator` -> verifica daca alocarea este valida semantic
- `HouseholdPersistenceService` -> persistenta pentru household-uri si rezidenti
- `SpawnBatchTracker` -> urmaresete batch-urile de spawn si permit compensarea
- `NpcSpawnPlan` -> planul concret de spawn al NPC-urilor
- `FamilyBindingPlan` -> planul de legare a familiei
- `RoutineService` -> runtime-ul care muta NPC-ul pe sloturile de zi
- `RoutineEngine` -> calcula asignarea pe `home/work/social/idle`
- `RoutineAssignment` si `RoutineSlot` -> rezultatul si slotul de rutina

## Flux principal

`HouseAllocationPlanner` -> `HouseAllocation`

`HouseAllocationValidator` confirma daca alocarea poate fi aplicata.

`NpcSpawnOrchestrator` -> `NpcSpawnPlan` -> spawn NPC

`NpcSpawnOrchestrator` -> `FamilyBindingPlan` -> bind familie

`HouseholdPersistenceService` salveaza household-urile si rezidentii.

`RoutineService` -> `RoutineEngine` -> `RoutineAssignment` / `RoutineSlot`

## Relatii utile

- `HouseAllocationPlanner` transforma world-ul si regiunea in alocari operationale.
- `HouseAllocationValidator` opreste alocarile care ar produce bind-uri invalide.
- `NpcSpawnOrchestrator` este stratul care executa spawn-ul si gestioneaza rollback-ul.
- `HouseholdPersistenceService` este stratul de persistenta pentru household si backfill.
- `SpawnBatchTracker ajuta la compensare si la urmarirea operatiunilor pe lot.
- `RoutineService` muta NPC-urile dupa spawn in starea zilnica potrivita.

## Cum se citeste

1. Incepe cu `HouseAllocationPlanner`.
2. Continua cu `HouseAllocationValidator`.
3. Treci la `NpcSpawnOrchestrator`.
4. Urmareste `HouseholdPersistenceService` pentru persistenta.
5. Foloseste `RoutineService` si `RoutineEngine` pentru ce se intampla dupa spawn.

## Nota

Daca vrei doar traseul intre module si pachete, foloseste harta de pachete. Daca vrei relatiile dintre clasele de spawn, acesta este documentul potrivit.

