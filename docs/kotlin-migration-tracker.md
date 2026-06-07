# Kotlin Migration Tracker

Actualizat: 2026-06-07

## Scop

Acest document este trackerul operational pentru conversia Java -> Kotlin.

Planul activ si regulile sunt definite in:

- `rezumat-conversie-java-la-kotlin.md`
- `kotlin-style-guide.md`
- `kotlin-interop-api-addonuri.md`
- `kotlin-paper-packaging-si-smoke.md`
- `kotlin-code-review-checklist.md`
- `kotlin-coroutines-paper-policy.md`
- `kotlin-testing-strategy.md`

Istoricul de planificare este arhivat in:

- `arhiva/kotlin-migration/README.md`

Acest fisier tine evidenta concreta a slice-urilor.

## Status global

| Zona | Status |
|---|---|
| Gradle migrat de la Maven | facut |
| Documentatie Kotlin principala | facut |
| Kotlin activ in build | validat local |
| Primul test Kotlin | validat local |
| Prima clasa Kotlin de productie | validat local |
| JAR audit Kotlin | validat local pentru core |
| Smoke Paper dupa Kotlin | neinceput |
| `ainpc-api` convertit | in lucru (majoritar Kotlin; 3 interfețe Java pastrate pentru interop) |
| `ainpc-scenario-medieval` convertit | validat local |

## Taskuri ramase estimate

Estimare operationala la 2026-06-07: mai sunt aproximativ 33 taskuri pana la finalizarea conversiei de productie la Kotlin in core.

| Zona | Linii Java ramase | Taskuri estimate |
|---|---:|---:|
| `AINPCCommand.java` | 6961 | 12 |
| `ScenarioEngine.java` | 8083 | 14 |
| `NPCManager.java` | 2985 | 1 |
| Gate final si hardening | n/a | 6 |
| Total | 18029 | 33 |

## Regula de actualizare

Actualizeaza trackerul dupa fiecare slice Kotlin.

Un slice inseamna:

- un test convertit
- o clasa mica convertita
- o configurare Gradle
- o verificare de packaging
- un smoke test

Nu grupa mai multe pachete intr-un singur rand.

## Status board initial

| ID | Zona | Tip | Risc | Status | Gate |
|---|---|---|---:|---|---|
| KOT-001 | `ainpc-core-plugin` Gradle Kotlin | build | 2 | validat local | `clean build` |
| KOT-002 | Kotlin smoke test | test | 1 | validat local | test JUnit Kotlin |
| KOT-003 | test simplu `utils` | test | 1 | validat local | test specific |
| KOT-004 | test simplu `progression` | test | 1 | validat local | test specific |
| KOT-005 | `progression` filter/selector mic | productie | 2 | validat local | `*Progression*` |
| KOT-006 | `world.patch` enum/model | productie | 2 | validat local | `VillagePatchPlannerTest` |
| KOT-007 | `engine` selector/resolver mic | productie | 2 | validat local | `*Quest*` |
| KOT-008 | `spawn` hasher/model | productie | 2 | validat local | hash stabil |
| KOT-009 | JAR audit Kotlin | packaging | 3 | validat local | runtime Kotlin verificat |
| KOT-010 | Paper smoke core | smoke | 4 | planificat | server Paper porneste |
| KOT-011 | `gui` key aliases | productie | 2 | validat local | `GuiKeyTest` |
| KOT-012 | `gui` quest filter aliases | productie | 2 | validat local | `QuestLogGuiFilterTest` |
| KOT-013 | `world.patch` status/gap enums | productie | 1 | validat local | `VillagePatchPlannerTest` |
| KOT-014 | `ai.orchestration` enums/policy | productie | 2 | validat local | `AIOrchestrationServiceTest` |
| KOT-015 | `ai.orchestration` request/result | productie | 2 | validat local | `AIOrchestrationServiceTest` |
| KOT-016 | `ai.orchestration` service | productie | 2 | validat local | `AIOrchestrationServiceTest` |
| KOT-017 | `world.mapping` draft/wand enums | productie | 2 | validat local | `MappingIntentParserTest` |
| KOT-018 | `world.mapping` point/apply result | productie | 2 | validat local | `MappingDraftFactoryTest` |
| KOT-019 | `world.mapping` selection/suggestion | productie | 2 | validat local | `MappingDraftFactoryTest` |
| KOT-020 | `world.scan` feature/import models | productie | 2 | validat local | `SemanticVillageMapperTest` |
| KOT-021 | `routine` models | productie | 2 | validat local | `RoutineEngineTest` |
| KOT-022 | `spawn` validation/family models | productie | 2 | validat local | `ro.ainpc.spawn.*` |
| KOT-023 | `spawn` result/resolved models | productie | 2 | validat local | `ro.ainpc.spawn.*` |
| KOT-024 | `gui` primitives/session models | productie | 2 | validat local | `ro.ainpc.gui.*` |
| KOT-025 | `debug` semantic index | productie | 2 | validat local | `WorldMappingSemanticIndexTest` |
| KOT-026 | `gui` quest log paging | productie | 2 | validat local | `QuestLogGuiPageTest` |
| KOT-027 | `gui` item factory | productie | 2 | validat local | `ro.ainpc.gui.*` |
| KOT-028 | `gui` inventory holder | productie | 1 | validat local | `ro.ainpc.gui.*` |
| KOT-029 | `gui` inventory listener | productie | 2 | validat local | `ro.ainpc.gui.*` |
| KOT-030 | `gui` placeholder screen | productie | 1 | validat local | `ro.ainpc.gui.*` |
| KOT-031 | `gui` audit screen | productie | 1 | validat local | `ro.ainpc.gui.*` |
| KOT-032 | `gui` debug screen | productie | 1 | validat local | `ro.ainpc.gui.*` |
| KOT-033 | `gui` stats screen | productie | 2 | validat local | `ro.ainpc.gui.*` |
| KOT-034 | `gui` confirm action screen | productie | 1 | validat local | `ro.ainpc.gui.*` |
| KOT-035 | `gui` main hub screen | productie | 2 | validat local | `ro.ainpc.gui.*` |
| KOT-036 | `gui` npc manager screen | productie | 2 | validat local | `ro.ainpc.gui.*` |
| KOT-037 | `gui` npc interaction screen | productie | 2 | validat local | `ro.ainpc.gui.*` |
| KOT-038 | `gui` routine screen | productie | 2 | validat local | `ro.ainpc.gui.*` |
| KOT-039 | `gui` quest log screen | productie | 3 | validat local | `ro.ainpc.gui.*` |
| KOT-040 | `gui` world hub screen | productie | 3 | validat local | `ro.ainpc.gui.*` |
| KOT-041 | `gui` story screen | productie | 3 | validat local | `ro.ainpc.gui.*` |
| KOT-042 | `gui` quest detail screen | productie | 4 | validat local | `ro.ainpc.gui.*` |
| KOT-043 | `npc` state/action enums | productie | 2 | validat local | `RoutineEngineTest` |
| KOT-044 | `npc` emotions model | productie | 2 | validat local | `clean build` |
| KOT-045 | `npc` personality model | productie | 2 | validat local | `clean build` |
| KOT-046 | `npc` context model | productie | 3 | validat local | `clean build` |
| KOT-047 | `world` type enums | productie | 1 | validat local | `clean build` |
| KOT-048 | `world` story state model | productie | 1 | validat local | `clean build` |
| KOT-049 | `world` npc binding model | productie | 2 | validat local | `clean build` |
| KOT-050 | `world` node model | productie | 2 | validat local | `clean build` |
| KOT-051 | `world` place model | productie | 2 | validat local | `clean build` |
| KOT-052 | `world` region model | productie | 2 | validat local | `clean build` |
| KOT-053 | `world.patch` planner primitives | productie | 2 | validat local | `clean build` |
| KOT-054 | `world.patch` candidate/plan models | productie | 2 | validat local | `clean build` |
| KOT-055 | `world.patch` gap report model | productie | 2 | validat local | `clean build` |
| KOT-056 | `world` context snapshot model | productie | 3 | validat local | `clean build` |
| KOT-057 | `world` mapping index | productie | 2 | validat local | `clean build` |
| KOT-058 | `world` context snapshot builder | productie | 3 | validat local | `clean build` |
| KOT-059 | `world.mapping` draft model | productie | 2 | validat local | `clean build` |
| KOT-060 | `world.mapping` intent parser | productie | 3 | validat local | `clean build` |
| KOT-061 | `world.patch` planner service | productie | 3 | validat local | `clean build` |
| KOT-062 | `world.patch` analyzer service | productie | 4 | validat local | `clean build` |
| KOT-063 | `world.mapping` draft factory service | productie | 4 | validat local | `clean build` |
| KOT-064 | `world.mapping` wand service | productie | 4 | validat local | `clean build` |
| KOT-065 | `topology` category/consensus models | productie | 2 | validat local | `clean build` |
| KOT-066 | `story` place/region state models | productie | 2 | validat local | `clean build` |
| KOT-067 | `story` event model | productie | 2 | validat local | `clean build` |
| KOT-068 | `story` context snapshot model | productie | 3 | validat local | `clean build` |
| KOT-069 | `progression` stage snapshot model | productie | 2 | validat local | `clean build` |
| KOT-070 | `progression` objective snapshot model | productie | 2 | validat local | `clean build` |
| KOT-071 | `progression` anchor binding model | productie | 2 | validat local | `clean build` |
| KOT-072 | `progression` gui snapshot model | productie | 2 | validat local | `clean build` |
| KOT-073 | `progression` stored progression model | productie | 2 | validat local | `clean build` |
| KOT-074 | `progression` stored progression summary model | productie | 2 | validat local | `clean build` |
| KOT-075 | `progression` status snapshot model | productie | 2 | validat local | `clean build` |
| KOT-076 | `progression` progress snapshot model | productie | 2 | validat local | `clean build` |
| KOT-077 | `progression` definition model | productie | 3 | validat local | `clean build` |
| KOT-078 | `progression` gui entry model | productie | 3 | validat local | `clean build` |
| KOT-079 | `progression` filter utility | productie | 3 | validat local | `clean build` |
| KOT-080 | `managers` family member record | productie | 1 | validat local | `clean build` |
| KOT-081 | `engine.runtime` handler interfaces + registries | productie | 2 | validat local | `clean build` |
| KOT-082 | `engine.runtime` definition model | productie | 2 | validat local | `clean build` |
| KOT-083 | `engine.runtime` validation report model | productie | 2 | validat local | `clean build` |
| KOT-084 | `engine.runtime` execution context model | productie | 2 | validat local | `clean build` |
| KOT-085 | `engine.runtime` base registry | productie | 3 | validat local | `clean build` |
| KOT-086 | `platform` profile model | productie | 1 | validat local | `clean build` |
| KOT-087 | `listeners` registry | productie | 1 | validat local | `clean build` |
| KOT-088 | `listeners` abstract base listener | productie | 2 | validat local | `clean build` |
| KOT-089 | `engine` quest director request model | productie | 2 | validat local | `clean build` |
| KOT-090 | `engine` quest director decision model | productie | 3 | validat local | `clean build` |
| KOT-091 | `bootstrap` scheduler coordinator | productie | 3 | validat local | `clean build` |
| KOT-092 | `platform` main adapter | productie | 3 | validat local | `clean build` |
| KOT-093 | `listeners` player join listener | productie | 2 | validat local | `clean build` |
| KOT-094 | `engine` dependency validator util | productie | 2 | validat local | `clean build` |
| KOT-095 | `engine` quest decision intent resolver | productie | 2 | validat local | `clean build` |
| KOT-096 | `utils` npc name generator | productie | 1 | validat local | `clean build` |
| KOT-097 | `engine` feature pack metadata validator | productie | 2 | validat local | `clean build` |
| KOT-098 | `managers` conversation session manager | productie | 1 | validat local | `clean build` |
| KOT-099 | `listeners` villager lifecycle listener | productie | 2 | validat local | `clean build` |
| KOT-100 | `listeners` quest objective listener | productie | 2 | validat local | `clean build` |
| KOT-101 | `listeners` mapping wand listener | productie | 2 | validat local | `clean build` |
| KOT-102 | `world.scan` vanilla village scanner | productie | 2 | validat local | `clean build` |
| KOT-103 | `routine` routine service | productie | 3 | validat local | `clean build` |
| KOT-104 | `spawn` npc spawn plan model | productie | 2 | validat local | `clean build` |
| KOT-105 | `routine` routine engine | productie | 2 | validat local | `clean build` |
| KOT-106 | `listeners` npc interaction listener | productie | 3 | validat local | `clean build` |
| KOT-107 | `world` npc world binding service | productie | 3 | validat local | `clean build` |
| KOT-108 | `utils` message utils | productie | 2 | validat local | `clean build` |
| KOT-109 | `engine` quest director service | productie | 3 | validat local | `clean build` |
| KOT-110 | `ai` npc fact resolver | productie | 2 | validat local | `clean build` |
| KOT-111 | `addons` addon registry | productie | 3 | validat local | `clean build` |
| KOT-112 | `spawn` house allocation model | productie | 3 | validat local | `clean build` |
| KOT-113 | `engine` quest scenario contract | productie | 3 | validat local | `clean build` |
| KOT-114 | `managers` emotion manager | productie | 3 | validat local | `clean build` |
| KOT-115 | `listeners` npc chat listener | productie | 4 | validat local | `clean build` |
| KOT-116 | `progression` progression repository | productie | 4 | validat local | `clean build` |
| KOT-117 | `progression` progression service | productie | 4 | validat local | `clean build` |
| KOT-118 | `ai` dialog manager | productie | 4 | validat local | `clean build` |
| KOT-119 | `story` story context service | productie | 4 | validat local | `clean build` |
| KOT-120 | `story` story state service | productie | 4 | validat local | `clean build` |
| KOT-121 | `engine` quest anchor resolver | productie | 4 | validat local | `clean build` |
| KOT-122 | `gui` gui service | productie | 4 | validat local | `clean build` |
| KOT-123 | `spawn` spawn batch tracker | productie | 4 | validat local | `clean build` |
| KOT-124 | `spawn` house allocation validator | productie | 4 | validat local | `clean build` |
| KOT-125 | `spawn` house allocation planner | productie | 4 | validat local | `clean build` |
| KOT-126 | `world.scan` semantic village mapper | productie | 4 | validat local | `clean build` |
| KOT-127 | `managers` memory manager | productie | 3 | validat local | `clean build` |
| KOT-128 | `engine` dialogue engine | productie | 4 | validat local | `clean build` |
| KOT-129 | `engine` decision engine | productie | 4 | validat local | `clean build` |
| KOT-130 | `database` database manager | productie | 4 | validat local | `clean build` |
| KOT-131 | `managers` family manager | productie | 4 | validat local | `clean build` |
| KOT-132 | `core` plugin entrypoint | productie | 4 | validat local | `clean build` |

## Template pentru un slice

```text
ID:
Data:
Autor:
Zona:
Fisiere adaugate:
Fisiere sterse:
Fisiere modificate:
Tip:
Risc:
Motiv:
Compatibilitate Java:
Teste rulate:
Build rulat:
JAR audit:
Smoke Paper:
Rollback:
Status:
Observatii:
```

## Slice-uri executate

### KOT-001

```text
ID: KOT-001
Data: 2026-05-16
Autor: local
Zona: Gradle / ainpc-core-plugin
Fisiere adaugate: -
Fisiere sterse: -
Fisiere modificate: settings.gradle, build.gradle, gradle.properties, ainpc-core-plugin/build.gradle, .gitignore
Tip: build
Risc: 2
Motiv: activeaza Kotlin doar in modulul core, fara productie Kotlin
Compatibilitate Java: ainpc-api si ainpc-scenario-medieval raman Java-only
Teste rulate: .\gradlew.bat clean build
Build rulat: .\gradlew.bat clean build
JAR audit: plugin.yml prezent, version 1.0.0, runtime Kotlin prezent, fara clase Kotlin de productie
Smoke Paper: nu este necesar pana la prima clasa Kotlin de productie
Rollback: scoate pluginul Kotlin din core, kotlinVersion si configurarea Kotlin din root
Status: validat local
Observatii: Kotlin runtime intra in JAR-ul core prin runtimeClasspath; acceptat pentru pornirea migrarii.
```

### KOT-002

```text
ID: KOT-002
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/test/kotlin
Fisiere adaugate: ainpc-core-plugin/src/test/kotlin/ro/ainpc/kotlincheck/KotlinToolchainTest.kt
Fisiere sterse: -
Fisiere modificate: -
Tip: test
Risc: 1
Motiv: confirma ca JUnit 5 ruleaza teste Kotlin
Compatibilitate Java: nu expune API si nu schimba productie
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "*KotlinToolchainTest"
Build rulat: .\gradlew.bat clean build
JAR audit: test-only; JAR core nu contine clase Kotlin de productie
Smoke Paper: nu este necesar
Rollback: sterge testul Kotlin; daca nu exista alte fisiere Kotlin, se poate dezactiva pluginul
Status: validat local
Observatii: primul fisier .kt din repo este test-only.
```

### KOT-003

```text
ID: KOT-003
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/test/kotlin/ro/ainpc/utils
Fisiere adaugate: ainpc-core-plugin/src/test/kotlin/ro/ainpc/utils/NPCNameGeneratorTest.kt
Fisiere sterse: ainpc-core-plugin/src/test/java/ro/ainpc/utils/NPCNameGeneratorTest.java
Fisiere modificate: docs/kotlin-migration-tracker.md
Tip: test
Risc: 1
Motiv: converteste un test simplu de utilitar fara schimbare de productie
Compatibilitate Java: nu expune API si nu schimba clasele Java de productie
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.utils.NPCNameGeneratorTest"; .\gradlew.bat :ainpc-core-plugin:test
Build rulat: .\gradlew.bat clean build
JAR audit: nu este necesar, test-only
Smoke Paper: nu este necesar
Rollback: sterge testul Kotlin si readauga testul Java din istoric
Status: validat local
Observatii: conversie test-only; avertismentul JDK 25 pentru SQLite native access nu blocheaza build-ul.
```

### KOT-004

```text
ID: KOT-004
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/test/kotlin/ro/ainpc/progression
Fisiere adaugate: ainpc-core-plugin/src/test/kotlin/ro/ainpc/progression/ProgressionSelectorTest.kt
Fisiere sterse: ainpc-core-plugin/src/test/java/ro/ainpc/progression/ProgressionSelectorTest.java
Fisiere modificate: docs/kotlin-migration-tracker.md
Tip: test
Risc: 1
Motiv: converteste un test de selector fara schimbare de productie
Compatibilitate Java: nu expune API si nu schimba comportamentul clasei testate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.progression.ProgressionSelectorTest"; .\gradlew.bat :ainpc-core-plugin:test
Build rulat: .\gradlew.bat clean build
JAR audit: nu este necesar, test-only
Smoke Paper: nu este necesar
Rollback: sterge testul Kotlin si readauga testul Java din istoric
Status: validat local
Observatii: testele confirma in continuare selectorii simpli, selectorii calificati si aliasurile tracked.
```

### KOT-005

```text
ID: KOT-005
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/progression
Fisiere adaugate: ainpc-core-plugin/src/main/kotlin/ro/ainpc/progression/ProgressionSelector.kt
Fisiere sterse: ainpc-core-plugin/src/main/java/ro/ainpc/progression/ProgressionSelector.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste un selector mic, acoperit direct de teste si folosit din Java
Compatibilitate Java: metodele statice sunt expuse cu @JvmStatic; accesoriile Java-style raw(), definitionId(), commandSelector(), isTrackedAlias() sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.progression.ProgressionSelectorTest"; .\gradlew.bat :ainpc-core-plugin:test --tests "*Progression*"
Build rulat: .\gradlew.bat clean build
JAR audit: ProgressionSelector.class, ProgressionSelector$Companion.class, plugin.yml si runtime Kotlin prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge ProgressionSelector.kt si readauga ProgressionSelector.java din istoric
Status: validat local
Observatii: prima clasa Kotlin de productie din core; build-ul complet trece cu avertismentul cunoscut SQLite/JDK 25.
```

### KOT-009

```text
ID: KOT-009
Data: 2026-05-16
Autor: local
Zona: packaging / ainpc-core-plugin
Fisiere adaugate: -
Fisiere sterse: -
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: packaging
Risc: 3
Motiv: verifica JAR-ul dupa prima clasa Kotlin de productie
Compatibilitate Java: nu schimba cod; confirma doar continutul artefactului
Teste rulate: incluse in .\gradlew.bat clean build
Build rulat: .\gradlew.bat clean build
JAR audit: plugin.yml prezent; clasele ai/orchestration Kotlin, clasele debug convertite, clasele spawn convertite, clasele world.mapping convertite, clasele world.scan convertite, clasele routine convertite, clasele gui convertite, ProgressionSelector.class, QuestTemplateSelector.class, PatchBuildMode.class, PatchGapType.class, PatchType.class, PatchValidationStatus.class, kotlin/KotlinVersion.class si kotlin/jvm/internal/Intrinsics.class prezente
Smoke Paper: nu a fost rulat local; ramane planificat in KOT-010
Rollback: nu necesita rollback de cod; daca lipsesc clasele, revizuieste jar/runtimeClasspath
Status: validat local
Observatii: audit facut prin API-ul ZIP PowerShell deoarece comanda jar nu este pe PATH; dupa KOT-062 sunt prezente clase Kotlin din ai/orchestration, debug, engine, gui, npc, progression, routine, story, world, world.mapping, world.patch, world.scan si spawn.
```

### KOT-006

```text
ID: KOT-006
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/patch
Fisiere adaugate: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/patch/PatchBuildMode.kt, ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/patch/PatchType.kt
Fisiere sterse: ainpc-core-plugin/src/main/java/ro/ainpc/world/patch/PatchBuildMode.java, ainpc-core-plugin/src/main/java/ro/ainpc/world/patch/PatchType.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste doua enum-uri mici din planner, pastrand metoda Java-style id()
Compatibilitate Java: constantele enum raman aceleasi; metoda id() este pastrata explicit pentru apelurile Java existente
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.world.VillagePatchPlannerTest"; .\gradlew.bat :ainpc-core-plugin:test
Build rulat: .\gradlew.bat clean build
JAR audit: PatchBuildMode.class si PatchType.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierele Kotlin si readauga enum-urile Java din istoric
Status: validat local
Observatii: build-ul complet trece cu avertismentul cunoscut SQLite/JDK 25.
```

### KOT-007

```text
ID: KOT-007
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine
Fisiere adaugate: ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/QuestTemplateSelector.kt
Fisiere sterse: ainpc-core-plugin/src/main/java/ro/ainpc/engine/QuestTemplateSelector.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste selectorul mic de template-uri quest, fara a atinge ScenarioEngine
Compatibilitate Java: metodele statice sunt pastrate prin @JvmStatic; clasa devine publica in bytecode Kotlin
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.engine.QuestTemplateSelectorTest"; .\gradlew.bat :ainpc-core-plugin:test --tests "*Quest*"
Build rulat: .\gradlew.bat clean build
JAR audit: QuestTemplateSelector.class si QuestTemplateSelector$Companion.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga QuestTemplateSelector.java din istoric
Status: validat local
Observatii: comportamentul de selectare si normalizare progression kind ramane acoperit de testele existente.
```

### KOT-008

```text
ID: KOT-008
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn
Fisiere adaugate: ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/SpawnBatchPlanHasher.kt
Fisiere sterse: ainpc-core-plugin/src/main/java/ro/ainpc/spawn/SpawnBatchPlanHasher.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste hasher-ul determinist pentru batch-uri spawn
Compatibilitate Java: metodele publice statice sunt pastrate prin @JvmStatic pe object Kotlin
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.spawn.SpawnBatchPlanHasherTest"; .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.spawn.*"
Build rulat: .\gradlew.bat clean build
JAR audit: SpawnBatchPlanHasher.class prezent in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga SpawnBatchPlanHasher.java din istoric
Status: validat local
Observatii: hash-urile raman stabile pentru reordonarea alocarilor si cheile dry-run raman separate.
```

### KOT-011

```text
ID: KOT-011
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui
Fisiere adaugate: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/GuiKey.kt
Fisiere sterse: ainpc-core-plugin/src/main/java/ro/ainpc/gui/GuiKey.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste enum-ul de chei GUI si aliasurile lui
Compatibilitate Java: constantele enum raman aceleasi; id(), displayName() si fromId() sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.GuiKeyTest" --tests "ro.ainpc.gui.QuestLogGuiFilterTest"; .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"
Build rulat: .\gradlew.bat clean build
JAR audit: GuiKey.class si GuiKey$Companion.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga GuiKey.java din istoric
Status: validat local
Observatii: aliasurile pentru progression/quest GUI raman acoperite de test.
```

### KOT-012

```text
ID: KOT-012
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui
Fisiere adaugate: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/QuestLogGuiFilter.kt
Fisiere sterse: ainpc-core-plugin/src/main/java/ro/ainpc/gui/QuestLogGuiFilter.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste filtrele GUI pentru quest/progression log
Compatibilitate Java: constantele enum raman aceleasi; filter(), buttonLabel(), displayLabel(), matches(), primaryFilters(), normalizeFilter() si fromId() sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.GuiKeyTest" --tests "ro.ainpc.gui.QuestLogGuiFilterTest"; .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"
Build rulat: .\gradlew.bat clean build
JAR audit: QuestLogGuiFilter.class si QuestLogGuiFilter$Companion.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga QuestLogGuiFilter.java din istoric
Status: validat local
Observatii: ordinea primaryFilters si fallback-ul pentru filtre necunoscute raman acoperite de test.
```

### KOT-013

```text
ID: KOT-013
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/patch
Fisiere adaugate: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/patch/PatchGapType.kt, ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/patch/PatchValidationStatus.kt
Fisiere sterse: ainpc-core-plugin/src/main/java/ro/ainpc/world/patch/PatchGapType.java, ainpc-core-plugin/src/main/java/ro/ainpc/world/patch/PatchValidationStatus.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 1
Motiv: converteste doua enum-uri simple folosite de plannerul de patch-uri
Compatibilitate Java: constantele enum raman identice
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.world.VillagePatchPlannerTest"; .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.world.*"
Build rulat: .\gradlew.bat clean build
JAR audit: PatchGapType.class si PatchValidationStatus.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierele Kotlin si readauga enum-urile Java din istoric
Status: validat local
Observatii: conversie mecanica fara metode suplimentare.
```

### KOT-014

```text
ID: KOT-014
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/orchestration
Fisiere adaugate: AIOutputType.kt, AIResultStatus.kt, AIUseCase.kt, AIOrchestrationPolicy.kt
Fisiere sterse: AIOutputType.java, AIResultStatus.java, AIUseCase.java, AIOrchestrationPolicy.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste enum-urile si politica determinista AI orchestration
Compatibilitate Java: constantele enum raman identice; AIOrchestrationPolicy pastreaza constructorul, metodele record-style si forUseCase() prin @JvmStatic
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.ai.orchestration.AIOrchestrationServiceTest"; .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.ai.*"
Build rulat: .\gradlew.bat clean build
JAR audit: AIOutputType.class, AIResultStatus.class, AIUseCase.class si AIOrchestrationPolicy.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierele Kotlin si readauga fisierele Java din istoric
Status: validat local
Observatii: politica QUEST_DRAFT ramane acoperita de testul de orchestration.
```

### KOT-015

