# Propunere de quest: Blestemul Castelului Parasit

Status: concept de continut compatibil, neimplementat.
Actualizat: 2026-07-15.

Ideea de quest ramane disponibila pentru un scenario pack viitor. Mapping-ul unui castel sau existenta claselor de quest nu inseamna ca definitia este deja implementata.

## Directie narativa pastrata

- un NPC local, posibil fierarul, introduce problema;
- jucatorul investigheaza castelul si urmareste indicii;
- progresia poate combina explorare, interactiune si combat;
- stage-urile modifica story state-ul numai prin serviciile deterministe;
- recompensa si consecintele trebuie sa ramana configurabile in pack.

Aceste puncte sunt intentie de continut, nu objective keys sau comenzi existente.

## Cerinte de authoring

- `scenarioId`, `questId` si versiune stabile;
- objective types din catalogul canonic;
- ancore explicite pentru NPC, regiune, place si node;
- preconditii, recompense si mutatii story validate;
- fallback pentru ancore lipsa;
- texte si continut tematic in addon, nu in core;
- smoke test pentru acceptare, progres, restart si completare.

## Gate-uri pentru implementare

1. mapping-ul castelului este validat;
2. NPC-urile si binding-urile sunt identificabile;
3. definitia pack trece validatorul;
4. fiecare objective key are sursa runtime confirmata;
5. recompensa si story state-ul sunt auditate;
6. testul cap-coada trece pe server.

## Legaturi

- `planning/questuri-avansate-v2.md`
- `guides/quest-authoring-tutorial.md`
- `reference/objective-types-reference.md`
- `reference/quest-anchor-bindings.md`
- `architecture/mapping.md`
