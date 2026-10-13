package ro.ainpc.ai

import ro.ainpc.story.PlaceStoryState
import ro.ainpc.story.RegionStoryState
import ro.ainpc.story.StoryEvent
import ro.ainpc.story.StoryStateViewResolver
import ro.ainpc.world.WorldContextSnapshot

internal object StoryFactResolver {
    data class Facts(
        val regionState: String = "",
        val placeState: String = "",
        val placeHistory: String = "",
        val recentEventTitles: List<String> = emptyList(),
        val regionDescription: String = "",
        val placeDescription: String = "",
        val regionName: String = "",
        val placeName: String = "",
    )

    fun isLookupQuestion(playerMessage: String, facts: Facts = Facts()): Boolean {
        val message = DialogueMessageText.normalize(playerMessage)
        return wantsCurrentState(message, facts) || wantsHistory(message)
    }

    fun wantsHistory(playerMessage: String): Boolean {
        val message = DialogueMessageText.normalize(playerMessage)
        val refersToLocalWorld = DialogueMessageText.matches(
            message, "sat", "satul", "satului", "regiune", "regiunii", "loc", "locului",
            "cladire", "cladirii", "zona", "zonei", "aici", "acestui loc"
        )
        val asksAboutPast = DialogueMessageText.matches(
            message, "istorie", "istoria", "istoric", "istoricul", "history", "trecut", "trecutul",
            "s-a intamplat", "s a intamplat"
        )
        return (asksAboutPast && refersToLocalWorld) ||
            (DialogueMessageText.matches(message, "eveniment", "evenimente") &&
            !DialogueMessageText.matches(message, "acum", "activ", "active", "in prezent", "azi", "astazi"))
    }

    fun fromWorldContext(
        worldContext: WorldContextSnapshot,
        persistentRegionState: RegionStoryState?,
        persistentPlaceState: PlaceStoryState?,
        recentEvents: List<StoryEvent>,
    ): Facts {
        val region = worldContext.currentRegion()
        val place = worldContext.currentPlace()
        val effectiveRegion = StoryStateViewResolver.region(region, persistentRegionState)
        val effectivePlace = StoryStateViewResolver.place(place, persistentPlaceState)
        return Facts(
            regionState = cleanState(effectiveRegion?.stateKey),
            placeState = cleanState(effectivePlace?.stateKey),
            placeHistory = cleanText(place?.metadata()?.get("history")),
            recentEventTitles = recentEvents.mapNotNull { cleanText(it.title()).takeIf(String::isNotBlank) }.take(2),
            regionDescription = effectiveRegion?.description.orEmpty(),
            placeDescription = effectivePlace?.description.orEmpty(),
            regionName = cleanText(region?.name(), 80),
            placeName = cleanText(place?.displayName(), 80),
        )
    }

    fun fromSemanticLines(historyLines: List<String>, loreLines: List<String> = emptyList()): Facts {
        val regionLine = historyLines.firstOrNull { it.startsWith("- region_story_mode=") }
        val placeStateLine = historyLines.firstOrNull { it.startsWith("- place_story_state=") }
        val placeHistoryLine = historyLines.firstOrNull { it.startsWith("- place_history=") }
        val recentEventsIndex = historyLines.indexOf("- recent_events:")
        val eventTitles = if (recentEventsIndex < 0) emptyList() else historyLines.drop(recentEventsIndex + 1)
            .filter { it.startsWith("- ") }
            .mapNotNull { cleanText(it.substringAfter(" - ", "")).takeIf(String::isNotBlank) }
            .take(2)
        return Facts(
            regionState = cleanState(field(regionLine, "state")),
            placeState = cleanState(field(placeStateLine, "place_story_state")),
            placeHistory = cleanText(placeHistoryLine?.substringAfter(", history=", "")),
            recentEventTitles = eventTitles,
            regionDescription = cleanText(historyLines.firstOrNull { it.startsWith("- region_story_description=") }
                ?.substringAfter("=")),
            placeDescription = cleanText(historyLines.firstOrNull { it.startsWith("- place_story_description=") }
                ?.substringAfter("=")),
            regionName = loreName(loreLines, "- region="),
            placeName = loreName(loreLines, "- current_place="),
        )
    }

