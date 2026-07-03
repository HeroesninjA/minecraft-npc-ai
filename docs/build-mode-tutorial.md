# Build Mode Tutorial

Actualizat: 2026-07-01

Acest ghid arata fluxul recomandat pentru folosirea `Build Mode` in `AINPC`.

## Cand folosesti Build Mode

Foloseste `Build Mode` cand vrei sa creezi sau sa corectezi rapid:

- `Region`
- `Place`
- `Node`
- selectie cu `wand`
- selectie cu `sign`
- selectie cu `point`

## 1. Activeaza modul

Porneste cu:

```text
/ainpc build mode on region
```

Alternative utile:

```text
/ainpc build mode wand place
/ainpc build mode sign node
/ainpc build mode point region
```

## 2. Alege metoda de selectie

### Sign mode

Foloseste cand vrei sa descrii direct zona.

```text
/ainpc build mode sign region
```

Scrii pe sign ce reprezinta zona, iar sistemul deschide formularul potrivit.

Poti folosi si etichete simple pe linii:

```text
region
curte_castel
type=settlement
size=48
```

Sau pentru `place` / `node`:

```text
place
fieraria_george
type=forge
region=regatul_nordic
size=16
```

```text
node
avizier
type=quest_trigger
radius=2.5
```

### Wand mode

Foloseste cand zona este neregulata si ai nevoie de selectie vizuala.

```text
/ainpc build mode wand place
```

Pui `pos1` si `pos2`, iar preview-ul ramane vizibil pe durata editarii.

### Point mode

Foloseste cand vrei sa selectezi un bloc anume si sa pornesti din acel punct.

```text
/ainpc build mode point node
```

Același format de hinturi se poate folosi și direct în `/ainpc world create ai ...` și în wizard-ele de quest/progression.

Click-ul pe bloc seteaza pozitia si deschide editorul potrivit.

## 3. Corecteaza valorile

Daca AI-ul sau inputul tau greseste, foloseste:

- `Auto rename` pentru conflicte de nume
- `Auto fit` pentru suprapuneri de spatiu
- `Auto move` pentru repoziționare sigura
- `Suggest` pentru valori compatibile

Exemplu:

```text
/ainpc world create ai preview region name=curte_castel type=castle size=48
/ainpc map edit
/ainpc map confirm
```

Daca vrei doar inspectie in chat si particule, fara sa deschizi formularul:

```text
/ainpc world create ai dryrun region name=curte_castel type=castle size=48
/ainpc map edit
/ainpc map confirm
```

`/ainpc map edit`, `/ainpc map open` si `/ainpc map gui` sunt aliasuri pentru acelasi pas: redeschid editorul potrivit pentru draftul curent.

Cand comanda contine hinturi structurate (`id=`, `name=`, `label=`, `type=`, `region=`, `place=`, `size=`, `radius=`, `height=`) si nu ai selectat deja zona cu wand/sign,
pluginul creeaza draftul in jurul pozitiei tale curente:

- `size=48` inseamna latime/adancime de aproximativ 48 blocuri
- `radius=12` inseamna 12 blocuri in fiecare directie pe X/Z
- `height=20` controleaza inaltimea selectiei

Daca vrei ID si nume afisat separate:

```text
/ainpc world create ai preview region id=curte_castel label=Curtea_Castelului type=castle size=48
```

Daca `id=` lipseste, `name=` devine fallback pentru ID local. Foloseste `label=` doar cand vrei nume afisat separat.

## 4. Confirma doar dupa preview

Nu confirma pana cand:

- numele este valid
- tipul este corect
- centrul sau suprafata nu intra in conflict
- preview-ul arata zona asteptata

## 5. Verifica istoric si export

Comenzi utile:

```text
/ainpc build mode history
/ainpc build mode export
/ainpc build mode clear-history
```

Le poti folosi si din:

- `Admin Hub`
- `World Hub`
- `Admin MCP`

## 6. Foloseste admin tooling

Din GUI-ul de admin ai acces rapid la:

- `Build status`
- `Build history`
- `Build export`
- `Clear build history`

Din MCP intern poti inspecta:

- `ainpc.build.mode.status`
- `ainpc.build.mode.history`
- `ainpc.build.mode.export`

## 7. Rezumat recomandat

Ordinea buna este:

1. pornesti `build mode`
2. alegi metoda: `sign`, `wand` sau `point`
3. corectezi cu `Auto rename` / `Auto fit` / `Auto move`
4. confirmi
5. verifici `history` sau `export`
