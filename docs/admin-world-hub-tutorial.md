# Admin Hub / World Hub Tutorial

Actualizat: 2026-07-01

Acest ghid explica fluxul rapid de lucru din hub-urile principale de administrare.

## 1. Admin Hub

Deschide `Admin Hub` cand vrei o vedere de ansamblu.

Ordinea practica a butoanelor este:

1. `World`
2. `Mapping admin`
3. `Manager NPC`
4. `Audit`
5. `Debug`
6. `MCP`
7. `Authoring`
8. `Story`
9. `Demo mapping`
10. `Salveaza mapping`

Ce folosesti cel mai des:

- `Build mode` status pentru inspectie rapida
- `Build history` pentru ultima actiune
- `Build export` pentru sumar compact
- `Clear build history` doar cand vrei reset local

## 2. World Hub

Deschide `World Hub` cand lucrezi direct cu zona curenta.

Fluxul util este:

1. verifici `World context`
2. verifici `Mapping snapshot`
3. verifici `Mediu`
4. deschizi `Regiune curenta` sau `Place curent`
5. inspectezi `Progresii active`
6. verifici `Mapping diagnostics`
7. folosesti `Where am I`
8. deschizi `Story`

Pentru administratori, hub-ul include si scurtaturi rapide pentru `Build mode`:

- `Build status`
- `Build history`
- `Build export`
- `Clear build history`

Cand ai deja un draft de mapping generat prin `world create ai preview|dryrun|inspect`, foloseste `map edit`, `map open` sau `map gui` ca sa redeschizi editorul curent inainte de confirmare.

## 3. Cand alegi fiecare hub

- `Admin Hub` pentru operatiuni globale si inspectie larga
- `World Hub` pentru contextul local al jucatorului si mapping-ul din zona
- `Admin MCP` pentru snapshot, feature flags si starea MCP

## 4. Ordinea recomandata

Cand investighezi o problema:

1. deschizi `Admin Hub`
2. verifici `Build mode` sau `MCP`
3. treci in `World Hub`
4. inspectezi mapping-ul local
5. folosesti `debugdump` daca ai nevoie de context semantic
