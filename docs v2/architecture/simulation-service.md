# Simulation Service

Status: canonical in `docs v2`.
Actualizat: 2026-05-06.

Acesta este rezumatul serviciului care tine NPC-urile "vii" intre interactiuni.

## Rol

- ruleaza tick-uri periodice pentru NPC-urile active;
- actualizeaza nevoi si stare curenta;
- citeste context local: timp, vreme, locatie, pericol, ancore;
- alege actiuni probabile prin scor;
- produce stare suficienta pentru dialog, rutina si debug.

## Stare curenta

- implementarea este distribuita intre `SchedulerCoordinator`, `NPCManager`, `DecisionEngine`, `AINPC`, `NPCContext` si `RoutineService`;
- tick-ul este controlat de `simulation.enabled` si `simulation.tick_seconds`;
- persistenta grea si operatiile costisitoare nu apar in tick-ul principal;
- simularea nu este motor de economie, story sau generare de questuri.

## Reguli

- foloseste date validate si fallback-uri sigure;
- nu scrie direct story state sau quest progress;
- nu face scanari grele sau DB costisitor in tick;
- degradeaza curat cand mapping-ul sau datele lipsesc;
- ramane separata de `RoutineService`, `StoryStateService` si progression.

## Legaturi

- `architecture/mapping.md`
- `architecture/story-context-service.md`
- `canonical/implementat-deja.md`
