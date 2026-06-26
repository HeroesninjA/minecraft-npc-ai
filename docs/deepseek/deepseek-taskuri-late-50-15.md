# DeepSeek Taskuri - Batch 17 (L851-L900)

Actualizat: 2026-06-25

Acest document continua backlog-ul pentru DeepSeek v4 Flash cu taskuri mici, sigure si implementabile incremental. Batch-ul acopera documente canonice individuale si relatii transversale intre zonele mari de documentatie.

Reguli:
- fiecare task schimba o singura zona mica;
- daca taskul atinge runtime, adauga test sau audit read-only;
- daca taskul atinge parserul, adauga warning clar pentru input invalid;
- nu introduce mecanici mari fara contract si regresie.

## Acoperire granulara si relatii canonice

### L851 Audit pentru `relatii-documentatie.md`
**Descriere tehnica:** Verifica daca `relatii-documentatie.md` include toate documentele active si traseele de citire relevante.
**Scop:** Pastreaza harta relatiilor ca index transversal util.
**Target:** `docs/relatii-documentatie.md`.
**Prompt AI:** Auditeaza `relatii-documentatie.md` fata de lista curenta de documente active si propune completari punctuale.
**Acceptare:** Raportul listeaza documente lipsa, relatii lipsa si relatii invechite.

### L852 Audit pentru `index-functional.md`
**Descriere tehnica:** Verifica daca indexul functional acopera toate punctele de intrare relevante.
**Scop:** Pastreaza navigarea orientata pe functionalitate.
**Target:** `docs/index-functional.md`.
**Prompt AI:** Auditeaza indexul functional si compara-l cu categoriile din `docs/categorii/`.
**Acceptare:** Fiecare categorie mare are cel putin un punct functional de intrare.

### L853 Audit pentru `index-navigare.md`
**Descriere tehnica:** Verifica daca indexul de navigare mai este util fata de README si categoriile noi.
**Scop:** Evita indexuri redundante sau invechite.
**Target:** `docs/index-navigare.md`.
**Prompt AI:** Verifica rolul curent al `index-navigare.md` si propune pastrare, redirect sau consolidare.
**Acceptare:** Raportul indica actiunea recomandata si motivul.

### L854 Audit pentru `start-here.md`
**Descriere tehnica:** Verifica daca punctul de start trimite catre documentele corecte pentru orientare rapida.
**Scop:** Reduce timpul de onboarding.
**Target:** `docs/start-here.md`.
**Prompt AI:** Auditeaza `start-here.md` si verifica daca traseul propus este aliniat cu README si categoriile.
**Acceptare:** Raportul propune un traseu scurt de citire fara duplicari.

### L855 Audit pentru `README.md`
**Descriere tehnica:** Verifica daca README-ul principal reflecta documentele active, arhiva si batch-urile DeepSeek curente.
**Scop:** Pastreaza indexul principal corect.
**Target:** `../README.md`.
**Prompt AI:** Auditeaza README-ul documentatiei si identifica intrari lipsa, vechi sau prost ordonate.
**Acceptare:** Raportul include corecturi concrete de lista si data.

### L856 Audit pentru `taskuri-prioritizate.md`
**Descriere tehnica:** Compara taskurile prioritizate cu batch-urile DeepSeek si categoriile canonice.
**Scop:** Evita doua backlog-uri care spun lucruri diferite.
**Target:** `../../taskuri-prioritizate.md`.
**Prompt AI:** Auditeaza `taskuri-prioritizate.md` fata de seria DeepSeek si categoriile docs.
**Acceptare:** Raportul separa taskuri canonice, duplicate si lipsuri.

### L857 Audit pentru `taskuri-finalizate.md`
**Descriere tehnica:** Verifica daca taskurile finalizate au dovezi si nu raman duplicate in backlogul activ.
**Scop:** Pastreaza starea taskurilor corecta.
**Target:** `docs/taskuri-finalizate.md`.
**Prompt AI:** Auditeaza taskurile finalizate si cauta duplicate ramase in documentele active.
**Acceptare:** Raportul include task, dovada si duplicate posibile.

