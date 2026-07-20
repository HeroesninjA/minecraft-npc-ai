package ro.ainpc.debug

import ro.ainpc.context.SensitiveDataRedactionPolicy
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean

internal data class DebugDumpArtifactSnapshot(
    val fileName: String,
    val format: DebugDumpArtifactFormat,
    val value: Any,
) {
    init {
        require(fileName.isNotBlank()) { "Numele artefactului debug dump nu poate fi gol." }
        require(Path.of(fileName).fileName.toString() == fileName) {
            "Artefactul debug dump trebuie sa foloseasca doar un nume de fisier: $fileName"
        }
        require(
            (format == DebugDumpArtifactFormat.TEXT && value is String) ||
                format == DebugDumpArtifactFormat.JSON
        ) { "Continut invalid pentru artefactul $fileName." }
    }

    companion object {
        fun text(fileName: String, content: String): DebugDumpArtifactSnapshot =
            DebugDumpArtifactSnapshot(fileName, DebugDumpArtifactFormat.TEXT, content)

        fun json(fileName: String, value: Any): DebugDumpArtifactSnapshot =
            DebugDumpArtifactSnapshot(fileName, DebugDumpArtifactFormat.JSON, value)
    }
}

internal class DebugDumpArtifactSet(initial: Iterable<DebugDumpArtifactSnapshot> = emptyList()) {
    private val artifacts = LinkedHashMap<String, DebugDumpArtifactSnapshot>()

    init {
        initial.forEach(::add)
    }

    fun add(artifact: DebugDumpArtifactSnapshot) {
        check(artifacts.putIfAbsent(artifact.fileName, artifact) == null) {
            "Artefact debug dump duplicat: ${artifact.fileName}"
        }
    }

    fun addText(fileName: String, content: String) = add(DebugDumpArtifactSnapshot.text(fileName, content))

    fun addJson(fileName: String, value: Any) = add(DebugDumpArtifactSnapshot.json(fileName, value))

    fun values(): List<DebugDumpArtifactSnapshot> = artifacts.values.toList()
}

internal data class DebugDumpCapturedSnapshot(
    val scope: String,
    val playerFilter: String?,
    val privacyMode: DebugDumpPrivacyMode,
    val capturedAt: LocalDateTime,
    val dataFolder: Path,
    val dumpsRoot: Path,
    val retentionPolicy: DebugDumpRetentionPolicy,
    val redactionPolicy: SensitiveDataRedactionPolicy,
    val server: DebugDumpServerSnapshotData,
    val runtimeArtifacts: List<DebugDumpArtifactSnapshot>,
    val mappingSnapshot: com.google.gson.JsonObject?,
    val storyScenarios: List<DebugDumpStoryScenarioSnapshot>,
)

internal data class DebugDumpCompletedSnapshot(
    val artifacts: List<DebugDumpArtifactSnapshot>,
    val completedAt: LocalDateTime,
    val databaseTransactionUsed: Boolean,
)

internal class DebugDumpExportGate {
    private val active = AtomicBoolean(false)

    fun tryAcquire(): Boolean = active.compareAndSet(false, true)

    fun release() {
        active.set(false)
    }
}
