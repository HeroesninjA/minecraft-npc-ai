package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DebugDumpAsyncPipelineTest {
    @Test
    fun commandCapturesBeforeWorkerAndReturnsMessagesToMainThread() {
        val source = File("src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt").readText()
        val capture = source.indexOf("service.captureRuntimeSnapshot(")
        val async = source.indexOf("scheduler.runTaskAsynchronously(plugin", capture)
        val write = source.indexOf("service.writeCapturedDump(capture)", async)
        val syncCallback = source.indexOf("scheduler.runTask(plugin", write)

        assertTrue(capture >= 0)
        assertTrue(async > capture)
        assertTrue(write > async)
        assertTrue(syncCallback > write)
        assertTrue(source.contains("debugDumpExportGate.tryAcquire()"))
        assertTrue(source.contains("debugDumpExportGate.release()"))
    }

    @Test
    fun mainThreadCaptureExcludesDatabaseRetentionAndLogIo() {
        val source = File("src/main/kotlin/ro/ainpc/debug/DebugDumpService.kt").readText()
        val captureStart = source.indexOf("internal fun captureRuntimeSnapshot(")
        val workerStart = source.indexOf("internal fun writeCapturedDump(", captureStart)
        val captureBody = source.substring(captureStart, workerStart)
        val workerBody = source.substring(workerStart)

        assertFalse(captureBody.contains("prepareStatement("))
        assertFalse(captureBody.contains("DebugDumpRetention.cleanup("))
        assertFalse(captureBody.contains("DebugDumpIO.readRecentServerLog("))
        assertFalse(captureBody.contains("DebugDumpBehaviorProfileJson.buildBehaviorProfilesJson(capture.dataFolder)"))
        assertTrue(workerBody.contains("databaseManager.executeTransaction"))
        assertTrue(workerBody.contains("DebugDumpRetention.cleanup("))
        assertTrue(workerBody.contains("DebugDumpIO.readRecentServerLog(dataFolder"))
        assertTrue(workerBody.contains("Artifact set frozen before write: true"))
    }

    @Test
    fun exportGateRejectsConcurrentJobsUntilRelease() {
        val gate = DebugDumpExportGate()

        assertTrue(gate.tryAcquire())
        assertFalse(gate.tryAcquire())
        gate.release()
        assertTrue(gate.tryAcquire())
    }
}
