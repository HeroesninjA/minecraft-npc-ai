# Conversie Java la Kotlin - Partea 3

Actualizat: 2026-05-17

## Scop

Acest document continua `conversie-java-la-kotlin.md` si `conversie-java-la-kotlin-partea-2.md` cu retete concrete de implementare.

Partea 1 defineste strategia. Partea 2 defineste runbook-ul de executie. Partea 3 arata cum se fac efectiv pasii tehnici: configurare Gradle, layout Kotlin, conversii tipice, interop Java si verificari pe JAR.

Continuare pe pachetele reale ale proiectului: `conversie-java-la-kotlin-partea-4.md`.

## Faza K: Configurare Gradle recomandata

Obiectiv:

- introducerea Kotlin in build fara sa afecteze modulele care raman Java

Parametru in `gradle.properties`:

```properties
kotlinVersion=2.2.0
```

Nota:

- configurarea Kotlin-first este deja aplicata in build-ul curent
- foloseste in continuare o versiune stabila compatibila cu Gradle-ul proiectului
- nu combina upgrade Gradle si introducere Kotlin in acelasi slice daca apar probleme

Root `build.gradle`, forma recomandata:

```groovy
plugins {
    id 'org.jetbrains.kotlin.jvm' version "${kotlinVersion}" apply false
}

ext {
    javaRelease = property('javaRelease').toString().toInteger()
    kotlinVersion = property('kotlinVersion').toString()
}
```

Aplicare in `ainpc-core-plugin/build.gradle`:

```groovy
plugins {
    id 'org.jetbrains.kotlin.jvm'
}

dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib:${rootProject.ext.kotlinVersion}"
}
```

Configurare Kotlin in root, pentru subproiectele care au pluginul activ:

```groovy
subprojects {
    plugins.withId('org.jetbrains.kotlin.jvm') {
        tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
            compilerOptions {
                jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
            }
        }
    }
}
```

Regula:

- Kotlin se aplica explicit pe modul, nu global pe toate modulele din prima zi.

## Faza L: Layout fisiere Kotlin

Structura recomandata pentru core:

```text
ainpc-core-plugin/
  src/main/java/
  src/main/kotlin/
  src/main/resources/
  src/test/java/
  src/test/kotlin/
```

Reguli:

- package-ul ramane `ro.ainpc...`
- nu muta Java in Kotlin doar pentru a schimba folderul
- Java si Kotlin pot coexista in acelasi package
- nu crea package-uri paralele de tip `kotlin` sau `kt`

Exemplu bun:

```text
src/main/java/ro/ainpc/world/mapping/MappingIntentParser.java
src/test/kotlin/ro/ainpc/world/mapping/MappingIntentParserKotlinTest.kt
```

Exemplu prost:

```text
src/main/kotlin/ro/ainpc/kotlin/world/mapping/...
```

Motiv:

- package-urile trebuie sa reflecte domeniul, nu limbajul.

## Faza M: Primul test Kotlin minim

Obiectiv:

- confirmarea ca `src/test/kotlin` compileaza si JUnit 5 ruleaza teste Kotlin

Exemplu:

```kotlin
package ro.ainpc.kotlincheck

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KotlinToolchainTest {
    @Test
    fun `compiles and runs kotlin tests`() {
        assertEquals("ainpc", "ai" + "npc")
    }
}
```

Comanda:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*KotlinToolchainTest"
```

Gate:

- testul ruleaza
- nu apar schimbari in codul de productie
- `clean build` trece

Stergere:

- dupa primele conversii reale, testul smoke poate fi sters daca devine redundant

## Faza N: Reteta pentru conversia unui test Java

Inainte:

```java
class ProgressionFilterTest {
    @Test
    void matchesActiveProgression() {
        assertTrue(ProgressionFilter.ACTIVE.matches("active"));
    }
}
```

Dupa:

```kotlin
package ro.ainpc.progression

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProgressionFilterTest {
    @Test
    fun matchesActiveProgression() {
        assertTrue(ProgressionFilter.ACTIVE.matches("active"))
    }
}
```

Reguli:

- pastreaza numele clasei de test daca nu creeaza conflict
- sterge fisierul Java dupa conversie
- nu schimba numele testului daca rapoartele istorice conteaza
- foloseste backticks in nume de test doar daca stilul echipei accepta asta

Comenzi:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.progression.ProgressionFilterTest"
.\gradlew.bat clean build
```

