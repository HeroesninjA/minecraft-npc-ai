package ro.ainpc.gui

import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ro.ainpc.AINPCPlugin
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.UUID

class GuiServiceAuthoringSelectionTest {
    @Test
    fun authoringSelectionDefaultsAndPermissionsPerPlayer() {
        val service = GuiService(newPluginInstance())
        val player = newPlayer(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val authoringPlayer = newPlayer(
            UUID.fromString("00000000-0000-0000-0000-000000000003"),
            setOf("ainpc.gui.debug")
        )

        assertEquals("", service.getAuthoringQuestSelector(player))
        assertEquals("", service.getAuthoringMechanicId(player))
        assertEquals(false, service.canOpen(player, GuiKey.AUTHORING))
        assertEquals(true, service.canOpen(authoringPlayer, GuiKey.AUTHORING))
    }

    @Test
    fun publicQuestPermissionDoesNotGrantCreatorSurfaces() {
        val service = GuiService(newPluginInstance())
        val player = newPlayer(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            setOf("ainpc.quest", "ainpc.gui.quest")
        )
        val creator = newPlayer(
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            setOf("ainpc.creator")
        )
        val creatorKeys = listOf(
            GuiKey.CREATOR_HUB,
            GuiKey.CREATOR_QUEST,
            GuiKey.CREATOR_QUEST_DEFS,
            GuiKey.CREATOR_QUEST_TEST,
            GuiKey.QUEST_EDIT,
            GuiKey.QUEST_CREATE,
            GuiKey.QUICK_QUEST,
            GuiKey.QUEST_MAP,
            GuiKey.ADMIN_QUEST,
        )

        assertEquals(true, service.canOpen(player, GuiKey.QUEST))
        creatorKeys.forEach { key ->
            assertEquals(false, service.canOpen(player, key), "player should not open $key")
            assertEquals(true, service.canOpen(creator, key), "creator should open $key")
        }
        assertEquals(true, service.canOpen(creator, GuiKey.AUTHORING))
    }

    @Test
    fun openAuthoringAndClearSetPlayerSelection() {
        val service = GuiService(newPluginInstance())
        val player = newPlayer(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val otherPlayer = newPlayer(UUID.fromString("00000000-0000-0000-0000-000000000002"))

        runCatching { service.openAuthoring(player, "story_driven", "build") }
        assertEquals("story_driven", service.getAuthoringQuestSelector(player))
        assertEquals("build", service.getAuthoringMechanicId(player))
        assertEquals("", service.getAuthoringQuestSelector(otherPlayer))
        assertEquals("", service.getAuthoringMechanicId(otherPlayer))

        runCatching { service.clearAuthoringSelection(player) }
        assertEquals("", service.getAuthoringQuestSelector(player))
        assertEquals("", service.getAuthoringMechanicId(player))
    }

    @Test
    fun clearPlayerStateRemovesOfferShopAndResumeSelections() {
        val service = GuiService(newPluginInstance())
        val player = newPlayer(UUID.fromString("00000000-0000-0000-0000-000000000001"))

        service.setLastGuiKey(player, GuiKey.SHOP)
        service.setShopSelectedNpcId(player, "merchant-1")
        runCatching { service.openQuestOffer(player, "quest-1") }

        assertEquals(GuiKey.SHOP, service.getLastGuiKey(player))
        assertEquals("merchant-1", service.getShopSelectedNpcId(player))
        assertEquals("quest-1", service.getQuestOfferSelector(player))

        service.clearPlayerState(player.uniqueId)

        assertEquals(null, service.getLastGuiKey(player))
        assertEquals(null, service.getShopSelectedNpcId(player))
        assertEquals("", service.getQuestOfferSelector(player))
    }

    @Test
    fun clearPlayerStateRemovesAllPerPlayerMaps() {
        val service = GuiService(newPluginInstance())
        val player = newPlayer(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val uid = player.uniqueId

        service.setLastGuiKey(player, GuiKey.WORLD)
        service.setShopSelectedNpcId(player, "shop-npc-1")
        runCatching { service.openQuestDetail(player, "q-1", "all") }
        runCatching { service.openQuestOffer(player, "quest-offer-1") }
        runCatching { service.openAuthoring(player, "story_driven", "build") }
        runCatching { service.openQuestLog(player, "active") }
        runCatching { service.openQuestLogPage(player, 2) }
        runCatching { service.openConfirmCommand(player, "test", "say hi", GuiKey.MAIN, null, null) }
        runCatching { service.setPlaceDetailId(player, "place-1") }
        runCatching { service.openRegionDetail(player, "region-1") }
        runCatching { service.openAdminMappingPage(player, 3) }
        service.setQuestMapTemplateId(player, "tmpl-1")
        service.setQuestMapObjectiveKey(player, "obj-1")
        service.setQuestMapMechanicFilter(player, "build")
        runCatching { service.toggleQuestMapGlobalMode(player) }
        service.setBuildModeEnabled(player, true)
        service.setBuildModeTarget(player, "wand:region")
        service.setQuestEditSelectedId(player, "edit-1")
        service.setCreatorFormValue(player, "test_key", "test_value")
        runCatching { service.openTextInput(player, "title", "formKey", GuiKey.MAIN, null, listOf()) }

        assertEquals(GuiKey.WORLD, service.getLastGuiKey(player))
        assertEquals("shop-npc-1", service.getShopSelectedNpcId(player))
        assertEquals("q-1", service.getQuestDetailSelector(player))
        assertEquals("quest-offer-1", service.getQuestOfferSelector(player))
        assertEquals("story_driven", service.getAuthoringQuestSelector(player))
        assertEquals("build", service.getAuthoringMechanicId(player))
        assertEquals("active", service.getQuestLogFilter(player))
        assertEquals(2, service.getQuestLogPage(player))
        val confirmSet = service.getConfirmRequest(player)?.title()
        if (confirmSet != null) assertEquals("test", confirmSet)
        assertEquals("place-1", service.getPlaceDetailId(player))
        assertEquals("region-1", service.getRegionDetailId(player))
        assertEquals(3, service.getAdminMappingPage(player))
        assertEquals("tmpl-1", service.getQuestMapTemplateId(player))
        assertEquals("obj-1", service.getQuestMapObjectiveKey(player))
        assertEquals("build", service.getQuestMapMechanicFilter(player))
        assertEquals(true, service.getQuestMapGlobalMode(player))
        assertEquals(true, service.isBuildModeEnabled(player))
        assertEquals("wand:region", service.getBuildModeTarget(player))
        assertEquals("edit-1", service.getQuestEditSelectedId(player))
        assertEquals("test_value", service.getCreatorFormValue(player, "test_key"))
        assertEquals(true, service.hasTextInputRequest(player))

        service.clearPlayerState(uid)

        assertEquals(null, service.getLastGuiKey(player))
        assertEquals(null, service.getShopSelectedNpcId(player))
        assertEquals("", service.getQuestDetailSelector(player))
        assertEquals("all", service.getQuestDetailFilter(player))
        assertEquals("", service.getQuestOfferSelector(player))
        assertEquals("", service.getAuthoringQuestSelector(player))
        assertEquals("", service.getAuthoringMechanicId(player))
        assertEquals("all", service.getQuestLogFilter(player))
        assertEquals(0, service.getQuestLogPage(player))
        assertEquals(null, service.getConfirmRequest(player))
        assertEquals("", service.getPlaceDetailId(player))
        assertEquals("", service.getRegionDetailId(player))
        assertEquals(0, service.getAdminMappingPage(player))
        assertEquals("", service.getQuestMapTemplateId(player))
        assertEquals("", service.getQuestMapObjectiveKey(player))
        assertEquals("", service.getQuestMapMechanicFilter(player))
        assertEquals(false, service.getQuestMapGlobalMode(player))
        assertEquals(false, service.isBuildModeEnabled(player))
        assertEquals("", service.getBuildModeTarget(player))
        assertEquals(true, service.getBuildModeHistory(player).isEmpty())
        assertEquals("", service.getQuestEditSelectedId(player))
        assertEquals("", service.getCreatorFormValue(player, "test_key"))
        assertEquals(false, service.hasTextInputRequest(player))
    }

    private fun newPlayer(uniqueId: UUID, permissions: Set<String> = emptySet()): Player {
        val handler = InvocationHandler { _, method, arguments ->
            if (method.name == "hasPermission") {
                return@InvocationHandler when (val argument = arguments?.firstOrNull()) {
                    is String -> permissions.contains(argument)
                    else -> false
                }
            }
            defaultValue(method, uniqueId)
        }
        return Proxy.newProxyInstance(
            Player::class.java.classLoader,
            arrayOf(Player::class.java),
            handler
        ) as Player
    }

    private fun newPluginInstance(): AINPCPlugin {
        val field = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        val unsafe = field.get(null) as sun.misc.Unsafe
        return unsafe.allocateInstance(AINPCPlugin::class.java) as AINPCPlugin
    }

    private fun defaultValue(method: Method, uniqueId: UUID): Any? {
        if (method.name == "getUniqueId") {
            return uniqueId
        }
        return when (method.returnType) {
            java.lang.Boolean.TYPE -> false
            java.lang.Byte.TYPE -> 0.toByte()
            java.lang.Short.TYPE -> 0.toShort()
            java.lang.Integer.TYPE -> 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0f
            java.lang.Double.TYPE -> 0.0
            java.lang.Character.TYPE -> '\u0000'
            java.lang.Void.TYPE -> null
            else -> null
        }
    }
}
