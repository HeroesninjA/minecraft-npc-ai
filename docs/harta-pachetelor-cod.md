# Harta Pachetelor de Cod

Actualizat: 2026-06-29

Aceasta este o harta doar-documentatie pentru vibe coding.
Nu schimba codul runtime.
Scopul ei este sa arate rapid cum se leaga modulele, pachetele si straturile de responsabilitate.
Pentru orientare rapida, incepe cu [harta scurta](./harta-pachetelor-cod-scurta.md); pentru relatii intre clase, foloseste [harta claselor](./harta-clase-cod.md); pentru detaliu complet, foloseste aceasta pagina.

## Index rapid

- `Module` - relatia dintre proiecte si dependentele lor externe.
- `Flux intre module` - ordinea corecta de citire intre module.
- `Harta pe pachete` - rolul fiecarui pachet din fiecare modul.
- `Servicii si clase cheie` - intrarile runtime si serviciile expuse.
- `Subsisteme de lucru` - straturile de domeniu si orchestration.
- `Relatii intre clase cheie` - dependentele observabile intre clasele centrale.
- `Relatii intre pachete` - ierarhia de la exterior la authoring.
- `Reguli de lectura` - ordinea recomandata pentru explorare.
- `Note` - limitele documentului si cum se mentine.

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
- `NpcWorldBindingService` - salvare, citire, listare si stergere de binding-uri NPC->world.
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

### `environment`

- `EnvironmentContext` - data class cu timeOfDay (6 stari), weather (5 tipuri), season (4 anotimpuri), temperature (6 niveluri), lightLevel si specialEvents
- `EnvironmentEngine` - tick per world, getContext/getContextForLocation, registerSpecialEvent/clearSpecialEvent
- Integrat in `AINPCPlugin`, `NPCContext`, `StoryContextService`, `StoryGui`

## Relatii intre clase cheie

Relatiile de mai jos sunt observabile din constructori, campuri si apeluri directe. Nu sunt reguli de arhitectura obligatorii; sunt o harta de orientare pentru vibe coding.

### Fluxul principal

- `AINPCScenarioMedievalPlugin` porneste addonul si expune `MedievalScenarioAddon`.
- `AINPCPlatform` este punctul central pentru init si configuratie.
- `GuiService` foloseste `GuiSessionManager` si popularea de ecrane concrete.
- `RoutineService` foloseste `RoutineEngine` si publica evenimente prin `AINPCEventSource`.
- `NpcSpawnOrchestrator` foloseste `NpcSpawnPlan`, `HouseAllocationValidator` si managerii din `AINPCPlugin`.
- `StoryStateService` foloseste `DatabaseManager`, `Gson` si `AINPCEventSource`.
- `EnvironmentEngine` observa lumile prin `World.getFullTime()` si `World.hasStorm()`.
- `WorldAdminService` foloseste `MappingIndex` si expune date prin `WorldAdminApi`.
- `NpcWorldBindingService` foloseste `DatabaseManager` prin `StatementProvider`.

### Decizie si authoring

- `QuestDirector` consuma `QuestDirectorRequest` si produce `QuestDirectorDecision`.
- `QuestDirector` compara `ProgressionDefinition` cu semnalele din `StoryContextSnapshot`.
- `QuestAuthoringService` analizeaza definitii si rezuma warnings pentru authoring.
- `QuestAnchorResolver` este punctul de rezolvare pentru ancorele de progres.
- `AIOrchestrationService` consuma `AIOrchestrationRequest` si produce `AIOrchestrationResult`.
- `AIOrchestrationService` aplica `AIOrchestrationPolicy` in functie de `AIUseCase`.

### Persistenta si inspectie

- `StoryStateService` persista `RegionStoryState`, `PlaceStoryState` si evenimente.
- `WorldAdminService` scrie si citeste `WorldRegion`, `WorldPlace` si `WorldNode`.
- `NpcWorldBindingService` gestioneaza `NpcWorldBinding` si `StatementProvider`.
- `DebugDumpService` serializeaza continut pentru inspectie read-only.

### Detaliere `world` si `quest`

