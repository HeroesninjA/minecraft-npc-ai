# TODO Caracteristici

Actualizat: 2026-05-25

## Exista deja

- [x] NPC-uri AI bazate pe `Villager`
- [x] Dialog contextual cu memorie, emotii si relatie cu jucatorul
- [x] Comenzi administrative prin `/ainpc`, `/npc` si `/ai`
- [x] Pachete tematice configurabile prin YAML
- [x] Modularizare initiala in `ainpc-api`, `ainpc-core-plugin` si `ainpc-scenario-medieval`
- [x] Scenariu medieval separat ca addon de continut
- [x] Sursele si resursele core sunt mutate in `ainpc-core-plugin/src/main`
- [x] Task-urile programate au fost extrase din `AINPCPlugin` in `SchedulerCoordinator`
- [x] Folderul legacy `src/src` a fost eliminat dupa migrarea surselor in modulul core
- [x] Mapping-ul are auto-indexare interna dezactivabila prin `world_admin.auto_index.enabled`
- [x] Mapping-ul are lookup initial pentru `findNode(...)` si `findNodesNear(...)`
- [x] `WorldContextSnapshot` initial pentru legatura mapping -> NPCContext -> prompt AI
- [x] Obiective quest initiale `visit_place` si `inspect_node`
- [x] `QuestAnchorResolver` initial cu ancore persistate si reflectate in `questVariables`
- [x] `objective_id` stabil initial pentru progres si quest anchor bindings noi
- [x] Persistenta dedicata `quest_anchor_bindings` pentru ancore semantice de quest
- [x] Audit si comanda admin read-only pentru `quest_anchor_bindings`
- [x] `/ainpc audit quest` valideaza initial si quest templates din feature packs
- [x] `/ainpc audit quest` raporteaza strict tipuri necunoscute de obiective/reward-uri si story actions incomplete
- [x] `/ainpc audit quest` raporteaza sumar generic pentru progresiile persistate prin `StoredProgressionSummary`
- [x] Teste de contract runtime pentru Q01-Q05 din addonul medieval
- [x] Q06 medieval initial cu `visit_place`, `inspect_node`, `talk_to_npc` si `record_story_event`
- [x] Q07 medieval initial cu `collect_item`, `talk_to_npc`, `deliver_to_npc` si `record_story_event`
- [x] Q08 medieval initial cu `visit_region`, `kill_mob`, `talk_to_npc` si `record_story_event`
- [x] Selector quest per jucator pentru NPC-uri care pot oferi mai multe questuri in lant
- [x] Runtime initial multi-quest pe jucator: progres curent per template si update-uri de obiective peste toate questurile active
- [x] Selector explicit pentru `/ainpc quest status <questCode|templateId>` si `/ainpc quest track [start] <questCode|templateId>`
- [x] Selector explicit pentru `/ainpc quest abandon tracked|<questCode|templateId>`
- [x] Comanda admin read-only `/ainpc quest debug <tracked|questCode|templateId>` pentru progres si variabile persistente
- [x] `quest log` marcheaza questul urmarit cand tracking-ul persistent are selector explicit
- [x] Filtre initiale pentru `quest log`: active/current/tracked/main/side/repeatable/completed/failed/archived/all
- [x] `quest log` prioritizeaza tracked -> main -> side -> repeatable si afiseaza sumar curent pe status/categorii
- [x] `quest log` afiseaza actiuni rapide pentru status, track, abandon si debug admin
- [x] `quest log` grupeaza vizual questurile curente in tracked/main/side/repeatable/template lipsa
- [x] Categorii quest `main`, `side`, `repeatable` in contract si limite initiale prin `quest.max_active`
- [x] Stages runtime initial: obiectivele cu `phase`/`stage` sunt evaluate doar in etapa curenta, cu `player_quests.current_stage_id` persistat si fallback plat pentru questurile vechi
- [x] Persistenta pentru quest tracked prin `player_quests.tracked`, restaurata la load
- [x] `/ainpc audit quest` valideaza unicitatea si statusul activ pentru `player_quests.tracked`
- [x] `/ainpc audit quest` avertizeaza pentru chei legacy in `objective_progress` si `quest_anchor_bindings.objective_key`
- [x] Raport dedicat `quest-audit-report.txt` in `/ainpc debugdump quest`
- [x] Export/debugdump pentru quest templates incarcate in `loaded-quest-definitions.json`
- [x] `loaded-quest-definitions.json` expune pentru obiective `normalized_type`, `semantic_anchor_type`, `semantic_reference_prefix` si `semantic_reference_value`
- [x] `/ainpc audit quest` verifica referintele semantice de obiective fata de `world-mapping.semantic_index`
- [x] `quest-audit-report.txt` verifica aceleasi referinte semantice si include contractele/progresiile non-`QUEST`
- [x] Export/debugdump complet pentru `player_quests` in `player-quest-progress.json`
- [x] Export/debugdump generic pentru progresii in `player-progressions.json`, ca view peste `player_quests` cu metadata de mecanica
- [x] `world-mapping.json` expune `semantic_index` pentru tokeni de resolver: place tags/types si node type/metadata
- [x] Export/debugdump pentru `region_story_state` si `place_story_state` in `story-states.json`
- [x] Export/debugdump pentru `story_events` in `story-events.json`
- [x] `/ainpc debugdump story` pentru inspectie dedicata story state/events
- [x] Metadata initiala pentru `ProgressionService`: feature packs pot declara `mechanics`, iar scenariile pot seta `mechanic`/`progress`
- [x] `ProgressionService` initial ca strat de rutare peste runtime-ul existent din `ScenarioEngine`
- [x] `ProgressionSelector` initial pentru selectori simpli, `tracked/current`, `mechanic:definition` si `pack:mechanic:definition`
- [x] `ProgressionDefinition` initial ca model read-only peste definitiile jucabile din feature packs
- [x] Snapshot-uri initiale `ProgressionStatusSnapshot`, `ProgressionProgressSnapshot`, `ProgressionGuiSnapshot`, `ProgressionGuiEntry` si `ProgressionStageSnapshot` peste rezultatele compatibile din runtime-ul curent
- [x] Quest GUI foloseste snapshot-ul generic de progres si are filtre interactive pentru `all`/`active`/`quest`/`contract`/`duty`/`bounty`/`event`/`tutorial`/`ritual`
- [x] Quest GUI are paginare initiala si grupare dupa mecanica prin `QuestLogGuiPage`
- [x] `/ainpc gui quest <filter>` si `/quest gui <filter>` deschid direct quest log-ul filtrat
- [x] Quest Detail GUI afiseaza binding-urile persistate din `quest_anchor_bindings` pentru progresia selectata, marcheaza ancora pe fiecare obiectiv mapat si ofera shortcut admin catre `/ainpc quest anchors`
- [x] `/ainpc quest anchors` accepta filtrare dupa `template_id` sau `quest_code`, ca diagnosticul text sa corespunda cu detaliile din GUI
- [x] `ProgressionRepository` read-only peste `player_quests`, expus prin `ProgressionService.getStoredProgressions()`
- [x] `StoredProgression` expune metadata generica `category`, `scenarioKind` si `baseType` pentru filtrare/debugdump peste progresii persistate
- [x] `ProgressionFilter` comun pentru filtre explicite `kind:`, `scenario:`, `base:`, `category:`, `mechanic:` in definitions si stored progressions
- [x] `StoredProgressionSummary` pentru sumar read-only peste progresiile persistate, inclusiv category/scenarioKind/baseType
- [x] `ProgressionRepository` citeste `quest_anchor_bindings` pentru o progresie dupa `template_id`, cu fallback controlat dupa `quest_code`
- [x] Comanda admin read-only `/ainpc progression stored [jucator|uuid|all] [filter] [limit]` si aliasuri filtrate `/ainpc contract|duty|bounty|event|tutorial|ritual stored ...`
- [x] Comanda read-only `/ainpc progression definitions [filter]` si aliasuri filtrate `/ainpc contract|duty|bounty|event|tutorial|ritual definitions [filter]`
- [x] Prima mecanica `npc_duties` peste runtime-ul comun, cu D01 ca sarcina NPC non-quest filtrabila prin `duty`
- [x] Prima mecanica `local_bounties` peste runtime-ul comun, cu B01 ca bounty local filtrabil prin `bounty`
- [x] Al doilea bounty `local_bounties`, B02, foloseste alt giver, `tag:farm`, cooldown si reward proprii peste aceeasi mecanica
- [x] Prima mecanica `village_events` peste runtime-ul comun, cu E01 ca eveniment local filtrabil prin `event`
- [x] Prima mecanica `onboarding` peste runtime-ul comun, cu T01 ca tutorial filtrabil prin `tutorial`
- [x] Prima mecanica `village_rituals` peste runtime-ul comun, cu R01 ca ritual local filtrabil prin `ritual`
- [x] `AIOrchestrationService` initial pentru politici AI, fallback determinist si rezultate care nu pot executa runtime direct
- [x] Document canonic initial pentru generarea automata de questuri cu AI prin `QuestSeed`/`QuestDraft`, validare si review admin
- [x] Contracte initiale pentru runtime extensibil: registri de actiuni/conditii/trigger-e, context de executie, definitii runtime, variable provider si raport de validare
- [x] Contractul runtime recunoaste `investigation` ca `QuestScenarioContract.Kind.INVESTIGATION`, inclusiv pentru snapshot-uri generice de progres
- [x] Availability aplica initial `max_active` si pe mecanici de progres declarate in addon
- [x] Addonul medieval are primul contract non-`QUEST` (`C01`) pe mecanica `village_contracts`
- [x] Addonul medieval are contractul mapat `C02`, care foloseste `visit_place tag:market`, `inspect_node quest_board`, story event pe place si runtime-ul generic de progres
- [x] `quest log` afiseaza/grupeaza dupa mecanica si are filtre initiale `quest`/`contract`/`duty`/`bounty`/`event`/`tutorial`/`ritual`
- [x] Selectorii de progres accepta forme cu mecanica, de exemplu `village_contracts:C01`, `npc_duties:D01`, `local_bounties:B01`, `local_bounties:B02`, `village_events:E01`, `onboarding:T01` si `village_rituals:R01`
- [x] Fatade initiale pentru progres generic: `/ainpc progression ...`, `/progression ...`, `/ainpc contract ...`, `/contract ...`, `/ainpc duty ...`, `/duty ...`, `/ainpc bounty ...`, `/bounty ...`, `/ainpc event ...`, `/event ...`, `/ainpc tutorial ...`, `/tutorial ...`, `/ainpc ritual ...`, `/ritual ...`
- [x] Script `scripts/smoke-paper-quests.ps1` pentru pregatirea smoke test-ului Paper pe questuri
- [x] Comenzi read-only `/ainpc demo status|next|definition|script|phases|evidence|runbook|smoke|summary|commands|restart [regionId] [player]` pentru readiness, blocaje, definitie demo functional, faze, dovezi, runbook, smoke check, lista compacta de comenzi, restart gate, sumar rapid si ghidul primului demo intern jucabil
- [x] Moduri experimentale read-only `/ainpc demo experimental|experimental5|experimental25|experimental25deep|experimental25ops [regionId] [player]` pentru analiza interna voluminoasa inainte de stabilizare API
- [x] `StoryContextService` initial si comanda `/ainpc story context`
- [x] `StoryStateService` initial cu `region_story_state`, `place_story_state` si `story_events`
- [x] Comenzi read-only `/ainpc story region`, `/ainpc story place` si `/ainpc story events`
- [x] Actiuni de quest `set_story_state` si `record_story_event`
- [x] Comanda `/ainpc world demo create [regionId]` pentru mapping demo minim in jurul jucatorului
- [x] Mapping-ul demo foloseste layout mai spatios pentru case/piata/workplaces/altar si avertizeaza ca trebuie aleasa o zona relativ plata
- [x] Comanda `/ainpc world bind npc ...` pentru legarea initiala NPC -> home/work/social places
- [x] Mapping wand initial: `/ainpc wand`, selectie pos1/pos2/punct, parser determinist si `/ainpc map preview|confirm|cancel` pentru `region`/`place`/`node`
- [x] Mapping wand poate crea si confirma draft-uri `npc_bind` pe roluri `home`/`work`/`social`, actualizand profilul NPC, metadata mapping si `npc_world_bindings`
- [x] Mapping wand poate crea si confirma draft-uri `quest_anchor`, cu context player/progresie/objective_id si upsert in `quest_anchor_bindings`
- [x] `scripts/smoke-paper-mapping.ps1` genereaza si checklist-ul manual pentru flux wand complet `region`/`place`/`node`/`npc_bind`/`quest_anchor`
- [x] Planner `/ainpc world household plan ...` pentru `HouseAllocation` din mapping
- [x] Spawn `/ainpc world household spawn ...` prin `NpcSpawnOrchestrator`
- [x] Planner `/ainpc world settlement plan ...` pentru toate casele dintr-o regiune
- [x] Spawn `/ainpc world settlement spawn ...` pentru household-uri secventiale pe regiune
- [x] Rollback global practic pentru `settlement spawn` daca un household ulterior esueaza
- [x] Patch planner read-only initial: `VillageGapAnalyzer`, `VillagePatchPlanner` si `/ainpc patch analyze|plan|validate`
- [x] Script `scripts/smoke-paper-mapping.ps1` pentru pregatirea smoke test-ului Paper mapping/spawn
- [x] Persistenta initiala `npc_world_bindings` pentru home/work/social place si node IDs
- [x] `/ainpc audit db` valideaza initial `npc_world_bindings`
- [x] Comanda read-only `/ainpc world bindings ...` pentru inspectie `npc_world_bindings`
- [x] Export/debugdump `npc-world-bindings.json` pentru `npc_world_bindings`
- [x] `HouseAllocation` initial pentru case cu mai multi rezidenti si conversie catre `NpcSpawnPlan`
- [x] Dry-run si spawn batch initial pentru household, cu rollback practic daca spawn-ul esueaza la mijloc
- [x] Villagerii existenti convertiti in AINPC primesc control de baza impotriva miscarii vanilla haotice
- [x] Rutina are cooldown initial si distante mai mari ca NPC-urile sa nu sara prea des intre ancore
- [x] NPC Interaction GUI afiseaza rutina curenta si are actiuni rapide mai clare pentru nearest quest/story/routine

