package ro.ainpc.engine

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.bukkit.metadata.MetadataValue
import org.bukkit.plugin.PluginManager
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitScheduler
import org.bukkit.scheduler.BukkitTask
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.events.dialog.DialogMessageReceivedEvent
import ro.ainpc.bootstrap.ServiceRegistry
import ro.ainpc.gui.GuiService
import ro.ainpc.listeners.NPCChatListener
import ro.ainpc.listeners.NpcFoodInteraction
import ro.ainpc.managers.ConversationSessionManager
import ro.ainpc.managers.NPCManager
import ro.ainpc.managers.buildProfileData
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCState
import ro.ainpc.npc.NpcInteractionProfile
import ro.ainpc.npc.NpcFoodAid
import ro.ainpc.npc.NpcFoodRequestPreferences
import ro.ainpc.npc.NpcSimulationMode
import ro.ainpc.npc.NpcSocialExperience
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.logging.Handler
import java.util.logging.LogRecord
import java.util.logging.Logger

class NpcFoodRequestServiceTest {
    @Test
    fun `async chat routes a recent food refusal on the main thread with passive listening disabled`() {
        val fixture = Fixture(System.currentTimeMillis())
        val chat = ChatHarness(fixture)
        assertTrue(fixture.request())
        val event = chat.event("Bob, nu pot acum")

        CompletableFuture.runAsync { chat.listener.onAsyncChat(event) }.get(5, TimeUnit.SECONDS)

        assertTrue(event.isCancelled)
        assertEquals(1, fixture.sent.size)
        assertFalse(fixture.npc.foodRequestPreferences.isDeferred(fixture.helper.id, fixture.now))
        chat.drain()
        assertTrue(fixture.npc.foodRequestPreferences.isDeferred(fixture.helper.id, System.currentTimeMillis()))
        assertEquals(2, fixture.sent.size)
        assertEquals(1, chat.received.size)
        assertNull(fixture.sessions.getConversationSessionId(fixture.helper.player))
    }

    @Test
    fun `an active conversation keeps priority over a pending food reply even with the same NPC`() {
        val fixture = Fixture(System.currentTimeMillis())
        val chat = ChatHarness(fixture)
        assertTrue(fixture.request())
        for (partner in listOf(fixture.npc, AINPC(null))) {
            fixture.sessions.startConversation(fixture.helper.player, partner)
            val session = fixture.sessions.getConversationSessionId(fixture.helper.player)

            assertFalse(chat.replyDirect("Nu pot acum"))

            assertFalse(fixture.npc.foodRequestPreferences.isDeferred(fixture.helper.id, fixture.now))
            assertEquals(session, fixture.sessions.getConversationSessionId(fixture.helper.player))
            assertEquals(1, fixture.sent.size)
            fixture.sessions.clearConversation(fixture.helper.player)
        }
        assertTrue(chat.replyDirect("Nu pot acum"))
        assertTrue(fixture.npc.foodRequestPreferences.isDeferred(fixture.helper.id, System.currentTimeMillis()))
    }

    @Test
    fun `cancelled chat and public event veto cannot create a food refusal preference`() {
        val fixture = Fixture(System.currentTimeMillis())
        val chat = ChatHarness(fixture)
        assertTrue(fixture.request())
        val cancelled = chat.event("Nu pot acum").apply { isCancelled = true }
        chat.listener.onAsyncChat(cancelled)
        chat.drain()
        assertTrue(chat.received.isEmpty())
        assertFalse(fixture.npc.foodRequestPreferences.isDeferred(fixture.helper.id, fixture.now))

        chat.veto = true
        chat.listener.onAsyncChat(chat.event("Nu pot acum"))
        chat.drain()
        assertFalse(fixture.npc.foodRequestPreferences.isDeferred(fixture.helper.id, fixture.now))
        assertEquals(1, fixture.sent.size)
        chat.veto = false
        chat.listener.onAsyncChat(chat.event("Nu pot acum"))
        chat.drain()
        assertTrue(fixture.npc.foodRequestPreferences.isDeferred(fixture.helper.id, System.currentTimeMillis()))
    }

    @Test
    fun `decline and promise postpone only that helper without crediting help or changing needs`() {
        for ((message, duration) in listOf("Nu pot acum" to 900_000L, "Iti aduc mancare" to 600_000L)) {
            val fixture = Fixture()
            val receipts = fixture.npc.socialExperience.snapshot()
            val needs = fixture.needs()
            assertTrue(fixture.request())
            fixture.npc.changeStateFromSimulation(NPCState.EATING)

            assertTrue(fixture.service.tryReply(fixture.helper.player, message, fixture.now + 1L))
            assertFalse(fixture.service.tryReply(fixture.helper.player, message, fixture.now + 2L))
            assertTrue(fixture.npc.foodRequestPreferences.isDeferred(fixture.helper.id, fixture.now + 1L))
            assertEquals(receipts, fixture.npc.socialExperience.snapshot())
            assertEquals(needs, fixture.needs())
            assertEquals(NPCState.EATING, fixture.npc.currentState)
            assertNull(fixture.sessions.getConversationSessionId(fixture.helper.player))
            fixture.npc.changeStateFromSimulation(NPCState.IDLE)
            assertFalse(fixture.service.tryRequest(fixture.npc, fixture.now + duration))
            assertTrue(fixture.service.tryRequest(fixture.npc, fixture.now + duration + 1L))
            assertEquals(3, fixture.sent.size)
        }
    }

