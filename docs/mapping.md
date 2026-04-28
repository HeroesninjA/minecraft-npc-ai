# Mapping

Actualizat: 2026-04-27

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

Pentru `nodes`, indexarea exista deja intern ca fundatie pentru lookup-uri viitoare de tip `findNodesNear(...)`, `inspect_node` sau trigger-e de scenariu.

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
- `/ainpc world save`

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
- baza pentru obiectivele existente de tip `visit_region`
- fundatie pentru viitoarele integrari cu questuri, NPC-uri si generare

Pe scurt: infrastructura este prezenta, dar integrarea completa in gameplay nu este inca terminata.

## Ce nu este inca legat complet

Sistemul de mapping exista, dar urmatoarele piese nu sunt inca conectate complet:

- NPC-urile nu folosesc inca `homePlaceId`, `workPlaceId`, `socialPlaceId`
- `NPCContext` nu expune inca complet `currentPlaceId` si semantica locului
- questurile nu au inca obiective precum `visit_place` sau `inspect_node`
- `places` nu au stare dinamica de tip `open`, `closed`, `under_attack`
- persistenta este in `config.yml`, nu in baza de date

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

Pasii urmatori nu mai tin de "daca exista mapping", ci de legarea lui efectiva cu:

- NPC-uri
- questuri
- scenarii
- generare automata
