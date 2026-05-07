# Faze Urmatoare pe Categorii

Actualizat: 2026-05-07

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

## Legenda

| Eticheta | Sens |
|---|---|
| `ACUM` | trebuie lucrat inainte de urmatorul salt mare |
| `URMATOR` | intra imediat dupa validarea fazei curente |
| `DUPA MVP` | util, dar nu trebuie sa blocheze primul release jucabil |
| `AMANAT` | risc de overengineering daca este inceput prea devreme |

## Ordinea scurta recomandata

1. Smoke test Paper pentru mapping/spawn: `world demo create -> settlement plan -> settlement spawn -> audit -> save -> reload`.
2. Smoke test Paper pentru questuri: Q01-Q08 plus contractul C01, cu progres, anchors, story events si reload.
3. Inspectie si debug mai bune pentru datele persistente: `npc_world_bindings`, story state/events, progression exports.
4. Generator narativ de populatie pe regiune: nume, roluri, familii, home/work/social.
5. First playable medieval slice: 5-10 NPC-uri, 3-5 questuri completabile, documentatie scurta pentru admin.
6. Hardening pentru spawn si migrari: backup, rollback, transaction/compensation si debugdump.
7. Extractii controlate: `ProgressionService`, `SimulationService`, registri de scenarii.
8. API/addon stabilization si schema YAML compacta.
9. Generare/worldgen si authoring AI numai peste validatoare si dry-run.

## Faze globale

| Faza | Nume | Obiectiv | Status | Nu incepe urmatoarea pana cand |
|---|---|---|---|---|
| F0 | Baseline verificabil | Codul, TODO-ul si documentatia reflecta aceeasi realitate | continuu | `mvn test`, audit/debugdump si docs sunt sincronizate |
| F1 | Paper smoke mapping/spawn | Mapping demo si spawn pe regiune merg pe server real | ACUM | save/reload nu pierde mapping, NPC bindings sau ancore |
| F2 | Paper smoke quest/progression | Q01-Q08 si C01 pot fi inspectate si testate cap-coada | ACUM | progresul, stages, anchors si story events supravietuiesc reload-ului |
| F3 | First playable medieval | Un demo mic este instalabil si jucabil de admin/tester | URMATOR | exista 3-5 questuri completabile si NPC-uri cu roluri clare |
| F4 | Observabilitate si hardening date | Datele partiale pot fi vazute, exportate si reparate controlat | URMATOR | debugdump/audit acopera mapping, NPC bindings, quest/progression, story |
| F5 | Extractie servicii runtime | Motoarele mari sunt sparte fara schimbari functionale riscante | DUPA MVP | exista teste/smoke pentru fluxul pe care il extragi |
| F6 | API, addonuri si schema | Addonurile folosesc contracte clare, nu internals | DUPA MVP | addonul medieval este exemplu stabil |
| F7 | Generare si authoring AI | AI produce drafturi validate, nu executa direct | AMANAT | exista validatoare, template-uri si dry-run mature |
| F8 | Productie publica | Release cu backup, rollback, migrari si operare clara | DUPA MVP | demo-playable trece restart, audit si smoke test |

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

- audit/debugdump dedicat pentru story state si story events;
- inspectie read-only pentru `npc_world_bindings`;
- debugdump pentru settlement spawn, rollback si bindings;
- verificari clare pentru pack-uri invalide, capabilities si dependencies;
- output de debug fara date sensibile.

Subcategorii:

| Subcategorie | Documente | Urmatorul pas |
|---|---|---|
| Audit DB | `audit.md`, `npc-world-bindings.md` | comanda read-only pentru inspectia bindings |
| Debugdump quest/progression | `questuri-avansate.md`, `progression-service.md` | verifica pe Paper exporturile `player-progressions.json` si `player-quest-progress.json` |
| Debugdump story | `story-context-service.md`, `story-si-context-ai.md` | export dedicat pentru state/events, nu doar context read-only |
| Debugdump spawn | `ordine-spawn-npc-cladiri-region-node.md` | raport pentru plan, NPC creati, rollback si esecuri |

### A3. Release, backup si migration

Status: `URMATOR`.

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
- NPC-urile generate nu se dubleaza;
- `npc_world_bindings` ramane coerent;
- auditul poate explica problemele ramase.

### B2. Inspectie si backfill pentru NPC bindings

Status: `ACUM`.

Livrabile:

- comanda read-only pentru `npc_world_bindings`;
- audit pentru place/node lipsa sau tip incompatibil;
- backfill controlat din `profile_data.owned_locations`;
- clarificare cand `profile_data` este fallback si cand DB-ul dedicat este sursa.