    @Test
    fun `absent failed expired wrong player and unrelated replies cannot create a deferral`() {
        val fixture = Fixture()
        assertFalse(fixture.service.tryReply(fixture.helper.player, "Nu pot acum", fixture.now))
        fixture.failSend = true
        assertFalse(fixture.request())
        fixture.failSend = false
        assertFalse(fixture.service.tryReply(fixture.helper.player, "Nu pot acum", fixture.now))
        assertTrue(fixture.request())
        val stranger = fixture.newPlayer(UUID.randomUUID(), "Helper", 2.0)
        assertFalse(fixture.service.tryReply(stranger.player, "Nu pot acum", fixture.now))
        for (message in listOf("Maria, nu pot acum", "Am zis nu pot acum", "Ti-am dat mancare", "Da")) {
            assertFalse(fixture.service.tryReply(fixture.helper.player, message, fixture.now), message)
        }
        assertFalse(fixture.service.tryReply(fixture.helper.player, "Nu pot acum", fixture.now + 45_000L))
        assertFalse(fixture.npc.foodRequestPreferences.isDeferred(fixture.helper.id, fixture.now))
        assertEquals(1, fixture.sent.size)
    }

    @Test
    fun `reply rechecks identity lifecycle world safety visibility flags and busy conversations`() {
        val changes: List<Pair<String, Fixture.() -> Unit>> = listOf(
            "removed" to { registry.clear() },
            "replaced with same UUID" to {
                newNpc(npc.uuid).socialExperience.restore(npc.socialExperience.snapshot())
            },
            "lifecycle changed" to { npc.markEntityUnavailable() },
            "mode changed and restored" to { npc.simulationMode = NpcSimulationMode.LIGHT; npc.simulationMode = NpcSimulationMode.FULL },
            "danger" to { npc.context.isInDanger = true },
            "not hungry" to { npc.hungerLevel = 35 },
            "sleeping" to { npc.changeStateFromSimulation(NPCState.SLEEPING) },
            "another NPC conversation" to { sessions.startConversation(helper.player, AINPC(null)) },
            "same NPC conversation" to { sessions.startConversation(helper.player, npc) },
            "NPC speaking with another player" to { sessions.startConversation(newPlayer(UUID.randomUUID(), "Other", 2.0).player, npc) },
            "input opened" to { busyPlayers.add(helper.id) },
            "disconnected" to { helper.connected = false },
            "different world" to { helper.world = world("other") },
            "out of range" to { helper.positionX = 8.0 },
            "hidden NPC" to { helper.canSeeNpc = false },
            "wall" to { lineOfSight = false },
            "requests disabled" to { config.set("simulation.food_requests_enabled", false) },
            "aid disabled" to { config.set("simulation.food_help_enabled", false) },
        )
        changes.forEach { (label, change) ->
            val fixture = Fixture()
            assertTrue(fixture.request(), label)
            fixture.change()
            assertFalse(fixture.service.tryReply(fixture.helper.player, "Nu pot acum", fixture.now + 1L), label)
            assertFalse(fixture.npc.foodRequestPreferences.isDeferred(fixture.helper.id, fixture.now + 1L), label)
            assertEquals(1, fixture.sent.size, label)
        }
    }

    @Test
    fun `vetoed replies preserve opportunity and state changes inside the event invalidate acceptance`() {
        val fixture = Fixture()
        assertTrue(fixture.request())
        assertTrue(fixture.service.tryReply(fixture.helper.player, "Bob, nu pot acum", fixture.now) { false })
        assertFalse(fixture.npc.foodRequestPreferences.isDeferred(fixture.helper.id, fixture.now))
        assertEquals(1, fixture.sent.size)

        assertTrue(fixture.service.tryReply(fixture.helper.player, "Bob, nu pot acum", fixture.now) {
            fixture.npc.markEntityUnavailable()
            true
        })
        assertFalse(fixture.npc.foodRequestPreferences.isDeferred(fixture.helper.id, fixture.now))
        assertEquals(1, fixture.sent.size)
    }

    @Test
    fun `new physical food transfer invalidates the outstanding request even when hunger remains`() {
        val fixture = Fixture()
        assertTrue(fixture.request())
        val result = NpcFoodAid.offer(
            fixture.npc, fixture.helper.id, "Helper", "minecraft:apple", fixture.now + 1L,
            consumeOne = { true }, rollback = { error("unexpected rollback") },
        )
        assertEquals(NpcFoodAid.Status.ACCEPTED, result.status)
        assertEquals(32, fixture.npc.hungerLevel)

        assertFalse(fixture.service.tryReply(fixture.helper.player, "Nu pot acum", fixture.now + 2L))
        assertEquals(2, fixture.npc.socialExperience.snapshot().size)
        assertFalse(fixture.npc.foodRequestPreferences.isDeferred(fixture.helper.id, fixture.now))
    }

    @Test
    fun `acknowledgement failure keeps the accepted pause and logout only discards the pending reply`() {
        val fixture = Fixture()
        assertTrue(fixture.request())
        fixture.failSend = true
        assertTrue(fixture.service.tryReply(fixture.helper.player, "Iti aduc mancare", fixture.now))
        fixture.service.forgetPlayer(fixture.helper.id)
        assertTrue(fixture.npc.foodRequestPreferences.isDeferred(fixture.helper.id, fixture.now))

        val other = Fixture()
        assertTrue(other.request())
        other.service.forgetPlayer(other.helper.id)
        assertFalse(other.service.tryReply(other.helper.player, "Nu pot acum", other.now))
        assertFalse(other.npc.foodRequestPreferences.isDeferred(other.helper.id, other.now))
    }

    @Test
    fun `main thread check precedes all world configuration and delivery access`() {
        val service = NpcFoodRequestService(
            config = { error("configuration read off thread") },
            isNpcInConversation = { error("conversation read off thread") },
            isPlayerBusy = { error("player read off thread") },
            send = { _, _, _ -> error("delivery off thread") },
            isMainThread = { false },
        )

        assertFalse(service.tryRequest(AINPC(null), 1_000_000_000L))
    }

    @Test
    fun `live flags disable requests without consuming a later opportunity`() {
        for (flag in listOf("simulation.food_requests_enabled", "simulation.food_help_enabled")) {
            val fixture = Fixture()
            fixture.config.set(flag, false)

            assertFalse(fixture.request(), flag)
            assertTrue(fixture.sent.isEmpty(), flag)
            fixture.config.set(flag, true)
            assertTrue(fixture.request(), flag)
            assertEquals(1, fixture.sent.size, flag)
        }
    }

