# Kotlin Interop, API si Addonuri

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Reguli de interoperabilitate Java/Kotlin pentru API si addonuri.

## Regula

- `ainpc-api` ramane Java pana exista motiv clar;
- tipurile publice trebuie sa ramana usor de consumat din Java;
- orice tip Kotlin public are test Java de consum.

## Ce protejeaza

- addonuri scrise in Java;
- call-site-uri Java existente;
- compatibilitate pentru constructori, gettere si default args.

## Legaturi

- `reference/kotlin-code-review-checklist.md`
- `reference/kotlin-style-guide.md`
