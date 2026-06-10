# Prim Demo: Functionalitate Minima Diversa

Actualizat: 2026-05-26

## Scop

Acest document transforma documentatia existenta intr-un plan executabil pentru primul demo intern jucabil AINPC.

Prin "functionalitate minima diversa" intelegem un demo mic, dar care arata cate o felie verificabila din subsistemele
principale:

- server Paper pornit cu core si addon medieval;
- mapping semantic pentru un sat demo;
- NPC-uri persistente cu home/work/social si rutina inspectabila;
- interactiune clara cu NPC-ul prin click, chat si GUI/comenzi;
- dialog cu fallback sigur cand AI-ul extern nu raspunde;
- minim un quest clasic si minim o mecanica non-quest prin progression generic;
- story context, story event sau story state inspectabil;
- audit, debugdump, backup si restart fara pierdere de stare.

Demo-ul nu este release public. Este un milestone intern/testabil care arata ca sistemele se leaga cap-coada.

## Surse Analizate

Documente folosite ca baza:

- `implementat-deja.md`
- `faze-observatii-avertizari.md`
- `roadmap-orientativ.md`
- `server-npc-mvp-si-faze.md`
- `playable-village-ux.md`
- `debugging-si-testare.md`
- `release-checklist.md`
- `TODO.md`

Regula de interpretare:

- `implementat-deja.md` si `TODO.md` decid statusul real;
- acest document decide ordinea demo-ului;
- documentele de specialitate raman sursa pentru detalii tehnice.

## Definitia De Gata

Primul demo este gata doar daca toate criteriile de mai jos trec pe un server Paper dedicat:

| Zona               | Prag minim                                                                                                      |
|--------------------|-----------------------------------------------------------------------------------------------------------------|
| Build              | `gradlew clean test` si `gradlew assemble` trec sau exista exceptie documentata                                 |
| Instalare          | JAR-ul core si addonul medieval sunt in `plugins/` si apar ca enabled                                           |
| Config             | `plugins/AINPC/config.yml`, `quests.yml` si `ainpc_data.db` exista                                              |
| Mapping            | `demo_sat` are regiune, case, locuri de munca, social hub si node-uri inspectabile                              |
| NPC                | 3-5 NPC-uri exista, apar in `/ainpc list` si au bindings inspectabile                                           |
| Rutina             | `/ainpc routine status nearest` explica locul curent sau urmatorul slot                                         |
| Interactiune       | click dreapta si/sau GUI-ul arata o actiune urmatoare clara                                                     |
| Dialog             | NPC-ul raspunde sau cade pe fallback fara sa opreasca serverul                                                  |
| Quest              | un quest poate fi vazut, acceptat, progresat si inspectat                                                       |
| Progression divers | cel putin o mecanica non-quest este listata si inspectabila: contract, duty, bounty, event, tutorial sau ritual |
| Story              | `story context`, `story events` sau story state arata efectul demo-ului                                         |
| Persistenta        | dupa restart, mapping-ul, NPC-urile, bindings si progresul minim raman                                          |
| Definitie demo     | `/ainpc demo definition` explica in joc ce inseamna demo functional                                             |
| Readiness          | `/ainpc demo status demo_sat` raporteaza PASS/WARN/FAIL si pasi urmatori                                        |
| Urmatorul pas      | `/ainpc demo next demo_sat` listeaza blocajele, warning-urile si comenzile urmatoare                            |
| Faze demo          | `/ainpc demo phases demo_sat <player>` listeaza D0-D9 cu gate-uri si comenzi                                    |
| Script demo        | `/ainpc demo script demo_sat <player>` listeaza fluxul manual fara sa modifice date                             |
| Dovezi demo        | `/ainpc demo evidence demo_sat <player>` listeaza output-urile de capturat pentru milestone                     |
| Runbook demo       | `/ainpc demo runbook demo_sat <player>` rezuma operarea demo intr-un singur ghid compact                        |
| Smoke demo         | `/ainpc demo smoke demo_sat <player>` ruleaza checklist-ul minim de verificare rapida                           |
| Summary demo       | `/ainpc demo summary demo_sat <player>` da un rezumat rapid cu urmatorii pasi                                   |
| Comenzi demo       | `/ainpc demo commands demo_sat <player>` listeaza fluxul compact de comenzi pentru rulare manuala               |
| Restart gate       | `/ainpc demo restart demo_sat` listeaza verificarile obligatorii inainte si dupa restart                         |
| Experimental       | `/ainpc demo experimental* demo_sat <player>` listeaza pachete instabile de analiza interna                     |
| Operare            | `/ainpc audit all` si `/ainpc debugdump all` ruleaza fara secrete expuse                                        |

