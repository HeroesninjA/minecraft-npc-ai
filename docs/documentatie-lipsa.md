# Documentatie Lipsa si Idei Recomandate

Actualizat: 2026-05-16

Acest document listeaza documentatiile care lipsesc sau merita separate din documentele mari existente.

## Prioritate mare

| Document propus | De ce lipseste | Categorie |
|---|---|---|
| `migration-si-backup.md` | Urmeaza modele persistente noi; trebuie strategie de backup, migration si rollback | Operare |

## Prioritate medie

| Document propus | De ce lipseste | Categorie |
|---|---|---|
| `story-state-service.md` | Story-ul are design, dar nu contract pentru flags, events si integrare cu questuri | Quest, story si AI |
| `settlement-plan.md` | Spawn order v2 descrie ideea, dar merita contract separat pentru generator | Generare |
| `patch-planner.md` | Scannerul vanilla exista initial, dar lipseste specificatia pentru completarea lipsurilor | Generare |
| `addon-developer-guide.md` | API-ul exista, dar lipseste ghidul pentru cine scrie addonuri | API si addonuri |
| `scenario-pack-schema.md` | Feature packs exista, dar schema YAML nu este documentata ca referinta compacta | API si questuri |

## Prioritate mica

| Document propus | De ce lipseste | Categorie |
|---|---|---|
| `prompt-safety-guide.md` | Prompturile AI trebuie sa aiba reguli clare de context, limite si fallback | Quest, story si AI |
| `performance-notes.md` | Indexarea si task-urile periodice pot avea impact pe servere mari | Operare |
| `observability-and-logs.md` | Lipsesc conventii pentru loguri, debugdump si date sensibile | Operare |
| `test-fixtures-and-demo-world.md` | Lipseste descrierea unui demo world minimal pentru teste manuale repetabile | Operare |

## Ordine recomandata

1. `migration-si-backup.md`
2. `story-state-service.md`
3. `addon-developer-guide.md`
4. `scenario-pack-schema.md`
5. `prompt-safety-guide.md`

Aceste cinci documente reduc cel mai mult riscul pentru urmatoarele faze, pentru ca proiectul tocmai a inceput sa lege mapping-ul de questuri si AI context.

## Acoperite recent

| Document | Status |
|---|---|
| `release-checklist.md` | Creat ca checklist operational initial pentru build, inspectie JAR, Paper smoke, audit, restart, raport release si rollback rapid |
| `npc-world-bindings.md` | Creat ca document canonic initial pentru legaturi persistente NPC-home/work/social, audit, migration si sincronizare cu `WorldPlace` |
| `server-admin-runbook.md` | Creat ca ghid operational initial pentru instalare, config minim, comenzi de verificare, audit, debugdump, smoke test, backup rapid si troubleshooting |
| `worldedit-integration-contract.md` | Creat ca document canonic initial pentru adapter optional WorldEdit, capabilitati, dry-run, commit, rollback si fallback fara dependinta hard |
| `template-cladiri-si-marker-nodes.md` | Creat ca document canonic initial pentru metadata template, marker nodes, transformari, capabilitati si output `WorldPlace`/`WorldNode` |
| `patch-planner.md` | Creat ca document canonic initial pentru `GapReport`, `PatchCandidate`, `PatchPlan`, prioritizare, validare si integrare cu `SettlementPlan` |
| `settlement-plan.md` | Creat ca document canonic initial pentru plan complet de regiune/sat: region, buildings, nodes, population, households, patch plans, validare si commit |
| `households-persistente.md` | Creat ca document canonic initial pentru `households`, `household_residents`, migration, audit si integrare cu `npc_world_bindings` |
| `generare-populatie-narativa.md` | Creat ca document dedicat pentru generatorul narativ: `PopulationPlan`, household-uri, rezidenti, roluri, validari si conversie catre `HouseAllocation` |
| `simulare-sat-si-lume.md` | Creat ca document canonic initial pentru simulare de comunitate: household-uri, resurse, economie, reputatie, evenimente, quest/story hooks si rolul AI |
| `simulation-service.md` | Creat ca document tehnic pentru serviciul logic de simulare NPC: tick periodic, nevoi, scoring, rutina, persistenta si limite |
| `simulation-service-partea-2.md` | Creat ca plan de extractie si hardening pentru `SimulationService`: API, comenzi, audit, debugdump, teste si performanta |
| `simulation-service-partea-3.md` | Creat ca design avansat pentru semnale de simulare, reguli, cooldown, settlement aggregation si consumatori quest/story/AI |
| `simulation-service-partea-4.md` | Creat ca runbook de implementare pentru `SimulationService`: PR slicing, feature flags, migration, test matrix, smoke tests si rollback |
| `interactiuni.md` | Creat ca document canonic pentru click, chat privat, sesiuni, ascultare pasiva, intentii de quest si directia `InteractionService` |
| `gui-interfete.md` | Creat ca document de implementare pentru GUI-uri: quest, world, statistici, shop, manager, debug, audit si interactiune NPC profesionala |
| `quest-anchor-bindings.md` | Creat dupa implementarea initiala a tabelei `quest_anchor_bindings`; auditul, comanda admin read-only si exportul `debugdump quest` exista initial |
| `story-context-service.md` | Creat dupa implementarea initiala a `StoryContextService`; ramane necesara documentatia separata pentru story state persistent si events |
| `conversie-java-la-kotlin.md` | Creat ca plan principal pe faze pentru migrarea Java -> Kotlin |
| `conversie-java-la-kotlin-partea-2.md` | Creat ca runbook operational pentru executia conversiei Kotlin |
| `conversie-java-la-kotlin-partea-3.md` | Creat cu retete concrete pentru Gradle Kotlin, conversii si interop |
| `conversie-java-la-kotlin-partea-4.md` | Creat ca harta pe pachetele reale din repo |
| `conversie-java-la-kotlin-partea-5.md` | Creat pentru tracking, teste, JAR audit, smoke si rollback |
| `rezumat-conversie-java-la-kotlin.md` | Creat ca rezumat al celor 5 parti |
| `kotlin-style-guide.md` | Creat ca document de stil Kotlin pentru proiect |
| `kotlin-interop-api-addonuri.md` | Creat ca document de compatibilitate Java/API/addonuri |
| `kotlin-paper-packaging-si-smoke.md` | Creat ca runbook pentru runtime Kotlin, JAR audit si Paper smoke |
| `kotlin-migration-tracker.md` | Creat ca tracker operational pentru slice-urile Kotlin |
| `kotlin-code-review-checklist.md` | Creat ca checklist de review pentru schimbari Kotlin |
| `kotlin-coroutines-paper-policy.md` | Creat ca politica pentru coroutine in context Paper |
| `kotlin-gradle-activation-plan.md` | Creat ca plan exact pentru activarea Kotlin in Gradle |
| `kotlin-testing-strategy.md` | Creat ca strategie de testare pentru conversiile Kotlin |
