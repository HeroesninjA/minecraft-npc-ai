package ro.ainpc.gui

import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.UUID

class GuiServiceAuthoringSelectionTest {
    @Test
    fun authoringSelectionDefaultsAndPermissionsPerPlayer() {
        val service = GuiService(newPluginInstance())
        val player = newPlayer(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val authoringPlayer = newPlayer(
            UUID.fromString("00000000-0000-0000-0000-000000000003"),
            setOf("ainpc.gui.debug")
        )

        assertEquals("", service.getAuthoringQuestSelector(player))
        assertEquals("", service.getAuthoringMechanicId(player))
        assertEquals(false, service.canOpen(player, GuiKey.AUTHORING))
        assertEquals(true, service.canOpen(authoringPlayer, GuiKey.AUTHORING))
    }

    @Test
    fun openAuthoringAndClearSetPlayerSelection() {
        val service = GuiService(newPluginInstance())
        val player = newPlayer(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val otherPlayer = newPlayer(UUID.fromString("00000000-0000-0000-0000-000000000002"))

        runCatching { service.openAuthoring(player, "story_driven", "build") }
        assertEquals("story_driven", service.getAuthoringQuestSelector(player))
        assertEquals("build", service.getAuthoringMechanicId(player))
        assertEquals("", service.getAuthoringQuestSelector(otherPlayer))
        assertEquals("", service.getAuthoringMechanicId(otherPlayer))

        runCatching { service.clearAuthoringSelection(player) }
        assertEquals("", service.getAuthoringQuestSelector(player))
        assertEquals("", service.getAuthoringMechanicId(player))
    }

    private fun newPlayer(uniqueId: UUID, permissions: Set<String> = emptySet()): Player {
        val handler = InvocationHandler { _, method, arguments ->
            if (method.name == "hasPermission") {
                return@InvocationHandler when (val argument = arguments?.firstOrNull()) {
                    is String -> permissions.contains(argument)
                    else -> false
                }
            }
            defaultValue(method, uniqueId)
        }
        return Proxy.newProxyInstance(
            Player::class.java.classLoader,
            arrayOf(Player::class.java),
            handler
        ) as Player
    }

    private fun newPluginInstance(): AINPCPlugin {
        val field = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        val unsafe = field.get(null) as sun.misc.Unsafe
        return unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
    }

    private fun defaultValue(method: Method, uniqueId: UUID): Any? {
        if (method.name == "getUniqueId") {
            return uniqueId
        }
        return when (method.returnType) {
            java.lang.Boolean.TYPE -> false
            java.lang.Byte.TYPE -> 0.toByte()
            java.lang.Short.TYPE -> 0.toShort()
            java.lang.Integer.TYPE -> 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0f
            java.lang.Double.TYPE -> 0.0
            java.lang.Character.TYPE -> '\u0000'
            java.lang.Void.TYPE -> null
            else -> null
        }
    }
}
