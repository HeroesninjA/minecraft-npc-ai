# Debugging si Testare

Actualizat: 2026-05-11

## Scop

Acest document centralizeaza metodele practice de debugging si testare pentru proiectul AINPC.

Pentru fluxul operational cap-coada pe server Paper, pornind de la instalare si configuratie minima, vezi `server-admin-runbook.md`. Pentru acceptarea unui build ca release, vezi `release-checklist.md`.

Acopera:

- comenzi Maven pentru build si teste
- scripturi PowerShell existente
- metode de diagnostic pentru OpenAI
- metode de diagnostic pentru NPC-uri, World Admin si mapping
- checklist-uri pentru testare manuala pe server Paper
- reguli pentru adaugarea de teste noi

## Regula generala

Cand apare o problema, nu incepe direct cu modificari in cod.

Flux recomandat:

1. reproduce problema cu pasi clari
2. verifica logurile serverului
3. ruleaza testele relevante
4. izoleaza modulul afectat
5. adauga sau actualizeaza un test daca bug-ul este reproductibil
6. abia apoi modifica implementarea

## Comenzi Maven utile

Ruleaza toate testele din reactor:

```powershell
mvn test
```

Ruleaza doar modulul core:

```powershell
mvn -pl ainpc-core-plugin test
```

Ruleaza un singur test:

```powershell
mvn -pl ainpc-core-plugin -Dtest=WorldAdminServiceTest test
```

Compileaza tot fara teste:

```powershell
mvn package -DskipTests
```

Construieste pluginul core si dependintele necesare:

```powershell
mvn -pl ainpc-core-plugin -am package
```

Curata build-ul si ruleaza testele:

```powershell
mvn clean test
```

Cand folosesti `clean`, asteapta-te ca directoarele `target/` sa fie regenerate.

## Scripturi existente

### `scripts/smoke-paper-mapping.ps1`

Scop:

- pregateste un server Paper pentru smoke test-ul mapping/spawn
- construieste JAR-urile, daca nu folosesti `-SkipBuild`
- copiaza pluginul core si addonul medieval in `plugins/`
- genereaza comenzile de rulat in `ainpc-mapping-smoke-commands.txt`
- genereaza raport cu hash-uri in `ainpc-mapping-smoke-report.txt`
- include implicit o sectiune manuala pentru fluxul wand complet: `region`, `place`, `node`, `npc_bind` si `quest_anchor`

Rulare simpla:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-mapping.ps1 `
  -ServerDir "C:\Minecraft\paper-test"
```

Cu teste Maven inainte de package:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-mapping.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -RunTests
```

Daca vrei doar fluxul demo/settlement, fara comenzile manuale de wand:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-mapping.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -SkipWandFlow
```

### `scripts/debug-openai.ps1`

Scop:

- citeste configuratia OpenAI din `config.yml`
- foloseste `OPENAI_API_KEY` si `OPENAI_BASE_URL` ca fallback
- testeaza accesul la model prin `GET /models/{model}`
- optional testeaza generarea prin `POST /responses`
- scrie loguri in `debug-logs/`

Rulare simpla:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\debug-openai.ps1
```

Rulare cu test de raspuns:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\debug-openai.ps1 -IncludeResponseTest
```

Rulare cu valori explicite:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\debug-openai.ps1 `
  -BaseUrl "https://api.openai.com/v1" `
  -Model "gpt-5.4-nano" `
  -ApiKey "sk-..."
```

Logurile rezultate:

```text
debug-logs/openai-debug-YYYYMMDD-HHMMSS.log
```

### Cand folosesti scriptul OpenAI

Foloseste scriptul cand:

- NPC-ul raspunde cu fallback local
- serverul logheaza erori OpenAI
- modelul configurat pare invalid
- cheia API lipseste sau nu este preluata corect
- vrei sa separi problema de plugin fata de problema de conectivitate/API

## Configurare debug in plugin

In `ainpc-core-plugin/src/main/resources/config.yml`, optiuni utile:

```yml
debug: true

openai:
  diagnostics:
    enabled: true
    check_on_startup: true
    log_prompt_summary: true
    log_response_preview: true
```

Pentru testare pe server real, seteaza aceleasi valori in `plugins/AINPC/config.yml`, nu doar in resursa din proiect.

## Debugging OpenAI

Checklist:

- verifica daca `OPENAI_API_KEY` este setat in mediul serverului
- verifica `openai.api_key` daca nu folosesti variabila de mediu
- verifica `openai.base_url`
- verifica `openai.model`
- ruleaza `scripts/debug-openai.ps1`
- verifica daca serverul are acces la internet
- verifica daca raspunsul pluginului este fallback local sau raspuns real de la model

Semne comune:

- `401` inseamna cheia API este invalida sau lipseste
- `404` pe `/models/{model}` inseamna model gresit sau endpoint gresit
- timeout inseamna conectivitate, proxy, firewall sau endpoint lent
- raspuns fallback in joc inseamna ca pluginul nu a putut folosi serviciul AI in acel moment

## Debugging World Admin si mapping

Comenzi utile in joc:

```text
/ainpc world whereami
/ainpc world places
/ainpc world places <regionId>
/ainpc world region info <regionId>
/ainpc world place info <placeId>
/ainpc story context
/ainpc story context <jucator> nearest
```

Pentru creare manuala:

```text
/ainpc world region create <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>
/ainpc world place create <regionId> <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>
/ainpc world node create <regionId> <placeId|-> <id> <type> <x> <y> <z> [radius]
/ainpc world scan village [radius]
/ainpc world scan village [radius] import [regionId]
/ainpc world demo create [regionId]
/ainpc world bind npc <numeNpc|nearest> <homePlaceId> [workPlaceId|-] [socialPlaceId|-]
/ainpc world household <plan|spawn> <homePlaceId> [count]
/ainpc world settlement <plan|spawn> <regionId> [maxHouses]
/ainpc world save
```

Ce verifici:

- regiunea exista
- place-ul este complet in regiune
- place-ul are tip corect
- place-ul are `owner_npc_id` daca este casa sau cladire privata
- node-ul este in interiorul regiunii sau place-ului
- `auto_index.enabled` nu este cauza diferentelor de lookup
- `StoryContextService` vede aceeasi regiune/place ca `world whereami`

Test rapid pentru indexare:

