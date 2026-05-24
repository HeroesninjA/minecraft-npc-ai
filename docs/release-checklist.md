# Release Checklist

Actualizat: 2026-05-07

Status: checklist operational initial pentru build, test si livrare pe server Paper. Nu este un proces complet de release management si nu inlocuieste `migration-si-backup.md`, care ramane de creat.

## Scop

Acest document defineste ce trebuie verificat inainte ca un build AINPC sa fie pus pe un server Paper de test, demo sau productie.

Release inseamna aici:

```text
artefact JAR identificabil
-> instalat pe server Paper
-> pornire fara erori critice
-> smoke test minim
-> audit si debugdump disponibile
-> rollback/backup clar
```

Nu considera release un simplu `mvn package`.

## Tipuri de release

| Tip | Cand se foloseste | Prag minim |
| --- | --- | --- |
| `dev-snapshot` | Test local rapid | Compileaza si porneste pe server local |
| `paper-test` | Test pe server Paper dedicat | Build curat, audit, smoke mapping/NPC |
| `demo-playable` | Demo pentru jucatori/testeri | Paper test plus quest smoke, restart si backup |
| `production-public` | Server public sau date reale | Toate verificarile plus plan de rollback si backup complet |

`demo-playable` este un milestone intern/de test pentru first playable. Nu inseamna lansare publica si nu trebuie folosit ca presiune pentru publicarea proiectului inainte sa fie matur.

Daca serverul are date reale, trateaza release-ul ca `production-public`, chiar daca build-ul este numit experimental.

## Date de completat

Inainte de release, noteaza:

```text
Release ID:
Data:
Commit / branch:
Versiune Maven:
Server tinta:
Paper / server jar:
JAR core:
JAR addonuri:
Config schimbat: da/nu
DB migration: da/nu
World changes: da/nu
Backup facut: da/nu
Smoke owner:
Decizie finala:
```

Fara aceste date, nu poti compara corect doua build-uri cand apare un bug.

## Stop imediat

Opreste release-ul daca apare oricare dintre situatiile de mai jos:

- `mvn test` esueaza fara explicatie acceptata;
- JAR-ul core lipseste sau nu contine `plugin.yml`;
- serverul nu incarca pluginul;
- logul de startup are exceptii repetate;
- `/ainpc` nu raspunde;
- `/ainpc audit db` raporteaza eroare critica;
- `/ainpc audit npc` raporteaza duplicate pe date care trebuie pastrate;
- lipseste backup-ul pentru server cu date reale;
- exista migration necesara dar fara plan de backup/rollback;
- cheia OpenAI este pusa in repo, screenshot sau debugdump public;
- `debug: true` ramane activ pe server public fara motiv temporar.

Nu continua cu "vedem dupa". Repari, refaci build-ul si reiei checklistul.

## 1. Pre-release local

Verifica starea repo:

```powershell
git status --short
```

Bifeaza:

- intelegi ce fisiere intra in release;
- nu incluzi accidental schimbari locale netestate;
- documentatia relevanta este actualizata pentru comenzi/config noi;
- `TODO.md` sau backlogul marcheaza clar ce ramane nerezolvat;
- nu exista secrete in fisiere versionate;
- `target/` si artefactele generate nu sunt tratate ca sursa de adevar.

Pentru schimbari de comenzi sau config, verifica:

```text
ainpc-core-plugin/src/main/resources/plugin.yml
ainpc-core-plugin/src/main/resources/config.yml
docs/server-admin-runbook.md
docs/debugging-si-testare.md
```

## 2. Build local

