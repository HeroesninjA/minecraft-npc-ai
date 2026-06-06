# Documentatie

Actualizat: 2026-06-04

Acest folder contine documentatia tehnica locala a proiectului. Documentele nu au toate acelasi rol: unele descriu ce exista deja in cod, altele sunt design pentru faze viitoare.

## Citire rapida

Ordinea recomandata pentru orientare:

1. `constitutie-proiect.md`
2. `audit-constitutie-proiect.md`
3. `implementat-deja.md`
4. `faze-observatii-avertizari.md`
5. `faze-urmatoare-categorii.md`
6. `server-npc-mvp-si-faze.md`
7. `prim-demo-functionalitate-minima-diversa.md`
8. `mcp-docker-server-mvp-si-faze.md`
9. `coding-automation-stack-linux-vscode-deepseek-mcp.md`
10. `faze-urmatoare-250.md`
11. `faze-urmatoare-250-partea-2.md`
12. `playable-village-ux.md`
13. `ordine-spawn-npc-cladiri-region-node.md`
14. `simulare-sat-si-lume.md`
15. `simulation-service.md`
16. `simulation-service-partea-2.md`
17. `simulation-service-partea-3.md`
18. `simulation-service-partea-4.md`
19. `environment-context-si-engine.md`
20. `dialog-si-conversatii.md`
21. `interactiuni.md`
22. `gui-interfete.md`
23. `ai-orchestrare-si-mecanici.md`
24. `spring-ai-mcp-serviciu-intern.md`
25. `generare-automata-questuri-ai.md`
26. `story-si-context-ai.md`
27. `story-context-service.md`
28. `progression-service.md`
29. `player-onboarding-initiere.md`
30. `lucru-alternat-quest-mapping-progression.md`
31. `roadmap-orientativ.md`
32. `server-admin-runbook.md`
33. `release-checklist.md`
34. `debugging-si-testare.md`
35. `prevenire-duplicare-npc.md`
36. `conversie-java-la-kotlin.md`
37. `conversie-java-la-kotlin-partea-2.md`
38. `conversie-java-la-kotlin-partea-3.md`
39. `conversie-java-la-kotlin-partea-4.md`
40. `conversie-java-la-kotlin-partea-5.md`
41. `rezumat-conversie-java-la-kotlin.md`
42. `kotlin-style-guide.md`
43. `kotlin-interop-api-addonuri.md`
44. `kotlin-paper-packaging-si-smoke.md`
45. `kotlin-migration-tracker.md`
46. `kotlin-code-review-checklist.md`
47. `kotlin-coroutines-paper-policy.md`
48. `kotlin-gradle-activation-plan.md`
49. `kotlin-testing-strategy.md`

## Navigare pe categorii

Documentele canonice raman in radacina `docs/`. Pentru citire pe domenii, foloseste indexurile din `categorii/`:

| Categorie | Index |
|---|---|
| Stare si roadmap | `categorii/00-stare-roadmap/README.md` |
| World mapping si spawn | `categorii/01-world-mapping-spawn/README.md` |
| NPC, rutine si simulare | `categorii/02-npc-rutine-simulare/README.md` |
| Quest, story si AI | `categorii/03-quest-story-ai/README.md` |
| Generare si worldgen | `categorii/04-generare-worldgen/README.md` |
| API, modularizare si addonuri | `categorii/05-api-modularizare-addonuri/README.md` |
| Operare si hardening | `categorii/06-operare-hardening/README.md` |
| Referinte si arhiva | `categorii/07-referinte-arhiva/README.md` |

Pentru documente care lipsesc sau merita separate, vezi `documentatie-lipsa.md`.

## Faze principale

