package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class RoutineGuiDiagnosticsTest {
    @Test
    fun routineGuiShowsRoutineStatus() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/RoutineGui.kt").readText()

        assertTrue(source.contains("Rutine NPC"))
        assertTrue(source.contains("Rutine Dezactivate"))
        assertTrue(source.contains("Timp World"))
    }

    @Test
    fun routineGuiListsNpcs() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/RoutineGui.kt").readText()

        assertTrue(source.contains("getAllNPCs"))
        assertTrue(source.contains("npc.name"))
        assertTrue(source.contains("routine status"))
        assertTrue(source.contains("NPC Bindings"))
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

        assertTrue(source.contains("Status Nearest"))
        assertTrue(source.contains("Ruleaza Tick Rutina"))
        assertTrue(source.contains("Manager NPC"))
        assertTrue(source.contains("NPC Admin"))
        assertTrue(source.contains("Deschide Managerul NPC Admin."))
    }

    @Test
    fun routineGuiUsesStandardControls() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/RoutineGui.kt").readText()

        assertTrue(source.contains("GuiNavigation.addStandardControls"))
        assertTrue(source.contains("GuiKey.ROUTINE"))
        assertTrue(source.contains("fillEmpty"))
    }
}