- `WorldContextSnapshotBuilder` cere `WorldAdminService` si produce `WorldContextSnapshot` pentru zona curenta.
- `WorldContextSnapshotBuilder` foloseste `AINPC` si lista de NPC-uri apropiate pentru contextul local.
- `StoryContextSnapshot` inglobeaza `WorldContextSnapshot` si starea persistenta de poveste.
- `QuestDirectorRequest` transporta `StoryContextSnapshot`, `List<ProgressionDefinition>` si blocajele de decizie.
- `QuestDirector` consuma `QuestDirectorRequest` si intoarce `QuestDirectorDecision`.
- `HouseAllocationValidator` consuma `WorldAdminApi`, `WorldPlaceInfo` si `WorldNodeInfo` pentru validarea alocarii.
- `WorldAdminService` alimenteaza `MappingIndex`, iar `MappingIndex` face lookup spatial pentru regiuni, locuri si noduri.

### Detaliere `ai` si `gui`

- `OpenAIPromptSnapshotFactory` construieste `PromptSnapshot` din `AINPC`, `DialogManager.DialogRequest`, `FamilyManager` si `TopologyConsensus`.
- `OpenAIPromptBuilder` consuma `PromptSnapshot`, `DialogHistory`, `NPCRelationship` si contextul DB pentru promptul final.
- `AIOrchestrationService` aplica `AIOrchestrationPolicy` si produce `AIOrchestrationResult` pornind de la `AIOrchestrationRequest`.
- `GuiService` coordoneaza `GuiSessionManager`, `GuiScreen` si ecranele concrete din `ro.ainpc.gui.screens`.
- `GuiSessionManager` tine `GuiSession` si maparea per jucator.
- `MainHubGui` deschide fluxurile principale de lucru: quest, interactiune, world, stats, routine, story, manager, audit, debug si authoring.
- `QuestDetailGui` consuma `ProgressionGuiSnapshot`, `ProgressionGuiEntry` si `ProgressionAnchorBinding` pentru detalierea progresiei.
- `GuiRenderContext` este contextul comun pe care il consuma toate ecranele pentru iteme, butoane si navigare.
- `GuiScreen` defineste contractul minim pentru randarea unui ecran.

### Detaliere `spawn` si `routine`

- `NpcSpawnOrchestrator` consuma `NpcSpawnPlan` si creeaza/reutilizeaza NPC-uri prin `NPCManager`.
- `NpcSpawnOrchestrator` foloseste `HouseAllocationValidator` pentru validarea locuintei, rezidentilor si ancorelor de world.
- `HouseAllocation` transforma alocarea unei case in `NpcSpawnPlan`, `FamilyBindingPlan` si metadata pentru place.
- `HouseAllocationValidator` valideaza `HouseAllocation` prin `WorldAdminApi`, `WorldPlaceInfo` si `WorldNodeInfo`.
- `SpawnBatchTracker` urmareste batch-uri de spawn, pasi de household si rollback pentru NPC-urile create.
- `HouseholdPersistenceService` persista household-uri si rezidenti prin `HouseholdPersistenceServiceState`.
- `NpcSpawnOrchestrator` foloseste `FamilyManager` pentru `FamilyBindingPlan` si `HouseholdPersistenceService` pentru rezidenti persistati.
- `RoutineService` parcurge NPC-urile din `NPCManager`, cere `RoutineEngine.assign` si aplica starea de rutina.
- `RoutineEngine` produce `RoutineAssignment` si `RoutineSlot` pe baza timpului lumii si a ancorelor NPC.
- `RoutineService` publica schimbari prin `AINPCRoutineChangedEvent` si `AINPCEventSource`.

### Detaliere `commands`, `listeners`, `platform` si `database`

- `AINPCPlugin` este bootstrap-ul principal: creeaza `AINPCPlatform`, `DatabaseManager`, servicii, manageri, motoare, `GuiService`, `MappingWandService`, comenzi, listenere si scheduler.
- `AINPCPlugin` publica `AINPCPlatformApi` prin `server.servicesManager`, deci addonurile externe ar trebui sa consume API-ul, nu implementarea interna.
- `AINPCPlatform` compune `AddonRegistry`, `WorldAdminService`, `RuntimeFeatureResolver` si `PlatformProfile`.
- `AINPCPlatform.initialize` reincarca profilul, configureaza addonurile, reincarca world admin si inregistreaza descriptorul core.
- `AddonRegistry` tine `AddonDescriptor`, `AINPCAddon`, ordinea de incarcare si filtrarea addonurilor dezactivate.
- `DatabaseManager` este gateway-ul unic pentru conexiuni, schema, statement-uri si executie async.
- `AINPCCommand` este routerul operational pentru `/ainpc` si aliasuri: quest, world, progression, audit, debug, population, migration si repair.
- `AINPCTabCompleter` completeaza suprafata de comenzi legata de acelasi `AINPCPlugin`.
- `ListenerRegistry` inregistreaza listener-ele Paper: interactiune NPC, chat NPC, obiective quest, join player, lifecycle villager, mapping wand si GUI inventory.
- `NPCInteractionListener` leaga click-ul pe villager de `NPCManager`, `DialogManager`, `ScenarioEngine` si evenimentele publice de interactiune/dialog.
- `MappingWandListener` leaga click-ul pe block de `MappingWandService`, `MappingPoint` si modurile `MappingWandMode`.
- `RecentEventsBuffer` este initializat din `ListenerRegistry` pentru inspectie si debug recent.

