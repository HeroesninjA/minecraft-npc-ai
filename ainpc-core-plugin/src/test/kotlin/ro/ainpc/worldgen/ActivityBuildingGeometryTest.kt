package ro.ainpc.worldgen

import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.settlement.BuildingTemplateConfigLoader

class ActivityBuildingGeometryTest {
    private val definitions = BuildingTemplateConfigLoader().parseYamlString(
        requireNotNull(javaClass.getResourceAsStream("/building_templates.yml")).bufferedReader().use { it.readText() },
    ).associateBy { it.templateId }

    @Test
    fun geometryUsesSemanticBoundsAndKeepsAllAnchorsWalkable() {
        for (templateId in listOf("house_small", "house_family", "shop", "forge", "farm", "tavern", "market", "leisure_garden", "community_hall", "workshop", "workshop_shared", "bakery", "library")) {
            val definition = definitions.getValue(templateId)
            val geometry = ActivityBuildingGeometry().generate(definition)
            assertEquals(BoundingBox(0, 0, 0, definition.footprintWidth, definition.footprintHeight, definition.footprintDepth), geometry.getBoundingBox())
            val cells = geometry.blocks.associateBy { Triple(it.x, it.y, it.z) }
            assertEquals(geometry.blocks.size, cells.size)
            for (anchor in definition.anchors) {
                assertEquals(Material.AIR, cells.getValue(Triple(anchor.offsetX, 1, anchor.offsetZ)).material)
                assertEquals(Material.AIR, cells.getValue(Triple(anchor.offsetX, 2, anchor.offsetZ)).material)
                assertEquals(when (templateId) {
                    "forge" -> Material.STONE_BRICKS
                    "bakery" -> Material.BRICKS
                    "tavern", "farm", "community_hall" -> Material.SPRUCE_PLANKS
                    "market" -> Material.COBBLESTONE
                    "leisure_garden" -> Material.GRASS_BLOCK
                    "workshop", "workshop_shared" -> Material.SPRUCE_PLANKS
                    else -> Material.OAK_PLANKS
                },
                    cells.getValue(Triple(anchor.offsetX, 0, anchor.offsetZ)).material)
                assertEquals(anchor.offsetX, geometry.anchors.getValue(anchor.anchorId).blockX)
            }
            val furniture = when (templateId) {
                "house_small", "house_family" -> Material.WHITE_WOOL
                "forge" -> Material.SMITHING_TABLE
                "leisure_garden" -> Material.OAK_SLAB
                "community_hall" -> Material.SPRUCE_FENCE
                "workshop", "workshop_shared" -> Material.CRAFTING_TABLE
                "bakery" -> Material.SMOKER
                "library" -> Material.LECTERN
                "farm" -> Material.COMPOSTER
                else -> Material.BARREL
            }
            assertTrue(geometry.blocks.any { it.material == furniture })
            assertEquals(geometry, ActivityBuildingGeometry().generate(definition))
        }
    }

    @Test
    fun houseRequiresExplicitGeometryAndKeepsSleepingAndSpawnPositionsSeparate() {
        val house = definitions.getValue("house_small")
        val geometry = ActivityBuildingGeometry().generate(house)
        val sleeping = house.anchors.single { it.nodeType == "bed" }
        val spawn = house.anchors.single { it.nodeType == "npc_spawn" }
        val marker = geometry.blocks.single { it.material == Material.WHITE_WOOL }
        assertEquals(1, kotlin.math.abs(marker.x - sleeping.offsetX) + kotlin.math.abs(marker.z - sleeping.offsetZ))
        assertTrue(sleeping.offsetX != spawn.offsetX || sleeping.offsetZ != spawn.offsetZ)
        assertTrue(!ActivityBuildingGeometry.supports(house.copy(metadata = house.metadata - "geometry")))
        for (invalid in listOf(
            house.copy(metadata = house.metadata - "geometry"),
            house.copy(metadata = house.metadata + ("max_residents" to "2")),
            house.copy(tags = house.tags - "home"),
            house.copy(anchors = house.anchors.filter { it.nodeType != "bed" }),
            house.copy(anchors = house.anchors.filter { it.nodeType != "npc_spawn" }),
            house.copy(anchors = house.anchors.map { if (it.nodeType == "npc_spawn") it.copy(offsetX = sleeping.offsetX, offsetZ = sleeping.offsetZ) else it }),
        )) {
            assertThrows(IllegalArgumentException::class.java) { ActivityBuildingGeometry().generate(invalid) }
        }
    }

