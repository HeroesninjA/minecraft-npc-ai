# Changelog

Toate schimbarile notabile ale proiectului trebuie documentate aici.

Formatul este orientat pe intrari scurte, verificabile:

- `Added` pentru functionalitate noua
- `Changed` pentru schimbari de comportament sau arhitectura
- `Fixed` pentru bugfix-uri
- `Docs` pentru documentatie
- `Tests` pentru verificari adaugate sau rulate

## [Unreleased]

### Added

- Mecanica medievala `npc_duties` si sarcina `D01 - Rondul Strajerului`, cu `base_type: DUTY`, progres `kind=duty`, obiective mapate si story event regional.
- `ScenarioType.DUTY`, `QuestScenarioContract.Kind.DUTY` si fatadele `/ainpc duty ...` / `/duty ...` pentru inspectarea sarcinilor NPC prin runtime-ul comun.
- Mecanica medievala `local_bounties` si scenariul `B01 - Recompensa Drumului Vechi`, cu `base_type: BOUNTY`, progres `kind=bounty`, obiectiv `kill_mob` si story event regional.
- Scenariul `B02 - Panza de la Marginea Fermei`, ca al doilea bounty `local_bounties`, cu giver `farmer`, ancora `tag:farm`, cooldown si reward proprii.
- `ScenarioType.BOUNTY` si fatada `/ainpc bounty ...` / `/bounty ...` pentru bounty-uri locale peste runtime-ul comun.
- Mecanica medievala `village_events` si scenariul `E01 - Alarma Fantanii din Piata`, cu `base_type: WORLD_EVENT`, progres `kind=event`, obiective mapate si story event pe place.
- `ScenarioType.WORLD_EVENT`, `QuestScenarioContract.Kind.EVENT` si fatadele `/ainpc event ...` / `/event ...` pentru evenimente locale peste runtime-ul comun.
- Mecanica medievala `onboarding` si scenariul `T01 - Indrumarea Avizierului`, cu `base_type: TUTORIAL`, progres `kind=tutorial`, obiective mapate si story event pe place.
- `ScenarioType.TUTORIAL`, `QuestScenarioContract.Kind.TUTORIAL` si fatadele `/ainpc tutorial ...` / `/tutorial ...` pentru tutoriale peste runtime-ul comun.
- Mecanica medievala `village_rituals` si scenariul `R01 - Luminile Vechiului Altar`, cu `base_type: RITUAL`, progres `kind=ritual`, obiective mapate pe altar si story event pe place.
- `ScenarioType.RITUAL`, `QuestScenarioContract.Kind.RITUAL` si fatadele `/ainpc ritual ...` / `/ritual ...` pentru ritualuri locale peste runtime-ul comun.
- `QuestLogGuiFilter` si filtre interactive in `QuestLogGui` pentru `all`, `active`, `quest`, `contract`, `duty`, `bounty`, `event`, `tutorial` si `ritual`.
- `QuestLogGuiPage` pentru gruparea randurilor dupa mecanica si paginarea testabila a quest/progression log-ului.
- Contract medieval `C02 - Avizierul Pietei`, expus prin mecanica `village_contracts`, cu obiective `visit_place`, `inspect_node` si story event pe place.
- `QuestScenarioContract.Kind.INVESTIGATION` pentru contracte/questuri de investigatie, inclusiv inferenta din `inspect_node`.
- Metadata `category`, `scenarioKind` si `baseType` in `StoredProgression`, exportata in `player-progressions.json` si folosita de filtrele read-only.
- Persistenta story initiala prin tabelele DB `region_story_state`, `place_story_state` si `story_events`.
- `StoryStateService` pentru citirea/scrierea starii story pe regiune/place si pentru inregistrarea evenimentelor story.
- Modele runtime pentru story persistent: `RegionStoryState`, `PlaceStoryState` si `StoryEvent`.
- Integrare initiala a story state-ului persistent in `StoryContextSnapshot` si in blocul `STORY_CONTEXT`.
- Comenzi read-only pentru story state persistent: `/ainpc story region <regionId>`, `/ainpc story place <placeId>` si `/ainpc story events <regionId|placeId> [limit]`.
- Actiuni de quest `set_story_state` si `record_story_event`, executate controlat la finalizarea questului.
- `QuestEntryDefinition` pastreaza acum metadata, `variables` si `payload` din YAML pentru actiuni non-item.
- `VillageGapAnalyzer` read-only pentru capacitate, case/paturi, workplace-uri cerute, social hub, quest trigger si node-uri lipsa in mapping.
- `VillagePatchPlanner` read-only pentru transformarea gap-urilor in `PatchCandidate` si `PatchPlan`, cu prioritate, cost, risc, build mode si validare de capabilitati.
- Modele initiale pentru patch planner: `GapReport`, `VillageGap`, `PatchCandidate`, `PatchPlan`, `PatchPlannerResult`, `PatchPlannerOptions`, `PatchGapType`, `PatchType`, `PatchBuildMode` si `PatchValidationStatus`.
- Comanda admin read-only `/ainpc patch analyze|plan|validate <regionId> [targetPopulation] [profesiiCSV]`.
- `QuestDirector` read-only cu `QuestDirectorRequest` si `QuestDirectorDecision`, pentru decizii `no_action`, `candidate_found`, `seed_suggested` sau `blocked` fara executie de progres.