| Faza | Scop | Documente principale | Status scurt |
|---|---|---|---|
| 0 | Baseline, audit si verificare stare curenta | `implementat-deja.md`, `audit.md`, `debugging-si-testare.md`, `analiza-erori-si-plan-rezolvare.md` | Functional initial; necesita resincronizare dupa schimbari mari |
| 1 | World mapping si spawn order minim verificabil | `mapping.md`, `playable-village-ux.md`, `ordine-spawn-npc-cladiri-region-node.md`, `settlement-plan.md`, `generare-populatie-narativa.md`, `households-persistente.md`, `rutine-npc-si-timeline.md`, `simulare-sat-si-lume.md`, `simulation-service.md`, `simulation-service-partea-2.md`, `simulation-service-partea-3.md`, `simulation-service-partea-4.md` | Nucleu implementat initial; generatorul complet si simularea de comunitate lipsesc |
| 2 | First playable demo intern | `prim-demo-functionalitate-minima-diversa.md`, `playable-village-ux.md`, `roadmap-orientativ.md`, `pregatire-questuri-avansate.md`, `questuri-avansate-v2.md`, `progression-service.md`, `player-onboarding-initiere.md`, `dialog-si-conversatii.md`, `interactiuni.md`, `gui-interfete.md`, `ai-orchestrare-si-mecanici.md`, `generare-automata-questuri-ai.md`, `story-si-context-ai.md`, `story-context-service.md`, `environment-context-si-engine.md`, `reactie-npc-jucator.md`, `simulare-sat-si-lume.md`, `simulation-service.md`, `simulation-service-partea-2.md`, `simulation-service-partea-3.md`, `simulation-service-partea-4.md`, `betonquest-directii-potrivite-pentru-ainpc.md` | Organizare interna pe componente, mecanici si ordine de dezvoltare; story context si story persistence exista initial, continutul demo jucabil lipseste; release-ul public ramane amanat pana la gate-uri de maturitate |
| 3 | Modularizare, API si addonuri | `documentatie-api.md`, `strategie-plugin-modular-si-scenarii-programabile.md`, `refactorizare-si-impartire-pe-module.md` | Baza exista; contractele trebuie stabilizate |
| 4 | Runtime scenarii extensibil | `questuri-avansate-v2.md`, `progression-service.md`, `story-si-context-ai.md`, `npc-uri-temporare-si-episodice.md`, `mapping.md` | Registri initiali exista; integrarea reala in `ScenarioEngine`, validatorul complet si runtime-ul generic raman treptate |
| 5 | Generare sate si authoring asistat | `settlement-plan.md`, `patch-planner.md`, `template-cladiri-si-marker-nodes.md`, `worldedit-integration-contract.md`, `generare-sate-fara-worldedit.md`, `generare-sate-worldedit-si-npc.md`, `generare-ai-si-constructie-automata.md`, `story-si-context-ai.md` | Scanner/mapper si patch planner read-only initial exista; generarea completa ramane viitoare |
| 6 | Hardening productie si livrare | `server-admin-runbook.md`, `release-checklist.md`, `reducere-marime-jar.md`, `audit.md`, `debugging-si-testare.md`, `gui-interfete.md`, `prevenire-duplicare-npc.md` | Backlog tehnic; nu bloca demo-ul matur fara motiv, dar este obligatoriu inainte de productie publica |

## Index documente