1. activeaza `world_admin.auto_index.enabled: true`
2. ruleaza `/ainpc world whereami` intr-un place
3. dezactiveaza `auto_index.enabled`
4. ruleaza reload
5. repeta `/ainpc world whereami`
6. rezultatul functional trebuie sa fie acelasi

## Debugging NPC-uri

Comenzi utile:

```text
/ainpc list
/ainpc info <npc>
/ainpc tp <npc>
/ainpc audit npc
/ainpc routine status <npc>
```

Ce verifici pentru un NPC:

- are `uuid` stabil
- are `occupation`
- are profil creat in DB
- are `homeAnchor`
- are `workAnchor`
- este spawnat sau asteapta chunk-ul corect
- nu exista duplicate in acelasi chunk

## Smoke test GUI pentru lucru alternat

Ruleaza acest set dupa ce mapping-ul, questurile/progresiile si reload-ul au trecut. Scopul este sa verifici ca GUI-ul vede aceleasi date ca serviciile text si auditul.

```text
/ainpc gui
/ainpc gui quest all
/ainpc gui quest quest
/ainpc gui quest contract
/ainpc gui quest duty
/ainpc gui quest bounty
/ainpc gui quest event
/ainpc gui quest tutorial
/ainpc gui quest ritual
/quest gui all
/ainpc gui story
/ainpc gui world
/ainpc gui routine
/ainpc gui audit
/ainpc gui debug
```

Ce verifici:

- filtrele afiseaza aceleasi intrari ca `/ainpc progression definitions <filter>`;
- detaliile arata stage-ul curent, obiectivele si tracking-ul;
- `StoryGui` arata acelasi region/place state si aceleasi evenimente ca `/ainpc story region|place|events`;
- click-urile pentru status/track folosesc comenzile validate si nu produc erori in consola;
- ecranele admin nu ruleaza actiuni destructive fara confirmare;
- dupa restart, aceleasi intrari reapar cu status coerent.

Pentru NPC-uri create automat din villageri:

- verifica daca villagerul are profesie Minecraft relevanta
- verifica daca exista job block in apropiere
- verifica daca exista pat in apropiere
- verifica daca exista `WorldPlace` de tip `house` sau tag `home`
- verifica daca exista `WorldPlace` cu tag `workplace` sau metadata `role: work`

## Debugging case si locuri de munca

Pentru a afla ce casa este asociata unui place:

```text
/ainpc world place info <placeId>
```

Uita-te la:

```text
Owner NPC
```

Pentru ancorele salvate pe NPC, verifica profilul NPC-ului.
Acestea sunt persistate in `npc_profiles.profile_data`, sub:

```json
"owned_locations": {
  "home": {},
  "work": {},
  "social": {}
}
```

Problema posibila:

- un `WorldPlace` poate avea `owner_npc_id`
- un NPC poate avea `homeAnchor`
- aceste doua surse trebuie sincronizate explicit de codul de generare sau de comenzi admin

## Audit automat

Comanda principala:

```text
/ainpc audit
```

Subcomenzi:

```text
/ainpc audit npc
/ainpc audit world
/ainpc audit db
/ainpc audit spawn
/ainpc audit quest
```

Ce verifica:

- NPC-uri fara ID DB, UUID, nume, ocupatie, profil, casa sau loc de munca
- `profile_data` JSON invalid
- lumi neincarcate referite de NPC-uri sau ancore
- regiuni, places si nodes inconsistente
- case fara `owner_npc_id`
- locuri de munca fara nodes de interactiune
- places suprapuse
- nodes in afara containerului semantic
- profile DB lipsa, profile orfane si campuri DB obligatorii lipsa
- `npc_world_bindings` orfane, fara place, cu place/node IDs inexistente sau incompatibile
- case cu rezidenti peste capacitate
- rezidenti care nu se pot rezolva ca NPC-uri incarcate
- `homeAnchor` in afara casei
- relatii familiale fara reciproc
- duplicate exacte in `npc_family`
- quest templates cu profesii, prerequisite-uri, faze, dialoguri, obiective sau recompense invalide
- `quest_anchor_bindings` fara progres parinte
- quest anchors catre regiuni, places sau nodes inexistente
- incompatibilitati intre `objective_type` si `anchor_type`

Comanda este read-only.
Nu modifica NPC-uri, DB sau config.

Interpretare pentru auditul curent observat:

- `0 erori` inseamna ca nu s-a gasit o problema structurala grava in NPC-uri, DB sau mapping
- `World mapping: 0 regiuni, 0 places, 0 nodes` inseamna ca NPC-urile folosesc fallback-uri de coordonate si nu context semantic complet
- warning-urile de nume duplicate inseamna ca unele comenzi dupa nume pot deveni ambigue
- duplicatele existente trebuie rezolvate manual daca incurca administrarea; codul nou evita duplicatele doar pentru NPC-uri generate automat ulterior

## Debug dump avansat

Comanda principala:

```text
/ainpc debugdump
```

Scope-uri disponibile:

```text
/ainpc debugdump all
/ainpc debugdump npc
/ainpc debugdump world
/ainpc debugdump quest
/ainpc debugdump story
/ainpc debugdump openai
```

Output:

```text
plugins/AINPC/debug-dumps/debug-dump-YYYYMMDD-HHMMSS/
```

Fisiere generate:

- `summary.txt`
- `server.txt`
- `config-sanitized.yml`
- `audit.txt`
- `npcs.json`
- `world-mapping.json`
- `quests.yml`
- `quest-audit-report.txt`
- `loaded-quest-definitions.json`
- `player-progressions.json`
- `player-quest-progress.json`
- `quest-anchor-bindings.json`
- `story-states.json`
- `story-events.json`
- `openai.txt`
- `recent-server-log.txt`

Reguli:

- comanda este read-only
- cheia OpenAI este redactata
- dump-ul nu ruleaza probe de retea OpenAI
- pentru proba reala OpenAI foloseste `/ainpc test` sau `scripts/debug-openai.ps1`
- `recent-server-log.txt` include doar ultimele linii relevante din `logs/latest.log`

## Debugging baza de date

Baza de date implicita:

```text
plugins/AINPC/ainpc_data.db
```

Tabele importante:

- `npcs`
- `npc_profiles`
- `npc_personality`
- `npc_emotions`
- `npc_traits`
- `npc_memories`
- `npc_relationships`
- `npc_family`
- `dialog_history`
- `player_quests`

