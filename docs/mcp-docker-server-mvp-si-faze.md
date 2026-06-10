# MCP Docker Server MVP si Faze Ulterioare

Actualizat: 2026-05-22

## Scop

Acest document defineste pasii necesari pentru o versiune minim functionala a serverului MCP din Docker si fazele ulterioare.

Prin "server MCP minim functional" intelegem un stack local care:

- porneste prin Docker fara sa blocheze IDE-ul;
- expune endpoint MCP real pe `http://127.0.0.1:3000/mcp`;
- poate porni sidecar-ul Serena pe `http://127.0.0.1:9121/mcp` pentru navigare si refactor semantic de cod;
- pastreaza datele persistente in `data/`;
- tine codul proiectului in Chroma si keyword index;
- tine reguli, changelog si istoric conversatii Codex;
- poate fi folosit de Codex/JetBrains prin configuratie reparabila;
- are verificari, backup, restore si mentenanta one-shot;
- are watcher live pentru actualizarea vector DB dupa schimbari de cod.

Documentul foloseste ca proiect tinta:

```text
C:\Users\HeroesninjA\IdeaProjects\test
```

si ca server MCP:

```text
C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server
```

## Definitia De Gata Pentru MVP

MVP-ul serverului MCP Docker este gata doar daca toate punctele de mai jos sunt adevarate:

| Zona | Criteriu minim |
|---|---|
| Docker | serviciile `mcp`, `chroma`, `db` si sidecar-ul optional `serena` ruleaza |
| Health | `http://127.0.0.1:3000/health` raspunde `200` |
| MCP protocol | `http://127.0.0.1:3000/mcp` expune tools/resources prin Streamable HTTP |
| Serena sidecar | `http://127.0.0.1:9121/mcp` expune tools de coding semantic si este raportat in `ctx:status.serena` |
| Persistenta | `data/` este montat si include memorie, changelog, rules, keyword index si Chroma |
| Tools | sunt disponibile tool-uri pentru vectori, memory, changelog, rules, status si context pack |
| Index | colectia `ainpc_code` are acelasi numar de chunks in keyword cache si Chroma |
| Codex config | `.codex/config.toml`, `.ai/mcp/mcp.json`, `%USERPROFILE%\.codex` si JetBrains Codex config pointeaza la `/mcp`; config-urile Codex includ si sidecar-ul `serena` |
| JetBrains MCP UI | `llm.mcpServers.xml` are `ainpc-project-memory` enabled cu URL-ul corect |
| Watcher | code watcher ruleaza fara duplicate si fara lock blocat |
| Session sync | watcherul de sesiuni Codex actualizeaza `.ai/codex-conversations.md` |
| Backup | exista backup complet, optional cu Chroma |
| Restore | exista comanda documentata pentru restore controlat |
| Autostart | exista Scheduled Task sau fallback `.cmd` in Startup folder |
| Recovery | `ctx:doctor -- -Repair` poate repara stack-ul fara bucle infinite |

## Status Verificat

Verificare rulata la 2026-05-22:

```powershell
cd "C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp"
npm run ctx:doctor -- -Repair
npm run ctx:audit -- "C:\Users\HeroesninjA\IdeaProjects\test" ainpc_code
npm run ctx:status -- "C:\Users\HeroesninjA\IdeaProjects\test"
npm run ctx:test -- "C:\Users\HeroesninjA\IdeaProjects\test" ainpc_code
npm run ctx:backup -- -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test" -Collection ainpc_code -KeepLatestJson 20 -KeepLatestChroma 10
```

Rezultat curent:

| Zona | Status |
|---|---|
| Docker | `mcp`, `chroma`, `db` si `serena` ruleaza; containerul MCP este healthy |
| MCP | `/health` raspunde OK; `/mcp` expune 16 tools si 4 resources |
| Serena | sidecar-ul `serena` raspunde pe `http://127.0.0.1:9121/mcp` si expune 23 tools de coding semantic |
| Index | `ainpc_code` are keyword cache si Chroma in parity; `ctx:status` raporteaza acelasi numar pentru keyword chunks si Chroma vectors |
| Audit | `ctx:audit` raporteaza `ok: true`, fara missing/extra/duplicate IDs |
| Config | JetBrains Codex, JetBrains MCP UI, global Codex, project `.codex` si `.ai/mcp` au URL-ul `http://127.0.0.1:3000/mcp`; global/project Codex si `.ai/mcp` includ si `serena` la `http://127.0.0.1:9121/mcp` |
| Watcher | code watcher si session-sync watcher ruleaza fara lock activ |
| Backup | backup JSON verificat creat prin comanda de mai sus in `data/backups/`; backup cu Chroma ramane opt-in prin `-IncludeChroma` |
| Teste U7 | `npm run ctx:test` trece pentru health, smoke, memory, vector, context pack, Serena smoke, wrapper PowerShell si lifecycle Docker fallback |
| Economie token | testul staged context din 2026-05-22 are `4/4` rulari pe attempt 1, buget selectat mediu `900`, consum mediu `475.75` tokens si economie medie reala `1724.25` tokens fata de buget fix `2200` |
| Observabilitate U4 | `ctx:status` raporteaza keyword/Chroma parity, loguri watcher, erori recente, erori active nerecuperate, loguri oversized si `warnings` |
| Backup U6 | backup-urile includ manifest verificat, `backup-state.json`, status in `ctx:status` si retentie opt-in |
| Restore U6 | `ctx:test-restore` valideaza restore izolat din ultimul backup, fara sa atinga datele live |
| Integrare U8 | `codex:preflight` verifica starea, detecteaza drift de config, poate repara non-distructiv si incarca prompt context |
| Dashboard U5 | `/admin` si `/admin/status.json` ofera dashboard local read-only pentru starea MCP, cu citire tail-limited pentru loguri |
| Autostart U11 | Startup fallback foloseste path absolut catre Windows PowerShell, nu `powershell.exe` dependent de PATH |
| Securitate U2 | porturile Docker `3000`, `9121`, `8000` si `5432` sunt bind-uite explicit pe `127.0.0.1`; write tools au audit local bounded |
| Embeddings U3 | `project_status` raporteaza embedding health si signature; backup manifest include provider/model/url/signature |

