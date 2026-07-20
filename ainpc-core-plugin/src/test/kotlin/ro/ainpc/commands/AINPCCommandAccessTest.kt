package ro.ainpc.commands

import org.bukkit.command.CommandSender
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

class AINPCCommandAccessTest {
    @Test
    fun creatorAccessRequiresAdminOrCreatorPermission() {
        assertFalse(hasCreatorAccess(senderWith("ainpc.quest")))
        assertTrue(hasCreatorAccess(senderWith("ainpc.creator")))
        assertTrue(hasCreatorAccess(senderWith("ainpc.admin")))
    }

    private fun senderWith(vararg permissions: String): CommandSender {
        val allowed = permissions.toSet()
        return Proxy.newProxyInstance(
            CommandSender::class.java.classLoader,
            arrayOf(CommandSender::class.java)
        ) { _, method, args ->
            when (method.name) {
                "hasPermission" -> (args?.firstOrNull() as? String) in allowed
                else -> when (method.returnType) {
                    java.lang.Boolean.TYPE -> false
                    java.lang.Integer.TYPE -> 0
                    java.lang.Long.TYPE -> 0L
                    java.lang.Double.TYPE -> 0.0
                    java.lang.Void.TYPE -> null
                    else -> null
                }
            }
        } as CommandSender
    }
}
