# Conversie Java la Kotlin - Partea 5

Actualizat: 2026-05-16

## Scop

Acest document continua seria Java -> Kotlin cu partea de control al executiei: tracking, criterii de acceptare, matrice de teste, audit de JAR, smoke test Paper si rollback.

Partile anterioare definesc strategia, runbook-ul, retetele si harta pe pachete. Partea 5 este documentul de folosit cand conversia chiar incepe si trebuie masurata.

## Regula principala

Conversia la Kotlin nu este gata cand codul compileaza. Este gata cand:

- build-ul local trece
- testele relevante trec
- JAR-ul contine runtime-ul necesar
- pluginul porneste pe Paper
- addonul medieval se incarca
- API-ul ramane consumabil din Java
- rollback-ul este clar

## Faza AQ: Tracker de conversie

Pentru fiecare slice, noteaza statusul in acest format:

```text
ID:
Pachet:
Fisiere convertite:
Tip:
Risc:
Motiv:
Teste rulate:
Smoke Paper:
Interop Java:
JAR audit:
Rollback:
Status:
Observatii:
```

Valori recomandate pentru `Tip`:

- test
- utilitar
- model
- validator
- selector
- repository
- service
- Paper integration
- API public

Valori recomandate pentru `Risc`:

- 1 - minim
- 2 - mic
- 3 - mediu
- 4 - mare
- 5 - critic

Valori recomandate pentru `Status`:

- planificat
- in lucru
- blocat
- convertit
- validat local
- validat Paper
- amanat
- respins

## Faza AR: Status board initial

Porneste cu acest board:

| ID | Zona | Tip | Risc | Status | Gate |
|---|---|---|---:|---|---|
| KOT-001 | Gradle Kotlin core | build | 2 | planificat | `clean build` |
| KOT-002 | Kotlin smoke test | test | 1 | planificat | test JUnit Kotlin |
| KOT-003 | Test simplu `utils` | test | 1 | planificat | test specific |
| KOT-004 | Test simplu `progression` | test | 1 | planificat | test specific |
| KOT-005 | `progression` model/filter | model | 2 | planificat | `*Progression*` |
| KOT-006 | `world.patch` enum/model | model | 2 | planificat | `VillagePatchPlannerTest` |
| KOT-007 | `engine` resolver/selector mic | selector | 2 | planificat | `*Quest*` |
| KOT-008 | `spawn` hasher/model | utilitar/model | 2 | planificat | `*Spawn*`, hash stabil |
| KOT-009 | JAR audit Kotlin | packaging | 3 | planificat | clase Kotlin + stdlib |
| KOT-010 | Paper smoke core | smoke | 4 | planificat | server Paper porneste |

Regula:

- nu treci la KOT-006 daca KOT-001 - KOT-005 nu sunt stabile
- nu treci la Paper integration daca JAR audit nu este clar

## Faza AS: Criterii de acceptare per risc

Risc 1:

- test specific trece
- testele modulului trec
- nu exista schimbari de productie

Risc 2:

- test specific trece
- testele modulului trec
- `clean build` trece
- call-site-urile Java compileaza fara adaptoare mari

Risc 3:

- toate cele de la risc 2
- teste de regresie pentru pachet
- verificare manuala a semnaturilor Java daca exista consumatori Java
- JAR generat

Risc 4:

- toate cele de la risc 3
- smoke Paper relevant
- audit `plugin.yml`
- audit runtime Kotlin in JAR

Risc 5:

- toate cele de la risc 4
- plan de rollback scris inainte
- conversie facuta dupa refactorizare
- test Java de consum daca este API public
- confirmare ca addonul medieval se incarca

## Faza AT: Matrice de teste

Teste rapide dupa fiecare slice mic:

```powershell
.\gradlew.bat :ainpc-core-plugin:test
```

Build complet dupa fiecare slice cu cod de productie:

```powershell
.\gradlew.bat clean build
```

JAR dupa orice schimbare de build, resurse sau packaging:

```powershell
.\gradlew.bat assemble
```

