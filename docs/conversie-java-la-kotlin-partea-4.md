# Conversie Java la Kotlin - Partea 4

Actualizat: 2026-05-16

## Scop

Acest document continua seria de conversie Java -> Kotlin cu o harta pe pachetele reale din proiect.

Partile anterioare raspund la:

- Partea 1: de ce si in ce faze mari se face conversia
- Partea 2: cum se executa slice-urile si cand se opresc
- Partea 3: cum arata configurarea si conversiile concrete

Partea 4 raspunde la intrebarea practica: ce convertesti din acest repo, in ce ordine si cu ce risc.

Continuare pentru tracking, teste, smoke si rollback: `conversie-java-la-kotlin-partea-5.md`.

## Inventar curent

In `ainpc-core-plugin/src/main/java` exista aproximativ 178 fisiere Java.

Pachete principale:

- `addons`
- `ai`
- `bootstrap`
- `commands`
- `database`
- `debug`
- `engine`
- `gui`
- `listeners`
- `managers`
- `npc`
- `platform`
- `progression`
- `routine`
- `spawn`
- `story`
- `topology`
- `utils`
- `world`

Module:

- `ainpc-api`
- `ainpc-core-plugin`
- `ainpc-scenario-medieval`

Regula de baza:

- Kotlin intra prima data in `ainpc-core-plugin`.
- `ainpc-api` ramane Java pana cand exista test Java de consum pentru orice tip public convertit.
- `ainpc-scenario-medieval` ramane Java pana cand core-ul dovedeste ca packaging-ul Kotlin merge pe server Paper.

## Scor de risc

Foloseste scorul de mai jos inainte sa convertesti orice clasa.

| Scor | Tip clasa | Actiune |
|---:|---|---|
| 1 | test simplu, utilitar pur, enum intern | convertibil devreme |
| 2 | model intern immutable, selector, validator simplu | convertibil dupa primele teste Kotlin |
| 3 | serviciu determinist cu dependinte injectate | convertibil dupa teste de modul |
| 4 | DB, Bukkit/Paper service, GUI, listener helper | convertibil dupa smoke test relevant |
| 5 | entrypoint Paper, API public, command/router mare, manager mare | nu converti pana la refactorizare |

Regula:

- intr-un singur slice convertesti doar clase cu acelasi scor sau cu risc mai mic.

## Faza AA: Zone interzise la inceput

Nu converti in primele sprinturi:

- `ainpc-api`
- `AINPCPlugin`
- `AINPCCommand`
- `AINPCTabCompleter`
- `ScenarioEngine`
- `FeaturePackLoader`
- `NPCManager`
- `DatabaseManager`
- `OpenAIService`
- listener-ele principale Paper
- clasele principale din `ainpc-scenario-medieval`

Motiv:

- aceste clase sunt puncte de integrare, lifecycle, DB, Paper sau API public
- o conversie aici ar amesteca schimbarea de limbaj cu riscuri de comportament

Gate pentru a le atinge:

- clasa a fost subtiata
- exista teste relevante
- exista smoke test Paper
- rollback-ul este clar

## Faza AB: Sprint 1 recomandat

Obiectiv:

- pornirea Kotlin cu risc minim

Slice-uri:

1. activeaza Kotlin in Gradle pentru `ainpc-core-plugin`
2. adauga test Kotlin minim in `src/test/kotlin`
3. converteste un test simplu din `utils`
4. converteste un test simplu din `progression`
5. ruleaza `.\gradlew.bat clean build`
6. ruleaza `.\gradlew.bat assemble`

Nu converti cod de productie inainte ca testele Kotlin sa ruleze.

Gate:

- build complet verde
- JAR core generat
- nu exista schimbari in `plugin.yml`

## Faza AC: Sprint 2 recomandat

Obiectiv:

- prima conversie de productie cu risc mic

Candidati:

- `utils/NPCNameGenerator.java`
- helperi mici fara stare globala
- enum-uri interne neexpuse public
- filtre sau selectori mici din `progression`

De evitat:

- `MessageUtils`, daca formatul mesajelor este sensibil pentru comenzile live
- orice cod care interactioneaza direct cu Paper runtime

Gate:

- testele unitare pentru clasa convertita trec
- codul Java ramas poate apela clasa fara schimbari mari

## Faza AD: `progression`

Prioritate:

- buna pentru Kotlin, dar trebuie impartita pe risc

Convertibil devreme:

- `ProgressionFilter`
- `ProgressionSelector`
- snapshot-uri simple
- entry-uri GUI immutable
- obiecte de status fara DB

Convertibil mediu:

- `ProgressionDefinition`
- `ProgressionAnchorBinding`
- clase care au parsing sau liste interne

Convertibil tarziu:

- `ProgressionRepository`
- `ProgressionService`

Reguli:

- repository-ul ramane Java pana cand testele DB sunt stabile cu Kotlin in proiect
- service-ul ramane Java pana cand modelele si selectorii sunt deja convertiti
- nu schimba `Optional`/nullable in acelasi slice cu conversia clasei

Teste relevante:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*Progression*"
```

## Faza AE: `world.mapping`

Prioritate:

- buna pentru Kotlin deoarece are parsere, drafturi si obiecte de rezultat

Convertibil devreme:

- `MappingPoint`
- `MappingDraftKind`
- `MappingWandMode`
- teste pentru `MappingIntentParser`

Convertibil mediu:

- `MappingDraft`
- `MappingDraftSuggestion`
- `MappingDraftApplyResult`
- `MappingIntentParser`

Convertibil tarziu:

- `MappingDraftFactory`
- `MappingWandService`

Reguli:

- parserul se converteste doar cu teste de intentii existente
- service-ul care interactioneaza cu wand/player ramane Java pana dupa smoke test
- pastreaza mesajele si ID-urile generate identice

Teste relevante:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*Mapping*"
```

## Faza AF: `world.patch`

Prioritate:

- foarte buna pentru Kotlin daca este mentinuta read-only si determinista

Convertibil devreme:

- `PatchType`
- `PatchGapType`
- `PatchBuildMode`
- `PatchValidationStatus`
- `PatchPlannerOptions`, daca accessorii Java sunt verificati

Convertibil mediu:

- `VillageGap`
- `GapReport`
- `PatchCandidate`
- `PatchPlan`
- `PatchPlannerResult`

Convertibil tarziu:

- `VillageGapAnalyzer`
- `VillagePatchPlanner`

Reguli:

- ai grija la accessorii Java pentru modelele convertite in `data class`
- plannerul se converteste doar dupa ce modelele sunt stabile
- nu schimba algoritmul de planificare in acelasi slice

Teste relevante:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*VillagePatchPlannerTest"
```

## Faza AG: `engine`

Prioritate:

- convertibil selectiv; pachetul contine si clase cu risc foarte mare

Convertibil devreme:

- `QuestDecisionIntentResolver`
- `QuestTemplateSelector`
- `QuestDirectorDecision`
- `QuestDirectorRequest`, daca accessorii Java sunt verificati

Convertibil mediu:

- `FeaturePackDependencyValidator`
- `FeaturePackMetadataValidator`
- `QuestScenarioContract`
- clase mici din `engine.runtime`

Convertibil tarziu:

- `QuestDirector`
- `QuestAnchorResolver`
- `DialogueEngine`
- `DecisionEngine`

Nu converti inainte de refactorizare:

- `ScenarioEngine`
- `FeaturePackLoader`

Reguli:

- validatoarele pot fi Kotlin daca mesajele raman identice
- runtime registry-urile se convertesc doar cu teste dedicate
- `ScenarioEngine` trebuie intai spart in componente mai mici

Teste relevante:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*Quest*"
.\gradlew.bat :ainpc-core-plugin:test --tests "*FeaturePack*"
.\gradlew.bat :ainpc-core-plugin:test --tests "*ScenarioRuntime*"
```

## Faza AH: `spawn`

Prioritate:

- buna pentru modele si validatoare, mai riscanta pentru orchestrare si persistenta

Convertibil devreme:

- `SpawnBatchPlanHasher`
- `SpawnBatchTracker`
- `HouseAllocation`
- `HouseAllocationValidationResult`
- `FamilyBindingPlan`
- `FamilyBindingResult`

Convertibil mediu:

- `HouseAllocationValidator`
- `HouseAllocationPlanner`
- planuri si rezultate de spawn

Convertibil tarziu:

- `HouseholdPersistenceService`
- `NpcSpawnOrchestrator`

Reguli:

- persistenta households ramane Java pana la stabilizarea testelor DB
- orchestratorul ramane Java pana cand planurile si rezultatele sunt stabile
- nu schimba hash-ul generat in conversia hasher-ului

Teste relevante:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*Spawn*"
.\gradlew.bat :ainpc-core-plugin:test --tests "*House*"
```

## Faza AI: `routine`

Prioritate:

- buna pentru modele si engine determinist

Convertibil devreme:

- `RoutineSlot`
- `RoutineScheduleEntry`
- `RoutineAssignment`
- `RoutineTickSummary`

Convertibil mediu:

- `RoutineEngine`

Convertibil tarziu:

- `RoutineService`

Reguli:

- engine-ul se converteste doar dupa ce modelele sunt stabile
- service-ul ramane Java daca are dependinte Paper/runtime greu de testat

Teste relevante:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*Routine*"
```

## Faza AJ: `story`

