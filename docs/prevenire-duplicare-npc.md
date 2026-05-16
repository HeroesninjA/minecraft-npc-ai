# Prevenire si Remediere Duplicare NPC

Actualizat: 2026-05-09

## Scop

Acest document descrie cum se previne duplicarea NPC-urilor si cum se rezolva bugurile unde acelasi NPC apare de doua ori.

Termenul corect folosit aici este `duplicare`; daca bug report-ul foloseste `dublicare`, trateaza-l ca aceeasi problema.

Documentul acopera:

- reguli de identitate pentru NPC-uri permanente
- mecanismele existente in cod
- cauze comune de duplicate
- procedura de diagnostic pe server
- procedura de cleanup pentru entitati si DB
- backlog de hardening pentru fazele urmatoare

## Definitii

Nu toate duplicatele sunt acelasi tip de bug.

| Tip | Simptom | Gravitate |
|---|---|---|
| Duplicare de entitate | Exista doua entitati `Villager` in lume pentru acelasi rand DB | Mare; poate aparea dupa restart/chunk load |
| Duplicare DB + entitate | Exista doua randuri in `npcs` si doua entitati aproape identice in lume | Mare; necesita cleanup controlat |
| Duplicare DB fara entitate | Exista doua randuri similare in `npcs`, dar unul este inactiv | Medie; poate redeveni bug la restaurare |
| Duplicare de plan | Acelasi `HouseAllocation` sau `NpcSpawnPlan` este commit-uit de doua ori | Mare; trebuie oprita prin idempotenta |
| Nume duplicat legitim | Doua NPC-uri au acelasi nume, dar locatii/profiluri diferite | Nu este neaparat bug, dar face comenzile dupa nume ambigue |

## Invariantul de identitate

Pentru un NPC permanent trebuie sa fie adevarate simultan:

1. exista un singur rand in `npcs` pentru identitatea lui
2. `npcs.uuid` este UUID-ul entitatii Bukkit curente sau al ultimei entitati atasate
3. exista cel mult o entitate `Villager` activa pentru randul DB
4. cache-ul `NPCManager` are o singura intrare dupa DB id, UUID si entity UUID
5. `npc_profiles`, `npc_personality`, `npc_emotions`, `npc_traits`, relatiile si memoria refera acelasi `npc_id`
6. ancorele `home/work/social` descriu acelasi NPC, nu o copie creata ulterior

Regula practica: daca doua NPC-uri au acelasi nume si sunt la distanta de maximum 1.5 block-uri in aceeasi lume, trateaza-le ca duplicate pana dovedesti contrariul.

## Mecanisme existente in cod

Protectii curente:

