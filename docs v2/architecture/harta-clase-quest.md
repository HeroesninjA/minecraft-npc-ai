# Harta claselor pentru quest

Status: canonical in `docs v2`.
Actualizat: 2026-06-21.

Aceasta harta urmareste subsistemul quest, progress si story association.

## Noduri principale

- `QuestDirector`;
- `QuestAnchorResolver`;
- `QuestAuthoringService`;
- `ProgressionService`;
- `StoryContextService`;
- `StoryStateService`;
- `QuestDirectorRequest`;
- `QuestDirectorDecision`.

## Flux

- directorul decide ce tip de cerere primeste prioritate;
- anchor resolver leaga obiectivele de world;
- progression pastreaza progresul real;
- story context si story state explica si persista naratiunea.

## Reguli

- questurile trebuie sa fie auditable;
- ancorele trebuie sa fie semantice si stabile;
- progression este stratul real de stare;
- story completeaza, nu inlocuieste, progresul.

## Legaturi

- `architecture/mapping.md`
- `architecture/story-context-service.md`
- `reference/harta-clase-cod.md`
