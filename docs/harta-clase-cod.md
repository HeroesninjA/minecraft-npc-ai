# Harta claselor de cod

Actualizat: 2026-06-21

Acest document este doar documentatie. Nu schimba runtime-ul si nu modifica fluxul de executie.

Pentru orientare rapida, citeste mai intai [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa a pachetelor](./harta-pachetelor-cod.md).
Pentru o intrare rapida in subharti, foloseste [indexul hartii claselor](./harta-clase-index.md).
Pentru detaliu pe world, foloseste [harta claselor pentru world](./harta-clase-world.md).
Pentru detaliu pe AI, foloseste [harta claselor pentru AI](./harta-clase-ai.md).
Pentru detaliu pe quest, foloseste [harta claselor pentru quest](./harta-clase-quest.md).
Pentru detaliu pe spawn, foloseste [harta claselor pentru spawn](./harta-clase-spawn.md).
Pentru detaliu pe debug, foloseste [harta claselor pentru debug](./harta-clase-debug.md).
Pentru detaliu pe platforma si database, foloseste [harta claselor pentru platform si database](./harta-clase-platform-db.md).
Pentru detaliu pe NPC, foloseste [harta claselor pentru NPC](./harta-clase-npc.md).

## Scop

Harta de mai jos arata cum se leaga clasele principale din codul efectiv: puncte de intrare, servicii, manageri, snapshot-uri, validatoare si subsisteme.

Nu este un inventar complet al tuturor claselor. Este o harta de lucru pentru vibe coding si pentru navigare rapida intre nodurile care conteaza.

## Citire rapida

- `AINPCPlugin` este punctul de intrare pentru pluginul core.
- `AINPCPlatform` leaga addonurile, profilul de runtime si serviciile de platforma.
- `WorldAdminService` este centrul pentru mapping semantic, regiuni, locuri si node-uri.
- `DialogManager` gestioneaza dialogul, contextul AI si efectele asupra relatiilor si emotiilor.
- `AIOrchestrationService` controleaza decizia AI si fallback-ul.
- `AINPC` este modelul de NPC persistent si entity bridge.

## Noduri principale

### Intrare si bootstrap

- `AINPCPlugin` -> `DatabaseManager`, `AINPCPlatform`, `AddonRegistry`, `NPCManager`, `DialogManager`, `OpenAIService`, `AIOrchestrationService`, `RoutineService`, `SpawnOrchestrator`, `HouseholdPersistenceService`, `NpcWorldBindingService`, `FeaturePackLoader`, `ProgressionService`, `StoryContextService`, `StoryStateService`, `GuiService`, `MappingWandService`, `RecentEventsBuffer`
- `AINPCPlatform` -> `AddonRegistry`, `WorldAdminService`, `RuntimeFeatureResolver`, `PlatformProfile`, `RuntimeFeatureSnapshot`
- `AddonRegistry` -> `AINPCPlatformApi`, `AINPCAddon`, `AddonDescriptor`

### World si mapping

- `WorldAdminService` -> `WorldRegion`, `WorldPlace`, `WorldNode`, `WorldContextSnapshotBuilder`, `WorldMappingSemanticIndex`
- `WorldContextSnapshotBuilder` -> `WorldContextSnapshot`
- `WorldContextSnapshot` -> `NpcBindingInfo`, `NearbyNpcInfo`
- `WorldAdminService` si `WorldContextSnapshotBuilder` sunt baza pentru `mapping`, `spawn`, `quest anchor` si inspectare.

### NPC si stare

- `AINPC` -> `NPCContext`, `NPCPersonality`, `NPCEmotions`, `NPCState`, `NPCAction`
- `NPCManager` -> `AINPC`, `NPCContext`, `NpcVillageSnapshot`, `NpcRepairCounters`
- `NpcFactResolver` -> fapte NPC, intentii si extragere de semnale pentru dialog si story

### AI si dialog