Teste tematice:

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests "*Progression*"
.\gradlew.bat :ainpc-core-plugin:test --tests "*Mapping*"
.\gradlew.bat :ainpc-core-plugin:test --tests "*VillagePatchPlannerTest"
.\gradlew.bat :ainpc-core-plugin:test --tests "*Quest*"
.\gradlew.bat :ainpc-core-plugin:test --tests "*FeaturePack*"
.\gradlew.bat :ainpc-core-plugin:test --tests "*Spawn*"
.\gradlew.bat :ainpc-core-plugin:test --tests "*Routine*"
.\gradlew.bat :ainpc-core-plugin:test --tests "*Story*"
.\gradlew.bat :ainpc-core-plugin:test --tests "*Gui*"
.\gradlew.bat :ainpc-core-plugin:test --tests "*AIOrchestration*"
```

Addon:

```powershell
.\gradlew.bat :ainpc-scenario-medieval:test
.\gradlew.bat :ainpc-scenario-medieval:assemble
```

API:

```powershell
.\gradlew.bat :ainpc-api:build
```

Regula:

- daca atingi `ainpc-api`, ruleaza si addonul medieval
- daca atingi packaging, ruleaza `assemble`
- daca atingi Paper runtime, ruleaza smoke test

## Faza AU: Audit JAR Kotlin

Cand apar primele fisiere `.kt`, verifica JAR-ul core.

Comanda:

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$jarPath = "ainpc-core-plugin\build\libs\ainpc-core-plugin-1.0.0.jar"
$jar = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
try {
  "plugin.yml present: $($null -ne $jar.GetEntry("plugin.yml"))"
  "kotlin stdlib present: $($null -ne ($jar.Entries | Where-Object { $_.FullName -like "kotlin/*" } | Select-Object -First 1))"
  "ainpc classes:"
  $jar.Entries |
    Where-Object { $_.FullName -like "ro/ainpc/*.class" -or $_.FullName -like "ro/ainpc/*/*.class" } |
    Select-Object -First 30 -ExpandProperty FullName
} finally {
  $jar.Dispose()
}
```

Ce cauti:

- `plugin.yml` exista
- clasele Kotlin proprii exista
- runtime-ul Kotlin este inclus daca este necesar
- nu exista duplicate evidente

Semne de problema:

- `ClassNotFoundException: kotlin.jvm...`
- JAR-ul addonului are Kotlin dar nu are runtime disponibil
- `plugin.yml` lipseste sau are versiune nefiltrata
- marimea JAR-ului creste neasteptat fara explicatie

## Faza AV: Smoke Paper pentru Kotlin

Smoke minim dupa prima clasa Kotlin de productie:

1. ruleaza `.\gradlew.bat clean assemble`
2. copiaza JAR-urile prin scriptul smoke
3. porneste serverul Paper
4. verifica incarcarea pluginului core
5. verifica incarcarea addonului medieval
6. ruleaza `/plugins`
7. ruleaza `/ainpc`
8. ruleaza un audit read-only

Script mapping:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-mapping.ps1 `
  -ServerDir "C:\Minecraft\paper-test"
```

