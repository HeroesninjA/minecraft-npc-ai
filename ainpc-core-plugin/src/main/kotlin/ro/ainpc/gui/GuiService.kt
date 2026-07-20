package ro.ainpc.gui

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import ro.ainpc.AINPCPlugin
import ro.ainpc.commands.AINPCCommandMutationPolicy
import ro.ainpc.gui.screens.AuditGui
import ro.ainpc.gui.screens.ConfirmActionGui
import ro.ainpc.gui.screens.DebugGui
import ro.ainpc.gui.screens.MainHubGui
import ro.ainpc.gui.screens.NpcManagerGui
import ro.ainpc.gui.screens.PlaceholderGui
import ro.ainpc.gui.screens.NpcMemoryGui
import ro.ainpc.gui.screens.QuestAuthoringGui
import ro.ainpc.gui.screens.QuestDetailGui
import ro.ainpc.gui.screens.QuestLogGui
import ro.ainpc.gui.screens.QuestOfferNpcGui
import ro.ainpc.gui.screens.QuestOfferGui
import ro.ainpc.gui.screens.RelationshipGui
import ro.ainpc.gui.screens.RoutineGui
import ro.ainpc.gui.screens.StoryAuthoringGui
import ro.ainpc.gui.screens.StatsGui
import ro.ainpc.gui.screens.StoryGui
import ro.ainpc.gui.screens.WorldHubGui
import ro.ainpc.gui.screens.WorldPlaceGui
import ro.ainpc.gui.screens.WorldRegionGui
import ro.ainpc.gui.screens.AdminMappingGui
import ro.ainpc.gui.screens.AdminQuestGui
import ro.ainpc.gui.screens.AdminHubGui
import ro.ainpc.gui.screens.AdminMcpGui
import ro.ainpc.gui.screens.CreatorHubGui
import ro.ainpc.gui.screens.PlayerHubGui
import ro.ainpc.gui.screens.QuestCreatorGui
import ro.ainpc.gui.screens.QuestCreatorDefinitionsGui
import ro.ainpc.gui.screens.QuestCreatorTestGui
import ro.ainpc.gui.screens.QuestEditGui
import ro.ainpc.gui.screens.QuestCreateGui
import ro.ainpc.gui.screens.QuickQuestGui
import ro.ainpc.gui.screens.MappingCreatorGui
import ro.ainpc.gui.screens.MappingCreateRegionGui
import ro.ainpc.gui.screens.MappingCreatePlaceGui
import ro.ainpc.gui.screens.MappingCreateNodeGui
import ro.ainpc.gui.screens.NpcInteractionGui
import ro.ainpc.gui.screens.QuestMapGui
import ro.ainpc.gui.screens.ShopGui
import java.util.EnumMap
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentMap

class GuiService(private val plugin: AINPCPlugin) {
    private val skipConfirmations: Boolean
        get() = plugin.config.getBoolean("gui.skip_confirmations", false)
    private val guiEnabled: Boolean
        get() = plugin.config.getBoolean("features.gui", true)
    private val sessionManager = GuiSessionManager()
    private val screens: MutableMap<GuiKey, GuiScreen> = EnumMap(GuiKey::class.java)
    private val questDetailSelectors: ConcurrentMap<UUID, String> = ConcurrentHashMap()
    private val questDetailFilters: ConcurrentMap<UUID, String> = ConcurrentHashMap()
    private val questOfferSelectors: ConcurrentMap<UUID, String> = ConcurrentHashMap()
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
    private val questMapGlobalModes: ConcurrentMap<UUID, Boolean> = ConcurrentHashMap()
    private val buildModeEnabled: ConcurrentMap<UUID, Boolean> = ConcurrentHashMap()
    private val buildModeTarget: ConcurrentMap<UUID, String> = ConcurrentHashMap()
    private val buildModeHistory: ConcurrentMap<UUID, ConcurrentLinkedDeque<BuildModeHistoryEntry>> = ConcurrentHashMap()
    private val questEditSelectedIds: ConcurrentMap<UUID, String> = ConcurrentHashMap()
    private val creatorFormValues: ConcurrentMap<UUID, MutableMap<String, String>> = ConcurrentHashMap()
    private val textInputRequests: ConcurrentMap<UUID, TextInputRequest> = ConcurrentHashMap()
    private val shopSelectedNpcIds: ConcurrentMap<UUID, String> = ConcurrentHashMap()
    private val lastGuiKeys: ConcurrentMap<UUID, GuiKey> = ConcurrentHashMap()

