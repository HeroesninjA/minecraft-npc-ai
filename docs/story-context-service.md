# StoryContextService

Actualizat: 2026-04-30

Status: implementat initial ca strat read-only peste mapping si quest anchors.

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

Serviciul este initializat in `AINPCPlugin` si expus prin `getStoryContextService()`.

`NPCContext` include sectiunea `STORY_CONTEXT` in prompt cand exista un jucator care interactioneaza cu NPC-ul.

Comanda admin read-only:

```text
/ainpc story context [jucator] [numeNpc|nearest]
```

Fara NPC tinta, contextul se construieste pentru locatia jucatorului.

## Ce contine snapshot-ul

Snapshot-ul poate include:

- NPC-ul tinta si ocupatia lui
- jucatorul tinta
- regiunea curenta, `storyState`, `storyMode` si `storyPool`
- place-ul curent, tipul, tag-urile si metadata
- node-uri apropiate relevante pentru quest/story
- quest anchors active pentru jucator
- warnings daca mapping-ul, DB-ul sau locatia nu sunt disponibile

## Reguli

- Serviciul este read-only.
- Nu creeaza story state nou.
- Nu scrie in DB.
- Nu genereaza questuri sau povesti prin AI.
- Nu inlocuieste `QuestAnchorResolver`; doar expune ancorele active ca parte din context.

## Limitari

- Story state-ul persistent dedicat pe regiune/place nu exista inca.
- Evenimentele story recente nu sunt persistate inca.
- Semnalele story din place vin temporar din `metadata`.
- Contextul depinde de mapping existent; daca serverul are 0 regiuni/places/nodes, snapshot-ul va contine warnings si fallback limitat.

## Faza urmatoare

Urmatorul pas tehnic este persistenta story:

- `region_story_state`
- `place_story_state`
- `story_events`
- actiuni de quest precum `set_story_state` si `record_story_event`
- audit/debug pentru story state
