# Structuri exterioare satului

Actualizat: 2026-07-02

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).

## Scop

Acest document defineste structurile care pot exista in afara satului principal: castel, padure, fantana izolata, casa izolata, mini-sat, sat de barbari, dungeon si alte zone de explorare, plus tipuri extinse inregistrate semantic.

Rolul lui este sa standardizeze cum sunt descrise aceste structuri in `Region`, `Place`, `Node`, `SettlementPlan`, quest anchors si generarea asistata. Documentul este design operational; nu inseamna ca toate tipurile sunt deja implementate complet si nu limiteaza schema la o lista inchisa de exemple.

## Status implementare

Implementat initial:

- model de baza `ExteriorStructureType` pentru structurile exterioare deja cunoscute;
- `barbarian_village` este suportat ca alias pentru tipul neutru `faction_settlement`, ca sa nu forteze lore ostil in core;
- catalog read-only `ExteriorStructureBlueprintCatalog` pentru tipurile de baza, alias-uri, tags, places, nodes si reguli minime;
- comanda `/ainpc world outside types` listeaza tipurile de structuri exterioare suportate;
- comanda `/ainpc world outside blueprint <type>` afiseaza scheletul semantic recomandat pentru mapping;
- comanda `/ainpc world outside plan <type> <baseId>` propune read-only ID-uri de regiune, places si nodes pentru mapping manual;
- `ExteriorStructureAnalyzer` valideaza read-only mapping-ul unei regiuni existente;
- comanda `/ainpc world outside report <regionId>` afiseaza tipul detectat, places, nodes si anchors;
- comanda `/ainpc world outside validate <regionId>` afiseaza erori si warning-uri fara sa scrie in lume, DB sau config;
- teste unitare pentru dungeon, casa izolata si aliasul de sat de barbari/factiune.

## Tipuri de baza si extensibilitate

Tipurile descrise aici sunt exemple de baza, nu o lista inchisa.

Regula dorita:

- tipurile noi trebuie sa poata fi adaugate fara hardcode dispersat
- tipurile trebuie sa fie indexabile semantic prin `type`, `tags`, `aliases`, `family` si `questAnchors`
- daca doua tipuri au acelasi rol functional, pot imparti aceeasi familie, dar trebuie sa aiba `typeId` stabil separat
- validarea trebuie sa poata lucra cu registry sau catalog, nu doar cu un enum fix

Exemple de extensii compatibile:

- `garden`
- `museum`
- `arena`
- `watch_post`
- `relay_station`
- `herbalist_hut`
- `faction_hall`
- `road_checkpoint`

Notare importanta:

- `castle`, `forest`, `hamlet`, `barbarian_village`, `dungeon` sunt puncte de plecare utile
- ele nu acopera toate formele posibile de lume
- catalogul poate creste cu alte tipuri de explorare, administratie, tranzit sau gameplay

## Nu este inca implementat:

- creare automata de structuri exterioare;
- builder nativ sau WorldEdit pentru aceste structuri;
- spawn automat NPC/mobi/loot;
- persistenta separata pentru `StructurePlan`; planul curent este doar preview text read-only;
- GUI dedicat pentru preview.

## Regula principala

Structurile exterioare nu trebuie tratate ca decor fara semantica.

Fluxul corect este:

```text
structura fizica sau planificata
-> regiune sau place semantic
-> nodes stabile
-> tags si metadata
-> validare
-> mapping
-> quest/story/NPC/runtime consuma ID-uri semantice
```

Reguli:

- coordonatele sunt pentru plasare si detectie, nu pentru logica de gameplay;
- fiecare structura importanta trebuie sa aiba ID semantic stabil;
- fiecare structura cu interactiune trebuie sa aiba cel putin un `WorldNode`;
- zonele periculoase trebuie sa aiba tag-uri clare de risc;
- generatorul produce planuri validate, nu construieste direct fara confirmare.

## Model semantic minim

### Region

Foloseste `Region` pentru zone mari care pot contine mai multe locuri:

- castel mare;
- padure;
- mini-sat;
- sat de barbari;
- dungeon mare;
- zona de ruine;
- munte, pestera sau mina extinsa;
- teritoriu de factiune.

Exemplu:

```yaml
id: fortareata_nordului
type: castle
tags: [fortified, noble, military, quest_hub]
dangerLevel: low
```

### Place

Foloseste `Place` pentru o cladire, curte, camera, campament sau punct semantic dintr-o regiune:

- sala tronului din castel;
- poarta castelului;
- luminis in padure;
- fantana veche;
- casa izolata;
- tabara de barbari;
- intrarea in dungeon;
- camera cu boss.

Exemplu:

```yaml
id: fortareata_nordului:poarta_principala
type: castle_gate
tags: [entrance, guarded, checkpoint]
```