Ruleaza testele:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean test
```

Construieste artefactele:

```powershell
.\gradlew.bat assemble
```

Verifica JAR-urile:

```powershell
Get-ChildItem .\ainpc-core-plugin\build\libs\*.jar | Select-Object Name,Length
Get-ChildItem .\ainpc-scenario-medieval\build\libs\*.jar | Select-Object Name,Length
Get-ChildItem .\ainpc-api\build\libs\*.jar | Select-Object Name,Length
```

Artefacte asteptate pentru versiunea curenta:

```text
ainpc-core-plugin/build/libs/ainpc-core-plugin-1.0.0.jar
ainpc-scenario-medieval/build/libs/ainpc-scenario-medieval-1.0.0.jar
ainpc-api/build/libs/ainpc-api-1.0.0.jar
```

Calculeaza hash-uri pentru raport:

```powershell
Get-FileHash .\ainpc-core-plugin\build\libs\ainpc-core-plugin-1.0.0.jar -Algorithm SHA256
Get-FileHash .\ainpc-scenario-medieval\build\libs\ainpc-scenario-medieval-1.0.0.jar -Algorithm SHA256
Get-FileHash .\ainpc-api\build\libs\ainpc-api-1.0.0.jar -Algorithm SHA256
```

## 3. Inspectie JAR

Verifica resursele core:

```powershell
jar tf .\ainpc-core-plugin\build\libs\ainpc-core-plugin-1.0.0.jar | Select-String "plugin.yml|config.yml|quests.yml"
```

Verifica clasele principale:

```powershell
jar tf .\ainpc-core-plugin\build\libs\ainpc-core-plugin-1.0.0.jar | Select-String "ro/ainpc/AINPCPlugin.class|ro/ainpc/managers/NPCManager.class"
```

Bifeaza:

- `plugin.yml` exista;
- `config.yml` exista;
- `quests.yml` exista;
- clasa `ro/ainpc/AINPCPlugin.class` exista;
- JAR-ul addonului medieval exista daca release-ul include quest demo;
- dimensiunea JAR-ului nu s-a schimbat inexplicabil.

Pentru schimbari de shade, vezi `reducere-marime-jar.md`. Nu activa `minimizeJar` intr-un release fara smoke test real.

## 4. Pregatire server Paper

Pe serverul tinta:

1. Opreste serverul.
2. Fa backup la `plugins/AINPC/`.
3. Fa backup la lumea afectata daca testul modifica mapping, NPC-uri sau structuri.
4. Noteaza JAR-urile vechi si hash-urile lor, daca exista.
5. Copiaza JAR-urile noi in `plugins/`.
6. Porneste serverul.

Pentru server cu date reale, backup minim:

```text
plugins/AINPC/config.yml
plugins/AINPC/quests.yml
plugins/AINPC/ainpc_data.db
plugins/AINPC/debug-dumps/
world/
world_nether/
world_the_end/
```

Nu edita `ainpc_data.db` cat timp serverul este pornit.

Backup verificat cu restore-check:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\release-backup-restore-check.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -ReleaseId "<release-id>"
```

Adauga `-IncludeWorlds` cand release-ul poate modifica entitati, structuri sau mapping in lume. Pastreaza `backup_zip`, `backup_zip_sha256` si `restore_report_path` in raportul de release.

## 5. Startup smoke

Imediat dupa pornire:

```text
/plugins
/ainpc
/ainpc audit
/ainpc audit db
/ainpc audit world
/ainpc list
```

Bifeaza:

- pluginul apare incarcat;
- `/ainpc` afiseaza help;
- auditul ruleaza fara crash;
- DB-ul este initializat;
- WorldAdmin este disponibil sau lipsa lui este explicita;
- logul nu are exceptii repetate;
- nu apar warning-uri noi despre dependinte lipsa.

Rezultat acceptabil pe server nou:

```text
World mapping: 0 regiuni, 0 places, 0 nodes
```

Aceasta stare este acceptabila doar daca release-ul nu pretinde mapping deja configurat.

## 6. Smoke mapping si spawn

Varianta asistata:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-mapping.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -RunTests
```

Comenzi manuale minime:

```text
/ainpc world demo create demo_sat
/ainpc world settlement plan demo_sat
/ainpc world settlement spawn demo_sat
/ainpc audit world
/ainpc audit spawn
/ainpc world save
```

Dupa restart:

```text
/ainpc audit world
/ainpc audit spawn
/ainpc world places demo_sat
```

Bifeaza:

- regiunea de test exista;
- places/nodes sunt persistate dupa restart;
- spawn planul nu produce NPC-uri fara casa;
- auditul spawn nu raporteaza rezidenti neclari;
- nu apar duplicate dupa restart.

## 7. Smoke NPC si rutina

Comenzi:

```text
/ainpc create TestReleaseNpc
/ainpc list
/ainpc info TestReleaseNpc
/ainpc audit npc
/ainpc routine status TestReleaseNpc
```

Daca exista mapping:

```text
/ainpc world bind npc TestReleaseNpc <homePlaceId> [workPlaceId|-] [socialPlaceId|-]
/ainpc routine status TestReleaseNpc
/ainpc audit spawn
```

Bifeaza:

- NPC-ul este creat o singura data;
- `info` afiseaza date coerente;
- rutina nu produce erori;
- auditul NPC nu raporteaza UUID/profil lipsa;
- dupa restart, NPC-ul nu se dubleaza.

Pentru probleme de duplicare, opreste release-ul si urmeaza `prevenire-duplicare-npc.md`.

## 8. Smoke quest

Varianta asistata:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-quests.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -PlayerName "NumeleTau" `
  -RunTests
