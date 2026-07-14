# Generare populatie narativa

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Modelul pentru populatie de sat care pare comunitate, nu lista de entitati.

## Ce adauga

- nume potrivite temei;
- roluri sociale clare;
- profesii legate de locuri reale;
- familii simple;
- alocari corecte pe case;
- motivatii pentru questuri si story locale.

## Pipeline

- mapping -> population plan -> household plan -> allocation -> spawn plan;
- planul poate fi verificat in dry-run;
- seed-ul trebuie sa dea rezultate stabile.

## Regula

- mapping-ul este sursa de adevar;
- populatia nu inventeaza infrastructura;
- spawn-ul vine dupa validare.

## Legaturi

- `architecture/comportament-natural-npc-rutine-alocari.md`
- `architecture/mapping.md`
