# Ce Este Implementat Deja

Actualizat: 2026-05-07

Status verificat:
- build-ul multi-module trece cu `mvn test`
- informatiile din acest fisier sunt bazate pe codul actual, nu pe viziunea de produs

## Module existente

Proiectul este impartit in 3 module Maven:

- `ainpc-api`
  - API public pentru runtime mode, world mode, addon registry si world admin
- `ainpc-core-plugin`
  - modulul de build pentru pluginul principal
  - detine sursele reale in `ainpc-core-plugin/src/main/java`
  - detine resursele reale in `ainpc-core-plugin/src/main/resources`
- `ainpc-scenario-medieval`
  - addon plugin separat pentru scenariul medieval
  - livreaza si sincronizeaza pack-ul `medieval_quest.yml`

## Pluginul principal

Pluginul principal `AINPCPlugin` face deja urmatoarele la startup:

- incarca `config.yml`
- incarca `quests.yml`
- initializeaza SQLite
- initializeaza serviciul OpenAI
- ruleaza diagnostice pentru model la startup si reload
- incarca feature packs
- initializeaza managerii principali
- incarca NPC-urile persistate
- descopera villagerii existenti din lumi
- verifica si completeaza profilurile lipsa
- initializeaza motoarele de decizie, dialog si scenarii
- initializeaza `StoryContextService` read-only
- initializeaza `StoryStateService` pentru persistenta story
- inregistreaza comenzile si listener-ele
- porneste task-urile periodice de simulare, decay si autosave
- porneste task-ul periodic de rutina NPC, daca `routine.enabled` este activ

## NPC-uri

Sistemul de NPC-uri are deja implementate:

- NPC-uri bazate pe entitati `Villager`
- creare manuala prin comanda
- persistenta in baza de date
- reincarcare si restaurare din chunk-uri incarcate
- asociere automata a villagerilor existenti cu sistemul AI
- creare automata de profil pentru villagerii naturali
- actualizare de ocupatie si profil la schimbarea carierei
- sincronizare intre modelul logic si entitatea Bukkit
- cautare NPC dupa nume, UUID, entity si proximitate
- afisare info despre NPC
- teleport la NPC
- completare automata pentru `homeAnchor` si `workAnchor`
- completare automata pentru `socialAnchor`
- bind manual initial prin `/ainpc world bind npc ...` catre home/work/social places
- persistenta initiala `npc_world_bindings` pentru home/work/social place si node IDs
- planner initial `/ainpc world household plan ...` care produce `HouseAllocation` din mapping
- spawn initial `/ainpc world household spawn ...` prin `NpcSpawnOrchestrator`
- planner initial `/ainpc world settlement plan ...` pentru toate casele dintr-o regiune
- spawn initial `/ainpc world settlement spawn ...` pentru household-uri secventiale pe regiune
- rollback global practic pentru `settlement spawn`, daca un household ulterior esueaza
- selectie de ancora din `WorldPlace`, bloc fizic apropiat sau fallback la pozitia NPC-ului
- evitare de nume duplicate pentru NPC-urile generate automat in viitor

Capabilitati suplimentare existente in model:
- personalitate
- emotii
- context curent
- ocupatie
- backstory
- profil generat si persistat
- ancore de simulare:
  - `homeAnchor`
  - `workAnchor`
  - `socialAnchor`

## Dialog si interactiune

Sistemul de dialog implementat deja include:

- click dreapta pe NPC pentru pornirea interactiunii
- validare de distanta maxima de interactiune
- sesiune privata de conversatie intre jucator si NPC
- salut de prima intalnire
- salut diferit pentru jucatori deja cunoscuti
- NPC-ul se uita la jucator in interactiune
- cooldown intre mesaje
- mesaj de tip "NPC se gandeste"
- raspuns generat prin OpenAI
- fallback de mesaje de eroare cand AI-ul nu raspunde corect
- inchidere conversatie prin mesaj de tip `pa`

Sistemul de chat mai are si:

- `passive_listen_enabled`
- selectare automata a NPC-ului din apropiere
- activare prin mentionarea numelui NPC-ului
- auto-engage pentru NPC-ul cel mai apropiat in raza mica