    @Test
    fun familyHouseProvidesOneDecorativeSleepingMarkerPerResident() {
        val house = definitions.getValue("house_family")
        val geometry = ActivityBuildingGeometry().generate(house)
        val markers = geometry.blocks.filter { it.material == Material.WHITE_WOOL }
        assertEquals(2, markers.size)
        for (sleeping in house.anchors.filter { it.nodeType == "bed" }) {
            assertTrue(markers.any { kotlin.math.abs(it.x - sleeping.offsetX) + kotlin.math.abs(it.z - sleeping.offsetZ) == 1 })
        }
        assertTrue(house.anchors.map { it.offsetX to it.offsetZ }.distinct().size == house.anchors.size)
    }

    @Test
    fun refusesUnsupportedDimensionsAndBlockedOrInvalidAnchors() {
        val shop = definitions.getValue("shop")
        val invalid = listOf(
            shop.copy(placeType = "house"),
            shop.copy(footprintWidth = Int.MAX_VALUE),
            shop.copy(footprintWidth = 100, footprintDepth = 100),
            shop.copy(footprintHeight = 2),
            shop.copy(supportedRotations = listOf(90)),
            shop.copy(anchors = emptyList()),
            shop.copy(anchors = shop.anchors + shop.anchors.first()),
            shop.copy(anchors = shop.anchors.map { it.copy(offsetY = 0) }),
            shop.copy(anchors = shop.anchors.map { if (it.nodeType == "entrance") it.copy(offsetX = 0, offsetZ = 0) else it }),
        )
        for (definition in invalid) {
            assertThrows(IllegalArgumentException::class.java) { ActivityBuildingGeometry().generate(definition) }
        }
    }

    @Test
    fun operationsTranslateGeometryWithoutChangingSemanticOffsets() {
        val definition = definitions.getValue("shop")
        val geometry = ActivityBuildingGeometry().generate(definition)
        val operations = geometry.toBlockOperations("test", 120, 65, -80)
        assertTrue(operations.all { it.worldName == "test" && it.x in 120..128 && it.y in 65..70 && it.z in -80..-73 })
        assertEquals(geometry.blocks.size, operations.size)
        assertEquals(124, geometry.getAnchor("counter", 120, 65, -80)!!.blockX)
    }

    @Test
    fun tavernProvidesBarAndTableBesideSeparateActivityAnchors() {
        val definition = definitions.getValue("tavern")
        val geometry = ActivityBuildingGeometry().generate(definition)
        assertEquals(1, geometry.blocks.count { it.material == Material.BARREL })
        assertEquals(1, geometry.blocks.count { it.material == Material.SPRUCE_FENCE })
        assertEquals(1, geometry.blocks.count { it.material == Material.SPRUCE_PRESSURE_PLATE })
        val social = definition.anchors.single { it.nodeType == "social" }
        val table = geometry.blocks.single { it.material == Material.SPRUCE_FENCE }
        assertEquals(1, kotlin.math.abs(table.x - social.offsetX) + kotlin.math.abs(table.z - social.offsetZ))
        val invalid = definition.copy(anchors = definition.anchors.filter { it.nodeType != "social" })
        assertThrows(IllegalArgumentException::class.java) { ActivityBuildingGeometry().generate(invalid) }
    }

