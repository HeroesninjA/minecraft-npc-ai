package ro.ainpc.world.mapping

import ro.ainpc.world.PlaceType
import ro.ainpc.world.RegionType
import ro.ainpc.world.WorldNodeType
import java.text.Normalizer
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Locale

object MappingIntentParser {
    @JvmStatic
    fun suggest(kind: MappingDraftKind, description: String?): MappingDraftSuggestion {
        val clean = cleanDescription(description)
        val normalized = normalize(clean)
        return when (kind) {
            MappingDraftKind.REGION -> suggestRegion(clean, normalized)
            MappingDraftKind.PLACE -> suggestPlace(clean, normalized)
            MappingDraftKind.NODE -> suggestNode(clean, normalized)
            MappingDraftKind.NPC_BIND -> MappingDraftSuggestion(
                slugOrFallback(clean, "npc_bind"),
                displayName(clean, "NPC Bind"),
                "npc_bind",
                emptyList(),
                mapOf("draft_only" to "true"),
                2.5,
                listOf("Confirmarea aplica bind-ul NPC pe profil, metadata mapping si npc_world_bindings.")
            )

            MappingDraftKind.QUEST_ANCHOR -> MappingDraftSuggestion(
                slugOrFallback(clean, "quest_anchor"),
                displayName(clean, "Quest Anchor"),
                "quest_anchor",
                emptyList(),
                mapOf("draft_only" to "true"),
                2.5,
                listOf("Confirmarea scrie direct in quest_anchor_bindings pentru progresia aleasa.")
            )
        }
    }

    @JvmStatic
    fun slugOrFallback(value: String?, fallback: String): String {
        var normalized = normalize(value)
            .replace(Regex("[^a-z0-9]+"), "_")
            .replace(Regex("^_+|_+$"), "")
            .replace(Regex("_+"), "_")

        if (normalized.isBlank()) {
            return fallback
        }
        if (normalized.length > 42) {
            normalized = normalized.substring(0, 42).replace(Regex("_+$"), "")
        }
        return normalized
    }

    @JvmStatic
    fun cleanDescription(description: String?): String {
        var value = description?.trim().orEmpty()
        value = value.replace(Regex("(?i)\\b(aici|asta|acesta|aceasta|este|va fi|e|un|o|the|this)\\b"), " ")
        value = value.replace(Regex("\\s+"), " ").trim()
        return if (value.isBlank()) "mapping draft" else value
    }

    @JvmStatic
    fun normalize(value: String?): String {
        val raw = value?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val ascii = Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        return ascii
            .replace('\u0103', 'a')
            .replace('\u00e2', 'a')
            .replace('\u00ee', 'i')
            .replace('\u0219', 's')
            .replace('\u0163', 't')
            .replace('\u021b', 't')
    }

    private fun suggestRegion(clean: String, normalized: String): MappingDraftSuggestion {
        var type = RegionType.CUSTOM
        val tags = LinkedHashSet<String>()
        if (containsAny(normalized, "sat", "village", "asezare")) {
            type = RegionType.SETTLEMENT
            tags.addAll(listOf("settlement", "village"))
        } else if (containsAny(normalized, "castel", "castle", "cetate")) {
            type = RegionType.CASTLE
            tags.addAll(listOf("castle", "fortified"))
        } else if (containsAny(normalized, "dungeon", "temnita", "cripta")) {
            type = RegionType.DUNGEON
            tags.addAll(listOf("dungeon", "danger"))
        } else if (containsAny(normalized, "pestera", "cave", "mina")) {
            type = RegionType.CAVE
            tags.addAll(listOf("cave", "underground"))
        } else if (containsAny(normalized, "padure", "drum", "camp", "wilderness")) {
            type = RegionType.WILDERNESS
            tags.addAll(listOf("wilderness"))
        }

        tags.add("manual")
        return MappingDraftSuggestion(
            preferredId(clean, normalized, type.id),
            displayName(clean, "Regiune"),
            type.id,
            tags.toList(),
            mapOf("source" to "wand_prompt"),
            2.5,
            emptyList()
        )
    }

    private fun suggestPlace(clean: String, normalized: String): MappingDraftSuggestion {
        var type = PlaceType.CUSTOM
        val tags = LinkedHashSet<String>()
        val metadata = LinkedHashMap<String, String>()
        var publicAccess = true

        if (containsAny(normalized, "casa")) {
            type = PlaceType.HOUSE
            tags.addAll(listOf("home", "residential"))
            publicAccess = false
            if (containsAny(normalized, "fierar", "blacksmith")) {
                tags.add("blacksmith")
                metadata["profession"] = "blacksmith"
            }
        } else if (containsAny(normalized, "fierarie", "forja", "fierar", "blacksmith", "forge")) {
            type = PlaceType.FORGE
            tags.addAll(listOf("workplace", "blacksmith", "shop"))
            metadata["profession"] = "blacksmith"
            metadata["role"] = "work"
        } else if (containsAny(normalized, "taverna", "han", "inn", "tavern")) {
            type = PlaceType.TAVERN
            tags.addAll(listOf("social", "public", "food"))
        } else if (containsAny(normalized, "piata", "market", "targ")) {
            type = PlaceType.MARKET
            tags.addAll(listOf("public", "social", "trade"))
            metadata["role"] = "social"
        } else if (containsAny(normalized, "ferma", "farm", "lan")) {
            type = PlaceType.FARM
            tags.addAll(listOf("farm", "workplace", "food"))
            metadata["role"] = "work"
        } else if (containsAny(normalized, "magazin", "shop", "negustor")) {
            type = PlaceType.SHOP
            tags.addAll(listOf("shop", "trade", "public"))
        } else if (containsAny(normalized, "tabara", "camp")) {
            type = PlaceType.CAMP
            tags.add("camp")
        } else if (containsAny(normalized, "camera", "sala")) {
            type = PlaceType.CASTLE_ROOM
            tags.add("room")
        } else if (containsAny(normalized, "cripta", "pestera", "grota")) {
            type = PlaceType.CAVE_ROOM
            tags.addAll(listOf("cave", "danger"))
        }

        tags.add("manual")
        metadata["source"] = "wand_prompt"
        metadata["public_access_hint"] = if (publicAccess) "true" else "false"
        return MappingDraftSuggestion(
            preferredId(clean, normalized, type.id),
            displayName(clean, "Place"),
            type.id,
            tags.toList(),
            metadata,
            2.5,
            emptyList()
        )
    }

