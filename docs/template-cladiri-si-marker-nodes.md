# Template Cladiri si Marker Nodes

Actualizat: 2026-05-04

Status: document canonic initial pentru metadata de template-uri, ancore si marker nodes. Acest document este design de contract; catalogul complet de template-uri, loader-ul si integrarea builder nu sunt considerate implementate pana cand exista cod, validatoare si teste.

## Scop

O cladire generata sau importata nu este utila pentru AINPC daca ramane doar o forma de blocuri.

Fiecare template trebuie sa declare:

- ce fel de loc creeaza
- ce dimensiuni si footprint are
- unde sunt intrarea, paturile, locul de munca si punctele sociale
- ce `WorldPlace` si `WorldNode` trebuie generate
- ce rotatii suporta
- ce capabilitati necesita
- ce reguli de validare trebuie aplicate inainte de build

Acest document defineste contractul dintre:

```text
SettlementPlan / PatchPlan
-> StructureTemplate
-> builder nativ sau WorldEdit
-> WorldPlace / WorldNode
-> NPC bindings, routine, quest, story
```

## Problema curenta

Documentele de generare mentioneaza schematics, template-uri si metadata, dar nu exista inca o schema canonica.

Fara schema:

- un template poate fi lipit fara ancore
- rotatia poate muta gresit intrarea
- o cladire poate fi vizuala, dar inutila pentru rutine
- questurile nu gasesc node-uri de interactiune
- patch planner-ul nu poate valida daca o structura rezolva gap-ul
- builder-ul nu poate raporta ce mapping trebuie creat

## Principii

### 1. Template-ul declara semantica, nu doar forma

Un template valid trebuie sa spuna:

```text
aceasta este o casa
are capacitate 3
are 3 paturi
are intrare spre nord
creeaza place type=house
creeaza node-uri bed/home/entrance/npc_spawn
```

### 2. Marker nodes sunt contract de gameplay

Marker nodes nu sunt decor. Ele sunt punctele prin care runtime-ul intelege structura.

Exemple:

- `entrance`
- `bed`
- `workstation`
- `meeting_point`
- `quest_trigger`
- `storage`
- `danger`

### 3. Transformarile trebuie sa fie deterministe

Rotatia, offset-ul si oglindirea trebuie sa transforme marker-ele la fel ca blocurile.

Nu este acceptabil:

```text
cladirea se roteste
dar ancorele raman pe coordonatele vechi
```

### 4. Template-ul nu decide spawn-ul final

Template-ul poate declara `npc_spawn` sau `bed`, dar nu creeaza NPC-uri.

Spawn-ul ramane in:

- `PopulationPlan`
- `HouseAllocation`
- `NpcSpawnPlan`
- `NpcSpawnOrchestrator`

### 5. Template-ul trebuie sa fie validabil fara build

Inainte de constructie trebuie sa putem verifica:

- fisierul exista
- dimensiunile sunt valide
- marker-ele obligatorii exista
- rotatiile suportate sunt clare
- place/node output-ul este complet
- capabilitatile sunt disponibile

## Relatia cu alte documente

- `settlement-plan.md` foloseste `templateId` in `BuildingPlan` si `PatchPlan`.
- `patch-planner.md` alege patch-uri si poate cere template-uri.
- `generare-ai-si-constructie-automata.md` descrie builder-ul si WorldEdit ca executanti.
- `worldedit-integration-contract.md` defineste contractul adapterului WorldEdit pentru capabilitati, dry-run, commit si rollback.
- `generare-sate-fara-worldedit.md` descrie catalogul nativ de cladiri mici.
- `mapping.md` defineste rezultatul final `WorldPlace` si `WorldNode`.

## Structura recomandata pe disc

Layout conceptual:

```text
templates/
  medieval/
    house_small_oak.yml
    house_small_oak.schem
    forge_small.yml
    forge_small.schem
  native/
    well_small.yml
    market_stall.yml
```

Pentru builder nativ, fisierul `.schem` poate lipsi daca template-ul declara `buildMode: native_patch`.

## Schema `StructureTemplate`

Model conceptual:

```text
StructureTemplate
  id
  version
  themeId
  category
  buildMode
  assetPath
  footprint
  origin
  supportedRotations
  placeOutput
  markerNodes
  connectors
  requiredCapabilities
  stylePalettes
  validationRules
  metadata
```

Campuri:

