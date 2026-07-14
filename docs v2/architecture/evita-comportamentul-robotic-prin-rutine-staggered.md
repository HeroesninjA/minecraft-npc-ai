# Evita comportamentul robotic prin rutine staggered

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Modelul de variatie controlata pentru rutinele NPC.

## Principiu

- satul are ritm, nu sincronizare perfecta;
- fiecare NPC are offset stabil;
- jitter-ul zilnic este mic;
- rolurile au ferestre diferite;
- plecarile si intoarcerile apar in valuri.

## Ce evita

- ora fixa identica pentru toti;
- acelasi minut pentru toate tranzitiile;
- pattern mecanic pentru toate gospodariile.

## Legaturi

- `architecture/comportament-natural-npc-rutine-alocari.md`
- `planning/ce-mai-trebuie-pentru-sat-semantic.md`
