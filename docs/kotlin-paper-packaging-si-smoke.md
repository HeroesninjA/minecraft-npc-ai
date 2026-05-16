# Kotlin Paper Packaging si Smoke Test

Actualizat: 2026-05-16

## Scop

Acest document descrie verificarea JAR-urilor si smoke testele necesare dupa introducerea Kotlin in pluginul Paper.

Se aplica cand:

- exista primul fisier Kotlin de productie
- se modifica `build.gradle`
- se modifica shading-ul JAR-ului
- se activeaza Kotlin in addonul medieval
- se atinge entrypoint-ul Paper sau `plugin.yml`

## Regula principala

Un build Gradle verde nu este suficient pentru Paper.

Trebuie verificat:

- JAR-ul contine `plugin.yml`
- `plugin.yml` are versiune filtrata corect
- clasele Kotlin proprii sunt in JAR
- runtime-ul Kotlin este disponibil
- serverul Paper incarca pluginul
- addonul medieval se incarca dupa core

## Build minim

```powershell
.\gradlew.bat clean build
.\gradlew.bat assemble
```

JAR-uri asteptate:

```text
ainpc-core-plugin/build/libs/ainpc-core-plugin-1.0.0.jar
ainpc-scenario-medieval/build/libs/ainpc-scenario-medieval-1.0.0.jar
ainpc-api/build/libs/ainpc-api-1.0.0.jar
```

## Audit JAR core

Comanda:

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$jarPath = "ainpc-core-plugin\build\libs\ainpc-core-plugin-1.0.0.jar"
$jar = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
try {
  "plugin.yml present: $($null -ne $jar.GetEntry("plugin.yml"))"
  "kotlin runtime present: $($null -ne ($jar.Entries | Where-Object { $_.FullName -like "kotlin/*" } | Select-Object -First 1))"
  "kotlin metadata present: $($null -ne ($jar.Entries | Where-Object { $_.FullName -like "META-INF/*.kotlin_module" } | Select-Object -First 1))"
  $jar.Entries |
    Where-Object { $_.FullName -like "ro/ainpc/*" } |
    Select-Object -First 40 -ExpandProperty FullName
} finally {
  $jar.Dispose()
}
```

Rezultat acceptabil:

- `plugin.yml present: True`
- runtime Kotlin prezent daca exista clase Kotlin
- clasele `ro/ainpc/...` exista

## Verificare `plugin.yml`

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$jarPath = "ainpc-core-plugin\build\libs\ainpc-core-plugin-1.0.0.jar"
$jar = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
try {
  $entry = $jar.GetEntry("plugin.yml")
  $reader = [System.IO.StreamReader]::new($entry.Open())
  try { $reader.ReadToEnd() } finally { $reader.Dispose() }
} finally {
  $jar.Dispose()
}
```

Verifica:

- `version: 1.0.0`
- `main: ro.ainpc.AINPCPlugin`
- `api-version: '1.21'`
- comenzile sunt prezente

## Runtime Kotlin in core

Daca `ainpc-core-plugin` are clase Kotlin:

- `kotlin-stdlib` trebuie sa fie disponibila la runtime
- daca JAR-ul core este shaded, stdlib trebuie inclusa in JAR sau livrata prin alta metoda controlata
- serverul Paper nu trebuie sa depinda de instalarea Kotlin separata

Semne de problema:

```text
NoClassDefFoundError: kotlin/jvm/internal/Intrinsics
ClassNotFoundException: kotlin.Unit
```

Actiune:

- verifica dependinta `implementation "org.jetbrains.kotlin:kotlin-stdlib:..."`
- verifica task-ul de JAR/shading
- inspecteaza JAR-ul

## Runtime Kotlin in addon

Daca `ainpc-scenario-medieval` primeste Kotlin:

- addonul trebuie sa aiba acces la runtime Kotlin
- nu presupune automat ca runtime-ul din core este vizibil addonului
- testul pe server decide, nu presupunerea

Optiuni:

- shade runtime Kotlin si in addon
- mentine addonul Java
- foloseste o strategie comuna documentata pentru dependinte Paper

Recomandare:

- amana Kotlin in addon pana cand core-ul este stabil.

## Smoke test Paper minim

Pregatire:

```powershell
.\gradlew.bat clean assemble
```

Mapping smoke:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-mapping.ps1 `
  -ServerDir "C:\Minecraft\paper-test"
```

Quest smoke:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-paper-quests.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -PlayerName "NumeleTau"
```

Pe server:

```text
plugins
ainpc
ainpc audit world
ainpc audit quest
```

Gate:

- serverul porneste
- `AINPCPlugin` este enabled
- `AINPCScenarioMedieval` este enabled
- nu exista erori Kotlin in consola
- comenzile de baza raspund

## Cand smoke testul este obligatoriu

Ruleaza smoke test cand:

- introduci prima clasa Kotlin de productie
- schimbi `build.gradle`
- schimbi task-ul de JAR/shading
- schimbi `plugin.yml`
- atingi `AINPCPlugin`
- atingi listener-e Paper
- atingi comenzi
- atingi GUI runtime
- activezi Kotlin in addon

## Checklist release Kotlin

Inainte de release:

- `.\gradlew.bat clean build`
- `.\gradlew.bat assemble`
- audit JAR core
- audit JAR addon, daca addonul are Kotlin
- verificare `plugin.yml`
- smoke Paper mapping
- smoke Paper quest
- rollback documentat

## Rollback packaging

Daca Paper nu porneste:

1. opreste serverul
2. pastreaza logul
3. revino la ultimul JAR Java stabil
4. verifica daca eroarea este runtime Kotlin, `plugin.yml` sau shading
5. nu continua conversia pana cand smoke-ul trece

Rollback build:

- scoate pluginul Kotlin din modul
- scoate dependinta `kotlin-stdlib`
- sterge fisierele `.kt` din slice-ul curent
- ruleaza `clean build`
- ruleaza `assemble`

## Definitia de gata

Packaging-ul Kotlin este acceptat cand:

- JAR-ul core contine tot ce trebuie
- `plugin.yml` este corect
- serverul Paper porneste fara erori Kotlin
- addonul medieval se incarca
- comenzile de baza merg
- smoke testele relevante sunt trecute
