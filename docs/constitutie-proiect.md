# Constitutie Proiect AINPC

Actualizat: 2026-05-28

Importanta: ridicata. Acest document este reper constitutional pentru arhitectura, directie, reguli de dezvoltare si decizii intre alternative.

Acest document stabileste regulile de baza ale proiectului AINPC. Este document de orientare si control: cand exista conflict intre idei, backlog, implementari rapide sau documente vechi, acest document fixeaza directia generala, iar documentele de specialitate fixeaza detaliile.

## Articolul 1. Identitate

AINPC este un plugin Paper pentru Minecraft, modular, configurabil si flexibil, care construieste o lume cu NPC-uri persistente, sate lizibile, questuri, rutina, context narativ si integrare AI controlata.

Scopul proiectului nu este sa adauge doar comenzi sau NPC-uri izolate. Scopul este sa produca un server in care playerul intelege rapid cine sunt NPC-urile, unde locuiesc, ce rol au, ce se intampla in sat si de ce actiunile sale conteaza.

Proiectul trebuie sa ramana jucabil inainte sa devina complex. Orice sistem nou trebuie sa intareasca experienta de joc, debugging-ul sau stabilitatea operationala. Flexibilitatea proiectului inseamna configuratii clare, module decuplate si extensii controlate, nu comportament implicit greu de inteles.

## Articolul 2. Principii obligatorii

1. Playability inainte de volum. Un sat mic, coerent si verificabil valoreaza mai mult decat multe mecanici incomplete.
2. Date validate inainte de automatizare. AI-ul, generatoarele si patch planner-ul produc drafturi sau planuri validate, nu executa modificari directe fara control.
3. Core determinist, AI asistiv. Regulile de progres, reward, spawn, persistenta si audit apartin serviciilor deterministe. AI-ul formuleaza, propune, rezuma sau completeaza continut validat.
4. Mapping semantic inainte de gameplay avansat. Questurile, rutina, story si dialogul trebuie sa consume regiuni, places, nodes si anchors clare.
5. Modularizare fara fragmentare prematura. Modulele trebuie sa protejeze contracte reale, nu sa ascunda cod instabil in multe pachete mici.
6. Compatibilitate pentru addonuri. `ainpc-api` trebuie tratat ca suprafata publica; codul intern din core nu devine API doar pentru ca este accesibil.
7. Test si audit pentru schimbari riscante. Orice schimbare care afecteaza spawn, DB, quest progress, packaging sau API trebuie sa aiba verificare explicita.
8. Documentatia descrie starea reala. Cand implementarea schimba statusul unei faze, se actualizeaza documentele canonice relevante.
9. Configurabilitate explicita. Orice mecanica majora trebuie sa poata fi activata, dezactivata sau ajustata prin config, addon sau profil de scenariu, fara recompilare.
10. Dezactivare completa. Orice caracteristica majora trebuie sa aiba o cale clara de dezactivare fara sa strice restul pluginului: AI, story, questuri, generare, GUI, rutine, simulare, world mapping, addonuri, integrari externe si storage avansat.
11. Core neutru. Codul core trebuie sa ramana independent de tema, lore, epoca, stil vizual sau scenariu, astfel incat orice addon valid sa se poata potrivi fara schimbari in core. Core-ul poate include continut demo sau fallback, dar numai daca este marcat clar, configurabil si dezactivabil cand se activeaza un addon real.
12. Feature-uri neperturbatoare. O functionalitate noua nu are voie sa strige peste logica generala a pluginului, sa preia controlul implicit asupra gameplay-ului sau sa produca efecte deranjante pentru player/admin. Ea trebuie sa intre in sistem prin contracte clare, feature flag, audit si fallback, nu prin efecte ascunse.

## Articolul 3. Structura canonica

Structura principala a proiectului este:

| Zona | Rol constitutional |
|---|---|
| `ainpc-api` | Contract public pentru addonuri, platform API, world info, addon registry si tipuri stabile expuse in afara core-ului |
| `ainpc-core-plugin` | Implementarea principala Paper: comenzi, NPC, quest, story, progression, mapping, spawn, GUI, AI orchestration, debug si audit |
| `ainpc-scenario-medieval` | Addon/scenariu exemplar pentru continut medieval si pachete de quest configurabile |
| Addonuri de scenariu | Module care adauga reguli, quest packs, triggers, conditions, actions si comportament specific unei teme |
| Addonuri de story | Module care adauga fire narative, personaje, reputatii, evenimente, dialoguri si stari narative |
| Addonuri de resurse si textura | Pachete optionale pentru iteme, modele, sunete, texturi si prezentare vizuala compatibila cu serverul |
| Datapack-uri compatibile | Directie de interoperabilitate pentru retete, loot tables, advancements, tags, worldgen sau continut vanilla-friendly |
| `docs` | Sursa canonica pentru design, roadmap, runbook-uri si reguli de dezvoltare |
| `scripts` | Automatizari locale pentru context MCP, release, smoke, backup si verificari operationale |
| `data`, `.ai`, `.codex` | Context local, memorie MCP/Codex si artefacte operationale; nu sunt loc pentru secrete sau specificatii canonice de gameplay |
| Server MCP pentru Paper | Directie viitoare pentru integrare controlata intre plugin, context, instrumente de administrare, debug, AI si automatizari externe |

Regula de proprietate:

- `ainpc-api` nu depinde de `ainpc-core-plugin`.
- `ainpc-core-plugin` poate consuma `ainpc-api`, dar nu trebuie sa forteze addonurile sa cunoasca internals.
- `ainpc-scenario-medieval` demonstreaza extensibilitatea, nu stabileste reguli globale pentru toate scenariile.
- Core-ul nu trebuie sa hardcodeze continut medieval, story specific, textura, economie, lore sau reguli care apartin unui addon. Continutul demo din core este permis doar ca bootstrap verificabil si trebuie sa poata fi oprit cand un addon inlocuieste acel domeniu.
- Addonurile trebuie sa declare tipul, dependintele, capabilitatile si feature flags pe care le activeaza.
- Addonurile de resurse/textura si datapack-urile compatibile nu trebuie sa fie obligatorii pentru functionarea core-ului.
- Cand un addon activ furnizeaza scenariu, story, questuri, resurse sau reguli, demo-ul echivalent din core trebuie sa intre in modul dezactivat, fallback sau explicit namespaced, fara coliziuni de ID-uri.
- Documentele din radacina `docs/` raman canonice; `docs/categorii/` sunt indexuri de navigare.
- Serverul MCP dedicat pluginului Paper trebuie sa ramana optional, local-first si securizat; el extinde observabilitatea si automatizarea, nu inlocuieste regulile runtime din core.

## Articolul 4. Directia de dezvoltare

Directia proiectului se dezvolta in opt axe, in aceasta ordine de maturizare:

1. Baseline verificabil: build, audit, debugdump, documentatie, context MCP, teste si status real al codului.
2. Sat jucabil: mapping semantic, regiuni, places, nodes, spawn controlat, households, rutine si interactiuni clare.
3. Demo intern: player onboarding, dialog, questuri cap-coada, progression, story state, GUI si smoke pe server Paper.
4. Modularizare publica: API stabil, addon registry, scenarii configurabile si contracte Java/Kotlin compatibile.
5. Runtime extensibil: scenario runtime, triggers, conditions, actions, generare asistata de questuri si story context matur.
6. Generare harti si structuri: settlement plan, template-uri, marker nodes, patch planner, integrare optionala WorldEdit si generare controlata de sate, cladiri, drumuri si zone semantice.
7. Infrastructura server si date: server MCP dedicat pentru pluginul Paper, integrare MySQL, connection pooling prin HikariCP, backup, migration, audit si tooling operational.
8. AI specializat si productie: model propriu sau fine-tuned pentru domeniul AINPC, raspunsuri ultra rapide aproape real-time, hardening, rollback si release public.

Aceasta ordine nu interzice lucrul exploratoriu, dar interzice tratarea explorarii ca functionalitate stabila. Orice functie noua trebuie marcata clar ca design, initial, experimental sau production-ready.

## Articolul 5. Reguli de prioritate

Cand doua taskuri concureaza, prioritatea se decide astfel:

1. Fix pentru build/test blocant.
2. Protectie date, backup, migration sau rollback.
3. Smoke test si observabilitate pentru functionalitate existenta.
4. Functionalitate necesara primului demo intern.
5. Stabilizare API sau addonuri.
6. Generare automata, AI avansat si sisteme mari de simulare.
7. Refactor cosmetic.

Nu se accepta cresterea complexitatii daca nu exista o cale clara de testare, audit sau demonstratie in joc.