### L858 Audit pentru `implementat-deja.md`
**Descriere tehnica:** Verifica daca documentul de implementat reflecta codul confirmat si nu design viitor.
**Scop:** Pastreaza sursa de adevar pentru ce exista.
**Target:** `docs/implementat-deja.md`.
**Prompt AI:** Auditeaza `implementat-deja.md` si marcheaza afirmatiile care par design, nu implementare confirmata.
**Acceptare:** Raportul separa confirmat, incert si design.

### L859 Audit pentru `constitutie-proiect.md`
**Descriere tehnica:** Verifica daca regulile constitutionale sunt reflectate in categoriile principale.
**Scop:** Leaga principiile de documentatia operationala.
**Target:** `docs/constitutie-proiect.md`.
**Prompt AI:** Auditeaza constitutia proiectului fata de README, categorii si taskuri prioritizate.
**Acceptare:** Raportul indica reguli fara corespondent operational.

### L860 Audit pentru `audit-constitutie-proiect.md`
**Descriere tehnica:** Verifica daca auditul constitutiei este actualizat fata de documentele noi si starea curenta.
**Scop:** Pastreaza auditul util pentru decizii.
**Target:** `docs/audit-constitutie-proiect.md`.
**Prompt AI:** Auditeaza documentul de audit constitutional si propune update-uri pentru zonele schimbate.
**Acceptare:** Raportul listeaza sectiuni invechite si remediere propusa.

### L861 Audit pentru `faze-observatii-avertizari.md`
**Descriere tehnica:** Verifica daca avertizarile globale apar si in categoriile unde conteaza.
**Scop:** Evita pierderea riscurilor in documente lungi.
**Target:** `docs/faze-observatii-avertizari.md`.
**Prompt AI:** Auditeaza avertizarile globale si verifica daca sunt propagate in documentele de categorie.
**Acceptare:** Fiecare avertizare majora are categorie si document afectat.

### L862 Audit pentru `faze-urmatoare-categorii.md`
**Descriere tehnica:** Verifica daca fazele pe categorii sunt aliniate cu cele 8 categorii canonice.
**Scop:** Face planul de lucru comparabil cu structura docs.
**Target:** `docs/faze-urmatoare-categorii.md`.
**Prompt AI:** Auditeaza fazele urmatoare pe categorii fata de `docs/categorii/`.
**Acceptare:** Fiecare categorie are faze sau motiv pentru lipsa lor.

### L863 Audit pentru `roadmap-orientativ.md`
**Descriere tehnica:** Verifica daca roadmap-ul intern mai corespunde modulelor si documentelor active.
**Scop:** Evita orientarea dupa directii vechi.
**Target:** `docs/roadmap-orientativ.md`.
**Prompt AI:** Auditeaza roadmap-ul orientativ fata de harta pachetelor si categoriile documentatiei.
**Acceptare:** Raportul propune actualizari de ordine si responsabilitate.

### L864 Audit pentru `harta-pachetelor-cod.md`
**Descriere tehnica:** Verifica daca harta completa a pachetelor include modulele si pachetele actuale.
**Scop:** Pastreaza orientarea tehnica utila.
**Target:** `docs/harta-pachetelor-cod.md`.
**Prompt AI:** Auditeaza harta pachetelor fata de structura actuala a codului si raporteaza drift.
**Acceptare:** Raportul listeaza pachete lipsa, mutate sau redundante.

### L865 Audit pentru `harta-pachetelor-cod-scurta.md`
**Descriere tehnica:** Verifica daca harta scurta ramane un rezumat fidel al hartii complete.
**Scop:** Pastreaza orientarea rapida corecta.
**Target:** `docs/harta-pachetelor-cod-scurta.md`.
**Prompt AI:** Compara harta scurta cu harta completa si propune aliniere.
**Acceptare:** Diferentele importante sunt raportate cu actiune recomandata.

### L866 Audit pentru hartile de clase pe subsistem
**Descriere tehnica:** Verifica `harta-clase-*.md` pentru acoperire pe AI, GUI, NPC, quest, spawn si world.
**Scop:** Pastreaza hartile de clase utile pentru navigare rapida.
**Target:** `docs/harta-clase-*.md`.
**Prompt AI:** Auditeaza hartile de clase si identifica subsisteme lipsa sau intrari invechite.
**Acceptare:** Raportul grupeaza problemele pe harta.

