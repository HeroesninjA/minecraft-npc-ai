# AINPC Plugin

![Build Status](https://github.com/HeroesninjA/test/actions/workflows/build.yml/badge.svg)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Plugin Minecraft Paper pentru NPC-uri AI cu poveste, emotii, amintiri si dialog realist.

## Descriere

AINPC este un plugin Paper 1.21 care adauga NPC-uri inteligente in lumea Minecraft. NPC-urile au:
- **Personalitate** si **emotii** care influenteaza interactiunile
- **Rutine zilnice** bazate pe ancore (casa, munca, social)
- **Quest-uri** si **progression** cu mecanici variate (contracte, bounty-uri, ritualuri)
- **Memorie** - NPC-urile isi amintesc interactiunile cu jucatorii
- **Poveste** generata dinamic

## Comenzi principale

| Comanda | Descriere |
|---------|-----------|
| `/ainpc` | Comanda principala pentru gestionarea NPC-urilor AI |
| `/quest` | Quest log si comenzi rapide pentru quest-uri |
| `/progression` | Log si comenzi pentru mecanici de progres |
| `/npcquest` | Comanda rapida pentru quest-urile NPC |

## Structura proiectului

```
ainpc-core-plugin/        # Plugin principal
ainpc-api/                # API public pentru addon-uri
ainpc-scenario-medieval/  # Scenariu medieval
```

## Build

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-local.ps1
```

## AI tooling

- Codex foloseste `.codex/config.toml` pentru MCP-ul local.
- OpenCode foloseste `opencode.json` in root-ul proiectului; `opencode.jsonc` ramane varianta comentata/editabila.
- OpenCode are si o referinta locala `docs/` pentru notele de arhitectura si stack-ul MCP.
- Drift-ul intre OpenCode si Codex se verifica cu `.\scripts\check-opencode-config-drift.ps1`.
- Sincronizarea dintre `opencode.json` si `opencode.jsonc` se verifica cu `.\scripts\check-opencode-config-sync.ps1`.
- Verificarea completa OpenCode se ruleaza cu `.\scripts\check-opencode-config.ps1`.
- Trackerul pentru gap-urile MCP runtime se ruleaza cu `.\scripts\mcp-runtime-gap-status.ps1`.
- Ambele expun acelasi stack MCP local: `ainpc-project-memory`, `serena` si `context7`.
- Pentru taskuri de API sau librarii externe, OpenCode si Codex pot folosi automat `context7`.

## Testare

```powershell
# Toate testele unitare
.\scripts\run-tests.ps1

# Doar GUI
.\scripts\run-tests.ps1 -Module gui

# Test singular
.\scripts\run-tests.ps1 -Test "ro.ainpc.gui.GuiKeyTest"

# Statistici teste per categorie
.\scripts\run-tests.ps1 -Count
```

Module disponibile: `gui`, `quest`, `progression`, `story`, `mapping`, `npc`, `command`, `debug`, `spawn`, `listener`, `economy`, `ai`

## Tehnologii

- **Kotlin** 2.3.21
- **Paper API** 1.21
- **SQLite** / **MySQL** (prin HikariCP)
- **Gson** pentru serializare JSON

## Statistici

- 0 fisiere Java
- ~1060 teste unitare (1048 trec, 3 pre-existente AI)
- 100% Kotlin
