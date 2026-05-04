# SettlementPlan

Actualizat: 2026-05-04

Status: document canonic initial pentru planul complet de sat/regiune. Acest document defineste contractul de design pentru `SettlementPlan`; implementarea completa, serializarea, comenzile `spawnplan` si validatoarele raman faze viitoare pana cand exista cod si teste.

## Scop

`SettlementPlan` este planul inspectabil care descrie ce exista sau ce urmeaza sa existe intr-o regiune inainte de spawn, persistenta finala sau constructie fizica.

El leaga:

- mapping-ul semantic
- cladiri si node-uri
- populatie narativa
- household-uri
- ancore home/work/social
- validari
- spawn plans
- patch/build plans

Fluxul tinta:

```text
scan/import/generator
-> SettlementPlan
-> validate
-> dry-run
-> mapping updates / population plan / house allocations
-> spawn / persistenta / audit
```

Nu trebuie sa mai existe un salt direct de la regiune la NPC-uri spawnate fara plan inspectabil.

## Ce problema rezolva

Acum exista bucati separate:

- `WorldRegion`, `WorldPlace`, `WorldNode`
- planner initial pentru `WorldPlace house -> HouseAllocation`
- planner initial pentru `WorldRegion -> HouseAllocation list`
- `PopulationPlan` propus pentru populatie narativa
- `households` persistente propuse
- `VillagePatchPlanner` propus pentru completarea satelor

Lipseste contractul care le pune intr-un singur plan coerent.

Fara `SettlementPlan`:

- generatorul nu poate fi validat complet inainte de spawn
- populatia poate fi valida, dar mapping-ul incomplet
- patch planner-ul poate propune cladiri fara legatura cu NPC-urile
- rollback-ul nu are context complet
- adminul nu poate inspecta un plan cap-coada

## Principii

### 1. Planul este read-only pana la commit

`SettlementPlan` nu modifica lumea, DB-ul sau config-ul prin simpla creare.

Operatii permise inainte de commit:

- inspectie
- validare
- dry-run
- export
- discard

### 2. ID-uri stabile

ID-urile din plan trebuie sa fie deterministe:

```text
demo_sat:house_01
demo_sat:house_01:bed_01
demo_sat:forge_01
demo_sat:popescu_ion
```

Nu folosi timestamp-uri pentru `placeId`, `nodeId`, `householdKey` sau `npcKey`.

### 3. Separare intre intentie si efect

Planul descrie intentia:

- ce regiune
- ce cladiri
- ce node-uri
- ce populatie
- ce household-uri
- ce ancore

Efectele vin doar prin faze controlate:

- import mapping
- constructie fizica
- spawn NPC
- scriere DB

### 4. Planul nu inlocuieste sursele finale de adevar

Dupa commit:

- mapping-ul sta in `WorldAdmin`
- NPC-urile stau in DB/runtime
- household-urile stau in `households`
- binding-urile stau in `npc_world_bindings`
- quest anchors stau in `quest_anchor_bindings`

Planul ramane audit trail sau artefact de reproducere, nu sursa runtime finala.

### 5. AI poate produce draft, nu commit

AI-ul poate propune un draft de `SettlementPlan`, dar:

- validatorul verifica schema
- mapping-ul verifica ancorele
- build validator verifica terenul
- adminul sau runtime-ul controlat decide commit-ul

## Relatia cu alte documente

- `generare-populatie-narativa.md` defineste `PopulationPlan`, folosit in `SettlementPlan`.
- `households-persistente.md` defineste persistenta rezultata dupa commit.
- `patch-planner.md` defineste `GapReport`, `PatchCandidate` si detaliile pentru `PatchPlan`.
- `template-cladiri-si-marker-nodes.md` defineste template metadata si marker nodes care devin planned places/nodes.
- `ordine-spawn-npc-cladiri-region-node.md` defineste ordinea de spawn si rollback.
- `generare-sate-fara-worldedit.md` si `generare-ai-si-constructie-automata.md` definesc generarea/constructia.
- `worldedit-integration-contract.md` defineste adapterul optional pentru executia structurilor planificate cu WorldEdit.
- `mapping.md` defineste modelul final `WorldRegion -> WorldPlace -> WorldNode`.

