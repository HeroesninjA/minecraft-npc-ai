# Generare de Sate Fara WorldEdit

Actualizat: 2026-05-11

Status curent:

- `VanillaVillageScanner` exista initial in cod
- `SemanticVillageMapper` exista initial in cod
- `VillageGapAnalyzer` exista initial in cod si produce `GapReport` read-only
- `VillagePatchPlanner` exista initial in cod si produce `PatchPlan` read-only
- comanda `/ainpc world scan village [radius]` face dry-run
- comanda `/ainpc world scan village [radius] import [regionId]` importa mapping semantic runtime
- comanda `/ainpc patch analyze|plan|validate <regionId> [targetPopulation] [profesiiCSV]` inspecteaza gap-uri si planuri fara scrieri
- `/ainpc world save` persista mapping-ul rezultat in `config.yml`
- `NativePatchBuilder`, commit-ul semantic-only si integrarea completa cu `SettlementPlan` raman de implementat

## Scop

Acest document descrie cum poate fi extinsa generarea de sate in faza curenta a proiectului, fara integrare cu `WorldEdit API`.

Directia corecta nu este sa inlocuim complet mecanica vanilla de sate. Directia corecta este sa o adaptam la nevoile AINPC:

- NPC-uri cu casa, familie si loc de munca clar
- cladiri mai bine proportionate
- distante mai bune intre locuinte
- mapping semantic `region/place/node`
- rutine zilnice si questuri care inteleg satul
- generare controlata, dar compatibila cu stilul Minecraft

Nu este pragmatic sa refacem de la zero sistemul de generare a satelor. Minecraft are deja ani de reguli pentru teren, biome, structuri, POI-uri, drumuri si compatibilitate cu worldgen-ul. AINPC trebuie sa foloseasca aceasta baza si sa adauge stratul semantic care lipseste pentru gameplay-ul nostru.

Problema curenta:

- sunt prea putine case generate
- sunt prea putine cladiri dedicate meseriilor
- NPC-urile primesc casa si loc de munca, dar lumea nu are inca suficiente structuri semantice pentru toate rolurile
- generarea trebuie sa ramana simpla, nativa si usor de controlat

Obiectivul nu este sa construim sate spectaculoase imediat.
Obiectivul este sa avem sate coerente, cu suficiente locuinte, locuri de munca si mapping semantic.

## Decizie pentru etapa curenta

Pentru moment, generarea de sate trebuie extinsa printr-un adaptor nativ peste logica de sat, nu prin `WorldEdit` si nu printr-un sistem care ignora complet vanilla.

Asta inseamna:

- folosim stilul vanilla ca baza vizuala si functionala
- scanam si mapam satele vanilla existente inainte sa construim ceva nou
- pastram ideea de centru, drumuri, case, locuri de munca si puncte sociale
- adaugam loturi, curti si distante minime ca sa nu iasa case prea mici sau inghesuite
- folosim cladiri parametrice doar unde vanilla nu ofera destula semantica
- generam mapping automat in `WorldAdmin`
- legam NPC-urile de case, familii si locuri de munca

`WorldEdit` ramane util pe termen lung, dar nu trebuie introdus acum ca dependinta obligatorie.
Cand devine necesar pentru template-uri mari, contractul separat este `worldedit-integration-contract.md`.

## Regula: adaptare vanilla, nu inlocuire

AINPC trebuie sa trateze satul vanilla ca baseline.

Ce pastram din vanilla:

- logica de sat cu centru recognoscibil
- drumuri simple si accesibile
- case si locuri de munca pe baza de POI
- clopot, fantana, piata sau punct central
- stil de materiale potrivit biomului
- simplitatea structurala Minecraft

Ce adauga AINPC:

- plan semantic pentru `WorldRegion`, `WorldPlace` si `WorldNode`
- loturi cu dimensiuni minime
- distante intre cladiri
- case potrivite pentru mai multi rezidenti
- ancore pentru usa, pat, masa, workstation si punct social
- legatura intre casa, familie, profesie si rutina
- validare dupa generare

Ce nu trebuie facut:

- nu stergem complet satul vanilla doar ca sa construim altul
- nu rescriem worldgen-ul Minecraft de la zero
- nu inlocuim sistemul vanilla de POI-uri daca il putem citi si adapta
- nu fortam cladiri custom peste orice teren
- nu micsoram casele sub limita doar ca sa incapa mai multe
- nu facem core-ul dependent de WorldEdit sau de alte pluginuri
- nu transformam satele in structuri perfecte, dar artificiale