- `DatabaseManager` creeaza `npcs.uuid` cu `UNIQUE NOT NULL`.
- Tabelele dependente folosesc `npc_id` si foreign keys cu `ON DELETE CASCADE`, iar conexiunea activeaza `PRAGMA foreign_keys = ON`.
- `NPCManager.loadAllNPCs()` incarca randurile DB in cache.
- `NPCManager.discoverExistingVillagers()` asociaza villagerii deja incarcati cu profilurile existente.
- `AINPC.applyPersistentIdentity(...)` scrie marker persistent pe entitate: `npc_managed`, `npc_database_id`, `npc_uuid`, `npc_name` si `npc_source_key`.
- `npc_profiles.profile_data.spawn_state` pastreaza amprenta ultimei stari runtime: `spawned`, `entity_uuid`, `source_key`, locatie, chunk si timestamp de update.
- `NPCManager.ensureVillagerIsNPC(...)` prefera UUID-ul DB, apoi markerul `npc_database_id`, apoi compatibilitatea legacy dupa nume/locatie.
- NPC-urile create din `NpcSpawnPlan` primesc `source_key` stabil in `profile_data`, derivat din `familyId`/`homePlaceId`/`spawnNodeId` si `npcKey`.
- `npc_source_keys` tine un index DB dedicat `source_key -> npc_id`, cu un singur owner canonic si cleanup automat prin foreign key.
- `spawn_batches` si `spawn_batch_steps` pastreaza jurnalul pentru settlement spawn: `batch_key`, `plan_hash`, status, `dry_run`, household steps, `household_id`, NPC-uri create/reutilizate si rollback.
- `/ainpc world household spawn ...` scrie acum si batch singular `scope_type=household`; `settlement spawn` ramane batch de regiune cu household-uri ca pasi, fara batch-uri nested pentru fiecare casa.
- Dry-run-urile pot fi jurnalizate separat prin `spawn.batches.track_dry_runs`; cheile lor folosesc prefixul `dryrun:` si nu suprascriu batch-urile reale.
- La pornirea unui batch existent, pasii vechi din `spawn_batch_steps` sunt curatati numai cand nu se pierde jurnal de rollback; daca batch-ul vechi este `RUNNING` si are pasi cu `created_npc_ids` nevid, rerularea este blocata pana la `inspect` si `repair`, chiar daca ID-urile nu mai pot fi parsate.
- Rollback-ul global de settlement foloseste `created_npc_ids` din pasii batch-ului curent, cu fallback pe lista din memorie.
- `households` si `household_residents` pastreaza household-ul logic, casa, family id, resident key, NPC canonic si `source_key` pentru fiecare rezident planificat.
- `/ainpc world household status <householdId|homePlaceId>`, `/ainpc world household place <homePlaceId>`, `/ainpc world household resident <npcId|numeNpc|nearest>` si `/ainpc world household list [limit]` inspecteaza read-only aceste tabele dupa spawn/restart.
- `/ainpc migration households dryrun|apply [limit]` poate face backfill idempotent din `npc_world_bindings` si metadata `resident_npc_ids` catre `households` si `household_residents`; dry-run este obligatoriu inainte de apply, iar NPC-ii deja mutati in alt household sunt sariti.
- `/ainpc repair households dryrun|apply` elimina randurile duplicate din `household_residents` pentru acelasi `npc_id` sau acelasi `source_key`, pastrand canonic rezidentul activ cel mai recent.
- `NPCManager` pastreaza un index canonic pe `source_key`; daca exista randuri DB duplicate, randul cu ID mai mic castiga si celelalte nu mai pot restaura/spawna entitati noi.
- Daca o entitate live indica prin `npc_database_id` spre un rand duplicat, `ensureVillagerIsNPC(...)` o reasociaza la randul canonic pentru acel `source_key`.
- `NPCManager.restoreMissingNPCsInLoadedChunks()` si `restoreNPCsForChunk(...)` restaureaza numai NPC-urile fara entitate activa.
- Startup-ul ruleaza explicit `restoreMissingNPCsInLoadedChunks()` dupa `discoverExistingVillagers()`, ca entitatile din chunk-uri deja incarcate sa fie reconciliate imediat.
- Startup-ul ruleaza `reconcileDuplicateLiveNPCEntities(...)` dupa descoperirea villagerilor si inainte de restore, astfel entitatile live duplicate dupa acelasi `npc_id` sau `npc_source_key` sunt eliminate/reatasate inainte sa se creeze copii noi. Control: `npc.auto_cleanup_duplicate_entities`.
- Dupa startup, restaurarea initiala si sincronizarea periodica, `NPCManager.enforceControlledEntitySettings(...)` reaplica setarile live pentru villagerii AINPC: AI natural, gravitatie, coliziune, sunet, invulnerabilitate si persistenta.
- `VillagerLifecycleListener` ruleaza `ensureVillagerIsNPC(...)` pentru spawn, chunk load si schimbari de profesie.
- `NPCManager.attachLoadedNPC(...)` cauta entitate existenta inainte sa cheme `npc.spawn()`.
- `NPCManager.findEquivalentActiveNPC(...)` evita spawn-ul daca exista deja un NPC activ cu acelasi nume/display name in aceeasi locatie.
- `/ainpc audit npc` raporteaza entitati live marcate AINPC fara `npc_database_id`, cu `npc_id` inexistent sau duplicate dupa acelasi `npc_id`/`source_key`.
- `/ainpc audit npc` raporteaza si duplicate DB dupa `uuid`, dupa `profile_data.source_key` si dupa acelasi nume in aceeasi locatie apropiata.
- `/ainpc audit db` raporteaza household-uri fara `home_place_id`, referinte household/resident catre place-uri sau node-uri inexistente in mapping, rezidenti orfani, NPC inexistent, mismatch de `source_key`, `resident_count` inconsistent, NPC asignat in mai multe household-uri, pasi de spawn batch fara `household_id`, batch-uri `RUNNING` cu NPC-uri create care cer repair inainte de retry si inconsistente intre statusul batch-ului si statusul pasilor.
- `/ainpc debugdump all|world` exporta `households.json` cu sumar pe source, family, home_place, household si rezidenti pe casa.
- `/ainpc duplicates` ofera raport operational scurt pentru duplicate dupa `source_key`, nume+locatie si entitati live marcate AINPC.
- `/ainpc repair duplicates dryrun|apply` poate sterge randuri DB duplicate dupa `source_key`, poate sterge randuri vechi duplicate dupa acelasi nume in aceeasi locatie apropiata, elimina entitati live duplicate dupa `npc_id`/`source_key` si reconstruieste indexul `npc_source_keys`, pastrand randul canonic.
- `/ainpc delete-id <id> confirm` permite cleanup sigur dupa ID numeric, fara ambiguitatea comenzii dupa nume.
- `/ainpc repair batch list [problem|all|failed|running|rolled_back|succeeded]` listeaza ultimele batch-uri relevante, ca adminul sa gaseasca repede cheia pentru rollback sau sincronizare pasi; pentru batch-uri `RUNNING` cu pasi creatori afiseaza `rollback_pending_steps`.
- `/ainpc repair batch <batchKey> inspect` afiseaza read-only pasii persistati ai batch-ului: place, household, status, NPC-uri create/reutilizate si erori/warning-uri scurte; daca retry-ul este blocat, inspectia afiseaza explicit numarul de pasi creatori jurnalizati si, cand exista, numarul de ID-uri parsabile.
- `/ainpc repair batch <batchKey> dryrun|apply` poate face rollback controlat pentru NPC-urile create de un spawn batch esuat, citind `spawn_batch_steps.created_npc_ids`; `apply` refuza batch-urile `SUCCEEDED`, recalculeaza `resident_count` pentru household-urile afectate si marcheaza pasii cu NPC-uri create ca `ROLLED_BACK`.
- `/ainpc repair batch <batchKey> mark-steps` repara doar statusul pasilor pentru batch-uri deja `ROLLED_BACK`, fara stergere de NPC-uri.
- `/ainpc repair batch <batchKey> mark-failed` inchide manual un batch `RUNNING` ca `FAILED`, fara sa stearga NPC-uri si fara sa modifice pasii; refuza daca exista ID-uri parsabile pentru rollback automat.
- Rerularea unui spawn batch ramas `RUNNING` dupa crash este refuzata daca batch-ul are pasi creatori jurnalizati, ca sa nu fie sters jurnalul de rollback.
- `NpcSpawnResult` diferentiaza NPC-urile create de cele reutilizate, astfel rollback-ul household/settlement sterge doar NPC-urile create in batch-ul curent.
- `NPCManager.deleteNPC(...)` face `despawn()` si sterge randul din `npcs`.
- `NpcSpawnOrchestrator.spawnHousehold(...)` face rollback pentru NPC-urile create in acel batch daca un pas ulterior esueaza.

