# Interactiune, dialog si reactie

Status: index derivat pentru fluxurile player-NPC si story-NPC.
Actualizat: 2026-07-15.

## Flux player-NPC

1. `architecture/interactiuni.md` detine click-ul, chatul, sesiunea si alegerea tintei.
2. `ScenarioEngine` are prioritate pentru intentiile de quest si poate inchide fluxul cu mesaje proprii.
3. Pentru dialogul normal, `architecture/dialog-si-conversatii.md` detine selectia fapt-template-AI si fallback-ul.
4. Dupa o replica nevida, `DialogManager` aplica mutatiile player-NPC descrise in `architecture/reactie-npc-jucator.md`.

## Flux story-NPC

1. `StoryAuthoringService` persista evenimentul.
2. `StoryReactionService` aplica reactia locala configurata in cod NPC-urilor din regiune.
3. Acest flux nu trece prin `DialogManager` si nu foloseste relatia player-NPC.

## Regula de proprietate

- acest document ruteaza si nu redefinește contractele;
- nu descrie cele doua fluxuri drept un motor unic;
- `RelationshipService` inseamna relatii NPC-NPC, iar `DialogManager` detine relatia player-NPC;
- progresul de quest si story state raman la serviciile lor deterministe.