Documentatia detaliata pentru fluxul click/chat/sesiune/intentie este in `interactiuni.md`.

## GUI

Exista o prima fundatie GUI inventory pentru operare in joc:

- pachet `ro.ainpc.gui` cu `GuiService`, `GuiScreen`, `GuiSessionManager`, `AINPCGuiHolder`, `GuiButton`, contexte de render/click si factory pentru iteme
- `GuiInventoryListener` anuleaza click/drag in inventarele AINPC si curata sesiunile la close/quit
- `AINPCPlugin` initializeaza `GuiService`, iar `ListenerRegistry` inregistreaza listenerul GUI
- comanda `/ainpc gui [main|quest|world|stats|interact|shop|manager|audit|debug]`
- comanda rapida `/quest gui`
- tab-completion pentru `/ainpc gui` si `/quest gui`
- permisiuni `ainpc.gui`, `ainpc.gui.quest`, `ainpc.gui.stats`, `ainpc.gui.interact`, `ainpc.gui.shop`, `ainpc.gui.world`, `ainpc.gui.manager`, `ainpc.gui.audit`, `ainpc.gui.debug`
- `ScenarioEngine.getQuestGuiSnapshot(...)` expune pentru GUI un snapshot read-only cu questuri curente/arhivate, status, categorie, selector, stage curent, obiective, stage-uri si recompense
- ecrane initiale:
  - hub principal
  - quest log navigabil peste snapshot-ul GUI din `ScenarioEngine`
  - detalii quest cu obiective, stage-uri, recompense, status, tracking, debug admin si abandon cu confirmare
  - world context peste `WorldAdminApi`
  - statistici jucator si NPC-uri apropiate
  - interactiune NPC cu info si quest status
  - manager NPC admin cu info/teleport
  - audit GUI care ruleaza comenzile audit existente
  - debug GUI care ruleaza `debugdump` si `test`
  - confirmare pentru actiuni destructive
  - shop placeholder pregatit pentru provider dedicat

Limitare actuala:

- GUI-ul este un wrapper peste comenzi si servicii existente; nu exista inca paginare completa, filtre interactive pe quest log, target selection avansat sau shop/economie reala.

## Memorie, relatii si emotii

Sunt deja prezente:

- memorii per NPC si jucator
- istoric de dialog
- relatii persistente cu jucatorii
- scoruri de relatie:
  - afectiune
  - incredere
  - respect
  - familiaritate
- emotii persistente si decay periodic
- reactii emotionale la evenimente
- recunoastere a jucatorului la login pe baza memoriilor

Persistenta exista in tabele dedicate:
- `npc_memories`
- `npc_relationships`
- `dialog_history`
- `npc_emotions`

## Familie

Exista deja infrastructura pentru familie:

- tabel `npc_family`
- `FamilyManager`
- comanda `family`
- `FamilyBindingPlan`
- `FamilyBindingResult`
- `HouseAllocation`
- `HouseAllocationValidator`
- conversie `HouseAllocation -> NpcSpawnPlan`
- bind initial pentru familii spawnate dupa ce toti membrii au ID DB valid
- relatii reciproce inferate din roluri precum father, mother, son, daughter, brother si sister

Acest lucru inseamna ca sistemul de relatii familiale este deja prezent la nivel de baza de date si runtime, chiar daca nu reprezinta inca un gameplay extins.

## Simulare de viata

Exista deja un sistem de simulare periodica pentru NPC-uri:

- tick periodic de simulare
- nevoi de baza configurabile:
  - foame
  - energie
  - social
  - confort
  - siguranta
- scop curent si activitate planificata
- verificare stare `isAtHome`, `isAtWork`, `isAtSocialSpot`
- rebalansare a satelor incarcate
- repopulare automata de villageri in jurul paturilor disponibile

## Rutine NPC

Exista deja o implementare initiala de rutina zilnica:

- `RoutineEngine`
- `RoutineService`
- `RoutineAssignment`
- `RoutineTickSummary`
- sloturi de rutina:
  - `HOME`
  - `WORK`
  - `SOCIAL`
  - `IDLE`
