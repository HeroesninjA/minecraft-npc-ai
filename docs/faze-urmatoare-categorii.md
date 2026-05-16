# Faze Urmatoare pe Categorii

Actualizat: 2026-05-09

## Scop

Acest document strange intr-un singur loc fazele urmatoare din documentatia existenta si le grupeaza pe categorii si subcategorii de lucru.

Rolul lui este operational:

- ajuta la alegerea urmatorului task;
- arata ce depinde de ce;
- separa ce trebuie facut acum de ce trebuie amanat;
- leaga fiecare categorie de documentele canonice.

Nu inlocuieste documentele mari. `implementat-deja.md` ramane sursa pentru ce exista in cod, iar documentele de specialitate raman sursa pentru detalii tehnice.

## Surse analizate

Sinteza este bazata pe:

- indexurile din `docs/README.md` si `docs/categorii/`;
- `TODO.md`;
- `implementat-deja.md`;
- `faze-observatii-avertizari.md`;
- `roadmap-orientativ.md`;
- documentele de domeniu pentru mapping, spawn, questuri, progression, story, dialog, simulare, API, addonuri, generare si operare.
- `lucru-alternat-quest-mapping-progression.md`, pentru cadenta dintre mapping concret, quest/contract jucabil, GUI peste snapshot-uri si extractie mica spre `ProgressionService`.

## Legenda

| Eticheta | Sens |
|---|---|
| `ACUM` | trebuie lucrat inainte de urmatorul salt mare |
| `URMATOR` | intra imediat dupa validarea fazei curente |
| `DUPA DEMO MATUR` | util, dar nu trebuie sa blocheze demo-ul playable intern si stabil |
| `AMANAT` | risc de overengineering daca este inceput prea devreme |

## Principiu de maturitate

`First playable` inseamna demo intern/testabil, nu lansare publica. Obiectivul este sa existe un traseu medieval mic, verificabil cap-coada, care poate arata direct riscurile reale din questuri, spawn, persistenta si story.

Lansarea publica ramane o faza ulterioara. Nu se grabeste doar pentru ca demo-ul porneste.

Gate-uri minime inainte de orice `production-public`:

- demo-ul trece smoke test pe Paper dupa restart;
- questurile demo se pot accepta, progresa si finaliza fara interventii manuale fragile;
- `audit` si `debugdump` explica mapping, NPC bindings, quest/progression si story state;
- exista backup, rollback si plan de migration pentru date reale;
- addonul medieval nu depinde de internals instabile sau configuratie nedocumentata.

## Ordinea scurta recomandata

1. Smoke test Paper pentru mapping/spawn: `world demo create -> settlement plan -> settlement spawn -> audit -> save -> reload`.
2. Smoke test Paper pentru questuri/progresii: Q01-Q08 plus C01/C02, D01, B01/B02, E01, T01 si R01, cu progres, anchors, story events si reload.
3. Inspectie si debug mai bune pentru datele persistente: `npc_world_bindings`, story state/events, progression exports.
4. Generator narativ de populatie pe regiune: nume, roluri, familii, home/work/social.
5. First playable medieval demo: 5-10 NPC-uri, 3-5 questuri completabile, documentatie scurta pentru admin/tester.
6. Hardening pentru spawn si migrari: backup, rollback, transaction/compensation si debugdump.
7. Extractii controlate: `ProgressionService`, `SimulationService`, registri de scenarii.
8. API/addon stabilization si schema YAML compacta.
9. Generare/worldgen si authoring AI numai peste validatoare si dry-run.

## Faze globale

