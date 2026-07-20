# Mapping pentru harti construite manual

Status: ghid operational.
Verificat in cod: 2026-07-18.
Depinde de: `architecture/mapping.md`.

Foloseste acest flux pentru a adauga semantica peste o lume construita manual. Comenzile de draft necesita un jucator cu permisiunea `ainpc.admin`, mapping activ si world admin disponibil.

## Flux asistat

### 1. Region

```text
/ainpc wand mode region
/ainpc wand pos1
/ainpc wand pos2
/ainpc map region id=sat_demo name=Sat_Demo type=settlement
/ainpc map preview
/ainpc map confirm
```

### 2. Place

Selecteaza un volum complet in interiorul regiunii, apoi creeaza draftul:

```text
/ainpc wand mode place
/ainpc wand pos1
/ainpc wand pos2
/ainpc map place region=sat_demo id=piata type=market
/ainpc map preview
/ainpc map confirm
```

### 3. Node

Pentru un node dintr-un place:

```text
/ainpc wand mode node
/ainpc wand point
/ainpc map node region=sat_demo place=sat_demo:piata id=punct_intalnire type=interaction
/ainpc map preview
/ainpc map confirm
```

O ancora regionala poate omite `place`; punctul trebuie totusi sa fie in interiorul regiunii.

### 4. Audit si salvare

```text
/ainpc audit world
/ainpc world save
```

`/ainpc map confirm` este deja actiunea explicita de confirmare si aplica draftul doar in runtime. Fara `/ainpc world save`, modificarile raman nesalvate. Atat confirmarea, cat si salvarea sunt blocate cand runtime-ul MCP este `read_only`; preview-ul, editarea si anularea draftului raman disponibile.

## Corectare si anulare

- `/ainpc map preview` reafiseaza draftul curent;
- `/ainpc map edit` deschide editorul GUI al draftului;
- `/ainpc map cancel` elimina draftul curent;
- `/ainpc wand status` arata modul si selectia;
- `/ainpc wand reset pos1|pos2|point|all` curata doar partea ceruta.

## Alternativa directa

Pentru valori deja cunoscute, comenzile world pot evita draftul:

```text
/ainpc world region create <id> <type> [x1] [y1] [z1] [x2] [y2] [z2] --confirm
/ainpc world place create <regionId> <id> <type> [x1] [y1] [z1] [x2] [y2] [z2] --confirm
/ainpc world node create <regionId> <placeId|-> <id> <type> [x] [y] [z] [radius] --confirm
```

`-` creeaza un node direct sub regiune. Sufixul final `--confirm` este obligatoriu pentru mutatiile directe si este eliminat inainte de parsarea argumentelor. Dupa succes, handlerul pastreaza mesajele proprii de audit, iar dispatcher-ul afiseaza un singur reminder `/ainpc world save`; comanda de save nu cere un al doilea flag.

## Erori care trebuie corectate, nu ignorate

- un `Place` iese din bounds-ul regiunii;
- doua places din aceeasi regiune se suprapun;
- un `Node` este in afara containerului sau are raza `<= 0`;
- regiunea, place-ul si node-ul folosesc lumi diferite;
- selectorul scurt are mai multe potriviri;
- ID-ul calificat exista deja.

## Legaturi

- `architecture/mapping.md`
- `reference/mapping-stack.md`
- `guides/build-mode-region-place-node.md`
