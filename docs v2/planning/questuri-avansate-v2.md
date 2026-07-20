# Roadmap activ pentru quest si progresie V2

Status: roadmap activ, nu sursa de stare.
Actualizat: 2026-07-15.

Starea confirmata ramane in `canonical/implementat-deja.md`, iar contractele raman in documentele de arhitectura si referinta. Aceasta pagina contine numai diferentele dintre runtime-ul actual si un flux quest V2 coerent cap-coada.

## Baza deja implementata

- `ScenarioEngine` acopera acceptare, refuz, abandon, progres, stage-uri, tracking, recompense si persistenta;
- `ProgressionService` ofera proiectia generica, selectori, snapshot-uri, audit si operatii comune;
- registry-ul si runtime-ul recunosc 12 tipuri canonice de obiective;
- `QuestAnchorResolver` rezolva regiuni, places, nodes si NPC-uri, iar repository-ul persista binding-uri per jucator;
- formularul avansat produce draft JSON, iar Quick Quest produce preview YAML;
- pack-ul medieval include exemple `QUEST`, `TRADE_DEAL`, `DUTY`, `BOUNTY`, `WORLD_EVENT`, `TUTORIAL` si `RITUAL`.

Aceasta baza nu demonstreaza automat ca fiecare suprafata, tip sau mecanica este completa si coerenta.

## P0 - Contracte care blocheaza increderea

### 1. Binding global explicit (IMPLEMENTAT)

- `ProgressionAnchorBinding.GLOBAL_PLAYER_UUID = "__global__"` defineste namespace-ul global;
- `queryAnchorBindings` cu `"__global__"` filtreaza explicit `b.player_uuid = '__global__'`;
- `saveAnchorBinding` si `deleteAnchorBinding` accepta `__global__`;
- `QuestAnchorBindingService` interogheaza namespace-ul global si face merge personal peste global per `objectiveKey`;
- query-ul cu `""` pastreaza comportamentul fara filtru pentru operatiuni administrative.

### 2. Delivery fara camp supraincarcat (IMPLEMENTAT)

- `QuestEntryDefinition.npcTarget` expune campul `npc_target` din metadata YAML;
- `QuestAnchorResolver` foloseste `npcTarget` cu prioritate pentru `talk_to_npc` si `deliver_to_npc`;
- fallback la `itemId` pentru compatibilitate cu pack-urile vechi;
- `item` ramane campul pentru materialul livrat.

### 3. Un singur contract de export (IMPLEMENTAT)

- `handleQuickQuestExport` genereaza acum YAML in format Feature Pack complet (acelasi ca `buildQuickYaml`);
- suporta obiective multiple cu target si amount per obiectiv;
- reward-urile au item si amount separate explicit;
- comanda `ainpc quest backup` creeaza backup ZIP al pack-urilor, iar `reload` functioneaza fara argumente pentru reload complet.

## P1 - Consistenta suprafetelor

### 4. Matrice unica de capabilitati (IMPLEMENTAT)

- QuickQuest expune 10 tipuri (talk_to_npc, deliver_to_npc, collect_item, visit_place, visit_region, inspect_node, kill_mob, craft_item, place_block, break_block);
- QuestCreateGui expune toate 12 tipurile din registru;
- QuestSeedFactory propune toate 12 tipurile din registru (aliniat cu full set);
- `OptionalTypeSyncAuditTest` valideaza ca fiecare suprafata foloseste numai tipuri din registru si ca factory-ul acopera tot setul;
- QuickQuest ramane un subset documentat (exclude type-urile mai putin comune `use_item`, `equip_item` pentru simplitatea wizard-ului).

### 5. Test cap-coada pentru fiecare tip (IMPLEMENTAT)