    @Test
    fun marketIsOpenAirWithSupportedStallAndClearMeetingArea() {
        val definition = definitions.getValue("market")
        val geometry = ActivityBuildingGeometry().generate(definition)
        assertEquals(1, geometry.blocks.count { it.material == Material.BARREL })
        assertEquals(3, geometry.blocks.count { it.material == Material.WHITE_WOOL })
        assertEquals(2 * (definition.footprintHeight - 1), geometry.blocks.count { it.material == Material.OAK_FENCE })
        assertTrue(geometry.blocks.filter { it.y > 0 && (it.x == 0 || it.z == 0 || it.x == definition.footprintWidth || it.z == definition.footprintDepth) }
            .all { it.material == Material.AIR })
        val meeting = definition.anchors.single { it.nodeType == "meeting_point" }
        assertTrue(geometry.blocks.filter { it.x == meeting.offsetX && it.z == meeting.offsetZ && it.y > 0 }
            .all { it.material == Material.AIR })
    }

    @Test
    fun farmHasAccessibleComposterAndHayWithoutBlockingSpawn() {
        val farm = definitions.getValue("farm")
        val geometry = ActivityBuildingGeometry().generate(farm)
        val workstation = farm.anchors.single { it.nodeType == "workstation" }
        val composter = geometry.blocks.single { it.material == Material.COMPOSTER }
        assertEquals(1, kotlin.math.abs(composter.x - workstation.offsetX) + kotlin.math.abs(composter.z - workstation.offsetZ))
        assertEquals(1, geometry.blocks.count { it.material == Material.HAY_BLOCK })
        for (invalid in listOf(
            farm.copy(anchors = farm.anchors.map { if (it.nodeType == "entrance") it.copy(offsetY = 0) else it }),
            farm.copy(anchors = farm.anchors.map { if (it.nodeType == "npc_spawn") it.copy(offsetY = 0) else it }),
        )) {
            assertThrows(IllegalArgumentException::class.java) { ActivityBuildingGeometry().generate(invalid) }
        }
    }

    @Test
    fun gardenRequiresExplicitGeometryAndHasNoWorkplaceFurniture() {
        val garden = definitions.getValue("leisure_garden")
        val geometry = ActivityBuildingGeometry().generate(garden)
        assertEquals(1, geometry.blocks.count { it.material == Material.OAK_SLAB })
        assertTrue(geometry.blocks.none { it.material in setOf(Material.BARREL, Material.SMITHING_TABLE) })
        assertTrue(geometry.blocks.filter { it.y >= 2 }.all { it.material == Material.AIR })
        for (invalid in listOf(garden.copy(metadata = mapOf("max_workers" to "0")),
            garden.copy(metadata = garden.metadata + ("max_workers" to "1")), garden.copy(tags = garden.tags + "work"),
            garden.copy(anchors = garden.anchors.filter { it.nodeType != "social" }))) {
            assertThrows(IllegalArgumentException::class.java) { ActivityBuildingGeometry().generate(invalid) }
        }
    }

    @Test
    fun communityHallIsCoveredSocialOnlyAndRequiresPositiveAssociationCapacity() {
        val hall = definitions.getValue("community_hall")
        val geometry = ActivityBuildingGeometry().generate(hall)
        val cells = geometry.blocks.associateBy { Triple(it.x, it.y, it.z) }
        assertEquals(Material.SPRUCE_PLANKS, cells.getValue(Triple(0, 1, 4)).material)
        assertEquals(Material.SPRUCE_PLANKS, cells.getValue(Triple(4, hall.footprintHeight, 4)).material)
        assertEquals(2, geometry.blocks.count { it.material == Material.SPRUCE_FENCE })
        assertEquals(2, geometry.blocks.count { it.material == Material.SPRUCE_PRESSURE_PLATE })
        assertTrue(geometry.blocks.none { it.material in setOf(Material.BARREL, Material.SMITHING_TABLE) })
        for (invalid in listOf(
            hall.copy(metadata = hall.metadata - "geometry"),
            hall.copy(metadata = hall.metadata - "max_social_associations"),
            hall.copy(metadata = hall.metadata + ("max_workers" to "1")),
            hall.copy(metadata = hall.metadata + ("max_social_associations" to "0")),
            hall.copy(tags = hall.tags + "work"),
            hall.copy(tags = hall.tags - "social"),
            hall.copy(anchors = hall.anchors.filter { it.nodeType != "social" }),
            hall.copy(anchors = hall.anchors + hall.anchors.last().copy(anchorId = "work", nodeType = "workstation", offsetX = 6)),
        )) {
            assertThrows(IllegalArgumentException::class.java) { ActivityBuildingGeometry().generate(invalid) }
        }
    }

