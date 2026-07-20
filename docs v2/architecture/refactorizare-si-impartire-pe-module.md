# Refactorizare si Impartire pe Module

Status: baseline implementat plus roadmap activ.
Actualizat: 2026-07-15.

Repository-ul este deja un build Gradle multi-project. Referintele vechi la module Maven nu mai descriu proiectul curent.

## Module curente

| Modul | Rol | Limbaj principal | Packaging |
| --- | --- | --- | --- |
| `ainpc-api` | contracte publice, evenimente si DTO-uri | Kotlin | JAR de biblioteca |
| `ainpc-core-plugin` | runtime Paper si persistenta | Kotlin | JAR Paper cu dependinte runtime incluse |
| `ainpc-scenario-medieval` | addon Paper, config si pack-uri tematice | Kotlin | JAR subtire, dependent de core |
| `ainpc-mcp-service` | sidecar Spring Boot MCP | Java | `bootJar` separat |

Modulele sunt declarate in `settings.gradle`; versiunile comune si conventiile JVM sunt in `build.gradle` si `gradle.properties`.

## Separare implementata

- `ainpc-api` nu depinde de internals core;
- core-ul expune `AINPCPlatformApi` prin Bukkit `ServicesManager`;
- addonul medieval compileaza cu `ainpc-api` si declara dependinta Paper fata de core;
- continutul tematic este livrat prin resursele addonului, nu prin default packs instalate de core;
- `ServiceRegistry` detine fazele de initializare, reload si shutdown;
- `SchedulerCoordinator` detine pornirea si oprirea task-urilor programate.

## Datorie structurala activa

- `ScenarioEngine`, `FeaturePackLoader`, `NPCManager` si unele servicii raman clase mari;
- suprafata `ainpc-api` contine contracte partiale sau neexpuse;
- dependintele addonurilor de cod sunt diagnosticate, nu impuse in lifecycle;
- granita dintre API Kotlin-first si consum Java nu este stabilizata complet;
- packaging-ul core este un fat JAR construit manual din `runtimeClasspath`, deci necesita smoke test de duplicate si classloading.

## Directie de refactorizare

1. stabilizeaza contractele publice si elimina accessorii runtime nefunctionali;
2. decide daca graful de dependinte devine gate sau ramane diagnostic explicit;
3. extrage parsarea, validarea si registrul din `FeaturePackLoader` fara schimbarea schemei;
4. extrage lifecycle-ul quest/scenario din `ScenarioEngine` pe limite deja testabile;
5. mentine core-ul ca plugin universal si addonurile tematice ca module separate;
6. adauga verificari ABI, packaging si Paper smoke la release.

## Ce nu este obiectiv

- revenirea la Maven;
- mutarea tuturor claselor in module mici fara limita functionala clara;
- expunerea internals core doar pentru a evita un adaptor API;
- prezentarea roadmap-ului drept refactor deja finalizat.

## Legaturi

- `architecture/strategie-plugin-modular-si-scenarii-programabile.md`
- `reference/documentatie-api.md`
- `reference/kotlin-paper-packaging-si-smoke.md`
- `planning/stabilizare-api-si-addonuri.md`