Script quest:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-quests.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -PlayerName "NumeleTau"
```

Log-uri de cautat:

- erori de incarcare clasa Kotlin
- erori la `plugin.yml`
- erori de dependinta addon
- stacktrace la pornire
- erori la comenzi simple

Gate:

- serverul porneste
- core este enabled
- addonul medieval este enabled
- comenzile de baza raspund

## Faza AW: Rollback

Rollback pentru test convertit:

- sterge `.kt`
- restaureaza `.java`
- ruleaza testul specific

Rollback pentru utilitar/model:

- restaureaza Java
- sterge fisierul Kotlin
- verifica call-site-urile Java
- ruleaza testele pachetului

Rollback pentru build Kotlin:

- scoate pluginul Kotlin din modul
- scoate dependinta Kotlin stdlib
- scoate `kotlinVersion` daca nu mai exista niciun modul Kotlin
- sterge `src/main/kotlin` si `src/test/kotlin` daca sunt goale
- ruleaza `clean build`

Rollback pentru packaging:

- revino la ultimul `build.gradle` stabil
- ruleaza `clean assemble`
- inspecteaza JAR-ul

Regula:

- un slice Kotlin trebuie sa poata fi revertit fara sa atinga feature-uri neterminate.

## Faza AX: Criterii pentru Kotlin ca default in core

Kotlin poate deveni default pentru cod nou in `ainpc-core-plugin` doar dupa:

- cel putin 10 fisiere convertite cu succes
- cel putin 3 pachete atinse fara regresii
- `clean build` stabil
- smoke Paper reusit
- addon medieval incarcat
- JAR audit clar
- reguli de interop respectate
- niciun tip public din `ainpc-api` rupt

Pana atunci:

- Kotlin este permis doar cu referinta la documentele de conversie
- Java ramane default pentru entrypoints, API si clase mari

## Faza AY: Raport dupa fiecare sprint Kotlin

La finalul fiecarui sprint, actualizeaza un raport scurt:

```text
Sprint:
Fisiere Kotlin adaugate:
Fisiere Java eliminate:
Pachete atinse:
Teste rulate:
Smoke Paper:
Probleme interop:
Probleme packaging:
Rollback necesar:
Decizie pentru sprintul urmator:
```

Metrici utile:

- numar fisiere `.kt`
- numar fisiere `.java` ramase
- numar teste Kotlin
- durata `clean build`
- marime JAR core
- marime JAR addon medieval

Comenzi pentru inventar:

```powershell
(Get-ChildItem -Recurse -Filter *.kt | Measure-Object).Count
(Get-ChildItem -Recurse -Filter *.java | Measure-Object).Count
Get-ChildItem .\ainpc-core-plugin\build\libs\*.jar | Select-Object Name,Length
Get-ChildItem .\ainpc-scenario-medieval\build\libs\*.jar | Select-Object Name,Length
```

## Faza AZ: Blocaje si decizii

Blocaj: Kotlin rupe prea multe call-site-uri Java.

Decizie:

- opreste conversia clasei
- pastreaza Java
- extrage o clasa mica noua si converteste doar acea clasa

Blocaj: JAR-ul porneste local, dar serverul Paper da `ClassNotFoundException`.

Decizie:

- verifica includerea `kotlin-stdlib`
- verifica daca problema este in core sau addon
- nu continua conversia pana cand smoke-ul trece

Blocaj: model Kotlin schimba equals/hashCode.

Decizie:

- adauga test de comportament
- pastreaza Java daca identitatea obiectului era parte din contract
- evita `data class` pentru acel caz

Blocaj: API public devine greu de folosit din Java.

Decizie:

- revino la Java in `ainpc-api`
- muta Kotlin doar in implementarea core
- adauga test Java de consum inainte de o noua incercare

Blocaj: conversia cere coroutine.

Decizie:

- opreste slice-ul
- coroutine necesita design separat pentru scheduler Paper
- nu introduce coroutine ca efect secundar al conversiei de limbaj

## Tabel de acceptare finala

| Zona | Acceptare minima |
|---|---|
| Gradle | Kotlin plugin activ doar unde este necesar |
| Build | `clean build` trece |
| Teste | teste specifice si tematice trec |
| JAR core | `plugin.yml`, clase Kotlin si runtime verificate |
| Paper | serverul porneste fara erori Kotlin |
| Addon | `ainpc-scenario-medieval` se incarca dupa core |
| API | Java consumers continua sa compileze |
| Docs | seria de documente este actualizata dupa descoperiri |
| Rollback | fiecare slice poate fi revertit izolat |

## Definitia de gata pentru Partea 5

Partea 5 este indeplinita cand:

- exista tracker pentru slice-uri Kotlin
- exista matrice de teste si smoke
- exista procedura de audit JAR
- exista procedura de rollback
- exista criterii pentru a decide cand Kotlin devine default in core
- exista format de raport dupa fiecare sprint

## Urmatorul pas

Dupa partea 5, urmatorul document ar trebui creat doar daca apare o nevoie concreta.

Exemple de motive reale:

- se introduce coroutine support
- se decide convertirea `ainpc-api`
- se decide convertirea entrypoint-ului Paper
- se descopera probleme de packaging Kotlin pe server

Fara un astfel de motiv, seria este suficienta pentru a incepe conversia controlata.
