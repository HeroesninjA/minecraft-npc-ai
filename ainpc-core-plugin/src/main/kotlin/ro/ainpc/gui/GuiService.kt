package ro.ainpc.gui

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import ro.ainpc.AINPCPlugin
import ro.ainpc.gui.screens.AuditGui
import ro.ainpc.gui.screens.ConfirmActionGui
import ro.ainpc.gui.screens.DebugGui
import ro.ainpc.gui.screens.MainHubGui
import ro.ainpc.gui.screens.NpcManagerGui
import ro.ainpc.gui.screens.PlaceholderGui
import ro.ainpc.gui.screens.QuestAuthoringGui
import ro.ainpc.gui.screens.QuestDetailGui
import ro.ainpc.gui.screens.QuestLogGui
import ro.ainpc.gui.screens.RoutineGui
import ro.ainpc.gui.screens.StatsGui
import ro.ainpc.gui.screens.StoryGui
import ro.ainpc.gui.screens.WorldHubGui
import ro.ainpc.gui.screens.WorldPlaceGui
import ro.ainpc.gui.screens.WorldRegionGui
import ro.ainpc.gui.screens.AdminMappingGui
import ro.ainpc.gui.screens.AdminQuestGui
import ro.ainpc.gui.screens.NpcInteractionGui
import ro.ainpc.gui.screens.QuestMapGui
import java.util.EnumMap
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class GuiService(private val plugin: AINPCPlugin) {
    private val sessionManager = GuiSessionManager()
    private val screens: MutableMap<GuiKey, GuiScreen> = EnumMap(GuiKey::class.java)
    private val questDetailSelectors: ConcurrentMap<UUID, String> = ConcurrentHashMap()
    private val questDetailFilters: ConcurrentMap<UUID, String> = ConcurrentHashMap()
    private val authoringQuestSelectors: ConcurrentMap<UUID, String> = ConcurrentHashMap()
    private val authoringMechanicIds: ConcurrentMap<UUID, String> = ConcurrentHashMap()
    private val questLogFilters: ConcurrentMap<UUID, String> = ConcurrentHashMap()
    private val questLogPages: ConcurrentMap<UUID, Int> = ConcurrentHashMap()
    private val confirmRequests: ConcurrentMap<UUID, ConfirmRequest> = ConcurrentHashMap()
    private val placeDetailIds: ConcurrentMap<UUID, String> = ConcurrentHashMap()
    private val regionDetailIds: ConcurrentMap<UUID, String> = ConcurrentHashMap()
    private val adminMappingPages: ConcurrentMap<UUID, Int> = ConcurrentHashMap()
    private val questMapTemplateIds: ConcurrentMap<UUID, String> = ConcurrentHashMap()
    private val questMapObjectiveKeys: ConcurrentMap<UUID, String> = ConcurrentHashMap()
    private val questMapMechanicFilters: ConcurrentMap<UUID, String> = ConcurrentHashMap()

    fun getPlaceDetailId(player: Player?): String {
        if (player == null) return ""
        return placeDetailIds.getOrDefault(player.uniqueId, "")
    }

    fun setPlaceDetailId(player: Player?, placeId: String?) {
        if (player == null) return
        if (placeId.isNullOrBlank()) placeDetailIds.remove(player.uniqueId)
        else placeDetailIds[player.uniqueId] = placeId
    }

    fun openPlaceDetail(player: Player?, placeId: String?) {
        if (player == null || placeId.isNullOrBlank()) return
        placeDetailIds[player.uniqueId] = placeId
        open(player, GuiKey.PLACE)
    }

    fun getRegionDetailId(player: Player?): String {
        if (player == null) return ""
        return regionDetailIds.getOrDefault(player.uniqueId, "")
    }

    fun openRegionDetail(player: Player?, regionId: String?) {
        if (player == null || regionId.isNullOrBlank()) return
        regionDetailIds[player.uniqueId] = regionId
        open(player, GuiKey.REGION)
    }

    init {
        register(MainHubGui())
        register(QuestLogGui())
        register(QuestDetailGui())
        register(StoryGui())
        register(WorldHubGui())
        register(WorldPlaceGui())
        register(WorldRegionGui())
        register(AdminMappingGui())
        register(AdminQuestGui())
        register(StatsGui())
        register(NpcInteractionGui())
        register(RoutineGui())
        register(NpcManagerGui())
        register(AuditGui())
        register(DebugGui())
        register(QuestAuthoringGui())
        register(QuestMapGui())
        register(ConfirmActionGui())
        register(PlaceholderGui(GuiKey.SHOP, "Shop NPC", "Nu exista inca un serviciu shop conectat."))
    }

    fun sessions(): GuiSessionManager = sessionManager

    fun open(player: Player?, key: GuiKey?) {
        if (player == null || key == null) {
            return
        }
        if (!canOpen(player, key)) {
            plugin.messageUtils.sendMessage(player, "no_permission")
            return
        }

        val screen = screens.getOrDefault(key, screens[GuiKey.MAIN]) ?: return
        val session = sessionManager.create(player, key)
        val holder = AINPCGuiHolder(session.getSessionId(), key)
        val inventory = Bukkit.createInventory(
            holder,
            normalizeSize(screen.size(player)),
            GuiItemFactory.text(screen.title(player))
        )
        holder.attach(inventory)

        val context = GuiRenderContext(plugin, this, player, inventory)
        screen.render(context)
        session.setButtons(context.buttons())
        player.openInventory(inventory)
    }

    fun open(player: Player?, rawKey: String?): Boolean {
        if (player == null) {
            return false
        }
        val key = GuiKey.fromId(rawKey)
        if (key.isEmpty) {
            plugin.messageUtils.send(player, "&cGUI necunoscut: &f$rawKey")
            return false
        }
        open(player, key.get())
        return true
    }

    fun openQuestDetail(player: Player?, questSelector: String?) {
        openQuestDetail(player, questSelector, getQuestLogFilter(player))
    }

    fun openQuestDetail(player: Player?, questSelector: String?, sourceFilter: String?) {
        if (player == null || questSelector.isNullOrBlank()) {
            plugin.messageUtils.sendActionBar(player, "&cQuest indisponibil.")
            return
        }
        questDetailSelectors[player.uniqueId] = questSelector
        questDetailFilters[player.uniqueId] = QuestLogGuiFilter.normalizeFilter(sourceFilter)
        open(player, GuiKey.QUEST_DETAIL)
    }

    fun openAuthoring(player: Player?, questSelector: String?, mechanicId: String?) {
        if (player == null) {
            return
        }
        if (!questSelector.isNullOrBlank()) {
            authoringQuestSelectors[player.uniqueId] = questSelector
        } else {
            authoringQuestSelectors.remove(player.uniqueId)
        }
        if (!mechanicId.isNullOrBlank()) {
            authoringMechanicIds[player.uniqueId] = mechanicId
        } else {
            authoringMechanicIds.remove(player.uniqueId)
        }
        open(player, GuiKey.AUTHORING)
    }

    fun getAuthoringQuestSelector(player: Player?): String {
        if (player == null) {
            return ""
        }
        return authoringQuestSelectors.getOrDefault(player.uniqueId, "")
    }

    fun getAuthoringMechanicId(player: Player?): String {
        if (player == null) {
            return ""
        }
        return authoringMechanicIds.getOrDefault(player.uniqueId, "")
    }

    fun clearAuthoringSelection(player: Player?) {
        if (player == null) {
            return
        }
        authoringQuestSelectors.remove(player.uniqueId)
        authoringMechanicIds.remove(player.uniqueId)
        open(player, GuiKey.AUTHORING)
    }

    fun cycleAuthoringQuestSelector(player: Player?, step: Int) {
        if (player == null) {
            return
        }
        val selectors = AuthoringSelectionSupport.questSelectorOptions(plugin.progressionService.getDefinitions())
        if (selectors.isEmpty()) {
            return
        }
        val currentSelector = getAuthoringQuestSelector(player)
        authoringQuestSelectors[player.uniqueId] = AuthoringSelectionSupport.cycle(selectors, currentSelector, step)
        open(player, GuiKey.AUTHORING)
    }

    fun cycleAuthoringMechanicId(player: Player?, step: Int) {
        if (player == null) {
            return
        }
        val mechanics = AuthoringSelectionSupport.mechanicOptions(plugin.progressionService.getDefinitions())
        if (mechanics.isEmpty()) {
            return
        }
        val currentMechanicId = getAuthoringMechanicId(player)
        authoringMechanicIds[player.uniqueId] = AuthoringSelectionSupport.cycle(mechanics, currentMechanicId, step)
        open(player, GuiKey.AUTHORING)
    }

    fun getQuestDetailSelector(player: Player?): String {
        if (player == null) {
            return ""
        }
        return questDetailSelectors.getOrDefault(player.uniqueId, "")
    }

    fun getQuestDetailFilter(player: Player?): String {
        if (player == null) {
            return QuestLogGuiFilter.ALL.filter()
        }
        return questDetailFilters.getOrDefault(player.uniqueId, QuestLogGuiFilter.ALL.filter())
    }

    fun openQuestLog(player: Player?, filter: String?) {
        if (player == null) {
            return
        }
        val normalizedFilter = QuestLogGuiFilter.normalizeFilter(filter)
        val previousFilter = questLogFilters.put(player.uniqueId, normalizedFilter)
        if (previousFilter == null || !previousFilter.equals(normalizedFilter, ignoreCase = true)) {
            questLogPages[player.uniqueId] = 0
        }
        open(player, GuiKey.QUEST)
    }

    fun openQuestLogPage(player: Player?, pageIndex: Int) {
        if (player == null) {
            return
        }
        questLogPages[player.uniqueId] = maxOf(0, pageIndex)
        open(player, GuiKey.QUEST)
    }

    fun getQuestLogFilter(player: Player?): String {
        if (player == null) {
            return QuestLogGuiFilter.ALL.filter()
        }
        return questLogFilters.getOrDefault(player.uniqueId, QuestLogGuiFilter.ALL.filter())
    }

    fun getQuestLogPage(player: Player?): Int {
        if (player == null) {
            return 0
        }
        return maxOf(0, questLogPages.getOrDefault(player.uniqueId, 0))
    }

    fun clearPlayerState(playerId: UUID?) {
        if (playerId == null) {
            return
        }
        sessionManager.closePlayer(playerId)
        questDetailSelectors.remove(playerId)
        questDetailFilters.remove(playerId)
        authoringQuestSelectors.remove(playerId)
        authoringMechanicIds.remove(playerId)
        questLogFilters.remove(playerId)
        questLogPages.remove(playerId)
        confirmRequests.remove(playerId)
        placeDetailIds.remove(playerId)
        regionDetailIds.remove(playerId)
        adminMappingPages.remove(playerId)
        questMapTemplateIds.remove(playerId)
        questMapObjectiveKeys.remove(playerId)
        questMapMechanicFilters.remove(playerId)
    }

    fun getQuestMapMechanicFilter(player: Player?): String {
        if (player == null) return ""
        return questMapMechanicFilters.getOrDefault(player.uniqueId, "")
    }

    fun setQuestMapMechanicFilter(player: Player?, mechanic: String?) {
        if (player == null) return
        if (mechanic.isNullOrBlank()) questMapMechanicFilters.remove(player.uniqueId)
        else questMapMechanicFilters[player.uniqueId] = mechanic
    }

    fun getQuestMapTemplateId(player: Player?): String {
        if (player == null) return ""
        return questMapTemplateIds.getOrDefault(player.uniqueId, "")
    }

    fun setQuestMapTemplateId(player: Player?, templateId: String?) {
        if (player == null) return
        if (templateId.isNullOrBlank()) questMapTemplateIds.remove(player.uniqueId)
        else questMapTemplateIds[player.uniqueId] = templateId
    }

    fun getQuestMapObjectiveKey(player: Player?): String {
        if (player == null) return ""
        return questMapObjectiveKeys.getOrDefault(player.uniqueId, "")
    }

    fun setQuestMapObjectiveKey(player: Player?, objectiveKey: String?) {
        if (player == null) return
        if (objectiveKey.isNullOrBlank()) questMapObjectiveKeys.remove(player.uniqueId)
        else questMapObjectiveKeys[player.uniqueId] = objectiveKey
    }

    fun openQuestMap(player: Player?, templateId: String?) {
        if (player == null) return
        setQuestMapTemplateId(player, templateId)
        setQuestMapObjectiveKey(player, null)
        setQuestMapMechanicFilter(player, null)
        open(player, GuiKey.QUEST_MAP)
    }

    fun getAdminMappingPage(player: Player?): Int {
        if (player == null) return 0
        return maxOf(0, adminMappingPages.getOrDefault(player.uniqueId, 0))
    }

    fun openAdminMappingPage(player: Player?, page: Int) {
        if (player == null) return
        adminMappingPages[player.uniqueId] = maxOf(0, page)
        open(player, GuiKey.ADMIN_MAPPING)
    }

    fun openConfirmCommand(
        player: Player?,
        title: String?,
        command: String?,
        returnKey: GuiKey?,
        returnSelector: String?,
        warningLines: List<String>?
    ) {
        if (player == null || command.isNullOrBlank()) {
            return
        }

        confirmRequests[player.uniqueId] = ConfirmRequest(
            title,
            command,
            returnKey ?: GuiKey.MAIN,
            returnSelector,
            warningLines
        )
        open(player, GuiKey.CONFIRM)
    }

    fun getConfirmRequest(player: Player?): Optional<ConfirmRequest> {
        if (player == null) {
            return Optional.empty()
        }
        return Optional.ofNullable(confirmRequests[player.uniqueId])
    }

    fun returnFromConfirm(player: Player?, request: ConfirmRequest?) {
        if (player == null || request == null) {
            return
        }
        confirmRequests.remove(player.uniqueId)
        if (request.returnKey() == GuiKey.QUEST_DETAIL && request.returnSelector().isNotBlank()) {
            openQuestDetail(player, request.returnSelector(), getQuestDetailFilter(player))
            return
        }
        open(player, request.returnKey())
    }

    fun runConfirmedCommand(player: Player?, request: ConfirmRequest?) {
        if (player == null || request == null) {
            return
        }
        confirmRequests.remove(player.uniqueId)
        runCommand(player, request.command())
    }

    fun handleClick(
        player: Player?,
        sessionId: UUID?,
        rawSlot: Int,
        clickType: ClickType?,
        inventoryAction: InventoryAction?
    ) {
        if (player == null || sessionId == null) {
            return
        }

        val optionalSession = sessionManager.find(sessionId)
        if (optionalSession.isEmpty) {
            return
        }

        val session = optionalSession.get()
        if (session.getPlayerId() != player.uniqueId) {
            return
        }

        val button = session.getButtons()[rawSlot]
        if (button == null) {
            return
        }
        if (!button.enabled() || button.action() == null) {
            plugin.messageUtils.sendActionBar(player, "&cButon indisponibil.")
            return
        }

        try {
            button.action()!!.execute(
                GuiClickContext(
                    plugin,
                    this,
                    player,
                    session,
                    rawSlot,
                    clickType ?: ClickType.LEFT,
                    inventoryAction ?: InventoryAction.NOTHING
                )
            )
        } catch (exception: RuntimeException) {
            plugin.logger.warning("Eroare la click GUI " + session.getKey().id() + ": " + exception.message)
            plugin.messageUtils.sendActionBar(player, "&cActiunea GUI a esuat. Vezi consola.")
        }
    }

    fun runCommand(player: Player?, command: String?) {
        if (player == null || command.isNullOrBlank()) {
            return
        }
        player.closeInventory()
        val normalized = if (command.startsWith("/")) command.substring(1) else command
        plugin.server.dispatchCommand(player, normalized)
    }

    fun canOpen(player: Player?, key: GuiKey?): Boolean {
        if (player == null || key == null) {
            return false
        }
        return when (key) {
            GuiKey.MAIN -> hasAny(player, "ainpc.admin", "ainpc.gui")
            GuiKey.QUEST, GuiKey.QUEST_DETAIL -> hasAny(player, "ainpc.admin", "ainpc.gui.quest", "ainpc.quest")
            GuiKey.STORY -> hasAny(player, "ainpc.admin", "ainpc.gui.story")
            GuiKey.AUTHORING -> hasAny(player, "ainpc.admin", "ainpc.gui.debug")
            GuiKey.WORLD -> hasAny(player, "ainpc.admin", "ainpc.gui.world")
            GuiKey.PLACE, GuiKey.REGION -> hasAny(player, "ainpc.admin", "ainpc.gui.world")
            GuiKey.STATS -> hasAny(player, "ainpc.admin", "ainpc.gui.stats", "ainpc.info")
            GuiKey.INTERACT -> hasAny(player, "ainpc.admin", "ainpc.gui.interact", "ainpc.talk")
            GuiKey.ROUTINE -> hasAny(player, "ainpc.admin", "ainpc.gui.routine", "ainpc.gui.manager")
            GuiKey.SHOP -> hasAny(player, "ainpc.admin", "ainpc.gui.shop")
            GuiKey.MANAGER -> hasAny(player, "ainpc.admin", "ainpc.gui.manager")
            GuiKey.AUDIT -> hasAny(player, "ainpc.admin", "ainpc.gui.audit")
            GuiKey.DEBUG -> hasAny(player, "ainpc.admin", "ainpc.gui.debug")
            GuiKey.ADMIN_MAPPING, GuiKey.ADMIN_QUEST -> hasAny(player, "ainpc.admin", "ainpc.gui.world", "ainpc.gui.quest")
            GuiKey.QUEST_MAP -> hasAny(player, "ainpc.admin", "ainpc.gui.quest", "ainpc.gui.quest_map", "ainpc.creator")
            GuiKey.CONFIRM -> true
        }
    }

    data class ConfirmRequest(
        val title: String?,
        val command: String?,
        val returnKey: GuiKey?,
        val returnSelector: String?,
        val warningLines: List<String>?
    ) {
        private val safeTitle = title ?: "Confirmare"
        private val safeCommand = command ?: ""
        private val safeReturnKey = returnKey ?: GuiKey.MAIN
        private val safeReturnSelector = returnSelector ?: ""
        private val safeWarningLines = warningLines?.toList() ?: listOf()

        fun title(): String = safeTitle
        fun command(): String = safeCommand
        fun returnKey(): GuiKey = safeReturnKey
        fun returnSelector(): String = safeReturnSelector
        fun warningLines(): List<String> = safeWarningLines.toList()
    }

    private fun register(screen: GuiScreen) {
        screens[screen.key()] = screen
    }

    private fun hasAny(player: Player, vararg permissions: String): Boolean {
        for (permission in permissions) {
            if (player.hasPermission(permission)) {
                return true
            }
        }
        return false
    }

    private fun normalizeSize(requestedSize: Int): Int {
        val clamped = maxOf(9, minOf(54, requestedSize))
        return ((clamped + 8) / 9) * 9
    }
}
