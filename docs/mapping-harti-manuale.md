# Mapping pentru harti construite manual

Actualizat: 2026-05-03

## Scop

Acest document descrie cum trebuie tratate hartile Minecraft construite manual, unde pluginul vede blocuri, dar nu poate sti sigur ce reprezinta fiecare constructie.

Ideea valida este:

- harta poate ramane construita manual
- pluginul nu deduce automat semantica doar din blocuri
- administratorul adauga peste harta un strat semantic
- NPC-urile, questurile si AI-ul consuma acel strat semantic, nu doar coordonate brute

Documentul foloseste doar concepte valide in sistemul curent: `Region`, `Place`, `Node`, `world_admin`, comenzi `/ainpc world ...`, audit si scanare vanilla initiala.

## De ce este nevoie de mapping manual

Minecraft si serverul vad blocuri:

- piatra
- lemn
- usi
- torte
- cufere
- scari
- ziduri
- paturi
- workstations

Dar blocurile nu spun semantic:

- aceasta cladire este casa fierarului
- aceasta camera este dungeon
- aceasta zona este intrarea in pestera
- aceasta constructie este fieraria
- aceasta regiune este regatul nordic
- acest punct este locul de predare pentru un quest

De aceea, pentru harti construite manual, mapping-ul trebuie introdus manual sau semi-automat si confirmat de om.

## Ce poate fi dedus automat

Automatizarea este valida doar ca asistenta, nu ca adevar final.

Pluginul poate propune candidati pe baza de indicii, de exemplu:

- multe paturi si usi pot indica o zona locuita
- workstation-uri pot indica locuri de munca
- cufere, intuneric si spatiu subteran pot indica o zona de explorare
- sate vanilla pot fi scanate si transformate initial in mapping semantic

Limitarea importanta:

- o cladire decorativa poate semana cu o casa
- o camera cu cufere poate fi depozit, nu dungeon
- o pestera amenajata poate fi mina, baza secreta sau intrare de quest
- un castel poate contine mai multe places, nu un singur loc semantic

Concluzie: detectia automata poate crea drafturi, dar adminul trebuie sa confirme tipul si scopul zonei.

## Modelul valid acum

Mapping-ul curent are trei niveluri:

- `Region` = zona mare, de exemplu sat, castel, dungeon, pestera, regat sau wilderness
- `Place` = loc semantic in interiorul unei regiuni, de exemplu casa, fierarie, taverna, camera, piata
- `Node` = punct exact de interactiune, de exemplu intrare, nicovala, pat, trigger de quest, punct de dialog

Exemplu corect:

- `Region`: `regatul_nordic`
- `Place`: `regatul_nordic:fieraria_george`
- `Node`: `regatul_nordic:fieraria_george:nicovala`

Regula simpla:

- `Region` spune in ce zona mare esti
- `Place` spune ce reprezinta constructia sau camera
- `Node` spune unde se intampla interactiunea exacta

## Tipuri valide

Tipuri de regiuni existente:

- `settlement`
- `castle`
- `dungeon`
- `cave`
- `wilderness`
- `custom`

Tipuri de places existente:

- `house`
- `shop`
- `forge`
- `tavern`
- `farm`
- `market`
- `castle_room`
- `cave_room`
- `camp`
- `custom`

Tipuri de nodes existente:

- `npc_spawn`
- `entrance`
- `bed`
- `workstation`
- `home`
- `work`
- `social`
- `meeting_point`
- `quest_trigger`
- `boss`
- `interaction`
- `progression`
- `custom`

Daca ai nevoie de ceva mai specific decat tipurile existente, foloseste tipul apropiat si completeaza prin `tags` si `metadata`.

Exemplu:

- o fierarie este `type: "forge"` cu tag-uri `blacksmith`, `workplace`, `shop`
- o cripta poate fi `type: "cave_room"` sau o regiune `type: "dungeon"` cu tag-uri `crypt`, `undead`, `danger`
- un regat intreg poate fi `type: "custom"` cu tag-uri `kingdom`, `north`, `medieval`

## Configurare valida in `config.yml`

Sursa curenta este `world_admin` din `config.yml`.

Exemplu:

```yml
world_admin:
  enabled: true
  auto_index:
    enabled: true
  regions:
    regatul_nordic:
      name: "Regatul Nordic"
      world: "world"
      type: "custom"
      min: { x: 80, y: 40, z: -220 }
      max: { x: 420, y: 120, z: 180 }
      tags: [kingdom, north, medieval]

      places:
        fieraria_george:
          name: "Fieraria lui George"
          type: "forge"
          min: { x: 125, y: 60, z: -45 }
          max: { x: 143, y: 76, z: -25 }
          tags: [workplace, blacksmith, shop]
          owner_npc_id: "george"
          public_access: false
          metadata:
            role: "work"
            profession: "blacksmith"
            quest_hint: "sabie_de_fier"

          nodes:
            nicovala:
              type: "workstation"
              x: 134
              y: 64
              z: -34
              radius: 2.0
              metadata:
                interaction: "forge"

        cripta_veche:
          name: "Cripta Veche"
          type: "cave_room"
          min: { x: -250, y: 20, z: 300 }
          max: { x: -180, y: 55, z: 390 }
          tags: [dungeon, crypt, danger, undead]
          public_access: true
          metadata:
            danger_level: "high"
            quest_anchor: "cripta_undead"

          nodes:
            intrare:
              type: "entrance"
              x: -216
              y: 43
              z: 302
              radius: 3.0
```

Observatii:

- `shape: CUBOID` nu este camp curent; regiunile si places sunt cuboid implicit prin `min` si `max`
- node-urile sunt puncte cu `x`, `y`, `z` si `radius`
- `owner_npc_id` trebuie tinut stabil si aliniat cu identificatorul NPC-ului folosit de server
- `metadata` poate pastra semantica suplimentara pana cand exista campuri dedicate

## Comenzi existente utile

Comenzile valide pentru creare si inspectie sunt:

```text
/ainpc world whereami
/ainpc world places [regionId]
/ainpc world region info <regionId>
/ainpc world place info <placeId>
/ainpc world region create <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>
/ainpc world place create <regionId> <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>
/ainpc world node create <regionId> <placeId|-> <id> <type> <x> <y> <z> [radius]
/ainpc world demo create [regionId]
/ainpc world bind npc <numeNpc|nearest> <homePlaceId> [workPlaceId|-] [socialPlaceId|-]
/ainpc world household <plan|spawn> <homePlaceId> [count]
/ainpc world settlement <plan|spawn> <regionId> [maxHouses]
/ainpc world save
/ainpc audit world
/ainpc debugdump world
```

Exemplu de lucru, executat din lumea in care marchezi harta:

```text
/ainpc world region create regatul_nordic custom 80 40 -220 420 120 180
/ainpc world place create regatul_nordic fieraria_george forge 125 60 -45 143 76 -25
/ainpc world node create regatul_nordic fieraria_george nicovala workstation 134 64 -34 2
/ainpc world save
/ainpc audit world
```

Pentru sate vanilla exista si scanare initiala:

```text
/ainpc world scan village 80
/ainpc world scan village 80 import satul_scanat
/ainpc world save
```

Prima comanda inspecteaza. Varianta cu `import` creeaza mapping runtime care trebuie verificat si salvat.

Pentru un demo rapid controlat, poti sta in centrul zonei dorite si rula:

```text
/ainpc world demo create demo_sat
/ainpc world settlement plan demo_sat
/ainpc world settlement spawn demo_sat
/ainpc audit world
/ainpc audit spawn
/ainpc world save
```

Aceasta comanda creeaza doar strat semantic pentru un sat mic. Nu construieste cladirile fizice, deci este potrivita pentru marcarea rapida a unei zone deja pregatite sau pentru testarea questurilor bazate pe `placeId` si `nodeId`.
Casele demo sunt marcate cu `metadata.owner_status: pending`, pentru ca owner-ul real se stabileste dupa spawn-ul NPC-urilor.
Bind-ul NPC-place seteaza `homeAnchor`, optional `workAnchor` si optional `socialAnchor` in profilul NPC-ului si marcheaza metadata inversa pe places.
Household plannerul poate genera un `HouseAllocation` dintr-o casa mapata si il poate executa controlat prin `NpcSpawnOrchestrator`. Daca NPC-ul exista deja, foloseste in locul spawn-ului manual `/ainpc world bind npc nearest demo_sat:house_1 demo_sat:fierarie demo_sat:piata`.
Settlement plannerul face acelasi lucru pentru toate casele dintr-o regiune. Pentru test gradual, foloseste limita `[maxHouses]`, de exemplu `/ainpc world settlement plan demo_sat 2`.

