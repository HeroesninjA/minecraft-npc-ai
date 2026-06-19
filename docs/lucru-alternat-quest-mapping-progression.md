# Lucru Alternat: Quest, Mapping, Story, ProgressionService si GUI

Actualizat: 2026-06-18

## Navigare rapida

- `docs/mapping.md`
- `docs/npc-world-bindings.md`
- `docs/questuri-avansate-v2.md`
- `docs/quest-anchor-bindings.md`
- `docs/progression-service.md`
- `docs/story-si-context-ai.md`
- `docs/generare-automata-questuri-ai.md`
- `docs/gui-interfete.md`
- `docs/debugging-si-testare.md`
- `docs/patch-planner.md`

## Stare curenta

- [x] `QuestLogGui`, `QuestDetailGui` si `QuestAuthoringGui` expun diagnostice read-only pentru progresie si authoring.
- [x] `WorldHubGui`, `StoryGui`, `DebugGui` si `MainHubGui` expun carduri compacte pentru mapping, story, build si version snapshot.
- [x] `debugdump quest`, `debugdump story`, `debugdump mapping`, `debugdump npc`, `debugdump ai` si `/npc version` sunt conectate.
- [x] `build-info.properties` si `BuildVersionInfo` expun versiunea, hash-ul si timestamp-ul ultimului build.
- [x] Documentele canonice au linkuri inverse si documentul coordonator are navigare rapida.
- [ ] `DebugGui` poate primi inca shortcut-uri suplimentare pentru audit sau history, daca apar necesitati simple.

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

## Documente conectate

Acest document este un strat de coordonare. Implementarea reala si regulile detaliate vin din documentele canonice de mai jos:

| Document | Rol in aceasta documentatie |
|---|---|
| `docs/mapping.md` | baza pentru regiuni, places, nodes si ancorele semantice folosite de questuri |
| `docs/npc-world-bindings.md` | sursa pentru home/work/social bindings si validarea rutinei NPC |
| `docs/questuri-avansate-v2.md` | defineste structura questurilor, obiectivele, stages si diversitatea gameplay-ului |
| `docs/quest-anchor-bindings.md` | ofera contractul pentru ancorele persistente ale progresiilor |
| `docs/progression-service.md` | descrie runtime-ul generic care unifica quest, contract, duty, bounty, event, tutorial si ritual |
| `docs/story-si-context-ai.md` | conecteaza story context cu selectia de quest si cu efectele narative |
| `docs/generare-automata-questuri-ai.md` | limitele pentru `QuestSeed`, `QuestDraft`, validare si export controlat |
| `docs/gui-interfete.md` | contractul pentru ecranele GUI peste quest, progression, world, NPC, audit si debug |
| `docs/debugging-si-testare.md` | smoke tests, audit si debugdump folosite ca verificare pentru fiecare slice |
| `docs/patch-planner.md` | analiză read-only a gap-urilor de mapping înainte de a construi conținut nou |

Folosire practica:

- cand lipsește maparea, citește `docs/mapping.md` și `docs/patch-planner.md`;
- cand lipsește contextul narativ, citește `docs/story-si-context-ai.md`;
- cand trebuie unificată logica de progres, citește `docs/progression-service.md` și `docs/questuri-avansate-v2.md`;
- cand trebuie vizibilitate în UI, citește `docs/gui-interfete.md`;
- cand trebuie validare sau smoke, citește `docs/debugging-si-testare.md`.

## Stare implementata

Pana la acest punct, alternanta a fost conectata si in cod prin:

- `QuestLogGui`, `QuestDetailGui` si `QuestAuthoringGui` pentru progresie, selectie si authoring read-only;
- `WorldHubGui`, `StoryGui`, `DebugGui` si `MainHubGui` pentru navigare, mapping, story, debug si shortcut-uri;
- `debugdump quest`, `debugdump story`, `debugdump mapping`, `debugdump npc`, `debugdump ai` si `/npc version` pentru observabilitate directa;
- metadata de build expusa prin `build-info.properties`, `BuildVersionInfo` si carduri compacte in GUI;
- documentele canonice legate in ambele sensuri prin secțiunea de documente conectate.

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

`AI-QUEST-DOC-02 - Quest authoring read-only, GUI si dump` documenteaza partea operativa:

- Mapping: selectorul de quest si mecanica sunt tratate ca selectie explicita, nu ca sursa de executie.
- Story: snapshot-ul de authoring expune contextul narativ, dar ramane doar lectura.
- Quest/progression: `QuestAuthoringService` si `QuestAuthoringSnapshot` sunt stratul de inspectie pentru `QuestSeed` si `QuestDraft`.
- GUI/comenzi: `/ainpc authoring`, `next`, `prev`, `clear`, `reset` si `dump` folosesc acelasi flow, iar GUI-ul retine selecția per-player.
- Persistenta: starea authoring este temporara pe jucator; nu exista inca persistenta dedicata pentru draft-uri validate.
- Debug/audit: `/ainpc debugdump authoring` expune acelasi snapshot in text pentru review rapid.
- Limita: fluxul este intentionat read-only; nu creeaza, nu publica si nu activeaza questuri live.

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
61. F061 - Finalizat 2026-06-18: extinde `ProgressionService` cu metoda read-only pentru obiectivele unei progresii stocate.
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
97. F097 - Finalizat 2026-06-18: scrie planul narativ in debugdump fara sa modifice DB.
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

## Continuare dupa F135

Aceasta continuare pastreaza alternanta dintre mapping, story, quest/progression, `ProgressionService` si GUI. Fiecare faza trebuie sa ramana mica si verificabila pe Paper sau prin audit/read-only.