### L867 Audit pentru `json-yaml-contract.md`
**Descriere tehnica:** Verifica daca contractul JSON/YAML este aliniat cu objective types, feature packs si story actions.
**Scop:** Pastreaza contractele de date coerente.
**Target:** `docs/json-yaml-contract.md`.
**Prompt AI:** Auditeaza contractul JSON/YAML fata de documentele de quest, objective si story.
**Acceptare:** Raportul listeaza campuri lipsa, aliasuri si exemple necesare.

### L868 Audit pentru `json-yaml-contract-exemple.md`
**Descriere tehnica:** Verifica daca exemplele acopera cazurile active si nu folosesc aliasuri invechite.
**Scop:** Pastreaza exemplele executabile si clare.
**Target:** `docs/json-yaml-contract-exemple.md`.
**Prompt AI:** Auditeaza exemplele JSON/YAML si raporteaza exemple invalide sau incomplete.
**Acceptare:** Fiecare exemplu problematic are remediere propusa.

### L869 Audit pentru `objective-types-reference.md`
**Descriere tehnica:** Verifica daca referinta objective types este aliniata cu contractul, exemplele si GUI-ul.
**Scop:** Evita divergenta intre parser, docs si UI.
**Target:** `docs/objective-types-reference.md`.
**Prompt AI:** Auditeaza referinta objective types fata de documentele de contract si GUI.
**Acceptare:** Raportul listeaza tipuri lipsa, aliasuri si divergente.

### L870 Audit pentru `objective-examples.md`
**Descriere tehnica:** Verifica daca exemplele de obiective acopera toate tipurile relevante.
**Scop:** Face authoring-ul de questuri mai sigur.
**Target:** `docs/objective-examples.md`.
**Prompt AI:** Auditeaza exemplele de obiective si identifica tipurile fara exemplu.
**Acceptare:** Fiecare tip lipsa primeste propunere de exemplu.

### L871 Audit pentru `obiective-quest-tipuri.md`
**Descriere tehnica:** Compara documentul romanesc despre tipuri de obiective cu referinta canonica.
**Scop:** Reduce duplicarea si divergenta terminologica.
**Target:** `docs/obiective-quest-tipuri.md`.
**Prompt AI:** Auditeaza `obiective-quest-tipuri.md` fata de `objective-types-reference.md`.
**Acceptare:** Raportul propune consolidare sau redirect pentru informatie duplicata.

### L872 Audit pentru `stage-metadata-contract.md`
**Descriere tehnica:** Verifica daca metadata de stage este reflectata in quest docs si exemple.
**Scop:** Pastreaza stages liniare verificabile.
**Target:** `docs/stage-metadata-contract.md`.
**Prompt AI:** Auditeaza contractul stage metadata fata de quest docs si JSON/YAML examples.
**Acceptare:** Raportul indica campuri lipsa si exemple necesare.

### L873 Audit pentru `quest-anchor-bindings.md`
**Descriere tehnica:** Verifica daca ancorele de quest sunt aliniate cu mapping, story si audit commands.
**Scop:** Pastreaza legatura dintre lume si quest coerenta.
**Target:** `docs/quest-anchor-bindings.md`.
**Prompt AI:** Auditeaza quest anchor bindings fata de mapping si story docs.
**Acceptare:** Raportul listeaza campuri, comenzi sau verificari lipsa.

### L874 Audit pentru `progression-service.md`
**Descriere tehnica:** Verifica daca ProgressionService este prezentat ca runtime generic si nu duplicat per mecanica.
**Scop:** Reduce riscul de engine-uri paralele.
**Target:** `docs/progression-service.md`.
**Prompt AI:** Auditeaza `progression-service.md` fata de questuri, tutoriale, bounty-uri si event-uri.
**Acceptare:** Raportul indica duplicari de runtime sau responsabilitati neclare.

### L875 Audit pentru `api-events-listeners-triggers.md`
**Descriere tehnica:** Verifica daca event-urile publice sunt aliniate cu quest, story, dialog, NPC si context.
**Scop:** Pastreaza API-ul de extensie coerent.
**Target:** `docs/api-events-listeners-triggers.md`.
**Prompt AI:** Auditeaza contractul de event-uri fata de documentele consumatoare.
**Acceptare:** Raportul listeaza event-uri documentate, implementate initial si backlog.

