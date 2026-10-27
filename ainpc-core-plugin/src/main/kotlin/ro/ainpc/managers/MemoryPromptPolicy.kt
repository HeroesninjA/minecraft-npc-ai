package ro.ainpc.managers

import com.google.gson.JsonPrimitive

internal object MemoryPromptPolicy {
    const val MAX_ENTRY_CHARS = 500

    fun normalizeEntry(text: String): String = text.replace('\n', ' ').replace('\r', ' ').trim()

    fun renderEntry(normalizedEntry: String): String = JsonPrimitive(normalizedEntry).toString()
}
