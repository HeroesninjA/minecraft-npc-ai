package ro.ainpc.engine

import java.util.Collections
import java.util.LinkedHashMap
import java.util.Locale

class PlayerQuestProgress(
    private val templateId: String?,
    private val questCode: String?,
    private val status: QuestStatus?,
    private val startedAt: Long,
    private val completedAt: Long,
    private val updatedAt: Long,
    currentPhase: String?,
    objectiveProgress: Map<String, Int>?,
    questVariables: Map<String, String>?,
) {
    private val currentPhase: String = currentPhase ?: ""
    private val objectiveProgress: Map<String, Int> =
        Collections.unmodifiableMap(LinkedHashMap(objectiveProgress ?: emptyMap()))
    private val questVariables: Map<String, String> =
        Collections.unmodifiableMap(LinkedHashMap(questVariables ?: emptyMap()))

    fun templateId(): String? = templateId

    fun questCode(): String? = questCode

    fun status(): QuestStatus? = status

    fun startedAt(): Long = startedAt

    fun completedAt(): Long = completedAt

    fun updatedAt(): Long = updatedAt

    fun currentPhase(): String = currentPhase

    fun objectiveProgress(): Map<String, Int> = objectiveProgress

    fun questVariables(): Map<String, String> = questVariables

    fun isCurrent(): Boolean = status == QuestStatus.OFFERED || status == QuestStatus.ACTIVE

    fun isOffered(): Boolean = status == QuestStatus.OFFERED

    fun isActive(): Boolean = status == QuestStatus.ACTIVE

    fun isCompleted(): Boolean = status == QuestStatus.COMPLETED
}

enum class QuestStatus {
    NOT_STARTED,
    OFFERED,
    ACTIVE,
    COMPLETED,
    FAILED;

    fun isArchived(): Boolean = this == COMPLETED || this == FAILED

    fun storageValue(): String = name.lowercase(Locale.ROOT)

    companion object {
        @JvmStatic
        fun fromStorage(value: String?): QuestStatus {
            if (value.isNullOrBlank()) {
                return NOT_STARTED
            }

            return try {
                valueOf(value.trim().uppercase(Locale.ROOT))
            } catch (_: IllegalArgumentException) {
                NOT_STARTED
            }
        }
    }
}
