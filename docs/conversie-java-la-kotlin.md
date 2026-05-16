# Conversie Java la Kotlin

Actualizat: 2026-05-16

## Scop

Acest document descrie o migrare pe faze de la Java la Kotlin pentru proiectul AINPC, dupa conversia build-ului la Gradle.

Migrarea trebuie facuta incremental. Java si Kotlin pot coexista in acelasi modul, deci nu este nevoie de o rescriere completa. Obiectivul corect este reducerea codului repetitiv si cresterea sigurantei tipurilor fara sa rupi compatibilitatea cu Paper, addonurile si API-ul public.

Continuare operationala: `conversie-java-la-kotlin-partea-2.md`.

## Principii

- Nu se converteste tot proiectul intr-un singur pas.
- Nu se schimba comportamentul in aceeasi faza cu o conversie mecanica mare.
- `ainpc-api` trebuie sa ramana usor de consumat din Java.
- Tipurile Kotlin expuse public trebuie verificate explicit pentru compatibilitate Java.
- JAR-urile pluginurilor Paper trebuie sa includa runtime-ul Kotlin daca au clase Kotlin.
- Fiecare faza se inchide cu `.\gradlew.bat clean build`.
- Daca o conversie mare produce multe erori, se revine la un slice mai mic.

## Starea curenta

Structura actuala Gradle:

- `ainpc-api`
- `ainpc-core-plugin`
- `ainpc-scenario-medieval`

Build-ul foloseste:

- Java release `21`
- Gradle Wrapper
- `gradle.properties` pentru parametrii de versiune
- JAR shaded pentru `ainpc-core-plugin`
- `plugin.yml` filtrat prin Gradle

## Faza 0: Baseline si reguli de siguranta

Obiectiv:

- sa existe un punct stabil inainte de introducerea Kotlin

De facut:

1. ruleaza `.\gradlew.bat clean build`
2. noteaza JAR-urile generate in `build/libs`
3. verifica `plugin.yml` din JAR-uri pentru `version: 1.0.0`
4. confirma ca testele existente trec
5. alege conventia de conversie: cate un package sau cate o clasa mica per schimbare

Gate de iesire:

- build complet verde
- niciun fisier Java convertit inca
- planul de conversie este acceptat

## Faza 1: Activare Kotlin in Gradle fara conversie de cod

Obiectiv:

- proiectul sa poata compila fisiere `.kt`, dar codul existent sa ramana Java

De facut:

1. adauga `kotlinVersion` in `gradle.properties`
2. adauga pluginul Kotlin JVM in build-ul Gradle
3. aplica pluginul Kotlin doar pe modulele care vor contine Kotlin
4. configureaza Kotlin cu target JVM compatibil cu Java 21
5. adauga dependinta Kotlin standard library unde este nevoie
6. ruleaza `.\gradlew.bat clean build`

Recomandare initiala:

- activeaza Kotlin intai in `ainpc-core-plugin`
- activeaza Kotlin in `ainpc-scenario-medieval` doar cand exista prima clasa Kotlin acolo
- amana `ainpc-api` pana cand regulile de compatibilitate Java sunt clare

Exemplu orientativ pentru Gradle:

```groovy
plugins {
    id 'org.jetbrains.kotlin.jvm' version "${kotlinVersion}" apply false
}

subprojects {
    plugins.withId('org.jetbrains.kotlin.jvm') {
        dependencies {
            implementation "org.jetbrains.kotlin:kotlin-stdlib:${rootProject.ext.kotlinVersion}"
        }
    }
}
```

Observatie:

- configuratia finala trebuie adaptata la forma reala a `build.gradle`
- versiunea Kotlin se alege in momentul implementarii si se tine in `gradle.properties`

Gate de iesire:

- proiectul compileaza cu pluginul Kotlin activ
- nu exista inca conversie functionala
- JAR-ul core continua sa includa dependintele runtime necesare

## Faza 2: Conversie teste si utilitare fara risc

Obiectiv:

- validarea toolchain-ului Kotlin pe cod cu risc mic

Tintele recomandate:

- teste unitare simple
- utilitare pure fara Bukkit lifecycle
- value objects interne fara persistenta directa
- clase mici care nu sunt API public

Exemple potrivite:

- teste din `src/test/java`
- helperi mici din `utils`
- parsere simple fara efecte laterale