```text
ID: KOT-015
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/orchestration
Fisiere adaugate: AIOrchestrationRequest.kt, AIOrchestrationResult.kt
Fisiere sterse: AIOrchestrationRequest.java, AIOrchestrationResult.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste request/result cu sanitizare si fallback-uri deterministe
Compatibilitate Java: metodele record-style useCase(), actorId(), context(), status(), outputType(), fallbackUsed(), runtimeExecutable() si errorCode() sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.ai.orchestration.AIOrchestrationServiceTest"; .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.ai.*"
Build rulat: .\gradlew.bat clean build
JAR audit: AIOrchestrationRequest.class si AIOrchestrationResult.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierele Kotlin si readauga record-urile Java din istoric
Status: validat local
Observatii: testul confirma trim pentru campuri, ignorarea cheilor goale si context imutabil din Java.
```

### KOT-016

```text
ID: KOT-016
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/orchestration
Fisiere adaugate: AIOrchestrationService.kt
Fisiere sterse: AIOrchestrationService.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: finalizeaza pachetul ai/orchestration in Kotlin
Compatibilitate Java: constructorul public si metodele policyFor(), orchestrate(), fallback() si enabled() sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.ai.orchestration.AIOrchestrationServiceTest"; .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.ai.*"
Build rulat: .\gradlew.bat clean build
JAR audit: AIOrchestrationService.class prezent in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga AIOrchestrationService.java din istoric
Status: validat local
Observatii: fallback-ul determinist ramane acoperit de testul de orchestration.
```

### KOT-017

```text
ID: KOT-017
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/mapping
Fisiere adaugate: MappingDraftKind.kt, MappingWandMode.kt
Fisiere sterse: MappingDraftKind.java, MappingWandMode.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste enum-urile pentru mapping draft si wand
Compatibilitate Java: constantele enum raman identice; id(), fromId(), draftKind() si usesPointSelection() sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.world.mapping.MappingIntentParserTest" --tests "ro.ainpc.world.MappingDraftFactoryTest"; .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.world.*"
Build rulat: .\gradlew.bat clean build
JAR audit: MappingDraftKind.class si MappingWandMode.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierele Kotlin si readauga enum-urile Java din istoric
Status: validat local
Observatii: MappingWandMode pastreaza maparea catre MappingDraftKind.
```

### KOT-018

```text
ID: KOT-018
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/mapping
Fisiere adaugate: MappingPoint.kt, MappingDraftApplyResult.kt
Fisiere sterse: MappingPoint.java, MappingDraftApplyResult.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modele mici folosite de mapping factory si comenzi
Compatibilitate Java: constructorii si metodele record-style worldName(), x(), y(), z(), kind(), createdId() si message() sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.world.mapping.MappingIntentParserTest" --tests "ro.ainpc.world.MappingDraftFactoryTest"; .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.world.*"
Build rulat: .\gradlew.bat clean build
JAR audit: MappingPoint.class si MappingDraftApplyResult.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierele Kotlin si readauga record-urile Java din istoric
Status: validat local
Observatii: MappingPoint pastreaza trim pe worldName si format().
```

### KOT-019

```text
ID: KOT-019
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/mapping
Fisiere adaugate: MappingWandSelection.kt, MappingDraftSuggestion.kt
Fisiere sterse: MappingWandSelection.java, MappingDraftSuggestion.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste selection/suggestion pentru mapping wand si intent parser
Compatibilitate Java: MappingWandSelection.empty(), withPos1(), withPos2(), withPoint(), bounds(), hasPoint() si nested MappingBounds sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.world.mapping.MappingIntentParserTest" --tests "ro.ainpc.world.MappingDraftFactoryTest"; .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.world.*"
Build rulat: .\gradlew.bat clean build
JAR audit: MappingWandSelection.class, MappingWandSelection$MappingBounds.class si MappingDraftSuggestion.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierele Kotlin si readauga record-urile Java din istoric
Status: validat local
Observatii: bounds() si formatul MappingBounds raman validate indirect prin MappingDraftFactoryTest.
```

### KOT-020

```text
ID: KOT-020
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/scan
Fisiere adaugate: VanillaVillageFeatureType.kt, VanillaVillageFeature.kt, VanillaVillageScanResult.kt, SemanticVillageImportResult.kt
Fisiere sterse: VanillaVillageFeatureType.java, VanillaVillageFeature.java, VanillaVillageScanResult.java, SemanticVillageImportResult.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modelele mici folosite de scanarea si importul semantic vanilla village
Compatibilitate Java: constantele enum si metodele record-style type(), material(), x(), byType(), count(), success() etc. sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.world.SemanticVillageMapperTest"; .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.world.*"
Build rulat: .\gradlew.bat clean build
JAR audit: VanillaVillageFeatureType.class, VanillaVillageFeature.class, VanillaVillageScanResult.class si SemanticVillageImportResult.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierele Kotlin si readauga fisierele Java din istoric
Status: validat local
Observatii: SemanticVillageMapper si VanillaVillageScanner raman Java pentru moment.
```

### KOT-021

```text
ID: KOT-021
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/routine
Fisiere adaugate: RoutineSlot.kt, RoutineAssignment.kt, RoutineScheduleEntry.kt, RoutineTickSummary.kt
Fisiere sterse: RoutineSlot.java, RoutineAssignment.java, RoutineScheduleEntry.java, RoutineTickSummary.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modelele mici folosite de engine-ul de rutina
Compatibilitate Java: constantele enum, constructorii si metodele record-style slot(), activity(), assignment(), disabled() etc. sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.routine.RoutineEngineTest"; .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.routine.*"
Build rulat: .\gradlew.bat clean build
JAR audit: RoutineSlot.class, RoutineAssignment.class, RoutineScheduleEntry.class si RoutineTickSummary.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierele Kotlin si readauga fisierele Java din istoric
Status: validat local
Observatii: RoutineEngine si RoutineService raman Java pentru moment.
```

### KOT-022

```text
ID: KOT-022
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn
Fisiere adaugate: FamilyBindingResult.kt, FamilyBindingPlan.kt, HouseAllocationValidationResult.kt
Fisiere sterse: FamilyBindingResult.java, FamilyBindingPlan.java, HouseAllocationValidationResult.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modelele de validare casa si family binding
Compatibilitate Java: constructorii, FamilyBindingPlan.member(), FamilyBindingPlan.Member si metodele record-style sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.spawn.HouseAllocationValidatorTest"; .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.spawn.*"; .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.world.*"
Build rulat: .\gradlew.bat clean build
JAR audit: FamilyBindingResult.class, FamilyBindingPlan.class, FamilyBindingPlan$Member.class si HouseAllocationValidationResult.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierele Kotlin si readauga fisierele Java din istoric
Status: validat local
Observatii: valid este calculat in continuare din lista de erori, ca in record-ul Java original.
```

### KOT-023

```text
ID: KOT-023
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn
Fisiere adaugate: NpcSpawnResult.kt, ResolvedNpcSpawnPlan.kt, HouseholdSpawnResult.kt, SettlementSpawnResult.kt
Fisiere sterse: NpcSpawnResult.java, ResolvedNpcSpawnPlan.java, HouseholdSpawnResult.java, SettlementSpawnResult.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste rezultatele spawn si planul spawn rezolvat
Compatibilitate Java: metodele statice created(), reused(), failed(), dryRunSuccess(), success() si metodele record-style sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.spawn.*"; .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.world.*"
Build rulat: .\gradlew.bat clean build
JAR audit: NpcSpawnResult.class, ResolvedNpcSpawnPlan.class, HouseholdSpawnResult.class si SettlementSpawnResult.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierele Kotlin si readauga fisierele Java din istoric
Status: validat local
Observatii: ResolvedNpcSpawnPlan pastreaza verificarea plan != null si cloneaza Location la constructie.
```

### KOT-024

```text
ID: KOT-024
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui
Fisiere adaugate: GuiAction.kt, GuiButton.kt, GuiClickContext.kt, GuiNavigation.kt, GuiRenderContext.kt, GuiScreen.kt, GuiSession.kt, GuiSessionManager.kt
Fisiere sterse: GuiAction.java, GuiButton.java, GuiClickContext.java, GuiNavigation.java, GuiRenderContext.java, GuiScreen.java, GuiSession.java, GuiSessionManager.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste primitivele GUI si managementul simplu de sesiune fara a muta serviciul GUI mare
Compatibilitate Java: GuiAction ramane functional interface; GuiButton pastreaza enabled(), disabled(), icon(), action() si enabled(); GuiClickContext pastreaza metodele record-style; GuiNavigation expune addStandardControls() prin @JvmStatic
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"
Build rulat: .\gradlew.bat clean build
JAR audit: GuiAction.class, GuiButton.class, GuiClickContext.class, GuiNavigation.class, GuiRenderContext.class, GuiScreen.class, GuiSession.class, GuiSessionManager.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierele Kotlin si readauga fisierele Java GUI din istoric
Status: validat local
Observatii: GuiService si ecranele concrete raman Java pentru moment; lambdas Java catre GuiAction au fost validate prin compilare si testele GUI.
```

### KOT-025

```text
ID: KOT-025
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/debug
Fisiere adaugate: WorldMappingSemanticIndex.kt
Fisiere sterse: WorldMappingSemanticIndex.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste indexul semantic de mapping folosit pentru validari/debug
Compatibilitate Java: metodele record-style regionCandidates(), placeCandidates(), nodeCandidates(), placeTags(), placeTypes(), nodeTypes(), nodeMetadataValues() sunt pastrate; from() ramane static prin @JvmStatic
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.debug.WorldMappingSemanticIndexTest"
Build rulat: .\gradlew.bat clean build
JAR audit: WorldMappingSemanticIndex.class, WorldMappingSemanticIndex$Companion.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga WorldMappingSemanticIndex.java din istoric
Status: validat local
Observatii: sortarea interna a ID-urilor a fost adaptata la Kotlin (sortWith) pentru a evita API depreciat tratat ca eroare la compilare.
```

### KOT-026

```text
ID: KOT-026
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui
Fisiere adaugate: QuestLogGuiPage.kt
Fisiere sterse: QuestLogGuiPage.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste paginatorul de quest log GUI cu grupare pe mecanica si paginare pe randuri
Compatibilitate Java: metodele record-style rows(), pageIndex(), pageCount(), totalRows(), totalEntries(), displayPage(), hasPrevious(), hasNext() sunt pastrate; fromEntries() ramane static prin @JvmStatic; QuestLogGuiPage.Row pastreaza header()/entry() statice
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.QuestLogGuiPageTest"; .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"
Build rulat: .\gradlew.bat clean build
JAR audit: QuestLogGuiPage.class, QuestLogGuiPage$Row.class, QuestLogGuiPage$Companion.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga QuestLogGuiPage.java din istoric
Status: validat local
Observatii: logica de evitare a header-ului orfan si repetarea header-ului la split pe pagina este pastrata si acoperita de testele existente.
```

### KOT-027

```text
ID: KOT-027
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui
Fisiere adaugate: GuiItemFactory.kt
Fisiere sterse: GuiItemFactory.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste factory-ul de iteme GUI folosit transversal in ecrane si mapping wand
Compatibilitate Java: metodele statice item(...), disabled(...), filler(), text(), wrapLore(...), compact(), stripLegacy() sunt pastrate prin @JvmStatic
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"
Build rulat: .\gradlew.bat clean build
JAR audit: GuiItemFactory.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga GuiItemFactory.java din istoric
Status: validat local
Observatii: executiile Gradle au fost rulate serial pentru a evita erori false generate de doua build-uri pornite in paralel pe acelasi workspace.
```

### KOT-028

```text
ID: KOT-028
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui
Fisiere adaugate: AINPCGuiHolder.kt
Fisiere sterse: AINPCGuiHolder.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 1
Motiv: converteste holder-ul de inventar pentru sesiunea GUI
Compatibilitate Java: metodele sessionId(), key(), attach() si getInventory() sunt pastrate; clasa implementeaza in continuare InventoryHolder
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"
Build rulat: .\gradlew.bat clean build
JAR audit: AINPCGuiHolder.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga AINPCGuiHolder.java din istoric
Status: validat local
Observatii: getInventory() este non-null in semnatura Kotlin pentru compatibilitate stricta cu InventoryHolder.
```

### KOT-029

```text
ID: KOT-029
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/listeners
Fisiere adaugate: GuiInventoryListener.kt
Fisiere sterse: GuiInventoryListener.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste listener-ul GUI pentru click/drag/close/quit
Compatibilitate Java: comportamentul ramane identic pentru anulare evenimente GUI, forward catre GuiService si curatare sesiune pe quit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"
Build rulat: .\gradlew.bat clean build
JAR audit: GuiInventoryListener.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga GuiInventoryListener.java din istoric
Status: validat local
Observatii: listener-ul foloseste in continuare aceeasi conditie de boundary pe rawSlot si aceeasi inchidere de sesiune pe InventoryCloseEvent.
```

### KOT-030

```text
ID: KOT-030
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/screens
Fisiere adaugate: PlaceholderGui.kt
Fisiere sterse: PlaceholderGui.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 1
Motiv: converteste ecranul placeholder folosit pentru zone GUI fara provider activ
Compatibilitate Java: comportamentul de titlu, size=54, item central, controale standard si filler ramane identic
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"
Build rulat: .\gradlew.bat clean build
JAR audit: PlaceholderGui.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga PlaceholderGui.java din istoric
Status: validat local
Observatii: constructorul ramas simplu permite in continuare inregistrarea din GuiService fara schimbari.
```

### KOT-031

```text
ID: KOT-031
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/screens
Fisiere adaugate: AuditGui.kt
Fisiere sterse: AuditGui.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 1
Motiv: converteste ecranul de audit operational din hub-ul GUI
Compatibilitate Java: cheia GUI, titlul, sloturile butoanelor, comenzile /ainpc audit <mode> si controalele standard raman identice
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"
Build rulat: .\gradlew.bat clean build
JAR audit: AuditGui.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga AuditGui.java din istoric
Status: validat local
Observatii: butoanele raman construite prin GuiButton.enabled + runCommand in acelasi mod ca varianta Java.
```

### KOT-032

```text
ID: KOT-032
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/screens
Fisiere adaugate: DebugGui.kt
Fisiere sterse: DebugGui.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 1
Motiv: converteste ecranul de debug operational din hub-ul GUI
Compatibilitate Java: sloturile, scope-urile debugdump, butonul de test OpenAI si comenzile executate raman identice
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"
Build rulat: .\gradlew.bat clean build
JAR audit: DebugGui.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga DebugGui.java din istoric
Status: validat local
Observatii: builder-ele de butoane folosesc aceeasi compozitie GuiButton.enabled + GuiItemFactory.item ca in Java.
```

### KOT-033

```text
ID: KOT-033
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/screens
Fisiere adaugate: StatsGui.kt
Fisiere sterse: StatsGui.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste ecranul de statistici jucator/NPC din hub-ul GUI
Compatibilitate Java: sloturile, comenziile asociate butoanelor, sortarea NPC-urilor si limita de 14 intrari raman identice
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"
Build rulat: .\gradlew.bat clean build
JAR audit: StatsGui.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga StatsGui.java din istoric
Status: validat local
Observatii: snapshot-ul din GUI pastreaza formatul health cu Locale.ROOT si aceleasi campuri afisate in lore.
```

### KOT-034

```text
ID: KOT-034
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/screens
Fisiere adaugate: ConfirmActionGui.kt
Fisiere sterse: ConfirmActionGui.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 1
Motiv: converteste ecranul de confirmare pentru comenzi operationale
Compatibilitate Java: fallback-ul pentru cerere expirata, butonul inapoi, butoanele confirma/anuleaza si executia comenzilor raman identice
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"
Build rulat: .\gradlew.bat clean build
JAR audit: ConfirmActionGui.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga ConfirmActionGui.java din istoric
Status: validat local
Observatii: lore-ul include in continuare linia de comanda „/&lt;command&gt;” construita din request.command().
```

### KOT-035

```text
ID: KOT-035
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/screens
Fisiere adaugate: MainHubGui.kt
Fisiere sterse: MainHubGui.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste ecranul principal de navigare GUI
Compatibilitate Java: sloturile, butoanele de navigare, verificarea de permisiuni, fallback-ul locked si comenzile rapide raman identice
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"
Build rulat: .\gradlew.bat clean build
JAR audit: MainHubGui.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga MainHubGui.java din istoric
Status: validat local
Observatii: compozitia lore pentru snapshot hub (NPC count + world mapping + locatie) este pastrata in aceeasi ordine.
```

### KOT-036

```text
ID: KOT-036
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/screens
Fisiere adaugate: NpcManagerGui.kt
Fisiere sterse: NpcManagerGui.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste ecranul de administrare NPC (listare, info, tp, routine, familie)
Compatibilitate Java: sloturile, combinatiile click/right/shift, comenzile rulate si limita afisata raman identice
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"
Build rulat: .\gradlew.bat clean build
JAR audit: NpcManagerGui.class, NpcManagerGui$Companion.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga NpcManagerGui.java din istoric
Status: validat local
Observatii: slot-ul de rutina este tratat null-safe in Kotlin (fallback IDLE) pentru a pastra compilarea stabila fara schimbare functionala.
```

### KOT-037

```text
ID: KOT-037
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/screens
Fisiere adaugate: NpcInteractionGui.kt
Fisiere sterse: NpcInteractionGui.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste ecranul de interactiune NPC cu nearest progression actions
Compatibilitate Java: sloturile, actiunile click/right/shift, selectia progresiei nearest si comenzile generate raman identice
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"
Build rulat: .\gradlew.bat clean build
JAR audit: NpcInteractionGui.class, NpcInteractionGui$Companion.class, NpcInteractionGui$NearbyProgression.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga NpcInteractionGui.java din istoric
Status: validat local
Observatii: prioritatea progresiilor (tracked/current/active/offered/other/archived) ramane neschimbata.
```

### KOT-038

```text
ID: KOT-038
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/screens
Fisiere adaugate: RoutineGui.kt
Fisiere sterse: RoutineGui.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste ecranul de rutina NPC (preview, status, tick, manager shortcuts)
Compatibilitate Java: sloturile, comenzile admin, fallback-urile de permisiune si reprezentarea programului zilnic raman identice
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"
Build rulat: .\gradlew.bat clean build
JAR audit: RoutineGui.class, RoutineGui$Companion.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga RoutineGui.java din istoric
Status: validat local
Observatii: campurile nullable din rutina (slot/assignment/targetState) sunt tratate null-safe pentru compatibilitate la compilare, fara schimbare de flux functional.
```

### KOT-039

```text
ID: KOT-039
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/screens
Fisiere adaugate: QuestLogGui.kt
Fisiere sterse: QuestLogGui.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste ecranul central de progresii (filtre, paginare, tracking, actiuni entry)
Compatibilitate Java: sloturile, materialele, comenzile status/track, paginarea, filtrele si comportamentul click/right/shift sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"
Build rulat: .\gradlew.bat clean build
JAR audit: QuestLogGui.class, QuestLogGui$Companion.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga QuestLogGui.java din istoric
Status: validat local
Observatii: logica grupare+header rows este in continuare delegata la QuestLogGuiPage convertit anterior.
```

### KOT-040

```text
ID: KOT-040
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/screens
Fisiere adaugate: WorldHubGui.kt
Fisiere sterse: WorldHubGui.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste ecranul world hub (context mapping, progresii locale, ancore, comenzi world)
Compatibilitate Java: sloturile, controalele de permisiune, comenzile whereami/scan/demo/save si integrarea cu Story/Quest GUI raman identice
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"
Build rulat: .\gradlew.bat clean build
JAR audit: WorldHubGui.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga WorldHubGui.java din istoric
Status: validat local
Observatii: query-urile pentru anchor bindings pastreaza fallback-ul pe SQLException, cu warning logger fara intreruperea render-ului.
```

### KOT-041

```text
ID: KOT-041
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/screens
Fisiere adaugate: StoryGui.kt
Fisiere sterse: StoryGui.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste ecranul story snapshot (region/place state, events, comenzi story/debugdump)
Compatibilitate Java: sloturile, fallback-urile pentru service indisponibil, formatarea evenimentelor si comenzile text raman identice
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"
Build rulat: .\gradlew.bat clean build
JAR audit: StoryGui.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga StoryGui.java din istoric
Status: validat local
Observatii: filtrarea materialelor pe eventType (quest/complete/ritual/alert/alarm) si compactarea payload-ului au ramas neschimbate.
```

### KOT-042

```text
ID: KOT-042
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/screens
Fisiere adaugate: QuestDetailGui.kt
Fisiere sterse: QuestDetailGui.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 4
Motiv: converteste ecranul detaliat de progresie (objectives/stages/rewards/anchor diagnostics/actions)
Compatibilitate Java: selector fallback all, butoanele de actiuni, confirm abandon, tracking/status/progress/debug si fallback-urile missing selection/quest raman identice
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"
Build rulat: .\gradlew.bat clean build
JAR audit: QuestDetailGui.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga QuestDetailGui.java din istoric
Status: validat local
Observatii: pachetul `gui/screens` din core este acum complet migrat la Kotlin.
```

### KOT-043

```text
ID: KOT-043
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/npc
Fisiere adaugate: NPCState.kt, NPCAction.kt
Fisiere sterse: NPCState.java, NPCAction.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste enum-urile de baza pentru state machine si scoring de actiuni NPC
Compatibilitate Java: getter-ele Java-style pentru displayName/baseScore/description raman disponibile; aliasul static HELP ramane disponibil; semnaturile metodelor de clasificare (isWorkState/isSocialState/etc.) sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks; .\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.routine.RoutineEngineTest"
Build rulat: .\gradlew.bat clean build
JAR audit: NPCState.class, NPCAction.class, NPCAction$ActionCategory.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierele Kotlin si readauga enum-urile Java din istoric
Status: validat local
Observatii: dupa o rulare paralela Gradle care a generat erori false de lock/up-to-date, validarea finala a fost rerulata serial si a trecut.
```

### KOT-044

```text
ID: KOT-044
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/npc
Fisiere adaugate: NPCEmotions.kt
Fisiere sterse: NPCEmotions.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modelul emotional NPC (plutchik), folosit in UI/statistics/dialog context
Compatibilitate Java: proprietatile si getter-ele JavaBean raman disponibile (inclusiv dominantEmotion/dominantEmotionColor); metoda statica getEmotionColor ramane disponibila prin @JvmStatic
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: NPCEmotions.class, NPCEmotions$Companion.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga NPCEmotions.java din istoric
Status: validat local
Observatii: in prima iteratie au aparut unresolved references pe dominantEmotion in GUI; conversia a fost ajustata sa expuna aceeasi proprietate ca in Java interop.
```

### KOT-045

```text
ID: KOT-045
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/npc
Fisiere adaugate: NPCPersonality.kt
Fisiere sterse: NPCPersonality.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modelul Big Five (OCEAN) pentru personalitatea NPC
Compatibilitate Java: constructorul gol si constructorul cu 5 parametri raman disponibili; metodele statice generateRandom() si fromArchetype() raman disponibile prin @JvmStatic; getter/setter JavaBean pentru trait-uri raman compatibile
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: NPCPersonality.class, NPCPersonality$Companion.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga NPCPersonality.java din istoric
Status: validat local
Observatii: logica de clamp, arhetipuri si affinity pe topic a fost pastrata 1:1 functional.
```

### KOT-046

```text
ID: KOT-046
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/npc
Fisiere adaugate: NPCContext.kt
Fisiere sterse: NPCContext.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste contextul runtime NPC (lume, entitati apropiate, relatie jucator, descriere prompt)
Compatibilitate Java: metodele publice folosite extern (updateFromWorld, syncSimulationState, addRecentEvent, generateContextDescription) si getter/setter-urile JavaBean raman disponibile prin proprietatile Kotlin
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: NPCContext.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga NPCContext.java din istoric
Status: validat local
Observatii: in pachetul `ro.ainpc.npc` a ramas Java doar clasa mare `AINPC`.
```

### KOT-047

```text
ID: KOT-047
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world
Fisiere adaugate: WorldNodeType.kt, RegionType.kt
Fisiere sterse: WorldNodeType.java, RegionType.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 1
Motiv: converteste enum-urile de tip pentru noduri si regiuni world mapping
Compatibilitate Java: getter-ul JavaBean getId() ramane disponibil; fromId(String) ramane static prin @JvmStatic
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: WorldNodeType.class, RegionType.class, WorldNodeType$Companion.class, RegionType$Companion.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierele Kotlin si readauga enum-urile Java din istoric
Status: validat local
Observatii: fallback-ul CUSTOM pentru ID gol/necunoscut este pastrat neschimbat.
```

### KOT-048

```text
ID: KOT-048
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world
Fisiere adaugate: StoryState.kt
Fisiere sterse: StoryState.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 1
Motiv: converteste modelul simplu pentru state/pool story in world mapping
Compatibilitate Java: getMode(), getStateKey()/setStateKey(), getStoryPool() si setStoryPool(List) raman disponibile; getStoryPool returneaza in continuare o copie imutabila observabila extern
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: StoryState.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga StoryState.java din istoric
Status: validat local
Observatii: setStoryPool pastreaza copy semantics (clear + addAll) si ignora null input ca in varianta Java.
```

### KOT-049

```text
ID: KOT-049
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world
Fisiere adaugate: NpcWorldBinding.kt
Fisiere sterse: NpcWorldBinding.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modelul de binding NPC -> world places/nodes folosit de serviciul de persistenta si comenzi admin
Compatibilitate Java: accesorii record-style (npcId(), npcUuid(), createdAt(), etc.) sunt pastrate explicit; fromSpawnPlan ramane static prin @JvmStatic
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: NpcWorldBinding.class, NpcWorldBinding$Companion.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga NpcWorldBinding.java din istoric
Status: validat local
Observatii: prima varianta Kotlin a rupt interop-ul cu apeluri Java de tip record accessor; a fost corectata in acelasi slice prin expunerea tuturor metodelor `fieldName()`.
```

### KOT-050

```text
ID: KOT-050
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world
Fisiere adaugate: WorldNode.kt
Fisiere sterse: WorldNode.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modelul de node world folosit in indexare, mapare si demo settlement
Compatibilitate Java: getter-ele JavaBean, putMetadata(...), contains(...), isNear(...), distanceSquared(...) raman disponibile; `placeId` accepta null ca in varianta Java
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: WorldNode.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga WorldNode.java din istoric
Status: validat local
Observatii: prima varianta Kotlin a marcat `placeId` non-null si a cauzat NPE in createDemoSettlement; a fost corectata in acelasi slice prin nullable `placeId`.
```

### KOT-051

```text
ID: KOT-051
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world
Fisiere adaugate: WorldPlace.kt
Fisiere sterse: WorldPlace.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modelul de place pentru mapare regiune, metadata si ownership NPC
Compatibilitate Java: getter-ele JavaBean, setOwnerNpcId(String) tolerant la null, isPublicAccess()/setPublicAccess(boolean), getTags()/setTags(...) si getMetadata()/putMetadata(...) raman disponibile
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: WorldPlace.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga WorldPlace.java din istoric
Status: validat local
Observatii: setOwnerNpcId ramane null-safe explicit pentru a pastra comportamentul Java (null -> string gol).
```

### KOT-052

```text
ID: KOT-052
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world
Fisiere adaugate: WorldRegion.kt
Fisiere sterse: WorldRegion.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modelul de regiune world (boundaries, tags, story state)
Compatibilitate Java: getter-ele JavaBean, setTags(...), contains(...), getStoryState()/setStoryState(...) raman disponibile prin proprietati/methods Kotlin
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: WorldRegion.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga WorldRegion.java din istoric
Status: validat local
Observatii: initializarea implicita StoryState(EVOLUTIVE, "default") este pastrata neschimbata.
```

