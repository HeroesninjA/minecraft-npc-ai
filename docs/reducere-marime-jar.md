# Reducere Marime JAR

Actualizat: 2026-04-28

## Scop

Acest document descrie cum poate fi refactorizat proiectul ca JAR-ul final al `ainpc-core-plugin` sa fie mai mic.

Concluzia importanta este ca marimea JAR-ului nu vine din codul AINPC, ci din dependintele impachetate prin `maven-shade-plugin`.

## Starea actuala

Comanda de referinta:

```powershell
mvn package -DskipTests
```

Dimensiune observata pentru core:

- `ainpc-core-plugin-1.0.0.jar`: aproximativ `16.6 MB`
- `ainpc-core-plugin-1.0.0-shaded.jar`: aproximativ `16.6 MB`

Cele mai mari zone din JAR-ul core:

- `org/sqlite`: aproximativ `12.83 MB` comprimat
- `kotlin`: aproximativ `1.59 MB` comprimat
- `okhttp3`: aproximativ `0.70 MB` comprimat
- `okio`: aproximativ `0.34 MB` comprimat
- `ro/ainpc`: aproximativ `0.35 MB` comprimat
- `com/google`: aproximativ `0.24 MB` comprimat

Interpretare:

- codul propriu AINPC este sub 1 MB in JAR
- resursele YAML sunt foarte mici
- `sqlite-jdbc` este cauza principala a dimensiunii
- OkHttp trage dupa el Okio si Kotlin stdlib

## Dependinte care umfla JAR-ul

Dependinte relevante din `ainpc-core-plugin`:

```text
org.xerial:sqlite-jdbc:3.45.1.0
com.squareup.okhttp3:okhttp:4.12.0
com.squareup.okio:okio
org.jetbrains.kotlin:kotlin-stdlib
com.google.code.gson:gson:2.11.0
ro.ainpc:ainpc-api:1.0.0
```

Dimensiuni aproximative ale artifactelor locale:

- `sqlite-jdbc-3.45.1.0.jar`: `12.88 MB`
- `kotlin-stdlib-1.8.21.jar`: `1.59 MB`
- `okhttp-4.12.0.jar`: `0.75 MB`
- `okio-jvm-3.6.0.jar`: `0.34 MB`
- `gson-2.11.0.jar`: `0.28 MB`

## Principiu de refactorizare

Pentru reducerea reala a JAR-ului, nu trebuie inceput cu clasele mari precum `ScenarioEngine` sau `NPCManager`.

Aceste clase sunt importante pentru mentenanta, dar nu sunt cauza principala a dimensiunii.

Ordinea corecta este:

1. elimina dependintele mari din shaded JAR
2. inlocuieste librariile externe cu API-uri JDK unde este rezonabil
3. muta dependintele grele in module optionale sau in runtime loader
4. abia apoi optimizeaza codul propriu

## Faza 0: Stabilirea unei masuratori standard

Inainte de orice schimbare, trebuie sa existe o masuratoare repetabila.

Comenzi:

```powershell
mvn clean package -DskipTests
Get-ChildItem ainpc-core-plugin\target\*.jar | Select-Object Name,Length
mvn -pl ainpc-core-plugin dependency:tree
```

Recomandare:

- documenteaza dimensiunea JAR-ului dupa fiecare schimbare
- nu evalua optimizarea doar dupa build incremental
- foloseste mereu `mvn clean package -DskipTests` pentru comparatii

## Faza 1: Scoate dependintele inutile din shade

In prezent, `maven-shade-plugin` impacheteaza dependintele compile-scope.

Trebuie verificat daca intra in JAR dependinte care sunt deja oferite de Paper sau nu sunt necesare runtime:

- `org.slf4j:slf4j-api`
- `org.jetbrains:annotations`
- `com.google.errorprone:error_prone_annotations`
- fisiere `META-INF/*.SF`, `META-INF/*.DSA`, `META-INF/*.RSA`
- `META-INF/versions/9/module-info.class`

Impact estimat:

- mic, probabil sub `0.2 MB`

Motiv:

- acestea nu sunt cauza principala, dar curata JAR-ul si reduc warning-urile shade

Directie de configurare:

```xml
<configuration>
    <createDependencyReducedPom>false</createDependencyReducedPom>
    <minimizeJar>false</minimizeJar>
    <filters>
        <filter>
            <artifact>*:*</artifact>
            <excludes>
                <exclude>META-INF/*.SF</exclude>
                <exclude>META-INF/*.DSA</exclude>
                <exclude>META-INF/*.RSA</exclude>
                <exclude>META-INF/versions/9/module-info.class</exclude>
            </excludes>
        </filter>
    </filters>
</configuration>
```

Atentie:

- nu activa direct `minimizeJar` fara teste runtime
- minimizarea poate rupe cod folosit prin reflection, `ServiceLoader` sau drivere JDBC

## Faza 2: Inlocuieste OkHttp cu `java.net.http.HttpClient`

Proiectul foloseste Java 21, deci are deja client HTTP modern in JDK:

- `java.net.http.HttpClient`
- `java.net.http.HttpRequest`
- `java.net.http.HttpResponse`

Dependinta care poate fi eliminata:

- `com.squareup.okhttp3:okhttp`

Dependinte eliminate indirect:

- `com.squareup.okio:okio`
- `com.squareup.okio:okio-jvm`
- `org.jetbrains.kotlin:kotlin-stdlib`
- `org.jetbrains.kotlin:kotlin-stdlib-common`
- `org.jetbrains.kotlin:kotlin-stdlib-jdk7`
- `org.jetbrains.kotlin:kotlin-stdlib-jdk8`

Impact estimat:

- aproximativ `2.6 MB` mai putin in JAR

Clase afectate:

- `ainpc-core-plugin/src/main/java/ro/ainpc/ai/OpenAIService.java`

Directie de refactorizare:

1. inlocuieste `OkHttpClient` cu `HttpClient`
2. construieste request-ul cu `HttpRequest.newBuilder()`
3. trimite JSON-ul cu `BodyPublishers.ofString(...)`
4. citeste raspunsul cu `BodyHandlers.ofString()`
5. pastreaza aceeasi logica pentru:
   - timeout
   - fallback local
   - status HTTP non-2xx
   - diagnostic logging

Exemplu conceptual:

```java
HttpClient client = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
    .build();

HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/responses"))
    .timeout(Duration.ofSeconds(readTimeoutSeconds))
    .header("Content-Type", "application/json")
    .header("Authorization", "Bearer " + apiKey)
    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
```

Verificari obligatorii dupa schimbare:

```powershell
mvn clean test
mvn package -DskipTests
```

Teste runtime recomandate:

- OpenAI indisponibil -> fallback local
- API key lipsa -> fallback local si mesaj clar
- HTTP 401/403 -> eroare diagnosticata corect
- timeout -> backoff activat
- raspuns valid -> dialog generat

## Faza 3: Externalizeaza `sqlite-jdbc`

Aceasta este optimizarea cu cel mai mare impact.

Problema:

- `sqlite-jdbc` are aproximativ `12.88 MB`
- contine drivere native pentru mai multe platforme
- este impachetat in JAR-ul pluginului core

Optiuni de refactorizare:

### Optiunea A: Paper library loader

Daca versiunea de Paper folosita pe server suporta incarcarea de librarii Maven din `plugin.yml`, `sqlite-jdbc` poate fi scos din shaded JAR si declarat ca librarie runtime.

Directie:

- marcheaza `sqlite-jdbc` ca `provided` sau scoate-l din shade
- declara coordonata Maven in `plugin.yml`, daca infrastructura serverului permite asta

Impact estimat:

- JAR-ul pluginului scade cu aproximativ `12.8 MB`
- serverul descarca sau incarca libraria separat

Avantaj:

- cel mai simplu mod de a reduce JAR-ul fara sa rescrii persistenta

Risc:

- depinde de suportul exact al serverului pentru library loading
- trebuie testat pe serverul Paper tinta, nu doar cu Maven

### Optiunea B: modul separat `ainpc-storage-sqlite`

Se creeaza un modul/plugin separat pentru persistenta SQLite.

Structura:

```text
ainpc-api
ainpc-core-plugin
ainpc-storage-sqlite
ainpc-scenario-medieval
```

Core-ul defineste in API:

- `StorageProvider`
- `NpcRepository`
- `MemoryRepository`
- `QuestProgressRepository`

`ainpc-storage-sqlite` implementeaza contractele si depinde de `sqlite-jdbc`.

Impact estimat:

- `ainpc-core-plugin` devine mult mai mic
- `ainpc-storage-sqlite` ramane JAR-ul greu
- serverul instaleaza storage-ul doar daca are nevoie de SQLite

Avantaj:

- arhitectura devine mai modulara
- se pot adauga ulterior storage-uri alternative

Risc:

- refactorizare mai mare
- necesita contracte stabile pentru persistenta

### Optiunea C: storage local fara JDBC pentru modul minim

Pentru un core foarte mic, se poate introduce un storage simplificat:

- YAML
- JSON
- fisiere per NPC

Impact:

- elimina `sqlite-jdbc` din core
- reduce mult dimensiunea JAR-ului

Risc:

- poate fi mai slab pentru volum mare de date
- necesita migrari de date daca exista deja servere active

Recomandare:

- foloseste aceasta optiune doar pentru mod de dezvoltare sau servere mici
- pentru productie, prefera storage modular sau SQLite externalizat

