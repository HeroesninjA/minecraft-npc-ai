# Interactiuni

Status: contract canonic pentru intrarea jucatorului in runtime.
Actualizat: 2026-07-14.

Acest document defineste capturarea si rutarea intentiei jucatorului.

## Intrari

- click dreapta pe NPC;
- chat privat sau ascultare pasiva;
- intentii de quest sau actiune;
- evenimente relevante din lume.

## Responsabilitate

- normalizeaza intentia si contextul;
- routeaza cererea catre serviciul determinist potrivit;
- nu decide progresul si nu persista direct stare;
- produce semnale pentru reactie si dialog.

## Legaturi

- `architecture/interactiune-dialog-reactie-stack.md`
- `architecture/reactie-npc-jucator.md`
- `architecture/dialog-si-conversatii.md`
