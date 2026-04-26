# Mapping System Notes

Actualizat: 2026-04-26

## Scop

Sistem pentru maparea si etichetarea locatiilor din lume, astfel incat NPC-urile, questurile si scenariile sa inteleaga nu doar coordonate, ci si sensul unui loc.

Exemple de locuri:
- `casa_fierarului`
- `fierarie`
- `magazin`
- `taverna`
- `piata`
- `ferma`

Ideea de baza:
- coordonatele spun `unde`
- tipul locului spune `ce este`
- tag-urile spun `cum se foloseste`

## De ce este important

- NPC-urile pot sti daca sunt acasa, la munca sau intr-un loc public
- Dialogul poate deveni contextual
- Rutinele zilnice devin credibile
- Questurile pot folosi destinatii semantice, nu doar coordonate
- Regiunile si cladirile pot avea identitate proprie

## Model recomandat

Structura buna este in 3 nivele:

- `Region` = zona mare, de exemplu sat, castel, pestera
- `Place` = loc semantic, de exemplu casa fierarului, fierarie, magazin
- `Node` = punct exact, de exemplu usa, pat, tejghea, quest trigger

Observatie:
- `WorldRegion` exista deja si ramane zona mare
- `WorldNode` exista deja si ramane punct exact
- trebuie adaugat un nou nivel: `WorldPlace`

## Clasa noua recomandata

Fisier nou propus:
- `src/src/main/java/ro/ainpc/world/WorldPlace.java`

Campuri minime:
- `id`
- `regionId`
- `displayName`
- `worldName`
- `placeType`
- `minX`
- `minY`
- `minZ`
- `maxX`
- `maxY`
- `maxZ`
- `tags`
- `ownerNpcId`
- `publicAccess`
- `metadata`

Metode utile:
- `contains(worldName, x, y, z)`
- `hasTag(tag)`

## Enum nou recomandat

Fisier nou propus:
- `src/src/main/java/ro/ainpc/world/PlaceType.java`

Tipuri initiale:
- `HOUSE`
- `SHOP`
- `FORGE`
- `TAVERN`
- `FARM`
- `CASTLE_ROOM`
- `CUSTOM`

## Config recomandat

Locurile pot fi definite sub fiecare regiune in `world_admin.regions.<regionId>.places`.

Exemplu:

```yml
world_admin:
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
        casa_fierarului:
          name: "Casa fierarului"
          type: "house"
          min: { x: 130, y: 64, z: 145 }
          max: { x: 138, y: 72, z: 153 }
          tags: [home, residential, blacksmith]
          owner_npc_id: "npc_ion"

        fierarie:
          name: "Fierarie"
          type: "forge"
          min: { x: 140, y: 64, z: 145 }
          max: { x: 149, y: 73, z: 155 }
          tags: [workplace, blacksmith, shop]
          owner_npc_id: "npc_ion"
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

Reguli utile:
- `id` trebuie sa fie stabil si folosit intern
- `name` este textul afisat
- `tags` sunt folosite in AI, questuri si rutine

## Modificari in WorldAdminService

Fisier vizat:
- `src/src/main/java/ro/ainpc/world/WorldAdminService.java`

Adaugari recomandate:
- colectie `placesByRegion`
- incarcare `places` din config
- suport pentru noduri in interiorul unui place sau mapare la regiune

Metode noi utile:
- `getPlaces(regionId)`
- `findPlace(worldName, x, y, z)`
- `findPlacesByTag(regionId, tag)`
- `findPlaceById(placeId)`

## Modificari in API

Fisier vizat:
- `ainpc-api/src/main/java/ro/ainpc/api/WorldAdminApi.java`

Extensii recomandate:
- expunere pentru cautare si listare de places
- doar metode de citire, fara dependinte Bukkit in API-ul public

## Integrare cu NPC

Fisier relevant:
- `src/src/main/java/ro/ainpc/npc/AINPC.java`

Exista deja:
- `homeAnchor`
- `workAnchor`
- `socialAnchor`

Adaugari recomandate:
- `homePlaceId`
- `workPlaceId`
- `socialPlaceId`

Ideea corecta:
- ancora ramane punct util
- place-ul devine locul semantic real

Exemplu:
- `homePlaceId = casa_fierarului`
- `workPlaceId = fierarie`

## Integrare cu NPCContext

Fisier relevant:
- `src/src/main/java/ro/ainpc/npc/NPCContext.java`

Adaugari recomandate:
- `currentRegionId`
- `currentPlaceId`
- `currentPlaceType`
- `currentPlaceTags`

Reguli utile:
- `isAtHome` sa poata folosi si `homePlaceId`, nu doar raza fata de ancora
- `isAtWork` sa poata folosi si `workPlaceId`
- dialogul sa includa numele si tipul locului curent

Exemple de efect:
- NPC-ul poate spune ca este la fierarie
- NPC-ul poate spune ca magazinul este inchis noaptea
- comportamentul poate diferi in functie de `home`, `workplace`, `public`, `shop`

## Integrare cu questuri

Fisier relevant:
- `src/src/main/java/ro/ainpc/engine/ScenarioEngine.java`

Exista deja:
- suport pentru `visit_region`

Adaugari recomandate:
- `visit_place`
- `talk_to_npc_at_place`
- `bring_item_to_place`

Exemple:
- mergi la `fierarie`
- vorbeste cu fierarul la `magazin`
- du minereul la `forge`

## Cum sa incepi simplu

Nu e nevoie de un editor vizual la inceput.

MVP recomandat:
1. `WorldPlace`
2. `PlaceType`
3. incarcare din YAML
4. `findPlace(...)`
5. legare NPC -> place
6. folosire in `NPCContext`
7. obiectiv `visit_place`

## Ordine buna de implementare

1. Adauga `WorldPlace` si `PlaceType`
2. Extinde `WorldAdminService` sa incarce `places`
3. Adauga metodele de cautare pentru places
4. Extinde `WorldAdminApi`
5. Leaga `AINPC` de `homePlaceId`, `workPlaceId`, `socialPlaceId`
6. Injecteaza place-ul curent in `NPCContext`
7. Extinde `ScenarioEngine` cu obiective bazate pe place
8. Adauga mai tarziu comenzi admin pentru setare rapida

## Concluzie

Acest sistem nu este doar pentru organizare.
Este o baza pentru:
- AI contextual
- rutine credibile
- questuri clare
- scenarii coerente
- lume mai usor de extins