    @Test
    fun workshopRequiresExplicitGeometryAndRealWorkCapacity() {
        val workshop = definitions.getValue("workshop")
        val geometry = ActivityBuildingGeometry().generate(workshop)
        assertEquals(1, geometry.blocks.count { it.material == Material.CRAFTING_TABLE })
        assertTrue(geometry.blocks.none { it.material == Material.SMITHING_TABLE })
        for (invalid in listOf(
            workshop.copy(metadata = workshop.metadata - "geometry"),
            workshop.copy(metadata = workshop.metadata + ("max_workers" to "0")),
            workshop.copy(tags = workshop.tags - "work"),
            workshop.copy(anchors = workshop.anchors.filter { it.nodeType != "workstation" }),
        )) {
            assertThrows(IllegalArgumentException::class.java) { ActivityBuildingGeometry().generate(invalid) }
        }
    }

    @Test
    fun sharedWorkshopPlacesTwoTablesBesideSeparateWorkAnchors() {
        val workshop = definitions.getValue("workshop_shared")
        val geometry = ActivityBuildingGeometry().generate(workshop)
        val tables = geometry.blocks.filter { it.material == Material.CRAFTING_TABLE }
        assertEquals(2, tables.size)
        for (workstation in workshop.anchors.filter { it.nodeType == "workstation" }) {
            assertTrue(tables.any { kotlin.math.abs(it.x - workstation.offsetX) + kotlin.math.abs(it.z - workstation.offsetZ) == 1 })
        }
        assertTrue(tables.map { it.x to it.z }.distinct().size == 2)
    }

    @Test
    fun bakeryRequiresExplicitGeometryAndRealWorkCapacity() {
        val bakery = definitions.getValue("bakery")
        val geometry = ActivityBuildingGeometry().generate(bakery)
        assertEquals(1, geometry.blocks.count { it.material == Material.SMOKER })
        assertTrue(geometry.blocks.none { it.material == Material.CRAFTING_TABLE })
        for (invalid in listOf(
            bakery.copy(metadata = bakery.metadata - "geometry"),
            bakery.copy(metadata = bakery.metadata + ("max_workers" to "0")),
            bakery.copy(tags = bakery.tags - "work"),
            bakery.copy(anchors = bakery.anchors.filter { it.nodeType != "workstation" }),
        )) {
            assertThrows(IllegalArgumentException::class.java) { ActivityBuildingGeometry().generate(invalid) }
        }
    }

    @Test
    fun libraryRequiresWorkAndSocialSemanticsWithAccessibleFurniture() {
        val library = definitions.getValue("library")
        val geometry = ActivityBuildingGeometry().generate(library)
        assertEquals(1, geometry.blocks.count { it.material == Material.LECTERN })
        assertEquals(1, geometry.blocks.count { it.material == Material.BOOKSHELF })
        for ((anchorId, material) in listOf("desk" to Material.LECTERN, "reading" to Material.BOOKSHELF)) {
            val anchor = library.anchors.single { it.anchorId == anchorId }
            val furniture = geometry.blocks.single { it.material == material }
            assertEquals(1, kotlin.math.abs(furniture.x - anchor.offsetX) + kotlin.math.abs(furniture.z - anchor.offsetZ))
        }
        for (invalid in listOf(
            library.copy(metadata = library.metadata - "geometry"),
            library.copy(metadata = library.metadata + ("max_workers" to "0")),
            library.copy(tags = library.tags - "work"),
            library.copy(tags = library.tags - "social"),
            library.copy(anchors = library.anchors.filter { it.nodeType != "workstation" }),
            library.copy(anchors = library.anchors.filter { it.nodeType != "social" }),
        )) {
            assertThrows(IllegalArgumentException::class.java) { ActivityBuildingGeometry().generate(invalid) }
        }
    }
}
