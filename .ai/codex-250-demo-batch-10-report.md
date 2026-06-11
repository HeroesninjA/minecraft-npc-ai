# Codex 250 Demo Batch 10 Report

Data: 2026-05-26
Interval: 12 task-uri locale/MIXED
Zona: debugdump output contract

## Task-uri acoperite

| ID | Status | Acoperire |
| --- | --- | --- |
| T200 | MIXED | `DebugDumpService` declara `quest-audit-report.txt`, `loaded-quest-definitions.json`, `player-progressions.json`, `player-quest-progress.json`, `quest-anchor-bindings.json`; exportul live ramane Paper-only. |
| T201 | MIXED | `DebugDumpService` declara `story-states.json` si `story-events.json`; exportul live ramane Paper-only. |
| T203 | PASS | `DebugDumpOutputContractTest` verifica `world-mapping.json` cu `semantic_index`. |
| T204 | PASS | Testul verifica `npc-world-bindings.json` cu `rows`, `row_count`, counters si referinte lipsa. |
| T205 | PASS | Testul verifica contractul `player-progressions.json`: counters, status/template maps si rows. |
| T206 | PASS | Testul verifica contractul `story-events.json`: rows, event type si progression link counters. |
| T207 | PASS | `DebugDumpService` declara `quest-audit-report.txt` in lista de export. |
| T209 | PASS | Folderul/exporturile debugdump sunt indexate prin raport si documentatie. |
| T241 | PASS | Exista raport nou pentru batch-ul curent. |
| T243 | PASS | Raportul noteaza fisierele si contractele validate. |
| T244 | PASS | Raportul noteaza testele rulate. |
| T245 | PASS | Raportul separa clar contractul local de exportul Paper live. |

## Schimbari

- Adaugat `DebugDumpOutputContractTest`, care valideaza contractul static al exporturilor debugdump.
- Testul acopera lista de fisiere exportate, campurile JSON cheie si rutarea continutului sensibil prin `DebugDumpSecrets`.

## Teste

```text
.\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.debug.DebugDumpOutputContractTest --tests ro.ainpc.debug.DebugDumpSecretsTest
BUILD SUCCESSFUL
```

## Limite

- Acest batch nu creeaza un debugdump fizic, pentru ca asta cere plugin incarcat pe Paper.
- Contractele locale reduc riscul de regresii in structura exportului pana la testul live.