MVP-ul operational este acceptat pentru proiectul curent. Urmatoarele schimbari ar trebui tratate ca faze ulterioare, nu ca blocante pentru folosirea MCP in Codex.

## Non-Obiective Pentru MVP

Nu bloca MVP-ul MCP pe aceste lucruri:

- autentificare externa multi-user;
- deployment public pe internet;
- UI web complet pentru administrare;
- observabilitate Prometheus/Grafana;
- suport multi-proiect automat;
- embeddings remote obligatorii;
- backup incremental sofisticat;
- cluster Chroma sau Postgres production-grade;
- integrare cu orice IDE in afara de Codex/JetBrains curent.

Acestea intra in faze ulterioare.

## Structura De Baza

```text
C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\
  docker-compose.yml
  data\
    chroma\
    backups\
    keyword-index.json
    rules.json
    changelog.json
    conversation_memory.json
    codex-session-sync-state.json
  mcp\
    server.js
    package.json
    RUNBOOK.md
    scripts\
  serena container\
    image: ghcr.io/oraios/serena:latest
    project mount: C:\Users\HeroesninjA\IdeaProjects\test -> /workspaces/test
```

Endpointuri:

```text
Health: http://127.0.0.1:3000/health
MCP:    http://127.0.0.1:3000/mcp
Serena: http://127.0.0.1:9121/mcp
Chroma: http://127.0.0.1:8000
DB:     127.0.0.1:5432
```

Regula importanta: clientii MCP trebuie sa foloseasca `/mcp`, nu radacina `http://localhost:3000`.
Serverul accepta si `http://127.0.0.1:3000/` ca alias local de compatibilitate pentru JetBrains ACP cand pastreaza un URL vechi fara path, dar configuratia corecta ramane `/mcp`.
Pentru containerul `mcp`, Serena este apelata prin URL intern Docker `http://serena:9121/mcp`; pentru Codex/host se foloseste `http://127.0.0.1:9121/mcp`.
Porturile Docker sunt publicate doar pe `127.0.0.1`; nu expune stack-ul pe LAN/Internet fara faza U2 remote completa.

## Faza 0: Cerinte Locale

Scop: masina poate porni stack-ul fara improvizatii.

Cerinte:

- Docker Desktop pornit;
- PowerShell disponibil;
- Node.js disponibil;
- Docker CLI sau Docker Compose disponibil in PATH ori in locatiile standard Docker Desktop din `Program Files`;
- `npm install` rulat in folderul `mcp`, daca dependintele lipsesc;
- proiectul tinta exista;
- porturile `3000`, `9121`, `8000` si `5432` nu sunt ocupate de alt stack important.

Verificari:

```powershell
docker --version
node --version
npm --version
```

Gate:

- comenzile raspund;
- Docker poate rula `docker compose ps`;
- nu exista proces vechi care tine porturile `3000` sau `9121` cu alt server.

## Faza 1: Pornire Docker Non-Blocking

Scop: serverul porneste in fundal si nu blocheaza IDE-ul.

Comenzi:

```powershell
cd "C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server"
docker compose up -d --build
docker compose ps
```

Verificari:

```powershell
curl.exe -i http://127.0.0.1:3000/health
```

Semne bune:

- containerul MCP este `healthy`;
- containerul Serena este `Up` si publica `127.0.0.1:9121->9121`;
- Chroma este `Up`;
- Postgres este `Up`;
- `/health` raspunde cu `{"status":"ok"}`.

Gate:

- `mcp-ai-server-mcp-1` ruleaza;
- health check trece;
- comanda a revenit in terminal, nu a ramas atasata la loguri.

## Faza 2: MCP Protocol Si Smoke Test

Scop: endpointul este MCP real, nu doar HTTP simplu.

Comenzi:

```powershell
cd "C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp"
npm run mcp:smoke
npm run ctx:status -- "C:\Users\HeroesninjA\IdeaProjects\test"
```

Gate minim:

- `mcp:smoke` intoarce `ok: true`;
- tool count este mai mare decat zero;
- resources includ `project://rules`, `project://changelog`, `project://memory`, `project://status`;
- `ctx:status` nu raporteaza eroare la MCP;
- `ctx:status.serena.ok` este `true` cand sidecar-ul Serena este pornit.

Tools asteptate pentru MVP:

```text
vector_upsert
vector_search
vector_upsert_many
vector_search_by_file
vector_delete_by_file
memory_add
memory_list
memory_search
changelog_add
changelog_list
changelog_search
rules_set
rules_list
rules_search
project_status
context_pack
```

Tools Serena asteptate pentru coding semantic:

```text
initial_instructions
get_symbols_overview
find_symbol
find_referencing_symbols
find_implementations
find_declaration
get_diagnostics_for_file
rename_symbol
replace_symbol_body
insert_after_symbol
insert_before_symbol
safe_delete_symbol
search_for_pattern
```

Serena poate expune si tools de memorie/dashboard proprii, dar pentru MVP conteaza in primul rand navigarea si refactorul pe simboluri.

## Faza 3: Persistenta In `data/`

Scop: restartul Docker nu sterge contextul.

Fisiere minime:

```text
data/rules.json
data/changelog.json
data/conversation_memory.json
data/keyword-index.json
data/chroma/chroma.sqlite3
```

Verificare:

```powershell
npm run ctx:status -- "C:\Users\HeroesninjA\IdeaProjects\test"
```

Gate:

- `chroma.sqlite3` exista;
- `keyword-index.json` exista;
- counts din `project_status` arata reguli, changelog, memory si vectori.

## Faza 4: Configurare Codex Si JetBrains

Scop: Codex foloseste serverul MCP automat, fara configurare manuala repetata.

Comanda principala:

```powershell
cd "C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp"
npm run ctx:register-codex -- -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test"
```

Aceasta repara:

- `%CODEX_HOME%\config.toml`;
- `%USERPROFILE%\.codex\config.toml`;
- `C:\Users\HeroesninjA\IdeaProjects\test\.codex\config.toml`;
- `C:\Users\HeroesninjA\IdeaProjects\test\.ai\mcp\mcp.json`;
- JetBrains `llm.mcpServers.xml`.

Prin default, comanda repara si intrarea Serena in config-urile Codex TOML/JSON. JetBrains `llm.mcpServers.xml` ramane pe `ainpc-project-memory`, ca MCP UI JetBrains sa nu fie suprascris cu sidecar-ul de coding.

Config TOML minim:

```toml
[mcp_servers.ainpc-project-memory]
url = "http://127.0.0.1:3000/mcp"

[mcp_servers.serena]
url = "http://127.0.0.1:9121/mcp"
startup_timeout_sec = 30
```

Config JSON minim:

```json
{
  "mcpServers": {
    "ainpc-project-memory": {
      "transport": "streamable_http",
      "url": "http://127.0.0.1:3000/mcp"
    },
    "serena": {
      "transport": "streamable_http",
      "url": "http://127.0.0.1:9121/mcp"
    }
  }
}
```

Gate:

```powershell
npm run ctx:status -- "C:\Users\HeroesninjA\IdeaProjects\test"
```

Trebuie sa arate `urlMatches: true` pentru:

- `jetbrains`;
- `global`;
- `project`;
- `aiMcpJson`;
- `jetbrainsMcpSettings`.

Pentru Serena trebuie sa arate:

- `codexConfig.sidecars.serena.ready: true`;
- `codexConfig.sidecars.serena.global.urlMatches: true`;
- `codexConfig.sidecars.serena.project.urlMatches: true`;
- `codexConfig.sidecars.serena.aiMcpJson.urlMatches: true`.

## Faza 5: Indexare Initiala A Proiectului

Scop: codul proiectului intra in vector DB si keyword cache.

Comenzi:

```powershell
npm run ctx:rules -- "C:\Users\HeroesninjA\IdeaProjects\test"
npm run ctx:changelog -- "C:\Users\HeroesninjA\IdeaProjects\test" 20
npm run ctx:index -- "C:\Users\HeroesninjA\IdeaProjects\test" ainpc_code
```

Sau refresh complet:

```powershell
npm run ctx:refresh -- "C:\Users\HeroesninjA\IdeaProjects\test" 20 ainpc_code
```

Audit:

```powershell
npm run ctx:audit -- "C:\Users\HeroesninjA\IdeaProjects\test" ainpc_code
```

Gate:

- `ok: true`;
- `expected`, `keyword` si `chroma` sunt egale;
- `missingIds` si `extraIds` sunt goale;
- lock-ul `data/index-ainpc_code.lock` nu exista dupa terminare.

## Faza 6: Context Pack Si Optimizare Token

Scop: Codex primeste context scurt, relevant, nu tot repo-ul.

Wrapper local recomandat din proiect (`test/scripts`):

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-staged.ps1 `
  -Query "QuestTemplateSelector progression objective" `
  -Profile code `
  -NoTests
```

Shortcut pentru cazuri de implementare cod:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-code.ps1 `
  -Query "QuestTemplateSelector progression objective" `
  -NoTests
```

Scriptul aplica automat etapele `900/8 -> 1400/12 -> 2200/14`.
Pragurile implicite pentru context suficient sunt:

- `code`: `450`;
- `debug`: `320`;
- `planning`: `650`;
- `full`: `800`.

Auto-scope implicit (token efficiency):

- `code`: include `ainpc-core-plugin/src/main/`, `ainpc-api/src/main/`, `ainpc-scenario-medieval/src/main/`, `scripts/`;
- `debug`: include cod + `docs/`;
- `planning`: include `docs/`;
- exclude implicit `src/test`, `target`, `build`, `.gradle`, `.kotlin`, `.idea`, `node_modules`.

Pentru context larg fara filtre automate, adauga `-DisableAutoScope`.

Poti salva output-ul selectat:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-staged.ps1 `
  -Query "QuestTemplateSelector progression objective" `
  -Profile code `
  -NoTests `
  -OutFile ".ai\last-mcp-context.txt"
```

Poti salva si sumarul tehnic al incercarii alese (JSON):

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-staged.ps1 `
  -Query "QuestTemplateSelector progression objective" `
  -Profile code `
  -NoTests `
  -SummaryJson ".ai\last-mcp-context-summary.json"
```

Pentru analiza periodica a eficientei token:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-summary-report.ps1 `
  -InputPath ".ai" `
  -FilePattern "*mcp-context*summary*.json*" `
  -OutFile ".ai\mcp-context-report.json" `
  -RowsCsvOutFile ".ai\mcp-context-report-rows.csv" `
  -ProfilesCsvOutFile ".ai\mcp-context-report-profiles.csv"
