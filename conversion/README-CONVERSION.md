Java -> Kotlin conversion helper

This project includes a Gradle helper that collects Java sources into `build/conversion/input` so you can run a conversion tool on them while preserving the original tree.

Usage:

1. From the workspace root run the Gradle task (VS Code: `Run Task...` -> `Gradle: prepare Java->Kotlin`):

```powershell
.\gradlew.bat prepareJavaToKotlinConversion
```

2. The Java files will be copied to `build/conversion/input` preserving relative paths.

Current Java sources remaining in this repository:
- `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`
- `ainpc-core-plugin/src/main/java/ro/ainpc/engine/ScenarioEngine.java`
- `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`

3. Convert files using one of these approaches (recommended):

- Use IntelliJ IDEA's Java -> Kotlin conversion (the j2k converter):
  - Open the `build/conversion/input` folder as a project in IntelliJ and use `Code | Convert Java File to Kotlin File` or the project-level conversion.

- Use the experimental Gradle automation task if you have a CLI converter available:
  - Set `JAVA_TO_KOTLIN_CONVERTER_COMMAND` or pass `-PjavaToKotlinConverterCommand="<path>"`.
  - Optionally set `JAVA_TO_KOTLIN_CONVERTER_ARGS` or `-PjavaToKotlinConverterArgs="<args>"`.
  - The task will pass the prepared input directory and output directory to the converter.
  - Run:

```powershell
.\gradlew.bat convertJavaToKotlin -PjavaToKotlinConverterCommand="<converter-path>" -PjavaToKotlinConverterArgs="--input {input} --output {output}"
```

  - If your converter does not use `{input}` / `{output}` placeholders, the task appends the input and output paths by default.

- If you have IntelliJ IDEA installed, you can open the prepared source tree directly in IDEA:
  - Run:

```powershell
.\gradlew.bat openJavaToKotlinInputInIdea
```

  - If IDEA is not installed at the default path, set `-PideaLauncherPath="<path>"` or `IDEA_PATH="<path>"`.

4. After conversion, review and refine Kotlin code manually; automatic converters are not perfect and require human review.

Notes:
- The Gradle task does not perform Java->Kotlin conversion automatically because this repository does not bundle a universal headless J2K converter. It prepares sources and provides instructions so you can run the conversion in IntelliJ IDEA or with your own CLI converter.
- Current repo status: only three Java source files remain, so the codebase is already largely converted.
- `idea.bat` does support basic launcher commands such as `--help`, `--version`, opening a project directory, opening a file with optional `--line`/`--column`, `diff`, and `merge`, but it does not expose a supported command to execute an internal IDE action like `ConvertJavaToKotlin` directly.
- From the installed IntelliJ IDEA 2026.1 environment, the Kotlin plugin exposes the Java-to-Kotlin conversion action as `ConvertJavaToKotlin`, implemented by `org.jetbrains.kotlin.idea.actions.JavaToKotlinAction`.
- There is also an internal performance-testing command class `ConvertJavaToKotlinCommand` with a matching internal command string `convertJavaToKotlin`, but this is not a supported public CLI invocation for `idea.bat`.
- Use `openJavaToKotlinInputInIdea` to launch IntelliJ directly on the prepared input folder and convert files manually. On Windows, the task now uses PowerShell to detach the IDE process so the Gradle task can return immediately.
- Use `openRemainingJavaInIdea` to open all remaining Java source files in the prepared conversion input tree:

```powershell
.\gradlew.bat openRemainingJavaInIdea
```

- The Gradle task accepts `-PideaLauncherPath='<path>'` or `IDEA_PATH='<path>'` and will try to resolve either a launcher file (`idea.bat`, `idea64.exe`) or an IntelliJ installation directory containing `bin/idea64.exe`.
- Use `-PideaFileToOpen='<path>'` with `openJavaToKotlinInputInIdea` to open a specific file or directory instead of `build/conversion/input`.
- Use `-PideaLine=<line>` and `-PideaColumn=<column>` together with `ideaFileToOpen` to open a file at a specific cursor position.
- You can inspect the resolved launcher location with:

```powershell
.\gradlew.bat ideaLauncherInfo
```
- Use `listJavaSources` to preview the Java files that will be copied into `build/conversion/input`.
- Use `kotlinRatio` to calculate the Kotlin/Java share in the repository by file count and line count:

```powershell
.\gradlew.bat kotlinRatio
```

- Use `conversionNextSteps` to print a short, actionable summary of the remaining conversion work and the recommended commands to run next:

```powershell
.\gradlew.bat conversionNextSteps
```

- Use `javaToKotlinConverterInfo` to inspect configured converter settings, discover PATH candidates, and verify the IDEA launcher path:

```powershell
.\gradlew.bat javaToKotlinConverterInfo
```
- The current workflow is intentionally manual or external-CLI-driven; full IntelliJ automation would need a custom playback/remote-driver integration and is not yet implemented here.
- If you want, I can also help locate or configure a specific external converter binary for your environment.