## Flux recomandat pentru harti manuale

1. Construieste harta manual in Minecraft.
2. Imparte lumea in regiuni mari: sat, castel, pestera, dungeon, regat, wilderness.
3. Marcheaza cladirile si camerele importante ca `places`.
4. Marcheaza punctele exacte de interactiune ca `nodes`.
5. Adauga `tags` si `metadata` pentru sensul real al locului.
6. Ruleaza `/ainpc world settlement plan <regionId>` pentru regiunea care trebuie populata automat.
7. Ruleaza `/ainpc world settlement spawn <regionId>` doar dupa ce planul arata corect.
8. Leaga manual NPC-urile existente prin `/ainpc world bind npc ...` sau prin `owner_npc_id` unde este cazul.
9. Ruleaza `/ainpc audit world` si `/ainpc audit spawn`.
10. Salveaza cu `/ainpc world save`.
11. Foloseste `placeId` si `nodeId` in questuri, story si AI.

Acest flux este mai sigur decat detectia automata 100%, deoarece omul confirma intentia creativa a hartii.

## Ce consuma NPC AI

Pentru NPC-uri si AI, informatia utila nu este lista de blocuri.

Informatia utila este:

- numele semantic al locului
- tipul locului
- regiunea din care face parte
- tag-urile
- owner-ul
- accesul public sau privat
- nivelul de pericol
- node-urile importante
- quest hooks sau story hooks

Exemplu:

```text
Loc: Fieraria lui George
Tip: forge
Regiune: Regatul Nordic
Owner: george
Tags: workplace, blacksmith, shop
Node important: nicovala
Quest hint: sabie_de_fier
```

Cu acest context, un NPC poate raspunde coerent despre unde este, ce face, ce quest poate oferi si de ce locul conteaza.

## Folosire in questuri

Questurile trebuie sa foloseasca tinte semantice cand pot.

Exemple bune:

- `visit_region` catre `regatul_nordic`
- `visit_place` catre `regatul_nordic:fieraria_george`
- `inspect_node` catre `regatul_nordic:fieraria_george:nicovala`
- quest de intrare in cripta legat de node-ul `intrare`

Exemple slabe:

- coordonate hardcodate fara nume semantic
- "mergi la X Y Z" fara context
- dungeon definit doar prin blocuri, fara `region`, `place` sau `node`

Coordonatele sunt bune pentru plasare si detectie. Gameplay-ul trebuie sa foloseasca ID-uri semantice stabile.

## Ce nu este implementat acum

Urmatoarele idei sunt valide ca directii viitoare, dar nu trebuie prezentate ca existente:

- comanda `/region wand`
- comenzi de forma `/ai loc create`, `/ai loc desc`, `/ai loc npc`
- editor 2D/3D in browser pentru selectarea zonelor
- fisier canonic `locations.yml`
- detectie automata completa pentru case, dungeon-uri, fierarii si regate
- asociere perfecta `place -> npc -> place` prin campuri persistente dedicate

Directii viitoare realiste:

- selectie cu doua colturi in joc
- creare place/node din pozitia curenta
- highlight vizual pentru regiuni si places
- editor extern de mapping
- export/import separat de `config.yml`
- persistenta in baza de date pentru mapping dinamic
- tabela dedicata pentru `npc_world_bindings`, peste bind-ul initial bazat pe anchors si metadata
- generator narativ complet pentru household-uri, peste plannerul determinist actual
- confirmare umana pentru zone propuse automat

## Regula finala

Pentru harti construite manual, sistemul corect este:

```text
harta construita manual
-> regiuni marcate de admin
-> places marcate de admin
-> nodes marcate de admin
-> tags si metadata
-> audit
-> salvare
-> NPC AI, questuri si story folosesc mapping-ul
```

Pluginul nu trebuie sa pretinda ca intelege singur lumea doar din blocuri.
El trebuie sa pastreze peste lumea Minecraft un strat semantic validat.