### Changed

- Filtrele de quest/progression accepta `duty`, `kind:duty`, `base:DUTY` si `mechanic:npc_duties` pentru definitii si progresii persistate.
- Filtrele de quest/progression accepta `bounty`, `kind:bounty`, `base:BOUNTY` si `mechanic:local_bounties` pentru definitii si progresii persistate.
- Filtrele de quest/progression accepta `event`, `kind:event`, `base:WORLD_EVENT` si `mechanic:village_events` pentru definitii si progresii persistate.
- Filtrele de quest/progression accepta `tutorial`, `kind:tutorial`, `base:TUTORIAL` si `mechanic:onboarding` pentru definitii si progresii persistate.
- Filtrele de quest/progression accepta `ritual`, `kind:ritual`, `base:RITUAL` si `mechanic:village_rituals` pentru definitii si progresii persistate.
- Aliasurile `/ainpc contract nearest|accept|decline`, `/ainpc duty nearest|accept|decline`, `/ainpc bounty nearest|accept|decline`, `/ainpc event nearest|accept|decline`, `/ainpc tutorial nearest|accept|decline` si `/ainpc ritual nearest|accept|decline` folosesc acum selectie kind-aware, ca sa nu aleaga accidental un quest de alt tip al aceluiasi NPC.
- Rutarea pentru aliasurile de mecanici (`contract`, `duty`, `bounty`, `event`, `tutorial`, `ritual`) foloseste acum un helper comun bazat pe `progressionKind`.
- `/ainpc gui quest <filter>` si `/quest gui <filter>` deschid direct quest log-ul filtrat, iar `GuiService` pastreaza filtrul ales per jucator.
- `QuestLogGui` afiseaza headers de mecanica, controleaza pagina anterioara/urmatoare si pastreaza pagina curenta per jucator in `GuiService`.
- Villagerii existenti convertiti in AINPC primesc aceleasi setari controlate ca NPC-urile spawnate de plugin: AI vanilla oprit, fara coliziune, invulnerabili si persistenti.
- Rutina NPC muta mai rar NPC-urile intre ancore, folosind cooldown, raza de sosire mai mare si corectie doar la schimbare de slot sau distanta mare.
- Demo mapping-ul `/ainpc world demo create` foloseste un layout mai spatios pentru case, piata, locuri de munca si altar, cu warning pentru alegerea unei zone plate.
- `HouseAllocationPlanner` prefera nodurile `WORK`/`work_anchor` pentru ancora de munca inaintea nodurilor `WORKSTATION`.
- `NpcInteractionGui` afiseaza rutina NPC-ului si expune actiuni rapide mai clare pentru quest nearest, accept nearest, story si rutina.
- `AINPCPlugin` initializeaza si expune `StoryStateService` prin `getStoryStateService()`.
- `StoryContextService` include, cand exista mapping curent, starea persistenta a regiunii/place-ului si evenimentele story recente.
- Tab completion-ul pentru `/ainpc story` include acum `context`, `region`, `place` si `events`.
- Verificarea si acordarea recompenselor pe iteme ignora actiunile story, ca sa nu fie tratate ca materiale Minecraft invalide.
- Tab completion pentru filtrele `investigation` la comenzile de definitions/stored.
- `loaded-quest-definitions.json` include metadata derivata pentru obiective: tip normalizat, tip de ancora semantica si referinta prefix/valoare.
- Filtrarea `progression definitions` si `progression stored` foloseste un `ProgressionFilter` comun, cu suport pentru filtre explicite precum `scenario:investigation`, `base:TRADE_DEAL` si `mechanic:village_contracts`.
- `StoredProgressionSummary`, auditul si `player-progressions.json` agrega acum si dupa `category`, `scenarioKind` si `baseType`.
- `world-mapping.json` include un `semantic_index` pentru tokenii folositi de resolverul de ancore, inclusiv tag-uri de place si metadata de node.
- `/ainpc audit quest` foloseste `semantic_index` ca sa avertizeze cand obiectivele `visit_region`, `visit_place` sau `inspect_node` cer tokeni care lipsesc din mapping-ul curent.
- `quest-audit-report.txt` din `/ainpc debugdump quest` valideaza aceiasi candidati jucabili si foloseste acelasi `semantic_index`, inclusiv pentru contracte non-`QUEST`.
- `/ainpc audit quest strict` verifica toate randurile din `quest_anchor_bindings`, iar auditul valideaza `objective_key` fata de definitia progresiei incarcate.

