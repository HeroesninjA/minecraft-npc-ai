package ro.ainpc.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.bootstrap.ServiceRegistry
import ro.ainpc.engine.DecisionEngine
import ro.ainpc.managers.NPCManager
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCAction
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NpcCognitiveState
import ro.ainpc.npc.NpcFoodAid
import ro.ainpc.npc.NpcFoodRequestPreferences
import ro.ainpc.utils.MessageUtils
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AINPCDecisionDiagnosticCommandTest {
    private val commandPluginField = Class.forName("ro.ainpc.commands.AINPCCommandMisc")
        .getDeclaredField("ainpcCommandMiscPlugin").apply { isAccessible = true }
    private val previousPlugin = commandPluginField.get(null)
    private val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let { field ->
        field.isAccessible = true
        field.get(null) as sun.misc.Unsafe
    }
    private val plugin = unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
    private val engine = DecisionEngine(plugin)
    private val npc = AINPC(null).apply {
        name = "Bob"
        hungerLevel = 20
        context.hungerLevel = 20
        currentGoal = "revina acasa"
        lastSimulationTickAt = 123L
        changeStateFromSimulation(NPCState.WALKING)
        selfCareGoal.applied(NPCAction.EAT, NpcCognitiveState.capture(this), "world:home", false,
            System.currentTimeMillis())
    }
    private val messages = mutableListOf<String>()
    private var admin = true
    private val sender = createSender()

    init {
        setField(JavaPlugin::class.java, plugin, "newConfig", YamlConfiguration())
        val services = ServiceRegistry(plugin)
        val manager = unsafe.allocateInstance(NPCManager::class.java) as NPCManager
        setField(NPCManager::class.java, manager, "npcsByUuid", ConcurrentHashMap(mapOf(npc.uuid to npc)))
        setField(ServiceRegistry::class.java, services, "npcManager", manager)
        setField(ServiceRegistry::class.java, services, "decisionEngine", engine)
        setField(ServiceRegistry::class.java, services, "messageUtils", MessageUtils(plugin))
        setField(AINPCPlugin::class.java, plugin, "services", services)
        initAinpcCommandMiscPlugin(plugin)
    }

    @AfterEach
    fun restoreCommandPlugin() {
        commandPluginField.set(null, previousPlugin)
    }

    @Test
    fun `ready shadow command lists hypothetical steps without starting travel or care`() {
        plugin.config.set("simulation.decision.care_planner_shadow_enabled", true)
        npc.worldName = "world"
        npc.homeAnchor = AINPC.OwnedLocation("home", "acasa", "world", 1.0, 64.0, 3.0)
        val before = NpcCognitiveState.capture(npc)
        val goal = npc.selfCareGoal.snapshot(System.currentTimeMillis())
        val revision = npc.decisionRevision

        val serverField = Bukkit::class.java.getDeclaredField("server").apply { isAccessible = true }
        val previousServer = serverField.get(null)
        val server = Proxy.newProxyInstance(Server::class.java.classLoader, arrayOf(Server::class.java)) {
            _, method, _ -> if (method.name == "getWorld") null else error("Unexpected server API: ${method.name}")
        } as Server
        serverField.set(null, server)
        try {
            repeat(2) { assertTrue(handleDecision(sender, arrayOf("decision", "Bob"))) }
        } finally {
            serverField.set(null, previousServer)
        }

        assertTrue(messages.any { it.contains("preferata: EAT") })
        assertTrue(messages.any { it.contains("RETURN_HOME -> EAT_AT_HOME -> OBSERVE_RECOVERY") })
        assertTrue(messages.any { it.contains("ruta, resursele si succesul raman neverificate") })
        assertEquals(before, NpcCognitiveState.capture(npc))
        assertEquals(goal, npc.selfCareGoal.snapshot(System.currentTimeMillis()))
        assertEquals(revision, npc.decisionRevision)
        assertEquals(123L, npc.lastSimulationTickAt)
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
        assertNull(npc.lastDecision)
    }

    @Test
    fun `shadow flag exposes a non executing proposal without altering the command decision state`() {
        plugin.config.set("simulation.decision.care_planner_shadow_enabled", true)
        val before = NpcCognitiveState.capture(npc)
        val revision = npc.decisionRevision

        repeat(2) { assertTrue(handleDecision(sender, arrayOf("decision", "Bob"))) }

        assertTrue(messages.any { it.contains("Plan ingrijire [SHADOW, fara executie]") })
        assertTrue(messages.any { it.contains("casa nu este confirmata") })
        assertFalse(messages.any { it.contains("Pasi propusi:") })
        assertEquals(before, NpcCognitiveState.capture(npc))
        assertEquals(revision, npc.decisionRevision)
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
        assertNull(npc.lastDecision)
    }

    @Test
    fun `actual named decision command includes read only cognitive diagnostics without a server`() {
        val before = NpcCognitiveState.capture(npc)
        val goal = npc.selfCareGoal.snapshot(System.currentTimeMillis())
        val stateRevision = npc.stateRevision
        val decisionRevision = npc.decisionRevision

        repeat(2) { assertTrue(handleDecision(sender, arrayOf("decision", "Bob"))) }

        assertTrue(messages.any { it.contains("Atentie:") })
        assertTrue(messages.any { it.contains("Ultima decizie aplicata:") && it.contains("fara inregistrare recenta") })
        assertTrue(messages.any { it.contains("EAT / RETURNING_HOME") })
        assertTrue(messages.any { it.contains("Continuitate ingrijire:") && it.contains("goal_continuity_enabled") })
        assertFalse(messages.any { it.contains("Plan ingrijire [SHADOW") })
        assertEquals(before, NpcCognitiveState.capture(npc))
        assertEquals(goal, npc.selfCareGoal.snapshot(System.currentTimeMillis()))
        assertEquals(stateRevision, npc.stateRevision)
        assertEquals(decisionRevision, npc.decisionRevision)
        assertEquals(123L, npc.lastSimulationTickAt)
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
        assertNull(npc.lastDecision)
        assertTrue(npc.actionExperience.snapshot().isEmpty())
        val cache = DecisionEngine::class.java.getDeclaredField("decisionCache").apply { isAccessible = true }
            .get(engine) as Map<*, *>
        assertTrue(cache.isEmpty())
    }

    @Test
    fun `permission and console usage checks never expose cognitive diagnostics`() {
        admin = false
        assertTrue(handleDecision(sender, arrayOf("decision", "Bob")))
        assertEquals(1, messages.size)
        assertTrue(messages.single().contains("no_permission"))
        messages.clear()
        admin = true
        assertTrue(handleDecision(sender, arrayOf("decision")))
        assertEquals(1, messages.size)
        assertTrue(messages.single().contains("Utilizare:"))
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
    }

    @Test
    fun `named command reads a confirmed transfer without consuming again or changing evidence`() {
        val helperUuid = UUID.randomUUID()
        val now = System.currentTimeMillis()
        var inventoryAmount = 3
        val result = NpcFoodAid.offer(npc, helperUuid, "Alex", "minecraft:bread", now,
            consumeOne = { inventoryAmount--; true }, rollback = { inventoryAmount++ })
        assertEquals(NpcFoodAid.Status.ACCEPTED, result.status)
        val receipts = npc.socialExperience.snapshot()
        val state = NpcCognitiveState.capture(npc)
        val stateRevision = npc.stateRevision
        val decisionRevision = npc.decisionRevision

        repeat(2) { assertTrue(handleDecision(sender, arrayOf("decision", "Bob"))) }

        assertTrue(messages.any { it.contains("1 inregistrari pastrate") })
        assertTrue(messages.any { it.contains("id=${result.receipt!!.id}; recordedAt=$now") })
        assertTrue(messages.any { it.contains("minecraft:bread x1; satietate 20 -> 40") &&
            it.contains("UUID=$helperUuid") })
        assertTrue(messages.any { it.contains("Pauza cereri hrana:") && it.contains("neinspectata") })
        assertEquals(2, inventoryAmount)
        assertEquals(receipts, npc.socialExperience.snapshot())
        assertEquals(state, NpcCognitiveState.capture(npc))
        assertEquals(stateRevision, npc.stateRevision)
        assertEquals(decisionRevision, npc.decisionRevision)
        assertEquals(123L, npc.lastSimulationTickAt)
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
        assertTrue(npc.actionExperience.snapshot().isEmpty())
    }

    @Test
    fun `player command shows only its own pause even for players with the same name`() {
        val helperUuid = UUID.randomUUID()
        val otherUuid = UUID.randomUUID()
        val now = System.currentTimeMillis()
        assertTrue(npc.foodRequestPreferences.defer(helperUuid, NpcFoodRequestPreferences.Reply.PROMISE, now))
        val helper = createSender(helperUuid)
        val other = createSender(otherUuid)

        repeat(2) { assertTrue(handleDecision(helper, arrayOf("decision", "Bob"))) }

        assertTrue(messages.any { it.contains("Pauza cereri hrana (UUID=$helperUuid)") && it.contains("PROMISE") })
        assertEquals(NpcFoodRequestPreferences.Reply.PROMISE, npc.foodRequestPreferences.activeReply(helperUuid, now))
        assertTrue(npc.socialExperience.snapshot().isEmpty())
        messages.clear()
        assertTrue(handleDecision(other, arrayOf("decision", "Bob")))
        assertTrue(messages.any { it.contains("UUID=$otherUuid") && it.contains("fara refuz sau promisiune activa") })
        assertFalse(messages.any { it.contains("PROMISE") || it.contains(helperUuid.toString()) })
        assertTrue(npc.foodRequestPreferences.defer(helperUuid, NpcFoodRequestPreferences.Reply.DECLINE, now))
        messages.clear()
        assertTrue(handleDecision(helper, arrayOf("decision", "Bob")))
        assertTrue(messages.any { it.contains("DECLINE - refuz respectat") })
        assertFalse(messages.any { it.contains("PROMISE") })
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
    }

    @Test
    fun `food diagnostic snapshot stays frozen after the promised delivery`() {
        val helperUuid = UUID.randomUUID()
        val now = 1_000_000L
        assertTrue(npc.foodRequestPreferences.defer(helperUuid, NpcFoodRequestPreferences.Reply.PROMISE, now))
        val before = engine.cognitiveDiagnostic(npc, now = now, viewerUuid = helperUuid)
        val beforeLines = formatDecisionCognition(before)
        val result = NpcFoodAid.offer(npc, helperUuid, "Alex", "minecraft:bread", now + 1L,
            consumeOne = { true }, rollback = { error("Unexpected rollback") })
        assertEquals(NpcFoodAid.Status.ACCEPTED, result.status)

        val after = engine.cognitiveDiagnostic(npc, now = now + 1L, viewerUuid = helperUuid)

        assertEquals(now, before.capturedAt)
        assertEquals(0, before.foodAid.retainedReceipts)
        assertNull(before.foodAid.lastReceipt)
        assertEquals(NpcFoodRequestPreferences.Reply.PROMISE, before.foodAid.activeReply)
        assertEquals(20, before.state.hungerLevel)
        assertEquals(beforeLines, formatDecisionCognition(before))
        assertEquals(1, after.foodAid.retainedReceipts)
        assertEquals(result.receipt, after.foodAid.lastReceipt)
        assertNull(after.foodAid.activeReply)
        assertEquals(40, after.state.hungerLevel)
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
    }

    @Test
    fun `diagnostic expiry reads do not prune or renew pauses and never create receipts`() {
        val helperUuid = UUID.randomUUID()
        val now = 1_000_000L
        val before = NpcCognitiveState.capture(npc)
        for (reply in NpcFoodRequestPreferences.Reply.entries) {
            assertTrue(npc.foodRequestPreferences.defer(helperUuid, reply, now))
            val deadline = now + reply.delayMillis
            assertEquals(reply, engine.cognitiveDiagnostic(npc, now = deadline - 1L,
                viewerUuid = helperUuid).foodAid.activeReply)
            assertNull(engine.cognitiveDiagnostic(npc, now = deadline, viewerUuid = helperUuid).foodAid.activeReply)
            assertNull(engine.cognitiveDiagnostic(npc, now = deadline + 1L, viewerUuid = helperUuid).foodAid.activeReply)
            assertEquals(reply, npc.foodRequestPreferences.activeReply(helperUuid, now + 1L))
            assertEquals(reply, engine.cognitiveDiagnostic(npc, now = now - 1L,
                viewerUuid = helperUuid).foodAid.activeReply)
        }
        assertEquals(before, NpcCognitiveState.capture(npc))
        assertTrue(npc.socialExperience.snapshot().isEmpty())
        assertEquals(0, engine.decisionStatistics(npc.uuid).total)
    }

    private fun createSender(viewerUuid: UUID? = null): CommandSender = Proxy.newProxyInstance(
        CommandSender::class.java.classLoader,
        arrayOf(if (viewerUuid == null) CommandSender::class.java else Player::class.java),
    ) { _, method, arguments ->
        when (method.name) {
            "hasPermission" -> {
                assertEquals("ainpc.admin", arguments!![0])
                admin
            }
            "sendMessage" -> {
                messages += LegacyComponentSerializer.legacyAmpersand().serialize(arguments!![0] as Component)
                null
            }
            "getUniqueId" -> viewerUuid
            "getName" -> "Alex"
            else -> error("Unexpected command sender access: ${method.name}")
        }
    } as CommandSender

    private fun setField(owner: Class<*>, target: Any, name: String, value: Any) {
        owner.getDeclaredField(name).apply { isAccessible = true }.set(target, value)
    }
}
