# Harta claselor pentru rutine

Actualizat: 2026-06-25

Acest document este doar documentatie. Nu schimba runtime-ul si nu modifica ordinea de executie.

Pentru orientare rapida, citeste mai intai [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md), apoi [harta claselor de cod](./harta-clase-cod.md).

## Scop

Aceasta harta urmareste subsistemul `routine`: motorul de rutina zilnica pentru NPC-uri, alocarea sloturilor de activitate, profilele de comportament si miscarea asistata.

## Noduri principale

- `RoutineService` -> orchestreaza bucla principala de rutina, itereaza NPC-urile si aplica miscarea
- `RoutineEngine` -> motorul de decizie: atribuie slot/activitate/goal in functie de ora, ocupatie si nevoi
- `RoutineAssignment` -> rezultatul unei atribuiri de rutina (slot, activitate, goal, stare tinta, ancora tinta)
- `RoutineSlot` -> enum: HOME, WORK, SOCIAL, IDLE
- `RoutineScheduleEntry` -> o intrare in programul zilnic (label, ora, atribuire)
- `RoutineTickSummary` -> sumar per tick: evaluati, mutati, sariti
- `BehaviorProfile` -> profil de comportament per ocupatie (intrari program, parametri miscare)
- `BehaviorProfileLoader` -> incarca si valideaza profile din `behavior_profiles.yml`

## Flux principal

`RoutineService.runRoutineTick()` -> itereaza NPC spawnati -> `RoutineEngine.assignRoutine(npc)` -> `RoutineAssignment` -> aplica teleport sau miscare naturala

`BehaviorProfileLoader` -> `BehaviorProfile` -> `RoutineEngine` foloseste profilele pentru decizii per ocupatie

## Relatii utile

- `RoutineService` este punctul de intrare; este apelat periodic din `SchedulerCoordinator`
- `RoutineEngine` este motorul pur de decizie, fara efecte laterale
- `BehaviorProfile` si `BehaviorProfileLoader` sunt stratul de configuratie pentru rutine
- `RoutineAssignment` leaga decizia de actiunea concreta (teleport/movement)
- `RoutineTickSummary` ofera telemetrie pentru debugging

## Cum se citeste

1. Incepe cu `RoutineService` (punctul de intrare)
2. Continua cu `RoutineEngine` (logica de decizie)
3. Treci la `BehaviorProfile` si `BehaviorProfileLoader` (configuratia)
4. Foloseste `RoutineAssignment` pentru detalii de executie
5. Consulta `RoutineTickSummary` pentru debugging
