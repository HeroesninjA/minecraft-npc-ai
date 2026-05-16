# Lucru Alternat: Quest, Mapping, Story, ProgressionService si GUI

Actualizat: 2026-05-11

## Scop

Acest document stabileste cum trebuie lucrat in fazele urmatoare cand dezvoltarea atinge simultan:

- questuri si contracte jucabile;
- world mapping semantic;
- `ProgressionService` ca directie de runtime generic;
- sistemul GUI ca strat vizual peste progres, mapping, NPC-uri, audit si debug;
- story state ca strat narativ optional peste mapping si progres;
- AI authoring ca sursa de drafturi validate, nu executie live.

Regula principala: nu dezvolta mult timp doar una dintre aceste parti. Questurile, mapping-ul, story context-ul, progression runtime-ul si GUI-ul trebuie sa se verifice reciproc prin slice-uri mici, testabile pe Paper.

## De ce alternanta este necesara

Questurile fara mapping ajung sa foloseasca locuri inventate sau fallback-uri fragile.

Mapping-ul fara questuri ramane doar structura de date, fara dovada ca locurile, node-urile si bindings-urile sunt utile in gameplay.

`ProgressionService` extras prea devreme risca sa devina abstractie mare fara suficiente cazuri reale. El trebuie extras din comportament deja verificat: questuri, contracte, stages, obiective, anchors, tracking si reload.

GUI-ul construit prea devreme risca sa ascunda lipsa de runtime stabil. GUI-ul construit prea tarziu face sistemul greu de testat pe server. El trebuie sa intre in fiecare slice ca prezentare read-only sau actiune validata peste servicii existente.

Directia corecta este:

```text
mapping concret
-> gap/patch check read-only cand lipseste infrastructura
-> story baseline sau efect story explicit
-> quest/contract/progres mic
-> GUI snapshot
-> smoke test
-> observabilitate
-> extractie progression
```

## Rolurile partilor implicate

| Parte | Rol | Nu trebuie sa faca |
|---|---|---|
| Mapping | Sursa de adevar pentru regiuni, places, nodes si legaturi NPC-loc | Nu decide progres, reward sau status de quest |
| Quest/contract | Gameplay concret care consuma mapping si produce progres verificabil | Nu inventeaza locatii daca mapping-ul nu le poate rezolva |
| Story state | Context narativ, evenimente si flags care pot influenta dialog, selectie de quest si drafturi | Nu scrie direct in progres si nu acorda reward-uri |
| QuestDirector | Punte optionala intre story context, mapping si continut jucabil | Nu executa progres si nu sare peste validatoare |
| ProgressionService | Runtime generic pentru progres, obiective, stages, status, tracking si recompense | Nu trebuie extras prin migrare mare inainte de demo stabil |
| GUI system | Prezentare si control sigur peste snapshot-uri, comenzi validate, audit si debug | Nu muta logica din `ScenarioEngine`, `ProgressionService`, `WorldAdminApi` sau DB in inventare |
| AI authoring | Produce `QuestDraft`, texte, motivatie si explicatii peste context validat | Nu porneste questuri live, nu scrie config live si nu modifica story/progres direct |
| Patch Planner | Produce `GapReport` si `PatchPlan` read-only cand mapping-ul nu sustine continutul dorit | Nu construieste si nu scrie mapping pana nu exista commit/builder validat |

## Regula story vs quest

Story-ul si questurile sunt conectabile, dar nu sunt acelasi lucru.

```text
Story poate exista fara quest.
Quest poate exista fara story.
QuestDirector poate lega optional story -> quest.
```

Exemple:

- story fara quest: `market_unrest=true`, eveniment recent in `story_events`, reputatie locala scazuta;
- quest fara story: tutorial, livrare simpla, bounty repetabil, sarcina NPC de rutina;
- quest legat de story: story state-ul produce context, `QuestDirector` selecteaza/geneaza candidat, iar completarea scrie `record_story_event` sau `set_story_state`.

Fluxul corect pentru story-driven selection:

```text
StoryStateService
-> StoryContextService
-> QuestDirector
-> QuestAnchorResolver / QuestDraftValidator
-> ProgressionService
-> GUI / audit / debugdump
```

Interzis:

- `StoryStateService` care scrie direct in `player_quests`;
- AI care creeaza quest live direct;
- dialog care completeaza obiective fara runtime;
- GUI care schimba story/progres fara serviciu validat.

## Cadenta recomandata

Lucrul ar trebui impartit in cicluri scurte.

### Pasul 1 - Mapping verificabil

Scop: exista o regiune demo cu places/nodes suficiente pentru urmatorul quest.

Livrabile:

- `region_id` stabil;
- places clare: piata, fierarie, taverna, poarta, casa;
- nodes utile: `quest_trigger`, `inspect`, `work`, `storage`, `entrance`;
- `npc_world_bindings` pentru home/work/social;
- audit world/db fara erori majore.

Comenzi de verificare:

```text
/ainpc world demo create demo_sat
/ainpc world places demo_sat
/ainpc world bindings list
/ainpc audit world
/ainpc audit db
```

### Pasul 1.5 - Patch Planner read-only

Scop: inainte sa adaugi continut nou, verifica daca mapping-ul poate sustine populatia, profesiile, locurile sociale si ancorele de quest.

Livrabile:

- `GapReport` pentru regiune;
- lista de gap-uri: case/paturi, workplace-uri, social hub, quest trigger, entrance/work nodes;
- `PatchPlan` read-only pentru completari posibile;
- decizie clara: folosesti mapping existent, adaugi node manual prin wand sau amani pana exista builder.

Comenzi de verificare:

```text
/ainpc patch analyze demo_sat 6 blacksmith,farmer,merchant,innkeeper
/ainpc patch plan demo_sat 8 blacksmith,farmer,merchant,innkeeper
/ainpc patch validate demo_sat 8 blacksmith,farmer,merchant,innkeeper
```

Reguli:

- `add_node` semantic-only poate fi valid ca propunere usoara;
- patch-urile care cer `native-block-build` raman blocate pana exista builder;
- nu porni questuri care cer ancore lipsa doar pentru ca AI-ul le-a propus.

### Pasul 2 - Quest sau contract peste mapping

Scop: un singur continut jucabil foloseste mapping-ul real.

Livrabile:

- un quest sau contract cu obiective concrete;
- `objective_id` stabil;
- `visit_place`, `inspect_node`, `talk_to_npc`, `deliver_to_npc` sau `visit_region`, dupa caz;
- quest anchors persistente;
- story event optional la completare;
- progress vizibil in GUI/log.

Comenzi de verificare:

```text
/ainpc quest log all
/ainpc quest status <selector>
/ainpc quest progress <selector>
/ainpc quest anchors
/ainpc audit quest
```

Pentru mecanici non-quest:

```text
/ainpc progression log
/ainpc progression stored all contract
/ainpc contract log
/ainpc contract stored all
/ainpc contract status <selector>
```

### Pasul 2.2 - Story baseline peste mapping

Scop: verifici ca lumea poate avea stare narativa proprie, chiar si fara quest activ.

Livrabile:

- story state sau story event legat de `regionId` ori `placeId`;
- semnale story vizibile in `StoryContextSnapshot`;
- audit/debugdump care arata sursa story-ului;
- decizie explicita daca slice-ul este `story_only`, `writes_story`, `story_driven` sau `no_story`.

Comenzi de verificare:

```text
/ainpc story region <regionId>
/ainpc story place <placeId>
/ainpc story events <regionId|placeId> 10
/ainpc story context
/ainpc debugdump story
```

Reguli:

- story baseline nu creeaza automat randuri in `player_quests`;
- story state-ul se scrie prin `StoryStateService` sau actiuni validate, nu prin GUI/dialog direct;
- un story event trebuie sa aiba scope clar: region sau place;
- daca story-ul doar informeaza dialogul, ramane `story_only`;
- daca story-ul trebuie sa declanseze continut jucabil, trece prin `QuestDirector`.

