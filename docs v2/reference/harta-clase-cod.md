# Harta Claselor de Cod

Status: index derivat din hartile specializate.
Actualizat: 2026-07-16.

Acesta este rezumatul global pentru clasele principale din codul AINPC.

## Citire rapida

- `AINPCPlugin` este punctul de intrare;
- `ServiceRegistry` detine bootstrap-ul, iar `AINPCPlatform` expune profilul si fatada publica;
- `AddonRegistry`, `FeaturePackLoader` si `AINPCScenarioMedievalPlugin` separa lifecycle-ul de cod de continutul declarativ;
- `WorldAdminService`, `VanillaVillageScanner`, `SemanticVillageMapper` si clasele `VillagePatch*` acopera mapping-ul semantic si completarea lui;
- `NPCInteractionListener`, `NPCChatListener` si `ConversationSessionManager` formeaza intrarea player-NPC;
- `DialogManager`, `DialogueEngine` si `OpenAIService` formeaza calea de dialog activa, iar `AIOrchestrationService` este scaffold;
- `DialogManager` detine relatia player-NPC, iar `RelationshipService` relatiile NPC-NPC;
- `ProgressionService`, `QuestDirector` si `StoryContextService` acopera quest/story;
- `HouseAllocationPlanner` si `NpcSpawnOrchestrator` acopera spawn-ul, iar `NarrativeGenerator` ramane preview separat;
- `RoutineService` acopera atribuirea si miscarea de rutina;
- `GuiService` acopera ecranele;
- `DebugDumpService` si `RecentEventsBuffer` acopera diagnosticul.

## Rol

- ofera o vedere de ansamblu pentru navigare rapida;
- leaga subhartile specializate;
- ramane un index de orientare, nu un inventar complet.

## Legaturi

- `reference/harta-clase-index.md`
- `architecture/harta-clase-world.md`
- `architecture/harta-clase-addons.md`
- `architecture/harta-clase-settlement.md`
- `architecture/harta-clase-ai.md`
- `architecture/harta-clase-quest.md`
- `architecture/harta-clase-spawn.md`
- `architecture/harta-clase-gui.md`
- `architecture/harta-clase-debug.md`