## Prioritate curenta

- [ ] Pass Paper pe playable village: teren plat, case distantate, NPC-uri cu home/work/social clare, rutina inspectabila si fara fuga haotica
- [ ] Smoke test Paper pentru flux wand complet: `region`/`place`/`node`/`npc_bind`/`quest_anchor`, audit, save si reload
- [ ] Smoke test Paper pentru `/ainpc world demo create -> settlement plan -> settlement spawn -> audit -> save -> reload`
- [ ] Generator narativ pentru populatie pe regiune: nume, roluri, familii si distributie pe case/work/social
- [ ] Smoke test Paper pentru Q01-Q05: oferta, acceptare, progres, completare, reward
- [ ] Smoke test Paper pentru Q06-Q08 pe mapping demo si NPC-uri medievale
- [ ] Smoke test Paper pentru C02 pe mapping demo prin `/ainpc contract ...` si `/ainpc progression stored ...`
- [ ] Smoke test Paper pentru D01 pe mapping demo prin `/ainpc duty ...` si `/ainpc progression stored ...`
- [ ] Smoke test Paper pentru B01 pe mapping demo prin `/ainpc bounty ...` si `/ainpc progression stored ...`
- [ ] Smoke test Paper pentru B02 pe mapping demo prin `/ainpc bounty ...`, `tag:farm` si `/ainpc progression stored ...`
- [ ] Smoke test Paper pentru E01 pe mapping demo prin `/ainpc event ...` si `/ainpc progression stored ...`
- [ ] Smoke test Paper pentru T01 pe mapping demo prin `/ainpc tutorial ...` si `/ainpc progression stored ...`
- [ ] Smoke test Paper pentru R01 pe mapping demo prin `/ainpc ritual ...` si `/ainpc progression stored ...`
- [ ] Smoke test Paper pentru `/ainpc patch analyze|plan|validate` pe `demo_sat`
- [ ] Smoke test Paper pentru story fara quest, quest fara story si quest cu `record_story_event`
- [x] Model read-only pentru `QuestDirectorDecision`, fara executie de progres
- [ ] Modele si validator read-only pentru `QuestSeed`/`QuestDraft`
- [x] Debugdump/audit pentru settlement spawn, rollback si legaturi NPC-place
- [x] Export/debugdump complet pentru `quest_anchor_bindings`
- [x] Audit strict complet pentru `quest_anchor_bindings`, inclusiv validarea `objective_key` fata de definitia progresiei
- [x] Comanda read-only dedicata pentru inspectie `npc_world_bindings`