| Document | Faza | Rol | Observatie / avertizare |
|---|---:|---|---|
| `constitutie-proiect.md` | toate | Document constitutional cu importanta ridicata pentru identitate, structura, directie, reguli si criterii de maturitate | Foloseste-l cand exista conflict intre backlog, idei noi si reguli de dezvoltare |
| `audit-constitutie-proiect.md` | toate | Audit static al codului fata de constitutia proiectului | Foloseste-l ca backlog de conformitate pentru neutralizarea core-ului, feature flags, addon types si storage |
| `analiza-erori-si-plan-rezolvare.md` | 0 | Raport tehnic si ordine de remediere | Foloseste-l ca backlog verificabil, nu ca lista automata de buguri active |
| `ai-orchestrare-si-mecanici.md` | 2, 3, 4, 5 | Contract pentru AI transversal peste dialog, questuri, story, environment, reactii, generare si debug | Foloseste `AIOrchestrationService` ca orchestrator, nu ca god service de gameplay |
| `spring-ai-mcp-serviciu-intern.md` | 2, 3, 4, 6 | Design pentru Spring AI MCP ca sidecar runtime intern langa pluginul Paper | Nu incarca Spring Boot direct in JAR-ul Paper; incepe cu tool-uri read-only si fallback |
| `audit.md` | 0, 6 | Documentatie pentru `/ainpc audit` si backlog securitate | Auditul runtime este read-only si nu dovedeste securitate completa |
| `betonquest-directii-potrivite-pentru-ainpc.md` | 2, 4 | Inspiratie pentru questuri mature | Nu copia modelul BetonQuest integral inainte de stabilizarea runtime-ului propriu |
| `debugging-si-testare.md` | 0, 6 | Testare Maven, debug dump, smoke tests | Diferentiaza clar testele locale de testele pe server Paper real |
| `conversie-java-la-kotlin.md` | 3, 6 | Plan pe faze pentru introducerea Kotlin in proiectul Gradle | Migrarea trebuie facuta incremental; pastreaza `ainpc-api` Java-friendly pana exista motiv clar sa il schimbi |
| `conversie-java-la-kotlin-partea-2.md` | 3, 6 | Runbook operational pentru executia conversiei Java -> Kotlin | Foloseste-l pentru ordinea slice-urilor, gate-uri, rollback si riscuri de interop |
| `conversie-java-la-kotlin-partea-3.md` | 3, 6 | Retete concrete pentru configurare Gradle Kotlin, conversii si interop Java | Foloseste-l cand incepi implementarea efectiva a primelor fisiere `.kt` |
| `conversie-java-la-kotlin-partea-4.md` | 3, 6 | Harta de conversie pe pachetele reale din repo | Stabileste ce pachete si clase se convertesc devreme, mediu sau tarziu |
| `conversie-java-la-kotlin-partea-5.md` | 3, 6 | Controlul executiei conversiei Kotlin: tracking, teste, JAR audit, smoke si rollback | Foloseste-l dupa primele slice-uri, ca sa decizi daca migrarea este stabila |
| `rezumat-conversie-java-la-kotlin.md` | 3, 6 | Rezumatul celor 5 parti pentru migrarea Java -> Kotlin | Punct rapid de pornire inainte de implementare sau revizuire |
| `kotlin-style-guide.md` | 3, 6 | Reguli de stil Kotlin pentru codul AINPC | Stabileste cum se folosesc `data class`, `object`, nullability, sealed, colectii si servicii |
| `kotlin-interop-api-addonuri.md` | 3, 6 | Contract Java interop pentru API si addonuri | Obligatoriu inainte de orice tip Kotlin expus catre Java sau addonuri |
| `kotlin-paper-packaging-si-smoke.md` | 6 | Runbook pentru JAR, runtime Kotlin si smoke test Paper | Foloseste-l dupa prima clasa Kotlin de productie sau orice schimbare de packaging |
| `kotlin-migration-tracker.md` | 3, 6 | Tracker operational pentru slice-urile Kotlin | Actualizeaza-l dupa fiecare conversie sau smoke relevant |
| `kotlin-code-review-checklist.md` | 3, 6 | Checklist de review pentru schimbari Kotlin | Foloseste-l la fiecare PR/slice care adauga `.kt` sau modifica packaging/API |
| `kotlin-coroutines-paper-policy.md` | 6 | Politica pentru coroutine in context Paper | Coroutine sunt amanate pana exista design separat de lifecycle si scheduling |
| `kotlin-gradle-activation-plan.md` | 3, 6 | Plan exact pentru activarea Kotlin in Gradle | Primul document de executie pentru KOT-001/F001 |
| `kotlin-testing-strategy.md` | 3, 6 | Strategie de testare pentru conversiile Kotlin | Defineste testele minime, tematice, API Java si smoke Paper |
| `dialog-si-conversatii.md` | 2, 4, 5 | Evolutia dialogului pe masura ce avanseaza quest, story, environment, memorie si reputatie | Dialogul formuleaza raspunsuri peste context validat; nu trebuie sa decida progres sau reward-uri |
| `documentatie-api.md` | 3 | Contract public curent | Nu trata clasele interne core ca API stabil pentru addonuri |
| `addon-config-template.md` | 3 | Separarea config core vs config addon | Core-ul ramane universal, addonurile isi livreaza propriul template |
| `documentatie-lipsa.md` | toate | Idei de documentatie lipsa si prioritate | Foloseste-l ca backlog de documentatie, nu ca status de implementare |
| `environment-context-si-engine.md` | 2, 4, 5 | Contract pentru context read-only de mediu si EnvironmentEngine viitor | Implementeaza intai `EnvironmentContextService`; `EnvironmentEngine` complet ramane pentru questuri sistemice si world events |
| `faze-observatii-avertizari.md` | toate | Harta centrala pe faze | Document de control pentru citirea restului documentatiei |
| `faze-urmatoare-categorii.md` | toate | Sinteza operationala a fazelor urmatoare, grupate pe categorii si subcategorii | Foloseste-l pentru alegerea urmatorului task; documentele canonice raman sursa pentru detalii |
| `faze-urmatoare-250.md` | toate | Backlog operational cu urmatoarele 250 faze | Lista lunga pentru planificare; executa fazele in slice-uri mici, validate |
| `faze-urmatoare-250-partea-2.md` | toate | Backlog operational F251-F500 pentru generare sate/cladiri, context, comportament realist si API | Continua lista lunga; prioritizeaza config validat, dry-run, context read-only si documentare API |
| `server-npc-mvp-si-faze.md` | toate | Plan operational pentru versiunea minim functionala a serverului NPC si fazele ulterioare | Foloseste-l ca checklist de MVP Paper inainte de mecanici mari sau release public |
| `prim-demo-functionalitate-minima-diversa.md` | 2 | Plan executabil pentru primul demo intern cu mapping, NPC, rutina, dialog, quest, progression, story si audit | Foloseste-l ca script de integrare minima diversa dupa ce MVP-ul Paper de baza porneste |
| `mcp-docker-server-mvp-si-faze.md` | 0, 6 | Plan operational pentru versiunea minim functionala a serverului MCP din Docker si fazele ulterioare | Foloseste-l pentru setup, repair, backup, watcher si integrarea Codex/JetBrains |
| `coding-automation-stack-linux-vscode-deepseek-mcp.md` | 0, 3, 6 | Design pentru stack Linux/VS Code Server de coding automation cu DeepSeek, multi-LLM, MCP, embeddings, reranking si componente extensibile | Este pentru dezvoltare si operare, nu pentru AI runtime al pluginului |
| `generare-ai-si-constructie-automata.md` | 5 | Directie pentru generare AI si template-uri | AI-ul trebuie sa genereze drafturi validate, nu sa execute direct modificari in lume |
| `generare-automata-questuri-ai.md` | 2, 4 | Contract pentru generarea asistata de questuri cu AI | AI-ul produce `QuestDraft`, validatorul si adminul decid daca ajunge in pack |
| `generare-populatie-narativa.md` | 1, 2 | Contract pentru generarea dry-run a populatiei pe regiune | Produce `PopulationPlan` si se converteste ulterior in `HouseAllocation`, fara spawn direct |
| `generare-sate-fara-worldedit.md` | 1, 5 | Generare si completare sate vanilla fara WorldEdit obligatoriu | Scannerul/mapperul si patch planner-ul read-only sunt initiale; builder-ul complet lipseste |
| `generare-sate-worldedit-si-npc.md` | 5 | Design pentru integrare optionala WorldEdit | Integrarea WorldEdit trebuie sa ramana optionala, nu dependinta obligatorie a demo-ului |
| `gui-interfete.md` | 2, 4, 6 | Contract de implementare pentru GUI-uri: quest, world, statistici, shop, manager, debug, audit si interactiune NPC | GUI-ul este strat de prezentare peste servicii validate; nu muta logica runtime in inventare |
| `households-persistente.md` | 1, 2 | Contract pentru tabelele `households` si `household_residents` | Design initial; inlocuieste treptat dependenta de `metadata.residents` ca sursa de adevar |
| `implementat-deja.md` | 0 | Rezumat al functionalitatii confirmate in cod | Actualizeaza-l dupa fiecare schimbare de faza care modifica statusul real |
| `interactiuni.md` | 2, 4 | Contract tehnic pentru click, chat privat, sesiuni, ascultare pasiva, intentii de quest si efecte sociale | Interactiunea routeaza intentii catre servicii deterministe; dialogul formuleaza raspunsul, nu decide progresul |
| `lucru-alternat-quest-mapping-progression.md` | 1, 2, 4 | Protocol de lucru alternat intre mapping, questuri, story, `ProgressionService` si GUI system | Foloseste slice-uri mici: mapping concret, story baseline, quest/contract jucabil, GUI peste snapshot-uri, audit/debugdump si abia apoi extractie generic progression |
| `mapping.md` | 1, 4, 5 | Stare, limitari, reguli de consum si evolutie pentru regiuni/places/nodes | Mapping-ul poate exista in cod, dar serverul poate avea 0 regiuni pana la config/import |
| `mapping-harti-manuale.md` | 1, 4, 5 | Ghid pentru harti construite manual, strat semantic validat de admin si directia wand + prompturi naturale | Detectia automata poate propune zone, dar nu trebuie tratata ca adevar semantic |
| `mapping-pentru-implementari-ulterioare.md` | 4, 5 | Redirect istoric catre `mapping.md` | Pastreaza-l doar pentru linkuri vechi; continutul canonic este in `mapping.md` |
| `migration-si-backup.md` | 0, 6 | Runbook pentru backup cu restore-check, migration si rollback operational | Nu rula migration sau cleanup pe date reale fara backup verificat |
| `npc-uri-temporare-si-episodice.md` | 4 | NPC-uri temporare, episodice si non-villager | Pastreaza persistenta light separata de NPC-urile permanente |
| `ordine-spawn-npc-cladiri-region-node.md` | 1, 5, 6 | v2 pentru fazele urmatoare de spawn order | v1 este arhivat in `arhiva/`; v2 se concentreaza pe generator, planuri, persistenta, migration si rollback |
| `patch-planner.md` | 5 | Contract pentru gap analyzer si patch planner peste sate/mapping partial | Produce initial `GapReport`/`PatchPlan` prin `/ainpc patch analyze|plan|validate`, dar nu construieste direct |
| `playable-village-ux.md` | 1, 2, 5 | Criterii pentru sat jucabil, interactiuni NPC clare, rutina stabila, spatiere, teren si questuri vizibile | Prioritizeaza playability inainte de mecanici noi; codul mare fara sat lizibil nu produce demo bun |
| `player-onboarding-initiere.md` | 2, 4, 6 | Design pentru prima intrare a playerului, profil, starter kit, reset controlat si legare cu tutorialul T01 | Profilul playerului este separat de `npc_profiles`; resetul profilului nu trebuie sa stearga questuri fara optiune explicita |
| `prevenire-duplicare-npc.md` | 1, 6 | Runbook pentru prevenirea si remedierea duplicarii NPC | Nu edita DB live; foloseste-l inainte de cleanup manual sau retry de spawn |
| `pregatire-questuri-avansate.md` | 2, 4 | Pregatiri minime si status pentru Q06-Q08, stages liniare, ID-uri stabile, audit si smoke test | Stabilizeaza `objective_id`, stage runtime initial si auditul inainte de branching |
| `progression-service.md` | 2, 4 | Directie pentru runtime generic de progres peste questuri, contracte, datorii, evenimente, tutoriale si ritualuri | `Quest` trebuie sa ramana fatada compatibila; progresul generic nu trebuie sa forteze toate scenariile in quest log |
| `quest-anchor-bindings.md` | 2, 4 | Contract DB pentru ancore semantice de quest | Binding-urile, auditul, comanda admin read-only si exportul `debugdump quest` exista initial |
| `questuri-avansate-v2.md` | 2, 4 | Faze V2 pentru diversitate de questuri, mapping, progres generic si mecanici non-quest | Nu sari direct la branching complex pana cand questurile cap-coada sunt stabile |
| `reactie-npc-jucator.md` | 2, 4 | Reactii bazate pe istoric, emotii si reputatie | Evita sisteme mari de reputatie inainte de primul scenariu jucabil |
| `README.md` | toate | Indexul documentatiei | Pastreaza aici lista completa a documentelor si fazele principale |
| `reducere-marime-jar.md` | 6 | Plan pentru micsorarea JAR-ului | Nu activa `minimizeJar` fara smoke test pe server |
| `release-checklist.md` | 0, 6 | Checklist pentru build, JAR inspect, Paper smoke, audit, restart si rollback | Nu considera release un simplu `mvn package`; cere server smoke si backup pentru date reale |
| `refactorizare-si-impartire-pe-module.md` | 3 | Migrare si spargere pe module/clase | Refactorizarea trebuie facuta incremental, cu teste dupa fiecare pas |
| `roadmap-orientativ.md` | toate | Organizare interna pe componente, mecanici si ordine de dezvoltare | Foloseste-l pentru alegerea ordinii de lucru; `implementat-deja.md` ramane sursa pentru ce exista in cod |
| `rutine-npc-si-timeline.md` | 1, 4 | Rutine zilnice, timeline si hook-uri | Rutina actuala foloseste pathfinding Paper unde poate si teleport doar ca fallback |
| `server-admin-runbook.md` | 0, 6 | Ghid operational pentru instalare, config minim, audit, debugdump si smoke test pe Paper | Nu edita DB live; foloseste backup inainte de cleanup sau upgrade |
| `settlement-plan.md` | 1, 5 | Contract pentru planul complet de regiune/sat inainte de mapping, populatie, spawn sau patch/build | Design initial; planul trebuie validat si inspectat inainte de commit |
| `simulation-service.md` | 1, 2 | Contract tehnic pentru tick-ul periodic de simulare NPC, nevoi, scoring, stare, rutina si persistenta | In cod este inca serviciu logic distribuit intre scheduler, NPC manager, decision engine si routine service |
| `simulation-service-partea-2.md` | 1, 2, 6 | Plan de extractie si hardening pentru `SimulationService`: API, summary, preview, comenzi, audit, debugdump, teste si performanta | Design pentru pasul urmator; primele etape trebuie sa fie refactorizare fara schimbare functionala |
| `simulation-service-partea-3.md` | 2, 4, 6 | Design avansat pentru semnale de simulare, evenimente controlate, agregare pe settlement si consumatori quest/story/AI | Semnalele trebuie sa fie read-only pana exista consumatori expliciti, cooldown, audit si debugdump |
| `simulation-service-partea-4.md` | 1, 2, 6 | Runbook de implementare pentru `SimulationService`: PR slicing, feature flags, migrari, comenzi, test matrix, smoke tests si rollback | Foloseste-l ca checklist de livrare; nu combina refactorul de baza cu gameplay automat |
| `simulare-sat-si-lume.md` | 1, 2, 4 | Contract de ansamblu pentru simulare de comunitate, resurse, economie, reputatie si evenimente | Design initial; NPC-urile trebuie sa devina participanti in simulare, nu singura sursa de adevar |
| `story-context-service.md` | 2, 4 | Context narativ read-only peste mapping, quest anchors si story state persistent | Implementat initial; audit/debugdump story state exista initial, validatorul de story actions ramane de maturizat |
| `story-si-context-ai.md` | 2, 4, 5 | Design pentru story, context AI, mapping semantic si quest anchors | AI-ul trebuie sa consume context validat si limitat, nu coordonate brute sau toata harta |
| `strategie-plugin-modular-si-scenarii-programabile.md` | 3, 4 | Strategie addonuri si scenarii programabile | Registrii de scenarii trebuie introdusi inainte de authoring AI matur |
| `surse-inspiratie-plugin-ainpc.md` | toate | Surse de inspiratie si comparatii | Foloseste-l pentru directie, nu ca specificatie de implementare |
| `template-cladiri-si-marker-nodes.md` | 5 | Contract pentru metadata de template-uri, ancore si marker nodes | Template-urile trebuie sa produca mapping semantic, nu doar blocuri |
| `worldedit-integration-contract.md` | 5 | Contract API pentru integrarea optionala WorldEdit | WorldEdit executa structuri aprobate; planificarea, validarea si mapping-ul raman in core |

## Referinte externe folderului

- `TODO.md` ramane in radacina proiectului ca lista de lucru.
- `docs.text` ramane separat ca document intern de referinta.
- `docs/documentatie/` contine materiale brute, inclusiv PDF-uri si liste auxiliare.
- `docs/arhiva/` contine versiuni vechi pastrate pentru istoric, inclusiv `ordine-spawn-npc-cladiri-region-node-v1.md`.
- `docs/categorii/` contine indexuri pe domenii; nu inlocuieste documentele canonice.

## Regula de mentenanta

Cand codul schimba o faza din "design" in "implementat initial", actualizeaza cel putin:

- `implementat-deja.md`
- `faze-observatii-avertizari.md`
- documentul de specialitate afectat
- `TODO.md`, daca schimba prioritatea curenta
