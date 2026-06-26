# Taskuri prioritizate

Actualizat: 2026-06-21 (revizia 4 — audit de consisten?a + backlog extins)

Taskurile finalizate au fost mutate in `docs/taskuri-finalizate.md`; restul sunt taskuri deschise sau propuneri.

Aceasta este o lista comuna de implementare, derivata din documentatia canonica.

Ordinea este pragmatica: intai fundatia, apoi world/NPC/spawn, apoi quest/AI/story/GUI, apoi debug/operare, apoi modularizare si igiena istorica.

## Reguli de lucru

- Nu marca un task ca finalizat fara verificare in cod sau documentatia canonica.
- Pastreaza separat taskurile finalizate de backlog-ul deschis.
- Orice categorie noua trebuie sa aiba taskuri concrete, verificabile si ordonate.
- Taskurile suplimentare trebuie sa ramana in acelasi stil: scurte, actionabile, fara ambiguitate.
- Daca apare o contradictie intre stare si continut, continutul si auditul au prioritate.

## Reguli pentru consum AI

- Citeste intai `Reguli de lucru`, apoi `Index rapid`, apoi categoria relevanta.
- Trateaza `?` ca finalizat verificat, nu ca intentie.
- Trateaza sectiunile `Taskuri suplimentare` si `Taskuri extinse` ca backlog propus, nu ca implementare existenta.
- Nu presupune ca un task nementionat in alt document este optional; verifica referinta canonica inainte de concluzie.
- Daca un AI extern foloseste documentul, trebuie sa urmeze aceeasi ierarhie: reguli, stare, categorie, taskuri detaliate.
- Nu interpreta termenii generali ca echivalenti cu implementarea; cere sau cauta dovezi in documentele legate.
- Cand exista o discrepanta intre titlul unei sectiuni si continutul ei, foloseste continutul si marcajele de stare.
- Foloseste numele documentelor si prioritatile ca puncte de intrare, nu ca sursa unica de adevar.
- Daca un task pare vag, trateaza-l ca obiectiv si nu ca specificatie completa.
- Daca documentul este folosit de un alt AI de coding, conserva acelasi sens al termenilor: „finalizat”, „deschis”, „backlog”, „audit”, „canonic”.

## Index rapid

- `P0` - fundatie, API si runtime: 10 documente
- `P1` - world, NPC si spawn: 21 documente
- `P2` - quest, AI, story si GUI: 25 documente
- `P3` - debug, testare, release si hardening: 21 documente
- `P4` - modularizare, Kotlin si addonuri: 16 documente
- `P5` - istoric si igiena documentatiei: 27 documente

Aceste numere indica documentele repere asociate fiecarei categorii; nu inseamna ca toate taskurile din categorie sunt inchise.

## Harta compacta

