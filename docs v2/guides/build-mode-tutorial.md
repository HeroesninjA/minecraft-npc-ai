# Build Mode - pornire rapida

Status: ghid operational.
Verificat in cod: 2026-07-15.
Depinde de: `architecture/mapping.md`.

`Build Mode` pregateste o sesiune de authoring. Selectia, draftul si preview-ul nu sunt persistenta.

## Exemplu minim

```text
/ainpc build mode wand region
/ainpc wand pos1
/ainpc wand pos2
/ainpc map region id=zona_demo type=settlement
/ainpc map preview
/ainpc map confirm
/ainpc audit world
/ainpc world save
/ainpc build mode off
```

Pentru un `Place`, schimba tinta in `place`. Pentru un `Node`, foloseste `point node` si seteaza punctul cu `/ainpc wand point`.

## Comenzi de control

- `/ainpc build mode status` - afiseaza stilul si tinta sesiunii;
- `/ainpc build mode history` - afiseaza istoricul pasilor Build Mode;
- `/ainpc build mode export` - exporta starea de authoring disponibila;
- `/ainpc map edit` - deschide GUI-ul pentru draftul curent;
- `/ainpc map cancel` - anuleaza draftul, fara a sterge mapping confirmat.

## Regula de siguranta

Ordinea obligatorie este `selectie -> draft -> preview/edit -> confirm -> audit -> save`. Oprirea Build Mode sau existenta unui preview nu salveaza mapping-ul.

## Legaturi

- `guides/build-mode-region-place-node.md`
- `guides/mapping-harti-manuale.md`
- `reference/mapping-stack.md`