136. F136 - Definește un `ProgressionGuiSnapshot` comun pentru quest, contract, duty, bounty, event, tutorial și ritual.
137. F137 - Grupează progresiile în GUI după `kind` și `mechanic`, cu comportament identic între text și ecran.
138. F138 - Extragă un strat read-only pentru definițiile generice de progresie, reutilizat de comenzi și GUI.
139. F139 - Standardizează selectorii `tracked`, `current`, `templateId` și `questCode` într-un singur flux de rezolvare.
140. F140 - Adaugă sumar generic pentru progresii active, tracked, arhivate și fără definiție încărcată.
141. F141 - Extinde `QuestDetailGui` cu stage, objectives, anchors și story effects declarate.
142. F142 - Adaugă puntea dintre `WorldHubGui` și progresiile active ale contextului curent de regiune sau place.
143. F143 - Integrează în `RoutineGui` home/work/social bindings și scurtătura către diagnosticul de binding.
144. F144 - Întărește `/ainpc audit quest` cu verificări stricte pentru `objective_id`, template și bindings persistente.
145. F145 - Extinde `debugdump story` cu lanțul de evenimente și motivul pentru care un quest a fost sau nu a fost creat.
146. F146 - Adaugă `debugdump progression` cu statistici pe kind, mecanică și stare de persistență.
147. F147 - Standardizează lookup-ul de ancore pe `region`, `place`, `node` și `npc`, cu fallback explicat în audit.
148. F148 - Verifică fluxul story-driven printr-un NPC interaction path care selectează un quest existent, nu un draft AI.
149. F149 - Introdu un caz `story_only` vizibil în story UI și debugdump, dar absent din quest log și progression.
150. F150 - Introdu un caz `writes_story` în care completarea questului scrie un eveniment narativ explicit și auditabil.
151. F151 - Confirmă un caz `quest_only` pentru tutorial sau duty, fără efecte narative și fără `story_events` noi.
152. F152 - Rulează un `GapReport` focalizat pe quest triggers, social hubs, workplaces și entrance nodes pentru o regiune demo.
153. F153 - Leagă `PatchPlanner` de validarea ancorelor lipsă, dar păstrează totul read-only până există builder stabil.
154. F154 - Adaugă un ecran admin de audit compact care agregă mapping, quest/progression, story și binding diagnostics.
155. F155 - Extinde exportul generic în JSON și validează că poate fi citit după reload fără pierderi de semnificație.
156. F156 - Extrage citirea read-only a progresiilor curente și tracked într-un strat comun între `ProgressionService` și GUI.
157. F157 - Leagă selecția de quest de `StoryContextSnapshot` și documentează clar rezultatele `candidate_found`, `seed_suggested` și `blocked`.
158. F158 - Verifică smoke-ul complet după restart pentru mapping, quest, story și GUI, cu raport standardizat.
159. F159 - Închide ciclul cu sincronizarea TODO/docs/changelog, astfel încât faza următoare să pornească dintr-o stare documentată și verificabilă.
160. F160 - Pregătește următorul pachet de alternanță pentru extinderea `ProgressionService` fără a rupe contractele deja validate.

## Continuare dupa F160

Seria următoare păstrează aceeași regulă: slice mic, legat de mapping, story, progresie și GUI, cu o verificare clară la final.

161. F161 - Definește un contract read-only pentru progresii pe regiune și place, fără query direct din DB în GUI.
162. F162 - Adaugă un sumar de progresie pe NPC, astfel încât home/work/social să poată fi urmărite în contextul rutinei.
163. F163 - Grupează progresiile active după ancoră și mecanică, cu aceeași ordine în CLI, audit și GUI.
164. F164 - Extinde `ProgressionService` cu un API de lookup pentru obiectivele curente ale unei progresii.
165. F165 - Standardizează statusul unei progresii la nivel de snapshot: offered, active, tracked, completed, archived.
166. F166 - Leagă `QuestDetailGui` de snapshot-ul generic de progresie, fără fallback la citiri ad-hoc din YAML.
167. F167 - Adaugă afișare explicită pentru reward și story effects în detaliul de progresie, doar dacă sunt declarate.
168. F168 - Extinde `WorldHubGui` cu progresiile apropiate de regiunea curentă, dar doar ca prezentare read-only.
169. F169 - Adaugă un shortcut GUI către `/ainpc progression stored` pentru debug rapid și comparare cu snapshot-ul afișat.
170. F170 - Întărește auditul pentru progresii fără definiție încărcată și explică clar dacă lipsa este de mapping sau de registry.
171. F171 - Adaugă audit pentru ancore duplicate pe aceeași progresie și fă diferența între binding intenționat și coliziune.
172. F172 - Standardizează `objective_id` în toate rapoartele, GUI-urile și dump-urile pentru aceeași progresie.
173. F173 - Verifică fluxul story-driven când aceeași stare narativă poate produce mai multe candidate quests, dar doar unul este expus.
174. F174 - Adaugă o regulă de blocare pentru `QuestDirector` când un quest candidat ar încălca cooldown, max active sau constraints de mapping.
175. F175 - Introdu un caz testabil pentru `seed_suggested`, unde output-ul rămâne draft și nu pornește nimic live.
176. F176 - Adaugă un caz testabil pentru `no_action`, unde story-ul este valid, dar nu cere quest sau progresie.
177. F177 - Adaugă un caz testabil pentru `blocked`, unde lipsa de ancore sau de context este explicată în output și în debugdump.
178. F178 - Extinde `debugdump quest` cu motivul rezolvării unei ancore, inclusiv fallback-ul folosit.
179. F179 - Extinde `debugdump world` cu legătura dintre place, node și progresiile care îl consumă.
180. F180 - Adaugă o vedere de audit pentru `player-progressions.json`, astfel încât exportul să poată fi verificat fără deschidere manuală.
181. F181 - Leagă `PatchPlanner` de un raport scurt în GUI admin, nu doar de comenzi text, pentru gaps de mapping.
182. F182 - Adaugă un smoke flow pentru `quest_only` după reload, cu verificare explicită că story-ul rămâne neschimbat.
183. F183 - Adaugă un smoke flow pentru `writes_story` după reload, cu verificare explicită a evenimentului narativ persistat.
184. F184 - Adaugă un smoke flow pentru `story_only` după reload, cu verificare explicită că nu apare progresie nouă.
185. F185 - Închide această etapă cu un raport sintetic care compară mapping, story și progression înainte și după restart.

## Continuare dupa F185

Seria aceasta păstrează părțile mici și verificabile. Fiecare fază trebuie să confirme un singur contract clar, nu să deschidă o migrare mare.

186. F186 - Definește un snapshot comun pentru `QuestDirectorDecision` și expune-l read-only în debug și GUI admin.
187. F187 - Adaugă un raport compact pentru candidate quests, astfel încât să se poată vedea de ce a fost ales sau respins fiecare candidat.
188. F188 - Standardizează modul în care `blocked` explică lipsa de mapping, lipsa de story sau conflictul de cooldown.
189. F189 - Leagă `QuestDirector` de un set minim de teste deterministe pentru `candidate_found`, `seed_suggested`, `no_action` și `blocked`.
190. F190 - Extinde `StoryContextSnapshot` cu surse explicite pentru regiune, place și event chain, fără a introduce scriere automată.
191. F191 - Adaugă un ecran story-readonly în GUI care arată context, sursă și ultimul efect narativ, fără editare.
192. F192 - Integrează `QuestDetailGui` cu story links explicite, astfel încât efectele narative declarate să poată fi urmărite rapid.
193. F193 - Adaugă un tab de audit pentru mapping gaps care separă lipsa de place, node, NPC binding și quest anchor.
194. F194 - Extinde `debugdump world` cu lista de noduri relevante pentru questing și indicatorul de acoperire semantică.
195. F195 - Adaugă un raport de consistență pentru `npc_world_bindings` și `quest_anchor_bindings` în aceeași execuție de audit.
196. F196 - Standardizează exportul pentru progresii stocate, astfel încât `player-progressions.json` să includă `kind`, `mechanic` și `status`.
197. F197 - Adaugă un comparator read-only între snapshot-ul GUI și exportul JSON, pentru a detecta discrepanțe fără editare manuală.
198. F198 - Refactorizează citirea progresiilor curente într-un API comun pentru quest, contract, duty, bounty, event, tutorial și ritual.
199. F199 - Adaugă un flow de validare pentru obiective cu multiple ancore, cu fallback clar și ordonat.
200. F200 - Verifică un quest complet care trece prin două ancore diferite, dar produce un singur progres final valid.
201. F201 - Adaugă un smoke pentru progresii cu reward declarate, astfel încât reward-ul să fie observabil după finalizare și reload.
202. F202 - Adaugă un smoke pentru progresii fără reward, dar cu efect story, și documentează clar comportamentul așteptat.
203. F203 - Standardizează mesajele de eroare pentru `QuestAnchorResolver`, astfel încât să indice sursa lipsă și acțiunea sigură următoare.
204. F204 - Extinde `PatchPlanner` cu un rezumat de impact pentru questuri care cer social hub, work node sau entrance node.
205. F205 - Adaugă un raport de diferență între mapping-ul necesar și mapping-ul existent pentru un quest seed ales manual.
206. F206 - Leagă `ProgressionService` de un test de regresie pentru selectorii `tracked` și `current` după reload.
207. F207 - Adaugă o verificare GUI pentru filtrarea după mecanică, astfel încât aceeași listă să apară identic în `quest`, `contract` și `progression`.
208. F208 - Integrează `RoutineGui` cu diagnosticul de binding și cu un shortcut la `npc_world_bindings` pentru NPC-ul selectat.
209. F209 - Adaugă o verificare de coerență între `debugdump story` și `debugdump quest` pentru questurile care scriu evenimente narative.
210. F210 - Închide seria cu un raport de stare care spune clar ce părți sunt gata pentru următoarea extracție sau extindere.