### Detaliere `progression`, `story`, `dialog`, `npc` si `authoring`

- `ProgressionService` este centrul pentru definitii, progresii stocate, ancore si snapshot-uri GUI.
- `ProgressionService` consuma `ProgressionRepository`, `ScenarioEngine` si `ProgressionDefinition` ca sa construiasca statusul de progres.
- `StoredProgression` este forma persistata pentru progresie si expune selectorul, statusul si randarea chat.
- `StoryContextService` construieste `StoryContextSnapshot` din `WorldContextSnapshot`, ancore active, stare persistenta si evenimente recente.
- `StoryContextService` publica rezultate prin `AINPCEventSource` pentru a tine contextul narativ sincronizat.
- `DialogManager` gestioneaza istoric, relatie NPC-jucator, emotii si request-ul AI pentru dialog.
- `DialogManager` construieste cererea catre AI prin `publishAiRequestBuilt` si foloseste `PromptSnapshot` / `DialogContext` ca structuri de lucru.
- `NPCManager` este registrul operational al NPC-urilor si coordoneaza incarcarea, reconcilierea, profilurile si mapping-ul sursa->NPC.
- `NPCManager` alimenteaza `DialogManager`, `RoutineService`, `NpcSpawnOrchestrator` si listener-ele de interactiune.
- `MappingWandService` construieste drafturi de authoring pentru world mapping, NPC bind si quest anchor.
- `MappingWandService` pastreaza sesiuni pe jucator si folosește `MappingWandDraftFactory` pentru preview si confirmare.
- `SemanticVillageMapper` traduce scan-uri de sat in regiuni, case, ferme si noduri de lucru.

### Detaliere `api` si `addons`

- `AINPCPlatformApi` este contractul public prin care addonurile acceseaza `runtimeMode`, `worldMode`, `defaultStoryMode`, `addonRegistry`, `worldAdmin` si directoarele de date.
- `WorldAdminApi` este contractul public pentru citire si lookup in world admin.
- `AddonRegistryApi` este contractul public pentru inregistrarea si interogarea addonurilor.
- `AINPCAddon` defineste ciclul de viata public al unui addon: `getDescriptor`, `onLoad`, `onEnable`, `onDisable`.
- `AddonDescriptor` descrie addonul prin `supportedRuntimeModes`, `capabilities` si `dependencies`.
- `AINPCPlatform.registerCoreDescriptor` inregistreaza descriptorul nucleului si face vizibile capabilitatile core.
- `AINPCPlatform.reloadContent` delega reload-ul catre plugin pentru a pastra API-ul curat.
- `AddonRegistry.registerAddon` valideaza descriptorul, ruleaza `onLoad`, inregistreaza descriptorul si ruleaza `onEnable`.
- `AINPCScenarioMedievalPlugin` consuma serviciul API expus de core, inregistreaza `MedievalScenarioAddon` si sincronizeaza managed pack-urile.
- `AINPCScenarioMedievalPlugin` face `unregisterAddon` la oprire pentru a curata starea din registry.
- `MedievalScenarioAddon` este implementarea concreta a addonului medieval si expune descriptorul sau.

### Detaliere `debug` si `audit`

- `DebugDumpService` produce dump-uri text si JSON pentru inspectie read-only si poate include informatii OpenAI si log recent.
- `RecentEventsBuffer` pastreaza ultimele evenimente relevante pentru debug si expunere operationala.
- `DebugGui` deschide actiuni pentru dump-uri si inspectie din interfata.
- `AINPCCommand.handleDebugDump` si `handleAudit` reunesc inspectarea de world, story, AI, mapping si date operationale.
- `AuditReport` colecteaza `errors`, `warnings` si `infos` pentru rapoarte de audit.
- `VanillaVillageScanner` si `VillagePatchPlanner` alimenteaza auditul de world mapping si gap planning.
- `VillagePatchPlanner` transforma scan-urile si candidatele in planuri cu risc, cost si capabilitati cerute.