### KOT-053

```text
ID: KOT-053
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/patch
Fisiere adaugate: PatchPlannerOptions.kt, PatchPlannerResult.kt, VillageGap.kt
Fisiere sterse: PatchPlannerOptions.java, PatchPlannerResult.java, VillageGap.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste primitivele folosite de planner (options/result/gap) fara atingerea planner-ului principal
Compatibilitate Java: accesoriile record-style (fieldName()) sunt pastrate explicit; metodele statice forTargetPopulation(...) / defaultCapabilities() sunt pastrate prin @JvmStatic
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: PatchPlannerOptions.class, PatchPlannerResult.class, VillageGap.class, companion classes, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierele Kotlin si readauga cele 3 fisiere Java din istoric
Status: validat local
Observatii: normalizarea capability/profession si default-urile defensive (target/min/max/errors lists) sunt pastrate.
```

### KOT-054

```text
ID: KOT-054
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/patch
Fisiere adaugate: PatchCandidate.kt, PatchPlan.kt
Fisiere sterse: PatchCandidate.java, PatchPlan.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modelele candidate/plan folosite de planner-ul de patch-uri
Compatibilitate Java: accesoriile record-style (candidateId(), patchId(), type(), priority(), etc.) si metoda valid() sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: PatchCandidate.class, PatchPlan.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierele Kotlin si readauga cele 2 fisiere Java din istoric
Status: validat local
Observatii: fallback-urile defensive pentru liste si campuri text sunt pastrate in constructorul Kotlin.
```

### KOT-055

```text
ID: KOT-055
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/patch
Fisiere adaugate: GapReport.kt
Fisiere sterse: GapReport.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste raportul agregat de gap-uri folosit de analyzer/planner
Compatibilitate Java: accesoriile record-style (regionId(), targetPopulation(), gaps(), errors(), capacityByHouse(), etc.) si metodele success()/hasGaps() sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: GapReport.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga GapReport.java din istoric
Status: validat local
Observatii: normalizarea numerica si defensive copies pentru colectii/map sunt pastrate 1:1 functional.
```

### KOT-056

```text
ID: KOT-056
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world
Fisiere adaugate: WorldContextSnapshot.kt
Fisiere sterse: WorldContextSnapshot.java
Fisiere modificate: ainpc-core-plugin/src/main/kotlin/ro/ainpc/npc/NPCContext.kt, docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste snapshot-ul world context folosit in prompt-uri runtime (region/place/node/bindings/nearby NPCs)
Compatibilitate Java: accesoriile record-style pentru snapshot si nested info classes sunt pastrate; empty(), isEmpty() si toPromptBlock() raman disponibile
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: WorldContextSnapshot.class, nested classes, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga WorldContextSnapshot.java din istoric
Status: validat local
Observatii: in acelasi slice a fost necesara ajustarea apelurilor din NPCContext de la `isEmpty` proprietate la `isEmpty()` metoda.
```

### KOT-057

```text
ID: KOT-057
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world
Fisiere adaugate: MappingIndex.kt
Fisiere sterse: MappingIndex.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste indexul intern pe chunk-uri pentru lookup rapid de regiuni/places/nodes
Compatibilitate Java: semnaturile metodelor package-private (index/find/findNear/indexed*) sunt pastrate functional; logica de chunk normalization si deduplicare LinkedHashSet pentru nearby nodes ramane aceeasi
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: MappingIndex.class, nested ChunkKey class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga MappingIndex.java din istoric
Status: validat local
Observatii: filtrarea pe radius si sortarea dupa distanceSquared in findNodesNear sunt pastrate.
```

### KOT-058

```text
ID: KOT-058
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world
Fisiere adaugate: WorldContextSnapshotBuilder.kt
Fisiere sterse: WorldContextSnapshotBuilder.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste builder-ul care compune contextul world pentru prompt-uri NPC (region/place/nodes/bindings/nearby NPCs)
Compatibilitate Java: constructorul si metoda build(...) raman compatibile; structura warning-urilor si limitele pentru nearby places/nodes/NPCs raman neschimbate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: WorldContextSnapshotBuilder.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga WorldContextSnapshotBuilder.java din istoric
Status: validat local
Observatii: dupa conversie a fost eliminat un warning Kotlin de conditie redundanta in filtrarea nearby NPCs.
```

### KOT-059

```text
ID: KOT-059
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/mapping
Fisiere adaugate: MappingDraft.kt
Fisiere sterse: MappingDraft.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modelul de draft folosit de mapping wand / intent parser
Compatibilitate Java: accesoriile record-style (localId(), qualifiedId(), warnings(), etc.) si helper-ele isNode()/isBox()/isNpcBind()/isQuestAnchor() sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: MappingDraft.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga MappingDraft.java din istoric
Status: validat local
Observatii: normalizarea implicita pentru qualifiedId/displayName/typeId/tags/metadata/warnings/confirmationCommand este pastrata.
```

### KOT-060

```text
ID: KOT-060
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/mapping
Fisiere adaugate: MappingIntentParser.kt
Fisiere sterse: MappingIntentParser.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste parserul de intentii pentru mapping wand (region/place/node/npc_bind/quest_anchor)
Compatibilitate Java: API-ul static suggest(...), slugOrFallback(...), cleanDescription(...), normalize(...) ramane disponibil prin @JvmStatic pe object Kotlin
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: MappingIntentParser.class, companion/object support classes, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga MappingIntentParser.java din istoric
Status: validat local
Observatii: regulile de normalizare diacritice, heuristici tip/place/node si fallback-urile de ID/displayName/radius sunt pastrate.
```

### KOT-061

```text
ID: KOT-061
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/patch
Fisiere adaugate: VillagePatchPlanner.kt
Fisiere sterse: VillagePatchPlanner.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste planner-ul care transforma gap-uri in candidate + patch plan-uri validate
Compatibilitate Java: API-ul public plan(...) ramane neschimbat; regulile patchType/buildMode/capabilities/template/cost/risk raman identice
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: VillagePatchPlanner.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga VillagePatchPlanner.java din istoric
Status: validat local
Observatii: ordinea sortarii candidate-urilor (priority desc, risk asc, candidateId asc) si limita maxPatchCount sunt pastrate.
```

### KOT-062

```text
ID: KOT-062
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/patch
Fisiere adaugate: VillageGapAnalyzer.kt
Fisiere sterse: VillageGapAnalyzer.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 4
Motiv: converteste analyzer-ul principal de gap-uri (capacity/work/social/quest anchors) folosit de planner
Compatibilitate Java: API-ul public analyze(...) ramane neschimbat; regulile de clasificare house/work/social/profession si severitatile gap-urilor sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: VillageGapAnalyzer.class, plugin.yml si kotlin/jvm/internal/Intrinsics.class prezente in ainpc-core-plugin-1.0.0.jar
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga VillageGapAnalyzer.java din istoric
Status: validat local
Observatii: pachetul `world.patch` din core este acum complet migrat la Kotlin.
```

### KOT-063

```text
ID: KOT-063
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/mapping
Fisiere adaugate: MappingDraftFactory.kt
Fisiere sterse: MappingDraftFactory.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 4
Motiv: converteste factory-ul principal de draft/apply pentru world mapping (region/place/node/npc_bind/quest_anchor)
Compatibilitate Java: API-ul public createDraft(...) si apply(...) ramane neschimbat; regulile de validare, id-uri, warnings si metadata sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga MappingDraftFactory.java din istoric
Status: validat local
Observatii: au fost necesare ajustari Kotlin pentru nullability pe point() si setter boolean (`isPublicAccess`) ca sa pastreze interop-ul Java.
```

### KOT-064

```text
ID: KOT-064
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/mapping
Fisiere adaugate: MappingWandService.kt
Fisiere sterse: MappingWandService.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 4
Motiv: converteste serviciul principal pentru sesiuni wand, preview particule si confirmare draft-uri mapping
Compatibilitate Java: API-ul public pentru sesiuni/mode/selectie/draft/confirmare ramane neschimbat; accesoriile record-style pentru MappingWandSession si MappingWandAuditEntry sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga MappingWandService.java din istoric
Status: validat local
Observatii: dupa acest slice, pachetul `world.mapping` din core este complet migrat la Kotlin.
```

### KOT-065

```text
ID: KOT-065
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/topology
Fisiere adaugate: TopologyCategory.kt, TopologyConsensus.kt
Fisiere sterse: TopologyCategory.java, TopologyConsensus.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modelele de topologie folosite la clasificare biome si prompt block AI
Compatibilitate Java: getter-ele JavaBean pentru campuri enum/model raman disponibile; metodele statice fromBiome(...) si fromId(...) sunt pastrate prin @JvmStatic
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierele Kotlin si readauga fisierele Java din istoric
Status: validat local
Observatii: un fail temporar la `clean build` a aparut doar cand doua comenzi Gradle au rulat in paralel; rerun serial a trecut complet.
```

### KOT-066

```text
ID: KOT-066
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/story
Fisiere adaugate: PlaceStoryState.kt, RegionStoryState.kt
Fisiere sterse: PlaceStoryState.java, RegionStoryState.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste doua modele story state folosite de serviciile de context/state
Compatibilitate Java: accesoriile record-style (placeId(), regionId(), storyMode(), stateKey(), variables(), etc.) sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierele Kotlin si readauga fisierele Java din istoric
Status: validat local
Observatii: copy semantics pentru map-uri si fallback-urile defensive (default stateKey/storyMode) sunt pastrate.
```

### KOT-067

```text
ID: KOT-067
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/story
Fisiere adaugate: StoryEvent.kt
Fisiere sterse: StoryEvent.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modelul de eveniment story folosit in snapshot/context si persistenta
Compatibilitate Java: accesoriile record-style (id(), scopeType(), eventType(), payload(), createdAt(), etc.) sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga StoryEvent.java din istoric
Status: validat local
Observatii: normalizarea string-urilor si copy semantics pentru payload (filtrare key blank, value null->\"\") sunt pastrate.
```

### KOT-068

```text
ID: KOT-068
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/story
Fisiere adaugate: StoryContextSnapshot.kt
Fisiere sterse: StoryContextSnapshot.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste snapshot-ul story (prompt block + nested quest anchor snapshot) folosit in story context service
Compatibilitate Java: API-ul public empty(), isEmpty(), toPromptBlock() si accesoriile record-style pentru snapshot + nested QuestAnchorSnapshot sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga StoryContextSnapshot.java din istoric
Status: validat local
Observatii: cand compileJava si clean build au fost lansate in paralel, a aparut fail intermitent pe compileTestKotlin; rerun serial clean build a trecut complet.
```

### KOT-069

```text
ID: KOT-069
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/progression
Fisiere adaugate: ProgressionStageSnapshot.kt
Fisiere sterse: ProgressionStageSnapshot.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modelul stage snapshot folosit in GUI/progression view
Compatibilitate Java: accesoriile record-style (id(), label(), completionMode(), objectiveIds(), etc.) si metoda statica fromQuestGuiStage(...) sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga ProgressionStageSnapshot.java din istoric
Status: validat local
Observatii: fail-ul intermitent pe compileTestKotlin apare cand ruleaza in paralel doua comenzi Gradle; validarea finala este facuta serial.
```

### KOT-070

```text
ID: KOT-070
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/progression
Fisiere adaugate: ProgressionObjectiveSnapshot.kt
Fisiere sterse: ProgressionObjectiveSnapshot.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modelul objective snapshot folosit in progression GUI/status
Compatibilitate Java: accesoriile record-style (key(), type(), stageId(), currentAmount(), requiredAmount(), complete(), active()) si metoda statica fromQuestGuiObjective(...) sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga ProgressionObjectiveSnapshot.java din istoric
Status: validat local
Observatii: limitarile numerice din varianta Java sunt pastrate (currentAmount >= 0, requiredAmount >= 1).
```

### KOT-071

```text
ID: KOT-071
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/progression
Fisiere adaugate: ProgressionAnchorBinding.kt
Fisiere sterse: ProgressionAnchorBinding.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modelul de binding intre objective/progression si ancora world
Compatibilitate Java: accesoriile record-style (playerUuid(), templateId(), objectiveKey(), anchorType(), anchorId(), status(), etc.) si metodele matchesAnchor(...), anchorSelector(), displayLabel() sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga ProgressionAnchorBinding.java din istoric
Status: validat local
Observatii: a aparut un crash intermitent de Kotlin incremental cache/daemon; rezolvat prin `gradlew --stop`, curatare `ainpc-core-plugin/build/kotlin` si rerulare seriala a gate-urilor.
```

### KOT-072

```text
ID: KOT-072
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/progression
Fisiere adaugate: ProgressionGuiSnapshot.kt
Fisiere sterse: ProgressionGuiSnapshot.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste snapshot-ul de GUI progression (current/archived entries + summary)
Compatibilitate Java: accesoriile record-style, metodele empty(), fromQuestGuiSnapshot(...) si allEntries() sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga ProgressionGuiSnapshot.java din istoric
Status: validat local
Observatii: ajustare de tip pentru resolver (`Function<..., ProgressionDefinition?>`) ca sa permita fallback null identic cu varianta Java.
```

### KOT-073

```text
ID: KOT-073
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/progression
Fisiere adaugate: StoredProgression.kt
Fisiere sterse: StoredProgression.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modelul persistat de progression folosit in repository/service/gui
Compatibilitate Java: accesoriile record-style si helper-ele current()/archived() sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga StoredProgression.java din istoric
Status: validat local
Observatii: sanitizarea campurilor si fallback-urile pentru JSON/compatibilitySource au ramas identice.
```

### KOT-074

```text
ID: KOT-074
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/progression
Fisiere adaugate: StoredProgressionSummary.kt
Fisiere sterse: StoredProgressionSummary.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste sumarul agregat pentru colectia de progression-uri stocate
Compatibilitate Java: accesoriile record-style si metoda statica from(...) sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga StoredProgressionSummary.java din istoric
Status: validat local
Observatii: semnatura from(...) accepta elemente nullable pentru a pastra comportamentul defensiv al variantei Java (skip null).
```

### KOT-075

```text
ID: KOT-075
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/progression
Fisiere adaugate: ProgressionStatusSnapshot.kt
Fisiere sterse: ProgressionStatusSnapshot.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste snapshot-ul de status pentru comenzile progression
Compatibilitate Java: accesoriile record-style, overload-urile statice fromResult(...), si toQuestInteractionResult() sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga ProgressionStatusSnapshot.java din istoric
Status: validat local
Observatii: semantica handled/notHandled si maparea campurilor entry/definition/systemMessages sunt pastrate.
```

### KOT-076

```text
ID: KOT-076
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/progression
Fisiere adaugate: ProgressionProgressSnapshot.kt
Fisiere sterse: ProgressionProgressSnapshot.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste snapshot-ul de progres (obiective + mesaje sistem) pentru comenzile progression
Compatibilitate Java: accesoriile record-style, overload-urile statice fromResult(...), toQuestInteractionResult() si completedObjectiveCount() sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga ProgressionProgressSnapshot.java din istoric
Status: validat local
Observatii: maparea objective snapshots din QuestGuiEntry este pastrata 1:1 functional.
```

### KOT-077

```text
ID: KOT-077
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/progression
Fisiere adaugate: ProgressionDefinition.kt
Fisiere sterse: ProgressionDefinition.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste definitia progression derivata din scenario definition / quest contract
Compatibilitate Java: accesoriile record-style, fromScenarioDefinition(...), isProgressionCandidate(...) si regulile de fallback pentru IDs/labels sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga ProgressionDefinition.java din istoric
Status: validat local
Observatii: normalizarea category/scenarioKind la lowercase ROOT si regulile progressionId/templateId raman neschimbate functional.
```

### KOT-078

```text
ID: KOT-078
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/progression
Fisiere adaugate: ProgressionGuiEntry.kt
Fisiere sterse: ProgressionGuiEntry.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste modelul principal pentru randurile GUI progression + helpere de comanda/selectie
Compatibilitate Java: accesoriile record-style, fromQuestGuiEntry(...), commandRoot(), commandSelector(), guiFilter(), command(...), trackStartCommand() si trackStopCommand() sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga ProgressionGuiEntry.java din istoric
Status: validat local
Observatii: heuristica commandSelector (fallback tracked + first non blank) si maparea obiective/stages din QuestGuiEntry raman pastrate functional.
```

### KOT-079

```text
ID: KOT-079
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/progression
Fisiere adaugate: ProgressionFilter.kt
Fisiere sterse: ProgressionFilter.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste utilitarul de filtrare pentru definition/stored progressions (all filter, semantic statuses, field filters)
Compatibilitate Java: API-ul static-like isAllFilter(...), matchesDefinition(...), matchesStored(...) este pastrat prin @JvmStatic; regulile FieldFilter.parse/isKnownField raman identice functional
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga ProgressionFilter.java din istoric
Status: validat local
Observatii: normalizarea filtrelor (lowercase/replace '-'/' ') si aliasurile de camp/statut sunt pastrate.
```

### KOT-080

```text
ID: KOT-080
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers
Fisiere adaugate: FamilyMemberRecord.kt
Fisiere sterse: FamilyMemberRecord.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 1
Motiv: converteste record-ul mic folosit in managerul de familie
Compatibilitate Java: accesoriile record-style name(), relationType(), alive(), relatedNpcId() sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga FamilyMemberRecord.java din istoric
Status: validat local
Observatii: clasa Kotlin e data class pentru a pastra semantica de egalitate/hash similara record-ului Java.
```

### KOT-081

```text
ID: KOT-081
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/runtime
Fisiere adaugate: ScenarioRuntimeHandler.kt, ScenarioTriggerHandler.kt, ScenarioActionHandler.kt, ScenarioVariableProvider.kt, ScenarioConditionHandler.kt, ScenarioActionRegistry.kt, ScenarioTriggerRegistry.kt, ScenarioConditionRegistry.kt
Fisiere sterse: ScenarioRuntimeHandler.java, ScenarioTriggerHandler.java, ScenarioActionHandler.java, ScenarioVariableProvider.java, ScenarioConditionHandler.java, ScenarioActionRegistry.java, ScenarioTriggerRegistry.java, ScenarioConditionRegistry.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste contractele runtime mici (handler interfaces + registries) fara logica de business complexa
Compatibilitate Java: semnaturile metodelor raman aceleasi; registries expun in continuare validateAction/validateTrigger/validateCondition cu aceeasi semantica
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierele Kotlin si readauga cele 8 fisiere Java din istoric
Status: validat local
Observatii: conversie mecanica 1:1, fara schimbari functionale in validarea definitiilor runtime.
```

### KOT-082

```text
ID: KOT-082
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/runtime
Fisiere adaugate: ScenarioRuntimeDefinition.kt
Fisiere sterse: ScenarioRuntimeDefinition.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modelul runtime definition (id/type/parameters + sanitizare + lookup parameter)
Compatibilitate Java: accesoriile record-style si metoda parameter(...) sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks; .\gradlew.bat :ainpc-core-plugin:test --tests "*ScenarioRuntimeRegistryTest"
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga ScenarioRuntimeDefinition.java din istoric
Status: validat local
Observatii: prima varianta Kotlin a folosit map copy mutabil (toMap) si a rupt testul de contract pentru imutabilitate; corectat la java.util.Map.copyOf(...) si revalidat.
```

### KOT-083

```text
ID: KOT-083
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/runtime
Fisiere adaugate: ScenarioValidationReport.kt
Fisiere sterse: ScenarioValidationReport.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste raportul de validare runtime (errors/warnings/infos + merge)
Compatibilitate Java: API-ul public error/warn/info/valid/isValid/hasWarnings/merge/errors/warnings/infos ramane identic functional
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga ScenarioValidationReport.java din istoric
Status: validat local
Observatii: listele raman interne mutabile, iar getter-ele continua sa expuna copii imutabile.
```

### KOT-084

```text
ID: KOT-084
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/runtime
Fisiere adaugate: ScenarioExecutionContext.kt
Fisiere sterse: ScenarioExecutionContext.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste contextul runtime (player/npc/location/progression vars) folosit de handlers runtime
Compatibilitate Java: accesoriile record-style si variable(...) sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga ScenarioExecutionContext.java din istoric
Status: validat local
Observatii: sanitizarea map-ului de variabile pastreaza copy imutabil cu Map.copyOf.
```

### KOT-085

```text
ID: KOT-085
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/runtime
Fisiere adaugate: ScenarioRuntimeRegistry.kt
Fisiere sterse: ScenarioRuntimeRegistry.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste registrul generic de runtime handlers (register/find/supports/validateDefinition)
Compatibilitate Java: API-ul public si semnaturile raman echivalente; handlers() ramane map ne-modificabil
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga ScenarioRuntimeRegistry.java din istoric
Status: validat local
Observatii: normalizarea type/label este pastrata cu lowercase default locale, ca in Java original.
```

### KOT-086

```text
ID: KOT-086
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/platform
Fisiere adaugate: PlatformProfile.kt
Fisiere sterse: PlatformProfile.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 1
Motiv: converteste modelul simplu de profil platform (runtime/world/story mode)
Compatibilitate Java: getter-ele JavaBean (getRuntimeMode/getWorldMode/getDefaultStoryMode) si metoda statica fromConfig(...) sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga PlatformProfile.java din istoric
Status: validat local
Observatii: maparea cheilor config (`platform.runtime_mode`, `platform.world_mode`, `platform.default_story_mode`) ramane identica.
```

### KOT-087

```text
ID: KOT-087
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/listeners
Fisiere adaugate: ListenerRegistry.kt
Fisiere sterse: ListenerRegistry.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 1
Motiv: converteste registrul central de listener-e Bukkit
Compatibilitate Java: constructorul si metoda registerAll() raman echivalente; ordinea de inregistrare a listener-elor este pastrata
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga ListenerRegistry.java din istoric
Status: validat local
Observatii: nu exista schimbari functionale in wiring-ul listener-elor.
```

### KOT-088

```text
ID: KOT-088
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/listeners
Fisiere adaugate: AbstractPluginListener.kt
Fisiere sterse: AbstractPluginListener.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste baza comuna pentru listener-ele pluginului (messages/conversations/scheduler/callSync)
Compatibilitate Java: campul protected `plugin` este pastrat accesibil direct pentru subclasele Java prin `@JvmField`; semantica helper-elor runSync/runLater/callSync ramane neschimbata
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga AbstractPluginListener.java din istoric
Status: validat local
Observatii: prima varianta Kotlin a rupt accesul direct `plugin` din subclase Java; corectat in acelasi slice cu `@JvmField`.
```

### KOT-089

```text
ID: KOT-089
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine
Fisiere adaugate: QuestDirectorRequest.kt
Fisiere sterse: QuestDirectorRequest.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modelul request pentru quest director (story context + definitions + blocking reasons)
Compatibilitate Java: accesoriile record-style si helper-ul static forStoryContext(...) sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga QuestDirectorRequest.java din istoric
Status: validat local
Observatii: sanitizarea definitions (skip null) si blockingReasons (trim + drop blank) ramane identica functional.
```

### KOT-090

```text
ID: KOT-090
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine
Fisiere adaugate: QuestDirectorDecision.kt
Fisiere sterse: QuestDirectorDecision.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste modelul de decizie al quest director (status/candidate/blocked/warnings/signals)
Compatibilitate Java: accesoriile record-style, factory-urile statice noAction/blocked/seedSuggested/candidateFound si enum Status.id() sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga QuestDirectorDecision.java din istoric
Status: validat local
Observatii: comportamentul original care forteaza runtimeExecutable=false in constructor a fost pastrat.
```

### KOT-091

```text
ID: KOT-091
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/bootstrap
Fisiere adaugate: SchedulerCoordinator.kt
Fisiere sterse: SchedulerCoordinator.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste coordonatorul de task-uri periodice (restore/simulation/routine/emotion/memory/persistence/rebalance/quest tracking)
Compatibilitate Java: API-ul start() si logica de scheduling (delay/period/config keys/debug logs) sunt pastrate functional
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga SchedulerCoordinator.java din istoric
Status: validat local
Observatii: intervalele de tick si fallback-urile config (min values) au ramas identice.
```

### KOT-092

```text
ID: KOT-092
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/platform
Fisiere adaugate: AINPCPlatform.kt
Fisiere sterse: AINPCPlatform.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste adaptorul principal de platforma (profile/addon registry/world admin/config paths/content reload)
Compatibilitate Java: implementarea AINPCPlatformApi ramane echivalenta; getRuntimeMode/getWorldMode/getDefaultStoryMode/getAddonRegistry/getWorldAdmin/getDataDirectory/getPackDirectory/getAddonConfigDirectory/reloadContent sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga AINPCPlatform.java din istoric
Status: validat local
Observatii: prima varianta Kotlin a facut `worldAdminService` privat si a rupt accesul direct din cod Kotlin existent; corectat in acelasi slice prin expunere proprietate publica.
```

### KOT-093

```text
ID: KOT-093
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/listeners
Fisiere adaugate: PlayerJoinListener.kt
Fisiere sterse: PlayerJoinListener.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste listener-ul de recunoastere la login (scan NPC nearby + memory stats + emotion nudges)
Compatibilitate Java: constructorul si handler-ul onPlayerJoin(...) raman echivalente functional; delay-ul runLater(40L) este pastrat
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga PlayerJoinListener.java din istoric
Status: validat local
Observatii: pragurile de recunoastere (memoryCount/emotionalImpact) si ajustarea emotiilor happiness/anger raman identice.
```

### KOT-094

```text
ID: KOT-094
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine
Fisiere adaugate: FeaturePackDependencyValidator.kt
Fisiere sterse: FeaturePackDependencyValidator.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste utilitarul de rezolvare dependente intre pack-uri
Compatibilitate Java: API-ul static-like missingDependencies(...) si resolveAvailablePackIds(...) este pastrat prin @JvmStatic; normalizarea/filtrarea raman neschimbate functional
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga FeaturePackDependencyValidator.java din istoric
Status: validat local
Observatii: ordinea stabila LinkedHashSet/LinkedHashMap in rezolvare a fost pastrata.
```

### KOT-095

```text
ID: KOT-095
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine
Fisiere adaugate: QuestDecisionIntentResolver.kt
Fisiere sterse: QuestDecisionIntentResolver.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste resolver-ul de intentii pentru decizii quest (offer/accept/decline/abandon/status/complete)
Compatibilitate Java: enum Intent si metoda normalize(...) raman consumabile din Java; normalize este expusa cu @JvmStatic
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga QuestDecisionIntentResolver.java din istoric
Status: validat local
Observatii: normalizarea textului (diacritice/punctuatie/spatii) si regulile pentru intentii scurte au ramas echivalente functional.
```

### KOT-096

```text
ID: KOT-096
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/utils
Fisiere adaugate: NPCNameGenerator.kt
Fisiere sterse: NPCNameGenerator.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 1
Motiv: converteste generatorul de nume NPC (pool male/female + count)
Compatibilitate Java: randomName(...), predefinedNames(...), predefinedNameCount() sunt pastrate cu @JvmStatic
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga NPCNameGenerator.java din istoric
Status: validat local
Observatii: seturile de nume si fallback-ul de gender la `male` au ramas identice.
```

### KOT-097

