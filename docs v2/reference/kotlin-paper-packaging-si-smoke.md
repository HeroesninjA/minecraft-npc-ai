# Kotlin Paper Packaging si Smoke Test

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Verificarea JAR-urilor si smoke testele dupa introducerea Kotlin.

## Verifica

- `plugin.yml` in JAR;
- runtime Kotlin inclus;
- clasele Kotlin proprii prezente;
- addonul medieval incarca dupa core;
- serverul Paper porneste si incarca pluginul.

## Regula

- build-ul Gradle verde nu este suficient;
- packaging-ul si smoke-ul Paper sunt obligatorii.

## Legaturi

- `reference/kotlin-style-guide.md`
- `operations/test-fixtures-and-demo-world.md`
