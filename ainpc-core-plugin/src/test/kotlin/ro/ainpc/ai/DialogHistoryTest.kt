package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DialogHistoryTest {
    @Test
    fun `only stable complete exchanges become conversation context`() {
        val history = DialogHistory.BranchStatus.entries.map { status ->
            DialogHistory("mesaj $status", "raspuns $status", 1L, status)
        } + listOf(DialogHistory("", "raspuns", 2L), DialogHistory("mesaj", " \n\t", 3L))

        assertEquals(
            listOf(DialogHistory.BranchStatus.SELECTED, DialogHistory.BranchStatus.EXECUTED),
            DialogHistory.recentStable(history).map { it.branchStatus }
        )
        assertTrue(DialogHistory.recentStable(null).isEmpty())
    }

    @Test
    fun `latest five complete exchanges are ordered after invalid entries are discarded`() {
        val valid = (1..8).map { index -> DialogHistory("mesaj $index", "raspuns $index", index.toLong()) }
        val failed = (9..14).map { index ->
            DialogHistory("mesaj $index", "raspuns $index", index.toLong(), DialogHistory.BranchStatus.FAILED)
        }

        assertEquals(listOf(4L, 5L, 6L, 7L, 8L), DialogHistory.recentStable((valid + failed).reversed()).map { it.timestamp })
    }

    @Test
    fun `equal timestamps preserve supplied order and caller list is not retained`() {
        val first = DialogHistory("primul", "prima replica", 1L)
        val second = DialogHistory("al doilea", "a doua replica", 1L)
        val history = mutableListOf(first, second)

        val snapshot = DialogHistory.recentStable(history)
        history.clear()

        assertEquals(listOf(first, second), snapshot)
    }
}
