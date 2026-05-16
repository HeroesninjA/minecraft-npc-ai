# Simulation Service - Partea 3

Actualizat: 2026-05-06

Status: design avansat pentru semnale, evenimente si integrarea `SimulationService` cu household-uri, settlement simulation, quest/story si AI. Partea 1 descrie starea reala din cod. Partea 2 descrie extractia si hardening-ul serviciului. Partea 3 descrie cum poate deveni simularea sursa controlata de semnale de gameplay.

## Scop

Partea 3 raspunde la intrebarea: ce facem dupa ce `SimulationService` exista ca serviciu real, testabil si observabil?

Tinta nu este ca simularea sa devina un motor care face totul. Tinta este sa produca semnale clare, validate si auditate, pe care alte sisteme le pot consuma:

```text
SimulationService
-> SimulationNpcSnapshot
-> SimulationSignal
-> SettlementSimulation / QuestDirector / StoryContext / AI prompt
```

Regula centrala:

```text
Simularea observa si emite semnale.
Sistemele specializate decid daca semnalele devin quest, story, dialog sau avertizare admin.
```

## Ce adauga partea 3

Partea 2 se opreste la:

- serviciu extras
- summary de tick
- snapshot NPC
- audit
- debugdump
- teste

Partea 3 adauga design pentru:

- semnale de simulare
- reguli declarative pentru detectarea problemelor
- cooldown si TTL pentru semnale
- agregare pe household si regiune
- persistenta semnalelor importante
- integrare cu quest/story fara side effects ascunse
- expunere catre AI ca rezumat controlat
- rollout pe faze, fara schimbari bruste de gameplay

## Principii

### 1. Semnalele nu sunt actiuni

Un semnal precum:

```text
region_safety_low
```

nu trebuie sa spawneze mobi, sa porneasca questuri sau sa scrie story state direct. El spune doar ca o regula observabila a detectat o stare.

### 2. Consumatorii sunt expliciti

Fiecare consumator trebuie sa declare ce semnale citeste:

| Consumator | Ce poate face |
|---|---|
| `DebugDumpService` | Exporta semnale si warnings |
| `/ainpc simulation audit` | Afiseaza probleme read-only |
| `StoryContextService` | Include rezumat de semnale in context |
| `QuestDirector` viitor | Recomanda questuri pe baza de semnale |
| `SettlementSimulation` | Agrega semnale pe regiune |
| `DialogManager` | Foloseste semnale filtrate pentru ton si context |

### 3. Semnalele au cooldown

Orice semnal care poate alimenta gameplay trebuie sa aiba:

- cheie de deduplicare
- cooldown
- timestamp
- severitate
- TTL
- sursa

Fara aceste limite, serverul poate genera acelasi eveniment la fiecare tick.

### 4. AI-ul nu vede date brute

AI-ul primeste rezumate:

```text
Satul are mai multe semnale recente de siguranta scazuta.
```

Nu primeste:

- toate coordonatele
- lista completa de NPC-uri
- valori interne nefiltrate
- semnale care contin date private despre jucatori

## Model conceptual

### SimulationSignal

Model recomandat:

```text
id
type
scope
severity
source
subjectType
subjectId
regionId
placeId
worldName
x
y
z
message
cooldownKey
metadata
createdAt
expiresAt
consumedAt optional
```

Campuri:

| Camp | Rol |
|---|---|
| `type` | Identificator stabil: `npc_need_critical`, `region_safety_low` |
| `scope` | `NPC`, `HOUSEHOLD`, `PLACE`, `REGION`, `GLOBAL` |
| `severity` | `INFO`, `WARNING`, `CRITICAL` |
| `source` | `simulation`, `routine`, `household`, `settlement` |
| `subjectType` | Entitatea principala: `npc`, `household`, `place`, `region` |
| `subjectId` | ID-ul entitatii principale |
| `cooldownKey` | Cheie pentru deduplicare |
| `metadata` | JSON mic, fara date sensibile |
| `expiresAt` | Cand semnalul nu mai este relevant |