### Pasul 2.5 - Story context si QuestDirector

Scop: stabilesti daca acest continut este independent de story, produce story sau este ales de story.

Cele trei forme sunt valide:

| Forma | Cand se foloseste | Exemplu |
|---|---|---|
| story fara quest | lumea are stare narativa, dar nu cere progres | `market_unrest=true`, dialog schimbat |
| quest fara story | progres mecanic simplu | tutorial, livrare, duty repetabil |
| quest story-driven | story-ul cere continut potrivit | investigatie dupa eveniment in piata |

Livrabile:

- decizie explicita: `no_story`, `writes_story`, `story_driven` sau combinatie controlata;
- daca este `writes_story`, story action-ul este in YAML si trece auditul;
- daca este `story_driven`, `QuestDirector` selecteaza un template existent sau produce `QuestSeed`;
- daca este generat cu AI, output-ul este `QuestDraft`, nu quest activ.

Comenzi de verificare:

```text
/ainpc story context
/ainpc story region <regionId>
/ainpc story place <placeId>
/ainpc story events <regionId|placeId> 10
/ainpc debugdump story
```

Reguli:

- story-ul nu scrie direct in `player_quests`;
- `ProgressionService` ramane proprietarul progresului;
- story actions sunt efecte controlate la finalizare sau puncte explicite, nu text ascuns in dialog;
- GUI-ul arata efectul sau contextul, dar nu devine motor story.

### Pasul 2.6 - QuestDraft AI, doar dupa context valid

Scop: folosesti AI pentru authoring numai cand mapping-ul, story context-ul si regulile runtime sunt suficient de clare.

Livrabile:

- `QuestSeed` cu regiune, tema, mecanica, obiective permise si limite;
- `QuestDraft` strict;
- raport de validare;
- export YAML dezactivat;
- audit quest dupa export.

Nu face:

- activare automata pe server live;
- reward-uri inventate de AI;
- obiective fara listener;
- story flags fara validator;
- ancore bazate pe coordonate brute.

### Pasul 3 - GUI minim pentru slice

Scop: jucatorul sau adminul poate vedea progresul si contextul fara sa citeasca DB-ul sau logurile brute.

Livrabile:

- `ProgressionGuiSnapshot` sau snapshot read-only echivalent pentru datele afisate;
- filtru GUI potrivit: `quest`, `contract`, `duty`, `bounty`, `event`, `tutorial`, `ritual` sau `all`;
- detalii pentru stage, obiective, rewards si tracking;
- butoane care apeleaza comenzi/servicii validate, nu modifica stare direct;
- fallback text clar pentru aceeasi actiune.

Comenzi de verificare:

```text
/ainpc gui quest all
/ainpc gui quest contract
/quest gui bounty
/ainpc gui world
/ainpc gui routine
```

Pentru admin:

```text
/ainpc gui audit
/ainpc gui debug
```

### Pasul 4 - Observabilitate

Scop: daca ceva nu merge pe server, cauza poate fi vazuta fara editare manuala in DB.

Livrabile:

- audit pentru template, anchors, player progress si story state;
- debugdump cu datele necesare;
- mesaj clar cand mapping-ul lipseste sau questul nu poate rezolva target-ul.

Comenzi de verificare:

```text
/ainpc audit quest
/ainpc debugdump quest
/ainpc debugdump story
/ainpc debugdump world
```

Auditul de quest trebuie sa raporteze si sumarul generic al progresiilor persistate: cate randuri exista, cate sunt curente/arhivate/tracked si daca exista progresii fara definitie incarcata.

### Pasul 5 - Extractie mica spre ProgressionService

Scop: generalizezi doar comportamentul dovedit de quest/contract.

Livrabile acceptabile:

- `ProgressionDefinition` peste definitiile existente;
- `ProgressionSelector` pentru selectori stabili; implementat initial pentru selectori simpli, `tracked/current`, `mechanic:definition` si `pack:mechanic:definition`;
- comanda read-only pentru inspectie, implementata initial prin `/ainpc progression definitions [filter]`;
- comanda admin read-only pentru progres persistent, implementata initial prin `/ainpc progression stored [jucator|uuid|all] [filter] [limit]`;
- filtre explicite pentru metadata generica, implementate initial prin `ProgressionFilter` cu forme ca `scenario:investigation`, `base:TRADE_DEAL` si `mechanic:village_contracts`;
- snapshot-uri generice pentru status/progress/GUI; implementat initial prin `ProgressionStatusSnapshot`, `ProgressionProgressSnapshot`, `ProgressionGuiSnapshot`, `ProgressionGuiEntry` si `ProgressionStageSnapshot`;
- repository separat pentru persistenta, dar cu `player_quests` pastrat initial; implementat initial read-only prin `ProgressionRepository`;
- sumar generic peste persistenta, implementat initial prin `StoredProgressionSummary`;
- export generic in `player-progressions.json`.

Nu face in acest pas:

- migrare DB mare catre `player_progressions`;
- runtime separat pentru contracte;
- rescriere completa a `ScenarioEngine`;
- branching complex daca questurile liniare inca nu sunt stabile pe Paper.

## Reguli de decizie

Lucreaza la mapping cand:

- un quest cere un place/node care nu exista;
- anchors se rezolva prin fallback fragil;
- NPC-urile nu au home/work/social clar;
- auditul world/db nu poate explica legaturile.

Lucreaza la patch planner cand:

- mapping-ul exista, dar nu sustine populatia sau profesiile dorite;
- lipsesc social hub, `quest_trigger`, entrance sau work nodes;
- ai nevoie de o decizie read-only inainte de constructie manuala;
- AI-ul sau story-ul propune continut pentru o ancora care lipseste.

Lucreaza la quest cand:

- mapping-ul exista, dar nu este folosit intr-un flux jucabil;
- demo-ul nu are 3-5 questuri completabile;
- progresul nu supravietuieste reload-ului;
- GUI/log nu arata clar urmatorul pas pentru jucator.

Lucreaza la story cand:

- dialogul sau contextul trebuie sa reflecte evenimente recente fara sa porneasca automat questuri;
- un quest trebuie sa lase urma persistenta prin `record_story_event` sau `set_story_state`;
- vrei ca `QuestDirector` sa aleaga continut pe baza starii lumii;
- auditul/debugdump nu poate explica de ce un quest este disponibil sau blocat narativ.

Lucreaza la AI authoring cand:

- exista mapping si story context suficient pentru un `QuestSeed`;
- vrei variante de continut, nu executie live;
- validatorul poate respinge clar obiective/rewards/story actions invalide;
- adminul poate revizui si exporta YAML dezactivat.

Lucreaza la GUI cand:

- progresul exista, dar jucatorul nu poate vedea clar stage-ul, obiectivele sau tracking-ul;
- adminul trebuie sa ruleze prea multe comenzi text pentru acelasi diagnostic;
- acelasi snapshot poate deservi quest log, details, NPC interaction sau debug;
- o actiune GUI are nevoie de confirmare sau fallback text inainte de a fi sigura.

Lucreaza la `ProgressionService` cand:

- aceeasi logica exista deja in quest si contract;
- selectorii, statusul sau tracking-ul sunt duplicati;
- ai cel putin un caz non-quest functional, cum sunt `C01` si `C02`;
- exportul generic poate fi validat cu date reale din `player_quests`.

## Slice minim recomandat

Fiecare slice nou trebuie sa contina bucatile de control potrivite pentru scopul lui:

| Bucata | Intrebare de control |
|---|---|
| Mapping | Unde se intampla in lumea reala a serverului? |
| Patch planner | Lipseste ceva din mapping si ce propunere read-only exista? |
| Story | Exista context narativ, efect story sau decizie explicita ca nu se foloseste story? |
| Quest/progression | Ce progres concret vede jucatorul? |
| GUI | Cum vede sau controleaza jucatorul/adminul progresul fara comenzi fragile? |
| Persistenta | Ce ramane dupa reload? |
| Debug/audit | Cum aflam de ce nu merge? |

Daca mapping-ul, progresul, persistenta sau auditul lipsesc, slice-ul nu este gata. Patch planner-ul, story-ul si AI authoring-ul sunt obligatorii doar cand slice-ul cere infrastructura noua, context narativ sau continut generat.

## Sablon pentru un slice nou

Foloseste acelasi format pentru fiecare bucata noua de gameplay sau infrastructura:

```text
Nume slice:
Scop jucabil:
Mapping folosit:
Patch planner:
Story mode:
Quest/progression:
GUI:
Persistenta:
Audit/debugdump:
Smoke Paper:
Test automat:
Limite:
```

Campuri recomandate:

| Camp | Ce trebuie scris |
|---|---|
| `Nume slice` | ID scurt, de forma `Q09`, `C03`, `STORY-QUEST-01`, `GUI-MAP-03` |
| `Scop jucabil` | ce face jucatorul sau adminul concret |
| `Mapping folosit` | region/place/node/tag/NPC role consumat |
| `Patch planner` | `not_needed`, `gap_report_clean`, `requires_manual_node`, `blocked_native_patch` |
| `Story mode` | `no_story`, `writes_story`, `story_driven`, `story_only` |
| `Quest/progression` | `base_type`, `mechanic`, `scenario_kind`, selector si stages |
| `GUI` | ce ecran arata progres/context/diagnostic |
| `Persistenta` | ce tabele/fisiere se modifica si ce trebuie sa ramana dupa restart |
| `Audit/debugdump` | ce comanda explica succesul sau eroarea |
| `Smoke Paper` | pasii minimi pe server real |
| `Test automat` | testul de contract/unitate pentru partea determinista |
| `Limite` | ce ramane neimplementat si ce nu trebuie presupus |

Exemplu scurt:

```text
Nume slice: Q09 - Umbra de pe Avizier
Scop jucabil: investigatie scurta in piata
Mapping folosit: tag:market, node quest_board, role merchant
Patch planner: gap_report_clean
Story mode: story_driven + writes_story
Quest/progression: QUEST/main_quests/investigation, stages INVESTIGATE/RETURN
GUI: Quest detail arata stage, objective anchors si story warning daca lipseste
Persistenta: player_quests, quest_anchor_bindings, story_events
Audit/debugdump: audit quest strict, debugdump quest/story/world
Smoke Paper: accepta, viziteaza piata, inspecteaza avizierul, raporteaza
Test automat: anchor resolver pentru tag:market + quest_board
Limite: fara branching, fara reward AI, fara activare automata
```

## Matrice story/quest

Fiecare continut nou trebuie incadrat explicit:

| Caz | Story state | Progression | Cand e corect |
|---|---|---|---|
| `story_only` | da | nu | atmosfera, context AI, reactie NPC, event istoric |
| `quest_only` | nu | da | tutorial, delivery simplu, duty repetabil, bounty generic |
| `writes_story` | scrie la final sau la stage | da | quest care lasa urma in lume |
| `story_driven` | citeste ca input | da | story state alege quest existent sau seed |
| `story_driven_ai_draft` | citeste ca input | nu pana la aprobare | AI produce `QuestDraft`, adminul si validatorul decid |

Reguli:

- `story_only` trebuie sa apara in `/ainpc story ...` si `debugdump story`, dar nu in `quest log`.
- `quest_only` trebuie sa apara in `quest/progression log`, dar nu trebuie sa modifice story state.
- `writes_story` trebuie sa aiba story actions explicite si auditabile.
- `story_driven` cere decizie explicabila de `QuestDirector`.
- `story_driven_ai_draft` nu devine runtime pana la export, audit si activare controlata.

## Contract initial pentru QuestDirector

`QuestDirector` exista initial ca strat determinist read-only in `ro.ainpc.engine`. Decizia lui nu porneste progresii si pastreaza `runtimeExecutable=false`.

Input initial implementat:

```text
QuestDirectorRequest
  storyContextSnapshot
  definitions
  preferredMechanicId optional
  questSeedAllowed
  blockingReasons
```

Input planificat ulterior:

```text
playerId optional
npcId optional
regionId
semanticIndexExcerpt
activeProgressions
cooldowns
maxCandidates
```

Output initial implementat:

```text
QuestDirectorDecision
  status = candidate_found | seed_suggested | blocked | no_action
  reason
  selectedProgressionId optional
  selectedTemplateId optional
  selectedMechanicId optional
  selectedDefinitionId optional
  matchedSignals
  candidateTemplateIds
  blockedReasons
  warnings
  runtimeExecutable = false
```

Reguli:

- `candidate_found` inseamna template existent, nu progres pornit.
- `seed_suggested` inseamna `QuestSeed`, nu `QuestDraft` si nu YAML live.
- `blocked` trebuie sa explice lipsa: mapping, story condition, cooldown, max active, reward invalid sau runtime lipsa.
- `no_action` este rezultat valid cand story-ul nu cere quest.
- decizia trebuie sa poata fi reprodusa in debugdump fara AI.

## Contract GUI pentru alternanta

GUI-ul nu trebuie sa incerce sa fie motor de gameplay. Pentru fiecare slice, GUI-ul are trei roluri:

1. arata starea curenta;
2. ofera actiuni validate deja existente;
3. deschide diagnostice pentru admin.

Ecrane si date minime:

| Ecran | Trebuie sa arate | Nu trebuie sa faca |
|---|---|---|
| Quest log | progresii grupate dupa mecanica, tracked/current, status | nu calculeaza progres |
| Quest detail | stage, obiective, rewards, anchors, story effects declarate | nu completeaza obiective direct |
| World GUI | regiune/place/node curent, ancore relevante, progresii apropiate cand exista API | nu cauta DB direct |
| Routine GUI | home/work/social bindings, slot rutina, shortcut la binding diagnostics | nu decide rutina |
| Audit GUI | ruleaza comenzi audit/debug existente | nu repara fara confirmare explicita |
| Draft GUI viitor | seed, draft, validare, export dezactivat | nu activeaza live automat |

Fallback obligatoriu:

- orice actiune GUI trebuie sa aiba comanda text echivalenta;
- orice eroare GUI trebuie sa indice audit/debugdump relevant;
- orice buton admin care scrie stare cere confirmare sau dry-run.

## Contract debug/audit pentru alternanta

Cand un slice esueaza, adminul trebuie sa poata raspunde la intrebari fara editare DB.

| Intrebare | Sursa |
|---|---|
| Exista regiunea/place/node-ul? | `/ainpc audit world`, `world-mapping.json` |
| Exista legatura NPC home/work/social? | `/ainpc world bindings`, `/ainpc audit db`, `npc-world-bindings.json` |
| Exista ancora de quest? | `/ainpc quest anchors`, `quest-anchor-bindings.json` |
| Obiectivul are `objective_id` valid? | `/ainpc audit quest strict`, `loaded-quest-definitions.json` |
| Progresia este activa/tracked/completa? | `/ainpc progression stored ...`, `player-progressions.json` |
| Story state-ul exista fara quest? | `/ainpc story region/place/events`, `story-states.json`, `story-events.json` |
| Questul a scris story event? | `debugdump story` si `debugdump quest` |
| Mapping-ul poate sustine un quest nou? | `/ainpc patch analyze|plan|validate` |
| Draftul AI este valid? | viitor `QuestDraftValidationReport` |

Definitia de eroare buna:

```text
Ce lipseste?
Unde s-a cautat?
Ce comanda verifica?
Ce actiune sigura urmeaza?
```

