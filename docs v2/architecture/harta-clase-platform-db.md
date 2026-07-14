# Harta claselor pentru platform si database

Status: canonical in `docs v2`.
Actualizat: 2026-06-21.

Aceasta harta urmareste infrastructura de platforma si persistenta.

## Noduri principale

- `AINPCPlatform` pentru bootstrap si reload;
- `AINPCPlatformApi` pentru contract public;
- `AddonRegistry` si `AddonRegistryApi` pentru addonuri;
- `DatabaseManager` pentru conexiuni, schema si executie SQL;
- `DatabaseDialect` pentru adaptare SQL;
- `PlatformProfile` pentru profilul de executie;
- `RuntimeFeatureResolver` si `RuntimeFeatureSnapshot` pentru capabilitati active.

## Flux

- platforma orchestreaza bootstrap-ul;
- registrul pastreaza addonurile active;
- managerul de baza de date rezolva persistenta;
- resolverul de feature-uri decide ce este activ.

## Reguli

- API-ul public ramane separat de internals;
- feature flags se rezolva central;
- persistenta si shutdown-ul trebuie sa fie ordonate si auditabile.

## Legaturi

- `reference/documentatie-api.md`
- `reference/harta-clase-index.md`
- `canonical/constitutie-proiect.md`
