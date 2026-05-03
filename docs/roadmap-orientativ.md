# Organizare Interna: Componente, Mecanici si Ordine de Dezvoltare

Actualizat: 2026-05-03

## Rolul documentului

Acest fisier inlocuieste vechiul roadmap orientativ ca document intern de organizare.

Scopul lui este sa raspunda clar la trei intrebari:

1. ce componente tehnice exista si cine poarta responsabilitatea fiecareia
2. ce mecanici si engine-uri trebuie dezvoltate mai departe
3. in ce ordine se lucreaza, ca proiectul sa ajunga la un prim release jucabil fara sa sara prematur la sisteme prea mari

Nu este un document de marketing si nu este o promisiune de versiuni publice. Este harta interna dupa care se alege ce se implementeaza, ce se stabilizeaza si ce se amana.

## Surse de adevar

Pentru a evita confuzia intre design si cod existent:

- `docs/implementat-deja.md` este sursa principala pentru ce este confirmat in cod.
- `docs/faze-observatii-avertizari.md` este harta de control pe faze, riscuri si dependinte.
- `TODO.md` este lista curenta de lucru, nu specificatie completa.
- `docs/debugging-si-testare.md` este sursa pentru verificari, smoke tests si debug.

Regula interna:

- daca o sarcina nu poate fi legata de o componenta, o mecanica si o faza de mai jos, nu intra in lucru pana nu este clarificata.

## Principiul de dezvoltare

Ordinea sanatoasa pentru proiect este:

1. baza stabila si diagnosticabila
2. lume semantica reala: `WorldRegion -> WorldPlace -> WorldNode`
3. spawn si alocare NPC peste acea lume
4. rutina si interactiune NPC peste ancore stabile
5. questuri mici, completabile, persistente
6. story context si story state controlat
7. modularizare si API public curate
8. runtime de scenarii extensibil
9. addonuri tematice separate
10. authoring asistat de AI peste validatoare, nu executie directa din prompt

Fiecare faza trebuie sa lase in urma un rezultat testabil in joc, nu doar infrastructura.

## Harta interna a componentelor

| Componenta | Zona / modul | Rol intern | Focus urmator |
|---|---|---|---|
| Platforma core | `ainpc-core-plugin` | Pornire plugin, config, servicii, comenzi, listenere, schedulere | Stabilitate, reload sigur, mesaje clare la startup |
| API public | `ainpc-api` | Contracte pentru addonuri, runtime mode, world mode si world admin | Stabilizarea contractelor care nu expun internals |
| Addon medieval | `ainpc-scenario-medieval` | Continut demo separat si sincronizare pack medieval | Transformare in scenariu jucabil cap-coada |
| Persistenta | SQLite prin managerii core | Salveaza NPC-uri, memorii, relatii, questuri, story state si ancore | Audit, export/debugdump si migrari controlate |
| World admin | regiuni, places, nodes | Model semantic al lumii folosit de NPC, quest si story | Mapping demo real si validare de consistenta |
| Scanner / mapper semantic | scanare sat vanilla si import | Detecteaza paturi, clopote, workstation-uri, usi, farmland si creeaza mapping semantic initial | Import mai complet si corectii manuale clare |
| Spawn si household | `HouseAllocation`, `NpcSpawnPlan`, orchestrare spawn | Leaga case, rezidenti, familie, ancore si spawn batch | Populare mai narativa si tranzactii DB complete |
| NPC lifecycle | manageri NPC, villager sync, profiluri | Creeaza, restaureaza si sincronizeaza NPC-uri AI | Generator automat si persistenta dedicata peste bind-ul initial home/work/social |
| Rutina si simulare | `RoutineEngine`, `RoutineService`, nevoi NPC | Muta NPC-urile intre ancore si ruleaza simulare de baza | Pasi intermediari si evenimente sociale controlate |
| Dialog si AI | dialog, context NPC, OpenAI, fallback | Conversatie contextualizata cu memorie, emotii si story context | Reactii mai clare la quest, reputatie locala si istoric |
| Quest runtime | `ScenarioEngine`, quest progress, obiective | Ofera, urmareste si finalizeaza questuri persistente | Questuri in lant si separare progresiva a runtime-ului |
| Quest anchors | `QuestAnchorResolver`, `quest_anchor_bindings` | Leaga obiectivele de regiuni, places, nodes si NPC-uri | Export/debugdump complet si validare mai explicita |
| Story runtime | `StoryContextService`, `StoryStateService` | Snapshot read-only pentru prompt si persistenta story state/events | Audit/debugdump dedicat si reguli clare de scriere |
| Feature packs | `FeaturePackLoader`, pack-uri YAML | Incarca traits, professions, dialogues, topologies si scenarios | Validare de schema, dependencies si capabilities |
| Addon registry | `AddonRegistryApi`, `AINPCAddon` | Inregistreaza addonuri si continut separat | Compatibilitate, versionare si erori clare la load |
| Operare | `/ainpc audit`, `/ainpc debugdump`, teste | Verifica starea runtime si strange context de investigatie | Acoperire mai buna pentru story, quest anchors si scenarii |