## Exemple pe matricea story/quest

Aceste exemple sunt intentionat mici. Scopul lor este sa verifice contractul, nu sa adauge continut mare.

### `STORY-ONLY-01 - Tensiune in piata`

- Story mode: `story_only`.
- Context: piata are eveniment local, de exemplu zvonuri despre preturi sau lipsa proviziilor.
- Mapping: cere doar regiune/place existent cu tag semantic `market`.
- Quest/progression: nu creeaza intrare in log, nu porneste progresie.
- Persistenta: scrie sau actualizeaza `story_state` / `story_event`.
- GUI: apare in story/diagnostic view, nu in quest log.
- Smoke: dupa restart, story state-ul exista si nu apare niciun `player_quests` nou.

### `QUEST-ONLY-01 - Livrare simpla`

- Story mode: `quest_only`.
- Context: un NPC cere livrare simpla catre alt NPC sau place.
- Mapping: cere doua ancore existente, sursa si destinatie.
- Quest/progression: porneste progresie normala cu obiective si reward.
- Persistenta: scrie doar progresia jucatorului.
- GUI: apare in quest log si quest detail.
- Smoke: acceptare, progres, completare si reward fara `story_events` noi.

### `WRITES-STORY-01 - Raport la negustor`

- Story mode: `writes_story`.
- Context: questul poate fi pornit independent, dar finalul afecteaza povestea locala.
- Mapping: foloseste NPC negustor si place `market`.
- Quest/progression: quest normal, executat de `ProgressionService`.
- Persistenta: la final ruleaza actiune declarata `record_story_event`.
- GUI: questul apare in log; efectul de story apare doar in detaliu/debug dupa completare.
- Smoke: inainte de completare nu exista story event, dupa completare exista exact evenimentul asteptat.

### `STORY-DRIVEN-AI-01 - Avizier vandalizat`

- Story mode: `story_driven_ai_draft`.
- Context: story state-ul spune ca avizierul din piata a fost vandalizat.
- Mapping: cere `quest_board`, `market` si cel putin un NPC eligibil.
- Quest/progression: `QuestDirector` produce decizie; AI poate propune `QuestSeed`/`QuestDraft`, dar nu activeaza live.
- Persistenta: draftul sta in export/review, nu in progresia jucatorului.
- GUI: apare in Draft GUI viitor sau raport admin, nu in quest log pana la aprobare.
- Smoke: decizia poate fi reprodusa fara AI, validatorul explica de ce draftul este valid sau blocat.

## Raport standard de smoke

Fiecare slice care schimba runtime, mapping, story sau GUI trebuie sa poata fi raportat in acelasi format.

```text
Data:
Build/JAR:
Server:
Seed/World:
Slice:
Commands run:
Expected:
Actual:
Audit:
Debugdump files:
Restart result:
Issues:
Decision:
```

`Decision` poate fi:

- `pass` - comportamentul a fost confirmat si poate ramane in backlog ca stabil.
- `pass_with_notes` - merge, dar exista limitari documentate.
- `blocked` - lipseste mapping, runtime, validare sau GUI.
- `reject` - comportamentul contrazice contractul si trebuie schimbat inainte de continuare.

## Ordine practica pentru fazele urmatoare

1. Verifica mapping demo pe Paper: create, plan, spawn, audit, save, reload.
2. Ruleaza `/ainpc patch analyze|plan|validate` pentru demo si noteaza daca lipsesc ancore sau capabilitati.
3. Testeaza Q01-Q05 cap-coada fara mapping complex.
4. Testeaza Q06-Q08 pe mapping demo.
5. Testeaza C01 ca progres non-quest prin `contract` si `progression`.
6. Testeaza C02 ca progres non-quest peste mapping-ul demo: piata, `quest_board`, story event si persistenta prin `ProgressionService`.
7. Verifica story state separat: story fara quest, quest fara story si quest cu `record_story_event`.
8. Verifica acelasi set in GUI: `/ainpc gui quest all`, filtre pe mecanica, detalii, tracking si status.
9. Adauga 1-2 questuri noi numai daca folosesc locuri/NPC-uri deja mapate si apar corect in GUI.
10. Pentru continut AI, creeaza doar `QuestSeed`/`QuestDraft` si export dezactivat; nu activa live fara audit.
11. Extrage o bucata mica in `ProgressionService` numai dupa ce smoke test-ul si GUI-ul trec.
12. Actualizeaza docs si TODO dupa fiecare slice care schimba comportamentul real.

## Slice implementat 2026-05-08

`C02 - Avizierul Pietei` este slice-ul curent pentru alternanta:

- Mapping: consuma `tag:market` si node-ul semantic `quest_board` din demo settlement.
- Quest/progression: este `TRADE_DEAL` cu mecanica `village_contracts`, deci apare prin fatada `/ainpc contract ...`.
- Persistenta: foloseste acelasi progres persistent compatibil citit de `ProgressionRepository`, cu metadata `scenarioKind=investigation` disponibila in view-ul generic `StoredProgression`.
- Debug/audit: definitia este validata de testul pack-ului medieval, ancorele tag/node sunt acoperite prin testul `QuestAnchorResolver`, iar `loaded-quest-definitions.json` arata pentru obiective tipul normalizat si referinta semantica asteptata.
- Mapping debug: `world-mapping.json` include `semantic_index`, unde `market` trebuie sa apara la place candidates/tags/types si `quest_board` la node candidates/metadata.
- Audit: `/ainpc audit quest` si `quest-audit-report.txt` din debugdump folosesc acelasi index ca sa avertizeze daca un obiectiv semantic cere un token care lipseste din mapping-ul curent.
- Smoke: script-ul include pasii pentru `contract definitions`, `progression definitions investigation`, `contract status/progress`, `contract stored` si `progression stored investigation`; filtrarea stabila accepta si `scenario:investigation`.
- Extractie mica ProgressionService: `QuestScenarioContract` recunoaste `investigation`, iar `ProgressionDefinition.scenarioKind()` pastreaza tipul real pentru contractul C02.
- GUI: apare in `/ainpc gui quest contract` si in `/quest gui contract`, prin acelasi snapshot generic folosit de `QuestLogGui`; daca NPC-ul este in apropiere, `NpcInteractionGui` foloseste aceeasi intrare pentru status/accept kind-aware.

`D01 - Rondul Strajerului` extinde acelasi fir cu o mecanica non-quest noua:

- Mapping: consuma regiunea `type:settlement` si node-ul semantic `quest_board`, deja prezente in demo settlement.
- Quest/progression: este `DUTY` cu mecanica `npc_duties` si progres `kind=duty`, deci nu este nici quest principal, nici contract comercial.
- Persistenta: foloseste aceeasi tabela compatibila `player_quests`, citita prin `ProgressionRepository`, cu `baseType=DUTY`, `mechanic=npc_duties` si `scenarioKind=duty`.
- Debug/audit: apare in `/ainpc audit quest`, in `loaded-quest-definitions.json`, in `player-progressions.json` si in filtrele `progression definitions duty` / `progression stored duty`.
- Smoke: script-ul include pasii `/ainpc duty definitions`, `/ainpc duty nearest`, `/ainpc duty status/progress/track`, `/ainpc duty stored` si `/ainpc progression stored duty`.
- UX comenzi: `/ainpc contract nearest|accept|decline`, `/ainpc duty nearest|accept|decline`, `/ainpc bounty nearest|accept|decline`, `/ainpc event nearest|accept|decline`, `/ainpc tutorial nearest|accept|decline` si `/ainpc ritual nearest|accept|decline` folosesc selectie dupa `progressionKind`, nu simplu fallback la orice quest al NPC-ului.
- GUI: apare in filtrul `duty`, cu status/stage/obiective citite din `ProgressionGuiSnapshot`.

