# Interactiune, dialog si reactie

Status: index derivat pentru fluxul player-NPC.
Actualizat: 2026-07-14.

## Flux

1. `architecture/interactiuni.md` capteaza si routeaza intentia.
2. serviciul determinist valideaza si executa actiunea.
3. `architecture/reactie-npc-jucator.md` evalueaza si aplica reactia NPC.
4. `architecture/dialog-si-conversatii.md` formuleaza raspunsul final.

## Regula

- acest document descrie ordinea si nu redefineste contractele celor trei componente;
- progresul si persistenta raman la serviciile deterministe.
