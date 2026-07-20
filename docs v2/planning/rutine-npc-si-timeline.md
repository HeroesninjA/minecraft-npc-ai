# Rutine NPC si timeline

Status: roadmap activ; nu confirma implementarea.
Actualizat: 2026-07-15.

Rutina de baza exista. Acest document pastreaza numai munca ramasa pana la un timeline coerent.

## Baseline implementat

- `RoutineEngine`, `RoutineAssignment`, `RoutineService` si `RoutineCoordinator`;
- profile pe ocupatie, ferestre orare si offset stabil;
- ancore home/work/social si miscare cu pathfinding plus teleport fallback;
- `AINPCRoutineChangedEvent` public;
- household-uri persistente si family binding in fluxul de spawn.

## Partial sau neconectat

- ora vine direct din `World.time`; nu exista `TimeService`;
- household-ul si familia nu influenteaza atribuirea rutinei;
- evenimentul de rutina nu are consumator core pentru quest sau story;
- pause, resume si custom override pastreaza stare in `RoutineCoordinator`, dar nu sunt aplicate de tick;
- `NpcSimulationMode` si `NpcInteractionProfile` nu reduc costul actorilor temporari.

## Ordinea ramasa

1. conecteaza pause/override si defineste precedenta fata de starile ocupate;
2. aplica modurile de simulare si lifecycle in schedulere;
3. stabileste consumatori read-only, idempotenti, pentru evenimentul de rutina;
4. introduce o abstractie de timp numai daca apar calendar sau timp accelerat;
5. proiecteaza `TimelineEngine` dupa ce sursele si ownership-ul evenimentelor sunt stabile.

## Non-obiective imediate

- scheduler global care rescrie quest, story si economie;
- pathfinding semantic pe drumuri in acelasi refactor;
- timeline bazat pe AI ca autoritate.

## Legaturi

- `architecture/simulation-service.md`
- `architecture/harta-clase-routine.md`
- `architecture/households-persistente.md`
- `architecture/npc-uri-temporare-si-episodice.md`
