# Codex 250 Demo Paper Live Checklist

Data: 2026-05-26
Scope: MIXED si LIVE gates din T001-T250
Marker: PAPER_LIVE_REQUIRED=true

## Reguli

- Ruleaza doar dupa `.\gradlew.bat test assemble` verde.
- Foloseste server Paper real, pluginul AINPC incarcat si cel putin un player online.
- Nu declara LIVE gate trecut fara output salvat sau debugdump.
- Nu opri fortat serverul; pentru restart foloseste consola Paper `stop`.

## Phase L0 - Local Artifact Gate

| Gate | Command | Expected |
| --- | --- | --- |
| L0.1 | `.\gradlew.bat test assemble` | BUILD SUCCESSFUL |
| L0.2 | verifica JAR core | `ainpc-core-plugin/build/libs` contine artifact |
| L0.3 | verifica JAR addon medieval | `ainpc-scenario-medieval/build/libs` contine artifact |
| L0.4 | verifica JAR API | `ainpc-api/build/libs` contine artifact |

## Phase L1 - Paper Boot Gate

| Gate | Command | Expected |
| --- | --- | --- |
| L1.1 | porneste Paper normal | AINPC enabled |
| L1.2 | `/ainpc demo definition` | definitia demo apare |
| L1.3 | `/ainpc demo status demo_sat` | raport PASS/WARN/FAIL |
| L1.4 | `/ainpc demo next demo_sat` | blocaje si next steps clare |

## Phase L2 - World And Settlement Gate

| Gate | Command | Expected |
| --- | --- | --- |
| L2.1 | `/ainpc world demo create demo_sat` | mapping demo creat sau deja existent |
| L2.2 | `/ainpc world places demo_sat` | places demo listate |
| L2.3 | `/ainpc demo status demo_sat` | places/nodes fara FAIL critic |
| L2.4 | `/ainpc demo smoke demo_sat <player>` | smoke checklist afisat |

## Phase L3 - NPC, Routine, Quest, Story Gate

| Gate | Command | Expected |
| --- | --- | --- |
| L3.1 | `/ainpc info nearest` | NPC apropiat sau mesaj clar ca lipseste |
| L3.2 | `/ainpc routine status nearest` | status rutina sau fallback clar |
| L3.3 | `/ainpc progression stored <player>` | progres citibil |
| L3.4 | `/ainpc story events demo_sat` | events listate sau mesaj gol controlat |
| L3.5 | `/ainpc audit all` | fara critic nerezolvat |

## Phase L4 - Debugdump Gate

| Gate | Command | Expected |
| --- | --- | --- |
| L4.1 | `/ainpc debugdump all` | folder export creat |
| L4.2 | inspecteaza `config-sanitized.yml` | fara API key, token sau password in clar |
| L4.3 | inspecteaza `openai.txt` | fara `sk-` sau bearer token in clar |
| L4.4 | inspecteaza `recent-server-log.txt` | secrete redactate |
| L4.5 | inspecteaza JSON exports | `world-mapping.json`, `npc-world-bindings.json`, `player-progressions.json`, `story-events.json` exista cand datele exista |

## Phase L5 - Restart Gate

| Gate | Command | Expected |
| --- | --- | --- |
| L5.1 | `/ainpc demo restart demo_sat` | checklist restart afisat |
| L5.2 | `/ainpc debugdump all` | debugdump before creat |
| L5.3 | Paper console `stop` | shutdown curat |
| L5.4 | porneste Paper normal | plugin enabled dupa restart |
| L5.5 | `/ainpc world places demo_sat` | mapping persistat |
| L5.6 | `/ainpc list` | NPC persistati |
| L5.7 | `/ainpc world bindings list 20` | bindings persistate |
| L5.8 | `/ainpc demo status demo_sat` | readiness final |
| L5.9 | `/ainpc debugdump all` | debugdump after creat |

## Phase L6 - Release Decision Gate

| Gate | Decision | Expected |
| --- | --- | --- |
| L6.1 | REMOVE | modurile experimentale se sterg inainte de release |
| L6.2 | KEEP_INTERNAL | modurile raman marcate experimental si nepublice |
| L6.3 | PROMOTE | modurile primesc API stabil, docs si teste dedicate |

## Evidence Required

- output `/ainpc demo status demo_sat`
- output `/ainpc audit all`
- path debugdump before
- path debugdump after
- log startup dupa restart
- decizie release: REMOVE, KEEP_INTERNAL sau PROMOTE

Template completabil: `.ai/codex-250-demo-paper-evidence-template.md`.

Validator local:

```text
powershell -ExecutionPolicy Bypass -File .\scripts\validate-demo-paper-evidence.ps1 -EvidenceFile ".ai\codex-250-demo-paper-evidence-template.md" -AllowPending
powershell -ExecutionPolicy Bypass -File .\scripts\validate-demo-paper-evidence.ps1 -EvidenceFile ".ai\paper-evidence-filled.md" -FailOnWarnings
```
