# Admin / MCP Tutorial

Acest ghid arata fluxul recomandat pentru inspectia si administrarea starii runtime prin GUI si MCP.

## 1. Deschide hub-ul de admin

Din joc, mergi in:

- `Admin Hub`
- `World Hub`
- `Admin MCP`

Aceste panouri iti permit sa verifici rapid mapping-ul, progresiile si starea MCP.

## 2. Verifica build mode

Pentru build mode ai trei niveluri utile:

### In chat

```text
/ainpc build mode status
/ainpc build mode history
/ainpc build mode export
/ainpc build mode clear-history
```

### In GUI

Din `Admin Hub`, `World Hub` si `Admin MCP` ai scurtaturi pentru:

- `Build status`
- `Build history`
- `Build export`
- `Clear build history`

### Prin MCP

Tool-urile disponibile sunt:

- `ainpc.build.mode.status`
- `ainpc.build.mode.history`
- `ainpc.build.mode.export`

## 3. Inspecteaza snapshot-ul

In `Admin MCP` poti vedea:

- health-ul MCP
- feature flags
- snapshot-ul runtime
- build mode activ
- playerii activi
- istoricul recent

## 4. Foloseste debugdump cand ai nevoie de context semantic

Comenzi utile:

```text
/ainpc debugdump world summary
/ainpc debugdump mapping summary
/ainpc debugdump quest summary
/ainpc debugdump progression summary
```

Acestea sunt utile cand vrei sa verifici repede ce vede sistemul.

## 5. Lucreaza in ordinea corecta

Ordinea buna este:

1. deschizi GUI-ul potrivit
2. verifici statusul runtime
3. inspectezi history sau export
4. corectezi daca e nevoie
5. confirmi doar dupa ce ai context complet

## 6. Cand folosesti clear-history

Folosește `clear-history` doar cand vrei sa resetezi jurnalul local al playerului curent.

Nu sterge starea runtime a mapping-ului; curata doar istoricul build mode din sesiunea curenta.

