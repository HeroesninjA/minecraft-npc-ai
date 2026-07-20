# Simulare sat si lume

Status: vedere derivata, nu contract runtime.
Actualizat: 2026-07-15.

Aceasta pagina arata cum se compun subsistemele existente. Nu exista in cod un motor unic de simulare a satului.

## Straturi existente

- mapping: `Region -> Place -> Node` si ancore semantice;
- spawn: planuri, validare, batch tracking si rollback;
- persistenta: NPC-uri, world bindings si household-uri;
- rutina: sloturi home/work/social, pathfinding direct si teleport fallback;
- simulare individuala: nevoi, context, actiune, stare si goal;
- social/economie/evenimente: servicii si schedulere separate.

## Ce nu trebuie dedus

- `NpcPopulationService` raporteaza statistici; nu genereaza sau echilibreaza populatia;
- repopularea satelor este un flux separat, bazat pe paturi si dezactivat implicit prin `villagers.auto_repopulate.enabled`;
- household-ul persistent nu conduce automat familia, rutina sau interactiunile sociale;
- nu exista agregare canonica de resurse, productie sau stare a satului;
- AI-ul nu este autoritate pentru starea lumii.

## Traseu de lectura

- `architecture/mapping.md`
- `planning/npc-population-world-stack.md`
- `architecture/simulation-service.md`
- `architecture/households-persistente.md`
- `planning/rutine-npc-si-timeline.md`