- `DialogManager` -> `OpenAIService`, `PromptSnapshot`, `DialogHistory`, `NPCRelationship`, `NPCEmotions`, `NpcFactResolver`, `StoryContextService`
- `OpenAIPromptSnapshotFactory` -> `PromptSnapshot`
- `AIOrchestrationService` -> `AIOrchestrationPolicy`, `AIOrchestrationRequest`, `AIOrchestrationResult`
- `OpenAIService` -> `OpenAITextSupport`, `OpenAIConnectionProbe`

### Quest, progression si story

- `ProgressionService` -> definitii de progres si mecanici generice
- `StoryContextService` -> context narativ si semnale pentru AI/quest
- `StoryStateService` -> stare persistenta si evenimente de story
- `QuestDirector` -> `QuestDirectorRequest`, `QuestDirectorDecision`
- `QuestAnchorResolver` -> ancore de quest si mapping semantic

### Spawn, rutina si persistenta

- `RoutineService` -> `RoutineEngine`, `RoutineAssignment`, `RoutineSlot`
- `NpcSpawnOrchestrator` -> `NpcSpawnPlan`, `NPCManager`
- `HouseholdPersistenceService` -> stare de persistenta pentru household
- `NpcWorldBindingService` -> legaturi NPC -> world / home / work / social

### GUI, comenzi si operare

- `GuiService` -> `GuiSessionManager`, `GuiScreen`
- `WorldHubGui`, `StoryGui`, `StatsGui` -> ecrane operationale pe zone diferite
- `AINPCCommand`, `AINPCTabCompleter`, `ListenerRegistry` -> suprafata operationala a pluginului
- `DatabaseManager` -> persistenta, schema si conexiuni

### Patch, audit si debug

- `VillageGapAnalyzer` -> `VillageGap`, `PatchCandidate`, `PatchPlannerResult`
- `VillagePatchPlanner` -> `PatchPlan`, `PatchPlannerResult`
- `ExteriorStructureAnalyzer` -> `ExteriorStructurePlan`, `ExteriorStructureReport`
- `DebugDumpService`, `AuditReport`, `RecentEventsBuffer` -> inspectare si diagnostic

## Relatii intre clase cheie

### Fluxul principal

`AINPCPlugin` -> `AINPCPlatform` -> `WorldAdminService` -> `WorldContextSnapshotBuilder` -> `WorldContextSnapshot`

`AINPCPlugin` -> `DialogManager` -> `OpenAIService` / `PromptSnapshot` / `NPCRelationship`

`AINPCPlugin` -> `AIOrchestrationService` -> `AIOrchestrationPolicy` -> `AIOrchestrationRequest`

`AINPCPlugin` -> `GuiService` -> `GuiSessionManager` -> `GuiScreen`

### Fluxul world -> spawn -> rutina

`WorldAdminService` -> `WorldRegion` -> `WorldPlace` -> `WorldNode`

`NpcSpawnOrchestrator` -> `NpcSpawnPlan` -> `NPCManager`

`RoutineService` -> `RoutineEngine` -> `RoutineAssignment`

`NpcWorldBindingService` leaga aceste fluxuri impreuna.

### Fluxul AI -> dialog -> story

`DialogManager` -> `OpenAIService` -> `OpenAIPromptSnapshotFactory`

`DialogManager` -> `NpcFactResolver` -> `NPCEmotions`

`StoryContextService` si `StoryStateService` alimenteaza si salveaza contextul narativ.

## Reguli de lectura

- Incepe cu clasele de bootstrap daca vrei sa intelegi ordinea de pornire.
- Foloseste `WorldAdminService` si `WorldContextSnapshotBuilder` daca vrei traseul semantic al lumii.
- Foloseste `DialogManager` si `AIOrchestrationService` daca vrei traseul AI.
- Foloseste `AINPC` si `NPCManager` daca vrei traseul unui NPC concret.

## Nota

Aceasta harta este complementara hartii de pachete. Daca te intereseaza numai relatiile intre directoare si module, foloseste mai intai harta de pachete. Daca te intereseaza cum curg datele intre clase, foloseste acest document.