De evitat in aceasta faza:

- `AINPCPlugin`
- `AINPCCommand`
- `ScenarioEngine`
- `FeaturePackLoader`
- `NPCManager`
- orice clasa folosita direct de Paper ca entrypoint

Reguli:

- converteste o clasa sau un grup foarte mic odata
- pastreaza numele pachetelor
- nu schimba semantica metodelor
- foloseste `@JvmStatic`, `@JvmOverloads` sau `@JvmField` doar cand exista consumatori Java reali

Gate de iesire:

- `.\gradlew.bat clean build` trece
- testele convertite ruleaza
- nu apar schimbari de comportament

## Faza 3: Modele interne si DTO-uri non-publice

Obiectiv:

- folosirea Kotlin pentru modele interne unde reduce codul boilerplate

Tintele recomandate:

- clase model interne immutable
- snapshot-uri interne
- rezultate de validare
- optiuni de planner
- obiecte simple cu campuri finale

Avantaje Kotlin aici:

- `data class`
- `sealed class` sau `sealed interface` pentru rezultate controlate
- null-safety explicita
- constructori mai clari

Atentie:

- nu transforma automat orice POJO in `data class`
- verifica equals/hashCode daca obiectul este folosit in colectii
- verifica serializarea YAML/JSON daca exista constructori sau campuri asteptate de librarii Java
- evita `Pair` si `Triple` in API-uri; foloseste tipuri cu nume

Gate de iesire:

- build complet verde
- testele pentru parsing, mapping, quest si progres trec
- clasele Java existente pot consuma noile tipuri Kotlin fara wrapper-e ciudate

## Faza 4: Servicii mici si pipeline-uri deterministe

Obiectiv:

- convertirea serviciilor cu responsabilitate clara, fara lifecycle Paper direct

Tintele recomandate:

- validatori
- selectori
- hasher-e
- parsere
- servicii read-only
- repository-uri mici doar dupa ce testele acopera schema folosita

Exemple de zone potrivite:

- validatoare de feature pack
- selectie de template-uri
- intent parsers
- planificare read-only
- logic de progres fara Bukkit runtime direct

Reguli:

- fiecare conversie trebuie sa aiba teste
- nu converti simultan modelul, parserul si persistenta aceluiasi flux
- pastreaza interop Java clara pentru clasele ramase Java
- evita extensii Kotlin care ascund prea mult flow-ul pentru restul echipei

Gate de iesire:

- `.\gradlew.bat clean build`
- testele relevante pentru serviciul convertit trec
- codul Java ramas nu devine mai greu de citit din cauza interop-ului

## Faza 5: API public si compatibilitate addonuri

Obiectiv:

- decizie explicita daca `ainpc-api` ramane Java sau accepta Kotlin

Recomandare conservatoare:

- pastreaza `ainpc-api` in Java cat timp API-ul este consumat de addonuri externe
- foloseste Kotlin in implementarea core, nu in contractele publice
- daca introduci Kotlin in API, documenteaza impactul pentru pluginuri Java

Riscuri in API:

- parametri nullable neclari pentru Java
- `object`, `companion object` si top-level functions expuse cu nume JVM mai putin evidente
- default arguments care nu sunt naturale din Java fara `@JvmOverloads`
- colectii Kotlin expuse catre Java ca tipuri mai putin clare
- sealed types greu de consumat de addonuri Java simple

Regula:

- orice tip Kotlin expus din `ainpc-api` trebuie verificat printr-un test Java care il consuma.

Gate de iesire:

- addonul `ainpc-scenario-medieval` compileaza fara dependinte interne
- exista cel putin un test Java pentru contractele publice convertite
- documentatia API este actualizata

## Faza 6: Entry points Paper si clase mari

Obiectiv:

- convertirea claselor importante doar dupa ce au fost subtiate

De evitat pana la refactorizare:

- `AINPCPlugin`
- `AINPCCommand`
- `AINPCTabCompleter`
- `ScenarioEngine`
- `FeaturePackLoader`
- `NPCManager`
- `OpenAIService`

Ordine recomandata:

1. sparge clasa mare in servicii Java mici
2. acopera serviciile cu teste
3. converteste serviciile mici la Kotlin
4. lasa entrypoint-ul Java pana cand nu mai contine logica complexa
5. converteste entrypoint-ul doar daca exista un avantaj real

