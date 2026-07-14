# Households persistente

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Modelul persistent pentru household-uri, rezidenti si legatura cu rutina satului.

## Ce rezolva

- casa principala pentru un grup;
- rezidenti persistenti;
- compatibilitate cu metadata veche;
- sursa clara pentru familie si rutina;
- audit si migration mai simple.

## Reguli

- household-ul are o casa principala;
- un NPC permanent are cel mult un household activ;
- familia si household-ul sunt concepte diferite;
- metadata veche ramane doar fallback.

## Legaturi

- `architecture/generare-populatie-narativa.md`
- `architecture/comportament-natural-npc-rutine-alocari.md`