    @Test
    fun `only full simulation and full AI interaction permit autonomous requests`() {
        NpcSimulationMode.entries.filter { it != NpcSimulationMode.FULL }.forEach { mode ->
            val fixture = Fixture()
            fixture.npc.simulationMode = mode
            assertFalse(fixture.request(), mode.name)
            assertTrue(fixture.sent.isEmpty(), mode.name)
        }
        NpcInteractionProfile.entries.filter { it != NpcInteractionProfile.FULL_AI }.forEach { profile ->
            val fixture = Fixture()
            fixture.npc.interactionProfile = profile
            assertFalse(fixture.request(), profile.name)
            assertTrue(fixture.sent.isEmpty(), profile.name)
        }
    }

    @Test
    fun `unavailable NPCs competing needs and stored interlocutors suppress requests`() {
        val cases: List<Pair<String, Fixture.() -> Unit>> = listOf(
            "unspawned" to { npc.spawned = false },
            "missing entity" to { npc.bukkitEntity = null },
            "invalid entity" to { entityValid = false },
            "dead entity" to { entityDead = true },
            "non villager" to { npc.bukkitEntity = nonVillager() },
            "satisfied hunger" to { npc.hungerLevel = 35 },
            "invalid hunger" to { npc.hungerLevel = -1 },
            "danger" to { npc.context.isInDanger = true },
            "low safety" to { npc.safetyLevel = 39 },
            "invalid safety" to { npc.safetyLevel = 101 },
            "stronger fatigue" to { npc.energyLevel = 0 },
            "stored player" to { npc.context.setInteractingPlayer(helper.player) },
            "stored NPC" to { npc.context.interactingNPC = AINPC(null) },
            "active NPC conversation" to { sessions.startConversation(helper.player, npc) },
        )

        cases.forEach { (label, change) ->
            val fixture = Fixture().apply(change)
            assertFalse(fixture.request(), label)
            assertTrue(fixture.sent.isEmpty(), label)
        }
        for (timestamp in listOf(0L, -1L)) {
            val fixture = Fixture()
            assertFalse(fixture.service.tryRequest(fixture.npc, timestamp))
            assertTrue(fixture.sent.isEmpty())
        }
    }

    @Test
    fun `requests are restricted to available states and never interrupt busy states`() {
        val available = setOf(NPCState.IDLE, NPCState.WALKING, NPCState.WAITING, NPCState.RESTING, NPCState.CURIOUS)

        NPCState.entries.forEach { state ->
            val fixture = Fixture()
            fixture.npc.changeStateFromSimulation(state)

            assertEquals(state in available, fixture.request(), state.name)
            assertEquals(if (state in available) 1 else 0, fixture.sent.size, state.name)
            assertEquals(state, fixture.npc.currentState)
        }
    }

    @Test
    fun `exact helper evidence excludes namesakes absent expired and future receipts`() {
        val mutations: List<Pair<String, Fixture.() -> Unit>> = listOf(
            "missing receipt" to { npc.socialExperience.restore(emptyList()) },
            "expired receipt" to { remember(helper, timestamp = now - WEEK - 1L, replace = true) },
            "future receipt" to { remember(helper, timestamp = now + 1L, replace = true) },
            "same name stranger" to {
                val stranger = newPlayer(UUID(0L, 99L), helper.name, 1.0)
                npc.context.nearbyPlayers.clear()
                npc.context.nearbyPlayers.add(stranger.player)
            },
        )

        mutations.forEach { (label, change) ->
            val fixture = Fixture().apply(change)
            assertFalse(fixture.request(), label)
            assertTrue(fixture.sent.isEmpty(), label)
        }
        val fixture = Fixture()
        fixture.remember(fixture.helper, timestamp = fixture.now - WEEK, replace = true)
        assertTrue(fixture.request())
        assertEquals(fixture.helper.id, fixture.sent.single().playerUuid)
    }

    @Test
    fun `offline disconnected hidden dead spectator invalid and busy players receive no request`() {
        val changes: List<Pair<String, Fixture.() -> Unit>> = listOf(
            "offline" to { helper.online = false },
            "disconnected" to { helper.connected = false },
            "invalid" to { helper.valid = false },
            "dead" to { helper.dead = true },
            "spectator" to { helper.gameMode = GameMode.SPECTATOR },
            "invisible" to { helper.invisible = true },
            "vanished" to { helper.vanished = true },
            "cannot see NPC" to { helper.canSeeNpc = false },
            "blocked line of sight" to { lineOfSight = false },
            "busy input" to { busyPlayers.add(helper.id) },
            "other conversation" to { sessions.startConversation(helper.player, AINPC(null)) },
            "other world" to { helper.world = world("other") },
            "distant" to { helper.positionX = 5.01 },
            "invalid position" to { helper.positionX = Double.NaN },
            "infinite position" to { helper.positionX = Double.POSITIVE_INFINITY },
            "invalid NPC position" to { npcX = Double.NaN },
        )

        changes.forEach { (label, change) ->
            val fixture = Fixture().apply(change)
            assertFalse(fixture.request(), label)
            assertTrue(fixture.sent.isEmpty(), label)
        }
    }

    @Test
    fun `distance uses current locations and the configured radius capped at six blocks`() {
        for (invalidRadius in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            val fixture = Fixture()
            fixture.config.set("npc.interaction_distance", invalidRadius)
            assertFalse(fixture.request(), invalidRadius.toString())
        }
        for ((radius, distance, expected) in listOf(
            Triple(2.0, 2.0, true), Triple(2.0, 2.01, false),
            Triple(100.0, 6.0, true), Triple(100.0, 6.01, false),
        )) {
            val fixture = Fixture()
            fixture.config.set("npc.interaction_distance", radius)
            fixture.helper.positionX = distance
            assertEquals(expected, fixture.request(), "$radius / $distance")
        }
    }

