# Schema tehnica pentru structuri de sat, oras si structuri exterioare

Actualizat: 2026-07-02

Acest document descrie schema de date si contractul semantic pentru generarea de:

- case si locuinte
- cladiri de profesie
- cladiri de utilitate
- cladiri de timp liber
- structuri publice
- structuri in afara satului
- structuri pentru questuri
- structuri exterioare

Scopul este sa generam continut reutilizabil, validabil si conectat la NPC, rutina, quest si story.

## 1. Principiul de baza

Fiecare structura trebuie sa existe in doua planuri:

- **plan semantic**: ce este si la ce foloseste
- **plan spatial**: unde este si cum se construieste

Daca lipseste unul dintre ele, structura devine decor gol sau hardcode fragil.

## 2. Tipuri de baza si taxonomie extensibila

### 2.1 Structuri rezidentiale

```text
house
home
apartment
farmhouse
shared_house
inn_room
```

### 2.2 Structuri de profesie

```text
smithy
farm
shop
bakery
mill
workshop
carpentry
tailor_shop
tavern
town_hall
temple
```

### 2.3 Structuri de utilitate

```text
well
storage
barn
gate
bridge
path
road
stable
collection_point
lighting
```

### 2.4 Structuri de timp liber

```text
park
market_square
plaza
bench_area
public_garden
campfire_area
performance_area
```

### 2.5 Structuri publice

```text
administration
court
church
library
watchtower
training_ground
civic_hall
```

### 2.6 Structuri in afara satului

```text
forest
ruin
cave
outpost
fort
shrine
caravan_stop
hidden_camp
border_watch
```

### 2.7 Structuri pentru questuri

```text
quest_board
ancient_altar
sealed_door
investigation_house
delivery_point
secret_cellar
bandit_camp
abandoned_tower
crypt
```

### 2.8 Tipuri suplimentare si indexare

Listele de mai sus sunt tipurile de baza folosite pentru orientare si compatibilitate initiala. Ele nu limiteaza schema.

Noul continut trebuie sa poata introduce tipuri suplimentare fara a modifica schema de baza, atata timp cat sunt:

- inregistrate in taxonomia proiectului
- indexabile semantic
- compatibile cu validarea si query-urile de world/story/quest

Exemple de extensii valide:

- `garden`
- `museum`
- `arena`
- `watch_post`
- `herbalist_hut`
- `faction_hall`
- `relay_station`
- `road_checkpoint`

Reguli:

- nu hardcoda tipurile in locuri care ar trebui sa accepte registry sau alias-uri
- foloseste `typeId` stabil, dar lasa lista de tipuri sa fie extinsa prin configuratie sau catalog
- daca un tip nou are aceleasi functii semantice ca unul existent, poate reutiliza aceeasi categorie, dar cu alt `typeId`
- indexarea trebuie sa permita cautare dupa `typeId`, `tags`, `aliases`, `family` si `questAnchors`

## 3. Modelul de date comun

Toate structurile importante ar trebui sa respecte o forma similara.

```yaml
id: smithy_north_01
type: smithy
category: profession
displayName: Fieraria Nordului
parentRegion: village_center
parentPlace: village_center:main_street
tags: [work, fire, trade, npc_access]
dangerLevel: none
faction: none
entryNodes:
  - smithy_north_01:door
interactionNodes:
  - smithy_north_01:workbench
questAnchors:
  - repair_tool
  - forge_order
npcRoles:
  - blacksmith
  - apprentice
spawnPolicy:
  allowNpcSpawn: true
  allowQuestSpawn: false
requiresValidation: true
```

## 4. Campuri obligatorii

### 4.1 Identitate

- `id`
- `type`
- `category`
- `displayName`

### 4.2 Context spatial

- `parentRegion`
- `parentPlace`
- `bounds`
- `entryNodes`

### 4.3 Context semantic

- `tags`
- `dangerLevel`
- `faction`
- `questAnchors`
- `npcRoles`

### 4.4 Politica de generatie

- `spawnPolicy`
- `requiresValidation`
- `buildMode`
- `templateId`

## 5. Model de template

Un template este un plan reutilizabil pentru o structura.

### 5.1 Template de casa

```yaml
template:
  id: house_small_01
  type: house
  category: residential
  footprint:
    width: 7
    depth: 9
    height: 6
  markers:
    - id: door
      type: entry
      x: 3
      y: 0
      z: 8
    - id: bed
      type: npc_sleep
      x: 2
      y: 1
      z: 3
    - id: storage
      type: utility
      x: 5
      y: 1
      z: 3
  outputs:
    placeType: home
    nodeTypes:
      - door
      - bed
      - storage
  buildMode: native_builder
  requiresValidation: true
```

### 5.2 Template de fierarie

```yaml
template:
  id: smithy_01
  type: smithy
  category: profession
  footprint:
    width: 9
    depth: 11
    height: 7
  markers:
    - id: door
      type: entry
      x: 4
      y: 0
      z: 10
    - id: forge
      type: work
      x: 4
      y: 1
      z: 4
    - id: counter
      type: interaction
      x: 6
      y: 1
      z: 5
  outputs:
    placeType: work
    nodeTypes:
      - work
      - interaction
  buildMode: template
  requiresValidation: true
```

