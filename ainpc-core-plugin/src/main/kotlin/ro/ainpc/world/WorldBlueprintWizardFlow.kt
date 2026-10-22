package ro.ainpc.world

import java.util.Locale

internal enum class WorldBlueprintWizardStep {
    STORY, PROPERTY, PROFESSION_LIMIT, PROFESSION, ARCHITECTURE, MAP_RULE, REVIEW,
}

internal data class WorldBlueprintWizardUpdate(
    val blueprint: WorldBlueprint,
    val nextStep: WorldBlueprintWizardStep,
)

internal object WorldBlueprintWizardFlow {
    private val removalPattern = Regex("^sterge\\s+([a-z][a-z0-9_-]{0,31})$", RegexOption.IGNORE_CASE)

    fun editStep(category: String): WorldBlueprintWizardStep? = when (category.lowercase(Locale.ROOT)) {
        "story" -> WorldBlueprintWizardStep.STORY
        "property" -> WorldBlueprintWizardStep.PROPERTY
        "profession-limit" -> WorldBlueprintWizardStep.PROFESSION_LIMIT
        "profession" -> WorldBlueprintWizardStep.PROFESSION
        "architecture" -> WorldBlueprintWizardStep.ARCHITECTURE
        "map-rule" -> WorldBlueprintWizardStep.MAP_RULE
        else -> null
    }

    fun currentSummary(blueprint: WorldBlueprint, step: WorldBlueprintWizardStep): String = when (step) {
        WorldBlueprintWizardStep.STORY -> blueprint.story.ifBlank { "necompletata" }.take(120)
        WorldBlueprintWizardStep.PROPERTY -> blueprint.properties.keys.sorted().joinToString().ifBlank { "niciuna" }.take(180)
        WorldBlueprintWizardStep.PROFESSION_LIMIT -> blueprint.professionTypeLimit.toString()
        WorldBlueprintWizardStep.PROFESSION -> blueprint.professions.keys.sorted().joinToString().ifBlank { "niciuna" }.take(180)
        WorldBlueprintWizardStep.ARCHITECTURE -> blueprint.architecture.ifBlank { "necompletata" }.take(120)
        WorldBlueprintWizardStep.MAP_RULE -> blueprint.mapRules.keys.sorted().joinToString().ifBlank { "niciuna" }.take(180)
        WorldBlueprintWizardStep.REVIEW -> "revizie"
    }

    fun firstMissing(blueprint: WorldBlueprint): WorldBlueprintWizardStep = when {
        blueprint.story.isBlank() -> WorldBlueprintWizardStep.STORY
        blueprint.properties.isEmpty() -> WorldBlueprintWizardStep.PROPERTY
        blueprint.professions.isEmpty() -> WorldBlueprintWizardStep.PROFESSION_LIMIT
        blueprint.architecture.isBlank() -> WorldBlueprintWizardStep.ARCHITECTURE
        blueprint.mapRules.isEmpty() -> WorldBlueprintWizardStep.MAP_RULE
        else -> WorldBlueprintWizardStep.REVIEW
    }

    fun prompt(step: WorldBlueprintWizardStep): String = when (step) {
        WorldBlueprintWizardStep.STORY -> "Scrie povestea lumii (maxim 4000 caractere)."
        WorldBlueprintWizardStep.PROPERTY -> "Scrie cheie: descriere, sterge <cheie> sau gata pentru pasul urmator."
        WorldBlueprintWizardStep.PROFESSION_LIMIT -> "Scrie limita tipurilor de meserii (1..64)."
        WorldBlueprintWizardStep.PROFESSION -> "Scrie id maxNpc descriere, sterge <id> sau gata pentru pasul urmator."
        WorldBlueprintWizardStep.ARCHITECTURE -> "Descrie stilul arhitectural (maxim 2000 caractere)."
        WorldBlueprintWizardStep.MAP_RULE -> "Scrie cheie: descriere, sterge <cheie> sau gata pentru revizie."
        WorldBlueprintWizardStep.REVIEW -> "Draft complet. Revizuieste-l cu show, inspect, validate, plan si review; nu este aplicat."
    }