| Camp | Rol |
|---|---|
| `id` | ID unic de template |
| `version` | Versiunea template-ului |
| `themeId` | Tema: `medieval`, `modern`, `fantasy` |
| `category` | `house`, `workplace`, `social`, `decor`, `road`, `dungeon` |
| `buildMode` | `native_patch`, `worldedit_template`, `semantic_only` |
| `assetPath` | Fisier schematic sau resursa interna |
| `footprint` | Dimensiune ocupata pe teren |
| `origin` | Punctul local de referinta |
| `supportedRotations` | Rotatii permise |
| `placeOutput` | Ce `WorldPlace` creeaza |
| `markerNodes` | Marker-ele locale care devin `WorldNode` |
| `connectors` | Puncte pentru drumuri/intrari |
| `requiredCapabilities` | Capabilitati necesare |
| `stylePalettes` | Variante de materiale |
| `validationRules` | Reguli inainte de build |

## Exemplu YAML

```yaml
id: medieval:house_small_oak
version: 1
theme_id: medieval
category: house
build_mode: worldedit_template
asset_path: templates/medieval/house_small_oak.schem

footprint:
  width: 9
  height: 7
  depth: 11

origin:
  x: 4
  y: 0
  z: 5

supported_rotations: [0, 90, 180, 270]

place_output:
  type: house
  tags: [home, private, family]
  capacity: 3
  public_access: false
  metadata:
    role: home

marker_nodes:
  - id: entrance
    type: entrance
    role: entrance
    local: { x: 4, y: 1, z: 0 }
    radius: 1.5
    tags: [door]
  - id: bed_01
    type: bed
    role: home
    local: { x: 2, y: 1, z: 7 }
    radius: 1.0
  - id: bed_02
    type: bed
    role: home
    local: { x: 6, y: 1, z: 7 }
    radius: 1.0
  - id: npc_spawn
    type: npc_spawn
    role: spawn
    local: { x: 4, y: 1, z: 4 }
    radius: 1.0

connectors:
  - id: front_path
    type: path
    local: { x: 4, y: 0, z: -1 }
    direction: north

required_capabilities:
  - worldedit-execution
  - semantic-place-mapping
```

## `place_output`

`place_output` descrie ce `WorldPlace` rezulta.

Campuri recomandate:

```text
type
tags
capacity
public_access
owner_mode
metadata
```

Tipuri comune:

- `house`
- `forge`
- `farm`
- `market`
- `tavern`
- `shop`
- `camp`
- `custom`

Reguli:

- template-ul de casa trebuie sa aiba `capacity`
- workplace-ul trebuie sa aiba tag `workplace`
- social place-ul trebuie sa aiba tag `social` sau node `meeting_point`
- `custom` trebuie sa aiba tag-uri explicite

## Marker nodes

### Campuri recomandate

```text
id
type
role
local
radius
tags
metadata
required
```

`id` este local in template. ID-ul final se compune din `placeId` si `id`.

Exemplu:

```text
template marker: bed_01
placeId: demo_sat:casa_01
nodeId final: demo_sat:casa_01:bed_01
```

### Tipuri recomandate

```text
entrance
bed
home
npc_spawn
work
workstation
counter
meeting_point
social
quest_trigger
interaction
storage
danger
patrol
road_connector
```

### Marker-ele obligatorii pe categorie

Casa:

- `entrance`
- cel putin un `bed` sau `home`
- `npc_spawn` recomandat

Workplace:

- `entrance`
- `work` sau `workstation`
- `interaction` recomandat

Social:

- `meeting_point` sau `social`
- `quest_trigger` recomandat pentru piata/taverna

Decor interactibil:

- `interaction`

Road/path:

- `road_connector`

## Transformari spatiale

Marker-ele sunt definite in coordonate locale.

Builder-ul trebuie sa aplice:

```text
worldPosition = placementOrigin + rotate(local - origin) + originOffset
```

Reguli:

- rotatia marker-elor trebuie sa urmeze rotatia structurii
- connectorii trebuie sa isi roteasca directia
- bounds-urile finale trebuie recalculare dupa rotatie
- radius-ul ramane neschimbat
- metadata nu trebuie sa contina coordonate absolute hardcodate

Rotatii suportate:

```text
0
90
180
270
```

Oglindirea trebuie evitata in MVP daca nu exista validare separata.

## Connectori

Connectorii spun unde pot fi legate drumuri, garduri sau intrari.

Model:

```text
connectorId
type
local
direction
width
required
```

Tipuri:

- `path`
- `door`
- `fence_gate`
- `road`
- `water_access`

Patch planner-ul poate folosi connectorii pentru `connect_path`.

## Capabilitati

Template-ul poate cere:

```text
worldedit-execution
native-block-build
structure-templates
semantic-place-mapping
terrain-validation
palette-substitution
```

Regula:

- daca o capabilitate lipseste, template-ul nu poate fi executat
- poate ramane disponibil ca draft sau recomandare