```text
ID: KOT-097
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine
Fisiere adaugate: FeaturePackMetadataValidator.kt
Fisiere sterse: FeaturePackMetadataValidator.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste validatorul de metadata pentru feature pack-uri (addon type/capabilities/dependencies/runtime modes)
Compatibilitate Java: apelul static validate(...) este pastrat prin @JvmStatic; accesorii record-style packId()/errors()/warnings() au fost pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga FeaturePackMetadataValidator.java din istoric
Status: validat local
Observatii: prima varianta Kotlin a rupt interop-ul Java pentru ValidationResult (packId()/errors()/warnings()); corectat in acelasi slice si revalidat.
```

### KOT-098

```text
ID: KOT-098
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers
Fisiere adaugate: ConversationSessionManager.kt
Fisiere sterse: ConversationSessionManager.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 1
Motiv: converteste managerul mic pentru starea sesiunilor de conversatie active
Compatibilitate Java: API-ul public (startConversation/touchConversation/isInConversation/isExpired/getConversationNpcId/getConversationPartner/clearConversation) ramane echivalent
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga ConversationSessionManager.java din istoric
Status: validat local
Observatii: semantica timeout-ului si lookup-ul partenerului de conversatie prin NPC UUID au ramas identice.
```

### KOT-099

```text
ID: KOT-099
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/listeners
Fisiere adaugate: VillagerLifecycleListener.kt
Fisiere sterse: VillagerLifecycleListener.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste listener-ul de lifecycle pentru villageri (spawn/chunk load/career change/death)
Compatibilitate Java: constructorul si handler-ele de eveniment raman echivalente; delay-urile runLater (60L/80L/1L) sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga VillagerLifecycleListener.java din istoric
Status: validat local
Observatii: logica de sincronizare villager->NPC si rebalance pe chunk load/spawn a ramas neschimbata functional.
```

### KOT-100

```text
ID: KOT-100
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/listeners
Fisiere adaugate: QuestObjectiveListener.kt
Fisiere sterse: QuestObjectiveListener.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste listener-ul de progres obiective quest (move/kill/pickup/drop/inventory)
Compatibilitate Java: constructorul si handler-ele de eveniment raman echivalente; refreshInventoryProgressNextTick ruleaza in continuare cu delay 1 tick
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga QuestObjectiveListener.java din istoric
Status: validat local
Observatii: conversia a inclus o corectie minora pentru warning Kotlin (`event.to`), fara schimbare functionala.
```

### KOT-101

```text
ID: KOT-101
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/listeners
Fisiere adaugate: MappingWandListener.kt
Fisiere sterse: MappingWandListener.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste listener-ul pentru interactiunile cu mapping wand (set point/pos1/pos2 + preview)
Compatibilitate Java: constructorul si handler-ul onPlayerInteract raman echivalente; gating-ul pe `EquipmentSlot.HAND` si permisiunea `ainpc.admin` sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga MappingWandListener.java din istoric
Status: validat local
Observatii: logica de selectie pe moduri (`NODE`, `NPC_BIND`, `QUEST_ANCHOR`) si mesajele de draft au ramas identice functional.
```

### KOT-102

```text
ID: KOT-102
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/scan
Fisiere adaugate: VanillaVillageScanner.kt
Fisiere sterse: VanillaVillageScanner.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste scanner-ul vanilla pentru semnale de sat (bell/bed/door/workstation/farmland)
Compatibilitate Java: metoda de instanta scan(...) este pastrata; constantele publice DEFAULT/MAX radius sunt expuse cu @JvmField
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga VanillaVillageScanner.java din istoric
Status: validat local
Observatii: clamp-ul razelor, scanarea pe volum si warning-urile pentru lipsa clopotului/limitare de raza au ramas echivalente.
```

### KOT-103

```text
ID: KOT-103
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/routine
Fisiere adaugate: RoutineService.kt
Fisiere sterse: RoutineService.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste serviciul de tick pentru rutina NPC (assign/teleport/natural movement/cooldown)
Compatibilitate Java: API-ul public runRoutineTick(), preview(...) si getter-ul routineEngine raman echivalente
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga RoutineService.java din istoric
Status: validat local
Observatii: conversia a necesitat fixuri in acelasi slice pentru interop Kotlin existent (acces routineEngine) si nullability pe target anchor/world, fara schimbare functionala.
```

### KOT-104

```text
ID: KOT-104
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn
Fisiere adaugate: NpcSpawnPlan.kt
Fisiere sterse: NpcSpawnPlan.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste modelul de plan spawn NPC (builder + source key + normalizare campuri)
Compatibilitate Java: accesoriile record-style (npcKey(), name(), age(), etc.) sunt pastrate explicit; builder(...) ramane disponibil static
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga NpcSpawnPlan.java din istoric
Status: validat local
Observatii: regulile de sanitizare (trim, age fallback 30, gender fallback male, sourceKey normalize) au ramas echivalente functional.
```

### KOT-105

```text
ID: KOT-105
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/routine
Fisiere adaugate: RoutineEngine.kt
Fisiere sterse: RoutineEngine.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste motorul de asignare rutina pe intervale de timp (home/work/social/idle + preview day)
Compatibilitate Java: API-ul public assign(...) si previewDay(...) ramane echivalent
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga RoutineEngine.java din istoric
Status: validat local
Observatii: regulile de selectie stare dupa world time, fallback-urile de ancora si maparea ocupatie->NPCState au ramas neschimbate functional.
```

### KOT-106

```text
ID: KOT-106
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/listeners
Fisiere adaugate: NPCInteractionListener.kt
Fisiere sterse: NPCInteractionListener.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste listener-ul principal de interactiune NPC (quest interaction + sesiune conversatie + greeting async)
Compatibilitate Java: comportamentul handler-ului onPlayerInteractEntity, openOrRefreshConversation si startConversation ramane echivalent functional
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga NPCInteractionListener.java din istoric
Status: validat local
Observatii: fluxul async CompletableFuture pentru salut initial/revenire si mesajele quest/system au ramas neschimbate functional.
```

### KOT-107

```text
ID: KOT-107
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world
Fisiere adaugate: NpcWorldBindingService.kt
Fisiere sterse: NpcWorldBindingService.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste serviciul de persistenta pentru npc_world_bindings (save/get/list/count/delete)
Compatibilitate Java: constructorii publici, folosirea Optional in getBinding(...), aruncarea SQLException si contractul StatementProvider sunt pastrate
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga NpcWorldBindingService.java din istoric
Status: validat local
Observatii: query-urile SQL, normalizarea textelor nullable si validarea npc_id/binding au ramas echivalente functional.
```

### KOT-108

```text
ID: KOT-108
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/utils
Fisiere adaugate: MessageUtils.kt
Fisiere sterse: MessageUtils.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste utilitarul central de mesaje (config messages, colorize, action bar, NPC message, progress bars)
Compatibilitate Java: API-ul de instanta (sendMessage/send/sendActionBar/sendNPCMessage/format/get*Name) ramane echivalent
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga MessageUtils.java din istoric
Status: validat local
Observatii: prima varianta Kotlin a necesitat o corectie de nullability la mesajul citit din config (`getString(...) ?: messageKey`); fara schimbare functionala.
```

### KOT-109

```text
ID: KOT-109
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine
Fisiere adaugate: QuestDirector.kt
Fisiere sterse: QuestDirector.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste directorul de decizie quest (demand signals, scoring progression definitions, candidate/seed/blocked decision)
Compatibilitate Java: API-ul public decide(...) ramane echivalent functional
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga QuestDirector.java din istoric
Status: validat local
Observatii: conversia a inclus corectii de nullability pentru persistent region/place state in acelasi slice; logica de scoring/normalizare a ramas neschimbata functional.
```

### KOT-110

```text
ID: KOT-110
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai
Fisiere adaugate: NpcFactResolver.kt
Fisiere sterse: NpcFactResolver.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 2
Motiv: converteste resolver-ul factual NPC (intent detect, raspunsuri, normalizare text, descriere activitate/locatie)
Compatibilitate Java: metodele statice resolve/describeCurrentActivity/describeLocation sunt pastrate prin @JvmStatic; NpcFacts pastreaza accesoriile record-style
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga NpcFactResolver.java din istoric
Status: validat local
Observatii: ordinea intent-urilor detectate, regulile de join natural al raspunsurilor si normalizarea diacriticelor raman echivalente functional.
```

### KOT-111

```text
ID: KOT-111
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/addons
Fisiere adaugate: AddonRegistry.kt
Fisiere sterse: AddonRegistry.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste registrul addonurilor (descriptor/addon register/unregister, configure, sort/load order, strict validation, shutdown)
Compatibilitate Java: implementarea AddonRegistryApi si metodele synchronized raman echivalente functional
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga AddonRegistry.java din istoric
Status: validat local
Observatii: ordinea de load (core first, apoi loadOrderIds, apoi id case-insensitive), filtrarea disabled addons si regulile strictValidation au ramas neschimbate functional.
```

### KOT-112

```text
ID: KOT-112
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn
Fisiere adaugate: HouseAllocation.kt
Fisiere sterse: HouseAllocation.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste modelul de alocare casa/familie + resident plans + buildere pentru spawn/family binding metadata
Compatibilitate Java: metodele builder(...) si accesoriile record-style pentru HouseAllocation/ResidentPlan sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga HouseAllocation.java din istoric
Status: validat local
Observatii: regulile de sanitizare, fallback-uri (`maxResidents`, `effectiveHomeNodeId`, `humanizeId`) si generarea metadata/family binding au ramas echivalente functional.
```

### KOT-113

```text
ID: KOT-113
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine
Fisiere adaugate: QuestScenarioContract.kt
Fisiere sterse: QuestScenarioContract.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste contractul mecanicilor de quest (kind/category/acceptance/completion/tracking/tags + infer kind)
Compatibilitate Java: factory-urile statice (defaultContract/fromScenarioDefinition/fromQuestEntries) sunt pastrate prin @JvmStatic; accesoriile record-style sunt pastrate explicit
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga QuestScenarioContract.java din istoric
Status: validat local
Observatii: maparea id->enum, inferenta obiectivelor, normalizarea tagurilor si display names au ramas echivalente functional.
```

### KOT-114

```text
ID: KOT-114
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers
Fisiere adaugate: EmotionManager.kt
Fisiere sterse: EmotionManager.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste managerul de emotii NPC (apply/decay/interaction effects/particles/reports/process events/mood set)
Compatibilitate Java: API-ul public al managerului a ramas echivalent functional, inclusiv update pe main thread si persistenta async
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga EmotionManager.java din istoric
Status: validat local
Observatii: maparea evenimentelor emotionale, pragurile de intensitate si efectele de particule au ramas neschimbate functional.
```

### KOT-115

```text
ID: KOT-115
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/listeners
Fisiere adaugate: NPCChatListener.kt
Fisiere sterse: NPCChatListener.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 4
Motiv: converteste listener-ul principal pentru chat privat player-NPC (target resolution, sesiuni conversatie, intentii quest, pipeline dialog async)
Compatibilitate Java: comportamentul handler-elor `onAsyncChat`/`onPlayerQuit` si fluxurile de conversatie/quest raman echivalente functional
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga NPCChatListener.java din istoric
Status: validat local
Observatii: conversia a necesitat corectie de interop pe enum-ul `DialogManager.DialogStatus` si un `else` defensiv in `when`; fara schimbare functionala.
```

### KOT-116

```text
ID: KOT-116
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/progression
Fisiere adaugate: ProgressionRepository.kt
Fisiere sterse: ProgressionRepository.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 4
Motiv: converteste repository-ul SQL pentru progresii si quest anchor bindings (find/summarize/query/save)
Compatibilitate Java: API-ul repository-ului si semantica `StatementProvider` + exceptii `SQLException` raman echivalente functional
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga ProgressionRepository.java din istoric
Status: validat local
Observatii: query-urile SQL, maparea row->StoredProgression/ProgressionAnchorBinding, matching-ul definitiilor si fallback-urile de id/code au ramas echivalente functional.
```

### KOT-117

```text
ID: KOT-117
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/progression
Fisiere adaugate: ProgressionService.kt
Fisiere sterse: ProgressionService.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 4
Motiv: converteste serviciul de progresie (selectors/snapshots/track/status/debug/progress/anchor bindings/objective suggestions)
Compatibilitate Java: API-ul public ProgressionService ramane echivalent functional; wiring-ul spre ScenarioEngine si ProgressionRepository este pastrat
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga ProgressionService.java din istoric
Status: validat local
Observatii: conversia a inclus o corectie minora de warning (`scenarioEngine() == null` eliminat), fara schimbare functionala.
```

### KOT-118

```text
ID: KOT-118
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai
Fisiere adaugate: DialogManager.kt
Fisiere sterse: DialogManager.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md, NPCChatListener.kt
Tip: productie
Risc: 4
Motiv: converteste managerul de dialog (pipeline async context->generate->persist->emotion, history/relationship CRUD, cooldown handling)
Compatibilitate Java: API-ul public processMessage/getRecentHistory/getRelationship/getRelationshipAsync/isOnCooldown si tipurile DialogResult/DialogStatus/DialogRequest/PromptDbContext raman echivalente functional
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga DialogManager.java din istoric
Status: validat local
Observatii: au fost necesare fixuri de interop in acelasi slice (DialogResult nullable response + eliminare clash JVM pe getStatus/getResponse) si curatare warning in NPCChatListener; fara schimbare functionala.
```

### KOT-119

```text
ID: KOT-119
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/story
Fisiere adaugate: StoryContextService.kt
Fisiere sterse: StoryContextService.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 4
Motiv: converteste serviciul de construire context story (world context, active quest anchors, persistent region/place state, recent events, story signals)
Compatibilitate Java: API-ul public buildForNpc/buildForPlayer si semantica de colectare semnale/context raman echivalente functional
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga StoryContextService.java din istoric
Status: validat local
Observatii: conversia a inclus curatare warning Kotlin pe guard redundant pentru world admin service, fara schimbare functionala.
```

### KOT-120

```text
ID: KOT-120
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/story
Fisiere adaugate: StoryStateService.kt
Fisiere sterse: StoryStateService.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 4
Motiv: converteste serviciul de persistenta story state/events (region/place state, record/list events, JSON parse/copy)
Compatibilitate Java: API-ul public si semantica SQL/JSON (Optional pentru get, recordEvent/listRecentEvents, validari id) raman echivalente functional
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga StoryStateService.java din istoric
Status: validat local
Observatii: maparea ResultSet->RegionStoryState/PlaceStoryState/StoryEvent si fallback-urile pentru JSON invalid au ramas neschimbate functional.
```

### KOT-121

```text
ID: KOT-121
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine
Fisiere adaugate: QuestAnchorResolver.kt
Fisiere sterse: QuestAnchorResolver.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 4
Motiv: converteste resolver-ul de ancore quest (region/place/node/npc matching, ordering by proximity, objective normalization, issues)
Compatibilitate Java: API-ul public `resolve(...)` si tipurile rezultate (`ResolvedQuestAnchors`, `ResolvedQuestAnchor`, `ResolutionIssue`) raman echivalente functional
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga QuestAnchorResolver.java din istoric
Status: validat local
Observatii: logica de matching pentru reference/objective type, colectarea issues si generarea quest variables au ramas neschimbate functional.
```

### KOT-122

```text
ID: KOT-122
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui
Fisiere adaugate: GuiService.kt
Fisiere sterse: GuiService.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 4
Motiv: converteste serviciul GUI principal (open/close/back/refresh, mapare butoane, dispatch click actions, integrare PlaceholderResolver)
Compatibilitate Java: API-ul public `open`, `close`, `closeAll`, `goBack`, `refresh`, `handleClick`, `contains` si `current` ramane echivalent functional
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga GuiService.java din istoric
Status: validat local
Observatii: conversia a inclus corectii de interop cu modelele Kotlin existente din gui (`GuiSession`, `GuiButton`, `GuiClickContext`) si fallback-uri defensive de nullability pentru click type/action; fara schimbare functionala.
```

### KOT-123

```text
ID: KOT-123
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn
Fisiere adaugate: SpawnBatchTracker.kt
Fisiere sterse: SpawnBatchTracker.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 4
Motiv: converteste tracker-ul de batch pentru spawn (begin/record/finalize, batch steps, query status/filter, parse id-uri NPC)
Compatibilitate Java: constantele de status si metodele statice (`normalizeBatchStatusFilter`, `isSupportedBatchStatusFilter`, `parseNpcDatabaseIds`) sunt pastrate; tipurile `BatchRecord`/`BatchStepRecord` pastreaza accesoriile record-style
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga SpawnBatchTracker.java din istoric
Status: validat local
Observatii: conversia a inclus o rulare de validare repetata secvential, dupa ce executia paralela a taskurilor cu `clean` a generat un rezultat fals negativ de clasa lipsa; build-ul final secvential este verde.
```

### KOT-124

```text
ID: KOT-124
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn
Fisiere adaugate: HouseAllocationValidator.kt
Fisiere sterse: HouseAllocationValidator.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 4
Motiv: converteste validatorul de alocare house/residents (structure checks, owner/residents metadata sync, home/work/social anchors, node semantics/exclusivity)
Compatibilitate Java: API-ul public `validate(...)` si fluxul de erori/warning-uri raman echivalente functional
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga HouseAllocationValidator.java din istoric
Status: validat local
Observatii: conversie mecanica 1:1 in Kotlin, cu pastrarea regulilor de matching pentru id/selectori si a validarii semantice pe metadata/type tokens.
```

### KOT-125

```text
ID: KOT-125
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn
Fisiere adaugate: HouseAllocationPlanner.kt
Fisiere sterse: HouseAllocationPlanner.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 4
Motiv: converteste planner-ul de household/settlement (house select, spawn/home capacity, workplace/social assignment, resident generation, planning result models)
Compatibilitate Java: API-ul public `plan(...)`, `planSettlement(...)` si tipurile `PlanningResult`/`SettlementPlanningResult` pastreaza accesoriile record-style si factory-urile statice `success/failed`
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga HouseAllocationPlanner.java din istoric
Status: validat local
Observatii: validarea initiala rulata in paralel cu task-uri ce contin `clean` a produs false negative de classpath; rerularea secventiala a confirmat build verde fara regresii.
```

### KOT-126

```text
ID: KOT-126
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/scan
Fisiere adaugate: SemanticVillageMapper.kt
Fisiere sterse: SemanticVillageMapper.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 4
Motiv: converteste importul semantic pentru scanarea vanilla village (region/place/node creation, clustering, overlap merge, workplace typing, metadata tagging)
Compatibilitate Java: API-ul public `importScan(...)` si contractul rezultatului `SemanticVillageImportResult` raman echivalente functional
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga SemanticVillageMapper.java din istoric
Status: validat local
Observatii: conversia a necesitat fixuri de interop Kotlin (`createNode` cu coordonate Double, `isPublicAccess` pe WorldPlace), fara schimbare functionala.
```

### KOT-127

```text
ID: KOT-127
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers
Fisiere adaugate: MemoryManager.kt
Fisiere sterse: MemoryManager.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 3
Motiv: converteste managerul de amintiri NPC (create/query/cleanup/statistics/async helpers + model Memory)
Compatibilitate Java: API-ul public al metodelor de manager si al tipurilor `MemoryStats`/`Memory` este pastrat cu accesorii Java-style
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga MemoryManager.java din istoric
Status: validat local
Observatii: conversia a necesitat corectie de clash JVM pe getter/setter in `Memory` (campuri private + metode explicite), fara schimbare functionala.
```

### KOT-128

```text
ID: KOT-128
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine
Fisiere adaugate: DialogueEngine.kt
Fisiere sterse: DialogueEngine.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 4
Motiv: converteste motorul de dialog NPC (intent selection, template processing, anti-repetition cache, contextual/openai pipeline, quick response helpers)
Compatibilitate Java: API-ul public `generateResponse(...)`, `generateQuickResponse(...)` si enum-ul `DialogueIntent` raman consumabile din Java
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga DialogueEngine.java din istoric
Status: validat local
Observatii: conversia a inclus ajustari de interop (`NPCContext.setInteractingPlayer`, `NPCEmotions.getShortDescription`, relatie nullable in pipeline) si eliminare clash JVM pe getter-ul enum `description`; build-ul final este verde.
```

### KOT-129

```text
ID: KOT-129
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine
Fisiere adaugate: DecisionEngine.kt
Fisiere sterse: DecisionEngine.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 4
Motiv: converteste motorul de decizie NPC (scoring traits/emotions/memory/context/needs/routine, cache cooldown, simulation tick lifecycle)
Compatibilitate Java: API-ul public `decideAction`, `runLifeSimulationTick`, `getTopActions`, `getActionScore` ramane echivalent functional
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga DecisionEngine.java din istoric
Status: validat local
Observatii: conversia a inclus ajustari de interop Java-style pe `NPCAction` (`isX()/getCategory()`), plus corectie de rotunjire pentru decay/recovery (`roundToInt`); build-ul final este verde.
```

### KOT-130

```text
ID: KOT-130
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/database
Fisiere adaugate: DatabaseManager.kt
Fisiere sterse: DatabaseManager.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 4
Motiv: converteste managerul SQLite central (initialize/schema bootstrap, helper statements cu lock/proxy, async executor DB, shutdown controlat)
Compatibilitate Java: API-ul public `initialize`, `prepareStatement`, `executeUpdate`, `runAsync`, `supplyAsync`, `close` este pastrat; clasa/metodele `prepareStatement` au ramas extensibile pentru testele Java
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga DatabaseManager.java din istoric
Status: validat local
Observatii: conversia a necesitat ajustare de compatibilitate pentru testele Java (`DatabaseManager` + `prepareStatement` open, constructor compatibil cu plugin null in test double), dupa care build-ul complet este verde.
```

### KOT-131

```text
ID: KOT-131
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers
Fisiere adaugate: FamilyManager.kt
Fisiere sterse: FamilyManager.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 4
Motiv: converteste managerul de familie (auto-generation spouse/parents/children/siblings, family report, bidirectional link, post-spawn physical binding)
Compatibilitate Java: API-ul public (`generateFamily`, `getFamily`, `hasSpouse`, `getFamilyReport`, `bindSpawnedFamily`, `clearFamily`) ramane echivalent functional
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga FamilyManager.java din istoric
Status: validat local
Observatii: conversia a inclus corectie de nullability pentru `relationType` in gruparea raportului de familie; build-ul final este verde.
```

### KOT-132

```text
ID: KOT-132
Data: 2026-05-17
Autor: local
Zona: ainpc-core-plugin/src/main/kotlin/ro/ainpc
Fisiere adaugate: AINPCPlugin.kt
Fisiere sterse: AINPCPlugin.java
Fisiere modificate: docs/kotlin-migration-tracker.md, docs/rezumat-conversie-java-la-kotlin.md
Tip: productie
Risc: 4
Motiv: converteste entrypoint-ul principal al pluginului (bootstrap config/platform/db/ai/managers/engines/commands/listeners, reload flow, shutdown flow)
Compatibilitate Java: API-ul public al pluginului (getter-ele de servicii/manageri + `getInstance()` static) ramane disponibil pentru call-site-urile Java existente
Teste rulate: .\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks
Build rulat: .\gradlew.bat clean build
JAR audit: neexecutat explicit in acest slice; gate principal ramas clean build + compileJava
Smoke Paper: nu a fost rulat local; KOT-010 ramane gate-ul Paper runtime
Rollback: sterge fisierul Kotlin si readauga AINPCPlugin.java din istoric
Status: validat local
Observatii: conversia a folosit `lateinit` + setter privat pentru componente, helper pentru inregistrarea aliasurilor de comenzi si companion `@JvmStatic getInstance`; build-ul final este verde.
```

## Exemplu de completare

```text
ID: KOT-002
Data: 2026-05-16
Autor: local
Zona: ainpc-core-plugin/src/test/kotlin
Fisiere adaugate: KotlinToolchainTest.kt
Fisiere sterse: -
Fisiere modificate: build.gradle, gradle.properties
Tip: test
Risc: 1
Motiv: confirma ca JUnit 5 ruleaza teste Kotlin
Compatibilitate Java: nu expune API
Teste rulate: .\gradlew.bat :ainpc-core-plugin:test --tests "*KotlinToolchainTest"
Build rulat: .\gradlew.bat clean build
JAR audit: nu este necesar, test-only
Smoke Paper: nu este necesar
Rollback: sterge testul Kotlin si dezactiveaza pluginul daca nu exista alte fisiere Kotlin
Status: validat local
Observatii: -
```

## Criterii de status

`planificat`:

- slice-ul este ales, dar nu inceput

`in lucru`:

- exista modificari locale, dar gate-ul nu este trecut

`validat local`:

- testele locale si build-ul necesar trec

`validat Paper`:

- smoke testul Paper a trecut

`blocat`:

- exista eroare care impiedica finalizarea slice-ului

`amanat`:

- slice-ul este valid ca idee, dar nu este momentul potrivit

`respins`:

- slice-ul nu trebuie facut in forma propusa

## Reguli de blocare

Marcheaza slice-ul `blocat` daca apare:

- `ClassNotFoundException` pentru Kotlin runtime
- schimbare neplanificata in `plugin.yml`
- eroare de compilare in addonul medieval
- schimbare de API public fara test Java de consum
- nevoie de coroutine
- nevoie de schimbare DB schema
- modificari in peste 5 call-site-uri Java pentru o clasa mica

## Rapoarte de sprint

La finalul unui grup de slice-uri:

```text
Sprint:
Slice-uri finalizate:
Slice-uri blocate:
Fisiere Kotlin totale:
Fisiere Java ramase:
Build:
Smoke Paper:
JAR core size:
JAR addon size:
Probleme interop:
Decizie urmatoare:
```

## Comenzi pentru inventar

```powershell
(Get-ChildItem -Recurse -Filter *.kt | Measure-Object).Count
(Get-ChildItem -Recurse -Filter *.java | Measure-Object).Count
Get-ChildItem .\ainpc-core-plugin\build\libs\*.jar | Select-Object Name,Length
Get-ChildItem .\ainpc-scenario-medieval\build\libs\*.jar | Select-Object Name,Length
```

## Definitia de gata

Trackerul este util doar daca:

- fiecare slice Kotlin are rand propriu
- gate-ul este explicit
- testele rulate sunt listate concret
- smoke testul este mentionat cand este necesar
- rollback-ul este scris inainte de conversii cu risc mare

### KOT-134

Data: 2026-05-17
ID: KOT-134
Status: validat local
Zona: `ro.ainpc.spawn`
Tip: productie
Risc: 4

Fisiere adaugate: NpcSpawnOrchestrator.kt
Fisiere sterse: NpcSpawnOrchestrator.java

Gate local:
- `.\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks` (PASS)
- `.\gradlew.bat clean build` (PASS)

Observatii interop:
- Suprafata publica a clasei a ramas echivalenta (`spawn`, `bindFamily`, `validateHouseAllocation`, `dryRunHouseAllocation`, `spawnHousehold`, `dryRunSettlement`, `spawnSettlement`, `resolve`).
- Conversie 1:1 fara schimbare de flux functional; au fost adaugate doar garduri de nulabilitate Kotlin.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 169 fisiere Kotlin, 9 fisiere Java (~95.0% Kotlin)

Rollback:
- sterge fisierul Kotlin si readauga `NpcSpawnOrchestrator.java` din istoric

### KOT-135

Data: 2026-05-17
ID: KOT-135
Status: validat local
Zona: `ainpc-api/src/main`
Tip: productie
Risc: 3