`B01 - Recompensa Drumului Vechi` adauga o a treia familie non-quest:

- Mapping: foloseste regiunea demo `type:settlement` ca ancora de patrula si un obiectiv combat `kill_mob` pentru amenintarea locala.
- Quest/progression: este `BOUNTY` cu mecanica `local_bounties`, progres `kind=bounty` si scenariu `hunt`.
- Persistenta: foloseste acelasi view generic prin `ProgressionRepository`, cu `baseType=BOUNTY`, `mechanic=local_bounties` si `scenarioKind=hunt`.
- Debug/audit: apare in definitiile generice, in filtrele `kind:bounty`/`base:BOUNTY`/`mechanic:local_bounties` si in story events regionale.
- Smoke: script-ul include pasii `/ainpc bounty definitions`, `/ainpc bounty nearest`, `/ainpc bounty status/progress/track`, `/ainpc bounty stored` si `/ainpc progression stored bounty`.
- GUI: apare in filtrul `bounty`, grupat sub mecanica `local_bounties`.

`B02 - Panza de la Marginea Fermei` verifica aceeasi mecanica cu a doua definitie:

- Mapping: foloseste `tag:farm` din demo settlement, deci bounty-ul este ancorat pe place-ul fermei, nu doar pe regiune.
- Quest/progression: ramane `BOUNTY` cu mecanica `local_bounties`, dar giver-ul este `farmer`, nu `guard`.
- Persistenta: foloseste acelasi `baseType=BOUNTY`, `mechanic=local_bounties` si `scenarioKind=hunt`, cu cooldown separat si reward diferit.
- Debug/audit: testul de anchor confirma rezolvarea `tag:farm`, iar filtrele `bounty`/`mechanic:local_bounties` listeaza ambele bounty-uri.
- Smoke: script-ul include pasii B02 prin `/ainpc bounty ...`, cu nota ca B01 trebuie completat sau abandonat daca limita `max_active` este deja ocupata.
- GUI: verifica paginarea si gruparea cand exista mai multe bounty-uri in aceeasi mecanica.

`E01 - Alarma Fantanii din Piata` adauga mecanica de eveniment local:

- Mapping: foloseste `tag:market` si node-ul semantic `quest_board` din demo settlement.
- Quest/progression: este `WORLD_EVENT` cu mecanica `village_events`, progres `kind=event` si scenariu `event`.
- Persistenta: foloseste acelasi view generic prin `ProgressionRepository`, cu `baseType=WORLD_EVENT`, `mechanic=village_events` si `scenarioKind=event`.
- Debug/audit: apare in definitiile generice, in filtrele `kind:event`/`base:WORLD_EVENT`/`mechanic:village_events` si in story events pe place.
- Smoke: script-ul include pasii `/ainpc event definitions`, `/ainpc event nearest`, `/ainpc event status/progress/track`, `/ainpc event stored` si `/ainpc progression stored event`.
- GUI: apare in filtrul `event`, cu stage-ul curent si obiectivele mapate vizibile in detalii.

`T01 - Indrumarea Avizierului` adauga mecanica de onboarding:

- Mapping: foloseste `tag:market` si node-ul semantic `quest_board`, ca tutorialul sa invete aceleasi ancore folosite de contracte si evenimente.
- Quest/progression: este `TUTORIAL` cu mecanica `onboarding`, progres `kind=tutorial` si scenariu `tutorial`.
- Persistenta: foloseste acelasi view generic prin `ProgressionRepository`, cu `baseType=TUTORIAL`, `mechanic=onboarding` si `scenarioKind=tutorial`.
- Debug/audit: apare in definitiile generice, in filtrele `kind:tutorial`/`base:TUTORIAL`/`mechanic:onboarding` si in story events pe place.
- Smoke: script-ul include pasii `/ainpc tutorial definitions`, `/ainpc tutorial nearest`, `/ainpc tutorial status/progress/track`, `/ainpc tutorial stored` si `/ainpc progression stored tutorial`.
- GUI: apare in filtrul `tutorial`, folosit ca verificare de onboarding pentru jucator.

`R01 - Luminile Vechiului Altar` adauga mecanica de ritual local:

- Mapping: foloseste noul altar din demo settlement, cu `tag:ritual` pe place si node-ul semantic `ritual_circle`.
- Quest/progression: este `RITUAL` cu mecanica `village_rituals`, progres `kind=ritual` si scenariu `ritual`.
- Persistenta: foloseste acelasi view generic prin `ProgressionRepository`, cu `baseType=RITUAL`, `mechanic=village_rituals` si `scenarioKind=ritual`.
- Debug/audit: apare in definitiile generice, in filtrele `kind:ritual`/`base:RITUAL`/`mechanic:village_rituals` si in story events pe place.
- Smoke: script-ul include pasii `/ainpc ritual definitions`, `/ainpc ritual nearest`, `/ainpc ritual status/progress/track`, `/ainpc ritual stored` si `/ainpc progression stored ritual`.
- GUI: apare in filtrul `ritual`, iar detaliile trebuie sa arate stage-ul `PREPARE/CEREMONY/RETURN` fara sa depinda de text hardcodat.

`GUI-MAP-01 - Punte World -> Progression` leaga mapping-ul curent de progresul vizibil:

- Mapping: `WorldHubGui` ramane ecranul de context pentru regiune, place, node si noduri apropiate.
- Quest/progression: acelasi ecran consuma `ProgressionService.getProgressionGuiSnapshot(...)` si citirea read-only de ancore din `ProgressionService`, nu citeste progresii direct din DB sau YAML.
- GUI: slotul de progresii deschide log-ul filtrat `active` prin click si `all` prin right click, iar slotul de ancore arata potrivirile pentru regiunea/place/node curent si ruleaza diagnosticul `/ainpc quest anchors all`.
- Debug/audit: ancorele persistate sunt accesibile din World GUI pentru cazurile in care obiectivele nu se leaga de mapping-ul semantic.
- Limita: acest pas nu introduce filtrare spatiala reala pe progressii; doar face vizibila puntea intre world context, snapshot-ul generic si diagnosticul de ancore.

`GUI-MAP-02 - Detaliu Progression -> Ancore Persistate` extinde puntea in ecranul de detalii:

- Mapping: detaliul unei progresii arata binding-urile persistate din `quest_anchor_bindings` pentru template-ul/codul progresiei selectate.
- Quest/progression: foloseste `ProgressionService`, nu query direct din GUI, ca sa pastreze progresiile generice peste quest/contract/duty/bounty/event/tutorial/ritual.
- GUI: `QuestDetailGui` are card dedicat pentru ancore; jucatorul vede read-only binding-urile proprii, obiectivele mapate afiseaza ancora persistata, iar adminul poate deschide diagnosticul text cu click.
- Persistenta: lookup-ul prefera `template_id` si cade controlat pe `quest_code` pentru progresii vechi sau template-uri mutate.
- Debug/audit: shortcut-ul admin ruleaza `/ainpc quest anchors <player> <templateId|questCode>`, iar comanda text are acelasi fallback dupa cod ca GUI-ul.
- Test: `ProgressionRepositoryTest` verifica citirea binding-urilor dupa template si fallback dupa cod de quest.

`MAP-WAND-01 - Draft manual pentru Region/Place/Node` porneste authoring-ul asistat:

- Mapping: adminul poate porni `/ainpc wand`, seta `pos1`/`pos2` sau un punct node si crea draft-uri prin `/ainpc map <region|place|node> <descriere>`.
- Quest/progression: marker-ele de tip node pot crea deja `quest_trigger` cu metadata precum `semantic=quest_board` sau `role=quest_anchor`, fara sa scrie direct in `quest_anchor_bindings`.
- GUI/comenzi: fluxul are preview text prin `/ainpc map preview`, confirmare explicita prin `/ainpc map confirm` si anulare prin `/ainpc map cancel`.
- Persistenta: confirmarea scrie in runtime-ul `WorldAdminService`; persistenta finala ramane controlata prin `/ainpc world save`.
- Debug/audit: dupa confirmare, fluxul recomandat ramane `/ainpc audit world` inainte de save.
- Limita: bind-ul NPC si quest anchor-ul persistent sunt lasate pentru slice-urile urmatoare; aici sunt pregatite ca moduri/directionare.
- Test: `MappingIntentParserTest`, `MappingDraftFactoryTest` si `AINPCTabCompleterTest` acopera parserul, aplicarea draft-ului si completarea comenzilor.

`MAP-WAND-02 - Draft NPC Bind din Wand` extinde authoring-ul asistat catre rutina NPC:

- Mapping: adminul seteaza un punct in interiorul unui `Place` si creeaza draft prin `/ainpc map npc_bind <npc|nearest> <home|work|social>`, sau foloseste modul curent `npc_bind`.
- Quest/progression: nu modifica progresii direct, dar face NPC-urile si rutina lor consumabile mai coerent de questuri, contracte si GUI.
- GUI/comenzi: acelasi flux `/ainpc map preview`, `/ainpc map confirm` si `/ainpc map cancel` arata selectorul NPC, rolul si locul tinta inainte de scriere.
- Persistenta: confirmarea salveaza profilul NPC cu ancora home/work/social, actualizeaza metadata mapping in runtime si scrie/imbina randul corespunzator in `npc_world_bindings`; persistenta finala a mapping-ului ramane prin `/ainpc world save`.
- Debug/audit: dupa confirmare, fluxul recomandat este `/ainpc world bindings`, `/ainpc audit spawn`, `/ainpc audit world` si apoi `/ainpc world save`.
- Limita: quest anchor-ul persistent direct ramane pentru slice-ul urmator, ca sa nu amestece bind-ul NPC cu scrierea in progresii.
- Test: `MappingDraftFactoryTest` acopera draft-ul `npc_bind`, iar `AINPCTabCompleterTest` verifica expunerea lui in completari.

`MAP-WAND-03 - Quest Anchor Persistent din Wand` inchide fluxul de authoring pentru ancore de progresie:

- Mapping: adminul selecteaza un punct; sistemul prefera un `Node` existent, apoi `Place`, apoi `Region` ca ancora persistenta.
- Quest/progression: draft-ul cere selector de progresie (`tracked`, `current`, `templateId` sau `questCode`) si `objective_id`, apoi confirma doar daca progresia exista in `player_quests`.
- GUI/comenzi: `/ainpc map quest_anchor [player:<jucator|uuid>] <selector> <objective_id> [objective_type] [reference]` are preview si confirmare prin acelasi flux `/ainpc map preview|confirm|cancel`.
- Persistenta: confirmarea face upsert in `quest_anchor_bindings` pe cheia `player_uuid/template_id/objective_key`, cu `quest_code`, `objective_type`, `reference`, `anchor_type`, `anchor_id` si label.
- Debug/audit: verificarea ramane `/ainpc quest anchors <player|uuid> <templateId|questCode>`, `/ainpc audit quest` si `quest-anchor-bindings.json`.
- Limita: fluxul nu porneste questuri si nu ghiceste `objective_id`; adminul trebuie sa lege o progresie deja existenta.
- Test: `MappingDraftFactoryTest` acopera draft-ul `quest_anchor`, `ProgressionRepositoryTest` acopera upsert-ul persistent, iar `AINPCTabCompleterTest` expune actiunea.

## Slice-uri documentate 2026-05-11

`PATCH-PLAN-01 - Gap/Patch Planner read-only` adauga un pas de control intre mapping si continut:

- Mapping: consuma `WorldAdminApi` pentru regiuni, places si nodes existente.
- Patch planner: `VillageGapAnalyzer` produce `GapReport`, iar `VillagePatchPlanner` produce `PatchCandidate` si `PatchPlan`.
- Quest/progression: nu porneste progresii, dar verifica daca regiunea poate sustine questuri care cer social hub, workplace-uri sau quest triggers.
- GUI/comenzi: expus initial prin `/ainpc patch analyze|plan|validate <regionId> [targetPopulation] [profesiiCSV]`.
- Persistenta: nu scrie mapping si nu persista planuri.
- Debug/audit: testele `VillagePatchPlannerTest` verifica gap-uri, planuri si mapping-ul demo.
- Limita: patch-urile `native_patch` raman blocate de lipsa capabilitatii `native-block-build`.

`STORY-QUEST-01 - Story-driven selection ca regula, nu executie` clarifica relatia dintre story si quest:

- Story: `StoryStateService` pastreaza flags/events; poate exista fara quest.
- Quest/progression: questurile pot exista fara story; `ProgressionService` ramane proprietarul progresului.
- QuestDirector: strat read-only initial care transforma `StoryContextSnapshot` si definitii de progres in `QuestDirectorDecision`.
- Mapping: `QuestAnchorResolver` ramane obligatoriu pentru legarea de regiuni, places, nodes si NPC-uri reale.
- GUI: afiseaza context si efecte, dar nu decide progres.
- Debug/audit: `QuestDirectorTest`, `debugdump story`, `debugdump quest` si auditul trebuie sa explice atat blocajele narative, cat si cele tehnice.
- Limita: story-ul nu scrie direct in `player_quests`, iar AI-ul nu creeaza quest live.

`AI-QUEST-DOC-01 - QuestSeed/QuestDraft` pregateste authoring-ul asistat:

- Mapping: AI-ul primeste doar semantic index si snapshot-uri compacte, nu harta bruta.
- Story: seed-ul poate porni din story context, dar ramane draft.
- Quest/progression: `QuestDraft` trebuie validat inainte sa devina YAML dezactivat intr-un pack.
- GUI/comenzi: comenzile propuse sunt admin-only si orientate pe inspect/validate/export, nu activare live.
- Persistenta: drafturile pot incepe ca fisiere in `plugins/AINPC/drafts/quest/`, apoi pot primi tabel dedicat.
- Debug/audit: fiecare request AI trebuie auditat fara secrete, iar exportul trece prin `/ainpc audit quest strict`.
- Limita: AI-ul nu acorda reward-uri, nu modifica story/progres si nu inventeaza obiective fara listener.

## Urmatoarele 100 de faze recomandate

Aceste faze sunt intentionat mici. Fiecare faza trebuie sa lase in urma cel putin o verificare: test automat, comanda Paper rulabila, audit, debugdump sau actualizare GUI.

