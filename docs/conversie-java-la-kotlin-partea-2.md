# Conversie Java la Kotlin - Partea 2

Actualizat: 2026-05-17

## Scop

Acest document continua `conversie-java-la-kotlin.md` cu un runbook operational. Partea 1 descrie strategia si fazele mari. Partea 2 descrie ordinea concreta de lucru, criteriile de oprire, riscurile de interop si lista de slice-uri recomandate pentru acest repo.

Obiectivul nu este sa transformi proiectul in Kotlin cat mai repede. Obiectivul este sa introduci Kotlin fara sa rupi pluginul Paper, addonul medieval, testele si compatibilitatea pentru cod Java.

Continuare cu retete concrete de implementare: `conversie-java-la-kotlin-partea-3.md`.

## Decizia de baza

Directia recomandata:

- `ainpc-core-plugin`: poate primi Kotlin primul
- `ainpc-scenario-medieval`: este deja migrat la Kotlin pe `src/main`; continua validarea prin teste/build
- `ainpc-api`: este majoritar Kotlin; interfetele API publice critice raman Java pentru interop stabil

Motiv:

- core-ul este implementare interna si poate beneficia de Kotlin fara sa forteze addonurile externe
- addonul medieval trebuie sa ramana un test de consumator
- API-ul public trebuie sa ramana simplu pentru autori de pluginuri Java

## Faza A: Pregatire build Kotlin

Scop:

- introducerea pluginului Kotlin fara conversie de cod

Modificari asteptate:

- `gradle.properties`
- `build.gradle`
- eventual `ainpc-core-plugin/build.gradle`

Parametru nou:

```properties
kotlinVersion=<versiune aleasa la implementare>
```

Reguli:

- versiunea Kotlin se tine in `gradle.properties`
- pluginul se declara o singura data la root
- se aplica initial doar in `ainpc-core-plugin`
- `ainpc-api` nu primeste Kotlin in primul pas

Validare:

```powershell
.\gradlew.bat clean build
```

Gate:

- build verde
- nu exista inca fisiere `.kt`
- JAR-ul core se genereaza

Rollback:

- se scoate pluginul Kotlin
- se scoate `kotlinVersion`
- nu trebuie schimbate surse Java

## Faza B: Smoke Kotlin minim

Scop:

- confirmarea ca toolchain-ul compileaza Kotlin in proiect

Varianta recomandata:

- adauga un test Kotlin foarte mic in `ainpc-core-plugin/src/test/kotlin`
- testul nu atinge Bukkit, DB sau Paper runtime
- testul poate valida o functie simpla sau o clasa de test interna

Reguli:

- nu adauga cod de productie Kotlin inca
- nu schimba pachetele existente
- nu modifica `plugin.yml`

Validare:

```powershell
.\gradlew.bat clean test
```

Gate:

- testul Kotlin ruleaza
- testele Java existente continua sa ruleze
- raportul de test Gradle este produs normal

Rollback:

- sterge testul Kotlin
- daca build-ul ramane instabil, revino la Faza A

## Faza C: Conversie teste existente

Scop:

- mutarea primelor fisiere reale de la Java la Kotlin cu risc minim

Candidati buni:

- teste pentru parsere simple
- teste pentru value objects
- teste care nu folosesc mock-uri complicate
- teste fara lifecycle Paper

Candidati de evitat:

- teste care pornesc servicii cu DB real
- teste care folosesc clase Bukkit greu de instantiat
- teste care verifica reflection peste semnaturi Java
- teste pentru comenzi mari, daca semnaturile sunt in schimbare

Ordine recomandata:

1. un test simplu din `utils`
2. un test simplu din `progression`
3. un test simplu din `world/mapping`
4. un test pentru validator sau selector

Reguli:

- converteste un fisier, ruleaza testul lui
- apoi ruleaza testele modulului
- abia dupa aceea treci la urmatorul fisier

Comenzi:

```powershell
.\gradlew.bat :ainpc-core-plugin:test
.\gradlew.bat clean build
```

Gate:

- testele convertite sunt lizibile
- nu apar schimbari de semantica
- nu apar helperi Kotlin globali inutili

## Faza D: Utilitare pure

Scop:

- prima conversie de cod de productie cu risc redus

Candidati buni:

- clase mici din `utils`
- hasher-e
- parsere de intentii
- obiecte helper fara stare globala

Candidati de evitat:

- servicii care tin referinte la plugin
- servicii care scriu DB
- clase care se inregistreaza in Paper
- clase cu multe apeluri din Java si semnaturi instabile

Reguli de conversie:

- pastreaza numele clasei daca este consumata din Java
- daca transformi metode statice Java in Kotlin, controleaza numele JVM
- evita top-level functions pentru cod consumat din Java
- prefera `object` doar pentru singleton-uri reale

Validare:

```powershell
.\gradlew.bat clean compileJava compileKotlin compileTestJava compileTestKotlin
.\gradlew.bat clean build
```

Gate:

- codul Java ramas compileaza fara adaptoare suplimentare
- testele care acopera utilitarul trec

## Faza E: Modele interne

Scop:

- reducerea boilerplate-ului pentru date interne

Candidati buni:

- snapshot-uri interne
- rezultate de validare
- optiuni immutable
- raspunsuri de planner
- structuri de date folosite doar in core

Reguli:

- foloseste `data class` numai cand equals/hashCode/toString sunt dorite
- evita `var` daca obiectul nu trebuie modificat dupa creare
- foloseste liste read-only in semnaturi interne
- nu expune colectii mutable fara motiv
- pentru Java callers, verifica numele getterelor generate

Risc principal:

- un `data class` schimba automat equals/hashCode fata de o clasa Java existenta care poate compara prin identitate

Validare:

```powershell
.\gradlew.bat :ainpc-core-plugin:test
```

Gate:

- teste pentru equals/hashCode unde conteaza
- parsing-ul YAML/JSON nu este afectat
- codul Java poate construi obiectele fara ambiguitate

## Faza F: Validatoare si selectori

Scop:

- convertirea logicii deterministe, unde Kotlin ajuta prin expresivitate si null-safety

Candidati buni:

- validatoare de feature pack
- validatoare de dependinte
- selectori de template-uri
- intent resolvers
- filtre de progres

Reguli:

- pastreaza erorile si mesajele identice
- nu combina conversia cu schimbarea algoritmului
- nu introduce DSL-uri Kotlin inca
- nu ascunde ramurile importante in chain-uri prea lungi

Validare:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*ValidatorTest"
.\gradlew.bat :ainpc-core-plugin:test --tests "*SelectorTest"
.\gradlew.bat clean build
```

Gate:

- mesajele de validare raman stabile
- testele de regresie trec
- codul este mai simplu, nu doar mai scurt

## Faza G: Repository-uri si persistenta

Scop:

- convertirea componentelor de persistenta numai dupa ce modelele si testele sunt stabile

Regula principala:

- persistenta nu este un loc bun pentru prima conversie Kotlin

Candidati acceptabili:

- repository-uri mici cu teste DB izolate
- mapper-e intre `ResultSet` si model intern
- query builders simpli

De evitat:

- `DatabaseManager` pana cand este spart in componente mai mici
- migrari DB in acelasi timp cu conversia limbajului
- schimbari de schema
- schimbari de tranzactionalitate

Validare:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*RepositoryTest"
.\gradlew.bat :ainpc-core-plugin:test --tests "*Persistence*"
.\gradlew.bat clean build
```

Gate:

- testele DB trec repetabil
- nu apar fisiere temporare lasate in repo
- SQL-ul generat sau executat ramane echivalent

## Faza H: Servicii Bukkit/Paper

Scop:

- conversia serviciilor care folosesc API Paper, dar nu sunt entrypoint-uri

Candidati acceptabili:

- servicii care primesc dependinte deja construite
- listener helpers fara inregistrare directa
- servicii de render GUI daca au teste suficiente

De evitat:

- clasa principala Paper
- listener-ele principale daca nu au teste sau smoke test clar
- comenzi mari cu multe ramuri

Reguli:

- verifica importurile Bukkit generate de Kotlin
- nu folosi `lateinit` pentru campuri care pot fi constructor parameters in servicii normale
- nu schimba threading-ul
- nu introduce coroutine peste schedulerul Paper

Validare:

```powershell
.\gradlew.bat clean build
.\gradlew.bat assemble
```

Gate:

- JAR-ul se genereaza
- smoke test Paper trece pentru zona atinsa

## Faza I: Clase mari

Scop:

- evitarea conversiilor masive greu de verificat

Clase care trebuie sparte inainte de conversie:

- `AINPCCommand`
- `ScenarioEngine`
- `FeaturePackLoader`
- `NPCManager`
- `OpenAIService`
- `AINPCPlugin`

Protocol:

1. extrage o responsabilitate mica in Java
2. adauga sau confirma teste
3. converteste clasa extrasa la Kotlin
4. lasa clasa mare Java ca orchestrator
5. repeta pana clasa mare devine subtire

Exemplu de slice:

- extrage validarea de argumente din comanda intr-o clasa mica Java
- testeaza clasa noua
- converteste clasa noua la Kotlin
- nu converti `AINPCCommand` in aceeasi schimbare

Gate:

- diff-ul este usor de inspectat
- comportamentul public ramane acelasi
- nu apar schimbari mari in `plugin.yml`

## Faza J: API public

Scop:

- protejarea addonurilor si integratorilor Java

Regula recomandata:

- `ainpc-api` ramane Java cel putin pana dupa stabilizarea scenariilor si addonurilor

Daca totusi se converteste o clasa din API:

- scrie un test Java care o consuma
- verifica constructorii vazuti din Java
- verifica nullability
- evita default parameters fara `@JvmOverloads`
- evita `internal`, pentru ca nu este o granita reala pentru Java
- evita top-level API public

