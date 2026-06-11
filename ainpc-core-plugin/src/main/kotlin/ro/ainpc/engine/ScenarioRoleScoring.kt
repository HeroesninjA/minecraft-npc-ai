package ro.ainpc.engine

import java.util.Locale

fun scoreBoolean(condition: Boolean, positiveScore: Int): Int =
    if (condition) positiveScore else 0

fun normalizeScenarioToken(value: String?): String =
    value?.trim()?.lowercase(Locale.ROOT) ?: ""
