package ro.ainpc.engine

import java.util.concurrent.ThreadLocalRandom

internal object DialogueTemplateRotation {
    fun choose(templates: List<String>, recent: MutableList<String>): String {
        require(templates.isNotEmpty())
        synchronized(recent) {
            val previous = recent.lastOrNull()
            var available = templates.filterNot { it in recent }
            if (available.isEmpty()) {
                recent.clear()
                available = templates.filterNot { it == previous }.ifEmpty { templates }
            }
            val selected = available[ThreadLocalRandom.current().nextInt(available.size)]
            recent.add(selected)
            if (recent.size > 5) recent.removeAt(0)
            return selected
        }
    }
}
