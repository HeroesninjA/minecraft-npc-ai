# Generare de sate fara WorldEdit

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Directia curenta pentru extinderea satelor fara a depinde de WorldEdit.

## Ce face

- scaneaza satul vanilla;
- mapeaza semantic regiuni, place-uri si node-uri;
- analizeaza gap-uri;
- propune patch-uri si planuri read-only;
- pastreaza comenzi de inspectie si import.

## Regula

- baza este stilul vanilla;
- AINPC adauga strat semantic si control;
- WorldEdit ramane optional pentru viitor.

## Legaturi

- `architecture/generare-sate-worldedit-si-npc.md`
- `architecture/mapping.md`
