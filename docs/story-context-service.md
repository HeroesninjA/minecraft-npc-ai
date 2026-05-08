# StoryContextService

Actualizat: 2026-05-07

Status: implementat initial ca strat read-only peste mapping, quest anchors si story state persistent initial; scrierile controlate vin prin actiuni de quest, iar audit/debugdump-ul pentru story state exista initial.

## Scop

`StoryContextService` construieste un context narativ compact pentru NPC, jucator si locatia curenta, fara sa lase AI-ul sa inventeze coordonate sau ID-uri executabile.

Este legatura practica dintre:

- `WorldContextSnapshot`
- regiune, place si node-uri mapate
- NPC-ul tinta si rolul lui
- quest anchors active din `quest_anchor_bindings`
- semnale story deduse din `storyState`, `storyPool`, tag-uri si metadata

## Ce exista in cod

Implementarea initiala este in `ro.ainpc.story`:

- `StoryContextService`
- `StoryContextSnapshot`
- `StoryContextSnapshot.QuestAnchorSnapshot`
- `StoryStateService`
- `RegionStoryState`
- `PlaceStoryState`
- `StoryEvent`

Serviciul este initializat in `AINPCPlugin` si expus prin `getStoryContextService()`.

Persistenta story initiala este expusa separat prin `getStoryStateService()` si foloseste tabelele:

- `region_story_state`
- `place_story_state`
- `story_events`

`NPCContext` include sectiunea `STORY_CONTEXT` in prompt cand exista un jucator care interactioneaza cu NPC-ul.

Quest completion poate scrie in persistenta story prin actiunile:

- `set_story_state`
- `record_story_event`

Comenzi admin read-only:

```text
/ainpc story context [jucator] [numeNpc|nearest]
/ainpc story region <regionId>
/ainpc story place <placeId>
/ainpc story events <regionId|placeId> [limit]
/ainpc debugdump story
```

Fara NPC tinta, contextul se construieste pentru locatia jucatorului.

## Ce contine snapshot-ul

Snapshot-ul poate include:

- NPC-ul tinta si ocupatia lui
- jucatorul tinta
- regiunea curenta, `storyState`, `storyMode` si `storyPool`
- place-ul curent, tipul, tag-urile si metadata
- story state persistent pentru regiune/place, daca exista
- evenimente story recente pentru regiune/place, daca exista
- node-uri apropiate relevante pentru quest/story
- quest anchors active pentru jucator
- warnings daca mapping-ul, DB-ul sau locatia nu sunt disponibile

## Reguli

- Serviciul este read-only.
- Nu creeaza story state nou.
- Nu scrie in DB.
- Nu genereaza questuri sau povesti prin AI.
- Nu inlocuieste `QuestAnchorResolver`; doar expune ancorele active ca parte din context.
- Scrierile in story state trebuie facute prin `StoryStateService`, nu prin `StoryContextService`.

## Limitari

- Actiunile story exista initial doar ca intrari de reward executate la finalizarea questului.
- Evenimentele story si story state-ul persistent pot fi inspectate read-only prin comenzi si exportate prin `story-states.json`/`story-events.json`.
- Semnalele story din place vin temporar din `metadata`.
- Contextul depinde de mapping existent; daca serverul are 0 regiuni/places/nodes, snapshot-ul va contine warnings si fallback limitat.

## Faza urmatoare

Urmatorii pasi tehnici sunt intarirea contractului de authoring:

- validator pentru actiunile story din feature packs
- reguli explicite pentru chei, scope, event types si retention