Formula arhitecturala corecta:

```text
vanilla worldgen ramane baza
-> AINPC scaneaza
-> AINPC mapeaza semantic
-> AINPC detecteaza lipsuri
-> AINPC adauga patch-uri mici
-> AINPC leaga NPC-uri, familii, rutine si questuri
```

## Scanner, mapper si patcher

Componenta principala nu trebuie sa fie initial un generator complet de sate. Trebuie sa fie un sistem de adaptare a satelor vanilla.

Pipeline recomandat:

```text
VanillaVillageScanner
-> SemanticMapper
-> VillageGapAnalyzer
-> VillagePatchPlanner
-> NativePatchBuilder
-> NpcBinder
```

### VanillaVillageScanner

Rol:

- gaseste sate vanilla sau zone cu POI-uri relevante
- detecteaza clopotul, paturile, workstation-urile, casele si drumurile
- identifica centrul aproximativ al satului
- strange date brute fara sa modifice lumea

Status implementare initiala:

- detecteaza `BELL`
- detecteaza paturi vanilla
- detecteaza workstation-uri vanilla
- detecteaza usi vanilla
- detecteaza `FARMLAND`
- limiteaza raza de scanare ca sa evite operatii prea grele
- returneaza warning daca nu exista clopot sau semnale utile

Output conceptual:

```text
detected_village_id
center
radius
beds
workstations
doors
bell
paths
candidate_houses
candidate_workplaces
```

### SemanticMapper

Rol:

- transforma datele brute in `WorldRegion`, `WorldPlace` si `WorldNode`
- decide ce structura este casa, loc de munca, piata, fantana sau drum
- adauga tag-uri si metadata pentru rutine si questuri

Status implementare initiala:

- creeaza `WorldRegion` de tip `SETTLEMENT`
- creeaza case din clustere de paturi
- creeaza noduri `bed`, `home` si `entrance`
- creeaza noduri `meeting_point` din clopot sau fallback la centrul scanarii
- creeaza ferme cand exista destul farmland
- creeaza locuri de munca din workstation-uri standalone
- nu construieste blocuri si nu modifica lumea vanilla

Exemplu:

```text
bed + usa + interior mic
-> WorldPlace type=house
-> nodes: entrance, bed, inside

blast_furnace + anvil + shelter
-> WorldPlace type=forge
-> nodes: entrance, workstation, counter
```

### VillageGapAnalyzer

Rol:

- verifica ce lipseste pentru ca satul sa functioneze cu AINPC
- nu construieste nimic
- produce o lista de probleme si recomandari

Status implementare initiala:

- calculeaza capacitate din `max_residents`, apoi din node-uri `bed` sau `home`
- detecteaza lipsa de capacitate, workplace-uri pentru profesii cerute, social hub, quest trigger si node-uri de intrare/work
- este expus prin `/ainpc patch analyze`

Exemple de lipsuri:

```text
nu sunt destule paturi pentru populatia tinta
nu exista casa potrivita pentru familie
nu exista loc de munca pentru fierar
nu exista punct social central
casele sunt prea mici pentru 3 rezidenti
nu exista node-uri clare pentru rutine
```

### VillagePatchPlanner

Pentru contractul dedicat, vezi `patch-planner.md`.

Rol:

- decide patch-uri mici, nu reconstructie completa
- transforma gap-urile in candidati si planuri inspectabile
- marcheaza capabilitatile lipsa in loc sa execute modificari
- prefera patch-uri semantic-only cand sunt suficiente

Status implementare initiala:

- produce `PatchCandidate` si `PatchPlan` read-only prin `/ainpc patch plan|validate`
- valideaza doar capabilitati declarate; implicit este permisa doar `semantic-place-mapping`
- patch-urile care cer `native-block-build` raman propuneri blocate pana exista builder

Patch-uri permise:

```text
adauga o casa de familie
adauga o fierarie mica
adauga o fantana sau punct social
adauga garduri, alei sau decor functional
adauga node-uri semantice fara sa schimbe masiv cladirea
extinde usor o casa existenta daca are sens
```

Patch-uri care trebuie evitate:

```text
sterge jumatate de sat
inlocuieste toate casele vanilla
rearanjeaza drumurile complet
forteaza cladiri mari peste teren nepotrivit
```

### NativePatchBuilder

Rol:

- executa patch-urile planificate
- construieste doar structuri mici sau medii
- poate folosi palete vanilla pe biom
- actualizeaza mapping-ul dupa fiecare patch