## Faza O: Reteta pentru utilitar static Java

Java tipic:

```java
public final class TextKeys {
    private TextKeys() {
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
```

Kotlin recomandat pentru consum Java simplu:

```kotlin
package ro.ainpc.utils

import java.util.Locale

object TextKeys {
    @JvmStatic
    fun normalize(value: String?): String {
        return value?.trim()?.lowercase(Locale.ROOT).orEmpty()
    }
}
```

Consum din Java ramane:

```java
TextKeys.normalize(input);
```

Regula:

- foloseste `object` plus `@JvmStatic` daca vrei sa pastrezi apelul static Java
- nu transforma automat totul in top-level function, pentru ca numele JVM devine mai putin evident

## Faza P: Reteta pentru model immutable

Java tipic:

```java
public final class PatchPlannerOptions {
    private final int radius;
    private final boolean dryRun;

    public PatchPlannerOptions(int radius, boolean dryRun) {
        this.radius = radius;
        this.dryRun = dryRun;
    }

    public int radius() {
        return radius;
    }

    public boolean dryRun() {
        return dryRun;
    }
}
```

Kotlin:

```kotlin
package ro.ainpc.world.patch

data class PatchPlannerOptions(
    val radius: Int,
    val dryRun: Boolean,
)
```

Atentie pentru Java:

```java
PatchPlannerOptions options = new PatchPlannerOptions(32, true);
int radius = options.getRadius();
boolean dryRun = options.getDryRun();
```

Risc:

- Java record-style accessors `radius()` devin getters Java `getRadius()`
- daca Java existent foloseste `radius()`, conversia rupe call-site-urile

Solutii:

- pastreaza Java pentru tipurile consumate intens
- adauga metode compatibile manual in Kotlin
- schimba call-site-urile intr-un slice separat, daca merita

Varianta compatibila:

```kotlin
data class PatchPlannerOptions(
    private val radiusValue: Int,
    private val dryRunValue: Boolean,
) {
    fun radius(): Int = radiusValue
    fun dryRun(): Boolean = dryRunValue
}
```

Regula:

- pentru modele interne pure, foloseste getters Kotlin normale
- pentru modele deja consumate mult din Java, verifica semnaturile inainte de conversie

## Faza Q: Reteta pentru rezultat inchis

Java tipic:

```java
public enum PatchValidationStatus {
    VALID,
    WARNING,
    INVALID
}
```

Enum simplu ramane foarte bine in Java sau Kotlin:

```kotlin
enum class PatchValidationStatus {
    VALID,
    WARNING,
    INVALID,
}
```

Pentru rezultate cu date atasate:

```kotlin
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Warning(val messages: List<String>) : ValidationResult
    data class Invalid(val errors: List<String>) : ValidationResult
}
```

Atentie:

- sealed types sunt utile intern
- pentru `ainpc-api`, sealed Kotlin poate fi incomod pentru addonuri Java

Regula:

- sealed pentru internals
- enum sau interfete Java simple pentru API public, pana exista motiv clar sa schimbi

## Faza R: Reteta pentru validator

Java tipic:

```java
public ValidationReport validate(FeaturePack pack) {
    List<String> errors = new ArrayList<>();
    if (pack.id() == null || pack.id().isBlank()) {
        errors.add("Feature pack id is required.");
    }
    return new ValidationReport(errors);
}
```

Kotlin:

```kotlin
fun validate(pack: FeaturePack): ValidationReport {
    val errors = mutableListOf<String>()
    if (pack.id.isNullOrBlank()) {
        errors += "Feature pack id is required."
    }
    return ValidationReport(errors)
}
```

Reguli:

- mesajele de eroare raman identice
- ordinea mesajelor ramane identica
- nu schimba simultan modelul `FeaturePack`
- adauga test pentru cazurile invalide