```

Preflight automat fara player, pe server Paper deja pornit cu RCON activ:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-quests.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -SkipBuild `
  -NoCopy `
  -RunRconPreflight `
  -RconHost "127.0.0.1" `
  -RconPort 25575 `
  -RconPassword "<parola-rcon>"
```

Acest preflight ruleaza plugin load, demo mapping, definitii progression/quest pentru addonul medieval si `/ainpc audit quest offline`. El nu inlocuieste testul cu player real pentru `nearest`, accept/status/track si obiective in lume.

Smoke automat asistat cu player online:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-quests.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -PlayerName "NumeleTau" `
  -SkipBuild `
  -NoCopy `
  -RunRconPlayerSmoke `
  -WaitForPlayerCheckpoints `
  -RconHost "127.0.0.1" `
  -RconPort 25575 `
  -RconPassword "<parola-rcon>"
```

Acesta scrie `ainpc-quest-smoke-player-rcon-report.json` si devine evidenta principala pentru gate-ul `demo-playable`.

Comenzi manuale minime:

```text
/ainpc audit quest
/ainpc quest log
/ainpc quest nearest
/ainpc quest accept nearest
/ainpc quest status nearest
/ainpc quest track start
/ainpc quest anchors
```

Bifeaza:

- addonul de scenariu este incarcat daca release-ul include quest demo;
- quest log-ul raspunde;
- `nearest` nu crapa fara NPC potrivit;
- accept/status/track functioneaza;
- `audit quest` nu raporteaza erori critice;
- ancorele semantice sunt explicite sau lipsa lor este raportata clar.

## 9. OpenAI si fallback

Verificari:

- `OPENAI_API_KEY` este setat in mediul serverului, daca vrei raspuns real;
- `openai.api_key` ramane gol in config pentru release public;
- `openai.base_url` foloseste HTTPS;
- modelul configurat este intentionat;
- fallback-ul local functioneaza cand API-ul nu este disponibil;
- debug logs nu expun cheia.

Script local util:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\debug-openai.ps1
```

Bifeaza in raport daca smoke-ul a fost rulat cu OpenAI real sau cu fallback local.

## 10. Audit si debugdump final

Ruleaza:

```text
/ainpc audit all
/ainpc debugdump all
```

Verifica artefactul:

```text
plugins/AINPC/debug-dumps/debug-dump-YYYYMMDD-HHMMSS/
```

Bifeaza:

- `audit.txt` exista;
- dump-ul contine sumar NPC/world/quest;
- configul din dump este sanitizat;
- nu incluzi secrete in raportul final;
- ai pastrat calea catre dump pentru investigatii ulterioare.

## 11. Verificare dupa restart

Opreste si porneste serverul complet.

Ruleaza:

```text
/ainpc audit all
/ainpc list
/ainpc world places demo_sat
/ainpc quest log
```

Bifeaza:

- serverul porneste a doua oara fara erori noi;
- NPC-urile nu se dubleaza;
- mapping-ul salvat ramane disponibil;
- quest state-ul nu intra in stare imposibila;
- rutinele nu pornesc task-uri duplicate.

Un release care trece doar inainte de restart nu este suficient pentru demo sau productie.

## 12. Config final

Pentru server public:

```yaml
debug: false
openai:
  api_key: ""
  diagnostics:
    enabled: false
```

Exceptie: poti lasa diagnostics activ pe server de test sau staging.

Bifeaza:

- cheia OpenAI vine din environment;
- `world_admin.enabled` are valoarea intentionata;
- `simulation.enabled` are valoarea intentionata;
- `routine.teleport_enabled` este acceptabil pentru modul curent;
- quest tracking nu produce spam vizual;
- mesajele de eroare sunt potrivite pentru jucatori.

## 13. API/addon freeze