    @Test
    fun `nearest eligible exact helper receives the only message with deterministic UUID ties`() {
        val fixture = Fixture()
        val nearer = fixture.newPlayer(UUID(0L, 2L), fixture.helper.name, 1.0)
        val tied = fixture.newPlayer(UUID(0L, 3L), fixture.helper.name, 1.0)
        val stranger = fixture.newPlayer(UUID(0L, 4L), fixture.helper.name, 0.5)
        fixture.remember(nearer)
        fixture.remember(tied)
        fixture.npc.context.nearbyPlayers.addAll(listOf(tied.player, stranger.player, nearer.player))

        assertTrue(fixture.request())
        assertFalse(fixture.request())
        assertEquals(listOf(nearer.id), fixture.sent.map { it.playerUuid })
    }

    @Test
    fun `failed send preserves the opportunity for a successful retry`() {
        val fixture = Fixture()
        fixture.failSend = true

        assertFalse(fixture.request())
        assertTrue(fixture.sent.isEmpty())
        fixture.failSend = false
        assertTrue(fixture.request())
        assertEquals(1, fixture.sent.size)
    }

    @Test
    fun `a throttled nearest helper allows a request to the next eligible helper`() {
        val fixture = Fixture()
        val secondHelper = fixture.newPlayer(UUID(0L, 2L), "Second", 3.0)
        fixture.remember(secondHelper)
        fixture.npc.context.nearbyPlayers.add(secondHelper.player)
        val secondNpc = fixture.newNpc(UUID(0L, 11L))
        fixture.remember(fixture.helper, npc = secondNpc)

        assertTrue(fixture.service.tryRequest(secondNpc, fixture.now))
        assertTrue(fixture.request())
        assertEquals(listOf(fixture.helper.id, secondHelper.id), fixture.sent.map { it.playerUuid })
        assertEquals(listOf(secondNpc.uuid, fixture.npc.uuid), fixture.sent.map { it.npcUuid })
    }

    @Test
    fun `successful private send stays on the main thread without changing needs evidence goals or sessions`() {
        val fixture = Fixture()
        val otherPlayer = fixture.newPlayer(UUID(0L, 20L), "Other", 30.0)
        val otherNpc = AINPC(null)
        fixture.sessions.startConversation(otherPlayer.player, otherNpc)
        val otherSession = fixture.sessions.getConversationSessionId(otherPlayer.player)
        fixture.npc.currentGoal = "sa ajung acasa"
        fixture.npc.context.lastPlayerMessage = "mesaj existent"
        val needs = fixture.needs()
        val receipts = fixture.npc.socialExperience.snapshot()
        val interactionAt = fixture.npc.context.lastInteractionTime

        assertTrue(fixture.request())

        val message = fixture.sent.single()
        assertEquals(fixture.helper.id, message.playerUuid)
        assertEquals(fixture.npc.uuid, message.npcUuid)
        assertTrue(message.text.contains("M-ai ajutat cu mancare inainte"))
        assertTrue(message.text.contains("Shift + click dreapta"))
        assertSame(Thread.currentThread(), message.thread)
        assertEquals(needs, fixture.needs())
        assertEquals(receipts, fixture.npc.socialExperience.snapshot())
        assertTrue(fixture.npc.actionExperience.snapshot().isEmpty())
        assertEquals(NPCState.IDLE, fixture.npc.currentState)
        assertEquals("sa ajung acasa", fixture.npc.currentGoal)
        assertEquals("mesaj existent", fixture.npc.context.lastPlayerMessage)
        assertEquals(interactionAt, fixture.npc.context.lastInteractionTime)
        assertNull(fixture.npc.context.interactingPlayer)
        assertNull(fixture.sessions.getConversationSessionId(fixture.helper.player))
        assertEquals(otherSession, fixture.sessions.getConversationSessionId(otherPlayer.player))
    }

    @Test
    fun `two NPCs share the player minute limit while each keeps its own five minute limit`() {
        val fixture = Fixture()
        val second = fixture.newNpc(UUID(0L, 11L))
        fixture.remember(fixture.helper, npc = second)

        assertTrue(fixture.request())
        assertFalse(fixture.service.tryRequest(second, fixture.now))
        assertFalse(fixture.service.tryRequest(second, fixture.now + 59_999L))
        assertTrue(fixture.service.tryRequest(second, fixture.now + 60_000L))
        assertFalse(fixture.service.tryRequest(fixture.npc, fixture.now + 299_999L))
        assertTrue(fixture.service.tryRequest(fixture.npc, fixture.now + 300_000L))
        assertEquals(listOf(fixture.npc.uuid, second.uuid, fixture.npc.uuid), fixture.sent.map { it.npcUuid })
        assertEquals(20, fixture.npc.hungerLevel)
        assertEquals(20, second.hungerLevel)
    }

