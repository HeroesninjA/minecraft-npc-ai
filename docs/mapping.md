# Mapping

Actualizat: 2026-05-03

## Scop

Acest document descrie sistemul de mapping ca document canonic unic:

- ce exista implementat acum
- cum se configureaza `regions / places / nodes`
- cum trebuie consumat mapping-ul in NPC-uri, questuri, story, AI si generare
- ce reguli de design trebuie respectate in implementarile urmatoare
- ce imbunatatiri raman backlog

## Ce este acum mapping-ul

Mapping-ul este stratul semantic care descrie lumea in trei niveluri:

- `Region` = zona mare
- `Place` = loc semantic
- `Node` = punct exact de interactiune

Exemplu:

- `Region`: `satul_central`
- `Place`: `satul_central:fierarie`
- `Node`: `satul_central:fierarie:forge_spot`

Ideea de baza ramane:

- coordonatele spun unde este ceva
- mapping-ul spune ce este acel loc

Pentru harti construite manual, vezi si `docs/mapping-harti-manuale.md`.
Documentul acela explica de ce pluginul nu poate deduce sigur din blocuri ce este o casa, o fierarie, o cripta sau un regat si cum trebuie adaugat stratul semantic de catre admin.

## Ce exista implementat

### Stare observata pe server

Auditul rulat pe server la 2026-04-28 a raportat:

- `0` erori
- `14` warning-uri
- `31` NPC-uri incarcate
- `31` randuri in `npcs`
- `31` randuri in `npc_profiles`
- `0` regiuni, `0` places si `0` nodes in world mapping

Interpretare:

- persistenta NPC si profilurile sunt functionale
- world mapping-ul este activabil si implementat, dar nu este inca populat in serverul testat
- warning-urile observate sunt in principal nume duplicate normalizate, de exemplu mai multi NPC cu numele `ion`, `gabriel` sau `madalina`
- duplicatele existente nu sunt redenumite automat, dar generarea automata noua incearca sa evite nume duplicate

### Model intern

Exista deja:

- `WorldRegion`
- `WorldPlace`
- `WorldNode`

`WorldPlace` este nivelul intermediar dintre regiune si node si contine:

- `id`
- `regionId`
- `displayName`
- `worldName`
- `placeType`
- bounds
- `tags`
- `ownerNpcId`
- `publicAccess`
- `metadata`

### Tipuri publice

API-ul public expune modele read-only:

- `WorldRegionInfo`
- `WorldPlaceInfo`
- `WorldNodeInfo`
- `PlaceType`

### Service si API

Sistemul este incarcat si servit prin:

- `WorldAdminService`
- `WorldAdminApi`
- `MappingIndex` intern pentru lookup rapid dupa locatie

Capabilitati publice disponibile acum:

- listare regiuni
- listare places
- listare nodes
- `findRegion(...)`
- `findPlace(...)`
- `findNode(...)`
- `findNodesNear(...)`
- `findPlacesByTag(...)`
- citire count-uri pentru regiuni, places si nodes

### Indexare automata

Mapping-ul construieste automat un index intern pe chunk-uri pentru:

- `regions`
- `places`
- `nodes`

Indexul este folosit pentru cautarile dupa locatie:

- `findRegion(...)`
- `findPlace(...)`
- `findNode(...)`
- `findNodesNear(...)`

Pentru `nodes`, indexarea este folosita deja pentru lookup-uri de tip `findNode(...)`, `findNodesNear(...)` si obiective initiale `inspect_node`.

Indexarea este activata implicit.

Configurare:

```yml
world_admin:
  enabled: true
  auto_index:
    enabled: true
```

Dezactivare:

```yml
world_admin:
  auto_index:
    enabled: false
```

Cand `auto_index.enabled` este `false`, sistemul nu construieste indexul si revine la cautarea liniara peste listele de regiuni si places. Comportamentul functional ramane acelasi, doar performanta lookup-ului poate fi mai slaba pe harti mari.

### Configurare

Mapping-ul este definit sub:

- `world_admin.regions`
- `world_admin.regions.<regionId>.places`
- `world_admin.regions.<regionId>.nodes`
- `world_admin.regions.<regionId>.places.<placeId>.nodes`

