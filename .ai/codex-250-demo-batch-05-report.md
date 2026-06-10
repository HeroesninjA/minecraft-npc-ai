# Codex 250 Demo Batch 05 Report

Data: 2026-05-26
Zona: npc / nearest / GUI interaction
Tip: experimental local, fara Paper live

## Task-uri acoperite

| ID | Status | Acoperire |
| --- | --- | --- |
| T071 | PASS | `AINPCTabCompleterTest` confirma ca `/ainpc list` ramane vizibil in suprafata principala. |
| T072 | PASS | `AINPCCommand.handleInfo` accepta explicit `/ainpc info nearest`; tab-complete sugereaza `nearest`. |
| T073 | PARTIAL | Selectorul explicit `nearest` foloseste acelasi flux local de NPC apropiat ca `/ainpc info` fara nume. Validarea tintei live ramane Paper-only. |
| T077 | PARTIAL | Fluxul `info nearest` pastreaza mesajul existent pentru lipsa NPC-urilor apropiate. Testul live ramane Paper-only. |
| T079 | PASS | Tab-complete confirma `/ainpc audit npc` ca optiune demo pentru audit local/live. |
| T080 | PASS | Tab-complete confirma `/ainpc repair duplicates dryrun` ca dry-run disponibil. |
| T081 | PASS | `AINPCProfileDefaultsTest` verifica descriere profil lizibila pentru nume/ocupatie/backstory. |
| T083 | PASS | `NpcInteractionGui` pastreaza actiuni nearest pentru progresie, story si rutina; rutina foloseste acum `nearest` explicit. |
| T085 | PARTIAL | Suprafata `repair duplicates dryrun` este testata; detectia DB/live completa ramane in testele managerului/integrare. |
| T086 | PASS | Fallback-ul `info nearest` este corectat si testat prin descriptor/sursa plus tab-complete. |
| T087 | PASS | Default emotions sunt verificate: neutral, happiness/trust/anticipation rezonabile. |
| T088 | PASS | Relationship bands sunt verificate pentru ENEMY/STRANGER/ACQUAINTANCE/FRIEND/CLOSE_FRIEND. |
| T090 | PASS | Tab-complete confirma `debugdump npc` ca export disponibil. |
| T091 | PASS | Butonul GUI `Rutina nearest` ruleaza acum `ainpc routine status nearest`, nu status generic. |

## Ce a fost schimbat

- Corectat `NpcInteractionGui`: actiunea `Rutina nearest` trimite selectorul `nearest`.
- Corectat `/ainpc info nearest`: handler-ul nu mai cauta un NPC numit literal `nearest`.
- Extins tab-complete pentru `/ainpc info nearest`, `/ainpc audit npc`, `/ainpc repair duplicates dryrun`, `/ainpc debugdump npc`.
- Adaugat `AINPCProfileDefaultsTest` pentru identitate, spawned false, emotii default, relatie initiala si descriere profil cu ancore.

## Test executat

```text
.\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCTabCompleterTest --tests ro.ainpc.commands.PluginCommandDescriptorTest --tests ro.ainpc.npc.AINPCProfileDefaultsTest
BUILD SUCCESSFUL
```

## Limite ramase

- Validarea reala pentru nearest target, entity UUID/databaseId pozitiv, spawn live si restart necesita server Paper.
- Acest batch intareste contractul local si reduce riscul ca demo script-ul sa contina comenzi acceptate in documentatie dar rupte in handler/completari.
