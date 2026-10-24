# Runtime de simulare NPC

Status: contract verificat in cod.
Actualizat: 2026-10-24.

Nu exista o clasa de productie numita `SimulationService`. Numele fisierului ramane pentru compatibilitate, iar implementarea reala este distribuita intre doua fluxuri independente: simularea nevoilor si rutina cu miscare.

## Fluxul de simulare

`SchedulerCoordinator -> NPCManager.runLifeSimulationTick() -> DecisionEngine.runLifeSimulationTick()`:

- proceseaza numai NPC-urile spawnate;
- actualizeaza `NPCContext` din lume si activitatea planificata;
- ajusteaza hunger, energy, social, comfort si safety dupa timpul real scurs, limitand rezultatele la 0-100; masa simulata acasa poate recupera satietatea la tick-ul urmator;
- calculeaza scoruri contextuale si alege ponderat dintre actiunile apropiate ca scor, cu alegere determinista pentru pericol, combat si conversatie; actualizeaza fortat starea prin `changeStateFromSimulation` si apoi `currentGoal`;
- publica `AINPCAddon.onNpcStateChange` numai daca starea rezultata este diferita;
- nu muta entitatea si nu scrie quest, story sau economie.

## Autoevaluare comuna pentru decizie si dialog

`NpcCognitiveState` este un instantaneu imuabil al nevoilor, starii, pericolului, interlocutorului, obiectivului si rutinei. `DecisionEngine` foloseste `needModifier` din acest model pentru contributia nevoilor la scorurile actiunilor; coeficientii si pragurile existente sunt pastrate.

Modelul identifica o prioritate de atentie si formuleaza o descriere a nevoilor si intentiei curente. Pericolul imediat are prioritate. Foamea si oboseala respecta pragurile de inceput si recuperare ale ingrijirii. Intentia ramane separata de actiunea executata: descrierea nu confirma consumul de obiecte, deplasarea sau reusita unui obiectiv.

`AINPC.generateContextDescription()` include aceasta autoevaluare in textul copiat de `OpenAIPromptSnapshotFactory`, iar `NpcCognitiveResponse` raspunde local la intrebari despre ganduri, nevoi si intentii prin `DialogueEngine`. Captura citeste numai campuri deja disponibile; nu executa actiuni si nu acceseaza lumea prin API Bukkit. Modelul nu adauga persistenta sau invatare episodica.

## Fluxul de rutina

`SchedulerCoordinator -> RoutineCoordinator.tick() -> RoutineService.runRoutineTick()`:

- proceseaza numai NPC-urile spawnate si sare peste starile ocupate; pentru NPC-uri in simulare completa, prioritizeaza intoarcerea la ancora casei cand foamea sau oboseala scade sub 35 si lasa masa sau somnul sa continue acasa pana cand nevoia ajunge la 60;
- in afara prioritatilor de ingrijire, rezolva profilul, fereastra orara, slotul si ancora prin `RoutineEngine`;
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
- navigatia spre casa pentru nevoi cere simultan simulare `FULL`, rutina activa si o ancora de casa; nu porneste in pericol, iar o pauza sau un override explicit al rutinei are prioritate;
- `RoutineService` respecta pauzele si override-urile din `RoutineCoordinator`; override-ul se aplica doar cat timp este valid si nu poate teleporta NPC-ul;
- `NpcSimulationMode.NONE` exclude NPC-ul, iar `LIGHT` actualizeaza doar nevoile; numai `FULL` intra in selectia actiunii, rutina si procesarea sociala;
- householdu-rile persistente nu sunt citite de motorul de rutina.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/bootstrap/SchedulerCoordinator.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManager.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/DecisionEngine.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/routine/RoutineCoordinator.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/routine/RoutineService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/routine/RoutineSelfCarePolicy.kt`

## Legaturi

- `reference/simulation-stack.md`
- `architecture/probabilitati-actiuni-npc.md`
- `architecture/comportament-natural-npc-rutine-alocari.md`
- `planning/rutine-npc-si-timeline.md`
- `canonical/implementat-deja.md`