## Continuare dupa F210

Seria următoare rămâne strict incrementală: un contract nou sau o verificare nouă pe fază, fără să deschidă mutări mari de runtime.

211. F211 - Definește un snapshot de diagnostic pentru progresiile fără definiție încărcată și expune-l în audit.
212. F212 - Adaugă un raport de consistență între `ProgressionService` și GUI pentru statusurile `offered`, `active`, `tracked`, `completed` și `archived`.
213. F213 - Extinde `QuestDetailGui` cu un indicator clar pentru ce ancoră a fost rezolvată și ce fallback a fost folosit.
214. F214 - Integrează story links în GUI-ul de detaliu pentru a diferenția între efect narativ, context narativ și lipsă de story.
215. F215 - Adaugă un ecran admin read-only pentru `QuestDirectorDecision`, cu motive, warnings și candidate template IDs.
216. F216 - Standardizează raportarea candidaților respinși când lipsesc `region`, `place`, `node` sau `npc` ancore.
217. F217 - Leagă `StoryContextService` de un test determinist care confirmă că contextul nu scrie progresie.
218. F218 - Adaugă un smoke flow pentru `story_driven` în care același story state produce un quest candidat fără activare live.
219. F219 - Extinde `debugdump story` cu explicarea exactă a lanțului care a generat candidatul sau blocarea.
220. F220 - Adaugă un raport de diferență pentru quest seeds între mapping-ul cerut și mapping-ul disponibil în regiunea demo.
221. F221 - Integrează `PatchPlanner` cu un mesaj scurt în GUI-ul de audit, astfel încât gap-urile să fie vizibile fără comenzi text.
222. F222 - Standardizează exportul `quest-anchor-bindings.json` și validează că aceeași ancora apare identic în audit și dump.
223. F223 - Adaugă un test de regresie pentru selectoarele de progresie `templateId` și `questCode` după reload și rename intern.
224. F224 - Extinde filtrarea GUI pentru mecanici astfel încât `quest`, `contract`, `duty`, `bounty`, `event`, `tutorial` și `ritual` să rămână aliniate.
225. F225 - Adaugă o verificare pentru progresii cu mai multe objective_id-uri și confirmă ordinea de rezolvare în snapshot.
226. F226 - Adaugă un smoke pentru progresii cu două etape, cu verificare explicită că stage transitions sunt stabile după reload.
227. F227 - Adaugă un raport pentru ancorele care revin prin fallback la `place` sau `region`, cu justificare în debug.
228. F228 - Standardizează mesajele de eroare pentru GUI când snapshot-ul nu găsește o progresie sau o ancoră persistentă.
229. F229 - Adaugă un audit combinat pentru `npc_world_bindings`, `quest_anchor_bindings` și `player-progressions.json` în aceeași execuție.
230. F230 - Verifică un flow complet în care `writes_story` produce și quest log, și story event, și export vizibil în debugdump.
231. F231 - Adaugă un flow complet pentru `quest_only` cu confirmare că story context rămâne doar informativ.
232. F232 - Adaugă un flow complet pentru `story_only` cu confirmare că GUI-ul arată contextul, dar nu oferă progresie.
233. F233 - Leagă `WorldHubGui` de o vedere compactă a nodurilor relevante pentru questing și a progresiilor apropiate.
234. F234 - Adaugă un raport sintetic pentru `GapReport`, `QuestDirectorDecision` și `ProgressionGuiSnapshot` în același debug bundle.
235. F235 - Închide seria cu o notă de stare care spune dacă fazele următoare pot începe extracția unui contract comun sau trebuie mai întâi întărit auditul.

236. F236 - Definește un snapshot read-only pentru progresiile fără `story_context_id` și expune-l în debugdump.
237. F237 - Adaugă un raport de aliniere între `QuestDetailGui` și `ProgressionGuiSnapshot` pentru obiectivele active.
238. F238 - Standardizează mesajele pentru ancorele rezolvate prin fallback, astfel încât GUI-ul să arate motivul exact.
239. F239 - Integrează `StoryContextService` cu un audit care confirmă că un context narativ nu creează progresii duplicate.
240. F240 - Adaugă o verificare pentru questurile care au `npc` și `place` dar lipsesc `node`, cu recomandare clară de remediere.
241. F241 - Extinde `QuestDirectorDecision` cu un rezumat compact pentru motivele de acceptare și de respingere.
242. F242 - Adaugă un smoke flow pentru `quest_only` în care story-ul rămâne informativ, dar nu influențează lista de progresii.
243. F243 - Leagă `ProgressionService` de un test de regresie pentru ordinea obiectivelor după reload și rename intern.
244. F244 - Adaugă un raport de consistență pentru mecanicile afișate în `quest`, `contract`, `duty` și `bounty`.
245. F245 - Extinde `WorldHubGui` cu o listă compactă a questurilor apropiate de NPC-ul selectat.
246. F246 - Standardizează exportul `progression-gui-snapshot.json` și validează că aceeași progresie apare identic în audit și dump.
247. F247 - Adaugă o verificare pentru `quest_anchor_bindings` care confirmă că ancorele active rămân stabile după reload.
248. F248 - Integrează `PatchPlanner` cu un rezumat vizibil în GUI-ul de audit pentru gap-urile de mapping.
249. F249 - Adaugă un smoke pentru `story_only` în care GUI-ul arată contextul narativ, dar fără call-to-action de progresie.
250. F250 - Extinde `DebugDump` cu explicarea lanțului de decizie pentru o progresie activă aleasă manual.
251. F251 - Adaugă o verificare read-only pentru progresiile `archived`, astfel încât să fie vizibile fără a fi reactivabile accidental.
252. F252 - Leagă `QuestDetailGui` de un indicator clar pentru starea `tracked` versus `current`.
253. F253 - Standardizează mesajele pentru candidații respinși când lipsesc `region` sau `place` din mapping.
254. F254 - Adaugă un raport combinat pentru `QuestDirectorDecision`, `GapReport` și `ProgressionGuiSnapshot` într-un singur bundle.
255. F255 - Introdu un test de regresie pentru selecția `templateId` după reload, cu confirmarea că rename-ul intern nu rupe snapshot-ul.
256. F256 - Adaugă un smoke flow pentru `story_driven` care confirmă diferența între context narativ și progresie activă.
257. F257 - Extinde GUI-ul de detaliu cu un mesaj scurt despre acțiunea următoare sigură atunci când o ancoră lipsește.
258. F258 - Adaugă un audit al progresiilor fără `quest_code` și indică explicit dacă problema este de mapping sau de import.
259. F259 - Leagă `ProgressionService` de un raport final care separă clar `offered`, `active`, `tracked`, `completed` și `archived`.
260. F260 - Închide seria cu o notă de stare despre ce parte poate fi extrasă într-un contract comun și ce parte mai cere întărirea auditului.

