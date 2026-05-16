# Villager Generation cu WorldEdit si NPC-uri

Actualizat: 2026-04-28

## Scop

Acest document descrie cum trebuie proiectata generarea de sate cu `WorldEdit API`, cum se asociaza cladirile cu NPC-uri si cum se pastreaza mapping-ul semantic in `WorldAdmin`.

Starea actuala:

- `WorldAdmin` are deja `Region`, `Place` si `Node`
- NPC-urile primesc deja `homeAnchor` si `workAnchor`
- auditul si debug dump-ul sunt implementate
- integrarea WorldEdit nu este inca implementata
- generatorul complet de sate nu este inca implementat
- mapping-ul serverului testat poate fi inca gol: `0` regiuni, `0` places, `0` nodes

WorldEdit trebuie tratat ca executant de constructii, nu ca sistem care decide gameplay-ul.

## Principiul de baza

Generatorul trebuie impartit in straturi clare:

1. `VillagePlanner` decide marimea, populatia, tipurile de cladiri si layout-ul
2. `NpcAllocator` decide cine locuieste unde si cine munceste unde
3. `StructureBuilder` construieste sau importa cladirile
4. `WorldEditStructureBuilder` executa paste de schematics, rotatie si offset
5. `SemanticMapper` creeaza `WorldRegion`, `WorldPlace` si `WorldNode`
6. `NpcBinder` seteaza `homeAnchor`, `workAnchor` si proprietarii de places

Core-ul AINPC trebuie sa ramana responsabil pentru plan, validare si mapping.
WorldEdit trebuie sa ramana doar adapter optional.

## Pipeline recomandat

Pipeline-ul complet pentru un sat:

1. alege zona candidata dupa biom, panta, apa si spatiu liber
2. calculeaza centrul satului si drumurile principale
3. decide populatia tinta
4. calculeaza cate case sunt necesare
5. alege cladirile de meserii necesare
6. adauga cladiri auxiliare: piata, fantana, hambar, garduri, depozite
7. rezerva footprint-uri pentru fiecare structura
8. verifica drumurile si spatiile de acces
9. alege schematic-ul sau builderul nativ pentru fiecare structura
10. executa constructia prin WorldEdit sau fallback nativ
11. creeaza `WorldRegion` pentru sat
12. creeaza `WorldPlace` pentru fiecare cladire importanta
13. creeaza `WorldNode` pentru intrari, paturi, lucru, piata si interactiuni
14. asociaza NPC-uri cu case si locuri de munca
15. ruleaza `/ainpc audit world` si `/ainpc audit npc`

## Configurare recomandata

Exemplu conceptual:

```yml
village_generation:
  enabled: true
  builder: "worldedit"
  population:
    target: 12
    residents_per_house: 2
    max_residents_per_house: 3
  layout:
    road_width: 3
    min_building_spacing: 3
    center_radius: 8
    max_slope_delta: 3
  required_buildings:
    - market
    - well
    - farm
    - forge
    - tavern
  decorative:
    enabled: true
    density: 0.25
    types: [well, haystack, fence, cart, lamp, barrel]
```

Aceasta configurare nu exista inca in cod.
Este contractul recomandat pentru implementarea urmatoare.

## Case si locuitori

Numarul de case trebuie calculat din populatie:

```text
case_necesare = ceil(populatie_tinta / residents_per_house)
```

Exemple:

- 6 NPC-uri, 2 locuitori pe casa -> 3 case
- 12 NPC-uri, 2 locuitori pe casa -> 6 case
- 12 NPC-uri, 3 locuitori pe casa -> 4 case

Reguli recomandate:

- fiecare casa are cel putin un node `bed`
- casele comune au mai multe node-uri `bed`
- `owner_npc_id` ramane proprietarul principal
- locuitorii suplimentari pot fi pusi in metadata pana apare un camp dedicat

Exemplu de mapping:

```yml
places:
  casa_fierarului:
    name: "Casa Fierarului"
    type: "house"
    tags: [home, private]
    owner_npc_id: "npc_ion"
    metadata:
      role: "home"
      residents: "npc_ion,npc_madalina"
      max_residents: "2"
```

Limitare actuala:

- `WorldPlace` are un singur `ownerNpcId`
- NPC-ul salveaza `homeAnchor`, dar nu are inca `homePlaceId`
- pentru asociere completa multi-locatar trebuie adaugat ulterior un model dedicat

## Locuri de munca

Fiecare meserie importanta trebuie sa primeasca o cladire sau zona de munca.

Mapare recomandata:

- `fierar` -> `forge`
- `fermier` -> `farm`
- `negustor` -> `market` sau `shop`
- `hangiu` -> `tavern`
- `garda` -> `guard_post` ca `custom`, `camp` sau `castle_room`
- `preot` -> `shrine` ca `custom`
- `bibliotecar` -> `library` ca `custom`
- `pescar` -> `fisher_hut` ca `custom`
- `miner` -> `mine_camp` ca `camp`

Pentru fiecare loc de munca:

- `WorldPlace` primeste tag `workplace`
- metadata primeste `role: work`
- metadata primeste `profession`
- exista cel putin un `WorldNode` de tip `interaction`

Exemplu:

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
    nodes:
      forge_spot:
        type: "interaction"
        x: 144
        y: 65
        z: 149
        radius: 2.0
        metadata:
          role: "work"
```

## Cladiri auxiliare

Un sat coerent nu trebuie sa aiba doar case.

Cladiri si zone auxiliare utile:

- `market` pentru schimburi, zvonuri si questuri sociale
- `well` pentru centru vizual si punct de intalnire
- `barn` sau `storage` pentru ferme si aprovizionare
- `guard_post` pentru securitate si questuri de paza
- `tavern` pentru dialog, zvonuri si NPC-uri temporare
- `farm_plot` pentru hrana si rutine de fermier
- `road` pentru conectivitate si navigare vizuala

Piata trebuie tratata ca `WorldPlace` daca are gameplay.
Fantana poate fi `WorldPlace` sau doar `WorldNode`, in functie de utilitate.

Regula:

- daca jucatorul sau NPC-ul interactioneaza cu locul, creeaza mapping semantic
- daca este doar decor, poate ramane fara `WorldPlace`

## Decoratii si non-building

Elemente decorative posibile:

- fantana
- capita de fan
- gard
- butoaie
- caruta
- felinare
- banci
- copaci ornamentali
- sperietoare
- stiva de lemne

Clasificare recomandata:

- `decorative_only` = nu intra in WorldAdmin
- `decorative_interactable` = primeste `WorldNode`
- `minor_place` = primeste `WorldPlace` de tip `custom`

Exemple:

- capita de fan simpla -> decor fara mapping
- fantana folosita in quest -> `WorldPlace custom` cu tag `well`
- gard de delimitare -> decor fara mapping
- panou de anunturi -> `WorldNode interaction`

## Dimensiunea caselor si drumurile

Casele nu trebuie scalate arbitrar.
Este mai sigur sa alegi variante de template:

- `house_tiny`
- `house_small`
- `house_medium`
- `house_shared`
- `house_large`

Pentru contractul canonic de template metadata, ancore si marker nodes, vezi `template-cladiri-si-marker-nodes.md`.

Fiecare template trebuie sa declare:

- latime
- lungime
- inaltime
- offset pentru usa
- conectori de drum
- ancore pentru paturi
- ancore pentru interior
- numar maxim de locuitori

Exemplu metadata pentru template:

```yml
template: house_small_oak
size: { width: 7, length: 8, height: 6 }
door_offset: { x: 3, y: 1, z: 0 }
road_connectors:
  - { side: "south", x: 3, z: 0 }
anchors:
  bed_1: { x: 2, y: 1, z: 5 }
  bed_2: { x: 4, y: 1, z: 5 }
  inside: { x: 3, y: 1, z: 4 }
