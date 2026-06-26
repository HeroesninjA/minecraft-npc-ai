# DeepSeek Taskuri - Batch 21 (L1051-L1100)

Actualizat: 2026-06-25

Acest document marcheaza trecerea la taskuri arhitecturale dupa finalizarea seriei de implementare curente (L001-L1050).

Status: draft incomplet. Nu executa automat acest batch pana cand intervalul L1061-L1100 este expandat in taskuri individuale cu campurile obligatorii.

---

### L1051 Runtime extensibil - ObjectiveHandler interface
**Descriere:** Creeaza interfata ObjectiveHandler care permite addonurilor sa defineasca tipuri custom de obiective.
**Target:** engine/runtime/ObjectiveHandler.kt
**Acceptare:** Un addon poate inregistra un handler pentru un tip de obiectiv.

### L1052 Runtime extensibil - ObjectiveHandlerRegistry
**Descriere:** Registru central pentru ObjectiveHandler, similar cu ScenarioActionRegistry.
**Target:** engine/runtime/ObjectiveHandlerRegistry.kt
**Acceptare:** Handlerele pot fi inregistrate si gasite dupa tip.

### L1053 Runtime extensibil - Migrare use_item la handler
**Descriere:** Convertește logica hardcodata pentru use_item in primul ObjectiveHandler.
**Target:** engine/runtime/objectivehandlers/UseItemObjective.kt, ScenarioEngine.kt
**Acceptare:** use_item functioneaza prin handler inregistrat, nu prin cod hardcodat.

### L1054 Runtime extensibil - Migrare equip_item
**Descriere:** La fel ca L1053 pentru equip_item.
**Target:** engine/runtime/objectivehandlers/EquipItemObjective.kt

### L1055 NPC Reputation System - baza de date
**Descriere:** Adauga tabela player_reputation pentru reputatie per regiune/factiune.
**Target:** DatabaseManager.kt
**Acceptare:** Tabela exista in schema implicit.

### L1056 NPC Reputation System - API si serviciu
**Descriere:** Interfata publica si serviciu pentru reputatie NPC/regiune.
**Target:** ainpc-api, core-plugin
**Acceptare:** Addonurile pot citi si modifica reputatia prin API.

### L1057 Quest chains - next_quest field
**Descriere:** Adauga suport pentru questuri in lant prin campul next_quest in YAML.
**Target:** FeaturePackYamlSupport.kt, ScenarioEngine.kt
**Acceptare:** Completarea unui quest ofera automat urmatorul.

### L1058 Quest chains - precondition checking
**Descriere:** Verifica prerequisite-urile si next_quest la oferirea questurilor.
**Target:** ScenarioEngine.kt, QuestDirector.kt

### L1059 Better error messages in YAML loading
**Descriere:** Mesaje de eroare care includ fisierul, linia si contextul pentru YAML invalid.
**Target:** FeaturePackLoader.kt, FeaturePackYamlSupport.kt

### L1060 Runtime extensibil - RecordStoryEventAction ca referinta
**Descriere:** Documenteaza RecordStoryEventAction ca model pentru implementarea de handler-e custom.
**Target:** ./arhiva/ sau docs/categorii/
**Acceptare:** Un addon developer poate urma modelul.

## Rezervare L1061-L1100

Interval rezervat, nu task executabil. Expandarea trebuie facuta ulterior in taskuri individuale `### L1061`, `### L1062` etc., fiecare cu campurile obligatorii.

(Rezervat)