### SimulationSignalType

Tipuri initiale recomandate:

| Tip | Scope | Severitate implicita |
|---|---|---|
| `npc_need_critical` | `NPC` | `WARNING` |
| `npc_missing_home` | `NPC` | `WARNING` |
| `npc_missing_work` | `NPC` | `INFO` |
| `npc_stale_simulation` | `NPC` | `WARNING` |
| `npc_in_danger` | `NPC` | `WARNING` |
| `household_over_capacity` | `HOUSEHOLD` | `WARNING` |
| `household_food_low` | `HOUSEHOLD` | `WARNING` |
| `place_unassigned_workforce` | `PLACE` | `INFO` |
| `region_safety_low` | `REGION` | `WARNING` |
| `region_social_tension_high` | `REGION` | `WARNING` |
| `region_workforce_gap` | `REGION` | `INFO` |

Pentru MVP, primele cinci semnale NPC sunt suficiente.

### SimulationRule

Regula este o verificare declarata si testabila.

Model conceptual:

```text
id
enabled
signalType
scope
condition
cooldownSeconds
ttlSeconds
severity
messageTemplate
```

Exemplu:

```yaml
simulation:
  signals:
    rules:
      npc_need_critical:
        enabled: true
        condition: "any_need_below"
        threshold: 15
        cooldown_seconds: 300
        ttl_seconds: 900
        severity: "WARNING"
```

Pentru inceput, conditiile nu trebuie sa fie un limbaj scriptabil. Este mai sigur sa fie enum-uri Java:

```text
ANY_NEED_BELOW
SAFETY_BELOW
MISSING_HOME
MISSING_WORK
STALE_SIMULATION
HOSTILES_NEARBY
```

## Flux de semnale

Flux recomandat:

```text
SimulationService.runTick()
-> produce SimulationNpcSnapshot
-> SimulationSignalEvaluator.evaluate(snapshot)
-> SimulationSignalRegistry.deduplicate(signal)
-> SimulationSignalStore.saveImportant(signal)
-> consumers read-only
```

Cu agregare:

```text
NPC snapshots
-> household aggregation
-> place aggregation
-> region aggregation
-> settlement signals
```

Regula de rollout:

1. Semnalele sunt doar in debugdump.
2. Semnalele apar in audit.
3. Semnalele apar in context AI filtrat.
4. Semnalele pot alimenta `QuestDirector` viitor.
5. Abia la final semnalele pot produce evenimente persistente de gameplay.

## Evaluator de semnale

Clasa recomandata:

```text
ro.ainpc.simulation.SimulationSignalEvaluator
```

Responsabilitati:

- primeste snapshot-uri read-only
- aplica reguli simple
- produce zero sau mai multe semnale
- nu scrie in DB
- nu modifica NPC-uri
- nu interactioneaza cu quest/story

API conceptual:

```java
public final class SimulationSignalEvaluator {
    public List<SimulationSignal> evaluateNpc(SimulationNpcSnapshot snapshot);
    public List<SimulationSignal> evaluateRegion(SettlementSimulationSnapshot snapshot);
}
```

## Registru si deduplicare

Clasa recomandata:

```text
SimulationSignalRegistry
```

Responsabilitati:

- tine semnalele active in memorie
- aplica cooldown pe `cooldownKey`
- expira semnalele dupa TTL
- ofera citiri read-only pentru audit/debug/context
- decide ce semnale merita persistate

Cheie de cooldown recomandata:

```text
<signalType>:<scope>:<subjectId>
```

Exemple:

```text
npc_need_critical:NPC:42
npc_missing_home:NPC:42
region_safety_low:REGION:satul_central
```

## Persistenta

Pentru inceput, semnalele pot ramane in memorie si in debugdump. Persistenta devine necesara cand:

- semnalul poate declansa quest/story
- adminul trebuie sa investigheze dupa restart
- exista cooldown mai lung decat durata unui restart
- se calculeaza trenduri pe regiune

Tabela posibila:

```sql
CREATE TABLE simulation_signals (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL,
    scope TEXT NOT NULL,
    severity TEXT NOT NULL,
    source TEXT NOT NULL,
    subject_type TEXT NOT NULL DEFAULT '',
    subject_id TEXT NOT NULL DEFAULT '',
    region_id TEXT NOT NULL DEFAULT '',
    place_id TEXT NOT NULL DEFAULT '',
    world TEXT NOT NULL DEFAULT '',
    x REAL,
    y REAL,
    z REAL,
    message TEXT NOT NULL DEFAULT '',
    cooldown_key TEXT NOT NULL DEFAULT '',
    metadata TEXT NOT NULL DEFAULT '{}',
    created_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    consumed_at INTEGER NOT NULL DEFAULT 0
);
```

Indexuri:

```sql
CREATE INDEX idx_simulation_signals_type ON simulation_signals(type);
CREATE INDEX idx_simulation_signals_scope_subject ON simulation_signals(scope, subject_id);
CREATE INDEX idx_simulation_signals_region ON simulation_signals(region_id);
CREATE INDEX idx_simulation_signals_expires ON simulation_signals(expires_at);
```

Reguli:

- `metadata` este JSON mic si validat.
- Semnalele expirate pot fi sterse periodic.
- `consumed_at` se seteaza doar de consumatori expliciti.
- Nu se stocheaza prompturi AI sau dialoguri brute.

## Snapshot de settlement

`SettlementSimulationSnapshot` este o agregare read-only, nu inlocuieste `WorldAdminService`.

Model recomandat:

```text
regionId
worldName
populationCount
spawnedNpcCount
householdCount
averageHunger
averageEnergy
averageSocial
averageComfort
averageSafety
missingHomeCount
missingWorkCount
criticalNeedCount
dangerNpcCount
activeSignals
updatedAt
warnings
```

Acest snapshot poate fi calculat din:

- `SimulationNpcSnapshot`
- `npc_world_bindings`
- household-uri persistente
- mapping region/place/node
- semnale active

Regula: snapshot-ul de settlement este derivat, nu sursa primara pentru NPC.

## Integrare cu household-uri

Semnalele household sunt utile abia cand exista sursa persistenta clara pentru household.

Tipuri recomandate:

```text
household_missing_residents
household_over_capacity
household_food_low
household_low_comfort
household_unbound_home
```

Reguli initiale:

- household fara `homePlaceId` produce warning operational
- household peste capacitate produce warning
- household cu rezidenti fara `homeAnchor` produce warning
- household cu media confortului sub prag produce semnal gameplay doar dupa cooldown

Aceste semnale nu trebuie sa mute NPC-uri. Rutina si binding-urile decid locatia.

## Integrare cu regiuni

Semnalele de regiune trebuie sa fie agregate, nu bazate pe un singur NPC.

Exemple:

| Semnal | Conditie recomandata |
|---|---|
| `region_safety_low` | media safety sub prag si cel putin N NPC afectati |
| `region_workforce_gap` | multe NPC-uri fara work binding |
| `region_social_tension_high` | social scazut la o parte mare din populatie |
| `region_recovery_needed` | energie/confort scazute dupa eveniment |

Config exemplu:

```yaml
simulation:
  settlement:
    enabled: false
    region_tick_seconds: 120
    min_population_for_region_signals: 4
    safety_low_threshold: 35
    safety_low_min_affected: 3
    social_tension_threshold: 30
    social_tension_min_ratio: 0.4
```

Pentru serverele fara mapping, agregarea regionala trebuie sa se dezactiveze curat.

## Integrare cu questuri

Questurile nu trebuie sa fie declansate direct de `SimulationService`.

Flux viitor corect:

```text
SimulationSignalRegistry
-> QuestDirector reads eligible signals
-> QuestDirector checks cooldown and quest limits
-> ScenarioEngine offers/starts quest through normal contract
```

