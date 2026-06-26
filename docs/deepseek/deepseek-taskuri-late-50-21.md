# DeepSeek Taskuri - Batch 23 (L1151-L1200)

Actualizat: 2026-06-25

Dupa implementarea completa a seriei L001-L1150, acest batch continua cu optimizari si consolidari.

Status: draft incomplet. Nu executa automat acest batch pana cand intervalul L1158-L1200 este expandat in taskuri individuale cu campurile obligatorii.

---

### L1151 Register all 8 objective handlers in ScenarioEngine
**Descriere:** Verifica si inregistreaza toti cei 8 handler-i in registerRuntimeHandlers().
**Target:** ScenarioEngine.kt
**Acceptare:** Toti handler-ele sunt inregistrati la startup.

### L1152 TalkToNpcObjectiveHandler
**Descriere:** Converte?te talk_to_npc in handler inregistrat.
**Target:** engine/runtime/objectivehandlers/TalkToNpcObjectiveHandler.kt

### L1153 BreakBlockObjectiveHandler
**Descriere:** Converte?te break_block in handler.
**Target:** engine/runtime/objectivehandlers/BreakBlockObjectiveHandler.kt

### L1154 PlaceBlockObjectiveHandler
**Descriere:** Converte?te place_block in handler.
**Target:** engine/runtime/objectivehandlers/PlaceBlockObjectiveHandler.kt

### L1155 CraftItemObjectiveHandler
**Descriere:** Converte?te craft_item in handler.
**Target:** engine/runtime/objectivehandlers/CraftItemObjectiveHandler.kt

### L1156 InspectNodeObjectiveHandler
**Descriere:** Converte?te inspect_node in handler.
**Target:** engine/runtime/objectivehandlers/InspectNodeObjectiveHandler.kt

### L1157 Verify all objective handlers work with ObjectiveHandlerRegistry
**Descriere:** Test care verifica ca toti cei 12+ handler-i sunt inregistrabili si gasibili.
**Target:** ObjectiveHandlerRegistryTest.kt

## Rezervare L1158-L1200

Interval rezervat, nu task executabil. Expandarea trebuie facuta ulterior in taskuri individuale `### L1158`, `### L1159` etc., fiecare cu campurile obligatorii.

(Rezervat)
