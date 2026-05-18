# Patch Planner

Actualizat: 2026-05-11

Status: document canonic si implementare initiala read-only pentru `VillageGapAnalyzer`, `VillagePatchPlanner` si comanda `/ainpc patch analyze|plan|validate`. Patch planner-ul produce `GapReport`, `PatchCandidate` si `PatchPlan`; builder-ul fizic si commit-ul de mapping nu sunt implementate.

## Scop

Patch planner-ul trebuie sa transforme un sat existent sau un mapping partial intr-un set de completari mici, validate si inspectabile.

El raspunde la intrebarea:

```text
Ce lipseste ca regiunea sa poata sustine populatie, rutine, questuri si story?
```

Dar nu executa constructii. El produce `PatchPlan` si recomandari pentru `SettlementPlan`.

Fluxul corect:

```text
VanillaVillageScanner
-> SemanticVillageMapper
-> VillageGapAnalyzer
-> VillagePatchPlanner
-> SettlementPlan.patchPlans
-> validate
-> builder optional
```

## Ce problema rezolva

Scannerul si mapperul pot detecta ce exista deja:

- case
- paturi
- usi
- workstation-uri
- clopot
- ferme
- node-uri sociale

Dar un sat vanilla poate fi incomplet pentru AINPC:

- prea putine case
- case prea mici pentru familii
- lipsa fierarie/taverna/piata
- lipsa node-uri `quest_trigger`
- lipsa loc social central
- lipsa bed nodes suficiente
- lipsa loc de munca pentru o profesie ceruta de pack

Patch planner-ul nu trebuie sa reconstruiasca satul. Trebuie sa propuna completari proportionale si reversibile.

## Principii

### 1. Completeaza, nu inlocui

AINPC trebuie sa adapteze satul existent.

Permis:

- adauga casa lipsa
- adauga node semantic intr-o cladire existenta
- adauga workstation lipsa
- extinde usor o casa
- adauga punct social mic
- adauga drum scurt de legatura

Interzis fara faza separata:

- sterge jumatate de sat
- inlocuieste toate casele
- muta centrul satului
- reconstruieste drumurile complet
- forteaza structuri mari pe teren nepotrivit

### 2. Plan inainte de blocuri

Patch planner-ul produce doar planuri.

Nu ruleaza:

- plasare de blocuri
- WorldEdit paste
- spawn NPC
- scrieri DB finale

### 3. Mapping dupa fiecare patch

Orice patch fizic trebuie sa aiba rezultat semantic:

```text
patch fizic
-> WorldPlace
-> WorldNode
-> tags/metadata
```

Daca un patch nu poate produce mapping, nu este util pentru AINPC.

### 4. Capabilitati explicite

Patch-urile pot cere capabilitati:

```text
native-block-build
worldedit-execution
semantic-place-mapping
terrain-validation
```

Un plan care cere capabilitati lipsa ramane valid doar ca propunere, nu poate fi commit-uit.

### 5. Teren si stil respectate

Patch planner-ul trebuie sa tina cont de:

- biom
- panta
- apa/lava
- coliziuni
- drumuri existente
- distanta fata de centru
- spatiu intre cladiri
- stilul vanilla/local

## Relatia cu alte documente

- `settlement-plan.md` defineste unde intra `PatchPlan` in planul complet.
- `template-cladiri-si-marker-nodes.md` defineste cum patch-urile bazate pe template produc places/nodes.
- `generare-sate-fara-worldedit.md` descrie adaptarea vanilla si builder-ul nativ.
- `generare-ai-si-constructie-automata.md` descrie integrarea optionala cu WorldEdit si AI.
- `worldedit-integration-contract.md` defineste cum patch-urile `worldedit_template` ajung la dry-run, commit si rollback prin adapter optional.
- `mapping.md` defineste rezultatul semantic final.
- `generare-populatie-narativa.md` poate cere patch-uri daca populatia tinta depaseste infrastructura.

## Input-uri

### Din scanner/mapper

```text
DetectedVillage
WorldRegion
WorldPlace list
WorldNode list
```

Semnale utile:

- paturi detectate
- workstation-uri detectate
- usi/interioare
- clopot/centru
- drumuri/cai
- farmland
- apa apropiata
- loturi candidate

### Din SettlementPlan

Patch planner-ul poate primi un `SettlementPlan` partial:

```text
regionPlan
buildingPlans
nodePlans
populationPlan optional
householdPlans optional
```

### Din configuratie

```text
targetPopulation
requiredProfessions
requiredPlaces
minHouseCount
styleTheme
allowedBuildModes
maxPatchCount
maxPatchRadius
```

## Output-uri

### GapReport

`GapReport` descrie ce lipseste.

```text
GapReport
  regionId
  targetPopulation
  currentCapacity
  requiredCapacity
  missingHomes
  missingWorkplaces
  missingSocialPlaces
  missingNodes
  unsafeAreas
  warnings
  errors
```

Exemple de gap-uri:

```text
MISSING_BEDS
MISSING_HOUSE_CAPACITY
MISSING_WORKPLACE_FOR_PROFESSION
MISSING_SOCIAL_HUB
MISSING_QUEST_TRIGGER_NODE
MISSING_ENTRANCE_NODE
HOUSE_TOO_SMALL_FOR_FAMILY
WORKPLACE_WITHOUT_WORK_NODE
```

### PatchCandidate

Un candidat este o propunere inca neverificata complet.

```text
PatchCandidate
  candidateId
  gapType
  patchType
  targetRegionId
  targetPlaceId
  approximateBounds
  priority
  cost
  risk
  requiredCapabilities
  reason
```

### PatchPlan

`PatchPlan` este candidatul validat si pregatit pentru `SettlementPlan`.

```text
PatchPlan
  patchId
  type
  buildMode
  targetRegionId
  targetPlaceId
  templateId
  bounds
  rotation
  stylePalette
  plannedPlaces
  plannedNodes
  requiredCapabilities
  validationStatus
  warnings
```

Tipuri:

```text
add_house
add_workplace
add_social_place
add_node
expand_house
decorate_place
connect_path
mark_existing_place
```

## Tipuri de patch

### `add_house`

Folosire:

- lipsesc paturi/case
- populatia tinta nu incape
- household-ul propus are nevoie de casa separata

Output semantic:

- `WorldPlace type=house`
- node-uri `entrance`, `bed`, `home`, `npc_spawn`

### `add_workplace`

Folosire:

- exista profesie ceruta fara workplace
- pack-ul cere fierarie/taverna/guard post etc.

Output semantic:

- `WorldPlace` compatibil cu profesia
- node `work` sau `workstation`
- optional node `interaction`

### `add_social_place`

Folosire:

- nu exista piata, fantana, taverna sau meeting point
- rutina sociala cade pe fallback prea des

Output semantic:

- `WorldPlace type=market/tavern/custom`
- node-uri `meeting_point`, `social`, `quest_trigger`

### `add_node`

Folosire:

- cladirea exista, dar ii lipseste ancora semantica
- vrei quest trigger sau interaction point fara constructie fizica

Output semantic:

- `WorldNode` nou in place existent

Acesta este cel mai sigur patch si trebuie preferat cand este suficient.

### `expand_house`

Folosire:

- casa exista, dar nu are spatiu sau paturi suficiente
- terenul permite extindere mica

Regula:

- nu extinde agresiv case vanilla
- daca extinderea devine mare, prefera `add_house`

### `connect_path`

Folosire:

- o cladire noua este izolata
- drumul scurt imbunatateste navigabilitatea

Regula:

- drum scurt si simplu
- nu rearanjeaza reteaua de drumuri

## Prioritizare

Ordine recomandata:

1. erori de validare critice
2. case/paturi pentru populatia minima
3. locuri de munca pentru profesii cerute
4. punct social central
5. node-uri pentru rutine
6. node-uri pentru quest/story
7. decor functional

Formula conceptuala:

```text
score =
  severity
  + gameplay_value
  + population_need
  + quest_need
  - build_cost
  - terrain_risk
  - style_mismatch
```

## Validari

### Erori critice

Patch-ul nu poate fi aplicat daca:

- bounds invalide
- coliziune cu regiune/place protejata
- capabilitate lipsa
- template lipsa
- teren imposibil pentru tipul cerut
- patch-ul nu produce mapping
- patch-ul depaseste limita de dimensiune
- patch-ul ar bloca intrari/drumuri existente

### Warning-uri

Patch-ul poate ramane valid cu warning daca:

- distanta fata de centru este mare
- stilul este fallback generic
- numarul de patch-uri este aproape de limita
- social place lipseste inca dupa patch
- nu exista builder disponibil, dar patch-ul poate fi semantic-only

## Build modes

### `semantic_only`

Nu schimba blocuri. Adauga doar mapping.

Util pentru:

- node-uri lipsa
- etichetare loc existent
- quest triggers

### `native_patch`

Construieste simplu cu cod propriu.

Util pentru:

- case mici
- fantana
- taraba
- garduri
- drum scurt

### `worldedit_template`

Foloseste template extern.

Pentru metadata si marker nodes obligatorii, vezi `template-cladiri-si-marker-nodes.md`.

Util pentru:

- cladiri mai complexe
- structuri tematice
- sate mai mari

Trebuie sa ramana optional si conditionat de capabilitate.

## Integrare cu SettlementPlan

Patch planner-ul nu returneaza direct comenzi de build. El completeaza:

```text
SettlementPlan.patchPlans
SettlementPlan.buildingPlans
SettlementPlan.nodePlans
SettlementPlan.validationReport
```

Flux:

```text
GapReport
-> PatchCandidates
-> PatchPlan list
-> SettlementPlan
-> validate
```

## Integrare cu PopulationPlan

Populatia poate cere patch-uri:

```text
targetPopulation=10
currentCapacity=6
-> missing capacity 4
-> add_house sau expand_house
```

Reguli:

- nu genera populatie peste capacitatea planului final
- daca patch-urile necesare nu pot fi aplicate, redu populatia tinta sau opreste planul
- `PopulationPlan` trebuie regenerat sau revalidat dupa patch-uri majore

