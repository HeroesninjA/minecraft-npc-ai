# Codex Demo Report Index

Data: 2026-05-26
Scop: index compact pentru rapoartele experimentale Prim Demo

## Reguli De Citire

- `LOCAL` inseamna inchis sau verificabil prin teste/documentatie locale.
- `MIXED` inseamna acoperire locala partiala, gate final pe Paper.
- `LIVE` inseamna nu se inchide fara server Paper, jucator, restart sau export real.

## Rapoarte

| Report | Range | Focus | Validation |
| --- | --- | --- | --- |
| `.ai/codex-250-demo-batch-01-report.md` | T231-T235, T241-T250 | backlog/report hygiene | backlog tests |
| `.ai/codex-250-demo-batch-02-report.md` | T011-T020, T027, T029 | demo command local validation | command tests |
| `.ai/codex-250-demo-batch-03-report.md` | T031-T043, T045, T048-T049 | world demo mapping | world tests |
| `.ai/codex-250-demo-batch-04-report.md` | T051-T058, T067, T069-T070 | settlement planner | planner tests |
| `.ai/codex-250-demo-batch-05-report.md` | T071-T090 partial | NPC/nearest/GUI | command/profile tests |
| `.ai/codex-250-demo-batch-06-report.md` | T091-T100 | routine/nearest/GUI | routine tests |
| `.ai/codex-250-demo-batch-07-99-task-report.md` | T101-T199 | 99-task tracking test | backlog report test |
| `.ai/codex-250-demo-batch-08-safe-session-report.md` | T200-T250 | final safe-session boundary | backlog report test |
| `.ai/codex-250-demo-batch-09-report.md` | 16 local/MIXED tasks | debugdump safety + Codex hygiene | secret safety test |
| `.ai/codex-250-demo-batch-10-report.md` | 12 local/MIXED tasks | debugdump output contract | debugdump contract test |
| `.ai/codex-demo-milestones.md` | M0-M6 | prioritized demo execution plan | backlog milestone test |
| `.ai/codex-paper-restart-runbook.md` | T211-T230 | Paper restart gate | restart runbook test |
| `.ai/codex-release-freeze-checklist.md` | T231-T250 | release freeze discipline | release checklist test |
| `.ai/codex-session-safe-capacity.md` | session policy | safe limits | backlog report test |
| `.ai/codex-250-demo-final-completion-report.md` | T001-T250 | final closure + validation evidence | backlog final report test |
| `.ai/codex-250-demo-paper-live-checklist.md` | MIXED/LIVE gates | Paper live verification | backlog live checklist test |
| `.ai/codex-250-demo-paper-evidence-template.md` | live run evidence | fill-in evidence template | backlog evidence template test |
| `.ai/codex-fast-250-experimental-backlog.md` | F001-F250 | fast programming experimental backlog | backlog fast-shape test |
| `.ai/codex-fast-250-experimental-guide.md` | F001-F250 rules | explicit fast programming guide | backlog fast-doc test |

## Concluzie Curenta

Backlog-ul T001-T250 este impartit in rapoarte interpretibile si are raport final de inchidere locala. Rapoartele mari sunt pentru tracking si triage, nu pentru a declara LIVE gates finalizate fara server Paper.