Comanda:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*FeaturePack*ValidatorTest"
```

## Faza S: Reteta pentru repository JDBC

Recomandare:

- nu converti repository-uri JDBC in primele faze

Daca totusi convertesti un mapper mic:

```kotlin
private fun mapRow(resultSet: ResultSet): StoredProgression {
    return StoredProgression(
        playerId = UUID.fromString(resultSet.getString("player_id")),
        progressionId = resultSet.getString("progression_id"),
        status = resultSet.getString("status"),
    )
}
```

Reguli:

- inchiderea resurselor ramane explicita
- tranzactiile nu se schimba
- SQL-ul nu se schimba
- testele DB ruleaza inainte si dupa conversie

De evitat:

- schimbarea DB schema
- schimbarea numelor de coloane
- mutarea la coroutine
- abstractizare noua peste JDBC doar pentru ca ai introdus Kotlin

## Faza T: Reteta pentru servicii Paper

Serviciile normale pot folosi constructor injection:

```kotlin
class QuestAnchorResolver(
    private val worldAdminService: WorldAdminService,
    private val logger: Logger,
) {
    fun resolve(selector: String): QuestAnchor? {
        // logic determinist
        return null
    }
}
```

Entry point Paper nu trebuie tratat la fel:

```kotlin
class AINPCPlugin : JavaPlugin() {
    override fun onEnable() {
        // Paper construieste pluginul; nu cere constructor cu argumente.
    }
}
```

Regula:

- serviciile pot fi Kotlin cu constructor injection
- clasa principala Paper ramane Java pana cand este foarte subtire si smoke-tested

Verificare obligatorie dupa entrypoint changes:

```powershell
.\gradlew.bat assemble
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-mapping.ps1 `
  -ServerDir "C:\Minecraft\paper-test"
```

## Faza U: Interop annotations

Foloseste doar cand exista consumator Java real.

`@JvmStatic`:

```kotlin
object IdNormalizer {
    @JvmStatic
    fun normalize(value: String?): String = value?.trim().orEmpty()
}
```

`@JvmOverloads`:

```kotlin
class RetryPolicy @JvmOverloads constructor(
    val maxAttempts: Int,
    val delayMillis: Long = 250L,
)
```

`@JvmField`:

```kotlin
object Constants {
    @JvmField
    val DEFAULT_REGION_ID: String = "demo_sat"
}
```

`@file:JvmName` pentru top-level functions:

```kotlin
@file:JvmName("MappingIds")

package ro.ainpc.world.mapping

fun normalizeMappingId(value: String): String = value.trim().lowercase()
```

Regula:

- fara adnotari JVM preventive peste tot
- adauga-le cand un call-site Java concret are nevoie de ele

## Faza V: Nullability

Regula generala:

- Kotlin trebuie sa faca nullability mai clara, nu sa ascunda problemele cu `!!`

Java:

```java
String regionId = config.getString("region");
if (regionId == null || regionId.isBlank()) {
    return Optional.empty();
}
```

Kotlin:

```kotlin
val regionId = config.getString("region")?.takeIf { it.isNotBlank() }
    ?: return Optional.empty()
```

Cand pastrezi `Optional`:

- daca metoda este consumata mult din Java
- daca API-ul existent foloseste `Optional`
- daca vrei compatibilitate fara schimbarea call-site-urilor

Cand folosesti nullable Kotlin:

- in internals Kotlin
- in cod nou neexpus public
- cand call-site-urile sunt tot Kotlin

Regula pentru API:

- nu schimba `Optional<T>` in `T?` in `ainpc-api` fara test Java de consum

## Faza W: Colectii

Kotlin intern:

```kotlin
fun entries(): List<ProgressionEntry>
```

Cand Java modifica lista:

```kotlin
fun mutableEntries(): MutableList<ProgressionEntry>
```

Reguli:

- nu expune `MutableList` daca nu trebuie modificata de caller
- intoarce copii pentru snapshot-uri
- documenteaza daca lista este live sau snapshot
- evita schimbarea contractului Java existent fara test

