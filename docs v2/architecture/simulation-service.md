# Runtime de simulare NPC

Status: contract verificat in cod.
Actualizat: 2026-07-16.

Nu exista o clasa de productie numita `SimulationService`. Numele fisierului ramane pentru compatibilitate, iar implementarea reala este distribuita intre doua fluxuri independente: simularea nevoilor si rutina cu miscare.

## Fluxul de simulare

`SchedulerCoordinator -> NPCManager.runLifeSimulationTick() -> DecisionEngine.runLifeSimulationTick()`:

- proceseaza numai NPC-urile spawnate;
- actualizeaza `NPCContext` din lume si activitatea planificata;
- ajusteaza hunger, energy, social, comfort si safety dupa timpul real scurs;
- alege o actiune prin scor, actualizeaza fortat starea prin `changeStateFromSimulation` si apoi `currentGoal`;
- publica `AINPCAddon.onNpcStateChange` numai daca starea rezultata este diferita;
- nu muta entitatea si nu scrie quest, story sau economie.

## Fluxul de rutina

`SchedulerCoordinator -> RoutineCoordinator.tick() -> RoutineService.runRoutineTick()`:

- proceseaza numai NPC-urile spawnate si sare peste starile ocupate;
- rezolva profilul, fereastra orara, slotul si ancora prin `RoutineEngine`;
- actualizeaza activitatea, goal-ul si starea prin `AINPC.changeState` cu regulile de prioritate;
- incearca intai pathfinding Paper direct spre ancora, apoi teleport configurabil;
- emite `AINPCRoutineChangedEvent` cand activitatea se schimba.

Rutina este singurul dintre cele doua fluxuri care deplaseaza NPC-ul. Nu exista rutare explicita pe reteaua semantica de drumuri.

## Activare si intervale

- simularea cere simultan `features.simulation=true` si `simulation.enabled=true`;
- rutina cere simultan `features.routine=true` si `routine.enabled=true`;
- configuratia livrata are flagurile globale active, dar ambele flaguri locale sunt `false`;
- intervalul configurat este 30 secunde pentru simulare si 120 secunde pentru rutina;
- ambele taskuri ruleaza pe thread-ul principal;
- persistenta starii NPC ruleaza separat la 5 minute: sincronizare pe thread-ul principal, apoi salvare async.
- rehidratarea `currentState` din profil este silentioasa si nu simuleaza o tranzitie runtime.

## Limite confirmate

- nu exista sumar comun pentru cele doua fluxuri;
- simularea nu emite semnale regionale pentru quest sau story;
- `RoutineCoordinator` contine API-uri de pause si override, dar tick-ul nu le consulta;
- `NpcSimulationMode` nu filtreaza tick-ul de simulare;
- householdu-rile persistente nu sunt citite de motorul de rutina.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/bootstrap/SchedulerCoordinator.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManager.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/DecisionEngine.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/routine/RoutineCoordinator.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/routine/RoutineService.kt`

## Legaturi

- `reference/simulation-stack.md`
- `architecture/comportament-natural-npc-rutine-alocari.md`
- `planning/rutine-npc-si-timeline.md`
- `canonical/implementat-deja.md`
