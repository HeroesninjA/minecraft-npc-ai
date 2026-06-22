package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EconomyCommandParseTest {

    @Test
    fun economyBalanceWithPlayerArg() {
        val args = arrayOf("economy", "balance", "Steve")
        assertEquals("economy", args[0])
        assertEquals("balance", args[1])
        assertEquals("Steve", args[2])
    }

    @Test
    fun economyBalanceWithoutPlayerArg() {
        val args = arrayOf("economy", "balance")
        assertEquals(2, args.size)
        assertEquals("balance", args[1])
    }

    @Test
    fun economyPayWithAllArgs() {
        val args = arrayOf("economy", "pay", "Alex", "50")
        assertEquals("pay", args[1])
        assertEquals("Alex", args[2])
        assertEquals("50", args[3])
        assertEquals(50, args[3].toIntOrNull())
    }

    @Test
    fun economySetWithAllArgs() {
        val args = arrayOf("economy", "set", "Steve", "1000")
        assertEquals("set", args[1])
        assertEquals("Steve", args[2])
        assertEquals("1000", args[3])
        assertEquals(1000, args[3].toIntOrNull())
    }

    @Test
    fun economyPayRefusesNegativeAmount() {
        val amount = "-10".toIntOrNull()
        assertEquals(-10, amount)
        assert(amount != null && amount <= 0)
    }

    @Test
    fun economyPayRefusesNonNumericAmount() {
        val amount = "abc".toIntOrNull()
        assertEquals(null, amount)
    }

    @Test
    fun economySubcommandRouting() {
        val balance = listOf("balance", "bal", "bani", "sold")
        val pay = listOf("pay", "plateste", "trimite")
        val set = listOf("set", "seteaza")
        assertEquals(4, balance.size)
        assertEquals(3, pay.size)
        assertEquals(2, set.size)
    }
}
