package ro.ainpc

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.file.Path

class PermissionNodeDeclarationAuditTest {

    @Test
    fun allPermissionNodesAreDeclaredInPluginYml() {
        val pluginYml = Path.of("src/main/resources/plugin.yml").toFile().readLines()
        val linesAfterPermissions = pluginYml
            .dropWhile { line -> !line.startsWith("permissions:") }
            .drop(1)
        val declaredNodes = linesAfterPermissions
            .filter { line -> line.startsWith("  ") && !line.startsWith("   ") && line.contains(":") }
            .map { line -> line.trim().substringBefore(":").trim() }
            .filter { it.startsWith("ainpc.") }
            .toSet()

        val enumNodes = ro.ainpc.api.PermissionNode.allNodes().toSet()

        println("Declared nodes in plugin.yml: $declaredNodes")
        println("Enum nodes: $enumNodes")

        val undeclared = enumNodes - declaredNodes
        val unused = declaredNodes - enumNodes

        val message = buildString {
            if (undeclared.isNotEmpty()) {
                appendLine("Noduri de permisiune in enum dar nedeclarate in plugin.yml:")
                undeclared.sorted().forEach { appendLine("  - $it") }
            }
            if (unused.isNotEmpty()) {
                appendLine("Noduri de permisiune declarate in plugin.yml dar nefolosite in enum:")
                unused.sorted().forEach { appendLine("  - $it") }
            }
        }

        Assertions.assertTrue(undeclared.isEmpty() && unused.isEmpty()) {
            "Discrepante intre PermissionNode si plugin.yml:\n$message"
        }
    }
}