```

Raportul ajuta la reglajul pragurilor/bugetelor pe date reale (attempt rate, tokens used, savings vs 2200).

Baseline verificat pe 2026-05-22:

- summary: `.ai\token-economy-test-20260522-summary.jsonl`;
- report: `.ai\token-economy-test-20260522-report.json`;
- CSV rows: `.ai\token-economy-test-20260522-rows.csv`;
- CSV profiles: `.ai\token-economy-test-20260522-profiles.csv`;
- `4/4` query-uri au fost rezolvate din attempt 1 (`900/8`);
- buget selectat mediu: `900` tokens, adica `1300` tokens economisiti fata de buget fix `2200`;
- consum real mediu: `475.75` tokens, adica `1724.25` tokens economisiti fata de buget fix `2200`;
- pe profiluri: `code=516`, `debug=235`, `planning=576` tokens mediu;
- toate rularile au folosit auto-scope implicit.

Interpretare: testul confirma economia pipeline-ului staged fata de un context fix de `2200` tokens. Nu este un benchmark full-repo; pentru asta ruleaza batch/nightly pe mai multe query-uri reale si compara cu raportul agregat.

Pentru auto-tune al pragului `MinCharsForSuccess` din raport:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-code.ps1 `
  -Query "QuestTemplateSelector progression objective" `
  -NoTests `
  -UseAdaptiveThresholds `
  -AdaptiveReportPath ".ai\mcp-context-report.json" `
  -AdaptiveMinRuns 5
```

Batch/nightly pentru set de query-uri:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-nightly.ps1 `
  -QueryFile ".ai\mcp-nightly-queries.txt" `
  -OutputDir ".ai" `
  -Profile code `
  -NoTests `
  -UseAdaptiveThresholds
```

Batch/nightly pe profiluri multiple:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-nightly-matrix.ps1 `
  -OutputRoot ".ai\nightly" `
  -ReportRoot ".ai" `
  -Profiles code,debug,planning `
  -NoTests `
  -UseAdaptiveThresholds `
  -EnsureQueryTemplates `
  -WriteCsvReports
```

Programare zilnica in Task Scheduler (preview):

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-register-schedule.ps1 `
  -TaskName "AINPC-MCP-Context-Nightly" `
  -StartTime "02:30"
```

Pentru aplicare efectiva, adauga `-Apply`.
Scriptul de scheduler foloseste implicit launcherul scurt `scripts/mcp-context-nightly-matrix-launcher.ps1` pentru a evita limita de lungime `/TR`.

Diagnoza scheduler daca apar erori la `-Apply`:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-scheduler-diagnose.ps1 `
  -OutFile ".ai\scheduler-diagnose.json"
```

Fallback daca `schtasks` ramane indisponibil:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-register-startup.ps1
```

Adauga `-Apply` pentru creare/update in Startup folder (user logon trigger).

Health-check rapid pentru intregul pipeline de automatizare MCP:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-automation-status.ps1 `
  -ProjectRoot "." `
  -AiDir ".ai" `
  -FreshHours 30 `
  -OutFile "mcp-context-automation-status.json"
```

Pentru gate strict (CI/local), adauga:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-automation-status.ps1 `
  -ProjectRoot "." `
  -AiDir ".ai" `
  -FreshHours 30 `
  -OutFile "mcp-context-automation-status.json" `
  -TextSummaryOutFile "mcp-context-automation-status.txt" `
  -FailOnUnhealthy
```

Comanda prompt-ready (start cu buget mic):

```powershell
npm run mcp:context -- "QuestTemplateSelector progression objective" 900 8 ainpc_code prompt code "" "" true hybrid
```

Wrapper PowerShell recomandat:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\codex-context.ps1 `
  -Query "QuestTemplateSelector progression objective" `
  -Profile code `
  -TokenBudget 900 `
  -TopK 8 `
  -Include "ainpc-core-plugin/src/main/kotlin/" `
  -Exclude "ainpc-core-plugin/src/test/" `
  -NoTests `
  -Retrieval hybrid
```

Escaladare controlata, doar cand contextul este insuficient:

```powershell
# pas 2
npm run mcp:context -- "QuestTemplateSelector progression objective" 1400 12 ainpc_code prompt code "" "" true hybrid

# pas 3 (cross-module / investigatii ambigue)
npm run mcp:context -- "QuestTemplateSelector progression objective" 2200 14 ainpc_code prompt code "" "" true hybrid
```

Simulare cost/context:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\context-simulate.ps1 `
  -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test" `
  -Query "MCP context stack final verification register-codex watcher audit backup" `
  -TokenBudget 900 `
  -TopK 8 `
  -NoTests `
  -Retrieval hybrid `
  -Profile debug
```

Gate:

- `mcp.token_used_estimate` este sub buget;
- rezultatele includ fisiere relevante;
- workflow-ul urmeaza `900/8 -> 1400/12 -> 2200/14` doar la nevoie;
- raportul fata de repo complet confirma reducere mare de tokens;
- filtrele goale nu sunt interpretate ca path fals.

## Faza 6.1: Serena Coding Sidecar

Scop: Codex poate folosi tools de coding semantic fara sa incarce fisiere mari in context si fara sa inlocuiasca memoria Chroma.

Roluri clare:

- `ainpc-project-memory` ramane MCP-ul principal pentru context, memory, rules, changelog, audit, backup si Chroma;
- `serena` este sidecar MCP pentru symbol search, references, diagnostics si refactor pe cod;
- Serena nu scrie in `data/write-audit.json`, pentru ca nu este serverul de memory/changelog/rules al proiectului;
- Serena nu trebuie folosita pentru runtime Paper/Minecraft live; pentru asta se face un MCP runtime separat.

Configurare Docker:

```yaml
serena:
  image: ghcr.io/oraios/serena:latest
  command:
    - serena
    - start-mcp-server
    - --transport
    - streamable-http
    - --host
    - 0.0.0.0
    - --port
    - "9121"
    - --project
    - /workspaces/test
    - --context=codex
    - --open-web-dashboard
    - "false"
  ports:
    - "127.0.0.1:9121:9121"
  volumes:
    - C:/Users/HeroesninjA/IdeaProjects/test:/workspaces/test
```

Verificare:

```powershell
docker compose ps serena
npm run ctx:status -- "C:\Users\HeroesninjA\IdeaProjects\test"
npm run codex:preflight -- -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test" -Collection ainpc_code -Query "Serena smoke" -NoTests -SkipContext -Json
```

Gate:

- `docker compose ps serena` arata containerul `Up`;
- `ctx:status.serena.ok` este `true`;
- `ctx:status.serena.toolCount` este cel putin `23` in configuratia curenta;
- `ctx:status.codexConfig.sidecars.serena.ready` este `true`;
- `codex:preflight` include `status.serena_ok: true` si `status.serena_tools`;
- `/admin/status.json` include `serena.ok: true`, `serena.tool_count` si `required_tools_present: true`.

Flux recomandat in sesiuni Codex/JetBrains:

```text
Activate the current dir as project using serena and read initial instructions.
```

Foloseste Serena pentru intrebari de tip:

- unde este simbolul/clasa/metoda;
- cine referentiaza un simbol;
- unde este declaratia/implementarea;
- ce diagnostice are un fisier;
- rename/refactor pe simboluri.

## Faza 7: Watcher Live Pentru Cod

Scop: dupa editari, Chroma si keyword index se actualizeaza fara reindex manual complet.

Pornire:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-watch-index.ps1 `
  -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test" `
  -Collection "ainpc_code"
```

Oprire:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-watch-index.ps1
```

Verificare:

```powershell
npm run ctx:status -- "C:\Users\HeroesninjA\IdeaProjects\test"
```

Gate:

- statusul raporteaza `watcher.running: true`;
- exista PID valid;
- editarea unui fisier produce actualizare in audit dupa scurt timp;
- nu apar doua watcher-e pentru acelasi proiect.

## Faza 8: Sincronizare Istoric Codex

Scop: conversatiile Codex devin context compact in proiect si MCP memory.

Comenzi:

```powershell
npm run codex:sync-sessions -- "C:\Users\HeroesninjA\IdeaProjects\test" 25 10
powershell -ExecutionPolicy Bypass -File .\scripts\start-session-sync.ps1 -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test"
```

Output principal:

```text
C:\Users\HeroesninjA\IdeaProjects\test\.ai\codex-conversations.md
```

Gate:

- `ctx:status` raporteaza `sessionSyncWatcher.running: true`;
- `codexSessionSync.sessions` este mai mare decat zero;
- `memorySynced` creste pentru sesiuni noi;
- fisierul `.ai/codex-conversations.md` este indexat in vector DB.

## Faza 9: Doctor, Mentenanta Si Recovery

Scop: exista comenzi one-shot pentru reparatie fara loop blocant.

Verificare:

```powershell
npm run ctx:doctor
```

Repair:

```powershell
npm run ctx:doctor -- -Repair
```

Mentenanta sigura:

```powershell
npm run ctx:maintain -- -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test" -Collection "ainpc_code"
```

Mentenanta cu backup Chroma:

```powershell
npm run ctx:maintain -- -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test" -Collection "ainpc_code" -IncludeChroma
```

Gate:

- doctor confirma Docker, MCP smoke si config;
- repair nu duplica watcher-ele;
- audit ramane curat dupa mentenanta;
- daca exista lock activ, indexarea manuala este sarita sau asteapta controlat, nu intra in loop.

## Faza 10: Backup Si Restore

Scop: memoria MCP si vector DB pot fi restaurate.

Backup fara Chroma:

```powershell
npm run ctx:backup -- -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test"
```

Backup complet cu Chroma:

```powershell
npm run ctx:backup -- -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test" -IncludeChroma
```

Restore controlat:

```powershell
npm run ctx:restore -- -ArchivePath "C:\path\to\context-backup.zip" -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test" -Force
```

Restore cu Chroma:

```powershell
npm run ctx:restore -- -ArchivePath "C:\path\to\context-backup.zip" -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test" -RestoreChroma -Force
```

Gate:

- backup-ul include `data/*.json`, `keyword-index.json`, scripturi, configuri Codex/JetBrains si Startup fallback;
- `-IncludeChroma` include `data/chroma`;
- restore cere explicit `-Force`;
- dupa restore rulezi `npm run ctx:doctor -- -Repair`.

## Faza 11: Autostart

Scop: dupa restart Windows, stack-ul revine fara pornire manuala.

Instalare:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install-context-autostart.ps1 `
  -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test" `
  -Collection "ainpc_code"
```

Dezinstalare:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\remove-context-autostart.ps1
```

Daca Scheduled Task este refuzat cu `Access is denied`, fallback-ul acceptat este:

```text
C:\Users\HeroesninjA\AppData\Roaming\Microsoft\Windows\Start Menu\Programs\Startup\AINPC MCP Context Stack.cmd
```

Gate:

- `ctx:status` raporteaza `autostart.startupFallback.exists: true` sau Scheduled Task valid;
- fallback-ul refera `start-context-stack.ps1`;
- fallback-ul refera proiectul corect.
- fallback-ul foloseste executabil PowerShell absolut, de forma `C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe`, nu `powershell.exe` rezolvat din PATH.

## Faza 12: Reparatii JetBrains ACP

Scop: erorile de sesiune JetBrains nu blocheaza serverul MCP sau Codex.

Inspectie:

```powershell
npm run ctx:repair-jetbrains-acp
```

Repair tintit pentru un chat:

```powershell
npm run ctx:repair-jetbrains-acp -- -ChatId "<jetbrains-chat-id>" -Repair
```

Reguli:

- nu sterge SQLite Codex direct;
- muta doar `.agentsession` stale dupa backup;
- foloseste `-AllStale` doar pentru cleanup deliberat;
- dupa repair ruleaza `ctx:register-codex` si `ctx:status`.

Semnatura problemei:

```text
Failed to initialize ACP session.
Internal error: "no rollout found for thread id ..."
```

Alta semnatura reparata prin compatibilitatea root MCP:

```text
MCP server npc-server failed to start
Unexpected content type: text/html ... Cannot POST /
```

Config-ul JetBrains/Codex trebuie tot reparat la `http://127.0.0.1:3000/mcp`; aliasul root exista doar ca protectie pentru cache-uri ACP vechi.

## MVP Final: Checklist Scurt

Ruleaza:

```powershell
cd "C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp"
npm run ctx:doctor -- -Repair
npm run ctx:audit -- "C:\Users\HeroesninjA\IdeaProjects\test" ainpc_code
npm run ctx:status -- "C:\Users\HeroesninjA\IdeaProjects\test"
npm run ctx:backup -- -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test" -IncludeChroma
```

MVP-ul este acceptat cand:

- `ctx:doctor -- -Repair` trece;
- `ctx:audit` are `ok: true`;
- `ctx:status` arata MCP healthy, configs corecte, watcher running, session sync running si autostart valid;
- exista backup final;
- Codex vede tool-urile MCP in sesiune noua.

## Faze Ulterioare

Ordinea recomandata dupa MVP:

1. U7 pentru teste automate minime ale serverului MCP si scripturilor PowerShell.
2. U4 pentru observabilitate operationala: log rotation, ultime erori si avertizari in `ctx:status`.
3. U6 pentru politica de backup incremental si pastrare arhive.
4. U8 pentru integrare mai buna cu sesiuni Codex/JetBrains, mai ales preflight si detectarea rescrierilor de config - initial implementat.
5. U1 doar cand apare al doilea proiect real, ca sa nu complici devreme configuratia curenta.

### U1: Multi-Proiect

Scop: acelasi server MCP poate indexa mai multe proiecte fara amestec de context.

Prioritati:

- colectii separate per proiect;
- status per proiect;
- watcher per proiect cu PID separat;
- backup partial per proiect;
- reguli clare pentru `ProjectRoot` si `Collection`.

### U2: Securitate Si Acces

Scop: serverul poate fi folosit in siguranta din afara masinii locale, daca va fi nevoie.

Status initial: implementat pentru hardening local-only.

Setarea curenta din `docker-compose.yml`:

```yaml
ports:
  - "127.0.0.1:3000:3000"
  - "127.0.0.1:9121:9121"
  - "127.0.0.1:8000:8000"
  - "127.0.0.1:5432:5432"
```

Write tools MCP append-uiesc metadate compacte in:

```text
C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\data\write-audit.json
```

Auditul este bounded prin `MCP_WRITE_AUDIT_LIMIT` si nu stocheaza continutul complet al memoriei sau codului.

Prioritati:

- bind explicit pe localhost pentru MVP - initial implementat;
- include si sidecar-ul Serena pe `127.0.0.1:9121`, nu pe host bind larg;
- token/header auth pentru acces remote;
- CORS/transport clar;
- protectie pentru tool-urile de write;
- audit pentru cine a scris memory/changelog/rules - initial implementat ca audit local pentru write tools.

### U3: Embeddings Production-Grade

Scop: embeddings stabile si reproductibile.

Status initial: implementat pentru observabilitate si reproducibilitate operationala.

Setarea curenta ramane local-safe:

```text
EMBEDDING_PROVIDER=local
EMBEDDING_MODEL=nomic-embed-text
```

Raportare:

- `project_status.embeddings` include provider, URL, model, dimensiune, fallback provider si `health`;
- health-ul local nu depinde de retea;
- pentru provider remote, health-ul foloseste timeout-ul `MCP_EMBEDDING_HEALTH_TIMEOUT_MS` si raporteaza `fallback_active`;
- `data/keyword-index.json` pastreaza embedding signature pentru vectorii indexati;
- `ctx:status` si `ctx:audit` avertizeaza daca signature-ul activ nu corespunde indexului;
- `ctx:index` refuza incrementalul pe mismatch si cere reindex controlat cu `--force`;
- `manifest.json` din backup include provider/model/url/signature si daca exista API key, fara valoarea cheii.

Reindex controlat dupa schimbarea providerului sau modelului:

```powershell
cd "C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp"
npm run ctx:backup -- -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test" -Collection ainpc_code -IncludeChroma -KeepLatestJson 20 -KeepLatestChroma 10
npm run ctx:index -- "C:\Users\HeroesninjA\IdeaProjects\test" ainpc_code --force
npm run ctx:audit -- "C:\Users\HeroesninjA\IdeaProjects\test" ainpc_code
```

Prioritati:

- alegere explicita intre local si OpenAI-compatible - initial implementat prin config si status;
- health check pentru embeddings provider - initial implementat;
- fallback documentat - initial implementat;
- reindex controlat cand modelul de embeddings se schimba - initial implementat prin signature gate si `--force`;
- raport cu modelul folosit in backup manifest - initial implementat.

### U4: Observabilitate

Scop: problemele devin vizibile fara investigatii manuale lungi.

Status initial: implementat pentru observabilitatea operationala minima.

Comanda principala:

```powershell
cd "C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp"
npm run ctx:status -- "C:\Users\HeroesninjA\IdeaProjects\test"
```

Acum statusul include:

- `index.chromaVectors` si `index.keywordChromaMatch`;
- `serena.ok`, `serena.toolCount`, `serena.tools` si erori de conectare la sidecar;
- `codexConfig.sidecars.serena` pentru drift in config-urile Codex ale sidecar-ului;
- `observability.logs` pentru `watch-index` si `session-sync`, stdout si stderr;
- `recentIssueCount`, `recentIssues`, `activeRecentIssueCount`, `activeRecentIssues`, `recoveredRecentIssueCount`, `lastIssueAt`, `lastSuccessAt` si `oversized` pentru fiecare log;
- `observability.latestIndexRun` cu statusul ultimului refresh, motivul, durata, timestampurile si sumarul JSON (`indexedFiles`, `skippedFiles`, `deletedFiles`, `totalChunks`, `keywordChunks`);
- `observability.warnings` si `warnings` la nivel de status doar pentru erori active nerecuperate, nu pentru erori tranzitorii urmate de succes;
- praguri configurabile prin `MCP_STATUS_LOG_MAX_BYTES`, `MCP_STATUS_LOG_TAIL_BYTES`, `MCP_STATUS_LOG_RECENT_MINUTES`, plus `MCP_ADMIN_LOG_TAIL_BYTES` si `MCP_ADMIN_LOG_RECENT_MINUTES` pentru dashboard;
- rotatie de loguri in `start-watch-index.ps1` si `start-session-sync.ps1` prin `-MaxLogBytes` si `-MaxRotatedLogs`;
- scriere atomica pentru `keyword-index.json` si fisierele JSON MCP, ca citirile concurente sa nu reseteze cache-ul la fallback;
- validare in `npm run ctx:test`.

Prioritati:

- log rotation pentru watchers - initial implementat;
- sumar de erori in `ctx:status` - initial implementat; include recovery-aware warnings pentru restarturi/downtime tranzitorii;
- metrici pentru durata indexarii - initial implementat prin `observability.latestIndexRun.durationMs`;
- numar fisiere schimbate per run - initial implementat prin `observability.latestIndexRun.result`;
- avertizare pentru Chroma/keyword mismatch - initial implementat.

### U5: UI Sau Dashboard Admin

Scop: adminul poate vedea starea MCP fara terminal.

Status initial: implementat ca dashboard local read-only.

Endpointuri:

```text
http://127.0.0.1:3000/admin
http://127.0.0.1:3000/admin?collection=ainpc_code
http://127.0.0.1:3000/admin/status.json
```

Dashboard-ul afiseaza:

- status health si stare generala;
- chunks, colectie, inventar Chroma read-only, keyword/Chroma parity si index lock;
- Serena online/offline, tool count, endpoint intern Docker si prezenta required tools;
- ultimul backup, verificarea lui si politica de retentie;
- watcher PID files;
- ultimul refresh de indexare: status, durata, motiv, fisiere indexate/sarite/sterse si chunks generate;
- ultimele erori recente din loguri, separate in active vs recovered;
- pragul de citire tail pentru loguri, astfel incat `/admin/status.json` sa nu citeasca fisiere de log complete cand acestea cresc;
- comenzi copy-only pentru doctor, audit, backup, preflight si restore test.

Dashboard-ul respecta parametrul `?collection=<name>` si il transmite catre `/admin/status.json`; comenzile copy-only pentru audit, backup si preflight folosesc aceeasi colectie. `/admin/status.json` expune si un inventar Chroma limitat/read-only cu lista de colectii disponibile si regulile de nume. Numele colectiilor trebuie sa aiba 3-63 caractere si sa contina doar litere, cifre, `_` si `-`; cererile admin invalide primesc HTTP 400, nu sunt sanitizate tacit in alt nume Chroma. Citirile de status raman read-only: o colectie Chroma inexistenta este raportata ca missing, nu este creata implicit.
Aceeasi validare si aceeasi regula read-only se aplica si pentru tool-urile MCP `project_status`, `vector_search`, `vector_search_by_file` si `context_pack`: colectiile lipsa nu sunt create implicit; doar upsert-urile creeaza colectii.

Prioritati:

- status health - initial implementat;
- chunks si colectii - initial implementat;
- ultimul backup - initial implementat;
- watcher PIDs - initial implementat;
- ultimele erori - initial implementat pentru erori recente; extins cu active/recovered dupa succes watcher;
- metrici ultimul refresh index - initial implementat din logurile `watch-index`;
- citire tail-limited pentru logurile dashboard-ului - implementat prin `MCP_ADMIN_LOG_TAIL_BYTES`;
- butoane pentru doctor/audit/backup one-shot - implementat ca butoane copy-only, nu executie din browser.

### U6: Backup Incremental

Scop: backup-urile nu devin prea mari cand Chroma creste.

Status initial: implementat ca politica operationala cu doua niveluri.

Comenzi recomandate:

```powershell
cd "C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp"

# backup JSON frecvent, fara Chroma
npm run ctx:backup -- -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test" -Collection ainpc_code -KeepLatestJson 20 -KeepLatestChroma 10

# backup complet cu Chroma, mai rar
npm run ctx:backup -- -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test" -Collection ainpc_code -IncludeChroma -KeepLatestJson 20 -KeepLatestChroma 10

# test de restore izolat, fara suprascrierea datelor live
npm run ctx:test-restore -- "C:\Users\HeroesninjA\IdeaProjects\test"
```

Backup-ul include acum:

- `manifest.json` in arhiva;
- verificare ZIP dupa creare, cu `-NoVerify` doar pentru cazuri speciale;
- `data/backup-state.json` cu ultimul backup, tipul lui si rezultatul verificarii;
- raportare in `ctx:status.backup`;
- retentie opt-in prin `-KeepLatest`, `-KeepLatestJson`, `-KeepLatestChroma`.
- test de restore izolat prin `ctx:test-restore`, care redirectioneaza project/data/Codex/JetBrains target-uri in `data/tmp`.

