# Scripturi AINPC

Scripturile ajuta la build, pregatire, smoke si agregarea dovezilor. Niciun executor de comenzi nu inlocuieste verificarea output-ului Paper si gate-urile din `docs v2/operations/demo-server-verification.md`.

## Cerinte

- ruleaza din radacina repository-ului;
- configureaza un JDK compatibil prin `JAVA_HOME` sau `PATH`;
- opreste controlat un server existent inainte de inlocuirea JAR-urilor;
- furnizeaza credentialele extern; pentru demo-ul Docker seteaza `RCON_PASSWORD` in mediul procesului;
- nu copia `ainpc-api-<version>.jar` in `plugins/`.

## Pornire demo Docker

```powershell
# RCON_PASSWORD trebuie configurat extern in procesul curent.
./scripts/setup-docker-demo.ps1 -Smoke -PlayerName Hero

# Oprire controlata a containerului demo.
docker compose down
```

`setup-docker-demo.ps1` construieste si copiaza core-ul plus addonul medieval, apoi porneste containerul. Optiunea `-Smoke` apeleaza executorul RCON legacy; raspunsurile necesita validare semantica.

## Teste locale

```powershell
./scripts/run-tests.ps1
./scripts/run-tests.ps1 -Module debug
./scripts/run-tests.ps1 -Test "ro.ainpc.gui.GuiKeyTest"
./scripts/run-tests.ps1 -Count
./scripts/run-tests.ps1 -List
```

Categoriile sunt `all`, `gui`, `quest`, `progression`, `story`, `mapping`, `npc`, `command`, `debug`, `spawn`, `listener`, `economy`, `ai`, `routine`, `topology`, `utils` si `database`.

## Audit RCON automatizabil

```powershell
$env:RCON_PASSWORD = "<secret>"
./scripts/ainpc-audit-rcon.ps1 -Mode all -Profile strict

# Replay/fixture local, fara conexiune si fara parola.
./scripts/ainpc-audit-rcon.ps1 -Mode quest -Profile full -ResponseFile ./.ai/quest-audit.json
```

Wrapperul valideaza un singur document `ainpc-audit-report` schema v1 si reconciliaza cererea, planul, sumarul, sectiunile, severitatile, verdictul si obiectele DB/spawn. Exit code-urile sunt `0=PASS`, `1=WARN`, `2=FAIL` si `3=transport/autentificare/contract invalid`; validarea nu demonstreaza autenticitatea datelor furnizate de server sau din fixture.

## Rolul scripturilor

| Script | Rol | Nu demonstreaza singur |
|---|---|---|
| `build-local.ps1` | build Gradle local | compatibilitatea Paper sau deploy-ul |
| `run-tests.ps1` | ruleaza suite/categorii de teste | smoke runtime pe server |
| `setup-docker-demo.ps1` | pregateste serverul Docker demo | gate-urile functionale |
| `deploy-demo.ps1` | copiaza core-ul si addonul pe un server existent | stop/restart sigur ori rollback complet |
| `smoke-paper-mapping.ps1` | smoke specializat mapping | toate domeniile demo |
| `smoke-paper-quests.ps1` | smoke specializat quest/RCON | interactiunea completa a playerului |
| `ainpc-audit-rcon.ps1` | audit JSON RCON cu contract semantic si exit code real | autenticitatea ori corectitudinea datelor runtime |
| `smoke-demo-complet.ps1` | executor legacy de comenzi/preparare | PASS semantic; marcheaza comenzile `EXECUTED_UNVERIFIED` |
| `test-demo.ps1` | checklist interactiv/preview | verificare automata |
| `validate-demo-paper-evidence.ps1` | valideaza structura fisierului de dovezi | adevarul output-ului introdus |
| `api-abi-check.ps1` | compara ABI-ul JVM public al `ainpc-api` cu baseline-ul comis | compatibilitatea comportamentala ori alegerea nivelului SemVer |
| `release-api-addon-freeze.ps1` | verifica artefactele API/addon, lifecycle-ul declarat si baseline-ul ABI | startup-ul Paper ori comportamentul addonului pe server |
| `release-report.ps1` | agrega artefacte si stari declarate | producerea dovezilor lipsa |
| `release-backup-restore-check.ps1` | arhiva, manifest si extractie de control | restore Paper/DB complet |

## Verificare ABI API

```powershell
./gradlew.bat :ainpc-api:verifyApiAbi

# Numai dupa un bump intentionat al apiVersion si revizuirea schimbarii.
./gradlew.bat :ainpc-api:updateApiAbiBaseline
```

Politica completa este in `docs v2/reference/api-versioning-and-abi.md`. Task-ul `:ainpc-api:check` depinde de verificarea ABI, iar freeze-ul de release transforma mismatch-ul in warning blocant cand este rulat cu `-FailOnWarnings`.

## Demo Paper Evidence Validator

Parametrul corect este `-EvidenceFile`, nu un director:

```powershell
./scripts/validate-demo-paper-evidence.ps1 `
  -EvidenceFile ".ai/codex-250-demo-paper-evidence-template.md" `
  -AllowPending `
  -FailOnWarnings `
  -JsonOutFile ".ai/demo-evidence-validation.json"
```

El verifica tokenurile obligatorii, markerii pending, decizia si cateva forme de secrete. Nu executa serverul si nu valideaza semantic comenzile copiate in raport.

## Deploy existent

```powershell
./scripts/deploy-demo.ps1 -ServerDir "C:/Minecraft/paper-test"
```

Scriptul ramane specific mediului: nu opreste Paper, nu gestioneaza restartul si nu valideaza raspunsurile runtime. Foloseste procedura completa din `docs v2/operations/server-admin-runbook.md` pentru release.