Fisiere adaugate: `AddonType.kt`, `PlaceType.kt`, `StoryMode.kt`, `WorldMode.kt`, `RuntimeMode.kt`, `AINPCAddon.kt`, `WorldNodeInfo.kt`, `WorldPlaceInfo.kt`, `WorldRegionInfo.kt`, `AddonDescriptor.kt`
Fisiere sterse: versiunile Java echivalente

Gate local:
- `.\gradlew.bat :ainpc-api:build` (PASS)
- `.\gradlew.bat :ainpc-core-plugin:test` (PASS)
- `.\gradlew.bat :ainpc-scenario-medieval:test` (PASS)

Observatii:
- interfețele `AddonRegistryApi`, `AINPCPlatformApi`, `WorldAdminApi` au ramas Java pentru interop Kotlin/Java stabil.

### KOT-136

Data: 2026-05-17
ID: KOT-136
Status: validat local
Zona: `ainpc-core-plugin/src/test/commands`
Tip: test
Risc: 2

Fisiere adaugate:
- `AINPCTabCompleterTest.kt`
- `AINPCCommandRoutingTest.kt`
- `PluginCommandDescriptorTest.kt`

Fisiere sterse:
- `AINPCTabCompleterTest.java`
- `AINPCCommandRoutingTest.java`
- `PluginCommandDescriptorTest.java`

Gate local:
- `.\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.commands.*"` (PASS)
- `.\gradlew.bat :ainpc-core-plugin:test` (PASS)

### KOT-137

Data: 2026-05-17
ID: KOT-137
Status: validat local
Zona: `ainpc-core-plugin/src/test/gui`
Tip: test
Risc: 1

Fisiere adaugate: `GuiKeyTest.kt`
Fisiere sterse: `GuiKeyTest.java`

Gate local:
- `.\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.GuiKeyTest"` (PASS)
- `.\gradlew.bat :ainpc-core-plugin:test` (PASS)

### KOT-138

Data: 2026-05-17
ID: KOT-138
Status: validat local
Zona: `Gradle Kotlin-first`
Tip: build
Risc: 2

Fisiere modificate:
- `build.gradle` (plugin Kotlin + stdlib/bom centralizat in `subprojects`)
- `ainpc-api/build.gradle`
- `ainpc-core-plugin/build.gradle`
- `ainpc-scenario-medieval/build.gradle`

Gate local:
- `.\gradlew.bat clean :ainpc-api:build :ainpc-core-plugin:test :ainpc-scenario-medieval:test` (PASS)

### KOT-139

Data: 2026-05-17
ID: KOT-139
Status: validat local
Zona: `ainpc-core-plugin/src/test/gui`
Tip: test
Risc: 1

Fisiere adaugate: `QuestLogGuiFilterTest.kt`
Fisiere sterse: `QuestLogGuiFilterTest.java`

Gate local:
- `.\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.QuestLogGuiFilterTest"` (PASS)
- `.\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.gui.*"` (PASS)
- `.\gradlew.bat :ainpc-core-plugin:test` (PASS)

### KOT-140

Data: 2026-05-17
ID: KOT-140
Status: validat local
Zona: `ainpc-core-plugin/src/test/world/mapping`
Tip: test
Risc: 1

Fisiere adaugate: `MappingIntentParserTest.kt`
Fisiere sterse: `MappingIntentParserTest.java`

Gate local:
- `.\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.world.mapping.MappingIntentParserTest"` (PASS)
- `.\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.world.mapping.*"` (PASS)
- `.\gradlew.bat :ainpc-core-plugin:test` (PASS)

### KOT-141

Data: 2026-05-17
ID: KOT-141
Status: validat local
Zona: `docs/* conversie Kotlin`
Tip: documentatie
Risc: 1

Fisiere modificate:
- `docs/conversie-java-la-kotlin.md`
- `docs/conversie-java-la-kotlin-partea-2.md`
- `docs/conversie-java-la-kotlin-partea-3.md`
- `docs/conversie-java-la-kotlin-partea-4.md`
- `docs/conversie-java-la-kotlin-partea-5.md`
- `docs/rezumat-conversie-java-la-kotlin.md`
- `docs/kotlin-migration-tracker.md`

Motiv:
- aliniaza documentatia la starea reala a migrarii (95% Kotlin in core, module si teste convertite, slice-uri noi)

### KOT-142

Data: 2026-05-19
ID: KOT-142
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 2

Fisiere adaugate:
- `DebugDumpIO.kt`

Fisiere modificate:
- `DebugDumpService.java` (delegare IO catre utilitar Kotlin)

Gate local:
- `.\gradlew.bat :ainpc-core-plugin:compileJava --rerun-tasks` (PASS)

Observatii:
- Slice incremental pentru conversia `DebugDumpService`: operatiile de IO (`writeText`, `writeJson`, citire `latest.log`) au fost mutate in Kotlin fara schimbare functionala.

### KOT-143

Data: 2026-05-19
ID: KOT-143
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 2

Fisiere adaugate:
- `DebugDumpFormatting.kt`

Fisiere modificate:
- `DebugDumpService.java` (delegare formatting/config/openai helpers catre Kotlin)

Gate local:
- `.\gradlew.bat :ainpc-core-plugin:compileJava --rerun-tasks` (PASS)

Observatii:
- `normalizeScope`, `sanitizeConfig` si `buildOpenAiInfo` au fost mutate in utilitar Kotlin, cu acelasi comportament functional.

### KOT-144

Data: 2026-05-20
ID: KOT-144
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 2

Fisiere adaugate:
- `DebugDumpServerSnapshot.kt`

Fisiere modificate:
- `DebugDumpService.java` (delegare summary/server snapshot catre Kotlin)

Gate local:
- `.\gradlew.bat :ainpc-core-plugin:compileJava --rerun-tasks` (PASS)

Observatii:
- `summary.txt` si `server.txt` sunt construite acum din Kotlin, fara schimbare de format intentionata.

### KOT-145

Data: 2026-05-20
ID: KOT-145
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 2

Fisiere adaugate:
- `DebugDumpNpcJson.kt`

Fisiere modificate:
- `DebugDumpService.java` (delegare `npcs.json` catre Kotlin)

Gate local:
- `.\gradlew.bat :ainpc-core-plugin:compileJava --rerun-tasks` (PASS)

Observatii:
- Serializarea JSON pentru NPC-uri si `owned_locations` a fost mutata in Kotlin; avertismentele introduse initial de nullability au fost curatate.

### KOT-146

Data: 2026-05-20
ID: KOT-146
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 2

Fisiere adaugate:
- `DebugDumpWorldJson.kt`

Fisiere modificate:
- `DebugDumpService.java` (delegare `world-mapping.json` catre Kotlin)

Gate local:
- `.\gradlew.bat :ainpc-core-plugin:compileJava --rerun-tasks` (PASS)

Observatii:
- Serializarea JSON pentru world mapping (`regions`, `places`, `nodes`, `semantic_index`) a fost mutata in Kotlin folosind helper-ele existente din `DebugDumpSupport`.

### KOT-147

Data: 2026-05-20
ID: KOT-147
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 1

Fisiere modificate:
- `DebugDumpSupport.kt`
- `DebugDumpService.java`

Gate local:
- `.\gradlew.bat :ainpc-core-plugin:compileJava --rerun-tasks` (PASS)

Observatii:
- `addStoredJson` a fost mutat in Kotlin si este consumat prin importul static existent din `DebugDumpService`.
- MCP context server a fost verificat in acelasi flux cu `context_pack`; logul Docker a ramas fara `Remote embeddings failed`.

### KOT-148

Data: 2026-05-20
ID: KOT-148
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 1

Fisiere modificate:
- `DebugDumpWorldJson.kt`
- `DebugDumpService.java`

Gate local:
- `.\gradlew.bat :ainpc-core-plugin:compileJava --rerun-tasks` (PASS)

Observatii:
- `buildWorldMappingSemanticIndexForAudit` a fost mutat in Kotlin si este apelat din auditul de questuri.
- MCP context server a fost folosit pentru context inaintea editarii; logul Docker a ramas curat.

### KOT-149

Data: 2026-05-20
ID: KOT-149
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 1

Fisiere modificate:
- `DebugDumpSupport.kt`
- `DebugDumpService.java`

Gate local:
- `.\gradlew.bat :ainpc-core-plugin:compileJava --rerun-tasks` (PASS)

Observatii:
- Helper-ele `hasStoryEventProgressionKey`, `addStoryEventProgressionKey` si `storyEventProgressionKey` au fost mutate in Kotlin si sunt consumate prin importul static existent.
- MCP context server a fost folosit pentru context inaintea editarii; logul Docker a ramas curat.

### KOT-150

Data: 2026-05-20
ID: KOT-150
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 1

Fisiere modificate:
- `DebugDumpSupport.kt`
- `DebugDumpService.java`

Gate local:
- `.\gradlew.bat :ainpc-core-plugin:compileJava --rerun-tasks` (PASS)

Observatii:
- Helper-ele `questEntryStage`, `isQuestRuntimeStage`, `collectQuestObjectiveReferences`, `questStageReferencesObjective` si `stageReferencesObjective` au fost mutate in Kotlin si sunt consumate prin importul static existent.
- Warning-urile Kotlin de nullability aparute initial in slice au fost curatate inainte de validare.
- MCP context server a fost folosit pentru context inaintea editarii; logul Docker a ramas curat.

### KOT-151

Data: 2026-05-20
ID: KOT-151
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 1

Fisiere modificate:
- `DebugDumpSupport.kt`
- `DebugDumpService.java`

Gate local:
- `.\gradlew.bat :ainpc-core-plugin:compileJava --rerun-tasks` (PASS)

Observatii:
- Helper-ele `addScenarioLookupKey`, `findScenarioForProgressionRow` si `hasRecordStoryEventAction` au fost mutate in Kotlin si sunt consumate prin importul static existent.
- Slice-ul pastreaza `buildProgressionScenarioLookup` in Java pentru ca depinde direct de `plugin`, dar scoate lookup-ul pur in `DebugDumpSupport`.
- MCP context server a fost folosit pentru context inaintea editarii.

### KOT-152

Data: 2026-05-20
ID: KOT-152
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 1

Fisiere modificate:
- `DebugDumpSupport.kt`
- `DebugDumpService.java`

Gate local:
- `.\gradlew.bat :ainpc-core-plugin:compileJava --rerun-tasks` (PASS)

Observatii:
- Helper-ul `findRecordStoryEventAction` a fost mutat in Kotlin si ramane apelat din `storyProgressionLinkJson` prin importul static existent.
- Conversia pastreaza fallback-ul pe primul reward `record_story_event` cand `event_key` nu potriveste explicit.

### KOT-153

Data: 2026-05-20
ID: KOT-153
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 1

Fisiere modificate:
- `DebugDumpSupport.kt`
- `DebugDumpService.java`

Gate local:
- `.\gradlew.bat :ainpc-core-plugin:compileJava --rerun-tasks` (PASS)

Observatii:
- Helper-ele `questStagesJson` si `questStageJson` au fost mutate in Kotlin si primesc explicit instanta `Gson` din `DebugDumpService`.
- Conversia pastreaza aceleasi campuri JSON pentru stage-uri: `id`, `description`, `completion_mode`, `next_stage`, `objective_ids`, `metadata`.
- Prima rulare a prins diferenta de interop pentru `getNextStageId()`, corectata inainte de validarea finala.

### KOT-133

Data: 2026-05-17
ID: KOT-133
Status: validat local
Zona: `ro.ainpc.npc`
Tip: productie
Risc: 5

Fisiere adaugate: AINPC.kt
Fisiere sterse: AINPC.java

Gate local:
- `.\gradlew.bat :ainpc-core-plugin:clean :ainpc-core-plugin:compileJava --rerun-tasks` (PASS)
- `.\gradlew.bat clean build` (PASS)

Observatii interop:
- constructorul `AINPC` accepta acum `AINPCPlugin?` pentru compatibilitate cu testele Java (`new AINPC(null)`).
- metoda `isProfileCreated()` a fost pastrata explicit pentru call-site-urile Java.
- call-site-urile Kotlin care foloseau `getX()/setX()` pe `AINPC` au fost normalizate la proprietati Kotlin in ecranele GUI/engine afectate.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 168 fisiere Kotlin, 10 fisiere Java (~94.4% Kotlin)

Rollback:
- sterge fisierul Kotlin si readauga `AINPC.java` din istoric

### KOT-154

Data: 2026-05-21
ID: KOT-154
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandModels.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- IntelliJ build targetat pentru fisierele slice-ului (`AINPCCommand.java`, `AINPCCommandModels.kt`) (PASS)

Observatii:
- Record-urile interne din `AINPCCommand` (`StoryContextTarget`, `StoryEventTarget`, `QuestDecisionTarget`, `QuestTrackRequest`, `WorldCommandLocation`, `QuestLogRequest`, `HouseholdMetadataBackfillInputs`, `ProgressionAliasConfig`) au fost mutate in Kotlin.
- Clasele Kotlin expun explicit aceleasi accesorii de tip record (`player()`, `kind()`, `worldName()`, etc.) pentru a pastra call-site-urile Java nemodificate.
- Rebuild-ul global al proiectului ramane rosu din cauza unor erori preexistente in testele Kotlin, fara legatura cu acest slice.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 196 fisiere Kotlin, 4 fisiere Java (~98.0% Kotlin)

Rollback:
- sterge `AINPCCommandModels.kt` si readauga record-urile din `AINPCCommand.java` din istoric

### KOT-155

Data: 2026-05-22
ID: KOT-155
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 2

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/debug/DebugDumpAudit.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/debug/DebugDumpService.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- IntelliJ build targetat pentru fisierele slice-ului (`DebugDumpService.java`, `DebugDumpAudit.kt`) (PASS)
- `.\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)

Observatii:
- Auditul general pentru debug dump (`buildAuditText`) a fost mutat in Kotlin.
- Cautarea NPC-ului incarcat dupa selector a fost mutata in acelasi helper Kotlin si reutilizata din `DebugDumpService`.
- Verificarea `uuid == null` din audit nu a fost portata, deoarece `AINPC.uuid` este non-null in modelul Kotlin curent.
- `JAVA_HOME` din shell-ul default era invalid/indisponibil; validarea Gradle a fost rulata cu `JAVA_HOME` setat doar pentru comanda.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 197 fisiere Kotlin, 4 fisiere Java (~98.0% Kotlin)

Rollback:
- sterge `DebugDumpAudit.kt` si readauga metodele mutate in `DebugDumpService.java` din istoric

### KOT-156

Data: 2026-05-22
ID: KOT-156
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 3

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/debug/DebugDumpProgressionJson.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/debug/DebugDumpService.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- IntelliJ build targetat pentru fisierele slice-ului (`DebugDumpService.java`, `DebugDumpProgressionJson.kt`) (PASS)
- `.\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)

Observatii:
- Exporturile JSON pentru `player-progressions.json`, `player-quest-progress.json` si `quest-anchor-bindings.json` au fost mutate in Kotlin.
- `DebugDumpService` pastreaza orchestration-ul fisierelor de dump si deleaga exporturile de progresie catre `DebugDumpProgressionJson`.
- Accesul la serviciile `lateinit` din plugin este protejat cu `runCatching` pentru a pastra raspunsurile `available=false` in locul unui crash in cazuri de initializare partiala.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 198 fisiere Kotlin, 4 fisiere Java (~98.0% Kotlin)

Rollback:
- sterge `DebugDumpProgressionJson.kt` si readauga metodele mutate in `DebugDumpService.java` din istoric

### KOT-157

Data: 2026-05-22
ID: KOT-157
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 3

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/debug/DebugDumpSpawnPersistenceJson.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/debug/DebugDumpService.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- IntelliJ build targetat pentru fisierele slice-ului (`DebugDumpService.java`, `DebugDumpSpawnPersistenceJson.kt`) (PASS)
- `.\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)

Observatii:
- Exporturile JSON pentru `households.json` si `spawn-batches.json` au fost mutate in Kotlin.
- `DebugDumpService` pastreaza doar orchestration-ul fisierelor si deleaga persistenta spawn/household catre `DebugDumpSpawnPersistenceJson`.
- Blocul `npc-world-bindings.json` ramane in Java pentru un slice separat, deoarece foloseste suplimentar indexuri `WorldAdmin`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 199 fisiere Kotlin, 4 fisiere Java (~98.0% Kotlin)

Rollback:
- sterge `DebugDumpSpawnPersistenceJson.kt` si readauga metodele mutate in `DebugDumpService.java` din istoric

### KOT-158

Data: 2026-05-22
ID: KOT-158
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 3

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/debug/DebugDumpNpcWorldBindingJson.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/debug/DebugDumpService.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- IntelliJ build targetat pentru fisierele slice-ului (`DebugDumpService.java`, `DebugDumpNpcWorldBindingJson.kt`) (PASS)
- `.\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)

Observatii:
- Exportul JSON pentru `npc-world-bindings.json` a fost mutat in Kotlin.
- Helper-ul Kotlin indexeaza `WorldAdmin` local pentru validarea referintelor place/node si reutilizeaza `DebugDumpAudit.findLoadedNpcBySelector`.
- `DebugDumpService` ramane orchestratorul fisierelor de dump si nu mai contine exporturile world/spawn mutate in slice-urile KOT-157/KOT-158.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 200 fisiere Kotlin, 4 fisiere Java (~98.0% Kotlin)

Rollback:
- sterge `DebugDumpNpcWorldBindingJson.kt` si readauga metodele mutate in `DebugDumpService.java` din istoric

### KOT-159

Data: 2026-05-23
ID: KOT-159
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 3

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/debug/DebugDumpStoryStateJson.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/debug/DebugDumpService.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- IntelliJ build targetat pentru fisierele slice-ului (`DebugDumpService.java`, `DebugDumpStoryStateJson.kt`) (PASS)
- `.\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)

Observatii:
- Exportul JSON pentru `story-states.json` a fost mutat in Kotlin.
- `DebugDumpStoryStateJson` pastreaza comportamentul existent pentru tabelele `region_story_state` si `place_story_state`, inclusiv counters si detectia JSON invalid.
- `story-events.json` ramane in Java pentru un slice separat, deoarece include cross-linking cu progresiile.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 201 fisiere Kotlin, 4 fisiere Java (~98.0% Kotlin)

Rollback:
- sterge `DebugDumpStoryStateJson.kt` si readauga metodele mutate in `DebugDumpService.java` din istoric

### KOT-160

Data: 2026-05-24
ID: KOT-160
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 3

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/debug/DebugDumpStoryEventJson.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/debug/DebugDumpService.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)

Observatii:
- Exportul JSON pentru `story-events.json` a fost mutat in Kotlin.
- `DebugDumpStoryEventJson` pastreaza cross-linking-ul cu `player_quests`, inclusiv campul `progression_link` si counters pentru event/scope/template/code/link.
- `DebugDumpService` ramane orchestratorul fisierelor de dump si delega acum atat `story-states.json`, cat si `story-events.json` catre helper-e Kotlin.
- Validarea IntelliJ MCP targetata a expirat in tool dupa 120s; gate-ul conclusiv pentru slice ramane compilarea Gradle.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 202 fisiere Kotlin, 4 fisiere Java (~98.1% Kotlin)

Rollback:
- sterge `DebugDumpStoryEventJson.kt` si readauga metodele mutate in `DebugDumpService.java` din istoric

### KOT-161

Data: 2026-05-24
ID: KOT-161
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 3

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/debug/DebugDumpQuestDefinitionJson.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/debug/DebugDumpService.java`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/debug/DebugDumpStoryEventJson.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)

Observatii:
- Exportul JSON pentru `loaded-quest-definitions.json` a fost mutat in Kotlin.
- `DebugDumpQuestDefinitionJson` pastreaza serializarea scenariilor, contractului efectiv, mecanicilor/progresiilor, rolurilor, obiectivelor si reward-urilor.
- Filtrul `isLoadedQuestDefinitionCandidate` ramane disponibil static pentru auditul Java din `DebugDumpService`.
- Warning-ul Kotlin pentru `scenario.baseType?.name` din `DebugDumpStoryEventJson` a fost curatat in acelasi slice.
- Validarea IntelliJ MCP targetata a expirat in tool dupa 120s; gate-ul conclusiv pentru slice ramane compilarea Gradle.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 203 fisiere Kotlin, 4 fisiere Java (~98.1% Kotlin)

Rollback:
- sterge `DebugDumpQuestDefinitionJson.kt`, revino linia `loaded-quest-definitions.json` din `DebugDumpService.java` si readauga metodele mutate din istoric

### KOT-162

Data: 2026-05-24
ID: KOT-162
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 4

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/debug/DebugDumpQuestAudit.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/debug/DebugDumpService.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)

Observatii:
- `quest-audit-report.txt` este construit acum in Kotlin prin `DebugDumpQuestAudit`.
- Auditul mutat include validarea quest templates, semantic references, stages/next_stage, persistenta `player_quests`, `quest_anchor_bindings`, JSON stocat si consistenta story/progression.
- `DebugDumpService.java` ramane doar orchestratorul dump-ului si nu mai contine logica de audit quest.
- O rescriere PowerShell intermediara a introdus BOM in `DebugDumpService.java`; a fost eliminat inainte de validarea finala.
- Validarea IntelliJ MCP targetata a expirat in tool dupa 120s; gate-ul conclusiv pentru slice ramane compilarea Gradle.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 204 fisiere Kotlin, 4 fisiere Java (~98.1% Kotlin)

Rollback:
- sterge `DebugDumpQuestAudit.kt` si readauga metodele de audit quest in `DebugDumpService.java` din istoric

### KOT-163