### Detaliere `world.scan` si `world.patch`

- `VanillaVillageScanner` produce `VanillaVillageScanResult` dintr-o locatie centrata pe un sat vanilla.
- `VanillaVillageScanResult` expune semnalele brute: `features`, `bells`, `beds`, `workstations`, `doors` si `farmlands`.
- `SemanticVillageMapper` consuma `VanillaVillageScanResult` si creeaza `SemanticVillageImportResult` prin `WorldAdminService`.
- `SemanticVillageMapper` creeaza regiuni, case, ferme si workplace-uri semantice, plus nodurile aferente.
- `SemanticVillageImportResult` rezuma ce place-uri si node-uri au fost create si ce warnings/errors au aparut.
- `VillageGapAnalyzer` consuma `WorldAdminApi` si produce `GapReport` pentru regiune selectata si optiuni de patch.
- `GapReport` agregheaza capacitate, lipsuri, gaps si warnings pentru o regiune.
- `VillagePatchPlanner` transforma `GapReport` si `PatchPlannerOptions` in `PatchPlannerResult`.
- `PatchPlannerResult` expune candidatele, planurile, warnings si errors pentru corectii propuse.
- `PatchCandidate` si `PatchPlan` reprezinta nivelul intermediar si final al planificarii de patch.
- `VillageGap` descrie o lipsa concreta care alimenteaza `VillageGapAnalyzer` si `VillagePatchPlanner`.

### Detaliere `world.exterior`, `world.fixture` si `topology`

- `ExteriorStructureAnalyzer` clasifica regiuni exterioare si valideaza semnalele comune, tipurile specifice si node-urile cheie.
- `ExteriorStructureBlueprintCatalog` ofera catalogul de blueprints pentru planificarea exterioara.
- `ExteriorStructurePlanner` transforma analiza in `ExteriorStructurePlan`.
- `ExteriorStructurePlan` expune tipul, place-urile, node-urile si warnings pentru structura exterioara planificata.
- `ExteriorStructureReport` rezuma rezultatul analizei pentru o regiune exterioara.
- `ControlledTestWorldFixturePlanner` construieste `ControlledTestWorldFixturePlan` pentru world-uri de test controlate.
- `ControlledTestWorldFixturePlan` expune regiunea de sat, regiunile exterioare, locurile si node-urile planificate.
- `ControlledTestWorldFixtureValidator` produce `ControlledTestWorldFixtureValidationReport` pentru verificarea fixture-ului.
- `ControlledTestWorldFixtureApplier` transforma planul in `ControlledTestWorldFixtureApplyResult` si creeaza regiunile, place-urile si node-urile.
- `ControlledTestWorldFixturePopulator` produce `ControlledTestWorldFixturePopulateResult` si NPC-uri de fixture.
- `FixtureSemanticContextBuilder` construieste `FixtureSemanticContext` pentru semantica, tensiuni si zvonuri de fixture.
- `TopologyCategory` si `TopologyConsensus` descriu categoria topologica si blocul de consens folosit in prompturi si structurare.

### Detaliere `engine` si feature packs

- `FeaturePackLoader` incarca pack-urile si populeaza registrii de trait-uri, profesii, topologii, scenarii, mecanici de progresie si dialoguri.
- `FeaturePackLoader` construieste consensul de topologie si expune `FeaturePack`, `ScenarioDefinition`, `ProfessionDefinition` si `ProgressionMechanicDefinition`.
- `FeaturePackMetadataValidator` verifica metadata pack-urilor, runtime modes si tipul addonului.
- `FeaturePackDependencyValidator` verifica dependintele pack-urilor fata de setul disponibil.
- `FeaturePackSupport` deduce capabilitatile si tipul addonului din metadata.
- `FeaturePackYamlSupport` citeste YAML pentru trait-uri, profesii, topologii, dialoguri, scenarii si progresie.
- `FeaturePackDefaults` aduce pack-uri fallback neutre si topologii default.
- `DecisionEngine` evalueaza actiuni si simuleaza viata NPC-urilor prin scoruri, needs si routine.
- `DialogueEngine` selecteaza intentul, alege template-ul si poate reformula prin AI cand este permis.
- `ScenarioEngine` incarca template-uri de quest si scenarii, gestioneaza progresul si publica evenimente de progresie.
- `ScenarioEngine` este puntea dintre feature packs, quest tracking, story actions si active scenarios.

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
- `ro.ainpc.environment` tine contextul de mediu (timp, vreme, anotimp, temperatura) si engine-ul asociat.
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
