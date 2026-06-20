package ro.ainpc.world.fixture

class FixtureSemanticContext(
    villageType: String?,
    historySummary: String?,
    primaryProblem: String?,
    secondaryProblem: String?,
    publicMood: String?,
    outsidePressure: String?,
    socialTensions: List<FixtureSocialTension>?,
    npcRelations: List<FixtureNpcRelation>?,
    knownRumors: List<FixtureRumor>?,
    lockedKnowledge: List<FixtureLockedKnowledge>?
) {
    val villageType: String = villageType ?: "frontier_hamlet"
    val historySummary: String = historySummary ?: ""
    val primaryProblem: String = primaryProblem ?: ""
    val secondaryProblem: String = secondaryProblem ?: ""
    val publicMood: String = publicMood ?: "cautious"
    val outsidePressure: String = outsidePressure ?: ""
    val socialTensions: List<FixtureSocialTension> = socialTensions ?: emptyList()
    val npcRelations: List<FixtureNpcRelation> = npcRelations ?: emptyList()
    val knownRumors: List<FixtureRumor> = knownRumors ?: emptyList()
    val lockedKnowledge: List<FixtureLockedKnowledge> = lockedKnowledge ?: emptyList()
}

data class FixtureSocialTension(
    val tensionId: String,
    val participants: List<String>,
    val state: String
)

data class FixtureNpcRelation(
    val fromNpcId: String,
    val toNpcId: String,
    val relationType: String,
    val intensity: String,
    val reason: String,
    val visibleToPlayer: Boolean
)

data class FixtureRumor(
    val rumorId: String,
    val sourceNpcId: String,
    val targetPlaceId: String
)

data class FixtureLockedKnowledge(
    val knowledgeId: String,
    val unlockCondition: String
)

class FixtureSemanticContextBuilder {
    fun build(prefix: String): FixtureSemanticContext {
        val p = prefix
        return FixtureSemanticContext(
            villageType = "frontier_hamlet",
            historySummary = "Sat mic aparut langa un drum vechi si o fantana uitata. Provizii lipsa si zvonuri despre miscari langa padure.",
            primaryProblem = "missing_supplies",
            secondaryProblem = "forest_rumors",
            publicMood = "cautious",
            outsidePressure = "forest_rumors",
            socialTensions = listOf(
                FixtureSocialTension("${p}tension_merchant_faction", listOf("${p}npc_negustor", "${p}npc_emisar"), "suspected")
            ),
            npcRelations = listOf(
                FixtureNpcRelation("${p}npc_fierar", "${p}npc_ucenic", "work", "high", "Ucenicul lucreaza zilnic in fierarie.", true),
                FixtureNpcRelation("${p}npc_negustor", "${p}npc_emisar", "debt", "medium", "Negustorul suspecteaza ca proviziile lipsa au trecut prin tabara.", false),
                FixtureNpcRelation("${p}npc_fermier_1", "${p}npc_fermier_2", "family", "high", "Dumitru si Maria sunt familie si lucreaza impreuna la ferma.", true),
                FixtureNpcRelation("${p}npc_lider", "${p}npc_hangiu", "trust", "medium", "Liderul are incredere in hangiu pentru informatii din sat.", true),
                FixtureNpcRelation("${p}npc_paznic", "${p}npc_mesager", "work", "medium", "Paznicul si mesagerul coordoneaza iesirile din sat.", false),
                FixtureNpcRelation("${p}npc_ierbar", "${p}npc_lider", "trust", "low", "Vindecatoarea ofera sfaturi liderului despre starea satului.", true),
            ),
            knownRumors = listOf(
                FixtureRumor("${p}rumor_forest_tracks", "${p}npc_hangiu", "${p}padure_veche"),
                FixtureRumor("${p}rumor_old_well", "${p}npc_ierbar", "${p}fantana_uitata"),
            ),
            lockedKnowledge = listOf(
                FixtureLockedKnowledge("${p}knowledge_crypt_origin", "talk_to_npc:${p}npc_pustnic")
            )
        )
    }
}