### NpcBinder

Rol:

- leaga NPC-urile de case, paturi, familie si locuri de munca
- creeaza household-uri
- seteaza `homeAnchor` si `workAnchor`
- pregateste datele pentru `RoutineEngine`

Exemplu complet:

```text
Sat vanilla detectat:
- 5 case
- 8 paturi
- 3 workstation-uri
- 1 clopot
- nu exista fierarie
- nu exista casa potrivita pentru familie mare

AINPC:
- creeaza WorldRegion pentru sat
- transforma casele in WorldPlace
- transforma paturile, usile si workstation-urile in WorldNode
- adauga o fierarie mica pe un lot liber
- adauga o casa de familie daca populatia cere
- aloca 2-3 NPC per casa unde exista paturi
- creeaza household-uri
- leaga NPC-urile de rutine
```

## Principiul de baza

Satul generat trebuie tratat ca un plan semantic, nu ca o lista de blocuri.

Pipeline recomandat cand AINPC trebuie sa completeze sau sa genereze parti lipsa:

1. alege zona satului
2. scaneaza ce exista deja in vanilla
3. creeaza mapping semantic pentru ce exista
4. decide populatia tinta
5. compara populatia tinta cu paturile, casele si locurile de munca existente
6. decide ce cladiri sau node-uri lipsesc
7. gaseste loturi libere potrivite
8. plaseaza doar patch-urile necesare
9. creeaza sau actualizeaza `WorldRegion`
10. creeaza `WorldPlace` pentru fiecare cladire importanta
11. creeaza `WorldNode` pentru intrari, paturi, lucru si interactiuni
12. asociaza NPC-uri cu `homeAnchor` si `workAnchor`

## Proportii prin loturi

Problema caselor prea mici si prea apropiate trebuie rezolvata inainte de constructie, la nivel de plan.

Generatorul nu trebuie sa plaseze direct cladiri. Generatorul trebuie sa plaseze loturi.

Formula:

```text
lot_width = building_width + side_yard_left + side_yard_right
lot_depth = building_depth + front_yard + back_yard
```

Dimensiuni recomandate:

```text
casa mica:      7x7 pana la 9x9 blocks
casa medie:     9x11 pana la 11x13 blocks
casa familie:   11x13 pana la 13x15 blocks
atelier:        11x11 pana la 15x15 blocks
piata:          minim 17x17 blocks
drum principal: 5 blocks latime
drum secundar:  3 blocks latime
spatiu intre cladiri: minim 6 blocks
curte fata:     minim 4 blocks
curte spate:    minim 3 blocks
curte laterala: minim 3 blocks pe fiecare parte
```

Exemplu:

```text
building: casa 9x11
curte fata: 4
curte spate: 3
curte laterala: 3 + 3

lot final: 15x18
```

Regula importanta:

- daca lotul nu incape, nu micsora casa sub dimensiunea minima
- alege alt loc, mareste zona satului sau redu numarul de cladiri
- mai putine case corecte sunt mai bune decat multe case prea mici si lipite

Config conceptual:

```yaml
village_generation:
  vanilla_adaptation:
    enabled: true
    preserve_vanilla_style: true
    replace_existing_village: false

  roads:
    main_width: 5
    side_width: 3

  spacing:
    min_building_gap: 6
    min_lot_gap: 1
    min_front_yard: 4
    min_back_yard: 3
    min_side_yard: 3

  buildings:
    house_small:
      min_size: [7, 7]
      max_size: [9, 9]
      min_lot: [13, 14]
    house_family:
      min_size: [11, 13]
      max_size: [13, 15]
      min_lot: [17, 21]
    blacksmith:
      min_size: [11, 11]
      max_size: [15, 15]
      min_lot: [19, 19]
```

## Regula importanta pentru case

Numarul de case trebuie sa fie legat de populatia satului, nu pus manual la o valoare mica.

Formula simpla pentru MVP:

```text
case_necesare = ceil(populatie_tinta / 2)
```

Exemple:

- 6 NPC-uri -> 3 case
- 10 NPC-uri -> 5 case
- 12 NPC-uri -> 6 case

Daca vrei un sat mai aerisit:

```text
case_necesare = populatie_tinta
```

Pentru gameplay, varianta cu o casa la 1-2 NPC-uri este suficienta.

## Regula pentru cladiri de meserii

Fiecare meserie importanta trebuie sa aiba o cladire sau zona de lucru dedicata.

MVP recomandat:

- `fierar` -> `forge`
- `fermier` -> `farm`
- `hangiu` -> `tavern`
- `negustor` -> `market` sau `shop`
- `preot` -> `chapel` sau `shrine` mapat ca `custom`
- `bibliotecar` -> `library` mapat ca `custom`
- `garda` sau `soldat` -> `guard_post` mapat ca `camp` sau `castle_room`
- `pescar` -> `fisher_hut` mapat ca `custom`
- `pietrar` -> `mason_yard` mapat ca `custom`
- `tamplar` -> `fletcher_workshop` mapat ca `shop`
- `cartograf` -> `cartographer_house` mapat ca `shop`
- `miner` -> `mine_camp` mapat ca `camp`

Cand nu exista tip dedicat in `PlaceType`, se foloseste `CUSTOM`, dar cu tag-uri clare.

Exemplu:

```yml
type: "custom"
tags: [workplace, chapel, cleric]
metadata:
  role: "work"
  profession: "preot"
```

## Tipuri minime de cladiri

Pentru o generatie utila, biblioteca nativa de cladiri ar trebui sa inceapa cu aceste template-uri parametrice:

Pentru contractul de template metadata si marker nodes, vezi `template-cladiri-si-marker-nodes.md`.

- `house_small`
- `house_medium`
- `house_shared`
- `forge_small`
- `farm_plot`
- `barn_small`
- `tavern_small`
- `market_stall`
- `guard_post`
- `shrine_small`
- `library_small`
- `fisher_hut`
- `workshop_small`

Acestea nu trebuie sa fie perfecte vizual.
Trebuie sa fie:

- valide structural
- rapide de construit
- usor de mapat semantic
- suficiente pentru rutine NPC

## Builder nativ recomandat

Fiecare cladire trebuie sa fie definita ca un builder mic, cu dimensiuni si ancore.

Model conceptual:

```java
interface NativeStructureBuilder {
    BuildResult build(BuildContext context, StructurePlan plan);
}
```

Un `StructurePlan` trebuie sa contina:

- id template
- pozitie
- rotatie cardinala
- dimensiuni
- paleta de materiale
- tip place
- tag-uri
- ancore semantice

Exemplu conceptual:

```json
{
  "templateId": "forge_small",
  "placeId": "sat_nou:fierarie",
  "placeType": "forge",
  "tags": ["workplace", "blacksmith", "shop"],
  "anchors": {
    "entrance": { "x": 0, "y": 1, "z": 3 },
    "work": { "x": 5, "y": 1, "z": 4 },
    "owner_spawn": { "x": 4, "y": 1, "z": 4 }
  }
}
```

## Palete de materiale

Ca sa nu arate toate satele identic, builderul nativ trebuie sa foloseasca palete.

Exemple:

- `plains_medieval`: oak, cobblestone, spruce roof
- `forest_medieval`: spruce, mossy cobblestone, dark oak roof
- `mountain_mining`: stone bricks, spruce, deepslate accents
- `riverside`: oak, barrels, campfires, stripped logs

Paleta trebuie aleasa dupa biom si tema.

## Layout simplu pentru sat

Pentru MVP, nu e nevoie de urbanism complicat.

Layout recomandat:

1. centru cu `bell` sau `market`
2. drum principal nord-sud sau est-vest
3. loturi de case pe doua parti ale drumului
4. cladiri de meserii mai aproape de centru
5. ferme la margine
6. guard post la intrare
7. taverna langa piata

Regula practica:

- casele trebuie sa aiba cel putin 6 blocuri intre ziduri
- loturile pot fi separate de garduri, alei sau curti
- cladirile de meserii la 10-24 blocuri de centru
- fermele la 18-32 blocuri de centru
- drumul principal trebuie sa ramana navigabil pentru NPC-uri

## Alegerea cladirilor dupa populatie

Pentru un sat mic:

```text
populatie: 4-6
case: 3
cladiri dedicate: 1-2
obligatoriu: piata, cel putin o ferma
```

Pentru un sat mediu:

```text
populatie: 8-12
case: 5-7
cladiri dedicate: 3-5
obligatoriu: piata, ferma, fierarie sau taverna
```

Pentru un sat mare:

```text
populatie: 14-20
case: 8-12
cladiri dedicate: 6-9
obligatoriu: piata, taverna, fierarie, ferma, garda
```

## Alegerea meseriilor

Generatorul trebuie sa aleaga meserii in functie de marimea satului.

Meserii de baza pentru aproape orice sat:

- fermier
- negustor
- fierar

Meserii pentru sate medii:

- hangiu
- garda
- preot
- tamplar

Meserii pentru sate mari sau tematice:

- bibliotecar
- cartograf
- pietrar
- pescar
- miner
- vindecator

## Mapping obligatoriu dupa constructie

Fiecare cladire generata trebuie sa devina `WorldPlace`.

Exemplu pentru casa:

```yml
places:
  casa_ion:
    name: "Casa lui Ion"
    type: "house"
    tags: [home, private]
    owner_npc_id: "npc_ion"
    metadata:
      role: "home"
```

Exemplu pentru loc de munca:

```yml
places:
  fierarie:
    name: "Fierarie"
    type: "forge"
    tags: [workplace, blacksmith, shop]
    owner_npc_id: "npc_ion"
    metadata:
      role: "work"
      profession: "fierar"
```

Pentru fiecare cladire este recomandat sa existe cel putin:

- node `entrance`
- node `inside`
- node `work` pentru cladiri de meserii
- node `bed` pentru case

## Legatura cu NPC-uri

Dupa generare:

- un NPC primeste o casa
- un NPC primeste un loc de munca
- casa primeste `owner_npc_id`
- cladirea de munca primeste `owner_npc_id` daca are proprietar clar
- `homeAnchor` si `workAnchor` se seteaza din ancorele mapping-ului

Regula:

- casa este proprietate privata
- locul de munca poate fi privat sau public

Exemple:

- fierarul are `casa_fierarului` si `fierarie`
- hangiul poate avea `camera_hangiului` si `taverna`
- fermierul poate avea `casa_fermierului` si `ferma_nord`
- garda poate avea `baraca_garzilor` si `post_paza`

## Fallback cand nu exista suficient spatiu

Daca generatorul nu gaseste suficient teren:

1. cauta alt lot in aceeasi zona
2. extinde raza satului
3. foloseste `house_shared` fara sa scada sub dimensiunea minima
4. transforma unele locuri de munca in zone exterioare
5. reduce populatia tinta
6. abandoneaza generarea si raporteaza motivul

Nu trebuie fortata constructia peste teren invalid.

## Validare minima

Inainte sa plasezi o cladire, verifica:

- zona nu se suprapune cu alta cladire
- zona nu intersecteaza apa adanca sau lava
- diferenta de inaltime nu este prea mare
- exista acces catre drum sau centru
- structura intra in regiunea satului
- nu intra peste un `WorldPlace` existent

Praguri simple pentru MVP:

```text
max_slope_delta: 3 blocuri
min_distance_between_buildings: 6 blocuri
max_distance_from_center: 40 blocuri pentru sat mic
```

## Comanda pentru generare controlata

Observatie: aceasta sectiune este incompleta ca implementare. Ea descrie directia si contractul dorit pentru comenzi, dar nu inseamna ca `village plan` si `village generate` sunt deja implementate in cod.

Se poate adauga o mecanica de tip:

```text
/ainpc village plan <numar_case> [radius]
/ainpc village generate <numar_case> [radius]
/ainpc village generate custom <numar_case> [radius]
```

Exemplu:

```text
/ainpc village plan 25 96
/ainpc village generate 25 96
```

Aceasta comanda nu trebuie sa incerce sa apeleze generatorul intern vanilla ca sa produca exact 25 de case. Core-ul AINPC trebuie sa faca propriul plan peste lumea existenta:

```text
comanda admin
-> GeneratedSettlementPlan
-> analiza terenului din jurul jucatorului
-> alegere centru, drum principal si zona de piata
-> impartire in loturi
-> validare loturi
-> GeneratedBuildingPlan pentru fiecare casa
-> NativeStructureBuilder pentru case
-> WorldRegion / WorldPlace / WorldNode
-> audit spawn/world
-> /ainpc world save
```

Regula importanta:

```text
numar_case = tinta ceruta, nu promisiune fortata
```

Daca adminul cere 25 de case, generatorul trebuie sa incerce 25, dar sa refuze sau sa raporteze partial daca terenul nu permite.

Exemplu de raport dry-run:

```text
Village plan:
- case cerute: 25
- case posibile: 21
- loturi respinse: 4
- motive: panta prea mare, apa, suprapunere cu place existent
- recomandare: mareste raza la 128 sau alege alta zona
```

Exemplu de rezultat dupa generare:

```text
Village generated:
- regiune: sat_nou
- case create: 21 / 25
- locuri de munca create: 5
- nodes create: 74
- warning: 4 case nu au incaput in terenul valid
```

Pentru comanda reala, fluxul sigur trebuie sa fie in doua etape:

1. `plan` sau `dry-run` calculeaza ce se poate construi fara sa modifice lumea
2. `generate` executa planul validat si creeaza mapping semantic

Comanda `generate` trebuie sa aiba protectii:

- nu construieste peste apa adanca sau lava
- nu construieste peste `WorldPlace` existent
- nu micsoreaza casele sub dimensiunea minima
- nu ignora distanta minima intre cladiri
- nu construieste daca nu poate crea `WorldRegion`, `WorldPlace` si `WorldNode`
- nu spawneaza NPC-uri automat in prima versiune, decat daca exista un pas separat de binding

Arhitectura minima pentru aceasta comanda:

```text
VillageGenerationService
GeneratedSettlementPlan
GeneratedLotPlan
GeneratedBuildingPlan
VillageTerrainAnalyzer
VillageLotPlanner
NativeStructureBuilder
HouseSmallBuilder
HouseFamilyBuilder
VillageGenerationResult
```

Config conceptual:

```yaml
village_generation:
  max_requested_houses: 40
  default_radius: 96
  allow_partial_generation: true
  require_dry_run_before_generate: true
  rollback_on_failure: true
```

Aceasta mecanica foloseste stilul vanilla si blocuri vanilla, dar nu depinde de API-ul intern de worldgen vanilla. Vanilla ramane sursa de inspiratie si compatibilitate vizuala; AINPC ramane responsabil pentru plan, constructie, mapping si audit.

## Ce trebuie evitat

- sa se genereze doar case fara locuri de munca
- sa se genereze meserii fara cladiri asociate
- sa se puna toate cladirile la intamplare
- sa se inlocuiasca complet mecanica vanilla fara motiv
- sa se micsoreze casele sub minim doar ca sa incapa
- sa se ignore curtile, drumurile si distantele dintre loturi
- sa se creeze cladiri fara `WorldPlace`
- sa se creeze NPC-uri fara casa si loc de munca
- sa se puna prea multe cladiri mari intr-un sat mic
- sa se faca dependenta directa de WorldEdit in core

## MVP recomandat

Ordinea buna de implementare:

1. defineste `GeneratedSettlementPlan` / `SettlementPlan`; vezi `settlement-plan.md`
2. defineste `PatchPlan`; vezi `patch-planner.md`
3. defineste `GeneratedLotPlan`
4. defineste `GeneratedBuildingPlan`
4. adauga reguli de proportii si distante minime
5. adauga un catalog nativ de cladiri mici si medii in stil vanilla
6. implementeaza builder pentru `house_small`
7. implementeaza builder pentru `house_family`
8. implementeaza builder pentru `forge_small`
9. implementeaza builder pentru `farm_plot`
10. adauga selectie de meserii dupa populatie
11. adauga mapping automat `WorldRegion` / `WorldPlace` / `WorldNode`
12. asociaza NPC-urile cu case, familii si locuri de munca
13. adauga comanda admin `village plan`
14. adauga comanda admin `village generate`
15. adauga audit si rollback pentru generare partiala

Comanda de test poate fi ceva de forma:

```text
/ainpc village generate <small|medium|large>
/ainpc village plan <numar_case> [radius]
/ainpc village generate <numar_case> [radius]
```

## Extindere ulterioara

Dupa ce builderul nativ produce sate coerente, se poate adauga optional:

- modul separat `ainpc-integration-worldedit`
- suport pentru schematics
- template metadata mai bogat
- rotatii si variante avansate
- rollback pentru constructii mari
- preview administrativ inainte de generare

Important:

- planificarea satului ramane in core sau intr-un modul AINPC
- WorldEdit, daca apare, devine doar executor de template-uri

## Concluzie

Pentru etapa curenta, problema "prea putine case si cladiri de meserii" se rezolva cel mai bine prin:

- adaptarea mecanicii vanilla, nu inlocuirea ei completa
- reguli clare de populatie
- loturi cu dimensiuni minime
- distante reale intre cladiri
- catalog nativ de cladiri mici
- cladiri dedicate pentru meserii importante
- mapping semantic obligatoriu
- asociere directa intre NPC, casa si loc de munca

Nu este nevoie de WorldEdit API pentru acest MVP.
Este mai important ca satul generat sa fie coerent pentru NPC-uri si questuri decat sa fie foarte complex vizual.