Ruleaza raportul de freeze pentru API si addonul medieval dupa build:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\release-api-addon-freeze.ps1 `
  -ReleaseId "<release-id>" `
  -FailOnWarnings
```

Bifeaza:

- JAR-ul `ainpc-api` exista si are hash notat;
- JAR-ul `ainpc-scenario-medieval` exista si are hash notat;
- semnatura normalizata API este pastrata in raport;
- `plugin.yml` din addon are `depend: [AINPCPlugin]`;
- addonul contine `packs/medieval_quest.yml` si `config-template.yml`;
- raportul `*-api-addon-freeze.json` are `ok=true`.

## 14. Raport release

Raport minim:

```text
Release ID:
Commit:
Core JAR SHA256:
Medieval addon SHA256:
API JAR SHA256:
Server:
Paper/API:
Teste Gradle:
Startup smoke:
Mapping smoke:
NPC smoke:
Quest smoke:
OpenAI mode:
Audit final:
Debugdump path:
Backup path:
Backup SHA256:
Restore-check:
Known issues:
Decizie: release / hold
```

Pastreaza raportul langa notitele de test sau in issue-ul intern.

Generator recomandat:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\release-report.ps1 `
  -ReleaseId "<release-id>" `
  -ServerDir "C:\Minecraft\paper-test" `
  -BackupReport "<backup-report-json>" `
  -QuestRconReport "<ainpc-quest-smoke-rcon-report.json>" `
  -QuestPlayerReport "<ainpc-quest-smoke-player-rcon-report.json>" `
  -ApiAddonFreezeReport "<release-id-api-addon-freeze.json>" `
  -TestsGradle "passed" `
  -StartupSmoke "passed" `
  -MappingSmoke "passed" `
  -NpcSmoke "passed" `
  -QuestSmoke "rcon-preflight-passed; player-smoke-passed" `
  -OpenAiMode "fallback-local" `
  -AuditFinal "passed" `
  -Decision hold
```

Output-ul standard este in `.ai\release-reports\` si include JSON plus Markdown. Pentru release real, foloseste `-Decision release` doar cand nu mai exista gate-uri deschise; scriptul marcheaza warning daca decizia este `release`, dar raportul `QuestPlayerReport` sau `ApiAddonFreezeReport` lipseste ori nu are `ok=true`.

## 15. Rollback rapid

Daca release-ul esueaza pe server:

1. Opreste serverul.
2. Pune inapoi JAR-urile vechi.
3. Restaureaza `plugins/AINPC/` daca noul build a modificat DB/config.
4. Restaureaza lumea daca testul a modificat structuri sau entitati intr-un mod nedorit.
5. Porneste serverul.
6. Ruleaza `/ainpc audit all`.
7. Noteaza motivul rollback-ului.

Nu incerca sa repari manual DB-ul live ca parte din rollback.

Pentru politici detaliate de backup, migration si rollback, foloseste `migration-si-backup.md`.

## Checklist scurt

```text
[ ] Git status inteles
[ ] Secrete verificate
[ ] Documentatie relevanta actualizata
[ ] gradlew clean test trecut
[ ] gradlew assemble trecut
[ ] JAR core exista
[ ] JAR addon exista daca este inclus
[ ] plugin.yml/config.yml/quests.yml exista in JAR
[ ] API/addon freeze report trecut
[ ] Hash-uri notate
[ ] Server oprit inainte de copiere
[ ] Backup plugins/AINPC facut
[ ] Restore-check backup trecut
[ ] Backup lume facut daca e necesar
[ ] Pluginul apare in /plugins
[ ] /ainpc raspunde
[ ] /ainpc audit all ruleaza
[ ] Smoke mapping trecut
[ ] Smoke NPC trecut
[ ] Smoke quest trecut daca release-ul include questuri
[ ] OpenAI/fallback verificat
[ ] Restart complet verificat
[ ] Debugdump final generat
[ ] Config final fara debug inutil
[ ] Raport release completat
[ ] Rollback path cunoscut
```

## Definitie de gata

Release-ul este gata cand:

- artefactele sunt identificabile prin versiune si hash;
- build-ul local trece;
- serverul Paper incarca pluginul;
- auditul final nu are erori critice;
- smoke-ul relevant trece inainte si dupa restart;
- backup-ul exista pentru date reale;
- backup-ul are restore-check trecut;
- raportul de release spune clar ce a fost testat si ce nu;
- exista o cale de rollback fara editare DB live.
