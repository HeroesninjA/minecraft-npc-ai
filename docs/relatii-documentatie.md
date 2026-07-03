# Relatii Documentatie

Actualizat: 2026-07-01

Acest document defineste cum sunt legate fisierele de documentatie intre ele.

## Reguli de baza

- Fiecare zona are un document canonic.
- Documentele de categorie sunt indexuri, nu surse primare.
- Documentele `README.md` din categorii trimit catre documentele canonice.
- Un document istoric poate ramane ca arhiva, dar nu mai primeste decizii noi.
- Daca doua documente par sa spuna acelasi lucru, unul trebuie marcat drept derivat sau istoric.

## Tipuri de relatie

- `canonic` - documentul care stabileste regula sau contractul principal.
- `derivat` - document care explica, operationalizeaza sau descompune un document canonic.
- `istoric` - versiune veche pastrata pentru context.
- `index` - document de navigare care grupeaza alte documente.
- `operational` - runbook, checklist sau ghid de executie.
- `dependent` - document care se bazeaza pe altul pentru continut sau ordine de citire.

## Harta principala

### 1. Stare, reguli, roadmap

- `constitutie-proiect.md` este baza pentru directie, reguli si criterii de maturitate.
- `audit-constitutie-proiect.md` depinde de `constitutie-proiect.md` si verifica alinierea codului.
- `implementat-deja.md` este sursa de adevar pentru statusul confirmat in cod.
- `faze-observatii-avertizari.md` foloseste `constitutie-proiect.md` si `implementat-deja.md` ca intrare.
- `faze-urmatoare-categorii.md` rezuma urmatorii pasi pe categorii, pornind din stare si audit.
- `roadmap-orientativ.md` ordoneaza intern munca si urmeaza statusul real.
- `server-npc-mvp-si-faze.md` si `prim-demo-functionalitate-minima-diversa.md` sunt slice-uri de executie derivate din roadmap.
- `faze-urmatoare-250.md` si `faze-urmatoare-250-partea-2.md` sunt extensii de roadmap pentru secventa 250.

### 2. World mapping si spawn

- `mapping-stack.md` este punctul de intrare pentru citirea zonei de mapping.
- `npc-population-world-stack.md` este punctul de intrare pentru citirea zonei NPC population/world bindings.
- `mapping.md` este documentul canonic pentru `WorldRegion -> WorldPlace -> WorldNode`.
- `mapping-pentru-implementari-ulterioare.md` este redirect istoric catre `mapping.md`.
- `mapping-harti-manuale.md` detaliaza folosirea manuala a mapping-ului, hinturile inline, preview-ul vizual si editarea drafturilor.
- `build-mode-tutorial.md` este ghidul scurt pentru fluxul `world create ai preview` -> `map edit` -> `map confirm`.
- `build-mode-region-place-node.md` este contractul extins pentru build mode, selectie vizuala, preview si integrarea cu editorul.
- `npc-world-bindings.md` depinde de `mapping.md` pentru legaturi NPC -> place/node.
- `ordine-spawn-npc-cladiri-region-node.md` depinde de `mapping.md`, `npc-world-bindings.md` si planurile de settlement.
- `settlement-plan.md` defineste planul de regiune care precede spawn-ul.
- `playable-village-ux.md` impune criterii de lizibilitate pentru mapping, spawn si interactiune.
- `households-persistente.md`, `rutine-npc-si-timeline.md`, `prevenire-duplicare-npc.md` si `generare-populatie-narativa.md` depind de mapping si de bind-urile NPC.
- `simulare-sat-si-lume.md` descrie sistemul de simulare la nivel mare, peste mapping si NPC bindings.
- `environment-context-si-engine.md` si `gui-interfete.md` folosesc mapping-ul ca baza pentru inspectie si prezentare.
- `gui-stack.md` este punctul de intrare pentru citirea zonei UI.
- `gui-interfete.md` si `gui-admin-mapping-quest.md` includ acum reguli explicite de simplificare UI/UX: progressive disclosure, separare player/admin si layout-uri mai curate pentru authoring si editare.

### 3. NPC, rutine si simulare

- `simulation-stack.md` este punctul de intrare pentru citirea seriei de simulare NPC.

- `simulation-service.md` este contractul tehnic de baza.
- `simulation-service-partea-2.md`, `simulation-service-partea-3.md` si `simulation-service-partea-4.md` sunt etape successive de extractie si rollout.
- `dialog-si-conversatii.md` depinde de rutina, relatie, memorie si context.
- `interactiuni.md` defineste fluxul concret player-NPC si consuma starea NPC.
- `reactie-npc-jucator.md` se bazeaza pe istoric si emotii.
- `interactiune-dialog-reactie-stack.md` este punctul de intrare pentru citirea celor trei documente de mai sus.
- `npc-uri-temporare-si-episodice.md` este o ramura speciala a modelului de NPC.

