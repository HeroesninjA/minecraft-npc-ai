# Constitutie Proiect AINPC

Actualizat: 2026-05-27

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

## Articolul 3. Structura canonica

Structura principala a proiectului este:

| Zona | Rol constitutional |
|---|---|
| `ainpc-api` | Contract public pentru addonuri, platform API, world info, addon registry si tipuri stabile expuse in afara core-ului |
| `ainpc-core-plugin` | Implementarea principala Paper: comenzi, NPC, quest, story, progression, mapping, spawn, GUI, AI orchestration, debug si audit |
| `ainpc-scenario-medieval` | Addon/scenariu exemplar pentru continut medieval si pachete de quest configurabile |
| `docs` | Sursa canonica pentru design, roadmap, runbook-uri si reguli de dezvoltare |
| `scripts` | Automatizari locale pentru context MCP, release, smoke, backup si verificari operationale |
| `data`, `.ai`, `.codex` | Context local, memorie MCP/Codex si artefacte operationale; nu sunt loc pentru secrete sau specificatii canonice de gameplay |
| Server MCP pentru Paper | Directie viitoare pentru integrare controlata intre plugin, context, instrumente de administrare, debug, AI si automatizari externe |

Regula de proprietate:

- `ainpc-api` nu depinde de `ainpc-core-plugin`.
- `ainpc-core-plugin` poate consuma `ainpc-api`, dar nu trebuie sa forteze addonurile sa cunoasca internals.
- `ainpc-scenario-medieval` demonstreaza extensibilitatea, nu stabileste reguli globale pentru toate scenariile.
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

## Articolul 9. Reguli pentru API si addonuri

API-ul public trebuie sa fie mic, stabil si documentat.

- Tipurile expuse din `ainpc-api` trebuie sa fie Java-friendly pana cand exista motiv explicit pentru contracte Kotlin-only.
- Addonurile trebuie sa livreze propriul config template si propriile pack-uri de continut.
- Core-ul ramane universal; scenariile si temele apartin addonurilor.
- Breaking changes in API cer documentare, test de compatibilitate si motiv clar.
- Un addon demonstrativ nu trebuie sa introduca dependinte obligatorii pentru core.
- Configuratia trebuie gandita pe profiluri: core defaults, server profile, addon profile si scenario pack. Valorile implicite trebuie sa fie sigure si usor de explicat.

## Articolul 10. Reguli pentru documentatie

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

## Articolul 11. Reguli de lucru

Schimbarile trebuie livrate in slice-uri mici, verificabile.

Un slice bun are:

- scop clar;
- fisiere afectate putine;
- test sau comanda de verificare;
- efect vizibil in joc, in API sau in observabilitate;
- documentatie actualizata cand comportamentul public se schimba.

Un slice riscant este unul care combina refactor, migration, gameplay nou si schimbari de API. Acestea trebuie separate.

## Articolul 12. Definitia maturitatii

O functionalitate este considerata:

| Nivel | Definitie |
|---|---|
| Design | Exista in documentatie, fara cod functional garantat |
| Initial | Exista cod si teste locale de baza, dar integrarea in joc poate fi incompleta |
| Demo-ready | Poate fi demonstrata pe server Paper cu comenzi/GUI si date controlate |
| Stable | Are teste relevante, audit/debug, fallback-uri si documentatie actualizata |
| Production-ready | Are backup/rollback, migration verificat, smoke real si comportament operational repetabil |

Nicio functie nu trece la un nivel superior doar prin intentie. Trecerea se face prin dovada: test, smoke, audit, debugdump, documentatie sau demonstratie.

## Articolul 13. Surse de adevar

Ordinea surselor de adevar este:

1. Codul si testele care ruleaza.
2. `docs/implementat-deja.md` pentru status confirmat.
3. Acest document pentru reguli si directie.
4. Documentele canonice din `docs/` pentru contracte specializate.
5. `TODO.md` pentru lista operationala curenta.
6. Documentele arhivate si materialele brute doar ca istoric sau inspiratie.

Cand documentatia si codul se contrazic, nu se presupune automat ca documentatia este corecta. Se verifica implementarea, se decide statusul real si se actualizeaza documentele.

## Articolul 14. Linie rosie

Proiectul nu trebuie sa devina:

- un set de sisteme mari fara demo jucabil;
- un plugin dependent de AI pentru reguli critice;
- o colectie de documente care nu reflecta codul;
- un core rigid care nu poate sustine addonuri;
- un plugin greu de configurat sau imposibil de adaptat pe servere diferite;
- un generator care modifica lumea fara validare;
- un sistem de generare harti/structuri care ignora mapping-ul semantic si controlul adminului;
- un server greu de reparat dupa spawn gresit, migration gresit sau config gresit.

Directia corecta este un nucleu mic, verificabil si extensibil: sat clar, NPC-uri persistente, questuri functionale, context narativ, API stabil, configuratie flexibila, infrastructura MCP/MySQL pregatita si automatizare controlata.
