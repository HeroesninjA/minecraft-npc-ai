# Faze, Observatii si Avertizari

Actualizat: 2026-05-08

Status: audit documentatie dupa codul curent, mapping demo, bind NPC-place, household/settlement planner si rollback global practic pentru spawn pe regiune.

## Verdict

Documentatia este in mare parte coerenta, dar trebuie citita pe doua niveluri:

- ce este implementat initial si verificabil in cod
- ce este design recomandat pentru faze viitoare

Cel mai important avertisment: expresii precum "implementat initial" nu inseamna "gata pentru productie". Inseamna ca exista model/cod de baza, teste sau comenzi initiale, dar inca pot lipsi generatorul complet, persistenta dedicata, comenzi admin sau rollback tranzactional.

## Faza 0: Baseline si Audit

Scop:

- sa fie clar ce exista deja in cod
- sa existe comenzi de diagnostic si documentatie de testare
- sa nu fie confundat backlog-ul cu functionalitate implementata

Documente:

- `implementat-deja.md`
- `audit.md`
- `debugging-si-testare.md`
- `analiza-erori-si-plan-rezolvare.md`

Observatii:

- `implementat-deja.md` este sursa principala pentru starea confirmata in cod.
- `/ainpc audit` si `debugdump` sunt operational utile, dar nu inlocuiesc testele automate.
- Rapoartele din `analiza-erori-si-plan-rezolvare.md` trebuie revalidate dupa fiecare refactorizare.

Avertizari:

- Nu interpreta `audit.md` ca dovada de securitate completa.
- Nu folosi fisierele din `target/` ca documentatie sursa.
- Nu actualiza TODO-ul fara sa actualizezi si documentul tehnic relevant.

## Faza 1: Mapping, Spawn Order si Rutina Minima

Scop:

- `WorldRegion -> WorldPlace -> WorldNode` devine sursa semantica a lumii
- NPC-urile se spawneaza numai dupa ce exista casa, node-uri si alocare
- rutina foloseste `home/work/social anchors`

Documente:

- `mapping.md`
- `ordine-spawn-npc-cladiri-region-node.md`
- `rutine-npc-si-timeline.md`
- `generare-sate-fara-worldedit.md`

Status curent:

- world admin exista pentru regiuni, places si node-uri
- mapping-ul are lookup initial pentru node curent si node-uri apropiate
- exista comanda `/ainpc world demo create [regionId]` pentru mapping demo minim in jurul jucatorului
- exista comanda `/ainpc world bind npc <numeNpc|nearest> <homePlaceId> [workPlaceId|-] [socialPlaceId|-]` pentru legarea initiala NPC-place
- exista comanda `/ainpc world household <plan|spawn> <homePlaceId> [count]` pentru plan/spawn household din mapping
- exista comanda `/ainpc world settlement <plan|spawn> <regionId> [maxHouses]` pentru plan/spawn household-uri pe regiune
- `settlement spawn` are rollback global practic pentru household-urile create anterior daca o casa ulterioara esueaza
- exista persistenta initiala `npc_world_bindings` pentru home/work/social place si node IDs
- `/ainpc audit db` valideaza initial randurile `npc_world_bindings`
- auditul world raporteaza readiness minim: case, locuri de munca, locuri sociale si quest/interaction nodes
- scannerul vanilla si mapperul semantic exista initial
- `HouseAllocation` exista initial si produce `NpcSpawnPlan`
- `NpcSpawnOrchestrator` are validare, dry-run si spawn batch pentru household
- auditul spawn-order verifica case, rezidenti, ancore si familie reciproca
- rutina zilnica initiala foloseste ancorele
- `WorldContextSnapshot` este legat initial in `NPCContext` si prompt AI
- questurile suporta initial `visit_place` si `inspect_node`
- `QuestAnchorResolver` valideaza initial ancorele semantice, le persista in `quest_anchor_bindings` si le reflecta in `questVariables`
- `/ainpc audit quest` valideaza initial si quest templates din feature packs
- Q01-Q05 au teste de contract runtime si smoke script dedicat pentru server
- `StoryContextService` construieste initial `STORY_CONTEXT` din mapping, quest anchors active si story state persistent daca exista
- `StoryStateService` persista initial `region_story_state`, `place_story_state` si `story_events`
- comenzile `/ainpc story region`, `/ainpc story place` si `/ainpc story events` inspecteaza read-only story state-ul persistent
- `/ainpc audit quest`, `/ainpc debugdump quest` si `/ainpc debugdump story` acopera initial story state/events persistente
- quest completion poate executa `set_story_state` si `record_story_event` din YAML