## Non-Obiective Pentru Primul Demo

Nu bloca demo-ul pe:

- generator fizic complet de sate;
- WorldEdit obligatoriu;
- economie completa;
- reputatie globala;
- branching complex de questuri;
- runtime universal perfect pentru toate scenariile;
- GUI final pentru toate fluxurile;
- sute de NPC-uri;
- publicare pe server real cu date importante.

## Harta Functionala Minima

| Functionalitate     | Ce se arata in demo                        | Dovada minima                                                          |
|---------------------|--------------------------------------------|------------------------------------------------------------------------|
| World mapping       | un sat semantic numit `demo_sat`           | `/ainpc world places demo_sat`                                         |
| Spawn/NPC lifecycle | 3-5 NPC-uri legate de locuri               | `/ainpc list`, `/ainpc world bindings nearest`                         |
| Rutina              | NPC-ul are slot home/work/social           | `/ainpc routine status nearest`                                        |
| Interactiune        | playerul intelege ce poate face cu NPC-ul  | click dreapta, GUI, `quest nearest`                                    |
| Dialog              | conversatie stabila cu fallback            | click + mesaj scurt + log fara secret                                  |
| Quest clasic        | obiectiv peste place/node                  | `/ainpc quest nearest`, `/ainpc quest status nearest`                  |
| Progression generic | contract/duty/bounty/event/tutorial/ritual | `/ainpc progression definitions`, `/ainpc progression stored <player>` |
| Story               | lumea retine un eveniment sau context      | `/ainpc story context`, `/ainpc story events`                          |
| Audit/debug         | adminul poate investiga fara DB live edit  | `/ainpc audit all`, `/ainpc debugdump all`                             |
| Restart             | starea supravietuieste                     | audit repetat dupa restart                                             |

## Faza D0: Inghetare Scope Si Baseline

Scop: demo-ul are un target clar si nu devine o lista infinita de feature-uri.

Decizii de luat inainte de lucru:

- server tinta: Paper local separat;
- lume tinta: lume de test, nu lume reala;
- region id: `demo_sat`;
- numar NPC: 3-5;
- mecanici minime: un quest, un progression non-quest, un story event/context;
- owner smoke test: persoana care ruleaza fluxul cap-coada.

Checklist:

```text
ServerDir:
RegionId: demo_sat
PlayerName:
JAR core:
JAR addon medieval:
OpenAI enabled: da/nu
Backup initial: da/nu
```

Gate:

- exista server dedicat;
- exista backup initial daca serverul are deja date;
- este clar ce se demonstreaza si ce se amana.

## Faza D1: Build, Instalare Si Config

Scop: pluginul si addonul medieval pornesc curat.

Comenzi locale:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean test
.\gradlew.bat assemble
```

Artefacte asteptate:

```text
ainpc-core-plugin/build/libs/ainpc-core-plugin-1.0.0.jar
ainpc-scenario-medieval/build/libs/ainpc-scenario-medieval-1.0.0.jar
ainpc-api/build/libs/ainpc-api-1.0.0.jar
```

Config minim:

```yaml
world_admin:
  enabled: true
  auto_index:
    enabled: true

simulation:
  enabled: true

routine:
  enabled: true
  natural_movement:
    enabled: true