Limite curente:

- `/ainpc delete <nume>` sterge dupa nume, nu dupa ID; daca exista nume duplicate, comanda poate selecta NPC-ul gresit.
- `households` si `household_residents` exista ca persistenta initiala pentru spawn-urile noi; mai trebuie backfill complet pentru sate vechi.
- `handleEntityDeath(...)` marcheaza entitatea ca indisponibila, dar nu sterge randul DB. Daca NPC-ul nu trebuie sa revina, trebuie sters explicit.
- Cand o entitate NPC moare sau este reatasata dupa reload, `NPCManager` persista imediat profilul runtime fara sa astepte salvarea periodica.
- Rollback-ul batch nu este inca tranzactie completa peste DB, entitati si constructii fizice.

## Cauze comune

Cele mai probabile surse de duplicare:

- spawn direct prin `AINPC.spawn()` in afara `NPCManager`
- retry manual dupa eroare, fara rollback sau fara verificare de idempotenta
- restart/chunk load unde entitatea exista deja, dar codul creeaza alta in loc sa o ataseze
- doua comenzi `/ainpc create` cu acelasi nume in aceeasi locatie
- generator care ruleaza acelasi plan de doua ori
- `HouseAllocation` construit fara `npcKey` stabil
- import/scanner care produce aceleasi `placeId`/`nodeId` sub ID-uri diferite
- backup DB restaurat peste o lume care inca are entitatile vechi
- editare manuala DB in timp ce serverul ruleaza
- stergere de entitate in joc fara stergerea randului DB, urmata de restaurare automata

## Reguli de prevenire

### 1. Un singur punct de spawn