## Articolul 6. Reguli pentru AI

AI-ul este componenta asistiva a proiectului, nu autoritatea finala.

AI-ul poate:

- propune dialoguri, quest drafturi, descrieri, rezumate si planuri;
- consuma context limitat si validat;
- ajuta adminul sa inteleaga starea lumii;
- genera drafturi care trec prin validatoare.
- folosi in viitor un model specializat sau fine-tuned pentru vocabularul, regulile, questurile si contextul AINPC.

AI-ul nu poate:

- acorda reward-uri direct;
- modifica DB sau lumea fara plan validat si comanda explicita;
- decide progresul questului fara serviciu determinist;
- primi secrete in prompturi, audit entries sau debugdump;
- inlocui mapping-ul semantic validat de admin.

Directia AI pe termen lung este latenta foarte mica: raspunsuri aproape real-time pentru interactiuni de joc, fara sa blocheze thread-ul principal Paper si fara sa sacrifice validarea determinista. Fine-tuning-ul sau modelul propriu devin acceptabile doar dupa ce exista dataset curat, evaluari, fallback local/extern si masuratori de latenta.

## Articolul 7. Reguli pentru date si persistenta

Persistenta trebuie tratata ca infrastructura critica.

- Orice migration sau cleanup pe date reale cere backup verificat.
- Tabelele si fisierele care definesc NPC, households, quest state, story state si bindings trebuie sa aiba cale de inspectie si repair.
- Datele generate automat trebuie sa fie idempotente sau sa aiba mecanism de detectie a duplicatelor.
- Identificatorii de quest, stage, objective, region, place, node si anchor trebuie sa fie stabili.
- Debugdump si audit trebuie sa evite secretele si sa expuna suficient context pentru diagnostic.
- SQLite poate ramane baza simpla pentru dezvoltare si demo, dar directia de productie include MySQL cu HikariCP pentru pooling, timeouts, health checks si configuratie clara.
- Integrarea MySQL nu trebuie sa rupa migration, backup, teste locale sau compatibilitatea cu servere mici care folosesc storage local.

## Articolul 8. Reguli pentru gameplay

Gameplay-ul trebuie sa fie inteligibil pentru player si operabil pentru admin.

- NPC-urile permanente trebuie sa aiba rol, locatie, rutina si legatura cu lumea.
- Questurile trebuie sa poata fi explicate prin obiective clare, status vizibil si rezultat verificabil.
- Story state-ul trebuie sa reflecte evenimente importante, nu sa fie un jurnal generic fara efect.
- GUI-ul este strat de prezentare peste servicii validate; nu muta logica de business in inventare.
- Rutinele si simularea trebuie sa aiba fallback-uri sigure cand pathfinding-ul sau mapping-ul lipseste.
- Un feature este matur doar cand poate fi demonstrat in joc, inspectat prin comenzi/debug si acoperit minim de teste.

## Articolul 9. Reguli pentru feature-uri noi si sisteme neperturbatoare

Feature-urile noi trebuie implementate ca extensii controlate ale arhitecturii, nu ca scurtaturi care suprascriu fluxurile existente.

Reguli obligatorii:

- Orice feature nou trebuie sa declare explicit ce domeniu atinge: NPC, mapping, quest, story, progression, GUI, routine, simulation, generation, AI, storage sau addon runtime.
- Feature-ul trebuie sa aiba un flag, config, profil de addon sau mod experimental clar. Default-ul trebuie sa fie conservator daca feature-ul poate modifica lumea, DB-ul, progresul, rutina, story-ul sau interactiunea playerului.
- Feature-ul nu trebuie sa porneasca efecte automate mari doar pentru ca pluginul s-a incarcat. Startup-ul poate pregati servicii, cache-uri si validatoare, dar actiunile cu impact trebuie sa fie opt-in sau pornite de un scheduler/config explicit.
- Feature-ul nu trebuie sa blocheze thread-ul principal Paper cu AI, DB, scanari, pathfinding greu, generare sau network IO. Daca are nevoie de lucru greu, foloseste task-uri controlate, timeouts, rezumate si degradare sigura.
- Feature-ul nu trebuie sa scrie direct in domeniul altui serviciu. Quest progress se modifica prin serviciul de quest/progression, story state prin story service, mapping prin world admin/mapping service, spawn prin orchestratorul de spawn, iar DB prin repository/service responsabil.
- Feature-ul nu trebuie sa devina zgomotos: fara spam in chat, bossbar, actionbar, log sau GUI; fara notificari repetate; fara teleportari sau schimbari de stare vizibile fara motiv clar pentru player/admin.
- Feature-ul trebuie sa fie inspectabil: status, audit, debugdump, log sumar sau test. Daca adminul nu poate vedea ce s-a intamplat si de ce, feature-ul nu este matur.
- Feature-ul trebuie sa fie idempotent sau sa aiba protectii impotriva duplicarii, mai ales pentru spawn, repair, migration, mapping si generare.
- Feature-ul trebuie sa degradeze curat cand lipsesc date: mapping absent, addon dezactivat, AI indisponibil, DB in eroare, player offline sau NPC despawnat. Degradarea corecta este no-op, warning auditabil sau rezultat read-only, nu exceptie vizibila sau stare corupta.
- Feature-ul trebuie sa respecte core-ul neutru: vocabularul, profesiile, lore-ul, economia, rewards speciale si regulile de scenariu apartin addonurilor/configului, nu fallback-ului hardcodat.