### Docs

- Actualizat smoke script-ul Paper si documentele de progres pentru slice-ul D01 `npc_duties`.
- Actualizat smoke script-ul Paper si documentele de progres pentru slice-ul B01 `local_bounties`.
- Actualizat smoke script-ul Paper si documentele de progres pentru slice-ul B02 `local_bounties`.
- Actualizat smoke script-ul Paper si documentele de progres pentru slice-ul E01 `village_events`.
- Actualizat smoke script-ul Paper si documentele de progres pentru slice-ul T01 `onboarding`.
- Actualizat smoke script-ul Paper si documentele de progres pentru slice-ul R01 `village_rituals`.
- Actualizat `docs/gui-interfete.md`, `docs/implementat-deja.md` si `TODO.md` pentru filtrele interactive din Quest GUI.
- Actualizat `docs/gui-interfete.md`, `docs/implementat-deja.md`, `docs/faze-urmatoare-categorii.md` si `TODO.md` pentru paginarea si gruparea din Quest GUI.
- Creat `docs/playable-village-ux.md` si actualizate indexurile pentru pass-ul initial de playable village: layout demo mai spatios, rutina mai stabila, interactiuni NPC mai clare si criterii pentru sat jucabil.
- Documentata directia pentru mapping wand + prompturi naturale in `docs/mapping-harti-manuale.md`, cu flux draft, preview si confirmare pentru `region`/`place`/`node`, NPC bind si quest anchors.
- Arhivat `docs/questuri-avansate.md` in `docs/arhiva/questuri-avansate-v1.md` si creat `docs/questuri-avansate-v2.md` cu faze pentru diversitate de questuri, mapping semantic, mecanici non-quest si `ProgressionService`.
- Actualizat `TODO.md`, `docs/progression-service.md` si `docs/lucru-alternat-quest-mapping-progression.md` pentru slice-ul C02 mapping/contract/progression.
- Actualizat `TODO.md` pentru a muta persistenta story initiala la functionalitati existente.
- Actualizate documentele de stare pentru story: `docs/story-context-service.md`, `docs/story-si-context-ai.md`, `docs/implementat-deja.md`, `docs/faze-observatii-avertizari.md`, `docs/README.md` si `docs/categorii/03-quest-story-ai/README.md`.
- Actualizat `docs/quest-anchor-bindings.md`, `docs/lucru-alternat-quest-mapping-progression.md` si `TODO.md` pentru auditul strict al anchor bindings.
- Actualizat `docs/patch-planner.md`, indexurile de documentatie, `docs/implementat-deja.md`, `docs/generare-sate-fara-worldedit.md`, `docs/ordine-spawn-npc-cladiri-region-node.md` si `TODO.md` pentru implementarea initiala read-only a Patch Planner.
- Creat `docs/generare-automata-questuri-ai.md` si actualizate indexurile pentru contractul `QuestSeed`/`QuestDraft`, validare, review admin si export YAML dezactivat.
- Documentata regula story-driven quest selection: story-ul decide directia prin context/`QuestDirector`, iar `ProgressionService` ramane proprietarul executiei questului.
- Extins `docs/lucru-alternat-quest-mapping-progression.md` si `docs/implementat-deja.md` cu patch planner read-only, story vs quest independent, `QuestDirector`, `QuestDraft` AI, sablon de slice, contracte GUI/debug, exemple concrete, raport standard de smoke si fazele F101-F120.
- Adaugat in `docs/debugging-si-testare.md` smoke test-ul read-only pentru Patch Planner si raportul standard pentru `/ainpc patch analyze|plan|validate`.
- Documentata integrarea fara conflict intre `WorldContext`, `StoryContext`, `MemoryContext` si `ProgressionContext`, inclusiv proprietari unici si prioritatea `Progression > Story > Memory > AI text`.
- Actualizat protocolul de lucru alternat ca story-ul sa fie fir principal: `Quest, Mapping, Story, ProgressionService si GUI`, cu pas dedicat pentru story baseline peste mapping si fazele F121-F135 pentru story lane.
- Adaugat in `docs/debugging-si-testare.md` smoke test-ul Story Lane pentru `story_only`, `quest_only` si `writes_story`, inclusiv cazul `blocked` cand lipseste writer sigur pentru story fara quest.
- Extins auditul quest/debugdump cu verificare initiala pentru contradictii story/progression: progresie completata cu `record_story_event`, dar fara `story_event` asociat detectabil prin payload.
- Extins `story-events.json` din debugdump cu `progression_link` pentru legatura story event -> progresie/scenariu/action `record_story_event`, cand legatura este unica.
- Adaugat `StoryGui` read-only pentru `/ainpc gui story`, cu region state, place state, story events recente si fallback text catre comenzile `/ainpc story ...`.
- Documentata matricea de permisiuni story: `read_only`, `write_admin` si `write_runtime`, cu regula ca AI-ul si GUI-ul story nu scriu direct in persistenta.
- Intarita validarea pack-urilor pentru story actions: scope `region`/`place`, target, `event_key` obligatoriu si payload minim pentru `record_story_event`.
- Extins `QuestDirectorTest` cu caz story demand + preferinta de mecanica, confirmand ca selectia ramane read-only (`runtimeExecutable=false`).
- Adaugate teste pentru story local fara NPC tinta, story regional si blocaj `QuestDirector` cu motiv story condition lipsa.
- Extins raportul `Smoke Test Story Lane` cu verificarea prioritatii `Progression > Story > Memory > AI text` si camp pentru conflicte de context.