## Model principal

### SettlementPlan

Model conceptual:

```text
SettlementPlan
  planId
  version
  source
  status
  seed
  themeId
  regionPlan
  buildingPlans
  nodePlans
  populationPlan
  householdPlans
  spawnPlanRefs
  patchPlans
  validationReport
  createdAt
  updatedAt
```

Campuri:

| Camp | Rol |
|---|---|
| `planId` | ID stabil al planului |
| `version` | Versiunea schema/contract |
| `source` | `manual`, `vanilla_scan`, `ai_draft`, `demo`, `migration` |
| `status` | `DRAFT`, `VALIDATED`, `COMMITTED`, `DISCARDED`, `FAILED` |
| `seed` | Seed determinist |
| `themeId` | Tema de continut, de exemplu `medieval` |
| `regionPlan` | Regiunea tinta |
| `buildingPlans` | Cladiri/place-uri planificate |
| `nodePlans` | Node-uri planificate |
| `populationPlan` | Populatia narativa propusa |
| `householdPlans` | Gospodarii rezultate sau propuse |
| `spawnPlanRefs` | Referinte catre spawn/house allocations |
| `patchPlans` | Constructii/completari fizice propuse |
| `validationReport` | Erori, warning-uri si summary |

### RegionPlan

```text
RegionPlan
  regionId
  displayName
  worldName
  regionType
  bounds
  center
  tags
  storySeed
  metadata
```

Reguli:

- `regionId` este obligatoriu
- `worldName` este obligatoriu daca planul modifica mapping-ul
- bounds trebuie sa fie valide
- regiunea nu trebuie sa se suprapuna cu regiuni incompatibile fara optiune explicita

### BuildingPlan

```text
BuildingPlan
  buildingKey
  placeId
  displayName
  placeType
  bounds
  entrance
  capacity
  templateId
  buildMode
  tags
  requiredNodes
metadata
```

`BuildingPlan.templateId` refera un `StructureTemplate`. Pentru contractul de template si marker nodes, vezi `template-cladiri-si-marker-nodes.md`.

`buildMode` recomandat:

```text
existing
semantic_only
native_patch
worldedit_template
external
```

Reguli:

- `placeId` trebuie sa fie stabil
- `placeType` trebuie sa fie compatibil cu `PlaceType`
- casele trebuie sa declare sau sa poata calcula `capacity`
- workplace-urile trebuie sa aiba cel putin un node de lucru sau un fallback explicit

### NodePlan

```text
NodePlan
  nodeId
  placeId
  nodeType
  x
  y
  z
  radius
  role
  tags
  metadata
```

Node-uri utile:

- `bed`
- `home`
- `entrance`
- `npc_spawn`
- `work`
- `workstation`
- `social`
- `meeting_point`
- `quest_trigger`
- `interaction`
- `storage`
- `danger`

Reguli:

- node-ul trebuie sa fie in regiune
- daca are `placeId`, trebuie sa fie in bounds-ul place-ului
- node-urile obligatorii pe tip de cladire trebuie verificate

### PopulationPlan

`SettlementPlan` poate include sau referi un `PopulationPlan`.

Pentru contractul detaliat, vezi `generare-populatie-narativa.md`.

Reguli:

- populatia nu trebuie sa depaseasca capacitatea caselor
- profesiile reale trebuie sa aiba work place
- `npcKey` trebuie sa fie stabil
- generatorul de populatie nu spawneaza direct NPC-uri

### HouseholdPlan

`HouseholdPlan` poate veni din `PopulationPlan`, dar `SettlementPlan` il pastreaza ca parte verificabila.

```text
HouseholdPlan
  householdKey
  homePlaceId
  familyId
  primaryOwnerNpcKey
  residentNpcKeys
  capacity
  bedNodeIds
  status
```

Pentru persistenta finala, vezi `households-persistente.md`.

### PatchPlan

`PatchPlan` descrie modificari fizice propuse pentru lume.

