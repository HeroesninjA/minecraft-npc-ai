# Kotlin Style Guide

Actualizat: 2026-05-25

## Scop

Acest document stabileste stilul Kotlin pentru proiectul AINPC.

Se aplica dupa ce Kotlin este activat in Gradle si inainte ca fisierele `.kt` sa devina uzuale in `ainpc-core-plugin`.

## Principii

- Tot codul nou de productie se scrie in Kotlin. Nu se adauga fisiere Java noi.
- Daca un entry point Java existent trebuie atins pentru cablaj, schimbarea Java trebuie sa ramana minima, iar logica noua sta in Kotlin.
- Kotlin trebuie sa faca intentia mai clara, nu doar codul mai scurt.
- Codul critic Paper, DB si API public ramane explicit.
- Java si Kotlin trebuie sa poata coexista fara adaptoare inutile.
- Stilul proiectului conteaza mai mult decat folosirea tuturor facilitatilor Kotlin.

## Layout

Layout permis:

```text
ainpc-core-plugin/src/main/kotlin/ro/ainpc/...
ainpc-core-plugin/src/test/kotlin/ro/ainpc/...
```

Reguli:

- package-ul ramane `ro.ainpc...`
- nu se creeaza package-uri `kotlin`, `kt` sau `kts`
- fisierele Kotlin stau langa domeniul real, nu intr-un folder separat conceptual
- clasele Java si Kotlin pot exista in acelasi package

## Naming

Clase:

- `PascalCase`
- numele ramane identic cu versiunea Java cand exista call-site-uri Java

Functii:

- `camelCase`
- nume explicite pentru domeniu
- evita prescurtari care ascund comportamentul

Constante:

- `UPPER_SNAKE_CASE` doar pentru constante reale
- valori de config sau defaults pot sta in `object`

Teste:

- pastreaza numele clasei de test cand convertesti din Java
- numele de test cu backticks este permis doar pentru teste noi si clare

## `val` si `var`

Regula:

- foloseste `val` implicit
- foloseste `var` doar cand starea se modifica real

Bun:

```kotlin
val normalizedId = input.trim().lowercase()
```

De evitat:

```kotlin
var normalizedId = input
normalizedId = normalizedId.trim()
normalizedId = normalizedId.lowercase()
```

Exceptii:

- acumulatoare locale simple
- stare interna controlata intr-un service
- builder intern unde mutabilitatea este limitata

## Nullability

Regula:

- nullability trebuie exprimata in tipuri
- `!!` este interzis in cod de productie, cu exceptie justificata in comentariu

Bun:

```kotlin
val regionId = config.getString("region")?.takeIf { it.isNotBlank() }
    ?: return Optional.empty()
```

De evitat:

```kotlin
val regionId = config.getString("region")!!
```

Pentru API public:

- nu inlocui `Optional<T>` cu `T?` fara test Java de consum
- nu transforma contracte Java existente fara documentare

## `data class`

Foloseste `data class` pentru:

- modele immutable interne
- snapshot-uri
- rezultate de validare
- optiuni de planner

Nu folosi `data class` cand:

- identitatea obiectului conteaza
- equals/hashCode existente nu trebuie schimbate
- clasa este API public consumat din Java fara test de compatibilitate
- librariile Java asteapta constructor sau accessors specifice

Checklist:

- equals/hashCode sunt dorite?
- getterele Java generate sunt acceptabile?
- constructorul vazut din Java este clar?
- serializarea YAML/JSON ramane compatibila?

## `object`

Foloseste `object` pentru:

- utilitare stateless
- singleton-uri reale
- grupuri de constante

Pentru consum Java:

```kotlin
object IdNormalizer {
    @JvmStatic
    fun normalize(value: String?): String = value?.trim().orEmpty()
}
```

Regula:

- `object` fara `@JvmStatic` poate schimba modul in care Java apeleaza metoda
- adauga `@JvmStatic` doar cand exista call-site Java real

## Top-level functions

Permise doar in internals Kotlin.

Pentru API public sau cod consumat din Java:

- prefera clasa sau `object`
- daca folosesti top-level, seteaza nume JVM:

```text
@file:JvmName("MappingIds")

package ro.ainpc.world.mapping
```

Regula:

- top-level functions nu sunt prima alegere pentru cod care trebuie consumat de Java.

## `sealed`

Foloseste `sealed interface` sau `sealed class` pentru:

- rezultate interne cu set inchis de variante
- state machine interne
- validari interne

Evita in:

- `ainpc-api`
- contracte pentru addonuri Java
- tipuri care trebuie extinse din afara modulului

## Colectii

Reguli:

- foloseste `List` pentru snapshot-uri read-only
- foloseste `MutableList` doar cand callerul trebuie sa modifice lista
- intoarce copii pentru snapshot-uri publice
- nu expune colectii mutable din internals fara motiv

Bun:

```kotlin
fun entries(): List<ProgressionEntry> = entries.toList()
```

## Exceptions

Reguli:

- nu transforma exceptii verificate Java in runtime failures fara decizie
- pastreaza mesajele existente in conversii mecanice
- logheaza prin loggerul proiectului
- nu folosi `println`

## Scope functions

Permise:

- `let` pentru null handling local
- `also` pentru logging/debug local
- `apply` pentru configurare de obiect

De evitat:

- chain-uri lungi care ascund flow-ul
- combinatii `let/apply/run/also` greu de urmarit
- scope functions in cod critic daca un `if` simplu este mai clar

## Coroutines

Regula:

- nu se introduc coroutine ca parte a conversiei Java -> Kotlin

Motiv:

- Paper are propriul model de scheduling
- coroutine necesita document separat pentru lifecycle, cancellation si thread safety

## Paper si Bukkit

Reguli:

- entrypoint-ul Paper ramane Java pana la subtiere si smoke test
- listener-ele principale nu se convertesc devreme
- nu schimba threading-ul
- nu schimba `plugin.yml` intr-o conversie mecanica
- verifica server Paper dupa orice schimbare in lifecycle sau packaging

## Stil pentru servicii

Servicii normale:

```kotlin
class QuestSelector(
    private val logger: Logger,
    private val repository: QuestRepository,
)
```

Reguli:

- constructor injection pentru servicii normale
- evita `lateinit` daca dependinta poate fi constructor parameter
- metodele publice trebuie sa ramana clare pentru call-site-uri Java

## Stil pentru teste

Reguli:

- foloseste JUnit 5
- pastreaza assertions explicite
- nu ascunde setup-ul in DSL-uri premature
- testeaza interop Java separat pentru API public

Exemplu:

```kotlin
class ProgressionSelectorTest {
    @Test
    fun selectsTrackedProgression() {
        assertEquals("tracked", ProgressionSelector.tracked().id())
    }
}
```

## Anti-patterns

- conversii masive de pachete intregi fara gate-uri
- `!!` repetat
- `lateinit` pentru dependinte normale
- top-level mutable state
- DSL-uri interne premature
- coroutine introduse accidental
- schimbare de schema DB in acelasi slice
- schimbare de comportament public intr-o conversie mecanica

## Definitia de gata

Un fisier Kotlin respecta stilul proiectului daca:

- are package corect
- are `val` implicit
- nu foloseste `!!` fara justificare
- pastreaza compatibilitatea Java unde este necesar
- are teste sau este acoperit de teste existente
- nu schimba comportamentul public
- nu introduce dependinte sau runtime nou fara documentare
