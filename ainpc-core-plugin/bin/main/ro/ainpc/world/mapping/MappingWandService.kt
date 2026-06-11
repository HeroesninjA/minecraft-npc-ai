package ro.ainpc.world.mapping

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import ro.ainpc.AINPCPlugin
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.world.WorldAdminService
import java.time.Instant
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentMap
import kotlin.math.max
import kotlin.math.min

class MappingWandService(private val plugin: AINPCPlugin) {
    private val wandKey = NamespacedKey(plugin, "mapping_wand")
    private val draftFactory = MappingDraftFactory()
    private val sessions: ConcurrentMap<UUID, MappingWandSession> = ConcurrentHashMap()
    private val recentConfirmedDrafts: ConcurrentLinkedDeque<MappingWandAuditEntry> = ConcurrentLinkedDeque()

    fun start(player: Player, mode: MappingWandMode?): MappingWandSession {
        val safeMode = mode ?: MappingWandMode.PLACE
        val session = sessions.compute(player.uniqueId) { _, existing ->
            val selection = existing?.selection() ?: MappingWandSelection.empty()
            val draft = existing?.draft()
            MappingWandSession(safeMode, selection, draft)
        }
        giveWand(player)
        return session!!
    }

    fun ensureSession(player: Player): MappingWandSession {
        return sessions.computeIfAbsent(player.uniqueId) {
            MappingWandSession(MappingWandMode.PLACE, MappingWandSelection.empty(), null)
        }
    }

    fun session(playerId: UUID): Optional<MappingWandSession> = Optional.ofNullable(sessions[playerId])

    fun mode(playerId: UUID): MappingWandMode = session(playerId).map { it.mode() }.orElse(MappingWandMode.PLACE)

    fun setMode(player: Player, mode: MappingWandMode?): MappingWandSession = start(player, mode)

    fun setPos1(player: Player, point: MappingPoint?): MappingWandSession =
        updateSelection(player) { selection -> selection.withPos1(point) }

    fun setPos2(player: Player, point: MappingPoint?): MappingWandSession =
        updateSelection(player) { selection -> selection.withPos2(point) }

    fun setPoint(player: Player, point: MappingPoint?): MappingWandSession =
        updateSelection(player) { selection -> selection.withPoint(point) }

    fun resetPos1(player: Player): MappingWandSession = updateSelection(player) { it.withoutPos1() }

    fun resetPos2(player: Player): MappingWandSession = updateSelection(player) { it.withoutPos2() }

    fun resetPoint(player: Player): MappingWandSession = updateSelection(player) { it.withoutPoint() }

    fun createDraft(
        player: Player,
        explicitKind: MappingDraftKind?,
        description: String?,
        worldAdmin: WorldAdminService?
    ): MappingDraft {
        val session = ensureSession(player)
        val kind = explicitKind ?: session.mode().draftKind()
        val draft = draftFactory.createDraft(
            player.uniqueId,
            kind,
            session.selection(),
            description,
            worldAdmin
        )
        sessions[player.uniqueId] = MappingWandSession(session.mode(), session.selection(), draft)
        return draft
    }

    fun confirmDraft(player: Player, worldAdmin: WorldAdminService?): MappingDraftApplyResult {
        val session = ensureSession(player)
        if (session.draft() == null) {
            throw IllegalArgumentException("Nu exista draft de confirmat. Ruleaza /ainpc map <descriere>.")
        }
        val result = draftFactory.apply(session.draft(), worldAdmin)
        recordConfirmedDraft(player, session.draft(), result.createdId(), result.message())
        sessions[player.uniqueId] = MappingWandSession(session.mode(), session.selection(), null)
        return result
    }

    fun recordConfirmedDraft(player: Player?, draft: MappingDraft?, resultId: String?, resultMessage: String?) {
        if (player == null || draft == null) {
            return
        }
        recentConfirmedDrafts.addFirst(
            MappingWandAuditEntry(
                Instant.now().toEpochMilli(),
                player.uniqueId,
                player.name,
                draft.kind(),
                draft.qualifiedId(),
                resultId,
                resultMessage,
                draft.worldName(),
                draft.regionId(),
                draft.placeId(),
                draft.x(),
                draft.y(),
                draft.z(),
                draft.metadata()
            )
        )
        while (recentConfirmedDrafts.size > MAX_RECENT_CONFIRMED_DRAFTS) {
            recentConfirmedDrafts.pollLast()
        }
    }

    fun recentConfirmedDrafts(): List<MappingWandAuditEntry> = recentConfirmedDrafts.toList()

    fun showSelectionPreview(player: Player?, session: MappingWandSession?) {
        if (player == null || session == null) {
            return
        }
        val selection = session.selection()
        if (session.mode().usesPointSelection() && selection.hasPoint()) {
            val point = requireNotNull(selection.point())
            showRadiusPreview(
                player,
                point.worldName(),
                point.x().toDouble(),
                point.y().toDouble(),
                point.z().toDouble(),
                previewRadiusForMode(session.mode())
            )
            return
        }
        selection.bounds().ifPresent { bounds -> showBoundsPreview(player, bounds) }
    }