- scheduler periodic configurabil prin `routine.tick_seconds`
- fallback daca lipseste ancora de munca
- evitarea intreruperii NPC-urilor aflate in interactiuni importante
- comanda `/ainpc routine tick`
- comanda `/ainpc routine status [numeNpc]`

Rutina foloseste ancorele existente:

- `homeAnchor`
- `workAnchor`
- `socialAnchor`

Limitare actuala:

- pentru MVP, mutarea este facuta prin teleport controlat, deoarece villagerii AINPC au AI dezactivat

## Questuri

Sistemul de questuri este deja functional la nivel de baza si mediu.

Ce este implementat:

- questuri definite in scenario packs prin `base_type: "QUEST"`
- questuri fallback in `quests.yml`
- asociere quest cu profesia NPC-ului
- oferire, acceptare, refuz, abandon, status, reset si completare fortata
- progres persistent per jucator in tabela `player_quests`
- metadata initiala pentru runtime generic de progres: feature packs pot declara `mechanics`, scenariile pot seta `mechanic` si `progress/progression`, iar scenariile non-`QUEST` cu progres activ pot intra in runtime-ul jucabil actual
- availability-ul aplica initial si `max_active` pe mecanica de progres, nu doar pe categoriile legacy `quest.max_active`
- addonul medieval are mecanici separate `main_quests`, `side_quests` si `village_contracts`, inclusiv contractul non-`QUEST` `C01` bazat pe `TRADE_DEAL`
- `quest log`, statusul si GUI-ul afiseaza initial mecanica de progres; log-ul poate filtra `quest` si `contract`
- selectorii de progres accepta si forme explicite cu mecanica, de exemplu `village_contracts:C01` sau `medieval_quest:village_contracts:C01`
- fatade initiale peste runtime-ul comun: `/ainpc progression ...`, `/progression ...`, `/ainpc contract ...` si `/contract ...`
- selector explicit pentru `quest status`, `quest track` si `quest abandon`
- comanda admin read-only `quest debug` pentru progres, variabile si obiective template
- filtre initiale in `quest log` pentru status, tracking si categorii `main`/`side`/`repeatable`
- sortare initiala in `quest log`: tracked primul, apoi questuri principale, secundare si repetabile, cu sumar pe status/categorii
- actiuni rapide in `quest log` pentru status, tracking, abandon si debug admin
- grupare vizuala in `quest log` pentru tracked, main, side, repeatable si template lipsa
- stages runtime initial peste `current_phase` si `player_quests.current_stage_id`: obiectivele cu metadata `phase`/`stage` sau listate in `quest.stages.<stage>.objectives` progreseaza doar in etapa curenta, questurile fara metadata raman pe fluxul plat, iar DB-ul vechi face backfill din `current_phase`
- `FeaturePackLoader` incarca `quest.stages` in `QuestStageDefinition`, inclusiv `completion_mode`, `next_stage`, `objectives` si metadata
- tranzitiile de stage sunt reflectate in `quest_variables` prin `stage.current`, `stage.previous`, `stage.changed_at`, `stage.completed.<stage>` si `stage.last_completed`
- rezolvare initiala de ancore semantice prin `QuestAnchorResolver`
- persistenta dedicata a ancorelor rezolvate in `quest_anchor_bindings`
- reflectare a ancorelor rezolvate in `questVariables` pentru compatibilitate runtime
- comanda admin read-only `/ainpc quest anchors [jucator|uuid|all] [templateId]`
- raport dedicat `quest-audit-report.txt` si export complet `loaded-quest-definitions.json`, `player-progressions.json`, `player-quest-progress.json`, `quest-anchor-bindings.json` si `story-events.json` prin `/ainpc debugdump quest`
- audit read-only `/ainpc audit quest`
- audit/debugdump initial pentru progression mechanics, metadata `progression_*` din definitiile incarcate si progres persistent exportat generic ca `player-progressions.json`
- validare initiala de quest templates in `/ainpc audit quest`
- validare `phase`/`stage`, stage IDs, `completion_mode`, `next_stage` si objective IDs pentru questuri etapizate in `/ainpc audit quest` si `debugdump quest`
- validare pentru `player_quests.tracked`: cel mult un quest tracked activ per jucator
- warning de audit pentru chei legacy in progresul persistent al obiectivelor
- teste de contract pentru Q01-Q08 din addonul medieval, inclusiv validare ca `phase` de obiectiv refera o faza existenta si ca Q06-Q08 au `quest.stages` explicite
- smoke script `scripts/smoke-paper-quests.ps1`
- context narativ read-only prin `StoryContextService` pentru quest anchors active
- mesaje de briefing si progres
- finalizare cu recompensa
- finalizare cu actiuni story `set_story_state` si `record_story_event`
- arhivare pentru questuri completate sau esuate

