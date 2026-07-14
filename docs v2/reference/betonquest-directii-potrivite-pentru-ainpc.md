# Directii din BetonQuest care se potrivesc pentru AINPC

Status: canonical in `docs v2`.
Actualizat: 2026-07-11.

Rezumatul ideilor din BetonQuest care merita preluate in AINPC.

## Ce preia

- building blocks declarative;
- registri pentru extensie;
- API public separat de core;
- progres persistent pe obiective;
- event bus pentru schimbari de stare.

## Ce nu copiaza direct

- API static greu de controlat;
- DSL prea mare prea devreme;
- suprafata mare de hooks inainte de runtime stabil.

## Legaturi

- `architecture/progression-service.md`
- `reference/api-events-listeners-triggers.md`
