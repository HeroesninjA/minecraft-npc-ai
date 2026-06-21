# API, Modularizare si Addonuri

Actualizat: 2026-06-21

Aceasta categorie acopera API-ul public, modulele Maven, addonurile si scenariile programabile.

## Documente

| Document | Rol |
|---|---|
| `../../documentatie-api.md` | Contract public curent |
| `../../api-events-listeners-triggers.md` | Contract pentru event-uri publice, listener-e si trigger-e peste quest, story, dialog, NPC si context |
| `../../ai-orchestrare-si-mecanici.md` | Contract conceptual pentru `AIOrchestrationService`, tool calls si validare |
| `../../spring-ai-mcp-serviciu-intern.md` | Directie pentru modulul sidecar `ainpc-mcp-service` si integrarea sa cu pluginul Paper |
| `../../strategie-plugin-modular-si-scenarii-programabile.md` | Strategie pentru addonuri si scenarii |
| `../../refactorizare-si-impartire-pe-module.md` | Plan de refactorizare si impartire pe module |
| `../../arhiva/kotlin-migration/README.md` | Istoric pentru seria de conversie Java -> Kotlin |
| `../../kotlin-style-guide.md` | Reguli de stil Kotlin pentru codul AINPC |
| `../../kotlin-interop-api-addonuri.md` | Contract Java interop pentru API si addonuri |
| `../../arhiva/kotlin-migration/README.md` | Istoric pentru trackerul Kotlin |
| `../../kotlin-code-review-checklist.md` | Checklist de review pentru schimbari Kotlin |
| `../../kotlin-testing-strategy.md` | Strategie de testare pentru conversiile Kotlin |
| `../../arhiva/kotlin-migration/README.md` | Index pentru istoricul arhivat al conversiei Kotlin |
| `../../harta-pachetelor-cod-scurta.md` | Harta scurta pentru navigare rapida intre module si pachete |
| `../../harta-pachetelor-cod.md` | Harta doar-documentatie pentru module si pachete de cod |
| `../../harta-clase-cod.md` | Harta doar-documentatie pentru relatiile dintre clasele cheie |
| `../../gui-interfete.md` | Contract pentru framework GUI intern, ecrane, snapshots, actiuni si extensibilitate |
| `../../betonquest-directii-potrivite-pentru-ainpc.md` | Directii pentru runtime de questuri configurabile |
| `../../reducere-marime-jar.md` | Impact asupra buildului si livrarii |

## Zone neacoperite complet

- Registrii runtime pentru scenarii exista initial in core, dar nu sunt inca API public stabil pentru addonuri.
- Evenimentele custom publice pentru quest, story, dialog, NPC si context sunt documentate; lifecycle-ul initial de progression, offer, objective progress si stage change exista in `ainpc-api`, iar restul evenimentelor raman backlog.
- Ghid oficial pentru dezvoltatorii de addonuri.
- Compatibilitate intre versiuni API.
- Contract clar pentru `capabilities` si `dependencies`.
- Template minim de addon.
- Conversia `ainpc-api` la Kotlin este amanata pana exista teste Java de consum si motiv clar.

## Relatii

Vezi ../../relatii-documentatie.md pentru catalogul pe fisiere si lanturile de citire aferente acestei categorii.
Pentru harta de cod, foloseste ../../harta-pachetelor-cod-scurta.md pentru orientare rapida si ../../harta-pachetelor-cod.md pentru detaliu complet.
Pentru relatii intre clase, foloseste ../../harta-clase-cod.md.
