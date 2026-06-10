package ro.ainpc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.database.DatabaseDialect
import ro.ainpc.database.DatabaseDialectSql
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.relativeTo
import kotlin.io.path.readText

class StorageDialectStaticAuditTest {
    private val sqliteSpecificPatterns = mapOf(
        "ON_CONFLICT" to Regex("""(?i)\bON\s+CONFLICT\b"""),
        "INSERT_OR" to Regex("""(?i)\bINSERT\s+OR\s+(IGNORE|REPLACE)\b"""),
        "SQLITE_DATETIME" to Regex("""(?i)\bdatetime\s*\("""),
        "SQLITE_LIMIT_MAX" to Regex("""(?i)\bLIMIT\s+MAX\s*\(\s*0\s*,"""),
        "SQLITE_SCALAR_MIN_MAX" to Regex("""\b(MIN|MAX)\s*\([^()\n,]+,\s*(?:[^()\n]+|(?:MIN|MAX)\s*\([^()\n,]+,\s*[^()\n]+\))\)"""),
        "PRAGMA" to Regex("""(?i)\bPRAGMA\b""")
    )

    @Test
    fun sqliteSpecificRuntimeSqlRemainsExplicitlyInventoried() {
        val sourceRoot = Path.of("src/main")

        val actual = scanSqliteSpecificSql(sourceRoot, sqliteSpecificPatterns)

        assertEquals(
            mapOf(
                "ON_CONFLICT" to mapOf(
                    "java/ro/ainpc/engine/ScenarioEngine.java" to 1,
                    "java/ro/ainpc/managers/NPCManager.java" to 1,
                    "kotlin/ro/ainpc/ai/DialogManager.kt" to 1,
                    "kotlin/ro/ainpc/progression/ProgressionRepository.kt" to 1,
                    "kotlin/ro/ainpc/spawn/HouseholdPersistenceServiceState.kt" to 4,
                    "kotlin/ro/ainpc/spawn/SpawnBatchTracker.kt" to 2,
                    "kotlin/ro/ainpc/story/StoryStateService.kt" to 2,
                    "kotlin/ro/ainpc/world/NpcWorldBindingService.kt" to 1
                ),
                "INSERT_OR" to mapOf(
                    "java/ro/ainpc/managers/NPCManager.java" to 3,
                    "kotlin/ro/ainpc/database/DatabaseManager.kt" to 2
                ),
                "SQLITE_DATETIME" to mapOf(
                    "kotlin/ro/ainpc/managers/MemoryManager.kt" to 3
                ),
                "SQLITE_LIMIT_MAX" to mapOf(
                    "kotlin/ro/ainpc/managers/MemoryManager.kt" to 1
                ),
                "SQLITE_SCALAR_MIN_MAX" to mapOf(
                    "kotlin/ro/ainpc/ai/DialogManager.kt" to 4,
                    "kotlin/ro/ainpc/spawn/HouseholdPersistenceServiceState.kt" to 1
                ),
                "PRAGMA" to mapOf(
                    "kotlin/ro/ainpc/database/DatabaseManager.kt" to 5
                )
            ),
            actual,
            "SQLite-specific SQL changed. Port it through dialect helpers or update docs/storage-provider-roadmap.md deliberately."
        )
    }

    @Test
    fun dmlTranslationRemovesInventoriedSqliteOnlyDmlPatternsForMysql() {
        val sourceRoot = Path.of("src/main")
        val dmlPatterns = sqliteSpecificPatterns.filterKeys { key -> key != "PRAGMA" }
        val offenders = mutableListOf<String>()

        Files.walk(sourceRoot).use { paths ->
            paths
                .filter { path -> path.isRegularFile() && path.name.matches(Regex(""".*\.(java|kt|yml|yaml)""")) }
                .forEach { path ->
                    val text = path.readText()
                    extractSqlTextBlocks(text).forEachIndexed { index, sqlBlock ->
                        if (dmlPatterns.values.none { pattern -> pattern.containsMatchIn(sqlBlock) }) {
                            return@forEachIndexed
                        }
                        val translated = DatabaseDialectSql.translateDml(sqlBlock, DatabaseDialect.MYSQL)
                        dmlPatterns.forEach { (name, pattern) ->
                            if (pattern.containsMatchIn(translated)) {
                                offenders.add("${path.relativeTo(sourceRoot).toString().replace('\\', '/')}#$index: $name")
                            }
                        }
                        if (Regex("""(?i)\bexcluded\.""").containsMatchIn(translated)) {
                            offenders.add("${path.relativeTo(sourceRoot).toString().replace('\\', '/')}#$index: EXCLUDED_ALIAS")
                        }
                    }
                }
        }

        assertTrue(
            offenders.isEmpty(),
            "MySQL DML translation left SQLite-only constructs behind: $offenders"
        )
    }

    private fun extractSqlTextBlocks(text: String): List<String> =
        Regex("\"\"\"(.*?)\"\"\"", setOf(RegexOption.DOT_MATCHES_ALL))
            .findAll(text)
            .map { match -> match.groupValues[1].trimIndent().trim() }
            .toList()

    private fun scanSqliteSpecificSql(sourceRoot: Path, patterns: Map<String, Regex>): Map<String, Map<String, Int>> {
        return patterns.mapValues { (_, pattern) ->
            Files.walk(sourceRoot).use { paths ->
                paths
                    .filter { path -> path.isRegularFile() && path.name.matches(Regex(""".*\.(java|kt|yml|yaml)""")) }
                    .map { path ->
                        val relativePath = path.relativeTo(sourceRoot).toString().replace('\\', '/')
                        relativePath to pattern.findAll(path.readText()).count()
                    }
                    .filter { (_, count) -> count > 0 }
                    .toList()
                    .toMap()
            }
        }
    }
}
