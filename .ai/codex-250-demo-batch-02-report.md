# Codex 250 Demo Batch 02

Data: 2026-05-26
Status: PASS

## Task-uri Selectate

Batch-ul 02 implementeaza verificari locale pentru zona `demo-command`:

- T011 | demo-command | verify | `/ainpc demo definition`
- T012 | demo-command | verify | `/ainpc demo status demo_sat`
- T013 | demo-command | verify | `/ainpc demo next demo_sat`
- T014 | demo-command | verify | `/ainpc demo script demo_sat <player>`
- T015 | demo-command | verify | `/ainpc demo phases demo_sat <player>`
- T016 | demo-command | verify | `/ainpc demo evidence demo_sat <player>`
- T017 | demo-command | verify | `/ainpc demo runbook demo_sat <player>`
- T018 | demo-command | verify | `/ainpc demo smoke demo_sat <player>`
- T019 | demo-command | verify | `/ainpc demo summary demo_sat <player>`
- T020 | demo-command | verify | `/ainpc demo commands demo_sat <player>`
- T027 | demo-command | harden | definition nu primeste regionId in tab-completion
- T029 | demo-command | test | tab-completion demo aliases

## Implementare

Consolidat testele existente:

- `AINPCTabCompleterTest` acopera aliasurile stable si experimentale pentru `/ainpc demo`.
- `PluginCommandDescriptorTest` verifica linia principala de help din `AINPCCommand.java`, ca sa ramana sincronizata cu familia de comenzi demo.

## Comenzi Rulate

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCTabCompleterTest --tests ro.ainpc.commands.PluginCommandDescriptorTest
.\gradlew.bat test assemble
```

## Paper

Nu s-a rulat server Paper real in acest batch. Verificarea este locala: tab-completion, help text si build/test.

## Urmatorul Batch Recomandat

Urmatorul batch poate acoperi 10-20 task-uri din `world` sau `settlement`, preferabil prin teste unitare pe serviciile de mapping/planner deja existente.
