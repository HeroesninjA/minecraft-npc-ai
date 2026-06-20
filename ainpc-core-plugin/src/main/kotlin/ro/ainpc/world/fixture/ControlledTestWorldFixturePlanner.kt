package ro.ainpc.world.fixture

import java.util.Locale

class ControlledTestWorldFixturePlanner {
    fun plan(prefix: String? = DEFAULT_PREFIX): ControlledTestWorldFixturePlan {
        val normalizedPrefix = normalizePrefix(prefix)
        val villageId = "${normalizedPrefix}sat_central"

        return ControlledTestWorldFixturePlan(
            "${normalizedPrefix}world_fixture_01",
            normalizedPrefix,
            villageRegion(villageId),
            exteriorRegions(normalizedPrefix),
            listOf(
                "Planul este read-only: nu creeaza mapping, blocuri, NPC-uri, mobi, loot sau quest progress.",
                "Nu foloseste WorldEdit si nu cere API WorldEdit; structurile sunt doar ancore semantice.",
                "Activeaza-l doar in profil test/demo si muta-l ulterior in addon demo sau test resources."
            )
        )
    }

    private fun villageRegion(villageId: String): ControlledFixtureRegionPlan =
        ControlledFixtureRegionPlan(
            villageId,
            "settlement",
            "Sat Central Test",
            "sat principal controlat pentru smoke tests",
            0,
            0,
            listOf("test_fixture", "demo", "settlement", "safe", "controlled"),
            listOf(
                place("$villageId:piata", "market", "start, social, quest trigger", "meeting_point", "quest_trigger"),
                place("$villageId:casa_1", "house", "home coordinator", "door", "bed", "home", "npc_spawn"),
                place("$villageId:casa_2", "house", "home craft worker si trainee", "door", "bed", "home", "npc_spawn"),
                place("$villageId:casa_3", "house", "home resource workers", "door", "bed", "home", "npc_spawn"),
                place("$villageId:casa_4", "house", "home social host", "door", "bed", "home", "npc_spawn"),
                place("$villageId:casa_5", "house", "home exchange and lore workers", "door", "bed", "home", "npc_spawn"),
                place("$villageId:fierarie", "forge", "work anchor craft_worker", "door", "workstation", "work", "inspect"),
                place("$villageId:ferma", "farm", "work anchor resource_workers", "work", "resource", "storage"),
                place("$villageId:taverna", "tavern", "social anchor si zvonuri", "door", "social", "conversation"),
                place("$villageId:altar", "custom", "lore si ritual inspect", "interaction", "inspect"),
                place("$villageId:depozit", "custom", "missing supplies inspect", "door", "storage", "inspect", "quest_trigger"),
                place("$villageId:post_paza", "camp", "guard routine si road control", "watch", "work", "interaction"),
                place("$villageId:drum_nord", "road", "iesire spre turn/padure", "entrance", "road_exit"),
                place("$villageId:drum_est", "road", "iesire spre cripta/tabara", "entrance", "road_exit")
            ),
            listOf("origin", "entry_north", "entry_east")
        )

    private fun exteriorRegions(prefix: String): List<ControlledFixtureRegionPlan> =
        listOf(
            exterior(
                "${prefix}padure_veche",
                "forest",
                "Padure Veche",
                "prima zona de investigatie",
                -180,
                0,
                listOf("entry", "trail", "resource", "danger_marker"),
                placeSuffixes = listOf("entry", "trail", "clearing")
            ),
            exterior(
                "${prefix}fantana_uitata",
                "fountain",
                "Fantana Uitata",
                "istoric local si clue ritual",
                140,
                -80,
                listOf("entry", "interaction", "inspect"),
                placeSuffixes = listOf("fountain")
            ),
            exterior(
                "${prefix}casa_izolata",
                "isolated_house",
                "Casa Izolata",
                "NPC izolat si informatie ascunsa",
                -150,
                120,
                listOf("entry", "door", "bed", "home", "storage"),
                placeSuffixes = listOf("casa")
            ),
            exterior(
                "${prefix}cripta_lupilor",
                "dungeon",
                "Cripta Lupilor",
                "dovada finala pentru quest",
                260,
                40,
                listOf("entrance", "exit", "chamber", "evidence", "loot_marker"),
                placeSuffixes = listOf("entrance", "main_chamber", "exit")
            ),
            exterior(
                "${prefix}tabara_factiune",
                "faction_settlement",
                "Tabara Factiune",
                "tensiune sociala si negociere",
                360,
                -180,
                listOf("entry", "campfire", "leader_anchor", "storage"),
                placeSuffixes = listOf("campfire", "leader_tent", "storage")
            ),
            exterior(
                "${prefix}turn_paza",
                "watchtower",
                "Turn Paza",
                "observatie drum si control perimetru",
                120,
                200,
                listOf("entry", "lookout", "guard_anchor"),
                placeSuffixes = listOf("tower")
            ),
            exterior(
                "${prefix}ruine_vechi",
                "ruins",
                "Ruine Vechi",
                "lore inspect si context istoric",
                -300,
                -160,
                listOf("entry", "inspect", "hidden_room", "loot_marker"),
                placeSuffixes = listOf("entrance", "ruins_core")
            )
        )

    private fun exterior(
        id: String,
        type: String,
        displayName: String,
        role: String,
        offsetX: Int,
        offsetZ: Int,
        requiredNodes: List<String>,
        placeSuffixes: List<String>
    ): ControlledFixtureRegionPlan =
        ControlledFixtureRegionPlan(
            id,
            type,
            displayName,
            role,
            offsetX,
            offsetZ,
            listOf("test_fixture", "demo", "exterior", type, "controlled"),
            placeSuffixes.map { suffix ->
                place("$id:$suffix", if (suffix == "casa") "house" else "custom", role, "entry", "interaction")
            },
            requiredNodes
        )

    private fun place(
        id: String,
        type: String,
        role: String,
        vararg requiredNodes: String
    ): ControlledFixturePlacePlan =
        ControlledFixturePlacePlan(id, type, role, requiredNodes.toList(), listOf("test_fixture", type))

    private fun normalizePrefix(value: String?): String {
        val normalized = value?.trim()?.lowercase(Locale.ROOT).orEmpty()
            .replace('-', '_')
            .replace(' ', '_')
            .replace(Regex("[^a-z0-9_]+"), "_")
            .trim('_')
            .replace(Regex("_+"), "_")
        val base = normalized.ifBlank { DEFAULT_PREFIX.trimEnd('_') }
        return if (base.endsWith("_")) base else "${base}_"
    }

    companion object {
        const val DEFAULT_PREFIX: String = "test_"
    }
}
