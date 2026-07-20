# Harta claselor pentru platforma si database

Status: harta verificata in `docs v2`.
Actualizat: 2026-07-16.

Aceasta harta separa bootstrap-ul, fatada publica, feature resolution si persistenta.
Comportamentul backend-urilor, schema si limitele de migrare sunt detinute de `reference/storage-runtime.md`.

## Noduri principale

- `ServiceRegistry` pentru fazele de initializare, reload si shutdown;
- `AINPCPlatform` pentru profilul runtime si implementarea `AINPCPlatformApi`;
- `AINPCPlatformApi` pentru contractul public oferit prin Bukkit `ServicesManager`;
- `AddonRegistry` si `AddonRegistryApi` pentru descriptori si addonuri de cod;
- `IntegrationRegistry` si `IntegrationRegistryApi` pentru integrari externe;
- `DatabaseManager` pentru conexiuni, schema si executie SQL;
- `StoryStateService` pentru `story_events`, coada `story_pending_events` si mutarea tranzactionala dintre ele;
- `DatabaseDialect` pentru adaptare SQL;
- `PlatformProfile` pentru modurile de executie;
- `RuntimeFeatureResolver` si `RuntimeFeatureSnapshot` pentru capabilitatile active.

## Flux

```text
AINPCPlugin
-> ServiceRegistry.initialize
-> AINPCPlatform.reloadFromConfig
-> servicii core si feature packs
-> ServicesManager.register(AINPCPlatformApi)
-> schedulere si runtime
```

La shutdown, `ServiceRegistry` opreste task-urile, persista serviciile, inchide baza de date, opreste platforma si elimina providerii Bukkit.

## Limite

- `AINPCPlatform` nu detine singur bootstrap-ul complet;
- nu toate interfetele din `ainpc-api` au provider runtime;
- persistenta core nu este API public pentru addonuri.

## Reguli

- API-ul public ramane separat de internals;
- feature flags se rezolva central;
- persistenta si shutdown-ul raman ordonate si auditabile;
- un serviciu este documentat ca disponibil numai daca are provider conectat.

## Legaturi

- `reference/documentatie-api.md`
- `reference/storage-runtime.md`
- `architecture/harta-clase-addons.md`
- `reference/harta-clase-index.md`
- `planning/stabilizare-api-si-addonuri.md`