## Componente lipsa sau incomplete confirmate in cod

- [ ] Runtime extensibil complet pentru scenarii: registrii exista initial, dar `ScenarioEngine` nu consuma inca aceste contracte ca runtime principal
- [x] `ScenarioActionRegistry`
- [x] `ScenarioConditionRegistry`
- [x] `ScenarioTriggerRegistry`
- [x] `ScenarioExecutionContext`
- [x] `ScenarioVariableProvider`
- [x] `ScenarioValidationReport`
- [x] `AIOrchestrationService` initial, dezactivat implicit pentru executie reala prin `ai.orchestration.enabled`
- [x] Validator initial pentru addonuri si feature pack-uri:
- [x] verificare `dependencies`
- [x] verificare `capabilities`
- [x] verificare compatibilitate cu `RuntimeMode`
- [x] mesaje clare la load pentru incompatibilitati
- [x] Multi-quest runtime matur pe jucator, cu UX complet pentru prioritizare, grupare si actiuni rapide in `quest log`
- [ ] Sistem semantic de `places` peste world admin:
- [ ] locuri de tip `fierarie`, `taverna`, `casa_fierarului`
- [ ] API public pentru interogarea acestor locuri
- [ ] Sistem extins de reward:
- [ ] reputatie
- [ ] economie / monede
- [ ] progresie jucator
- [ ] factiuni sau afiliere regionala
- [ ] Comenzi de debug si inspectie pentru:
- [ ] prompt AI
- [ ] scenarii active
- [x] quest progress
- [x] validare initiala quest templates prin `/ainpc audit quest`
- [x] audit strict initial pentru continut quest avansat
- [ ] validare completa pack-uri si addonuri
- [ ] Suita de teste automate pentru questuri, world admin si addon registry
- [x] Build-ul core foloseste sursele din `ainpc-core-plugin/src/main`, nu din `src/src`
- [x] Curatarea sau arhivarea folderului legacy `src/src` dupa validarea tuturor referintelor istorice

