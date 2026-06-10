# Codex 250 Demo Batch 06 Report

Data: 2026-05-26
Zona: routine / nearest / GUI
Tip: experimental local, fara Paper live

## Task-uri acoperite

| ID | Status | Acoperire |
| --- | --- | --- |
| T091 | PASS | `/ainpc routine status nearest` este acceptat explicit de handler si de tab-complete. |
| T092 | PASS | `/ainpc routine tick` ramane in tab-complete si in documentatia demo ca evaluare manuala cu sumar. |
| T093 | PASS | `RoutineEngineTest.nightSendsNpcHomeToSleep` si fallback-urile verifica folosirea home anchor. |
| T094 | PASS | `RoutineEngineTest.workHoursSendNpcToWorkAnchor` verifica work anchor si stare de lucru. |
| T095 | PASS | `RoutineEngineTest.lowSocialNeedUsesSocialAnchorOutsideNight` verifica social anchor. |
| T096 | PASS | `missingAllAnchorsFallsBackToIdleWithClearReason` verifica fallback clar fara ancore/binding util. |
| T097 | PARTIAL | `nearest` explicit nu mai este interpretat ca nume NPC; mesajul live fara NPC apropiat ramane Paper-only. |
| T098 | PARTIAL | Documentatia noteaza cooldown/config pentru miscare; validarea miscarii live ramane Paper-only. |
| T099 | PASS | `RoutineGui` foloseste `ainpc routine status nearest` pentru butonul Status nearest. |
| T100 | PASS | Documentatia clarifica faptul ca `routine tick/status` raporteaza sumar admin, nu actionbar spam. |

## Ce a fost schimbat

- `AINPCCommand.handleRoutineStatus` accepta selectorul `nearest`.
- `AINPCTabCompleter` sugereaza `nearest` pentru `routine status`.
- `RoutineGui` ruleaza `ainpc routine status nearest`.
- `RoutineEngineTest` acopera fallback-uri pentru lipsa ancorelor, timp negativ, dupa-amiaza fara work, seara fara social si prioritizarea sigurantei.
- Documentatia demo D4 a fost actualizata cu contractul MVP pentru rutina.

## Test executat

```text
.\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.routine.RoutineEngineTest --tests ro.ainpc.commands.AINPCTabCompleterTest --tests ro.ainpc.commands.PluginCommandDescriptorTest
BUILD SUCCESSFUL
```

## Limite ramase

- Confirmarea distantei reale pentru nearest, pathfinder Paper, cooldown-ul de miscare si lipsa spam-ului vizual necesita server Paper live.
- Batch-ul intareste contractul local si documentatia pentru fluxul demo, nu simuleaza entitati Bukkit live.