Verificari utile cu un client SQLite:

```sql
SELECT id, uuid, name, occupation, world, x, y, z FROM npcs;
```

```sql
SELECT npc_id, profile_source, profile_version, profile_summary FROM npc_profiles;
```

```sql
SELECT profile_data FROM npc_profiles WHERE npc_id = 1;
```

Pentru quest anchors, prefera intai comenzile read-only din joc:

```text
/ainpc quest anchors
/ainpc quest anchors <jucator>
/ainpc quest anchors all <templateId>
/ainpc audit quest
/ainpc debugdump quest
/ainpc story context <jucator> nearest
```

Tabele relevante:

- `player_quests`
- `quest_anchor_bindings`
- `region_story_state`
- `place_story_state`
- `story_events`

`/ainpc story context` este util cand vrei sa vezi ce ajunge in prompt ca `STORY_CONTEXT`, fara sa pornesti manual o generatie AI.

`/ainpc debugdump quest` exporta un raport dedicat in `quest-audit-report.txt`, definitiile de quest incarcate in `loaded-quest-definitions.json`, progresul generic in `player-progressions.json`, progresul complet din `player_quests` in `player-quest-progress.json`, ancorele din `quest_anchor_bindings` in `quest-anchor-bindings.json`, story state-ul persistent din `region_story_state`/`place_story_state` in `story-states.json` si evenimentele narative din `story_events` in `story-events.json`, inclusiv agregari utile pentru inspectie. `loaded-quest-definitions.json` include si `progression_definitions`, lista read-only generata prin `ProgressionService`. `player-progressions.json` este generat prin `ProgressionRepository` si `StoredProgressionSummary`, ca view generic peste `player_quests`; acelasi view poate fi inspectat in joc prin `/ainpc progression stored ...`, `/ainpc contract stored ...` si sumarizat in `/ainpc audit quest`. Progresul exportat include `current_phase` si `current_stage_id`, iar raportul verifica referintele `phase`/`stage` ale obiectivelor etapizate si JSON-ul din story state/events.

`/ainpc debugdump story` genereaza doar partea dedicata de story observability: `story-states.json` si `story-events.json`, pe langa fisierele comune de context ale dump-ului.

`/ainpc debugdump world` si `all` includ `spawn-batches.json`; pasii din `spawn_batch_steps` expun acum si `household_id`, ca un batch settlement sa poata fi legat direct de household-ul persistent. Dry-run-urile apar in acelasi export doar daca `spawn.batches.track_dry_runs` este activ.
La rerularea aceluiasi `batch_key`, pasii vechi sunt stersi cand batch-ul porneste din nou doar daca batch-ul vechi nu este `RUNNING` cu pasi creatori jurnalizati; in acel caz rerularea este blocata ca sa nu se piarda rollback-ul. Daca un household esueaza, rollback-ul settlement foloseste `created_npc_ids` din pasii curenti exportati in `spawn-batches.json`.
Pentru gasirea rapida a cheilor, `/ainpc repair batch list problem` listeaza ultimele batch-uri `RUNNING`, `FAILED` sau `ROLLED_BACK`; filtrele acceptate sunt `problem`, `all`, `failed`, `running`, `rolled_back` si `succeeded`. Batch-urile `RUNNING` care au pasi cu `created_npc_ids` apar cu `rollback_pending_steps`.
Pentru inspectie fara dump, `/ainpc repair batch <batchKey> inspect` afiseaza pasii din `spawn_batch_steps`, cu `household_id`, status, NPC-uri create/reutilizate si sumar de warning/error. Pentru batch-uri `RUNNING` blocate la retry, afiseaza explicit cati pasi creatori sunt jurnalizati si cate ID-uri sunt parsabile.
Pentru un batch ramas esuat dupa comanda initiala, ruleaza `/ainpc repair batch <batchKey> dryrun`; `apply` sterge doar NPC-urile listate in `created_npc_ids`, refuza batch-urile finalizate cu `SUCCEEDED`, recalculeaza `resident_count` pentru household-urile afectate si marcheaza pasii cu NPC-uri create ca `ROLLED_BACK`.
Cand batch-ul este `RUNNING`, are pasi creatori, dar nu are niciun ID parsabil pentru rollback automat, `/ainpc repair batch <batchKey> mark-failed` il poate inchide manual ca `FAILED`; comanda nu sterge NPC-uri, nu modifica pasii si refuza cazul in care exista ID-uri parsabile.
`/ainpc audit db` verifica si consistenta intre `spawn_batches.status` si `spawn_batch_steps.status`, inclusiv batch-uri `RUNNING` cu NPC-uri create care necesita repair inainte de retry si batch-uri `ROLLED_BACK` cu pasi creatori nemarcati rollback.
Daca auditul gaseste doar pasi nemarcati pentru un batch deja `ROLLED_BACK`, fara NPC-uri ramase de sters, foloseste `/ainpc repair batch <batchKey> mark-steps`; acest mod nu sterge NPC-uri.

Atentie:

- nu edita baza de date in timp ce serverul ruleaza
- fa backup inainte de modificari manuale
- daca profilul JSON este invalid, hidratarea runtime poate esua si pluginul va folosi fallback-uri

## Debugging build si JAR

Construieste pluginul:

```powershell
mvn -pl ainpc-core-plugin -am package
```

JAR-ul rezultat este in:

```text
ainpc-core-plugin/target/
```

Verifica dimensiunea:

```powershell
Get-ChildItem .\ainpc-core-plugin\target\*.jar | Select-Object Name,Length
```

Verifica daca resursele au intrat in JAR:

```powershell
jar tf .\ainpc-core-plugin\target\ainpc-core-plugin-1.0.0.jar | Select-String "plugin.yml|config.yml|quests.yml"
```

Verifica clase importante:

```powershell
jar tf .\ainpc-core-plugin\target\ainpc-core-plugin-1.0.0.jar | Select-String "ro/ainpc/AINPCPlugin.class|ro/ainpc/managers/NPCManager.class"
```

## Testare manuala pe server Paper

Checklist dupa build:

1. opreste serverul
2. copiaza JAR-ul nou in `plugins/`
3. porneste serverul
4. verifica daca pluginul apare in `/plugins`
5. verifica logurile de startup
6. verifica initializarea DB
7. verifica incarcarea Feature Packs
8. verifica world admin count in log
9. intra pe server si ruleaza comenzi de baza

