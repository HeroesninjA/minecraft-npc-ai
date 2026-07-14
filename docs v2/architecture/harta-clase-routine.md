# Harta claselor pentru rutine

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Harta scurta pentru motorul de rutina zilnica al NPC-urilor.

## Noduri principale

- `RoutineService`;
- `RoutineEngine`;
- `RoutineAssignment`;
- `RoutineSlot`;
- `RoutineScheduleEntry`;
- `RoutineTickSummary`;
- `BehaviorProfile`;
- `BehaviorProfileLoader`.

## Flux

- tick de rutina;
- atribuire slot si goal;
- aplicare miscare sau teleport;
- sumarizare pentru debug.

## Legaturi

- `architecture/comportament-natural-npc-rutine-alocari.md`
- `reference/feature-flags-lifecycle.md`