1. F001 - Finalizat 2026-05-10: ruleaza smoke Paper pentru startup, `/plugins`, `/ainpc`, `/ainpc audit all` si shutdown curat.
2. F002 - Finalizat 2026-05-10: documenteaza rezultatul smoke Paper in `debugging-si-testare.md`, cu data, versiune Java si versiune Paper.
3. F003 - Finalizat 2026-05-10: ruleaza smoke Paper pentru `/ainpc world demo create demo_sat`, audit world si save.
4. F004 - Finalizat 2026-05-10: verifica dupa restart ca `demo_sat`, places si nodes raman incarcate.
5. F005 - Finalizat 2026-05-10: ruleaza `settlement plan` pe `demo_sat` si salveaza raportul de planificare.
6. F006 - Finalizat 2026-05-10: ruleaza `settlement spawn` cu limita mica, de exemplu 2 case, si verifica rollback-ul.
7. F007 - Finalizat 2026-05-10: ruleaza `settlement spawn` complet si verifica `npc_world_bindings`.
8. F008 - Finalizat 2026-05-10: verifica rutina NPC dupa spawn: home, work, social si slot curent in GUI.
9. F009 - Finalizat 2026-05-10: verifica pathfinding Paper catre ancore si noteaza cazurile unde cade pe teleport fallback.
10. F010 - Finalizat 2026-05-10: ruleaza `debugdump world` si confirma ca mapping-ul, bindings si households apar coerent.
11. F011 - Ruleaza smoke manual pentru `/ainpc wand mode region` pe o zona mica.
12. F012 - Ruleaza smoke manual pentru `/ainpc wand mode place` intr-o regiune existenta.
13. F013 - Ruleaza smoke manual pentru `/ainpc wand mode node` cu `quest_board`.
14. F014 - Ruleaza smoke manual pentru `/ainpc map preview|confirm|cancel` pe fiecare tip de draft.
15. F015 - Verifica erorile de selectie invalida: fara regiune, fara place, fara point.
16. F016 - Finalizat 2026-05-10: adauga audit pentru draft-uri wand confirmate recent, daca logul operational nu este suficient.
17. F017 - Finalizat 2026-05-10: adauga preview vizual cu particule pentru bounds region/place.
18. F018 - Finalizat 2026-05-10: adauga preview vizual cu particule pentru node radius.
19. F019 - Finalizat 2026-05-10: adauga comanda de inspectie pentru ultima selectie wand a jucatorului.
20. F020 - Finalizat 2026-05-10: adauga optiune de reset partial pentru `pos1`, `pos2` sau `point`.
21. F021 - Ruleaza smoke pentru `npc_bind nearest home` peste o casa.
22. F022 - Ruleaza smoke pentru `npc_bind nearest work` peste fierarie/ferma/shop.
23. F023 - Ruleaza smoke pentru `npc_bind nearest social` peste piata/taverna.
24. F024 - Verifica dupa restart ca profilul NPC pastreaza ancorele home/work/social.
25. F025 - Verifica dupa restart ca `npc_world_bindings` pastreaza place/node IDs.
26. F026 - Finalizat 2026-05-10: adauga audit pentru divergenta intre profil NPC si `npc_world_bindings`.
27. F027 - Finalizat 2026-05-10: adauga reparare dry-run pentru divergenta profil NPC -> `npc_world_bindings`.
28. F028 - Finalizat 2026-05-10: adauga reparare dry-run pentru divergenta `npc_world_bindings` -> metadata mapping.
29. F029 - Finalizat 2026-05-10: adauga sumar in GUI Routine pentru place/node IDs persistate.
30. F030 - Finalizat 2026-05-10: adauga shortcut din Routine GUI catre `/ainpc world bindings npc <id>`.
31. F031 - Ruleaza smoke pentru `quest_anchor tracked <objective_id>` pe node.
32. F032 - Ruleaza smoke pentru `quest_anchor current <objective_id>` pe place.
33. F033 - Ruleaza smoke pentru `quest_anchor <templateId> <objective_id>` pe region.
34. F034 - Ruleaza smoke pentru `quest_anchor player:<nume> <questCode> <objective_id>`.
35. F035 - Verifica dupa restart ca `quest_anchor_bindings` ramane si apare in `/ainpc quest anchors`.
36. F036 - Verifica `QuestDetailGui` dupa manual anchor, cu lore pe obiectivul mapat.
37. F037 - Finalizat 2026-05-10: adauga validare mai stricta pentru `objective_id` fata de definitia progresiei.
38. F038 - Finalizat 2026-05-10: adauga sugestii de `objective_id` in tab completion pentru progresia tracked.
39. F039 - Adauga optiune de listare obiective mapabile pentru o progresie selectata.
40. F040 - Adauga rollback controlat pentru inlocuirea unui quest anchor manual gresit.
41. F041 - Ruleaza Q01 cap-coada pe Paper: oferta, acceptare, progres, completare, reward.
42. F042 - Ruleaza Q02 cap-coada pe Paper si verifica persistenta dupa restart.
43. F043 - Ruleaza Q03 cap-coada pe Paper si verifica abandon/reacceptare.
44. F044 - Ruleaza Q04 cap-coada pe Paper si verifica edge cases de inventar.
45. F045 - Ruleaza Q05 cap-coada pe Paper si verifica tracking marker.
46. F046 - Ruleaza Q06 pe mapping demo cu `visit_place` si `inspect_node`.
47. F047 - Ruleaza Q07 delivery/social si verifica NPC secundar.
48. F048 - Ruleaza Q08 hunt si verifica `visit_region`, combat si raportare.
49. F049 - Verifica `quest log`, `quest status`, `quest progress` pentru Q01-Q08.
50. F050 - Verifica `quest anchors` pentru toate questurile active dupa restart.
51. F051 - Ruleaza C01 ca progres non-quest prin `/ainpc contract`.
52. F052 - Ruleaza C02 pe mapping demo cu piata si `quest_board`.
53. F053 - Ruleaza D01 prin `/ainpc duty` si verifica status/progress/stored.
54. F054 - Ruleaza B01 prin `/ainpc bounty` si verifica story event regional.
55. F055 - Ruleaza B02 prin `/ainpc bounty` si verifica `tag:farm`.
56. F056 - Ruleaza E01 prin `/ainpc event` si verifica story event pe place.
57. F057 - Ruleaza T01 prin `/ainpc tutorial` si verifica onboarding GUI.
58. F058 - Ruleaza R01 prin `/ainpc ritual` si verifica altarul demo.
59. F059 - Verifica limitele `max_active` intre questuri si mecanici non-quest.
60. F060 - Verifica `progression stored all` dupa toate mecanicile rulate.
61. F061 - Extinde `ProgressionService` cu metoda read-only pentru obiectivele unei progresii stocate.
62. F062 - Muta formatarile comune de status/progress din comenzi in snapshot-uri reutilizabile.
63. F063 - Adauga selector comun pentru `tracked/current/templateId/questCode` reutilizat de comenzi si GUI.
64. F064 - Adauga API read-only pentru anchors pe obiectiv, nu doar pe progresie.
65. F065 - Adauga sumar generic pentru progresii curente pe regiune/place/node.
66. F066 - Adauga cautare de progresii dupa anchor `region/place/node/npc`.
67. F067 - Adauga cache scurt pentru definitii progression, invalidat la reload.
68. F068 - Adauga validare pentru definitii duplicate dupa `mechanic:definition`.
69. F069 - Adauga raport de compatibilitate pentru progresii fara definitie incarcata.
70. F070 - Pregateste contractul pentru viitoare tabela `player_progressions`, fara migrare activa.
71. F071 - Extinde Quest GUI cu grupare mai clara pe tracked/current/offered.
72. F072 - Extinde Quest Detail GUI cu actiuni rapide pentru track/status/debug.
73. F073 - Extinde Quest Detail GUI cu lista completa de anchors pe obiective.
74. F074 - Extinde World GUI cu progresii active legate de place-ul curent.
75. F075 - Extinde World GUI cu progresii active legate de node-ul curent.
76. F076 - Extinde Routine GUI cu legaturi catre home/work/social place info.
77. F077 - Adauga GUI read-only pentru `npc_world_bindings`.
78. F078 - Adauga GUI read-only pentru `quest_anchor_bindings`.
79. F079 - Adauga ecran GUI de audit compact pentru world/quest/db/spawn.
80. F080 - Adauga fallback text pentru fiecare actiune GUI noua.
81. F081 - Extinde `/ainpc audit world` cu validare pentru suprapuneri suspecte de places.
82. F082 - Extinde `/ainpc audit world` cu validare pentru nodes in afara containerului.
83. F083 - Finalizat 2026-05-11: extinde `/ainpc audit quest` cu validare de `objective_id` fata de template si audit strict pentru toate `quest_anchor_bindings`.
84. F084 - Extinde `/ainpc audit quest` cu raport pentru anchors manuale vs anchors rezolvate automat.
85. F085 - Extinde `/ainpc audit db` cu validare pentru timestamps si randuri orfane.
86. F086 - Extinde `debugdump world` cu ultima versiune de semantic index.
87. F087 - Extinde `debugdump quest` cu rezumat pe obiective active si anchors.
88. F088 - Extinde `debugdump story` cu legaturi catre progresii care au scris story state.
89. F089 - Adauga raport scurt de smoke test generat automat din debugdump.
90. F090 - Adauga checklist pentru compararea starii inainte/dupa restart.
91. F091 - Adauga generator narativ minim pentru nume si roluri pe regiune.
92. F092 - Leaga generatorul narativ de `HouseAllocation` fara sa schimbe spawn-ul existent.
93. F093 - Adauga distributie determinista pe familii pentru case.
94. F094 - Adauga distributie determinista pe work places dupa ocupatie.
95. F095 - Adauga distributie determinista pe social places dupa regiune.
96. F096 - Expune planul narativ in comanda dry-run inainte de spawn.
97. F097 - Scrie planul narativ in debugdump fara sa modifice DB.
98. F098 - Ruleaza smoke Paper pentru populatie generata pe regiune mica.
99. F099 - Stabileste criteriile de release `paper-test` pentru mapping/progression/gui.
100. F100 - Marcheaza demo-ul ca matur doar dupa smoke Paper complet, restart, audit, debugdump si raport documentat.

