# Prevenire si Remediere Duplicare NPC

Actualizat: 2026-05-01

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
- `NPCManager.restoreMissingNPCsInLoadedChunks()` si `restoreNPCsForChunk(...)` restaureaza numai NPC-urile fara entitate activa.
- `VillagerLifecycleListener` ruleaza `ensureVillagerIsNPC(...)` pentru spawn, chunk load si schimbari de profesie.
- `NPCManager.ensureVillagerIsNPC(...)` verifica in ordine: entity UUID, UUID DB, match legacy dupa nume/locatie, NPC activ echivalent, apoi creeaza profil nou.
- `NPCManager.attachLoadedNPC(...)` cauta entitate existenta inainte sa cheme `npc.spawn()`.
- `NPCManager.findEquivalentActiveNPC(...)` evita spawn-ul daca exista deja un NPC activ cu acelasi nume/display name in aceeasi locatie.
- `NPCManager.deleteNPC(...)` face `despawn()` si sterge randul din `npcs`.
- `NpcSpawnOrchestrator.spawnHousehold(...)` face rollback pentru NPC-urile create in acel batch daca un pas ulterior esueaza.

Limite curente:

- `/ainpc delete <nume>` sterge dupa nume, nu dupa ID; daca exista nume duplicate, comanda poate selecta NPC-ul gresit.
- Marcarea unui villager ca "al pluginului" este inferata din `!hasAI`, `isInvulnerable` sau `isSilent`; nu exista inca marker persistent dedicat pe entitate.
- Nu exista inca tabele mature `spawn_batches`, `households`, `household_residents` si `npc_world_bindings`.
- `handleEntityDeath(...)` marcheaza entitatea ca indisponibila, dar nu sterge randul DB. Daca NPC-ul nu trebuie sa revina, trebuie sters explicit.
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
-> restore only missing NPCs -> rebalance population
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
/ainpc debugdump npc
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
| Doua nume identice in locatii diferite | Coliziune de nume | Nu sterge; redenumeste sau adauga comanda delete-by-id in backlog |
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

## Hardening recomandat

Prioritate mare:

- comanda `/ainpc duplicates` read-only
- comanda `/ainpc delete-id <id>` pentru cleanup sigur
- comanda `/ainpc repair duplicates dryrun|apply`
- marker persistent pe entitate cu `PersistentDataContainer`: `ainpc:npc_id`, `ainpc:uuid`, `ainpc:source_key`
- tabela `npc_world_bindings`
- tabela `spawn_batches` cu status si rollback steps
- constrangere unica pentru `source + sourceKey` la NPC-urile create din planuri
- debugdump extins cu entity UUID separat de DB UUID

Prioritate medie:

- audit care grupeaza NPC-uri dupa nume normalizat + locatie apropiata
- migration dry-run pentru duplicate legacy
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
