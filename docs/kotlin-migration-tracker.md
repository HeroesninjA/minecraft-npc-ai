# Kotlin Migration Tracker

Actualizat: 2026-05-16

## Scop

Acest document este trackerul operational pentru conversia Java -> Kotlin.

Planul si regulile sunt definite in:

- `rezumat-conversie-java-la-kotlin.md`
- `conversie-java-la-kotlin.md`
- `conversie-java-la-kotlin-partea-2.md`
- `conversie-java-la-kotlin-partea-3.md`
- `conversie-java-la-kotlin-partea-4.md`
- `conversie-java-la-kotlin-partea-5.md`
- `kotlin-style-guide.md`
- `kotlin-interop-api-addonuri.md`
- `kotlin-paper-packaging-si-smoke.md`

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
| `ainpc-api` convertit | amanat |
| `ainpc-scenario-medieval` convertit | amanat |

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
Observatii: audit facut prin API-ul ZIP PowerShell deoarece comanda jar nu este pe PATH; dupa KOT-025 sunt prezente clase Kotlin din ai/orchestration, debug, engine, gui, progression, routine, world.mapping, world.patch, world.scan si spawn.
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
