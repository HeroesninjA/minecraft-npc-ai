# Harta Pachetelor de Cod

Actualizat: 2026-06-20

Aceasta este o harta doar-documentatie pentru vibe coding.
Nu schimba codul runtime.
Scopul ei este sa arate rapid cum se leaga modulele, pachetele si straturile de responsabilitate.

## Module

| Modul | Rol | Depinde de |
|---|---|---|
| `ainpc-api` | contracte publice, tipuri shared, evenimente si API addon | Paper API doar la compile/test |
| `ainpc-core-plugin` | implementarea principala a pluginului | `ainpc-api`, Paper API, biblioteci runtime |
| `ainpc-scenario-medieval` | addon/sablon de scenariu medieval | `ainpc-api`, Paper API |

## Flux intre module

- `ainpc-api` defineste contractele.
- `ainpc-core-plugin` implementeaza runtime-ul principal.
- `ainpc-scenario-medieval` consuma contractele si ofera un addon de continut.
- orice extensie noua ar trebui sa depinda de `ainpc-api`, nu de detaliile interne din `ainpc-core-plugin`.

## Harta pe pachete

### `ainpc-api`

- `ro.ainpc.api` - contracte de nivel inalt pentru platforma.
- `ro.ainpc.api.events` - marker si baza pentru evenimente.
- `ro.ainpc.api.events.context` - evenimente de context.
- `ro.ainpc.api.events.dialog` - evenimente pentru dialog.
- `ro.ainpc.api.events.npc` - evenimente pentru NPC.
- `ro.ainpc.api.events.quest` - lifecycle si progres quest.
- `ro.ainpc.api.events.story` - stare si actiuni narative.
- `ro.ainpc.world` - modele partajate pentru world, place, node, mode.
- `ro.ainpc.addons` - contracte addon, descriptor si tipuri asociate.
- `ro.ainpc.platform` - mod runtime si profile.

### `ainpc-core-plugin`

- `ro.ainpc` - plugin entrypoint si asamblare generala.
- `ro.ainpc.bootstrap` - coordonare de startup.
- `ro.ainpc.platform` - profil runtime si feature flags.
- `ro.ainpc.platform.features` - stari si surse pentru functionalitati.
- `ro.ainpc.addons` - registry si integrare addon.
- `ro.ainpc.database` - persistenta si dialecte.
- `ro.ainpc.commands` - suprafata de comenzi admin, authoring si debug.
- `ro.ainpc.listeners` - ascultatori Bukkit/Paper care traduc evenimente in servicii.
- `ro.ainpc.gui` - infrastructura UI generica.
- `ro.ainpc.gui.screens` - ecrane concrete.
- `ro.ainpc.gui.listeners` - input si interactiuni GUI.
- `ro.ainpc.ai` - prompturi, snapshot-uri si integrare OpenAI.
- `ro.ainpc.ai.orchestration` - politica, request, rezultat si status pentru orchestration AI.
- `ro.ainpc.debug` - dump-uri, rapoarte si citire-only diagnostics.
- `ro.ainpc.engine` - motor de scenarii, quest, dialog si autoring.
- `ro.ainpc.engine.runtime` - registrii si handler-e runtime pentru scenarii.
- `ro.ainpc.managers` - manageri de nivel aplicatie pentru NPC, familie, memorie si audit.
- `ro.ainpc.npc` - model de domeniu NPC.
- `ro.ainpc.progression` - persistenta si snapshot-uri pentru progres.
- `ro.ainpc.routine` - rutina, sloturi si engine de programare.
- `ro.ainpc.story` - stare narativa, context si events.
- `ro.ainpc.spawn` - planificare, validare si executie a spawn-ului.
- `ro.ainpc.topology` - consens si clasificare pentru init si ordine de incarcare.
- `ro.ainpc.utils` - utilitare partajate.
- `ro.ainpc.world` - modelul de lume, binding-uri si service-urile de baza.
- `ro.ainpc.world.mapping` - authoring, wand, draft-uri si intent parsing.
- `ro.ainpc.world.patch` - gap analysis si patch planning.
- `ro.ainpc.world.scan` - scanare/import semantic pentru sate vanilla.
- `ro.ainpc.world.fixture` - fixture controlat pentru testare/demo.
- `ro.ainpc.world.exterior` - structuri exterioare si planificare semantica.

### `ainpc-scenario-medieval`

- `ro.ainpc.addons.medieval` - pluginul/addonul de scenariu medieval.
- `ro.ainpc.scenario` - teste si validari de scenariu.

## Servicii si clase cheie

### `ainpc-api`

- `AINPCPlatformApi` - contractul platformei pentru consumatori.
- `WorldAdminApi` - contractul pentru administrare de lume.
- `AddonRegistryApi` - contractul pentru registrul de addonuri.
- `AINPCEventSource` - baza comuna pentru surse de evenimente.

### `ainpc-core-plugin`

