# Schema YAML concreta pentru village, city, outpost, dungeon si quest structure

Actualizat: 2026-07-02

Acest document completeaza `docs/schema-structuri-sat-oras-si-exterioare.md` cu exemple YAML concrete pentru tipurile de baza ale lumii.

Scopul este sa avem un format clar, reutilizabil si validabil pentru tipurile de baza si pentru tipuri extinse, fara hardcode rigid:

- `village`
- `city`
- `outpost`
- `dungeon`
- `quest_structure`

Tipurile de mai sus sunt exemple de baza, nu o lista inchisa. Schema trebuie sa permita tipuri suplimentare prin taxonomie, alias-uri si indexare semantica.

## 1. Reguli generale

Orice structura generata trebuie sa aiba:

- `id` stabil
- `type` semantic
- `category`
- `displayName`
- `parentRegion`
- `tags`
- `entryNodes`
- `interactionNodes`
- `questAnchors`
- `requiresValidation`

Optional, dupa caz:

- `parentPlace`
- `npcRoles`
- `spawnPolicy`
- `dangerLevel`
- `faction`
- `buildMode`
- `templateId`

Tipurile noi trebuie sa poata folosi acelasi contract, atata timp cat sunt inregistrate in taxonomia proiectului si pot fi indexate semantic.

## 2. Format comun

```yaml
id: example_id
type: example_type
category: example_category
displayName: Example Name
parentRegion: world_north
parentPlace: world_north:main_route
tags:
  - semantic_tag_1
  - semantic_tag_2
dangerLevel: none
faction: none
entryNodes:
  - example_id:entry
interactionNodes:
  - example_id:interaction
questAnchors:
  - example_anchor
npcRoles:
  - example_role
spawnPolicy:
  allowNpcSpawn: false
  allowQuestSpawn: true
buildMode: template
requiresValidation: true
```

### 2.1 Reguli pentru extensii

Cand adaugi un tip nou:

- pastreaza `type` stabil si semantic
- adauga `tags` si `questAnchors` relevante
- foloseste `category` existent sau introduce o categorie noua doar daca aduce valoare reala
- evita hardcode-ul in favorul unui registry sau catalog de tipuri
- asigura indexare dupa `type`, `tags`, `aliases`, `family` sau `indexKeys` daca sunt disponibile in implementare

## 3. `village`

Satul este o asezare mica sau medie, cu case, locuri de munca, utilitati si spatiu social.

### Scop

- viata zilnica pentru NPC
- casa + munca + social + trasee naturale
- baza pentru questuri locale

### Exemplu

```yaml
id: village_greenfield
type: village
category: settlement
displayName: Satul Greenfield
parentRegion: world_north
tags:
  - residential
  - farming
  - market
  - social
dangerLevel: low
faction: village_faction
entryNodes:
  - village_greenfield:north_gate
  - village_greenfield:south_path
interactionNodes:
  - village_greenfield:market_square
  - village_greenfield:well
questAnchors:
  - missing_supplies
  - broken_fence
  - village_meeting
npcRoles:
  - villager
  - farmer
  - blacksmith
  - merchant
spawnPolicy:
  allowNpcSpawn: true
  allowQuestSpawn: true
buildMode: native_builder
requiresValidation: true
```

### Noduri recomandate

- gate
- path
- house_door
- workbench
- market_counter
- well
- bench
- notice_board

## 4. `city`

Orasul are densitate mai mare, cartiere distincte si o separare mai clara intre zona rezidentiala, zona comerciala si zona administrativa.

### Scop

- economie mai complexa
- multe roluri NPC
- spatiu pentru cladiri publice si social
- generator de questuri urbane

### Exemplu

```yaml
id: city_elyndor
type: city
category: settlement
displayName: Orasul Elyndor
parentRegion: capital_domain
tags:
  - urban
  - trade
  - administration
  - culture
dangerLevel: medium
faction: city_council
entryNodes:
  - city_elyndor:main_gate
  - city_elyndor:harbor_gate
interactionNodes:
  - city_elyndor:civic_hall
  - city_elyndor:central_market
  - city_elyndor:library_steps
questAnchors:
  - lost_record
  - guild_request
  - civic_dispute
npcRoles:
  - citizen
  - guard
  - merchant
  - clerk
  - artisan
spawnPolicy:
  allowNpcSpawn: true
  allowQuestSpawn: true
buildMode: template
requiresValidation: true
```

### Sectoare recomandate

- residential district
- commercial district
- admin district
- craft district
- public district
- outer gates

## 5. `outpost`

Outpost-ul este o structura mica in afara satului sau orasului, folosita pentru control, tranzit, observatie sau gameplay de margine.

### Scop

- control de zona
- punct de odihna
- legatura intre regiuni
- questuri de traseu sau observare

### Exemplu

