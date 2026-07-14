# Kotlin Style Guide

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Stilul Kotlin pentru proiectul AINPC.

## Principii

- codul nou de productie se scrie in Kotlin;
- Java existent poate ramane, dar logica noua merge in Kotlin;
- intentia trebuie sa fie mai clara, nu doar mai scurta;
- codul critic pentru Paper, DB si API public ramane explicit.

## Reguli

- foloseste `val` implicit;
- foloseste `var` doar cand starea se modifica;
- pastreaza package-urile `ro.ainpc...`;
- evita facilitati Kotlin care complica interop-ul.

## Legaturi

- `reference/kotlin-interop-api-addonuri.md`
- `reference/kotlin-code-review-checklist.md`
