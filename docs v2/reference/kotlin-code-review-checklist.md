# Kotlin Code Review Checklist

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Checklist pentru review-ul schimbarilor Kotlin.

## Verifica

- package-ul si compatibilitatea Java;
- threading si runtime Paper;
- schema DB si plugin.yml;
- interop Java/Kotlin;
- testele relevante si build-ul curat.

## Regula

- schimbarea de comportament se trateaza separat de conversia mecanica;
- adnotarile JVM se adauga doar cand exista nevoie reala.

## Legaturi

- `reference/kotlin-interop-api-addonuri.md`
- `reference/kotlin-paper-packaging-si-smoke.md`
