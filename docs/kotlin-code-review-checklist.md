# Kotlin Code Review Checklist

Actualizat: 2026-05-16

## Scop

Acest document este checklist-ul pentru review-ul schimbarilor Kotlin.

Se foloseste pentru orice PR sau slice care:

- activeaza Kotlin in Gradle
- adauga fisiere `.kt`
- converteste fisiere Java
- modifica packaging-ul JAR
- atinge API public sau addonuri

## Intrebarea principala

Schimbarea este o conversie mecanica sau schimba comportamentul?

Daca schimba comportamentul, trebuie tratata ca feature/refactor separat, nu ca simpla conversie Kotlin.

## Checklist general

- package-ul a ramas corect
- numele clasei a ramas compatibil daca era folosit din Java
- nu s-au schimbat mesaje de eroare fara motiv
- nu s-a schimbat schema DB
- nu s-a schimbat `plugin.yml` accidental
- nu s-a schimbat threading-ul
- nu s-au introdus coroutine
- nu exista `!!` nejustificat
- nu exista `lateinit` pentru dependinte normale
- testele relevante au fost rulate

## Gradle

Verifica:

- `kotlinVersion` este in `gradle.properties`
- pluginul Kotlin este aplicat doar pe modulele necesare
- `ainpc-api` nu primeste Kotlin fara decizie explicita
- `kotlin-stdlib` este disponibila pentru modulul care are clase Kotlin
- task-ul de JAR include runtime-ul necesar
- `clean build` trece

Comenzi:

```powershell
.\gradlew.bat clean build
.\gradlew.bat assemble
```

## Interop Java

Verifica:

- Java call-sites compileaza
- metodele statice au ramas statice daca era nevoie
- constructorii folositi din Java sunt disponibili
- default arguments Kotlin nu au rupt apelurile Java
- getterele generate sunt acceptabile
- `Optional` nu a fost schimbat in nullable fara decizie
- colectiile expuse sunt clare pentru Java

Cand este necesar:

- `@JvmStatic`
- `@JvmOverloads`
- `@JvmField`
- `@file:JvmName`

Regula:

- adnotarile JVM se adauga pentru nevoi reale, nu preventiv peste tot.

## API public

Daca s-a atins `ainpc-api`:

- exista test Java de consum
- addonul medieval compileaza
- documentatia API este actualizata
- nullability este clara pentru Java
- nu s-au introdus sealed types greu de consumat
- nu s-au introdus top-level functions publice fara nume JVM controlat

Comenzi:

```powershell
.\gradlew.bat :ainpc-api:build
.\gradlew.bat :ainpc-scenario-medieval:build
```

## Paper runtime

Daca s-au atins entrypoint-uri, listener-e, comenzi, GUI sau packaging:

- JAR-ul core este generat
- `plugin.yml` este prezent si corect
- runtime-ul Kotlin este prezent daca exista clase Kotlin
- smoke testul Paper a fost rulat
- addonul medieval se incarca dupa core

Comenzi:

```powershell
.\gradlew.bat clean assemble
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-mapping.ps1 `
  -ServerDir "C:\Minecraft\paper-test"
```

## Kotlin style

Verifica:

- `val` este folosit implicit
- `var` are motiv clar
- `data class` este folosita doar cand equals/hashCode sunt dorite
- `object` reprezinta singleton real sau utilitar stateless
- top-level functions nu sunt public API accidental
- `sealed` este folosit doar intern
- scope functions nu fac codul greu de urmarit
- colectiile mutable nu sunt expuse inutil

## Nullability

Verifica:

- `?` este folosit explicit unde valoarea poate lipsi
- `!!` lipseste sau are justificare clara
- conversia nu ascunde un caz invalid
- Java callers nu primesc ambiguitate noua
- `Optional` este pastrat in API public existent

## DB si persistenta

Daca s-a atins un repository:

- SQL-ul este echivalent
- tranzactiile nu s-au schimbat
- resursele sunt inchise corect
- testele DB trec repetabil
- nu exista fisiere temporare lasate in repo

Comenzi:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*RepositoryTest"
.\gradlew.bat :ainpc-core-plugin:test --tests "*Persistence*"
```

## AI si networking

Daca s-a atins AI/OpenAI:

- payload-ul trimis nu s-a schimbat accidental
- fallback policy nu s-a schimbat in conversie mecanica
- logging-ul nu expune date sensibile
- nu s-au introdus coroutine sau threading nou
- testele de orchestrare trec

Comanda:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*AIOrchestration*"
```

## Acceptare

O conversie Kotlin este acceptabila daca:

- diff-ul este mic si inteligibil
- comportamentul este neschimbat sau schimbarea este documentata separat
- testele relevante trec
- interop-ul Java este verificat
- packaging-ul Paper este verificat cand este cazul
- rollback-ul este clar

## Respinge sau imparte schimbarea daca

- converteste o clasa mare direct
- atinge API public fara test Java
- introduce coroutine
- schimba DB schema
- rupe addonul medieval
- necesita multe modificari Java nelegate
- face codul mai greu de citit decat versiunea Java