    @Test
    fun `actual inventory delivery fulfils a promise without bypassing request cooldowns`() {
        val fixture = Fixture(seedHelper = false)
        assertFalse(fixture.request())
        assertEquals(NpcFoodAid.Status.ACCEPTED,
            NpcFoodInteraction.offer(fixture.npc, fixture.helper.player, fixture.now - 1L)!!.status)
        assertEquals(2, fixture.helper.heldFood!!.amount)
        assertEquals(32, fixture.npc.hungerLevel)
        assertEquals(1, fixture.npc.socialExperience.snapshot().size)
        assertTrue(fixture.request())
        val needs = fixture.needs()
        val evidence = fixture.npc.socialExperience.snapshot()
        assertTrue(fixture.service.tryReply(fixture.helper.player, "Iti aduc mancare", fixture.now + 1L))
        assertTrue(fixture.npc.foodRequestPreferences.isDeferred(fixture.helper.id, fixture.now + 1L))
        assertEquals(needs, fixture.needs())
        assertEquals(evidence, fixture.npc.socialExperience.snapshot())

        assertEquals(NpcFoodAid.Status.ACCEPTED,
            NpcFoodInteraction.offer(fixture.npc, fixture.helper.player, fixture.now + 2L)!!.status)
        assertEquals(1, fixture.helper.heldFood!!.amount)
        assertEquals(44, fixture.npc.hungerLevel)
        assertEquals(44, fixture.npc.context.hungerLevel)
        assertEquals(2, fixture.npc.socialExperience.snapshot().size)
        assertFalse(fixture.npc.foodRequestPreferences.isDeferred(fixture.helper.id, fixture.now + 2L))
        assertEquals(2, fixture.sent.size)
        assertFalse(fixture.service.tryReply(fixture.helper.player, "Nu pot acum", fixture.now + 3L))
        fixture.npc.hungerLevel = 20
        fixture.npc.context.hungerLevel = 20
        assertFalse(fixture.service.tryRequest(fixture.npc, fixture.now + 299_999L))
        assertTrue(fixture.service.tryRequest(fixture.npc, fixture.now + 300_000L))
        assertEquals(3, fixture.sent.size)
        assertEquals(fixture.helper.id, fixture.sent.last().playerUuid)
        assertTrue(fixture.npc.actionExperience.snapshot().isEmpty())
        assertNull(fixture.sessions.getConversationSessionId(fixture.helper.player))
    }

    @Test
    fun `namesake delivery and a rolled back inventory write cannot fulfil another helper promise`() {
        val fixture = Fixture(seedHelper = false)
        assertEquals(NpcFoodAid.Status.ACCEPTED,
            NpcFoodInteraction.offer(fixture.npc, fixture.helper.player, fixture.now - 1L)!!.status)
        assertTrue(fixture.request())
        assertTrue(fixture.service.tryReply(fixture.helper.player, "Iti aduc mancare", fixture.now + 1L))
        val namesake = fixture.newPlayer(UUID(0L, 2L), fixture.helper.name, 1.0)
        assertEquals(NpcFoodAid.Status.ACCEPTED,
            NpcFoodInteraction.offer(fixture.npc, namesake.player, fixture.now + 2L)!!.status)
        assertTrue(fixture.npc.foodRequestPreferences.isDeferred(fixture.helper.id, fixture.now + 2L))
        assertEquals(namesake.id, fixture.npc.socialExperience.snapshot().last().helperUuid)

        fixture.npc.hungerLevel = 20
        fixture.npc.context.hungerLevel = 20
        fixture.helper.failInventoryWrite = true
        val evidence = fixture.npc.socialExperience.snapshot()
        assertEquals(NpcFoodAid.Status.TRANSFER_FAILED,
            NpcFoodInteraction.offer(fixture.npc, fixture.helper.player, fixture.now + 3L)!!.status)
        assertEquals(2, fixture.helper.heldFood!!.amount)
        assertEquals(20, fixture.npc.hungerLevel)
        assertEquals(20, fixture.npc.context.hungerLevel)
        assertEquals(evidence, fixture.npc.socialExperience.snapshot())
        assertTrue(fixture.npc.foodRequestPreferences.isDeferred(fixture.helper.id, fixture.now + 3L))
        assertFalse(fixture.service.tryRequest(fixture.npc, fixture.now + 300_000L))
        assertEquals(2, fixture.sent.size)
    }

    @Test
    fun `voluntary physical help does not cancel an explicitly declined request pause`() {
        val fixture = Fixture()
        assertTrue(fixture.request())
        assertTrue(fixture.service.tryReply(fixture.helper.player, "Nu pot acum", fixture.now + 1L))
        assertEquals(NpcFoodAid.Status.ACCEPTED,
            NpcFoodInteraction.offer(fixture.npc, fixture.helper.player, fixture.now + 2L)!!.status)
        assertTrue(fixture.npc.foodRequestPreferences.isDeferred(fixture.helper.id, fixture.now + 2L))
        assertEquals(2, fixture.helper.heldFood!!.amount)
        fixture.npc.hungerLevel = 20
        fixture.npc.context.hungerLevel = 20
        assertFalse(fixture.service.tryRequest(fixture.npc, fixture.now + 300_000L))
        assertTrue(fixture.service.tryRequest(fixture.npc, fixture.now + 900_001L))
    }

    @Test
    fun `actual food delivery survives profile hydration and requests only its exact helper`() {
        val fixture = Fixture(seedHelper = false)
        assertFalse(fixture.request())
        assertEquals(NpcFoodAid.Status.ACCEPTED,
            NpcFoodInteraction.offer(fixture.npc, fixture.helper.player, fixture.now - 1L)!!.status)
        val receipts = fixture.npc.socialExperience.snapshot()
        val profile = buildProfileData(fixture.npc, Gson())
        val namesake = fixture.newPlayer(UUID(0L, 2L), fixture.helper.name, 1.0)

        val restored = fixture.restoreProfile(profile)
        restored.context.nearbyPlayers.add(namesake.player)

        assertNotEquals(fixture.npc.runtimeIdentity, restored.runtimeIdentity)
        assertEquals(fixture.npc.uuid, restored.uuid)
        assertEquals(receipts, restored.socialExperience.snapshot())
        assertEquals(32, restored.hungerLevel)
        assertEquals(2, fixture.helper.heldFood!!.amount)
        assertTrue(fixture.service.tryRequest(restored, fixture.now))
        assertEquals(listOf(fixture.helper.id), fixture.sent.map { it.playerUuid })
        assertEquals(receipts, fixture.npc.socialExperience.snapshot())
        assertEquals(receipts, restored.socialExperience.snapshot())
        assertTrue(restored.actionExperience.snapshot().isEmpty())
        assertNull(fixture.sessions.getConversationSessionId(fixture.helper.player))
    }