Observatii:

- Pentru demo-ul playable, fluxul corect este `plan -> constructie/import -> region -> place -> node -> HouseAllocation -> NpcSpawnPlan -> spawn -> save -> family bind -> audit`.
- Pentru demo manual cu NPC existent, fluxul scurt este `world demo create -> world bind npc -> audit spawn -> world save`.
- Pentru demo cu spawn generat, fluxul scurt este `world demo create -> world household plan -> world household spawn -> audit spawn -> world save`.
- Pentru demo de sat intreg, fluxul scurt este `world demo create -> world settlement plan -> world settlement spawn -> audit spawn -> world save`.
- `metadata.residents` ramane solutia temporara pentru case cu mai multi locuitori.
- Rutina actuala este suficienta pentru comportament controlat, nu pentru pathfinding natural.

Avertizari:

- Generatorul real care produce automat `HouseAllocation` dintr-un `SettlementPlan` nu este inca gata; contractul de plan este separat in `settlement-plan.md`.
- Plannerul actual produce `HouseAllocation` pentru o casa sau pentru toate casele dintr-o regiune, dar nu genereaza inca narativ locuitori pe roluri complexe.
- Spawn-ul pe regiune are rollback global practic pentru NPC-urile create anterior, dar nu este tranzactie DB completa peste toate efectele secundare.
- `npc_world_bindings` exista initial, dar rutina si auditul spawn inca folosesc in mare parte ancorele din profil.
- Bind-ul NPC-place scrie acum si `npc_world_bindings`, dar metadata pe place ramane compatibilitate si inspectie admin.
- `quest_anchor_bindings` exista initial si are audit/comanda admin read-only.
- Daca serverul nu are mapping importat/configurat, codul cade pe fallback-uri vanilla.
- Spawn-ul batch are rollback practic pentru NPC-urile create, dar nu tranzactie DB completa.

## Faze Urmatoare Imediate

Aceste faze continua direct munca de mapping/spawn inainte de a muta focusul principal pe questuri mari.

### Faza 1.1: Smoke Test Paper

Scop:

- sa se confirme pe server real ca mapping-ul demo poate fi creat, populat, auditat, salvat si incarcat din nou.

Flux minim:

```text
/ainpc world demo create demo_sat
/ainpc world settlement plan demo_sat
/ainpc world settlement spawn demo_sat
/ainpc audit world
/ainpc audit spawn
/ainpc world save
```

Dupa reload/restart:

```text
/ainpc audit world
/ainpc audit spawn
/ainpc world places demo_sat
```

### Faza 1.2: Persistenta Dedicata Pentru Bindings

Status:

- implementata initial.

Scop:

- legatura NPC-place sa nu mai stea doar in `profile_data` si metadata de place.

Livrabile:

- tabela `npc_world_bindings` pentru home/work/social place si node IDs
- scriere din `world bind`, `household spawn` si `settlement spawn`
- backfill initial din `profile_data.owned_locations` cand ancorele cad in mapped places
- audit DB pentru bindings orfane, place-uri/node-uri inexistente si tipuri incompatibile

Ramas:

- comanda read-only dedicata pentru inspectie bindings
- contractul pentru household-uri persistente este separat in `households-persistente.md`
- migration/backfill explicit pentru servere vechi si cazuri ambigue
- integrare mai completa in rutina, NPCContext si audit spawn-order

### Faza 1.3: Generator Narativ De Populatie

Scop:

- plannerul determinist sa devina generator de sat credibil, nu doar alocare minima de rezidenti.

Document:

- `generare-populatie-narativa.md`

Livrabile:

- nume, roluri si profesii dupa `placeType`, tags si story local
- familii simple coerente pentru case cu mai multi rezidenti
- distributie controlata pe case, locuri de munca si puncte sociale
- reguli de capacitate si fallback explicite