Stari de quest implementate:
- `NOT_STARTED`
- `OFFERED`
- `ACTIVE`
- `COMPLETED`
- `FAILED`

Tipuri de obiective implementate:
- `collect_item`
- `deliver_to_npc`
- `talk_to_npc`
- `visit_region`
- `visit_place`
- `inspect_node`
- `kill_mob`

Tracking implementat deja:
- conversatie cu NPC
- miscare intre blocuri pentru `visit_region`, `visit_place` si `inspect_node`
- ucidere de entitati pentru `kill_mob`
- verificare inventar pentru obiective bazate pe iteme
- progresul `visit_place` si `inspect_node` respecta ancora rezolvata si persistata

Interactiunea cu questurile functioneaza prin:
- comenzi admin/jucator
- click pe NPC
- cuvinte-cheie in conversatia cu NPC-ul:
  - `misiune`
  - `quest`
  - `accept`
  - `refuz`
  - `renunt`
  - `status`
  - `progres`

Limitare actuala importanta:
- runtime-ul poate pastra mai multe questuri curente per jucator, poate selecta si persista quest tracked, aplica limite initiale pe categoriile `main`, `side` si `repeatable`, sorta/filtra/grupa `quest log` si afisa actiuni rapide

## Context story si AI

Exista deja un strat initial read-only pentru context narativ si persistenta story initiala:

- `StoryContextService`
- `StoryContextSnapshot`
- `StoryStateService`
- `RegionStoryState`
- `PlaceStoryState`
- `StoryEvent`
- integrare in `NPCContext` pentru sectiunea `STORY_CONTEXT`
- comanda admin `/ainpc story context [jucator] [numeNpc|nearest]`
- comenzi admin read-only `/ainpc story region`, `/ainpc story place` si `/ainpc story events`
- actiuni de quest `set_story_state` si `record_story_event`
- incarcare de quest anchors active din `quest_anchor_bindings`
- tabele `region_story_state`, `place_story_state` si `story_events`
- semnale story din regiune, place metadata, node-uri relevante si quest anchors
- semnale story persistente din regiune/place si evenimente recente, cand exista date persistate

Limitari actuale:

- nu exista inca audit/debugdump dedicat pentru story state-ul persistent
- `StoryContextService` nu genereaza questuri si nu scrie in DB
- actiunile story sunt suportate initial doar la finalizarea questului, ca intrari in `rewards`
- contextul este util doar cat mapping-ul si quest anchors sunt suficient populate

## Feature packs si scenarii

Exista deja un loader de pack-uri YAML:

- incarca automat fisiere `.yml` si `.yaml` din folderul `packs/`
- salveaza pachetele default lipsa
- incarca:
  - traits
  - professions
  - dialogues
  - topologies
  - scenarios

Pack-uri default existente:
- `medieval.yml`
- `modern.yml`
- `social.yml`

Pack addon existent:
- `medieval_quest.yml`

Scenariile suportate in `ScenarioEngine` includ deja:
- `THEFT`
- `CONFLICT`
- `CELEBRATION`
- `EMERGENCY`
- `ROMANCE`
- `TRADE_DEAL`
- `QUEST`
- `GOSSIP_SPREAD`

## Addonuri si API public

Exista deja o baza reala pentru extensibilitate:

- `AINPCPlatformApi`
- `AddonRegistryApi`
- `WorldAdminApi`
- `AINPCAddon`
- `AddonDescriptor`
- `AddonType`

