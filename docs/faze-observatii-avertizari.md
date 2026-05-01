# Faze, Observatii si Avertizari

Actualizat: 2026-04-30

Status: audit documentatie dupa codul curent si dupa conectarea initiala `HouseAllocation -> NpcSpawnPlan`.

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

## Faza 1: Mapping, Spawn Order si Rutina MVP

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
- scannerul vanilla si mapperul semantic exista initial
- `HouseAllocation` exista initial si produce `NpcSpawnPlan`
- `NpcSpawnOrchestrator` are validare, dry-run si spawn batch pentru household
- auditul spawn-order verifica case, rezidenti, ancore si familie reciproca
- rutina zilnica initiala foloseste ancorele
- `WorldContextSnapshot` este legat initial in `NPCContext` si prompt AI
- questurile suporta initial `visit_place` si `inspect_node`
- `QuestAnchorResolver` valideaza initial ancorele semantice, le persista in `quest_anchor_bindings` si le reflecta in `questVariables`
- `StoryContextService` construieste initial `STORY_CONTEXT` din mapping si quest anchors active

Observatii:

- Pentru MVP, fluxul corect este `plan -> constructie/import -> region -> place -> node -> HouseAllocation -> NpcSpawnPlan -> spawn -> save -> family bind -> audit`.
- `metadata.residents` ramane solutia temporara pentru case cu mai multi locuitori.
- Rutina actuala este suficienta pentru comportament controlat, nu pentru pathfinding natural.

Avertizari:

- Generatorul real care produce automat `HouseAllocation` dintr-un `SettlementPlan` nu este inca gata.
- `homePlaceId/workPlaceId/socialPlaceId` nu sunt persistate in model dedicat.
- `quest_anchor_bindings` exista initial si are audit/comanda admin read-only.
- Daca serverul nu are mapping importat/configurat, codul cade pe fallback-uri vanilla.
- Spawn-ul batch are rollback practic pentru NPC-urile create, dar nu tranzactie DB completa.

## Faza 2: First Playable Release

Scop:

- un scenariu medieval mic, instalabil si jucabil cap-coada
- NPC-uri cu roluri clare
- questuri completabile si persistente
- documentatie scurta pentru admin

Documente:

- `roadmap-orientativ.md`
- `questuri-avansate.md`
- `story-si-context-ai.md`
- `reactie-npc-jucator.md`
- `betonquest-directii-potrivite-pentru-ainpc.md`

Observatii:

- First playable trebuie sa prioritizeze continutul testabil, nu infrastructura infinita.
- Questurile existente sunt functionale la nivel de baza, dar modelul multi-quest matur poate astepta.
- Story-ul pentru first playable poate folosi `StoryContextService`, dar nu trebuie sa depinda de generare libera sau story state nepersistat.
- Reactiile NPC-jucator trebuie extinse incremental peste memoria si emotiile existente.

Avertizari:

- Nu bloca release-ul pe economie, reputatie globala sau RPG complet.
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

- `questuri-avansate.md`
- `story-si-context-ai.md`
- `npc-uri-temporare-si-episodice.md`
- `mapping.md`
- `strategie-plugin-modular-si-scenarii-programabile.md`

Observatii:

- Directia corecta este `ScenarioActionRegistry`, `ScenarioConditionRegistry`, `ScenarioTriggerRegistry` si validator separat.
- Obiectivele `visit_place` si `inspect_node` exista initial dupa `visit_region`.
- `WorldContextSnapshot`, `QuestAnchorResolver`, `quest_anchor_bindings` si `StoryContextService` exista initial; urmatorul strat recomandat este story state persistent.
- NPC-urile temporare trebuie sa foloseasca persistenta light sau deloc, in functie de scop.

Avertizari:

- Nu transforma `ScenarioEngine` intr-o clasa si mai mare.
- Nu lega questurile de coordonate brute cand exista mapping semantic disponibil.
- Nu lasa AI-ul sa genereze story/quest executabil fara validare de ancore.
- Nu trata `StoryContextService` ca runtime de story; este doar snapshot read-only pana exista `region_story_state` si `story_events`.
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

- Directia fara WorldEdit este buna pentru MVP deoarece reduce dependintele obligatorii.
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
- `roadmap-orientativ.md` este sursa principala pentru prioritatea de produs.
- `debugging-si-testare.md` este sursa principala pentru cum verifici ca ceva chiar merge.
- `TODO.md` ramane lista de lucru, nu documentatie tehnica completa.

## Avertizari Globale

- Nu trata `0 warnings` in audit ca garantie ca gameplay-ul este complet.
- Nu introduce generare automata fara dry-run si validare de plan.
- Nu construi modele persistente noi fara strategie de migration/backfill.
- Nu extinde API-ul public cu metode care expun accidental structura interna.
- Nu modifica simultan worldgen, spawn, rutine si questuri daca nu exista teste care sa le lege.
