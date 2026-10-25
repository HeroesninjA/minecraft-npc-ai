package ro.ainpc.listeners

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.PlayerInventory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NpcFoodAid
import java.lang.reflect.Proxy
import java.util.UUID

class NpcFoodInteractionTest {
    private val helperUuid = UUID.randomUUID()
    private val npc = AINPC(null).apply { hungerLevel = 20 }
    private var held: ItemStack? = TestStack(Material.BREAD, 3, "named bread")
    private var sneaking = true
    private var writes = 0
    private var failWrite = false
    private var replacement: ItemStack? = null
    private val inventory = Proxy.newProxyInstance(
        PlayerInventory::class.java.classLoader, arrayOf(PlayerInventory::class.java),
    ) { _, method, arguments ->
        when (method.name) {
            "getHeldItemSlot" -> 0
            "getItemInMainHand" -> held ?: TestStack(Material.AIR, 0)
            "getItem" -> replacement ?: held
            "setItem" -> {
                assertEquals(0, arguments!![0])
                held = arguments[1] as ItemStack?
                writes++
                if (failWrite) {
                    failWrite = false
                    throw IllegalStateException("partial inventory failure")
                }
                null
            }
            else -> throw UnsupportedOperationException(method.name)
        }
    } as PlayerInventory
    private val player = Proxy.newProxyInstance(
        Player::class.java.classLoader, arrayOf(Player::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "isSneaking" -> sneaking
            "getInventory" -> inventory
            "getUniqueId" -> helperUuid
            "getName" -> "Helper"
            else -> throw UnsupportedOperationException(method.name)
        }
    } as Player

    @Test
    fun `intentional food offer consumes one actual item and records its owner`() {
        val result = NpcFoodInteraction.offer(npc, player, 100_000L)!!

        assertEquals(NpcFoodAid.Status.ACCEPTED, result.status)
        assertEquals(2, held!!.amount)
        assertTrue(held!!.isSimilar(TestStack(Material.BREAD, 1, "named bread")))
        assertEquals(40, npc.hungerLevel)
        assertEquals(40, npc.context.hungerLevel)
        assertEquals(helperUuid, npc.socialExperience.snapshot().single().helperUuid)
        assertEquals(1, writes)
    }

    @Test
    fun `paired offhand event cannot transfer a second item while hunger remains active`() {
        npc.hungerLevel = 0
        held = TestStack(Material.APPLE, 3)

        assertEquals(NpcFoodAid.Status.ACCEPTED, NpcFoodInteraction.offer(npc, player, 100_000L)!!.status)
        assertNull(NpcFoodInteraction.offer(npc, player, 100_001L, EquipmentSlot.OFF_HAND))

        assertEquals(2, held!!.amount)
        assertEquals(12, npc.hungerLevel)
        assertEquals(1, npc.socialExperience.snapshot().size)
        assertEquals(1, writes)
    }

    @Test
    fun `the last food item is removed from the selected slot`() {
        held = TestStack(Material.APPLE, 1)

        assertEquals(NpcFoodAid.Status.ACCEPTED, NpcFoodInteraction.offer(npc, player, 100_000L)!!.status)
        assertNull(held)
        assertEquals(32, npc.hungerLevel)
    }

    @Test
    fun `ordinary interaction and unsupported held items leave inventory and needs unchanged`() {
        sneaking = false
        assertNull(NpcFoodInteraction.offer(npc, player, 100_000L))
        sneaking = true
        held = TestStack(Material.STONE, 3)
        assertNull(NpcFoodInteraction.offer(npc, player, 100_000L))

        assertEquals(0, writes)
        assertEquals(20, npc.hungerLevel)
        assertTrue(npc.socialExperience.snapshot().isEmpty())
    }

    @Test
    fun `satisfied need and changed held stack do not consume anything`() {
        npc.hungerLevel = 35
        assertEquals(NpcFoodAid.Status.NOT_HUNGRY, NpcFoodInteraction.offer(npc, player, 100_000L)!!.status)
        npc.hungerLevel = 20
        replacement = TestStack(Material.APPLE, 3)
        assertEquals(NpcFoodAid.Status.TRANSFER_FAILED, NpcFoodInteraction.offer(npc, player, 100_000L)!!.status)

        assertEquals(0, writes)
        assertEquals(3, held!!.amount)
        assertEquals(20, npc.hungerLevel)
        assertTrue(npc.socialExperience.snapshot().isEmpty())
    }

    @Test
    fun `partial inventory failure rolls back the original slot without creating help evidence`() {
        failWrite = true

        val result = NpcFoodInteraction.offer(npc, player, 100_000L)!!

        assertEquals(NpcFoodAid.Status.TRANSFER_FAILED, result.status)
        assertEquals(3, held!!.amount)
        assertTrue(held!!.isSimilar(TestStack(Material.BREAD, 1, "named bread")))
        assertEquals(20, npc.hungerLevel)
        assertTrue(npc.socialExperience.snapshot().isEmpty())
        assertEquals(2, writes)
    }

    private class TestStack(
        private val material: Material,
        private var count: Int,
        private val tag: String = "",
    ) : ItemStack() {
        override fun getType(): Material = material
        override fun getAmount(): Int = count
        override fun setAmount(amount: Int) { count = amount }
        override fun isSimilar(stack: ItemStack?): Boolean =
            stack is TestStack && material == stack.material && tag == stack.tag
        public override fun clone(): ItemStack = TestStack(material, count, tag)
    }
}