### Node

Foloseste `Node` pentru puncte de interactiune precise:

- usa;
- cufar;
- fantana;
- altar;
- pat;
- workstation;
- panou de anunturi;
- spawn marker;
- quest trigger;
- boss arena center;
- exit point.

Exemplu:

```yaml
id: fortareata_nordului:poarta_principala:guard_post
type: guard_post
tags: [npc_anchor, patrol, dialogue]
```

## Tipuri canonice de structuri

| Tip | Nivel recomandat | Scop gameplay | Nodes minime | Tags recomandate |
|---|---|---|---|---|
| `castle` | `Region` | autoritate, aparare, quest hub, factiuni | `gate`, `throne`, `guard_post`, `barracks`, `storage` | `fortified`, `military`, `noble`, `guarded` |
| `forest` | `Region` | explorare, resurse, ambuscade, mister | `entrance`, `clearing`, `resource_node`, `trail_marker`, `danger_point` | `wilderness`, `resource`, `ambush`, `natural` |
| `fountain` | `Place` sau `Node` | reper, ritual, apa, social, quest clue | `water_source`, `offering_spot`, `inspect_point` | `landmark`, `water`, `ritual`, `social` |
| `isolated_house` | `Place` | NPC izolat, refugiu, quest, vendor rar | `door`, `bed`, `storage`, `hearth`, `npc_spawn` | `remote`, `home`, `shelter`, `quest` |
| `hamlet` | `Region` | mini-sat, economie mica, extensie populatie | `center`, `house`, `well`, `workplace`, `road_link` | `settlement`, `small`, `civilian` |
| `barbarian_village` | `Region` | factiune ostila/neutra, raid, diplomatie | `gate`, `chieftain_hut`, `campfire`, `training_ground`, `loot_storage` | `faction`, `tribal`, `hostile_optional`, `raid` |
| `dungeon` | `Region` sau `Place` | lupta, puzzle, loot, boss, risc | `entrance`, `checkpoint`, `loot`, `trap`, `boss_room`, `exit` | `danger`, `combat`, `loot`, `instanced_optional` |
| `camp` | `Place` | tabara temporara, trader, banditi, refugiati | `campfire`, `tent`, `npc_spawn`, `storage` | `temporary`, `camp`, `faction` |
| `ruins` | `Region` sau `Place` | lore, explorare, puzzle, resurse rare | `entrance`, `inscription`, `loot`, `hidden_room` | `ancient`, `lore`, `exploration` |
| `cave_or_mine` | `Region` | resurse, pericol, trecere subterana | `entrance`, `resource_node`, `danger_point`, `exit` | `underground`, `resource`, `danger` |
| `shrine` | `Place` | ritual, reputatie, story event, quest | `altar`, `offering_spot`, `inspect_point` | `ritual`, `sacred`, `story` |
| `watchtower` | `Place` | observatie, aparare, semnalizare | `ladder`, `lookout`, `signal_fire`, `guard_post` | `military`, `lookout`, `road_control` |

## Definitii pe structuri principale

### Castel

Un castel mare trebuie sa fie `Region`, nu o singura cladire plata.

Places recomandate:

- `main_gate`;
- `courtyard`;
- `throne_room`;
- `barracks`;
- `armory`;
- `kitchen`;
- `dungeon_cells`;
- `tower`;
- `market_or_court_area`.

NPC-uri potrivite:

- gardian;
- capitan;
- nobil;
- servitor;
- fierar militar;
- prizonier;
- curier.

Quest anchors utile:

- `request_audience`;
- `deliver_message`;
- `inspect_gate`;
- `free_prisoner`;
- `report_to_captain`;
- `defend_courtyard`.

Reguli:

- poarta trebuie sa fie node explicit;
- zonele publice si restrictionate trebuie separate prin tags;
- `dungeon_cells` din castel nu este automat acelasi lucru cu `dungeon` de explorare.

### Padure

O padure este `Region` cand are mai multe zone de gameplay. Poate fi `Place` doar daca este un mic luminis langa sat.

Places recomandate:

- `forest_edge`;
- `main_trail`;
- `clearing`;
- `hunter_camp`;
- `ancient_tree`;
- `wolf_den`;
- `hidden_shrine`;
- `resource_grove`.

NPC-uri potrivite:

- vanator;
- padurar;
- bandit;
- pustnic;
- spirit sau NPC episodic;
- comerciant ratacit.

Quest anchors utile:

- `collect_herbs`;
- `track_footprints`;
- `find_missing_npc`;
- `clear_ambush`;
- `inspect_ancient_tree`.

Reguli:

- drumurile si intrarile trebuie marcate ca nodes, altfel questurile devin confuze;
- resursele trebuie sa aiba `resource_node`, nu doar tag generic pe regiune;
- zonele periculoase trebuie separate de zonele sigure.

