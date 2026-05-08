# Lucru Alternat: Quest, Mapping si ProgressionService

Actualizat: 2026-05-07

## Scop

Acest document stabileste cum trebuie lucrat in fazele urmatoare cand dezvoltarea atinge simultan:

- questuri si contracte jucabile;
- world mapping semantic;
- `ProgressionService` ca directie de runtime generic.

Regula principala: nu dezvolta mult timp doar una dintre aceste parti. Questurile, mapping-ul si progression runtime-ul trebuie sa se verifice reciproc prin slice-uri mici, testabile pe Paper.

## De ce alternanta este necesara

Questurile fara mapping ajung sa foloseasca locuri inventate sau fallback-uri fragile.

Mapping-ul fara questuri ramane doar structura de date, fara dovada ca locurile, node-urile si bindings-urile sunt utile in gameplay.

`ProgressionService` extras prea devreme risca sa devina abstractie mare fara suficiente cazuri reale. El trebuie extras din comportament deja verificat: questuri, contracte, stages, obiective, anchors, tracking si reload.

Directia corecta este:

```text
mapping concret -> quest/contract mic -> smoke test -> observabilitate -> extractie progression
```

## Rolurile celor trei parti

| Parte | Rol | Nu trebuie sa faca |
|---|---|---|
| Mapping | Sursa de adevar pentru regiuni, places, nodes si legaturi NPC-loc | Nu decide progres, reward sau status de quest |
| Quest/contract | Gameplay concret care consuma mapping si produce progres verificabil | Nu inventeaza locatii daca mapping-ul nu le poate rezolva |
| ProgressionService | Runtime generic pentru progres, obiective, stages, status, tracking si recompense | Nu trebuie extras prin migrare mare inainte de demo stabil |

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

### Pasul 3 - Observabilitate

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

### Pasul 4 - Extractie mica spre ProgressionService

Scop: generalizezi doar comportamentul dovedit de quest/contract.

Livrabile acceptabile:

- `ProgressionDefinition` peste definitiile existente;
- `ProgressionSelector` pentru selectori stabili; implementat initial pentru selectori simpli, `tracked/current`, `mechanic:definition` si `pack:mechanic:definition`;
- comanda read-only pentru inspectie, implementata initial prin `/ainpc progression definitions [filter]`;
- comanda admin read-only pentru progres persistent, implementata initial prin `/ainpc progression stored [jucator|uuid|all] [filter] [limit]`;
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

Lucreaza la `ProgressionService` cand:

- aceeasi logica exista deja in quest si contract;
- selectorii, statusul sau tracking-ul sunt duplicati;
- ai cel putin un caz non-quest functional, cum este `C01`;
- exportul generic poate fi validat cu date reale din `player_quests`.

## Slice minim recomandat

Fiecare slice nou trebuie sa contina toate cele patru bucati:

| Bucata | Intrebare de control |
|---|---|
| Mapping | Unde se intampla in lumea reala a serverului? |
| Quest/progression | Ce progres concret vede jucatorul? |
| Persistenta | Ce ramane dupa reload? |
| Debug/audit | Cum aflam de ce nu merge? |

Daca una dintre cele patru lipseste, slice-ul nu este gata.

## Ordine practica pentru fazele urmatoare

1. Verifica mapping demo pe Paper: create, plan, spawn, audit, save, reload.
2. Testeaza Q01-Q05 cap-coada fara mapping complex.
3. Testeaza Q06-Q08 pe mapping demo.
4. Testeaza C01 ca progres non-quest prin `contract` si `progression`.
5. Adauga 1-2 questuri noi numai daca folosesc locuri/NPC-uri deja mapate.
6. Extrage o bucata mica in `ProgressionService` numai dupa ce smoke test-ul trece.
7. Actualizeaza docs si TODO dupa fiecare slice care schimba comportamentul real.

## Gate pentru demo playable matur

Un demo intern este matur doar cand:

- exista un sat demo verificat dupa restart;
- exista 3-5 questuri sau contracte completabile;
- mapping-ul si NPC bindings sunt coerente;
- quest anchors si player progress raman dupa reload;
- `audit` si `debugdump` pot explica mapping, quest/progression si story state;
- nu este nevoie de editare manuala in DB ca demo-ul sa mearga.

## Legaturi canonice

- `mapping.md` pentru modelul `WorldRegion -> WorldPlace -> WorldNode`;
- `npc-world-bindings.md` pentru legaturi NPC -> home/work/social;
- `questuri-avansate.md` pentru obiective, stages si evolutia questurilor;
- `quest-anchor-bindings.md` pentru ancore semantice persistente;
- `progression-service.md` pentru runtime generic de progres;
- `debugging-si-testare.md` pentru smoke tests, audit si debugdump.