## Integrare cu quest/story

Questurile si story-ul pot cere ancore:

```text
blacksmith workplace
notice board
meeting point
danger node
storage node
```

Patch planner-ul poate propune `add_node` sau `add_social_place`, dar nu creeaza quest progress.

Regula:

- patch-ul creeaza ancore candidate
- `QuestAnchorResolver` creeaza binding-uri runtime doar cand questul este oferit/acceptat

## Comenzi recomandate

Implementate read-only:

```text
/ainpc patch analyze <regionId> [targetPopulation] [profesiiCSV]
/ainpc patch plan <regionId> [targetPopulation] [profesiiCSV]
/ainpc patch validate <regionId> [targetPopulation] [profesiiCSV]
```

Aceste comenzi nu scriu mapping, nu construiesc blocuri si nu persista planuri. Ele analizeaza regiunea curenta din `WorldAdminApi`, afiseaza gap-urile si, pentru `plan`/`validate`, propun patch-uri inspectabile in mesajul admin.

Planificat ulterior:

```text
/ainpc patch inspect <planId>
/ainpc patch discard <planId>
```

Executie ulterioara, doar dupa persistenta planurilor si validare explicita:

```text
/ainpc patch commit <planId>
```

Pentru MVP, `commit` ramane neimplementat. `analyze`, `plan` si `validate` sunt partea utila pentru audit si pregatirea urmatorilor pasi.

Capabilitatea implicita permisa este doar:

```text
semantic-place-mapping
```

De aceea patch-urile `add_node` pot fi validate ca `semantic_only`, iar patch-urile care cer `native-block-build` sunt propuneri blocate pana cand exista builder nativ.

## Exemplu

Input:

```text
region=demo_sat
targetPopulation=8
current houses=3
bed capacity=5
workplaces=[farm, market]
missing profession=blacksmith
social nodes=0
```

GapReport:

```text
MISSING_BEDS amount=3
MISSING_WORKPLACE_FOR_PROFESSION blacksmith
MISSING_SOCIAL_HUB
MISSING_QUEST_TRIGGER_NODE
```

PatchPlan:

```yaml
patches:
  - patch_id: demo_sat:add_house_04
    type: add_house
    build_mode: native_patch
    reason: capacity_missing
    planned_places: [demo_sat:casa_04]
  - patch_id: demo_sat:add_forge_01
    type: add_workplace
    build_mode: native_patch
    reason: missing_blacksmith_workplace
    planned_places: [demo_sat:fierarie]
  - patch_id: demo_sat:add_notice_board
    type: add_node
    build_mode: semantic_only
    reason: missing_quest_trigger_node
    planned_nodes: [demo_sat:piata:notice_board]
```

## Faze mici

### PP0: Contract documentat

Status: implementat.

Livrabile:

- documentul de fata
- lista de gap types
- lista de patch types

Nu necesita server Paper.

### PP1: Gap analyzer read-only

Status: implementat initial.

Livrabile:

- calculeaza capacitate case/paturi
- detecteaza lipsa workplace/social/quest nodes
- produce `GapReport`

### PP2: Patch candidates

Status: implementat initial.

Livrabile:

- transforma gap-uri in candidati
- calculeaza prioritate/cost/risc
- nu scrie mapping

### PP3: PatchPlan in SettlementPlan

Status: partial. Modelul `PatchPlan` si validarea de capabilitati exista, dar integrarea in `SettlementPlan` si persistenta/inspectia de plan lipsesc.

Livrabile:

- adauga patch-uri in `SettlementPlan`
- valideaza capabilitati
- export inspectabil

### PP4: Semantic-only commit

Status: neimplementat.

Livrabile:

- aplica doar node-uri si tags semantice
- fara blocuri fizice
- audit post-commit

### PP5: Builder integration

Status: neimplementat.

Livrabile:

- `native_patch` pentru structuri mici
- WorldEdit adapter optional
- rollback/undo documentat

## Ce trebuie evitat

- Nu reconstrui satul cand lipsesc doar node-uri.
- Nu executa patch-uri din analyzer.
- Nu aplica patch-uri fara `SettlementPlan` validat.
- Nu crea structuri fara mapping semantic.
- Nu forta WorldEdit ca dependinta obligatorie.
- Nu genera populatie finala inainte sa revalidezi capacitatea dupa patch-uri.

## Definitia de gata pentru MVP

Patch planner-ul este suficient pentru MVP cand:

- poate produce `GapReport` pentru un mapping demo sau sat scanat
- poate propune patch-uri pentru case, workplace, social place si quest node
- fiecare patch are motiv, prioritate, cost si risc
- patch-urile pot fi incluse in `SettlementPlan`
- patch-urile pot fi validate fara a modifica lumea
- patch-urile semantic-only pot fi aplicate separat in faza controlata

Primele trei puncte exista initial in cod. Urmatoarele raman faze de integrare inainte de builder nativ sau WorldEdit pentru patch-uri fizice.
