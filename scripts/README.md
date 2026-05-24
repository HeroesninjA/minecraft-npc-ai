# Scripts

## MCP Context Staged

Script: `scripts/mcp-context-staged.ps1`

Exemplu implicit (token optimization):

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-staged.ps1 `
  -Query "QuestTemplateSelector progression objective" `
  -Profile code `
  -NoTests
```

Cu filtre include/exclude:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-staged.ps1 `
  -Query "StoryContextService region state events" `
  -Profile debug `
  -Include "ainpc-core-plugin/src/main/kotlin/" `
  -Exclude "ainpc-core-plugin/src/test/" `
  -NoTests
```

Cu export in fisier:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-staged.ps1 `
  -Query "StoryContextService region state events" `
  -Profile debug `
  -NoTests `
  -OutFile ".ai\last-mcp-context.txt"
```

Cu export metadata JSON (attempt ales + tokeni):

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-staged.ps1 `
  -Query "StoryContextService region state events" `
  -Profile debug `
  -NoTests `
  -SummaryJson ".ai\last-mcp-context-summary.json"
```

Cu prag adaptiv din raport agregat:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-staged.ps1 `
  -Query "NPCManager createNPC" `
  -Profile code `
  -NoTests `
  -UseAdaptiveThresholds `
  -AdaptiveReportPath ".ai\mcp-context-report.json" `
  -AdaptiveMinRuns 5
```

Ce face:

- ruleaza context retrieval in etape: `900/8`, `1400/12`, `2200/14`;
- opreste escaladarea cand output-ul pare suficient;
- opreste retry-ul cand output-ul inutil se repeta identic;
- scrie in summary `selected_useful`, `selected_failed`, `selected_failure_reason` si `context_tokens_present`;
- aplica implicit auto-scope pe profil:
  - `code`: include `ainpc-core-plugin/src/main/`, `ainpc-api/src/main/`, `ainpc-scenario-medieval/src/main/`, `scripts/`;
  - `debug`: include codul + `docs/`;
  - `planning`: include `docs/`;
  - `full`: include codul + `docs/`;
  - exclude implicit `src/test`, `target`, `build`, `.gradle`, `.kotlin`, `.idea`, `node_modules`;
- foloseste prag minim pe profil pentru suficienta contextului:
  - `code`: `495` caractere;
  - `debug`: `352` caractere;
  - `planning`: `715` caractere;
  - `full`: `800` caractere;
- foloseste `codex-context.ps1` din MCP server root daca exista;
- are fallback la `npm run mcp:context` daca wrapperul lipseste.

Pentru context complet fara auto-scope:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-staged.ps1 `
  -Query "QuestTemplateSelector progression objective" `
  -Profile code `
  -NoTests `
  -DisableAutoScope
```

## MCP Context Code Shortcut

Script: `scripts/mcp-context-code.ps1`

Scurtatura pentru profilul `code` peste `mcp-context-staged.ps1`.

Exemplu:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-code.ps1 `
  -Query "NPCManager createNPC" `
  -NoTests `
  -OutFile ".ai\last-mcp-context.txt" `
  -SummaryJson ".ai\last-mcp-context-summary.json"
```

Cu prag adaptiv:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-code.ps1 `
  -Query "QuestTemplateSelector progression objective" `
  -NoTests `
  -UseAdaptiveThresholds `
  -AdaptiveReportPath ".ai\mcp-context-report.json"
```

## MCP Context Summary Report

Script: `scripts/mcp-context-summary-report.ps1`

Analizeaza fisierele de tip summary JSON/JSONL si produce statistici de eficienta token.

Exemplu:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-summary-report.ps1 `
  -InputPath ".ai" `
  -FilePattern "*mcp-context*summary*.json*" `
  -OutFile ".ai\mcp-context-report.json" `
  -RowsCsvOutFile ".ai\mcp-context-report-rows.csv" `
  -ProfilesCsvOutFile ".ai\mcp-context-report-profiles.csv"
```

Raportul include:

- distributia attempt-urilor (`attempt1/2/3`);
- separat `usable_runs` si `failed_runs`;
- medie `selected_token_budget` si `tokens_used` doar pentru rulari utile;
- economii medii vs buget fix `2200` doar pentru rulari utile;
- motive de esec (`failure_reason`) pentru output fara token metadata, fara hits sau sub prag;
- breakdown pe profil (`code/debug/planning/full`).
- optional CSV pentru analiza in Excel (`rows` + `profiles`).

## MCP Context Nightly Batch

Script: `scripts/mcp-context-nightly.ps1`

Ruleaza un set de query-uri din fisier, append-eaza summary JSONL si regenereaza raportul agregat.

Exemplu:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-nightly.ps1 `
  -QueryFile ".ai\mcp-nightly-queries.txt" `
  -OutputDir ".ai" `
  -Profile code `
  -NoTests `
  -UseAdaptiveThresholds `
  -SaveContextOutputs `
  -WriteCsvReports
```

Pentru o masuratoare curata, fara istoric vechi:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-nightly.ps1 `
  -QueryFile ".ai\mcp-nightly-queries.txt" `
  -OutputDir ".ai\nightly-clean" `
  -Profile code `
  -NoTests `
  -FreshRun `
  -WriteCsvReports
```

Fisier query:

- un query pe linie;
- liniile cu `#` sunt ignorate.

## MCP Context Nightly Matrix

Script: `scripts/mcp-context-nightly-matrix.ps1`

Ruleaza batch nightly pentru mai multe profiluri (`code/debug/planning/full`) cu fisiere de query separate.

Exemplu:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-nightly-matrix.ps1 `
  -OutputRoot ".ai\nightly" `
  -ReportRoot ".ai" `
  -Profiles code,debug,planning `
  -NoTests `
  -UseAdaptiveThresholds `
  -EnsureQueryTemplates `
  -SaveContextOutputs `
  -WriteCsvReports
```

Pentru recalibrare token economy, foloseste `-FreshRun` ca sa stergi doar summary/report-urile generate anterior din `OutputRoot` si rapoartele globale din `ReportRoot`:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-nightly-matrix.ps1 `
  -OutputRoot ".ai\nightly-token-economy" `
  -ReportRoot ".ai\token-economy-current" `
  -Profiles code,debug,planning `
  -NoTests `
  -EnsureQueryTemplates `
  -FreshRun `
  -WriteCsvReports
```

Output principal:

- `.ai\nightly\<profile>\mcp-context-nightly-run.json`;
- `.ai\mcp-context-report-global.json`;
- `.ai\mcp-context-nightly-matrix-run.json`.
- optional CSV: `*-rows.csv` si `*-profiles.csv`.

## MCP Context Schedule Registration

Script: `scripts/mcp-context-register-schedule.ps1`

Face preview sau inregistrare Task Scheduler pentru rulare zilnica a matrix script.
Implicit foloseste launcherul scurt `scripts/mcp-context-nightly-matrix-launcher.ps1` pentru a evita limita `/TR` din `schtasks`.

Preview (fara schimbari):

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-register-schedule.ps1 `
  -TaskName "AINPC-MCP-Context-Nightly" `
  -StartTime "02:30"
```

Aplicare (creeaza/actualizeaza taskul):

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-register-schedule.ps1 `
  -TaskName "AINPC-MCP-Context-Nightly" `
  -StartTime "02:30" `
  -Apply
```

Cu diagnoza automata daca `-Apply` esueaza:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-register-schedule.ps1 `
  -TaskName "AINPC-MCP-Context-Nightly" `
  -StartTime "02:30" `
  -Apply `
  -DiagnoseOnFailure `
  -DiagnoseOutFile ".ai\scheduler-diagnose.json"
```

## MCP Startup Registration (Fallback)

Script: `scripts/mcp-context-register-startup.ps1`

Fallback cand `schtasks` nu poate fi folosit: scrie un `.cmd` in Startup folder-ul userului curent care lanseaza `mcp-context-nightly-matrix-launcher.ps1` la logon.

Preview:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-register-startup.ps1
```

Aplicare:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-register-startup.ps1 -Apply
```

Stergere:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-register-startup.ps1 -Remove -Apply
```

## MCP Scheduler Diagnose

Script: `scripts/mcp-context-scheduler-diagnose.ps1`

Verifica starea serviciului Task Scheduler si probeaza `schtasks /Query` + `schtasks /Create`.

Exemplu:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-scheduler-diagnose.ps1 `
  -OutFile ".ai\scheduler-diagnose.json"