Exemplu:

```yml
world_admin:
  enabled: true
  auto_index:
    enabled: true
  regions:
    satul_central:
      name: "Satul Central"
      world: "world"
      type: "settlement"
      min: { x: 100, y: 60, z: 100 }
      max: { x: 220, y: 90, z: 220 }
      tags: [village, public]

      places:
        fierarie:
          name: "Fierarie"
          type: "forge"
          min: { x: 140, y: 64, z: 145 }
          max: { x: 149, y: 73, z: 155 }
          tags: [workplace, blacksmith, shop]
          owner_npc_id: "npc_ion"
          public_access: false

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

### ID-uri calificate automat

`place` si `node` primesc automat prefixul de scope daca in config folosesti ID local.

Exemple:

- `fierarie` devine `satul_central:fierarie`
- `forge_spot` devine `satul_central:fierarie:forge_spot`

Asta evita coliziunile intre regiuni diferite.

### Comenzi admin existente

Sunt disponibile deja:

- `/ainpc world whereami [jucator]`
- `/ainpc world places [regionId]`
- `/ainpc world region info <regionId>`
- `/ainpc world place info <placeId>`
- `/ainpc world region create <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>`
- `/ainpc world place create <regionId> <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>`
- `/ainpc world node create <regionId> <placeId|-> <id> <type> <x> <y> <z> [radius]`
- `/ainpc world scan village [radius]`
- `/ainpc world scan village [radius] import [regionId]`
- `/ainpc world demo create [regionId]`
- `/ainpc world bind npc <numeNpc|nearest> <homePlaceId> [workPlaceId|-] [socialPlaceId|-]`
- `/ainpc world household <plan|spawn> <homePlaceId> [count]`
- `/ainpc world settlement <plan|spawn> <regionId> [maxHouses]`
- `/ainpc world save`
- `/ainpc audit [all|npc|world|db|spawn|quest]`
- `/ainpc debugdump [all|npc|world|quest|openai]`

### Demo mapping minim

Pentru Faza 1 exista o comanda care creeaza un mapping semantic demo in jurul pozitiei jucatorului:

```text
/ainpc world demo create [regionId]
```

Comanda creeaza:

- o regiune de tip `settlement`
- `4` case
- `1` piata
- `1` fierarie
- `1` ferma
- `1` taverna
- node-uri pentru `bed`, `home`, `entrance`, `npc_spawn`, `work`, `workstation`, `social`, `meeting_point`, `quest_trigger` si `interaction`

Important:

- comanda marcheaza semantic zona, nu construieste blocuri fizice
- daca nu dai `regionId`, se foloseste `demo_sat`
- casele demo primesc `metadata.owner_status: pending`, deoarece owner-ul real se leaga abia in faza de spawn/NPC
- dupa creare ruleaza `/ainpc audit world`
- dupa ce alegi sau creezi NPC-urile, ruleaza bind-ul NPC-place
- daca auditul este acceptabil, ruleaza `/ainpc world save`

### Bind NPC la mapping

Pentru faza urmatoare exista acum comanda:

```text
/ainpc world bind npc <numeNpc|nearest> <homePlaceId> [workPlaceId|-] [socialPlaceId|-]
```

Ce face:

- rezolva NPC-ul incarcat dupa nume, ID, UUID sau `nearest`
- rezolva `homePlaceId`, optional `workPlaceId` si optional `socialPlaceId`
- alege automat cel mai bun node semantic din place pentru ancora respectiva
- salveaza `homeAnchor`, `workAnchor` si `socialAnchor` in `npc_profiles.profile_data`
- salveaza `home_place_id`, `work_place_id`, `social_place_id` si node-urile aferente in `npc_world_bindings`
- marcheaza home place-ul cu `owner_npc_id`, `metadata.owner_status: assigned`, `resident_npc_ids` si `resident_names`
- marcheaza work place-ul cu `worker_npc_ids` si social place-ul cu `social_npc_ids`, daca sunt date

Exemplu pentru mapping-ul demo:

```text
/ainpc world demo create demo_sat
/ainpc world bind npc nearest demo_sat:house_1 demo_sat:fierarie demo_sat:piata
/ainpc audit spawn
/ainpc world save
```

Aceasta este legarea initiala practica intre mapping si NPC-uri. Tabela `npc_world_bindings` pastreaza ID-urile stabile de place/node, iar `profile_data.owned_locations` ramane cache/fallback pentru coordonate si rutina.

### Household plan din mapping

Pentru faza urmatoare exista acum planner de household peste mapping:

```text
/ainpc world household plan <homePlaceId> [count]
/ainpc world household spawn <homePlaceId> [count]
```

Ce face:

- porneste de la o casa mapata ca `house/home`
- citeste capacitatea din `metadata.max_residents`, `maxResidents` sau `capacity`
- alege node-uri `npc_spawn` si `bed/home` din casa
- alege un workplace din aceeasi regiune, daca exista
- alege un loc social din aceeasi regiune, daca exista
- produce un `HouseAllocation` validabil prin `HouseAllocationValidator`
- `plan` ruleaza dry-run prin `NpcSpawnOrchestrator`
- `spawn` creeaza NPC-urile si apoi actualizeaza metadata mapping prin bind home/work/social

Exemplu sigur:

```text
/ainpc world demo create demo_sat
/ainpc world household plan demo_sat:house_1
/ainpc world household spawn demo_sat:house_1
/ainpc audit spawn
/ainpc world save
```

Pentru demo-ul curent, o casa cu `max_residents: 2` poate genera doar rezidentii pentru care exista perechi suficiente de `npc_spawn` si `bed/home`. Plannerul reduce count-ul si emite warning in loc sa produca un plan invalid.

### Settlement plan din mapping

Pentru popularea unei regiuni intregi exista acum:

```text
/ainpc world settlement plan <regionId> [maxHouses]
/ainpc world settlement spawn <regionId> [maxHouses]
```

Ce face:

- cauta toate casele/home places din regiune
- genereaza cate un `HouseAllocation` valid pentru fiecare casa
- ruleaza dry-run pentru toate household-urile cu `plan`
- ruleaza spawn secvential pentru household-uri cu `spawn`
- opreste executia la prima eroare
- face rollback global pentru household-urile create anterior daca o casa ulterioara esueaza
- actualizeaza metadata mapping doar dupa ce toate household-urile au reusit

Exemplu:

```text
/ainpc world demo create demo_sat
/ainpc world settlement plan demo_sat
/ainpc world settlement spawn demo_sat
/ainpc audit spawn
/ainpc world save
```

Pentru testare graduala:

```text
/ainpc world settlement plan demo_sat 2
/ainpc world settlement spawn demo_sat 2
```

## Fazele urmatoare pentru mapping si spawn

Prioritatea curenta nu mai este alegerea intre mapping si questuri. Mapping-ul MVP are acum comenzi initiale pentru demo, bind, household planner si settlement planner. Urmatorul pas este validarea lui pe server Paper, apoi questurile trebuie construite peste mapping-ul validat.

### Faza M1: Smoke test Paper

Flux de verificat:

```text
/ainpc world demo create demo_sat
/ainpc world settlement plan demo_sat
/ainpc world settlement spawn demo_sat
/ainpc audit world
/ainpc audit spawn
/ainpc world save
```

Dupa reload/restart se verifica din nou:

```text
/ainpc audit world
/ainpc audit spawn
/ainpc world places demo_sat
```

Conditie de iesire:

- mapping-ul ramane incarcat dupa reload
- NPC-urile spawnate raman legate de case, work/social places si familie
- auditul nu raporteaza erori de spawn order sau places lipsa

### Faza M2: Persistenta dedicata NPC-place

Implementat initial:

- tabela `npc_world_bindings`
- legaturi explicite `homePlaceId`, `workPlaceId`, `socialPlaceId`
- node IDs pentru home/work/social cand planul sau mapping-ul le poate rezolva
- scriere din `world bind`, `household spawn` si `settlement spawn`
- backfill initial din ancorele existente, cand coordonatele cad intr-un mapped place
- audit DB pentru randuri orfane, place-uri si node-uri invalide

Ramas de maturizat:

- comanda read-only dedicata pentru inspectie bindings
- migration/backfill explicit pentru servere vechi
- audit dedicat `worldbindings`

### Faza M3: Generator narativ de populatie

De facut:

- generare de nume, roluri, profesii si relatii dupa mapping
- reguli pentru mai multi rezidenti per casa
- distributie coerenta intre case, locuri de munca si puncte sociale
- integrare cu story local fara ca AI-ul sa scrie direct in DB

### Faza M4: Hardening spawn pe regiune

De facut:

- tranzactie DB completa sau compensare documentata peste spawn, profil, familie si metadata
- debugdump pentru plan, reusite partiale si rollback
- test automat pentru esec la household intermediar
- mesaje admin clare cand un settlement spawn este refuzat sau anulat

### Faza M5: Quest slice peste mapping

De facut dupa smoke test:

- 3-5 questuri medievale care folosesc `visit_place` si `inspect_node`
- quest anchors persistente verificate in audit/debugdump
- story state/events scrise la completare
- test manual cap-coada dupa reload

## Ce valideaza sistemul acum

La creare runtime exista deja validare pentru:

- ID-uri duplicate
- regiuni suprapuse
- `place` in afara regiunii
- `node` in afara regiunii sau a `place`-ului
- lume invalida sau nealiniata cu containerul

## Ce foloseste deja mapping-ul

In acest moment, mapping-ul este folosit in principal pentru:

- inspectie administrativa
- cautare semantica de `region` si `place`
- cautare semantica de `node` curent si node-uri apropiate
- baza pentru obiectivele existente de tip `visit_region`
- baza pentru obiectivele initiale `visit_place` si `inspect_node`
- `QuestAnchorResolver` initial pentru validarea si bind-uirea ancorelor in `quest_anchor_bindings`
- rezolvarea automata a unor ancore NPC (`homeAnchor`, `workAnchor`) din `WorldPlace`
- rezolvarea initiala a `socialAnchor` din noduri sau locuri sociale
- bind manual initial prin `/ainpc world bind npc ...` pentru ancore NPC si metadata home/work/social
- generare initiala `HouseAllocation` dintr-o casa mapata prin `/ainpc world household plan ...`
- spawn household initial prin `/ainpc world household spawn ...`
- generare si spawn initial pe regiune prin `/ainpc world settlement plan/spawn ...`
- `WorldContextSnapshot` in `NPCContext`, inclusiv `WORLD_CONTEXT` pentru promptul AI
- rutina zilnica initiala a NPC-urilor
- import semantic initial peste sate vanilla scanate
- audit si debugging pentru inconsistenta intre NPC-uri, places si nodes
- fundatie pentru viitoarele integrari mai complete cu questuri, scenarii si generare

Pe scurt: infrastructura este prezenta si este consumata partial de NPC-uri, dar integrarea completa in gameplay nu este inca terminata.

## Integrare actuala cu NPC-uri

NPC-urile au deja ancore runtime/persistente:

- `homeAnchor`
- `workAnchor`
- `socialAnchor`

Pentru NPC-urile create sau profilate automat, sistemul incearca sa completeze casa si locul de munca in aceasta ordine:

1. cauta `WorldPlace` semantic in `WorldAdmin`
2. cauta blocuri fizice apropiate, precum pat sau workstation
3. foloseste fallback la locatia curenta a NPC-ului

Pentru case, mapping-ul recunoaste:

- `PlaceType.HOUSE`
- tag `home`
- tag `house`
- metadata `role: home`
- metadata `purpose: home`

Pentru locuri de munca, mapping-ul recunoaste:

- tag `work`
- tag `workplace`
- tag `job`
- metadata `role: work`
- metadata `purpose: work`
- tipuri precum `forge`, `shop`, `farm`, `market`, `tavern`

Important:

- NPC-ul salveaza acum coordonata si label-ul ancorei in `npc_profiles.profile_data`
- `npc_world_bindings` salveaza acum ID-urile stabile `home_place_id`, `work_place_id`, `social_place_id` si node IDs
- `WorldPlace` poate avea `owner_npc_id`
- comanda `/ainpc world bind npc ...` seteaza initial ancorele NPC si metadata inversa pe place
- asocierea inversa pe `WorldPlace` ramane in metadata pentru compatibilitate si inspectie admin

## Ce nu este inca legat complet

Sistemul de mapping exista, dar urmatoarele piese nu sunt inca conectate complet:

- `npc_world_bindings` exista initial, dar rutina si toate auditurile nu se bazeaza inca exclusiv pe aceasta tabela
- household plannerul este determinist si minim; nu este inca generator narativ complet de populatie
- settlement spawn are rollback global practic pentru NPC-urile create anterior, dar nu este inca tranzactie DB completa peste mapping si familie
- `NPCContext` expune context semantic initial prin `WorldContextSnapshot`, dar nu consuma inca direct `npc_world_bindings`
- questurile au `visit_place`, `inspect_node`, `QuestAnchorResolver`, persistenta initiala in `quest_anchor_bindings` si audit/comanda admin read-only pentru binding-uri
- `places` nu au stare dinamica de tip `open`, `closed`, `under_attack`
- persistenta este in `config.yml`, nu in baza de date

## Audit si debug pentru mapping

Exista audit read-only prin:

```text
/ainpc audit
/ainpc audit world
```

Auditul verifica:

- world admin activ dar fara regiuni
- regiuni, places si nodes inexistente sau inconsistente
- readiness minim pentru gameplay: case, locuri de munca, locuri sociale si quest/interaction nodes
- `place` in afara regiunii
- `node` in afara containerului
- case fara `owner_npc_id`
- locuri de munca fara nodes de interactiune
- owner NPC care nu se potriveste cu NPC-uri incarcate
- places suprapuse
- spawn order pentru case, rezidenti, ancore NPC si relatii familiale prin `/ainpc audit spawn`

Exista si dump avansat prin:

```text
/ainpc debugdump world
/ainpc debugdump all
```

Acesta exporta:

- `world-mapping.json`
- `audit.txt`
- `config-sanitized.yml`
- `recent-server-log.txt`
- sumar despre NPC-uri si contextul serverului

## Starea observata daca mapping-ul este gol

Daca auditul raporteaza:

```text
World mapping: 0 regiuni, 0 places, 0 nodes.
```

atunci pluginul poate functiona in continuare, dar pierde contextul semantic.

Efecte:

- NPC-urile folosesc fallback-uri pentru casa si loc de munca
- `/ainpc world whereami` nu poate explica locatia semantic
- questurile `visit_place` si `inspect_node` nu pot progresa fara places/nodes reale
- generarea viitoare nu are inca un strat persistent de continut semantic

Acesta nu este un crash si nu este coruptie de date.
Este o lipsa de continut world admin.

## Rolul actual in arhitectura

In forma actuala, mapping-ul este fundatia pentru:

- world admin semantic
- questuri bazate pe locatii cu sens
- rutine si dialog contextual pentru NPC-uri
- generare de structuri care trebuie transformate in continut utilizabil

Fara acest strat, restul sistemului poate lucra doar cu coordonate brute.

## Reguli de folosire in implementari viitoare

### Coordonatele nu sunt contract de gameplay

Orice sistem care are nevoie de sensul unui loc nu trebuie sa lucreze direct cu coordonate brute.

Sistemele de gameplay trebuie sa consume:

- `regionId`
- `placeId`
- `nodeId`
- `placeType`
- `tags`
- `metadata`

Coordonatele raman utile pentru:

- detectie
- validare
- plasare fizica
- navigatie

Semantica trebuie sa conduca gameplay-ul.

### Fluxul corect de consum

Fluxul recomandat este:

1. un sistem pleaca de la locatie, NPC, eveniment, quest sau plan de generatie
2. `WorldAdminService` rezolva `region`, `place` si `node`
3. contextul semantic este injectat in runtime
4. logica reactioneaza pe baza de `id`, `type`, `tags`, `metadata`
5. rezultatul este persistat sau transmis mai departe

Pe scurt:

- coordonatele spun unde este ceva
- mapping-ul spune ce este acel loc
- gameplay-ul decide ce inseamna acel loc

### `Region` nu trebuie folosit ca `Place`

Regiunea descrie aria mare.
Cladirea, camera sau zona functionala trebuie modelata ca `place`.

Exemplu slab:

- tot satul este folosit ca destinatie pentru o interactiune precisa

Exemplu bun:

- satul este `region`
- fieraria este `place`
- nicovala este `node`

### `Node` nu trebuie sa inlocuiasca `Place`

Node-ul este punct exact, nu spatiu semantic.

Daca un sistem vrea sa stie:

- unde locuieste un NPC
- unde munceste
- unde se afla o scena

atunci tinta buna este `place`, nu doar `node`.

### ID-urile trebuie sa fie stabile

ID-urile sunt identificatori de gameplay.

Reguli:

- nu redenumi ID-uri fara migrare
- nu folosi texte afisate drept identificator intern
- generarea trebuie sa produca namespace-uri coerente
- ID-urile locale trebuie lasate sa fie calificate automat de loader unde este cazul

### Tag-urile trebuie sa fie semantice

Tag-uri bune:

- `home`
- `workplace`
- `shop`
- `public`
- `restricted`
- `danger`
- `ritual`

Tag-uri slabe:

- `cool`
- `important`
- `misc`

### Codul trebuie sa foloseasca API/service, nu YAML direct

Codul de gameplay nu trebuie sa parseze direct `config.yml`.

El trebuie sa consume:

- `WorldAdminApi`
- `WorldAdminService`
- `WorldRegionInfo`
- `WorldPlaceInfo`
- `WorldNodeInfo`

## Integrare viitoare pe subsisteme

### NPC-uri si rutine

Mapping-ul trebuie sa devina sursa principala pentru rutina sociala si profesionala a NPC-urilor.

Exemple:

- un NPC merge la `homePlaceId` seara
- un NPC lucreaza la `workPlaceId` ziua
- un NPC socializeaza intr-un `socialPlaceId`
- un NPC foloseste `nodes` diferite in acelasi `place`

Exemple concrete:

- `homePlaceId = satul_central:casa_fierarului`
- `workPlaceId = satul_central:fierarie`
- `socialPlaceId = satul_central:taverna`

Ce trebuie facut in implementari viitoare:

- `AINPC` sa tina ID-urile semantice persistente
- `NPCContext` sa stie locul semantic curent
- planner-ul de rutina sa aleaga destinatii prin `placeId`, nu prin coordonate hardcodate
- fallback-ul la coordonate sa ramana doar pentru cazurile fara mapping

### Dialog si raspuns AI

Dialogul trebuie sa consume context semantic scurt, validat si limitat.

In loc de:

- `NPC-ul este la x=144 y=65 z=149`

AI-ul si dialogul trebuie sa stie:

- este in `fierarie`
- locul are tip `forge`
- tag-urile sunt `workplace`, `blacksmith`, `shop`
- owner-ul este un anumit NPC
- accesul este public sau privat

Efecte:

- replici contextuale
- ton diferit in loc public vs privat
- raspunsuri diferite in functie de program si tipul locului
- motivatie mai buna pentru scenarii si evenimente

Regula: promptul AI primeste sumar semantic, nu dump complet al mapping-ului.

### Questuri

Mapping-ul este critic pentru questuri bazate pe locuri reale.

Questurile nu trebuie sa tina doar `target coordinates`.
Trebuie sa tina tinta semantica.

Tipuri de obiective potrivite:

- `visit_region`
- `visit_place`
- `inspect_node`
- `talk_to_npc_at_place`
- `bring_item_to_place`
- `kill_mob_in_place`
- `escort_npc_to_place`

Exemple:

- mergi la fierarie
- vorbeste cu fierarul la tejghea
- inspecteaza intrarea in pestera
- du minereul in camera de topire

Starea actuala:

- `visit_region`, `visit_place`, `inspect_node` exista initial
- `QuestAnchorResolver` poate valida si bind-ui ancore semantice
- `quest_anchor_bindings` persista initial ancorele de quest

Urmatorul pas este ca `/quest track` si mesajele de obiectiv sa foloseasca aceste ancore pentru indicii clare.

### Generare de sate, castele, pesteri si structuri

Mapping-ul este puntea dintre constructie si gameplay.

Fara mapping, o structura generata este doar blocuri.
Cu mapping, structura devine continut utilizabil.

Pipeline recomandat dupa generare:

1. se construieste structura
2. se creeaza `region`
3. se creeaza `places`
4. se creeaza `nodes`
5. se asociaza NPC-uri si roluri
6. se adauga quest hooks si story hooks
7. se ruleaza auditul
8. se salveaza mapping-ul

Exemplu pentru un sat generat:

- `region = satul_nou`
- `place = satul_nou:piata`
- `place = satul_nou:fierarie`
- `place = satul_nou:taverna`
- `node = satul_nou:fierarie:forge_spot`
- `node = satul_nou:piata:announcement_board`

Abia dupa acest pas generarea devine utila pentru AI si questuri.

### Scenarii narative

Scenariile viitoare nu trebuie sa porneasca doar din evenimente abstracte.

Ele pot porni din:

- un loc
- o combinatie de tag-uri
- un `node`
- o stare a unui `place`

Exemple:

- furt in `market`
- incendiu in `forge`
- conflict in `tavern`
- aparitie episodica la `castle_gate`

Ce inseamna asta:

- mapping-ul devine sursa de trigger
- mapping-ul devine selectie de context
- mapping-ul devine filtru de validare pentru scenarii

Pentru designul complet al stratului `mapping -> indexare -> quest -> story`, vezi `story-si-context-ai.md`.

### Admin si debugging

Mapping-ul trebuie sa ramana baza pentru operare si debug, nu doar runtime.

Directii utile:

- inspectie rapida a locului curent
- listare de `places` si `nodes`
- verificare de suprapuneri
- verificare de ownership
- validare de `tags`
- debug pentru quest anchors si NPC world bindings

Comenzile existente sunt deja punctul de plecare:

- `/ainpc world whereami`
- `/ainpc world places`
- `/ainpc world region info ...`
- `/ainpc world place info ...`
- `/ainpc world node create ...`
- `/ainpc world save`
- `/ainpc audit world`
- `/ainpc debugdump world`

## Exemple de fluxuri viitoare

### NPC merge la munca

1. planner-ul verifica ora si rutina
2. gaseste `workPlaceId`
3. cere `place` din world admin
4. cauta un `node` cu rol de lucru
5. muta NPC-ul spre acel punct
6. dialogul si comportamentul se schimba deoarece `currentPlaceType = forge`

### Quest de inspectie

1. questul cere `inspect_node`
2. tinta este `satul_central:fierarie:forge_spot`
3. listener-ul detecteaza apropierea jucatorului
4. progresul pe obiectiv este actualizat
5. jurnalul si dialogul se schimba in functie de noul progres

### Generare de sat

1. sistemul alege zona
2. construieste cladirile
3. inregistreaza `region`
4. inregistreaza `places`
5. adauga `nodes`
6. salveaza mapping-ul
7. NPC-urile si questurile incep sa foloseasca automat noua structura

## Imbunatatiri recomandate

### API public de scriere mai complet

Acum exista creare runtime prin comenzi si service, dar pe termen lung merita un contract public clar pentru mutatii controlate.

Exemple:

- `createRegion`
- `createPlace`
- `createNode`
- `removeRegion`
- `removePlace`
- `removeNode`
- `updatePlaceMetadata`

### Persistenta mai buna decat `config.yml`

Pentru continut generat dinamic, `config.yml` este bun ca start, dar nu este suficient pe termen lung.

Imbunatatiri:

- persistenta in baza de date
- versiuni de mapping
- snapshot / rollback
- export / import

### Stare dinamica pentru `places`

In prezent, `place` este in principal o definitie statica.
Pe viitor, un `place` ar trebui sa poata avea si stare.

Exemple:

- `open`
- `closed`
- `under_attack`
- `burned`
- `contested`
- `quest_locked`

Asta ar ajuta:

- dialogul
- questurile
- scenariile
- rutinele

### Tipuri si roluri mai bogate pentru `nodes`

Roluri semantice utile:

- `entry`
- `exit`
- `sleep`
- `work`
- `guard`
- `shop_counter`
- `ritual_center`
- `loot_spawn`
- `conversation_anchor`

Aceste roluri pot sta fie in `type`, fie in metadata standardizata.

### Relatii intre `places`

Sistemul actual descrie containment, dar nu si relatii intre locuri.

Relatii utile:

- `connected_to`
- `adjacent_to`
- `owned_by`
- `serves`
- `depends_on`

Exemple:

- taverna este conectata la piata
- fieraria serveste satul central
- camera tronului apartine castelului

### Ierarhii si sub-places

Pentru castele, pesteri si orase mari, un singur nivel de `place` poate deveni prea simplu.

Extensii utile:

- `parentPlaceId`
- `subplaces`
- camere interioare
- zone functionale din cladiri mari

Exemple:

- `castel:sala_mare`
- `castel:turn_nord`
- `castel:turn_nord:camera_garda`

### Evenimente Bukkit pentru mapping

Evenimente utile pentru extensii:

- `AINPCRegionEnteredEvent`
- `AINPCPlaceEnteredEvent`
- `AINPCPlaceExitedEvent`
- `AINPCNodeReachedEvent`
- `AINPCMappingReloadedEvent`

Acestea ar permite:

- quest hooks
- scene triggers
- analytics
- addonuri externe

### Validare mai stricta

Verificari utile:

- `place` complet in interiorul regiunii
- `node` in interiorul containerului
- suprapuneri per tip
- owner NPC valid
- tag-uri necunoscute
- metadata obligatorie pentru anumite tipuri

### Editor si unelte admin mai bune

Pe termen lung, configurarea manuala devine lenta.

Imbunatatiri utile:

- selectie din world cu doua colturi
- `setpos1 / setpos2` intern
- creare `place` din selectie
- creare `node` la pozitia curenta
- highlight vizual pentru region / place / node
- export rapid din selectie

### Integrare cu navigatie si pathing

Mapping-ul devine mai util cand se leaga de miscare.

Exemple:

- `preferred entry node`
- `sleep node`
- `work node`
- drumuri sau muchii intre `places`
- costuri de traversare

Astfel:

- NPC-urile merg mai coerent
- escort quests sunt mai stabile
- scenele cu miscare sunt mai usor de orchestrat

### Template-uri si generare semantica

Pentru constructie automata, mapping-ul ar trebui generat din template metadata.

Exemple:

- o schematica de fierarie vine cu `placeType=forge`
- anumite markere din template definesc `nodes`
- template-ul declara tag-uri, owner roles si hooks

Asta reduce munca manuala dupa generare.

### Standardizare pentru tags si metadata

Pe termen lung, trebuie evitat haosul semantic.

Merita definite:

- tag-uri oficiale
- chei oficiale de metadata
- conventii de naming

Exemple:

- `role=work`
- `access=public`
- `business=blacksmith`
- `danger=high`

## Ordine buna de evolutie

1. leaga NPC-urile si `NPCContext` de `placeId`
2. adauga tracking vizual/textual pentru `visit_place` si `inspect_node`
3. extinde `WorldContextSnapshot` pentru AI, questuri si story
4. adauga evenimente de mapping
5. standardizeaza `tags` si metadata
6. introdu stare dinamica pentru `places`
7. adauga relatii intre `places`
8. adauga editor si unelte vizuale
9. leaga mapping-ul de generare automata si template-uri

## Documente conexe

Foloseste documentele astfel:

- `docs/mapping.md`
  pentru starea actuala, contractul conceptual, reguli de consum si roadmap mapping
- `docs/mapping-pentru-implementari-ulterioare.md`
  redirect istoric catre acest document
- `docs/mapping-harti-manuale.md`
  pentru harti construite manual, etichetare semantica si limitele detectiei automate
- `docs/generare-ai-si-constructie-automata.md`
  pentru rolul mapping-ului in constructie si generare

## Concluzie

`regions / places / nodes` sunt deja implementate si functionale.

Ce era inainte doar propunere a devenit acum infrastructura reala:

- model intern
- API public de citire
- creare runtime
- salvare in `config.yml`
- comenzi admin de inspectie si creare
- scanare vanilla initiala si import semantic runtime
- audit read-only
- debug dump pentru exportarea starii

Pasii urmatori nu mai tin de "daca exista mapping", ci de completarea legaturii cu:

- NPC-uri la nivel de `homePlaceId`, `workPlaceId`, `socialPlaceId`
- questuri
- scenarii
- generare automata
