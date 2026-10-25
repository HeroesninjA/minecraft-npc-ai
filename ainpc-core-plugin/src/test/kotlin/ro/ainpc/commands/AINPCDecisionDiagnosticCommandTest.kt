package ro.ainpc.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
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
import ro.ainpc.utils.MessageUtils
import java.lang.reflect.Proxy
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
    private val sender = Proxy.newProxyInstance(CommandSender::class.java.classLoader,
        arrayOf(CommandSender::class.java)) { _, method, arguments ->
        when (method.name) {
            "hasPermission" -> {
                assertEquals("ainpc.admin", arguments!![0])
                admin
            }
            "sendMessage" -> {
                messages += LegacyComponentSerializer.legacyAmpersand().serialize(arguments!![0] as Component)
                null
            }
            else -> error("Unexpected command sender access: ${method.name}")
        }
    } as CommandSender

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

    private fun setField(owner: Class<*>, target: Any, name: String, value: Any) {
        owner.getDeclaredField(name).apply { isAccessible = true }.set(target, value)
    }
}
