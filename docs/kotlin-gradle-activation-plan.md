# Kotlin Gradle Activation Plan

Actualizat: 2026-05-16

## Scop

Acest document descrie exact cum se activeaza Kotlin in build-ul Gradle curent, fara sa convertesti inca fisiere Java.

Este documentul de executie pentru primul slice real al migrarii:

- KOT-001 din `kotlin-migration-tracker.md`
- Faza K din `conversie-java-la-kotlin-partea-3.md`
- F001 din `conversie-java-la-kotlin-partea-4.md`

## Starea curenta

Build-ul este Gradle multi-project:

- `settings.gradle`
- `build.gradle`
- `gradle.properties`
- `ainpc-api/build.gradle`
- `ainpc-core-plugin/build.gradle`
- `ainpc-scenario-medieval/build.gradle`

Parametrii sunt in `gradle.properties`.

Kotlin nu este inca activ.

## Decizie initiala

Kotlin se activeaza prima data doar in:

```text
ainpc-core-plugin
```

Nu se activeaza initial in:

```text
ainpc-api
ainpc-scenario-medieval
```

Motiv:

- core-ul este implementare interna
- API-ul trebuie sa ramana Java-friendly
- addonul medieval ramane test de consumator Java

## Pasul 1: Adauga versiunea Kotlin

In `gradle.properties`, adauga:

```properties
kotlinVersion=<versiune stabila aleasa>
```

Regula:

- versiunea se alege in momentul implementarii
- nu se combina cu upgrade Gradle
- daca apare incompatibilitate, revii doar la acest slice

## Pasul 2: Declara pluginul Kotlin la root

In `build.gradle`, adauga blocul `plugins` in partea de sus:

```groovy
plugins {
    id 'org.jetbrains.kotlin.jvm' version "${kotlinVersion}" apply false
}
```

Atentie:

- daca Gradle nu permite interpolarea directa aici in forma curenta, foloseste alternativa cu `pluginManagement` in `settings.gradle`
- nu aplica pluginul global pe toate subproiectele

Alternativa in `settings.gradle`:

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
```

## Pasul 3: Expune `kotlinVersion` in `ext`

In `build.gradle`:

```groovy
ext {
    javaRelease = property('javaRelease').toString().toInteger()
    kotlinVersion = property('kotlinVersion').toString()
    paperVersion = property('paperVersion').toString()
    gsonVersion = property('gsonVersion').toString()
    sqliteVersion = property('sqliteVersion').toString()
    okhttpVersion = property('okhttpVersion').toString()
    junitVersion = property('junitVersion').toString()
}
```

Regula:

- toate versiunile raman centralizate in `gradle.properties`

## Pasul 4: Configureaza compilarea Kotlin

In `build.gradle`, in `subprojects`, adauga configurare doar pentru modulele cu plugin Kotlin:

```groovy
plugins.withId('org.jetbrains.kotlin.jvm') {
    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
        }
    }
}
```

Regula:

- Java ramane pe `options.release = 21`
- Kotlin trebuie sa produca bytecode compatibil cu Java 21

## Pasul 5: Activeaza pluginul in core

In `ainpc-core-plugin/build.gradle`, adauga la inceput:

```groovy
plugins {
    id 'org.jetbrains.kotlin.jvm'
}
```

Pastreaza dependintele existente.

Adauga:

```groovy
dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib:${rootProject.ext.kotlinVersion}"
}
```

Regula:

- `kotlin-stdlib` trebuie sa fie runtime dependency pentru modulul care contine clase Kotlin
- JAR-ul shaded al core-ului trebuie sa o includa cand exista cod Kotlin de productie

## Pasul 6: Creeaza folderele Kotlin

Creeaza doar daca adaugi testul smoke:

```text
ainpc-core-plugin/src/test/kotlin
```

Pentru cod de productie, creeaza mai tarziu:

```text
ainpc-core-plugin/src/main/kotlin
```

Regula:

- nu crea foldere goale doar pentru estetica

## Pasul 7: Build fara fisiere Kotlin

Prima verificare trebuie sa fie fara fisiere `.kt`.

Comanda:

```powershell
.\gradlew.bat clean build
```

Gate:

- build verde
- JAR-urile se genereaza
- nu exista schimbari de comportament

## Pasul 8: Primul test Kotlin

Dupa build verde, adauga testul smoke din `conversie-java-la-kotlin-partea-3.md`.

Comanda:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*KotlinToolchainTest"
.\gradlew.bat clean build
```

Gate:

- JUnit ruleaza test Kotlin
- testele Java continua sa ruleze

## Pasul 9: Audit JAR

Dupa prima clasa Kotlin de productie, nu doar dupa test, ruleaza:

```powershell
.\gradlew.bat clean assemble
```

Apoi foloseste `kotlin-paper-packaging-si-smoke.md` pentru audit JAR.

## Probleme posibile

Problema:

```text
Plugin [id: 'org.jetbrains.kotlin.jvm'] was not found
```

Actiune:

- verifica `pluginManagement` in `settings.gradle`
- verifica versiunea Kotlin
- verifica accesul la Gradle Plugin Portal

Problema:

```text
Cannot access KotlinCompile
```

Actiune:

- foloseste numele complet al clasei Gradle Kotlin task
- verifica daca pluginul este aplicat inainte de configurare

Problema:

```text
NoClassDefFoundError: kotlin/jvm/internal/Intrinsics
```

Actiune:

- verifica `kotlin-stdlib`
- verifica JAR shaded
- ruleaza audit JAR

## Rollback

Pentru rollback complet:

1. sterge testele `.kt`
2. scoate pluginul Kotlin din `ainpc-core-plugin/build.gradle`
3. scoate `kotlin-stdlib`
4. scoate configurarea Kotlin din root `build.gradle`
5. scoate `kotlinVersion` din `gradle.properties`
6. ruleaza `.\gradlew.bat clean build`

## Definitia de gata

Kotlin este activat corect in Gradle cand:

- `ainpc-core-plugin` are plugin Kotlin activ
- `ainpc-api` ramane Java-only
- `ainpc-scenario-medieval` ramane Java-only
- `.\gradlew.bat clean build` trece
- testul Kotlin smoke trece, daca a fost adaugat
- prima clasa Kotlin de productie declanseaza audit JAR si smoke Paper
