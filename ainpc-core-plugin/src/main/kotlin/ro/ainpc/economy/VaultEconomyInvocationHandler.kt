package ro.ainpc.economy

import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.util.logging.Level

class VaultEconomyInvocationHandler(private val plugin: AINPCPlugin) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        return when (method.name) {
            "isEnabled" -> true
            "getName" -> "AINPC Economy"
            "currencyNameSingular" -> "coin"
            "currencyNamePlural" -> "coins"
            "format" -> formatAmount(args)
            "fractionalDigits" -> 0
            "has" -> hasBalance(args)
            "hasAccount" -> true
            "createPlayerAccount" -> createAccount(args)
            "getBalance" -> getBalance(args)
            "withdrawPlayer" -> withdrawPlayer(args)
            "depositPlayer" -> depositPlayer(args)
            "bankBalance", "bankDeposit", "bankHas", "bankWithdraw",
            "createBank", "deleteBank", "bankBreak",
            "isBankMember", "isBankOwner" -> null
            "toString" -> "AINPC Economy Vault Bridge"
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> args?.firstOrNull() === proxy
            "bankX" -> if (method.name.startsWith("bank")) null else fallback(method)
            else -> fallback(method)
        }
    }

    private fun resolvePlayer(args: Array<out Any>?): OfflinePlayer? {
        if (args == null || args.isEmpty()) return null
        val first = args[0]
        return when (first) {
            is OfflinePlayer -> first
            is Player -> first as OfflinePlayer
            is String -> plugin.server.getOfflinePlayer(java.util.UUID.fromString(first))
            else -> null
        }
    }

    private fun getOnlinePlayer(offline: OfflinePlayer?): Player? {
        return offline?.player
    }

    private fun getAmount(args: Array<out Any>?): Double {
        if (args == null || args.size < 2) return 0.0
        return (args[1] as? Number)?.toDouble() ?: 0.0
    }

    private fun formatAmount(args: Array<out Any>?): String {
        val amount = getAmount(args)
        return "%.0f %s".format(amount, if (amount == 1.0) "coin" else "coins")
    }

    private fun hasBalance(args: Array<out Any>?): Boolean {
        val player = resolvePlayer(args) ?: return false
        val amount = getAmount(args)
        return getPlayerBalance(player) >= amount
    }

    private fun createAccount(args: Array<out Any>?): Boolean = true

    private fun getBalance(args: Array<out Any>?): Double {
        val player = resolvePlayer(args) ?: return 0.0
        return getPlayerBalance(player)
    }

    private fun withdrawPlayer(args: Array<out Any>?): Any? {
        val player = resolvePlayer(args) ?: return createResponse(false, 0.0, 0.0, "Jucator negasit")
        val amount = getAmount(args)
        val online = getOnlinePlayer(player)
        if (online == null) return createResponse(false, amount, getPlayerBalance(player), "Jucatorul este offline")
        val balance = getPlayerBalance(player)
        if (balance < amount) {
            return createResponse(false, amount, balance, "Fonduri insuficiente")
        }
        plugin.economyService.setBalance(online, (balance - amount).toInt())
        return createResponse(true, amount, (balance - amount), "")
    }

    private fun depositPlayer(args: Array<out Any>?): Any? {
        val player = resolvePlayer(args) ?: return createResponse(false, 0.0, 0.0, "Jucator negasit")
        val amount = getAmount(args)
        val online = getOnlinePlayer(player)
        if (online == null) return createResponse(false, amount, getPlayerBalance(player), "Jucatorul este offline")
        val balance = getPlayerBalance(player)
        plugin.economyService.setBalance(online, (balance + amount).toInt())
        return createResponse(true, amount, (balance + amount), "")
    }

    private fun getPlayerBalance(player: OfflinePlayer?): Double {
        if (player == null) return 0.0
        return plugin.economyService.getBalanceByUuid(player.uniqueId).toDouble()
    }

    private fun fallback(method: Method): Any? {
        return when (method.returnType.name) {
            "boolean" -> false
            "int" -> 0
            "double" -> 0.0
            "long" -> 0L
            else -> null
        }
    }

    private fun createResponse(success: Boolean, amount: Double, balance: Double, errorMessage: String): Any? {
        try {
            val economyClass = Class.forName("net.milkbowl.vault.economy.Economy")
            for (inner in economyClass.declaredClasses) {
                if (inner.simpleName == "EconomyResponse") {
                    val responseTypeClass = Class.forName("net.milkbowl.vault.economy.EconomyResponse\$ResponseType")
                    val responseType = if (success) {
                        responseTypeClass.enumConstants.first { it.toString() == "SUCCESS" }
                    } else {
                        responseTypeClass.enumConstants.first { it.toString() == "FAILURE" }
                    }
                    val constructor = inner.getDeclaredConstructor(
                        responseTypeClass, Double::class.javaPrimitiveType,
                        Double::class.javaPrimitiveType, String::class.java
                    )
                    constructor.isAccessible = true
                    return constructor.newInstance(responseType, amount, balance, errorMessage)
                }
            }
            return null
        } catch (e: Exception) {
            plugin.logger.fine("[Vault] Nu am putut crea EconomyResponse: ${e.message}")
            return null
        }
    }
}