Capabilitati deja implementate:
- core-ul publica `AINPCPlatformApi` ca serviciu Bukkit
- addonurile se pot inregistra prin registry
- descriptorii de addon pot fi listati si clasificati
- continutul poate cere `reloadContent()`
- addonul medieval se inregistreaza si sincronizeaza pack-ul propriu in folderul de packs al core-ului

## World admin

World admin-ul exista deja la nivel de regiuni, places si noduri.

Capabilitati implementate:
- activare/dezactivare din config
- definire manuala de regiuni cuboidale
- tip de regiune
- tag-uri pe regiune
- `StoryState` per regiune
- definire de `WorldPlace` per regiune
- tip semantic pentru place: casa, shop, forge, tavern, farm, market, camp si custom
- `ownerNpcId`, `publicAccess`, tag-uri si metadata pe place
- definire de `WorldNode` per regiune
- definire de `WorldNode` sub un `WorldPlace`
- metadata pe nod
- cautare a regiunii curente dupa coordonate
- cautare a place-ului curent dupa coordonate
- cautare a node-ului curent dupa coordonate si radius
- cautare de node-uri apropiate prin `findNodesNear(...)`
- listare si cautare de places dupa tag
- auto-index intern pentru regiuni, places si nodes
- expunere a numarului de regiuni, places si noduri prin API
- `WorldContextSnapshot` initial pentru context semantic in `NPCContext` si prompt AI
- `StoryContextService` initial pentru context narativ peste mapping si quest anchors
- comenzi admin pentru inspectie si creare manuala
- comanda `/ainpc world demo create [regionId]` pentru generare rapida de mapping demo semantic in jurul jucatorului
- comanda `/ainpc world bind npc <numeNpc|nearest> <homePlaceId> [workPlaceId|-] [socialPlaceId|-]`
- comanda `/ainpc world household <plan|spawn> <homePlaceId> [count]`
- comanda `/ainpc world settlement <plan|spawn> <regionId> [maxHouses]`
- tabela `npc_world_bindings` pentru binding-uri persistente NPC -> home/work/social places si nodes
- metadata initiala pentru bind home/work/social: `owner_npc_id`, `resident_npc_ids`, `worker_npc_ids`, `social_npc_ids`
- audit pentru inconsistenta intre regiuni, places, nodes si NPC-uri
- scanare vanilla initiala prin `/ainpc world scan village`
- import semantic initial prin `/ainpc world scan village <radius> import [regionId]`
- validare initiala pentru alocarea rezidentilor intr-o casa inainte de spawn batch
- generare determinista initiala `WorldPlace house -> HouseAllocation -> NpcSpawnPlan`
- generare determinista initiala `WorldRegion -> houses -> HouseAllocation list`

Scannerul vanilla detecteaza:

- clopote
- paturi
- workstation-uri
- usi
- farmland

Mapperul semantic poate crea runtime:

- `WorldRegion` de tip settlement
- `WorldPlace` pentru case
- `WorldPlace` pentru ferme
- `WorldPlace` pentru locuri de munca detectate
- `WorldNode` pentru `bed`, `home`, `entrance`, `workstation`, `work` si `meeting_point`

Tipuri deja prezente:
- `RegionType`
- `PlaceType`
- `WorldNodeType`
- `StoryMode`
- `WorldMode`

Limitari actuale:
- mapping-ul este functional, dar serverul de test poate avea inca `0` regiuni, `0` places si `0` nodes pana cand sunt definite in config sau prin comenzi
- NPC-urile salveaza ancore de locatie si pot fi legate manual la places, dar nu au inca `homePlaceId` sau `workPlaceId` persistent explicit
- `visit_place` si `inspect_node` exista initial cu `QuestAnchorResolver` si persistenta dedicata `quest_anchor_bindings`
- scannerul vanilla si mapperul semantic exista initial, dar generatorul complet de sate/cladiri si patch-uri native nu este inca implementat ca pipeline complet
- exista `HouseAllocation` ca model intern, dar generatorul complet nu il produce inca automat dintr-un `SettlementPlan`
- household plannerul actual este minim si determinist; nu genereaza inca familii narative complexe sau populatie pe tot satul
- settlement spawn ruleaza household-uri secvential, se opreste la prima eroare si sterge NPC-urile create in household-uri anterioare
- rollback-ul global este practic la nivel de NPC, nu tranzactie DB completa peste mapping/family bind