    fun showDraftPreview(player: Player?, draft: MappingDraft?) {
        if (player == null || draft == null) {
            return
        }
        if (draft.isBox()) {
            showBoundsPreview(
                player,
                MappingWandSelection.MappingBounds(
                    draft.worldName(),
                    draft.minX(),
                    draft.minY(),
                    draft.minZ(),
                    draft.maxX(),
                    draft.maxY(),
                    draft.maxZ()
                )
            )
            return
        }
        if (draft.isNode() || draft.isNpcBind() || draft.isQuestAnchor()) {
            showRadiusPreview(player, draft.worldName(), draft.x(), draft.y(), draft.z(), draft.radius())
        }
    }

    fun clear(playerId: UUID) {
        sessions.remove(playerId)
    }

    fun cancelDraft(playerId: UUID) {
        val session = sessions[playerId]
        if (session != null) {
            sessions[playerId] = MappingWandSession(session.mode(), session.selection(), null)
        }
    }

    fun isWandItem(stack: ItemStack?): Boolean {
        if (stack == null || stack.type == Material.AIR || !stack.hasItemMeta()) {
            return false
        }
        val meta: ItemMeta = stack.itemMeta ?: return false
        return meta.persistentDataContainer.has(wandKey, PersistentDataType.BYTE)
    }

    fun createWandItem(): ItemStack {
        val stack = ItemStack(Material.BLAZE_ROD)
        val meta = stack.itemMeta
        if (meta != null) {
            meta.displayName(GuiItemFactory.text("&dAINPC Mapping Wand"))
            meta.lore(
                listOf(
                    GuiItemFactory.text("&7Stanga: pos1 pentru region/place"),
                    GuiItemFactory.text("&7Dreapta: pos2 pentru region/place"),
                    GuiItemFactory.text("&7Node/npc_bind/quest_anchor: punct semantic"),
                    GuiItemFactory.text("&8/ainpc wand mode <region|place|node|npc_bind|quest_anchor>")
                )
            )
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            meta.persistentDataContainer.set(wandKey, PersistentDataType.BYTE, 1.toByte())
            stack.itemMeta = meta
        }
        return stack
    }

    private fun giveWand(player: Player) {
        val wand = createWandItem()
        if (player.inventory.firstEmpty() >= 0) {
            player.inventory.addItem(wand)
        } else {
            player.world.dropItemNaturally(player.location, wand)
            plugin.messageUtils.send(player, "&eInventarul este plin; wand-ul a fost lasat langa tine.")
        }
    }

    private fun updateSelection(player: Player, updater: SelectionUpdater): MappingWandSession {
        return sessions.compute(player.uniqueId) { _, existing ->
            val safe = existing ?: MappingWandSession(MappingWandMode.PLACE, MappingWandSelection.empty(), null)
            MappingWandSession(safe.mode(), updater.update(safe.selection()), safe.draft())
        }!!
    }

    private fun showBoundsPreview(player: Player, bounds: MappingWandSelection.MappingBounds) {
        if (!canPreviewInPlayerWorld(player, bounds.worldName())) {
            return
        }

        val stepX = previewStep(bounds.minX(), bounds.maxX())
        val stepY = previewStep(bounds.minY(), bounds.maxY())
        val stepZ = previewStep(bounds.minZ(), bounds.maxZ())

        var x = bounds.minX()
        while (x <= bounds.maxX()) {
            spawnPreviewParticle(player, Particle.END_ROD, x + 0.5, bounds.minY() + 0.5, bounds.minZ() + 0.5)
            spawnPreviewParticle(player, Particle.END_ROD, x + 0.5, bounds.minY() + 0.5, bounds.maxZ() + 0.5)
            spawnPreviewParticle(player, Particle.END_ROD, x + 0.5, bounds.maxY() + 0.5, bounds.minZ() + 0.5)
            spawnPreviewParticle(player, Particle.END_ROD, x + 0.5, bounds.maxY() + 0.5, bounds.maxZ() + 0.5)
            x += stepX
        }
        var y = bounds.minY()
        while (y <= bounds.maxY()) {
            spawnPreviewParticle(player, Particle.END_ROD, bounds.minX() + 0.5, y + 0.5, bounds.minZ() + 0.5)
            spawnPreviewParticle(player, Particle.END_ROD, bounds.maxX() + 0.5, y + 0.5, bounds.minZ() + 0.5)
            spawnPreviewParticle(player, Particle.END_ROD, bounds.minX() + 0.5, y + 0.5, bounds.maxZ() + 0.5)
            spawnPreviewParticle(player, Particle.END_ROD, bounds.maxX() + 0.5, y + 0.5, bounds.maxZ() + 0.5)
            y += stepY
        }
        var z = bounds.minZ()
        while (z <= bounds.maxZ()) {
            spawnPreviewParticle(player, Particle.END_ROD, bounds.minX() + 0.5, bounds.minY() + 0.5, z + 0.5)
            spawnPreviewParticle(player, Particle.END_ROD, bounds.maxX() + 0.5, bounds.minY() + 0.5, z + 0.5)
            spawnPreviewParticle(player, Particle.END_ROD, bounds.minX() + 0.5, bounds.maxY() + 0.5, z + 0.5)
            spawnPreviewParticle(player, Particle.END_ROD, bounds.maxX() + 0.5, bounds.maxY() + 0.5, z + 0.5)
            z += stepZ
        }
    }

