package ro.ainpc.version

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class BuildVersionInfoContractTest {
    @Test
    fun buildVersionInfoReadsBuildMetadataResource() {
        val source = File("src/main/kotlin/ro/ainpc/version/BuildVersionInfo.kt").readText()
        val buildScript = File("build.gradle").readText()

        assertTrue(source.contains("/build-info.properties"))
        assertTrue(source.contains("buildHash"))
        assertTrue(source.contains("buildTimestamp"))
        assertTrue(buildScript.contains("build-info.properties"))
        assertTrue(buildScript.contains("buildHash"))
        assertTrue(buildScript.contains("buildTimestamp"))
    }
}
