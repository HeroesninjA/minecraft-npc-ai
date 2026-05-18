package ro.ainpc.listeners

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import ro.ainpc.AINPCPlugin
import ro.ainpc.managers.ConversationSessionManager
import ro.ainpc.npc.AINPC
import ro.ainpc.utils.MessageUtils
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

abstract class AbstractPluginListener(
    @JvmField protected val plugin: AINPCPlugin
) : Listener {

    protected fun messages(): MessageUtils = plugin.messageUtils

    protected fun conversations(): ConversationSessionManager = plugin.conversationSessionManager

    protected fun beginConversationSession(player: Player, npc: AINPC): CompletableFuture<Boolean> {
        conversations().startConversation(player, npc)
        return plugin.memoryManager.ensureFirstMeetingMemoryAsync(npc, player)
    }

    protected fun refreshConversationSession(player: Player) {
        conversations().touchConversation(player)
    }

    protected fun runSync(task: Runnable) {
        plugin.server.scheduler.runTask(plugin, task)
    }

    protected fun runLater(task: Runnable, delayTicks: Long) {
        plugin.server.scheduler.runTaskLater(plugin, task, delayTicks)
    }

    protected fun <T> callSync(supplier: Supplier<T>): T {
        if (Bukkit.isPrimaryThread()) {
            return supplier.get()
        }

        val future = CompletableFuture<T>()
        runSync(
            Runnable {
                try {
                    future.complete(supplier.get())
                } catch (throwable: Throwable) {
                    future.completeExceptionally(throwable)
                }
            }
        )

        try {
            return future.get(2, TimeUnit.SECONDS)
        } catch (e: Exception) {
            throw IllegalStateException("Nu s-a putut executa operatia pe main thread.", e)
        }
    }
}