## Comenzi existente

Comenzi principale disponibile:
- `/ainpc create`
- `/ainpc delete`
- `/ainpc info`
- `/ainpc quest`
- `/ainpc world`
- `/ainpc story`
- `/ainpc story region`
- `/ainpc story place`
- `/ainpc story events`
- `/ainpc audit`
- `/ainpc debugdump`
- `/ainpc list`
- `/ainpc family`
- `/ainpc routine`
- `/ainpc mood`
- `/ainpc tp`
- `/ainpc reload`
- `/ainpc test`

Aliasuri:
- `/npc`
- `/ai`

Comanda rapida pentru questuri:
- `/npcquest`
- alias `/npcq`

## Listenere deja inregistrate

In prezent sunt inregistrate:

- `NPCInteractionListener`
- `NPCChatListener`
- `QuestObjectiveListener`
- `PlayerJoinListener`
- `VillagerLifecycleListener`

Comportament deja activ:
- interactiune directa cu NPC-uri
- chat privat si pasiv
- progres de quest prin miscare si kill
- recunoastere la login
- transformare/sincronizare a villagerilor naturali in NPC-uri AI

## Baza de date

Tabele deja create si folosite:
- `npcs`
- `npc_personality`
- `npc_emotions`
- `npc_profiles`
- `npc_traits`
- `npc_memories`
- `npc_relationships`
- `npc_family`
- `dialog_history`
- `player_quests`
- `quest_anchor_bindings`

Acest lucru inseamna ca persistenta de baza este deja implementata pentru:
- NPC-uri
- personalitate
- emotii
- profiluri
- trasaturi
- memorii
- relatii
- familie
- dialog
- progres de quest
- ancore semantice pentru obiective de quest

## Ce nu este inca implementat complet

Pentru claritate, urmatoarele directii nu sunt inca livrate complet in codul actual:

- legare completa NPC <-> `WorldPlace` prin `homePlaceId`, `workPlaceId`, `socialPlaceId`
- comenzi si audit/debugdump complet pentru story state persistent
- generator complet de sate, case si cladiri de meserii
- integrare optionala cu WorldEdit API
- questuri cu etape reale si branching avansat
- economie functionala
- reputatie globala pe factiuni sau regiuni
- sistem de progres RPG complet
- reward system extins dincolo de iteme
- multi-quest runtime matur pe jucator

## Rezumat

Proiectul are deja implementate:

- plugin principal functional pe Paper
- SQLite si persistenta pentru subsistemele importante
- NPC-uri AI bazate pe Villager
- dialog contextual cu OpenAI
- memorii, emotii si relatii
- simulare de viata de baza
- questuri functionale cu progres persistent
- feature packs si scenarii YAML
- API si registry pentru addonuri
- world admin la nivel de regiuni, places si noduri
- `WorldContextSnapshot` initial pentru context AI/NPC peste mapping
- obiective native initiale `visit_place` si `inspect_node`
- `QuestAnchorResolver` initial cu ancore persistate in `quest_anchor_bindings`
- `StoryContextService` initial cu `/ainpc story context`
- `StoryStateService` initial cu `region_story_state`, `place_story_state` si `story_events`
- comenzi read-only pentru story state persistent si evenimente story
- actiuni de quest pentru story state si story events
- scanare vanilla initiala si import semantic pentru world mapping
- `npc_world_bindings` initial pentru home/work/social place si node IDs
- comanda pentru mapping demo minim in jurul jucatorului
- rutina zilnica initiala peste `home/work/social anchors`
- `HouseAllocation` initial pentru case cu mai multi rezidenti si spawn batch cu rollback practic
- audit read-only pentru NPC, world mapping, DB si spawn order
- debug dump avansat in `plugins/AINPC/debug-dumps/`
- addon medieval separat care se integreaza cu core-ul

Pe scurt:
- nu este doar un prototip de dialog
- este deja un nucleu functional de plugin modular cu NPC-uri, questuri si extensibilitate
