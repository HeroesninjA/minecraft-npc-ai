package ro.ainpc.commands

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AINPCCommandMutationPolicyTest {
    @Test
    fun blocksEveryMappingAndFixtureMutationRoute() {
        val cases = linkedMapOf(
            command(AINPCSubcommandRoute.MAP, "map", "confirm") to "ainpc map confirm",
            command(AINPCSubcommandRoute.MAP, "map", "CONFIRMA") to "ainpc map confirm",
            command(AINPCSubcommandRoute.PATCH, "patch", "apply", "village", "patch-1") to "ainpc patch apply",
            command(AINPCSubcommandRoute.BUILDING, "building", "auto-place", "house", "village") to
                "ainpc building auto-place",
            command(AINPCSubcommandRoute.WORLD, "world", "fixture", "apply") to
                "ainpc world fixture apply",
            command(AINPCSubcommandRoute.WORLD, "world", "fixture", "populate") to
                "ainpc world fixture populate",
            command(AINPCSubcommandRoute.WORLD, "world", "scan", "village", "32", "import") to
                "ainpc world scan village import",
            command(AINPCSubcommandRoute.WORLD, "world", "demo", "create") to
                "ainpc world demo create",
            command(AINPCSubcommandRoute.WORLD, "world", "bind", "npc", "nearest", "home") to
                "ainpc world bind npc",
            command(AINPCSubcommandRoute.WORLD, "world", "household", "spawn", "house") to
                "ainpc world household spawn",
            command(AINPCSubcommandRoute.WORLD, "world", "settlement", "spawn", "village") to
                "ainpc world settlement spawn",
            command(AINPCSubcommandRoute.WORLD, "world", "settlement", "auto", "village") to
                "ainpc world settlement auto",
            command(AINPCSubcommandRoute.WORLD, "world", "save") to "ainpc world save",
        )

        for ((case, expected) in cases) {
            assertEquals(expected, AINPCCommandMutationPolicy.blockedOperation(case.route, case.args))
        }

        for (mode in listOf("region", "place", "node")) {
            for (action in listOf("create", "edit", "remove", "delete")) {
                assertEquals(
                    "ainpc world $mode $action",
                    AINPCCommandMutationPolicy.blockedOperation(
                        AINPCSubcommandRoute.WORLD,
                        arrayOf("world", mode.uppercase(), action.uppercase()),
                    ),
                )
            }
        }
    }

    @Test
    fun keepsPlanningInspectionAndDryRunsAvailable() {
        val cases = listOf(
            command(AINPCSubcommandRoute.WORLD, "world", "places"),
            command(AINPCSubcommandRoute.WORLD, "world", "outside", "report", "village"),
            command(AINPCSubcommandRoute.WORLD, "world", "fixture", "plan"),
            command(AINPCSubcommandRoute.WORLD, "world", "fixture", "validate"),
            command(AINPCSubcommandRoute.WORLD, "world", "fixture", "context"),
            command(AINPCSubcommandRoute.WORLD, "world", "fixture", "populate", "--dry-run"),
            command(AINPCSubcommandRoute.WORLD, "world", "scan", "village", "32"),
            command(AINPCSubcommandRoute.WORLD, "world", "settlement", "plan", "village"),
            command(AINPCSubcommandRoute.WORLD, "world", "household", "plan", "house"),
            command(AINPCSubcommandRoute.WORLD, "world", "bindings", "list"),
            command(AINPCSubcommandRoute.WORLD, "world", "create", "ai", "preview", "region"),
            command(AINPCSubcommandRoute.PATCH, "patch", "plan", "village"),
            command(AINPCSubcommandRoute.MAP, "map", "preview"),
            command(AINPCSubcommandRoute.MAP, "map", "cancel"),
            command(AINPCSubcommandRoute.BUILDING, "building", "templates"),
            command(AINPCSubcommandRoute.WAND, "wand", "pos1"),
        )

        for (case in cases) {
            assertNull(
                AINPCCommandMutationPolicy.blockedOperation(case.route, case.args),
                "Ruta read-only a fost clasificata gresit: ${case.args.joinToString(" ")}",
            )
        }
    }

    @Test
    fun requiresExplicitConfirmationAndStripsOnlyTheFinalFlag() {
        val args = arrayOf("world", "place", "remove", "sat:house")
        val confirmedArgs = args + "--confirm"
        val mutation = AINPCCommandMutationPolicy.mutation(AINPCSubcommandRoute.WORLD, args)

        assertNotNull(mutation)
        assertTrue(mutation!!.requiresConfirmation)
        assertTrue(mutation.worldSaveReminder)
        assertFalse(AINPCCommandMutationPolicy.hasExplicitConfirmation(args))
        assertTrue(AINPCCommandMutationPolicy.hasExplicitConfirmation(confirmedArgs))
        assertEquals(args.toList(), AINPCCommandMutationPolicy.executionArgs(confirmedArgs).toList())
        assertEquals(
            "/ainpc world place remove sat:house --confirm",
            AINPCCommandMutationPolicy.confirmationCommand(args),
        )
        assertTrue(AINPCCommandMutationPolicy.requiresConfirmation("ainpc world place remove sat:house"))
        assertFalse(AINPCCommandMutationPolicy.requiresConfirmation("ainpc world place remove sat:house --confirm"))
        assertEquals(
            "ainpc world place remove sat:house --confirm",
            AINPCCommandMutationPolicy.confirmedCommand("ainpc world place remove sat:house"),
        )
        assertEquals(
            "ainpc world scan village 48",
            AINPCCommandMutationPolicy.confirmedCommand("ainpc world scan village 48"),
        )
    }

    @Test
    fun treatsDraftConfirmationAndSaveAsAlreadyExplicit() {
        val mapMutation = AINPCCommandMutationPolicy.mutation(
            AINPCSubcommandRoute.MAP,
            arrayOf("map", "confirm"),
        )
        val saveMutation = AINPCCommandMutationPolicy.mutation(
            AINPCSubcommandRoute.WORLD,
            arrayOf("world", "save"),
        )

        assertNotNull(mapMutation)
        assertFalse(mapMutation!!.requiresConfirmation)
        assertTrue(mapMutation.worldSaveReminder)
        assertNotNull(saveMutation)
        assertFalse(saveMutation!!.requiresConfirmation)
        assertFalse(saveMutation.worldSaveReminder)
        assertFalse(AINPCCommandMutationPolicy.requiresConfirmation("ainpc map confirm"))
        assertFalse(AINPCCommandMutationPolicy.requiresConfirmation("ainpc world save"))
    }

    @Test
    fun defersSaveReminderUntilIncrementalMappingFinishes() {
        val scanImport = AINPCCommandMutationPolicy.mutation(
            AINPCSubcommandRoute.WORLD,
            arrayOf("world", "scan", "village", "48", "import"),
        )
        val settlementAuto = AINPCCommandMutationPolicy.mutation(
            AINPCSubcommandRoute.WORLD,
            arrayOf("world", "settlement", "auto", "village"),
        )
        val settlementSpawn = AINPCCommandMutationPolicy.mutation(
            AINPCSubcommandRoute.WORLD,
            arrayOf("world", "settlement", "spawn", "village"),
        )

        assertNotNull(scanImport)
        assertFalse(scanImport!!.worldSaveReminder)
        assertNotNull(settlementAuto)
        assertFalse(settlementAuto!!.worldSaveReminder)
        assertNotNull(settlementSpawn)
        assertTrue(settlementSpawn!!.worldSaveReminder)
    }

    @Test
    fun commandDispatcherAppliesPolicyBeforeRouteHandling() {
        val source = File("src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt").readText()
        val policyLookup = source.indexOf("AINPCCommandMutationPolicy.mutation(subCommand, args)")
        val confirmationGate = source.indexOf("adminMutation?.requiresConfirmation == true")
        val argumentNormalization = source.indexOf("AINPCCommandMutationPolicy.executionArgs(args)")
        val routeDispatch = source.indexOf("val handled = when (subCommand)")
        val saveReminder = source.indexOf("adminMutation?.worldSaveReminder == true")

        assertTrue(policyLookup >= 0, "Dispatcher-ul nu consulta politica mutatiilor.")
        assertTrue(confirmationGate > policyLookup, "Confirmarea trebuie verificata dupa clasificare.")
        assertTrue(argumentNormalization > confirmationGate, "Flag-ul trebuie eliminat numai dupa confirmare.")
        assertTrue(routeDispatch > policyLookup, "Politica mutatiilor trebuie aplicata inainte de rutare.")
        assertTrue(saveReminder > routeDispatch, "Reminder-ul de save trebuie trimis dupa rutare.")
        assertTrue(
            source.substring(policyLookup, routeDispatch).contains("isRuntimeReadOnly(plugin)"),
            "Clasificarea mutatiei nu este legata de starea runtime read-only.",
        )
        assertTrue(source.contains("handleWorld(sender, executionArgs)"))
        assertTrue(source.contains("handlePatch(sender, executionArgs)"))
        assertTrue(source.contains("handleBuilding(sender, executionArgs)"))
        assertTrue(source.contains("plugin.platform.worldAdminService.hasUnsavedChanges()"))
    }

    @Test
    fun guiBridgesExistingAndDirectMutationConfirmations() {
        val source = File("src/main/kotlin/ro/ainpc/gui/GuiService.kt").readText()

        assertTrue(source.contains("AINPCCommandMutationPolicy.confirmedCommand(command)"))
        assertTrue(source.contains("AINPCCommandMutationPolicy.confirmedCommand(request.command())"))
        assertTrue(source.contains("AINPCCommandMutationPolicy.requiresConfirmation(normalized)"))
        assertTrue(source.contains("Confirma mutatia"))
    }

    private fun command(route: AINPCSubcommandRoute, vararg args: String): CommandCase =
        CommandCase(route, arrayOf(*args))

    private data class CommandCase(
        val route: AINPCSubcommandRoute,
        val args: Array<String>,
    )
}