### L876 Audit pentru `documentatie-api.md`
**Descriere tehnica:** Verifica daca API-ul public curent corespunde directiei de addonuri si modularizare.
**Scop:** Evita promisiuni API neacoperite.
**Target:** `docs/documentatie-api.md`.
**Prompt AI:** Auditeaza documentatia API fata de strategia de addonuri si evenimente.
**Acceptare:** Raportul separa API stabil, draft si backlog.

### L877 Audit pentru `strategie-plugin-modular-si-scenarii-programabile.md`
**Descriere tehnica:** Verifica daca strategia de module si scenarii are legatura clara cu API-ul si feature packs.
**Scop:** Pastreaza extensibilitatea implementabila.
**Target:** `docs/strategie-plugin-modular-si-scenarii-programabile.md`.
**Prompt AI:** Auditeaza strategia modulara fata de API, addons si feature pack docs.
**Acceptare:** Raportul indica contracte lipsa si dependinte.

### L878 Audit pentru `refactorizare-si-impartire-pe-module.md`
**Descriere tehnica:** Verifica daca planul de module corespunde structurii actuale Maven/Kotlin.
**Scop:** Evita refactorizari documentate care nu mai sunt corecte.
**Target:** `docs/refactorizare-si-impartire-pe-module.md`.
**Prompt AI:** Auditeaza planul de modularizare fata de structura curenta a repo-ului.
**Acceptare:** Raportul listeaza module actuale, propuse si incertitudini.

### L879 Audit pentru `spring-ai-mcp-serviciu-intern.md`
**Descriere tehnica:** Verifica daca sidecar-ul MCP intern ramane read-only initial si cu fallback clar.
**Scop:** Protejeaza integrarea AI de executie directa riscanta.
**Target:** `docs/spring-ai-mcp-serviciu-intern.md`.
**Prompt AI:** Auditeaza documentul Spring AI MCP fata de regulile AI authoring si MCP stack.
**Acceptare:** Raportul marcheaza orice tool cu risc de write neclar.

### L880 Audit pentru `mcp-docker-server-mvp-si-faze.md`
**Descriere tehnica:** Verifica daca documentul MCP Docker ramane aliniat cu instructiunile locale de context.
**Scop:** Pastreaza operarea MCP si backup-ul coerente.
**Target:** `docs/mcp-docker-server-mvp-si-faze.md`.
**Prompt AI:** Auditeaza documentul MCP Docker fata de regulile de context, backup si watcher.
**Acceptare:** Raportul indica drift, comenzi vechi sau riscuri de configurare.

### L881 Audit pentru `coding-automation-stack-linux-vscode-deepseek-mcp.md`
**Descriere tehnica:** Verifica daca stack-ul de coding automation ramane compatibil cu contextul MCP curent.
**Scop:** Evita instructiuni de automatizare invechite.
**Target:** `./coding-automation-stack-linux-vscode-deepseek-mcp.md`.
**Prompt AI:** Auditeaza stack-ul coding automation fata de workflow-ul MCP si DeepSeek batch guide.
**Acceptare:** Raportul separa aplicabil, invechit si riscant.

### L882 Audit pentru `deepseek-batch-guide.md`
**Descriere tehnica:** Verifica daca ghidul DeepSeek reflecta toate batch-urile active si arhivate.
**Scop:** Pastreaza seria DeepSeek navigabila.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Auditeaza ghidul DeepSeek fata de fisierele batch existente si arhiva.
**Acceptare:** Raportul listeaza batch-uri lipsa, intervale gresite si teme incomplete.

### L883 Audit pentru batch-urile DeepSeek active
**Descriere tehnica:** Verifica daca batch-urile active au 50 taskuri, intervale consecutive si tema distincta.
**Scop:** Evita drift in seria de backlog.
**Target:** `./deepseek-taskuri-late-50-*.md`.
**Prompt AI:** Auditeaza batch-urile DeepSeek active pentru count, interval si tema.
**Acceptare:** Raportul include status per batch.