```

## MCP Automation Status

Script: `scripts/mcp-context-automation-status.ps1`

Face health-check unificat pentru automatizarea MCP:

- freshness pentru matrix/global reports;
- prezenta fallback startup file;
- stare scheduler din ultimul diagnostic;
- profile cu erori.

Exemplu:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-automation-status.ps1 `
  -ProjectRoot "." `
  -AiDir ".ai" `
  -FreshHours 30 `
  -OutFile "mcp-context-automation-status.json"
```

Pentru CI / gate strict:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-automation-status.ps1 `
  -ProjectRoot "." `
  -AiDir ".ai" `
  -FreshHours 30 `
  -OutFile "mcp-context-automation-status.json" `
  -TextSummaryOutFile "mcp-context-automation-status.txt" `
  -FailOnUnhealthy
```

## OpenAI Debug

Script: `scripts/debug-openai.ps1`

Exemple:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\debug-openai.ps1
```

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\debug-openai.ps1 -IncludeResponseTest
```

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\debug-openai.ps1 `
  -BaseUrl "https://api.openai.com/v1" `
  -Model "gpt-5.4-nano" `
  -ApiKey "sk-..."
```

Ce face:

- citeste `openai.base_url`, `openai.model` si `openai.api_key` din `config.yml` daca exista
- foloseste `OPENAI_API_KEY` si `OPENAI_BASE_URL` ca fallback
- sondeaza `GET /models/{model}`
- optional testeaza si `POST /responses`
- mascheaza cheia API in log

Output:

- log principal in `debug-logs/openai-debug-YYYYMMDD-HHMMSS.log`

## Paper Mapping Smoke Test

Script: `scripts/smoke-paper-mapping.ps1`

Exemplu simplu:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-mapping.ps1 `
  -ServerDir "C:\Minecraft\paper-test"
```

Cu teste Gradle inainte de assemble:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-mapping.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -RunTests
```

Cu alta regiune demo:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-mapping.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -RegionId "sat_test"
```

Fara sectiunea manuala pentru wand:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-mapping.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -SkipWandFlow
```

Ce face:

- ruleaza `.\gradlew.bat assemble`, daca nu folosesti `-SkipBuild`
- optional ruleaza si `.\gradlew.bat test` cu `-RunTests`
- copiaza JAR-ul core si addonul medieval in `plugins/`
- genereaza `ainpc-mapping-smoke-commands.txt` in folderul serverului
- genereaza `ainpc-mapping-smoke-report.txt` cu hash-uri si verificari de baza

Comenzile generate includ fluxul demo/settlement pentru consola Paper si o sectiune manuala pentru `wand` care trebuie rulata in joc ca OP, deoarece `pos1`/`pos2`/`point` folosesc pozitia jucatorului.

## Paper Quest Smoke Test

Script: `scripts/smoke-paper-quests.ps1`

Exemplu pentru serverul local:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-quests.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -PlayerName "NumeleTau"
```

Fara rebuild, daca JAR-urile sunt deja generate:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-quests.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -PlayerName "NumeleTau" `
  -SkipBuild
```

Preflight automat prin RCON, fara player online:

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

Smoke asistat cu player online prin RCON:

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

Ce face:

- ruleaza `.\gradlew.bat assemble`, daca nu folosesti `-SkipBuild`
- optional ruleaza si `.\gradlew.bat test` cu `-RunTests`
- copiaza JAR-ul core si addonul medieval in `plugins/`
- genereaza `ainpc-quest-smoke-commands.txt` in folderul serverului
- genereaza `ainpc-quest-smoke-report.txt` cu hash-uri si verificari de baza
- cu `-RunRconPreflight`, ruleaza automat plugin load, demo mapping, definitii progression/quest si `audit quest offline`
- scrie raportul RCON in `ainpc-quest-smoke-rcon-report.json`; parola RCON nu este salvata in raport
- cu `-RunRconPlayerSmoke`, verifica playerul online si ruleaza fluxul Q01 `nearest`/accept/status/track/give/turn-in
- scrie raportul player smoke in `ainpc-quest-smoke-player-rcon-report.json`; parola RCON nu este salvata in raport

Comenzile generate testeaza `/ainpc audit quest`, oferta/acceptarea unui quest cu `nearest`, tracking-ul si finalizarea rapida a Q01 cu iteme date prin `give`.

Preflight-ul RCON valideaza partea fara player. Pentru `demo-playable`, foloseste `-RunRconPlayerSmoke`; cu `-WaitForPlayerCheckpoints`, scriptul se opreste inainte de pasii unde playerul trebuie pozitionat langa NPC.

## Release Backup Restore Check

Script: `scripts/release-backup-restore-check.ps1`

Backup minim pentru release:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\release-backup-restore-check.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -ReleaseId "lts-rc1"
```