Codul nou nu trebuie sa cheme direct:

```java
npc.spawn();
```

Spawn-ul permanent trebuie sa treaca prin:

```text
NpcSpawnOrchestrator -> NPCManager.createNPCFromPlan -> AINPC.spawn -> save -> register
```

Pentru comenzi manuale:

```text
/ainpc create -> NPCManager.createNPC(...)
```

### 2. Verificare idempotenta inainte de insert

Inainte de orice `INSERT INTO npcs`, fluxul trebuie sa verifice:

1. exista deja NPC dupa entity UUID?
2. exista deja NPC dupa `npcs.uuid`?
3. exista un villager legacy cu acelasi nume si aceeasi locatie?
4. exista un NPC activ echivalent in aceeasi locatie?
5. planul are un `npcKey`/`sourceKey` care a fost deja commit-uit?

Profilul nou se creeaza doar daca toate verificarile sunt negative.

### 3. Planuri stabile, nu spawn random

Pentru generator si spawn-order:

- foloseste `HouseAllocation`
- fiecare rezident are `npcKey` stabil
- fiecare NPC are `spawnNodeId`
- fiecare casa are `placeId`
- fiecare ocupatie reala are `workPlaceId` sau `workNodeId`
- `dryrun` si `commit` folosesc aceeasi validare

Nu rula generatorul direct in lume fara plan validat.

### 4. Fara retry orb

Daca spawn-ul a esuat:

1. citeste eroarea
2. ruleaza audit/debugdump
3. verifica daca batch-ul a creat deja ceva
4. ruleaza rollback sau cleanup
5. abia apoi repeta cu acelasi plan sau cu plan corectat

Nu apasa repetat aceeasi comanda de spawn pentru ca "poate merge a doua oara".

### 5. Startup si chunk load in ordine controlata

Ordinea corecta la startup/chunk load:

```text
load DB -> discover existing villagers -> attach existing entities
-> cleanup duplicate live entities -> restore only missing NPCs -> rebalance population
```

Daca se inverseaza ordinea, pluginul poate spawna o copie inainte sa observe entitatea existenta.

### 6. Nu edita DB live

Pentru cleanup manual:

- opreste serverul
- fa backup la `plugins/AINPC/ainpc_data.db`
- foloseste SQLite cu `PRAGMA foreign_keys = ON`
- porneste serverul doar dupa verificarea query-urilor

## Procedura de diagnostic

### 1. Opreste sursa de duplicate

Pana identifici cauza:

- nu mai rula generatorul
- nu mai rula acelasi spawn plan
- evita `/ainpc create` in zona afectata
- nu sterge random villageri din lume

### 2. Colecteaza starea din joc

Ruleaza:

```text
/ainpc list
/ainpc audit npc
/ainpc audit spawn
/ainpc duplicates
/ainpc repair duplicates dryrun
/ainpc repair batch list problem
/ainpc repair batch <batchKey> inspect
/ainpc repair batch <batchKey> dryrun
/ainpc debugdump npc
```

Ruleaza `apply` numai dupa ce `dryrun` arata exact actiunile asteptate:

```text
/ainpc repair duplicates apply
/ainpc repair batch <batchKey> apply
```

Verifica in dump:

```text
plugins/AINPC/debug-dumps/<dump>/npcs.json
plugins/AINPC/debug-dumps/<dump>/audit.txt
plugins/AINPC/debug-dumps/<dump>/recent-server-log.txt
```

Cauta mesaje relevante:

```text
Elimin villager duplicat pentru NPC-ul
Sar peste spawn pentru NPC-ul
asteapta incarcarea chunk-ului pentru restaurare
a ramas fara entitate activa dupa moarte
```

### 3. Verifica DB-ul

Cu serverul oprit si DB backup facut, ruleaza intr-un client SQLite:

```sql
PRAGMA foreign_keys = ON;
```

Lista NPC-uri:

```sql
SELECT id, uuid, name, display_name, occupation, world, x, y, z
FROM npcs
ORDER BY world, name, x, y, z, id;
```

Duplicate de UUID:

```sql
SELECT uuid, COUNT(*) AS count
FROM npcs
GROUP BY uuid
HAVING COUNT(*) > 1;
```

Nume si locatii foarte apropiate:

```sql
SELECT lower(name) AS name_key,
       world,
       round(x * 2) / 2 AS rx,
       round(y * 2) / 2 AS ry,
       round(z * 2) / 2 AS rz,
       COUNT(*) AS count,
       group_concat(id) AS ids
FROM npcs
GROUP BY lower(name), world, rx, ry, rz
HAVING COUNT(*) > 1;
```

Profiluri pentru candidatii gasiti:

```sql
SELECT n.id,
       n.uuid,
       n.name,
       n.world,
       n.x,
       n.y,
       n.z,
       p.profile_source,
       p.profile_summary
FROM npcs n
LEFT JOIN npc_profiles p ON p.npc_id = n.id
WHERE n.id IN (1, 2)
ORDER BY n.id;
```

Inlocuieste `1, 2` cu ID-urile suspecte.

## Clasificare rapida

| Ce vezi | Interpretare | Actiune |
|---|---|---|
| Un rand DB, doua entitati in lume | Duplicare de entitate | Sterge doar entitatea extra |
| Doua randuri DB, doua entitati similare | Duplicare completa | Pastreaza un rand, sterge randul pierzator controlat |
| Doua randuri DB similare, unul inactiv | Duplicare DB latenta | Sterge randul necanonic daca nu este un NPC mort intentionat |
| Doua nume identice in locatii diferite | Coliziune de nume | Nu sterge dupa nume; foloseste `/ainpc delete-id <id> confirm` daca este cleanup real |
| Duplicatul reapare dupa restart | DB/plan/generator inca creeaza copia | Nu mai curata manual pana nu gasesti sursa |

## Remediere: entitate duplicata, DB corect

Foloseste acest flux cand exista un singur rand in `npcs`, dar doua villager entities in lume.

1. Ruleaza `/ainpc debugdump npc`.
2. Verifica in `npcs.json` UUID-ul NPC-ului canonic.
3. Intra in chunk-ul cu duplicatul si asteapta cateva secunde; `VillagerLifecycleListener` poate reatasa sau elimina automat duplicatele legacy.
4. Daca duplicatul ramane, elimina doar entitatea extra, nu randul DB.
5. Ruleaza `/ainpc audit npc`.
6. Reporneste serverul si incarca acelasi chunk.
7. Verifica daca entitatea extra nu revine.

Atentie:

- Nu folosi comenzi globale de tip kill pe toti villagerii.
- Nu folosi `/ainpc delete <nume>` pentru acest caz; acea comanda sterge randul DB si NPC-ul canonic.

## Remediere: duplicate DB + entitate

Foloseste acest flux cand doua randuri din `npcs` reprezinta acelasi NPC.

### 1. Alege randul canonic

Pastreaza randul care are:

- questuri active sau progres important
- relatii/familie corecte
- `profile_data` mai complet
- `home/work/social anchors` corecte
- UUID-ul entitatii care trebuie sa ramana in lume
- ID-ul referit de `owner_npc_id`, `metadata.residents` sau alte binding-uri

### 2. Daca numele este unic

Poti folosi:

```text
/ainpc delete <nume>
```

Dar foloseste comanda doar daca esti sigur ca `getNPCByName(...)` nu poate alege alt NPC cu acelasi nume.

### 3. Daca numele este duplicat

Nu folosi `/ainpc delete <nume>`.

Flux recomandat:

1. opreste serverul
2. fa backup la `plugins/AINPC/ainpc_data.db`
3. identifica `loser_id`
4. ruleaza cleanup SQL cu foreign keys active
5. porneste serverul
6. ruleaza audit

Exemplu:

```sql
PRAGMA foreign_keys = ON;

SELECT id, uuid, name, world, x, y, z
FROM npcs
WHERE id IN (12, 13);

DELETE FROM npcs
WHERE id = 13;
```

Stergerea din `npcs` trebuie sa curete automat datele dependente care au `ON DELETE CASCADE`. Verifica totusi dupa pornire:

```text
/ainpc audit all
/ainpc debugdump npc
```

### 4. Curata entitatea ramasa

Dupa stergerea DB, daca entitatea fizica a randului pierzator ramane in lume, sterge doar entitatea extra din zona afectata. Nu sterge NPC-ul canonic.

## Remediere: NPC mort care reapare

`EntityDeathEvent` nu sterge randul DB. Codul marcheaza NPC-ul ca fara entitate activa, astfel incat datele lui pot ramane pentru restaurare.

Daca moartea trebuie sa fie permanenta:

```text
/ainpc delete <nume>
```