## Continuare dupa F100

Aceste faze continua aceeasi alternanta, dar introduc explicit patch planner, story-driven selection si authoring AI.

101. F101 - Ruleaza `/ainpc patch analyze demo_sat 6 blacksmith,farmer,merchant,innkeeper` si documenteaza daca mapping-ul demo are gap-uri.
102. F102 - Ruleaza `/ainpc patch plan demo_sat 8 ...` si confirma ca patch-urile native sunt blocate de capabilitate lipsa, nu aplicate.
103. F103 - Finalizat 2026-05-11: adauga raport de smoke pentru Patch Planner in `debugging-si-testare.md`.
104. F104 - Finalizat 2026-05-11: creeaza criterii pentru `QuestDirector`: input `StoryContextSnapshot`, output template candidat sau `seed_suggested`.
105. F105 - Finalizat 2026-05-11: defineste model read-only `QuestDirectorDecision`, fara executie.
106. F106 - Adauga audit pentru decizia `QuestDirector`: de ce a ales sau respins un quest.
107. F107 - Adauga caz explicit story fara quest: story state vizibil in `/ainpc story region`, fara progres nou.
108. F108 - Adauga caz explicit quest fara story: tutorial/duty fara `record_story_event`.
109. F109 - Adauga caz quest cu story: completarea scrie `record_story_event` si apare in `debugdump story`.
110. F110 - Documenteaza matricea `no_story` / `writes_story` / `story_driven` in exemplele de quest.
111. F111 - Creeaza modelele `QuestSeed` si `QuestDraft` ca structuri interne, fara AI provider.
112. F112 - Creeaza validator de schema pentru `QuestDraft`, fara runtime execution.
113. F113 - Creeaza validator de mapping pentru `QuestDraft` peste semantic index.
114. F114 - Creeaza validator de reward/story actions pentru `QuestDraft`.
115. F115 - Adauga export YAML dezactivat pentru un `QuestDraft` valid.
116. F116 - Adauga audit quest peste exportul YAML dezactivat.
117. F117 - Adauga debugdump pentru drafturi AI fara secrete.
118. F118 - Adauga GUI/admin read-only pentru inspectia unui draft.
119. F119 - Adauga smoke Paper pentru un quest exportat din draft si activat manual pe server de test.
120. F120 - Abia dupa F111-F119, leaga `AIOrchestrationService` la use case-ul `QUEST_DRAFT`.

## Continuare story dupa F120

Aceste faze trateaza story-ul ca fir principal al alternantei, nu ca efect secundar al questurilor.

121. F121 - Finalizat 2026-05-11: creeaza raport de smoke pentru `story_only`: story state/event exista, dar `player_quests` ramane neschimbat.
122. F122 - Finalizat 2026-05-11: creeaza raport de smoke pentru `quest_only`: progresia merge cap-coada fara `story_events` noi.
123. F123 - Finalizat 2026-05-11: creeaza raport de smoke pentru `writes_story`: completarea produce exact story event-ul declarat.
124. F124 - Finalizat 2026-05-11: adauga audit pentru contradictii story/progression: progresie completata cu `record_story_event`, dar fara `story_event` asociat detectabil.
125. F125 - Finalizat 2026-05-11: adauga debugdump cross-link in `story-events.json`: story event -> progresie/quest/action care l-a scris, cand exista.
126. F126 - Finalizat 2026-05-11: adauga snapshot read-only pentru story in GUI/admin: region state, place state, ultimele evenimente.
127. F127 - Finalizat 2026-05-11: adauga fallback text pentru GUI story: comanda echivalenta `/ainpc story ...`.
128. F128 - Finalizat 2026-05-11: adauga matrice de permisiuni pentru actiuni story: read-only, write-admin, write-runtime.
129. F129 - Finalizat 2026-05-11: adauga validare pack pentru `set_story_state` si `record_story_event`: scope, target, event key, payload minim.
130. F130 - Finalizat 2026-05-11: adauga test pentru `QuestDirector` care primeste story demand si preferinta de mecanica, dar ramane `runtimeExecutable=false`.
131. F131 - Finalizat 2026-05-11: adauga caz story local fara NPC tinta: eveniment pe place folosit doar in dialog/context.
132. F132 - Finalizat 2026-05-11: adauga caz story regional: event pe regiune vizibil pentru mai multe places.
133. F133 - Finalizat 2026-05-11: adauga caz story blocant: `QuestDirectorDecision.blocked` explica story condition lipsa.
134. F134 - Finalizat 2026-05-11: adauga sectiune in smoke report pentru prioritatea `Progression > Story > Memory > AI text`.
135. F135 - Marcheaza story lane ca matur doar dupa reload, debugdump, audit, GUI read-only si cel putin un caz story-only verificat.

## Gate pentru demo playable matur

Un demo intern este matur doar cand:

- exista un sat demo verificat dupa restart;
- exista 3-5 questuri sau contracte completabile;
- mapping-ul si NPC bindings sunt coerente;
- patch planner-ul poate explica lipsurile de infrastructura fara sa modifice lumea;
- story state-ul poate exista independent si poate fi inspectat fara sa porneasca questuri;
- quest anchors si player progress raman dupa reload;
- Quest/Progression GUI afiseaza corect questuri, contracte, duty-uri, bounty-uri, evenimente, tutoriale si ritualuri;
- World/Routine GUI explica locul si rutina fara sa inlocuiasca auditul;
- `audit` si `debugdump` pot explica mapping, quest/progression si story state;
- orice continut AI ramane draft validat/exportat controlat, nu runtime live automat;
- nu este nevoie de editare manuala in DB ca demo-ul sa mearga.

## Legaturi canonice

- `mapping.md` pentru modelul `WorldRegion -> WorldPlace -> WorldNode`;
- `npc-world-bindings.md` pentru legaturi NPC -> home/work/social;
- `patch-planner.md` pentru `GapReport`, `PatchPlan` si verificare read-only a lipsurilor de sat;
- `questuri-avansate-v2.md` pentru obiective, stages, diversitate si evolutia questurilor;
- `quest-anchor-bindings.md` pentru ancore semantice persistente;
- `progression-service.md` pentru runtime generic de progres;
- `story-si-context-ai.md` pentru story-driven quest selection si context AI;
- `generare-automata-questuri-ai.md` pentru `QuestSeed`, `QuestDraft`, validare si export controlat;
- `gui-interfete.md` pentru GUI system peste quest, progression, world, NPC, audit si debug;
- `debugging-si-testare.md` pentru smoke tests, audit si debugdump.