Data: 2026-05-24
ID: KOT-163
Status: validat local
Zona: `ro.ainpc.debug`
Tip: productie
Risc: 4

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/debug/DebugDumpService.kt`

Fisiere sterse:
- `ainpc-core-plugin/src/main/java/ro/ainpc/debug/DebugDumpService.java`

Fisiere modificate:
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)
- IntelliJ build targetat pentru `DebugDumpService.kt` si `AINPCCommand.java` (PASS)

Observatii:
- `DebugDumpService` a fost convertit complet la Kotlin dupa ce logica grea fusese extrasa in helper-ele `DebugDump*`.
- `createDump(String)` ramane apelabil din Java si declara `IOException` prin `@Throws(IOException::class)`.
- Clasa nested `DebugDumpResult` pastreaza accessorii record-style `directory()` si `scope()` folosite de `AINPCCommand.java`.
- Fallback-ul Java pentru `questConfig == null` a fost eliminat deoarece Kotlin vede `questConfig` ca non-null in API-ul pluginului curent.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 205 fisiere Kotlin, 3 fisiere Java (~98.6% Kotlin)

Rollback:
- sterge `DebugDumpService.kt` si readauga `DebugDumpService.java` din istoric

### KOT-164

Data: 2026-05-24
ID: KOT-164
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie
Risc: 2

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NpcVillageSnapshot.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)
- IntelliJ build targetat pentru `NpcVillageSnapshot.kt` si `NPCManager.java` (PASS)

Observatii:
- Record-ul privat `VillageSnapshot` din `NPCManager` a fost mutat intr-un model Kotlin intern pachetului.
- `NpcVillageSnapshot` expune accesorii `center()`, `bedLocations()` si `villagerCount()` pentru a pastra call-site-urile Java simple.
- Warning-urile Kotlin afisate de Gradle sunt preexistente in alte fisiere si nu includ fisierele slice-ului.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 206 fisiere Kotlin, 3 fisiere Java (~98.6% Kotlin)

Rollback:
- sterge `NpcVillageSnapshot.kt` si readauga record-ul privat `VillageSnapshot` in `NPCManager.java`

### KOT-165

Data: 2026-05-24
ID: KOT-165
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie
Risc: 2

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NpcRepairCounters.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)
- IntelliJ build targetat pentru `NpcRepairCounters.kt` si `NPCManager.java` (PASS)

Observatii:
- Clasa privata mutabila `RepairCounters` din `NPCManager` a fost mutata in Kotlin ca `NpcRepairCounters`.
- Campurile sunt expuse cu `@JvmField` pentru a pastra accesul direct din Java fara getter/setter changes.
- Warning-urile Kotlin afisate de Gradle sunt preexistente in alte fisiere si nu includ fisierele slice-ului.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 207 fisiere Kotlin, 3 fisiere Java (~98.6% Kotlin)

Rollback:
- sterge `NpcRepairCounters.kt` si readauga clasa privata `RepairCounters` in `NPCManager.java`

### KOT-166

Data: 2026-05-24
ID: KOT-166
Status: validat local
Zona: `ro.ainpc.engine`
Tip: productie
Risc: 2

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/SimpleQuestProfile.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/engine/ScenarioEngine.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)
- IntelliJ build targetat pentru `SimpleQuestProfile.kt` si `ScenarioEngine.java` (PASS)

Observatii:
- Record-ul privat `SimpleQuestProfile` din `ScenarioEngine` a fost mutat intr-un model Kotlin intern pachetului.
- `SimpleQuestProfile` pastreaza constructorul si accessorii folositi de Java: `title()`, `objectiveMaterial()`, `objectiveAmount()`, `rewardMaterial()`, `rewardAmount()`, `objectivePrompt()` si `hint()`.
- Warning-urile Kotlin afisate de Gradle sunt preexistente in alte fisiere si nu includ fisierele slice-ului.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 208 fisiere Kotlin, 3 fisiere Java (~98.6% Kotlin)

Rollback:
- sterge `SimpleQuestProfile.kt` si readauga record-ul privat `SimpleQuestProfile` in `ScenarioEngine.java`

### KOT-167

Data: 2026-05-24
ID: KOT-167
Status: validat local
Zona: `ro.ainpc.engine`
Tip: productie
Risc: 2

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/QuestCheckResults.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/engine/ScenarioEngine.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)
- IntelliJ build targetat pentru `QuestCheckResults.kt` si `ScenarioEngine.java` (PASS)

Observatii:
- Record-urile private `QuestInventoryCheck` si `QuestObjectiveCheck` din `ScenarioEngine` au fost mutate in Kotlin.
- `QuestCheckResults.kt` pastreaza accessorii Java `complete()`, `missingItems()` si `missingObjectives()`.
- Warning-urile Kotlin afisate de Gradle sunt preexistente in alte fisiere si nu includ fisierele slice-ului.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 209 fisiere Kotlin, 3 fisiere Java (~98.6% Kotlin)

Rollback:
- sterge `QuestCheckResults.kt` si readauga record-urile private `QuestInventoryCheck` si `QuestObjectiveCheck` in `ScenarioEngine.java`

### KOT-168

Data: 2026-05-24
ID: KOT-168
Status: validat local
Zona: `ro.ainpc.engine`
Tip: productie
Risc: 2

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/StoryActionTarget.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/engine/ScenarioEngine.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)
- IntelliJ build targetat pentru `StoryActionTarget.kt` si `ScenarioEngine.java` (PASS)

Observatii:
- Record-ul privat `StoryActionTarget` din `ScenarioEngine` a fost mutat intr-un model Kotlin intern pachetului.
- `StoryActionTarget` pastreaza accessorii Java `scopeType()`, `scopeId()`, `regionId()` si `placeId()`.
- Warning-urile Kotlin afisate de Gradle sunt preexistente in alte fisiere si nu includ fisierele slice-ului.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 210 fisiere Kotlin, 3 fisiere Java (~98.6% Kotlin)

Rollback:
- sterge `StoryActionTarget.kt` si readauga record-ul privat `StoryActionTarget` in `ScenarioEngine.java`

### KOT-169

Data: 2026-05-24
ID: KOT-169
Status: validat local
Zona: `ro.ainpc.engine`
Tip: productie
Risc: 3

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/QuestValidationResults.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/engine/ScenarioEngine.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)
- IntelliJ build targetat pentru `QuestValidationResults.kt` si `ScenarioEngine.java` (PASS)

Observatii:
- Record-urile private `QuestAvailability` si `QuestRewardCheck` din `ScenarioEngine` au fost mutate in Kotlin.
- Factory methods Java `allowed()`, `unavailable(...)` si `blocked(...)` sunt pastrate cu `@JvmStatic`.
- Copierea defensiva a listelor de issues este pastrata prin `java.util.List.copyOf(...)`.
- Warning-urile Kotlin afisate de Gradle sunt preexistente in alte fisiere si nu includ fisierele slice-ului.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 211 fisiere Kotlin, 3 fisiere Java (~98.6% Kotlin)

Rollback:
- sterge `QuestValidationResults.kt` si readauga record-urile private `QuestAvailability` si `QuestRewardCheck` in `ScenarioEngine.java`

### KOT-170

Data: 2026-05-24
ID: KOT-170
Status: validat local
Zona: `ro.ainpc.engine`
Tip: productie
Risc: 3

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/QuestTrackingModels.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/engine/ScenarioEngine.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)
- IntelliJ build targetat pentru `QuestTrackingModels.kt` si `ScenarioEngine.java` (PASS)

Observatii:
- Record-urile private `QuestTrackingTarget` si `QuestTrackingStep` din `ScenarioEngine` au fost mutate in Kotlin.
- Normalizarea `null -> ""` pentru `anchorType`, `anchorId`, `label`, `worldName` si `objectiveLabel` este pastrata.
- Accessorii Java record-style folositi de `ScenarioEngine` sunt pastrati: `anchorType()`, `anchorId()`, `label()`, `worldName()`, `x()`, `y()`, `z()`, `hasLocation()`, `objectiveLabel()` si `target()`.
- Warning-urile Kotlin afisate de Gradle sunt preexistente in alte fisiere si nu includ fisierele slice-ului.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 212 fisiere Kotlin, 3 fisiere Java (~98.6% Kotlin)

Rollback:
- sterge `QuestTrackingModels.kt` si readauga record-urile private `QuestTrackingTarget` si `QuestTrackingStep` in `ScenarioEngine.java`

### KOT-171

Data: 2026-05-24
ID: KOT-171
Status: validat local
Zona: `ro.ainpc.engine`
Tip: productie
Risc: 4

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/PlayerQuestProgress.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/engine/ScenarioEngine.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)
- IntelliJ build targetat pentru `PlayerQuestProgress.kt` si `ScenarioEngine.java` (PASS)

Observatii:
- Record-ul privat `PlayerQuestProgress` si enum-ul privat `QuestStatus` din `ScenarioEngine` au fost mutate in Kotlin impreuna, deoarece modelul depinde direct de enum.
- Accessorii Java record-style pentru progres sunt pastrati: `templateId()`, `questCode()`, `status()`, `startedAt()`, `completedAt()`, `updatedAt()`, `currentPhase()`, `objectiveProgress()` si `questVariables()`.
- Helper-ele de stare sunt pastrate: `isCurrent()`, `isOffered()`, `isActive()`, `isCompleted()`, `QuestStatus.isArchived()`, `QuestStatus.storageValue()` si `QuestStatus.fromStorage(...)`.
- Normalizarea `currentPhase == null ? "" : currentPhase` si copierea defensiva `Collections.unmodifiableMap(new LinkedHashMap<>(...))` sunt pastrate in Kotlin.
- Dupa acest slice, `ScenarioEngine.java` nu mai contine `private record`.
- Warning-urile Kotlin afisate de Gradle sunt preexistente in alte fisiere si nu includ fisierele slice-ului.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 213 fisiere Kotlin, 3 fisiere Java (~98.6% Kotlin)

Rollback:
- sterge `PlayerQuestProgress.kt` si readauga record-ul privat `PlayerQuestProgress` si enum-ul privat `QuestStatus` in `ScenarioEngine.java`

### KOT-172

Data: 2026-05-24
ID: KOT-172
Status: validat local
Zona: `ro.ainpc.engine`
Tip: productie
Risc: 3

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/QuestStateEnums.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/engine/ScenarioEngine.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)
- IntelliJ build targetat pentru `QuestStateEnums.kt` si `ScenarioEngine.java` (PASS)

Observatii:
- Enum-urile private `QuestDialogueContext` si `QuestObjectiveState` din `ScenarioEngine` au fost mutate in Kotlin.
- `QuestDialogueContext.dialogueKeys()` si `QuestObjectiveState.id()` / `displayName()` sunt pastrate pentru call-site-urile Java existente.
- Warning-urile Kotlin afisate de Gradle sunt preexistente in alte fisiere si nu includ fisierele slice-ului.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 214 fisiere Kotlin, 3 fisiere Java (~98.6% Kotlin)

Rollback:
- sterge `QuestStateEnums.kt` si readauga enum-urile private `QuestDialogueContext` si `QuestObjectiveState` in `ScenarioEngine.java`

### KOT-173

Data: 2026-05-24
ID: KOT-173
Status: validat local
Zona: `ro.ainpc.engine`
Tip: productie
Risc: 4

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/QuestLogFilter.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/engine/ScenarioEngine.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)
- IntelliJ build targetat pentru `QuestLogFilter.kt` si `ScenarioEngine.java` (PASS)

Observatii:
- Enum-ul privat `QuestLogFilter` din `ScenarioEngine` a fost mutat in Kotlin.
- Constantele, `displayName()`, `showsCurrent()` si `showsArchived()` pastreaza comportamentul din switch-urile Java.
- Dupa acest slice, `ScenarioEngine.java` nu mai contine `private record` sau `private enum`; raman doar API-urile nested publice (`ScenarioType`, `QuestGui*`, `QuestTrackingMarker`).
- Warning-urile Kotlin afisate de Gradle sunt preexistente in alte fisiere si nu includ fisierele slice-ului.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 215 fisiere Kotlin, 3 fisiere Java (~98.6% Kotlin)

Rollback:
- sterge `QuestLogFilter.kt` si readauga enum-ul privat `QuestLogFilter` in `ScenarioEngine.java`

### KOT-174

Data: 2026-05-24
ID: KOT-174
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/QuestAnchorBindingRow.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)
- IntelliJ build targetat pentru `QuestAnchorBindingRow.kt` si `AINPCCommand.java` (PASS)

Observatii:
- Record-ul privat `QuestAnchorBindingRow` din `AINPCCommand` a fost mutat in Kotlin.
- Constructorul si accessorii Java record-style sunt pastrati pentru query/format/audit call-site-uri.
- Nu a fost introdusa normalizare noua; valorile nullable din `ResultSet` raman nullable.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 216 fisiere Kotlin, 3 fisiere Java (~98.6% Kotlin)

Rollback:
- sterge `QuestAnchorBindingRow.kt` si readauga record-ul privat `QuestAnchorBindingRow` in `AINPCCommand.java`

### KOT-175

Data: 2026-05-24
ID: KOT-175
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 3

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AuditReport.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)
- IntelliJ build targetat pentru `AuditReport.kt`, `QuestAnchorBindingRow.kt` si `AINPCCommand.java` (PASS)

Observatii:
- Clasa privata mutabila `AuditReport` din `AINPCCommand` a fost mutata in Kotlin.
- Listele `errors`, `warnings` si `infos` sunt expuse cu `@JvmField` pentru a pastra accesul direct existent din Java.
- Metodele `error(...)`, `warn(...)` si `info(...)` raman apelabile din `AINPCCommand`.
- Dupa acest slice, `AINPCCommand.java` nu mai contine tipuri private nested.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 217 fisiere Kotlin, 3 fisiere Java (~98.6% Kotlin)

Rollback:
- sterge `AuditReport.kt` si readauga clasa privata `AuditReport` in `AINPCCommand.java`

### KOT-176

Data: 2026-05-24
ID: KOT-176
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 3

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)
- IntelliJ build targetat pentru `AINPCCommandText.kt` si `AINPCCommand.java` (PASS)

Observatii:
- Helper-ele pure de text/parsing din `AINPCCommand` au fost mutate in Kotlin: `formatBounds`, `formatList`, `formatMap`, `formatCountMap`, `formatOptional`, `formatOnOff`, `parseInt`, `parseDouble`, `shortenBatchValue` si `valueOrDash`.
- Fisierul Kotlin foloseste `@file:JvmName("AINPCCommandText")`, iar Java foloseste static import pentru a pastra call-site-urile curate.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 218 fisiere Kotlin, 3 fisiere Java (~98.6% Kotlin)

Rollback:
- sterge `AINPCCommandText.kt` si readauga helper-ele mutate in `AINPCCommand.java`

### KOT-177

Data: 2026-05-24
ID: KOT-177
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 3

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)
- IntelliJ build targetat pentru `AINPCCommandText.kt` si `AINPCCommand.java` (PASS)

Observatii:
- Helper-ul `normalizeAuditKey` a fost mutat in Kotlin.
- Comportamentul Java `String.toLowerCase()` a fost pastrat prin `Locale.getDefault()`.
- Doua method references `this::normalizeAuditKey` au fost convertite in lambdas catre helperul static.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 218 fisiere Kotlin, 3 fisiere Java (~98.6% Kotlin)

Rollback:
- muta `normalizeAuditKey` inapoi in `AINPCCommand.java` si revino la `this::normalizeAuditKey` unde era cazul

### KOT-178

Data: 2026-05-24
ID: KOT-178
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 3

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)
- IntelliJ build targetat pentru `AINPCCommandText.kt` si `AINPCCommand.java` (PASS)

Observatii:
- Helper-ele pure de comparare/fallback string au fost mutate in Kotlin: `equalsIgnoreCase`, `sameNonBlankIgnoreCase`, `sameOptionalId`, `isNoneSelector`, `firstNonBlank` si `firstNonBlankFromMap`.
- Call-site-urile Java folosesc in continuare aceleasi nume prin static import.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 218 fisiere Kotlin, 3 fisiere Java (~98.6% Kotlin)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java` si sterge implementarea lor din `AINPCCommandText.kt`

### KOT-179

Data: 2026-05-24
ID: KOT-179
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 3

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` cu `JAVA_HOME` setat local la JDK 25 (PASS)
- IntelliJ build targetat pentru `AINPCCommandText.kt` si `AINPCCommand.java` (PASS)

Observatii:
- Helper-ele pure audit/story au fost mutate in Kotlin: `jsonString`, `safeAuditValue`, `questReferencePrefix`, `normalizeStoryActionScope`, `hasAnyMetadata` si `hasAnyMapEntry`.
- `jsonString` pastreaza dependenta Gson strict in helperul Kotlin, fara schimbari de schema sau payload.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 218 fisiere Kotlin, 3 fisiere Java (~98.6% Kotlin)

Rollback:
- readauga helper-ele audit/story mutate in `AINPCCommand.java` si sterge implementarea lor din `AINPCCommandText.kt`

### KOT-180

Data: 2026-06-01
ID: KOT-180
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie
Risc: 2

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/ManagedVillagerAuditIssue.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/DuplicateRepairResult.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.commands.AINPCTabCompleterTest` (PASS)

Observatii:
- Record-urile interne `ManagedVillagerAuditIssue` si `DuplicateRepairResult` au fost mutate din `NPCManager.java` in Kotlin top-level.
- Accessorii Java-style (`error()`, `message()`, `applied()`, `actions()` etc.) au fost pastrati pentru call-site-urile Java existente.
- Factory-urile `ManagedVillagerAuditIssue.error(...)` si `warning(...)` raman disponibile din Java prin `@JvmStatic`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~64.7% Kotlin dupa linii)

Rollback:
- readauga cele doua record-uri la finalul `NPCManager.java` si sterge fisierele Kotlin nou create

### KOT-181

Data: 2026-06-01
ID: KOT-181
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.commands.AINPCTabCompleterTest --tests ro.ainpc.CoreNeutralityStaticAuditTest --tests ro.ainpc.StorageDialectStaticAuditTest` (PASS)

Observatii:
- Helper-ele pure `parseIntegerStrict`, `parseDoubleStrict`, `overlaps`, `validBounds`, `matchesAnyToken` si `requiresWorkAnchor` au fost mutate in Kotlin.
- Call-site-urile Java folosesc in continuare aceleasi nume prin static import-ul existent din `AINPCCommandText`.
- Varargs si return types nullable raman compatibile cu Java (`Integer`/`Double` boxed).

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~64.8% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java` si sterge implementarile lor din `AINPCCommandText.kt`

### KOT-182

Data: 2026-06-01
ID: KOT-182
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.commands.AINPCTabCompleterTest` (PASS)

Observatii:
- Helper-ele de afisare `formatLocation` si `formatDistance` au fost mutate in Kotlin.
- Comportamentul textelor existente a fost pastrat: `necunoscuta`, `alta lume`, respectiv formatul cu o zecimala.
- Call-site-urile Java continua sa foloseasca aceleasi nume prin static import-ul `AINPCCommandText`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~64.8% Kotlin dupa linii)

Rollback:
- readauga `formatLocation` si `formatDistance` in `AINPCCommand.java` si sterge implementarile lor din `AINPCCommandText.kt`

### KOT-183

Data: 2026-06-01
ID: KOT-183
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.commands.AINPCTabCompleterTest --tests ro.ainpc.world.VillagePatchPlannerTest` (PASS)

Observatii:
- Helper-ele pure `formatMappingPoint`, `joinArgs`, `parsePatchProfessionList` si `formatListOrNone` au fost mutate in Kotlin.
- `joinArgs` pastreaza comportamentul de concatenare cu spatiu din indexul normalizat la minim 0.
- `parsePatchProfessionList` pastreaza tratarea valorilor goale si `-` ca lista vida.
- Rularea extinsa care a inclus `MappingDraftFactoryTest` a esuat in `createsAndAppliesPlaceDraftInsideSelectedRegion` pe assert-ul pentru tag-ul `blacksmith`; testul nu trece prin helper-ele mutate in acest slice.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~64.9% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java` si sterge implementarile lor din `AINPCCommandText.kt`

### KOT-184

Data: 2026-06-01
ID: KOT-184
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.commands.AINPCTabCompleterTest --tests ro.ainpc.world.VillagePatchPlannerTest` (PASS)

Observatii:
- Formatter-ele pure `formatVillageGap`, `formatPatchCandidate` si `formatPatchPlan` au fost mutate in Kotlin.
- Method references Java `this::...` au fost inlocuite cu lambda-uri simple catre helper-ele top-level.
- Formatul textelor pentru patch gap/candidate/plan a ramas echivalent cu implementarea Java.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~64.9% Kotlin dupa linii)

Rollback:
- readauga formatter-ele mutate in `AINPCCommand.java` si revino lambda-urile la `this::...`

### KOT-185

Data: 2026-06-01
ID: KOT-185
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.commands.AINPCTabCompleterTest --tests ro.ainpc.story.StoryStateServiceTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `formatStoryEvent`, `formatStoryMetadata`, `inferRegionIdFromPlaceId`, `formatStoryTime`, `auditNpcLabel` si `sanitizeForChat` au fost mutate in Kotlin.
- `formatStoryTime` pastreaza pattern-ul `yyyy-MM-dd HH:mm:ss` si `ZoneId.systemDefault()`.
- Sanitizarea pentru chat pastreaza eliminarea `&` si inlocuirea newline/carriage return cu spatii.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~65.1% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java` si sterge implementarile lor din `AINPCCommandText.kt`

### KOT-186

Data: 2026-06-01
ID: KOT-186
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.commands.AINPCTabCompleterTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `formatWandSelectionPart`, `formatMappingDraftKind`, `isAuditOptionSupported`, `isStrictQuestAuditOption` si `auditModeLabel` au fost mutate in Kotlin.
- Comportamentul pentru `wand clear punct` ramane mapat la `point`.
- Label-ul de audit pastreaza formatul `mode option` cand optiunea este prezenta.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~65.1% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java` si sterge implementarile lor din `AINPCCommandText.kt`

### KOT-187

Data: 2026-06-01
ID: KOT-187
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.commands.AINPCTabCompleterTest --tests ro.ainpc.progression.ProgressionSelectorTest --tests ro.ainpc.progression.ProgressionDefinitionTest` (PASS)

Observatii:
- Helper-ele quest/progression `progressionAliasLogFilter`, `isHelpMode`, `isQuestLogFilter`, `normalizeQuestLogFilter`, `isQuestAcceptMode`, `isQuestDeclineMode`, `commandLabelForKind` si `normalizeProgressionKind` au fost mutate in Kotlin.
- Sinonimele romanesti si englezesti pentru filtrele quest log au fost pastrate.
- Call-site-urile Java continua sa foloseasca aceleasi nume prin static import-ul `AINPCCommandText`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~65.3% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java` si sterge implementarile lor din `AINPCCommandText.kt`

### KOT-188

Data: 2026-06-01
ID: KOT-188
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.commands.AINPCTabCompleterTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.StorageDialectStaticAuditTest` (PASS)

Observatii:
- Helper-ele `formatBatchNpcIdList`, `sameNearbyLocation`, `formatNpcIdentities`, `formatNpcIdentity`, `nodeLabel`, `npcBindingId`, `distanceSquaredToPlaceCenter`, `distanceSquared`, `placeCenterX`, `placeAnchorY` si `placeCenterZ` au fost mutate in Kotlin.
- Formatul listelor de NPC-uri din batch pastreaza limita de 3 ID-uri si sufixul `,+N`.
- Calculele pentru centrul/ancora unui place si distanta pana la node raman echivalente cu implementarea Java.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~65.4% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java` si sterge implementarile lor din `AINPCCommandText.kt`

### KOT-189

Data: 2026-06-01
ID: KOT-189
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.engine.QuestScenarioContractTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele quest/audit `questEntryStage`, `normalizeQuestStageCompletionMode`, `isSupportedQuestStageCompletionMode`, `normalizeQuestStageReference`, `semanticAnchorTypeForObjective`, `questEntryId`, `isLegacyObjectiveProgressKey`, `isSupportedQuestObjectiveType`, `normalizeQuestObjectiveType`, `normalizeQuestRewardType`, `formatObjectiveCandidates` si `lastSelectorSegment` au fost mutate in Kotlin.
- Method references ramase catre `questEntryStage` au fost convertite in lambda-uri catre helperul top-level.
- Normalizarile pentru tipuri de obiective/recompense si referinte stage pastreaza sinonimele existente.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~65.7% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java` si revino lambda-urile la `this::questEntryStage`

### KOT-190

Data: 2026-06-01
ID: KOT-190
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.world.HouseAllocationPlannerTest --tests ro.ainpc.StorageDialectStaticAuditTest` (PASS)

Observatii:
- Helper-ele de clasificare si geometrie pentru mapping/world `isWorkplace`, `isSocialPlace`, `hasPendingOwner`, `placeInsideRegion`, `pointInsidePlace`, `pointInsideRegion`, `placesIntersect`, `isHousePlace`, `parseResidents`, `parsePositiveIntMetadata`, `hasAnySemanticNode` si `nodeMatchesAny` au fost mutate in Kotlin.
- Method references din stream-uri pentru house/workplace/social place au fost convertite in lambda-uri catre helper-ele top-level Kotlin.
- Logica pentru intersectii, includere in regiuni si metadate de rezidenti pastreaza comportamentul Java existent.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~65.9% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java` si revino lambda-urile la method references locale

### KOT-191

Data: 2026-06-01
ID: KOT-191
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `formatQuestAnchorBinding`, `generatedQuestObjectiveKey`, `displayQuestObjectiveKey`, `normalizeQuestObjectiveLookupKey`, `parseNpcIdSelector`, `bindingReferencesAnyPlace`, `sameMappingBinding`, `formatNpcWorldBindingPlaces`, `metadataListContains` si `formatOwnedLocation` au fost mutate in Kotlin.
- Method references catre `displayQuestObjectiveKey` au fost convertite in lambda-uri catre helperul top-level.
- Normalizarea cheilor de objective lookup, formatarea quest anchor bindings si comparatia binding-urilor NPC/world pastreaza comportamentul Java existent.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~66.0% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java` si revino lambda-urile la `this::displayQuestObjectiveKey`

### KOT-192

Data: 2026-06-01
ID: KOT-192
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.CoreNeutralityStaticAuditTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.world.NpcWorldBindingServiceTest` (PASS)

Observatii:
- Helper-ele de audit/repair `hasStoryEventProgressionKey`, `addStoryEventProgressionKey`, `storyEventProgressionKey`, `hasRecordStoryEventAction`, `isQuestAuditCandidate`, `collectKnownQuestReferences`, `addQuestReference`, `isQuestAnchorTypeCompatible`, `ownedLocationInsidePlace`, `isNpcBindingRepairTarget`, `isMappingMetadataRepairTarget`, `isRepairBatchTarget` si `isRepairBatchListAction` au fost mutate in Kotlin.
- Method reference-ul `this::isQuestAuditCandidate` a fost convertit in lambda catre helperul top-level.
- Handler-ele si query-urile DB au ramas in Java; slice-ul muta doar predicate si normalizari deterministe.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~66.2% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java` si revino lambda-ul la `this::isQuestAuditCandidate`

### KOT-193

Data: 2026-06-01
ID: KOT-193
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.CoreNeutralityStaticAuditTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.world.HouseAllocationPlannerTest` (PASS)

Observatii:
- Helper-ele quest audit/world `isQuestRuntimeStage`, `collectQuestObjectiveReferences`, `questStageReferencesObjective`, `stageReferencesObjective`, `extractSourceKeyFromProfileData` si `nodePriorityForAnchor` au fost mutate in Kotlin.
- Validarea quest stages/objective references si scorarea nodurilor pentru ancore pastreaza aceleasi prioritati si normalizari.
- Mutarea initiala a generat doua warning-uri Kotlin pentru conditii redundante; au fost curatate in acelasi slice inainte de gate-ul final.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~66.4% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java`

### KOT-194

Data: 2026-06-01
ID: KOT-194
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.progression.ProgressionSelectorTest --tests ro.ainpc.progression.ProgressionDefinitionTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `routeDirectCommandToQuest`, `routeSubcommandToQuest`, `compactUuid`, `storedProgressionMatchesDefinition` si `storedProgressionMatchesSelector` au fost mutate in Kotlin.
- Rutarea complexa a aliasurilor progression ramane in Java deoarece inca depinde de `progressionAliasSelector`, care foloseste `plugin` si player lookup.
- Semnaturile Kotlin top-level raman apelabile din Java prin static import-ul existent.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~66.5% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java`

### KOT-195

Data: 2026-06-01
ID: KOT-195
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `isTrackedQuestSelector`, `findRegionMatches`, `findPlaceMatches`, `parseRegionTypeStrict`, `parsePlaceTypeStrict` si `parseNodeTypeStrict` au fost mutate in Kotlin.
- Lookup-urile pentru regiuni/locuri pastreaza ordonarea dupa id si potrivirea case-insensitive existenta.
- Parser-ele stricte pastreaza comportamentul Java: tipurile necunoscute devin `null`, iar `custom` explicit ramane valid.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~66.6% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java`

### KOT-196

Data: 2026-06-01
ID: KOT-196
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.world.SemanticVillageMapperTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `toRegionInfo`, `toPlaceInfo` si `toNodeInfo` au fost mutate in Kotlin.
- Conversia pastreaza aceleasi campuri expuse catre DTO-urile API world, inclusiv tags, metadata, owner, story state si story pool.
- Safe-call-urile redundante aparute la mutare au fost eliminate inainte de gate-ul final.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~66.7% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java`

### KOT-197

Data: 2026-06-01
ID: KOT-197
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `findPlaceById`, `findNodeById` si `findPlaceContainingOwnedLocation` au fost mutate in Kotlin.
- Lookup-urile pastreaza potrivirea case-insensitive pe id pentru place/node si primul place care contine anchor-ul NPC.
- Parsing-ul de limite si mesajele de comanda au ramas in Java deoarece produc mesaje catre sender.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~66.8% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java`

### KOT-198

Data: 2026-06-01
ID: KOT-198
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.CoreNeutralityStaticAuditTest --tests ro.ainpc.StorageDialectStaticAuditTest` (PASS)

Observatii:
- Helper-ele `collectSourceKeyDuplicateFindings` si `collectNearbyNameDuplicateFindings` au fost mutate in Kotlin.
- Detectia pastreaza gruparea dupa `source_key` normalizat, ordonarea duplicatelor dupa database id si perechile de nume/locatie apropiata la distanta patrata `2.25`.
- Handler-ele de repair si trimiterea mesajelor catre sender au ramas in Java.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~66.9% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java`

### KOT-199

Data: 2026-06-01
ID: KOT-199
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `inferProfileAnchorPlace`, `inferProfileAnchorNode` si `preserveBindingMetadata` au fost mutate in Kotlin.
- Inferarea anchor-urilor pastreaza conversia coordonatelor prin `floor`, raza de cautare `2.5` si limita de `5` noduri.
- Pastrarea metadata binding conserva `npcUuid`, `npcName`, `familyId` si `createdAt` din binding-ul existent, ca in Java.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~66.9% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java`

### KOT-200

Data: 2026-06-01
ID: KOT-200
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.story.StoryStateServiceTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `hasAmbiguousRegionMatch` si `hasAmbiguousPlaceMatch` au fost mutate in Kotlin.
- Ambele raman wrapper-e simple peste `findRegionMatches`/`findPlaceMatches`, cu acelasi prag `size > 1`.
- Rezolvarea selectorilor si mesajele de ambiguitate raman in Java.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~67.0% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java`

### KOT-201

Data: 2026-06-01
ID: KOT-201
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.world.HouseAllocationPlannerTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `createOwnedLocationFromPlace` si `findBestAnchorNodeForPlace` au fost mutate in Kotlin.
- Selectia nodului pastreaza scorul `priority * 100_000 + distanceSquaredToPlaceCenter` si ignora nodurile cu prioritate negativa.
- Fallback-ul pentru owned location ramane centrul place-ului si y-ul de anchor calculat din bounds.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~67.0% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java`

### KOT-202

Data: 2026-06-01
ID: KOT-202
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `inferNpcWorldBindingFromProfile` si `collectMappingMetadataRepairActions` au fost mutate in Kotlin.
- Inferarea binding-ului pastreaza anchor-urile home/work/social, nodurile apropiate si campurile `source`, `createdAt=0`, `updatedAt=0`.
- Colectarea actiunilor metadata pastreaza aceleasi mesaje candidate pentru `owner_npc_id`, `resident_npc_ids`, `worker_npc_ids` si `social_npc_ids`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~67.2% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java`

### KOT-203

Data: 2026-06-01
ID: KOT-203
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.progression.ProgressionSelectorTest --tests ro.ainpc.progression.ProgressionDefinitionTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `findScenarioForProgression`, `collectObjectiveKeyLookup` si `addObjectiveLookupKey` au fost mutate in Kotlin.
- `findScenarioForProgression` primeste acum explicit `FeaturePackLoader`, astfel incat mesajele si contextul plugin raman in Java.
- Lookup-ul de objective pastreaza cheile `entryId`, `questEntryId`, `itemId` si cheia generata `generatedQuestObjectiveKey`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~67.2% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java` si revino apelul la `findScenarioForProgression(progression)`

### KOT-204

Data: 2026-06-01
ID: KOT-204
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.progression.ProgressionSelectorTest --tests ro.ainpc.progression.ProgressionDefinitionTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `buildProgressionScenarioLookup`, `addScenarioLookupKey` si `findScenarioForQuestAnchorRow` au fost mutate in Kotlin.
- Lookup-ul de scenarii primeste explicit `FeaturePackLoader`, iar apelurile Java au fost actualizate sa paseze `plugin.getFeaturePackLoader()`.
- Cheile de selectie pentru scenarii raman `templateId`, `progressionId`, `definitionId`, `code`, `packId:definitionId` si `packId:mechanicId:definitionId`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~67.3% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java` si revino apelurile la `buildProgressionScenarioLookup()`

