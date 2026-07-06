# Bot de Test Automat - Mineflayer

## Arhitectura

```
ainpc-bot/
├── package.json
├── bot-framework/
│   ├── client.js          # Conexiune Mineflayer + auto-reconnect
│   ├── commander.js       # Executa comenzi, citeste raspunsuri din chat
│   ├── navigator.js       # Pathfinding (mineflayer-pathfinder)
│   └── reporter.js        # Pass/fail, timpi, raport JSON
├── quests/
│   ├── registry.js
│   ├── Q01-beginer.js
│   ├── Q02-provizii.js
│   └── ...
├── tests/
│   ├── smoke.js
│   ├── quest-flow.js
│   └── npc-dialog.js
├── config.js
└── run.js
```

### Componente

| Fisier | Rol |
|--------|-----|
| `client.js` | Conexiune Mineflayer, auto-reconnect, event handlers |
| `commander.js` | `bot.chat()` + citire raspunsuri din chat cu timeout |
| `navigator.js` | `pathfinder.goto(x, y, z)` cu toleranta |
| `reporter.js` | Acumuleaza rezultate, genereaza raport JSON la final |
| `registry.js` | Incarca toate quest-urile din `quests/` |
| `config.js` | IP, port, timeout-uri, quest-uri active/inactive |

## Flux test quest

```
run.js → tests/smoke.js → tests/quest-flow.js
                                │
                                ├── Q01-beginer.js
                                │    1. bot.chat("/quest accept Q01")
                                │    2. bot.chat("/quest track Q01")
                                │    3. navigator.goto(171, -60, 148)  // cufar in forja
                                │    4. bot.chat("/quest log")
                                │    5. reporter.assert(progres.contine("cufar"))
                                │    6. navigator.goto(176, 64, 272)  // inapoi la NPC
                                │    7. bot.chat("/quest return")
                                │    8. reporter.pass("Q01 complet")
                                │
                                ├── Q02-provizii.js
                                └── ...
```

## Comunicare cu AINPC

GUI-urile nu sunt necesare — tot ce face un jucator prin `/npc gui` se poate face prin comenzi text.

| Actiune | Comanda |
|---------|---------|
| Accepta quest | `bot.chat("/quest accept Q01")` |
| Track quest | `bot.chat("/quest track Q01")` |
| Status quest | `bot.chat("/quest log")` + citeste raspunsul |
| Debug AINPC | `bot.chat("/ainpc debug status")` |
| Dialog NPC | Se apropie de coordonate NPC, scrie in chat; NPC-ul raspunde tot in chat |
| TPS / performanta | `bot.chat("/spark tps")` |

## Utilizare

```bash
# Rulare suite
node run.js --suite=quest-flow    # doar quest-uri
node run.js --suite=smoke         # doar smoke test
node run.js --suite=all           # tot

# Rulare questuri specifice
node run.js --quest=Q01 --quest=Q03

# Raport
# Reporterul scrie ainpc-bot/reports/<timestamp>.json
```

## Integrare deploy

In `deploy-ainpc.ps1`, dupa deploy/verificare:

```powershell
Write-Host "[7/7] Smoke test automat..." -ForegroundColor Yellow
$result = & node ainpc-bot/run.js --suite=smoke 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "  SMOKE TEST FAILED!" -ForegroundColor Red
    exit 1
}
Write-Host "  Smoke test OK" -ForegroundColor Green
```

## Faze de implementare

| Faza | Cand | Ce contine |
|------|------|-----------|
| 1 | Acum (setup proiect) | `package.json`, `client.js`, `commander.js`, `reporter.js`, `config.js`, `tests/smoke.js` — smoke test de baza (server UP, NPC-uri exista, OpenAI conectat) |
| 2 | Dupa ce satul e mapat si questurile sunt clare | `navigator.js`, quest-urile unul cate unul in `quests/` |
| 3 | Cand ai questuri stabile | Integrare in `deploy-ainpc.ps1` — deployul pica daca smoke testul pica |

## Nota

Botul NU poate interactiona cu GUI-urile custom AINPC (`/npc gui`). Testarea GUI-urilor ramane manuala sau prin RCON cu comenzi de genul `/npc gui admin`. Toate celelalte functionalitati (quest, dialog, tracking, debug) sunt testabile complet prin comenzi text, pe care botul le poate executa.