- `AINPCPlatform` - punct de intrare pentru platforma interna.
- `AddonRegistry` - registrul runtime pentru addonuri.
- `WorldAdminService` - administrare de lume si operare.
- `NpcWorldBindingService` - legatura dintre NPC si world state.
- `StoryStateService` si `StoryContextService` - stare si context narativ.
- `RoutineService` si `RoutineEngine` - programare si executie de rutina.
- `GuiService` si `GuiSessionManager` - coordonare UI.
- `AIOrchestrationService` si `OpenAIService` - orchestration si integrare AI.
- `DatabaseManager` - persistenta si conexiuni.
- `QuestDirector` si `QuestAuthoringService` - directie si authoring quest.
- `QuestAnchorResolver` - rezolvare ancore de progres.
- `NPCManager` si `ConversationSessionManager` - gestionare NPC si conversatii.
- `ListenerRegistry` - inregistrarea listenerelor runtime.
- `DebugDumpService` si `WorldMappingSemanticIndex` - export si inspectie read-only.
- `SchedulerCoordinator` - coordonare task-uri de startup si runtime.

### `ainpc-scenario-medieval`

- `AINPCScenarioMedievalPlugin` - intrarea addonului.
- `MedievalScenarioAddon` - adaptorul de scenariu pentru contractele API.

## Subsisteme de lucru

### `world`

- `WorldAdminService` - CRUD si validare pentru regiuni, locuri si noduri.
- `NpcWorldBindingService` - salvare, citire, listare si stergere de binding-uri NPC↔world.
- `WorldContextSnapshotBuilder` - constructie de snapshot pentru runtime si debug.

### `quest`

- `QuestDirector` - decide ce continut de quest este relevant.
- `QuestAuthoringService` - analizeaza si rezuma definitii de quest.
- `QuestAnchorResolver` - transforma ancorele in referinte folosibile.

### `story`

- `StoryStateService` - persistenta pentru stare narativa si evenimente.
- `StoryContextService` - citire si constructie de context narativ.

### `ai`

- `AIOrchestrationService` - aplica politica de orchestration si decide fallback.
- `OpenAIService` - integrarea de baza cu providerul AI.
- `OpenAIPromptBuilder` si `OpenAIPromptSnapshotFactory` - constructie de prompt si snapshot.

### `gui`

- `GuiService` - deschide si coordoneaza ecranele.
- `GuiSessionManager` - urmareste sesiunile deschise per jucator.
- `GuiScreen` si `GuiRenderContext` - contractul si contextul de randare.

### `spawn` si `routine`

- `NpcSpawnOrchestrator` - orchestration pentru spawn, rollback si persistenta.
- `RoutineService` - ruleaza tick-ul de rutina si publica schimbari.
- `RoutineEngine` - motorul de executie pentru reguli si sloturi.

### `debug`

- `DebugDumpService` - creeaza dump-uri text si JSON fara mutatii.
- `WorldMappingSemanticIndex` - index pentru inspectie si raportare.

## Relatii intre pachete

### Stratul exterior

- `ro.ainpc.commands` si `ro.ainpc.gui` sunt intrari pentru operator si authoring.
- `ro.ainpc.listeners` conecteaza evenimentele Paper/Bukkit la runtime.
- `ro.ainpc.debug` produce iesiri read-only pentru inspectie.

### Stratul de orchestrare

- `ro.ainpc.engine` coordoneaza `story`, `progression`, `world`, `ai` si `gui`.
- `ro.ainpc.spawn` foloseste `world`, `npc` si `story` pentru populare si household-uri.
- `ro.ainpc.routine` foloseste `npc` si `world` pentru programarea activitatilor.

### Stratul de domeniu

- `ro.ainpc.world` este baza pentru regiuni, locuri, noduri, binding-uri si snapshot-uri.
- `ro.ainpc.npc` tine starea personajului si atributiile sale de baza.
- `ro.ainpc.story` tine contextul narativ si starea pe regiune/loc.
- `ro.ainpc.progression` tine progresul si ancorele de progres.

### Stratul de authoring

- `ro.ainpc.world.mapping` produce draft-uri si aplicari controlate.
- `ro.ainpc.world.patch` propune corectii, nu aplica mutatii directe fara validare.
- `ro.ainpc.world.scan` si `ro.ainpc.world.fixture` sunt utile pentru import si testare.

### Stratul runtime/support

- `ro.ainpc.platform`, `ro.ainpc.topology` si `ro.ainpc.bootstrap` tin de pornire si feature gating.
- `ro.ainpc.ai.orchestration` trebuie tratat ca strat de decizie, nu ca loc pentru mutatii de date.

## Reguli de lectura

1. Citeste intai `ainpc-api`.
2. Dupa aceea urmeaza `ainpc-core-plugin`.
3. Pentru continut medieval, inspecteaza `ainpc-scenario-medieval`.
4. Pentru extensii noi, depinde de `ainpc-api`, nu de pachetele interne ale pluginului.

## Note

- Aceasta harta este doar pentru orientare.
- Nu modifica fluxul runtime in functie de ea.
- Cand un pachet se muta, actualizeaza doar acest document si eventual README-ul relevant.
