package ro.ainpc.economy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class VaultEconomyInvocationHandlerTest {
    @Test
    fun formatsTheSingleVaultAmountArgument() {
        assertEquals("1 coin", formatVaultAmount(1.0))
        assertEquals("12 coins", formatVaultAmount(12.0))
    }

    @Test
    fun parsesWholeAmountsFromRegularAndWorldOverloads() {
        assertEquals(7, parseVaultWholeAmount(arrayOf<Any>("player", 7.0)) ?: -1)
        assertEquals(9, parseVaultWholeAmount(arrayOf<Any>("player", "world", 9.0)) ?: -1)
        assertNull(parseVaultWholeAmount(arrayOf<Any>("player", -1.0)))
        assertNull(parseVaultWholeAmount(arrayOf<Any>("player", 1.5)))
        assertNull(parseVaultWholeAmount(arrayOf<Any>("player", Double.NaN)))
    }

    @Test
    fun createsEconomyResponseWithVaultConstructorOrder() {
        val response = createVaultEconomyResponse(
            responseTypeName = "SUCCESS",
            amount = 4.0,
            balance = 19.0,
            errorMessage = "",
            classLoader = FakeEconomyResponse::class.java.classLoader,
            responseClassName = FakeEconomyResponse::class.java.name,
            responseTypeClassName = FakeResponseType::class.java.name
        ) as FakeEconomyResponse

        assertEquals(4.0, response.amount)
        assertEquals(19.0, response.balance)
        assertEquals(FakeResponseType.SUCCESS, response.type)
        assertEquals("", response.errorMessage)
    }

    private class FakeEconomyResponse(
        val amount: Double,
        val balance: Double,
        val type: FakeResponseType,
        val errorMessage: String,
    )

    private enum class FakeResponseType {
        SUCCESS,
        FAILURE,
    }
}
