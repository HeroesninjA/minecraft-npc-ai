# Simulation Service - Partea 4

Actualizat: 2026-05-06

Status: runbook de implementare pentru introducerea treptata a `SimulationService`, a snapshot-urilor, a comenzilor admin si a semnalelor. Partile 1-3 descriu starea curenta, designul serviciului si semnalele avansate. Partea 4 descrie cum se livreaza efectiv fara regresii.

## Scop

Partea 4 este documentul de lucru pentru implementare.

Obiectivul este sa transforme designul in pasi mici, verificabili:

```text
refactor fara schimbare functionala
-> observabilitate
-> audit/debugdump
-> semnale read-only
-> persistenta optionala
-> consumatori controlati
```

Regula principala:

```text
Nu combina refactorul de baza cu gameplay nou in acelasi pas.
```

Extractia `SimulationService` trebuie sa fie intai o mutare de orchestrare. Semnalele, persistenta si consumatorii quest/story vin dupa ce comportamentul actual ramane stabil.

## Ce acopera partea 4

Acest document acopera:

- impartirea pe PR-uri
- ordinea fisierelor si responsabilitatilor
- invarianti care nu trebuie incalcati
- feature flags si compatibilitate config
- strategie de migration
- comenzi admin initiale
- audit/debugdump initial
- teste unitare si smoke tests
- rollout pe server
- rollback
- criterii de acceptare

Nu acopera:

- economie completa
- pathfinding real
- auto-start de questuri
- story events automate
- settlement simulation complet

## Milestone-uri

### M0 - Baseline

Scop:

- confirma starea curenta
- adauga teste daca lipsesc pentru comportamentul existent
- nu modifica runtime

Livrabile:

- `mvn test` verde
- documentatie actualizata
- lista de fisiere afectate pentru refactor

Exit criteria:

- se poate descrie fluxul curent exact:

```text
SchedulerCoordinator
-> NPCManager.runLifeSimulationTick()
-> DecisionEngine.runLifeSimulationTick(npc)
```

### M1 - Extractie fara schimbare functionala

Scop:

- introduce pachetul `ro.ainpc.simulation`
- muta orchestrarea tick-ului intr-un `SimulationService`
- pastreaza `DecisionEngine` ca sursa pentru update de nevoi si scoring

Livrabile:

- `SimulationService`
- `SimulationTickSummary`
- `SimulationNpcSnapshot` minimal
- `AINPCPlugin.getSimulationService()`
- schedulerul apeleaza `SimulationService.runTick()`
- `NPCManager.runLifeSimulationTick()` devine wrapper sau este eliminat controlat

Exit criteria:

- comportamentul NPC-urilor ramane acelasi
- rutina ramane neschimbata
- autosave ramane neschimbat
- `mvn test` trece

### M2 - Observabilitate admin

Scop:

- adminul poate inspecta simularea fara DB manual

Livrabile:

- `/ainpc simulation tick`
- `/ainpc simulation status [numeNpc|nearest]`
- `/ainpc simulation preview [numeNpc|nearest]`
- summary clar pentru tick
- snapshot clar pentru NPC

Exit criteria:

- `preview` nu modifica NPC-ul
- `tick` manual produce acelasi tip de schimbari ca tick-ul programat
- comenzile sunt admin-only

### M3 - Audit si debugdump

Scop:

- problemele de simulare devin vizibile si exportabile

Livrabile:

- audit read-only pentru simulare
- `simulation-npc-state.json`
- `simulation-tick-summary.json`
- `simulation-warnings.txt`
- includere in `/ainpc debugdump`

Exit criteria:

- JSON invalid in `profile_data` produce warning, nu crash
- NPC fara locatie valida produce warning
- NPC fara home/work/social poate fi vazut clar
- debugdump nu include memorii private sau prompturi brute

### M4 - Semnale read-only

Scop:

- introduce `SimulationSignal` fara gameplay automat

Livrabile:

- `SimulationSignal`
- `SimulationSignalEvaluator`
- `SimulationSignalRegistry`
- 2-3 reguli initiale:
  - `npc_need_critical`
  - `npc_missing_home`
  - `npc_stale_simulation`
- `/ainpc simulation signals`
- debugdump pentru semnale

Exit criteria:

- semnalele sunt deduplicate
- semnalele expira prin TTL
- semnalele nu modifica quest/story/world

### M5 - Persistenta optionala

Scop:

- semnalele importante supravietuiesc restartului

Livrabile:

- tabela `simulation_signals`, daca este justificata
- cleanup pentru semnale expirate
- export debug pentru semnale persistate
- migration testata

Exit criteria:

- serverul poate porni si fara tabela veche
- migration este idempotenta
- rollback-ul poate dezactiva persistenta fara sa blocheze pluginul

### M6 - Consumatori controlati

Scop:

- semnalele pot informa context AI, quest suggestions sau story context, fara actiuni ascunse

Livrabile:

- consumatori explicit configurati
- context AI filtrat
- audit pentru consumatori
- cooldown-uri lungi pentru orice semnal jucabil

Exit criteria:

- nu exista auto-start de quest fara contract separat
- nu exista story state scris direct din `SimulationService`
- semnalele pot fi oprite prin config

## Pachet si ownership

Structura recomandata:

```text
ro/ainpc/simulation/
  SimulationService.java
  SimulationTickSummary.java
  SimulationNpcSnapshot.java
  SimulationNeedState.java
  SimulationPolicy.java
  SimulationWarning.java
  SimulationSignal.java
  SimulationSignalType.java
  SimulationSignalEvaluator.java
  SimulationSignalRegistry.java
```

Fisiere care trebuie atinse in M1:

| Fisier | Schimbare |
|---|---|
| `AINPCPlugin` | initializeaza si expune `SimulationService` |
| `SchedulerCoordinator` | apeleaza `plugin.getSimulationService().runTick()` |
| `NPCManager` | muta sau deleaga tick-ul de simulare |
| `DecisionEngine` | ramane responsabil de decizie; ideal fara schimbari mari in M1 |
| `config.yml` | doar daca este nevoie de comentarii noi, nu schimbari de comportament |

Fisiere care nu trebuie atinse in M1:

- `ScenarioEngine`
- `StoryStateService`
- `QuestAnchorResolver`
- persistence quest
- feature packs

Motiv: M1 trebuie sa fie refactor controlat, nu extindere de gameplay.

## Invarianti

Aceste reguli trebuie sa ramana adevarate dupa fiecare milestone:

- `simulation.enabled=false` opreste tick-ul de simulare.
- `routine.enabled=true` continua sa ruleze rutina separat.
- `DecisionEngine` calculeaza in continuare scorurile de actiune.
- `ScenarioEngine` ramane singurul strat care modifica progresul de quest.
- `StoryStateService` ramane singurul strat care scrie story state.
- Tick-ul de simulare nu trimite requesturi AI.
- Tick-ul de simulare nu scaneaza chunk-uri global.
- Tick-ul de simulare nu executa DB writes per NPC.
- Autosave-ul existent ramane responsabil pentru persistenta profilului NPC.
- Comenzile noi sunt admin-only.

Daca un PR incalca unul dintre acesti invarianti, trebuie impartit sau refacut.

## Feature flags

Config recomandat pentru rollout:

```yaml
simulation:
  enabled: true
  tick_seconds: 30
  service:
    enabled: true
    debug_summary: false
  audit:
    enabled: true
  debugdump:
    include_simulation: true
  signals:
    enabled: false
    persist: false
    expose_to_ai: false
```

Compatibilitate:

- `simulation.enabled` ramane flag-ul principal.
- `simulation.service.enabled` poate fi introdus temporar pentru rollback rapid.
- Daca lipseste `simulation.service.enabled`, default-ul este `true`.
- Daca `simulation.signals.enabled=false`, evaluatorul nu produce semnale.
- Daca `simulation.signals.expose_to_ai=false`, prompturile raman neschimbate.

## Config migration

Nu este nevoie sa fortezi adminii sa schimbe configul manual pentru M1.

Reguli:

- valorile lipsa primesc default-uri in cod
- comentariile din `config.yml` pot fi actualizate
- schimbarea de config nu trebuie sa fie obligatorie pentru pornire
- config invalid produce warning si fallback

Exemplu fallback:

```text
simulation.tick_seconds < 10
-> foloseste 10
-> warning in debug/audit, nu crash
```

## DB migration

M1-M4 nu au nevoie de tabela noua.

Sursele curente raman:

```text
npc_profiles.profile_data.simulation
npc_profiles.profile_data.owned_locations
npc_world_bindings
```

Tabela `simulation_signals` se introduce doar in M5.

Reguli pentru M5:

- migration idempotenta
- `CREATE TABLE IF NOT EXISTS`
- indexuri create cu verificare
- cleanup pentru date expirate
- pluginul functioneaza si daca persistenta semnalelor este dezactivata

Nu muta starea de nevoi intr-o tabela dedicata pana cand exista query-uri reale care o cer.