    fun advance(blueprint: WorldBlueprint, step: WorldBlueprintWizardStep, input: String): WorldBlueprintWizardUpdate {
        val text = input.trim()
        require(text.isNotBlank()) { "Raspunsul nu poate fi gol." }
        val ready = text.equals("gata", ignoreCase = true)
        val entryToRemove = removalKey(text)
        val update = when (step) {
            WorldBlueprintWizardStep.STORY -> WorldBlueprintWizardUpdate(
                blueprint.copy(story = text), WorldBlueprintWizardStep.PROPERTY,
            )
            WorldBlueprintWizardStep.PROPERTY -> if (ready) {
                require(blueprint.properties.isNotEmpty()) { "Adauga cel putin o proprietate inainte de gata." }
                WorldBlueprintWizardUpdate(blueprint, WorldBlueprintWizardStep.PROFESSION_LIMIT)
            } else if (entryToRemove != null) {
                require(entryToRemove in blueprint.properties) { "Proprietatea $entryToRemove nu exista." }
                WorldBlueprintWizardUpdate(blueprint.copy(properties = blueprint.properties - entryToRemove), step)
            } else {
                val (key, value) = keyedText(text)
                WorldBlueprintWizardUpdate(blueprint.copy(properties = blueprint.properties + (key to value)), step)
            }
            WorldBlueprintWizardStep.PROFESSION_LIMIT -> {
                val limit = text.toIntOrNull() ?: throw IllegalArgumentException("Limita trebuie sa fie un numar intre 1 si 64.")
                WorldBlueprintWizardUpdate(blueprint.copy(professionTypeLimit = limit), WorldBlueprintWizardStep.PROFESSION)
            }
            WorldBlueprintWizardStep.PROFESSION -> if (ready) {
                require(blueprint.professions.isNotEmpty()) { "Adauga cel putin o meserie inainte de gata." }
                WorldBlueprintWizardUpdate(blueprint, WorldBlueprintWizardStep.ARCHITECTURE)
            } else if (entryToRemove != null) {
                require(entryToRemove in blueprint.professions) { "Meseria $entryToRemove nu exista." }
                WorldBlueprintWizardUpdate(blueprint.copy(professions = blueprint.professions - entryToRemove), step)
            } else {
                val parts = text.split(Regex("\\s+"), limit = 3)
                require(parts.size == 3) { "Format: id maxNpc descriere." }
                val key = parts[0].lowercase(Locale.ROOT)
                require(WorldBlueprintPolicy.validKey(key)) { "ID-ul meseriei este invalid." }
                val maxNpcs = parts[1].toIntOrNull() ?: throw IllegalArgumentException("maxNpc trebuie sa fie intre 0 si 512.")
                WorldBlueprintWizardUpdate(blueprint.copy(professions = blueprint.professions +
                    (key to WorldProfessionBlueprint(maxNpcs, parts[2]))), step)
            }
            WorldBlueprintWizardStep.ARCHITECTURE -> WorldBlueprintWizardUpdate(
                blueprint.copy(architecture = text), WorldBlueprintWizardStep.MAP_RULE,
            )
            WorldBlueprintWizardStep.MAP_RULE -> if (ready) {
                require(blueprint.mapRules.isNotEmpty()) { "Adauga cel putin o regula inainte de gata." }
                WorldBlueprintWizardUpdate(blueprint, WorldBlueprintWizardStep.REVIEW)
            } else if (entryToRemove != null) {
                require(entryToRemove in blueprint.mapRules) { "Regula $entryToRemove nu exista." }
                WorldBlueprintWizardUpdate(blueprint.copy(mapRules = blueprint.mapRules - entryToRemove), step)
            } else {
                val (key, value) = keyedText(text)
                WorldBlueprintWizardUpdate(blueprint.copy(mapRules = blueprint.mapRules + (key to value)), step)
            }
            WorldBlueprintWizardStep.REVIEW -> throw IllegalArgumentException("Wizard-ul este deja complet.")
        }
        val issues = WorldBlueprintPolicy.draftIssues(update.blueprint)
        require(issues.isEmpty()) { issues.joinToString("; ") }
        return update
    }

    private fun keyedText(text: String): Pair<String, String> {
        val pieces = text.split(':', limit = 2)
        require(pieces.size == 2) { "Format: cheie: descriere." }
        val key = pieces[0].trim().lowercase(Locale.ROOT)
        val value = pieces[1].trim()
        require(WorldBlueprintPolicy.validKey(key)) { "Cheia trebuie sa foloseasca litere mici, cifre, _ sau -." }
        require(value.isNotBlank()) { "Descrierea nu poate fi goala." }
        return key to value
    }

    private fun removalKey(text: String): String? = removalPattern.matchEntire(text)
        ?.groupValues?.get(1)?.lowercase(Locale.ROOT)
}
