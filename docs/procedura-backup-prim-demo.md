# Procedura Backup pentru Primul Demo AINPC

Actualizat: 2026-06-15

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).

## Scop

Backup-ul complet al datelor AINPC inainte de orice modificare pe serverul Paper.

## Ce se backup-uieste

| Cale | Descriere |
|------|-----------|
| `ServerDir/plugins/AINPC/` | Config, quests, date, mapping |
| `ServerDir/plugins/AINPC/ainpc_data.db` | Baza de date SQLite |
| `ServerDir/plugins/AINPC/config.yml` | Configuratie |
| `ServerDir/plugins/AINPC/quests.yml` | Definitii quest-uri |
| `ServerDir/plugins/*.jar` | JAR-urile plugin-ului |
| `ServerDir/world/` | Lumea Minecraft (optional) |

## Script PowerShell

```powershell
$ServerDir = "C:\Minecraft\paper-test"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupDir = "$ServerDir/backup-$timestamp"

New-Item -ItemType Directory -Path $backupDir -Force

# Config si date
Copy-Item -Recurse "$ServerDir/plugins/AINPC" "$backupDir/plugins-AINPC" -ErrorAction SilentlyContinue

# JAR-uri
Copy-Item "$ServerDir/plugins/ainpc-core-plugin-*.jar" "$backupDir/" -ErrorAction SilentlyContinue
Copy-Item "$ServerDir/plugins/ainpc-scenario-medieval-*.jar" "$backupDir/" -ErrorAction SilentlyContinue
Copy-Item "$ServerDir/plugins/ainpc-api-*.jar" "$backupDir/" -ErrorAction SilentlyContinue

Write-Host "Backup creat in: $backupDir"
```

## Cand se face backup

- Inainte de primul start al serverului cu plugin-ul
- Inainte de orice comanda `/ainpc world save`
- Inainte de orice modificare manuala a configuratiei
- Inainte de restartul pentru verificare persistenta (D8)

## Verificare backup

```powershell
Test-Path "$backupDir/plugins-AINPC/ainpc_data.db"
Test-Path "$backupDir/plugins-AINPC/config.yml"
Get-ChildItem "$backupDir/*.jar" | Select-Object Name, Length
```
