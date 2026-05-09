# Debugging si Testare

Actualizat: 2026-05-03

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
/ainpc gui world
/ainpc gui routine
/ainpc gui audit
/ainpc gui debug
```

Ce verifici:

- filtrele afiseaza aceleasi intrari ca `/ainpc progression definitions <filter>`;
- detaliile arata stage-ul curent, obiectivele si tracking-ul;
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