261. F261 - Definește un snapshot de diagnostic pentru questurile fără `objective_id` și expune-l în `debugdump`.
262. F262 - Adaugă un raport de aliniere între `QuestDirectorDecision` și `QuestDetailGui` pentru alegerile respinse.
263. F263 - Standardizează mesajele pentru fallback-ul de ancore astfel încât auditul să arate clar nivelul folosit.
264. F264 - Integrează `StoryContextService` cu o verificare read-only care confirmă că story-ul nu suprascrie progresia existentă.
265. F265 - Adaugă un smoke flow pentru `quest_only` cu confirmarea că obiectivele rămân vizibile după reload.
266. F266 - Extinde `ProgressionService` cu un test de regresie pentru selectoarele `current` și `tracked` după mutare internă.
267. F267 - Adaugă un raport de consistență pentru mecanicile `quest`, `contract`, `duty`, `bounty`, `event`, `tutorial` și `ritual`.
268. F268 - Leagă `WorldHubGui` de o listă compactă cu questurile apropiate de NPC-ul curent și de locurile relevante.
269. F269 - Standardizează exportul `quest-director-decision.json` și confirmă că aceeași decizie apare identic în audit și dump.
270. F270 - Adaugă o verificare pentru `quest_anchor_bindings` care confirmă că fallback-ul nu schimbă ancorele persistente.
271. F271 - Integrează `PatchPlanner` cu un mesaj scurt în GUI-ul de audit pentru gap-urile care blochează un quest seed.
272. F272 - Adaugă un smoke pentru `story_only` în care contextul narativ este inspectabil, dar nu creează acțiune de progresie.
273. F273 - Extinde `DebugDump` cu un lanț de decizie compact pentru o progresie activă și motivul ultimului update.
274. F274 - Adaugă o verificare read-only pentru progresiile `completed`, astfel încât să fie vizibile fără să afecteze starea.
275. F275 - Leagă `QuestDetailGui` de un indicator clar pentru obiectivul rezolvat și obiectivul curent.
276. F276 - Standardizează mesajele pentru candidații respinși când lipsesc `node` sau `npc` din mapping.
277. F277 - Adaugă un raport combinat pentru `GapReport`, `QuestDirectorDecision` și `ProgressionGuiSnapshot` într-un singur bundle.
278. F278 - Introdu un test de regresie pentru selecția `questCode` după reload, cu confirmarea că rename-ul intern nu rupe snapshot-ul.
279. F279 - Adaugă un smoke flow pentru `story_driven` care confirmă diferența dintre candidat narativ și progresie activă.
280. F280 - Extinde GUI-ul de detaliu cu un mesaj scurt despre acțiunea sigură următoare atunci când lipsește `place`.
281. F281 - Adaugă un audit al progresiilor fără `templateId` și indică explicit dacă problema este de mapping sau de import.
282. F282 - Leagă `ProgressionService` de un raport final care separă clar `offered`, `active`, `tracked`, `completed` și `archived` după refresh.
283. F283 - Standardizează exportul `progression-snapshot.json` și validează că aceeași progresie apare identic în audit și dump.
284. F284 - Adaugă o verificare pentru questurile cu mai multe obiective și confirmă ordinea lor stabilă după reload.
285. F285 - Închide seria cu o notă de stare despre ce parte poate fi extrasă într-un contract comun și ce parte mai cere întărirea auditului.

286. F286 - Definește un snapshot read-only pentru questurile fără `quest_code` și expune-l în `debugdump`.
287. F287 - Adaugă un raport de aliniere între `QuestDetailGui` și `ProgressionGuiSnapshot` pentru progresiile active.
288. F288 - Standardizează mesajele pentru fallback-ul de ancore astfel încât auditul să indice limpede motivul și nivelul folosit.
289. F289 - Integrează `StoryContextService` cu o verificare care confirmă că story-ul nu creează progresii duplicate după reload.
290. F290 - Adaugă un smoke flow pentru `quest_only` cu confirmarea că lista de obiective rămâne stabilă după restart.
291. F291 - Extinde `ProgressionService` cu un test de regresie pentru selectoarele `offered` și `active` după rename intern.
292. F292 - Adaugă un raport de consistență pentru mecanicile `quest`, `contract`, `duty`, `bounty`, `event`, `tutorial` și `ritual`.
293. F293 - Leagă `WorldHubGui` de o listă compactă cu questurile apropiate de NPC-ul selectat și de locurile relevante.
294. F294 - Standardizează exportul `quest-director-decision.json` și confirmă că aceeași decizie apare identic în audit și dump.
295. F295 - Adaugă o verificare pentru `quest_anchor_bindings` care confirmă că fallback-ul nu modifică ancorele persistente.
296. F296 - Integrează `PatchPlanner` cu un mesaj scurt în GUI-ul de audit pentru gap-urile care blochează un quest seed.
297. F297 - Adaugă un smoke pentru `story_only` în care contextul narativ este inspectabil, dar nu creează acțiune de progresie.
298. F298 - Extinde `DebugDump` cu un lanț de decizie compact pentru o progresie activă și motivul ultimei schimbări.
299. F299 - Adaugă o verificare read-only pentru progresiile `completed`, astfel încât să fie vizibile fără să afecteze starea.
300. F300 - Leagă `QuestDetailGui` de un indicator clar pentru obiectivul rezolvat și obiectivul curent.
301. F301 - Standardizează mesajele pentru candidații respinși când lipsesc `node` sau `npc` din mapping.
302. F302 - Adaugă un raport combinat pentru `GapReport`, `QuestDirectorDecision` și `ProgressionGuiSnapshot` într-un singur bundle.
303. F303 - Introdu un test de regresie pentru selecția `templateId` după reload, cu confirmarea că rename-ul intern nu rupe snapshot-ul.
304. F304 - Adaugă un smoke flow pentru `story_driven` care confirmă diferența dintre candidat narativ și progresie activă.
305. F305 - Extinde GUI-ul de detaliu cu un mesaj scurt despre acțiunea sigură următoare atunci când lipsește `place`.
306. F306 - Adaugă un audit al progresiilor fără `objective_id` și indică explicit dacă problema este de mapping sau de import.
307. F307 - Leagă `ProgressionService` de un raport final care separă clar `offered`, `active`, `tracked`, `completed` și `archived` după refresh.
308. F308 - Standardizează exportul `progression-snapshot.json` și validează că aceeași progresie apare identic în audit și dump.
309. F309 - Adaugă o verificare pentru questurile cu mai multe obiective și confirmă ordinea lor stabilă după reload.
310. F310 - Închide seria cu o notă de stare despre ce parte poate fi extrasă într-un contract comun și ce parte mai cere întărirea auditului.