### Fantana

Fantana poate fi:

- `Node`, daca este in centrul unui sat sau mini-sat;
- `Place`, daca este izolata si are gameplay propriu;
- anchor ritualic, daca este legata de story sau reputatie.

Nodes recomandate:

- `water_source`;
- `offering_spot`;
- `inspect_point`;
- `meeting_point`.

Quest anchors utile:

- `draw_water`;
- `inspect_water`;
- `leave_offering`;
- `meet_npc_at_well`.

Reguli:

- o fantana sociala trebuie marcata ca loc de intalnire;
- o fantana magica trebuie sa aiba tags separate, de exemplu `ritual`, `cursed`, `healing`;
- fantana nu trebuie folosita ca regiune.

### Casa izolata

Casa izolata este `Place`, exceptand cazul in care devine nucleul unei regiuni mai mari.

Nodes recomandate:

- `door`;
- `bed`;
- `hearth`;
- `storage`;
- `npc_spawn`;
- `garden`;
- `workbench`.

NPC-uri potrivite:

- pustnic;
- vanator;
- vindecator;
- fermier izolat;
- fost soldat;
- NPC episodic.

Quest anchors utile:

- `knock_door`;
- `deliver_food`;
- `ask_for_shelter`;
- `inspect_storage`;
- `escort_owner_home`.

Reguli:

- daca are rezident permanent, trebuie sa poata fi legata ulterior de household/binding;
- daca este doar decor, nu trebuie sa primeasca NPC permanent;
- distanta fata de sat trebuie pastrata in metadata pentru story si rutine.

### Mini-sat

Mini-satul este `Region` de tip `hamlet`. Este o asezare mica, nu doar un grup decorativ de case.

Places minime:

- `center`;
- `well`;
- `house_*`;
- `workplace_*`;
- `road_link`;
- optional `small_market`.

NPC-uri potrivite:

- 2-8 locuitori initial;
- un lider local;
- 1-2 meseriasi;
- fermieri sau paznici simpli.

Quest anchors utile:

- `help_hamlet`;
- `trade_supplies`;
- `repair_well`;
- `escort_between_settlements`;
- `defend_hamlet`.

Reguli:

- mini-satul trebuie sa foloseasca aceleasi reguli de playability ca satul principal, dar la scara mai mica;
- trebuie sa aiba drum sau legatura semantica spre satul principal;
- nu spawna populatie daca nu exista case si nodes minime.

### Sat de barbari

Satul de barbari este `Region` de factiune. Poate fi ostil, neutru sau negociabil in functie de scenariu; tag-ul nu trebuie sa forteze ostilitate permanenta.

Places recomandate:

- `outer_gate`;
- `campfire_center`;
- `chieftain_hut`;
- `training_ground`;
- `trophy_area`;
- `loot_storage`;
- `prison_cage`;
- `watch_post`.

NPC-uri potrivite:

- capetenie;
- razboinic;
- cercetas;
- saman;
- prizonier;
- negustor neutru;
- copil sau civil doar daca scenariul cere explicit non-combat.

Quest anchors utile:

- `challenge_chieftain`;
- `negotiate_truce`;
- `rescue_prisoner`;
- `steal_supplies`;
- `defend_against_raid`;
- `trade_with_outcast`.

Reguli:

- foloseste tag `hostile_optional`, nu `always_hostile`, daca diplomatia este posibila;
- separa zona civila de zona de lupta;
- loot-ul trebuie pus in nodes explicite, nu generat implicit peste toata regiunea;
- raidurile trebuie declansate de quest/story/runtime, nu de simpla existenta a regiunii.

### Dungeon

Dungeon-ul poate fi `Region` daca are mai multe camere, traseu, boss sau loot. Poate fi `Place` daca este o singura camera sub un castel, pestera sau ruina.

Places recomandate:

- `entrance`;
- `first_room`;
- `puzzle_room`;
- `trap_corridor`;
- `checkpoint`;
- `loot_room`;
- `boss_room`;
- `exit`;
- optional `safe_room`.

Nodes minime:

- `entrance`;
- `exit`;
- `loot`;
- `danger_point`;
- `checkpoint`;
- `boss_spawn` daca exista boss.

Quest anchors utile:

- `enter_dungeon`;
- `clear_room`;
- `solve_puzzle`;
- `open_locked_door`;
- `defeat_boss`;
- `recover_artifact`;
- `return_to_surface`.

Reguli:

- dungeon-ul trebuie sa aiba `dangerLevel` explicit;
- intrarea si iesirea trebuie marcate separat;
- loot-ul si boss-ul trebuie sa fie nodes, nu doar tag-uri;
- daca dungeon-ul este refolosibil, metadata trebuie sa spuna daca este persistent, resetabil sau instanced optional;
- nu activa spawn agresiv automat doar pentru ca exista `type: dungeon`.