### KOT-205

Data: 2026-06-01
ID: KOT-205
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.progression.ProgressionSelectorTest --tests ro.ainpc.progression.ProgressionDefinitionTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ul `validateQuestAnchorObjectiveDefinition` a fost mutat in Kotlin.
- Validarea pastreaza warning-ul pentru definitii lipsa, lista de candidati limitata la 8 si comparatia `objective_type` normalizata.
- `validateQuestAnchorTarget` ramane in Java deoarece inca depinde de lookup-ul NPC-urilor incarcate prin comanda.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~67.4% Kotlin dupa linii)

Rollback:
- readauga helper-ul mutat in `AINPCCommand.java`

### KOT-206

Data: 2026-06-01
ID: KOT-206
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.world.HouseAllocationPlannerTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `findLoadedNpcBySelector`, `ownerMatchesLoadedNpc` si `validateQuestAnchorTarget` au fost mutate in Kotlin.
- Lookup-ul NPC-urilor incarcate primeste acum explicit colectia de NPC-uri, astfel incat helper-ele Kotlin nu depind de `plugin`.
- Apelurile Java au fost actualizate sa paseze `plugin.getNpcManager().getAllNPCs()` la lookup/validare.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~67.5% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java` si revino apelurile la variantele fara colectia NPC explicita

### KOT-207

Data: 2026-06-01
ID: KOT-207
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 3

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.progression.ProgressionSelectorTest --tests ro.ainpc.progression.ProgressionDefinitionTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `validateQuestObjectiveEntry`, `validateQuestSemanticReferenceExists`, `validateQuestRewardEntry`, `validateMaterialReference`, `validateEntityReference`, `validateQuestSemanticReference` si `validateQuestStoryAction` au fost mutate in Kotlin.
- Validarea pastreaza regulile pentru material/entity Bukkit, prefixele semantice pentru objective references, verificarea world mapping semantic_index si cerintele pentru story actions.
- `validateQuestStoryAction` a fost mutat impreuna cu reward validation deoarece `validateQuestRewardEntry` il apeleaza direct.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~67.8% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java`

### KOT-208

Data: 2026-06-01
ID: KOT-208
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.progression.ProgressionSelectorTest --tests ro.ainpc.progression.ProgressionDefinitionTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `validateQuestEntries` si `validateQuestEntryId` au fost mutate in Kotlin.
- Bucla pastreaza validarea pentru lista goala, duplicate `entry_id`, amount invalid si delegarea catre validarea objective/reward mutata anterior.
- `validateQuestEntries` expune overload Java prin `@JvmOverloads`, astfel incat apelurile existente cu si fara `WorldMappingSemanticIndex` raman compatibile.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~67.9% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java`

### KOT-209

Data: 2026-06-04
ID: KOT-209
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS, cu `JAVA_HOME` setat local la JDK 21)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.progression.ProgressionSelectorTest --tests ro.ainpc.progression.ProgressionDefinitionTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, warning-uri existente de unchecked cast in testul de routing)

Observatii:
- Helper-ele `findScenarioForProgressionRow`, `parseStoredJsonObject`, `validateProgressionMechanicDefinitions`, `validateQuestProgressionMetadata`, `validateQuestPrerequisites`, `validateQuestRepeatability`, `validateQuestPhases`, `validateQuestDialogues` si `validateQuestGiverRole` au fost mutate in Kotlin.
- Call-site-urile Java raman neschimbate si folosesc static import-ul existent din `AINPCCommandText`.
- Validarile pastreaza mesajele pentru mechanics/progression, prerequisite-uri, repeatability, phases, quest dialogues si rolul `QUEST_GIVER`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~68.2% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java`

### KOT-210

Data: 2026-06-04
ID: KOT-210
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS, cu `JAVA_HOME` setat local la JDK 21)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.progression.ProgressionSelectorTest --tests ro.ainpc.progression.ProgressionDefinitionTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, warning-uri existente de unchecked cast in testul de routing)

Observatii:
- Helper-ele `validateQuestObjectiveStages`, `validateQuestStageDefinitions` si `validateQuestStageNextStage` au fost mutate in Kotlin.
- Validarea pastreaza detectia pentru objective-uri cu phase/stage necunoscut, amestecul de objective-uri staged/unstaged, duplicatele din `objective_ids`, `next_stage` catre sine si next stage fara obiective runtime.
- `AINPCCommand.java` pastreaza doar call-site-ul catre `validateQuestObjectiveStages(...)`, rezolvat prin static import.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~68.4% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java`

### KOT-211

Data: 2026-06-04
ID: KOT-211
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS, cu `JAVA_HOME` setat local la JDK 21)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.progression.ProgressionSelectorTest --tests ro.ainpc.progression.ProgressionDefinitionTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `auditStoryJsonColumn` si `validateQuestTemplate` au fost mutate in Kotlin.
- Validarea quest template pastreaza verificarea `quest.code`, `requires_player`, `quest.giver_profession`, progression metadata, entries, dialogues si stages prin helper-ele mutate anterior.
- `auditStoryJsonColumn` pastreaza fallback-ul `{}`/`[]` si mesajele pentru JSON invalid sau tip JSON gresit.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~68.5% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java`

### KOT-212

Data: 2026-06-04
ID: KOT-212
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS, cu `JAVA_HOME` setat local la JDK 21)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.progression.ProgressionSelectorTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `warnNpcBindingFieldDivergence`, `validateHouseholdPlaceReference`, `validateHouseholdNodeReference`, `validateNpcWorldPlaceBinding` si `validateNpcWorldNodeBinding` au fost mutate in Kotlin.
- Lookup-ul household pastreaza normalizarea cu `normalizeAuditKey`, iar lookup-ul `npc_world_bindings` ramane pe ID-ul brut, ca in Java.
- Validarile pastreaza warning/error pentru place/node lipsa, node in alt place, home non-house, work non-workplace si social non-social clar.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~68.6% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java`

### KOT-213

Data: 2026-06-04
ID: KOT-213
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS, cu `JAVA_HOME` setat local la JDK 21)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.progression.ProgressionSelectorTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `validateProfileJson` si `describeCurrentRow` au fost mutate in Kotlin.
- `describeCurrentRow` expune `@Throws(SQLException::class)` pentru interop Java explicit.
- `validateOwnedLocation` ramane in Java deoarece depinde de `plugin.getServer().getWorld(...)`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~68.7% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java`

### KOT-214

Data: 2026-06-04
ID: KOT-214
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS, cu `JAVA_HOME` setat local la JDK 21)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.commands.AINPCTabCompleterTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `routeProgressionAliasGuiArgs`, `routeProgressionAliasLogArgs` si `auditNpcProfileBindingDivergence` au fost mutate in Kotlin.
- Rutarea alias-urilor pastreaza transformarea pentru `gui` si `log`, inclusiv normalizarea filtrului prin `progressionAliasLogFilter`.
- `routeProgressionAliasSelectorArgs` si `progressionAliasSelector` raman in Java deoarece selectorul foloseste `plugin.getProgressionService()` si player lookup.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~68.8% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java`

### KOT-215

Data: 2026-06-04
ID: KOT-215
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS, cu `JAVA_HOME` setat local la JDK 21)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.world.HouseAllocationPlannerTest --tests ro.ainpc.world.SemanticVillageMapperTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ele `auditWorldReadiness` si `auditNpcSpawnBindings` au fost mutate in Kotlin.
- Readiness-ul pastreaza numararea caselor, locurilor de munca, locurilor sociale, quest/interaction nodes si warning-urile pentru node-uri lipsa.
- `auditHouseSpawnOrder` ramane in Java deoarece rezolva rezidenti prin `plugin.getNpcManager()`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~68.9% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java`

### KOT-216

Data: 2026-06-04
ID: KOT-216
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS, cu `JAVA_HOME` setat local la JDK 21)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.world.MappingDraftFactoryTest --tests ro.ainpc.world.SemanticVillageMapperTest --tests ro.ainpc.CoreNeutralityStaticAuditTest --tests ro.ainpc.StorageDialectStaticAuditTest` (PASS)

Observatii:
- Helper-ele `pointFromPlayer` si `validateMappingWandAuditEntry` au fost mutate in Kotlin.
- Validarea wand audit primeste explicit `WorldAdminApi` si colectia de NPC-uri incarcate, fara dependenta directa de `plugin`.
- Call-site-ul Java paseaza `plugin.getNpcManager().getAllNPCs()` pentru validarea `quest_anchor:npc`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~69.0% Kotlin dupa linii)

Rollback:
- readauga helper-ele mutate in `AINPCCommand.java` si revino apelul `validateMappingWandAuditEntry(report, entry, worldAdmin)`

### KOT-217

Data: 2026-06-04
ID: KOT-217
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS, cu `JAVA_HOME` setat local la JDK 21)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.world.MappingDraftFactoryTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS)

Observatii:
- Helper-ul `questAnchorTargetExists` a fost mutat in Kotlin cu dependente explicite: `WorldAdminApi` si NPC-uri incarcate.
- Apelul din `applyQuestAnchorDraft` pastreaza fallback-ul de eroare cand world admin lipseste sau tinta din mapping/NPC nu exista.
- Overload-ul Kotlin pastreaza verificarea `worldAdmin.isEnabled`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~69.0% Kotlin dupa linii)

Rollback:
- readauga helper-ul `questAnchorTargetExists(String, String)` in `AINPCCommand.java`

### KOT-218

Data: 2026-06-04
ID: KOT-218
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS, cu `JAVA_HOME` setat local la JDK 21)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.world.MappingDraftFactoryTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.world.SemanticVillageMapperTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.CoreNeutralityStaticAuditTest --tests ro.ainpc.StorageDialectStaticAuditTest` (PASS)

Observatii:
- Helper-ul `findNodesAtLocation` a fost mutat in Kotlin.
- Filtrarea pastreaza world name case-insensitive, raza minima `0.0`, calculul distantei patratice si sortarea dupa `WorldNodeInfo.id()`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~69.0% Kotlin dupa linii)

Rollback:
- readauga helper-ul mutat in `AINPCCommand.java`

### KOT-219

Data: 2026-06-04
ID: KOT-219
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS, cu `JAVA_HOME` setat local la JDK 21)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.world.HouseAllocationPlannerTest --tests ro.ainpc.world.SemanticVillageMapperTest --tests ro.ainpc.CoreNeutralityStaticAuditTest --tests ro.ainpc.StorageDialectStaticAuditTest` (PASS)

Observatii:
- Helper-ul `auditHouseSpawnOrder` a fost mutat in Kotlin.
- Dependenta pe NPC-urile incarcate este explicita prin parametrul `loadedNpcs`, iar Java paseaza colectia `npcs` deja folosita in auditul de spawn order.
- Validarile pentru `residents`, `max_residents`/`capacity`, node semantic `bed`/`home`/`npc_spawn`/`spawn`, duplicati si `homeAnchor` in interiorul casei sunt pastrate.
- Importurile devenite nefolosite in `AINPCCommand.java` si `AINPCCommandText.kt` au fost eliminate.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~69.1% Kotlin dupa linii)

Rollback:
- readauga helper-ul `auditHouseSpawnOrder(AuditReport, WorldAdminApi, WorldPlaceInfo)` in `AINPCCommand.java` si revino apelul din `auditSpawnOrder`

### KOT-220

Data: 2026-06-04
ID: KOT-220
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS, cu `JAVA_HOME` setat local la JDK 21)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.world.HouseAllocationPlannerTest --tests ro.ainpc.world.SemanticVillageMapperTest --tests ro.ainpc.CoreNeutralityStaticAuditTest --tests ro.ainpc.StorageDialectStaticAuditTest` (PASS)

Observatii:
- Helper-ul `validateOwnedLocation` a fost mutat in Kotlin si primeste explicit setul de lumi incarcate.
- Helper-ul `auditNpcs` a fost mutat in Kotlin cu dependente explicite: NPC-uri incarcate, lumi incarcate, audit issues pentru villager entities si indexul persistent `source_key`.
- Apelul Java din `handleAudit` pregateste doar datele din `plugin` si deleaga auditul NPC catre Kotlin.
- Verificarea imposibila `uuid == null` a fost eliminata dupa migrare, deoarece tipul expus catre Kotlin este non-null.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~69.3% Kotlin dupa linii)

Rollback:
- readauga `auditNpcs(AuditReport)` si `validateOwnedLocation(AuditReport, String, AINPC.OwnedLocation)` in `AINPCCommand.java` si revino apelul simplu `auditNpcs(report)`

### KOT-221

Data: 2026-06-04
ID: KOT-221
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS, cu `JAVA_HOME` setat local la JDK 21)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.world.HouseAllocationPlannerTest --tests ro.ainpc.world.SemanticVillageMapperTest --tests ro.ainpc.CoreNeutralityStaticAuditTest --tests ro.ainpc.StorageDialectStaticAuditTest` (PASS)

Observatii:
- Helper-ul `auditWorld` a fost mutat in Kotlin.
- Dependentele Java au fost facute explicite: `WorldAdminApi`, setul de lumi incarcate si colectia de NPC-uri incarcate.
- Validarile pentru regiuni/places/nodes, lumi neincarcate, bounds invalide, owner lipsa/neincarcat, workplace fara nodes, readiness si suprapuneri de places sunt pastrate.
- Kotlin foloseste proprietatile `WorldAdminApi.regions`, `places` si `nodes`, nu overload-urile `getPlaces(regionId)`/`getNodes(regionId)`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~69.5% Kotlin dupa linii)

Rollback:
- readauga `auditWorld(AuditReport)` in `AINPCCommand.java` si revino apelul simplu `auditWorld(report)`

### KOT-222

Data: 2026-06-04
ID: KOT-222
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin` (PASS, cu `JAVA_HOME` setat local la JDK 21)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.world.NpcWorldBindingServiceTest --tests ro.ainpc.world.HouseAllocationPlannerTest --tests ro.ainpc.world.SemanticVillageMapperTest --tests ro.ainpc.CoreNeutralityStaticAuditTest --tests ro.ainpc.StorageDialectStaticAuditTest` (PASS)

Observatii:
- Helper-ul `auditMappingWandDrafts` a fost mutat in Kotlin.
- Dependentele sunt explicite: `MappingWandService`, `WorldAdminApi`, NPC-uri incarcate si limita de preview.
- Java pastreaza fallback-ul defensiv pentru `plugin.getPlatform() == null` si doar pregateste datele din servicii.
- Validarea draft-urilor wand confirmate recent, mesajele de preview si warning-ul pentru world admin indisponibil sunt pastrate.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~69.5% Kotlin dupa linii)

Rollback:
- readauga `auditMappingWandDrafts(AuditReport)` in `AINPCCommand.java` si revino apelul simplu `auditMappingWandDrafts(report)`

### KOT-223

Data: 2026-06-05
ID: KOT-223
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- IntelliJ build pe `AINPCCommandText.kt` si `AINPCCommand.java` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)

Observatii:
- Logica `routeProgressionAlias(...)` si helper-ul de selector-routing au fost mutate in Kotlin.
- Java pastreaza un wrapper privat subtire pentru compatibilitate cu testul existent care reflecta metoda `routeProgressionAlias`.
- Mapper-ul pentru selector ramane in Java prin `this::progressionAliasSelector`, deoarece depinde de `plugin`, player online lookup si `ProgressionService.kindSelector(...)`.
- Rutarea `log` si `gui` continua sa foloseasca helper-ele Kotlin existente.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~69.6% Kotlin dupa linii)

Rollback:
- readauga implementarea completa `routeProgressionAlias(...)` si `routeProgressionAliasSelectorArgs(...)` in `AINPCCommand.java`

### KOT-224

Data: 2026-06-05
ID: KOT-224
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`

Gate local:
- IntelliJ build pe `AINPCCommandText.kt` si `AINPCCommand.java` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)

Observatii:
- Helper-ul `progressionAliasSelector` a fost mutat in Kotlin.
- Java nu mai pastreaza helper separat; wrapper-ul `routeProgressionAlias(...)` paseaza catre Kotlin un mapper lazy care furnizeaza `ProgressionService` si resolver-ul de player online.
- Pastrarea accesului lazy evita dereferentierea `plugin` pentru rutele `gui`/`log`, inclusiv testul existent care construieste `AINPCCommand(null)`.
- Regulile pentru selector blank, selector calificat cu `:`, `nearest`, selector tracked si nume de player online sunt pastrate.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~69.6% Kotlin dupa linii)

Rollback:
- readauga helper-ul `progressionAliasSelector(String, String)` in `AINPCCommand.java` si revino mapper-ul din `routeProgressionAlias(...)` la `this::progressionAliasSelector`

### KOT-225

Data: 2026-06-05
ID: KOT-225
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- IntelliJ build pe `AINPCCommandText.kt` si `AINPCCommand.java` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.world.QuestAnchorResolverTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)

Observatii:
- Helper-ele `shouldTreatQuestDecisionArgumentAsPlayer` si `shouldHandleAbandonAsQuestSelector` au fost mutate in Kotlin.
- Java paseaza explicit resolver-ele `NpcManager.getNPCByName` si `findOnlinePlayer`, pastrand dependentele pe runtime in `AINPCCommand`.
- Comportamentul pentru `nearest`, selector gol, NPC existent, player online si selector tracked ramane neschimbat.
- Mutarea pregateste zona `resolveQuestDecisionTarget` / `handleAbandonQuest` pentru extractii Kotlin ulterioare fara a muta inca flow-ul de comanda.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~69.7% Kotlin dupa linii)

Rollback:
- readauga helper-ele `shouldTreatQuestDecisionArgumentAsPlayer(String)` si `shouldHandleAbandonAsQuestSelector(String)` in `AINPCCommand.java` si revino apelurile la metodele private Java

### KOT-226

Data: 2026-06-05
ID: KOT-226
Status: validat local
Zona: `ro.ainpc.commands`
Tip: productie
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `docs/kotlin-migration-tracker.md`

Gate local:
- IntelliJ build pe `AINPCCommandText.kt` si `AINPCCommand.java` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileJava :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:test --tests ro.ainpc.commands.AINPCCommandRoutingTest --tests ro.ainpc.progression.ProgressionSelectorTest --tests ro.ainpc.progression.ProgressionRepositoryTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25; warning JDK 25/SQLite native access existent)

Observatii:
- Helper-ul `resolveProgressionStoredPlayerUuid` a fost mutat in Kotlin.
- Java paseaza resolver-ul `findOnlinePlayer`, iar Kotlin pastreaza ordinea de rezolvare: selector gol/`all`, UUID valid, apoi player online.
- Comportamentul pentru selector necunoscut ramane `null`, ca flow-ul Java sa continue sa trateze argumentul ca filtru.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 229 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~69.7% Kotlin dupa linii)

Rollback:
- readauga helper-ul `resolveProgressionStoredPlayerUuid(String)` in `AINPCCommand.java` si revino apelul la metoda privata Java

### KOT-227

Data: 2026-06-06
ID: KOT-227
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie
Risc: 1