## Faza 4: Nu shadui `ainpc-api` daca nu este necesar

`ainpc-api` este mic, deci impactul pe dimensiune este redus.

Totusi, pe termen lung, API-ul poate ramane separat daca:

- addonurile il au ca dependinta `provided`
- core-ul exporta API-ul prin services
- serverul incarca `ainpc-api` ca librarie comuna sau core-ul ramane singura sursa pentru clasele API

Impact estimat:

- mic, sub `0.1 MB`

Recomandare:

- nu prioritiza aceasta schimbare pentru dimensiune
- prioritizeaz-o doar pentru claritate modulara

## Faza 5: Activeaza `minimizeJar` doar dupa teste

`minimizeJar` poate reduce dimensiunea prin eliminarea claselor aparent nefolosite.

Problema:

- poate elimina clase folosite prin reflection
- poate afecta drivere JDBC
- poate afecta JSON/parsing daca anumite clase sunt descoperite dinamic

Directie sigura:

1. intai elimina OkHttp/Kotlin
2. apoi externalizeaza SQLite
3. abia dupa aceea testeaza `minimizeJar`

Configuratie experimentala:

```xml
<minimizeJar>true</minimizeJar>
```

Verificari obligatorii:

- pluginul porneste pe Paper
- baza de date initializeaza corect
- OpenAI fallback functioneaza
- dialogul AI functioneaza
- pack-urile YAML se incarca
- addonul medieval se conecteaza la core

Recomandare:

- daca apar erori greu de explicat dupa `minimizeJar`, dezactiveaza-l
- castigul dupa eliminarea SQLite si OkHttp va fi probabil mic

## Faza 6: Seteaza un buget de dimensiune

Pentru a preveni cresterea accidentala a JAR-ului, stabileste un buget.

Bugete recomandate:

- core cu SQLite shaded: maxim `18 MB`
- core fara OkHttp/Kotlin, cu SQLite shaded: maxim `15 MB`
- core fara SQLite shaded si fara OkHttp/Kotlin: sub `3 MB`
- addon de storage SQLite separat: poate avea `13-15 MB`

Verificare simpla in CI sau local:

```powershell
$jar = Get-Item "ainpc-core-plugin\target\ainpc-core-plugin-1.0.0.jar"
if ($jar.Length -gt 3MB) {
    throw "JAR-ul core depaseste bugetul de 3 MB: $([math]::Round($jar.Length / 1MB, 2)) MB"
}
```

Foloseste bugetul de `3 MB` doar dupa ce SQLite este externalizat.

## Ordine recomandata de implementare

1. masoara dimensiunea curenta si salveaza baseline-ul
2. inlocuieste OkHttp cu `java.net.http.HttpClient`
3. elimina dependinta `okhttp` din `ainpc-core-plugin/pom.xml`
4. ruleaza `mvn clean test` si `mvn package -DskipTests`
5. externalizeaza `sqlite-jdbc` prin Paper library loader sau modul separat
6. ruleaza test pe server Paper real
7. abia apoi testeaza `minimizeJar`
8. adauga un buget de dimensiune pentru JAR

## Estimare rezultat

Pornind de la aproximativ `16.6 MB`:

- dupa eliminarea OkHttp/Kotlin: aproximativ `14 MB`
- dupa externalizarea SQLite: aproximativ `1.5-3 MB`
- dupa filtre/minimizare: posibil sub `2 MB`, in functie de strategia aleasa

Cel mai mare castig vine din:

- scoaterea `sqlite-jdbc` din shaded JAR
- inlocuirea OkHttp cu clientul HTTP din JDK

## Definitia de gata

Refactorizarea pentru reducerea JAR-ului este considerata terminata cand:

- `mvn clean test` trece
- `mvn package -DskipTests` trece
- pluginul porneste pe server Paper real
- `ainpc-core-plugin` nu mai shaduieste `sqlite-jdbc`, daca tinta este un core mic
- `ainpc-core-plugin` nu mai depinde de OkHttp/Kotlin, daca se accepta `HttpClient`
- dimensiunea JAR-ului core este masurata si documentata
- exista o limita de dimensiune pentru a preveni regresii

## Recomandare finala

Pentru acest proiect, cea mai buna strategie este:

- pasul 1: inlocuieste OkHttp cu `java.net.http.HttpClient`
- pasul 2: muta SQLite in runtime library sau intr-un modul separat de storage
- pasul 3: pastreaza core-ul mic si lasa addonurile/storage-ul sa aduca dependinte grele doar cand sunt necesare

Refactorizarea claselor mari ramane importanta pentru mentenanta, dar nu va reduce semnificativ JAR-ul. Dimensiunea actuala este o problema de dependinte, nu de cod propriu.
