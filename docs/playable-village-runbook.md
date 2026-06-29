# Playable Village Runbook

Actualizat: 2026-06-28

## Scop

Acest document descrie pasii pentru a aduce un sat demo functional pe un server Paper local (Docker) sau VPS.

## Cerinte

- Docker Desktop (local) sau Java 21+ (VPS)
- Porturi libere: 25565 (Minecraft), 25575 (RCON)
- `.env` configurat (copie din `.env.example`)

## Setup Rapid (automat)

```powershell
# 1. Porneste serverul Docker
docker compose up -d
# asteapta ~30s pentru initializare

# 2. Ruleaza scriptul consolidat
.\scripts\playable-village-setup.ps1 -ServerDir .\paper-data -RegionId demo_sat -Population 8
```

## Setup Manual (pas cu pas)

### 1. Build
```powershell
.\gradlew.bat clean build
```

### 2. Deploy
```powershell
Copy-Item ainpc-core-plugin\build\libs\ainpc-core-plugin-*.jar paper-data\plugins\ainpc-core-plugin.jar
Copy-Item ainpc-scenario-medieval\build\libs\ainpc-scenario-medieval-*.jar paper-data\plugins\ainpc-scenario-medieval.jar
```

### 3. Porneste/Foloseste serverul
```powershell
docker compose up -d
# conecteaza-te cu RCON sau direct in joc
```

### 4. Creaza satul demo
```
/ainpc world demo create demo_sat
/ainpc world save
/ainpc audit world
```

### 5. Populeaza cu NPC-uri
```
/ainpc population plan demo_sat 8
/ainpc population inspect demo_sat
/ainpc world settlement plan demo_sat 6
/ainpc world settlement spawn demo_sat dry-run
/ainpc world settlement spawn demo_sat
/ainpc world save
/ainpc audit all
```

### 6. Verifica functionalitatea
```
# NPC-uri
/ainpc list
/ainpc info nearest
/ainpc routine status nearest

# Questuri
/quest log
/quest gui

# Progression
/progression stored

# Story
/ainpc story region
/ainpc story place

# Debug
/ainpc health
/ainpc audit quest

# MCP (daca sidecar-ul pornit)
/ainpc debugdump mcp
```

## Comenzi Utile RCON

```powershell
# Conectare (daca ai mcrcon instalat)
mcrcon -H 127.0.0.1 -P 25575 -p demo "/ainpc health"

# Fara mcrcon, foloseste docker
docker exec ainpc-demo rcon-cli "/ainpc health"
```

## Comanda Rapida Unica

Pentru a rula tot setup-ul (build-exclud) fara script:

```
/ainpc world demo create demo_sat
/ainpc world save
/ainpc population plan demo_sat 8
/ainpc population inspect demo_sat
/ainpc world settlement plan demo_sat 6
/ainpc world settlement spawn demo_sat
/ainpc world save
/ainpc audit all
```

## Smoke Test-uri

Dupa setup, ruleaza smoke test-urile pentru verificare:

```powershell
.\scripts\smoke-paper-mapping.ps1 -ServerDir .\paper-data -RegionId demo_sat
.\scripts\smoke-paper-quests.ps1 -ServerDir .\paper-data -PlayerName <nume_jucator>
```

## Probleme Comune

| Problema | Solutie |
|---|---|
| Containerul nu porneste | Verifica `.env`, port 25565 liber |
| Pluginul nu se incarca | Verifica versiunea Paper in `docker-compose.yml` |
| NPC-urile nu apar | Ruleaza `/ainpc world save` dupa spawn |
| Questurile nu apar | Verifica `/ainpc audit quest` |
| MCP nu raspunde | Porneste sidecar-ul: `java -jar ainpc-mcp-service.jar` |
| Mapping-ul e gol | Foloseste teren plat (`LEVEL_TYPE: FLAT` in `.env`) |

## Referinte

- `docker-compose.yml` - configuratie server Docker
- `.env.example` - template config
- `scripts/playable-village-setup.ps1` - script automat
- `scripts/smoke-paper-mapping.ps1` - smoke test mapping
- `scripts/smoke-paper-quests.ps1` - smoke test questuri