### 4. Quest, story si AI

- `progression-events-onboarding-stack.md` este punctul de intrare pentru citirea zonei progresie/event-uri/onboarding.
- `quest-evolution-stack.md` este punctul de intrare pentru citirea seriei de evolutie a questurilor.

- `questuri-faza-1-stabilizare.md`, `pregatire-questuri-avansate.md` si `questuri-avansate-v2.md` formeaza lantul de evolutie pentru questuri.
- `progression-service.md` este directia pentru progres generic peste questuri.
- `api-events-listeners-triggers.md` defineste contractul de evenimente folosit de quest, story, dialog si NPC.
- `lucru-alternat-quest-mapping-progression.md` leaga mapping-ul de questuri si progres.
- `story-context-quest-ai-stack.md` este punctul de intrare pentru citirea zonei story/context/quest AI.
- `quest-anchor-bindings.md` depinde de mapping si este folosit de quest/story.
- `story-context-service.md` si `story-si-context-ai.md` leaga mapping -> quest -> story -> AI.
- `story-context-service.md` si `generare-automata-questuri-ai.md` sunt dependente de contextul stabil al lumii.
- `ai-orchestrare-si-mecanici.md` si `spring-ai-mcp-serviciu-intern.md` descriu stratul AI care consuma context determinist.
- `ai-orchestrare-mcp-stack.md` este punctul de intrare pentru citirea celor doua documente AI de mai sus.
- `schema-scenariu-predefinit-testare.md` este un fixture de test peste mapping, NPC si quest/story.
- `betonquest-directii-potrivite-pentru-ainpc.md` este referinta externa, nu specificatie primara.

### 5. API, modularizare si addonuri

- `documentatie-api.md` este contractul public curent.
- `refactorizare-si-impartire-pe-module.md` si `strategie-plugin-modular-si-scenarii-programabile.md` descriu cum se extinde platforma.
- `kotlin-style-guide.md`, `kotlin-interop-api-addonuri.md`, `kotlin-code-review-checklist.md`, `kotlin-testing-strategy.md` si `kotlin-coroutines-paper-policy.md` sunt reguli si ghiduri de implementare.
- `gui-interfete.md` acopera si extensibilitatea GUI-ului intern pentru addonuri, strategia de simplificare a ecranelor principale si terminologia standard de UI.
- `gui-admin-mapping-quest.md` acopera fluxul admin de mapping/quest, regulile de separare view/edit/preview/confirm si terminologia standard de admin UI.
- `reducere-marime-jar.md` influenteaza livrarea si packaging-ul.
- `arhiva/kotlin-migration/README.md` este istoric pentru migrarile Java -> Kotlin.

### 6. Operare, hardening si infrastructura

- `server-admin-runbook.md`, `release-checklist.md`, `debugging-si-testare.md` si `migration-si-backup.md` formeaza lantul operational.
- `sistem-permisiuni-compatibilitate-pluginuri.md` descrie contractul de permisiuni Bukkit si compatibilitatea cu manageri externi precum LuckPerms.
- `audit.md` descrie auditul runtime.
- `mcp-docker-server-mvp-si-faze.md` si `deepseek/coding-automation-stack-linux-vscode-deepseek-mcp.md` tin de infrastructura de lucru.
- `kotlin-paper-packaging-si-smoke.md` si `kotlin-testing-strategy.md` sunt ghiduri de verificare pentru build si runtime.
- `analiza-erori-si-plan-rezolvare.md` colecteaza probleme istorice si planuri de remediere.
- `faze-observatii-avertizari.md` ramane referinta pentru riscuri si avertizari globale.

### 7. Referinte si arhiva

- `surse-inspiratie-plugin-ainpc.md` este referinta externa.
- `arhiva/` contine documente vechi pastrate pentru istoric.
- `arhiva/questuri-avansate-v1.md` este versiunea istorica inlocuita de `questuri-avansate-v2.md`.
- `documentatie/` contine materiale brute si liste auxiliare, nu specificatii canonice.

## Harti de citire pe documente

### A. Directie si status

- `constitutie-proiect.md` -> `audit-constitutie-proiect.md` -> `implementat-deja.md` -> `faze-observatii-avertizari.md`
- `constitutie-proiect.md` -> `faze-urmatoare-categorii.md` -> `roadmap-orientativ.md`
- `roadmap-orientativ.md` -> `server-npc-mvp-si-faze.md` -> `prim-demo-functionalitate-minima-diversa.md`
- `roadmap-orientativ.md` -> `faze-urmatoare-250.md` -> `faze-urmatoare-250-partea-2.md`

### B. Lume si mapping