### Faza 1.4: Hardening Tranzactional

Scop:

- spawn-ul pe regiune sa poata fi rulat fara date partiale greu de reparat.

Livrabile:

- tranzactie DB completa peste spawn, profil, familie si mapping metadata, acolo unde este posibil
- rollback verificabil in audit/debugdump
- mesaje admin clare pentru cauza exacta a esecului
- test pentru esec la household intermediar

### Faza 2.0: First Playable Pe Mapping

Scop:

- dupa ce demo-ul de sat trece smoke test-ul, questurile devin prioritatea principala.

Livrabile:

- 3-5 questuri medievale completabile pe locurile demo
- obiective reale `visit_place` si `inspect_node`
- story state/events scrise la completare
- verificare dupa reload pentru quest progress, anchors si story state

## Faza 2: First Playable Demo Intern

Scop:

- un scenariu medieval mic, instalabil si jucabil cap-coada de admin/tester
- NPC-uri cu roluri clare
- questuri completabile si persistente
- documentatie scurta pentru admin

Aceasta faza nu este lansare publica. Este un demo matur de validare pentru gameplay, persistenta, audit si operare.

Documente:

- `roadmap-orientativ.md`
- `questuri-avansate-v2.md`
- `story-si-context-ai.md`
- `reactie-npc-jucator.md`
- `betonquest-directii-potrivite-pentru-ainpc.md`

Observatii:

- Faza 2 incepe practic dupa ce Faza 1.1 trece pe Paper cu `demo_sat`, spawn pe regiune, audit, save si reload.
- First playable trebuie sa prioritizeze continutul testabil, nu infrastructura infinita.
- Questurile existente sunt functionale la nivel de baza, dar modelul multi-quest matur poate astepta.
- Story-ul pentru first playable poate folosi `StoryContextService` si `StoryStateService`, dar nu trebuie sa depinda de generare libera.
- Reactiile NPC-jucator trebuie extinse incremental peste memoria si emotiile existente.

Avertizari:

- Nu confunda demo-ul cu release-ul public; economie, reputatie globala si RPG complet raman pentru dupa validarea demo-ului.
- Nu introduce branching complex inainte ca 3-5 questuri simple sa fie stabile la reload.
- Nu lasa scenariul demo sa depinda de setari manuale fragile nedocumentate.

## Faza 3: Modularizare, API si Addonuri

Scop:

- core-ul ramane platforma
- addonurile depind de `ainpc-api`, nu de internals instabile
- scenariile si content packs pot fi livrate separat

Documente:

- `documentatie-api.md`
- `strategie-plugin-modular-si-scenarii-programabile.md`
- `refactorizare-si-impartire-pe-module.md`
- `reducere-marime-jar.md`

Observatii:

- Modularizarea initiala exista in `ainpc-api`, `ainpc-core-plugin` si `ainpc-scenario-medieval`.
- API-ul public este util, dar inca trebuie stabilizat pentru dezvoltatori externi.
- Refactorizarea managerilor mari trebuie facuta in pasi mici.

Avertizari:

- Nu expune clase interne doar ca sa rezolvi rapid un addon.
- Nu muta cod masiv fara teste dupa fiecare pas.
- Nu reduce JAR-ul prin `minimizeJar` fara smoke test real pe Paper.

## Faza 4: Runtime de Scenarii Extensibil

Scop:

- scenariile devin compuse din actiuni, conditii si trigger-e
- mapping-ul semantic devine consumat direct de questuri si NPCContext
- NPC-urile temporare si evenimentele pot fi controlate fara hack-uri

Documente:

- `questuri-avansate-v2.md`
- `story-si-context-ai.md`
- `npc-uri-temporare-si-episodice.md`
- `mapping.md`
- `strategie-plugin-modular-si-scenarii-programabile.md`

Observatii:

- Directia corecta este `ScenarioActionRegistry`, `ScenarioConditionRegistry`, `ScenarioTriggerRegistry` si validator separat.
- Registrii, contextul de executie, definitia runtime si raportul de validare exista initial in cod, dar nu sunt inca integrate ca motor principal in `ScenarioEngine`.
- `AIOrchestrationService` exista initial ca strat de politici si fallback, dezactivat implicit pentru orchestrare reala.
- Obiectivele `visit_place` si `inspect_node` exista initial dupa `visit_region`.
- `WorldContextSnapshot`, `QuestAnchorResolver`, `quest_anchor_bindings`, `StoryContextService`, `StoryStateService`, comenzile read-only story si actiunile story de quest exista initial; urmatorul strat recomandat este audit/debugdump si validare explicita pentru story actions.
- NPC-urile temporare trebuie sa foloseasca persistenta light sau deloc, in functie de scop.

Avertizari:

- Nu transforma `ScenarioEngine` intr-o clasa si mai mare.
- Nu lega questurile de coordonate brute cand exista mapping semantic disponibil.
- Nu lasa AI-ul sa genereze story/quest executabil fara validare de ancore.
- Nu trata `StoryContextService` ca runtime de story; este doar snapshot read-only. Scrierile story trebuie sa treaca prin `StoryStateService` si actiuni validate.
- Nu pastra NPC-uri temporare in DB permanent fara cleanup clar.

## Faza 5: Generare si Authoring Asistat

Scop:

- folosirea satelor vanilla ca baza
- completarea lipsurilor prin patch-uri mici
- AI-ul ajuta la authoring, dar pluginul valideaza si executa

Documente:

- `generare-sate-fara-worldedit.md`
- `generare-sate-worldedit-si-npc.md`
- `generare-ai-si-constructie-automata.md`
- `story-si-context-ai.md`
- `surse-inspiratie-plugin-ainpc.md`

Observatii:

- Directia fara WorldEdit este buna pentru demo-ul playable deoarece reduce dependintele obligatorii.
- WorldEdit poate ramane integrare optionala pentru servere care il au deja.
- Generarea AI trebuie sa produca planuri si drafturi validate, nu modificari directe.
- Story/quest authoring prin AI trebuie sa porneasca din `WorldContextSnapshot`, nu din harta completa.

Avertizari:

- Nu regenera sate intregi cand poti scana si completa satul existent.
- Nu lasa AI-ul sa decida spawn/build fara validator si dry-run.
- Nu combina worldgen, spawn, familie, questuri si timeline intr-un singur pas netestabil.

## Faza 6: Hardening Productie

Scop:

- reducerea riscului de date partiale sau corupte
- migrari sigure
- rollback complet
- observabilitate si mesaje clare pentru admini

Documente:

- `audit.md`
- `debugging-si-testare.md`
- `reducere-marime-jar.md`
- `analiza-erori-si-plan-rezolvare.md`

Observatii:

- Auditul runtime este util pentru diagnostic in joc.
- Debug dump-ul este util pentru investigatii offline.
- Hardening-ul trebuie prioritizat dupa riscul real, nu dupa marimea ideii.

Avertizari:

- Nu face migration/backfill fara backup si smoke test.
- Nu considera rollback-ul actual ca tranzactie completa.
- Nu activa debug extins pe server public fara politica de loguri.
- Nu muta dependinte sau shading fara verificare pe server real.

## Observatii Globale

- Documentele de stare trebuie actualizate cand codul trece de la design la implementare initiala.
- `ordine-spawn-npc-cladiri-region-node.md` este v2 pentru fazele urmatoare de spawn order.
- `story-si-context-ai.md` este documentul central pentru legatura `mapping -> indexare -> quest -> story`.
- `arhiva/ordine-spawn-npc-cladiri-region-node-v1.md` pastreaza istoricul complet al fazelor 0-10.
- `roadmap-orientativ.md` este sursa principala pentru organizarea interna pe componente, mecanici si ordinea de dezvoltare.
- `debugging-si-testare.md` este sursa principala pentru cum verifici ca ceva chiar merge.
- `TODO.md` ramane lista de lucru, nu documentatie tehnica completa.

## Avertizari Globale

- Nu trata `0 warnings` in audit ca garantie ca gameplay-ul este complet.
- Nu introduce generare automata fara dry-run si validare de plan.
- Nu construi modele persistente noi fara strategie de migration/backfill.
- Nu extinde API-ul public cu metode care expun accidental structura interna.
- Nu modifica simultan worldgen, spawn, rutine si questuri daca nu exista teste care sa le lege.