Prioritati:

- backup JSON frecvent - initial implementat;
- backup Chroma mai rar - initial implementat prin `-IncludeChroma`;
- pastrare N arhive recente - initial implementat opt-in;
- verificare ZIP dupa creare - initial implementat;
- restore test pe folder separat - initial implementat prin `ctx:test-restore`.

### U7: Teste Automate Pentru MCP Server

Scop: schimbarea serverului MCP nu rupe tool-urile Codex.

Status initial: implementat pentru gate-ul minim prin:

```powershell
cd "C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp"
npm run ctx:test
```

Acest test acopera:

- health MCP si heartbeat Chroma;
- prezenta celor 16 tools si 4 resources;
- binding local pentru Serena pe `127.0.0.1:9121` si lipsa binding-ului larg `"9121:9121"`;
- smoke test pentru Serena cu required tools de coding semantic;
- `memory_add`, `memory_list`, `memory_search`;
- `vector_upsert_many`, `vector_search`, `vector_search_by_file`, `vector_delete_by_file` pe colectia izolata `ainpc_u7_test`;
- `context_pack` cu buget mic (`TokenBudget 900`, `TopK 8`);
- statusul admin expune inventarul Chroma read-only si regulile de nume pentru colectii;
- tool-urile MCP de citire nu creeaza colectii Chroma lipsa;
- numele invalide de colectii sunt respinse de tool-urile MCP si `/admin/status.json`;
- `context-simulate.ps1` cu include/exclude goale.
- fallback pentru Docker/Compose cand Docker nu este in PATH, dar Docker Desktop este instalat in `Program Files`.
- helper comun `scripts/docker-compose-utils.ps1` pentru path-uri Docker Desktop, compose plugins si credential helpers.
- propagare corecta a exit code-ului nativ Docker/Compose, ca build/start/doctor sa nu raporteze succes fals dupa esec.
- `start-context-stack.ps1 -SkipRefresh` revine curat si nu lasa `docker-compose up` in foreground.
- `codex-preflight.ps1` raporteaza `serena_ok` si `serena_tools`.
- validare pentru Startup fallback: daca exista `.cmd`, trebuie sa foloseasca Windows PowerShell cu path absolut.
- `ctx:test-restore` pentru restore izolat din ultimul backup.

Prioritati:

- test pentru `mcp:smoke`;
- test pentru `memory_add/list/search`;
- test pentru `vector_upsert/search/delete`;
- test pentru `context_pack` cu buget mic (`TokenBudget 900`, `TopK 8`);
- test pentru scripts PowerShell cu argumente goale.
- test pentru rezolvarea Docker/Compose in afara PATH si pentru forwarding corect al argumentelor `up -d`.
- test pentru propagarea erorilor native din helper-ul Docker/Compose.

### U8: Integrare Mai Buna Cu Codex/IDE

Scop: sesiunea Codex primeste context corect automat.

Status initial: implementat ca preflight operational per task.

Comanda recomandata:

```powershell
cd "C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp"
npm run codex:preflight -- -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test" -Collection ainpc_code -Query "<task sau simbol>" -Repair
```

Preflight-ul acopera:

- MCP health, tool/resource inventory si watcher state;
- Serena sidecar health si tool count (`status.serena_ok`, `status.serena_tools`);
- keyword/Chroma parity si lock activ;
- drift in config-urile Codex/JetBrains raportat si in `ctx:status.codexConfig`, inclusiv `codexConfig.sidecars.serena`;
- `ctx:status.preflight.ready`, `blockers` si comanda recomandata de reparatie;
- reparatie non-distructiva cu `register-codex-mcp.ps1` si `start-context-stack.ps1 -SkipRefresh`, inclusiv readucerea sidecar-ului Serena cand este offline;
- `-SyncSessions` pentru sincronizare one-shot a istoricului compact inainte de context;
- iesire JSON prin `-Json` pentru teste si tooling.

Prioritati:

- preflight standard pe fiecare task - initial implementat;
- detectare cand JetBrains rescrie config - initial implementat prin `ctx:status.codexConfig`;
- reparatie automata non-distructiva - initial implementat prin `codex:preflight -- -Repair`;
- raportare Serena in preflight si repair sidecar - initial implementat;
- instructiuni scurte in `AGENTS.md` - initial implementat;
- istoric conversatii mai compact - acoperit prin session-sync existent si `-SyncSessions`.

### U9: Deployment Remote Optional

Scop: MCP poate rula pe alta masina, daca proiectul cere.

Prioritati:

- volum persistent remote;
- TLS/reverse proxy;
- autentificare;
- backup off-machine;
- separare intre read tools si write tools;
- politica explicita pentru sidecar-uri MCP, inclusiv daca Serena ramane local-only sau primeste auth separat;
- document de incident response.

## Reguli De Siguranta

- Nu porni scripturi watch in foreground daca lucrezi din IDE.
- Nu sterge `data/chroma` fara backup.
- Nu rula reindex fortat peste lock activ.
- Nu folosi `http://localhost:3000` fara `/mcp` pentru config MCP; root-ul este doar fallback local pentru ACP vechi.
- Nu folosi `http://localhost:9121` fara `/mcp` pentru config Serena.
- Nu expune Serena pe LAN/Internet; sidecar-ul are tools de editare/refactor si ramane local-only.
- Nu pune MCP public pe internet fara autentificare.
- Nu restaura Chroma peste date locale fara `-RestoreChroma` intentionat.
- Nu modifica SQLite/Chroma manual cat timp containerul ruleaza.