    fun getShopSelectedNpcId(player: Player): String? = shopSelectedNpcIds[player.uniqueId]

    fun setLastGuiKey(player: Player, key: GuiKey) {
        if (key != GuiKey.CONFIRM) {
            lastGuiKeys[player.uniqueId] = key
        }
    }

    fun getLastGuiKey(player: Player): GuiKey? = lastGuiKeys[player.uniqueId]

    fun popLastGuiKey(player: Player): GuiKey? {
        val key = lastGuiKeys.remove(player.uniqueId)
        return key
    }

    fun setShopSelectedNpcId(player: Player, npcId: String?) {
        if (npcId != null) shopSelectedNpcIds[player.uniqueId] = npcId
        else shopSelectedNpcIds.remove(player.uniqueId)
    }

    fun getCreatorFormValue(player: Player?, key: String): String {
        if (player == null) return ""
        return creatorFormValues[player.uniqueId]?.get(key).orEmpty()
    }

    fun setCreatorFormValue(player: Player?, key: String, value: String?) {
        if (player == null) return
        val map = creatorFormValues.getOrPut(player.uniqueId) { mutableMapOf() }
        if (value.isNullOrBlank()) map.remove(key)
        else map[key] = value
    }

    fun clearCreatorFormValues(player: Player?) {
        if (player == null) return
        creatorFormValues.remove(player.uniqueId)
    }

    fun clearQuestCreatorObjectiveDraft(player: Player?) {
        clearCreatorFields(
            player,
            "quest_obj_type",
            "quest_obj_target",
            "quest_obj_count",
            "quest_obj_dialog"
        )
    }

    fun clearQuestCreatorRewardDraft(player: Player?) {
        clearCreatorFields(
            player,
            "quest_reward_type",
            "quest_reward_value",
            "quest_reward_count",
            "quest_reward_event_key",
            "quest_reward_event_scope",
            "quest_reward_event_target",
            "quest_reward_event_title",
            "quest_reward_event_payload"
        )
    }

    fun clearQuestCreatorDialogDraft(player: Player?) {
        clearCreatorFields(
            player,
            "quest_dialog_type",
            "quest_dialog_speaker",
            "quest_dialog_text",
            "quest_system_msg"
        )
    }

    private fun clearCreatorFields(player: Player?, vararg keys: String) {
        if (player == null) return
        val map = creatorFormValues[player.uniqueId] ?: return
        for (key in keys) {
            map.remove(key)
        }
        if (map.isEmpty()) {
            creatorFormValues.remove(player.uniqueId)
        }
    }

    fun resetDraft(player: Player?) {
        if (player == null) return
        creatorFormValues.remove(player.uniqueId)
        plugin.messageUtils.send(player, "&eDraft resetat.&7 Schimbarile doar in memorie (nesalvate) au fost pierdute.")
    }

    fun openTextInput(
        player: Player?,
        title: String?,
        formKey: String,
        returnKey: GuiKey?,
        returnSelector: String? = null,
        promptLines: List<String> = listOf()
    ) {
        if (player == null || formKey.isBlank()) return
        val request = TextInputRequest(
            title = title,
            formKey = formKey,
            returnKey = returnKey ?: GuiKey.MAIN,
            returnSelector = returnSelector,
            promptLines = promptLines
        )
        request.recordCreation()
        textInputRequests[player.uniqueId] = request
        player.closeInventory()
        plugin.messageUtils.send(player, "&eInput text: &f${title ?: formKey}")
        for (line in promptLines) {
            plugin.messageUtils.send(player, line)
        }
        plugin.messageUtils.send(player, "&7Scrie in chat. Scrie &fclear&7 ca sa cureti, &fcancel&7 ca sa anulezi. Expira in ${request.timeoutTicks * 50}ms.")
    }

    fun hasTextInputRequest(player: Player?): Boolean {
        if (player == null) return false
        val existing = textInputRequests[player.uniqueId]
        if (existing != null && existing.isExpired()) {
            textInputRequests.remove(player.uniqueId)
            plugin.messageUtils.send(player, "&cSolicitarea de input a expirat (${existing.timeoutTicks * 50}ms). Incearca din nou.")
            return false
        }
        return existing != null
    }

    fun consumeTextInputRequest(player: Player?): TextInputRequest? {
        if (player == null) return null
        val existing = textInputRequests[player.uniqueId]
        if (existing != null && existing.isExpired()) {
            textInputRequests.remove(player.uniqueId)
            plugin.messageUtils.send(player, "&cSolicitarea de input a expirat (${existing.timeoutTicks * 50}ms). Incearca din nou.")
            return null
        }
        return textInputRequests.remove(player.uniqueId)
    }