Fisiere adaugate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`

Fisiere modificate:
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- IntelliJ build pe `NPCManagerText.kt` si `NPCManager.java` (PASS; warning-urile Kotlin raportate sunt existente in alte fisiere)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Helper-ele pure `normalizeSourceKey`, `valueOrFallback`, `readInt`, `readLong`, `readDouble`, `readString` si `truncateProfileText` au fost mutate in Kotlin.
- `NPCManager.java` pastreaza apelurile existente prin import static catre `NPCManagerText`, deci flow-ul runtime pentru DB, Bukkit entities si world mapping nu este schimbat.
- Strategia ramane conversie pe slice-uri mici; conversia completa a `NPCManager.java` este inca risc mare fara spargere suplimentara.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~69.8% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 595 fisiere Kotlin, 3 fisiere Java, 84.03% Kotlin dupa linii

Rollback:
- readauga helper-ele mutate in `NPCManager.java` si elimina importul static `NPCManagerText`

### KOT-228

Data: 2026-06-06
ID: KOT-228
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `docs/kotlin-migration-tracker.md`

Fisiere adaugate:
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS; warning-urile Kotlin raportate sunt existente in alte fisiere)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Helper-ele pure `nodeMatchesAny`, `matchesAnyToken`, `normalizeAnchorToken`, `nodeLabel` si `firstNonBlank` au fost mutate in Kotlin.
- Java pastreaza apelurile prin acelasi import static `NPCManagerText`, iar `nodePriority` ramane in Java.
- Testul nou acopera normalizarea `npc-spawn` -> `npc_spawn`, matching pe metadata, fallback-ul de label si citirile JSON mutate in `KOT-227`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~69.8% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.09% Kotlin dupa linii

Rollback:
- readauga helper-ele de node matching/label in `NPCManager.java` si elimina testul `NPCManagerTextTest`

### KOT-229

Data: 2026-06-06
ID: KOT-229
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS; warning-urile Kotlin raportate sunt existente in alte fisiere)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Helper-ul pur `nodePriority` a fost mutat in Kotlin peste helper-ele de matching mutate in `KOT-228`.
- `NPCManager.java` pastreaza doar apelurile `nodePriority(node, anchorRole)` prin import static, fara schimbare in selectia de noduri.
- Testul `NPCManagerTextTest` acopera ordinea de prioritate pentru `home`, `work` si `social`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~69.9% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.13% Kotlin dupa linii

Rollback:
- readauga `nodePriority(WorldNodeInfo, String)` in `NPCManager.java` si elimina testele de prioritate adaugate in `NPCManagerTextTest`

### KOT-230

Data: 2026-06-06
ID: KOT-230
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Helper-ele pure `isHomePlace`, `isWorkPlace`, `isSocialPlace`, `matchesOccupationPlaceType`, `isGenericWorkPlaceType` si `metadataEquals` au fost mutate in Kotlin.
- Referintele Java `this::isHomePlace` / `this::isSocialPlace` au fost inlocuite cu lambda-uri catre helper-ele Kotlin, pentru interop static sigur.
- `matchesOccupationPlaceType` ramane deliberat `false`, identic cu implementarea Java existenta.
- Testul `NPCManagerTextTest` acopera clasificarea home/work/social si regula ca `HOUSE` nu este tratat ca workplace.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~70.0% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.18% Kotlin dupa linii

Rollback:
- readauga helper-ele de clasificare `WorldPlaceInfo` in `NPCManager.java`, revino lambda-urile la metodele private si elimina testele de clasificare place din `NPCManagerTextTest`

### KOT-231

Data: 2026-06-06
ID: KOT-231
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Helper-ele pure `distanceSquaredToPlaceCenter(WorldPlaceInfo, Location)`, `distanceSquaredToPlaceCenter(WorldPlaceInfo, WorldNodeInfo)`, `distanceSquared`, `placeCenterX`, `placeAnchorY` si `placeCenterZ` au fost mutate in Kotlin.
- Java pastreaza apelurile directe prin import static catre `NPCManagerText`, inclusiv overload-ul pentru `Location` si cel pentru `WorldNodeInfo`.
- Testul `NPCManagerTextTest` acopera centrul orizontal, anchor Y, distanta catre node, distanta catre `Location` si helper-ul generic `distanceSquared`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~70.0% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.21% Kotlin dupa linii

Rollback:
- readauga helper-ele de distanta/centru `WorldPlaceInfo` in `NPCManager.java` si elimina testele de distanta din `NPCManagerTextTest`

### KOT-232

Data: 2026-06-06
ID: KOT-232
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Helper-ele pure `isOwnedByNpc` si `normalizeOwnerKey` au fost mutate in Kotlin.
- Testul `NPCManagerTextTest` acopera potrivirea ownership prin UUID, `npc_<databaseId>`, nume normalizat si cazurile negative.
- `AINPC(null)` este suficient pentru test, deoarece helper-ul foloseste doar proprietati de model (`uuid`, `databaseId`, `name`).

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~70.1% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.23% Kotlin dupa linii

Rollback:
- readauga `isOwnedByNpc(WorldPlaceInfo, AINPC)` si `normalizeOwnerKey(String)` in `NPCManager.java` si elimina testele de ownership din `NPCManagerTextTest`

### KOT-233

Data: 2026-06-07
ID: KOT-233
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Helper-ele pure `writeOwnedLocation` si `readOwnedLocation` au fost mutate in Kotlin.
- Java pastreaza apelurile directe prin import static catre `NPCManagerText`.
- Testul `NPCManagerTextTest` acopera round-trip JSON, anchor null fara scriere si respingerea anchor-ului cu world gol.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~70.1% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.27% Kotlin dupa linii

Rollback:
- readauga `writeOwnedLocation(JsonObject, String, AINPC.OwnedLocation)` si `readOwnedLocation(JsonObject, String)` in `NPCManager.java` si elimina testele JSON OwnedLocation din `NPCManagerTextTest`

### KOT-234

Data: 2026-06-07
ID: KOT-234
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Helper-ul pur `isGenericOccupation` a fost mutat in Kotlin.
- Java pastreaza apelurile directe prin import static catre `NPCManagerText`.
- Testul `NPCManagerTextTest` acopera valorile generice recunoscute (`locuitor`, `villager`, `resident`, `localnic`) si ocupatii specifice care trebuie sa ramana non-generice.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~70.2% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.28% Kotlin dupa linii

Rollback:
- readauga `isGenericOccupation(String)` in `NPCManager.java` si elimina testele de generic occupation din `NPCManagerTextTest`

### KOT-235

Data: 2026-06-07
ID: KOT-235
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Helper-ele pure `isWorkstation`, `matchesOccupationWorkstation` si `describeWorkAnchor` au fost mutate in Kotlin.
- `matchesOccupationWorkstation` si `describeWorkAnchor` pastreaza comportamentul Java existent: parametrul `occupation` este ignorat deliberat.
- Testul `NPCManagerTextTest` acopera materiale workstation acceptate, material neacceptat si formatarea label-ului din `Material`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~70.2% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.31% Kotlin dupa linii

Rollback:
- readauga `isWorkstation(Material)`, `matchesOccupationWorkstation(String, Material)` si `describeWorkAnchor(String, Material)` in `NPCManager.java` si elimina testele workstation din `NPCManagerTextTest`

### KOT-236

Data: 2026-06-07
ID: KOT-236
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Helper-ul pur `resolveGender` a fost mutat in Kotlin.
- Java pastreaza apelul direct prin import static catre `NPCManagerText`.
- Testul `NPCManagerTextTest` confirma ca doar `female` case-insensitive ramane special case, iar restul revine la `male`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~70.2% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.31% Kotlin dupa linii

Rollback:
- readauga `resolveGender(String)` in `NPCManager.java` si elimina testele de gender resolver din `NPCManagerTextTest`

### KOT-237

Data: 2026-06-07
ID: KOT-237
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Helper-ul pur `floorToBlock` a fost mutat in Kotlin.
- Java pastreaza apelurile existente prin import static catre `NPCManagerText`.
- Testul `NPCManagerTextTest` acopera explicit semantica `Math.floor`, inclusiv coordonate negative.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~70.2% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.32% Kotlin dupa linii

Rollback:
- readauga `floorToBlock(double)` in `NPCManager.java` si elimina testul `floorToBlockKeepsMathFloorSemantics` din `NPCManagerTextTest`

### KOT-238

Data: 2026-06-07
ID: KOT-238
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Helper-ul pur `safeNpcName` a fost mutat in Kotlin.
- Java pastreaza apelurile existente prin import static catre `NPCManagerText`.
- Testul `NPCManagerTextTest` acopera fallback-ul pentru NPC null, nume blank si nume valid.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~70.2% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.33% Kotlin dupa linii

Rollback:
- readauga `safeNpcName(AINPC)` in `NPCManager.java` si elimina testul `safeNpcNameFallsBackForMissingNames` din `NPCManagerTextTest`

### KOT-239

Data: 2026-06-07
ID: KOT-239
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Decizia `shouldReplacePersistedSourceKeyOwner` a fost mutata in Kotlin.
- Lookup-ul de stare `getNPCById(existingOwnerId)` ramane in `NPCManager.java`; Kotlin primeste doar `currentOwnerExists`.
- Testul `NPCManagerTextTest` acopera candidat invalid, owner invalid, owner lipsa, candidat canonic cu ID mai mic si candidat necanonic cu ID mai mare.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~70.3% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.34% Kotlin dupa linii

Rollback:
- readauga `shouldReplacePersistedSourceKeyOwner(int, int)` in `NPCManager.java`, readu apelul vechi cu 2 parametri si elimina testul `sourceKeyOwnerReplacementKeepsCanonicalRules` din `NPCManagerTextTest`

### KOT-240

Data: 2026-06-07
ID: KOT-240
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Comparatorii `isPreferredSourceKeyCandidate` si `isSameNpcRecord` au fost mutati in Kotlin.
- Java pastreaza apelurile existente prin import static catre `NPCManagerText`.
- Testul `NPCManagerTextTest` acopera prioritatea dupa `databaseId`, fallback-ul dupa UUID si echivalenta prin identitate, ID sau UUID.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~70.3% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.37% Kotlin dupa linii

Rollback:
- readauga `isPreferredSourceKeyCandidate(AINPC, AINPC)` si `isSameNpcRecord(AINPC, AINPC)` in `NPCManager.java` si elimina testele `sourceKeyCandidatePreferenceUsesDatabaseIdThenUuid` si `sameNpcRecordUsesIdentityDatabaseIdThenUuid` din `NPCManagerTextTest`

### KOT-241

Data: 2026-06-07
ID: KOT-241
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Helper-ul pur `generateBackstory` a fost mutat in Kotlin.
- Semnatura Kotlin accepta `name` nullable pentru a pastra comportamentul de concatenare Java daca valoarea ajunge null.
- Parametrul `profession` ramane ignorat deliberat, ca in Java; testul nu forteaza constante `Villager.Profession` in JUnit deoarece Bukkit poate necesita runtime server pentru initializare.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~70.3% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.38% Kotlin dupa linii

Rollback:
- readauga `generateBackstory(String, String, Villager.Profession)` in `NPCManager.java` si elimina testul `generatedBackstoryKeepsOccupationFallbackText` din `NPCManagerTextTest`

### KOT-242

Data: 2026-06-07
ID: KOT-242
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Helper-ele pure `namesMatch` si `isNpcPlannedForDeletion` au fost mutate in Kotlin.
- Java pastreaza apelurile existente prin import static catre `NPCManagerText`.
- Testul `NPCManagerTextTest` acopera compararea case-insensitive/null-safe si verificarea ID-ului pozitiv prezent in setul de stergeri planificate.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~70.3% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.39% Kotlin dupa linii

Rollback:
- readauga `namesMatch(String, String)` si `isNpcPlannedForDeletion(AINPC, Set<Integer>)` in `NPCManager.java` si elimina testele `namesMatchIsCaseInsensitiveAndNullSafe` si `npcPlannedForDeletionRequiresPositiveIdInSet` din `NPCManagerTextTest`

### KOT-243

Data: 2026-06-07
ID: KOT-243
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Helper-ele `isSameNpcLocation`, `formatLocation` si `buildVillageKey` au fost mutate in Kotlin.
- Testul foloseste un `World` proxy minimal, fara dependinte noi, ca sa valideze `Location.world`, `distanceSquared`, formatarea cu `floorToBlock` si cheia grosiera de sat.
- Java pastreaza apelurile existente prin import static catre `NPCManagerText`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~70.4% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.43% Kotlin dupa linii

Rollback:
- readauga `isSameNpcLocation(Location, Location)`, `formatLocation(Location)` si `buildVillageKey(Location)` in `NPCManager.java` si elimina testul `locationHelpersKeepWorldDistanceAndFloorRules` plus helper-ul de test `world(String)` din `NPCManagerTextTest`

### KOT-244

Data: 2026-06-07
ID: KOT-244
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Helper-ul `buildProfileSummary` a fost mutat in Kotlin.
- Testul `NPCManagerTextTest` acopera fallback-ul pentru nume/ocupatie, varsta si gen implicite, trait dominant non-echilibrat si trunchierea `backstory`.
- Java pastreaza apelul existent din `buildProfileData` prin import static catre `NPCManagerText`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~70.4% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.45% Kotlin dupa linii

Rollback:
- readauga `buildProfileSummary(AINPC)` in `NPCManager.java` si elimina testul `profileSummaryKeepsFallbacksTraitsAndBackstoryTruncation` din `NPCManagerTextTest`

### KOT-245

Data: 2026-06-07
ID: KOT-245
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Helper-ul `getVillagerDisplayName` a fost mutat in Kotlin.
- `PlainTextComponentSerializer` este acum file-private in `NPCManagerText.kt`, iar campul static Java nefolosit a fost eliminat.
- Testul foloseste un `Villager` proxy minimal pentru `customName()`, fara dependinte noi si fara server Bukkit.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~70.4% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.47% Kotlin dupa linii

Rollback:
- readauga `getVillagerDisplayName(Villager)` si campul static `PLAIN_TEXT` in `NPCManager.java`, elimina `PLAIN_TEXT` din `NPCManagerText.kt` si elimina testul `villagerDisplayNameSerializesAndTrimsCustomName` plus helper-ul de test `villager(Component?)`

### KOT-246

Data: 2026-06-07
ID: KOT-246
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Helper-ul `buildSpawnState` a fost mutat in Kotlin.
- Testul `NPCManagerTextTest` acopera campurile JSON stabile, coordonatele runtime, chunk key-urile cu `floorToBlock` si timestamp-ul `updated_at` in intervalul curent.
- Java pastreaza apelul existent din `buildProfileData` prin import static catre `NPCManagerText`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~70.5% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.49% Kotlin dupa linii

Rollback:
- readauga `buildSpawnState(AINPC)` in `NPCManager.java` si elimina testul `spawnStateJsonKeepsRuntimeCoordinatesAndChunkKeys` din `NPCManagerTextTest`

### KOT-247

Data: 2026-06-07
ID: KOT-247
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 2

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Input selectat / filtrat:
- Candidati analizati: `generateUniqueAutoName`, `uniquifyNpcName`, `isNpcNameTaken`, `inferOccupationFromPrimaryScenario`, `mapProfessionToOccupation`, `buildProfileData`, `readPersistentString`, `isLegacyPluginVillager`, `matchesPersistentIdentity`
- Filtru aplicat: fara DB, fara registry Bukkit/Paper runtime, fara acces direct la harti interne manager, fara `plugin` ca dependinta ascunsa
- Input ales: `buildProfileData(AINPC)`, deoarece este JSON determinist din `AINPC`, cu o singura dependinta externa izolata explicit ca `Gson`

Microtaskuri:
- `buildProfileData` a fost mutat in Kotlin cu semnatura `buildProfileData(AINPC, Gson)`.
- Apelul Java din `persistProfileData` a fost actualizat sa paseze campul existent `gson`.
- Metoda Java duplicata a fost eliminata.
- Testul `NPCManagerTextTest` parseaza JSON-ul rezultat si verifica campuri stabile din profil, `traits`, `spawn_state`, `simulation`, `personality`, `emotions` si `owned_locations`.

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Campul `gson` ramane in `NPCManager.java`, deoarece este folosit si pentru `fromJson` la hidratarea profilului.
- Importul Java `JsonArray` ramas nefolosit a fost eliminat.
- Testul evita compararea stringului JSON brut si valideaza structura parsata, pentru stabilitate fata de timestamp-ul `updated_at`.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~70.6% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.56% Kotlin dupa linii

Rollback:
- readauga `buildProfileData(AINPC)` in `NPCManager.java`, readu apelul `buildProfileData(npc)` in `persistProfileData`, elimina `buildProfileData(AINPC, Gson)` din `NPCManagerText.kt` si elimina testul `profileDataJsonKeepsNestedRuntimeAndProfileFields` din `NPCManagerTextTest`

### KOT-248

Data: 2026-06-07
ID: KOT-248
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`

Input selectat / filtrat:
- Candidati analizati: `inferOccupationFromPrimaryScenario`, `generatePersonalityForProfession`, `generatePersonalityForOccupation`, `mapProfessionToOccupation`
- Filtru aplicat: fara `plugin`, fara registry Bukkit/Paper runtime, fara constante `Villager.Profession` in test JUnit
- Input ales: `generatePersonalityForProfession` si `generatePersonalityForOccupation`, deoarece codul existent doar returneaza `NPCPersonality.generateRandom()`

Microtaskuri:
- `generatePersonalityForProfession` a fost mutat in Kotlin.
- `generatePersonalityForOccupation` a fost mutat in Kotlin si pastreaza semnatura Java interop.
- Metodele Java duplicate au fost eliminate.
- Testul `NPCManagerTextTest` valideaza invariantul rezultatului randomizat: toate trait-urile raman clamp-uite in intervalul `0.0..1.0`.

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- `occupation` si `profession` raman parametri pastrati pentru compatibilitate, dar sunt ignorati deliberat ca in comportamentul Java actual.
- Testul paseaza `profession = null` pentru a evita initializarea runtime Bukkit in JUnit.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~70.6% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.58% Kotlin dupa linii

Rollback:
- readauga `generatePersonalityForProfession(Villager.Profession)` si `generatePersonalityForOccupation(String, Villager.Profession)` in `NPCManager.java` si elimina testul `generatedPersonalityHelpersReturnClampedTraits` din `NPCManagerTextTest`

### KOT-249

Data: 2026-06-07
ID: KOT-249
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Input selectat / filtrat:
- Candidati analizati: `sortRepairCandidates`, `markNpcPlannedForDeletion`, `generateUniqueAutoName`, `uniquifyNpcName`, `isNpcNameTaken`
- Filtru aplicat: fara acces la harta interna `npcsByUuid` in helper-ul Kotlin, fara generator random de nume in acelasi slice, fara dependinte Bukkit runtime
- Input ales: `sortRepairCandidates` si `markNpcPlannedForDeletion`, deoarece sunt calcule pure/mutatii explicite pe `AINPC` si `MutableSet<Int>`

Microtaskuri:
- `sortRepairCandidates` a fost mutat in Kotlin.
- `markNpcPlannedForDeletion` a fost mutat in Kotlin.
- Metodele Java duplicate au fost eliminate.
- Testul `NPCManagerTextTest` valideaza ordinea dupa `databaseId`, apoi UUID, si faptul ca doar ID-urile pozitive sunt marcate pentru stergere.

Gate local:
- IntelliJ build pe `NPCManagerText.kt`, `NPCManager.java` si `NPCManagerTextTest.kt` (PASS)
- `.\\gradlew.bat :ainpc-core-plugin:compileKotlin :ainpc-core-plugin:compileJava :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.StorageDialectStaticAuditTest --tests ro.ainpc.CoreNeutralityStaticAuditTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Sortarea foloseste `databaseId` crescator si apoi UUID, pastrand intentia comparatorului Java.
- `markNpcPlannedForDeletion` ramane o mutatie explicita pe setul primit, fara dependinte pe manager.

Inventar dupa slice:
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java (~98.7% Kotlin dupa numar de fisiere; ~70.7% Kotlin dupa linii)
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.60% Kotlin dupa linii

Rollback:
- readauga `sortRepairCandidates(List<AINPC>)` si `markNpcPlannedForDeletion(AINPC, Set<Integer>)` in `NPCManager.java` si elimina testul `repairCandidateHelpersSortAndMarkByCanonicalIds` din `NPCManagerTextTest`

### KOT-250

Data: 2026-06-07
ID: KOT-250
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Input selectat / filtrat:
- Candidati analizati: `shouldPreferEnvironmentOccupation`, `inferOccupationFromEnvironment`, `mapProfessionToOccupation`, `findNearbyWorkstation`
- Filtru aplicat: fara scanare world, fara `Registry` Bukkit in test JUnit, fara dependinta pe `plugin`
- Input ales: `shouldPreferEnvironmentOccupation`, deoarece este o regula determinista pentru alegerea ocupatiei si nu depinde de Paper runtime

Microtaskuri:
- `shouldPreferEnvironmentOccupation` a fost mutat in Kotlin cu semnatura Java interop.
- Metoda Java duplicata a fost eliminata din `NPCManager.java`.
- Testul `NPCManagerTextTest` valideaza cazurile null/blank, ocupatie generica, match case-insensitive si ocupatie mapata diferita.

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)

Observatii:
- Parametrul `profession` ramane in semnatura pentru compatibilitate cu apelul existent din Java, dar nu influenteaza regula actuala.
- Estimarea ramasa a fost actualizata la 39 taskuri: `NPCManager.java` scade de la 8 la 7 taskuri estimate.

Inventar dupa slice:
- fisiere Java de productie ramase in core: `AINPCCommand.java`, `ScenarioEngine.java`, `NPCManager.java`
- linii Java ramase in cele 3 fisiere: 18.144 in working tree curent
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java, 70.36% Kotlin dupa linii
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.62% Kotlin dupa linii

Rollback:
- readauga `shouldPreferEnvironmentOccupation(Villager.Profession, String, String)` in `NPCManager.java` si elimina testul `environmentOccupationPreferenceOnlyOverridesGenericOrSameMappedOccupation` din `NPCManagerTextTest`

### KOT-251

Data: 2026-06-07
ID: KOT-251
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Input selectat / filtrat:
- Candidati analizati: `createFallbackHomeAnchor`, `createFallbackWorkAnchor`, `findNearestHomeAnchor`, `findNearestWorkAnchor`, `findNearestSocialAnchor`
- Filtru aplicat: fara scanare block/world, fara `Tag.BEDS`, fara predicate Bukkit runtime, fara acces la `plugin`
- Input ales: `createFallbackHomeAnchor` si `createFallbackWorkAnchor`, deoarece sunt constructori deterministi pentru `AINPC.OwnedLocation`

Microtaskuri:
- `createFallbackHomeAnchor` a fost mutat in Kotlin.
- `createFallbackWorkAnchor` a fost mutat in Kotlin.
- Metodele Java duplicate au fost eliminate din `NPCManager.java`.
- Testul `NPCManagerTextTest` valideaza tipul, label-ul, world-ul si coordonatele pentru ancore fallback home/work.

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Label-ul pentru work anchor pastreaza fallback-ul pentru ocupatii generice (`resident`, `villager`, `locuitor`, `localnic`).
- Estimarea ramasa a fost actualizata la 38 taskuri: `NPCManager.java` scade de la 7 la 6 taskuri estimate.

Inventar dupa slice:
- fisiere Java de productie ramase in core: `AINPCCommand.java`, `ScenarioEngine.java`, `NPCManager.java`
- linii Java ramase in cele 3 fisiere: 18.117 in working tree curent
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java, 70.4% Kotlin dupa linii
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.64% Kotlin dupa linii

Rollback:
- readauga `createFallbackHomeAnchor(AINPC, Location)` si `createFallbackWorkAnchor(AINPC, Location)` in `NPCManager.java` si elimina testul `fallbackAnchorsUseNpcNameOccupationAndCenterCoordinates` din `NPCManagerTextTest`

### KOT-252

Data: 2026-06-07
ID: KOT-252
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Input selectat / filtrat:
- Candidati analizati: `createVillagerSeededRandom`, `inferOccupationFromEnvironment`, `applyThemeDefaults`, `resolveOccupationForVillager`
- Filtru aplicat: fara `plugin`, fara feature pack loader, fara registry Bukkit, fara scanare world/block
- Input ales: `createVillagerSeededRandom` si `inferOccupationFromEnvironment`, deoarece sunt helper-e auto-profile mici si testabile cu proxy JUnit

Microtaskuri:
- `createVillagerSeededRandom` a fost mutat in Kotlin si pastreaza seed-ul bazat pe XOR-ul UUID-ului.
- `inferOccupationFromEnvironment` a fost mutat in Kotlin ca placeholder explicit `null`.
- Metodele Java duplicate au fost eliminate din `NPCManager.java`.
- Testul `NPCManagerTextTest` valideaza secventa determinista `Random` si faptul ca inferenta environment ramane dezactivata.

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Proxy-ul JUnit pentru `Villager` a fost extins doar cu `getUniqueId`, fara runtime Paper.
- Estimarea ramasa a fost actualizata la 37 taskuri: `NPCManager.java` scade de la 6 la 5 taskuri estimate.

Inventar dupa slice:
- fisiere Java de productie ramase in core: `AINPCCommand.java`, `ScenarioEngine.java`, `NPCManager.java`
- linii Java ramase in cele 3 fisiere: 18.109 in working tree curent
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java, 70.41% Kotlin dupa linii
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.65% Kotlin dupa linii

Rollback:
- readauga `createVillagerSeededRandom(Villager)` si `inferOccupationFromEnvironment(Villager)` in `NPCManager.java`, apoi elimina testul `villagerSeededRandomUsesUuidBitsAndEnvironmentInferenceStaysDisabled` din `NPCManagerTextTest`

### KOT-253

Data: 2026-06-07
ID: KOT-253
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Input selectat / filtrat:
- Candidati analizati: `resolveOccupationForVillager`, `mapProfessionToOccupation`, `inferOccupationFromPrimaryScenario`
- Filtru aplicat: fara `Registry` Bukkit in test, fara `plugin`, fara feature pack loader; doar decizia pura pe valori deja calculate
- Input ales: `resolveOccupationChoice`, helper nou Kotlin extras din ordinea de decizie a `resolveOccupationForVillager`

Microtaskuri:
- Logica de alegere intre ocupatia mapata, ocupatia inferata si ocupatia din tema a fost mutata in Kotlin.
- `resolveOccupationForVillager` din Java calculeaza doar inputurile dependente de Bukkit/plugin si delega decizia la Kotlin.
- Testul `NPCManagerTextTest` valideaza prioritatea mapped non-generic, inferred preferat, themed fallback si fallback generic final.

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- `inferOccupationFromPrimaryScenario` ramane in Java deoarece depinde de `plugin.getFeaturePackLoader()`.
- Estimarea ramasa a fost actualizata la 36 taskuri: `NPCManager.java` scade de la 5 la 4 taskuri estimate.

Inventar dupa slice:
- fisiere Java de productie ramase in core: `AINPCCommand.java`, `ScenarioEngine.java`, `NPCManager.java`
- linii Java ramase in cele 3 fisiere: 18.094 in working tree curent
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java, 70.44% Kotlin dupa linii
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.67% Kotlin dupa linii

Rollback:
- readauga ramurile de decizie direct in `resolveOccupationForVillager` din `NPCManager.java`, apoi elimina `resolveOccupationChoice` si testul `occupationChoicePreservesMappedInferredAndThemedPriority`

### KOT-254

Data: 2026-06-07
ID: KOT-254
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Input selectat / filtrat:
- Candidati analizati: `toOwnedLocation`, `findBestNodeForPlace`, `findBestRegionNode`
- Filtru aplicat: fara `WorldAdminApi`, fara `plugin`, fara cautare in registri/noduri runtime
- Input ales: overload-urile `toOwnedLocation`, deoarece transforma determinist `WorldPlaceInfo`/`WorldNodeInfo` in `AINPC.OwnedLocation`

Microtaskuri:
- `toOwnedLocation(String, WorldPlaceInfo, WorldNodeInfo)` a fost mutat in Kotlin.
- `toOwnedLocation(String, WorldNodeInfo, String)` a fost mutat in Kotlin.
- Metodele Java duplicate au fost eliminate din `NPCManager.java`.
- Testul `NPCManagerTextTest` valideaza conversia din place center, conversia prin node preferat si fallback-ul de label pentru node.

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Selectia celui mai bun node ramane in Java deoarece depinde de `WorldAdminApi`; conversia finala a anchor-ului este acum Kotlin.

Inventar dupa slice:
- fisiere Java de productie ramase in core: `AINPCCommand.java`, `ScenarioEngine.java`, `NPCManager.java`
- linii Java ramase in cele 3 fisiere: 18.057 in working tree curent
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java, 70.5% Kotlin dupa linii
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.70% Kotlin dupa linii

Rollback:
- readauga overload-urile `toOwnedLocation` in `NPCManager.java`, elimina overload-urile Kotlin si testul `ownedLocationConversionUsesPlaceCenterOrPreferredNode`

### KOT-255

Data: 2026-06-07
ID: KOT-255
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Input selectat / filtrat:
- Candidati analizati: `uniquifyNpcName`, `generateUniqueAutoName`, `isNpcNameTaken`
- Filtru aplicat: fara `NPCNameGenerator`, fara acces direct la `npcsByUuid`, fara randomizare in helper-ul Kotlin
- Input ales: `uniquifyNpcName`, deoarece este o bucla determinista peste un `Predicate<String>` Java interop

Microtaskuri:
- `uniquifyNpcName` a fost mutat in Kotlin cu semnatura `Predicate<String>`.
- La finalul KOT-255, `generateUniqueAutoName` din Java pasa `this::isNpcNameTaken` catre helper-ul Kotlin; dupa KOT-257 nu mai exista metoda Java dedicata `isNpcNameTaken`.
- Metoda Java duplicata a fost eliminata.
- Testul `NPCManagerTextTest` valideaza trim-ul, fallback-ul `NPC` si primul suffix liber.

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- La finalul KOT-255, `generateUniqueAutoName` ramasese in Java; a fost mutat ulterior in Kotlin prin KOT-256.
- Estimarea ramasa a fost actualizata la 34 taskuri: `NPCManager.java` scade de la 4 la 2 taskuri estimate dupa KOT-254 si KOT-255.

Inventar dupa slice:
- fisiere Java de productie ramase in core: `AINPCCommand.java`, `ScenarioEngine.java`, `NPCManager.java`
- linii Java ramase in cele 3 fisiere: 18.057 in working tree curent
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java, 70.5% Kotlin dupa linii
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.70% Kotlin dupa linii

Rollback:
- readauga `uniquifyNpcName(String)` in `NPCManager.java`, readu apelul `uniquifyNpcName(NPCNameGenerator.randomName(gender, random))`, elimina `uniquifyNpcName(String?, Predicate<String>)` din Kotlin si testul `uniquifyNpcNameTrimsFallbacksAndAppendsFirstFreeSuffix`

### KOT-256

Data: 2026-06-07
ID: KOT-256
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Input selectat / filtrat:
- Candidati analizati: `generateUniqueAutoName`, `isNpcNameTaken`, `applyThemeDefaults`, `mapProfessionToOccupation`
- Filtru aplicat: fara acces direct la `npcsByUuid`, fara DB, fara `plugin`, fara `Registry` Bukkit
- Input ales: `generateUniqueAutoName`, deoarece poate primi `Predicate<String>` pentru verificarea numelor ocupate si ramane determinist peste `NPCNameGenerator` + `Random`

Microtaskuri:
- `generateUniqueAutoName` a fost mutat in Kotlin cu semnatura `Predicate<String>`.
- La finalul KOT-256, `applyAutoProfile` pasa `this::isNpcNameTaken` catre helper-ul Kotlin; dupa KOT-257 paseaza o lambda catre helper-ul Kotlin cu `npcsByUuid.values()`.
- Importul Java direct pentru `NPCNameGenerator` a fost eliminat din `NPCManager.java`.
- Testul `NPCManagerTextTest` valideaza alegerea primului nume liber din lista predefinita shuffle-uita determinist.

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- La finalul KOT-256, `isNpcNameTaken` ramasese in Java; a fost mutat ulterior in Kotlin prin KOT-257.
- Estimarea ramasa a fost actualizata la 33 taskuri: `NPCManager.java` scade de la 2 la 1 task estimat.

Inventar dupa slice:
- fisiere Java de productie ramase in core: `AINPCCommand.java`, `ScenarioEngine.java`, `NPCManager.java`
- linii Java ramase in cele 3 fisiere: 18.044 in working tree curent
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java, 70.53% Kotlin dupa linii
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.72% Kotlin dupa linii

Rollback:
- readauga `generateUniqueAutoName(String, Random)` in `NPCManager.java`, readu apelul `generateUniqueAutoName(gender, random)`, elimina `generateUniqueAutoName(String?, Random, Predicate<String>)` din Kotlin si testul `generatedAutoNameUsesFirstFreeShuffledPredefinedName`

### KOT-257

Data: 2026-06-07
ID: KOT-257
Status: validat local
Zona: `ro.ainpc.managers`
Tip: productie + test
Risc: 1

Fisiere modificate:
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManagerText.kt`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/managers/NPCManagerTextTest.kt`
- `docs/kotlin-migration-tracker.md`
- `docs/rezumat-conversie-java-la-kotlin.md`

Input selectat / filtrat:
- Candidati analizati: `isNpcNameTaken`, `applyThemeDefaults`, `mapProfessionToOccupation`, `findNearestBlock`
- Filtru aplicat: fara DB, fara `plugin`, fara `Registry` Bukkit, fara world/block scan
- Input ales: `isNpcNameTaken`, deoarece poate primi colectia `AINPC` ca input explicit si ramane o comparatie determinista

Microtaskuri:
- Logica `isNpcNameTaken` a fost mutata in Kotlin cu semnatura `isNpcNameTaken(String?, Collection<AINPC>)`.
- `applyAutoProfile` din Java paseaza un predicate lambda catre `generateUniqueAutoName`, folosind `npcsByUuid.values()`.
- Metoda Java privata `isNpcNameTaken(String)` a fost eliminata.
- Testul `NPCManagerTextTest` valideaza blank-safety, trim si comparatia case-insensitive.

Gate local:
- `.\\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.managers.NPCManagerTextTest` (PASS, cu `JAVA_HOME` setat local la JDK 25)
- `.\\gradlew.bat kotlinRatio` (PASS)

Observatii:
- Estimarea macro ramane 33 taskuri, deoarece in `NPCManager.java` ramane un task mare riscant pe DB/Paper/world runtime; acest slice doar inchide ultimul helper sigur mic din zona auto-profile.

Inventar dupa slice:
- fisiere Java de productie ramase in core: `AINPCCommand.java`, `ScenarioEngine.java`, `NPCManager.java`
- linii Java ramase in cele 3 fisiere: 18.029 in working tree curent
- `ainpc-core-plugin/src/main`: 230 fisiere Kotlin, 3 fisiere Java, 70.55% Kotlin dupa linii
- global Gradle `kotlinRatio`: 596 fisiere Kotlin, 3 fisiere Java, 84.73% Kotlin dupa linii

Rollback:
- readauga `isNpcNameTaken(String)` in `NPCManager.java`, readu apelul `generateUniqueAutoName(gender, random, this::isNpcNameTaken)`, elimina `isNpcNameTaken(String?, Collection<AINPC>)` din Kotlin si testul `npcNameTakenIsBlankSafeTrimmedAndCaseInsensitive`
