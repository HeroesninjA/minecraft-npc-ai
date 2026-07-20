package ro.ainpc.api

enum class PermissionNode(
    val node: String,
    val level: PermissionLevel,
    val description: String,
    val defaultValue: String = "op"
) {
    ADMIN("ainpc.admin", PermissionLevel.ADMIN, "Acces la toate comenzile admin"),
    CREATOR("ainpc.creator", PermissionLevel.AUTHOR, "Acces la modul creator (mapping si quest design)"),
    TALK("ainpc.talk", PermissionLevel.PLAY, "Permite jucatorilor sa vorbeasca cu NPC-urile", "true"),
    INFO("ainpc.info", PermissionLevel.VIEW, "Vizualizarea informatiilor despre NPC-uri", "true"),
    QUEST("ainpc.quest", PermissionLevel.PLAY, "Folosirea quest log-ului si comenzilor normale de quest", "true"),
    GUI("ainpc.gui", PermissionLevel.VIEW, "Deschiderea hub-ului GUI AINPC", "true"),
    GUI_QUEST("ainpc.gui.quest", PermissionLevel.VIEW, "Deschiderea GUI-ului de questuri", "true"),
    GUI_STORY("ainpc.gui.story", PermissionLevel.VIEW, "Deschiderea GUI-ului story read-only", "op"),
    GUI_STATS("ainpc.gui.stats", PermissionLevel.VIEW, "Deschiderea GUI-ului de statistici", "true"),
    GUI_INTERACT("ainpc.gui.interact", PermissionLevel.VIEW, "Deschiderea GUI-ului de interactiuni NPC", "true"),
    GUI_RELATIONSHIP("ainpc.gui.relationship", PermissionLevel.VIEW, "Deschiderea GUI-ului de relatii NPC", "op"),
    GUI_NPC("ainpc.gui.npc", PermissionLevel.VIEW, "Deschiderea GUI-ului de memorii NPC fara ainpc.info", "op"),
    GUI_ROUTINE("ainpc.gui.routine", PermissionLevel.VIEW, "Deschiderea GUI-ului de rutine NPC", "op"),
    GUI_SHOP("ainpc.gui.shop", PermissionLevel.VIEW, "Deschiderea GUI-ului de shop NPC", "true"),
    GUI_WORLD("ainpc.gui.world", PermissionLevel.VIEW, "Deschiderea GUI-ului world", "op"),
    GUI_MANAGER("ainpc.gui.manager", PermissionLevel.VIEW, "Deschiderea managerului NPC GUI", "op"),
    GUI_AUDIT("ainpc.gui.audit", PermissionLevel.DIAGNOSTIC, "Deschiderea GUI-ului audit", "op"),
    GUI_DEBUG("ainpc.gui.debug", PermissionLevel.DIAGNOSTIC, "Deschiderea GUI-ului debug", "op"),
    GUI_MCP("ainpc.gui.mcp", PermissionLevel.ADMIN, "Deschiderea GUI-ului administrativ MCP", "op"),
    GUI_QUEST_MAP("ainpc.gui.quest_map", PermissionLevel.VIEW, "Deschiderea GUI-ului quest map", "op");

    companion object {
        private val nodeMap: Map<String, PermissionNode> = entries.associateBy { it.node }

        fun fromNode(node: String): PermissionNode? = nodeMap[node]

        fun allNodes(): List<String> = entries.map { it.node }

        fun nodesByLevel(level: PermissionLevel): List<PermissionNode> = entries.filter { it.level == level }

        fun undeclaredNodes(declared: Set<String>): List<PermissionNode> =
            entries.filter { it.node !in declared }
    }

    fun hasPermission(player: Any): Boolean {
        return try {
            val method = player.javaClass.getMethod("hasPermission", String::class.java)
            method.invoke(player, node) as Boolean
        } catch (_: Exception) {
            false
        }
    }
}

enum class PermissionLevel {
    VIEW,
    PLAY,
    AUTHOR,
    MUTATE,
    ADMIN,
    DIAGNOSTIC
}