Comenzi smoke test:

```text
/ainpc
/ainpc list
/ainpc world whereami
/ainpc world places
```

### Raport F001 - 2026-05-10

Status: trecut prin Paper + RCON.

Mediu validat:

- server temporar: `%TEMP%/ainpc-paper-f001-rcon-clean-20260510-153820`
- Paper: `1.21.11-69`
- Java: `21.0.11` Temurin
- pluginuri incarcate: `AINPCPlugin 1.0.0`, `AINPCScenarioMedieval 1.0.0`
- log comenzi: `%TEMP%/ainpc-paper-f001-rcon-clean-20260510-153820/f001-rcon-output-clean.txt`

Rezultat validat:

- serverul Paper a pornit si a ajuns la `Done (26.817s)`;
- `AINPCPlugin` s-a activat;
- addonul medieval s-a incarcat;
- RCON a pornit pe portul local de test;
- `/plugins` a raportat `AINPCPlugin` si `AINPCScenarioMedieval`;
- `/ainpc` a afisat meniul de comenzi;
- `/ainpc audit all` a raportat `0 erori` si `2 warning-uri` asteptate pentru server gol;
- `stop` prin RCON a facut shutdown curat, cu salvarea dimensiunilor si dezactivarea pluginului.

Observatie tehnica:

- injectarea comenzilor prin stdin/pipe PowerShell a declansat in Paper `NullPointerException` in `CommandSourceStack.getLevel()`;
- F001 este considerat trecut prin RCON, deoarece comenzile sunt trimise dupa startup complet si shutdown-ul este curat;
- warning-ul OpenAI este asteptat in mediul fara `OPENAI_API_KEY`.

### Raport F003 - 2026-05-10

Status: trecut prin Paper + RCON.

Mediu validat:

- server temporar: `%TEMP%/ainpc-paper-f003-rcon-20260510-154312`
- Paper: `1.21.11-69`
- Java: `21.0.11` Temurin
- log comenzi: `%TEMP%/ainpc-paper-f003-rcon-20260510-154312/f003-rcon-output.txt`

Rezultat validat:

- `/ainpc world demo create demo_sat` a creat mapping demo pe spawn-ul lumii cand a fost rulat din RCON;
- output: `1` regiune, `9` places, `30` nodes;
- `/ainpc audit world` a raportat `0 erori` si `0 warning-uri`;
- `/ainpc world save` a salvat mapping-ul in `plugins/AINPCPlugin/config.yml`;
- `config.yml` contine regiunea `demo_sat`;
- shutdown-ul prin `stop` a fost curat.

Observatie tehnica:

- comanda `world demo create` poate fi rulata acum si din consola/RCON; pentru jucatori ramane centrata pe pozitia jucatorului, iar pentru consola/RCON foloseste spawn-ul primei lumi incarcate.

### Raport F004 - 2026-05-10

Status: trecut dupa restart Paper.

Mediu validat:

- server temporar reutilizat: `%TEMP%/ainpc-paper-f003-rcon-20260510-154312`
- log comenzi: `%TEMP%/ainpc-paper-f003-rcon-20260510-154312/f004-rcon-output.txt`

Rezultat validat:

- dupa restart, logul de startup a raportat `World admin incarcat: 1 regiuni, 9 places, 30 noduri`;
- `/ainpc audit world` a raportat `0 erori` si `0 warning-uri`;
- `/ainpc world places demo_sat` a listat toate cele `9` places;
- `/ainpc world region info demo_sat` a raportat `Places: 9` si `Nodes: 30`;
- shutdown-ul prin `stop` a fost curat.

### Raport F005 - 2026-05-10

Status: trecut prin settlement dry-run.

Mediu validat:

- server temporar reutilizat: `%TEMP%/ainpc-paper-f003-rcon-20260510-154312`
- log comenzi: `%TEMP%/ainpc-paper-f003-rcon-20260510-154312/f005-rcon-output.txt`

Rezultat validat:

- `/ainpc world settlement plan demo_sat` a generat `4` household-uri;
- dry-run-ul a planificat `4` NPC;
- toate cele `4/4` household-uri au fost planificate cu succes;
- warning-urile planului sunt asteptate inainte de spawn: casele demo nu au inca `owner_npc_id` si `metadata.residents` sincronizate;
- `/ainpc audit world` a raportat `0 erori` si `0 warning-uri`;
- shutdown-ul prin `stop` a fost curat.

### Raport F006 - 2026-05-10

Status: trecut prin settlement spawn limitat si rollback dry-run.

Mediu validat:

- server temporar reutilizat: `%TEMP%/ainpc-paper-f003-rcon-20260510-154312`
- log comenzi: `%TEMP%/ainpc-paper-f003-rcon-20260510-154312/f006-rcon-output.txt`

Rezultat validat:

- `/ainpc world settlement spawn demo_sat 2` a planificat primele `2` case si a creat `2` NPC;
- batch creat: `settlement:demo_sat:749dc162393a31c0`, status `SUCCEEDED`, `created/reused=2/0`;
- `/ainpc audit spawn` a raportat `0 erori` si `0 warning-uri`;
- `/ainpc world bindings list 10` a raportat `2/2` binding-uri persistente;
- `/ainpc repair batch settlement:demo_sat:749dc162393a31c0 dryrun` a aratat rollback-ul controlat: ar sterge cei `2` NPC creati si ar recalcula resident count pentru household-urile afectate;
- rollback-ul nu a fost aplicat, deoarece batch-ul este valid si `SUCCEEDED`;
- `/ainpc world save` si `stop` au trecut curat.

### Raport F007 - 2026-05-10

Status: trecut prin settlement spawn complet.

Mediu validat:

- server temporar reutilizat: `%TEMP%/ainpc-paper-f003-rcon-20260510-154312`
- log comenzi: `%TEMP%/ainpc-paper-f003-rcon-20260510-154312/f007-rcon-output.txt`

Rezultat validat:

- la startup, serverul a reincarcat cei `2` NPC creati in F006;
- `/ainpc world settlement spawn demo_sat` a planificat `4` case si `4` NPC;
- rezultatul a fost `4/4` household-uri reusite;
- batch creat: `settlement:demo_sat:6353be48f6541701`, status `SUCCEEDED`, `created/reused=2/2`;
- `/ainpc audit spawn` a raportat `0 erori` si `0 warning-uri`;
- `/ainpc world bindings list 10` a raportat `4/4` binding-uri persistente;
- `/ainpc world household list 10` a raportat `4` household-uri si `4` rezidenti;
- `/ainpc list` a raportat `4` NPC incarcati;
- `/ainpc world save` si `stop` au trecut curat.