311. F311 - Definește un snapshot read-only pentru progresiile fără `templateId` și expune-l în `debugdump`.
312. F312 - Adaugă un raport de aliniere între `QuestDetailGui` și `ProgressionGuiSnapshot` pentru progresiile complete.
313. F313 - Standardizează mesajele pentru fallback-ul de ancore astfel încât auditul să indice limpede nivelul și motivul.
314. F314 - Integrează `StoryContextService` cu o verificare care confirmă că story-ul nu rescrie o progresie deja existentă după reload.
315. F315 - Adaugă un smoke flow pentru `quest_only` cu confirmarea că obiectivele și statusurile rămân stabile după restart.
316. F316 - Extinde `ProgressionService` cu un test de regresie pentru selectoarele `tracked` și `completed` după rename intern.
317. F317 - Adaugă un raport de consistență pentru mecanicile `quest`, `contract`, `duty`, `bounty`, `event`, `tutorial` și `ritual`.
318. F318 - Leagă `WorldHubGui` de o listă compactă cu questurile apropiate de NPC-ul selectat și de locurile relevante.
319. F319 - Standardizează exportul `quest-director-decision.json` și confirmă că aceeași decizie apare identic în audit și dump.
320. F320 - Adaugă o verificare pentru `quest_anchor_bindings` care confirmă că fallback-ul nu schimbă ancorele persistente.
321. F321 - Integrează `PatchPlanner` cu un mesaj scurt în GUI-ul de audit pentru gap-urile care blochează un quest seed.
322. F322 - Adaugă un smoke pentru `story_only` în care contextul narativ este inspectabil, dar nu generează acțiune de progresie.
323. F323 - Extinde `DebugDump` cu un lanț de decizie compact pentru o progresie activă și motivul ultimei schimbări.
324. F324 - Adaugă o verificare read-only pentru progresiile `archived`, astfel încât să fie vizibile fără să afecteze starea.
325. F325 - Leagă `QuestDetailGui` de un indicator clar pentru obiectivul rezolvat și obiectivul curent.
326. F326 - Standardizează mesajele pentru candidații respinși când lipsesc `node` sau `npc` din mapping.
327. F327 - Adaugă un raport combinat pentru `GapReport`, `QuestDirectorDecision` și `ProgressionGuiSnapshot` într-un singur bundle.
328. F328 - Introdu un test de regresie pentru selecția `questCode` după reload, cu confirmarea că rename-ul intern nu rupe snapshot-ul.
329. F329 - Adaugă un smoke flow pentru `story_driven` care confirmă diferența dintre candidat narativ și progresie activă.
330. F330 - Extinde GUI-ul de detaliu cu un mesaj scurt despre acțiunea sigură următoare atunci când lipsește `place`.
331. F331 - Adaugă un audit al progresiilor fără `story_context_id` și indică explicit dacă problema este de mapping sau de import.
332. F332 - Leagă `ProgressionService` de un raport final care separă clar `offered`, `active`, `tracked`, `completed` și `archived` după refresh.
333. F333 - Standardizează exportul `progression-snapshot.json` și validează că aceeași progresie apare identic în audit și dump.
334. F334 - Adaugă o verificare pentru questurile cu mai multe obiective și confirmă ordinea lor stabilă după reload.
335. F335 - Închide seria cu o notă de stare despre ce parte poate fi extrasă într-un contract comun și ce parte mai cere întărirea auditului.

336. F336 - Definește un snapshot read-only pentru progresiile fără `questCode` și expune-l în `debugdump`.
337. F337 - Adaugă un raport de aliniere între `QuestDetailGui` și `ProgressionGuiSnapshot` pentru progresiile inactive.
338. F338 - Standardizează mesajele pentru fallback-ul de ancore astfel încât auditul să indice limpede nivelul și motivul.
339. F339 - Integrează `StoryContextService` cu o verificare care confirmă că story-ul nu rescrie progresia după reload.
340. F340 - Adaugă un smoke flow pentru `quest_only` cu confirmarea că statusurile rămân stabile după restart.
341. F341 - Extinde `ProgressionService` cu un test de regresie pentru selectoarele `active` și `archived` după rename intern.
342. F342 - Adaugă un raport de consistență pentru mecanicile `quest`, `contract`, `duty`, `bounty`, `event`, `tutorial` și `ritual`.
343. F343 - Leagă `WorldHubGui` de o listă compactă cu questurile apropiate de NPC-ul selectat și de locurile relevante.
344. F344 - Standardizează exportul `quest-director-decision.json` și confirmă că aceeași decizie apare identic în audit și dump.
345. F345 - Adaugă o verificare pentru `quest_anchor_bindings` care confirmă că fallback-ul nu schimbă ancorele persistente.
346. F346 - Integrează `PatchPlanner` cu un mesaj scurt în GUI-ul de audit pentru gap-urile care blochează un quest seed.
347. F347 - Adaugă un smoke pentru `story_only` în care contextul narativ este inspectabil, dar nu generează acțiune de progresie.
348. F348 - Extinde `DebugDump` cu un lanț de decizie compact pentru o progresie activă și motivul ultimei schimbări.
349. F349 - Adaugă o verificare read-only pentru progresiile `archived`, astfel încât să fie vizibile fără să afecteze starea.
350. F350 - Leagă `QuestDetailGui` de un indicator clar pentru obiectivul rezolvat și obiectivul curent.
351. F351 - Standardizează mesajele pentru candidații respinși când lipsesc `node` sau `npc` din mapping.
352. F352 - Adaugă un raport combinat pentru `GapReport`, `QuestDirectorDecision` și `ProgressionGuiSnapshot` într-un singur bundle.
353. F353 - Introdu un test de regresie pentru selecția `templateId` după reload, cu confirmarea că rename-ul intern nu rupe snapshot-ul.
354. F354 - Adaugă un smoke flow pentru `story_driven` care confirmă diferența dintre candidat narativ și progresie activă.
355. F355 - Extinde GUI-ul de detaliu cu un mesaj scurt despre acțiunea sigură următoare atunci când lipsește `place`.
356. F356 - Adaugă un audit al progresiilor fără `story_context_id` și indică explicit dacă problema este de mapping sau de import.
357. F357 - Leagă `ProgressionService` de un raport final care separă clar `offered`, `active`, `tracked`, `completed` și `archived` după refresh.
358. F358 - Standardizează exportul `progression-snapshot.json` și validează că aceeași progresie apare identic în audit și dump.
359. F359 - Adaugă o verificare pentru questurile cu mai multe obiective și confirmă ordinea lor stabilă după reload.
360. F360 - Închide seria cu o notă de stare despre ce parte poate fi extrasă într-un contract comun și ce parte mai cere întărirea auditului.

