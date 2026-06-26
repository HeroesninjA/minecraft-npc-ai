# DeepSeek Taskuri - Batch 22 (L1101-L1150)

Actualizat: 2026-06-25

Continua seria arhitecturala din Batch 21 cu migrarea obiectivelor la handler-e si consolidarea.

Status: draft incomplet. Nu executa automat acest batch pana cand intervalul L1106-L1150 este expandat in taskuri individuale cu campurile obligatorii.

---

### L1101 CollectItemObjectiveHandler
**Descriere:** Converte?te logica hardcodata pentru collect_item in handler inregistrat.
**Target:** engine/runtime/objectivehandlers/CollectItemObjectiveHandler.kt
**Acceptare:** collect_item poate fi procesat prin handler.

### L1102 DeliverToNpcObjectiveHandler
**Descriere:** Converte?te deliver_to_npc in handler inregistrat.
**Target:** engine/runtime/objectivehandlers/DeliverToNpcObjectiveHandler.kt
**Acceptare:** deliver_to_npc functioneaza prin handler.

### L1103 KillMobObjectiveHandler
**Descriere:** Converte?te kill_mob in handler inregistrat.
**Target:** engine/runtime/objectivehandlers/KillMobObjectiveHandler.kt
**Acceptare:** kill_mob functioneaza prin handler.

### L1104 VisitRegionObjectiveHandler
**Descriere:** Converte?te visit_region in handler.
**Target:** engine/runtime/objectivehandlers/VisitRegionObjectiveHandler.kt

### L1105 VisitPlaceObjectiveHandler
**Descriere:** Converte?te visit_place in handler.
**Target:** engine/runtime/objectivehandlers/VisitPlaceObjectiveHandler.kt

## Rezervare L1106-L1150

Interval rezervat, nu task executabil. Expandarea trebuie facuta ulterior in taskuri individuale `### L1106`, `### L1107` etc., fiecare cu campurile obligatorii.

(Rezervat pentru continuare)
