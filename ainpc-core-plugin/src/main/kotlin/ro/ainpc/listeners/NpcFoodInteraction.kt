package ro.ainpc.listeners

import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NpcFoodAid

internal object NpcFoodInteraction {
    fun offer(npc: AINPC, player: Player, now: Long, hand: EquipmentSlot = EquipmentSlot.HAND): NpcFoodAid.Result? {
        if (hand != EquipmentSlot.HAND || !player.isSneaking) return null
        val inventory = player.inventory
        val slot = inventory.heldItemSlot
        val original = inventory.itemInMainHand.clone()
        val itemKey = original.type.key.asString()
        if (original.amount <= 0 || NpcFoodAid.foodGain(itemKey) == 0) return null

        return NpcFoodAid.offer(
            npc, player.uniqueId, player.name, itemKey, now,
            consumeOne = {
                val current = inventory.getItem(slot)
                if (inventory.heldItemSlot != slot || current == null ||
                    current.amount != original.amount || !current.isSimilar(original)
                ) {
                    false
                } else {
                    inventory.setItem(slot, if (current.amount == 1) null else current.clone().apply {
                        amount = current.amount - 1
                    })
                    true
                }
            },
            rollback = { inventory.setItem(slot, original.clone()) },
        )
    }
}
