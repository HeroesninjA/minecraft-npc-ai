# Codex 250 Demo Batch 01

Data: 2026-05-26
Status: PASS

## Task-uri Selectate

Batch-ul 01 implementeaza partea de workflow Codex si raportare pentru backlog-ul experimental:

- T231 | codex | hygiene | marcheaza experimental in final
- T232 | codex | hygiene | nu promova experimental ca API
- T233 | codex | hygiene | ruleaza `clean test assemble` dupa bulk patch
- T234 | codex | hygiene | pastreaza task-uri in `.ai`
- T235 | codex | hygiene | separa docs de cod runtime
- T241 | codex | report | genereaza raport dupa fiecare experiment
- T242 | codex | report | noteaza warnings neblocante
- T243 | codex | report | noteaza fisiere modificate
- T244 | codex | report | noteaza comenzi rulate
- T245 | codex | report | noteaza ce nu a fost testat pe Paper
- T246 | codex | planning | grupeaza task-uri pe P0/P1/P2
- T247 | codex | planning | transforma task-urile in milestones
- T248 | codex | planning | identifica blocaje reale dupa demo Paper
- T249 | codex | planning | arhiveaza experimentele nereusite
- T250 | codex | release | freeze API inainte de publicare

## Implementare

Adaugat test automat:

- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/commands/CodexDemoBacklogTest.kt`

Testul valideaza ca `.ai/codex-250-demo-task-backlog.md` ramane usor de interpretat de Codex:

- exact 250 task-uri;
- ID-uri unice si continue de la `T001` la `T250`;
- format stabil cu 6 campuri separate prin `|`;
- prioritati valide `P0`, `P1`, `P2`;
- campuri nenule pentru area/type/scope/acceptance;
- acoperire pentru ariile majore: build, config, demo-command, world, settlement, npc, routine, dialogue, quest, progression, story, audit, debugdump, restart, codex.

## Comenzi Rulate

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.CodexDemoBacklogTest
```

Rezultat: PASS

## Paper

Nu s-a rulat test Paper real in acest batch. Task-urile alese sunt locale, pentru structura backlog-ului si workflow Codex.

## Urmatorul Batch Recomandat

Urmatorul batch ar trebui sa aleaga 10-20 task-uri P0/P1 din una dintre zonele runtime:

- `demo-command` pentru acoperire comenzi;
- `world` pentru mapping demo;
- `settlement` pentru spawn/bindings;
- `restart` pentru persistenta.