```yaml
id: outpost_forest_watch
type: outpost
category: exterior
displayName: Postul de observatie din padure
parentRegion: forest_border
tags:
  - exterior
  - watch
  - route
  - story
dangerLevel: low
faction: kingdom_watch
entryNodes:
  - outpost_forest_watch:gate
interactionNodes:
  - outpost_forest_watch:lookout
  - outpost_forest_watch:campfire
questAnchors:
  - observe_tracks
  - deliver_report
npcRoles:
  - guard
  - scout
spawnPolicy:
  allowNpcSpawn: true
  allowQuestSpawn: true
buildMode: template
requiresValidation: true
```

### Noduri recomandate

- gate
- lookout
- campfire
- storage
- signal_point

## 6. `dungeon`

Dungeon-ul este o structura de risc, explorare sau boss gameplay. Poate fi o pestera, o ruina, o cripata sau un complex mai mare.

### Scop

- explorare
- lupta
- loot
- mister
- quest progres

### Exemplu

```yaml
id: dungeon_wolf_crypt
type: dungeon
category: exterior
displayName: Cripta Lupilor
parentRegion: dark_woods
tags:
  - dungeon
  - loot
  - boss
  - story
dangerLevel: high
faction: none
entryNodes:
  - dungeon_wolf_crypt:entrance
interactionNodes:
  - dungeon_wolf_crypt:checkpoint
  - dungeon_wolf_crypt:loot_room
  - dungeon_wolf_crypt:boss_room
questAnchors:
  - enter_dungeon
  - defeat_boss
  - recover_artifact
spawnPolicy:
  allowNpcSpawn: false
  allowQuestSpawn: true
buildMode: template
requiresValidation: true
```

### Noduri recomandate

- entrance
- checkpoint
- trap_corridor
- loot_room
- boss_room
- exit
- inspection_point

### Reguli

- intrarea si iesirea trebuie sa fie separate
- loot-ul trebuie sa fie node sau anchor, nu doar tag
- dangerLevel trebuie sa fie explicit
- dungeon-ul nu trebuie sa declanseze automat spawn agresiv fara control runtime

## 7. `quest_structure`

Quest structure este orice loc creat sau marcat pentru progresie, investigatie, livrare, rescue sau activare narativa.

### Scop

- sustinere de quest
- legare de story
- progresie controlata
- semnale de world state

### Exemplu

```yaml
id: quest_board_town_center
type: quest_structure
category: quest
displayName: Panoul de anunturi al orasului
parentRegion: city_elyndor
tags:
  - quest
  - notice
  - social
  - civic
dangerLevel: none
faction: city_council
entryNodes:
  - quest_board_town_center:front
interactionNodes:
  - quest_board_town_center:board
questAnchors:
  - accept_bounty
  - collect_notice
  - start_local_job
spawnPolicy:
  allowNpcSpawn: false
  allowQuestSpawn: true
buildMode: native_builder
requiresValidation: true
```

### Alte exemple

- altar antic
- casa investigabila
- poarta inchisa
- punct de livrare
- pivnita secreta
- turn abandonat
- magazin cu lipsa de marfa
- tabara banditilor

## 8. Schema recomandata pe tip

### Village

```text
residential + profession + utility + leisure + public
```

### City

```text
residential + commerce + craft + admin + public + social + gates
```

### Outpost

```text
control + route + watch + small shelter + supply
```

### Dungeon

```text
entrance + danger path + checkpoint + loot + boss + exit
```

### Quest structure

```text
anchor + interaction + objective + story hook
```

## 9. Set minim de noduri pe tip

### Village

- `house_door`
- `work_node`
- `market_node`
- `social_node`
- `entry_node`

### City

- `district_gate`
- `market_counter`
- `civic_entry`
- `public_square`
- `house_entry`

### Outpost

- `gate`
- `lookout`
- `campfire`
- `storage`

### Dungeon

- `entrance`
- `checkpoint`
- `loot`
- `boss_spawn`
- `exit`

### Quest structure

- `front`
- `board`
- `inspect_point`
- `interaction`

## 10. Policy recomandat pentru spawn

```yaml
spawnPolicy:
  allowNpcSpawn: true
  allowQuestSpawn: true
  allowMonsterSpawn: false
  allowAmbientSpawn: false
  allowStoryOverrides: true
```

### Reguli

- satul si orasul permit NPC spawn
- dungeon-ul poate permite spawn de quest, dar nu neaparat NPC
- outpost-ul poate permite NPC de paza
- quest_structure poate permite doar interactiuni si progres

## 11. Reguli pentru build mode

Posibile valori:

```text
manual
template
native_builder
worldedit_optional
scan_only
```

### Recomandare

- `template` pentru structuri repetitive
- `native_builder` pentru locuri simple si controlabile
- `worldedit_optional` doar ca adapter
- `scan_only` pentru mapping fara constructie

