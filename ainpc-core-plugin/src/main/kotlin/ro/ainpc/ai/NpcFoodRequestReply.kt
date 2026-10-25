package ro.ainpc.ai

import ro.ainpc.npc.NpcFoodRequestPreferences

internal object NpcFoodRequestReply {
    private val declines = setOf(
        "nu pot acum", "nu te pot ajuta acum", "nu pot sa te ajut acum",
        "nu am mancare", "nu am paine", "nu am mere",
    )
    private val promises = setOf(
        "iti aduc mancare", "iti aduc paine", "iti aduc un mar", "vin cu mancare", "te ajut cu mancare",
    )

    fun parse(message: String, aliases: List<String>): NpcFoodRequestPreferences.Reply? {
        if (message.length > 200 || '?' in message || DialogueMessageText.hasQuotedSpan(message)) return null
        val normalized = DialogueMessageText.normalize(message).trimEnd('.', '!', ' ')
        val bodies = listOf(normalized) + aliases.map(DialogueMessageText::normalize)
            .filter(String::isNotBlank).mapNotNull { alias ->
                val address = Regex("^${Regex.escape(alias)}\\s*,\\s*").find(normalized) ?: return@mapNotNull null
                normalized.substring(address.range.last + 1)
            }
        return bodies.firstNotNullOfOrNull { body ->
            when (body) {
                in declines -> NpcFoodRequestPreferences.Reply.DECLINE
                in promises -> NpcFoodRequestPreferences.Reply.PROMISE
                else -> null
            }
        }
    }
}
