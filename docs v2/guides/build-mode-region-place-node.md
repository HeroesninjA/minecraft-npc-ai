# Build Mode pentru `Region / Place / Node`

Status: canonical in `docs v2`.
Actualizat: 2026-07-01.

Acesta este ghidul detaliat pentru authoring-ul semantic al mapping-ului.

## Scop

- selectie vizuala pentru `Region`, `Place` si `Node`;
- preview persistent pe durata sesiunii;
- compatibilitate cu `maintenance mode`;
- fluxuri asistate pentru mapping si comenzi legate de world.

## Moduri

- `Sign Mode` pentru cazuri simple si rapide;
- `Multi-Selection Wand` pentru forme neregulate;
- `Point / Anchor Mode` pentru node-uri si ancore exacte.

## Reguli

- `Build Mode` descrie intentie, nu reconstructie completa a lumii;
- selectia trebuie sa ramana vizibila pana la confirmare sau anulare;
- fiecare selectie produce un rezultat semantic verificabil;
- AI poate sugera, dar runtime-ul valideaza;
- mapping-ul si questurile folosesc acelasi strat semantic.

## Rezultat asteptat

- adminul poate crea sau corecta mapping fara sa scrie manual fiecare camp;
- preview-ul si auditul arata conflictul inainte de scriere;
- fluxul ramane sigur pentru lume si date.

## Legaturi

- `guides/build-mode-tutorial.md`
- `reference/mapping-stack.md`
- `architecture/mapping.md`
