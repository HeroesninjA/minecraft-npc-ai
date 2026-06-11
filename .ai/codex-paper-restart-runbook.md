# Codex Paper Restart Runbook

Data: 2026-05-26
Scope: T211-T230
Marker: FORBID_FORCED_KILL=true

## Reguli

- Nu declara restart gate trecut fara server Paper real.
- Nu folosi kill fortat, Task Manager end task sau oprire dura a procesului.
- Opreste Paper prin comanda normala `stop` din consola sau mecanismul controlat al serverului.
- Pastreaza debugdump before/after pentru comparatie.

## Phase R0 - Precheck Before Stop

| ID | Command | Expected |
| --- | --- | --- |
| T211 | `/ainpc demo restart demo_sat` | checklist afisat |
| T212 | `/ainpc audit all` | fara critic |
| T213 | `/ainpc debugdump all` | export before creat |
| T214 | `/ainpc world save` | save reusit |

## Phase R1 - Controlled Stop And Start

| ID | Action | Expected |
| --- | --- | --- |
| T215 | Paper console `stop` | shutdown curat |
| T216 | porneste Paper normal | AINPC si addon enabled |
| T229 | evita kill fortat | procedura clara, fara corruptie intentionata |

## Phase R2 - Post-Restart Verification

| ID | Command | Expected |
| --- | --- | --- |
| T217 | `/ainpc world places demo_sat` | mapping persistat |
| T218 | `/ainpc list` | NPC persistati |
| T219 | `/ainpc world bindings list 20` | bindings persistate |
| T220 | `/ainpc progression stored <player>` | progres persistat |
| T221 | `/ainpc story events demo_sat` | story persistat |
| T222 | `/ainpc demo status demo_sat` | readiness final |

## Phase R3 - Recovery Checks

| ID | Symptom | First action |
| --- | --- | --- |
| T223 | NPC lipsa dupa restart | ruleaza `/ainpc audit npc`, apoi inspecteaza debugdump npc before/after |
| T224 | bindings lipsa dupa restart | ruleaza `/ainpc repair npc-bindings dryrun` si `/ainpc repair mapping-metadata dryrun` |
| T225 | progress lipsa dupa restart | ruleaza `/ainpc progression stored <player>` si inspecteaza DB/debugdump progression |

## Phase R4 - Release Discipline

| ID | Requirement | Expected |
| --- | --- | --- |
| T226 | restart gate documentat | acest runbook exista si este indexat |
| T227 | compara debugdump before/after | diferente notate |
| T228 | persistence service tests | teste locale verzi |
| T230 | doua rulari consecutive | ambele restart gates verzi |

## Evidence Folder

Salveaza path-urile catre:

- debugdump before
- debugdump after
- log startup dupa restart
- output `/ainpc demo status demo_sat`
- output `/ainpc audit all`
