# Simulation Service - Partea 2

Acesta este pasul de extractie si maturizare a `SimulationService`.

## Scop

- introduce un orchestrator clar pentru tick-ul de simulare;
- pastreaza responsabilitatile separate intre decision, routine, story si quest;
- ramane testabil si observabil fara rescriere completa a runtime-ului.

## Livrabile

- `SimulationTickSummary`;
- `SimulationNpcSnapshot`;
- `SimulationService`;
- `SimulationPolicy`;
- audit si debugdump.
