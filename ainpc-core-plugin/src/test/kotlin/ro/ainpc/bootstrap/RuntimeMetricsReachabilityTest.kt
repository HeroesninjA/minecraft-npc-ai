package ro.ainpc.bootstrap

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.readText

class RuntimeMetricsReachabilityTest {
    @Test
    fun allDocumentedRuntimeDomainsHaveRealProducers() {
        val sources = mapOf(
            "RuntimeMetricNames.ROUTINE_TICK" to Path.of("src/main/kotlin/ro/ainpc/routine/RoutineCoordinator.kt"),
            "RuntimeMetricNames.DATABASE_OPERATION" to Path.of("src/main/kotlin/ro/ainpc/database/DatabaseManager.kt"),
            "RuntimeMetricNames.COMMAND_DISPATCH" to Path.of("src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt"),
            "observedTask(" to Path.of("src/main/kotlin/ro/ainpc/bootstrap/SchedulerCoordinator.kt"),
            "RuntimeMetricNames.AI_ORCHESTRATION" to Path.of("src/main/kotlin/ro/ainpc/ai/orchestration/AIOrchestrationService.kt"),
            "RuntimeMetricNames.DEBUG_DUMP_EXPORT" to Path.of("src/main/kotlin/ro/ainpc/debug/DebugDumpService.kt"),
            "RuntimeMetricNames.RUNTIME_SNAPSHOT_EXPORT" to Path.of("src/main/kotlin/ro/ainpc/mcp/bridge/RuntimeSnapshotProducer.kt"),
        )

        for ((expected, path) in sources) {
            val source = path.readText()
            assertTrue(source.contains(expected), "$path does not expose $expected")
        }
    }
}
