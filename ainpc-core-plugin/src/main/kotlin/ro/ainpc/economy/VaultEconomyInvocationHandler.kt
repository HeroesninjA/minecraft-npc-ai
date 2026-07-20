package ro.ainpc.economy

import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.util.Locale
import java.util.UUID

class VaultEconomyInvocationHandler(private val plugin: AINPCPlugin) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        return when (method.name) {
            "isEnabled" -> plugin.isEnabled
            "getName" -> "AINPC Economy"
            "currencyNameSingular" -> "coin"
            "currencyNamePlural" -> "coins"
            "format" -> formatVaultAmount((args?.firstOrNull() as? Number)?.toDouble() ?: 0.0)
            "fractionalDigits" -> 0
            "has" -> hasBalance(args)
            "hasAccount" -> true
            "createPlayerAccount" -> true
            "getBalance" -> getBalance(args)
            "withdrawPlayer" -> withdrawPlayer(args)
            "depositPlayer" -> depositPlayer(args)
            "bankBalance", "bankDeposit", "bankHas", "bankWithdraw",
            "createBank", "deleteBank", "isBankMember", "isBankOwner" ->
                createResponse(false, 0.0, 0.0, "Operatiunile bancare nu sunt suportate")
            "getBanks" -> emptyList<String>()
            "hasBankSupport" -> false
            "toString" -> "AINPC Economy Vault Bridge"
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> args?.firstOrNull() === proxy
            else -> fallback(method)
        }
    }

    @Suppress("DEPRECATION")
    private fun resolvePlayer(args: Array<out Any>?): OfflinePlayer? {
        val first = args?.firstOrNull() ?: return null
        return when (first) {
            is OfflinePlayer -> first
            is String -> {
                val value = first.trim()
                if (value.isEmpty()) {
                    null
                } else {
                    runCatching { UUID.fromString(value) }
                        .fold(
                            onSuccess = { plugin.server.getOfflinePlayer(it) },
                            onFailure = { plugin.server.getOfflinePlayer(value) }
                        )
                }
            }
            else -> null
        }
    }

    private fun getOnlinePlayer(offline: OfflinePlayer?): Player? = offline?.player

    private fun hasBalance(args: Array<out Any>?): Boolean {
        val player = resolvePlayer(args) ?: return false
        val amount = parseVaultWholeAmount(args) ?: return false
        return getPlayerBalance(player) >= amount
    }

    private fun getBalance(args: Array<out Any>?): Double {
        val player = resolvePlayer(args) ?: return 0.0
        return getPlayerBalance(player).toDouble()
    }

    private fun withdrawPlayer(args: Array<out Any>?): Any? {
        val player = resolvePlayer(args) ?: return createResponse(false, 0.0, 0.0, "Jucator negasit")
        val amount = parseVaultWholeAmount(args)
            ?: return createResponse(false, 0.0, getPlayerBalance(player).toDouble(), "Suma invalida")
        val online = getOnlinePlayer(player)
            ?: return createResponse(false, amount.toDouble(), getPlayerBalance(player).toDouble(), "Jucatorul este offline")
        val balance = getPlayerBalance(player)
        if (balance < amount) {
            return createResponse(false, amount.toDouble(), balance.toDouble(), "Fonduri insuficiente")
        }
        val newBalance = balance - amount
        plugin.economyService.setBalance(online, newBalance)
        return createResponse(true, amount.toDouble(), newBalance.toDouble(), "")
    }

    private fun depositPlayer(args: Array<out Any>?): Any? {
        val player = resolvePlayer(args) ?: return createResponse(false, 0.0, 0.0, "Jucator negasit")
        val amount = parseVaultWholeAmount(args)
            ?: return createResponse(false, 0.0, getPlayerBalance(player).toDouble(), "Suma invalida")
        val online = getOnlinePlayer(player)
            ?: return createResponse(false, amount.toDouble(), getPlayerBalance(player).toDouble(), "Jucatorul este offline")
        val balance = getPlayerBalance(player)
        val newBalance = balance.toLong() + amount.toLong()
        if (newBalance > Int.MAX_VALUE) {
            return createResponse(false, amount.toDouble(), balance.toDouble(), "Soldul depaseste limita suportata")
        }
        plugin.economyService.setBalance(online, newBalance.toInt())
        return createResponse(true, amount.toDouble(), newBalance.toDouble(), "")
    }

    private fun getPlayerBalance(player: OfflinePlayer): Int =
        plugin.economyService.getBalanceByUuid(player.uniqueId)

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
        return try {
            createVaultEconomyResponse(
                responseTypeName = if (success) "SUCCESS" else "FAILURE",
                amount = amount,
                balance = balance,
                errorMessage = errorMessage
            )
        } catch (exception: ReflectiveOperationException) {
            plugin.logger.fine("[Vault] Nu am putut crea EconomyResponse: ${exception.message}")
            null
        }
    }
}

internal fun formatVaultAmount(amount: Double): String =
    String.format(Locale.ROOT, "%.0f %s", amount, if (amount == 1.0) "coin" else "coins")

internal fun parseVaultWholeAmount(args: Array<out Any>?): Int? {
    val amount = args
        ?.drop(1)
        ?.firstOrNull { it is Number }
        ?.let { (it as Number).toDouble() }
        ?: return null
    if (!amount.isFinite() || amount < 0.0 || amount > Int.MAX_VALUE || amount % 1.0 != 0.0) {
        return null
    }
    return amount.toInt()
}

internal fun createVaultEconomyResponse(
    responseTypeName: String,
    amount: Double,
    balance: Double,
    errorMessage: String,
    classLoader: ClassLoader = VaultEconomyInvocationHandler::class.java.classLoader,
    responseClassName: String = "net.milkbowl.vault.economy.EconomyResponse",
    responseTypeClassName: String = "net.milkbowl.vault.economy.EconomyResponse\$ResponseType",
): Any {
    val responseTypeClass = Class.forName(responseTypeClassName, true, classLoader)
    val responseType = responseTypeClass.enumConstants.first {
        (it as Enum<*>).name == responseTypeName
    }
    val responseClass = Class.forName(responseClassName, true, classLoader)
    val constructor = responseClass.getDeclaredConstructor(
        Double::class.javaPrimitiveType,
        Double::class.javaPrimitiveType,
        responseTypeClass,
        String::class.java
    )
    constructor.isAccessible = true
    return constructor.newInstance(amount, balance, responseType, errorMessage)
}
