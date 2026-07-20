package ro.ainpc.settlement

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText

class SettlementPlanRuntimeBoundaryTest {
    @Test
    fun runtimeModulesDoNotConsumeDeprecatedSettlementPlan() {
        val projectRoot = Path.of("..").toAbsolutePath().normalize()
        val runtimeModules = listOf(
            "ainpc-core-plugin",
            "ainpc-mcp-service",
            "ainpc-scenario-medieval"
        )

        val offenders = runtimeModules.flatMap { module ->
            val sourceRoot = projectRoot.resolve(module).resolve("src/main")
            if (!Files.isDirectory(sourceRoot)) {
                emptyList()
            } else {
                Files.walk(sourceRoot).use { paths ->
                    paths
                        .filter { path ->
                            path.isRegularFile() &&
                                (path.name.endsWith(".kt") || path.name.endsWith(".java"))
                        }
                        .filter { path -> Regex("""\bSettlementPlan\b""").containsMatchIn(path.readText()) }
                        .map { path -> projectRoot.relativize(path).toString() }
                        .toList()
                }
            }
        }

        assertTrue(
            offenders.isEmpty(),
            "Deprecated SettlementPlan must remain outside runtime production code: $offenders"
        )
    }
}