| Faza | Nume | Obiectiv | Status | Nu incepe urmatoarea pana cand |
|---|---|---|---|---|
| F0 | Baseline verificabil | Codul, TODO-ul si documentatia reflecta aceeasi realitate | continuu | `mvn test`, audit/debugdump si docs sunt sincronizate |
| F1 | Paper smoke mapping/spawn | Mapping demo si spawn pe regiune merg pe server real | ACUM | save/reload nu pierde mapping, NPC bindings sau ancore |
| F2 | Paper smoke quest/progression | Q01-Q08, C01/C02, D01, B01/B02, E01, T01 si R01 pot fi inspectate si testate cap-coada | ACUM | progresul, stages, anchors si story events supravietuiesc reload-ului |
| F3 | First playable medieval demo | Un demo mic este instalabil si jucabil de admin/tester | URMATOR | exista 3-5 questuri completabile si NPC-uri cu roluri clare |
| F4 | Observabilitate si hardening date | Datele partiale pot fi vazute, exportate si reparate controlat | URMATOR | debugdump/audit acopera mapping, NPC bindings, quest/progression, story |
| F5 | Extractie servicii runtime | Motoarele mari sunt sparte fara schimbari functionale riscante | DUPA DEMO MATUR | exista teste/smoke pentru fluxul pe care il extragi |
| F6 | API, addonuri si schema | Addonurile folosesc contracte clare, nu internals | DUPA DEMO MATUR | addonul medieval este exemplu stabil |
| F7 | Generare si authoring AI | AI produce drafturi validate, nu executa direct | AMANAT | exista validatoare, template-uri si dry-run mature |
| F8 | Productie publica | Release cu backup, rollback, migrari si operare clara | DUPA DEMO MATUR | demo-playable trece restart, audit, debugdump, backup si rollback |

## Categoria A - Stare, roadmap, audit si operare

Documente principale:

- `implementat-deja.md`
- `faze-observatii-avertizari.md`
- `roadmap-orientativ.md`
- `audit.md`
- `debugging-si-testare.md`
- `server-admin-runbook.md`
- `release-checklist.md`
- `documentatie-lipsa.md`

### A1. Sincronizare stare proiect

Status: `ACUM`, continuu.

Livrabile:

- `TODO.md` actualizat dupa schimbari mari;
- `implementat-deja.md` actualizat cand ceva trece din design in implementat initial;
- documentul de specialitate actualizat pentru fiecare schimbare;
- indexurile din `docs/README.md` si `docs/categorii/` tinute la zi.

Urmatorul task bun:

- dupa fiecare sesiune mare pe quest/progression, actualizeaza `faze-observatii-avertizari.md` si documentul curent de faze.

### A2. Audit si debugdump

Status: `ACUM`.

Livrabile:

- audit/debugdump dedicat pentru story state si story events exista initial prin `/ainpc audit quest`, `/ainpc debugdump quest` si `/ainpc debugdump story`;
- inspectie read-only pentru `npc_world_bindings` exista initial prin `/ainpc world bindings ...`;
- export offline pentru `npc_world_bindings` exista initial prin `npc-world-bindings.json` in debugdump `world/all`;
- debugdump pentru settlement spawn, rollback si bindings;
- verificari clare pentru pack-uri invalide, capabilities si dependencies;
- output de debug fara date sensibile.

Subcategorii:

| Subcategorie | Documente | Urmatorul pas |
|---|---|---|
| Audit DB | `audit.md`, `npc-world-bindings.md` | backfill/migration si household bindings persistente |
| Debugdump quest/progression | `questuri-avansate-v2.md`, `progression-service.md` | verifica pe Paper exporturile `player-progressions.json` si `player-quest-progress.json` |
| Debugdump story | `story-context-service.md`, `story-si-context-ai.md` | export dedicat pentru state/events, nu doar context read-only |
| Debugdump spawn | `ordine-spawn-npc-cladiri-region-node.md` | raport pentru plan, NPC creati, rollback si esecuri |

### A3. Release, backup si migration

Status: `PARTIAL IMPLEMENTAT`.

Livrabile:

- document nou `migration-si-backup.md`;
- politica de backup inainte de migration/backfill;
- rollback rapid pentru server cu date reale;
- verificari Paper pentru `dev-snapshot`, `paper-test`, `demo-playable`, `production-public`.

Nu face:

- migration/backfill fara backup;
- release doar pe baza de `mvn package`;
- activare debug extins pe server public fara politica de loguri.

## Categoria B - World mapping, spawn si populatie

Documente principale:

- `mapping.md`
- `mapping-harti-manuale.md`
- `npc-world-bindings.md`
- `ordine-spawn-npc-cladiri-region-node.md`
- `settlement-plan.md`
- `generare-populatie-narativa.md`
- `households-persistente.md`
- `prevenire-duplicare-npc.md`

### B1. Smoke test mapping/spawn pe Paper