max_residents: 2
```

Reguli pentru potrivirea cu drumurile:

- calculeaza drumurile inainte de cladiri
- plaseaza intrarea casei catre cel mai apropiat drum
- pastreaza `min_building_spacing`
- lasa clearance langa usa
- nu roti schematic-ul daca rotatia rupe ancorele
- foloseste conectori de drum declarati in metadata

Cand spatiul este mic:

1. foloseste template mai mic
2. muta casa mai departe de centru
3. transforma casa in `house_shared`
4. reduce populatia tinta
5. esueaza controlat si raporteaza motivul

## Integrare WorldEdit

Integrarea trebuie sa fie optionala.

Modul recomandat:

- `ainpc-integration-worldedit`
- dependinta soft pe WorldEdit
- capabilitate declarata: `worldedit-execution`
- fallback la builder nativ pentru structuri mici

Contractul API si limitele adapterului sunt detaliate in `worldedit-integration-contract.md`.

Interfata recomandata:

```java
public interface StructureBuildService {
    BuildResult build(BuildContext context, StructurePlan plan);
    boolean supports(String builderType);
}
```

Adapterul WorldEdit ar trebui sa faca doar:

- incarcare schematic
- rotatie
- paste la origine calculata
- replace de materiale dupa paleta
- raportare bounds reale
- raportare anchors transformate
- optional rollback/undo daca integrarea suporta asta

Adapterul nu trebuie sa decida:

- cine locuieste in casa
- ce meserie are NPC-ul
- ce quest porneste in piata
- ce `WorldPlace` este privat sau public

## Asociere cu NPC-uri

Dupa constructie si mapping:

1. fiecare NPC primeste o casa disponibila
2. fiecare NPC primeste un loc de munca compatibil cu ocupatia
3. casa seteaza `owner_npc_id` pentru proprietarul principal
4. locul de munca seteaza `owner_npc_id` daca este privat
5. NPC-ul primeste `homeAnchor` din place-ul de casa
6. NPC-ul primeste `workAnchor` din place-ul de munca
7. profilul se salveaza in `npc_profiles.profile_data`

In starea actuala, asocierea este bazata pe ancore si `owner_npc_id`.
Urmatorul pas tehnic este adaugarea de ID-uri persistente explicite:

- `homePlaceId`
- `workPlaceId`
- `socialPlaceId`

## Debugging si testare

Comenzi utile dupa generare:

```text
/ainpc audit
/ainpc audit npc
/ainpc audit world
/ainpc debugdump all
/ainpc world whereami
/ainpc world places <regionId>
/ainpc world place info <placeId>
/ainpc info <npc>
```

Ce trebuie verificat:

- exista o regiune pentru sat
- fiecare cladire importanta are `WorldPlace`
- fiecare loc de munca are cel putin un node
- fiecare casa are tag `home`
- fiecare loc de munca are tag `workplace` sau metadata `role: work`
- fiecare NPC are `homeAnchor`
- fiecare NPC are `workAnchor`
- casele nu se suprapun
- intrarile sunt orientate spre drum
- decoratiile nu blocheaza drumurile
- restartul pastreaza mapping-ul

Probleme care trebuie raportate clar:

- teren invalid
- lipsa spatiu pentru numarul de case
- schematic lipsa
- WorldEdit indisponibil
- rotatie invalida pentru template
- ancore lipsa in metadata
- NPC fara casa disponibila
- meserie fara loc de munca compatibil

## MVP recomandat pentru implementare

Ordinea pragmatica:

1. defineste `StructurePlan`, `VillagePlan` si `BuildResult`
2. adauga metadata pentru template-uri
3. implementeaza builder nativ pentru o casa mica si o ferma
4. implementeaza `SemanticMapper` catre `WorldAdmin`
5. implementeaza `NpcAllocator`
6. adauga comanda de test `/ainpc village generate <small|medium|large>`
7. adauga adapter WorldEdit intr-un modul separat
8. adauga schematics pentru case, forge, market, tavern si well
9. adauga audit specializat pentru sate generate
10. adauga teste automate pentru plan, footprint si mapping

## Concluzie

Generarea buna de sate nu inseamna doar paste de cladiri.

Rezultatul corect trebuie sa produca simultan:

- constructii vizibile
- drumuri coerente
- case suficiente pentru populatie
- locuri de munca pentru meserii
- cladiri auxiliare pentru gameplay
- decoratii controlate
- `WorldRegion`, `WorldPlace` si `WorldNode`
- asocieri clare intre NPC, casa si loc de munca

WorldEdit este util pentru volum si template-uri.
AINPC trebuie sa controleze semantica, NPC-urile si gameplay-ul.
