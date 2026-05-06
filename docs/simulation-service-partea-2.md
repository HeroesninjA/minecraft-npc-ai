# Simulation Service - Partea 2

Actualizat: 2026-05-06

Status: design tehnic pentru pasul urmator. Partea 1 descrie implementarea curenta distribuita. Acest document descrie cum ar trebui extras si maturizat un `SimulationService` real, fara sa schimbe contractele de quest, story sau rutina.

## Scop

Partea 2 raspunde la intrebarea: ce trebuie construit dupa ce stim cum functioneaza simularea curenta?

Tinta este o componenta clara, testabila si observabila:

```text
ro.ainpc.simulation.SimulationService
```

Aceasta componenta trebuie sa orchestreze tick-ul de simulare, dar sa pastreze responsabilitatile separate:

- `DecisionEngine` ramane locul unde se calculeaza actiunea potrivita
- `RoutineService` ramane locul unde NPC-ul este mutat spre ancora potrivita
- `StoryStateService` ramane singurul strat care scrie story state
- `ScenarioEngine` ramane singurul strat care modifica progresul de quest
- `WorldAdminService` ramane sursa pentru mapping semantic

## Motivatie

In codul curent, simularea este functionala, dar logica este impartita:

```text
SchedulerCoordinator
-> NPCManager
-> DecisionEngine
-> AINPC / NPCContext
-> autosave in NPCManager
```

Aceasta impartire este acceptabila pentru MVP, dar devine greu de mentinut cand apar:

- comenzi admin dedicate pentru simulare
- debugdump pe starea nevoilor
- teste unitare pentru decay si recovery
- audit pentru valori imposibile sau timestamp-uri stale
- integrare cu household-uri persistente
- indicatori de sat precum hrana, siguranta si tensiune
- evenimente de simulare care pot informa quest/story

`SimulationService` trebuie introdus ca orchestrator, nu ca rescriere completa.

## Design tinta

Pachet recomandat:

```text
ainpc-core-plugin/src/main/java/ro/ainpc/simulation
```

Clase recomandate:

| Clasa | Rol |
|---|---|
| `SimulationService` | Orchestratorul tick-ului si API-ul folosit de scheduler/comenzi |
| `SimulationTickSummary` | Rezumat agregat pentru un tick complet |
| `SimulationNpcSnapshot` | Snapshot read-only pentru un NPC |
| `SimulationNeedState` | Valorile normalizate ale nevoilor |
| `SimulationPolicy` | Praguri si reguli citite din config |
| `SimulationWarning` | Warning-uri auditabile pentru stari suspecte |
| `SimulationDebugExporter` | Export pentru debugdump, daca nu ramane in `DebugDumpService` |

Nu este obligatoriu ca toate clasele sa apara in primul commit. Ordinea sanatoasa este:

```text
SimulationTickSummary
-> SimulationNpcSnapshot
-> SimulationService
-> SimulationPolicy
-> audit/debugdump
```

## API minim

Contract recomandat pentru prima extractie:

```java
public final class SimulationService {
    public SimulationTickSummary runTick();
    public SimulationTickSummary runTick(Collection<AINPC> npcs);
    public SimulationNpcSnapshot preview(AINPC npc);
    public SimulationNpcSnapshot runTickFor(AINPC npc);
    public boolean resetNeeds(AINPC npc);
}
```

Reguli:

- `runTick()` este folosit de scheduler.
- `runTick(Collection<AINPC>)` este util in teste si comenzi admin.
- `preview(...)` nu modifica NPC-ul.
- `runTickFor(...)` modifica un singur NPC si intoarce snapshot dupa aplicare.
- `resetNeeds(...)` seteaza valori default stabile, nu sterge profilul NPC-ului.

## Rezumat de tick

`SimulationTickSummary` trebuie sa explice ce s-a intamplat fara sa ceara citirea logurilor.

Campuri recomandate:

```text
enabled
totalNpcs
spawnedNpcs
evaluatedNpcs
updatedNeeds
changedState
skippedNotSpawned
skippedInvalidLocation
skippedMissingContext
warnings
startedAt
finishedAt
durationMillis
```

Exemplu de output pentru comanda admin:

```text
=== Simulation Tick ===
NPC total: 18
Spawnati: 16
Evaluati: 16
Nevoi actualizate: 15
Stari schimbate: 7
Skip fara locatie: 1
Warnings: 2
Durata: 4 ms
```

## Snapshot NPC

`SimulationNpcSnapshot` trebuie sa fie read-only si sigur pentru debug.

Campuri recomandate:

```text
npcId
uuid
name
spawned
world
x
y
z
state
currentGoal
plannedRoutineActivity
hunger
energy
social
comfort
safety
timeOfDay
weather
atHome
atWork
atSocialSpot
inDanger
nearbyPlayers
nearbyNpcs
nearbyHostileMobs
homeAnchor
workAnchor
socialAnchor
lastSimulationTickAt
warnings
```

Acest snapshot poate alimenta:

- `/ainpc simulation status`
- `/ainpc simulation preview`
- debugdump
- teste de regresie
- prompt AI, daca este convertit intr-o forma scurta si filtrata

## Politica de nevoi

In prezent, decay-ul este calculat direct in `DecisionEngine.updateNeeds(...)`.

Pentru testabilitate, regulile ar trebui mutate treptat intr-un strat dedicat, dar fara sa sparga scorurile existente.

Contract conceptual:

```java
public final class SimulationPolicy {
    int defaultHunger();
    int defaultEnergy();
    int defaultSocial();
    int defaultComfort();
    int defaultSafety();
    double hungerDecay();
    double energyDecay();
    double socialDecay();
    double comfortDecay();
    double safetyRecovery();
    double maxTickFactor();
}
```

Reguli:

- valorile raman clampate intre 0 si 100
- primul tick pentru un NPC nou seteaza timestamp, dar nu aplica decay agresiv
- tick-ul dupa lag/restart este plafonat
- configuratia invalida se normalizeaza, nu produce crash
- valorile extreme produc warnings, nu corectii ascunse, cu exceptia clamp-ului existent

## Config recomandat

Extensii posibile peste configuratia curenta:

```yaml
simulation:
  enabled: true
  tick_seconds: 30
  max_npcs_per_tick: 250
  debug_summary: false
  audit_warnings: true
  inactive:
    update_unspawned: false
    max_stale_minutes: 60
  needs:
    default_hunger: 82
    default_energy: 78
    default_social: 72
    default_comfort: 70
    default_safety: 84
    hunger_decay: 1.5
    energy_decay: 1.2
    social_decay: 0.8
    comfort_decay: 0.6
    safety_recovery: 1.0
    max_tick_factor: 4.0
```

Pentru MVP, `max_npcs_per_tick` poate ramane doar documentat. Devine important pe servere cu multe NPC-uri spawnate.

## Comenzi admin

Comenzile recomandate pentru faza urmatoare:

```text
/ainpc simulation tick
/ainpc simulation status [numeNpc|nearest]
/ainpc simulation preview [numeNpc|nearest]
/ainpc simulation reset-needs <numeNpc|nearest>
/ainpc simulation audit
```

Roluri:

| Comanda | Rol |
|---|---|
| `tick` | Ruleaza manual tick-ul complet si afiseaza `SimulationTickSummary` |
| `status` | Afiseaza snapshot-ul salvat/curent al NPC-ului |
| `preview` | Calculeaza ce s-ar intampla fara sa modifice starea |
| `reset-needs` | Reseteaza nevoile unui NPC la valori default |
| `audit` | Cauta stari imposibile, timestamp-uri stale si ancore lipsa |

Toate comenzile trebuie sa ceara permisiune admin, pentru ca pot expune locatie, stare interna si profil NPC.

## Debugdump

