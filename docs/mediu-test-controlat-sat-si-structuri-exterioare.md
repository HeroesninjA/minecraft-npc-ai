# Mediu test controlat: sat si structuri exterioare

Actualizat: 2026-06-16

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).

## Scop

Acest document defineste un mediu controlat pentru testarea AINPC: un sat predefinit si cateva structuri exterioare mapate semantic. Scopul este sa existe o harta predictibila pentru smoke tests, demo intern, verificare mapping, quest anchors, rutine NPC si validatoare.

Pentru schema completa de scenariu peste acest fixture, inclusiv cladiri controlate, NPC-uri controlate, relatii, context semantic si questuri story smoke, vezi `schema-scenariu-predefinit-testare.md`.

Important:

- acesta este fixture/cod de test;
- nu este continut final al proiectului;
- nu trebuie sa ramana in core ca lume hardcodata permanenta;
- nu trebuie sa fie activ implicit pe servere reale;
- trebuie sa fie usor de sters, dezactivat sau mutat intr-un addon demo separat.

## De ce este necesar

Fara un mediu controlat, testarea este instabila:

- un server poate avea `0` regiuni mapate;
- o harta construita manual poate avea coordonate diferite;
- validatoarele pot trece pe un server si esua pe altul;
- questurile cu `visit_place` si `inspect_node` sunt greu de verificat fara ancore stabile;
- rutinele NPC depind de home/work/social anchors;
- structurile exterioare nu pot fi comparate corect daca fiecare test foloseste alt layout.

Un fixture controlat rezolva problema doar pentru testare. Nu inlocuieste mapping-ul real, addonurile sau lumile serverelor.

## Regula constitutionala

Mediul controlat trebuie sa respecte regulile proiectului:

- activare explicita prin comanda, config de test sau profil demo;
- default dezactivat pe productie;
- fara world build automat in MVP;
- fara spawn automat de NPC/mobi/loot;
- fara quest progress automat;
- mapping semantic inainte de orice gameplay;
- validare si audit dupa creare;
- idempotenta sau refuz clar daca fixture-ul exista deja;
- nume si ID-uri namespaced cu prefix de test, de exemplu `test_` sau `demo_`;
- posibilitate de cleanup/rollback inainte de utilizare pe server real.

## Avertizare obligatorie despre cod

Orice cod introdus pentru acest mediu trebuie tratat ca temporar.

Regula:

```text
cod pentru mediu controlat
-> util pentru test/demo intern
-> nu devine arhitectura finala
-> nu se amesteca cu generatorul matur
-> se muta ulterior in addon demo, test fixtures sau scripts
-> se sterge cand exista pipeline real de template/build/import
```

Nu se accepta ca acest cod sa devina:

- generator permanent hardcodat;
- dependinta pentru questurile reale;
- continut implicit al core-ului;
- sursa de adevar pentru structurile finale;
- motiv pentru a evita validatoare, rollback sau authoring real.

## Layout recomandat

### Sat principal

Region:

```yaml
id: test_sat_central
type: settlement
tags: [test_fixture, demo, settlement, safe]
```

Places minime:

- `test_sat_central:piata`
- `test_sat_central:casa_1`
- `test_sat_central:casa_2`
- `test_sat_central:casa_3`
- `test_sat_central:fierarie`
- `test_sat_central:ferma`
- `test_sat_central:taverna`
- `test_sat_central:altar`
- `test_sat_central:drum_nord`
- `test_sat_central:drum_est`

Nodes minime:

- `meeting_point` in piata;
- `quest_trigger` in piata;
- `bed`, `home`, `npc_spawn` in case;
- `workstation` si `work` in fierarie;
- `work` in ferma;
- `social` in taverna;
- `interaction` la altar;
- `entrance` pe drumurile principale.

### Structuri exterioare

Structurile initiale trebuie sa fie putine si clare:

| Region/Place | Tip | Rol test |
|---|---|---|
| `test_padure_veche` | `forest` | `visit_region`, resurse, danger marker, entry node |
| `test_fantana_uitata` | `fountain` | `inspect_node`, ritual/offering simplu |
| `test_casa_izolata` | `isolated_house` | NPC izolat, home node, storage inspect |
| `test_cripta_lupilor` | `dungeon` | entrance/exit, loot node, boss marker fara spawn automat |
| `test_tabara_factiune` | `faction_settlement` | factiune neutra/ostila optional, campfire, leader anchor |
| `test_turn_paza` | `watchtower` | lookout, guard anchor, road control |
| `test_ruine_vechi` | `ruins` | lore inspect, hidden room, loot marker |

Acestea trebuie create ca mapping semantic, nu ca blocuri fizice obligatorii.

## Comenzi tinta

Status implementare initiala:

- `/ainpc world fixture plan [prefix]` exista ca plan read-only;
- `/ainpc world fixture validate [prefix]` exista ca validare read-only a mapping-ului prezent;
- planul listeaza satul controlat, places, nodes, structuri exterioare si offset-uri relative;
- planul nu foloseste WorldEdit;
- planul si validarea nu creeaza mapping, blocuri, NPC-uri, mobi, loot sau quest progress;
- `fixture create` ramane faza urmatoare si trebuie sa fie mapping-only, opt-in.

