package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ro.ainpc.world.WorldBlueprint

class WorldBlueprintListSelectionTest {
    @Test
    fun `lists all 64 persisted drafts across two stable pages`() {
        val drafts = (0 until 64).reversed().map { WorldBlueprint("world_${it.toString().padStart(2, '0')}") }

        val first = WorldBlueprintListSelection.select(drafts, emptyList(), 0)
        val last = WorldBlueprintListSelection.select(drafts, emptyList(), 999)

        assertEquals(2, first.count)
        assertEquals(0, first.index)
        assertEquals(36, first.entries.size)
        assertEquals("world_00", first.entries.first().worldName)
        assertEquals("world_35", first.entries.last().worldName)
        assertEquals(1, last.index)
        assertEquals(28, last.entries.size)
        assertEquals("world_36", last.entries.first().worldName)
        assertEquals("world_63", last.entries.last().worldName)
    }

    @Test
    fun `empty list and negative page keep a valid first page`() {
        val empty = WorldBlueprintListSelection.select(emptyList(), emptyList(), 5)
        val first = WorldBlueprintListSelection.select(listOf(WorldBlueprint("world")), emptyList(), -10)

        assertEquals(1, empty.count)
        assertEquals(0, empty.index)
        assertEquals(emptyList<WorldBlueprintListEntry>(), empty.entries)
        assertEquals(0, first.index)
        assertEquals("world", first.entries.single().worldName)
    }

    @Test
    fun `loaded worlds without drafts appear once beside offline drafts`() {
        val result = WorldBlueprintListSelection.select(
            listOf(WorldBlueprint("alpha"), WorldBlueprint("world")),
            listOf("world", "new_world", "new_world"), 0,
        )

        assertEquals(listOf("alpha", "new_world", "world"), result.entries.map { it.worldName })
        assertEquals(false, result.entries[0].loaded)
        assertEquals(true, result.entries[1].loaded)
        assertEquals(null, result.entries[1].blueprint)
        assertEquals(true, result.entries[2].loaded)
        assertEquals("world", result.entries[2].blueprint?.worldName)
    }
}
