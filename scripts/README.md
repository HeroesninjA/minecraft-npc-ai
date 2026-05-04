# Scripts

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

Cu teste Maven inainte de package:

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

Ce face:

- ruleaza `mvn package -DskipTests`, daca nu folosesti `-SkipBuild`
- optional ruleaza si `mvn test` cu `-RunTests`
- copiaza JAR-ul core si addonul medieval in `plugins/`
- genereaza `ainpc-mapping-smoke-commands.txt` in folderul serverului
- genereaza `ainpc-mapping-smoke-report.txt` cu hash-uri si verificari de baza

Comenzile generate trebuie rulate in consola Paper sau in joc ca OP.

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

- ruleaza `mvn package -DskipTests`, daca nu folosesti `-SkipBuild`
- optional ruleaza si `mvn test` cu `-RunTests`
- copiaza JAR-ul core si addonul medieval in `plugins/`
- genereaza `ainpc-quest-smoke-commands.txt` in folderul serverului
- genereaza `ainpc-quest-smoke-report.txt` cu hash-uri si verificari de baza

Comenzile generate testeaza `/ainpc audit quest`, oferta/acceptarea unui quest cu `nearest`, tracking-ul si finalizarea rapida a Q01 cu iteme date prin `give`.