Exemplu snapshot:

```kotlin
fun entries(): List<ProgressionEntry> = entries.toList()
```

## Faza X: Exceptions si logging

Reguli:

- nu transforma exceptii verificate Java in erori runtime fara decizie explicita
- pastreaza mesajele log existente in conversii mecanice
- nu folosi `println`
- foloseste loggerul deja injectat sau existent

Kotlin:

```kotlin
try {
    repository.save(snapshot)
} catch (exception: SQLException) {
    logger.warning("Could not save progression snapshot: ${exception.message}")
}
```

Atentie:

- string interpolation poate schimba formatul mesajelor
- daca testele sau runbook-urile cauta texte exacte, pastreaza textul identic

## Faza Y: Inspectarea JAR-ului pentru Kotlin

Comanda pentru JAR core:

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$jar = [System.IO.Compression.ZipFile]::OpenRead("ainpc-core-plugin\build\libs\ainpc-core-plugin-1.0.0.jar")
try {
  $jar.Entries |
    Where-Object { $_.FullName -match "kotlin/|ro/ainpc/.*\.class" } |
    Select-Object -First 40 -ExpandProperty FullName
} finally {
  $jar.Dispose()
}
```

Ce verifici:

- clasele Kotlin proprii sunt in JAR
- `kotlin-stdlib` este disponibil daca modulul are Kotlin
- nu exista duplicate ciudate
- `plugin.yml` ramane in radacina JAR-ului

Verificare `plugin.yml`:

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$jar = [System.IO.Compression.ZipFile]::OpenRead("ainpc-core-plugin\build\libs\ainpc-core-plugin-1.0.0.jar")
try {
  $entry = $jar.GetEntry("plugin.yml")
  $reader = [System.IO.StreamReader]::new($entry.Open())
  try { $reader.ReadToEnd() } finally { $reader.Dispose() }
} finally {
  $jar.Dispose()
}
```

## Faza Z: PR template pentru conversii Kotlin

Fiecare slice de conversie trebuie sa poata raspunde la aceste intrebari:

```text
Scop:
- ce clasa/pachet a fost convertit si de ce

Tip conversie:
- test / utilitar / model / validator / repository / serviciu Paper / API

Compatibilitate Java:
- exista call-site-uri Java?
- s-au pastrat numele metodelor?
- au fost necesare @JvmStatic / @JvmOverloads / @JvmField?

Riscuri:
- nullability
- equals/hashCode
- serializare YAML/JSON
- DB/JDBC
- Paper lifecycle

Verificare:
- comenzi Gradle rulate
- teste specifice rulate
- JAR inspectat, daca este cazul
- smoke Paper, daca este cazul

Rollback:
- ce fisiere se pot readuce la Java fara sa afecteze restul migrarii
```

## Checklist final pentru un slice Kotlin

Inainte de merge:

- `.\gradlew.bat clean build` trece
- nu exista schimbari accidentale in `plugin.yml`
- nu exista schimbari accidentale in `gradle.properties`, in afara versiunilor planificate
- `ainpc-api` ramane consumabil din Java
- JAR-ul core se genereaza
- smoke Paper este rulat daca ai atins lifecycle, listener, comenzi, packaging sau runtime Kotlin

## Ordine recomandata dupa Partea 3

1. implementeaza Faza K fara clase `.kt`
2. implementeaza Faza M cu un test minim
3. converteste 1 test real dupa Faza N
4. converteste 1 utilitar dupa Faza O
5. converteste 1 validator dupa Faza R
6. inspecteaza JAR-ul dupa Faza Y
7. actualizeaza documentatia cu ce a mers si ce nu

## Definitia de gata pentru Partea 3

Partea 3 este indeplinita cand:

- exista configuratie Gradle Kotlin valida
- exista layout `src/main/kotlin` si `src/test/kotlin` unde este nevoie
- exista cel putin un exemplu real convertit
- regulile de interop sunt aplicate doar unde trebuie
- JAR-ul final este inspectat
- smoke testul Paper este rulat pentru schimbari care pot afecta incarcarea pluginului