Prioritate:

- buna pentru snapshot-uri si evenimente, medie pentru servicii cu DB

Convertibil devreme:

- `StoryEvent`
- `StoryContextSnapshot`
- `PlaceStoryState`
- `RegionStoryState`

Convertibil mediu:

- clase de model folosite doar intern

Convertibil tarziu:

- `StoryStateService`
- `StoryContextService`

Reguli:

- nu schimba formatul evenimentelor story
- serviciile se convertesc doar dupa testele DB/story

Teste relevante:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*Story*"
```

## Faza AK: `gui`

Prioritate:

- convertibil selectiv; GUI-ul este sensibil la Paper inventory API

Convertibil devreme:

- `GuiKey`
- `GuiNavigation`
- `GuiAction`
- `QuestLogGuiFilter`
- `QuestLogGuiPage`

Convertibil mediu:

- `GuiButton`
- `GuiRenderContext`
- `GuiClickContext`
- entry-uri si snapshot-uri GUI

Convertibil tarziu:

- `GuiService`
- `GuiSessionManager`
- `GuiInventoryListener`
- ecranele din `gui.screens`

Reguli:

- ecranele GUI se convertesc doar cu smoke test pe server
- nu schimba sloturile, titlurile sau itemele in conversia mecanica
- pastreaza textul vizibil identic daca testele sau adminii il folosesc

Teste relevante:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*Gui*"
```

## Faza AL: `ai`

Prioritate:

- conversie selectiva; evita clientul OpenAI initial

Convertibil devreme:

- enum-uri din `ai.orchestration`
- `AIOrchestrationRequest`, daca este model intern
- `AIOrchestrationResult`, daca accessorii Java sunt verificati
- `AIOrchestrationPolicy`

Convertibil mediu:

- `AIOrchestrationService`, doar daca testele acopera cazurile de fallback
- `NpcFactResolver`, daca nu atinge runtime greu

Nu converti devreme:

- `OpenAIService`
- `DialogManager`, daca are multe dependinte live

Reguli:

- nu schimba payload-urile trimise la AI in conversii mecanice
- nu schimba fallback policy in acelasi slice
- nu introduce coroutine pentru apeluri AI fara design separat

Teste relevante:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*AIOrchestration*"
```

## Faza AM: `world.scan` si `world`

Prioritate:

- buna pentru rezultate de scanare, mai riscanta pentru servicii admin

Convertibil devreme:

- `VanillaVillageFeatureType`
- `VanillaVillageFeature`
- `VanillaVillageScanResult`
- `SemanticVillageImportResult`
- `WorldContextSnapshot`
- `WorldNodeType`
- `RegionType`

Convertibil mediu:

- `SemanticVillageMapper`
- `WorldContextSnapshotBuilder`
- modele interne world, daca accessorii sunt verificati

Convertibil tarziu:

- `WorldAdminService`
- `NpcWorldBindingService`
- `VanillaVillageScanner`

Reguli:

- scannerul care foloseste Paper world API se converteste doar cu smoke test
- `WorldAdminService` ramane Java pana cand configurarea si persistenta sunt bine acoperite

Teste relevante:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*World*"
.\gradlew.bat :ainpc-core-plugin:test --tests "*Village*"
```

## Faza AN: `commands`, `listeners`, `managers`, `database`

Prioritate:

- tarzie

Nu converti devreme:

- `AINPCCommand`
- `AINPCTabCompleter`
- `ListenerRegistry`
- `NPCInteractionListener`
- `NPCChatListener`
- `MappingWandListener`
- `QuestObjectiveListener`
- `VillagerLifecycleListener`
- `NPCManager`
- `MemoryManager`
- `FamilyManager`
- `EmotionManager`
- `ConversationSessionManager`
- `DatabaseManager`

Ce poti face inainte:

- extrage clase mici Java din aceste componente
- testeaza clasele extrase
- converteste clasele extrase la Kotlin

Regula:

- nu converti routere, listener-e si manageri mari direct
- intai refactorizare fara schimbare functionala

Teste relevante:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*Command*"
.\gradlew.bat :ainpc-core-plugin:test --tests "*TabCompleter*"
```

Smoke obligatoriu:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-mapping.ps1 `
  -ServerDir "C:\Minecraft\paper-test"
