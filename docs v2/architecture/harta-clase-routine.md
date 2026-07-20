# Harta claselor pentru rutine

Status: harta verificata in cod.
Actualizat: 2026-07-15.

## Orchestrare

- `SchedulerCoordinator` porneste taskul numai cand flagurile globale si locale permit;
- `RoutineCoordinator` adauga sincronizare sociala si servicii optionale;
- `RoutineService` evalueaza batch-ul, aplica starea si mutarea;
- `RoutineTickSummary` raporteaza evaluated, moved si motivele principale de skip.

## Decizie

- `RoutineEngine` produce `RoutineAssignment`;
- `RoutineProfileResolver` alege `BehaviorProfile` dupa ocupatie sau fallback;
- `RoutineTimeResolver` aplica offsetul stabil si biasul de grup;
- `RoutineDecisionResolver` rezolva schedule entry, slot, ancora, activitate, goal si stare;
- `RoutineSlot` separa `HOME`, `WORK`, `SOCIAL` si `IDLE`.

## Dependente si limite

- ancorele vin din `AINPC`, hidratate inclusiv din `npc_world_bindings`;
- miscarea naturala foloseste Paper `Pathfinder`, nu routing semantic;
- `RoutineCoordinator` detine pause/custom override in memorie, dar `RoutineService` nu le citeste;
- nu exista `TimeService` sau `TimelineEngine` in runtime.

## Legaturi

- `architecture/comportament-natural-npc-rutine-alocari.md`
- `architecture/evita-comportamentul-robotic-prin-rutine-staggered.md`
- `reference/feature-flags-lifecycle.md`
- `planning/rutine-npc-si-timeline.md`
