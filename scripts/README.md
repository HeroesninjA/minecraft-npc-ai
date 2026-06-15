# Scripturi Demo AINPC

## Pornire rapida

```powershell
# 1. Docker (recomandat) — porneste un server Paper curat
.\scripts\setup-docker-demo.ps1
# Apoi conecteaza-te in Minecraft la localhost:25565

# 2. Smoke test complet cu RCON (automat, pe Docker)
.\scripts\smoke-demo-complet.ps1 -Rcon -RconHost localhost -RconPort 25575 -RconPass demo

# 3. Deploy pe un server Paper existent
.\scripts\deploy-demo.ps1 -ServerDir "C:\Minecraft\paper-test"

# 4. Testare interactiva ghidata
.\scripts\test-demo.ps1 -Interactive

# 5. Oprire Docker
docker compose down
```

## Comenzi

| Script | Comanda | Ce face |
|---|---|---|
| Docker | `setup-docker-demo.ps1` | Build + deploy JAR-uri + porneste container Paper |
| Smoke | `smoke-demo-complet.ps1 -Rcon` | Ruleaza TOATE comenzile demo automate prin RCON |
| Deploy | `deploy-demo.ps1` | Copiaza JAR-urile pe un server Paper existent |
| Test | `test-demo.ps1 -Interactive` | Ghid interactiv pas-cu-pas |
| RCON | `rcon-client.ps1` | Conectare RCON la server Paper |

## Smoke test automat

```powershell
# Build, deploy, conectare RCON, executie comenzi, raport
.\scripts\smoke-demo-complet.ps1 -Rcon -RegionId demo_sat -PlayerName Hero
```

Docker desktop trebuie sa ruleze pentru varianta Docker. Pentru varianta locala, ai nevoie de un server Paper pornit.
