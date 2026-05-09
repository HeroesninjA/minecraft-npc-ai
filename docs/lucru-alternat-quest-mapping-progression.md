# Lucru Alternat: Quest, Mapping, ProgressionService si GUI

Actualizat: 2026-05-09

## Scop

Acest document stabileste cum trebuie lucrat in fazele urmatoare cand dezvoltarea atinge simultan:

- questuri si contracte jucabile;
- world mapping semantic;
- `ProgressionService` ca directie de runtime generic;
- sistemul GUI ca strat vizual peste progres, mapping, NPC-uri, audit si debug.

Regula principala: nu dezvolta mult timp doar una dintre aceste parti. Questurile, mapping-ul, progression runtime-ul si GUI-ul trebuie sa se verifice reciproc prin slice-uri mici, testabile pe Paper.

## De ce alternanta este necesara

Questurile fara mapping ajung sa foloseasca locuri inventate sau fallback-uri fragile.

Mapping-ul fara questuri ramane doar structura de date, fara dovada ca locurile, node-urile si bindings-urile sunt utile in gameplay.

`ProgressionService` extras prea devreme risca sa devina abstractie mare fara suficiente cazuri reale. El trebuie extras din comportament deja verificat: questuri, contracte, stages, obiective, anchors, tracking si reload.

GUI-ul construit prea devreme risca sa ascunda lipsa de runtime stabil. GUI-ul construit prea tarziu face sistemul greu de testat pe server. El trebuie sa intre in fiecare slice ca prezentare read-only sau actiune validata peste servicii existente.

Directia corecta este:

```text
mapping concret -> quest/contract/progres mic -> GUI snapshot -> smoke test -> observabilitate -> extractie progression
```

## Rolurile celor patru parti

| Parte | Rol | Nu trebuie sa faca |
|---|---|---|
| Mapping | Sursa de adevar pentru regiuni, places, nodes si legaturi NPC-loc | Nu decide progres, reward sau status de quest |
| Quest/contract | Gameplay concret care consuma mapping si produce progres verificabil | Nu inventeaza locatii daca mapping-ul nu le poate rezolva |
| ProgressionService | Runtime generic pentru progres, obiective, stages, status, tracking si recompense | Nu trebuie extras prin migrare mare inainte de demo stabil |
| GUI system | Prezentare si control sigur peste snapshot-uri, comenzi validate, audit si debug | Nu muta logica din `ScenarioEngine`, `ProgressionService`, `WorldAdminApi` sau DB in inventare |

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

Lucreaza la quest cand:

- mapping-ul exista, dar nu este folosit intr-un flux jucabil;
- demo-ul nu are 3-5 questuri completabile;
- progresul nu supravietuieste reload-ului;
- GUI/log nu arata clar urmatorul pas pentru jucator.

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

Fiecare slice nou trebuie sa contina toate cele cinci bucati:

| Bucata | Intrebare de control |
|---|---|
| Mapping | Unde se intampla in lumea reala a serverului? |
| Quest/progression | Ce progres concret vede jucatorul? |
| GUI | Cum vede sau controleaza jucatorul/adminul progresul fara comenzi fragile? |
| Persistenta | Ce ramane dupa reload? |
| Debug/audit | Cum aflam de ce nu merge? |

Daca una dintre cele cinci lipseste, slice-ul nu este gata.

## Ordine practica pentru fazele urmatoare

1. Verifica mapping demo pe Paper: create, plan, spawn, audit, save, reload.
2. Testeaza Q01-Q05 cap-coada fara mapping complex.
3. Testeaza Q06-Q08 pe mapping demo.
4. Testeaza C01 ca progres non-quest prin `contract` si `progression`.
5. Testeaza C02 ca progres non-quest peste mapping-ul demo: piata, `quest_board`, story event si persistenta prin `ProgressionService`.
6. Verifica acelasi set in GUI: `/ainpc gui quest all`, filtre pe mecanica, detalii, tracking si status.
7. Adauga 1-2 questuri noi numai daca folosesc locuri/NPC-uri deja mapate si apar corect in GUI.
8. Extrage o bucata mica in `ProgressionService` numai dupa ce smoke test-ul si GUI-ul trec.
9. Actualizeaza docs si TODO dupa fiecare slice care schimba comportamentul real.

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

## Gate pentru demo playable matur

Un demo intern este matur doar cand:

- exista un sat demo verificat dupa restart;
- exista 3-5 questuri sau contracte completabile;
- mapping-ul si NPC bindings sunt coerente;
- quest anchors si player progress raman dupa reload;
- Quest/Progression GUI afiseaza corect questuri, contracte, duty-uri, bounty-uri, evenimente, tutoriale si ritualuri;
- World/Routine GUI explica locul si rutina fara sa inlocuiasca auditul;
- `audit` si `debugdump` pot explica mapping, quest/progression si story state;
- nu este nevoie de editare manuala in DB ca demo-ul sa mearga.

## Legaturi canonice

- `mapping.md` pentru modelul `WorldRegion -> WorldPlace -> WorldNode`;
- `npc-world-bindings.md` pentru legaturi NPC -> home/work/social;
- `questuri-avansate-v2.md` pentru obiective, stages, diversitate si evolutia questurilor;
- `quest-anchor-bindings.md` pentru ancore semantice persistente;
- `progression-service.md` pentru runtime generic de progres;
- `gui-interfete.md` pentru GUI system peste quest, progression, world, NPC, audit si debug;
- `debugging-si-testare.md` pentru smoke tests, audit si debugdump.