    fun resolve(playerMessage: String, facts: Facts): String? {
        val message = DialogueMessageText.normalize(playerMessage)
        val stateAnswer = if (wantsCurrentState(message, facts)) {
            val naturalSituation = asksAboutLocalSituation(message, facts)
            val wantsPlace = DialogueMessageText.matches(message, "starea locului", "starea cladirii") ||
                (naturalSituation && refersToPlace(message, facts) && !refersToRegion(message, facts))
            val wantsRegion = DialogueMessageText.matches(message, "starea regiunii", "starea satului") ||
                (naturalSituation && refersToRegion(message, facts) && !refersToPlace(message, facts))
            listOfNotNull(
                facts.regionState.takeIf {
                    it.isNotBlank() && !wantsPlace && (!naturalSituation || facts.regionDescription.isNotBlank())
                }
                    ?.let { currentStateAnswer("regiunii", it, facts.regionDescription) },
                facts.placeState.takeIf {
                    it.isNotBlank() && !wantsRegion && (!naturalSituation || facts.placeDescription.isNotBlank())
                }
                    ?.let { currentStateAnswer("locului", it, facts.placeDescription) },
            ).joinToString(" ").takeIf(String::isNotBlank)
        } else null
        val wantsEvents = DialogueMessageText.matches(message, "eveniment", "evenimente")
        val history = if (wantsHistory(message)) historyAnswer(facts, includePlaceHistory = !wantsEvents) else null
        return listOfNotNull(stateAnswer, history).joinToString(" ").takeIf(String::isNotBlank)
    }

    fun historyAnswer(facts: Facts, includePlaceHistory: Boolean = true): String? = listOfNotNull(
        facts.placeHistory.takeIf { includePlaceHistory && it.isNotBlank() }
            ?.let { "Istoria locului: $it." },
        facts.recentEventTitles.takeIf { it.isNotEmpty() }
            ?.let { "Eveniment recent: ${it.joinToString("; ")}." },
    ).joinToString(" ").takeIf(String::isNotBlank)

    fun wantsCurrentState(playerMessage: String, facts: Facts = Facts()): Boolean {
        val message = DialogueMessageText.normalize(playerMessage)
        if (DialogueMessageText.matches(
                message, "ieri", "maine", "poimaine", "a fost", "au fost", "era", "erau",
                "va fi", "vor fi", "va deveni", "de ce", "cand"
            )) return false
        return DialogueMessageText.matches(
            message, "starea povestii", "starea story", "starea regiunii", "starea locului",
            "starea cladirii", "starea satului"
        ) || asksAboutLocalSituation(message, facts)
    }

    private fun asksAboutLocalSituation(message: String, facts: Facts): Boolean =
        DialogueMessageText.isQuestion(message) &&
            DialogueMessageText.matches(
                message, "ce se intampla", "ce se petrece", "cum merg lucrurile",
                "cum stau lucrurile", "cum e situatia", "cum este situatia"
            ) &&
            (refersToRegion(message, facts) || refersToPlace(message, facts) ||
                DialogueMessageText.matches(message, "aici")) &&
            !DialogueMessageText.matches(message, "ce se intampla cu", "ce se petrece cu")

    private fun refersToRegion(message: String, facts: Facts): Boolean =
        mentionsCurrentName(message, facts.regionName) || REGION_SCOPE.containsMatchIn(message)

    private fun refersToPlace(message: String, facts: Facts): Boolean =
        mentionsCurrentName(message, facts.placeName) || PLACE_SCOPE.containsMatchIn(message)

    private fun mentionsCurrentName(message: String, name: String): Boolean {
        val normalizedName = DialogueMessageText.normalize(name)
        return normalizedName.length >= 4 && normalizedName != "necunoscut" &&
            DialogueMessageText.references(message, normalizedName)
    }

    private val REGION_SCOPE = Regex(
        "\\b(?:sat|satul|satului|regiune|regiunea|regiunii|zona|zonei)" +
            "(?:\\s+(?:acum|azi|astazi|acesta|aceasta))?\\s*[?!.,]?$"
    )

    private val PLACE_SCOPE = Regex(
        "\\b(?:loc|locul|locului|cladire|cladirea|cladirii)" +
            "(?:\\s+(?:acum|azi|astazi|acesta|aceasta))?\\s*[?!.,]?$"
    )

    private fun field(line: String?, key: String): String = line?.substringAfter("$key=", "")
        ?.substringBefore(",")?.trim().orEmpty()

    private fun loreName(lines: List<String>, prefix: String): String = cleanText(
        lines.firstOrNull { it.startsWith(prefix) }
            ?.substringAfter(", name=", "")?.substringBefore(", type="), 80,
    )

    private fun currentStateAnswer(scope: String, state: String, description: String): String {
        val text = cleanText(description)
        if (text.isBlank()) return "Starea curenta a $scope este $state."
        val punctuation = if (text.last() in ".!?…") "" else "."
        return "Starea curenta a $scope: $text$punctuation"
    }

    private fun cleanState(value: String?): String = cleanText(value, 80)

    private fun cleanText(value: String?, maxLength: Int = 200): String = value.orEmpty()
        .takeUnless { it == "<none>" }.orEmpty().trim().replace(Regex("\\s+"), " ")
        .take(maxLength).trim()
}