Daca numele este ambiguu, foloseste procedura DB cu serverul oprit si stergere dupa ID.

Daca moartea nu trebuie sa fie permanenta, reaparitia nu este bug de duplicare; este comportament de persistenta.

## Remediere: plan/generator rulat de doua ori

Daca sursa este un plan rulat de doua ori:

1. opreste rularile automate
2. identifica NPC-urile create de batch-ul gresit
3. pastreaza doar setul canonic
4. sterge setul repetat
5. marcheaza planul ca rulat sau arunca-l
6. adauga protectie de idempotenta in generator

Pentru fazele urmatoare, fiecare commit de plan trebuie sa aiba:

```text
planId
batchId
npcKey
source
sourceKey
status
```

Regula:

```text
acelasi planId + acelasi npcKey nu poate crea doua randuri npcs
```

## Verificare dupa fix

Checklist obligatoriu:

1. `/ainpc list` arata numarul asteptat
2. `/ainpc audit npc` nu raporteaza duplicate/UUID/profil lipsa
3. `/ainpc audit spawn` nu raporteaza rezidenti neclari sau case supraincarcate
4. `npcs.json` din debugdump contine o singura intrare pentru NPC-ul reparat
5. server restart complet
6. incarci chunk-ul afectat
7. verifici ca duplicatele nu reapar
8. verifici logul pentru mesaje repetate de skip/eliminare duplicate

Daca duplicatul reapare dupa restart, cleanup-ul manual nu este suficient. Trebuie gasita sursa: plan repetat, DB vechi, entitate ramasa in lume, import duplicat sau alt plugin.

## Teste recomandate pentru bug fix

Cand modifici cod pentru duplicare, adauga teste unde logica este determinista:

- `ensureVillagerIsNPC(...)` apelat de doua ori pentru acelasi villager nu creeaza doua randuri DB
- restore pe chunk cu entitate existenta nu apeleaza spawn nou
- match legacy dupa nume/locatie reataseaza NPC-ul, nu creeaza profil nou
- NPC activ echivalent blocheaza spawn-ul unei copii
- batch rollback sterge doar NPC-urile create in batch
- retry acelasi plan nu creeaza aceleasi `npcKey` de doua ori

Pentru server Paper real, smoke test minim:

```text
1. creeaza sau importa un sat
2. creeaza un NPC
3. ruleaza /ainpc list si /ainpc audit npc
4. restart server
5. incarca acelasi chunk
6. ruleaza iar /ainpc list si /ainpc audit npc
7. verifica vizual ca NPC-ul exista o singura data
```

## Fazele urmatoare

### Faza 1 - Validare Paper dupa restart

Status: `URMATOR IMEDIAT`.

Scop:

- confirma ca `npc_source_keys` este creat automat pe DB existent;
- ruleaza `/ainpc duplicates` inainte si dupa restart;
- ruleaza `/ainpc repair duplicates dryrun` si aplica doar daca actiunile sunt corecte;
- verifica vizual ca acelasi `source_key` nu creeaza entitate noua la chunk reload.

Criteriu de gata:

- `/ainpc audit npc` nu raporteaza duplicate dupa `npc_id` sau `source_key`;
- `/ainpc audit db` raporteaza `npc_source_keys` fara randuri orfane;
- restart + incarcare chunk nu schimba numarul de NPC-uri.

### Faza 2 - Migrare legacy controlata

Status: `URMATOR`.

Scop:

- snapshot/backup inainte de cleanup;
- dry-run pentru randuri DB duplicate dupa `source_key`;
- dry-run pentru villageri marcati AINPC fara rand DB;
- cleanup doar prin `/ainpc repair duplicates apply` si `/ainpc delete-id <id> confirm`.

Criteriu de gata:

- duplicatele vechi sunt reduse la un rand canonic;
- entitatile live ramase au `npc_database_id`, `npc_uuid` si `npc_source_key`;
- nu exista cleanup manual direct in SQLite cu serverul pornit.

### Faza 3 - Batch idempotent pentru spawn pe regiune

Status: `INCEPUT IN COD`.

Scop:

- tabela `spawn_batches` cu status, plan hash si pasi executati;
- retry pentru acelasi settlement/household fara NPC-uri noi;
- rollback explicit pentru pasii partiali;
- raport in debugdump cu planul, NPC-urile create si NPC-urile reutilizate.

Implementat initial:

- `SpawnBatchPlanHasher` calculeaza determinist `plan_hash` si `batch_key` din `HouseAllocation`;
- `NpcSpawnOrchestrator` scrie batch-ul real de settlement spawn in `spawn_batches`;
- fiecare household executat este inregistrat in `spawn_batch_steps`;
- rollback-ul global settlement citeste NPC-urile create din `spawn_batch_steps.created_npc_ids`, nu doar din lista runtime in memorie;
- `/ainpc audit db` raporteaza numarul de batch-uri, batch-urile `RUNNING`, `FAILED` sau `ROLLED_BACK` si inconsistentele batch/pasi;
- debug dump-ul `all`/`world` include `spawn-batches.json` cu ultimele batch-uri si pasii lor;
- rerularea unui batch finalizat produce warning si continua idempotent prin reutilizare dupa `source_key`;
- rerularea unui batch `RUNNING` cu pasi creatori jurnalizati este blocata pana la rollback/repair controlat.

Criteriu de gata:

- acelasi `settlement spawn` poate fi rulat de doua ori fara duplicate;
- esecul la household intermediar nu sterge NPC-uri existente inainte de batch;
- rollback-ul sterge doar NPC-urile create in batch-ul curent.

### Faza 4 - Household si resident IDs persistente

Status: `INCEPUT IN COD`.

Scop:

- tabele mature `households` si `household_residents`;
- constrangere unica pe resident logic in household;
- legatura explicita intre household resident, `npc_source_keys` si `npc_world_bindings`;
- audit pentru rezidenti orfani sau mutati in alta casa.

Implementat initial:

- `HouseAllocation.householdId()` produce ID stabil din `familyId` sau `homePlaceId`;
- `HouseholdPersistenceService` face upsert in `households` si `household_residents` dupa spawn household;
- fiecare resident salvat are `resident_key`, `npc_id`, `npc_uuid`, `npc_name`, `source_key`, noduri si place-uri semantice;
- daca acelasi NPC este mutat in alt household, randul vechi de rezident este eliminat inainte de upsert;
- `/ainpc audit db` raporteaza rezidenti orfani, NPC-uri lipsa, `source_key` nealiniat si count-uri incorecte;
- debug dump-ul `all`/`world` include `households.json`.

Criteriu de gata:

- fiecare resident planificat are un singur NPC canonic;
- schimbarea casei/workplace nu recreeaza NPC-ul;
- auditul explica orice conflict de resident.

### Faza 5 - Observabilitate si release gate

Status: `URMATOR`.

Scop:

- debugdump extins cu entity UUID separat de DB UUID;
- raport sumar pentru `npc_source_keys`, `npc_world_bindings`, household si batch;
- smoke test obligatoriu pe Paper inainte de release public;
- checklist de restart: spawn, audit, repair dryrun, debugdump.

Criteriu de gata:

- release-ul este blocat daca `/ainpc duplicates` raporteaza probleme;
- logurile nu contin mesaje repetate de restaurare/skip pentru acelasi NPC;
- testul Paper confirma NPC-uri stabile, cu gravitatie si miscare naturala.

## Hardening recomandat

Prioritate mare:

- validare Paper dupa restart pentru `npc_source_keys`
- migrare legacy controlata cu backup si dry-run
- tabela `spawn_batches` cu status si rollback steps
- constrangere unica pe batch/rezident pentru NPC-urile create din planuri
- debugdump extins cu entity UUID separat de DB UUID

Prioritate medie:

- audit care grupeaza NPC-uri dupa nume normalizat + locatie apropiata
- raport de entitati villager fara rand DB si randuri DB fara entitate
- selectie admin dupa ID peste tot unde azi se foloseste doar nume

## Anti-patterns

Evita:

- spawn direct in lume fara `NPCManager`
- retry de comanda fara audit
- cleanup DB cu serverul pornit
- `/ainpc delete <nume>` cand exista nume duplicate
- stergere globala de villageri
- generator care foloseste timestamp ca identitate de NPC
- planuri fara `npcKey`
- rollback care sterge NPC-uri existente inainte de batch
- import care produce aceleasi case/nodes sub ID-uri noi

## Concluzie

Prevenirea duplicarii inseamna idempotenta la fiecare nivel: DB, entitate Bukkit, plan de spawn, household si batch. Fixul corect nu este "sterge ce vezi", ci identificarea sursei, alegerea randului canonic, cleanup controlat si verificare dupa restart.