    private fun showRadiusPreview(player: Player, worldName: String?, x: Double, y: Double, z: Double, radius: Double) {
        if (!canPreviewInPlayerWorld(player, worldName)) {
            return
        }

        val safeRadius = max(0.75, min(if (radius > 0.0) radius else 2.5, 16.0))
        val centerX = x + 0.5
        val centerY = y + 1.0
        val centerZ = z + 0.5
        for (i in 0 until 32) {
            val angle = (Math.PI * 2.0 * i) / 32.0
            spawnPreviewParticle(
                player,
                Particle.ENCHANT,
                centerX + Math.cos(angle) * safeRadius,
                centerY,
                centerZ + Math.sin(angle) * safeRadius
            )
        }
        spawnPreviewParticle(player, Particle.HAPPY_VILLAGER, centerX, centerY, centerZ)
    }

    private fun canPreviewInPlayerWorld(player: Player, worldName: String?): Boolean {
        return !worldName.isNullOrBlank() &&
            player.world.name.equals(worldName, ignoreCase = true)
    }

    private fun previewStep(min: Int, max: Int): Int = max(1, max(1, max - min) / 10)

    private fun previewRadiusForMode(mode: MappingWandMode): Double {
        return when (mode) {
            MappingWandMode.NPC_BIND -> 1.5
            MappingWandMode.QUEST_ANCHOR -> 2.0
            MappingWandMode.NODE -> 2.5
            else -> 2.5
        }
    }

    private fun spawnPreviewParticle(player: Player, particle: Particle, x: Double, y: Double, z: Double) {
        player.spawnParticle(particle, Location(player.world, x, y, z), 1, 0.02, 0.02, 0.02, 0.0)
    }

    private fun interface SelectionUpdater {
        fun update(selection: MappingWandSelection): MappingWandSelection
    }

    class MappingWandSession(
        mode: MappingWandMode?,
        selection: MappingWandSelection?,
        private val draftValue: MappingDraft?
    ) {
        private val modeValue: MappingWandMode = mode ?: MappingWandMode.PLACE
        private val selectionValue: MappingWandSelection = selection ?: MappingWandSelection.empty()

        fun mode(): MappingWandMode = modeValue
        fun selection(): MappingWandSelection = selectionValue
        fun draft(): MappingDraft? = draftValue
    }

    class MappingWandAuditEntry(
        private val confirmedAtValue: Long,
        private val playerIdValue: UUID?,
        playerName: String?,
        private val kindValue: MappingDraftKind?,
        qualifiedId: String?,
        resultId: String?,
        resultMessage: String?,
        worldName: String?,
        regionId: String?,
        placeId: String?,
        private val xValue: Double,
        private val yValue: Double,
        private val zValue: Double,
        metadata: Map<String, String>?
    ) {
        private val playerNameValue: String = if (playerName.isNullOrBlank()) "<necunoscut>" else playerName.trim()
        private val qualifiedIdValue: String = qualifiedId?.trim().orEmpty()
        private val resultIdValue: String = resultId?.trim().orEmpty()
        private val resultMessageValue: String = resultMessage?.trim().orEmpty()
        private val worldNameValue: String = worldName?.trim().orEmpty()
        private val regionIdValue: String = regionId?.trim().orEmpty()
        private val placeIdValue: String = placeId?.trim().orEmpty()
        private val metadataValue: Map<String, String> = (metadata ?: emptyMap()).toMap()

        fun confirmedAt(): Long = confirmedAtValue
        fun playerId(): UUID? = playerIdValue
        fun playerName(): String = playerNameValue
        fun kind(): MappingDraftKind? = kindValue
        fun qualifiedId(): String = qualifiedIdValue
        fun resultId(): String = resultIdValue
        fun resultMessage(): String = resultMessageValue
        fun worldName(): String = worldNameValue
        fun regionId(): String = regionIdValue
        fun placeId(): String = placeIdValue
        fun x(): Double = xValue
        fun y(): Double = yValue
        fun z(): Double = zValue
        fun metadata(): Map<String, String> = metadataValue
    }

    companion object {
        private const val MAX_RECENT_CONFIRMED_DRAFTS = 25
    }
}