## Palete de materiale

Un template poate declara palete:

```yaml
style_palettes:
  plains_medieval:
    wall: oak_planks
    roof: spruce_stairs
    foundation: cobblestone
  mountain_medieval:
    wall: spruce_planks
    roof: dark_oak_stairs
    foundation: stone_bricks
```

Reguli:

- paleta nu schimba marker-ele
- paleta nu schimba footprint-ul
- paleta trebuie validata impotriva materialelor Minecraft

## Validari

### Erori critice

Template-ul este invalid daca:

- lipseste `id`
- lipseste `category`
- lipseste `build_mode`
- `asset_path` lipseste pentru `worldedit_template`
- footprint invalid
- marker obligatoriu lipseste
- doua marker-e au acelasi ID
- marker-ul este in afara footprint-ului
- `place_output.type` lipseste
- `custom` nu are tag semantic
- rotatie ceruta nu este suportata

### Warning-uri

Template-ul poate fi valid cu warning daca:

- lipseste `npc_spawn`, dar exista `bed`
- lipseste `quest_trigger` intr-un social place
- paleta ceruta lipseste si se foloseste fallback
- template-ul are capacitate mai mare decat bed nodes
- connectorii lipsesc pentru cladire izolata

## Integrare cu SettlementPlan

`BuildingPlan.templateId` refera `StructureTemplate.id`.

La validare:

```text
BuildingPlan.templateId
-> StructureTemplate
-> validate capabilities
-> transform marker nodes
-> produce planned WorldPlace/WorldNode
```

`SettlementPlan` nu trebuie sa copieze toata metadata template-ului. Trebuie sa pastreze:

- `templateId`
- rotatie
- bounds finale
- place/node output rezultat
- warnings

## Integrare cu PatchPlanner

Patch planner-ul alege template-uri pentru:

- `add_house`
- `add_workplace`
- `add_social_place`
- `connect_path`
- `decorate_place`

Regula:

- patch planner-ul nu executa template-ul
- doar verifica daca template-ul poate rezolva gap-ul
- executia vine prin builder dupa validare

## Integrare cu mapping

Dupa build sau semantic commit:

```text
place_output -> WorldPlace
marker_nodes -> WorldNode
connectors optional -> WorldNode sau metadata
```

Regula:

- fiecare node final trebuie sa aiba ID stabil
- node-urile trebuie sa intre in bounds-ul place-ului
- place-ul trebuie sa intre in bounds-ul regiunii

## Comenzi recomandate

Read-only:

```text
/ainpc template list [theme]
/ainpc template info <templateId>
/ainpc template validate <templateId>
/ainpc template markers <templateId>
```

Debug:

```text
/ainpc template preview <templateId> [rotation]
```

Preview-ul poate fi doar textual in MVP. Nu trebuie sa plaseze blocuri.

## Faze mici

### T0: Contract documentat

Livrabile:

- documentul de fata
- schema YAML recomandata
- marker types si reguli minime

Nu necesita server Paper.

### T1: Catalog read-only

Livrabile:

- incarcare metadata template
- listare template-uri
- validare schema

### T2: Marker transform

Livrabile:

- transformare local -> world pentru rotatii 0/90/180/270
- validare bounds
- output planned nodes

### T3: Integrare SettlementPlan

Livrabile:

- `BuildingPlan.templateId -> StructureTemplate`
- planned places/nodes generate din markers
- warnings in validation report

### T4: Integrare PatchPlanner

Livrabile:

- selectare template dupa gap type
- verificare capabilitati
- cost/risc dupa dimensiuni si build mode

### T5: Builder integration

Livrabile:

- builder nativ pentru template-uri simple
- WorldEdit adapter optional
- raport anchors transformate dupa build

## Ce trebuie evitat

- Nu lipi schematics fara metadata.
- Nu crea cladiri fara `WorldPlace`.
- Nu crea locuri de munca fara node `work` sau `workstation`.
- Nu roti structuri fara sa rotesti marker-ele.
- Nu pune coordonate absolute in template.
- Nu face WorldEdit obligatoriu pentru catalogul de baza.
- Nu folosi marker labels pentru logica; foloseste IDs si types.

## Definitia de gata pentru MVP

Contractul de template este suficient pentru MVP cand:

- template-ul poate fi validat fara build
- marker-ele obligatorii sunt verificate
- rotatia marker-elor este determinista
- `BuildingPlan` poate produce planned `WorldPlace` si `WorldNode`
- patch planner-ul poate alege template-uri dupa gap
- lipsa capabilitatilor este raportata clar

Abia dupa acest nivel merita introdus paste real cu WorldEdit sau builder nativ mai complex.