361. F361 - Definește un snapshot read-only pentru progresiile fără `questCode` și expune-l în `debugdump`.
362. F362 - Adaugă un raport de aliniere între `QuestDetailGui` și `ProgressionGuiSnapshot` pentru progresiile cu status `active`.
363. F363 - Standardizează mesajele pentru fallback-ul de ancore astfel încât auditul să indice limpede nivelul și motivul.
364. F364 - Integrează `StoryContextService` cu o verificare care confirmă că story-ul nu rescrie progresia după reload.
365. F365 - Adaugă un smoke flow pentru `quest_only` cu confirmarea că statusurile rămân stabile după restart.
366. F366 - Extinde `ProgressionService` cu un test de regresie pentru selectoarele `tracked` și `completed` după rename intern.
367. F367 - Adaugă un raport de consistență pentru mecanicile `quest`, `contract`, `duty`, `bounty`, `event`, `tutorial` și `ritual`.
368. F368 - Leagă `WorldHubGui` de o listă compactă cu questurile apropiate de NPC-ul selectat și de locurile relevante.
369. F369 - Standardizează exportul `quest-director-decision.json` și confirmă că aceeași decizie apare identic în audit și dump.
370. F370 - Adaugă o verificare pentru `quest_anchor_bindings` care confirmă că fallback-ul nu schimbă ancorele persistente.
371. F371 - Integrează `PatchPlanner` cu un mesaj scurt în GUI-ul de audit pentru gap-urile care blochează un quest seed.
372. F372 - Adaugă un smoke pentru `story_only` în care contextul narativ este inspectabil, dar nu generează acțiune de progresie.
373. F373 - Extinde `DebugDump` cu un lanț de decizie compact pentru o progresie activă și motivul ultimei schimbări.
374. F374 - Adaugă o verificare read-only pentru progresiile `archived`, astfel încât să fie vizibile fără să afecteze starea.
375. F375 - Leagă `QuestDetailGui` de un indicator clar pentru obiectivul rezolvat și obiectivul curent.
376. F376 - Standardizează mesajele pentru candidații respinși când lipsesc `node` sau `npc` din mapping.
377. F377 - Adaugă un raport combinat pentru `GapReport`, `QuestDirectorDecision` și `ProgressionGuiSnapshot` într-un singur bundle.
378. F378 - Introdu un test de regresie pentru selecția `templateId` după reload, cu confirmarea că rename-ul intern nu rupe snapshot-ul.
379. F379 - Adaugă un smoke flow pentru `story_driven` care confirmă diferența dintre candidat narativ și progresie activă.
380. F380 - Extinde GUI-ul de detaliu cu un mesaj scurt despre acțiunea sigură următoare atunci când lipsește `place`.
381. F381 - Adaugă un audit al progresiilor fără `story_context_id` și indică explicit dacă problema este de mapping sau de import.
382. F382 - Leagă `ProgressionService` de un raport final care separă clar `offered`, `active`, `tracked`, `completed` și `archived` după refresh.
383. F383 - Standardizează exportul `progression-snapshot.json` și validează că aceeași progresie apare identic în audit și dump.
384. F384 - Adaugă o verificare pentru questurile cu mai multe obiective și confirmă ordinea lor stabilă după reload.
385. F385 - Închide seria cu o notă de stare despre ce parte poate fi extrasă într-un contract comun și ce parte mai cere întărirea auditului.

386. F386 - Definește un snapshot read-only pentru progresiile fără `story_context_id` și expune-l în `debugdump`.
387. F387 - Adaugă un raport de aliniere între `QuestDetailGui` și `ProgressionGuiSnapshot` pentru progresiile cu status `tracked`.
388. F388 - Standardizează mesajele pentru fallback-ul de ancore astfel încât auditul să indice limpede nivelul și motivul.
389. F389 - Integrează `StoryContextService` cu o verificare care confirmă că story-ul nu rescrie progresia după reload.
390. F390 - Adaugă un smoke flow pentru `quest_only` cu confirmarea că obiectivele rămân stabile după restart.
391. F391 - Extinde `ProgressionService` cu un test de regresie pentru selectoarele `active` și `completed` după rename intern.
392. F392 - Adaugă un raport de consistență pentru mecanicile `quest`, `contract`, `duty`, `bounty`, `event`, `tutorial` și `ritual`.
393. F393 - Leagă `WorldHubGui` de o listă compactă cu questurile apropiate de NPC-ul selectat și de locurile relevante.
394. F394 - Standardizează exportul `quest-director-decision.json` și confirmă că aceeași decizie apare identic în audit și dump.
395. F395 - Adaugă o verificare pentru `quest_anchor_bindings` care confirmă că fallback-ul nu schimbă ancorele persistente.
396. F396 - Integrează `PatchPlanner` cu un mesaj scurt în GUI-ul de audit pentru gap-urile care blochează un quest seed.
397. F397 - Adaugă un smoke pentru `story_only` în care contextul narativ este inspectabil, dar nu generează acțiune de progresie.
398. F398 - Extinde `DebugDump` cu un lanț de decizie compact pentru o progresie activă și motivul ultimei schimbări.
399. F399 - Adaugă o verificare read-only pentru progresiile `archived`, astfel încât să fie vizibile fără să afecteze starea.
400. F400 - Leagă `QuestDetailGui` de un indicator clar pentru obiectivul rezolvat și obiectivul curent.
401. F401 - Standardizează mesajele pentru candidații respinși când lipsesc `node` sau `npc` din mapping.
402. F402 - Adaugă un raport combinat pentru `GapReport`, `QuestDirectorDecision` și `ProgressionGuiSnapshot` într-un singur bundle.
403. F403 - Introdu un test de regresie pentru selecția `templateId` după reload, cu confirmarea că rename-ul intern nu rupe snapshot-ul.
404. F404 - Adaugă un smoke flow pentru `story_driven` care confirmă diferența dintre candidat narativ și progresie activă.
405. F405 - Extinde GUI-ul de detaliu cu un mesaj scurt despre acțiunea sigură următoare atunci când lipsește `place`.
406. F406 - Adaugă un audit al progresiilor fără `questCode` și indică explicit dacă problema este de mapping sau de import.
407. F407 - Leagă `ProgressionService` de un raport final care separă clar `offered`, `active`, `tracked`, `completed` și `archived` după refresh.
408. F408 - Standardizează exportul `progression-snapshot.json` și validează că aceeași progresie apare identic în audit și dump.
409. F409 - Adaugă o verificare pentru questurile cu mai multe obiective și confirmă ordinea lor stabilă după reload.
410. F410 - Închide seria cu o notă de stare despre ce parte poate fi extrasă într-un contract comun și ce parte mai cere întărirea auditului.

