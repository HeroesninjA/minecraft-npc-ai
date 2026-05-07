# TODO Caracteristici

Actualizat: 2026-05-07

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
- [x] Export/debugdump complet pentru `player_quests` in `player-quest-progress.json`
- [x] Export/debugdump generic pentru progresii in `player-progressions.json`, ca view peste `player_quests` cu metadata de mecanica
- [x] Export/debugdump pentru `story_events` in `story-events.json`
- [x] Metadata initiala pentru `ProgressionService`: feature packs pot declara `mechanics`, iar scenariile pot seta `mechanic`/`progress`
- [x] Availability aplica initial `max_active` si pe mecanici de progres declarate in addon
- [x] Addonul medieval are primul contract non-`QUEST` (`C01`) pe mecanica `village_contracts`
- [x] `quest log` afiseaza/grupeaza dupa mecanica si are filtre initiale `quest`/`contract`
- [x] Selectorii de progres accepta forme cu mecanica, de exemplu `village_contracts:C01`
- [x] Fatade initiale pentru progres generic: `/ainpc progression ...`, `/progression ...`, `/ainpc contract ...`, `/contract ...`
- [x] Script `scripts/smoke-paper-quests.ps1` pentru pregatirea smoke test-ului Paper pe questuri
- [x] `StoryContextService` initial si comanda `/ainpc story context`
- [x] `StoryStateService` initial cu `region_story_state`, `place_story_state` si `story_events`
- [x] Comenzi read-only `/ainpc story region`, `/ainpc story place` si `/ainpc story events`
- [x] Actiuni de quest `set_story_state` si `record_story_event`
- [x] Comanda `/ainpc world demo create [regionId]` pentru mapping demo minim in jurul jucatorului
- [x] Comanda `/ainpc world bind npc ...` pentru legarea initiala NPC -> home/work/social places
- [x] Planner `/ainpc world household plan ...` pentru `HouseAllocation` din mapping
- [x] Spawn `/ainpc world household spawn ...` prin `NpcSpawnOrchestrator`
- [x] Planner `/ainpc world settlement plan ...` pentru toate casele dintr-o regiune
- [x] Spawn `/ainpc world settlement spawn ...` pentru household-uri secventiale pe regiune
- [x] Rollback global practic pentru `settlement spawn` daca un household ulterior esueaza
- [x] Script `scripts/smoke-paper-mapping.ps1` pentru pregatirea smoke test-ului Paper mapping/spawn
- [x] Persistenta initiala `npc_world_bindings` pentru home/work/social place si node IDs
- [x] `/ainpc audit db` valideaza initial `npc_world_bindings`
- [x] `HouseAllocation` initial pentru case cu mai multi rezidenti si conversie catre `NpcSpawnPlan`
- [x] Dry-run si spawn batch initial pentru household, cu rollback practic daca spawn-ul esueaza la mijloc

## Prioritate curenta

- [ ] Smoke test Paper pentru `/ainpc world demo create -> settlement plan -> settlement spawn -> audit -> save -> reload`
- [ ] Generator narativ pentru populatie pe regiune: nume, roluri, familii si distributie pe case/work/social
- [ ] Smoke test Paper pentru Q01-Q05: oferta, acceptare, progres, completare, reward
- [ ] Smoke test Paper pentru Q06-Q08 pe mapping demo si NPC-uri medievale
- [ ] Debugdump/audit pentru settlement spawn, rollback si legaturi NPC-place
- [x] Export/debugdump complet pentru `quest_anchor_bindings`
- [ ] Comanda read-only dedicata pentru inspectie `npc_world_bindings`

## Componente lipsa confirmate in cod

- [ ] Runtime extensibil pentru scenarii:
- [ ] `ScenarioActionRegistry`
- [ ] `ScenarioConditionRegistry`
- [ ] `ScenarioTriggerRegistry`
- [ ] `ScenarioExecutionContext`
- [ ] `ScenarioVariableProvider`
- [ ] `ScenarioValidationReport`
- [ ] Validator pentru addonuri si feature pack-uri:
- [ ] verificare `dependencies`
- [ ] verificare `capabilities`
- [ ] verificare compatibilitate cu `RuntimeMode`
- [ ] mesaje clare la load pentru incompatibilitati
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

- [ ] Rutine zilnice mai clare pentru fiecare NPC
- [ ] Reactii mai bune la reputatie, familie, emotii si istoric
- [ ] Roluri mai bine definite: negustor, gardian, fermier, quest giver
- [ ] Coordonare intre NPC-uri din acelasi sat sau aceeasi regiune
- [ ] Dialog care se schimba in functie de povestea locala

## Questuri si povesti

- [ ] Questuri in lant, nu doar interactiuni izolate
- [x] Obiective cu stari: inceput, progres, completat, esuat
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

## Lume si gameplay

- [ ] Regiuni cu identitate proprie: sat, castel, pestera, dungeon
- [ ] Demo mapping salvat si verificat pe server Paper real cu `settlement plan/spawn`, audit, save si reload
- [x] Bind manual initial NPC-place pentru demo mapping si harti manuale
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
- [ ] Audit/debugdump dedicat pentru story state si story events
- [ ] Debug pentru prompt, model AI si raspuns fallback
- [ ] Reload sigur pentru config, pack-uri si scenarii
- [ ] Mesaje de eroare mai clare pentru configuratii invalide

## Pentru primul release bun

- [ ] Un scenariu medieval complet jucabil
- [ ] Un set mic de NPC-uri cu roluri distincte
- [ ] Cateva questuri cap-coada care pot fi testate usor
- [ ] Instalare simpla si documentatie scurta pentru server admins
