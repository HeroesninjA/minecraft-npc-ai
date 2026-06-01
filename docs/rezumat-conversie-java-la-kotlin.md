# Rezumat Conversie Java la Kotlin

Actualizat: 2026-06-01

## Scop

Acesta este rezumatul seriei:

- `conversie-java-la-kotlin.md`
- `conversie-java-la-kotlin-partea-2.md`
- `conversie-java-la-kotlin-partea-3.md`
- `conversie-java-la-kotlin-partea-4.md`
- `conversie-java-la-kotlin-partea-5.md`
- `kotlin-style-guide.md`
- `kotlin-interop-api-addonuri.md`
- `kotlin-paper-packaging-si-smoke.md`
- `kotlin-migration-tracker.md`
- `kotlin-code-review-checklist.md`
- `kotlin-coroutines-paper-policy.md`
- `kotlin-gradle-activation-plan.md`
- `kotlin-testing-strategy.md`

Seria descrie cum se introduce Kotlin in proiectul AINPC dupa migrarea la Gradle, fara rescriere masiva si fara ruperea compatibilitatii cu Paper, addonul medieval sau API-ul public Java.

Status curent:

- Kotlin este activat si validat local in toate modulele Gradle (`ainpc-api`, `ainpc-core-plugin`, `ainpc-scenario-medieval`)
- `ainpc-api` este migrat majoritar la Kotlin pentru modele/enum-uri/descriptor; au ramas Java doar 3 interfețe API pentru interop stabil (`AddonRegistryApi`, `AINPCPlatformApi`, `WorldAdminApi`)
- `ainpc-scenario-medieval` este migrat la Kotlin pe `src/main`
- primul test Kotlin smoke exista in core si ruleaza cu JUnit 5
- teste Java mutate in Kotlin (pana acum): `NPCNameGeneratorTest`, `ProgressionSelectorTest`, `AINPCTabCompleterTest`, `AINPCCommandRoutingTest`, `PluginCommandDescriptorTest`, `GuiKeyTest`, `QuestLogGuiFilterTest`, `MappingIntentParserTest`
- primele clase Kotlin de productie sunt convertite in zonele `ai/orchestration`, `debug`, `engine`, `gui`, `progression`, `routine`, `spawn`, `world.mapping`, `world.patch` si `world.scan`
- `.\gradlew.bat clean build` trece dupa prima conversie de productie
- JAR-ul core are `plugin.yml` corect, clasele Kotlin de productie si runtime Kotlin prezent
- smoke Paper nu a fost rulat local inca si ramane urmatorul gate runtime
- inventar curent: 229 fisiere Kotlin si 3 fisiere Java in `ainpc-core-plugin/src/main`, aproximativ 98.7% Kotlin dupa numar de fisiere si aproximativ 66.5% Kotlin dupa linii
- fisiere Java de productie ramase in core: `AINPCCommand.java`, `ScenarioEngine.java`, `NPCManager.java`

## Ideea principala

Conversia trebuie facuta incremental.

Directia recomandata:

- Kotlin intra prima data in `ainpc-core-plugin`
- `ainpc-scenario-medieval` ramane initial Java si devine test de consumator
- `ainpc-api` ramane Java pana exista motiv clar si teste Java de consum
- clasele mari si entrypoint-urile Paper nu se convertesc devreme
- fiecare slice se inchide cu teste si, cand e cazul, smoke test Paper

## Partea 1: Strategie si faze mari

Document: `conversie-java-la-kotlin.md`

Rol:

- defineste strategia generala de migrare
- explica de ce Java si Kotlin trebuie sa coexiste o perioada
- stabileste principiile de siguranta
- descrie fazele mari: activare Kotlin, teste, utilitare, modele, servicii, API, Paper entrypoints si optimizare JAR

Concluzie:

- nu se converteste tot proiectul intr-un singur pas
- `ainpc-api` ramane Java-friendly
- orice clasa Kotlin expusa public trebuie verificata din Java
- JAR-urile Paper trebuie sa aiba runtime Kotlin disponibil daca includ clase Kotlin

## Partea 2: Runbook operational

Document: `conversie-java-la-kotlin-partea-2.md`

Rol:

- transforma strategia in pasi executabili
- defineste fazele A-J pentru executie zilnica
- explica ce se converteste prima data si ce se evita
- include criterii de oprire si rollback

Ordine recomandata:

1. activeaza Kotlin in `ainpc-core-plugin`
2. adauga test Kotlin minim
3. converteste teste simple
4. converteste utilitare pure
5. converteste modele interne
6. converteste validatoare si selectori
7. amana repository-uri, Paper services, clase mari si API public

Concluzie:

- fiecare conversie trebuie sa fie mica, reversibila si testabila
- daca apar multe modificari de interop, slice-ul este prea mare

## Partea 3: Retete concrete

Document: `conversie-java-la-kotlin-partea-3.md`

Rol:

- ofera exemple concrete de configurare si conversie
- arata cum se configureaza Gradle Kotlin
- defineste layout-ul `src/main/kotlin` si `src/test/kotlin`
- include retete pentru teste, utilitare statice, modele immutable, validatoare, JDBC, servicii Paper, nullability si colectii

Retete importante:

- `object` + `@JvmStatic` pentru utilitare consumate din Java
- `data class` doar cand equals/hashCode sunt dorite
- `sealed` doar pentru internals, nu pentru API public la inceput
- `Optional` ramane in API daca Java consumers depind de el
- `@JvmOverloads`, `@JvmField`, `@file:JvmName` se folosesc doar cand exista nevoie reala

Concluzie:

- Kotlin trebuie sa faca codul mai clar, nu doar mai scurt
- interop-ul Java se verifica explicit
- JAR-ul se inspecteaza dupa introducerea Kotlin

## Partea 4: Harta pe pachetele reale

Document: `conversie-java-la-kotlin-partea-4.md`

Rol:

- aplica strategia pe structura reala a repo-ului
- imparte pachetele in zone convertibile devreme, mediu si tarziu
- stabileste zone blocate la inceput
- include backlog F001-F030 pentru primele conversii

Zone bune devreme:

- teste simple
- `progression` filtre/selectori/modele
- `world.mapping` modele si parsere mici
- `world.patch` enum-uri, modele si rezultate
- `spawn` modele si hasher-e
- `routine` modele si engine determinist
- `story` snapshot-uri si evenimente

Zone de amanat:

- `ainpc-api`
- `AINPCPlugin`
- `AINPCCommand`
- `ScenarioEngine`
- `FeaturePackLoader`
- `NPCManager`
- `DatabaseManager`
- `OpenAIService`
- listener-ele principale Paper

Concluzie:

- conversia incepe cu clase mici si testabile
- clasele mari se sparg intai in Java, apoi se convertesc componentele extrase

## Partea 5: Control, teste, smoke si rollback

Document: `conversie-java-la-kotlin-partea-5.md`

Rol:

- defineste modul de urmarire a conversiei
- introduce status board pentru primele slice-uri
- stabileste criterii de acceptare pe niveluri de risc
- descrie matricea de teste, auditul JAR, smoke testul Paper si rollback-ul

Elemente cheie:

- tracker per slice cu ID, pachet, risc, teste, JAR audit si rollback
- acceptare diferita pentru risc 1-5
- `clean build` pentru cod de productie
- `assemble` si audit JAR pentru packaging
- smoke Paper pentru Paper runtime, listener-e, comenzi si packaging Kotlin

Concluzie:

- conversia nu este valida doar pentru ca compileaza
- trebuie confirmate JAR-ul, serverul Paper, addonul medieval si compatibilitatea Java

## Ordinea finala recomandata