    @Test
    fun `profile hydration is idempotent and never turns an undelivered promise into another receipt`() {
        val fixture = Fixture(seedHelper = false)
        assertEquals(NpcFoodAid.Status.ACCEPTED,
            NpcFoodInteraction.offer(fixture.npc, fixture.helper.player, fixture.now - 1L)!!.status)
        assertTrue(fixture.request())
        assertTrue(fixture.service.tryReply(fixture.helper.player, "Iti aduc mancare", fixture.now + 1L))
        val receipts = fixture.npc.socialExperience.snapshot()
        var profile = buildProfileData(fixture.npc, Gson())

        repeat(3) {
            val restored = fixture.restoreProfile(profile)

            assertEquals(receipts, restored.socialExperience.snapshot())
            assertEquals(32, restored.hungerLevel)
            assertNull(restored.foodRequestPreferences.activeReply(fixture.helper.id, fixture.now + 2L))
            assertEquals(NpcFoodRequestPreferences.Reply.PROMISE,
                fixture.npc.foodRequestPreferences.activeReply(fixture.helper.id, fixture.now + 2L))
            assertTrue(restored.actionExperience.snapshot().isEmpty())
            assertEquals(2, fixture.helper.heldFood!!.amount)
            assertEquals(2, fixture.sent.size)
            profile = buildProfileData(restored, Gson())
        }
        assertEquals(receipts, fixture.npc.socialExperience.snapshot())
    }

    @Test
    fun `restored missing malformed expired and future evidence cannot enable a food request`() {
        val source = Fixture(seedHelper = false)
        assertEquals(NpcFoodAid.Status.ACCEPTED,
            NpcFoodInteraction.offer(source.npc, source.helper.player, source.now - 1L)!!.status)
        val serialized = buildProfileData(source.npc, Gson())
        val cases: Map<String, (JsonObject) -> Unit> = linkedMapOf(
            "missing" to { simulation -> simulation.remove("social_experience") },
            "unsupported version" to { simulation ->
                simulation.getAsJsonObject("social_experience").addProperty("version", 2)
            },
            "malformed amount" to { simulation ->
                simulation.getAsJsonObject("social_experience").getAsJsonArray("receipts")[0]
                    .asJsonObject.addProperty("amount", 2)
            },
            "expired" to { simulation ->
                simulation.getAsJsonObject("social_experience").getAsJsonArray("receipts")[0]
                    .asJsonObject.addProperty("recorded_at", source.now - WEEK - 1L)
            },
            "future" to { simulation ->
                simulation.getAsJsonObject("social_experience").getAsJsonArray("receipts")[0]
                    .asJsonObject.addProperty("recorded_at", source.now + 1L)
            },
        )
        for ((reason, mutate) in cases) {
            val fixture = Fixture(seedHelper = false)
            val profile = Gson().fromJson(serialized, JsonObject::class.java)
            mutate(profile.getAsJsonObject("simulation"))
            val restored = fixture.restoreProfile(Gson().toJson(profile))
            val receipts = restored.socialExperience.snapshot()

            assertEquals(32, restored.hungerLevel, reason)
            assertFalse(fixture.service.tryRequest(restored, fixture.now), reason)
            assertTrue(fixture.sent.isEmpty(), reason)
            assertEquals(receipts, restored.socialExperience.snapshot(), reason)
            assertEquals(3, fixture.helper.heldFood!!.amount, reason)
            assertTrue(restored.actionExperience.snapshot().isEmpty(), reason)
        }
        assertEquals(2, source.helper.heldFood!!.amount)
        assertEquals(1, source.npc.socialExperience.snapshot().size)
    }

    @Test
    fun `rehydration rejects an old pending reply and retains the existing service cooldown`() {
        val fixture = Fixture(seedHelper = false)
        assertEquals(NpcFoodAid.Status.ACCEPTED,
            NpcFoodInteraction.offer(fixture.npc, fixture.helper.player, fixture.now - 1L)!!.status)
        assertTrue(fixture.request())
        val restored = fixture.restoreProfile(buildProfileData(fixture.npc, Gson()))

        assertFalse(fixture.service.tryReply(fixture.helper.player, "Iti aduc mancare", fixture.now + 1L))
        assertNull(restored.foodRequestPreferences.activeReply(fixture.helper.id, fixture.now + 1L))
        assertNull(fixture.npc.foodRequestPreferences.activeReply(fixture.helper.id, fixture.now + 1L))
        assertFalse(fixture.service.tryRequest(restored, fixture.now + 299_999L))
        assertTrue(fixture.service.tryRequest(restored, fixture.now + 300_000L))
        assertTrue(fixture.service.tryReply(fixture.helper.player, "Iti aduc mancare", fixture.now + 300_001L))
        assertEquals(NpcFoodRequestPreferences.Reply.PROMISE,
            restored.foodRequestPreferences.activeReply(fixture.helper.id, fixture.now + 300_001L))
        assertEquals(3, fixture.sent.size)
        assertEquals(fixture.npc.socialExperience.snapshot(), restored.socialExperience.snapshot())
        assertEquals(1, restored.socialExperience.snapshot().size)
        assertEquals(2, fixture.helper.heldFood!!.amount)
    }

    private class Fixture(val now: Long = 1_000_000_000L, seedHelper: Boolean = true) {
        val world = world("village")
        val config = YamlConfiguration().apply {
            set("simulation.food_requests_enabled", true)
            set("simulation.food_help_enabled", true)
            set("npc.interaction_distance", 5.0)
        }
        val busyPlayers = mutableSetOf<UUID>()
        val registry = mutableMapOf<UUID, AINPC>()
        val sessions = ConversationSessionManager(plugin()) { now }
        val sent = mutableListOf<Sent>()
        var entityValid = true
        var entityDead = false
        var lineOfSight = true
        var npcX = 0.0
        var failSend = false
        val helper = newPlayer(UUID(0L, 1L), "Helper", 2.0)
        val npc = newNpc(UUID(0L, 10L))
        private val mainThread = Thread.currentThread()
        val service = NpcFoodRequestService(
            config = { config },
            isNpcInConversation = sessions::hasActiveConversation,
            isPlayerBusy = { sessions.isInConversation(it) || it.uniqueId in busyPlayers },
            send = { player, source, text ->
                assertSame(mainThread, Thread.currentThread())
                if (failSend) throw IllegalStateException("delivery failed")
                sent.add(Sent(player.uniqueId, source.uuid, text, Thread.currentThread()))
            },
            isMainThread = { Thread.currentThread() === mainThread },
            findNpc = { registry[it] },
        )