## NPC-uri

- [x] Rutine zilnice mai clare pentru fiecare NPC, cu sloturi vizibile in GUI si comenzi de inspectie
- [ ] Reactii mai bune la reputatie, familie, emotii si istoric
- [ ] Roluri mai bine definite: negustor, gardian, fermier, quest giver
- [ ] Coordonare intre NPC-uri din acelasi sat sau aceeasi regiune
- [ ] Dialog care se schimba in functie de povestea locala

## Questuri si povesti

- [ ] Questuri in lant, nu doar interactiuni izolate
- [x] Obiective cu stari: inceput, progres, completat, esuat
- [x] Arhivare documentatie questuri avansate v1 si document canonic V2 pentru diversitate, faze si mecanici non-quest
- [ ] Recompense configurabile pe scenariu
- [ ] Questuri legate de locatie, anotimp, eveniment sau reputatie
- [ ] Povesti distribuite pe sate, regiuni si puncte de interes
- [x] Audit initial pentru quest templates: profesii, prerequisites, obiective si recompense
- [x] Quest anchors persistente pe `regionId`, `placeId`, `nodeId` si `npcId`
- [x] Primul quest medieval pe mapping: Q06 foloseste `visit_place`, `inspect_node` si story event
- [x] Quest social/delivery initial: Q07 foloseste NPC secundar si revenire la quest giver
- [x] Hunt contextual initial: Q08 foloseste regiune mapata, combat si raportare la garda
- [x] Context narativ read-only peste mapping si quest anchors
- [x] Story state pe regiune/place, pentru lumi care evolueaza in timp
- [x] Quest completion poate scrie story state si story events prin actiuni controlate
- [x] Primul contract medieval peste mapping: C02 foloseste piata/avizierul demo si ramane progresie non-`QUEST`
- [x] Primul bounty medieval peste mapping: B01 foloseste `BOUNTY`, `local_bounties`, `visit_region`, `kill_mob` si story event regional
- [x] Al doilea bounty medieval peste mapping: B02 foloseste `BOUNTY`, `local_bounties`, `visit_place tag:farm`, `kill_mob`, cooldown si story event pe place
- [x] Primul eveniment medieval peste mapping: E01 foloseste `WORLD_EVENT`, `village_events`, `visit_place`, `inspect_node`, `deliver_to_npc` si story event pe place
- [x] Primul tutorial medieval peste mapping: T01 foloseste `TUTORIAL`, `onboarding`, `visit_place`, `inspect_node`, `talk_to_npc` si story event pe place
- [x] Primul ritual medieval peste mapping: R01 foloseste `RITUAL`, `village_rituals`, altarul demo, `ritual_circle`, `deliver_to_npc` si story event pe place