Exemplu:

```text
region_safety_low
-> QuestDirector gaseste quest template "patrol_region"
-> verifica daca jucatorul este eligibil
-> verifica limita de side quests
-> ofera questul prin NPC relevant
```

Reguli:

- semnalul nu modifica `player_quests`
- semnalul nu acorda reward-uri
- semnalul nu seteaza `current_phase`
- questul trebuie sa aiba template validat
- acelasi semnal nu poate genera questuri infinite

Metadata utila pentru semnale consumate de quest:

```json
{
  "recommended_quest_tags": ["safety", "patrol"],
  "preferred_npc_roles": ["guard"],
  "region_id": "satul_central",
  "reason": "average_safety_below_threshold"
}
```

## Integrare cu story

Story-ul poate citi semnale in doua feluri:

1. Context read-only pentru dialog si inspectie.
2. Evenimente story controlate, scrise doar de un serviciu dedicat.

Flux read-only:

```text
SimulationSignalRegistry
-> StoryContextService
-> StoryContextSnapshot
-> DialogManager prompt block
```

Flux cu persistenta viitoare:

```text
SimulationSignal
-> StoryEventDirector
-> validate cooldown
-> StoryStateService.recordEvent(...)
```

Reguli:

- `SimulationService` nu apeleaza direct `StoryStateService`.
- `StoryEventDirector` trebuie sa fie explicit si auditat.
- Story events generate din semnale trebuie sa fie rare, nu per tick.

## Integrare cu AI

AI-ul trebuie sa primeasca semnale ca rezumat scurt:

```text
SIMULATION_SIGNALS:
- Siguranta in regiune este scazuta.
- Mai multi localnici sunt obositi si evita zona de lucru.
- Casa acestui NPC nu este inca mapata.
```

Nu trebuie sa primeasca:

```text
npc_id=42, x=123.4, y=64.0, z=-78.2, hunger=13, safety=22, cooldownKey=...
```

Reguli pentru prompt:

- maxim 3-5 semnale relevante
- prioritizare pe NPC curent, place curent, regiune curenta
- fara coordonate brute daca nu sunt necesare
- fara valori interne exacte, prefera bucket-uri: `low`, `medium`, `high`
- fara semnale expirate

## Comenzi viitoare

Extensii peste comenzile din partea 2:

```text
/ainpc simulation signals [numeNpc|region:<id>|all]
/ainpc simulation signals clear-expired
/ainpc simulation settlement [regionId]
/ainpc simulation rules
/ainpc simulation rule <id> <enable|disable>
```

Roluri:

| Comanda | Rol |
|---|---|
| `signals` | Afiseaza semnale active filtrate |
| `clear-expired` | Curata semnale expirate din registru/persistenta |
| `settlement` | Afiseaza agregarea pe regiune |
| `rules` | Listeaza regulile active si pragurile |
| `rule enable/disable` | Activeaza/dezactiveaza temporar o regula |

Comenzile care schimba reguli runtime trebuie sa fie admin-only si sa logheze schimbarea.

## Debugdump partea 3

Fisiere recomandate:

```text
simulation-signals.json
simulation-signal-rules.json
simulation-settlement-snapshots.json
simulation-signal-consumers.txt
```

`simulation-signals.json`:

- semnale active
- semnale persistate neexpirate
- cooldown state sumarizat

`simulation-signal-rules.json`:

- reguli incarcate
- praguri efective
- sursa configuratiei

`simulation-settlement-snapshots.json`:

- agregari per regiune
- warnings pentru mapping lipsa

`simulation-signal-consumers.txt`:

- ce sisteme consuma semnale
- ce actiuni sunt permise
- ce consumatori sunt dezactivati

## Audit partea 3

Verificari noi:

| Verificare | Severitate |
|---|---|
| regula activata cu tip de semnal necunoscut | eroare configuratie |
| cooldown zero pentru semnal jucabil | warning |
| TTL mai mic decat tick interval | warning |
| semnal persistat expirat dar necuratat | info |
| consumator activ pentru semnal fara cooldown | warning |
| semnal de quest fara template eligibil | info |
| agregare regionala activa fara mapping | warning |
| metadata semnal invalid JSON | eroare operationala |

Auditul ramane read-only.

## Rollout recomandat

Faza A: Observabilitate

- creeaza `SimulationSignal`
- creeaza evaluator pentru 2-3 semnale NPC
- exporta semnale in debugdump
- fara persistenta
- fara consumatori gameplay

Faza B: Audit si comenzi

- adauga `/ainpc simulation signals`
- adauga audit read-only
- adauga reguli configurabile simple
- adauga cooldown in memorie

Faza C: Persistenta usoara

- adauga tabela `simulation_signals`
- persista doar semnale `WARNING` si `CRITICAL`
- curata semnale expirate
- adauga export complet in debugdump

Faza D: Context AI

- adauga rezumat filtrat in `StoryContextService` sau `NPCContext`
- limiteaza numarul de semnale
- testeaza promptul sa nu contina date brute inutile

Faza E: Quest/story consumers

- creeaza consumatori expliciti
- foloseste cooldown-uri lungi
- porneste cu quest suggestions, nu auto-start
- adauga audit pentru consumatori

## Exemple de scenarii

### NPC fara casa

Detectie:

```text
npc.homeAnchor == null
```

Semnal:

```text
npc_missing_home, scope=NPC, severity=WARNING
```

Consumatori permisi:

- audit
- debugdump
- context AI limitat

Consumatori interzisi initial:

- auto-spawn casa
- auto-teleport
- auto-quest

### Siguranta scazuta in regiune

Detectie:

```text
averageSafety < 35
affectedNpcCount >= 3
```

Semnal:

```text
region_safety_low, scope=REGION, severity=WARNING
```

Consumatori viitori:

- `QuestDirector` recomanda quest de patrulare
- `StoryContextService` mentioneaza tensiunea locala
- admin audit vede regiunea afectata

### Household cu confort scazut

Detectie:

```text
household.averageComfort < 30
residentCount > 0
```

Semnal:

```text
household_low_comfort, scope=HOUSEHOLD, severity=INFO/WARNING
```

Consumatori viitori:

- patch planner poate sugera completari de casa
- dialogul poate reflecta nemultumirea locala
- story event se poate inregistra doar prin director separat

## Riscuri

Riscuri principale:

- prea multe semnale produc zgomot operational
- cooldown-uri gresite pot genera spam de questuri
- AI-ul poate exagera semnalele daca promptul este prea dramatic
- persistenta semnalelor poate creste DB-ul fara cleanup
- agregarea regionala poate deveni scumpa pe servere mari
- semnalele pot fi confundate cu sursa de adevar pentru quest/story

Masuri:

- porneste cu semnale putine
- toate semnalele jucabile au cooldown
- debugdump arata sursa si regula
- cleanup periodic pentru semnale expirate
- agregare pe interval separat, nu la fiecare tick NPC
- consumatori expliciti si auditabili

## Definition of Done

Partea 3 este implementata cand:

- exista model `SimulationSignal`
- exista evaluator read-only pentru semnale NPC
- exista registru cu cooldown si TTL
- exista comanda/admin view pentru semnale active
- debugdump exporta semnale si reguli
- auditul valideaza configuratia semnalelor
- semnalele sunt filtrate in context AI fara date brute
- agregarea pe regiune este optionala si dezactivabila
- niciun semnal nu modifica direct quest/story/world state
- `mvn test` trece

Pana atunci, partea 3 ramane ghid pentru transformarea simularii din stare interna in semnale controlate de gameplay.

Continuare: pentru impartirea pe PR-uri, feature flags, migrari, test matrix, smoke tests si rollback, vezi `simulation-service-partea-4.md`.
