package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class FeaturePackVersionContractTest {
    @Test
    fun loaderReadsAndReportsTheSameTopLevelVersionKey() {
        val source = File("src/main/kotlin/ro/ainpc/engine/FeaturePackLoader.kt").readText()

        assertTrue(source.contains("private const val PACK_VERSION_KEY = \"version\""))
        assertTrue(source.contains("config.getInt(PACK_VERSION_KEY, pack.schemaVersion)"))
        assertTrue(source.contains("are \$PACK_VERSION_KEY=\${pack.schemaVersion}"))
        assertTrue(source.contains("cheia top-level '\$PACK_VERSION_KEY'"))
        assertFalse(source.contains("schema_version"))
    }
}