        init {
            if (seedHelper) remember(helper)
        }

        fun request(): Boolean = service.tryRequest(npc, now)

        fun restoreProfile(profile: String): AINPC {
            val restored = newNpc(npc.uuid).apply { profileDataJson = profile }
            val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let { field ->
                field.isAccessible = true
                field.get(null) as sun.misc.Unsafe
            }
            val manager = unsafe.allocateInstance(NPCManager::class.java) as NPCManager
            NPCManager::class.java.getDeclaredField("plugin").apply { isAccessible = true }.set(manager, plugin())
            NPCManager::class.java.getDeclaredField("gson").apply { isAccessible = true }.set(manager, Gson())
            NPCManager::class.java.getDeclaredMethod("hydrateProfileRuntimeData", AINPC::class.java)
                .apply { isAccessible = true }.invoke(manager, restored)
            return restored
        }

        fun needs(): List<Int> = listOf(
            npc.hungerLevel, npc.energyLevel, npc.socialNeedLevel, npc.comfortLevel, npc.safetyLevel,
            npc.context.hungerLevel, npc.context.energyLevel,
        )

        fun newPlayer(id: UUID, name: String, positionX: Double): PlayerHandle = PlayerHandle(id, name, world, positionX)

        fun newNpc(id: UUID): AINPC = AINPC(null).apply {
            uuid = id
            name = "Bob"
            hungerLevel = 20
            context.hungerLevel = 20
            spawned = true
            bukkitEntity = Proxy.newProxyInstance(Villager::class.java.classLoader, arrayOf(Villager::class.java)) { proxy, method, args ->
                when (method.name) {
                    "isValid" -> entityValid
                    "isDead" -> entityDead
                    "getWorld" -> world
                    "getLocation" -> Location(world, npcX, 64.0, 0.0)
                    "getUniqueId" -> id
                    "hasLineOfSight" -> lineOfSight
                    "hasAI", "hasGravity", "isInvulnerable", "isCollidable", "isPersistent" -> true
                    "isSilent", "getRemoveWhenFarAway" -> false
                    "setAI", "setGravity", "setInvulnerable", "setSilent", "setCollidable",
                    "setPersistent", "setRemoveWhenFarAway" -> null
                    "equals" -> proxy === args?.firstOrNull()
                    "hashCode" -> System.identityHashCode(proxy)
                    "toString" -> "Villager($id)"
                    else -> throw UnsupportedOperationException("Villager.${method.name}")
                }
            } as Villager
            context.nearbyPlayers.add(helper.player)
            registry[id] = this
        }

        fun nonVillager(): Entity = Proxy.newProxyInstance(Entity::class.java.classLoader, arrayOf(Entity::class.java)) { _, method, _ ->
            when (method.name) {
                "isValid" -> true
                else -> throw UnsupportedOperationException("Entity.${method.name}")
            }
        } as Entity

        fun remember(player: PlayerHandle, timestamp: Long = now - 1L, replace: Boolean = false, npc: AINPC = this.npc) {
            val receipt = NpcSocialExperience.Receipt(
                UUID.randomUUID(), player.id, player.name, "minecraft:bread", 1, 10, 30, timestamp,
            )
            npc.socialExperience.restore((if (replace) emptyList() else npc.socialExperience.snapshot()) + receipt)
        }
    }

    private class ChatHarness(private val fixture: Fixture) {
        private val scheduled = LinkedBlockingQueue<Runnable>()
        val received = mutableListOf<DialogMessageReceivedEvent>()
        var veto = false
        val listener: NPCChatListener

        init {
            val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let { field ->
                field.isAccessible = true
                field.get(null) as sun.misc.Unsafe
            }
            val plugin = unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
            setField(JavaPlugin::class.java, plugin, "logger", Logger.getAnonymousLogger().apply {
                useParentHandlers = false
                addHandler(object : Handler() {
                    override fun publish(record: LogRecord) { throw AssertionError(record.message) }
                    override fun flush() = Unit
                    override fun close() = Unit
                })
            })
            val services = ServiceRegistry(plugin)
            val engine = DecisionEngine(plugin)
            setField(DecisionEngine::class.java, engine, "foodRequests", fixture.service)
            setField(ServiceRegistry::class.java, services, "decisionEngine", engine)
            setField(ServiceRegistry::class.java, services, "conversationSessionManager", fixture.sessions)
            val gui = unsafe.allocateInstance(GuiService::class.java) as GuiService
            setField(GuiService::class.java, gui, "textInputRequests", ConcurrentHashMap<UUID, GuiService.TextInputRequest>())
            setField(ServiceRegistry::class.java, services, "guiService", gui)
            setField(AINPCPlugin::class.java, plugin, "services", services)
            fixture.config.set("dialog.passive_listen_enabled", false)
            setField(JavaPlugin::class.java, plugin, "newConfig", fixture.config)
            val task = Proxy.newProxyInstance(BukkitTask::class.java.classLoader, arrayOf(BukkitTask::class.java)) { _, method, _ ->
                when (method.name) {
                    "getTaskId" -> 1
                    "isSync" -> true
                    else -> null
                }
            } as BukkitTask
            val scheduler = Proxy.newProxyInstance(BukkitScheduler::class.java.classLoader, arrayOf(BukkitScheduler::class.java)) { _, method, args ->
                when (method.name) {
                    "runTask" -> {
                        scheduled.add(args!![1] as Runnable)
                        task
                    }
                    else -> throw UnsupportedOperationException(method.name)
                }
            } as BukkitScheduler
            val manager = Proxy.newProxyInstance(PluginManager::class.java.classLoader, arrayOf(PluginManager::class.java)) { _, method, args ->
                when (method.name) {
                    "callEvent" -> {
                        val event = args!![0] as DialogMessageReceivedEvent
                        received.add(event)
                        event.isCancelled = veto
                        null
                    }
                    else -> throw UnsupportedOperationException(method.name)
                }
            } as PluginManager
            val server = Proxy.newProxyInstance(Server::class.java.classLoader, arrayOf(Server::class.java)) { _, method, _ ->
                when (method.name) {
                    "getScheduler" -> scheduler
                    "getPluginManager" -> manager
                    else -> throw UnsupportedOperationException(method.name)
                }
            } as Server
            setField(JavaPlugin::class.java, plugin, "server", server)
            listener = unsafe.allocateInstance(NPCChatListener::class.java) as NPCChatListener
            setField(ro.ainpc.listeners.AbstractPluginListener::class.java, listener, "plugin", plugin)
        }