### B3. Household-uri persistente

Status: `URMATOR`.

Livrabile:

- tabele `households` si `household_residents`;
- migration din `metadata.residents` unde este sigur;
- audit pentru case fara node-uri, rezidenti orfani si roluri conflictuale;
- integrare in rutina home/work/social.

### B4. Generator narativ de populatie

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

### B5. Hardening spawn pe regiune

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
- `questuri-avansate.md`
- `progression-service.md`
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
  - `story-events.json`;
  - `quest-audit-report.txt`.

### C2. First playable medieval quest slice

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
- `player-progressions.json` exista ca export generic peste `player_quests`.

Livrabile urmatoare:

- model intern `ProgressionDefinition`;
- `ProgressionSelector`;
- snapshot-uri `ProgressionStatusSnapshot`, `ProgressionProgressSnapshot`, `ProgressionGuiEntry`;
- separarea persistentei in repository;
- migrare planificata spre `player_progressions`, numai dupa stabilizarea modelului.

### C4. Story state auditabil

Status: `ACUM/URMATOR`.

Ce exista initial:

- `StoryContextService` read-only;
- `StoryStateService` pentru `region_story_state`, `place_story_state`, `story_events`;
- comenzi read-only pentru region/place/events;
- actiuni `set_story_state` si `record_story_event`.

Livrabile urmatoare:

- document canonic `story-state-service.md`;
- audit/debugdump dedicat pentru story state;
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

Status: `URMATOR`.

Livrabile:

- rutina consuma explicit `npc_world_bindings`;
- fallback clar cand lipseste binding-ul;
- audit pentru NPC fara home/work/social;
- reguli pentru intreruperea rutinei de catre interactiuni si questuri.

### D2. SimulationService extractie fara schimbare functionala

Status: `DUPA MVP`, dar pregatit in documentatie.

Livrabile:

- API minim pentru tick, preview si status;
- summary de tick;
- comenzi `/ainpc simulation tick/status/preview`;
- debugdump si audit;
- feature flag pentru rollout.

Regula:

- prima extractie nu trebuie sa schimbe gameplay-ul; doar muta responsabilitatea intr-un serviciu clar.

### D3. Semnale de simulare si consumatori controlati

Status: `DUPA MVP`.

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

Status: `DUPA MVP`.

Livrabile:

- separare clara intre `ainpc-api` si internals core;
- contracte versionate;
- compatibilitate documentata;
- ghid oficial `addon-developer-guide.md`.

### E2. Validator pentru feature packs

Status: `URMATOR/DUPA MVP`.

Livrabile:

- `scenario-pack-schema.md`;
- verificare `dependencies`;
- verificare `capabilities`;
- mesaje clare la load;
- raport de validare pentru admin.

### E3. Registri pentru scenarii

Status: `DUPA MVP`.

Livrabile:

- `ScenarioActionRegistry`;
- `ScenarioConditionRegistry`;
- `ScenarioTriggerRegistry`;
- `ScenarioExecutionContext`;
- `ScenarioVariableProvider`;
- `ScenarioValidationReport`.

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

Status: `DUPA MVP`, cu piese pregatite.

Livrabile:

- `SettlementPlan` validabil;
- `GapReport`;
- `PatchPlan`;
- prioritizare pentru case, locuri de munca, piata, nodes;
- dry-run si raport, fara executie directa.

### F2. Template cladiri si marker nodes

Status: `DUPA MVP`.

Livrabile:

- catalog read-only de template-uri;
- marker transform determinist;
- output semantic `WorldPlace` si `WorldNode`;
- validari pentru marker-ele obligatorii.

### F3. WorldEdit optional

Status: `AMANAT` pentru MVP.

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

Status: `URMATOR`.

Livrabile:

- filtre interactive pentru quest/contract/progression;
- grupare dupa mecanica;
- status obiective si stage-uri;
- actiuni rapide sigure;
- debug admin separat de actiunile jucatorului.

### G2. World GUI

Status: `URMATOR/DUPA MVP`.

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
| First playable release | Q01-Q08/C01 smoke si reload test |
| Migrare `player_progressions` | model `ProgressionDefinition` stabil si exporturi verificate |
| Runtime scenarii extensibil | questuri MVP stabile si teste pentru actiuni existente |
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
-> first playable medieval
-> observabilitate si hardening date
-> extractii servicii runtime
-> API si addonuri
-> generare/worldgen
-> authoring AI
```

Primul obiectiv real ramane un demo medieval mic, stabil si jucabil cap-coada.
