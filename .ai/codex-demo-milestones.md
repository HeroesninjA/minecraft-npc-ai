# Codex Demo Milestones

Data: 2026-05-26
Sursa: `.ai/codex-250-demo-task-backlog.md`

## Reguli

- P0 este gate: nu se declara demo functional fara aceste verificari.
- P1 este hardening: se face inainte de demo repetabil sau release intern.
- P2 este mentenanta/documentatie/teste auxiliare.
- Task-urile LIVE cer Paper real; task-urile LOCAL pot fi inchise prin teste sau documentatie.

## Milestone M0 - Local Baseline

Prioritate: P0
Scope: build, config, demo-command, codex hygiene
Task-uri cheie: T001, T006, T011, T012, T013, T117, T118, T202, T231, T232, T233
Gate: build verde, demo command read-only, zero secrete in debug paths, rapoarte marcate experimental.

## Milestone M1 - World And Settlement

Prioritate: P0/P1
Scope: world, settlement, NPC bindings
Task-uri cheie: T031, T032, T033, T034, T035, T036, T037, T038, T039, T051, T052, T064, T065
Gate: `demo_sat` exista, mapping-ul are places/nodes, settlement plan/spawn functioneaza, bindings persista.

## Milestone M2 - NPC And Routine

Prioritate: P0/P1
Scope: npc, routine, nearest, GUI
Task-uri cheie: T071, T072, T073, T076, T079, T091, T092, T093, T094, T095, T096, T099
Gate: NPC-urile sunt vizibile, `info nearest` si `routine status nearest` dau status clar, GUI nu ascunde starea reala.

## Milestone M3 - Dialogue, Quest, Progression

Prioritate: P0/P1
Scope: dialogue, quest, progression
Task-uri cheie: T111, T112, T113, T117, T118, T131, T132, T133, T134, T135, T145, T151, T152, T168
Gate: dialog cu fallback, quest accept/status, progression definitions/stored, persistenta dupa restart pentru progres.

## Milestone M4 - Story, Audit, Debugdump

Prioritate: P0/P1
Scope: story, audit, debugdump
Task-uri cheie: T171, T172, T174, T179, T181, T191, T192, T197, T198, T199, T200, T201, T202
Gate: story context/events inspectabile, audit all/db fara critic, debugdump all fara secrete.

## Milestone M5 - Restart And Release Discipline

Prioritate: P0/P1
Scope: restart, release, cleanup
Task-uri cheie: T211, T212, T213, T214, T215, T216, T217, T218, T219, T220, T222, T229, T230, T250
Gate: doua rulari consecutive trec inainte/dupa restart; API freeze inainte de publicare.

## Milestone M6 - Cleanup After Experiments

Prioritate: P1/P2
Scope: codex cleanup, docs, test debt
Task-uri cheie: T236, T237, T238, T239, T240, T241, T242, T243, T244, T245, T246, T247, T248, T249
Gate: modurile experimentale inutile sunt sterse sau compactate, task packs mari sunt mutate daca raman, rapoartele nereusite sunt arhivate.