        fun event(message: String): AsyncChatEvent {
            val constructor = AsyncChatEvent::class.java.constructors.single { it.parameterCount == 7 }
            val parameters = constructor.parameterTypes
            val renderer = Proxy.newProxyInstance(parameters[3].classLoader, arrayOf(parameters[3])) { _, _, _ -> Component.text(message) }
            val signature = Proxy.newProxyInstance(parameters[6].classLoader, arrayOf(parameters[6])) { _, _, _ -> null }
            return constructor.newInstance(
                true, fixture.helper.player, mutableSetOf<Any>(), renderer,
                Component.text(message), Component.text(message), signature,
            ) as AsyncChatEvent
        }

        fun drain() {
            while (true) (scheduled.poll() ?: return).run()
        }

        fun replyDirect(message: String): Boolean = NPCChatListener::class.java.getDeclaredMethod(
            "handleFoodRequestReply", Player::class.java, String::class.java,
        ).apply { isAccessible = true }.invoke(listener, fixture.helper.player, message) as Boolean

        private fun setField(owner: Class<*>, target: Any, name: String, value: Any) {
            owner.getDeclaredField(name).apply {
                isAccessible = true
                set(target, value)
            }
        }
    }

    private class PlayerHandle(val id: UUID, val name: String, var world: World, var positionX: Double) {
        var online = true
        var connected = true
        var valid = true
        var dead = false
        var gameMode = GameMode.SURVIVAL
        var invisible = false
        var vanished = false
        var canSeeNpc = true
        var heldFood: ItemStack? = TestStack(Material.APPLE, 3)
        var failInventoryWrite = false
        private val inventory = Proxy.newProxyInstance(
            PlayerInventory::class.java.classLoader, arrayOf(PlayerInventory::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getHeldItemSlot" -> 0
                "getItemInMainHand" -> heldFood ?: TestStack(Material.AIR, 0)
                "getItem" -> {
                    assertEquals(0, args!![0])
                    heldFood
                }
                "setItem" -> {
                    assertEquals(0, args!![0])
                    heldFood = args[1] as ItemStack?
                    if (failInventoryWrite) {
                        failInventoryWrite = false
                        throw IllegalStateException("partial inventory failure")
                    }
                    null
                }
                else -> throw UnsupportedOperationException("PlayerInventory.${method.name}")
            }
        } as PlayerInventory
        val player = Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { proxy, method, args ->
            when (method.name) {
                "getUniqueId" -> id
                "getName" -> name
                "getWorld" -> world
                "getLocation" -> Location(world, positionX, 64.0, 0.0)
                "isOnline" -> online
                "isConnected" -> connected
                "isValid" -> valid
                "isDead" -> dead
                "getGameMode" -> gameMode
                "isInvisible" -> invisible
                "isSneaking" -> true
                "getInventory" -> inventory
                "getMetadata" -> {
                    assertEquals("vanished", args?.firstOrNull())
                    if (vanished) listOf(vanishedMetadata()) else emptyList<MetadataValue>()
                }
                "canSee" -> canSeeNpc
                "equals" -> proxy === args?.firstOrNull()
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "Player($id)"
                else -> throw UnsupportedOperationException("Player.${method.name}")
            }
        } as Player

        private fun vanishedMetadata(): MetadataValue =
            Proxy.newProxyInstance(MetadataValue::class.java.classLoader, arrayOf(MetadataValue::class.java)) { _, method, _ ->
                when (method.name) {
                    "asBoolean" -> true
                    else -> throw UnsupportedOperationException("MetadataValue.${method.name}")
                }
            } as MetadataValue
    }

    private class TestStack(private val material: Material, private var count: Int) : ItemStack() {
        override fun getType(): Material = material
        override fun getAmount(): Int = count
        override fun setAmount(amount: Int) { count = amount }
        override fun isSimilar(stack: ItemStack?): Boolean = stack is TestStack && material == stack.material
        public override fun clone(): ItemStack = TestStack(material, count)
    }

    private data class Sent(val playerUuid: UUID, val npcUuid: UUID, val text: String, val thread: Thread)

    companion object {
        private const val WEEK = 7 * 24 * 60 * 60 * 1_000L

        private fun plugin(): AINPCPlugin {
            val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").let {
                it.isAccessible = true
                it.get(null) as sun.misc.Unsafe
            }
            return unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
        }

        private fun world(name: String): World =
            Proxy.newProxyInstance(World::class.java.classLoader, arrayOf(World::class.java)) { proxy, method, args ->
                when (method.name) {
                    "getName" -> name
                    "equals" -> proxy === args?.firstOrNull()
                    "hashCode" -> System.identityHashCode(proxy)
                    "toString" -> "World($name)"
                    else -> throw UnsupportedOperationException("World.${method.name}")
                }
            } as World
    }
}