Regula de integrare:

Un feature nou intra intai ca `preview`, `dryrun`, `read-only`, `experimental` sau `disabled by default` daca poate afecta lumea, datele sau experienta playerului. Devine comportament normal doar dupa test, audit/debug si documentatie.

### Rolul `SimulationService`

`SimulationService` este orchestratorul starii vii a NPC-urilor intre interactiunile directe cu playerul. Rolul lui este sa ruleze tick-ul de simulare, sa adune snapshot-uri, sa aplice decay/recovery pentru nevoi si sa expuna rezumate auditabile.

`SimulationService` poate:

- coordona tick-uri pentru NPC-uri active;
- citi context local validat: timp, vreme, locatie, pericol, ancore, apropierea playerilor si snapshot semantic;
- cere `DecisionEngine` sa calculeze actiunea probabila;
- actualiza nevoi, stare curenta, scop curent si semnale interne ale NPC-ului;
- produce `SimulationTickSummary`, `SimulationNpcSnapshot` si warning-uri pentru audit/debugdump;
- oferi preview read-only pentru comenzi admin sau GUI.

`SimulationService` nu poate:

- porni questuri automat;
- scrie direct story state;
- acorda reward-uri;
- genera cladiri, NPC-uri sau mapping;
- decide progresul questurilor;
- muta NPC-ul pe harta in locul `RoutineService`;
- folosi AI ca autoritate pentru stare sau progres.

Relatia corecta intre servicii:

| Serviciu | Responsabilitate |
|---|---|
| `SimulationService` | Intentie, nevoi, stare si rezumat de tick |
| `DecisionEngine` | Scoruri si alegerea actiunii probabile |
| `RoutineService` | Locatia/tinta zilnica si deplasarea controlata intre ancore |
| `ScenarioEngine` / progression | Progres quest, obiective, accept/complete/track |
| `StoryStateService` | Stari narative si evenimente story persistente |
| `WorldAdminService` / mapping | Regiuni, places, nodes, anchors si semantic world data |

Simularea poate produce semnale pentru story, quest sau AI, dar semnalele sunt input-uri validate, nu comenzi. Orice efect jucabil pornit dintr-un semnal de simulare trebuie sa treaca prin serviciul responsabil si prin config/feature flag.

## Articolul 10. Reguli pentru API si addonuri

API-ul public trebuie sa fie mic, stabil si documentat.

- Tipurile expuse din `ainpc-api` trebuie sa fie Java-friendly pana cand exista motiv explicit pentru contracte Kotlin-only.
- Addonurile trebuie sa livreze propriul config template si propriile pack-uri de continut.
- Core-ul ramane universal; scenariile si temele apartin addonurilor.
- Core-ul ofera infrastructura: lifecycle, config, registri, persistenta, validare, audit, debug, comenzi, GUI generic si contracte. Addonurile ofera continutul: tema, poveste, questuri, resurse, texturi, reguli speciale si integrare datapack.
- Continutul demo din core trebuie tratat ca exemplu de pornire, nu ca tema implicita permanenta. Configul trebuie sa permita `demo.enabled=false` sau un mecanism echivalent.
- Breaking changes in API cer documentare, test de compatibilitate si motiv clar.
- Un addon demonstrativ nu trebuie sa introduca dependinte obligatorii pentru core.
- Configuratia trebuie gandita pe profiluri: core defaults, server profile, addon profile si scenario pack. Valorile implicite trebuie sa fie sigure si usor de explicat.
- Tipurile de addon planificate sunt: scenariu, story, resursa/textura si compatibilitate datapack. Tipurile pot fi combinate doar daca manifestul declara clar ce activeaza.
- Addonurile de scenariu definesc continut si reguli jucabile. Addonurile de story definesc naratiune si stari. Addonurile de resursa/textura livreaza prezentare vizuala si audio. Compatibilitatea datapack trebuie sa ramana vanilla-friendly si optionala.
- Fiecare addon trebuie sa poata fi dezactivat fara sa corupa datele existente; la dezactivare, core-ul trebuie sa raporteze clar ce questuri, story hooks, resurse sau datapack hooks lipsesc.

