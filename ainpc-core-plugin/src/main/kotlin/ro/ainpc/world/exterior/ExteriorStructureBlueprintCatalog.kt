package ro.ainpc.world.exterior

object ExteriorStructureBlueprintCatalog {
    private val blueprints: Map<ExteriorStructureType, ExteriorStructureBlueprint> = linkedMapOf(
        ExteriorStructureType.CASTLE to blueprint(
            ExteriorStructureType.CASTLE,
            "Castel",
            "Regiune fortificata pentru autoritate, aparare, quest hub si factiuni.",
            "castle",
            listOf("exterior", "fortified", "military", "guarded"),
            listOf("main_gate", "courtyard"),
            listOf("throne_room", "barracks", "armory", "tower", "dungeon_cells"),
            listOf("entrance:main_gate"),
            listOf("guard_post", "meeting_point:courtyard", "interaction:throne"),
            listOf(
                "Separarea public/restricted trebuie facuta prin tags.",
                "Celulele din castel nu sunt automat dungeon de explorare."
            )
        ),
        ExteriorStructureType.FOREST to blueprint(
            ExteriorStructureType.FOREST,
            "Padure",
            "Regiune de explorare, resurse, drumuri, risc si questuri de cautare.",
            "wilderness",
            listOf("exterior", "wilderness", "natural", "resource"),
            listOf("forest_edge", "main_trail"),
            listOf("clearing", "hunter_camp", "resource_grove", "danger_point"),
            listOf("entrance:forest_edge"),
            listOf("meeting_point:clearing", "interaction:resource_node", "quest_trigger:trail_marker"),
            listOf(
                "Marcheaza separat zonele sigure si periculoase.",
                "Resursele trebuie sa fie nodes, nu doar tag pe regiune."
            )
        ),
        ExteriorStructureType.FOUNTAIN to blueprint(
            ExteriorStructureType.FOUNTAIN,
            "Fantana",
            "Reper social, apa, ritual, clue sau loc de intalnire.",
            "custom",
            listOf("exterior", "landmark", "water"),
            listOf("well"),
            listOf("meeting_spot"),
            listOf("interaction:water_source"),
            listOf("interaction:inspect_point", "meeting_point:meeting_spot"),
            listOf("Fantana izolata este place; fantana din centru poate ramane node.", "Nu folosi fantana ca regiune.")
        ),
        ExteriorStructureType.ISOLATED_HOUSE to blueprint(
            ExteriorStructureType.ISOLATED_HOUSE,
            "Casa izolata",
            "Locuinta la distanta pentru NPC izolat, refugiu, vendor rar sau quest episodic.",
            "custom",
            listOf("exterior", "isolated_house", "remote", "shelter"),
            listOf("house"),
            listOf("garden", "workbench"),
            listOf("entrance:door", "bed:bed"),
            listOf("interaction:storage", "home:hearth", "npc_spawn:resident_spawn"),
            listOf(
                "Rezidentul permanent cere household/binding ulterior.",
                "Daca este doar decor, nu spawna NPC permanent."
            )
        ),
        ExteriorStructureType.HAMLET to blueprint(
            ExteriorStructureType.HAMLET,
            "Mini-sat",
            "Asezare mica legata de satul principal prin drum si economie simpla.",
            "settlement",
            listOf("exterior", "hamlet", "small", "civilian"),
            listOf("center", "house_1", "road_link"),
            listOf("well", "house_2", "workplace_1", "small_market"),
            listOf("meeting_point:center", "entrance:road_link"),
            listOf("bed:house_bed", "work:work_anchor", "interaction:well"),
            listOf(
                "Nu spawna populatie daca lipsesc casele si nodes minime.",
                "Drumul spre satul principal trebuie sa fie semantic."
            )
        ),
        ExteriorStructureType.FACTION_SETTLEMENT to blueprint(
            ExteriorStructureType.FACTION_SETTLEMENT,
            "Asezare de factiune",
            "Asezare neutra ca tip core; poate reprezenta barbari, trib, banditi sau alta factiune din addon/config.",
            "settlement",
            listOf("exterior", "faction", "hostile_optional", "danger_medium"),
            listOf("campfire_center", "leader_hut"),
            listOf("outer_gate", "training_ground", "loot_storage", "prison_cage", "watch_post"),
            listOf("meeting_point:campfire", "entrance:outer_gate"),
            listOf("interaction:leader_anchor", "interaction:storage", "quest_trigger:challenge"),
            listOf(
                "Core-ul nu forteaza ostilitate permanenta.",
                "Foloseste faction:<id> sau tag echivalent pentru scenariu."
            )
        ),
        ExteriorStructureType.DUNGEON to blueprint(
            ExteriorStructureType.DUNGEON,
            "Dungeon",
            "Regiune sau place de lupta, puzzle, loot, risc si boss optional.",
            "dungeon",
            listOf("exterior", "danger", "combat", "loot"),
            listOf("entrance", "exit"),
            listOf("checkpoint", "loot_room", "trap_corridor", "boss_room", "safe_room"),
            listOf("entrance:entrance", "interaction:exit"),
            listOf("interaction:loot", "boss:boss_spawn", "quest_trigger:checkpoint"),
            listOf("Intrarea si iesirea trebuie separate.", "Spawn-ul agresiv nu porneste doar din type=dungeon.")
        ),
        ExteriorStructureType.CAMP to blueprint(
            ExteriorStructureType.CAMP,
            "Tabara",
            "Loc temporar pentru trader, refugiati, banditi, patrula sau eveniment.",
            "custom",
            listOf("exterior", "camp", "temporary"),
            listOf("camp_center"),
            listOf("tent", "storage", "watch_spot"),
            listOf("meeting_point:campfire"),
            listOf("npc_spawn:camp_spawn", "interaction:storage"),
            listOf(
                "Marcheaza daca tabara este temporara sau persistenta.",
                "Nu lega rezidenti permanenti fara intentie explicita."
            )
        ),
        ExteriorStructureType.RUINS to blueprint(
            ExteriorStructureType.RUINS,
            "Ruine",
            "Zona de lore, puzzle, explorare, clue-uri si loot rar.",
            "custom",
            listOf("exterior", "ruins", "ancient", "lore"),
            listOf("entrance", "main_ruin"),
            listOf("inscription", "hidden_room", "altar", "loot_room"),
            listOf("entrance:entrance"),
            listOf("interaction:inspect_point", "interaction:inscription", "interaction:loot"),
            listOf(
                "Lore-ul trebuie sa fie metadata/tag/story, nu doar blocuri.",
                "Loot-ul trebuie sa fie node explicit."
            )
        ),
        ExteriorStructureType.CAVE_OR_MINE to blueprint(
            ExteriorStructureType.CAVE_OR_MINE,
            "Pestera sau mina",
            "Zona subterana pentru resurse, risc, trecere si explorare.",
            "cave",
            listOf("exterior", "underground", "resource", "danger"),
            listOf("entrance"),
            listOf("resource_node", "danger_point", "exit", "deep_room"),
            listOf("entrance:entrance"),
            listOf("interaction:resource_node", "interaction:exit", "quest_trigger:danger_point"),
            listOf("Mina trebuie sa aiba iesire sau fallback clar.", "Resursele si pericolele sunt nodes.")
        ),
        ExteriorStructureType.SHRINE to blueprint(
            ExteriorStructureType.SHRINE,
            "Shrine",
            "Loc ritualic pentru altar, offering, reputatie, story event sau quest.",
            "custom",
            listOf("exterior", "shrine", "ritual", "story"),
            listOf("altar"),
            listOf("offering_spot", "inspect_area"),
            listOf("interaction:altar"),
            listOf("interaction:offering_spot", "quest_trigger:ritual"),
            listOf(
                "AI/story poate folosi shrine-ul doar prin context validat.",
                "Reward-ul/progresul ramane in serviciile deterministe."
            )
        ),
        ExteriorStructureType.WATCHTOWER to blueprint(
            ExteriorStructureType.WATCHTOWER,
            "Turn de paza",
            "Place defensiv pentru observatie, guard, semnalizare si control de drum.",
            "custom",
            listOf("exterior", "watchtower", "military", "lookout"),
            listOf("tower"),
            listOf("signal_spot", "road_control"),
            listOf("entrance:entrance", "interaction:lookout"),
            listOf("interaction:signal_fire", "npc_spawn:guard_post"),
            listOf("Accesul trebuie marcat ca node.", "Guard-ul este anchor, nu spawn automat.")
        )
    )

    @JvmStatic
    fun all(): List<ExteriorStructureBlueprint> = blueprints.values.toList()

    @JvmStatic
    fun get(type: ExteriorStructureType?): ExteriorStructureBlueprint? = blueprints[type]

    @JvmStatic
    fun find(typeId: String?): ExteriorStructureBlueprint? =
        get(ExteriorStructureType.fromId(typeId))

    private fun blueprint(
        type: ExteriorStructureType,
        title: String,
        summary: String,
        regionTypeHint: String,
        tags: List<String>,
        requiredPlaces: List<String>,
        recommendedPlaces: List<String>,
        requiredNodes: List<String>,
        recommendedNodes: List<String>,
        rules: List<String>
    ): ExteriorStructureBlueprint =
        ExteriorStructureBlueprint(
            type,
            title,
            summary,
            regionTypeHint,
            tags,
            requiredPlaces,
            recommendedPlaces,
            requiredNodes,
            recommendedNodes,
            rules
        )
}