### Tests

- Extinse testele pentru `DUTY`, `duty` selectors/filters si scenariul medieval D01.
- Adaugat test dedicat pentru selectia kind-aware a template-urilor mixte `contract`/`duty`.
- Extinse testele pentru `BOUNTY`, `bounty` selectors/filters si scenariul medieval B01.
- Extinse testele pentru al doilea bounty `B02`, inclusiv ancora `tag:farm` pe mapping-ul demo.
- Extinse testele pentru `WORLD_EVENT`, `event` selectors/filters si scenariul medieval E01.
- Extinse testele pentru `TUTORIAL`, `tutorial` selectors/filters si scenariul medieval T01.
- Extinse testele pentru `RITUAL`, `ritual` selectors/filters, ancorele `tag:ritual`/`ritual_circle` si scenariul medieval R01.
- Adaugat `QuestLogGuiFilterTest` pentru normalizarea filtrelor GUI si ordinea stabila a randului de filtre.
- Adaugat `QuestLogGuiPageTest` pentru gruparea dupa mecanica, clamp-ul paginii si cazul listei goale.
- Extins `WorldAdminServiceTest` pentru layout-ul demo mai spatios si warning-ul de zona plata.
- Adaugat test pentru rezolvarea ancorelor de contract mapat pe `tag:market` si `quest_board`.
- Adaugate teste pentru `investigation` in contractul runtime si in `ProgressionDefinition.scenarioKind()`.
- Adaugat `ProgressionFilterTest` pentru filtre explicite pe definitii si progresii persistate.
- Adaugat test pentru indexul semantic din world mapping pe cazul `market`/`quest_board`.
- Extins `ProgressionRepositoryTest` pentru metadata si filtre `scenarioKind`/`baseType`.
- Extins `AINPCTabCompleterTest` pentru optiunile `/ainpc audit quest strict|full`.
- Adaugat `VillagePatchPlannerTest` pentru gap analyzer, planificare read-only si mapping-ul demo cu populatie initiala jucabila.
- Extins `AINPCTabCompleterTest` pentru `/ainpc patch analyze|plan|validate`.
- Adaugat `QuestDirectorTest` pentru selectie determinista story-driven, blocaje, seed suggestion si garantia ca decizia nu este executabila runtime.
- Rulat `mvn -pl ainpc-core-plugin -am "-Dtest=QuestDirectorTest,VillagePatchPlannerTest,AINPCTabCompleterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`.
- Rezultat focalizat: `21` teste trecute, `0` esecuri.

### Remaining

- Audit/debugdump dedicat pentru story state si story events.
- Validator dedicat pentru actiunile story din feature packs.
