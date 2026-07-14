# StoryContextService

Status: contract canonic pentru proiectia narativa read-only.
Actualizat: 2026-07-14.

Serviciul construieste un snapshot compact pentru NPC, jucator si locatie.

## Intrari

- mapping semantic;
- quest anchors si progres relevant;
- story state persistent;
- evenimente recente si context local.

## Iesire

- context narativ redactat pentru AI, GUI, briefing si diagnostic;
- warnings explicite cand datele sau lumea lipsesc.

## Limite

- nu creeaza story state si nu scrie in DB;
- nu decide progresul questurilor;
- nu inlocuieste `QuestAnchorResolver`;
- nu transforma indicii narative in stare executabila.

## Legaturi

- `architecture/story-state-service.md`
- `architecture/mapping.md`
- `architecture/story-si-context-ai.md`