```

## Faza AO: `ainpc-scenario-medieval`

Prioritate:

- dupa ce core-ul are Kotlin stabil

Convertibil devreme:

- teste ale addonului, daca sunt simple

Convertibil mediu:

- `MedievalScenarioAddon`, doar daca API-ul consumat ramane clar

Convertibil tarziu:

- `AINPCScenarioMedievalPlugin`

Reguli:

- addonul trebuie sa ramana test de consumator pentru `ainpc-api`
- daca addonul contine Kotlin, verifica runtime-ul Kotlin in JAR sau la incarcare
- nu presupune ca runtime-ul Kotlin shaded in core este suficient pentru addon

Teste relevante:

```powershell
.\gradlew.bat :ainpc-scenario-medieval:test
.\gradlew.bat :ainpc-scenario-medieval:assemble
```

Smoke:

- porneste server Paper cu core + addon medieval
- confirma ca `depend: [AINPCPlugin]` este respectat
- confirma ca pack-ul medieval este incarcat

## Faza AP: `ainpc-api`

Prioritate:

- ultima zona de conversie

Pastreaza Java pentru:

- `AINPCAddon`
- `AINPCPlatformApi`
- `AddonRegistryApi`
- `WorldAdminApi`
- DTO-urile publice world
- enum-urile publice folosite de addonuri

Daca apare motiv real pentru Kotlin:

1. converteste un singur tip public
2. scrie test Java de consum
3. compileaza addonul medieval
4. documenteaza semnatura vazuta din Java

Comenzi:

```powershell
.\gradlew.bat :ainpc-api:build
.\gradlew.bat :ainpc-scenario-medieval:build
.\gradlew.bat clean build
```

Regula:

- niciun tip Kotlin in `ainpc-api` fara test Java de consum.

## Backlog recomandat F001-F030

F001 - Activeaza Kotlin in `ainpc-core-plugin`, fara fisiere `.kt`.

F002 - Adauga test smoke Kotlin minim.

F003 - Converteste un test simplu din `utils`.

F004 - Converteste un test simplu din `progression`.

F005 - Converteste `ProgressionFilter` sau un echivalent cu risc mic.

F006 - Converteste testul aferent clasei convertite.

F007 - Converteste `ProgressionSelector`, daca interop-ul Java este simplu.

F008 - Ruleaza `clean build` si `assemble`.

F009 - Inspecteaza JAR-ul core pentru clase Kotlin si `plugin.yml`.

F010 - Converteste un enum intern din `world.patch`.

F011 - Converteste un model intern simplu din `world.patch`.

F012 - Ruleaza testele `VillagePatchPlannerTest`.

F013 - Converteste `QuestDecisionIntentResolver` sau testele lui.

F014 - Ruleaza testele `QuestDecisionIntentResolverTest`.

F015 - Converteste `QuestTemplateSelector`, daca testele sunt suficiente.

F016 - Ruleaza testele `QuestTemplateSelectorTest`.

F017 - Converteste un model simplu din `spawn`.

F018 - Ruleaza testele `HouseAllocationTest` si `SpawnBatchPlanHasherTest`.

F019 - Converteste `SpawnBatchPlanHasher` doar daca hash-ul ramane identic.

F020 - Ruleaza `clean build`.

F021 - Converteste un model simplu din `routine`.

F022 - Ruleaza `RoutineEngineTest`.

F023 - Converteste un model simplu din `story`.

F024 - Ruleaza `StoryStateServiceTest`.

F025 - Converteste `GuiKey` sau testele lui.

F026 - Ruleaza `GuiKeyTest`.

F027 - Converteste `QuestLogGuiFilter`.

F028 - Ruleaza `QuestLogGuiFilterTest`.

F029 - Ruleaza `assemble` si inspecteaza JAR-ul.

F030 - Ruleaza smoke test Paper daca au fost atinse GUI, listener, command sau Paper API.

## Reguli pentru alegerea urmatorului fisier

Alege urmatorul fisier doar daca:

- are test direct sau poate primi test direct
- nu este entrypoint Paper
- nu este API public
- nu are DB sau lifecycle complex
- nu are multi-threading sau scheduler
- conversia nu cere modificari in peste 5 call-site-uri Java

Daca nu gasesti un fisier care respecta regulile:

- extrage intai o clasa mica Java
- testeaz-o
- apoi converteste-o la Kotlin

## Gate de maturitate

Kotlin poate deveni limbaj normal pentru cod nou in core doar dupa:

- cel putin 10 fisiere convertite fara regresii
- `clean build` stabil
- un smoke test Paper reusit
- JAR inspectat pentru runtime Kotlin
- nicio problema de incarcare addon medieval
- reguli de interop documentate si respectate

Pana atunci:

- Kotlin este permis doar in slice-uri controlate
- Java ramane default pentru API, Paper entrypoints si clase mari

## Definitia de gata pentru Partea 4

Partea 4 este indeplinita cand:

- exista o ordine clara pe pachetele reale
- fiecare pachet are zone permise si zone blocate
- exista backlog F001-F030 pentru primele conversii
- exista criterii de alegere a urmatorului fisier
- conversia nu incepe cu clase mari sau cu API public