- `ObjectiveTypePipelineAuditTest` valideaza pipeline-ul complet pentru toate cele 12 tipuri: handler inregistrat, normalizare corecta, required fields, non-deprecated, handler progress functional;
- `ScenarioObjectiveProgressTest` (30+ teste) acopera progres, stari (PENDING/STARTED/IN_PROGRESS/COMPLETED/FAILED), inspectie inventar, consum, ancore si reward-uri;
- `ObjectiveHandlerRegistryTest` valideaza toate cele 12 handler-e si progresul pentru use_item si collect_item;
- `MedievalQuestPackTest` valideaza structura YAML, tipuri suportate si fazele.

### 6. Observabilitate coerenta (IMPLEMENTAT)

- `DebugDumpQuestAudit` distinge acum erori (tip necunoscut) de warning-uri (alias deprecated, referinta semantica nerezolvata, entry_id instabil);
- audit-ul flag-uieste alias-uri deprecated cu recomandarea canonica;
- `DebugDumpOutputContractTest` si `DebugDumpQuestAuditSummaryTest` valideaza contractul de output;
- `CoreNeutralityStaticAuditTest` verifica separarea continutului tematic de nucleu.

## P2 - Extindere controlata

### 7. Mecanici non-quest verificate individual (IMPLEMENTAT)

- `NonQuestMechanicsAuditTest` valideaza fiecare tip non-quest cu minim 1 scenariu in pack-ul medieval: TRADE_DEAL, DUTY, BOUNTY, WORLD_EVENT, TUTORIAL, RITUAL;
- fiecare scenariu are obiective, rewards si sectiune quest completa;
- `QuestLogGuiFilter` acopera toate cele 6 tipuri + QUEST cu filtre dedicate si alias-uri localizate;
- `ScenarioType` enum si `QuestCreateGui` expun toate base_types;
- `AINPCCommand` are alias-uri de progresie pentru fiecare tip non-quest.

### 8. Generare AI reala, optionala (IMPLEMENTAT)

- Config: `quest.ai_draft_enabled` (default: false) si `quest.ai_draft_strict_validation` (default: true);
- Comanda: `/ainpc quest ai-draft [preview|confirm]` necesita `ainpc.admin`;
- Preview: valideaza completitudinea draftului si afiseaza JSON-ul;
- Confirm: exporta YAML-ul in `packs/ai_draft_<id>.yml` in format Feature Pack;
- Flow: admin completeaza formularul in Quest Create GUI -> preview -> confirm -> reload;
- Nu permite publicare autonoma: necesita actiune explicita `confirm` si `reload`;
- Provider-ul AI extern este optional; fara el, draftul foloseste continutul din formular.

## Gate de demo (VERIFICAT)

`DemoGateAuditTest` (8 teste) valideaza infrastructura pentru cele 7 conditii:

1. **mapping semantic valid si salvat** - comenzi save, WorldAdmin cu regiuni/places, persistenta;
2. **pack versionat, validat si incarcat** - FeaturePackLoader.loadAllPacks(), validare metadata, pack medieval existent;
3. **binding-uri izolate corect** - GLOBAL_PLAYER_UUID, isGlobalNamespace, test de izolare;
4. **acceptare, obiective multi-stage si turn-in** - acceptQuest, stage-uri in pack, return_to_giver;
5. **recompensa si story event** - grantQuestRewards, StoryStateService events, reward summary in audit;
6. **restart cu progres pastrat** - persistQuestProgress, loadPlayerQuests, teste de repository;
7. **aceeasi stare in comenzi, GUI si debugdump** - status/log in comenzi, audit report, GUI snapshot, QuestMapGui.

## In afara scopului pana la inchiderea P0

- publicare autonoma de continut generat;
- al doilea runtime de quest in paralel;
- ancore bazate numai pe coordonate brute;
- tipuri noi de obiective fara listener, validator, persistenta si test.

## Legaturi

- `canonical/implementat-deja.md`
- `architecture/progression-service.md`
- `architecture/generare-automata-questuri-ai.md`
- `reference/objective-types-reference.md`
- `reference/quest-anchor-bindings.md`
- `guides/quest-authoring-tutorial.md`
- `planning/quest-evolution-stack.md`
