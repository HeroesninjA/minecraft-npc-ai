# Kotlin Testing Strategy

Actualizat: 2026-05-16

## Scop

Acest document defineste strategia de testare pentru conversia Java -> Kotlin.

Se foloseste impreuna cu:

- `kotlin-migration-tracker.md`
- `kotlin-code-review-checklist.md`
- `kotlin-paper-packaging-si-smoke.md`

## Principii

- Testele trebuie sa dovedeasca faptul ca semantica Java a ramas aceeasi.
- Fiecare conversie mica are test specific.
- Fiecare conversie de productie are test de modul sau `clean build`.
- Orice schimbare care poate afecta Paper are smoke test.
- Orice schimbare in API are test Java de consum.

## Niveluri de testare

Nivel 1: test specific

- ruleaza doar testul clasei convertite
- folosit pentru feedback rapid

Nivel 2: test de pachet

- ruleaza toate testele tematice pentru pachet
- folosit dupa conversia unui model/validator/selector

Nivel 3: test de modul

- ruleaza `:ainpc-core-plugin:test`
- folosit dupa cod de productie Kotlin

Nivel 4: build complet

- ruleaza `clean build`
- folosit dupa fiecare slice important

Nivel 5: smoke Paper

- porneste server Paper cu JAR-urile generate
- folosit pentru runtime, packaging, listener-e, comenzi, GUI, entrypoints

## Matrice pe tip de conversie

| Tip conversie | Test minim | Test suplimentar |
|---|---|---|
| test Kotlin nou | test specific | `:ainpc-core-plugin:test` |
| test Java convertit | test specific | test de pachet |
| utilitar pur | test specific | `clean build` |
| model intern | test specific | test pentru equality/accessori daca e cazul |
| validator | test de validator | verificare mesaje de eroare |
| selector/resolver | test specific | test de pachet |
| repository | repository tests | repetare test DB |
| service Paper | `clean build` | smoke Paper |
| packaging | `assemble` | audit JAR |
| API public | test Java de consum | addon medieval build |

## Comenzi rapide

Core:

```powershell
.\gradlew.bat :ainpc-core-plugin:test
```

Build complet:

```powershell
.\gradlew.bat clean build
```

JAR:

```powershell
.\gradlew.bat assemble
```

Addon:

```powershell
.\gradlew.bat :ainpc-scenario-medieval:test
.\gradlew.bat :ainpc-scenario-medieval:build
```

API:

```powershell
.\gradlew.bat :ainpc-api:build
```

## Teste tematice

Progression:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*Progression*"
```

Mapping:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*Mapping*"
```

Patch planner:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*VillagePatchPlannerTest"
```

Quest si engine:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*Quest*"
.\gradlew.bat :ainpc-core-plugin:test --tests "*FeaturePack*"
.\gradlew.bat :ainpc-core-plugin:test --tests "*ScenarioRuntime*"
```

Spawn:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*Spawn*"
.\gradlew.bat :ainpc-core-plugin:test --tests "*House*"
```

Routine:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*Routine*"
```

Story:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*Story*"
```

GUI:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*Gui*"
```

AI orchestration:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*AIOrchestration*"
```

## Test Java de consum pentru API

Cand un tip Kotlin ajunge in API public:

```java
class KotlinApiConsumerTest {
    @Test
    void javaAddonCanUseKotlinApiType() {
        // construieste tipul
        // apeleaza getterele
        // verifica colectiile si Optional/nullability
    }
}
```

Reguli:

- testul trebuie sa fie Java, nu Kotlin
- testul trebuie sa simuleze un addon
- testul trebuie sa compileze fara helperi Kotlin

## Teste pentru equality

Daca o clasa Java devine `data class`, adauga test pentru:

- equals
- hashCode
- folosire in `HashSet` sau `Map`, daca este relevant
- comportament cand doua instante au aceleasi valori

Motiv:

- Java classes pot fi comparate prin identitate daca nu aveau equals custom
- `data class` schimba asta automat

## Teste pentru accessori Java

Daca Java apela:

```java
options.radius()
```

si Kotlin genereaza:

```java
options.getRadius()
```

trebuie decis:

- pastrezi metode compatibile in Kotlin
- sau modifici call-site-urile intr-un slice separat

Testul trebuie sa confirme forma aleasa.

## Teste DB

Pentru repository-uri:

- ruleaza testul de repository de cel putin doua ori
- verifica cleanup-ul fisierelor temporare
- nu schimba schema DB
- nu schimba tranzactiile

Comanda:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*RepositoryTest"
.\gradlew.bat :ainpc-core-plugin:test --tests "*RepositoryTest"
```

## Smoke Paper

Smoke devine obligatoriu cand:

- exista prima clasa Kotlin de productie
- se schimba packaging
- se schimba `plugin.yml`
- se ating comenzi, listener-e, GUI sau lifecycle

Comenzi:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-mapping.ps1 `
  -ServerDir "C:\Minecraft\paper-test"
```

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-quests.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -PlayerName "NumeleTau"
```

## Raport de testare

Pentru fiecare slice:

```text
ID slice:
Teste specifice:
Teste tematice:
Build complet:
JAR audit:
Smoke Paper:
Rezultat:
Probleme:
```

## Definitia de gata

Un slice Kotlin este testat suficient cand:

- testul specific trece
- testele tematice relevante trec
- `clean build` trece pentru cod de productie
- JAR audit este facut pentru packaging
- smoke Paper este facut pentru runtime Paper
- API public are test Java de consum
