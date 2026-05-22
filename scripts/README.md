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
- medie `selected_token_budget` si `tokens_used`;
- economii medii vs buget fix `2200`;
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

Ce face:

- ruleaza `.\gradlew.bat assemble`, daca nu folosesti `-SkipBuild`
- optional ruleaza si `.\gradlew.bat test` cu `-RunTests`
- copiaza JAR-ul core si addonul medieval in `plugins/`
- genereaza `ainpc-quest-smoke-commands.txt` in folderul serverului
- genereaza `ainpc-quest-smoke-report.txt` cu hash-uri si verificari de baza

Comenzile generate testeaza `/ainpc audit quest`, oferta/acceptarea unui quest cu `nearest`, tracking-ul si finalizarea rapida a Q01 cu iteme date prin `give`.