1. Pastreaza `ainpc-api` Java.
2. Activeaza Kotlin doar in `ainpc-core-plugin`.
3. Adauga un test Kotlin minim.
4. Converteste 1-2 teste simple.
5. Converteste un utilitar sau model intern mic.
6. Converteste un validator sau selector cu teste.
7. Ruleaza `clean build`.
8. Ruleaza `assemble`.
9. Inspecteaza JAR-ul core.
10. Ruleaza smoke test Paper dupa prima clasa Kotlin de productie.
11. Abia apoi continua cu pachete mai mari.

## Documente auxiliare adaugate

| Document | Rol |
|---|---|
| `kotlin-style-guide.md` | Reguli de stil Kotlin pentru proiect |
| `kotlin-interop-api-addonuri.md` | Contract pentru Java interop, API si addonuri |
| `kotlin-paper-packaging-si-smoke.md` | Verificari JAR, runtime Kotlin si smoke Paper |
| `kotlin-migration-tracker.md` | Tracker operational pentru slice-uri |
| `kotlin-code-review-checklist.md` | Checklist de review pentru schimbari Kotlin |
| `kotlin-coroutines-paper-policy.md` | Politica de amanare a coroutine in Paper |
| `kotlin-gradle-activation-plan.md` | Plan exact pentru activarea Kotlin in Gradle |
| `kotlin-testing-strategy.md` | Strategie de testare pentru conversii Kotlin |

## Comenzi esentiale

Compilare rapida:

```powershell
.\gradlew.bat clean compileJava compileTestJava
```

Build complet:

```powershell
.\gradlew.bat clean build
```

Generare JAR-uri:

```powershell
.\gradlew.bat assemble
```

Teste core:

```powershell
.\gradlew.bat :ainpc-core-plugin:test
```

Teste addon medieval:

```powershell
.\gradlew.bat :ainpc-scenario-medieval:test
```

Smoke mapping:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-mapping.ps1 `
  -ServerDir "C:\Minecraft\paper-test"
```

Smoke quest:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-quests.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -PlayerName "NumeleTau"
```

## Reguli care nu se negociaza

- Nu converti clase mari inainte de refactorizare.
- Nu converti `ainpc-api` fara test Java de consum.
- Nu introduce coroutine ca efect secundar al conversiei.
- Nu schimba schema DB in acelasi slice cu o conversie Kotlin.
- Nu schimba comportamentul public intr-o conversie mecanica.
- Nu continua daca JAR-ul Paper nu porneste.
- Nu folosi `!!` ca solutie generala pentru nullability.

## Definitia de gata pentru migrarea controlata

Migrarea poate fi considerata pornita corect cand:

- Kotlin este activ in `ainpc-core-plugin`
- exista cel putin un test Kotlin real
- primele conversii mici sunt validate
- `.\gradlew.bat clean build` trece
- JAR-ul core este inspectat
- serverul Paper porneste cu core + addon medieval, cand exista mediu Paper local disponibil
- `ainpc-api` ramane consumabil din Java

Kotlin poate deveni default pentru cod nou in core doar dupa:

- cel putin 10 fisiere convertite fara regresii
- cel putin 3 pachete atinse
- smoke Paper reusit
- JAR audit clar
- addon medieval incarcat
- reguli de interop respectate

## Citire rapida

Daca vrei doar directia:

- citeste acest rezumat
- apoi `conversie-java-la-kotlin-partea-4.md`

Daca vrei sa implementezi:

- citeste `conversie-java-la-kotlin-partea-2.md`
- apoi `conversie-java-la-kotlin-partea-3.md`
- foloseste `conversie-java-la-kotlin-partea-5.md` pentru tracking si verificari

Daca iei decizii de arhitectura:

- citeste `conversie-java-la-kotlin.md`
- apoi `conversie-java-la-kotlin-partea-4.md`
- apoi `conversie-java-la-kotlin-partea-5.md`