Atentie Paper:

- `plugin.yml` trebuie sa indice clasa principala corecta
- clasele entrypoint trebuie sa fie instantiabile de Paper
- nu folosi constructor injection in clasa principala Paper daca Paper asteapta constructor fara argumente
- verifica manual pornirea pe server Paper dupa orice schimbare la entrypoint

Gate de iesire:

- build local verde
- smoke test pe server Paper
- comenzile de baza `/ainpc` functioneaza
- addonul medieval se incarca dupa core

## Faza 7: Curatenie si standarde Kotlin

Obiectiv:

- stabilirea unui stil Kotlin consistent in proiect

Reguli recomandate:

- fisierele Kotlin folosesc aceleasi package-uri ca Java
- clasele mici si pure pot folosi expresii concise
- serviciile cu flow complex raman explicite
- nu se foloseste reflection Kotlin fara nevoie clara
- nu se introduc coroutine in runtime Paper fara un plan separat de scheduling
- nu se folosesc DSL-uri interne inainte ca modelul de domeniu sa fie stabil

De adaugat gradual:

- conventii pentru nullability
- reguli pentru `data class`
- reguli pentru `sealed`
- reguli pentru interoperabilitate Java
- reguli pentru logging si exceptions

Gate de iesire:

- documentatia API si runbook-urile sunt actualizate
- build-ul si smoke testele folosesc Gradle
- nu mai exista conversii partiale care lasa clase duble Java/Kotlin pentru acelasi concept

## Faza 8: Optimizare JAR si runtime Kotlin

Obiectiv:

- verificarea dimensiunii JAR-urilor si a dependintelor incluse dupa introducerea Kotlin

De verificat:

- `ainpc-core-plugin/build/libs/ainpc-core-plugin-1.0.0.jar`
- `ainpc-scenario-medieval/build/libs/ainpc-scenario-medieval-1.0.0.jar`
- includerea `kotlin-stdlib` unde exista clase Kotlin
- lipsa duplicarii inutile intre pluginuri
- compatibilitatea cu serverul Paper tinta

Decizie importanta:

- daca un modul Paper separat contine Kotlin, acel modul trebuie sa aiba acces la runtime-ul Kotlin la incarcare
- nu presupune ca runtime-ul Kotlin shaded in core este automat disponibil pentru addon, decat daca testul pe server confirma asta

Gate de iesire:

- JAR-urile pornesc pe server Paper curat
- nu exista `ClassNotFoundException` pentru clase Kotlin
- marimea JAR-ului este acceptabila pentru release

## Ordine practica recomandata

Ordinea cu risc mic pentru acest proiect:

1. activeaza Kotlin in Gradle fara conversii
2. converteste cateva teste simple
3. converteste utilitare pure
4. converteste modele interne immutable
5. converteste validatoare si selectori mici
6. pastreaza `ainpc-api` Java pana cand exista motiv clar sa il schimbi
7. converteste clase mari doar dupa refactorizare
8. ruleaza smoke test Paper dupa orice schimbare in entrypoint, plugin.yml sau packaging

## Definitia de gata

Conversia Java -> Kotlin poate fi considerata stabila doar cand:

- build-ul Gradle trece cu `clean build`
- JAR-urile se genereaza corect in `build/libs`
- pluginul core porneste pe Paper
- addonul medieval se incarca dupa core
- clasele Java ramase pot consuma clasele Kotlin fara adaptoare inutile
- API-ul public ramane documentat si compatibil pentru addonuri Java
- runtime-ul Kotlin este disponibil in JAR-urile care au nevoie de el

## Anti-patterns

- conversie masiva doar pentru schimbarea limbajului
- `!!` folosit ca solutie generala pentru nullability
- `lateinit` pentru dependinte care pot fi constructor parameters in servicii normale
- expunerea de `MutableList` si `MutableMap` fara nevoie clara
- top-level functions publice in API fara nume JVM controlat
- coroutine introduse peste schedulerul Paper fara design separat
- conversie a `AINPCPlugin` inainte de subtierea bootstrap-ului
- conversie a `ainpc-api` fara test Java de consum

## Comenzi utile

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

Smoke script dupa build:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-quests.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -PlayerName "NumeleTau"
```