## Axe de executie

### A0. Stabilitate, audit si debug

Scop:

- fiecare schimbare importanta sa poata fi verificata prin teste, comenzi admin sau debug dump
- datele partiale sa fie vizibile, nu ascunse

Se ataca acum:

- audit/debugdump dedicat pentru story state si story events
- export/debugdump complet pentru `quest_anchor_bindings`
- mesaje clare pentru config, pack-uri si compatibilitati invalide
- smoke tests pentru fluxurile critice

Se amana:

- tooling complex de productie fara un prim scenariu jucabil
- refactorizari mari care nu cresc verificabilitatea

### A1. Lume semantica si mapping

Scop:

- `WorldRegion`, `WorldPlace` si `WorldNode` sa devina baza comuna pentru spawn, rutina, quest si story

Se ataca acum:

- mapping demo real: regiune, case, locuri de munca, piata si node-uri
- bind initial NPC -> home/work/social places pentru demo si harti manuale
- import semantic mai util din satele vanilla
- validare intre regiuni, places, nodes si NPC-uri
- documentare scurta pentru admin: cum creezi/importi mapping-ul minim

Se amana:

- worldgen complet de la zero
- dependinta obligatorie de WorldEdit
- regenerarea agresiva a satelor existente

### A2. Spawn order, case si familii

Scop:

- NPC-urile sa fie create numai dupa ce exista context semantic suficient: casa, node-uri, alocare si relatii

Flux intern recomandat:

1. scanare sau definire manuala lume
2. `WorldRegion`
3. `WorldPlace` pentru case si locuri de munca
4. `WorldNode` pentru pat, intrare, workstation si punct social
5. `HouseAllocation`
6. `NpcSpawnPlan`
7. spawn batch
8. salvare DB
9. family bind
10. audit spawn order

Se ataca acum:

- generator real care produce automat `HouseAllocation` din mapping semantic
- plannerul initial pentru o casa: `world household plan/spawn`
- plannerul initial pentru regiune: `world settlement plan/spawn`
- rollback global practic pentru `settlement spawn`
- dry-run mai clar pentru planuri de spawn
- persistenta dedicata pentru legatura NPC-place, peste bind-ul initial bazat pe `profile_data` si metadata

Se amana:

- rollback tranzactional complet pana cand fluxul MVP este stabil
- spawn masiv fara plan serializabil si validabil

### A3. NPC lifecycle, rutina si simulare

Scop:

- NPC-ul sa para parte din lume, nu doar entitate de dialog

Se ataca acum:

- rutina zilnica peste `home/work/social anchors`
- reactii la stare, familie, emotii si context local
- evenimente sociale probabilistice simple
- evitarea intreruperii conversatiilor si questurilor active

Se amana:

- pathfinding natural complet
- NPC-uri non-villager ca sistem general
- simulare economica ampla

### A4. Dialog, memorie si reactie NPC-jucator

Scop:

- dialogul sa foloseasca memoria, relatia, emotiile, questurile si story context-ul fara sa devina dependent de AI pentru logica de joc