## Metadata recomandata

Pentru fiecare structura exterioara importanta:

```yaml
id: padurea_veche
type: forest
displayName: Padurea Veche
parentRegion: world_north
dangerLevel: medium
faction: none
tags: [wilderness, resource, ambush, story]
entryNodes:
  - padurea_veche:forest_edge:north_path
questAnchors:
  - collect_herbs
  - find_missing_npc
generation:
  buildMode: manual_or_template
  requiresValidation: true
```

Campuri utile:

| Camp | Rol |
|---|---|
| `type` | tip canonic folosit de validatoare si quest/story |
| `displayName` | nume pentru GUI, debug si dialog |
| `parentRegion` | legatura cu zona mare din lume |
| `dangerLevel` | `none`, `low`, `medium`, `high`, `boss` |
| `faction` | factiune dominanta sau `none` |
| `tags` | cautare semantica si filtre pentru AI |
| `entryNodes` | intrari valide pentru pathing si questuri |
| `questAnchors` | ancore candidate, nu quest progress |
| `generation.buildMode` | `manual`, `template`, `worldedit_optional`, `native_builder`, `scan_only` |
| `requiresValidation` | true pentru zone generate sau propuse automat |

## Reguli pentru generare

Generarea acestor structuri trebuie sa respecte acelasi contract ca generarea satelor:

```text
StructureDraft
-> validate terrain and overlaps
-> StructurePlan
-> planned Region/Place/Node
-> preview
-> confirm
-> build optional
-> semantic commit
-> audit/debugdump
```

Validari minime:

- structura nu se suprapune peste satul principal fara intentie explicita;
- intrarile sunt accesibile;
- fiecare place important are node-uri minime;
- pericolul este compatibil cu distanta fata de zone civile;
- exista fallback daca builder-ul sau WorldEdit lipseste;
- quest anchors sunt candidate, nu progres runtime;
- NPC spawn se face doar dupa mapping si validare.

## Integrare cu quest/story/AI

Questurile si story-ul trebuie sa consume structurile prin ID-uri semantice.

Exemple bune:

- `visit_region: padurea_veche`;
- `inspect_node: fantana_uitata:well:inspect_point`;
- `rescue_npc_from: tabara_barbari:prison_cage`;
- `defeat_boss_at: cripta_lupilor:boss_room:boss_spawn`;
- `deliver_message_to: fortareata_nordului:throne_room`.

Exemple de evitat:

- quest la coordonate brute fara nume semantic;
- "mergi in padure" fara `entryNode` sau `Place`;
- dungeon cu loot generic pe regiune, fara `loot_room` sau `loot` node;
- sat de barbari etichetat direct ostil fara scenariu/factiune;
- castel tratat ca o singura casa mare.

## Prioritate MVP

Ordinea recomandata pentru MVP:

1. Fantana izolata ca `Place` simplu cu `inspect_point`.
2. Casa izolata cu un NPC si nodes `door`, `bed`, `storage`.
3. Padure cu `entry`, `clearing`, `resource_node` si un quest simplu.
4. Mini-sat cu 2-3 case si drum spre satul principal.
5. Castel ca regiune read-only, fara gameplay complex initial.
6. Dungeon mic cu intrare, loot, boss room si exit.
7. Sat de barbari cu factiune si diplomatie/ostilitate controlata de quest.

Aceasta ordine produce valoare testabila fara sa blocheze proiectul in generare complexa.

Pentru testare controlata, foloseste un fixture separat descris in `mediu-test-controlat-sat-si-structuri-exterioare.md`. Acel fixture este cod de test temporar si nu trebuie sa ramana continut hardcodat in core.

## Ce trebuie evitat

- Nu crea structuri exterioare doar ca schematics fara metadata.
- Nu face WorldEdit obligatoriu pentru definirea semanticii.
- Nu spawna NPC-uri intr-o structura fara nodes home/work/social sau anchors clare.
- Nu lega questuri direct de coordonate brute.
- Nu trata toate zonele exterioare ca dungeon-uri.
- Nu transforma `dangerLevel` in spawn automat de mobi fara runtime controlat.
- Nu introduce factiuni ostile permanente fara optiune de story sau configurare.

## Definitia de gata

O structura exterioara este suficient definita cand:

- are `Region` sau `Place` cu ID stabil;
- are `type`, `displayName`, `tags` si `dangerLevel`;
- are intrari sau puncte de acces;
- are nodes pentru interactiunile importante;
- poate fi inspectata in debug/mapping;
- poate fi folosita de quest/story prin ID semantic;
- are reguli clare pentru NPC, loot, risc si generatie;
- poate fi validata inainte de build/spawn.

