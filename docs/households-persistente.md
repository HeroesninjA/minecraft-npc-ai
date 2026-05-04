# Households Persistente

Actualizat: 2026-05-04

Status: document canonic initial pentru modelul persistent de household-uri. Acest document este design si contract de lucru; tabelele `households` si `household_residents` nu sunt considerate implementate complet pana cand exista cod, migration, audit si comenzi de inspectie.

## Scop

Household-ul este unitatea sociala care leaga casa, familia, rezidentii, rutina si simularea satului.

Fara un model persistent dedicat, datele raman imprastiate intre:

- `WorldPlace.metadata.residents`
- `WorldPlace.ownerNpcId`
- `npc_profiles.profile_data.owned_locations`
- `npc_world_bindings`
- `npc_family`
- ancore runtime `homeAnchor`, `workAnchor`, `socialAnchor`

Acest document stabileste cum trebuie mutata sursa de adevar catre DB, fara sa rupa compatibilitatea cu datele create deja.

## Rol in arhitectura

Household-ul sta intre mapping si NPC.

```text
WorldRegion
-> WorldPlace home
-> Household
-> HouseholdResident
-> NPC
-> npc_world_bindings
-> RoutineEngine / Simulation / Quest
```

Regula:

- `WorldPlace` spune ce loc este casa
- `Household` spune cine locuieste acolo ca grup
- `HouseholdResident` spune ce rol are fiecare NPC in casa
- `npc_world_bindings` spune ancorele individuale home/work/social
- `npc_family` spune relatiile familiale reciproce

## Ce problema rezolva

Modelul actual cu metadata este bun pentru MVP, dar fragil pentru simulare:

- metadata poate ramane in urma dupa stergere NPC
- o casa cu mai multi rezidenti nu are stare proprie
- rutina nu are o sursa comuna pentru familie si household
- auditul nu poate distinge usor resident, owner, worker si visitor
- migration-ul este greu fara un contract explicit
- generatorul narativ nu are unde sa pastreze grupul rezultat

`households` si `household_residents` rezolva asta.

## Principii

### 1. Un household are o casa principala

Un household MVP trebuie sa aiba `home_place_id`.

Nu exista household permanent fara casa. Vizitatorii, comerciantii temporari sau NPC-ii episodici trebuie modelati separat prin lifecycle/persistence mode.

### 2. Un NPC permanent are cel mult un household principal

Pentru MVP:

```text
un NPC permanent -> 0 sau 1 household activ
```

`0` este permis doar pentru NPC-uri nemigrate, temporare, vizitatori sau cazuri incomplete raportate de audit.

### 3. Familia si household-ul nu sunt acelasi lucru

O familie poate locui intr-un household, dar:

- doi frati pot locui separat
- un ucenic poate locui cu familia fierarului fara sa fie ruda
- un batran poate locui cu un adult din alta familie
- colegii de camera pot avea household fara `family_id`

De aceea `family_id` este optional pe household si relation role exista separat pe resident.

### 4. Metadata ramane compatibilitate

`WorldPlace.metadata.residents` nu se sterge imediat.

Rolul lui dupa introducerea DB:

- fallback pentru servere vechi
- debug vizual rapid
- sursa de backfill

Nu trebuie sa mai fie sursa finala pentru rutina, audit matur sau simulare.

### 5. Bindings individuale raman in `npc_world_bindings`

Household-ul nu inlocuieste `npc_world_bindings`.

Household-ul raspunde la:

```text
cine locuieste impreuna?
care este casa grupului?
care este capacitatea si starea casei?
```

`npc_world_bindings` raspunde la:

```text
unde merge NPC-ul individual acasa, la munca si social?
ce node exact foloseste?
```

## Schema recomandata

### `households`

