# SettlementPlan

Acesta este contractul canonic pentru planul inspectabil de sat si regiune.

## Scop

- descrie ce exista sau urmeaza sa existe intr-o regiune inainte de spawn, persistenta finala sau constructie;
- leaga mapping-ul semantic, cladiri, node-uri, populatie narativa, household-uri si ancore;
- ofera validare, dry-run si export inainte de commit.

## Principii

- planul este read-only pana la commit;
- ID-urile sunt stabile si deterministe;
- planul trebuie sa acopere commit, dry-run, discard si audit;
- nu exista salt direct de la regiune la NPC-uri spawnate fara plan inspectabil.

## Flux tinta

- scan/import/generator;
- `SettlementPlan`;
- validare;
- dry-run;
- mapping / population / allocations;
- spawn, persistenta si audit.
