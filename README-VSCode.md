Config for working with this multi-module Kotlin/Gradle Minecraft plugin in VS Code

Quick start:

1. Install recommended extensions (Java, Gradle, Kotlin language server).
2. Use the `Run Task...` command and select `Gradle: build` to build the project.
3. Use `Gradle: test` to run tests.
4. To debug the plugin inside a Minecraft server, start the server with remote debugging enabled (e.g. `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005`) and use the `Attach to JVM` launch config.

Notes:
- The project uses the Gradle wrapper (`gradlew.bat`) on Windows; tasks call it directly.
- Compiler options and JVM target are configured in `build.gradle` for subprojects.
