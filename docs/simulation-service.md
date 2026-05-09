# Simulation Service

Actualizat: 2026-05-06

Status: document tehnic pentru serviciul logic `simulation.service`. In codul curent nu exista inca o clasa separata cu acest nume; responsabilitatea este impartita intre `SchedulerCoordinator`, `NPCManager`, `DecisionEngine`, `AINPC`, `NPCContext` si `RoutineService`.

Continuare: pentru planul de extractie, comenzi admin, audit, debugdump si teste, vezi `simulation-service-partea-2.md`.

## Scop

`simulation.service` descrie stratul care tine NPC-urile "vii" intre interactiunile directe cu jucatorul.

Rolul lui este sa:

- ruleze periodic un tick de simulare pentru NPC-urile active
- actualizeze nevoile de baza ale NPC-ului
- citeasca contextul lumii: timp, vreme, locatie, pericol si apropiere de ancore
- aleaga o actiune probabila pe baza de scor
- seteze starea curenta, obiectivul imediat si activitatea planificata
- ofere informatie stabila pentru dialog, rutina si debug

Nu este motor de economie, story sau generare de questuri. Pentru simularea de comunitate si lumea sistemica, vezi `simulare-sat-si-lume.md`.

## Componente reale

Implementarea curenta este distribuita astfel:

| Componenta | Rol |
|---|---|
| `SchedulerCoordinator` | Porneste task-ul periodic de simulare prin `simulation.enabled` si `simulation.tick_seconds` |
| `NPCManager` | Itereaza NPC-urile spawnate si pregateste ancorele de simulare |
| `DecisionEngine` | Ruleaza tick-ul de viata, actualizeaza nevoi, calculeaza scoruri si aplica rezultatul |
| `AINPC` | Pastreaza starea persistabila: nevoi, scop, rutina planificata, ancore si timestamp |
| `NPCContext` | Citeste contextul lumii si sincronizeaza semnalele pentru decizie/dialog |
| `RoutineService` | Serviciu separat, dar legat: muta NPC-ul spre home/work/social dupa rutina zilnica |

Fluxul principal:

```text
SchedulerCoordinator.scheduleLifeSimulation()
-> NPCManager.runLifeSimulationTick()
-> NPCManager.ensureSimulationAnchors(npc)
-> DecisionEngine.runLifeSimulationTick(npc)
-> NPCContext.updateFromWorld(...)
-> DecisionEngine.updateNeeds(...)
-> DecisionEngine.decideAction(...)
-> DecisionEngine.applySimulationOutcome(...)
```

## Tick-ul de simulare

Task-ul periodic este pornit doar daca:

```yaml
simulation:
  enabled: true
```

Intervalul este configurat prin:

```yaml
simulation:
  tick_seconds: 30
```

In cod, intervalul minim este 10 secunde. Task-ul porneste la aproximativ 15 secunde dupa startup, apoi ruleaza la intervalul configurat.

La fiecare tick, `NPCManager.runLifeSimulationTick()`:

- parcurge NPC-urile din cache
- sare peste NPC-urile care nu sunt spawnate
- se asigura ca NPC-ul are ancore de simulare
- trimite NPC-ul catre `DecisionEngine`

Tick-ul nu trebuie sa faca scanari grele, build-uri de lume sau operatii de DB costisitoare. Persistenta periodica este tratata separat de autosave.

## Nevoi simulate

Modelul curent tine cinci nevoi principale, toate in intervalul 0-100:

| Camp | Sens |
|---|---|
| `hungerLevel` | Satietate; scade in timp, scade mai mult cand NPC-ul munceste |
| `energyLevel` | Energie; scade in activitate, creste la somn/odihna/acasa |
| `socialNeedLevel` | Nevoie sociala; scade izolat, creste la interactiuni sau socializare |
| `comfortLevel` | Confort; creste acasa/in interior, scade afara si pe vreme rea |
| `safetyLevel` | Siguranta; scade in pericol, revine gradual cand zona este sigura |

Decay-ul este configurabil:

```yaml
simulation:
  needs:
    hunger_decay: 1.5
    energy_decay: 1.2
    social_decay: 0.8
    comfort_decay: 0.6
    safety_recovery: 1.0
```

`DecisionEngine.updateNeeds(...)` foloseste `lastSimulationTickAt` ca sa scaleze modificarile dupa timpul trecut. Factorul este limitat, ca un restart sau lag temporar sa nu distruga instant starea NPC-ului.

## Context consumat

`NPCContext.updateFromWorld(...)` strange semnale locale pentru decizie:

- timpul Minecraft, convertit in `MORNING`, `AFTERNOON`, `EVENING`, `NIGHT`
- vremea: `CLEAR`, `RAIN` sau `THUNDER`
- biome si categorie de topologie
- daca NPC-ul pare sa fie in interior
- jucatori, NPC-uri si mobi apropiati
- pericol local prin mobi ostili sau siguranta joasa
- apropiere de `homeAnchor`, `workAnchor` si `socialAnchor`
- snapshot semantic prin `WorldContextSnapshotBuilder`, daca mapping-ul este disponibil

Acest context este folosit de scoring si ajunge si in promptul de dialog prin descrierea NPC-ului/contextului.

## Scoring si actiuni

`DecisionEngine.decideAction(...)` alege o actiune prin scor.

Scorul combina:

- scorul de baza al actiunii
- personalitatea NPC-ului
- emotiile curente
- memoria/relatia cu jucatorul, cand exista interactiune
- contextul local: timp, vreme, pericol, locatia curenta
- nevoile curente
- activitatea planificata de rutina
- o variatie random mica