### L884 Audit pentru arhiva DeepSeek
**Descriere tehnica:** Verifica daca batch-urile arhivate sunt listate complet in README-ul arhivei.
**Scop:** Pastreaza istoricul accesibil.
**Target:** `./arhiva/README.md`.
**Prompt AI:** Auditeaza arhiva DeepSeek fata de fisierele arhivate si ghidul DeepSeek.
**Acceptare:** Raportul indica intrari lipsa, link activ lipsa sau motiv de arhivare lipsa.

### L885 Audit pentru `server-admin-runbook.md`
**Descriere tehnica:** Verifica daca runbook-ul admin acopera instalare, config, audit, debugdump, backup si smoke test.
**Scop:** Pastreaza operarea serverului repetabila.
**Target:** `docs/server-admin-runbook.md`.
**Prompt AI:** Auditeaza runbook-ul admin si identifica pasi lipsa pentru operare sigura.
**Acceptare:** Raportul grupeaza lipsurile pe setup, verificare, rollback si audit.

### L886 Audit pentru `release-checklist.md`
**Descriere tehnica:** Verifica daca checklist-ul de release include build, JAR audit, Paper smoke, backup si rollback.
**Scop:** Evita release-uri neverificate.
**Target:** `docs/release-checklist.md`.
**Prompt AI:** Auditeaza checklist-ul de release fata de runbook, backup si packaging docs.
**Acceptare:** Raportul propune pasi lipsa si ordine recomandata.

### L887 Audit pentru `migration-si-backup.md`
**Descriere tehnica:** Verifica daca backup, restore-check, migration si rollback sunt descrise operational.
**Scop:** Protejeaza datele persistente.
**Target:** `docs/migration-si-backup.md`.
**Prompt AI:** Auditeaza documentul de migration si backup pentru pasi operationali lipsa.
**Acceptare:** Raportul include cerinte de backup, restore test si rollback.

### L888 Audit pentru `debugging-si-testare.md`
**Descriere tehnica:** Verifica daca documentul acopera teste Maven, debugdump, smoke tests si rapoarte.
**Scop:** Pastreaza verificarea tehnica coerenta.
**Target:** `docs/debugging-si-testare.md`.
**Prompt AI:** Auditeaza documentul de debugging si testare fata de release si runbook.
**Acceptare:** Raportul listeaza comenzi, teste sau scenarii lipsa.

### L889 Audit pentru `audit.md`
**Descriere tehnica:** Verifica daca documentatia `/ainpc audit` acopera auditurile actuale si backlogul.
**Scop:** Pastreaza diagnosticarea admin corecta.
**Target:** `docs/audit.md`.
**Prompt AI:** Auditeaza `audit.md` fata de comenzile si debugdump-urile documentate.
**Acceptare:** Raportul separa audit implementat, audit propus si goluri.

### L890 Audit pentru `gui-interfete.md`
**Descriere tehnica:** Verifica daca GUI-urile sunt acoperite pe roluri, world, NPC, quest, debug si admin.
**Scop:** Pastreaza suprafetele vizuale coerente.
**Target:** `docs/gui-interfete.md`.
**Prompt AI:** Auditeaza `gui-interfete.md` fata de categoriile docs si fluxurile principale.
**Acceptare:** Raportul listeaza ecrane lipsa, roluri lipsa si fluxuri neclare.

### L891 Audit pentru `gui-admin-mapping-quest.md`
**Descriere tehnica:** Verifica daca GUI admin pentru mapping/quest ramane aliniat cu world si quest docs.
**Scop:** Evita divergenta intre admin GUI si runtime.
**Target:** `docs/gui-admin-mapping-quest.md`.
**Prompt AI:** Auditeaza GUI admin mapping quest fata de mapping, quest anchors si audit docs.
**Acceptare:** Raportul propune corecturi pentru flow, permisiuni si verificare.

### L892 Audit pentru `tutorial-gui-creator.md`
**Descriere tehnica:** Verifica daca tutorialul GUI creator corespunde framework-ului GUI actual.
**Scop:** Pastreaza documentatia de authoring GUI folosibila.
**Target:** `docs/tutorial-gui-creator.md`.
**Prompt AI:** Auditeaza tutorialul GUI creator fata de `gui-interfete.md` si harta de clase GUI.
**Acceptare:** Raportul indica pasi invechiti sau exemple lipsa.