### Faza 1: plan read-only

Comenzi recomandate:

```text
/ainpc world fixture plan
/ainpc world fixture validate
/ainpc world outside types
/ainpc world outside blueprint dungeon
/ainpc world outside plan dungeon test_cripta_lupilor
```

`fixture plan` trebuie sa afiseze:

- regiuni propuse;
- places propuse;
- nodes propuse;
- coordonate relative fata de punctul de origine;
- warning ca este fixture temporar;
- comenzi de validare dupa creare.

### Faza 2: create mapping-only

Comanda propusa:

```text
/ainpc world fixture create [prefix]
```

Reguli:

- creeaza doar `Region`, `Place`, `Node`;
- nu construieste blocuri;
- nu spawneaza NPC-uri;
- nu scrie quest progress;
- nu salveaza automat in `config.yml` fara `/ainpc world save`;
- refuza daca `demo.enabled=false`;
- refuza sau foloseste prefix nou daca fixture-ul exista deja.

### Faza 3: validate

Comenzi dupa creare:

```text
/ainpc audit world
/ainpc world outside validate test_padure_veche
/ainpc world outside validate test_fantana_uitata
/ainpc world outside validate test_casa_izolata
/ainpc world outside validate test_cripta_lupilor
/ainpc world outside validate test_tabara_factiune
/ainpc world outside validate test_turn_paza
/ainpc world outside validate test_ruine_vechi
```

### Faza 4: smoke gameplay

Abia dupa validare se testeaza:

- `whereami`;
- `visit_region`;
- `visit_place`;
- `inspect_node`;
- rutine NPC home/work/social;
- story context;
- debugdump world/story/quest;
- restart si reload.

## Coordonate relative recomandate

Fixture-ul trebuie sa fie pozitionat relativ la o origine aleasa de admin sau la spawn-ul lumii in consola/RCON.

Exemplu:

```text
origin = locatia playerului sau world spawn

test_sat_central        0, 0
test_padure_veche      -180, 0
test_fantana_uitata      90, -80
test_casa_izolata      -120, 110
test_cripta_lupilor     220, 40
test_tabara_factiune    260, -160
test_turn_paza           60, 180
test_ruine_vechi       -240, -140
```

Reguli:

- distantele trebuie sa fie suficient de mari ca structurile sa nu se suprapuna;
- drumurile trebuie sa aiba nodes de intrare/iesire;
- dungeon-ul si tabara de factiune trebuie sa fie la distanta de zona safe;
- bounds-urile trebuie sa fie simple si predictibile pentru teste.

## Date si prefixe

Prefix recomandat:

```text
test_
```

Alternative:

```text
demo_
fixture_
```

Reguli:

- toate ID-urile trebuie sa aiba acelasi prefix;
- nu amesteca fixture-ul cu mapping real;
- nu folosi nume de NPC reale sau lore final;
- nu pune secrete sau date reale in metadata;
- include tag `test_fixture` pe fiecare regiune si place.

## Validari minime pentru fixture

Fixture-ul este acceptabil cand:

- are cel putin o regiune de sat;
- are cel putin 3 case;
- are cel putin un social place;
- are cel putin un workplace;
- are cel putin un quest trigger;
- fiecare structura exterioara are entry node;
- dungeon-ul are entrance si exit;
- casa izolata are door si bed/home;
- tabara de factiune are campfire/leader anchor;
- toate structurile trec prin `/ainpc world outside validate`;
- `/ainpc audit world` nu raporteaza erori critice;
- `debugdump world` poate exporta mapping-ul.

## Ce nu trebuie implementat in acest fixture

Nu include in fixture-ul initial:

- schematics WorldEdit;
- constructie fizica de blocuri;
- spawn automat de NPC-uri;
- spawn automat de mobi;
- loot real;
- economie;
- reputatie de factiune;
- quest branching complex;
- story events permanente;
- migrari DB;
- cleanup agresiv fara dry-run.

## Plan de eliminare

Acest cod trebuie eliminat sau mutat cand exista una dintre urmatoarele:

- addon demo dedicat pentru continut de test;
- import/export de mapping matur;
- catalog de template-uri real;
- builder nativ validat;
- integrare WorldEdit optionala stabila;
- harta demo versionata separat de core.

La acel moment:

1. marcheaza codul fixture ca deprecated;
2. pastreaza comenzile doar daca sunt utile pentru addon/test;
3. muta continutul in addon demo sau test resources;
4. lasa in core doar validatoarele si contractele generale;
5. actualizeaza `implementat-deja.md` si acest document.

## Definitia de gata pentru primul slice

Primul slice trebuie sa livreze doar:

- documentul de fata;
- plan read-only pentru fixture;
- lista de regiuni/places/nodes propuse;
- niciun build fizic;
- niciun spawn;
- nicio scriere automata persistenta;
- teste unitare pentru planul fixture.

Abia dupa acest slice merita introdusa comanda `fixture create`, tot mapping-only si opt-in.