Status: `ACUM`.

Flux minim:

```text
/ainpc world demo create demo_sat
/ainpc world settlement plan demo_sat
/ainpc world settlement spawn demo_sat
/ainpc audit world
/ainpc audit spawn
/ainpc audit db
/ainpc world save
```

Dupa restart:

```text
/ainpc audit world
/ainpc audit spawn
/ainpc audit db
/ainpc world places demo_sat
```

Criteriu de gata:

- mapping-ul ramane dupa reload;
- demo-ul este creat intr-o zona relativ plata si nu inghesuie ancorele de casa/work/social;
- NPC-urile generate nu se dubleaza;
- NPC-urile convertite nu fug haotic si au rutina inspectabila prin `/ainpc routine status`;
- `npc_source_keys` ramane coerent dupa restart si chunk reload;
- `npc_world_bindings` ramane coerent;
- auditul poate explica problemele ramase.

### B2. Anti-duplicare NPC dupa restart

Status: `ACUM / URMATOR IMEDIAT`.

Livrabile:

- smoke test Paper pentru `/ainpc duplicates`, `/ainpc repair duplicates dryrun`, restart si chunk reload;
- verificare ca `npc_source_keys` se creeaza si se reindexeaza pe DB existent;
- cleanup legacy numai cu backup, dry-run si comenzi admin dupa ID;
- criteriu de release: zero duplicate dupa `npc_id`, `source_key` si entitati live marcate AINPC;
- debugdump extins cu owner canonic, DB UUID, entity UUID si source key.

Nu face:

- cleanup SQLite manual cu serverul pornit;
- spawn household/settlement repetat fara audit dupa restart;
- stergere dupa nume cand exista coliziuni.

### B3. Inspectie si backfill pentru NPC bindings

Status: `ACUM`.

Livrabile:

- extindere inspectie/backfill pentru `npc_world_bindings`;
- audit pentru place/node lipsa sau tip incompatibil;
- backfill controlat din `profile_data.owned_locations`;
- clarificare cand `profile_data` este fallback si cand DB-ul dedicat este sursa.
- sincronizare cu `npc_source_keys`, ca schimbarea casei/workplace sa nu recreeze NPC-ul.

### B4. Household-uri persistente

Status: `IMPLEMENTAT INITIAL / URMATOR`.

Livrabile:

- tabele `households` si `household_residents`;
- migration din `metadata.residents` unde este sigur;
- audit pentru case fara node-uri, rezidenti orfani si roluri conflictuale;
- integrare in rutina home/work/social.
- constrangere unica pe resident logic, legata de `source_key` canonic.

### B5. Generator narativ de populatie

Status: `URMATOR`.

Livrabile:

- `PopulationPlan` dry-run;
- nume, varste, roluri, relatii si profesii;
- distributie pe case, locuri de munca si social;
- reguli de capacitate;
- conversie determinista in `HouseAllocation`.

Nu face:

- spawn direct din generator fara plan inspectabil;
- generare libera prin AI fara validator.

### B6. Hardening spawn pe regiune

Status: `URMATOR`.

Livrabile:

- tranzactie DB completa unde este posibil;
- compensare documentata unde tranzactia completa nu este posibila;
- test pentru esec la household intermediar;
- debugdump cu plan, pasi executati, rollback si efecte ramase.

## Categoria C - Quest, story, progression si dialog

Documente principale:

- `questuri-faza-1-stabilizare.md`
- `pregatire-questuri-avansate.md`
- `questuri-avansate-v2.md`
- `progression-service.md`
- `lucru-alternat-quest-mapping-progression.md`
- `quest-anchor-bindings.md`
- `story-context-service.md`
- `story-si-context-ai.md`
- `dialog-si-conversatii.md`
- `interactiuni.md`
- `environment-context-si-engine.md`

### C1. Smoke test Q01-Q08 si C01

Status: `ACUM`.

Livrabile:

- Q01-Q05 testate cap-coada pe server Paper;
- Q06-Q08 testate pe mapping demo, cu stages si anchors;
- C01 testat ca progres non-`QUEST` expus ca `contract`;
- `quest log`, `progression log`, `contract log` verificate;
- `debugdump quest` verificat pentru fisierele:
  - `loaded-quest-definitions.json`;
  - `player-progressions.json`;
  - `player-quest-progress.json`;
  - `quest-anchor-bindings.json`;
  - `story-states.json`;
  - `story-events.json`;
  - `quest-audit-report.txt`.