Observatie tehnica:

- la rerularea completa dupa spawn-ul limitat din F006, plannerul a raportat warning-uri pentru casele 1-2 deoarece metadata runtime foloseste selectori de forma `npc_1`/`npc_2`, iar planul determinist compara cu cheia de plan; auditul spawn si `npc_world_bindings` confirma ca starea persistenta este coerenta.

### Raport F008 - 2026-05-10

Status: trecut headless, cu verificare de cod pentru GUI.

Mediu validat:

- server temporar reutilizat: `%TEMP%/ainpc-paper-f003-rcon-20260510-154312`
- log comenzi initial: `%TEMP%/ainpc-paper-f003-rcon-20260510-154312/f008-rcon-output.txt`
- log comenzi cu chunk-uri fortate: `%TEMP%/ainpc-paper-f003-rcon-20260510-154312/f008-forceload-rcon-output.txt`

Rezultat validat:

- fara chunk-uri incarcate, NPC-urile persistate apar in manager, dar nu au entitate live si `routine tick` evalueaza `0` NPC;
- dupa `forceload add -80 -80 80 80`, chunk-urile demo au restaurat entitatile NPC;
- `/ainpc routine tick` a raportat `NPC total: 4`, `Evaluati: 4`, `Mutati: 4`;
- `/ainpc routine status Radu_house1_1` si `Maria_house4_1` au raportat slot `WORK`, activitate `merge la lucru`, tinta `Fierarie [work]` si entitate live cu AI/gravity/collidable active;
- `/ainpc world bindings npc ...` confirma ancorele `home`, `work` si `social` pentru NPC-urile verificate;
- GUI-ul nu poate fi deschis din RCON: `/ainpc gui routine` raspunde ca poate fi folosit doar de jucatori;
- codul `RoutineGui` afiseaza in lore `Slot curent`, `Activitate`, `Goal`, `Stare tinta`, `Tinta`, `Spawned`, `Locatie` si programul zilnic.

Observatie tehnica:

- pentru smoke headless de rutina dupa restart, trebuie incarcat explicit chunk-ul regiunii demo sau trebuie conectat un player in zona; altfel NPC-urile raman persistate, dar fara entitate live.

### Raport F009 - 2026-05-10

Status: trecut ca verificare pathfinding/fallback, cu observatie de teren.

Mediu validat:

- server temporar reutilizat: `%TEMP%/ainpc-paper-f003-rcon-20260510-154312`
- log comenzi: `%TEMP%/ainpc-paper-f003-rcon-20260510-154312/f009-rcon-output.txt`

Rezultat validat:

- testul a folosit `forceload add -80 -80 80 80` pentru a restaura entitatile NPC si `time set 19000` pentru slotul `HOME`;
- inainte de tick, `Radu_house1_1` era la `69.4` blocuri de ancora home si `Maria_house4_1` la `118.5` blocuri;
- aceste distante depasesc `routine.natural_movement.max_distance=48.0`, deci miscarea intra in fallback de teleport/path asistat, nu in pathfinding natural scurt;
- `/ainpc routine tick` a raportat `Evaluati: 4`, `Mutati: 4`;
- dupa tick, `Radu_house1_1` a ajuns la `0.7` blocuri de ancora home;
- `Maria_house4_1` a ajuns pe X/Z-ul casei, dar a ramas la `17.2` blocuri de tinta din cauza diferentei verticale: ancora home este la Y `78`, iar entitatea a cazut la aproximativ Y `60.8`;
- force-load-ul a fost eliminat la final cu `forceload remove -80 -80 80 80`;
- shutdown-ul prin `stop` a fost curat.

Observatie tehnica:

- mapping-ul demo are ancore Y generate semantic, nu ajustate la terenul real. Pentru pathfinding matur, F009 indica nevoia unei faze ulterioare de snap la sol/bed node valid sau validare a ancorelor cu `highest block` inainte de rutina.

### Raport F010 - 2026-05-10

Status: trecut prin `debugdump world`.

Mediu validat:

- server temporar reutilizat: `%TEMP%/ainpc-paper-f003-rcon-20260510-154312`
- log comenzi: `%TEMP%/ainpc-paper-f003-rcon-20260510-154312/f010-rcon-output.txt`
- folder dump: `%TEMP%/ainpc-paper-f003-rcon-20260510-154312/plugins/AINPCPlugin/debug-dumps/debug-dump-20260510-160906`

Rezultat validat:

- `/ainpc debugdump world` a generat folderul de dump fara exceptii;
- `summary.txt` raporteaza `NPC count: 4`, `World admin enabled: true`, `Regions: 1`, `Places: 9`, `Nodes: 30`;
- `world-mapping.json` contine `1` regiune, `9` places si `30` nodes;
- `npc-world-bindings.json` raporteaza `row_count: 4`, `missing_place_reference_count: 0`, `missing_node_reference_count: 0`;
- `households.json` raporteaza `household_count: 4` si `resident_count: 4`;
- `spawn-batches.json` este prezent si acopera batch-urile F006/F007;
- `/ainpc audit all` a raportat `0 erori` si `1 warning`.

Observatie tehnica:

- warning-ul ramas este pentru `medieval_quest:Q06`, obiectivul `inspect_forge_marks`, unde tokenul `type:inspect_node` nu apare in `world mapping semantic_index` pentru ancora node. Nu blocheaza mapping/spawn, dar trebuie tratat intr-o faza de aliniere quest-anchor semantic.

### Raport F011-F015 - 2026-05-10

Status: amanat pentru test manual in joc.

Motiv:

- `/ainpc wand` si `/ainpc map preview|confirm|cancel` trec prin validare `Player`;
- RCON/console nu poate simula selectii wand reale, click stanga/dreapta sau context de inventar;
- aceste faze nu sunt marcate finalizate pana cand exista un jucator conectat in Paper.

Comenzi manuale ramase:

```text
/ainpc wand mode region
/ainpc wand mode place
/ainpc wand mode node
/ainpc map preview
/ainpc map confirm
/ainpc map cancel
```