### 5.3 Template de parc

```yaml
template:
  id: park_small_01
  type: park
  category: leisure
  footprint:
    width: 15
    depth: 15
    height: 4
  markers:
    - id: entrance
      type: entry
      x: 7
      y: 0
      z: 14
    - id: bench_1
      type: social
      x: 5
      y: 0
      z: 6
    - id: bench_2
      type: social
      x: 9
      y: 0
      z: 6
  outputs:
    placeType: social
    nodeTypes:
      - entry
      - social
  buildMode: native_builder
  requiresValidation: true
```

### 5.4 Template de structura exterioara

```yaml
template:
  id: forest_edge_outpost_01
  type: outpost
  category: exterior
  footprint:
    width: 11
    depth: 11
    height: 6
  markers:
    - id: entrance
      type: entry
      x: 5
      y: 0
      z: 10
    - id: lookout
      type: interaction
      x: 5
      y: 2
      z: 4
    - id: chest
      type: quest_anchor
      x: 7
      y: 1
      z: 6
  outputs:
    placeType: exterior
    nodeTypes:
      - entry
      - interaction
      - quest_anchor
  buildMode: template
  requiresValidation: true
```

## 6. Marker types recomandate

### Mandatory

- `entry`
- `exit`
- `door`
- `work`
- `bed`

### Optional, dar utile

- `interaction`
- `social`
- `storage`
- `quest_anchor`
- `inspection_point`
- `npc_spawn`
- `loot`
- `danger_point`
- `counter`
- `service_point`

## 7. Footprint si orientare

Template-ul trebuie sa defineasca footprint clar.

```text
width
depth
height
rotation support: 0/90/180/270
```

Reguli:

- marker-ele se transforma odata cu rotatia
- footprint-ul nu se schimba prin rotatie
- marker-ele trebuie sa ramana in bounds
- template-urile nu folosesc coordonate absolute de lume

## 8. Reguli de validare

### Erori critice

- lipseste `id`
- lipseste `type`
- lipseste `category`
- lipseste `placeType` in output
- lipseste marker obligatoriu
- marker in afara footprint-ului
- `buildMode` necunoscut
- template referentiaza node-uri imposibile
- `requiresValidation=false` la structuri care ar trebui validate

### Warning-uri

- lipsa `questAnchors` la structuri care ar putea fi folosite de questuri
- lipsa `npcRoles` la cladiri de profesie
- `allowNpcSpawn=true` dar fara node-uri de casa sau lucru
- template prea mare pentru zona disponibila
- fallback de material activat

## 9. Fluxul de generare

```text
StructureDraft
-> terrain check
-> semantic classification
-> template selection
-> marker transform
-> bounds validation
-> preview
-> confirm
-> build optional
-> semantic commit
-> audit
```

### Etapele

1. **Draft**: se propune tipul si locul.
2. **Validation**: se verifica terenul si suprapunerile.
3. **Template selection**: se alege template-ul potrivit.
4. **Transform**: marker-ele se rotesc si se muta.
5. **Preview**: se afiseaza rezultatul.
6. **Confirm**: se aproba generarea.
7. **Build**: se construieste fizic sau se ramane mapping-only.
8. **Commit**: se salveaza in semantic map.
9. **Audit**: se poate inspecta ce s-a creat si de ce.

## 10. Integrare cu NPC

Structurile trebuie sa livreze ancore pentru comportament natural.

### Rezidential

- `bed`
- `door`
- `storage`
- `courtyard`

### Profesie

- `work`
- `counter`
- `service_point`
- `interaction`

### Timp liber

- `social`
- `bench`
- `performance`
- `market_stall`

### Exterior

- `entry`
- `lookout`
- `loot`
- `quest_anchor`
- `danger_point`

## 11. Integrare cu quest/story

Questurile nu trebuie sa indice coordonate brute. Ele trebuie sa se lege de ID-uri semantice.

Exemple bune:

- `visit_place: smithy_north_01`
- `inspect_node: forest_edge_outpost_01:lookout`
- `deliver_item_to: market_square_01:counter`
- `recover_artifact_from: crypt_01:loot_room`

## 12. Integrare cu patch planner

Patch planner-ul are nevoie de categorii clare:

- `add_house`
- `add_workplace`
- `add_social_place`
- `add_utility`
- `add_public_structure`
- `add_external_structure`
- `add_quest_structure`

Regula:

- planner-ul alege template dupa gap
- template-ul produce output semantic
- build-ul fizic ramane optional

## 13. Ce trebuie evitat

- Nu folosi schematics fara metadata.
- Nu pune structuri fara `placeType`.
- Nu amesteca locuinte cu cladiri de productie fara categorie clara.
- Nu face template-uri cu marker-e hardcodate in lume.
- Nu transforma toate structurile in quest structures.
- Nu lasa structurile exterioare fara `entryNodes`.
- Nu pune NPC spawn fara locuri semantice bune.

## 14. Prioritatea de implementare

