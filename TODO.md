# TODO Caracteristici

Actualizat: 2026-05-03

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
- [x] Persistenta dedicata `quest_anchor_bindings` pentru ancore semantice de quest
- [x] Audit si comanda admin read-only pentru `quest_anchor_bindings`
- [x] `/ainpc audit quest` valideaza initial si quest templates din feature packs
- [x] Teste de contract runtime pentru Q01-Q05 din addonul medieval
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
- [ ] 3-5 questuri medievale completabile pe mapping-ul demo, cu `visit_place` si `inspect_node`
- [ ] Debugdump/audit pentru settlement spawn, rollback si legaturi NPC-place
- [ ] Export/debugdump complet pentru `quest_anchor_bindings`
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
- [ ] Multi-quest runtime matur pe jucator, nu doar un singur quest activ
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
- [ ] quest progress
- [x] validare initiala quest templates prin `/ainpc audit quest`
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
- [ ] Obiective cu stari: inceput, progres, completat, esuat
- [ ] Recompense configurabile pe scenariu
- [ ] Questuri legate de locatie, anotimp, eveniment sau reputatie
- [ ] Povesti distribuite pe sate, regiuni si puncte de interes
- [x] Audit initial pentru quest templates: profesii, prerequisites, obiective si recompense
- [x] Quest anchors persistente pe `regionId`, `placeId`, `nodeId` si `npcId`
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