Se ataca acum:

- raspunsuri mai clare cand NPC-ul are quest activ sau story state relevant
- fallback-uri deterministe pentru intentii de quest
- context prompt limitat si verificabil
- debug pentru prompt, model si fallback

Se amana:

- AI care decide direct progresul questului
- generare libera de actiuni runtime

### A5. Questuri si story

Scop:

- questurile sa fie mici, clare, persistente si conectate la locuri semantice

Se ataca acum:

- 3-5 questuri medievale completabile cap-coada
- obiective `visit_place` si `inspect_node` folosite real in demo
- quest anchors persistente auditate si exportabile
- story state si story events scrise numai prin actiuni validate
- briefing, progres, recompensa si finalizare consistente

Se amana:

- branching avansat
- multi-quest runtime matur pe jucator
- reputatie globala pe factiuni

### A6. Modularizare, API si addonuri

Scop:

- core-ul ramane platforma, iar scenariile si integrarile stau in addonuri separate

Se ataca dupa primul slice jucabil:

- clarificarea contractelor `ainpc-api`
- documentatie API pentru developer extern
- capabilitati si dependinte validate la load
- reload de continut mai sigur
- addon medieval ca exemplu curat, nu hack peste internals

Se amana:

- ecosistem mare de addonuri inainte ca un addon real sa fie stabil
- expunerea claselor interne doar pentru comoditate

### A7. Runtime de scenarii extensibil

Scop:

- scenariile sa fie compuse din actiuni, conditii, trigger-e si tranzitii validate

Se ataca dupa stabilizarea questurilor MVP:

- `ScenarioActionRegistry`
- `ScenarioConditionRegistry`
- `ScenarioTriggerRegistry`
- `ScenarioExecutionContext`
- `ScenarioVariableProvider`
- `ScenarioValidationReport`
- parser si validator separat pentru YAML

Regula:

- `ScenarioEngine` nu trebuie sa devina si mai mare; fiecare extensie noua trebuie sa impinga spre registri si validare.

Se amana:

- scripting general inainte de contracte clare
- executie de scenarii generate fara validare

### A8. Generare si authoring asistat de AI

Scop:

- AI-ul ajuta la authoring, dar pluginul valideaza si executa determinist

Model acceptat:

1. prompt
2. research/context
3. alegere template
4. draft YAML sau plan
5. validare schema/capabilities/anchors
6. import controlat in `packs/`

Se ataca tarziu:

- biblioteca de template-uri
- validator matur
- rapoarte de validare
- generare de drafturi, nu executie directa

Se amana explicit:

- `prompt -> executie directa`
- AI care construieste, spawneaza sau modifica DB fara dry-run

## Mecanici prioritare

| Mecanica | Motor / componenta | Depinde de | Status intern | Urmatorul pas |
|---|---|---|---|---|
| Instalare plugin si reload | Platforma core | config, DB, pack loader | functional initial | documentatie scurta si smoke test |
| Scanare sat vanilla | scanner world | lume incarcata | initial | import mai complet si raport de rezultate |
| Mapping semantic manual/importat | world admin | regiuni, places, nodes | functional initial | demo map populat si validat |
| Alocare casa si rezidenti | `HouseAllocation` | places/nodes valide | initial | generator din mapping semantic |
| Spawn batch household | spawn orchestrator | plan validat | initial | dry-run/rollback mai clar |
| Legare familie | `FamilyManager` | NPC-uri cu ID DB valid | initial | audit si cazuri de test mai clare |
| Rutina zilnica | routine engine/service | ancore home/work/social | initial | pasi intermediari si evenimente sociale |
| Dialog contextual | dialog/AI services | NPCContext, memorie, relatie | functional initial | debug prompt si fallback pe intentii |
| Quest simplu | `ScenarioEngine` | pack YAML, DB player quests | functional initial | continut medieval cap-coada |
| Quest pe locatie | quest anchors | mapping semantic | initial | folosire reala in demo si debugdump |
| Story context | `StoryContextService` | mapping, quest anchors, story state | initial | audit/debugdump dedicat |
| Story state persistent | `StoryStateService` | DB si actiuni quest validate | initial | reguli clare pentru scrieri si inspectie |
| Feature packs | `FeaturePackLoader` | YAML valid | functional initial | validator de schema/capabilities |
| Addon content | addon registry | API public | initial | stabilizare contract medieval addon |
| Runtime extensibil | registri scenarii | quest MVP stabil | lipseste complet | proiectare si introducere incrementala |
| Authoring AI | tool extern/template-uri | validatoare mature | viitor | nu se ataca inainte de A7 |

