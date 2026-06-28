package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuthoringCommandSupportTest {
    @Test
    fun parsesAuthoringModesAndArguments() {
        assertEquals(
            AuthoringCommandRequest(AuthoringCommandRequest.Mode.OPEN, "quest_a", "mechanic_x"),
            AuthoringCommandSupport.parse(arrayOf("authoring", "quest_a", "mechanic_x"))
        )
        assertEquals(
            AuthoringCommandRequest(AuthoringCommandRequest.Mode.DUMP, "quest_b", "mechanic_y"),
            AuthoringCommandSupport.parse(arrayOf("authoring", "dump", "quest_b", "mechanic_y"))
        )
        assertEquals(
            AuthoringCommandRequest(AuthoringCommandRequest.Mode.SUMMARY, "quest_c", "mechanic_z"),
            AuthoringCommandSupport.parse(arrayOf("authoring", "summary", "quest_c", "mechanic_z"))
        )
        assertEquals(
            AuthoringCommandRequest(AuthoringCommandRequest.Mode.CLEAR, null, null),
            AuthoringCommandSupport.parse(arrayOf("authoring", "clear"))
        )
        assertEquals(
            AuthoringCommandRequest(AuthoringCommandRequest.Mode.PREV, null, null),
            AuthoringCommandSupport.parse(arrayOf("authoring", "prev"))
        )
        assertEquals(
            AuthoringCommandRequest(AuthoringCommandRequest.Mode.NEXT, null, null),
            AuthoringCommandSupport.parse(arrayOf("authoring", "next"))
        )
        assertEquals(
            AuthoringCommandRequest(AuthoringCommandRequest.Mode.CLEAR, null, null),
            AuthoringCommandSupport.parse(arrayOf("authoring", "reset"))
        )
        assertEquals(
            AuthoringCommandRequest(AuthoringCommandRequest.Mode.PREV, null, null),
            AuthoringCommandSupport.parse(arrayOf("authoring", "previous"))
        )
        assertEquals(
            AuthoringCommandRequest(AuthoringCommandRequest.Mode.NEXT, null, null),
            AuthoringCommandSupport.parse(arrayOf("authoring", "forward"))
        )
    }

    @Test
    fun suggestsAuthoringModesWithAliases() {
        assertEquals(
            listOf("clear", "dump", "forward", "next", "prev", "previous", "reset", "summary", "summarize"),
            AuthoringCommandSupport.modeSuggestions("")
        )
        assertEquals(
            listOf("prev"),
            AuthoringCommandSupport.modeSuggestions("pr")
        )
        assertEquals(
            listOf("previous"),
            AuthoringCommandSupport.modeSuggestions("previ")
        )
        assertEquals(
            listOf("forward"),
            AuthoringCommandSupport.modeSuggestions("fo")
        )
    }

    @Test
    fun plansDumpAsConsoleFriendlyAndGuiModesAsPlayerOnly() {
        assertEquals(
            AuthoringCommandPlan(false, AuthoringCommandRequest.Mode.DUMP),
            AuthoringCommandSupport.plan(
                AuthoringCommandRequest(AuthoringCommandRequest.Mode.DUMP, "quest", "mechanic")
            )
        )
        assertEquals(
            AuthoringCommandPlan(false, AuthoringCommandRequest.Mode.SUMMARY),
            AuthoringCommandSupport.plan(
                AuthoringCommandRequest(AuthoringCommandRequest.Mode.SUMMARY, "quest", "mechanic")
            )
        )
        assertEquals(
            AuthoringCommandPlan(true, AuthoringCommandRequest.Mode.OPEN),
            AuthoringCommandSupport.plan(
                AuthoringCommandRequest(AuthoringCommandRequest.Mode.OPEN, "quest", "mechanic")
            )
        )
        assertEquals(
            AuthoringCommandPlan(true, AuthoringCommandRequest.Mode.CLEAR),
            AuthoringCommandSupport.plan(
                AuthoringCommandRequest(AuthoringCommandRequest.Mode.CLEAR, null, null)
            )
        )
    }

    @Test
    fun resolvesDumpSelectionUsingOverridesAndFallbacks() {
        assertEquals(
            AuthoringResolvedSelection("explicit_quest", "explicit_mechanic"),
            AuthoringCommandSupport.resolveDumpSelection(
                AuthoringCommandRequest(AuthoringCommandRequest.Mode.DUMP, "explicit_quest", "explicit_mechanic"),
                "current_quest",
                "current_mechanic"
            )
        )
        assertEquals(
            AuthoringResolvedSelection("current_quest", "current_mechanic"),
            AuthoringCommandSupport.resolveDumpSelection(
                AuthoringCommandRequest(AuthoringCommandRequest.Mode.DUMP, null, null),
                "current_quest",
                "current_mechanic"
            )
        )
        assertEquals(
            AuthoringResolvedSelection(null, null),
            AuthoringCommandSupport.resolveDumpSelection(
                AuthoringCommandRequest(AuthoringCommandRequest.Mode.DUMP, null, null),
                null,
                null
            )
        )
    }
}