## 12. Legatura cu NPC si rutina

### Village

- casa
- loc de munca
- loc social
- drumuri regulate

### City

- naveta
- profesii specializate
- pauze si tranzit
- zone publice mai dense

### Outpost

- schimburi de garda
- patrulare
- tranzit intre regiuni

### Dungeon

- quest, explorare, lupta
- nu rutina zilnica normala

### Quest structure

- activare, inspectie, dialog, livrare
- nu comportament de rutina

## 13. Legatura cu quest/story/AI

Structurile trebuie sa fie consumate prin ID semantic.

Exemple bune:

- `visit_place: village_greenfield:market_square`
- `inspect_node: city_elyndor:civic_hall`
- `deliver_item_to: quest_board_town_center:board`
- `defeat_boss_at: dungeon_wolf_crypt:boss_room`
- `report_to: outpost_forest_watch:lookout`

## 14. Validari obligatorii

- `type` exista si este din lista canonica
- `entryNodes` nu sunt goale pentru structuri active
- `interactionNodes` exista unde este nevoie
- `questAnchors` sunt semantice si valide
- `dangerLevel` este compatibil cu tipul
- `spawnPolicy` este clar si controlat
- `requiresValidation=true` pentru generare automata

## 15. Criterii de acceptare

Schema este buna cand:

- un designer sau generator poate crea un sat/oras fara hardcode manual per structura
- questurile si story-ul pot folosi aceleasi ID-uri
- NPC-urile gasesc locuri semantice pentru rutina
- outpost si dungeon sunt separate clar de sat/oras
- validarea prinde lipsurile inainte de build sau commit

## 16. Maparea la codul existent

Exemplele YAML de mai sus trebuie sa ramana compatibile cu modelele deja expuse in cod.

### 16.1 Regiuni

`WorldRegionInfo` este containerul semantic de nivel mare pentru sat, oras sau zona exterioara.

Campuri importante:

- `id`
- `name`
- `worldName`
- `typeId`
- `minX/minY/minZ/maxX/maxY/maxZ`
- `tags`
- `storyMode`
- `storyStateKey`
- `storyPool`

Regula:

- un `village`, `city`, `outpost` sau `dungeon` trebuie sa poata fi descris ca regiune cu identitate stabila
- `typeId` trebuie sa ramana compatibil cu validarea, debug-ul si story context

### 16.2 Places

`WorldPlaceInfo` descrie locurile locale dintr-o regiune.

Campuri importante:

- `id`
- `regionId`
- `displayName`
- `worldName`
- `placeType`
- `minX/minY/minZ/maxX/maxY/maxZ`
- `tags`
- `ownerNpcId`
- `publicAccess`
- `metadata`

Regula:

- `placeType` trebuie sa reflecte functia reala: casa, lucru, utilitate, social, civic sau exterior
- `metadata` este pentru detalii de runtime, nu pentru coordonate brute de quest

### 16.3 Nodes

`WorldNodeInfo` reprezinta punctul fin de interactiune.

Campuri importante:

- `id`
- `regionId`
- `placeId`
- `typeId`
- `worldName`
- `x/y/z`
- `radius`
- `metadata`

Regula:

- questurile, story-ul si AI-ul trebuie sa foloseasca node-uri prin ID semantic
- node-urile nu trebuie inlocuite cu coordonate brute in fluxurile de gameplay

### 16.4 Structuri exterioare

`ExteriorStructureAnalyzer` si `ExteriorStructurePlanner` sunt deja sursele de adevar pentru planificarea structurilor exterioare.

`ExteriorStructurePlan` expune:

- `type`
- `regionId`
- `displayName`
- `regionTypeHint`
- `tags`
- `plannedPlaces`
- `plannedNodes`
- `warnings`

Regula:

- analyzer-ul ramane read-only
- planner-ul produce doar plan
- build-ul fizic sau commit-ul semantic se face separat

## 17. Ordinea recomandata de implementare

Cand extinzi schema sau adaugi tipuri noi, ordinea corecta este:

1. `type` si `category`
2. `regionId` si `parentPlace`
3. `placeType` si `nodeTypes`
4. `entryNodes` si `interactionNodes`
5. `tags`, `npcRoles`, `questAnchors`
6. `spawnPolicy` si `buildMode`
7. validare, audit si debugdump

Aceasta ordine reduce riscul de a crea structuri frumoase, dar fara utilitate in cod.

## Concluzie

Acest format YAML transforma structurile din obiecte izolate in piese ale unei lumi controlate semantic. Daca fiecare `village`, `city`, `outpost`, `dungeon` si `quest_structure` are schema proprie, dar aceeasi baza comuna, atunci generarea devine predictibila, flexibila si usor de extins fara hardcode excesiv.