```sql
CREATE TABLE IF NOT EXISTS households (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    household_key TEXT NOT NULL UNIQUE,
    region_id TEXT NOT NULL,
    home_place_id TEXT NOT NULL,
    family_id TEXT NOT NULL DEFAULT '',
    primary_owner_npc_id INTEGER,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    source TEXT NOT NULL DEFAULT 'unknown',
    capacity INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

Campuri:

| Camp | Rol |
|---|---|
| `id` | ID DB intern |
| `household_key` | ID stabil logic, de exemplu `demo_sat:casa_01:household` |
| `region_id` | Regiunea in care se afla gospodaria |
| `home_place_id` | Casa principala din mapping |
| `family_id` | Familie principala, optional |
| `primary_owner_npc_id` | NPC-ul principal, optional |
| `status` | Starea household-ului |
| `source` | De unde a fost creat |
| `capacity` | Capacitate calculata sau declarata |
| `created_at` | Timestamp creare |
| `updated_at` | Timestamp update |

Statusuri recomandate:

```text
ACTIVE
INCOMPLETE
MIGRATED
ARCHIVED
INVALID
```

Surse recomandate:

```text
spawn_plan
population_plan
manual
metadata_backfill
profile_backfill
migration
```

### `household_residents`

```sql
CREATE TABLE IF NOT EXISTS household_residents (
    household_id INTEGER NOT NULL,
    npc_id INTEGER NOT NULL,
    npc_key TEXT NOT NULL DEFAULT '',
    relation_role TEXT NOT NULL DEFAULT '',
    social_role TEXT NOT NULL DEFAULT '',
    bed_node_id TEXT NOT NULL DEFAULT '',
    spawn_node_id TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    joined_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    PRIMARY KEY (household_id, npc_id),
    FOREIGN KEY (household_id) REFERENCES households(id) ON DELETE CASCADE
);
```

Campuri:

| Camp | Rol |
|---|---|
| `household_id` | Referinta la `households.id` |
| `npc_id` | Referinta la NPC-ul persistent |
| `npc_key` | Cheie stabila din plan, daca exista |
| `relation_role` | Rol familial sau social in casa |
| `social_role` | Rol narativ folosit de simulare/dialog |
| `bed_node_id` | Patul preferat, daca exista |
| `spawn_node_id` | Spawn node folosit la creare |
| `status` | Starea rezidentului in household |
| `joined_at` | Cand a intrat in household |
| `updated_at` | Ultima actualizare |

Statusuri recomandate:

```text
ACTIVE
LEFT
MISSING
DEAD
ARCHIVED
INVALID
```

Indexuri recomandate:

```sql
CREATE INDEX IF NOT EXISTS idx_households_region
ON households(region_id);

CREATE INDEX IF NOT EXISTS idx_households_home_place
ON households(home_place_id);