Cu lumi incluse:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\release-backup-restore-check.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -ReleaseId "lts-rc1" `
  -IncludeWorlds `
  -WorldNames world,world_nether,world_the_end
```

Ce face:

- copiaza `plugins/AINPC/`, JAR-urile AINPC din `plugins/` si `server.properties`;
- optional copiaza lumile cerute;
- creeaza o arhiva zip cu `backup-manifest.json`;
- calculeaza SHA256 pentru arhiva si pentru fiecare fisier inclus;
- face restore-check intr-un director temporar si verifica dimensiune + SHA256;
- scrie `*-report.json` si `*-restore-check.json` langa arhiva.

Arhiva poate contine config si DB reale. Trateaz-o ca artefact sensibil.

## Release API/Add-on Freeze

Script: `scripts/release-api-addon-freeze.ps1`

Exemplu:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\release-api-addon-freeze.ps1 `
  -ReleaseId "lts-rc1"
```

Ce face:

- calculeaza hash SHA256 pentru JAR-ul `ainpc-api` si addonul medieval;
- calculeaza hash SHA256 pentru sursele publice API normalizate;
- listeaza declaratiile publice detectate in `ainpc-api`;
- verifica intrari obligatorii in JAR-ul addonului: `plugin.yml`, `config-template.yml`, `packs/medieval_quest.yml` si clasele principale;
- verifica intrari obligatorii in JAR-ul API pentru interfetele/DTO-urile publice;
- verifica `plugin.yml` addon: dependinta core, main class si versiune expandata;
- scrie `.ai\release-reports\<releaseId>-api-addon-freeze.json`;
- scrie `.ai\release-reports\<releaseId>-api-addon-freeze.md`.

Pentru gate strict, adauga `-FailOnWarnings`.

## Release Report

Script: `scripts/release-report.ps1`

Exemplu pentru raport LTS/RC:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\release-report.ps1 `
  -ReleaseId "lts-rc1" `
  -ServerDir "C:\Minecraft\paper-test" `
  -BackupReport "C:\Minecraft\paper-test\ainpc-release-backups\ainpc-release-backup-lts-rc1-report.json" `
  -QuestRconReport "C:\Minecraft\paper-test\ainpc-quest-smoke-rcon-report.json" `
  -QuestPlayerReport "C:\Minecraft\paper-test\ainpc-quest-smoke-player-rcon-report.json" `
  -ApiAddonFreezeReport ".ai\release-reports\lts-rc1-api-addon-freeze.json" `
  -TestsGradle "passed" `
  -StartupSmoke "passed" `
  -MappingSmoke "passed" `
  -NpcSmoke "passed" `
  -QuestSmoke "rcon-preflight-passed; player-smoke-passed" `
  -OpenAiMode "fallback-local" `
  -AuditFinal "passed" `
  -Decision hold
```

Ce face:

- calculeaza SHA256 pentru JAR-urile core, addon medieval si API;
- citeste raportul backup/restore-check, daca este dat;
- citeste raportul quest RCON, daca este dat;
- citeste raportul quest player smoke, daca este dat;
- citeste raportul API/addon freeze, daca este dat;
- include commit, branch si `git status --short`;
- scrie `.ai\release-reports\<releaseId>-release-report.json`;
- scrie `.ai\release-reports\<releaseId>-release-report.md`.

Foloseste `-Decision release` doar cand checklistul LTS este complet. Pentru lipsa artefactului core, `-FailOnMissingRequired` opreste scriptul cu exit non-zero.
