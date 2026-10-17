package ro.ainpc.ai

import java.util.UUID

internal data class DialogueAddressCandidate(
    val npcId: UUID,
    val name: String,
    val displayName: String? = null,
)

internal enum class DialogueAddressStatus {
    NONE,
    UNIQUE,
    AMBIGUOUS,
}

internal data class DialogueAddressResolution(
    val status: DialogueAddressStatus,
    val npcId: UUID? = null,
    val matchedNpcIds: Set<UUID> = emptySet(),
)

internal object DialogueAddressResolver {
    private val directClause = Regex(
        "^(?:salut|hei|noroc|servus|buna|multumesc|mersi|tu|esti|te|iti|ce|cum|unde|cand|de ce|poti|ai|vrei)\\b",
    )
    private val reportedSpeech = Regex("\\b(?:a|ai|am|au)\\s+(?:spus|zis)\\b|\\b(?:spune|spunea|zice|zicea)\\b")

    fun resolve(message: String, candidates: List<DialogueAddressCandidate>): UUID? =
        resolveDetailed(message, candidates).npcId

    fun resolveDetailed(message: String, candidates: List<DialogueAddressCandidate>): DialogueAddressResolution {
        val normalized = DialogueMessageText.normalize(message)
        if (',' !in normalized) return DialogueAddressResolution(DialogueAddressStatus.NONE)

        val aliases = candidates.flatMap { candidate ->
            listOfNotNull(candidate.name, candidate.displayName)
                .map(DialogueMessageText::normalize)
                .filter(String::isNotBlank)
                .map { candidate.npcId to it }
        }.distinct()
        val addressedIds = mutableSetOf<UUID>()
        var body = normalized
        while (true) {
            val leadingMatches = aliases.mapNotNull { (npcId, alias) ->
                Regex("^${Regex.escape(alias)}\\s*,\\s*(?=\\S)").find(body)
                    ?.let { npcId to it.range.last + 1 }
            }
            if (leadingMatches.isEmpty()) break
            addressedIds.addAll(leadingMatches.map { it.first })
            body = body.substring(leadingMatches.maxOf { it.second })
        }
        val directFarewell = DialogueFarewell.isFarewell(body.substringBeforeLast(','))
        if ((directClause.containsMatchIn(body) || directFarewell) && !reportedSpeech.containsMatchIn(body)) {
            aliases.forEach { (npcId, alias) ->
                if (Regex(",\\s*${Regex.escape(alias)}\\s*[.!?]*$").containsMatchIn(body)) {
                    addressedIds.add(npcId)
                }
            }
        }
        return when (addressedIds.size) {
            0 -> DialogueAddressResolution(DialogueAddressStatus.NONE)
            1 -> DialogueAddressResolution(DialogueAddressStatus.UNIQUE, addressedIds.single(), addressedIds.toSet())
            else -> DialogueAddressResolution(DialogueAddressStatus.AMBIGUOUS, matchedNpcIds = addressedIds.toSet())
        }
    }
}
