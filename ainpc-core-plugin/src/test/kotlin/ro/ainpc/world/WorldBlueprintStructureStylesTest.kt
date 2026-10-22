package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.world.exterior.ExteriorStructureType

class WorldBlueprintStructureStylesTest {
    @Test
    fun `canonical types are normalized while descriptions keep their wording`() {
        val styles = WorldBlueprintStructureStyles.parse(
            " CASTLE = Turnuri de piatra ; watchtower=Acoperis abrupt = lemn ")!!

        assertNull(styles.error)
        assertEquals(listOf(ExteriorStructureType.CASTLE, ExteriorStructureType.WATCHTOWER),
            styles.byType.keys.toList())
        assertEquals("Turnuri de piatra", styles.byType[ExteriorStructureType.CASTLE])
        assertEquals("Acoperis abrupt = lemn", styles.byType[ExteriorStructureType.WATCHTOWER])
        assertNull(WorldBlueprintStructureStyles.parse(null))
    }

    @Test
    fun `malformed mapping rejects all type styles`() {
        listOf("", "castle", "castle=", "castle=Turnuri;", "custom=Turnuri",
            "unknown=Turnuri", "castle=Turnuri;CASTLE=Ziduri", "castle=${"A".repeat(201)}",
            List(13) { "castle=Turnuri" }.joinToString(";"),
        ).forEach { raw ->
            val styles = WorldBlueprintStructureStyles.parse(raw)!!
            assertNotNull(styles.error, raw)
            assertTrue(styles.byType.isEmpty(), raw)
        }
    }

    @Test
    fun `plan reports forbidden style and keeps valid style visible`() {
        val draft = WorldBlueprint("world", mapRules = mapOf(
            "forbidden_exterior_types" to "castle",
            WorldBlueprintStructureStyles.KEY to "castle=Turnuri de piatra"))
        val report = WorldBlueprintCompatibility.check(draft, null, null)

        assertEquals(2, report.structuredRuleCount)
        assertEquals(WorldBlueprintCompatibilityCode.CONFLICTING_STRUCTURE_STYLE_RULES,
            report.issues.single().code)
        assertEquals("Turnuri de piatra", report.structureStyles[ExteriorStructureType.CASTLE])

        val invalid = WorldBlueprintCompatibility.check(draft.copy(mapRules = mapOf(
            WorldBlueprintStructureStyles.KEY to "castle=Turnuri;castle=Ziduri")), null, null)
        assertEquals(WorldBlueprintCompatibilityCode.INVALID_STRUCTURE_STYLES_RULE,
            invalid.issues.single().code)
        assertTrue(invalid.structureStyles.isEmpty())
    }

    @Test
    fun `structure preview selects type style and falls back to global architecture`() {
        val draft = WorldBlueprint("world", architecture = "Case de lemn", mapRules = mapOf(
            "required_exterior_types" to "castle,watchtower",
            WorldBlueprintStructureStyles.KEY to "castle=Turnuri de piatra"))
        val castle = WorldBlueprintStructurePlanner.compile(draft, "castle", "keep", null, null, null)
        val watchtower = WorldBlueprintStructurePlanner.compile(draft, "watchtower", "tower", null, null, null)

        assertEquals("Turnuri de piatra", castle.architectureStyle)
        assertEquals("${WorldBlueprintStructureStyles.KEY}:castle", castle.architectureStyleSource)
        assertEquals("Case de lemn", watchtower.architectureStyle)
        assertEquals("architecture", watchtower.architectureStyleSource)
        assertNull(castle.architectureStyleIssue)

        val invalid = WorldBlueprintStructurePlanner.compile(draft.copy(mapRules = draft.mapRules +
            (WorldBlueprintStructureStyles.KEY to "castle=Turnuri;castle=Ziduri")),
            "castle", "keep", null, null, null)
        assertEquals("Case de lemn", invalid.architectureStyle)
        assertTrue(invalid.architectureStyleIssue?.contains(WorldBlueprintStructureStyles.KEY) == true)
    }

    @Test
    fun `AI preview reports only a newly forbidden type style`() {
        val base = WorldBlueprint("world", mapRules = mapOf(
            WorldBlueprintStructureStyles.KEY to "castle=Turnuri de piatra"))
        val candidate = base.copy(mapRules = base.mapRules + ("forbidden_exterior_types" to "castle"))

        assertEquals(listOf(WorldBlueprintCompatibilityCode.CONFLICTING_STRUCTURE_STYLE_RULES to "castle"),
            WorldBlueprintCompatibility.introducedStructuredRuleConflicts(base, candidate)
                .map { it.code to it.subject })
        assertTrue(WorldBlueprintCompatibility.introducedStructuredRuleConflicts(candidate,
            candidate.copy(mapRules = candidate.mapRules + ("drumuri" to "Ocoleste lacul"))).isEmpty())
    }
}