Pentru contractul complet al gap analyzer-ului si patch planner-ului, vezi `patch-planner.md`.

```text
PatchPlan
  patchId
  type
  buildMode
  targetRegionId
  targetPlaceId
  templateId
  bounds
  requiredCapabilities
  validationStatus
```

Tipuri:

- `add_house`
- `add_workplace`
- `add_social_place`
- `add_node`
- `expand_house`
- `decorate_place`
- `connect_path`

Regula:

- patch-ul nu ruleaza inainte ca planul sa fie validat
- daca cere WorldEdit, planul trebuie sa declare capabilitatea
- patch-ul trebuie sa produca mapping dupa executie

## Format serializat

Format recomandat: YAML pentru administrare, JSON pentru API intern daca este nevoie.

Exemplu minim:

```yaml
settlement_plans:
  demo_sat_plan_01:
    version: 1
    source: demo
    status: DRAFT
    seed: demo_sat:medieval:01
    theme_id: medieval
    region:
      id: demo_sat
      name: Demo Sat
      world: world
      type: settlement
      tags: [demo, medieval, village]
    buildings:
      - key: house_01
        place_id: demo_sat:casa_01
        name: Casa Popescu
        type: house
        build_mode: semantic_only
        capacity: 3
        tags: [home, family]
      - key: forge_01
        place_id: demo_sat:fierarie
        name: Fierarie
        type: forge
        build_mode: semantic_only
        tags: [workplace, blacksmith]
    nodes:
      - node_id: demo_sat:casa_01:bed_01
        place_id: demo_sat:casa_01
        type: bed
        role: home
      - node_id: demo_sat:fierarie:anvil
        place_id: demo_sat:fierarie
        type: workstation
        role: work
    population_plan_ref: demo_sat_pop_01
```

## Validari

### Erori critice

Planul nu poate fi commit-uit daca:

- lipseste `planId`
- lipseste `regionId`
- bounds-urile sunt invalide
- exista doua building-uri cu acelasi `placeId`
- exista doua node-uri cu acelasi `nodeId`
- node-ul este in afara regiunii
- node-ul este in afara place-ului declarat
- house capacity este depasita de household/population
- work place lipseste pentru profesie reala
- patch-ul cere capabilitate indisponibila
- planul are `PopulationPlan` invalid
- planul are `HouseholdPlan` invalid

### Warning-uri

Planul poate fi valid cu warning daca:

- lipseste social place
- lipsesc quest trigger nodes
- casele nu au suficiente bed nodes, dar au fallback
- exista workplace fara NPC asignat
- exista NPC fara work place pentru rol non-profesional
- planul are patch-uri fizice dar ruleaza doar `semantic_only`
- metadata veche poate intra in conflict cu planul

## Comenzi recomandate

```text
/ainpc spawnplan list
/ainpc spawnplan info <planId>
/ainpc spawnplan validate <planId>
/ainpc spawnplan dryrun <planId>
/ainpc spawnplan commit <planId>
/ainpc spawnplan discard <planId>
/ainpc spawnplan export <planId>
```

Numele `spawnplan` poate ramane pentru compatibilitate administrativa, dar intern planul trebuie gandit ca `SettlementPlan`, nu doar spawn.

## Dry-run

Dry-run trebuie sa raspunda la:

- cate regiuni/places/nodes ar fi create sau modificate
- cate household-uri ar fi create
- cati NPC ar fi spawnati
- cate bindings ar fi scrise
- ce patch-uri fizice ar fi necesare
- ce riscuri exista la rollback

Dry-run nu scrie DB si nu modifica mapping-ul.

## Commit

Commit-ul trebuie impartit in pasi controlati:

```text
validate final
-> backup/export plan
-> apply mapping
-> apply patch/build optional
-> write population/households
-> spawn NPC optional
-> write bindings
-> write metadata mirror
-> audit
```

Pentru MVP, commit-ul poate fi limitat la:

- mapping semantic
- conversie catre `HouseAllocation`
- spawn controlat prin orchestrator existent
- bindings si metadata mirror

