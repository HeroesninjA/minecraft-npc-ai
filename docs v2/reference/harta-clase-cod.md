# Harta Claselor de Cod

Status: canonical in `docs v2`.
Actualizat: 2026-06-21.

Acesta este rezumatul global pentru clasele principale din codul AINPC.

## Citire rapida

- `AINPCPlugin` este punctul de intrare;
- `AINPCPlatform` leaga addonuri, profil si platforma;
- `WorldAdminService` acopera mapping-ul semantic;
- `DialogManager` si `AIOrchestrationService` acopera AI-ul;
- `ProgressionService`, `QuestDirector` si `StoryContextService` acopera quest/story;
- `NpcSpawnOrchestrator` si `RoutineService` acopera spawn si rutina;
- `GuiService` acopera ecranele;
- `DebugDumpService` si `RecentEventsBuffer` acopera diagnosticul.

## Rol

- ofera o vedere de ansamblu pentru navigare rapida;
- leaga subhartile specializate;
- ramane un index de orientare, nu un inventar complet.

## Legaturi

- `reference/harta-clase-index.md`
- `architecture/harta-clase-world.md`
- `architecture/harta-clase-ai.md`
- `architecture/harta-clase-quest.md`
- `architecture/harta-clase-spawn.md`
- `guides/harta-clase-gui.md`
- `architecture/harta-clase-debug.md`