- `mapping-stack.md` -> `mapping.md` -> `mapping-harti-manuale.md` -> `build-mode-tutorial.md`
- `playable-village-ux.md` -> `settlement-plan.md` -> `mapping.md` -> `npc-world-bindings.md`
- `mapping.md` -> `ordine-spawn-npc-cladiri-region-node.md` -> `households-persistente.md`
- `mapping.md` -> `rutine-npc-si-timeline.md` -> `dialog-si-conversatii.md`
- `mapping.md` -> `gui-interfete.md` -> `environment-context-si-engine.md`
- `mapping.md` -> `prevenire-duplicare-npc.md` -> `generare-populatie-narativa.md`

### C. NPC, simulare si interactiuni

- `simulation-service.md` -> `simulation-service-partea-2.md` -> `simulation-service-partea-3.md` -> `simulation-service-partea-4.md`
- `rutine-npc-si-timeline.md` -> `reactie-npc-jucator.md` -> `interactiuni.md`
- `dialog-si-conversatii.md` <- `interactiuni.md` si `reactie-npc-jucator.md`
- `npc-uri-temporare-si-episodice.md` ramane o ramura laterala a modelului NPC

### D. Quest, story si AI

- `questuri-faza-1-stabilizare.md` -> `pregatire-questuri-avansate.md` -> `questuri-avansate-v2.md`
- `questuri-avansate-v2.md` -> `progression-service.md` -> `api-events-listeners-triggers.md`
- `mapping.md` -> `lucru-alternat-quest-mapping-progression.md` -> `quest-anchor-bindings.md`
- `story-context-service.md` -> `story-si-context-ai.md` -> `generare-automata-questuri-ai.md`
- `ai-orchestrare-si-mecanici.md` -> `spring-ai-mcp-serviciu-intern.md`
- `schema-scenariu-predefinit-testare.md` este fixture de integrare peste mapping, NPC si quest/story

### E. API, addonuri si operare

- `documentatie-api.md` -> `refactorizare-si-impartire-pe-module.md` -> `strategie-plugin-modular-si-scenarii-programabile.md`
- `kotlin-style-guide.md` -> `kotlin-interop-api-addonuri.md` -> `kotlin-code-review-checklist.md` -> `kotlin-testing-strategy.md`
- `server-admin-runbook.md` -> `release-checklist.md` -> `debugging-si-testare.md` -> `migration-si-backup.md`
- `audit.md` si `prevenire-duplicare-npc.md` sunt documente operationale de verificare

## Cum se citeste

Ordinea buna este:

1. documentul canonic
2. documentele derivate din aceeasi zona
3. documentele operationale sau de implementare
4. documentele istorice doar pentru context

## Ce sa actualizezi cand schimbi ceva

- daca schimbi directia, actualizeaza `constitutie-proiect.md`
- daca schimbi statusul real, actualizeaza `implementat-deja.md`
- daca schimbi o zona functionala, actualizeaza documentul canonic si indexul categoriei
- daca schimbi doar un pas de executie, actualizeaza documentul derivat, nu sursa primara

## Ghid scurt de navigare

Detalierea completa este sintetizata in `Tabel compact`. Pentru trasee de citire, foloseste `Rezumat operativ`. Pentru ce depinde de ce, foloseste `Dependente inverse`.

## Dependente inverse

### `relatii-documentatie.md`

- `docs/README.md`
- `docs/categorii/README.md`
- `docs/categorii/00-stare-roadmap/README.md`
- `docs/categorii/01-world-mapping-spawn/README.md`
- `docs/categorii/02-npc-rutine-simulare/README.md`
- `docs/categorii/03-quest-story-ai/README.md`
- `docs/categorii/04-generare-worldgen/README.md`
- `docs/categorii/05-api-modularizare-addonuri/README.md`
- `docs/categorii/06-operare-hardening/README.md`
- `docs/categorii/07-referinte-arhiva/README.md`

## Rezumat operativ

| Zona | Citire minima |
|---|---|
| Directie si stare | `constitutie-proiect.md` -> `audit-constitutie-proiect.md` -> `implementat-deja.md` -> `faze-observatii-avertizari.md` |
| Roadmap si executie | `faze-urmatoare-categorii.md` -> `roadmap-orientativ.md` -> `server-npc-mvp-si-faze.md` |
| World mapping | `mapping-stack.md` -> `mapping.md` -> `mapping-harti-manuale.md` -> `build-mode-tutorial.md` |
| Simulare NPC | `simulation-service.md` -> `simulation-service-partea-2.md` -> `simulation-service-partea-3.md` -> `simulation-service-partea-4.md` |
| Quest, story si AI | `questuri-faza-1-stabilizare.md` -> `questuri-avansate-v2.md` -> `progression-service.md` -> `story-context-service.md` |
| API si addonuri | `documentatie-api.md` -> `refactorizare-si-impartire-pe-module.md` -> `strategie-plugin-modular-si-scenarii-programabile.md` |
| Operare | `server-admin-runbook.md` -> `release-checklist.md` -> `debugging-si-testare.md` -> `migration-si-backup.md` |
| Permisiuni | `sistem-permisiuni-compatibilitate-pluginuri.md` -> `server-admin-runbook.md` |

