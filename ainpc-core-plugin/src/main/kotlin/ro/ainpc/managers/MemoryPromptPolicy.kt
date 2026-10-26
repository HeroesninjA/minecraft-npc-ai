package ro.ainpc.managers

internal object MemoryPromptPolicy {
    const val MAX_ENTRY_CHARS = 500

    fun normalizeEntry(text: String): String = text.replace('\n', ' ').replace('\r', ' ').trim()
}