Ordinea buna este:

1. schema comuna de date
2. template-uri pentru case si profesii
3. utilitati si spatii publice
4. structuri de timp liber
5. structuri externe
6. structuri pentru quest
7. validare si audit
8. integrare cu NPC, routine si story

## 15. Definitia de gata

Schema este suficient de buna cand:

- o structura poate fi definita fara coordonate brute finale
- template-ul produce `place` si `nodes` corecte
- validarea prinde erorile inainte de build
- quest/story pot folosi ID-uri semantice
- NPC poate folosi structura pentru rutina
- structurile exterioare pot fi inspectate si generate separat

## 16. Maparea la codul existent

Schema de mai sus trebuie sa ramana compatibila cu modelele si planificatoarele deja existente in cod.

### 16.1 Regiuni

`WorldRegionInfo` reprezinta containerul semantic de nivel mare.

Campuri relevante in cod:

- `id`
- `name`
- `worldName`
- `typeId`
- limitele spatiale `minX/minY/minZ/maxX/maxY/maxZ`
- `tags`
- `storyMode`
- `storyStateKey`
- `storyPool`

Regula tehnica:

- o regiune defineste contextul principal pentru sat, oras sau structura exterioara
- `typeId` trebuie sa fie stabil si folosibil in audit, debug si AI
- `tags` trebuie sa permita filtrare semantica, nu doar clasificare cosmetica

### 16.2 Places

`WorldPlaceInfo` reprezinta unitatea locala de gameplay.

Campuri relevante in cod:

- `id`
- `regionId`
- `displayName`
- `worldName`
- `placeType`
- limitele spatiale
- `tags`
- `ownerNpcId`
- `publicAccess`
- `metadata`

Regula tehnica:

- un sat sau oras este compus din multiple `places`
- fiecare `place` trebuie sa aiba un rol clar: casa, lucru, utilitate, social, civic sau exterior
- `metadata` este locul pentru detalii de runtime, nu pentru coordonate brute de quest

### 16.3 Nodes

`WorldNodeInfo` este punctul fin de interactiune.

Campuri relevante in cod:

- `id`
- `regionId`
- `placeId`
- `typeId`
- `worldName`
- `x/y/z`
- `radius`
- `metadata`

Regula tehnica:

- questurile si story-ul trebuie sa consume `node` prin ID semantic
- nodurile sunt mai precise decat place-urile si nu trebuie inlocuite cu coordonate brute in fluxurile de gameplay

### 16.4 Structuri exterioare

`ExteriorStructureType` si `ExteriorStructureAnalyzer` definesc contractul pentru tipurile din afara nucleului satului sau orasului.

`ExteriorStructurePlan` este output-ul citibil de planificator:

- `type`
- `regionId`
- `displayName`
- `regionTypeHint`
- `tags`
- `plannedPlaces`
- `plannedNodes`
- `warnings`

`ExteriorStructurePlanner` transforma intrarea semantica intr-un plan, iar `Analyzer` verifica daca structura propusa este valida, completa si compatibila cu tipul detectat.

Regula tehnica:

- analyzer-ul ramane read-only
- planner-ul produce doar plan, nu efecte laterale
- build-ul fizic sau commit-ul semantic trebuie sa fie o etapa separata

## 17. Schema recomandata pe categorie

### 17.1 Sat

```text
residential + profession + utility + leisure + public + optional_gate
```

Satul trebuie sa optimizeze proximitatea:

- casa langa munca
- munca langa utilitati
- spatii sociale in centru
- iesiri clare spre exterior

### 17.2 Oras

```text
residential + commerce + craft + admin + public + social + gate + district
```

Orasul trebuie sa introduca separare functionala:

- cartiere mai clare
- puncte civice si comerciale distincte
- acces controlat prin porti, artere si noduri de tranzit
- densitate mai mare, dar trasee lizibile

### 17.3 Structuri exterioare

```text
route + access + risk + lookout + loot_or_anchor + optional_shelter
```

Structurile exterioare trebuie sa fie tratate ca gameplay, nu decor:

- trebuie sa aiba motiv de existenta
- trebuie sa aiba intrare si orientare
- trebuie sa aiba un nivel de risc explicit
- trebuie sa poata sustine quest/story sau patrulare NPC

## 18. Regula de prioritate pentru implementare

Cand implementezi sau extinzi schema, ordinea corecta este:

1. `type` si `category`
2. `regionId` si `parentPlace`
3. `placeType` si `nodeTypes`
4. `entryNodes` si `interactionNodes`
5. `tags`, `npcRoles`, `questAnchors`
6. `spawnPolicy` si `buildMode`
7. validare, audit si debugdump

Aceasta ordine reduce riscul de a introduce structuri frumoase dar inutile.

## Concluzie

Generarea buna nu inseamna sa plasezi obiecte la intamplare. Inseamna sa definesti un contract comun pentru tip, rol, noduri, utilitate si conexiune. Cand structurile au acest contract, satul, orasul si zonele exterioare pot fi generate, extinse si folosite de NPC, quest si story fara hardcode manual peste tot.
