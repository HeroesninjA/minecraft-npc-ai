# Documentatie

Actualizat: 2026-04-30

Acest folder contine documentatia tehnica locala a proiectului. Documentele nu au toate acelasi rol: unele descriu ce exista deja in cod, altele sunt design pentru faze viitoare.

## Citire rapida

Ordinea recomandata pentru orientare:

1. `implementat-deja.md`
2. `faze-observatii-avertizari.md`
3. `ordine-spawn-npc-cladiri-region-node.md`
4. `story-si-context-ai.md`
5. `story-context-service.md`
6. `roadmap-orientativ.md`
7. `debugging-si-testare.md`

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
| 1 | World mapping si spawn order MVP | `mapping.md`, `ordine-spawn-npc-cladiri-region-node.md`, `rutine-npc-si-timeline.md` | Nucleu implementat initial; generatorul complet lipseste |
| 2 | First playable release | `roadmap-orientativ.md`, `questuri-avansate.md`, `story-si-context-ai.md`, `story-context-service.md`, `reactie-npc-jucator.md`, `betonquest-directii-potrivite-pentru-ainpc.md` | Directie de produs; story context read-only exista, continutul demo jucabil lipseste |
| 3 | Modularizare, API si addonuri | `documentatie-api.md`, `strategie-plugin-modular-si-scenarii-programabile.md`, `refactorizare-si-impartire-pe-module.md` | Baza exista; contractele trebuie stabilizate |
| 4 | Runtime scenarii extensibil | `questuri-avansate.md`, `story-si-context-ai.md`, `npc-uri-temporare-si-episodice.md`, `mapping.md` | Design partial; necesita registri actiuni/conditii/trigger |
| 5 | Generare sate si authoring asistat | `generare-sate-fara-worldedit.md`, `generare-sate-worldedit-si-npc.md`, `generare-ai-si-constructie-automata.md`, `story-si-context-ai.md` | Scanner/mapper initial exista; generarea completa ramane viitoare |
| 6 | Hardening productie si livrare | `reducere-marime-jar.md`, `audit.md`, `debugging-si-testare.md` | Backlog tehnic; nu bloca MVP-ul fara motiv |

## Index documente

| Document | Faza | Rol | Observatie / avertizare |
|---|---:|---|---|
| `analiza-erori-si-plan-rezolvare.md` | 0 | Raport tehnic si ordine de remediere | Foloseste-l ca backlog verificabil, nu ca lista automata de buguri active |
| `audit.md` | 0, 6 | Documentatie pentru `/ainpc audit` si backlog securitate | Auditul runtime este read-only si nu dovedeste securitate completa |
| `betonquest-directii-potrivite-pentru-ainpc.md` | 2, 4 | Inspiratie pentru questuri mature | Nu copia modelul BetonQuest integral inainte de stabilizarea runtime-ului propriu |
| `debugging-si-testare.md` | 0, 6 | Testare Maven, debug dump, smoke tests | Diferentiaza clar testele locale de testele pe server Paper real |
| `documentatie-api.md` | 3 | Contract public curent | Nu trata clasele interne core ca API stabil pentru addonuri |
| `documentatie-lipsa.md` | toate | Idei de documentatie lipsa si prioritate | Foloseste-l ca backlog de documentatie, nu ca status de implementare |
| `faze-observatii-avertizari.md` | toate | Harta centrala pe faze | Document de control pentru citirea restului documentatiei |
| `generare-ai-si-constructie-automata.md` | 5 | Directie pentru generare AI si template-uri | AI-ul trebuie sa genereze drafturi validate, nu sa execute direct modificari in lume |
| `generare-sate-fara-worldedit.md` | 1, 5 | Generare si completare sate vanilla fara WorldEdit obligatoriu | Scannerul/mapperul sunt initiale; patch planner si builder complet lipsesc |
| `generare-sate-worldedit-si-npc.md` | 5 | Design pentru integrare optionala WorldEdit | Integrarea WorldEdit trebuie sa ramana optionala, nu dependinta obligatorie a MVP-ului |
| `implementat-deja.md` | 0 | Rezumat al functionalitatii confirmate in cod | Actualizeaza-l dupa fiecare schimbare de faza care modifica statusul real |
| `mapping.md` | 1, 4, 5 | Stare, limitari, reguli de consum si evolutie pentru regiuni/places/nodes | Mapping-ul poate exista in cod, dar serverul poate avea 0 regiuni pana la config/import |
| `mapping-harti-manuale.md` | 1, 4, 5 | Ghid pentru harti construite manual si strat semantic validat de admin | Detectia automata poate propune zone, dar nu trebuie tratata ca adevar semantic |
| `mapping-pentru-implementari-ulterioare.md` | 4, 5 | Redirect istoric catre `mapping.md` | Pastreaza-l doar pentru linkuri vechi; continutul canonic este in `mapping.md` |
| `npc-uri-temporare-si-episodice.md` | 4 | NPC-uri temporare, episodice si non-villager | Pastreaza persistenta light separata de NPC-urile permanente |
| `ordine-spawn-npc-cladiri-region-node.md` | 1, 5, 6 | v2 pentru fazele urmatoare de spawn order | v1 este arhivat in `arhiva/`; v2 se concentreaza pe generator, planuri, persistenta, migration si rollback |
| `quest-anchor-bindings.md` | 2, 4 | Contract DB pentru ancore semantice de quest | Binding-urile, auditul si comanda admin read-only exista initial; lipseste export/debugdump complet |
| `questuri-avansate.md` | 2, 4 | Evolutia questurilor pe obiective si etape | Nu sari direct la branching complex pana cand questurile cap-coada sunt stabile |
| `reactie-npc-jucator.md` | 2, 4 | Reactii bazate pe istoric, emotii si reputatie | Evita sisteme mari de reputatie inainte de primul scenariu jucabil |
| `README.md` | toate | Indexul documentatiei | Pastreaza aici lista completa a documentelor si fazele principale |
| `reducere-marime-jar.md` | 6 | Plan pentru micsorarea JAR-ului | Nu activa `minimizeJar` fara smoke test pe server |
| `refactorizare-si-impartire-pe-module.md` | 3 | Migrare si spargere pe module/clase | Refactorizarea trebuie facuta incremental, cu teste dupa fiecare pas |
| `roadmap-orientativ.md` | toate | Roadmap produs si versiuni orientative | Roadmap-ul este prioritate de produs, nu promisiune ca totul este implementat |
| `rutine-npc-si-timeline.md` | 1, 4 | Rutine zilnice, timeline si hook-uri | Rutina actuala e MVP cu teleport controlat, nu pathfinding real |
| `story-context-service.md` | 2, 4 | Context narativ read-only peste mapping si quest anchors | Implementat initial; nu persista inca story state sau evenimente |
| `story-si-context-ai.md` | 2, 4, 5 | Design pentru story, context AI, mapping semantic si quest anchors | AI-ul trebuie sa consume context validat si limitat, nu coordonate brute sau toata harta |
| `strategie-plugin-modular-si-scenarii-programabile.md` | 3, 4 | Strategie addonuri si scenarii programabile | Registrii de scenarii trebuie introdusi inainte de authoring AI matur |
| `surse-inspiratie-plugin-ainpc.md` | toate | Surse de inspiratie si comparatii | Foloseste-l pentru directie, nu ca specificatie de implementare |

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