CREATE INDEX IF NOT EXISTS idx_household_residents_npc
ON household_residents(npc_id);
```

### Extensie `npc_world_bindings`

Cand modelul matur este introdus, `npc_world_bindings` trebuie sa poata pastra:

```text
household_id
family_id
```

Daca `household_id` nu exista initial in tabela, migration-ul trebuie sa-l adauge intr-o faza controlata.

## Invariante

Validari obligatorii:

- `household_key` este unic
- `home_place_id` exista in mapping
- `home_place_id` este casa sau place compatibil cu locuitul
- un household `ACTIVE` are cel putin un resident `ACTIVE`
- `resident_count <= capacity`, daca `capacity > 0`
- un NPC permanent nu apare ca `ACTIVE` in doua household-uri active
- `primary_owner_npc_id`, daca exista, este resident activ
- `bed_node_id`, daca exista, apartine `home_place_id`
- `spawn_node_id`, daca exista, apartine `home_place_id`

Warning-uri:

- household fara `family_id`
- household fara `primary_owner_npc_id`
- casa fara bed nodes
- resident fara `relation_role`
- resident fara `npc_world_bindings`
- metadata de place nu se potriveste cu DB

## Relatia cu `npc_world_bindings`

La creare sau migration:

```text
household.home_place_id
-> npc_world_bindings.home_place_id pentru fiecare resident
```

Node-urile individuale se aleg astfel:

1. `bed_node_id` din `household_residents`
2. `spawn_node_id` din `household_residents`
3. node `bed` liber din casa
4. node `home` sau `npc_spawn`
5. centrul place-ului ca fallback explicit

Regula:

- household-ul da contextul social
- binding-ul da ancora individuala

## Relatia cu `npc_family`

`npc_family` ramane sursa pentru relatii reciproce intre NPC-uri.

`household_residents.relation_role` este rolul in casa, nu dovada completa de familie.

Exemple:

```text
relation_role=father
-> trebuie sa aiba relatie reciproca in npc_family, daca family_id exista
```

```text
relation_role=apprentice
-> poate locui in household fara relatie de sange
```

Auditul trebuie sa raporteze:

- relation roles familiale fara relatie `npc_family`
- relatii `npc_family` intre NPC-uri din household-uri incompatibile, daca nu exista motiv
- family_id pe household fara membri conectati prin familie

## Relatia cu `WorldPlace.metadata`

Dupa introducerea DB:

`WorldPlace.metadata` poate pastra:

```text
owner_npc_id
resident_npc_ids
worker_npc_ids
social_npc_ids
household_id
household_key
```

Dar prioritatea citirii trebuie sa fie:

1. DB dedicat
2. `npc_world_bindings`
3. `WorldPlace.metadata`
4. `npc_profiles.profile_data`
5. fallback spatial

Aceasta prioritate trebuie documentata si folosita consecvent.

## Fluxuri de scriere

Un household poate veni dintr-un `PopulationPlan` izolat sau dintr-un `SettlementPlan` complet. Pentru planul de regiune cap-coada, vezi `settlement-plan.md`.

### Din `PopulationPlan`

```text
PopulationPlan
-> HouseholdPlan
-> validate
-> write households
-> write household_residents
-> write npc_world_bindings
-> optional metadata mirror
```

### Din `settlement spawn`

```text
HouseAllocation spawn reusit
-> NPC DB ids disponibile
-> family bind
-> household persist
-> npc_world_bindings persist
-> metadata mirror
```

### Din bind manual

Bind-ul manual nu trebuie sa creeze automat household complex fara confirmare.

Optiuni recomandate:

- daca NPC-ul nu are household, creeaza household single-person cu `source=manual`
- daca home place are household activ, intreaba/impune comanda separata pentru join
- daca NPC-ul are deja household, mutarea trebuie sa fie actiune explicita

Pentru MVP, comanda read-only si backfill sunt mai importante decat mutari complexe.

## Migration si backfill

Surse:

- `WorldPlace.metadata.residents`
- `WorldPlace.ownerNpcId`
- `npc_world_bindings.home_place_id`
- `npc_profiles.profile_data.owned_locations`
- `npc_family`
- planuri de spawn, daca au fost salvate

Pipeline:

```text
scan
-> infer household candidates
-> validate candidates
-> dry-run report
-> admin apply
-> write DB
-> mirror minimal metadata
-> audit
```

Reguli:

- migration este idempotenta
- nu sterge metadata veche
- nu creeaza duplicate daca se ruleaza de doua ori
- marcheaza cazurile ambigue ca `INCOMPLETE`
- cere interventie admin pentru case cu owner neclar sau rezidenti nerezolvati

Exemple de ambiguitate:

- acelasi NPC apare in doua case
- `owner_npc_id` nu se poate rezolva
- casa nu mai exista in mapping
- resident are homeAnchor in alta regiune
- mai multi NPC au acelasi nume si metadata foloseste nume, nu ID

## Comenzi recomandate

Inspectie:

```text
/ainpc household list <regionId>
/ainpc household info <householdKey|id>
/ainpc household npc <npc>
/ainpc household home <placeId>
```

Migration:

```text
/ainpc migration households dryrun
/ainpc migration households apply
```

Audit:

```text
/ainpc audit households
/ainpc debugdump households
```

Mutari ulterioare:

```text
/ainpc household add <household> <npc>
/ainpc household remove <household> <npc>
/ainpc household move <npc> <homePlaceId>
```

Pentru MVP, mutarile pot ramane backlog.

## Audit

`/ainpc audit households` trebuie sa verifice:

- household-uri fara casa
- case inexistente
- case care nu sunt compatibile cu locuitul
- household-uri active fara rezidenti
- rezidenti orfani fara NPC
- NPC prezent in mai multe household-uri active
- capacitate depasita
- bed/spawn nodes inexistente
- bed/spawn nodes in afara casei
- mismatch intre household si `npc_world_bindings`
- mismatch intre household si metadata
- familie nereciproca pentru relation roles familiale

Rezultat audit:

```text
households=4 active=4 incomplete=0
residents=9
errors=0 warnings=2
```

## Debugdump

Debugdump-ul trebuie sa includa fisiere separate:

```text
households.csv
household-residents.csv
household-audit.txt
household-metadata-comparison.txt
```

Scop:

- adminul poate vedea ce este in DB
- developerul poate compara DB cu mapping metadata
- migration-ul poate fi verificat fara acces direct la SQLite

## Integrare cu rutina

Rutina trebuie sa consume household-ul astfel:

- noaptea target principal = bed/home node din household
- seara target = home place sau social node din household
- evenimente sociale pot alege rezidenti din acelasi household
- NPC-ul evita sa fie mutat social departe daca household-ul are eveniment activ

Ordine target home:

1. `household_residents.bed_node_id`
2. `npc_world_bindings.home_node_id`
3. node `bed` disponibil in casa
4. node `home`
5. centrul casei

## Integrare cu simularea satului

Household-ul alimenteaza:

- `HouseholdState`
- consum de resurse
- nevoie de siguranta
- nevoie de hrana
- stare sociala
- reputatie pe household
- questuri locale

Exemple:

```text
household cu 4 rezidenti si foodLevel scazut
-> creste foodNeed
-> poate porni quest de aprovizionare
```

```text
household fara adult cu profesie
-> wealthLevel scazut
-> NPC poate cere ajutor sau munca
```

## Integrare cu questuri si story

Conditii recomandate:

```text
household_has_resident_role
household_resource_below
household_has_family
household_reputation_at_least
household_status
```

Actiuni recomandate:

```text
adjust_household_need
set_household_status
record_household_event
adjust_household_reputation
```

Story events trebuie sa poata mentiona:

- `regionId`
- `homePlaceId`
- `householdKey`
- NPC-urile afectate

## Faze mici

### H0: Contract documentat

Livrabile:

- documentul de fata
- schema recomandata
- reguli de prioritate intre DB, bindings si metadata

Nu necesita server Paper.

### H1: Inspectie read-only

Livrabile:

- serviciu sau raport care infereaza household-uri din date existente
- nu scrie DB
- listeaza erori si ambiguitati

### H2: Migration dry-run

Livrabile:

- candidati `households`
- candidati `household_residents`
- raport de aplicare
- idempotenta verificabila

### H3: Persistenta minima

Livrabile:

- tabele `households` si `household_residents`
- scriere din spawn reusit
- citire pentru inspectie

### H4: Audit si debugdump

Livrabile:

- `/ainpc audit households`
- `debugdump households`
- comparatie cu metadata si `npc_world_bindings`

### H5: Rutina foloseste household

Livrabile:

- home target din household resident
- fallback catre `npc_world_bindings`
- status inspectabil

## Ce trebuie evitat

- Nu sterge `metadata.residents` imediat dupa migration.
- Nu crea household-uri automate ambigue la startup fara dry-run.
- Nu permite NPC permanent activ in doua household-uri active.
- Nu trata `family_id` ca obligatoriu pentru toate household-urile.
- Nu pune work/social bindings direct in household; ele raman individuale.
- Nu bloca spawn-ul actual pana cand modelul persistent nu are migration si audit.

## Definitia de gata pentru MVP

Modelul este suficient pentru MVP cand:

- fiecare household activ are home place valid
- fiecare resident activ se rezolva la NPC persistent
- capacitatea este verificata
- household-ul se poate inspecta prin comanda sau debugdump
- auditul compara DB cu mapping metadata si bindings
- spawn-ul reusit poate scrie household-uri fara date partiale evidente
- rutina poate citi household-ul pentru target home

Abia dupa acest punct are sens sa legam economie, resurse si reputatie de gospodarii.