## Rollback

Rollback-ul trebuie sa stie ce pas a fost aplicat.

Model recomandat:

```text
planId
batchId
stepId
operation
targetType
targetId
beforeState
afterState
status
```

Pana la rollback tranzactional complet, planul trebuie sa produca cel putin un raport post-failure:

- ce NPC-uri au fost create
- ce mapping a fost scris
- ce metadata a fost schimbata
- ce bindings au fost create
- ce trebuie verificat manual

## Integrare cu generarea de sate

Scannerul si patch planner-ul trebuie sa produca sau sa completeze `SettlementPlan`.

Flux:

```text
VanillaVillageScanner
-> SemanticVillageMapper
-> VillageGapAnalyzer
-> VillagePatchPlanner
-> SettlementPlan
-> validate
```

`VillagePatchPlanner` nu trebuie sa construiasca direct. El adauga `PatchPlan`.

Pentru detalii despre prioritizare, validare si build modes, vezi `patch-planner.md`.

## Integrare cu populatia

`PopulationPlan` poate fi generat dupa ce `SettlementPlan` are case si locuri de munca suficiente.

Flux:

```text
SettlementPlan cu buildings/nodes
-> PopulationGenerator
-> PopulationPlan
-> HouseholdPlan
-> validate settlement complet
```

Regula:

- populatia depinde de capacitatea si rolurile cladirilor
- cladirile nu trebuie sa depinda de nume concrete de NPC, ci de roluri sau nevoi

## Integrare cu household persistent

Dupa commit, `HouseholdPlan` devine:

- rand in `households`
- randuri in `household_residents`
- legaturi in `npc_world_bindings`

Pentru contractul DB, vezi `households-persistente.md`.

## Integrare cu quest/story

`SettlementPlan` poate expune ancore pentru questuri, dar nu creeaza quest progress.

Exemple de ancore:

```text
blacksmith_workplace
tavern_social_hub
guard_post
farm_family_house
market_notice_board
```

Acestea pot fi folosite de:

- `QuestAnchorResolver`
- `StoryContextService`
- generare de drafturi quest/story

Regula:

- quest anchors runtime se persista doar cand questul este oferit/acceptat
- planul doar declara ancore candidate

## Faze mici

### SP0: Contract documentat

Livrabile:

- documentul de fata
- model minim pentru plan
- reguli de validare

Nu necesita server Paper.

### SP1: Model read-only in memorie

Livrabile:

- clase/structuri pentru plan
- builder din mapping existent
- validare fara scriere

### SP2: Export/inspect

Livrabile:

- export YAML/JSON
- comanda inspect read-only
- summary pentru admin

### SP3: Conversie catre PopulationPlan si HouseAllocation

Livrabile:

- `SettlementPlan -> PopulationPlan`
- `SettlementPlan -> List<HouseAllocation>`
- validare comuna

### SP4: Commit semantic

Livrabile:

- aplica mapping semantic
- nu construieste fizic
- nu face spawn direct fara dry-run

### SP5: Commit complet controlat

Livrabile:

- mapping
- optional patch/build
- population/households
- spawn
- bindings
- audit

## Ce trebuie evitat

- Nu folosi `SettlementPlan` ca DB runtime principal dupa commit.
- Nu genera NPC-uri direct din `BuildingPlan`.
- Nu construi fizic patch-uri fara validare de teren si capabilitati.
- Nu amesteca quest progress in planul de sat.
- Nu permite commit pe plan cu erori critice.
- Nu sterge planul imediat dupa commit; pastreaza-l pentru audit.

## Definitia de gata pentru MVP

`SettlementPlan` este suficient pentru MVP cand:

- poate fi creat din mapping demo existent
- listeaza regiune, places, nodes, household-uri si populatie propusa
- are validare cu erori si warning-uri clare
- poate fi inspectat fara scrieri
- poate fi convertit in `HouseAllocation`
- poate fi exportat pentru debug
- poate refuza commit cand lipsesc case, node-uri sau work anchors

Abia dupa acest punct merita introdus commit complet cu patch/build si rollback matur.
