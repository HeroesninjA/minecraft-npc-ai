# NPC World Bindings

Status: canonical in `docs v2`.
Actualizat: 2026-07-11.

Tabela persistenta care leaga NPC-urile de mapping-ul semantic.

## Ce pastreaza

- home/work/social place;
- home/work/social node;
- familia;
- sursa binding-ului;
- timestamp-uri de audit.

## Regula

- metadata veche ramane fallback;
- tabela dedicata devine sursa de adevar pentru binding-uri;
- auditul valideaza orfane, place-uri lipsa si node-uri gresite.

## Legaturi

- `planning/npc-population-world-stack.md`
- `architecture/households-persistente.md`
