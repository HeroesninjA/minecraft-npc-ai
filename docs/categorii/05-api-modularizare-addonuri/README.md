# API, Modularizare si Addonuri

Actualizat: 2026-05-07

Aceasta categorie acopera API-ul public, modulele Maven, addonurile si scenariile programabile.

## Documente

| Document | Rol |
|---|---|
| `../../documentatie-api.md` | Contract public curent |
| `../../ai-orchestrare-si-mecanici.md` | Contract conceptual pentru `AIOrchestrationService`, tool calls si validare |
| `../../strategie-plugin-modular-si-scenarii-programabile.md` | Strategie pentru addonuri si scenarii |
| `../../refactorizare-si-impartire-pe-module.md` | Plan de refactorizare si impartire pe module |
| `../../rezumat-conversie-java-la-kotlin.md` | Rezumat pentru seria de conversie Java -> Kotlin |
| `../../kotlin-style-guide.md` | Reguli de stil Kotlin pentru codul AINPC |
| `../../kotlin-interop-api-addonuri.md` | Contract Java interop pentru API si addonuri |
| `../../kotlin-migration-tracker.md` | Tracker operational pentru slice-urile Kotlin |
| `../../kotlin-code-review-checklist.md` | Checklist de review pentru schimbari Kotlin |
| `../../kotlin-gradle-activation-plan.md` | Plan exact pentru activarea Kotlin in Gradle |
| `../../kotlin-testing-strategy.md` | Strategie de testare pentru conversiile Kotlin |
| `../../gui-interfete.md` | Contract pentru framework GUI intern, ecrane, snapshots, actiuni si extensibilitate |
| `../../betonquest-directii-potrivite-pentru-ainpc.md` | Directii pentru runtime de questuri configurabile |
| `../../reducere-marime-jar.md` | Impact asupra buildului si livrarii |

## Zone neacoperite complet

- Registrii runtime pentru scenarii exista initial in core, dar nu sunt inca API public stabil pentru addonuri.
- Ghid oficial pentru dezvoltatorii de addonuri.
- Compatibilitate intre versiuni API.
- Contract clar pentru `capabilities` si `dependencies`.
- Template minim de addon.
- Conversia `ainpc-api` la Kotlin este amanata pana exista teste Java de consum si motiv clar.