## Lume si gameplay

- [ ] Regiuni cu identitate proprie: sat, castel, pestera, dungeon
- [ ] Demo mapping salvat si verificat pe server Paper real cu `settlement plan/spawn`, audit, save si reload
- [x] Unealta wand initiala pentru mapping manual: selectie pos1/pos2, punct node, prompt natural determinist si confirmare inainte de salvare
- [x] Bind manual initial NPC-place pentru demo mapping si harti manuale
- [x] Draft `npc_bind` prin wand pentru home/work/social peste place-ul selectat
- [ ] Spawn si comportament diferit pe tipuri de zona
- [ ] Generator real care produce automat `HouseAllocation` din regiuni, cladiri si node-uri
- [x] Planner pentru tot satul, nu doar pentru o singura casa
- [x] Persistenta dedicata initiala `npc_world_bindings`
- [ ] Generator narativ mai bun pentru nume, roluri si familii pe regiune
- [ ] Tranzactie DB completa pentru spawn pe regiune, peste mapping/family bind
- [ ] Economie de baza: monede, tranzactii, roluri comerciale
- [ ] Reputatie pe sat, regiune sau factiune
- [ ] Sistem de progres pentru jucator: nivel, skill-uri sau experienta

## Scenarii si addonuri

- [ ] API public stabil pentru scenarii si addonuri
- [ ] Incarcare curata a scenariilor ca module separate
- [ ] Addonuri de integrare cu pluginuri externe
- [ ] Feature packs noi pe langa medieval: social, modern, fantasy
- [ ] Posibilitatea de a combina mai multe addonuri fara configuratie fragila

## Admin si debug

- [ ] Comenzi mai bune pentru inspectarea starii unui NPC
- [x] Audit/debugdump dedicat pentru story state si story events
- [x] Debug pentru prompt, model AI si raspuns fallback
- [ ] Reload sigur pentru config, pack-uri si scenarii
- [ ] Mesaje de eroare mai clare pentru configuratii invalide

## Pentru demo playable matur

- [ ] Un scenariu medieval demo complet jucabil cap-coada
- [ ] Un set mic de NPC-uri cu roluri distincte
- [ ] Cateva questuri cap-coada care pot fi testate usor pe Paper
- [ ] Smoke test/restart/audit/debugdump trecute pentru mapping, NPC bindings, quest/progression si story state
- [ ] Instalare simpla si documentatie scurta pentru server admins/testeri
- [ ] Release-ul public ramane ulterior, dupa backup, rollback, migration plan si stabilizare API/addon