### L893 Audit pentru `sistem-permisiuni-compatibilitate-pluginuri.md`
**Descriere tehnica:** Verifica daca permisiunile acopera comenzi, GUI admin si compatibilitate cu manageri externi.
**Scop:** Pastreaza actiunile sensibile controlate.
**Target:** `docs/sistem-permisiuni-compatibilitate-pluginuri.md`.
**Prompt AI:** Auditeaza documentul de permisiuni fata de runbook, GUI si comenzi admin.
**Acceptare:** Raportul listeaza permisiuni lipsa si riscuri.

### L894 Audit pentru `prevenire-duplicare-npc.md`
**Descriere tehnica:** Verifica daca runbook-ul anti-duplicare NPC este aliniat cu spawn, DB si admin commands.
**Scop:** Pastreaza remedierea duplicarii executabila.
**Target:** `docs/prevenire-duplicare-npc.md`.
**Prompt AI:** Auditeaza documentul anti-duplicare NPC fata de spawn si world bindings.
**Acceptare:** Raportul propune pasi lipsa pentru diagnostic, dryrun si repair.

### L895 Audit pentru `reducere-marime-jar.md`
**Descriere tehnica:** Verifica daca reducerea JAR este aliniata cu Kotlin runtime, shading si release checklist.
**Scop:** Evita optimizari care rup runtime-ul Paper.
**Target:** `docs/reducere-marime-jar.md`.
**Prompt AI:** Auditeaza documentul reducere JAR fata de packaging Kotlin si release.
**Acceptare:** Raportul listeaza riscuri de minimize, shading si smoke test.

### L896 Audit pentru documentele Kotlin active
**Descriere tehnica:** Verifica ghidul de stil, interop, packaging, coroutines si testing ca set coerent.
**Scop:** Pastreaza conversia Kotlin controlata.
**Target:** `docs/kotlin-*.md`.
**Prompt AI:** Auditeaza documentele Kotlin active si identifica contradictii sau reguli lipsa.
**Acceptare:** Raportul grupeaza problemele pe stil, interop, packaging, coroutines si teste.

### L897 Audit pentru arhiva Kotlin
**Descriere tehnica:** Verifica daca documentele istorice Kotlin raman doar in arhiva si sunt listate complet.
**Scop:** Evita revenirea planurilor vechi in fluxul activ.
**Target:** `docs/arhiva/kotlin-migration/README.md`.
**Prompt AI:** Auditeaza arhiva Kotlin si verifica daca toate documentele istorice sunt inventariate.
**Acceptare:** Raportul indica documente nelistate sau motive de arhivare lipsa.

### L898 Matrice document-stare
**Descriere tehnica:** Creeaza o matrice care marcheaza fiecare document ca actual, istoric, design, runbook sau referinta.
**Scop:** Face consumul documentatiei mai sigur.
**Target:** Docs classification report.
**Prompt AI:** Creeaza o matrice de clasificare pentru toate documentele Markdown din `docs/`.
**Acceptare:** Fiecare document are o categorie de stare si justificare scurta.

### L899 Pregatire pentru urmatorul ciclu
**Descriere tehnica:** Noteaza urmatorul interval numeric si recomanda o tema pentru continuarea acoperirii docs.
**Scop:** Face continuarea previzibila.
**Target:** Next batch note.
**Prompt AI:** Scrie o nota scurta care pregateste urmatorul ciclu DeepSeek dupa L900.
**Acceptare:** Urmatorul interval si tema recomandata sunt clare.

### L900 Nota finala pentru batch-ul 17
**Descriere tehnica:** Rezuma extinderea L851-L900 si rolul ei in auditul granular al documentatiei.
**Scop:** Inchide batch-ul cu un rezumat canonic.
**Target:** Release note, docs index, changelog.
**Prompt AI:** Scrie o nota finala scurta pentru batch-ul L851-L900.
**Acceptare:** Documentul marcheaza clar ce documente canonice au fost acoperite si de ce.