Actiunile posibile sunt filtrate dupa `NPCState`. De exemplu, un NPC in `TALKING` primeste actiuni sociale, iar unul in `COMBAT` primeste actiuni defensive/agresive.

Rezultatul tick-ului este aplicat prin:

```text
NPCAction -> NPCState
NPCAction + context -> currentGoal
```

Exemple:

| Actiune | Stare posibila | Goal posibil |
|---|---|---|
| `START_WORK` | `WORKING` sau `WALKING` | sa ajunga la locul de munca |
| `EAT` | `EATING` sau `WALKING` | sa ajunga acasa pentru a manca |
| `SLEEP` | `SLEEPING` sau `WALKING` | sa se intoarca acasa pentru somn |
| `SOCIALIZE` | `SOCIALIZING` sau `WALKING` | sa ajunga la locul de intalnire |
| `FLEE` | `FLEEING` | sa gaseasca adapost |
| `OBSERVE` | `CURIOUS` | sa inteleaga ce se petrece in jur |

## Legatura cu rutina

`simulation.service` si `RoutineService` sunt apropiate, dar nu sunt acelasi lucru.

| Strat | Ce decide | Ce modifica |
|---|---|---|
| Simulare | Ce nevoie/stare/actiune are NPC-ul | nevoi, `currentState`, `currentGoal`, `plannedRoutineActivity` |
| Rutina | Unde ar trebui sa fie NPC-ul in ziua curenta | slot `HOME/WORK/SOCIAL/IDLE`, tinta, pathfinding Paper si teleport fallback |

`RoutineService` ruleaza separat prin:

```yaml
routine:
  enabled: true
  tick_seconds: 60
```

Rutina foloseste `RoutineEngine` ca sa aleaga una dintre ancore:

- `homeAnchor`
- `workAnchor`
- `socialAnchor`

Pentru MVP, villagerii AINPC au AI-ul Bukkit dezactivat, deci rutina poate teleporta controlat NPC-ul cand tinta este valida si suficient de departe. Simularea decide intentia; rutina ajuta NPC-ul sa ajunga in locul potrivit.

## Persistenta

Starea de simulare este persistata in `npc_profiles.profile_data`, in sectiunea JSON `simulation`:

```json
{
  "simulation": {
    "hunger_level": 82,
    "energy_level": 78,
    "social_need_level": 72,
    "comfort_level": 70,
    "safety_level": 84,
    "current_goal": "",
    "planned_routine_activity": "",
    "last_simulation_tick_at": 0
  }
}
```

Ancorele sunt persistate in aceeasi structura, in `owned_locations`:

```json
{
  "owned_locations": {
    "home": {
      "type": "home",
      "label": "Casa",
      "world": "world",
      "x": 0.0,
      "y": 64.0,
      "z": 0.0
    }
  }
}
```

La incarcare, `NPCManager` hidrateaza aceste campuri inapoi in `AINPC`. Daca exista `npc_world_bindings`, ancorele pot fi reconstruite si din mapping-ul semantic.

Autosave-ul periodic ruleaza separat prin scheduler:

```text
syncAllNPCEntityState()
-> saveAllNPCs(false)
```

## Comenzi si operare

Nu exista inca o comanda dedicata `/ainpc simulation tick`.

Comenzile operationale existente sunt pentru stratul de rutina:

```text
/ainpc routine tick
/ainpc routine status [numeNpc]
```

`/ainpc routine status` este util si pentru diagnostic de simulare, deoarece afiseaza:

- slotul estimat
- activitatea
- goal-ul
- starea tinta
- tinta de ancora
- rutina curenta salvata
- obiectivul curent

Pentru observarea starii simulate a NPC-ului, foloseste comenzile existente de info/debug NPC si inspecteaza profilul persistat daca este nevoie.

## Limite curente

- Nu exista inca o clasa `SimulationService` separata.
- Simularea este per NPC, nu agregata pe sat/regiune.
- Nu exista economie, resurse, reputatie regionala sau evenimente sistemice.
- Rutina foloseste pathfinding Paper spre ancora cand distanta este rezonabila; teleportul ramane fallback pentru distante mari sau target invalid.
- Nu exista audit dedicat pentru degradarea nevoilor sau pentru valori anormale.
- Tick-ul de simulare nu trebuie sa scrie direct story state, quest progress sau modificari de lume.

## Reguli de dezvoltare

Cand se extinde `simulation.service`, pastreaza limitele clare:

- simularea modifica stare de NPC, nu blocuri din lume
- questurile isi controleaza singure progresul prin `ScenarioEngine`
- story state se scrie prin `StoryStateService`
- contextul de mediu ramane read-only pana exista `EnvironmentEngine`
- AI-ul poate descrie consecinte, dar nu devine sursa de adevar pentru runtime
- operatiile grele se fac rar, incremental sau in servicii dedicate

## Directia urmatoare

Extractia intr-o clasa reala `SimulationService` merita facuta cand apare una dintre aceste nevoi:

- comanda admin dedicata pentru tick/preview/reset de nevoi
- teste unitare directe pentru degradare si recuperare
- raport de debugdump pentru starea de simulare
- integrare cu household-uri persistente
- integrare cu indicatori de sat: hrana, siguranta, tensiune, prosperitate
- evenimente de simulare care pot declansa quest/story hooks

Contract minim recomandat pentru o clasa viitoare:

```java
public final class SimulationService {
    SimulationTickSummary runTick();
    SimulationNpcSnapshot preview(AINPC npc);
    void runTickFor(AINPC npc);
    void resetNeeds(AINPC npc);
}
```

Aceasta clasa ar trebui sa orchestreze `DecisionEngine`, nu sa il dubleze si nu sa devina un god service pentru questuri, story, economie si dialog.

Partea urmatoare: `simulation-service-partea-2.md`.