## Comenzi M2

### `/ainpc simulation tick`

Output minim:

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

Reguli:

- admin-only
- ruleaza pe main thread pentru citiri Bukkit
- nu porneste task async separat
- nu face autosave imediat, decat daca se decide explicit si documentat

### `/ainpc simulation status`

Output minim:

```text
NPC: Matei (ID 42)
Spawned: true
State: WORKING
Goal: sa lucreze la Forja
Activity: lucreaza sau isi rezolva datoriile
Needs: hunger=74 energy=61 social=55 comfort=68 safety=82
Place: home=true work=false social=false danger=false
Last tick: 2026-05-06T...
Warnings: none
```

### `/ainpc simulation preview`

Reguli:

- nu modifica `AINPC`
- nu seteaza `lastSimulationTickAt`
- nu schimba `currentState`
- nu schimba `currentGoal`
- poate calcula actiunea probabila si warnings

Daca preview fara mutatie este greu de obtinut direct, prima versiune poate afisa doar snapshot si top actions read-only.

## Debugdump M3

Schema recomandata pentru `simulation-npc-state.json`:

```json
{
  "generated_at": 0,
  "total_npcs": 0,
  "snapshots": []
}
```

Snapshot compact:

```json
{
  "npc_id": 42,
  "uuid": "...",
  "name": "Matei",
  "spawned": true,
  "state": "WORKING",
  "current_goal": "sa lucreze la Forja",
  "planned_routine_activity": "lucreaza sau isi rezolva datoriile",
  "needs": {
    "hunger": 74,
    "energy": 61,
    "social": 55,
    "comfort": 68,
    "safety": 82
  },
  "anchors": {
    "home": "Casa",
    "work": "Forja",
    "social": "Piata"
  },
  "warnings": []
}
```

Regula: nu include dialog history, memorii, prompturi, API keys sau date private despre jucatori.

## Test matrix

Teste minime pentru M1:

| Test | Asteptare |
|---|---|
| `SimulationServiceDisabledTest` | `simulation.enabled=false` produce summary disabled |
| `SimulationServiceDelegationTest` | NPC spawnat este evaluat o data |
| `SimulationTickSummaryTest` | counterele sunt consistente |
| `RoutineCompatibilityTest` | rutina ramane independenta de simulare |

Teste minime pentru M2:

| Test | Asteptare |
|---|---|
| `SimulationPreviewTest` | preview nu modifica NPC-ul |
| `SimulationStatusFormattingTest` | output-ul nu crapa pe campuri lipsa |
| `SimulationCommandPermissionTest` | non-admin nu poate rula comenzi |

Teste minime pentru M3:

| Test | Asteptare |
|---|---|
| `SimulationAuditTest` | warning pentru ancora lipsa |
| `SimulationDebugDumpTest` | JSON valid si fara date sensibile |
| `SimulationProfileDataTest` | JSON invalid nu opreste auditul |

Teste minime pentru M4:

| Test | Asteptare |
|---|---|
| `SimulationSignalEvaluatorTest` | semnalele se produc doar cand pragurile sunt depasite |
| `SimulationSignalRegistryTest` | cooldown si TTL functioneaza |
| `SimulationSignalsDisabledTest` | flag-ul opreste semnalele |

## Smoke test pe Paper

Script manual recomandat dupa M2:

```text
1. Porneste serverul cu simulation.enabled=true si routine.enabled=true.
2. Verifica logurile de startup.
3. Ruleaza /ainpc list.
4. Ruleaza /ainpc simulation tick.
5. Ruleaza /ainpc simulation status nearest langa un NPC.
6. Ruleaza /ainpc routine status langa acelasi NPC.
7. Asteapta doua tick-uri programate.
8. Verifica daca NPC-ul nu intra in stare absurda sau nu dispare.
9. Ruleaza /ainpc debugdump.
10. Verifica fisierele simulation din dump.
```

Smoke test dupa M4:

```text
1. Activeaza simulation.signals.enabled=true.
2. Ruleaza /ainpc simulation signals all.
3. Forteaza un caz controlat de warning, daca exista comanda de test.
4. Verifica deduplicarea.
5. Verifica expirarea dupa TTL.
6. Ruleaza debugdump si audit.
7. Dezactiveaza signals si confirma ca nu se mai emit semnale noi.
```

## Rollback

Rollback rapid pentru M1-M3:

```yaml
simulation:
  service:
    enabled: false
```

Daca se pastreaza wrapper-ul vechi in `NPCManager`, flag-ul poate reveni temporar la fluxul vechi:

```text
SchedulerCoordinator
-> NPCManager.runLifeSimulationTick()
```

Daca nu se pastreaza wrapper-ul vechi, rollback-ul este revert de PR.

Rollback pentru M4:

```yaml
simulation:
  signals:
    enabled: false
    expose_to_ai: false
    persist: false
```

Rollback pentru M5:

- dezactiveaza persistenta semnalelor
- pastreaza tabela in DB
- nu sterge date automat
- cleanup manual doar dupa backup

## Logging

Loguri recomandate:

- la startup: status `SimulationService`
- la config invalid: warning scurt
- la tick periodic: doar daca `debug_summary=true`
- la comenzi admin: output catre sender, nu spam in console
- la migration: info scurt cu tabela/indexurile create
- la erori DB: warning/severe cu context minim

Nu loga per NPC la fiecare tick, decat intr-un mod de debug explicit si temporar.

## Performanta si limite

Bugete recomandate:

| Numar NPC spawnati | Buget tick |
|---:|---:|
| 50 | sub 5 ms |
| 250 | sub 20 ms |
| 500 | necesita batching sau tick partitioning |

Optimizari daca apar probleme:

- limiteaza NPC-urile evaluate per tick
- imparte evaluarea pe mai multe tick-uri
- cache-uieste snapshot-uri scurte
- nu recalcula agregari regionale la fiecare tick NPC
- separa tick-ul settlement la interval mai mare

Nu introduce optimizari complicate in M1 daca nu exista masuratori.

## PR slicing

Ordine recomandata de PR-uri:

1. `simulation-docs-and-tests-baseline`
2. `simulation-service-extraction`
3. `simulation-admin-commands`
4. `simulation-debugdump-audit`
5. `simulation-readonly-signals`
6. `simulation-signal-persistence`
7. `simulation-ai-context-consumers`
8. `simulation-quest-story-consumers`

Regula:

- un PR introduce un strat
- un PR are teste proprii
- un PR nu introduce simultan persistenta, comenzi si consumatori gameplay

## Checklist pentru M1

Inainte de merge:

- `SimulationService` exista si este initializat o singura data
- schedulerul foloseste serviciul
- `simulation.enabled` este respectat
- NPC-urile nespawnate sunt sarite
- locatiile invalide nu produc crash
- `DecisionEngine` nu este duplicat
- rutina trece in continuare prin `RoutineService`
- autosave ramane separat
- `mvn test` trece

## Checklist pentru M2

Inainte de merge:

- comenzile sunt admin-only
- `tick` afiseaza summary
- `status` merge pentru nume si `nearest`
- `preview` nu modifica NPC-ul
- output-ul este scurt si diagnosticabil
- tab completion este actualizat daca exista
- `mvn test` trece

## Checklist pentru M3

Inainte de merge:

- auditul este read-only
- debugdump produce JSON valid
- datele sensibile sunt excluse
- warning-urile sunt utile, nu zgomot
- fisierele debugdump sunt documentate
- `mvn test` trece

## Checklist pentru M4

Inainte de merge:

- semnalele sunt dezactivate implicit sau clar controlate prin config
- evaluatorul este read-only
- registry-ul aplica cooldown
- TTL-ul expira semnale
- niciun consumator gameplay nu este activ implicit
- debugdump exporta semnalele
- `mvn test` trece

## Riscuri de implementare

Riscuri:

- refactorul schimba accidental ordinea tick-ului
- preview-ul modifica NPC-ul
- comenzile admin declanseaza autosave neasteptat
- debugdump expune date prea detaliate
- semnalele produc zgomot
- persistenta semnalelor creste DB-ul fara cleanup
- consumatorii quest/story apar prea devreme

Masuri:

- separa PR-urile
- testeaza preview fara mutatie
- foloseste feature flags
- porneste semnalele in mod read-only
- cere smoke test Paper inainte de release
- pastreaza rollback simplu

## Definition of Done

Partea 4 este respectata cand implementarea ajunge macar la M4 astfel:

- `SimulationService` este stratul folosit de scheduler
- exista summary si snapshot
- exista comenzi admin initiale
- auditul si debugdump includ simularea
- semnalele read-only exista cu cooldown si TTL
- feature flags pot opri rapid serviciul extins
- nu exista gameplay automat ascuns
- testele Maven trec
- smoke testul Paper este documentat in raportul de release

Pana atunci, partea 4 ramane checklist-ul de implementare si rollout pentru seria `simulation.service`.