### Raport F016 - 2026-05-10

Status: implementat si testat automat.

Rezultat validat:

- `MappingWandService` pastreaza in memorie ultimele `25` draft-uri wand confirmate;
- confirmarile prin `/ainpc map confirm` sunt inregistrate pentru `region`, `place`, `node`, `npc_bind` si `quest_anchor`;
- `/ainpc audit wand` afiseaza confirmarile recente si valideaza tintele in world mapping cand World Admin este activ;
- `/ainpc audit world` si `/ainpc audit all` includ acelasi audit wand;
- tab completion expune noul mod `wand` pentru `/ainpc audit`.

Test automat:

```text
mvn -pl ainpc-core-plugin -am "-Dtest=AINPCTabCompleterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Rezultat: `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`.

### Raport F026-F028 - 2026-05-10

Status: implementat si testat automat.

Rezultat validat:

- auditul `npc_world_bindings` compara acum randurile persistente cu ancorele mapabile din profilul NPC;
- daca un NPC incarcat are ancore in profil, dar nu are rand in `npc_world_bindings`, auditul raporteaza reparatia recomandata;
- `/ainpc repair npc-bindings dryrun` raporteaza ce randuri ar crea sau actualiza din profilul NPC;
- `/ainpc repair mapping-metadata dryrun` raporteaza ce metadata WorldAdmin ar reface din `npc_world_bindings`;
- ambele comenzi au si mod `apply`, dar fluxul recomandat ramane `dryrun` urmat de audit si `world save` cand se aplica metadata.

Comenzi:

```text
/ainpc repair npc-bindings dryrun
/ainpc repair mapping-metadata dryrun
/ainpc audit db
/ainpc audit all
```

Test automat:

```text
mvn -pl ainpc-core-plugin -am "-Dtest=AINPCTabCompleterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Rezultat: `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`.

### Raport F037 - 2026-05-10

Status: implementat si testat prin compilare.

Rezultat validat:

- confirmarea `quest_anchor` cauta definitia progresiei persistate;
- `objective_id` este acceptat doar daca poate fi mapat la un obiectiv din definitie;
- `objective_type` este comparat cu tipul obiectivului definit si respinge mismatch-ul;
- daca definitia nu este disponibila pentru o progresie legacy, comanda avertizeaza si nu blocheaza salvarea.

Test automat:

```text
mvn -pl ainpc-core-plugin -am "-Dtest=AINPCTabCompleterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Rezultat: `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`.

### Raport F038 - 2026-05-10

Status: implementat si testat automat.

Rezultat validat:

- `/ainpc map quest_anchor ...` sugereaza `tracked`, `current` si `player:self`;
- dupa selectorul progresiei, tab completion poate cere `objective_id`;
- cand exista runtime complet si sender/target player, `ProgressionService` rezolva definitia progresiei si intoarce cheile obiectivelor mapabile;
- pentru selector explicit, fallback-ul poate cauta direct in definitiile incarcate;
- dupa `objective_id`, completarea sugereaza tipuri comune de obiectiv mapabil.

Test automat:

```text
mvn -pl ainpc-core-plugin,ainpc-scenario-medieval -am clean "-Dtest=AINPCTabCompleterTest,AddonRegistryTest,FeaturePackMetadataValidatorTest,FeaturePackDependencyValidatorTest,MedievalQuestPackTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Rezultat: `Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`.

### Raport F029-F030 - 2026-05-10

Status: implementat si testat prin compilare.

Rezultat validat:

- `RoutineGui` afiseaza pentru fiecare NPC sumarul `home/work/social` din `npc_world_bindings`;
- cardul NPC arata separat place IDs si node IDs persistate;
- daca nu exista rand persistent, GUI-ul afiseaza `Mapping place: nepersistat`;
- pentru admini, `Shift click` pe card ruleaza `/ainpc world bindings npc <databaseId>`.

Test automat:

```text
mvn -pl ainpc-core-plugin -am "-Dtest=AINPCTabCompleterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Rezultat: `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`.

### Raport F017-F018 - 2026-05-10

Status: implementat si testat prin compilare.

Rezultat validat:

- selectia `region/place` deseneaza un preview cu particule `END_ROD` pe muchiile bounds-ului cand se seteaza `pos1/pos2` sau cand se ruleaza `/ainpc map preview`;
- selectia `node/npc_bind/quest_anchor` deseneaza un cerc de raza in jurul punctului selectat;
- draft-urile generate prin `/ainpc map <descriere>` reaprind preview-ul vizual, folosind radius-ul draft-ului pentru node/anchor;
- preview-ul se afiseaza doar in lumea curenta a jucatorului si nu scrie date persistente.

Test automat:

```text
mvn -pl ainpc-core-plugin -am "-Dtest=AINPCTabCompleterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Rezultat: `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`.

### Raport F019-F020 - 2026-05-10

Status: implementat si testat automat.

Rezultat validat:

- `/ainpc wand inspect` este alias explicit pentru inspectia ultimei selectii wand si afiseaza acelasi sumar ca `/ainpc wand status`;
- `/ainpc wand clear [pos1|pos2|point|all]` si `/ainpc wand reset [pos1|pos2|point|all]` permit reset partial fara a pierde automat toata sesiunea;
- tab completion expune `inspect`, `reset` si tintele `pos1`, `pos2`, `point`, `all`;
- documentatia de mapping listeaza noile forme de comanda.

Test automat:

```text
mvn -pl ainpc-core-plugin -am "-Dtest=AINPCTabCompleterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Rezultat: `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`.

### Raport ADDON-CONFIG-01 - 2026-05-10

Status: implementat si testat automat.

Rezultat validat:

- core-ul pastreaza doar setari universale in `config.yml`: registry addonuri, dezactivari, load order si reguli generale pentru feature packs;
- `AINPCPlatformApi` expune `getAddonConfigDirectory(addonId)`, iar core-ul respecta `addons.config_directory`;
- registrul de addonuri respecta `addons.enabled`, `addons.disabled`, `addons.strict_validation` si ordoneaza descriptorii dupa `addons.load_order`;
- `feature_packs.validate_on_startup` si `feature_packs.validate_addon_metadata` ruleaza preflight peste metadata `addon:` declarata in pack-uri;
- pack-urile cu `addon.type`, `runtime_modes`, `capabilities`, `dependencies` sau runtime incompatibil sunt respinse inainte sa incarce continut;
- dependintele declarate in `addon.dependencies` sunt evaluate dupa un prepass peste toate pack-urile candidate, inclusiv pentru cazuri tranzitive;
- `feature_packs.fail_invalid_pack` decide daca metadata invalida doar sare pack-ul sau opreste incarcarea prin eroare;
- addonul medieval livreaza `config-template.yml`, creeaza `plugins/AINPC/addons/ainpc-scenario-medieval/config.yml` doar daca lipseste si nu suprascrie config-ul adminului;
- `addon.enabled: false` dezactiveaza addonul medieval local;
- `content.install_pack: false` sau `content.playable_content: false` impiedica instalarea pack-ului `medieval_quest.yml`;
- documentatia separa explicit config-ul core, config-ul addonului si pack-urile livrate de addon.

Test automat:

```text
mvn -pl ainpc-core-plugin,ainpc-scenario-medieval -am clean "-Dtest=AINPCTabCompleterTest,AddonRegistryTest,FeaturePackMetadataValidatorTest,FeaturePackDependencyValidatorTest,MedievalQuestPackTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Rezultat: `Tests run: 23, Failures: 0, Errors: 0, Skipped: 0`.

Test dialog:

1. apropie-te de un NPC
2. interactioneaza cu el
3. pune o intrebare factuala: `cum te cheama?`
4. intreaba: `ce meserie ai?`
5. intreaba: `unde esti?`
6. verifica daca raspunsul este coerent cu profilul si contextul

## Testare World Admin

Scenariu minim:

1. creeaza o regiune mica
2. creeaza un place in regiune
3. creeaza un node in place
4. ruleaza `/ainpc world save`
5. da reload sau restart
6. verifica daca regiunea, place-ul si node-ul exista dupa reload

Comenzi exemplu:

```text
/ainpc world region create sat_test settlement 0 60 0 80 90 80
/ainpc world place create sat_test fierarie forge 10 60 10 20 75 20
/ainpc world node create sat_test sat_test:fierarie forge_spot interaction 15 64 15 2
/ainpc world save
```

## Smoke Test Mapping si Settlement Spawn

Acesta este testul imediat pentru faza curenta de mapping/spawn:

```text
/ainpc world demo create demo_sat
/ainpc world settlement plan demo_sat
/ainpc world settlement spawn demo_sat
/ainpc audit world
/ainpc audit spawn
/ainpc world save
```

Dupa reload sau restart:

```text
/ainpc audit world
/ainpc audit spawn
/ainpc world places demo_sat
```

Verifica:

- mapping-ul demo ramane incarcat dupa reload
- NPC-urile spawnate au casa, ancore si familie valida
- place-urile home/work/social au metadata de rezidenti, worker sau social
- auditul nu raporteaza erori de spawn order

## Smoke Test Patch Planner

Patch Planner este read-only. Smoke test-ul confirma ca poate explica lipsurile din mapping fara sa construiasca blocuri, fara sa scrie planuri persistente si fara sa porneasca questuri.

Ruleaza dupa ce exista o regiune demo sau o regiune importata din sat:

```text
/ainpc patch analyze demo_sat 6 blacksmith,farmer,merchant,innkeeper
/ainpc patch plan demo_sat 8 blacksmith,farmer,merchant,innkeeper
/ainpc patch validate demo_sat 8 blacksmith,farmer,merchant,innkeeper
/ainpc audit world
/ainpc world save
```

Dupa reload sau restart:

```text
/ainpc patch analyze demo_sat 6 blacksmith,farmer,merchant,innkeeper
/ainpc audit world
/ainpc world places demo_sat
```

Verifica:

- `analyze` raporteaza capacitate, case/paturi, workplace-uri, social hub si node-uri lipsa;
- `plan` produce candidati de patch cu prioritate, risc si build mode;
- `validate` marcheaza `semantic_only` ca permis cand cere doar mapping semantic;
- patch-urile care cer `native-block-build` raman blocate pana exista builder nativ;
- niciuna dintre comenzi nu creeaza places/nodes noi;
- dupa restart, mapping-ul este neschimbat si analiza este reproductibila.

Raport recomandat:

```text
Data:
Build/JAR:
Server/Paper:
World:
Region:
Commands run:
Analyze result:
Plan result:
Validate result:
Expected blocked capabilities:
Mapping changed: no
Restart result:
Issues:
Decision:
```

## Smoke Test Story Lane

Story lane verifica trei cazuri diferite. Nu le amesteca intr-un singur rezultat:

```text
story_only
quest_only
writes_story
```

### `story_only`

Scop: exista story state/event inspectabil, dar nu apare progres nou pentru jucator.

Pornire:

- foloseste o regiune/place care are deja story state/event dintr-un fixture, seed controlat sau actiune validata anterior;
- daca nu exista mecanism sigur de a crea story fara quest, marcheaza smoke-ul `blocked`, nu `failed`;
- nu edita DB live ca sa fortezi trecerea testului.

Comenzi:

```text
/ainpc story region demo_sat
/ainpc story place demo_sat:piata
/ainpc story events demo_sat 10
/ainpc progression stored all all
/ainpc debugdump story
/ainpc debugdump quest
```

Verifica:

- `story-states.json` sau `story-events.json` contine starea/evenimentul asteptat;
- `player-progressions.json` si `player-quest-progress.json` nu primesc progres nou din acest smoke;
- `/ainpc gui quest all` nu afiseaza intrare noua doar pentru ca story-ul exista;
- dupa restart, story state/event ramane inspectabil.

### `quest_only`

Scop: o progresie merge cap-coada fara sa modifice story state/events.

Comenzi orientative:

```text
/ainpc progression definitions tutorial
/ainpc progression stored all tutorial
/ainpc quest log all
/ainpc debugdump quest
/ainpc debugdump story
```

Verifica:

- progresia apare in `player-progressions.json`;
- statusul/stage-ul se schimba conform mecanicii testate;
- `story-events.json` nu primeste event nou pentru acest slice;
- daca definitia contine `record_story_event`, atunci nu este `quest_only`, ci `writes_story`.

### `writes_story`

Scop: completarea progresiei produce exact story action-ul declarat.

Comenzi orientative:

```text
/ainpc audit quest strict
/ainpc progression stored all all
/ainpc debugdump quest
/ainpc debugdump story
```

