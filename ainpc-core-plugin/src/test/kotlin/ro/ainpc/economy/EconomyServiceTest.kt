package ro.ainpc.economy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EconomyServiceTest {

    @Test
    fun economyServiceTransfers() {
        val svc = FakeEconomyService()
        assertEquals(0, svc.getBalance("player1"))
        assertEquals(0, svc.getBalance("player2"))

        svc.deposit("player1", 100)
        assertEquals(100, svc.getBalance("player1"))

        assertTrue(svc.transfer("player1", "player2", 30))
        assertEquals(70, svc.getBalance("player1"))
        assertEquals(30, svc.getBalance("player2"))
    }

    @Test
    fun economyServiceWithdrawFailsWhenInsufficient() {
        val svc = FakeEconomyService()
        svc.deposit("player1", 50)
        assertFalse(svc.withdraw("player1", 100))
        assertEquals(50, svc.getBalance("player1"))
    }

    @Test
    fun economyServiceSetBalance() {
        val svc = FakeEconomyService()
        svc.setBalance("player1", 250)
        assertEquals(250, svc.getBalance("player1"))
    }

    @Test
    fun economyServiceDepositNegativeDoesNothing() {
        val svc = FakeEconomyService()
        assertFalse(svc.deposit("player1", -10))
        assertEquals(0, svc.getBalance("player1"))
    }

    @Test
    fun economyServiceWithdrawNegativeDoesNothing() {
        val svc = FakeEconomyService()
        assertFalse(svc.withdraw("player1", -10))
        assertEquals(0, svc.getBalance("player1"))
    }

    @Test
    fun economyServiceBalanceStaysNonNegative() {
        val svc = FakeEconomyService()
        svc.setBalance("player1", -5)
        assertTrue(svc.getBalance("player1") >= 0)
    }
}

class FakeEconomyService {
    private val balances = mutableMapOf<String, Int>()

    fun getBalance(player: String): Int = balances.getOrDefault(player, 0)

    fun setBalance(player: String, amount: Int) {
        balances[player] = amount.coerceAtLeast(0)
    }

    fun deposit(player: String, amount: Int): Boolean {
        if (amount <= 0) return false
        balances[player] = getBalance(player) + amount
        return true
    }

    fun withdraw(player: String, amount: Int): Boolean {
        if (amount <= 0) return false
        val current = getBalance(player)
        if (current < amount) return false
        balances[player] = current - amount
        return true
    }

    fun transfer(from: String, to: String, amount: Int): Boolean {
        if (withdraw(from, amount)) {
            deposit(to, amount)
            return true
        }
        return false
    }
}
