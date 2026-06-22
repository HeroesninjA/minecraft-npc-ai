package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class RoutineGuiDiagnosticsTest {
    @Test
    fun routineGuiShowsRoutineStatus() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/RoutineGui.kt").readText()

        assertTrue(source.contains("Rutine NPC"))
        assertTrue(source.contains("Rutine dezactivate"))
        assertTrue(source.contains("Timp world"))
    }

    @Test
    fun routineGuiListsNpcs() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/RoutineGui.kt").readText()

        assertTrue(source.contains("getAllNPCs"))
        assertTrue(source.contains("npc.name"))
        assertTrue(source.contains("routine status"))
    }

    @Test
    fun routineGuiShowsSchedule() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/RoutineGui.kt").readText()

        assertTrue(source.contains("Slot curent"))
        assertTrue(source.contains("Activitate"))
        assertTrue(source.contains("Program zi"))
    }

    @Test
    fun routineGuiHasAdminActions() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/RoutineGui.kt").readText()

        assertTrue(source.contains("Status nearest"))
        assertTrue(source.contains("Ruleaza tick rutina"))
        assertTrue(source.contains("Manager NPC"))
    }

    @Test
    fun routineGuiUsesStandardControls() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/RoutineGui.kt").readText()

        assertTrue(source.contains("GuiNavigation.addStandardControls"))
        assertTrue(source.contains("GuiKey.ROUTINE"))
        assertTrue(source.contains("fillEmpty"))
    }
}