| Prioritate | Focus | Documente repere |
| --- | --- | --- |
| [P0](#p0---fundatie-api-si-runtime) | Fundatie, API si runtime | `documentatie-api.md`, `start-here.md`, `index-functional.md` |
| [P1](#p1---world-npc-si-spawn) | World, NPC si spawn | `simulare-sat-si-lume.md`, `mapping.md`, `ordine-spawn-npc-cladiri-region-node.md` |
| [P2](#p2---quest-ai-story-si-gui) | Quest, AI, story si GUI | `questuri-avansate-v2.md`, `ai-orchestrare-si-mecanici.md`, `gui-interfete.md` |
| [P3](#p3---debug-testare-release-si-hardening) | Debug, testare, release si hardening | `debugging-si-testare.md`, `release-checklist.md`, `server-admin-runbook.md` |
| [P4](#p4---modularizare-kotlin-si-addonuri) | Modularizare, Kotlin si addonuri | `kotlin-style-guide.md`, `kotlin-interop-api-addonuri.md`, `kotlin-paper-packaging-si-smoke.md` |
| [P5](#p5---istoric-si-igiena-documentatiei) | Istoric si igiena documentatiei | `index-arhiva.md`, `index-navigare.md`, `arhiva/kotlin-migration/README.md` |

## P0 - Fundatie, API si runtime

- Construieste un traseu clar de reload si shutdown pentru platforma, addonuri si feature flags.
- Stabileste o baza coerenta pentru persistenta, dialect SQL si schema initiala.
- Fixeaza punctul principal de intrare in documentatie si navigare.

Documente: `documentatie-api.md`, `harta-clase-platform-db.md`, `harta-clase-index.md`, `start-here.md`, `index-functional.md`

### Taskuri detaliate

- Deseneaza traseul de bootstrap si reload pentru platforma.
- Specifica ce feature flags sunt citite la pornire si cum sunt rezolvate.
- Stabileste schema initiala si regulile SQL pe dialect.
- Fixeaza regula de navigare: `start-here -> index-functional -> harta-clase-index`.

#### `documentatie-api.md`

- Scrie contractul public minim pe care addonurile au voie sa-l consume.
- Separa clar API public de clasele interne din core.
- Stabileste ce metode sunt stabile si ce ramane backlog.
- Lega documentul de indexul functional si de harta de platforma.
- Noteaza ce inseamna compatibilitate Java pentru addonuri.

#### `harta-clase-platform-db.md`

- Stabileste relatia dintre `AINPCPlatform`, `AddonRegistry` si `DatabaseManager`.
- Arata fluxul de bootstrap, reload si shutdown pentru infrastructura.
- Stabileste rolul `RuntimeFeatureResolver` si al snapshot-urilor.
- Tine lista clasele de infrastructura la un nivel scurt si navigabil.
- Leaga harta de API-ul public si de documentele de addonuri.

#### `start-here.md`

- Pastreaza un singur punct de intrare principal pentru docs.
- Arata traseul recomandat: functional, clase, arhiva.
- Pastreaza legatura scurta spre `index-navigare`.
- Evita duplicarea cu celelalte indexuri.
- Actualizeaza-l daca apar hub-uri noi.

#### `index-functional.md`

- Grupeaza documentele canonice pe trasee de citire clare.
- Leaga familiile functionale de harta de clase si de arhiva.
- Pastreaza lista scurta si orientata pe executie.
- Pune taskurile prioritizate in fata documentelor de referinta.
- Evita sa repete explicatiile din `start-here.md`.

#### `harta-clase-index.md`

- Stabileste indexul central pentru subhartile de clase.
- Leaga fiecare subsistem de harta lui dedicata.
- Pastreaza navigarea rapida intre `world`, `AI`, `quest`, `spawn`, `GUI`, `debug`, `platform/db` si `NPC`.
- Clarifica ce harta este principala si ce harti sunt specializate.
- Mentine documentul strict ca index, nu ca sursa de decizie.

#### `constitutie-proiect.md`

- Stabileste regulile de baza ale proiectului.
- Leaga constitutia de API, runtime si documentatie.
- Clarifica ce este principiu si ce este implementare.
- Pastreaza deciziile arhitecturale explicite.
- Noteaza ce devine standard pentru toate subsistemele.

#### `implementat-deja.md`

- Stabileste ce exista deja si nu trebuie refacut.
- Leaga starea curenta de roadmap si taskuri.
- Clarifica ce este finalizat si ce este partial.
- Pastreaza lista actualizata si scurta.
- Noteaza dependentele care pot fi reutilizate.

#### `roadmap-orientativ.md`

- Stabileste directia pe termen scurt si mediu.
- Leaga roadmap-ul de taskurile prioritizate.
- Clarifica ce intra in urmatoarele loturi.
- Pastreaza prioritatile usor de citit.
- Noteaza ce decizie muta o sarcina intre faze.

#### `audit-constitutie-proiect.md`

- Stabileste cum se verifica respectarea constitutiei.
- Leaga auditul de documentele canonice si de runtime.
- Clarifica ce incalcari sunt critice.
- Pastreaza rezultatele de audit comparabile.
- Noteaza ce trebuie reparat dupa fiecare audit.

#### `constitusional.md`

- Stabileste o sinteza a regulilor constitutionale.
- Leaga documentul de constitutie si audit.
- Clarifica ce este rezumat si ce este norma.
- Pastreaza documentul scurt si citibil rapid.
- Noteaza daca o regula trebuie mutata in documentul canonic.

### Taskuri suplimentare P0

- Defineste versiunea semantica a API-ului public.
- Stabileste criteriile de compatibilitate binara pentru addonuri.
- Documenteaza ordinea de bootstrap pentru serviciile critice.
- Clarifica shutdown-ul idempotent pentru platforma si addonuri.
- Specifica fallback-ul cand un feature flag lipseste la pornire.
- Introdu validari explicite pentru schema SQL initiala.
- Leaga reload-ul de invalidarea cache-urilor si rebind.
- Defineste limitele de acces la clasele interne din core.
- Precizeaza ce date sunt snapshot si ce date sunt live state.
- Adauga checklist de validare pentru public API inainte de release.

### Taskuri extinse P0

- Specifica ordinea de initializare pentru handler-ele critice.
- Stabileste contractul de erori pentru bootstrap esuat.
- Clarifica ce se poate reconstrui dupa restart fara interventie.
- Documenteaza ce inseamna safe reload pentru addonuri.
- Defineste validarea pentru dependinte lipsa la pornire.
- Precizeaza ce servicii trebuie oprire ordonata si in ce ordine.
- Defineste cum se raporteaza un API incompatibil la incarcare.
- Clarifica ce configurari pot fi preluate din cache si care nu.
- Stabileste regula de versionare pentru contractele publice.
- Noteaza ce verifici dupa aplicarea unui restart complet.

### Taskuri extinse P0 - lot mare

- Defineste contractul de compatibilitate intre versiuni de API.
- Stabileste ce se intampla cand o resursa critica lipseste la start.
- Clarifica ordinea de validare pentru configurare, DB si runtime.
- Documenteaza cum se face fail fast pentru bootstrap corupt.
- Precizeaza ce exception handling este permis in startup.
- Stabileste ce loguri se emit in etapa de init si shutdown.
- Noteaza ce state trebuie curatat inainte de reload.
- Defineste cum se valideaza registration order pentru addonuri.
- Clarifica ce depinde de feature flags si ce este permanent.
- Stabileste criterii de acceptare pentru un startup fara regresii.

## P1 - World, NPC si spawn

- Stabileste ordinea corecta de spawn: alocare, validare, spawn, bind, persistenta, rollback.
- Stabileste auditul semantic pentru mapping, gap analysis si patch planning.
- Stabileste modelul principal `AINPC -> NPCManager -> NPCContext`.
- Stabileste rutina dupa spawn prin `RoutineService` si `RoutineEngine`.
- Stabileste validarea structurilor exterioare si a satului semantic.
- Stabileste traseul de inspectie pentru world mapping si duplicate NPC.

Documente: `simulare-sat-si-lume.md`, `ordine-spawn-npc-cladiri-region-node.md`, `mapping.md`, `npc-world-bindings.md`, `harta-clase-world.md`, `harta-clase-spawn.md`, `harta-clase-npc.md`

### Taskuri detaliate

- Defineste regulile de spawn sigur, cu rollback si audit.
- Normalizeaza identitatea NPC-ului intre entity, DB si context.
- Stabileste inspectia pentru duplicate, binding-uri si lock-uri.

#### `simulare-sat-si-lume.md`

- Scrie fluxul general de simulare a satului si lumii.
- Descrie ce semnale trebuie sa curga intre world, NPC si economie.
- Stabileste ce este read-only si ce este runtime activ.
- Leaga simularea de rutine, dialog si quest.
- Defineste ce ramane backlog pentru `EnvironmentEngine`.

#### `ordine-spawn-npc-cladiri-region-node.md`

- Stabileste ordinea corecta de spawn de la alocare la persistenta.
- Defineste clar unde se face rollback daca spawn-ul esueaza.
- Leaga documentul de household plan, family binding si routine.
- Noteaza ce trebuie validat inainte de spawn.
- Pastreaza exemplul de ordine ca referinta operationala.

#### `mapping.md`

- Stabileste contractul semantic `Region -> Place -> Node`.
- Clarifica ce este sursa de adevar pentru world mapping.
- Descrie cum se citesc node-urile pentru quest si GUI.
- Noteaza cum se leaga mapping-ul de audit si patch planning.
- Pastreaza documentul scurt si canonic.

#### `npc-world-bindings.md`

- Defineste formatul persistent pentru home/work/social.
- Clarifica backfill-ul din profile si metadata.
- Noteaza ce comenzi de inspectie trebuie sa existe.
- Stabileste cum se fac repair si deduplicate.
- Leaga binding-urile de spawn si routine.

#### `harta-clase-world.md`

- Leaga `WorldAdminService` de snapshot-urile de world si de inspectie.
- Stabileste traseul dintre mapping semantic, gap analysis si patch planning.
- Pastreaza vizibile clasele de infrastructura folosite des in world.
- Clarifica ce ramane parte de bootstrap si ce ramane parte de runtime.
- Mentine harta scurta si orientata pe citire rapida.

#### `harta-clase-spawn.md`

- Descrie fluxul de la planificarea spawn-ului la persistenta.
- Leaga validatorul de alocare de datele de world si de household.
- Stabileste unde se aplica rollback si unde se aplica persistenta.
- Pastreaza vizibile clasele care participa la spawn, bind si routine.
- Evita sa devina un document de implementare completa.

#### `harta-clase-npc.md`

- Stabileste relatia dintre entitatea NPC, context si stare.
- Leaga personalitatea, emotia si relatiile sociale de runtime.
- Clarifica ce clase citesc si ce clase modifica starea NPC-ului.
- Pastreaza vizibile punctele de integrare cu world si dialog.
- Ramai la un nivel de orientare, nu de cod complet.

#### `households-persistente.md`

- Stabileste contractul persistent pentru household-uri.
- Leaga household-ul de NPC, locuinta si contextul de world.
- Clarifica ce se poate reconstrui din DB si ce trebuie calculat.
- Pastreaza interactiunea cu spawn si backfill simpla.
- Evita sa dubleze modelul de routine sau binding.

#### `settlement-plan.md`

- Descrie planul de asezare pentru sat si zone adiacente.
- Leaga planul de world mapping si de generarea structurii.
- Stabileste ce decizii sunt de authoring si care sunt runtime.
- Pastreaza documentul orientat pe planificare, nu pe implementare.
- Marcheaza clar dependentele catre mapping si patch planning.

#### `generare-sate-worldedit-si-npc.md`

- Stabileste traseul de generare cu WorldEdit si NPC-uri.
- Leaga generarea de alocare, validare si bind.
- Specifica ce se verifica inainte de aplicare.
- Pastreaza pasii suficient de clari pentru operare manuala.
- Noteaza ce parte este experiment si ce parte este flux stabil.

#### `generare-sate-fara-worldedit.md`

- Descrie alternativa fara WorldEdit pentru generare.
- Stabileste ce capabilitati lipsesc si cum sunt compensate.
- Leaga fluxul de testare controlata si patch planning.
- Pastreaza documentul comparativ si pragmatic.
- Evita sa amestece presupuneri cu proceduri.

#### `mediu-test-controlat-sat-si-structuri-exterioare.md`

- Stabileste conditiile pentru mediu de test controlat.
- Leaga satul, structurile exterioare si fixture-urile.
- Clarifica ce este deterministic si ce este variabil.
- Pastreaza procedura de test reproductibila.
- Noteaza ce se reseteaza dupa fiecare rulare.

#### `mapping-harti-manuale.md`

- Stabileste cum se citesc si valideaza hartile manuale.
- Leaga exemplul manual de harta semantica principala.
- Clarifica ce este exemplu si ce este sursa de adevar.
- Pastreaza instructiunile scurte si verificabile.
- Noteaza unde se reflecta manualul in runtime.

#### `mapping-pentru-implementari-ulterioare.md`

- Stabileste ce ramane de facut in mapping.
- Leaga backlog-ul de documentele canonice curente.
- Clarifica ce scenarii sunt amanate si de ce.
- Pastreaza lista orientata pe implementare viitoare.
- Evita sa dubleze documentele deja finalizate.

#### `rutine-npc-si-timeline.md`

- Stabileste cum se programeaza rutina NPC in timp.
- Leaga timeline-ul de spawn, dialog si progres.
- Clarifica ce este periodic si ce este declansat de evenimente.
- Pastreaza ordinea de executie usor de urmarit.
- Noteaza ce se auditeaza cand rutina deviaza.

#### `simulation-service.md`

- Stabileste contractul principal al serviciului de simulare.
- Leaga simularea de world, NPC si evenimente.
- Clarifica ce este stare de citire si ce este executie.
- Pastreaza interfata de simulare stabila.
- Noteaza ce se reseteaza intre cicluri de simulare.

#### `simulation-service-partea-2.md`

- Continua descompunerea serviciului de simulare.
- Leaga partea a doua de fluxul principal si de fixtures.
- Clarifica ce ramane backend si ce este orchestration.
- Pastreaza progresia documentului usor de urmarit.
- Noteaza dependentele introduse de aceasta extensie.

#### `simulation-service-partea-3.md`

- Detaliaza ce ramane din simulare pentru partea a treia.
- Leaga scenariile de verificare de world si NPC.
- Clarifica ce este stabil si ce este experimental.
- Pastreaza documentul focalizat pe un singur pas.
- Noteaza cand este nevoie de rollback.

#### `simulation-service-partea-4.md`

- Incheie descompunerea serviciului de simulare.
- Leaga finalul de audit, debug si runnable flows.
- Clarifica ce se considera implementare completa.
- Pastreaza sumarul scurt si orientat pe executie.
- Noteaza dependentele finale ale serviciului.

#### `environment-context-si-engine.md`

- Stabileste contextul de mediu folosit de engine.
- Leaga contextul de world, NPC si AI.
- Clarifica ce este input si ce este rezultat.
- Pastreaza separatia dintre configurare si runtime.
- Noteaza ce trebuie inspectat cand contextul lipseste.

#### `storage-provider-roadmap.md`

- Stabileste directia pentru providerii de storage.
- Leaga roadmap-ul de persistenta si backup.
- Clarifica ce este alegere de arhitectura si ce este implementare.
- Pastreaza lista de optiuni scurta.
- Noteaza ce trebuie evaluat inainte de schimbarea providerului.

### Taskuri suplimentare P1

- Defineste raportarea conflictelor de mapping inainte de spawn.
- Specifica regulile de re-incercare pentru binding esuat.
- Stabileste semnatura auditului pentru duplicate NPC.
- Descrie recuperarea dupa rollback partial al spawn-ului.
- Documenteaza cum se actualizeaza household-ul dupa mutare.
- Clarifica ordinea de reaplicare a rutinei dupa backfill.
- Adauga regulile de reconciliere intre entity si DB.
- Precizeaza cum se trateaza locurile fara owner sau worker.
- Defineste validarea pentru structuri exterioare in world.
- Stabileste criterii de acceptare pentru sat semantic complet.

### Taskuri extinse P1

- Clarifica ce face patch planner cand mapping-ul este incomplet.
- Stabileste cum se marcheaza locurile invalide in audit.
- Documenteaza ordinea de reparatie pentru binding-uri multiple.
- Defineste fallback-ul cand un NPC nu are niciun context asociat.
- Precizeaza cum se trateaza structurile generate partial.
- Adauga reguli pentru reconcilierea intre world state si snapshot.
- Stabileste cum se rescrie metadata dupa mutarea unui NPC.
- Clarifica ce se intampla cand un loc este ocupat de doua ori.
- Documenteaza criteriile de respingere pentru spawn incomplet.
- Noteaza cum se valideaza integritatea relatiilor home-work-social.

### Taskuri extinse P1 - lot mare

- Defineste cum se atribuie prioritate intre doua locatii candidate.
- Clarifica ce se auditeaza cand un NPC este mutat manual.
- Documenteaza regula de fallback pentru locuinta lipsa.
- Stabileste ce inseamna o structura valida pentru spawn.
- Precizeaza cum se evita dublarea relatiei de proprietate.
- Adauga criterii de reparatie pentru entitati zombie in world.
- Noteaza ce se face cand mapping-ul contine metadata conflictuala.
- Defineste cum se trateaza regiuni fara noduri active.
- Clarifica ce refresh se aplica dupa backfill complet.
- Stabileste pasii de verificare dupa cleanup de duplicate.

## P2 - Quest, AI, story si GUI

- Stabileste regulile pentru generare de questuri cu AI doar ca draft.

Documente: `questuri-avansate-v2.md`, `progression-service.md`, `story-context-service.md`, `ai-orchestrare-si-mecanici.md`, `gui-interfete.md`, `dialog-si-conversatii.md`, `harta-clase-quest.md`, `harta-clase-ai.md`, `harta-clase-gui.md`

### Taskuri detaliate


#### `questuri-avansate-v2.md`

- Scrie fazele de evolutie pentru questurile avansate.
- Stabileste criteriile de trecere intre faze.
- Leaga questurile de mapping, story state si progression.
- Descrie ce mecanici noi sunt acceptate si care nu.
- Pastreaza documentul canonic pentru directia de quest.

#### `progression-service.md`

- Defineste runtime-ul generic de progres.
- Stabileste cum se reprezinta contractele, datoriile si evenimentele.
- Leaga progresul de quest, AI si addonuri.
- Clarifica snapshot-urile si tracking marker-ele.
- Pastreaza separatia intre selectie si executie.

#### `story-context-service.md`

- Scrie cum se compune contextul narativ din world si quest.
- Defineste ce semnale ajung in story context.
- Leaga contextul de event-urile persistente.
- Clarifica formatul pentru inspectie si debug.
- Pastreaza serviciul ca strat read-only.

#### `ai-orchestrare-si-mecanici.md`

- Scrie regulile pentru cand AI propune si cand runtime-ul executa.
- Stabileste mecanicile suportate de AI fara a rupe determinismul.
- Defineste fallback-urile si output-urile validate.
- Leaga AI de quest, story, environment si dialog.
- Noteaza explicit ce ramane draft.

#### `gui-interfete.md`

- Stabileste ecranele principale si traseele dintre ele.
- Defineste ce date arata fiecare GUI si de unde vin.
- Fixeaza filtrarea si selectia pentru authoring si inspectie.
- Leaga GUI de world, quest, story si debug.
- Pastreaza GUI-ul ca strat de prezentare, nu de business logic.

#### `dialog-si-conversatii.md`

- Stabileste cum curge conversatia intre player si NPC.
- Leaga dialogul de relatii, emotii si contextul narativ.
- Clarifica ce ramane prezentare si ce ramane decizie.
- Pastreaza exemplele mici si aplicabile.
- Noteaza ce este suport pentru authoring si ce este runtime.

#### `interactiuni.md`

- Stabileste ce tipuri de interactiuni exista cu NPC si world.
- Leaga interactiunea de GUI, dialog si evenimente.
- Clarifica ce actiuni sunt disponibile la player.
- Pastreaza documentul scurt si operational.
- Marcheaza dependentele catre sisteme de prezentare si state.

#### `reactie-npc-jucator.md`

- Stabileste cum reactioneaza NPC-ul la actiunile jucatorului.
- Leaga reactiile de emotii, relatii si context.
- Clarifica fallback-urile cand nu exista raspuns semantic.
- Pastreaza separatia dintre semnal si efect.
- Noteaza ce este model si ce este implementare.

#### `npc-uri-temporare-si-episodice.md`

- Stabileste ciclul de viata pentru NPC-uri temporare.
- Leaga durata de aparitie de questuri si scene.
- Clarifica ce se persista si ce se sterge la final.
- Pastreaza regulile de cleanup explicite.
- Evita amestecul cu NPC-urile persistente.

#### `quest-anchor-bindings.md`

- Stabileste cum se leaga ancorele de quest de world si NPC.
- Clarifica ce binding-uri sunt stabile si care sunt derivate.
- Leaga rezolvarea de progression si story.
- Pastreaza fallback-urile pentru ancore lipsa.
- Noteaza ce trebuie inspectat la reparatii.

#### `api-events-listeners-triggers.md`

- Stabileste ce evenimente si listeneri sunt expusi prin API.
- Leaga trigger-ele de quest, dialog si world.
- Clarifica ce este public si ce este intern.
- Pastreaza lista compacta si orientata pe extensibilitate.
- Noteaza ordinea in care se consuma evenimentele.

#### `lucru-alternat-quest-mapping-progression.md`

- Stabileste cum alterneaza quest, mapping si progresul.
- Leaga secventele de lucru de fluxul runtime.
- Clarifica ce este sincron si ce este amanat.
- Pastreaza tranzitiile simple si verificabile.
- Noteaza dependentele dintre subsisteme.

#### `generare-automata-questuri-ai.md`

- Stabileste cum AI propune questuri ca draft.
- Leaga generarea de validare, progression si story.
- Clarifica ce ramane manual si ce poate fi automat.
- Pastreaza output-urile validate si trasabile.
- Noteaza ce se respinge la generare.

#### `story-si-context-ai.md`

- Stabileste ce intra in contextul AI si de ce.
- Leaga story context de world, quest si dialog.
- Clarifica ce date sunt citite si ce date sunt scrise.
- Pastreaza contextul scurt si reproductibil.
- Noteaza fallback-urile cand contextul lipseste.

#### `spring-ai-mcp-serviciu-intern.md`

- Stabileste rolul serviciului intern MCP pentru AI.
- Leaga AI-ul de tool-uri, context si orchestration.
- Clarifica ce apeluri sunt interne si ce sunt expuse.
- Pastreaza separatia intre integrare si business logic.
- Noteaza conditiile de pornire si diagnostic.

#### `betonquest-directii-potrivite-pentru-ainpc.md`

- Stabileste ce idei din BetonQuest merita preluate.
- Leaga directiile de quest, dialog si progression.
- Clarifica ce este inspiratie si ce este implementare.
- Pastreaza lista scurta si argumentata.
- Noteaza ce nu se potriveste cu designul curent.

#### `questuri-faza-1-stabilizare.md`

- Stabileste ce inseamna stabilizare pentru questurile initiale.
- Leaga faza de testare, fixuri si validare.
- Clarifica ce mecanici sunt inchise si care nu.
- Pastreaza criteriile de iesire explicite.
- Noteaza ce se muta in backlog ulterior.

#### `pregatire-questuri-avansate.md`

- Stabileste pregatirea pentru questuri avansate.
- Leaga pregatirea de mapping, story si progression.
- Clarifica ce lipsea inainte de extindere.
- Pastreaza lista de preconditii scurta.
- Noteaza dependentele care trebuie rezolvate intai.

#### `obiective-quest-tipuri.md`

- Stabileste tipurile de obiective de quest.
- Leaga obiectivele de progression si anchors.
- Clarifica ce este generic si ce este specific.
- Pastreaza taxonomia scurta si usor de folosit.
- Noteaza cum se valideaza obiectivele noi.

#### `generare-ai-si-constructie-automata.md`

- Stabileste cum AI genereaza constructii sau variante de structura.
- Leaga generarea de world, template-uri si validare.
- Clarifica ce este propunere si ce este aplicare.
- Pastreaza output-urile trasabile si reversibile.
- Noteaza ce se respinge inainte de commit runtime.

#### `generare-populatie-narativa.md`

- Stabileste cum se genereaza populatia narativa.
- Leaga populatia de story context, dialog si relatii.
- Clarifica ce ramane sintetizat si ce ramane persistent.
- Pastreaza regulile de continut scurte si verificabile.
- Noteaza dependentele fata de world si quest.

#### `surse-inspiratie-plugin-ainpc.md`

- Stabileste ce surse de inspiratie sunt relevante pentru plugin.
- Leaga inspiratie de directia arhitecturala si de feature set.
- Clarifica ce este inspiratie si ce este cerinta.
- Pastreaza lista scurta si argumentata.
- Noteaza ce nu se potriveste cu obiectivele curente.

#### `harta-clase-quest.md`

- Leaga directorul de quest de resolver-ul de ancore si de progres.
- Stabileste ce clase rezolva, selecteaza si valideaza questurile.
- Pastreaza vizibil contextul narativ si starea de progres.
- Clarifica ce parte este selectie si ce parte este executie.
- Mentine harta suficient de scurta pentru navigare rapida.

#### `harta-clase-ai.md`

- Leaga snapshot-urile de prompt de engine-ul AI si dialog.
- Stabileste ce servicii citesc contextul narativ si ce servicii il scriu.
- Pastreaza separate piesele pentru orchestrare, prompting si reactii NPC.
- Clarifica dependentele spre quest, story si progress.
- Ofera un traseu scurt pentru debug si extindere.

#### `harta-clase-gui.md`

- Stabileste ce ecrane principale exista si cine le deschide.
- Leaga GUI-ul de quest, story, world si debug.
- Clarifica ce clase sunt doar de prezentare si care orchestreaza datele.
- Pastreaza lista de ecrane si manageri usor de parcurs.
- Evita duplicarea detaliilor de business logic.

### Taskuri suplimentare P2

- Descrie fluxul de aprobare pentru quest drafts.
- Clarifica modul in care scorul se combina cu preferintele mecanice.
- Defineste fallback-ul cand story context este gol.
- Specifica regulile de expunere a quest data in GUI.
- Stabileste ce evenimente de dialog sunt persistate.
- Detaliaza sincronizarea dintre progression si quest log.
- Precizeaza cum se versioneaza scenariile narative.
- Adauga reguli pentru conflict intre questuri active.
- Defineste criteriile de degradare pentru AI orchestration.
- Specifica ce se intampla cand un NPC reactioneaza in afara contextului.

### Taskuri extinse P2

- Stabileste ce se salveaza dintr-un dialog nereusit.
- Clarifica cine decide anularea unui quest activ.
- Documenteaza cum se refolosesc ancorele in questuri similare.
- Defineste cand GUI-ul afiseaza date stale si cum le marcheaza.
- Precizeaza ordinea de refresh pentru story si progression.
- Stabileste regula pentru conflict intre doua questuri cu acelasi anchor.
- Defineste ce semnale story sunt obligatorii pentru un quest avansat.
- Clarifica ce date AI poate vedea si ce date raman ascunse.
- Documenteaza comportamentul GUI cand lipsesc datele pentru un slot.
- Noteaza cum se recupereaza progresul dupa o intrerupere in AI flow.

### Taskuri extinse P2 - lot mare

- Defineste o politica pentru retry la generarea draftului de quest.
- Clarifica cand se marcheaza un quest ca suspendat, nu anulat.
- Stabileste ce date narative trebuie sincronizate la fiecare tick.
- Noteaza cum se selecteaza un fallback de dialog cand AI esueaza.
- Documenteaza ce campuri din quest sunt vizibile in authoring.
- Precizeaza ordinea de salvare pentru story, progression si GUI.
- Adauga reguli pentru conflict intre mai multe surse de semnale.
- Defineste ce se intampla cand un anchor se invalideaza in runtime.
- Clarifica cum se marcheaza o reactie NPC invalida.
- Stabileste criteriile pentru reactivarea unui quest dupa pauza.

## P3 - Debug, testare, release si hardening


Documente: `debugging-si-testare.md`, `release-checklist.md`, `server-admin-runbook.md`, `prevenire-duplicare-npc.md`, `harta-clase-debug.md`

### Taskuri detaliate

- Scrie checklist-ul de debug pentru dump, audit si logs.
- Defineste cazurile de restart si recuperare pentru NPC duplicat.
- Precizeaza ce intrari trebuie verificate in release checklist.
- Lega bufferul de evenimente de inspectia rapida.
- Documenteaza ce face un operator cand apar erori in productie.

#### `debugging-si-testare.md`

- Scrie procedura standard de diagnostic pentru buguri de runtime.
- Leaga testarea manuala de comenzi si debugdump.
- Clarifica ce faci cand un scenariu nu poate fi repro.
- Stabileste ordinea de izolare: logs, audit, dump, retry.
- Pastreaza documentul ca ghid operational, nu ca lista de taskuri.

#### `release-checklist.md`

- Stabileste ce trebuie sa treaca inainte de release.
- Leaga release-ul de backup si rollback.
- Define?te semnele de stop pentru build si deploy.
- Noteaza cum se valideaza jar-ul si versiunea.
- Pastreaza checklist-ul scurt si repetabil.

#### `server-admin-runbook.md`

- Scrie pa?ii de pornire si oprire a serverului Paper.
- Noteaza cum se aplica backup si restore.
- Stabileste cum verifici starea pluginului si a addonurilor.
- Leaga runbook-ul de debug si release.
- Pastreaza-l ca ghid de operator, nu ca design doc.

#### `prevenire-duplicare-npc.md`

- Stabileste identitatea NPC-ului si sursele ei de adevar.
- Noteaza cauzele comune ale duplicarii.
- Scrie procedura de cleanup pentru entitati si DB.
- Leaga prevenirea duplicarii de spawn si persistence.
- Pastreaza actiunile de repair clar auditabile.

#### `harta-clase-debug.md`

- Arata ce colecteaza `DebugDumpService` si `RecentEventsBuffer`.
- Leaga debug-ul semantic de world mapping si audit.
- Stabileste ce clase sunt utile pentru inspectie rapida.
- Pastreaza harta orientata pe troubleshooting, nu pe runtime.
- Clarifica legatura dintre inspectie, dump si reparatie.

#### `audit.md`

- Stabileste ce verifica auditul si cand se ruleaza.
- Leaga auditul de world mapping, debug si release.
- Clarifica ce rezultat este informativ si ce rezultat blocheaza.
- Pastreaza formatul de raport scurt si comparabil.
- Noteaza ce trebuie reparat dupa fiecare audit esuat.

#### `analiza-erori-si-plan-rezolvare.md`

- Stabileste cum se triereaza erorile recurente.
- Leaga analiza de logs, dump-uri si planul de fix.
- Clarifica ce este simptom si ce este cauza.
- Pastreaza actiunile in ordinea impactului.
- Marcheaza ce ramane deschis dupa analiza.

#### `documentatie-lipsa.md`

- Stabileste ce documente lipsesc si de ce conteaza.
- Leaga lipsurile de prioritatile curente.
- Clarifica ce este blocant si ce este doar de igiena.
- Pastreaza lista scurta si actualizata.
- Noteaza cine trebuie sa consume fiecare lipsa.

#### `patch-planner.md`

- Stabileste criteriile pentru un patch sigur.
- Leaga patch planning de audit si world mapping.
- Clarifica ce intra in patch si ce ramane backlog.
- Pastreaza pasi de aplicare si verificare clari.
- Evita sa devina un jurnal de implementare.

#### `reducere-marime-jar.md`

- Stabileste ce contribuie la marimea JAR-ului.
- Leaga reducerea de packaging si verificare de release.
- Clarifica ce poate fi eliminat fara a rupe runtime-ul.
- Pastreaza ipotezele masurabile.
- Noteaza ce se valideaza dupa fiecare optimizare.

#### `migration-si-backup.md`

- Stabileste rutina de backup inainte de migrare.
- Leaga migrarea de restore si de verificare.
- Clarifica ce date trebuie protejate explicit.
- Pastreaza pasii de rollback usor de urmat.
- Noteaza ce se testeaza dupa restore.

#### `server-credentials.md`

- Stabileste unde sunt pastrate credentialele serverului.
- Clarifica ce nu trebuie niciodata pus in documentatie publica.
- Leaga accesul de procedurile de operare.
- Pastreaza instructiunile scurte si restrictive.
- Noteaza rotatia si recuperarea credentialelor.

#### `mcp-docker-server-mvp-si-faze.md`

- Stabileste fazele pentru serverul MCP in Docker.
- Leaga MVP-ul de context, audit si backup.
- Clarifica ce intra in prima faza si ce ramane ulterior.
- Pastreaza progresul pe etape vizibil.
- Noteaza dependentele de infrastructura locala.

#### `server-npc-mvp-si-faze.md`

- Stabileste fazele pentru NPC MVP pe server.
- Leaga MVP-ul de spawn, dialog si debug.
- Clarifica ce este minim viabil si ce este extensie.
- Pastreaza criteriile pe etape usor de urmarit.
- Noteaza ce se verifica in fiecare faza.

#### `verificari-server-d1.md`

- Stabileste primul set de verificari pentru server.
- Leaga verificarea de pornire, bootstrap si health.
- Clarifica ce trebuie confirmat inainte de a avansa.
- Pastreaza rezultatul simplu si repetabil.
- Noteaza ce se opreste daca primul pas esueaza.

#### `verificari-server-d2.md`

- Stabileste al doilea set de verificari pentru server.
- Leaga verificarea de world, NPC si persistenta.
- Clarifica ce se compara cu starea asteptata.
- Pastreaza pasii scurti si auditabili.
- Noteaza ce necesita reparatie imediata.

#### `verificari-server-d3.md`

- Stabileste al treilea set de verificari pentru server.
- Leaga verificarea de quest, AI si dialog.
- Clarifica ce rezultate sunt acceptate.
- Pastreaza testele orientate pe fluxuri reale.
- Noteaza ce se marcheaza ca regresie.

#### `verificari-server-d4.md`

- Stabileste al patrulea set de verificari pentru server.
- Leaga verificarea de GUI, debug si operare.
- Clarifica ce trebuie vazut in interfe?e.
- Pastreaza ordinea de verificare stabila.
- Noteaza ce se intoarce in backlog.

#### `verificari-server-d5.md`

- Stabileste al cincilea set de verificari pentru server.
- Leaga verificarea de release si rollback.
- Clarifica ce se considera gata pentru etapa.
- Pastreaza criteriile de trecere explicite.
- Noteaza ce se face daca apare un esec.

#### `verificari-server-d6.md`

- Stabileste al saselea set de verificari pentru server.
- Leaga verificarea de hardening si backup.
- Clarifica ce este obligatoriu pentru stabilitate.
- Pastreaza raportarea scurta si comparabila.
- Noteaza ce trebuie corectat inainte de reluare.

#### `verificari-server-d7-d9.md`

- Stabileste verificarea extinsa pentru ultimele etape.
- Leaga rezultatele de toate subsistemele principale.
- Clarifica ce ramane deschis dupa secventa larga.
- Pastreaza sumarul mai degraba decat detaliul.
- Noteaza ce se transfera in audit sau hotfix.

### Taskuri suplimentare P3

- Defineste matricea de prioritate pentru incidente runtime.
- Specifica formatul minim al unui raport de bug reproducibil.
- Stabileste pasii de verificare dupa restart de server.
- Clarifica semnele de coruptie in snapshot-uri.
- Adauga checklist pentru inspectia logurilor de productie.
- Defineste cand un audit blocheaza release-ul.
- Stabileste pragurile pentru alertare manuala.
- Documenteaza procedura de rollback pentru fisiere de configurare.
- Precizeaza cum se valideaza repararea dupa duplicate cleanup.
- Adauga verificari pentru datele din debug dump.

### Taskuri extinse P3

- Stabileste ce loguri se pastreaza pentru investigatii lungi.
- Clarifica cand un incident trece din warning in blocking.
- Documenteaza cum se compara dump-urile intre rulari.
- Defineste ce sanitarizare se aplica in artefactele de debug.
- Precizeaza cum se inchide o anomalie dupa confirmarea repararii.
- Specifica ce campuri trebuie mascate in rapoartele trimise extern.
- Clarifica cum se marcheaza o regresie in raportul de incident.
- Stabileste procedura de verificare dupa repararea unui dump corupt.
- Documenteaza ce trebuie salvat cand auditul esueaza partial.
- Precizeaza criteriile de acceptare pentru un hotfix minimal.

### Taskuri extinse P3 - lot mare

- Defineste ce inseamna un log suficient pentru reproducere.
- Clarifica ce se considera dovada valida pentru inchiderea bugului.
- Stabileste cum se structureaza un raport de incident intern.
- Noteaza ce verifici dupa fiecare operatie de restore.
- Documenteaza cum se triereaza erorile intermitente.
- Precizeaza ce date sunt necesare pentru rollback rapid.
- Adauga reguli pentru compararea a doua snapshot-uri consecutive.
- Defineste cand un warning devine ticket obligatoriu.
- Clarifica ce artefacte sunt obligatorii in fiecare debug dump.
- Stabileste cum se documenteaza un hotfix aplicat manual.

## P4 - Modularizare, Kotlin si addonuri


Documente: `kotlin-style-guide.md`, `kotlin-interop-api-addonuri.md`, `kotlin-paper-packaging-si-smoke.md`, `kotlin-testing-strategy.md`, `strategie-plugin-modular-si-scenarii-programabile.md`, `harta-clase-platform-db.md`

### Taskuri detaliate

- Scrie regulile de stil Kotlin pentru acest repo.
- Stabileste ce tipuri Kotlin sunt sigure pentru Java interop.
- Defineste smoke test-ul de packaging Paper.
- Fixeaza strategia de testare pentru conversii pe module.
- Detaliaza strategiile pentru addonuri si scenarii programabile.

#### `kotlin-style-guide.md`

- Scrie conven?iile de nume si format pentru acest repo.
- Leaga ghidul de clasele si modulele existente.
- Clarifica ce stil este acceptat in core si in addonuri.
- Stabileste cum se trateaza null-safety si extension functions.
- Pastreaza ghidul scurt si aplicabil.

#### `kotlin-interop-api-addonuri.md`

- Stabileste ce tipuri si semnaturi sunt sigure pentru Java.
- Specifica ce ramane in API si ce ramane in core.
- Leaga contractul de addonuri de `ainpc-api`.
- Clarifica riscurile de interop pentru clasele publice.
- Pastreaza exemplele minimale si actualizabile.

#### `kotlin-paper-packaging-si-smoke.md`

- Scrie traseul de build pentru Paper cu Kotlin activ.
- Stabileste smoke test-ul minimal pentru JAR.
- Leaga packaging-ul de verificarea runtime-ului.
- Precizeaza ce artefacte se verifica dupa build.
- Pastreaza documentul orientat pe executie.

#### `kotlin-testing-strategy.md`

- Stabileste ce tipuri de teste exista pe conversii Kotlin.
- Leaga testarea de smoke, unit si integration unde exista.
- Defineste criteriile de acceptare pentru migrare.
- Clarifica cum se evita testele prea grele.
- Pastreaza strategia curata si incrementala.

#### `strategie-plugin-modular-si-scenarii-programabile.md`

- Stabileste directia de modularizare pentru plugin si addonuri.
- Leaga scenariile programabile de contractele din API.
- Clarifica ce ramane core, ce ramane addon si ce ramane scenariu.
- Pastreaza separatia dintre platforma, extensii si continut.
- Ofera un plan incremental, nu o rescriere totala.

#### `json-yaml-contract.md`

- Stabileste contractul dintre JSON si YAML in datele proiectului.
- Leaga formatul de config, feature flags si addonuri.
- Clarifica ce schema este obligatorie si ce este optionala.
- Pastreaza exemplele minimale si verificabile.
- Noteaza unde sunt valide datele si unde trebuie convertite.

#### `feature-flags-lifecycle.md`

- Stabileste ciclul de viata al feature flags.
- Leaga flag-urile de bootstrap, runtime si reload.
- Clarifica ce se decide la pornire si ce se poate schimba ulterior.
- Pastreaza regulile de fallback si de default.
- Noteaza auditul si inspectia pentru flag-uri.

#### `worldedit-integration-contract.md`

- Stabileste ce integreaza contractul cu WorldEdit.
- Leaga integrarea de generare, patch planning si validare.
- Clarifica ce apeluri sunt permise si ce ramane extern.
- Pastreaza exemplele mici si orientate pe compatibilitate.
- Noteaza limitarile de runtime si de editare.

#### `template-cladiri-si-marker-nodes.md`

- Stabileste modelul template pentru cladiri si marker nodes.
- Leaga template-ul de mapping si de generare.
- Clarifica ce e metadate si ce e layout.
- Pastreaza documentul ca ghid de authoring.
- Noteaza cum se valideaza consistenta template-urilor.

#### `structuri-exterioare-satului.md`

- Stabileste cum sunt modelate structurile exterioare.
- Leaga planificarea de world mapping si audit.
- Clarifica ce este generare si ce este inspectie.
- Pastreaza dependentele catre satul semantic vizibile.
- Noteaza ce parti sunt fixe si ce parti sunt parametrizate.

#### `addon-config-template.md`

- Stabileste template-ul standard pentru config de addon.
- Leaga config-ul de contractul JSON/YAML.
- Clarifica ce optiuni sunt sigure si care sunt avansate.
- Pastreaza exemplul minimal si complet.
- Noteaza cum se valideaza configuratia la incarcare.

#### `kotlin-code-review-checklist.md`

- Stabileste checklist-ul de review pentru cod Kotlin.
- Leaga review-ul de interop, null-safety si style.
- Clarifica ce probleme sunt blockers si ce sunt nitpicks.
- Pastreaza checklist-ul practic si scurt.
- Noteaza cum se aplica la PR-uri reale.

#### `kotlin-coroutines-paper-policy.md`

- Stabileste politica pentru coroutines in Paper.
- Leaga utilizarea de thread-safety si runtime constraints.
- Clarifica ce este acceptat in main thread si ce nu.
- Pastreaza exemplele orientate pe server code.
- Noteaza cum se auditeaza abuzul de async.

#### `refactorizare-si-impartire-pe-module.md`

- Stabileste cum se imparte proiectul pe module.
- Leaga refactorizarea de API, core si addonuri.
- Clarifica ce se muta si ce ramane neschimbat.
- Pastreaza ordinea de extragere simpla.
- Noteaza riscurile de compatibilitate intre module.

#### `json-yaml-contract-exemple.md`

- Ofera exemple concrete pentru contractul JSON/YAML.
- Leaga exemplele de schema si de addon config.
- Clarifica ce intrare este valida si de ce.
- Pastreaza exemplele scurte si reproductibile.
- Noteaza unde se folosesc in documentatia principala.

#### `deepseek/coding-automation-stack-linux-vscode-deepseek-mcp.md`

- Stabileste stack-ul de automatizare pentru coding asistat.
- Leaga toolchain-ul de MCP, VS Code si fluxul local.
- Clarifica ce este setup si ce este operare zilnica.
- Pastreaza instructiunile orientate pe reproducere.
- Noteaza ce trebuie actualizat cand toolchain-ul se schimba.

### Taskuri suplimentare P4

- Defineste standardul de packaging pentru module noi.
- Clarifica contractul dintre API si implementarile Kotlin.
- Specifica regulile pentru coroutines in taskuri server.
- Adauga checklist pentru evaluarea interop-ului la review.
- Stabileste criterii pentru extragerea unui modul nou.
- Precizeaza ce API-uri raman stabile pe termen lung.
- Documenteaza conventiile de naming intre module.
- Defineste testele obligatorii inainte de publicarea addonului.
- Clarifica gestionarea resurselor comune intre addonuri.
- Stabileste politica de compatibilitate pentru scenarii programabile.

### Taskuri extinse P4

- Stabileste ce ramane in Kotlin si ce trebuie expus ca Java-friendly.
- Clarifica pragul la care un helper devine modul separat.
- Documenteaza cum se testeaza API-urile publice cross-module.
- Defineste o regula unica pentru dependente circulare.
- Precizeaza cum se versionizeaza contractele dintre addonuri.
- Specifica ce clase sunt permise in layer-ul public al addonurilor.
- Clarifica cand se prefera interfata in locul clasei concrete.
- Documenteaza cum se evita duplicarea helperelor intre module.
- Stabileste ordinea de migrare cand un modul este spart in doua.
- Noteaza ce compatibilitate trebuie verificata la publicarea unui addon.

### Taskuri extinse P4 - lot mare

- Defineste ce inseamna o dependinta acceptabila intre module.
- Clarifica ce cod ramane comun si ce se extrage in helper.
- Stabileste reguli pentru API stabil versus API experimental.
- Noteaza cum se testeaza contractele publice dupa refactor.
- Documenteaza ce se face cand un addon cere o clasa interna.
- Precizeaza cand un wrapper Kotlin este obligatoriu.
- Adauga criterii pentru reducerea suprafetei publice.
- Defineste ce artefacte trebuie verificate in smoke test.
- Clarifica cum se gestioneaza breaking changes in addon API.
- Stabileste ce inseamna o migratie incrementala corecta.

## P5 - Istoric si igiena documentatiei


Documente: `index-arhiva.md`, `arhiva/kotlin-migration/README.md`, `arhiva/questuri-avansate-v1.md`, `arhiva/ordine-spawn-npc-cladiri-region-node-v1.md`, `index-navigare.md`, `start-here.md`

### Taskuri detaliate

- Pastreaza arhiva Kotlin ca istoric clar, nu ca sursa de implementare.
- Asociaza fiecare document vechi cu documentul canonic actual.
- Pastreaza hub-urile de navigare scurte si stabile.
- Curata periodic orice document istoric depasit.
- Verifica daca o informatie veche trebuie mutata sau rezumata.

#### `index-arhiva.md`

- Leaga documentele istorice de motivul arhivarii.
- Ofera acces rapid la istoria importanta fara zgomot.
- Pastreaza lista scurta si relevanta.
- Evita sa devina un catalog complet.
- Leaga arhiva de documentele canonice curente.

#### `index-navigare.md`

- Pastreaza un alias compact pentru `start-here`.
- Ofera un hub scurt catre functional, clase si arhiva.
- Nu dubla explicatiile din `start-here`.
- Ramai la legaturi rapide, fara detalii suplimentare.
- Actualizeaza-l doar daca apar hub-uri noi.

#### `start-here.md`

- Pastreaza traseul principal clar si scurt.
- Arata ordinea recomandata de citire.
- Trimite spre functional, clase si arhiva.
- Evita sa devina un index complet.
- Foloseste-l ca primul punct de intrare pentru docs.

#### `arhiva/kotlin-migration/README.md`

- Leaga intrarea de contextul istoric al migrarii Kotlin.
- Explica rapid de ce exista arhiva si ce acopera.
- Pastreaza referintele catre documentele de conversie.
- Evita sa se transforme in document de implementare curenta.
- Ramai la rolul de istoric si orientare.

#### `arhiva/kotlin-migration/conversie-java-la-kotlin.md`

- Stabileste primul pas istoric al migrarii Java la Kotlin.
- Leaga exemplele vechi de directia actuala.
- Clarifica ce solutii au ramas utile si care nu.
- Pastreaza documentul ca referinta istorica.
- Noteaza ce implica pentru interop si layout.

#### `arhiva/kotlin-migration/conversie-java-la-kotlin-partea-2.md`

- Continua contextul istoric al conversiei.
- Leaga partea a doua de progresul initial si de regresii.
- Clarifica ce s-a imbunatatit fata de prima parte.
- Pastreaza sectiunea ca jurnal istoric.
- Noteaza ce poate fi reutilizat ca idee, nu ca implementare.

#### `arhiva/kotlin-migration/conversie-java-la-kotlin-partea-3.md`

- Urmareste evolutia istorica a migrarii.
- Leaga partea a treia de deciziile de interop.
- Clarifica ce a functionat si ce a fost abandonat.
- Pastreaza contextul pentru comparatii.
- Noteaza dependentele de platforma si build.

#### `arhiva/kotlin-migration/conversie-java-la-kotlin-partea-4.md`

- Pastreaza continuarea istorica a migrarii.
- Leaga partea a patra de pasii anteriori.
- Clarifica ce s-a fixat in aceasta etapa.
- Pastreaza documentul strict in arhiva.
- Noteaza ce poate ghida refactorizarile actuale.

#### `arhiva/kotlin-migration/conversie-java-la-kotlin-partea-5.md`

- Incheie seria istorica de conversie.
- Leaga concluziile de starea curenta a codului.
- Clarifica ce lectii au ramas valabile.
- Pastreaza documentul ca referinta, nu ca sursa activa.
- Noteaza unde se vede impactul in documentele curente.

#### `arhiva/kotlin-migration/kotlin-gradle-activation-plan.md`

- Stabileste planul istoric pentru activarea Kotlin in Gradle.
- Leaga planul de conversia Java si de build.
- Clarifica ce pasi au fost necesari pentru migrare.
- Pastreaza planul ca material de arhiva.
- Noteaza ce a devenit ulterior standard in repo.

#### `arhiva/questuri-avansate-v1.md`

- Leaga versiunea veche de documentul canonic curent.
- Pastreaza contextul deciziilor istorice pentru quest.
- Stabileste ce idei au fost pastrate si ce idei au fost abandonate.
- Evita sa fie folosita ca sursa de adevar noua.
- Ofera un punct de referinta pentru comparatie.

#### `arhiva/ordine-spawn-npc-cladiri-region-node-v1.md`

- Pastreaza ordinea istorica de spawn ca referinta.
- Leaga exemplul vechi de fluxul curent de spawn.
- Stabileste ce s-a schimbat in noua ordine operationala.
- Evita confuzia dintre procedura veche si cea activa.
- Ofera context pentru reparatii si regresii.

#### `prim-demo-functionalitate-minima-diversa.md`

- Stabileste ce include demo-ul minim functional.
- Leaga demo-ul de taskurile de pregatire si validare.
- Clarifica ce este demonstratie si ce este implementare stabila.
- Pastreaza scopul demo-ului mic si masurabil.
- Noteaza ce trebuie sa mearga fara interventie manuala.

#### `50-taskuri-prim-demo.md`

- Stabileste backlog-ul initial pentru demo.
- Leaga fiecare task de o demonstratie concreta.
- Clarifica ce intra in MVP si ce ramane ulterior.
- Pastreaza ordinea de implementare usor de urmarit.
- Noteaza dependentele dintre taskuri.

#### `criterii-gata-prim-demo.md`

- Stabileste criteriile de done pentru demo.
- Leaga criteriile de functionalitate, stabilitate si prezentare.
- Clarifica ce inseamna suficient pentru o etapa demo.
- Pastreaza criteriile verificabile manual.
- Noteaza ce se respinge daca nu trece criteriile.

#### `inventar-comenzi-prim-demo.md`

- Stabileste lista de comenzi necesare pentru demo.
- Leaga comenziile de operare si de verificare.
- Clarifica ce comenzi sunt obligatorii si care sunt optionale.
- Pastreaza inventarul scurt si actualizabil.
- Noteaza ce trebuie testat dupa fiecare schimbare.

#### `procedura-backup-prim-demo.md`

- Stabileste backup-ul inainte de demo.
- Leaga procedura de restore si verificare.
- Clarifica ce artefacte sunt protejate.
- Pastreaza pasii scurti si repetabili.
- Noteaza cum se revine rapid la stare buna.

#### `env-prim-demo.md`

- Stabileste variabilele de mediu pentru demo.
- Leaga env-ul de build, runtime si verificari.
- Clarifica ce valori sunt obligatorii.
- Pastreaza exemplul minimal si reproductibil.
- Noteaza ce trebuie documentat la schimbarea mediului.

#### `sumar-implementare-demo.md`

- Stabileste ce s-a implementat pentru demo.
- Leaga sumarul de taskuri si criterii de gata.
- Clarifica ce ramane backlog.
- Pastreaza rezumatul scurt si orientat pe decizie.
- Noteaza ce se poate demonstra imediat.

#### `player-onboarding-initiere.md`

- Stabileste primul pas al jucatorului in sistem.
- Leaga onboarding-ul de tutorial, GUI si verificari.
- Clarifica ce este necesar pentru initiere minima.
- Pastreaza fluxul scurt si fara blocaje.
- Noteaza ce se masoara ca succes initial.

#### `playable-village-ux.md`

- Stabileste experienta minima de sat jucabil.
- Leaga UX-ul de spawn, interactiuni si navigare.
- Clarifica ce este feedback vizual si ce este logica.
- Pastreaza prioritatea pe lizibilitate si ritm.
- Noteaza ce trebuie verificat manual in joc.

#### `faze-observatii-avertizari.md`

- Stabileste observatiile si avertizarile curente.
- Leaga notele de faza de documentele canonice.
- Clarifica ce este risc real si ce este doar nota.
- Pastreaza lista scurta si actualizata.
- Noteaza ce necesita follow-up imediat.

#### `faze-urmatoare-categorii.md`

- Stabileste categoriile de lucru pentru fazele urmatoare.
- Leaga categoriile de taskuri si dependente.
- Clarifica ordinea de atac pentru iteratia urmatoare.
- Pastreaza rezumatul usor de scanat.
- Noteaza ce depinde de deciziile curente.

#### `faze-urmatoare-250.md`

- Stabileste obiectivele pentru urmatorul lot de 250.
- Leaga lotul de prioritatile curente.
- Clarifica ce ramane din backlog si ce intra acum.
- Pastreaza lista orientata pe implementare.
- Noteaza ce criterii de progres se urmaresc.

#### `faze-urmatoare-250-partea-2.md`

- Continua obiectivele din lotul precedent.
- Leaga partea a doua de blocajele rezolvate.
- Clarifica ce se muta in etapa urmatoare.
- Pastreaza continuitatea intre loturi.
- Noteaza dependentele care se inchid intre timp.

#### `verificari-server-d1-d9.md`

- Stabileste setul de verificari pe zile sau etape.
- Leaga fiecare verificare de un rezultat clar.
- Clarifica ce se marcheaza ca trecut sau esuat.
- Pastreaza ordinea verificabila si repetabila.
- Noteaza ce trebuie reparat intre verificari.

#### `schema-scenariu-predefinit-testare.md`

- Stabileste schema pentru scenarii de test predefinite.
- Leaga scenariile de world, NPC si quest.
- Clarifica ce este fixture si ce este output.
- Pastreaza formatul compatibil cu testarea manuala.
- Noteaza ce se valideaza dupa rulare.

### Taskuri suplimentare P5

- Defineste criteriile de mutare a unui document in arhiva.
- Clarifica ce documente sunt sursa de adevar curenta.
- Adauga reguli pentru pastrarea istoricului de decizie.
- Specifica cum se evita duplicarea intre indexuri.
- Stabileste procesul de curatare a documentelor obsolete.
- Defineste o schema de referinta intre vechi si canonic.
- Clarifica ce note merita pastrate ca istorie.
- Adauga verificare periodica a linkurilor moarte.
- Precizeaza cine actualizeaza indexurile de navigare.
- Stabileste criterii pentru rezumatul istoric minim.

### Taskuri extinse P5

- Defineste ce informatie se muta din canonic in istoric.
- Clarifica cand un index devine redundant si poate fi retras.
- Documenteaza cum se noteaza o decizie abandonata.
- Stabileste regula pentru re-etichetarea documentelor vechi.
- Precizeaza cum se compara doua versiuni istorice similare.
- Specifica ce documente istorice trebuie mentionate in fiecare arhiva.
- Clarifica cum se marcheaza un document partial depasit.
- Documenteaza politica pentru linkuri interne catre arhive.
- Stabileste cand un rezumat istoric merita actualizat.
- Noteaza cum se tine evidenta mutarilor intre categorii.

### Taskuri extinse P5 - lot mare

- Defineste ce se arhiveaza automat si ce necesita decizie manuala.
- Clarifica cum se elimina un document fara a pierde contextul.
- Stabileste regula pentru redenumirea fisierelor istorice.
- Noteaza cum se pastreaza urmele unei mutari de document.
- Documenteaza ce format are un rezumat istoric minim.
- Precizeaza cum se evita contradictiile intre arhiva si canonic.
- Adauga criterii pentru consolidarea documentelor aproape identice.
- Defineste ce se face cu linkurile catre documente mutate.
- Clarifica cum se marcheaza un document drept referinta, nu activ.
- Stabileste procesul de audit pentru documentatia veche.


