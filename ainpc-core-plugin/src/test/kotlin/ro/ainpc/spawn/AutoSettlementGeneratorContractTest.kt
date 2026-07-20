package ro.ainpc.spawn

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AutoSettlementGeneratorContractTest {
    @Test
    fun generatorOptsIntoExistingRegionReuse() {
        val source = File("src/main/kotlin/ro/ainpc/spawn/AutoSettlementGenerator.kt").readText()

        assertTrue(source.contains("allowExistingRegion = true"))
    }
}