## Tabel compact de referin?e

| Fisier | Rol | Depinde de |
|---|---|---|
| `docs/relatii-documentatie.md` | canonic | `docs/README.md`, categoriile, toate documentele principale |
| `constitutie-proiect.md` | canonic | directie si criterii de maturitate |
| `implementat-deja.md` | canonic | statusul confirmat in cod |
| `roadmap-orientativ.md` | canonic | ordinea interna de lucru |
| `mapping.md` | canonic | world mapping, spawn, bindings |
| `simulare-sat-si-lume.md` | canonic | simulare comunitate si consecinte |
| `simulation-service.md` | canonic | motorul de simulare NPC |
| `questuri-avansate-v2.md` | canonic | questuri avansate |
| `progression-service.md` | canonic | progres generic |
| `story-context-service.md` | canonic | context narativ |
| `ai-orchestrare-si-mecanici.md` | canonic | orchestration AI |
| `documentatie-api.md` | canonic | API public |
| `server-admin-runbook.md` | operational | instalare, verificari, audit, smoke |
| `release-checklist.md` | operational | release si rollback |
| `debugging-si-testare.md` | operational | testare si diagnostic |
| `migration-si-backup.md` | operational | backup si restore |
| `playable-village-ux.md` | canonic | criterii de playability |
| `settlement-plan.md` | canonic | plan de regiune |
| `npc-world-bindings.md` | dependent | binding NPC-place/node |
| `ordine-spawn-npc-cladiri-region-node.md` | dependent | ordine spawn si rollback |
| `quest-anchor-bindings.md` | dependent | ancore semantice de quest |
| `json-yaml-contract.md` | canonic | contracte JSON/YAML |
| `feature-flags-lifecycle.md` | canonic | feature flags |
| `worldedit-integration-contract.md` | canonic | adapter WorldEdit |
| `template-cladiri-si-marker-nodes.md` | canonic | template-uri si markeri |
| `structuri-exterioare-satului.md` | canonic | taxonomie zone exterioare |

### Legenda roluri

- `canonic` - sursa principala a unei zone.
- `derivat` - explica sau operationalizeaza un document canonic.
- `dependent` - consuma direct un alt document pentru continut.
- `operational` - runbook, checklist sau ghid de executie.
- `istoric` - versiune veche pastrata pentru context.

`Depinde de` este un rezumat scurt, nu o lista exhaustiva a tuturor referintelor.

## Documente suplimentare

### Demo si onboarding

- `50-taskuri-prim-demo.md` - operational
- `criterii-gata-prim-demo.md` - operational
- `env-prim-demo.md` - operational
- `inventar-comenzi-prim-demo.md` - operational
- `player-onboarding-initiere.md` - derivat
- `procedura-backup-prim-demo.md` - operational
- `sumar-implementare-demo.md` - derivat

### Worldgen si fixture

- `addon-config-template.md` - operational
- `generare-ai-si-constructie-automata.md` - derivat
- `generare-sate-fara-worldedit.md` - derivat
- `generare-sate-worldedit-si-npc.md` - derivat
- `mediu-test-controlat-sat-si-structuri-exterioare.md` - operational
- `patch-planner.md` - derivat

### Kotlin si arhiva

- `conversie-java-la-kotlin.md` - istoric
- `conversie-java-la-kotlin-partea-2.md` - istoric
- `conversie-java-la-kotlin-partea-3.md` - istoric
- `conversie-java-la-kotlin-partea-4.md` - istoric
- `conversie-java-la-kotlin-partea-5.md` - istoric
- `kotlin-gradle-activation-plan.md` - istoric

### Quest si mapping

- `obiective-quest-tipuri.md` - derivat
- `ordine-spawn-npc-cladiri-region-node-v1.md` - istoric

### Contracte si exemple

- `json-yaml-contract-exemple.md` - derivat

### Operare si suport

- `constitusional.md` - istoric
- `documentatie-lipsa.md` - operational
- `server-credentials.md` - operational
- `storage-provider-roadmap.md` - derivat
- `verificari-server-d1.md` - operational
- `verificari-server-d2.md` - operational
- `verificari-server-d3.md` - operational
- `verificari-server-d4.md` - operational
- `verificari-server-d5.md` - operational
- `verificari-server-d6.md` - operational
- `verificari-server-d7-d9.md` - operational