Validare:

```powershell
.\gradlew.bat :ainpc-api:build
.\gradlew.bat :ainpc-scenario-medieval:build
.\gradlew.bat clean build
```

Gate:

- addonul medieval compileaza
- testul Java de consum trece
- documentatia API este actualizata

## Matrice de risc

| Zona | Risc | Cand se converteste |
|---|---|---|
| Teste simple | Mic | imediat dupa activarea Kotlin |
| Utilitare pure | Mic | dupa test smoke Kotlin |
| Modele interne | Mediu | dupa ce exista teste pentru constructori si equality |
| Validatoare | Mediu | dupa ce mesajele de eroare sunt acoperite |
| Repository-uri | Mare | dupa teste DB stabile |
| Servicii Paper | Mare | dupa smoke test local |
| Entry points Paper | Foarte mare | doar dupa subtiere |
| `ainpc-api` | Foarte mare | doar cu test Java de consum |

## Checklist pentru fiecare conversie

Inainte:

- clasa are teste sau comportament usor de verificat
- nu este entrypoint Paper
- nu este API public, sau exista test Java de consum
- conversia poate fi izolata intr-un diff mic

In timpul conversiei:

- pastreaza package-ul
- pastreaza numele clasei daca este consumata din Java
- evita schimbarea mesajelor de eroare
- evita schimbarea tipurilor publice
- evita `!!`

Dupa:

- ruleaza testul specific
- ruleaza testele modulului
- ruleaza `clean build` pentru fazele importante
- inspecteaza JAR-ul daca modulul este plugin Paper

## Comenzi de verificare

Test modul core:

```powershell
.\gradlew.bat :ainpc-core-plugin:test
```

Test addon medieval:

```powershell
.\gradlew.bat :ainpc-scenario-medieval:test
```

Compilare toate modulele:

```powershell
.\gradlew.bat clean compileJava compileKotlin compileTestJava compileTestKotlin
```

Build complet:

```powershell
.\gradlew.bat clean build
```

JAR-uri:

```powershell
.\gradlew.bat assemble
Get-ChildItem .\ainpc-core-plugin\build\libs\*.jar
Get-ChildItem .\ainpc-scenario-medieval\build\libs\*.jar
```

Smoke Paper mapping:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-mapping.ps1 `
  -ServerDir "C:\Minecraft\paper-test"
```

Smoke Paper quest:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-quests.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -PlayerName "NumeleTau"
```

## Semnale ca trebuie oprit slice-ul

Opreste conversia curenta daca:

- trebuie sa modifici multe clase neatinse doar pentru interop
- apar schimbari in schema DB
- apar schimbari in `plugin.yml`
- testele incep sa depinda de ordinea de rulare
- ai nevoie de `!!` in multe locuri
- trebuie sa introduci coroutine pentru a termina conversia
- clasa convertita devine mai greu de citit decat versiunea Java

Actiune:

- revino la o conversie mai mica
- extrage intai codul in Java
- adauga teste
- converteste doar componenta extrasa

## Standard minim Kotlin pentru proiect

Preferat:

- constructor injection pentru servicii normale
- `val` implicit, `var` doar cand starea chiar se modifica
- `data class` pentru modele immutable simple
- `sealed interface` pentru rezultate inchise intern
- `when` exhaustiv pentru tipuri inchise
- functii mici cu nume clare

Evitat:

- `!!`
- `lateinit` in afara cazurilor de lifecycle clar
- top-level mutable state
- extensii globale pentru concepte de business instabile
- DSL-uri interne premature
- coroutines fara document separat
- expunerea Kotlin collections mutable in API public

## Ordine recomandata de slice-uri

Lista de pornire:

1. activeaza Kotlin in `ainpc-core-plugin`
2. adauga un test Kotlin minim
3. converteste 1 test simplu din `utils`
4. converteste 1 test simplu din `progression`
5. converteste 1 utilitar pur
6. converteste 1 validator mic
7. converteste 1 model intern immutable
8. converteste un selector sau resolver fara DB
9. ruleaza `clean build`
10. ruleaza `assemble` si inspecteaza JAR-ul

Abia dupa aceste slice-uri:

- se poate decide daca merita Kotlin in `ainpc-scenario-medieval`
- se poate decide daca un pachet intreg poate fi convertit
- se poate evalua daca API-ul ramane Java pe termen lung

## Definitia de final pentru Partea 2

Partea 2 este indeplinita cand:

- exista Kotlin activ in build fara regresii
- exista cel putin un test Kotlin real
- primele utilitare sau validatoare convertite au teste
- JAR-ul core include runtime-ul necesar
- smoke testul Paper nu raporteaza erori de incarcare
- `ainpc-api` ramane compatibil cu addonul medieval

## Legatura cu Partea 1

Foloseste `conversie-java-la-kotlin.md` pentru strategia generala si fazele mari.

Foloseste acest document pentru executia zilnica: ce convertesti primul, ce verifici si cand opresti conversia.