## Engine-uri de protejat

| Engine / serviciu | Responsabilitate | Regula interna |
|---|---|---|
| Platform startup | initializeaza config, DB, servicii, comenzi si schedulere | nu se incarca logica de gameplay direct in startup |
| Database layer | persistenta pentru NPC, quest, story, relatii si ancore | migrarile trebuie documentate si testate |
| World admin | sursa semantica pentru regiuni, places si nodes | coordonatele brute sunt fallback, nu contract de gameplay |
| Spawn orchestration | transforma alocari valide in NPC-uri reale | spawn fara plan validabil este interzis |
| Routine engine | misca NPC-urile intre ancore | nu trebuie sa intrerupa interactiuni critice |
| Dialog/AI layer | produce raspunsuri conversaionale | nu decide starea autoritara a questului |
| Scenario/quest engine | executa questuri si scenarii | trebuie spart treptat in registri, nu extins monolitic |
| Quest anchor resolver | leaga obiectivele de mapping semantic | binding-urile trebuie auditate si exportabile |
| Story context service | produce snapshot read-only | nu scrie in DB si nu genereaza questuri |
| Story state service | scrie/citeste story state si events | scrierile vin prin actiuni validate |
| Feature pack loader | incarca continut YAML | trebuie sa raporteze clar schema/dependinte/capabilitati invalide |
| Addon registry | conecteaza addonuri externe | nu trebuie sa expuna internals instabile |

## Ordinea de dezvoltare

### P0. Baseline verificabil

Obiectiv:

- build-ul si documentatia sa reflecte starea reala.

Livrabile:

- `mvn test` trece
- `implementat-deja.md`, `TODO.md` si documentul afectat sunt actualizate dupa schimbari majore
- comenzile de audit/debug functioneaza pentru fluxurile atinse

### P1. Mapping demo minim

Obiectiv:

- sa existe o lume semantica minima folosibila in test.

Livrabile:

- o regiune demo
- cateva case
- locuri de munca
- punct social/piata
- node-uri pentru pat, intrare, workstation si inspectie
- audit world fara erori majore

### P2. Spawn order complet pentru household

Obiectiv:

- NPC-urile demo sa fie create din plan, nu prin pasi manuali fragili.

Livrabile:

- `HouseAllocation` generat sau completat din mapping
- `NpcSpawnPlan` validabil
- dry-run de spawn
- spawn batch cu familie si ancore
- audit spawn order dupa creare

### P3. Rutina NPC si interactiune stabila

Obiectiv:

- NPC-urile demo sa aiba comportament zilnic minim si dialog consistent.

Livrabile:

- rutina home/work/social activa
- fallback cand lipsesc ancore
- interactiuni care nu rup rutina
- debug pentru dialog si prompt

### P4. First playable medieval slice

Obiectiv:

- un admin poate instala pluginul si juca un scenariu medieval mic de la inceput la sfarsit.

Livrabile:

- 5-10 NPC-uri cu roluri clare
- 3-5 questuri completabile
- obiective pe conversatie, iteme, regiuni, places si nodes
- recompense clare
- persistenta stabila la reload
- documentatie scurta de instalare si testare

### P5. Story si observabilitate

Obiectiv:

- story context-ul si story state-ul sa fie utile, verificabile si controlate.

Livrabile:

- audit/debugdump pentru story state si events
- export complet pentru `quest_anchor_bindings`
- rapoarte clare cand un quest nu isi poate rezolva ancora
- story actions validate in quest rewards

