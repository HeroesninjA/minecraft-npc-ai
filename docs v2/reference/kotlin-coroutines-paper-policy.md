# Kotlin Coroutines si Paper Policy

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Politica pentru coroutine in runtime-ul Paper.

## Regula

- coroutine nu intra in conversia initiala;
- scheduler-ul Paper ramane mecanismul principal;
- lifecycle-ul si shutdown-ul trebuie proiectate explicit.

## Interzis initial

- `kotlinx.coroutines`;
- `GlobalScope`;
- `runBlocking` in runtime Paper;
- dispatchere custom fara design dedicat.

## Legaturi

- `reference/kotlin-style-guide.md`
- `reference/kotlin-interop-api-addonuri.md`
