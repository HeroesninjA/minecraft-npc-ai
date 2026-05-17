package ro.ainpc.topology

class TopologyConsensus(
    val category: TopologyCategory,
    val descriptions: List<String>,
    val biomes: List<String>,
    val dialogueHints: List<String>,
    val suggestedTraits: List<String>,
    val sourcePacks: List<String>
) {
    fun toPromptBlock(): String {
        val builder = StringBuilder()
        builder.append("Topologie: ").append(category.displayName).append(".\n")

        if (descriptions.isNotEmpty()) {
            builder.append("Consens mediu: ")
                .append(descriptions.take(2).joinToString(" "))
                .append("\n")
        }

        if (dialogueHints.isNotEmpty()) {
            builder.append("Hint-uri dialog: ")
                .append(dialogueHints.take(3).joinToString(", "))
                .append("\n")
        }

        if (suggestedTraits.isNotEmpty()) {
            builder.append("Trasaturi potrivite: ")
                .append(suggestedTraits.take(4).joinToString(", "))
                .append("\n")
        }

        if (biomes.isNotEmpty()) {
            builder.append("Biome relevante: ")
                .append(biomes.take(5).joinToString(", "))
                .append("\n")
        }

        return builder.toString()
    }
}
