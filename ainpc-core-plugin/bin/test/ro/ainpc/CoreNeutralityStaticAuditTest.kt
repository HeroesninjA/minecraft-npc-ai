package ro.ainpc

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.relativeTo
import kotlin.io.path.readText

class CoreNeutralityStaticAuditTest {
    @Test
    fun mainRuntimeDoesNotReintroduceThematicProfessionTerms() {
        val sourceRoot = Path.of("src/main")
        val forbiddenTerms = listOf(
            "blacksmith",
            "farmer",
            "merchant",
            "innkeeper",
            "priest",
            "soldier",
            "fierar",
            "fermier",
            "negustor",
            "paznic",
            "preot",
            "soldat",
            "alchimist",
            "bibliotecar",
            "macelar"
        ).map { term -> Regex("""(?i)\b${Regex.escape(term)}\b""") }

        val offenders = Files.walk(sourceRoot).use { paths ->
            paths
                .filter { path -> path.isRegularFile() && path.name.matches(Regex(""".*\.(java|kt|yml|yaml)""")) }
                .flatMap { path ->
                    val text = path.readText()
                    forbiddenTerms
                        .filter { pattern -> pattern.containsMatchIn(text) }
                        .map { pattern -> "${path.relativeTo(sourceRoot)}: ${pattern.pattern}" }
                        .stream()
                }
                .toList()
        }

        assertTrue(
            offenders.isEmpty(),
            "Core runtime must keep thematic professions in addons/config, not src/main: $offenders"
        )
    }
}