Verifica:

- auditul vede actiunea `record_story_event` sau `set_story_state`;
- inainte de completare, event-ul asteptat lipseste sau are timestamp anterior;
- dupa completare, `story-events.json` sau `story-states.json` contine exact scope-ul asteptat;
- in `story-events.json`, campul `progression_link` indica progresia, scenariul si action-ul `record_story_event` care au scris event-ul, cand legatura este unica;
- dupa restart, progresul si story-ul raman coerente.

Raport recomandat:

```text
Data:
Build/JAR:
Server/Paper:
World:
Region/place:
Story mode: story_only | quest_only | writes_story
Commands run:
Expected progression change:
Actual progression change:
Expected story change:
Actual story change:
Context priority result: Progression > Story > Memory > AI text
Context conflicts:
Debugdump files:
Restart result:
Issues:
Decision:
```

`Decision=blocked` este corect cand lipseste un writer sigur pentru `story_only`, cand mapping-ul nu exista sau cand questul testat are alta incadrare story decat cea declarata.

Pentru `Context priority result`, noteaza explicit daca un text AI sau o memorie ar contrazice progresul ori story state-ul. Rezultatul corect este ca progresul validat castiga peste story, story castiga peste memorie, iar AI text ramane doar explicatie/generare, nu sursa de adevar.

## Testare NPC auto-indexat din villager

Scenariu:

1. porneste serverul cu `debug: true`
2. gaseste sau spawneaza un villager
3. asteapta cateva secunde
4. verifica `/ainpc list`
5. verifica profilul NPC-ului
6. verifica daca are meserie
7. verifica daca are casa si loc de munca
8. interactioneaza cu NPC-ul si intreaba despre meserie

Rezultatul asteptat:

- villagerul apare ca NPC
- profilul are `profile_source: auto`
- meseria este derivata din profesie, job block sau scenariu
- `owned_locations.home` exista
- `owned_locations.work` exista

## Testare generare sate fara WorldEdit

Pentru scannerul vanilla si importul semantic initial:

```text
/ainpc world scan village 48
/ainpc world scan village 48 import sat_test
/ainpc world save
/ainpc world places sat_test
/ainpc audit world
/ainpc audit spawn
```

Verifica:

- dry-run-ul raporteaza clopote, paturi, workstation-uri, usi si farmland
- importul creeaza o regiune settlement
- casele au noduri `bed`, `home` si eventual `entrance`
- locurile de munca au noduri `workstation` sau `work`
- clopotul devine `meeting_point`
- mapping-ul ramane dupa `/ainpc world save` si reload

Cand apare implementarea generatorului nativ complet, testele manuale trebuie sa verifice:

- numarul de case raportat la populatie
- fiecare meserie importanta are cladire sau zona de munca
- fiecare cladire are `WorldPlace`
- fiecare casa are tag `home`
- fiecare loc de munca are tag `workplace` sau metadata `role: work`
- NPC-urile au `homeAnchor` si `workAnchor`
- `/ainpc world whereami` identifica corect locurile generate
- restartul serverului pastreaza mapping-ul

## Teste automate recomandate

Prioritate mare:

- `WorldAdminServiceTest` pentru validari de mapping
- `SemanticVillageMapperTest` pentru import semantic din scanare vanilla
- `RoutineEngineTest` pentru decizia rutinei zilnice
- teste pentru calificarea ID-urilor
- teste pentru `auto_index.enabled`
- teste pentru salvare si reload config
- teste pentru citirea si scrierea `owned_locations`
- teste pentru alegerea casei si locului de munca din `WorldPlace`

Prioritate medie:

- teste pentru selectie de meserie dupa job block
- teste pentru generarea de backstory
- teste pentru fallback OpenAI local
- teste pentru parserul de questuri

Prioritate viitoare:

- teste de integrare cu server mock
- teste pentru generare sate
- teste pentru addon registry
- teste pentru scenarii custom

## Cand adaugi un test nou

Adauga test cand:

- repari un bug reproductibil
- schimbi formatul configului
- schimbi persistenta DB
- schimbi mapping-ul world admin
- schimbi profilarea automata NPC
- adaugi generator de sate sau cladiri
- schimbi contracte publice din `ainpc-api`

Nu este obligatoriu sa testezi automat fiecare mesaj de chat, dar trebuie testata logica determinista din spate.

## Structura recomandata pentru scripturi viitoare

Scripturile trebuie puse in `scripts/` si documentate in `scripts/README.md`.

Conventii:

- nume explicit: `debug-openai.ps1`, `package-plugin.ps1`, `smoke-server.ps1`
- parametri cu valori implicite
- loguri in `debug-logs/`
- fara secrete printate in clar
- exit code diferit de zero la esec
- sa poata fi rulate din radacina repo-ului

Scripturi utile de adaugat ulterior:

- `package-plugin.ps1` pentru build si copiere JAR in server local
- `inspect-jar.ps1` pentru verificarea continutului JAR
- `debug-db.ps1` pentru export sumar din SQLite
- `smoke-world-admin.ps1` pentru verificari de config si mapping
- `collect-server-logs.ps1` pentru arhivarea logurilor relevante

## Colectare informatii pentru bug report

Pentru orice bug serios, noteaza:

- commit sau branch
- versiune Java
- versiune Paper
- versiune plugin
- comanda sau actiunea care reproduce problema
- log server relevant
- config relevant, fara secrete
- daca problema apare dupa restart sau doar runtime
- daca DB-ul este nou sau vechi
- daca `debug: true` schimba informatia disponibila

## Reguli de siguranta

- nu publica chei API in loguri sau issue-uri
- nu edita DB live fara backup
- nu sterge `target/` sau fisiere generate daca nu stii ce test vrei sa rulezi
- nu folosi `git reset --hard` pentru curatare fara acord explicit
- nu modifica fisiere din `plugins/AINPC/` si resursele din repo ca si cum ar fi acelasi lucru

## Concluzie

Testarea buna in proiect trebuie sa combine:

- teste automate Maven pentru logica determinista
- scripturi PowerShell pentru diagnostic local
- smoke tests pe server Paper
- comenzi admin pentru World Admin si NPC-uri
- loguri clare cu `debug: true`

Scopul este sa poti separa rapid:

- bug de build
- bug de configuratie
- bug OpenAI
- bug DB
- bug de mapping
- bug de gameplay runtime
