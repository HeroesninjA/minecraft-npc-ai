package ro.ainpc.spawn

import ro.ainpc.npc.AINPC

class NpcSpawnResult(
    success: Boolean,
    created: Boolean,
    npc: AINPC?,
    errors: List<String>?,
    warnings: List<String>?
) {
    private val successValue = success
    private val createdValue = created
    private val npcValue = npc
    private val errorsValue = java.util.List.copyOf(errors ?: emptyList())
    private val warningsValue = java.util.List.copyOf(warnings ?: emptyList())

    fun success(): Boolean = successValue

    fun created(): Boolean = createdValue

    fun npc(): AINPC? = npcValue

    fun errors(): List<String> = errorsValue

    fun warnings(): List<String> = warningsValue

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is NpcSpawnResult) {
            return false
        }

        return successValue == other.successValue &&
            createdValue == other.createdValue &&
            npcValue == other.npcValue &&
            errorsValue == other.errorsValue &&
            warningsValue == other.warningsValue
    }

    override fun hashCode(): Int {
        var result = successValue.hashCode()
        result = 31 * result + createdValue.hashCode()
        result = 31 * result + (npcValue?.hashCode() ?: 0)
        result = 31 * result + errorsValue.hashCode()
        result = 31 * result + warningsValue.hashCode()
        return result
    }

    override fun toString(): String =
        "NpcSpawnResult[success=$successValue, created=$createdValue, npc=$npcValue, " +
            "errors=$errorsValue, warnings=$warningsValue]"

    companion object {
        @JvmStatic
        fun success(npc: AINPC?, warnings: List<String>?): NpcSpawnResult = created(npc, warnings)

        @JvmStatic
        fun created(npc: AINPC?, warnings: List<String>?): NpcSpawnResult =
            NpcSpawnResult(true, true, npc, emptyList(), warnings)

        @JvmStatic
        fun reused(npc: AINPC?, warnings: List<String>?): NpcSpawnResult =
            NpcSpawnResult(true, false, npc, emptyList(), warnings)

        @JvmStatic
        fun failed(errors: List<String>?, warnings: List<String>?): NpcSpawnResult =
            NpcSpawnResult(false, false, null, errors, warnings)
    }
}
