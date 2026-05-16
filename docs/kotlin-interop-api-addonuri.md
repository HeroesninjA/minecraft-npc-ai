# Kotlin Interop, API si Addonuri

Actualizat: 2026-05-16

## Scop

Acest document defineste regulile de interoperabilitate Java/Kotlin pentru AINPC.

Este obligatoriu pentru orice conversie care atinge:

- `ainpc-api`
- clase consumate de `ainpc-scenario-medieval`
- clase publice din `ainpc-core-plugin`
- utilitare Kotlin apelate din Java

## Decizie de baza

`ainpc-api` ramane Java pana cand exista un motiv clar sa fie convertit.

Motiv:

- addonurile Minecraft sunt frecvent scrise in Java
- API-ul trebuie sa fie simplu de consumat fara cunostinte Kotlin
- nullability, default args si top-level functions pot face API-ul mai greu de folosit din Java

## Reguli pentru `ainpc-api`

Nu converti in primele faze:

- `AINPCAddon`
- `AINPCPlatformApi`
- `AddonRegistryApi`
- `WorldAdminApi`
- DTO-urile publice world
- enum-urile publice folosite de addonuri

Daca se converteste un tip public:

1. se converteste un singur tip
2. se scrie test Java de consum
3. se compileaza addonul medieval
4. se documenteaza semnatura vazuta din Java
5. se ruleaza `clean build`

## Test Java de consum

Orice tip Kotlin public trebuie sa aiba un test Java care il foloseste ca un addon real.

Exemplu:

```java
class KotlinApiInteropTest {
    @Test
    void javaCanConstructAndReadKotlinType() {
        SomeKotlinApiType value = new SomeKotlinApiType("id");
        assertEquals("id", value.getId());
    }
}
```

Ce verifica testul:

- constructor accesibil din Java
- getter-e clare
- nullability acceptabila
- colectii consumabile
- metode statice disponibile daca au existat in Java

## Constructori si default arguments

Kotlin:

```kotlin
class RetryPolicy(
    val maxAttempts: Int,
    val delayMillis: Long = 250L,
)
```

Java nu vede default argument natural.

Daca Java trebuie sa poata apela constructorul scurt:

```kotlin
class RetryPolicy @JvmOverloads constructor(
    val maxAttempts: Int,
    val delayMillis: Long = 250L,
)
```

Regula:

- `@JvmOverloads` se foloseste doar cand exista call-site Java real
- in API public, evita default args daca semnatura Java trebuie sa fie strict controlata

## Metode statice

Java existent:

```java
IdNormalizer.normalize(input);
```

Kotlin compatibil:

```kotlin
object IdNormalizer {
    @JvmStatic
    fun normalize(input: String?): String = input?.trim().orEmpty()
}
```

Regula:

- daca Java apela metoda static, pastreaza apelul static
- evita schimbarea call-site-urilor Java doar pentru estetica Kotlin

## Campuri si constante

Kotlin:

```kotlin
object Defaults {
    @JvmField
    val REGION_ID: String = "demo_sat"
}
```

Regula:

- `@JvmField` este permis pentru constante consumate direct din Java
- pentru valori calculate, foloseste metoda sau getter

## Top-level functions

Evita top-level functions in API public.

Daca sunt necesare:

```kotlin
@file:JvmName("WorldIds")

package ro.ainpc.world

fun normalizeWorldId(value: String): String = value.trim().lowercase()
```

Regula:

- fara `@file:JvmName`, Java primeste nume generate care pot fi neclare
- prefera `object` pentru API stabil

## Nullability

Reguli:

- `T?` este clar in Kotlin, dar nu impune automat disciplina in Java
- pentru API public existent, pastreaza `Optional<T>` daca Java consumers il folosesc
- nu schimba `Optional<T>` in `T?` fara test Java

Kotlin intern:

```kotlin
fun findRegion(id: String): WorldRegion?
```

API Java-friendly:

```java
Optional<WorldRegionInfo> findRegion(String id);
```

## Colectii

Pentru API public:

- prefera `java.util.List`/`Collection` prin semnaturi clare
- evita `MutableList` expus public
- documenteaza daca lista este snapshot sau live

Regula:

- Java consumers nu trebuie sa ghiceasca daca pot modifica lista.

## `data class` in API

Risc:

- getterele Java devin `getId()`, nu `id()`
- equals/hashCode se schimba automat
- componentN/copy apar in bytecode si pot deveni suprafata accidentala

Recomandare:

- evita `data class` in `ainpc-api` pana cand API-ul este stabil
- pastreaza DTO-urile publice Java daca sunt deja consumate

## `sealed` in API

Evita sealed Kotlin in `ainpc-api` pentru primele faze.

Motiv:

- Java consumers pot consuma mai greu ierarhii sealed Kotlin
- addonurile pot avea nevoie de extensie

Foloseste sealed doar intern in core.

## `internal`

Atentie:

- `internal` este granita Kotlin, nu protectie reala pentru Java sau pentru arhitectura publica

Regula:

- nu te baza pe `internal` pentru API stability
- foloseste module Gradle, package-uri si documentatie API pentru granite reale

## Addon medieval ca test de compatibilitate

`ainpc-scenario-medieval` trebuie sa ramana testul minim pentru API.

Dupa orice schimbare in API:

```powershell
.\gradlew.bat :ainpc-api:build
.\gradlew.bat :ainpc-scenario-medieval:build
.\gradlew.bat clean build
```

Dupa orice schimbare de packaging:

```powershell
.\gradlew.bat assemble
```

Smoke Paper:

- porneste core + addon medieval
- confirma ca addonul se incarca dupa core
- confirma ca pack-ul medieval este gasit

## Matrice interop

| Schimbare | Test minim |
|---|---|
| Tip Kotlin in API | test Java de consum |
| Utilitar static convertit | call-site Java compileaza fara schimbare |
| Constructor cu defaults | test Java pentru constructori |
| Colectie expusa | test Java pentru iterare si imutabilitate asteptata |
| Optional -> nullable | interzis fara decizie explicita |
| `data class` public | test equals/getters/constructor Java |
| Top-level function publica | `@file:JvmName` si test Java |

## Definitia de gata

Interop-ul este acceptabil daca:

- Java consumers compileaza
- addonul medieval compileaza
- testul Java de consum trece pentru orice tip Kotlin public
- nu apar adaptoare inutile doar pentru Kotlin
- documentatia API este actualizata
- serverul Paper incarca addonul dupa core