    fun getQuestEditSelectedId(player: Player?): String {
        if (player == null) return ""
        return questEditSelectedIds.getOrDefault(player.uniqueId, "")
    }

    fun setQuestEditSelectedId(player: Player?, id: String?) {
        if (player == null) return
        if (id.isNullOrBlank()) questEditSelectedIds.remove(player.uniqueId)
        else questEditSelectedIds[player.uniqueId] = id
    }

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
        register(QuestOfferGui())
        register(StoryGui())
        register(WorldHubGui())
        register(WorldPlaceGui())
        register(WorldRegionGui())
        register(AdminMappingGui())
        register(AdminQuestGui())
        register(AdminMcpGui())
        register(StoryAuthoringGui())
        register(StatsGui())
        register(NpcMemoryGui())
        register(NpcInteractionGui())
        register(RelationshipGui())
        register(RoutineGui())
        register(NpcManagerGui())
        register(AuditGui())
        register(DebugGui())
        register(QuestAuthoringGui())
        register(QuestMapGui())
        register(ConfirmActionGui())
        register(AdminHubGui())
        register(CreatorHubGui())
        register(PlayerHubGui())
        register(QuestCreatorGui())
        register(QuestCreatorDefinitionsGui())
        register(QuestCreatorTestGui())
        register(QuestEditGui())
        register(QuestCreateGui())
        register(QuestOfferNpcGui())
        register(QuickQuestGui())
        register(MappingCreatorGui())
        register(MappingCreateRegionGui())
        register(MappingCreatePlaceGui())
        register(MappingCreateNodeGui())
        register(ShopGui())
    }

    fun sessions(): GuiSessionManager = sessionManager

    fun open(player: Player?, key: GuiKey?) {
        if (player == null || key == null) {
            return
        }
        if (!guiEnabled) {
            plugin.messageUtils.send(player, "&cGUI dezactivat (features.gui=false). Foloseste comenzi chat.")
            return
        }
        if (!canOpen(player, key)) {
            plugin.messageUtils.sendMessage(player, "no_permission")
            return
        }

        setLastGuiKey(player, key)
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
        if (key == null) {
            plugin.messageUtils.send(player, "&cGUI necunoscut: &f$rawKey")
            return false
        }
        open(player, key)
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

    fun openQuestOffer(player: Player?, questSelector: String?) {
        if (player == null || questSelector.isNullOrBlank()) return
        questOfferSelectors[player.uniqueId] = questSelector
        open(player, GuiKey.QUEST_OFFER)
    }

    fun getQuestOfferSelector(player: Player?): String {
        if (player == null) return ""
        return questOfferSelectors.getOrDefault(player.uniqueId, "")
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
        questOfferSelectors.remove(playerId)
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
        questMapGlobalModes.remove(playerId)
        buildModeEnabled.remove(playerId)
        buildModeTarget.remove(playerId)
        buildModeHistory.remove(playerId)
        questEditSelectedIds.remove(playerId)
        creatorFormValues.remove(playerId)
        textInputRequests.remove(playerId)
        shopSelectedNpcIds.remove(playerId)
        lastGuiKeys.remove(playerId)
    }

    fun getQuestMapGlobalMode(player: Player?): Boolean {
        if (player == null) return false
        return questMapGlobalModes.getOrDefault(player.uniqueId, false)
    }

    fun toggleQuestMapGlobalMode(player: Player?): Boolean {
        if (player == null) return false
        val current = questMapGlobalModes.getOrDefault(player.uniqueId, false)
        val next = !current
        if (next) questMapGlobalModes[player.uniqueId] = true
        else questMapGlobalModes.remove(player.uniqueId)
        return next
    }

    fun isBuildModeEnabled(player: Player?): Boolean {
        if (player == null) return false
        return buildModeEnabled.getOrDefault(player.uniqueId, false)
    }

    fun setBuildModeEnabled(player: Player?, enabled: Boolean) {
        if (player == null) return
        if (enabled) buildModeEnabled[player.uniqueId] = true
        else buildModeEnabled.remove(player.uniqueId)
        recordBuildModeHistory(player.uniqueId, if (enabled) "enabled" else "disabled", null, null)
    }

    fun toggleBuildMode(player: Player?): Boolean {
        if (player == null) return false
        val next = !isBuildModeEnabled(player)
        setBuildModeEnabled(player, next)
        return next
    }

    fun getBuildModeTarget(player: Player?): String {
        if (player == null) return ""
        return buildModeTarget.getOrDefault(player.uniqueId, "")
    }

    fun setBuildModeTarget(player: Player?, target: String?) {
        if (player == null) return
        if (target.isNullOrBlank()) {
            buildModeTarget.remove(player.uniqueId)
            recordBuildModeHistory(player.uniqueId, "target_cleared", null, null)
        } else {
            val normalized = target.trim()
            buildModeTarget[player.uniqueId] = normalized
            val parts = normalized.split(":", limit = 2)
            recordBuildModeHistory(
                player.uniqueId,
                "target_set",
                parts.getOrNull(0),
                parts.getOrNull(1)
            )
        }
    }

    fun getBuildModeHistory(player: Player?): List<BuildModeHistoryEntry> {
        if (player == null) return emptyList()
        return buildModeHistory[player.uniqueId]?.toList()?.reversed() ?: emptyList()
    }

    fun clearBuildModeHistory(player: Player?) {
        if (player == null) return
        buildModeHistory.remove(player.uniqueId)
    }

    private fun recordBuildModeHistory(playerId: UUID, action: String, style: String?, target: String?) {
        val deque = buildModeHistory.computeIfAbsent(playerId) { ConcurrentLinkedDeque() }
        deque.addLast(
            BuildModeHistoryEntry(
                timestampMillis = System.currentTimeMillis(),
                action = action,
                style = style?.ifBlank { null },
                target = target?.ifBlank { null }
            )
        )
        while (deque.size > 12) {
            deque.pollFirst()
        }
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
        if (!guiEnabled) {
            plugin.messageUtils.send(player, "&cGUI dezactivat (features.gui=false). Foloseste comenzi chat.")
            return
        }

        if (skipConfirmations) {
            if (warningLines.isNullOrEmpty()) {
                plugin.messageUtils.send(player, "&7[Auto-confirm] &f$title")
            } else {
                val firstWarning = warningLines.firstOrNull() ?: title ?: "Actiune"
                plugin.messageUtils.send(player, "&7[Auto-confirm] &f$firstWarning")
            }
            runCommand(player, AINPCCommandMutationPolicy.confirmedCommand(command))
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

    fun getConfirmRequest(player: Player?): ConfirmRequest? {
        if (player == null) {
            return null
        }
        return confirmRequests[player.uniqueId]
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
        runCommand(player, AINPCCommandMutationPolicy.confirmedCommand(request.command()))
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

        val session = sessionManager.find(sessionId) ?: return
        if (session.getPlayerId() != player.uniqueId) {
            return
        }
        if (!canOpen(player, session.getKey())) {
            plugin.messageUtils.sendActionBar(player, "&cNu mai ai permisiunea pentru acest ecran.")
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
        val normalized = if (command.startsWith("/")) command.substring(1) else command
        if (AINPCCommandMutationPolicy.requiresConfirmation(normalized)) {
            openConfirmCommand(
                player,
                "Confirma mutatia",
                normalized,
                getLastGuiKey(player) ?: GuiKey.MAIN,
                "",
                listOf(
                    "&eOperatia modifica mapping-ul sau starea asociata.",
                    "&7Dupa succes ruleaza &f/ainpc world save&7.",
                ),
            )
            return
        }
        player.closeInventory()
        plugin.server.dispatchCommand(player, normalized)
    }

    fun canOpen(player: Player?, key: GuiKey?): Boolean {
        if (player == null || key == null) {
            return false
        }
        return when (key) {
            GuiKey.MAIN -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.GUI)
            GuiKey.PLAYER_HUB -> hasAny(player, ro.ainpc.api.PermissionNode.GUI, ro.ainpc.api.PermissionNode.GUI_QUEST)
            GuiKey.QUEST, GuiKey.QUEST_DETAIL -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.GUI_QUEST, ro.ainpc.api.PermissionNode.QUEST)
            GuiKey.STORY -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.GUI_STORY)
            GuiKey.AUTHORING -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.CREATOR, ro.ainpc.api.PermissionNode.GUI_DEBUG)
            GuiKey.WORLD -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.GUI_WORLD)
            GuiKey.PLACE, GuiKey.REGION -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.GUI_WORLD)
            GuiKey.STATS -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.GUI_STATS, ro.ainpc.api.PermissionNode.INFO)
            GuiKey.INTERACT -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.GUI_INTERACT, ro.ainpc.api.PermissionNode.TALK)
            GuiKey.ROUTINE -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.GUI_ROUTINE, ro.ainpc.api.PermissionNode.GUI_MANAGER)
            GuiKey.RELATIONSHIP -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.GUI_RELATIONSHIP, ro.ainpc.api.PermissionNode.GUI_MANAGER)
            GuiKey.STORY_AUTHORING -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.GUI_STORY, ro.ainpc.api.PermissionNode.CREATOR)
            GuiKey.NPC_MEMORY -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.GUI_NPC, ro.ainpc.api.PermissionNode.INFO)
            GuiKey.SHOP -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.GUI_SHOP)
            GuiKey.MANAGER -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.GUI_MANAGER)
            GuiKey.AUDIT -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.GUI_AUDIT)
            GuiKey.DEBUG -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.GUI_DEBUG)
            GuiKey.ADMIN_MAPPING -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.CREATOR, ro.ainpc.api.PermissionNode.GUI_WORLD)
            GuiKey.ADMIN_QUEST -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.CREATOR)
            GuiKey.MCP -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.GUI_DEBUG, ro.ainpc.api.PermissionNode.GUI_MCP)
            GuiKey.ADMIN_HUB -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.GUI_WORLD, ro.ainpc.api.PermissionNode.GUI_AUDIT, ro.ainpc.api.PermissionNode.GUI_DEBUG)
            GuiKey.CREATOR_HUB -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.CREATOR)
            GuiKey.CREATOR_QUEST, GuiKey.CREATOR_QUEST_DEFS, GuiKey.CREATOR_QUEST_TEST -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.CREATOR)
            GuiKey.QUEST_EDIT, GuiKey.QUEST_CREATE, GuiKey.QUICK_QUEST -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.CREATOR)
            GuiKey.MAPPING_CREATOR, GuiKey.MAPPING_CREATE_REGION, GuiKey.MAPPING_CREATE_PLACE, GuiKey.MAPPING_CREATE_NODE -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.CREATOR, ro.ainpc.api.PermissionNode.GUI_WORLD)
            GuiKey.QUEST_MAP -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.CREATOR, ro.ainpc.api.PermissionNode.GUI_QUEST_MAP)
            GuiKey.QUEST_OFFER_NPC -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.GUI_QUEST, ro.ainpc.api.PermissionNode.TALK)
            GuiKey.QUEST_OFFER -> hasAny(player, ro.ainpc.api.PermissionNode.ADMIN, ro.ainpc.api.PermissionNode.GUI_QUEST, ro.ainpc.api.PermissionNode.QUEST)
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

    class TextInputRequest(
        title: String?,
        formKey: String,
        returnKey: GuiKey,
        returnSelector: String?,
        promptLines: List<String>,
        val timeoutTicks: Int = 600
    ) {
        private val _title: String? = title
        private val _formKey: String = formKey
        private val _returnKey: GuiKey = returnKey
        private val _returnSelector: String? = returnSelector
        private val _promptLines: List<String> = promptLines
        private var _createdAtMillis: Long = 0L

        fun title(): String? = _title
        fun formKey(): String = _formKey
        fun returnKey(): GuiKey = _returnKey
        fun returnSelector(): String? = _returnSelector
        fun promptLines(): List<String> = _promptLines
        val safeTitle: String get() = (_title ?: "").filter { it.isLetterOrDigit() || it == ' ' || it == '_' || it == '-' }
        val safeFormKey: String get() = _formKey.filter { it.isLetterOrDigit() || it == '_' || it == '-' || it == ':' }
        val safeReturnSelector: String? get() = _returnSelector
        val safePromptLines: List<String> get() = _promptLines.map { it.filter { c -> c.isLetterOrDigit() || c == ' ' || c == '_' || c == '-' || c == ':' || c == '.' || c == ',' || c == '!' || c == '?' } }
        val createdAtMillis: Long get() = _createdAtMillis

        fun recordCreation() {
            _createdAtMillis = System.currentTimeMillis()
        }

        fun isExpired(): Boolean {
            if (timeoutTicks <= 0) return false
            val timeoutMillis = timeoutTicks * 50L
            return (System.currentTimeMillis() - _createdAtMillis) >= timeoutMillis
        }
    }

    data class BuildModeHistoryEntry(
        val timestampMillis: Long,
        val action: String,
        val style: String?,
        val target: String?
    )

    private fun register(screen: GuiScreen) {
        screens[screen.key()] = screen
    }

    private fun hasAny(player: Player, vararg nodes: ro.ainpc.api.PermissionNode): Boolean {
        for (node in nodes) {
            if (player.hasPermission(node.node)) {
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