### P6. Stabilizare modulara

Obiectiv:

- addonurile sa consume `ainpc-api`, nu clase interne instabile.

Livrabile:

- contracte API documentate
- capabilities/dependencies verificate
- addon medieval curat
- reload de continut mai sigur

### P7. Runtime extensibil pentru scenarii

Obiectiv:

- scenariile sa fie compuse din piese validate si extensibile.

Livrabile:

- registri pentru actiuni, conditii si trigger-e
- context de executie separat
- validator de scenarii
- raport de validare pentru pack-uri

### P8. Ecosistem de addonuri

Obiectiv:

- continutul tematic sa poata fi livrat din jar-uri separate.

Livrabile:

- template addon oficial
- 2-3 addonuri sau pack-uri reale
- reguli de versionare
- mesaje clare pentru dependinte lipsa

### P9. Authoring asistat de AI

Obiectiv:

- AI-ul produce drafturi validate, nu executie directa.

Livrabile:

- template-uri de scenarii
- validator schema/capabilities/anchors
- raport de validare
- import controlat in `packs/`

## Prioritatea imediata

Urmatoarea ordine de lucru recomandata este:

1. audit/debugdump pentru story state si story events
2. export/debugdump complet pentru `quest_anchor_bindings`
3. mapping demo real cu regiune, case, locuri de munca, piata si nodes
4. generator sau completare automata `HouseAllocation` din mapping semantic
5. dry-run si audit mai clar pentru spawn household
6. 3-5 questuri medievale cap-coada care folosesc NPC-uri, places si nodes
7. fallback-uri deterministe pentru intentii de quest in dialog
8. documentatie scurta pentru instalare, testare si reset demo
9. stabilizare API/addon dupa ce slice-ul medieval este jucabil
10. registri de scenarii numai dupa ce questurile MVP sunt stabile

## Dependinte critice

Respecta aceste dependinte cand alegi taskurile:

| Nu incepe | Pana cand nu exista |
|---|---|
| spawn automat mare | mapping semantic minim si dry-run |
| rutina complexa | ancore home/work/social stabile |
| questuri pe locatie | `quest_anchor_bindings` verificabile |
| story AI mai bogat | story context/state auditabil |
| addon ecosystem | `ainpc-api` stabilizat |
| scenarii programabile | registri si validator |
| authoring AI | template-uri si validatoare mature |
| economie/reputatie globala | first playable release stabil |

## Ce nu se ataca acum

Pentru primul release jucabil, nu prioritiza:

- generare directa prin prompt
- economie completa
- reputatie globala pe factiuni sau regiuni
- sistem RPG complet
- NPC-uri non-villager ca platforma generala
- WorldEdit ca dependinta obligatorie
- branching complex inainte ca questurile simple sa fie stabile
- scripting general fara validator
- refactorizari mari care nu produc un flux testabil

## Definition of Done intern

O sarcina este considerata terminata numai daca:

- build-ul trece sau este mentionat clar de ce nu a fost rulat
- exista test automat, smoke test sau comanda admin pentru verificare
- configuratia noua are default sigur
- datele persistente noi au strategie de migration/backfill sau explicatie de compatibilitate
- audit/debugdump poate ajuta la investigarea erorilor principale
- documentatia afectata este actualizata
- nu a crescut coupling-ul intre addonuri si internals core

## Regula de decizie

Cand exista dubiu intre doua directii, se alege directia care:

1. apropie proiectul de un scenariu medieval jucabil
2. reduce riscul de date rupte sau greu de investigat
3. foloseste mapping semantic in loc de coordonate brute
4. lasa o componenta mai clara, nu mai monolitica
5. poate fi verificata prin test, audit, debugdump sau smoke test in joc

Concluzia interna:

- proiectul nu trebuie impins direct spre AI sau ecosistem mare
- ordinea corecta este lume semantica, spawn, rutina, quest, story, modularizare, runtime extensibil, apoi authoring AI
- primul obiectiv real ramane un demo medieval mic, stabil si jucabil cap-coada