411. F411 - Definește un snapshot read-only pentru progresiile fără `questCode` și expune-l în `debugdump`.
412. F412 - Adaugă un raport de aliniere între `QuestDetailGui` și `ProgressionGuiSnapshot` pentru progresiile cu status `tracked`.
413. F413 - Standardizează mesajele pentru fallback-ul de ancore astfel încât auditul să indice limpede nivelul și motivul.
414. F414 - Integrează `StoryContextService` cu o verificare care confirmă că story-ul nu rescrie progresia după reload.
415. F415 - Adaugă un smoke flow pentru `quest_only` cu confirmarea că obiectivele rămân stabile după restart.
416. F416 - Extinde `ProgressionService` cu un test de regresie pentru selectoarele `active` și `completed` după rename intern.
417. F417 - Adaugă un raport de consistență pentru mecanicile `quest`, `contract`, `duty`, `bounty`, `event`, `tutorial` și `ritual`.
418. F418 - Leagă `WorldHubGui` de o listă compactă cu questurile apropiate de NPC-ul selectat și de locurile relevante.
419. F419 - Standardizează exportul `quest-director-decision.json` și confirmă că aceeași decizie apare identic în audit și dump.
420. F420 - Adaugă o verificare pentru `quest_anchor_bindings` care confirmă că fallback-ul nu schimbă ancorele persistente.
421. F421 - Integrează `PatchPlanner` cu un mesaj scurt în GUI-ul de audit pentru gap-urile care blochează un quest seed.
422. F422 - Adaugă un smoke pentru `story_only` în care contextul narativ este inspectabil, dar nu generează acțiune de progresie.
423. F423 - Extinde `DebugDump` cu un lanț de decizie compact pentru o progresie activă și motivul ultimei schimbări.
424. F424 - Adaugă o verificare read-only pentru progresiile `archived`, astfel încât să fie vizibile fără să afecteze starea.
425. F425 - Leagă `QuestDetailGui` de un indicator clar pentru obiectivul rezolvat și obiectivul curent.
426. F426 - Standardizează mesajele pentru candidații respinși când lipsesc `node` sau `npc` din mapping.
427. F427 - Adaugă un raport combinat pentru `GapReport`, `QuestDirectorDecision` și `ProgressionGuiSnapshot` într-un singur bundle.
428. F428 - Introdu un test de regresie pentru selecția `templateId` după reload, cu confirmarea că rename-ul intern nu rupe snapshot-ul.
429. F429 - Adaugă un smoke flow pentru `story_driven` care confirmă diferența dintre candidat narativ și progresie activă.
430. F430 - Extinde GUI-ul de detaliu cu un mesaj scurt despre acțiunea sigură următoare atunci când lipsește `place`.
431. F431 - Fă un audit al progresiilor fără `story_context_id` și marchează explicit dacă problema vine din mapping sau din import.
432. F432 - Leagă `ProgressionService` de un raport final care separă clar `offered`, `active`, `tracked`, `completed` și `archived` după refresh.
433. F433 - Standardizează exportul `progression-snapshot.json` și validează că aceeași progresie apare identic în audit și dump.
434. F434 - Adaugă o verificare pentru questurile cu mai multe obiective și confirmă ordinea lor stabilă după reload.
435. F435 - Închide seria cu o notă de stare despre ce poate fi extras într-un contract comun și ce mai cere întărirea auditului.

436. F436 - Definește un snapshot read-only pentru progresiile fără `questCode` și expune-l în `debugdump`.
437. F437 - Adaugă un raport de aliniere între `QuestDetailGui` și `ProgressionGuiSnapshot` pentru progresiile cu status `completed`.
438. F438 - Standardizează mesajele pentru fallback-ul de ancore astfel încât auditul să indice limpede nivelul și motivul.
439. F439 - Integrează `StoryContextService` cu o verificare care confirmă că story-ul nu rescrie progresia după reload.
440. F440 - Adaugă un smoke flow pentru `quest_only` cu confirmarea că obiectivele rămân stabile după restart.
441. F441 - Extinde `ProgressionService` cu un test de regresie pentru selectoarele `tracked` și `archived` după rename intern.
442. F442 - Adaugă un raport de consistență pentru mecanicile `quest`, `contract`, `duty`, `bounty`, `event`, `tutorial` și `ritual`.
443. F443 - Leagă `WorldHubGui` de o listă compactă cu questurile apropiate de NPC-ul selectat și de locurile relevante.
444. F444 - Standardizează exportul `quest-director-decision.json` și confirmă că aceeași decizie apare identic în audit și dump.
445. F445 - Adaugă o verificare pentru `quest_anchor_bindings` care confirmă că fallback-ul nu schimbă ancorele persistente.
446. F446 - Integrează `PatchPlanner` cu un mesaj scurt în GUI-ul de audit pentru gap-urile care blochează un quest seed.
447. F447 - Adaugă un smoke pentru `story_only` în care contextul narativ este inspectabil, dar nu generează acțiune de progresie.
448. F448 - Extinde `DebugDump` cu un lanț de decizie compact pentru o progresie activă și motivul ultimei schimbări.
449. F449 - Adaugă o verificare read-only pentru progresiile `active`, astfel încât să fie vizibile fără să afecteze starea.
450. F450 - Leagă `QuestDetailGui` de un indicator clar pentru obiectivul rezolvat și obiectivul curent.
451. F451 - Standardizează mesajele pentru candidații respinși când lipsesc `node` sau `npc` din mapping.
452. F452 - Adaugă un raport combinat pentru `GapReport`, `QuestDirectorDecision` și `ProgressionGuiSnapshot` într-un singur bundle.
453. F453 - Introdu un test de regresie pentru selecția `templateId` după reload, cu confirmarea că rename-ul intern nu rupe snapshot-ul.
454. F454 - Adaugă un smoke flow pentru `story_driven` care confirmă diferența dintre candidat narativ și progresie activă.
455. F455 - Extinde GUI-ul de detaliu cu un mesaj scurt despre acțiunea sigură următoare atunci când lipsește `place`.
456. F456 - Adaugă un audit al progresiilor fără `questCode` și indică explicit dacă problema este de mapping sau de import.
457. F457 - Leagă `ProgressionService` de un raport final care separă clar `offered`, `active`, `tracked`, `completed` și `archived` după refresh.
458. F458 - Standardizează exportul `progression-snapshot.json` și validează că aceeași progresie apare identic în audit și dump.
459. F459 - Adaugă o verificare pentru questurile cu mai multe obiective și confirmă ordinea lor stabilă după reload.
460. F460 - Închide seria cu o notă de stare despre ce poate fi extras într-un contract comun și ce mai cere întărirea auditului.