### C2. First playable medieval demo

Status: `URMATOR`.

Livrabile:

- 3-5 questuri completabile intr-un sat demo;
- NPC-uri cu roluri clare: fierar, fermier, negustor, gardian, localnic;
- obiective pe conversatie, iteme, `visit_place`, `inspect_node`, `visit_region`, `kill_mob`;
- rewards simple si verificabile;
- story events la completare;
- progres stabil dupa reload.

Nu face:

- branching complex inainte ca fluxul liniar sa fie stabil;
- reputatie globala inainte de primul demo jucabil;
- AI care decide direct statusul questului.

### C3. ProgressionService generic

Status: `URMATOR`, inceput initial.

Ce exista initial:

- feature packs pot declara `mechanics`;
- scenariile pot seta `mechanic` si `progress/progression`;
- `quest`, `progression` si `contract` sunt fatade peste runtime comun;
- model intern `ProgressionDefinition`;
- `ProgressionSelector`;
- snapshot-uri `ProgressionStatusSnapshot`, `ProgressionProgressSnapshot`, `ProgressionGuiSnapshot`, `ProgressionGuiEntry` si `ProgressionStageSnapshot`;
- `ProgressionRepository` read-only peste `player_quests`, expus prin `ProgressionService`;
- `player-progressions.json` exista ca export generic peste `player_quests`.

Livrabile urmatoare:

- migrare planificata spre `player_progressions`, numai dupa stabilizarea modelului.

### C4. Story state auditabil

Status: `ACUM`, implementat initial.

Ce exista initial:

- `StoryContextService` read-only;
- `StoryStateService` pentru `region_story_state`, `place_story_state`, `story_events`;
- comenzi read-only pentru region/place/events;
- actiuni `set_story_state` si `record_story_event`.
- `/ainpc audit quest` valideaza JSON-ul din story state/events;
- `/ainpc debugdump quest` si `/ainpc debugdump story` exporta `story-states.json` si `story-events.json`.

Livrabile urmatoare:

- document canonic `story-state-service.md`;
- validator mai strict pentru story actions;
- reguli pentru chei, scope, event types si retention.

### C5. Dialog quest-aware si story-aware

Status: `URMATOR`.

Livrabile:

- `DialogueContextSnapshot` compact;
- fallback-uri deterministe pentru intentii de quest;
- raspunsuri diferite in functie de quest stage si story state;
- debug pentru prompt, model, fallback si intentie aleasa.

Regula:

- dialogul prezinta si explica; progresul real ramane in servicii deterministe.

## Categoria D - NPC, rutine, simulare si environment

Documente principale:

- `rutine-npc-si-timeline.md`
- `simulare-sat-si-lume.md`
- `simulation-service.md`
- `simulation-service-partea-2.md`
- `simulation-service-partea-3.md`
- `simulation-service-partea-4.md`
- `environment-context-si-engine.md`
- `reactie-npc-jucator.md`
- `npc-uri-temporare-si-episodice.md`

### D1. Rutina peste bindings stabile

Status: `IMPLEMENTAT INITIAL / URMATOR`.

Ce exista initial:

- villagerii convertiti in AINPC primesc control de baza impotriva miscarii vanilla haotice;
- rutina are cooldown si praguri mai mari pentru mutari intre ancore;
- `NpcInteractionGui` afiseaza activitatea de rutina curenta.

Livrabile:

- rutina consuma explicit `npc_world_bindings`;
- fallback clar cand lipseste binding-ul;
- audit pentru NPC fara home/work/social;
- reguli pentru intreruperea rutinei de catre interactiuni si questuri.

### D2. SimulationService extractie fara schimbare functionala

Status: `DUPA DEMO MATUR`, dar pregatit in documentatie.

Livrabile:

- API minim pentru tick, preview si status;
- summary de tick;
- comenzi `/ainpc simulation tick/status/preview`;
- debugdump si audit;
- feature flag pentru rollout.

Regula:

- prima extractie nu trebuie sa schimbe gameplay-ul; doar muta responsabilitatea intr-un serviciu clar.

### D3. Semnale de simulare si consumatori controlati

Status: `DUPA DEMO MATUR`.

Livrabile:

- `SimulationSignal`;
- reguli cu cooldown si deduplicare;
- snapshot pe settlement;
- consumatori expliciti: quest, story, AI;
- audit pentru semnale generate si ignorate.

### D4. EnvironmentContextService si EnvironmentEngine

Status: context read-only `URMATOR`, engine complet `AMANAT`.

Livrabile apropiate:

- snapshot read-only pentru vreme, timp, regiune, siguranta, activitate locala;
- conditii de quest bazate pe mediu;
- debug admin pentru context.

Engine complet se amana pana exista questuri sistemice reale care il cer.

## Categoria E - API, modularizare, addonuri si scenarii extensibile

Documente principale:

- `documentatie-api.md`
- `strategie-plugin-modular-si-scenarii-programabile.md`
- `refactorizare-si-impartire-pe-module.md`
- `betonquest-directii-potrivite-pentru-ainpc.md`
- `progression-service.md`

### E1. API public stabil

Status: `DUPA DEMO MATUR`.

Livrabile:

- separare clara intre `ainpc-api` si internals core;
- contracte versionate;
- compatibilitate documentata;
- ghid oficial `addon-developer-guide.md`.

### E2. Validator pentru feature packs

Status: `URMATOR/DUPA DEMO MATUR`.

Livrabile:

- `scenario-pack-schema.md`;
- verificare `dependencies`;
- verificare `capabilities`;
- mesaje clare la load;
- raport de validare pentru admin.

### E3. Registri pentru scenarii

Status: `IMPLEMENTAT INITIAL / INTEGRARE DUPA DEMO MATUR`.

Livrabile:

- `ScenarioActionRegistry` - exista initial;
- `ScenarioConditionRegistry` - exista initial;
- `ScenarioTriggerRegistry` - exista initial;
- `ScenarioExecutionContext` - exista initial;
- `ScenarioVariableProvider` - exista initial;
- `ScenarioValidationReport` - exista initial;
- integrare efectiva in `ScenarioEngine` - lipseste;
- validator complet pentru feature packs - lipseste.

Regula:

- registrii se introduc incremental, cand extragem din `ScenarioEngine`, nu prin rescriere brusca.

### E4. Addon medieval ca exemplu curat

Status: `URMATOR`.

Livrabile:

- addonul medieval ramane separat;
- pack-ul sau demonstreaza questuri, contracte, story actions si mapping anchors;
- fara dependenta de internals instabile;
- teste de contract pentru continut.

## Categoria F - Generare, worldgen si constructie asistata

Documente principale:

- `settlement-plan.md`
- `patch-planner.md`
- `template-cladiri-si-marker-nodes.md`
- `worldedit-integration-contract.md`
- `generare-sate-fara-worldedit.md`
- `generare-sate-worldedit-si-npc.md`
- `generare-ai-si-constructie-automata.md`

### F1. SettlementPlan si PatchPlanner

Status: `DUPA DEMO MATUR`, cu piese pregatite.

Livrabile:

- `SettlementPlan` validabil;
- `GapReport`;
- `PatchPlan`;
- prioritizare pentru case, locuri de munca, piata, nodes;
- dry-run si raport, fara executie directa.

### F2. Template cladiri si marker nodes

Status: `DUPA DEMO MATUR`.

Livrabile:

- catalog read-only de template-uri;
- marker transform determinist;
- output semantic `WorldPlace` si `WorldNode`;
- validari pentru marker-ele obligatorii.

### F3. WorldEdit optional

Status: `AMANAT` pentru demo playable.

Livrabile:

- adapter optional `ainpc-integration-worldedit`;
- detectie optionala;
- paste controlat;
- rollback;
- fallback clar cand WorldEdit lipseste.

Regula:

- WorldEdit nu devine dependinta obligatorie a core-ului.

### F4. AI pentru authoring

Status: `AMANAT`.

Livrabile:

- drafturi YAML si planuri;
- validare schema/capabilities/anchors;
- raport de validare;
- import controlat.

Nu face:

- `prompt -> spawn/build/DB write`;
- AI care modifica lumea fara dry-run si confirmare.

## Categoria G - GUI si UX admin

Documente principale:

- `gui-interfete.md`
- `interactiuni.md`
- `debugging-si-testare.md`
- `audit.md`
- `progression-service.md`

### G1. Quest/Progression GUI

Status: `IMPLEMENTAT INITIAL / URMATOR`.

Livrabile:

- filtre interactive pentru quest/contract/duty/bounty/event/tutorial/ritual;
- grupare dupa mecanica;
- status obiective si stage-uri;
- actiuni rapide sigure;
- debug admin separat de actiunile jucatorului.

Implementat initial:

- Quest GUI consuma `ProgressionGuiSnapshot`;
- quest log-ul are filtre interactive pe mecanicile curente;
- quest log-ul are paginare initiala si grupare dupa mecanica;
- `/ainpc gui quest <filter>` si `/quest gui <filter>` deschid direct view-ul filtrat.

Urmator:

- include verificarea GUI in fiecare slice din `lucru-alternat-quest-mapping-progression.md`;
- sincronizeaza detaliile GUI cu `ProgressionStatusSnapshot` si `ProgressionProgressSnapshot`;
- marcheaza vizual warnings din audit cand o definitie/progresie are mapping lipsa.

### G2. World GUI

Status: `URMATOR/DUPA DEMO MATUR`.

Livrabile:

- regions/places/nodes browse;
- inspectie household/settlement plan;
- audit vizual;
- selectii pentru binding-uri.

### G3. Debug/Audit GUI

Status: `URMATOR`.

Livrabile:

- butoane pentru audituri existente;
- rulare debugdump;
- protectii la actiuni destructive;
- mesaje clare pentru fisiere generate.

## Dependinte critice

| Nu incepe | Pana cand nu exista |
|---|---|
| Questuri mari pe locatie | mapping demo si `quest_anchor_bindings` verificate pe Paper |
| First playable demo | Q01-Q08/C01 smoke si reload test |
| Migrare `player_progressions` | model `ProgressionDefinition` stabil si exporturi verificate |
| Runtime scenarii extensibil | questuri demo stabile si teste pentru actiuni existente |
| Authoring AI | validator schema/capabilities/anchors |
| Worldgen automat | `SettlementPlan`, `PatchPlan`, dry-run si rollback |
| Reputatie/economie | demo medieval jucabil fara ele |
| API public extern | addon medieval curat si fara internals instabile |

## Ce trebuie amanat explicit

- economie completa;
- reputatie globala pe factiuni;
- RPG levels/skills;
- branching complex;
- scripting general;
- worldgen complet;
- AI cu tool calls care executa direct;
- ecosistem mare de addonuri;
- NPC non-villager ca platforma generala.

Acestea pot deveni faze bune mai tarziu, dar acum ar creste riscul si ar intarzia primul demo jucabil.

## Checklist pentru alegerea urmatorului task

Inainte sa alegi un task, verifica:

- ajuta direct primul scenariu medieval jucabil?
- reduce riscul de date corupte sau greu de diagnosticat?
- poate fi verificat prin test, audit, debugdump sau smoke Paper?
- are document canonic unde poate fi explicat?
- evita cresterea monolitului `ScenarioEngine`?
- foloseste mapping semantic in loc de coordonate brute?
- are fallback sigur daca lipseste config sau mapping?

Daca raspunsul este "nu" la mai multe puncte, taskul probabil trebuie amanat sau spart in subtaskuri mai mici.

## Documente noi recomandate

Prioritate mare:

- `migration-si-backup.md`
- `story-state-service.md`
- `scenario-pack-schema.md`

Prioritate medie:

- `addon-developer-guide.md`
- `prompt-safety-guide.md`
- `observability-and-logs.md`
- `test-fixtures-and-demo-world.md`

## Rezumat final

Ordinea corecta este:

```text
baseline verificabil
-> Paper smoke mapping/spawn
-> Paper smoke quest/progression
-> first playable medieval demo
-> observabilitate si hardening date
-> extractii servicii runtime
-> API si addonuri
-> generare/worldgen
-> authoring AI
```

Primul obiectiv real ramane un demo medieval mic, stabil si jucabil cap-coada.
