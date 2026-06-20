package ro.ainpc.version

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class BuildVersionInfoContractTest {
    @Test
    fun buildVersionInfoFormatsVersionSnapshotConsistently() {
        val lines = BuildVersionInfo.formatSnapshot(
            BuildVersionSnapshot("1.2.3", "abc123", "2026-06-19T10:00:00Z")
        )

        assertTrue(lines.contains("&6=== AINPC Version ==="))
        assertTrue(lines.contains("&eUltima versiune: &f1.2.3"))
        assertTrue(lines.contains("&eHash ultimul build: &fabc123"))
        assertTrue(lines.contains("&eData si ora buildului: &f2026-06-19T10:00:00Z"))
    }

    @Test
    fun buildVersionInfoReadsBuildMetadataResource() {
        val source = File("src/main/kotlin/ro/ainpc/version/BuildVersionInfo.kt").readText()
        val buildScript = File("../build.gradle").readText()

        assertTrue(source.contains("/build-info.properties"))
        assertTrue(source.contains("buildHash"))
        assertTrue(source.contains("buildTimestamp"))
        assertTrue(buildScript.contains("build-info.properties"))
        assertTrue(buildScript.contains("buildHash"))
        assertTrue(buildScript.contains("buildTimestamp"))
    }
}
