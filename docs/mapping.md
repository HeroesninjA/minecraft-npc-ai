# Mapping

Actualizat: 2026-04-30

## Scop

Acest document descrie strict starea actuala a sistemului de mapping dupa implementarile recente.

Pentru directia de evolutie si ideile pe termen lung, vezi:

- `docs/mapping-pentru-implementari-ulterioare.md`
- `docs/generare-ai-si-constructie-automata.md`

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
- `/ainpc world save`
- `/ainpc audit [all|npc|world|db|spawn]`
- `/ainpc debugdump [all|npc|world|quest|openai]`

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
- `WorldPlace` poate avea `owner_npc_id`
- nu exista inca un camp persistent explicit `homePlaceId` sau `workPlaceId` pe NPC
- asocierea inversa completa `place -> npc -> place` trebuie inca standardizata

## Ce nu este inca legat complet

Sistemul de mapping exista, dar urmatoarele piese nu sunt inca conectate complet:

- NPC-urile folosesc ancore de locatie, dar nu au inca `homePlaceId`, `workPlaceId`, `socialPlaceId`
- `NPCContext` expune context semantic initial prin `WorldContextSnapshot`, dar nu exista inca bindings persistente dedicate pe placeId/nodeId
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

## Ce document acopera ce

Foloseste documentele astfel:

- `docs/mapping.md`
  pentru starea actuala si contractul conceptual de baza
- `docs/mapping-pentru-implementari-ulterioare.md`
  pentru cum trebuie consumat mapping-ul in viitoarele sisteme
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