461. F461 - Definește un snapshot read-only pentru progresiile fără `story_context_id` și expune-l în `debugdump`.
462. F462 - Adaugă un raport de aliniere între `QuestDetailGui` și `ProgressionGuiSnapshot` pentru progresiile cu status `active`.
463. F463 - Standardizează mesajele pentru fallback-ul de ancore astfel încât auditul să indice limpede nivelul și motivul.
464. F464 - Integrează `StoryContextService` cu o verificare care confirmă că story-ul nu rescrie progresia după reload.
465. F465 - Adaugă un smoke flow pentru `quest_only` cu confirmarea că obiectivele rămân stabile după restart.
466. F466 - Extinde `ProgressionService` cu un test de regresie pentru selectoarele `tracked` și `completed` după rename intern.
467. F467 - Adaugă un raport de consistență pentru mecanicile `quest`, `contract`, `duty`, `bounty`, `event`, `tutorial` și `ritual`.
468. F468 - Leagă `WorldHubGui` de o listă compactă cu questurile apropiate de NPC-ul selectat și de locurile relevante.
469. F469 - Standardizează exportul `quest-director-decision.json` și confirmă că aceeași decizie apare identic în audit și dump.
470. F470 - Adaugă o verificare pentru `quest_anchor_bindings` care confirmă că fallback-ul nu schimbă ancorele persistente.
471. F471 - Integrează `PatchPlanner` cu un mesaj scurt în GUI-ul de audit pentru gap-urile care blochează un quest seed.
472. F472 - Adaugă un smoke pentru `story_only` în care contextul narativ este inspectabil, dar nu generează acțiune de progresie.
473. F473 - Extinde `DebugDump` cu un lanț de decizie compact pentru o progresie activă și motivul ultimei schimbări.
474. F474 - Adaugă o verificare read-only pentru progresiile `archived`, astfel încât să fie vizibile fără să afecteze starea.
475. F475 - Leagă `QuestDetailGui` de un indicator clar pentru obiectivul rezolvat și obiectivul curent.
476. F476 - Standardizează mesajele pentru candidații respinși când lipsesc `node` sau `npc` din mapping.
477. F477 - Adaugă un raport combinat pentru `GapReport`, `QuestDirectorDecision` și `ProgressionGuiSnapshot` într-un singur bundle.
478. F478 - Introdu un test de regresie pentru selecția `templateId` după reload, cu confirmarea că rename-ul intern nu rupe snapshot-ul.
479. F479 - Adaugă un smoke flow pentru `story_driven` care confirmă diferența dintre candidat narativ și progresie activă.
480. F480 - Extinde GUI-ul de detaliu cu un mesaj scurt despre acțiunea sigură următoare atunci când lipsește `place`.
481. F481 - Fă un audit al progresiilor fără `story_context_id` și marchează explicit dacă problema vine din mapping sau din import.
482. F482 - Leagă `ProgressionService` de un raport final care separă clar `offered`, `active`, `tracked`, `completed` și `archived` după refresh.
483. F483 - Standardizează exportul `progression-snapshot.json` și validează că aceeași progresie apare identic în audit și dump.
484. F484 - Adaugă o verificare pentru questurile cu mai multe obiective și confirmă ordinea lor stabilă după reload.
485. F485 - Închide seria cu o notă de stare despre ce poate fi extras într-un contract comun și ce mai cere întărirea auditului.

486. F486 - Finalizat 2026-06-19: definește un snapshot read-only pentru progresiile fără legătură story și expune-l în `debugdump`.
487. F487 - Finalizat 2026-06-19: adaugă un raport de aliniere între `QuestDetailGui` și `ProgressionGuiSnapshot` pentru progresiile active și urmărite.
488. F488 - Finalizat 2026-06-19: standardizează mesajele pentru fallback-ul de ancore astfel încât GUI-ul și auditul să indice limpede nivelul și motivul.
489. F489 - Finalizat 2026-06-19: integrează `StoryContextService` cu o verificare read-only care compară story-ul și progresia fără cale de scriere.
490. F490 - Adaugă un smoke flow pentru `quest_only` cu confirmarea că obiectivele rămân stabile după restart.
491. F491 - Finalizat 2026-06-19: extinde `ProgressionService` cu regresie pentru selectoarele `active` și `completed` după rename intern.
492. F492 - Adaugă un raport de consistență pentru mecanicile `quest`, `contract`, `duty`, `bounty`, `event`, `tutorial` și `ritual`.
493. F493 - Leagă `WorldHubGui` de o listă compactă cu questurile apropiate de NPC-ul selectat și de locurile relevante.
494. F494 - Standardizează exportul `quest-director-decision.json` și confirmă că aceeași decizie apare identic în audit și dump.
495. F495 - Adaugă o verificare pentru `quest_anchor_bindings` care confirmă că fallback-ul nu schimbă ancorele persistente.
496. F496 - Integrează `PatchPlanner` cu un mesaj scurt în GUI-ul de audit pentru gap-urile care blochează un quest seed.
497. F497 - Adaugă un smoke pentru `story_only` în care contextul narativ este inspectabil, dar nu generează acțiune de progresie.
498. F498 - Extinde `DebugDump` cu un lanț de decizie compact pentru o progresie activă și motivul ultimei schimbări.
499. F499 - Adaugă o verificare read-only pentru progresiile `archived`, astfel încât să fie vizibile fără să afecteze starea.
500. F500 - Leagă `QuestDetailGui` de un indicator clar pentru obiectivul rezolvat și obiectivul curent.
501. F501 - Standardizează mesajele pentru candidații respinși când lipsesc `node` sau `npc` din mapping.
502. F502 - Adaugă un raport combinat pentru `GapReport`, `QuestDirectorDecision` și `ProgressionGuiSnapshot` într-un singur bundle.
503. F503 - Introdu un test de regresie pentru selecția `templateId` după reload, cu confirmarea că rename-ul intern nu rupe snapshot-ul.
504. F504 - Adaugă un smoke flow pentru `story_driven` care confirmă diferența dintre candidat narativ și progresie activă.
505. F505 - Extinde GUI-ul de detaliu cu un mesaj scurt despre acțiunea sigură următoare atunci când lipsește `place`.
506. F506 - Fă un audit al progresiilor fără `story_context_id` și marchează explicit dacă problema vine din mapping sau din import.
507. F507 - Leagă `ProgressionService` de un raport final care separă clar `offered`, `active`, `tracked`, `completed` și `archived` după refresh.
508. F508 - Standardizează exportul `progression-snapshot.json` și validează că aceeași progresie apare identic în audit și dump.
509. F509 - Adaugă o verificare pentru questurile cu mai multe obiective și confirmă ordinea lor stabilă după reload.
510. F510 - Închide seria cu o notă de stare despre ce poate fi extras într-un contract comun și ce mai cere întărirea auditului.

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
