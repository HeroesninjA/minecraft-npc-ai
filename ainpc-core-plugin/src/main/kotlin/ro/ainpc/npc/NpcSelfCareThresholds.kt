package ro.ainpc.npc

internal object NpcSelfCareThresholds {
    const val START = 35
    const val RECOVERED = 60

    fun needsCare(level: Int, alreadyActive: Boolean): Boolean =
        level < if (alreadyActive) RECOVERED else START
}