Debugdump-ul recomandat:

```text
simulation-npc-state.json
simulation-tick-summary.json
simulation-warnings.txt
simulation-config.json
```

`simulation-npc-state.json`:

- lista de `SimulationNpcSnapshot`
- fara dialog history
- fara memorii private ale jucatorilor
- fara prompturi AI brute

`simulation-warnings.txt`:

- NPC spawnat fara profil
- NPC spawnat fara locatie valida
- `lastSimulationTickAt` foarte vechi
- nevoi in valori suspecte dupa hidratare
- ancora lipsa sau in lume inexistenta
- `profile_data` JSON invalid

Regula: debugdump-ul trebuie sa explice cauza probabila si unde se verifica manual, nu doar sa arunce JSON brut.

## Audit

Auditul pentru simulare trebuie sa fie read-only.

Verificari recomandate:

| Verificare | Severitate |
|---|---|
| `profile_data` nu este JSON valid | warning/eroare operationala |
| NPC spawnat fara `homeAnchor` | warning |
| NPC cu `lastSimulationTickAt` zero dupa mai multe tick-uri | warning |
| NPC spawnat intr-o lume inexistenta | eroare |
| nevoi in afara intervalului 0-100 dupa load | warning, chiar daca setter-ele le clampeaza |
| NPC in stare ocupata de foarte mult timp | warning |
| `simulation.enabled=false` dar `routine.enabled=true` | informatie, nu eroare |
| `routine.enabled=false` dar simularea produce goals de miscare | informatie |

Auditul nu trebuie sa repare automat datele. Repararea se face prin comenzi explicite.

## Persistenta si migrari

Prima faza nu are nevoie de tabela noua. Se poate continua cu:

```text
npc_profiles.profile_data.simulation
```

O tabela dedicata devine justificata doar cand apar:

- query-uri frecvente pe nevoi
- rapoarte agregate per regiune
- istoric de tick-uri
- snapshot-uri pentru debug intre restarturi

Tabela viitoare posibila:

```sql
CREATE TABLE npc_simulation_state (
    npc_id INTEGER PRIMARY KEY,
    hunger_level INTEGER NOT NULL,
    energy_level INTEGER NOT NULL,
    social_need_level INTEGER NOT NULL,
    comfort_level INTEGER NOT NULL,
    safety_level INTEGER NOT NULL,
    current_goal TEXT NOT NULL DEFAULT '',
    planned_routine_activity TEXT NOT NULL DEFAULT '',
    last_simulation_tick_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);
```

Regula de migration:

```text
profile_data.simulation ramane sursa compatibila
-> backfill in npc_simulation_state
-> citire prefera tabela noua
-> scriere dubla o perioada
-> dupa validare, profile_data pastreaza doar summary/cache
```

Aceasta migrare nu trebuie facuta pana cand apare o nevoie reala de query/reporting.

## Integrare cu household-uri

Cand household-urile persistente devin sursa de adevar, simularea trebuie sa consume:

```text
household_id
home_place_id
resident_npc_ids
family_id
capacity
food_level optional
comfort_level optional
safety_level optional
```

Efecte posibile:

- NPC fara casa scade mai repede la confort si siguranta
- NPC intr-un household aglomerat recupereaza mai greu confort
- NPC cu familie aproape recupereaza social mai repede
- household cu hrana mica reduce recuperarea de foame

Aceste reguli trebuie introduse gradual si cu config, ca sa nu schimbe brusc comportamentul serverelor existente.

## Integrare cu satul

Pentru agregarea pe regiune, `SimulationService` nu trebuie sa devina `SettlementSimulation`.

Flux recomandat:

```text
SimulationService produce snapshot-uri NPC
-> SettlementSimulation citeste snapshot-uri si mapping
-> calculeaza indicatori de regiune
-> expune stare read-only pentru quest/story/AI
```

Indicatori posibili:

```text
population_count
average_hunger
average_energy
average_safety
unassigned_home_count
unassigned_work_count
danger_warning_count
```

Questurile pot consuma acesti indicatori mai tarziu prin conditii validate, nu prin texte AI.

## Integrare cu quest/story

Reguli stricte:

- simularea nu finalizeaza questuri
- simularea nu acorda reward-uri
- simularea nu scrie direct `player_quests`
- simularea nu scrie direct story state
- simularea poate emite semnale validate pentru sisteme viitoare

Semnale posibile, pentru o faza ulterioara:

```text
npc_need_critical
npc_missing_home
region_safety_low
household_food_low
workforce_unassigned
```

Aceste semnale trebuie sa aiba cooldown, audit si persistenta daca pot declansa continut jucabil.

## Performanta

Reguli pentru server:

- Bukkit entity/world reads raman pe main thread.
- DB writes se fac prin mecanismul existent de autosave sau task async controlat.
- Tick-ul nu scaneaza toate chunk-urile.
- Tick-ul nu cauta pathfinding real.
- Tick-ul nu trimite requesturi AI.
- Tick-ul nu logheaza per NPC decat in debug explicit.
- Pe servere mari, se introduce batching sau limita `max_npcs_per_tick`.

Buget initial recomandat:

```text
sub 5 ms pentru 50 NPC spawnati
sub 20 ms pentru 250 NPC spawnati
0 requesturi externe
0 scanari globale de lume
```

Aceste valori sunt tinta operationala, nu garantie fara benchmark.

## Testare

Teste unitare recomandate:

| Test | Ce valideaza |
|---|---|
| `SimulationPolicyTest` | default-uri, clamp, config invalid |
| `SimulationNeedStateTest` | decay, recovery, tick factor plafonat |
| `SimulationServiceTest` | skip pentru NPC invalid, summary corect |
| `SimulationPreviewTest` | preview nu modifica NPC-ul |
| `DecisionEngineSimulationTest` | actiunile raman compatibile dupa extractie |
| `SimulationAuditTest` | warnings pentru JSON invalid, timestamp stale si ancore lipsa |

Teste de integrare recomandate:

- NPC cu home/work/social valid produce snapshot complet
- NPC fara ancora produce warning, nu crash
- `simulation.enabled=false` opreste tick-ul din scheduler
- `routine.enabled=true` ramane functional dupa extractie
- autosave pastreaza `simulation` in `profile_data`

## Plan incremental

Ordinea recomandata:

1. Adauga `SimulationTickSummary` si `SimulationNpcSnapshot` fara schimbare de comportament.
2. Creeaza `SimulationService` care apeleaza in continuare `DecisionEngine.runLifeSimulationTick(npc)`.
3. Muta `NPCManager.runLifeSimulationTick()` sa delege catre `SimulationService`.
4. Adauga comanda `/ainpc simulation tick` peste summary.
5. Adauga `preview` read-only pentru un NPC.
6. Extrage politica de nevoi intr-o clasa testabila.
7. Adauga audit read-only.
8. Adauga debugdump dedicat.
9. Abia apoi leaga household-uri si indicatori de sat.

Primele trei etape trebuie sa fie refactorizare fara schimbare functionala.

## Definition of Done

Partea 2 este implementata complet cand:

- scheduler-ul foloseste `SimulationService`
- `NPCManager` nu mai contine orchestrarea directa a tick-ului
- exista summary pentru tick manual si periodic
- exista snapshot read-only pentru un NPC
- exista teste pentru nevoi si preview
- exista audit read-only pentru stari suspecte
- debugdump include starea de simulare
- comportamentul existent al rutinei si dialogului ramane neschimbat
- `mvn test` trece

Pana atunci, documentul ramane ghid de implementare, iar partea 1 ramane descrierea starii reale din cod.

Continuare: pentru semnale de simulare, evenimente controlate, agregare pe settlement si consumatori quest/story/AI, vezi `simulation-service-partea-3.md`.
