package ro.ainpc.gui

import java.util.UUID

class GuiSession(
    sessionId: UUID,
    playerId: UUID,
    key: GuiKey,
    openedAt: Long
) {
    private val sessionIdValue = sessionId
    private val playerIdValue = playerId
    private val keyValue = key
    private val openedAtValue = openedAt
    private var buttonsValue: Map<Int, GuiButton> = emptyMap()

    fun getSessionId(): UUID = sessionIdValue

    fun getPlayerId(): UUID = playerIdValue

    fun getKey(): GuiKey = keyValue

    fun getOpenedAt(): Long = openedAtValue

    fun getButtons(): Map<Int, GuiButton> = buttonsValue

    fun setButtons(buttons: Map<Int, GuiButton>?) {
        buttonsValue = java.util.Map.copyOf(buttons ?: emptyMap())
    }
}