debug: false
```

Reguli:

- cheia OpenAI se pune in `OPENAI_API_KEY`, nu in fisiere commit-uite;
- demo-ul trebuie sa mearga si fara cheie, prin fallback;
- `debug: true` se foloseste doar temporar pentru investigatie.

Gate:

- `/plugins` arata AINPC si addonul medieval enabled;
- `/ainpc` raspunde;
- `/ainpc audit db` nu raporteaza eroare critica;
- logul de startup nu are stacktrace repetat.

## Faza D2: Sat Semantic Demo

Scop: toate sistemele urmatoare au locuri semantice comune.

Varianta rapida:

```text
/ainpc world demo create demo_sat
/ainpc world whereami
/ainpc world places demo_sat
/ainpc audit world
/ainpc world save
```

Varianta pe sat vanilla existent:

```text
/ainpc world scan village 64
/ainpc world scan village 64 import demo_sat
/ainpc audit world
/ainpc world save
```

Verificari:

- ruleaza comanda intr-o zona relativ plata;
- confirma ca exista case, market/social hub, work places si node-uri;
- confirma ca `StoryContextService` vede aceeasi regiune/place ca `world whereami`;
- nu presupune ca mapping-ul demo construieste blocuri fizice.
- `/ainpc demo status demo_sat` calculeaza NPC-urile si bindings-urile demo dupa place-urile din `demo_sat`; NPC-urile din alte regiuni sunt afisate doar ca total informativ.

Gate:

- `demo_sat` are regiune, places si nodes;
- auditul world nu raporteaza lipsuri critice pentru demo;
- dupa restart, `/ainpc world places demo_sat` arata aceleasi date.

## Faza D3: Populare NPC Si Bindings

Scop: satul are NPC-uri vizibile, persistente si legate de locuri.

Flux recomandat pentru demo de sat:

```text
/ainpc world settlement plan demo_sat 5
/ainpc world settlement spawn demo_sat 5
/ainpc list
/ainpc audit npc
/ainpc audit spawn
```

Flux manual, daca settlement spawn nu este potrivit:

```text
/ainpc create FierarDemo
/ainpc create FermierDemo
/ainpc create PaznicDemo
/ainpc world bind npc FierarDemo <homePlaceId> <workPlaceId> <socialPlaceId>
/ainpc world bind npc FermierDemo <homePlaceId> <workPlaceId> <socialPlaceId>
/ainpc world bind npc PaznicDemo <homePlaceId> <workPlaceId> <socialPlaceId>
/ainpc world bindings FierarDemo
/ainpc audit spawn
```

Gate:

- exista 3-5 NPC-uri;
- fiecare NPC are profil si entitate valida;
- bindings home/work/social pot fi inspectate;
- nu apar duplicate dupa restart;
- rollback-ul settlement spawn nu lasa NPC-uri partiale evidente.

## Faza D4: Rutina, Interactiune Si UX Vizibil

Scop: demo-ul se simte jucabil, nu doar tehnic.

Comenzi:

```text
/ainpc routine status nearest
/ainpc routine tick
/ainpc info nearest
/ainpc quest nearest
```

Contract routine pentru demo:

- `nearest` este selector explicit pentru `/ainpc routine status nearest`; nu trebuie tratat ca nume literal de NPC.
- GUI-ul de rutina si GUI-ul de interactiune trebuie sa ruleze aceeasi comanda explicita: `ainpc routine status nearest`.
- `routine tick` este comanda de evaluare manuala; nu trebuie sa spammeze actionbar-ul sau chat-ul, ci sa intoarca sumar admin cu total/evaluati/mutati/skip.
- planner-ul local de rutina trebuie sa cada sigur: fara work anchor foloseste social/home, fara ancore ramane `IDLE` cu motiv citibil.

Ce trebuie verificat in joc:

- click dreapta pe NPC deschide interactiunea asteptata;
- GUI-ul sau mesajele arata actiunea urmatoare;
- NPC-ul nu sare constant intre ancore;
- rutina foloseste home/work/social;
- daca nu exista quest, mesajul spune clar asta.
- daca nu exista NPC aproape, `routine status nearest` trebuie sa intoarca mesaj clar, nu eroare sau stacktrace.

Gate:

- playerul intelege cine este NPC-ul si ce poate face;
- rutina este inspectabila de admin;
- nu apar miscari haotice evidente in primele minute;
- GUI-ul nu ascunde starea reala a questului sau rutinei.
- limitarile MVP sunt acceptate: miscarea naturala depinde de AI/pathfinder Paper, iar teleport fallback este controlat de config si cooldown.

## Faza D5: Quest Clasic Plus Progression Divers

Scop: demo-ul arata gameplay, nu doar admin tooling.

Set minim recomandat:

| Tip                      | Exemplu de tinta                | De ce intra in demo                                |
|--------------------------|---------------------------------|----------------------------------------------------|
| Quest clasic             | Q06/Q07/Q08 sau `quest nearest` | arata oferta, acceptare, progres si reward         |
| Contract                 | C02                             | arata progres non-`QUEST` peste market/quest board |
| Tutorial                 | T01                             | arata onboarding/primul pas al playerului          |
| Bounty/duty/event/ritual | B01, B02, D01, E01 sau R01      | adauga diversitate fara runtime nou                |

Smoke minim:

```text
/ainpc audit quest
/ainpc quest log
/ainpc quest nearest
/ainpc quest accept nearest
/ainpc quest status nearest
/ainpc quest track start
/ainpc quest anchors
/ainpc progression definitions
/ainpc contract definitions
/ainpc tutorial definitions
/ainpc duty definitions
/ainpc bounty definitions
/ainpc event definitions
/ainpc ritual definitions
```

Dupa interactiuni:

```text
/ainpc progression stored <player>
/ainpc quest status nearest
/ainpc audit quest
```

Gate:

- minim un quest se poate accepta si inspecta;
- minim o mecanica non-quest este disponibila si are progres inspectabil;
- obiectivele folosesc locuri/noduri din `demo_sat`, nu doar iteme izolate;
- progresul ramane dupa restart;
- questurile nu depind de AI pentru a decide progresul.

## Faza D6: Story Context Si Efecte Narative Minime

Scop: lumea pare sa retina ceva dupa actiunile playerului.

Comenzi:

```text
/ainpc story context
/ainpc story context <player> nearest
/ainpc story region demo_sat
/ainpc story place <placeId>
/ainpc story events
/ainpc debugdump story
```

Gate:

- contextul story foloseste mapping-ul si quest anchors active;
- cel putin un story event sau story state poate fi inspectat dupa un scenariu;
- debugdump-ul story nu expune secrete;
- story-ul ramane read-only pentru AI, cu scrieri doar prin actiuni validate.

Regula debugdump safety:

- `config-sanitized.yml`, `openai.txt` si `recent-server-log.txt` nu trebuie sa contina API keys, bearer tokens, parole sau `OPENAI_API_KEY` in clar.
- redactarea locala este verificata prin `DebugDumpSecretsTest`; exportul fizic ramane gate Paper pentru milestone.
- contractul local al exporturilor este verificat prin `DebugDumpOutputContractTest`: lista de fisiere, `semantic_index`, bindings/progression/story rows si counters principali.

## Faza D7: Dialog Si AI Fallback

Scop: dialogul este o demonstratie de UX, nu un punct unic de esec.

Flux in joc:

```text
click dreapta pe NPC
scrie un mesaj scurt
verifica raspunsul
inchide conversatia cu "pa"
```

Test fara AI extern:

- porneste serverul fara `OPENAI_API_KEY`;
- repeta conversatia;
- confirma ca fallback-ul este clar si serverul ramane stabil.

Test cu AI extern, daca exista cheie:

```powershell
.\scripts\debug-openai.ps1
.\scripts\debug-openai.ps1 -IncludeResponseTest
```

Gate:

- conversatia porneste;
- distanta de interactiune este respectata;
- fallback-ul este acceptabil pentru demo;
- logurile nu contin cheia API;
- AI-ul nu executa direct progres, reward sau schimbari in lume.

## Faza D8: Repetitie Cap-Coada Si Restart

Scop: demo-ul poate fi reprodus, nu doar nimerit o data.

Comenzi automate utile:

```powershell
.\scripts\smoke-paper-mapping.ps1 -ServerDir "C:\Minecraft\paper-test" -SkipWandFlow
.\scripts\smoke-paper-quests.ps1 -ServerDir "C:\Minecraft\paper-test" -PlayerName "NumeleTau" -RegionId "demo_sat"
```

Comenzi manuale obligatorii:

```text
/ainpc demo phases demo_sat <player>
/ainpc demo definition
/ainpc demo script demo_sat <player>
/ainpc demo evidence demo_sat <player>
/ainpc demo runbook demo_sat <player>
/ainpc demo smoke demo_sat <player>
/ainpc demo summary demo_sat <player>
/ainpc demo next demo_sat
/ainpc demo commands demo_sat <player>
/ainpc demo restart demo_sat
/ainpc demo experimental demo_sat <player>
/ainpc demo experimental5 demo_sat <player>
/ainpc demo experimental25 demo_sat <player>
/ainpc demo experimental25deep demo_sat <player>
/ainpc demo experimental25ops demo_sat <player>
/ainpc demo status demo_sat
/ainpc audit all
/ainpc debugdump all
/ainpc world places demo_sat
/ainpc list
/ainpc progression stored <player>
```

Restart gate:

1. Ruleaza fluxul demo.
2. Ruleaza `/ainpc audit all`.
3. Ruleaza `/ainpc debugdump all`.
4. Opreste serverul.
5. Porneste serverul.
6. Ruleaza din nou auditul si verificarile de mapping/NPC/progression.

Gate:

- acelasi flux trece inainte si dupa restart;
- mapping-ul, NPC-urile, bindings si progresul exista dupa restart;
- debugdump-ul contine world, NPC, quest/progression si story;
- exista backup pentru `plugins/AINPC/` si JAR-urile folosite.

## Faza D9: Script De Demo Pentru Tester

Scop: demo-ul are o poveste de 8-12 minute pe care o poate urma oricine.

Comanda de ghid in joc:

```text
/ainpc demo phases demo_sat <player>
/ainpc demo definition
/ainpc demo script demo_sat <player>
/ainpc demo evidence demo_sat <player>
/ainpc demo runbook demo_sat <player>
/ainpc demo smoke demo_sat <player>
/ainpc demo summary demo_sat <player>
/ainpc demo next demo_sat
/ainpc demo commands demo_sat <player>
/ainpc demo restart demo_sat
/ainpc demo experimental demo_sat <player>
/ainpc demo experimental5 demo_sat <player>
/ainpc demo experimental25 demo_sat <player>
/ainpc demo experimental25deep demo_sat <player>
/ainpc demo experimental25ops demo_sat <player>
```

Aceste comenzi sunt read-only. `definition` explica pragul de demo functional, `phases` afiseaza gate-urile D0-D9, `script` afiseaza pasii de mai jos in chat, `evidence` spune ce output-uri trebuie capturate pentru milestone, `runbook` rezuma operarea, `smoke` afiseaza checklist-ul minim de verificare rapida, `summary` da pasii imediati in format scurt, `next` condenseaza blocajele si comenzile urmatoare, `commands` listeaza fluxul compact de rulare, iar `restart` listeaza gate-ul de persistenta inainte si dupa restart. Modurile `experimental`, `experimental5`, `experimental25`, `experimental25deep` si `experimental25ops` sunt doar pentru analiza interna si pot fi schimbate sau sterse inainte de release.

Pentru batch-uri Codex mari, rapoartele din `.ai/` folosesc o clasificare simpla:

- `LOCAL`: task-ul poate fi verificat prin teste unitare/statice, documentatie sau parser local.
- `MIXED`: exista acoperire locala, dar gate-ul final ramane pe server Paper.
- `LIVE`: task-ul cere server Paper, jucator real, restart, entitati live sau export fizic.

Raportul experimental `.ai/codex-250-demo-batch-07-99-task-report.md` aplica aceasta regula pentru T101-T199 si este validat de testul `CodexDemoBacklogTest`.
Raportul `.ai/codex-session-safe-capacity.md` fixeaza limita practica pentru sesiuni Codex: 10-20 task-uri implementate in siguranta, pana la 99 task-uri urmarite in raport verificat automat, iar task-urile `LIVE` nu se inchid fara Paper real.
Indexul `.ai/codex-demo-report-index.md` listeaza rapoartele batch si trebuie folosit ca punct de intrare inainte de a citi fisierele mari.
Planul `.ai/codex-demo-milestones.md` transforma backlog-ul T001-T250 in milestone-uri M0-M6, ordonate dupa gate-uri P0/P1/P2.
Runbook-ul `.ai/codex-paper-restart-runbook.md` detaliaza gate-ul Paper T211-T230 si interzice oprirea fortata a serverului.
Checklist-ul `.ai/codex-release-freeze-checklist.md` acopera T231-T250 si marcheaza experimentele ca nefiind API public pana la freeze.
Backlog-ul `.ai/codex-fast-250-experimental-backlog.md` defineste seria experimentala F001-F250 pentru fast programming, iar ghidul `.ai/codex-fast-250-experimental-guide.md` explica regulile: mai putine verificari, dar nu verificari zero; codul ramane intern pana trece prin cleanup, test sau smoke Paper.

Ordine recomandata:

1. Intri pe server si arati ca AINPC este loaded.
2. Rulezi `/ainpc demo status demo_sat` ca baseline.
3. Rulezi `/ainpc demo definition` daca testerul trebuie sa vada criteriile demo-ului.
4. Rulezi `/ainpc demo next demo_sat` ca sa vezi blocajele concrete.
5. Rulezi `/ainpc demo commands demo_sat <player>` daca vrei lista compacta de comenzi.
6. Rulezi `/ainpc world demo create demo_sat` doar daca mapping-ul lipseste.
7. Rulezi `/ainpc world places demo_sat` si arati satul semantic.
8. Rulezi `/ainpc world settlement plan demo_sat 5` si `/ainpc world settlement spawn demo_sat 5` daca NPC-urile lipsesc.
9. Rulezi `/ainpc list` si alegi un NPC.
10. Arati `/ainpc routine status nearest`.
11. Dai click dreapta pe NPC si pornesti dialogul.
12. Rulezi `/ainpc quest nearest` si accepti un quest.
13. Mergi la un place/node cerut de quest sau inspectezi obiectivul.
14. Arati `/ainpc progression stored <player>` pentru mecanica non-quest.
15. Arati `/ainpc story events demo_sat` sau `/ainpc story context`.
16. Rulezi `/ainpc audit all`.
17. Rulezi `/ainpc debugdump all`.
18. Rulezi `/ainpc demo status demo_sat` ca raport final de readiness.
19. Rulezi `/ainpc demo restart demo_sat` si urmezi checklist-ul daca demo-ul este pentru milestone.
20. Repeti auditul dupa restart daca demo-ul este pentru milestone.

Gate:

- testerul poate urma pasii fara sa editeze DB;
- fiecare pas are rezultat vizibil;
- problemele ramase sunt notate ca backlog, nu ascunse.

## Stop Conditions

Opreste demo-ul si repara inainte sa continui daca apare oricare situatie:

- pluginul nu se incarca;
- `/ainpc` nu raspunde;
- `/ainpc audit db` raporteaza eroare critica;
- `demo_sat` nu poate fi salvat sau reloaded;
- settlement spawn creeaza duplicate sau date partiale nerecuperabile;
- NPC-urile dispar dupa restart;
- quest/progression crapa la accept/status;
- debugdump expune secrete;
- AI-ul este necesar pentru progresul logic al jocului.

## Faze Dupa Primul Demo

Dupa ce D0-D9 trec, urmatoarea ordine pragmatica este:

1. Playable village hardening: teren plat, distante, drumuri, case clare.
2. Smoke complet pentru wand: `region`, `place`, `node`, `npc_bind`, `quest_anchor`.
3. Generator narativ de populatie: nume, roluri, familii, distributie.
4. 3-5 questuri medievale cap-coada peste `demo_sat`.
5. Mai multe mecanici progression: contract, duty, bounty, event, tutorial, ritual.
6. Story/reputatie/reactii NPC, dar doar peste context validat.
7. Release `demo-playable` cu backup, restart, audit, debugdump si hash-uri JAR.