    private fun suggestNode(clean: String, normalized: String): MappingDraftSuggestion {
        var type = WorldNodeType.CUSTOM
        val metadataTags = LinkedHashSet<String>()
        val metadata = LinkedHashMap<String, String>()
        var preferredId = ""
        var radius = 2.5

        if (containsAny(normalized, "avizier", "panou", "board", "quest board", "quest", "ancora")) {
            type = WorldNodeType.QUEST_TRIGGER
            preferredId = if (containsAny(normalized, "avizier", "panou", "board")) "quest_board" else "quest_anchor"
            metadata["semantic"] = preferredId
            metadata["role"] = "quest_anchor"
            metadataTags.addAll(listOf("board", "public", "quests"))
            radius = 2.0
        } else if (containsAny(normalized, "ritual", "cerc", "altar")) {
            type = WorldNodeType.QUEST_TRIGGER
            preferredId = "ritual_circle"
            metadata["semantic"] = "ritual_circle"
            metadata["role"] = "ritual_start"
            metadataTags.addAll(listOf("ritual", "quest_anchor"))
            radius = 3.0
        } else if (containsAny(normalized, "intrare", "usa", "poarta", "entrance")) {
            type = WorldNodeType.ENTRANCE
            preferredId = "entrance"
            metadata["semantic"] = "entrance"
            radius = 2.0
        } else if (containsAny(normalized, "pat", "bed")) {
            type = WorldNodeType.BED
            preferredId = "bed"
            metadata["semantic"] = "bed"
            radius = 1.5
        } else if (containsAny(normalized, "munca", "lucru", "work", "nicovala", "workstation")) {
            type = WorldNodeType.WORKSTATION
            preferredId = if (containsAny(normalized, "nicovala")) "anvil" else "workstation"
            metadata["semantic"] = preferredId
            metadata["role"] = "work"
            radius = 2.0
        } else if (containsAny(normalized, "aduna", "intalnire", "meeting", "social")) {
            type = WorldNodeType.MEETING_POINT
            preferredId = "meeting_point"
            metadata["semantic"] = "meeting_point"
            metadata["role"] = "social"
            radius = 4.0
        } else if (containsAny(normalized, "spawn", "aparitie")) {
            type = WorldNodeType.NPC_SPAWN
            preferredId = "npc_spawn"
            metadata["semantic"] = "npc_spawn"
            radius = 2.0
        }

        metadata["source"] = "wand_prompt"
        if (metadataTags.isNotEmpty()) {
            metadata["tags"] = metadataTags.joinToString(",")
        }
        val id = if (preferredId.isBlank()) preferredId(clean, normalized, type.id) else preferredId
        return MappingDraftSuggestion(
            id,
            displayName(clean, "Node"),
            type.id,
            emptyList(),
            metadata,
            radius,
            emptyList()
        )
    }

    private fun containsAny(normalized: String, vararg tokens: String): Boolean {
        for (token in tokens) {
            if (normalized.contains(normalize(token))) {
                return true
            }
        }
        return false
    }

    private fun preferredId(clean: String, normalized: String, fallback: String): String {
        val specific = ArrayList<String>()
        for (token in normalized.split(Regex("\\s+"))) {
            if (token.length <= 2 || isFiller(token)) {
                continue
            }
            specific.add(token)
        }
        val source = if (specific.isEmpty()) clean else specific.joinToString("_")
        return slugOrFallback(source, fallback)
    }

    private fun isFiller(token: String): Boolean {
        return when (token) {
            "aici", "este", "asta", "acesta", "aceasta", "pentru", "lui", "din", "de", "la", "si", "cu" -> true
            else -> false
        }
    }

    private fun displayName(clean: String?, fallback: String): String {
        val value = if (clean.isNullOrBlank()) fallback else clean.trim()
        val words = value.split(Regex("\\s+"))
        val formatted = ArrayList<String>()
        for (word in words) {
            if (word.isBlank()) {
                continue
            }
            if (word.length == 1) {
                formatted.add(word.uppercase(Locale.ROOT))
            } else {
                formatted.add(
                    word.substring(0, 1).uppercase(Locale.ROOT) +
                        word.substring(1).lowercase(Locale.ROOT)
                )
            }
        }
        return if (formatted.isEmpty()) fallback else formatted.joinToString(" ")
    }
}