## Articolul 11. Reguli pentru documentatie

Documentatia are trei nivele:

1. Constitutional: acest document stabileste principiile si directia.
2. Canonica: documentele din radacina `docs/` definesc contracte, faze, runbook-uri si status.
3. Navigationala: `docs/categorii/` grupeaza documentele fara sa le inlocuiasca.

La schimbari importante, se actualizeaza cel putin:

- `docs/implementat-deja.md`, daca statusul real s-a schimbat;
- documentul de specialitate afectat;
- `docs/README.md`, daca apare document nou sau se schimba ordinea de citire;
- `TODO.md`, daca se modifica prioritatea curenta.

Documentele vechi se muta in `docs/arhiva/` doar cand exista inlocuitor canonic clar.

## Articolul 12. Reguli de lucru

Schimbarile trebuie livrate in slice-uri mici, verificabile.

Un slice bun are:

- scop clar;
- fisiere afectate putine;
- test sau comanda de verificare;
- efect vizibil in joc, in API sau in observabilitate;
- documentatie actualizata cand comportamentul public se schimba.

Un slice riscant este unul care combina refactor, migration, gameplay nou si schimbari de API. Acestea trebuie separate.

## Articolul 13. Definitia maturitatii

O functionalitate este considerata:

| Nivel | Definitie |
|---|---|
| Design | Exista in documentatie, fara cod functional garantat |
| Initial | Exista cod si teste locale de baza, dar integrarea in joc poate fi incompleta |
| Demo-ready | Poate fi demonstrata pe server Paper cu comenzi/GUI si date controlate |
| Stable | Are teste relevante, audit/debug, fallback-uri si documentatie actualizata |
| Production-ready | Are backup/rollback, migration verificat, smoke real si comportament operational repetabil |

Nicio functie nu trece la un nivel superior doar prin intentie. Trecerea se face prin dovada: test, smoke, audit, debugdump, documentatie sau demonstratie.

## Articolul 14. Surse de adevar

Ordinea surselor de adevar este:

1. Codul si testele care ruleaza.
2. `docs/implementat-deja.md` pentru status confirmat.
3. Acest document pentru reguli si directie.
4. Documentele canonice din `docs/` pentru contracte specializate.
5. `TODO.md` pentru lista operationala curenta.
6. Documentele arhivate si materialele brute doar ca istoric sau inspiratie.

Cand documentatia si codul se contrazic, nu se presupune automat ca documentatia este corecta. Se verifica implementarea, se decide statusul real si se actualizeaza documentele.

## Articolul 15. Linie rosie

Proiectul nu trebuie sa devina:

- un set de sisteme mari fara demo jucabil;
- un plugin dependent de AI pentru reguli critice;
- o colectie de documente care nu reflecta codul;
- un core rigid care nu poate sustine addonuri;
- un plugin greu de configurat sau imposibil de adaptat pe servere diferite;
- un generator care modifica lumea fara validare;
- un sistem de generare harti/structuri care ignora mapping-ul semantic si controlul adminului;
- un server greu de reparat dupa spawn gresit, migration gresit sau config gresit.
- un set de feature-uri care se activeaza singure, spameaza playerul/adminul, muta NPC-uri fara explicatie sau modifica quest/story/storage fara serviciul responsabil.

Directia corecta este un nucleu mic, verificabil si extensibil: sat clar, NPC-uri persistente, questuri functionale, context narativ, API stabil, configuratie flexibila, infrastructura MCP/MySQL pregatita si automatizare controlata.
