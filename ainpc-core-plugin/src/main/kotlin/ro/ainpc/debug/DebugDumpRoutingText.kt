package ro.ainpc.debug

import com.google.gson.JsonParser
import ro.ainpc.AINPCPlugin

object DebugDumpRoutingText {
    @JvmStatic
    fun buildRoutingText(plugin: AINPCPlugin): String {
        return buildRoutingText(
            "AINPC Routing Dump",
            listOf(
                "ROUTING_WORLD -> ainpc.semantic.context / ainpc.semantic.context.summary",
                "ROUTING_STORY -> ainpc.story.semantic.context / ainpc.story.semantic.context.summary",
                "ROUTING_MAPPING -> ainpc.mapping.semantic.context / ainpc.mapping.semantic.context.summary",
                "ROUTING_QUEST -> ainpc.quest.semantic.context / ainpc.quest.semantic.context.summary",
                "ROUTING_QUEST_AUTHORING -> ainpc.quest.authoring.context.summary"
            ),
            plugin,
            true
        )
    }

    @JvmStatic
    fun buildSummaryText(plugin: AINPCPlugin): String {
        return buildRoutingText(
            "AINPC Routing Summary",
            listOf(
                "WORLD = lore/history/npc",
                "STORY = context/history/signals",
                "MAPPING = context/history/story links",
                "QUEST = lore/history/signals",
                "QUEST_AUTHORING = selector/seed/history/signals",
                "ROUTING = world/story/mapping/quest/authoring entrypoints"
            ),
            plugin,
            false
        )
    }

    private fun buildRoutingText(header: String, lines: List<String>, plugin: AINPCPlugin, includeMcpState: Boolean): String {
        val sb = StringBuilder()
        sb.append(header).append("\n")
        sb.append("Service: ").append("ainpc-core-plugin").append("\n")
        sb.append("Recommended order: ainpc.semantic.context.summary -> ainpc.story.semantic.context.summary -> ainpc.mapping.semantic.context.summary -> ainpc.quest.semantic.context.summary -> ainpc.quest.authoring.context.summary -> ainpc.routing.semantic.context.summary\n")
        for (line in lines) {
            sb.append("- ").append(line).append("\n")
        }
        if (includeMcpState) {
            appendMcpState(sb, plugin)
        }
        sb.append("Feature gate: ").append(plugin.server.name).append("\n")
        return DebugDumpSecrets.redactText(sb.toString())
    }

    private fun appendMcpState(sb: StringBuilder, plugin: AINPCPlugin) {
        val health = plugin.mcpRuntimeClient.health()
        sb.append("MCP runtime: ")
            .append(health.status)
            .append(" (")
            .append(health.durationMillis)
            .append("ms, ")
            .append(health.endpoint)
            .append(")\n")
        sb.append("MCP available: ").append(health.available).append("\n")
        if (!health.available || !health.enabled) {
            sb.append("MCP feature state unavailable: ").append(health.detail).append("\n")
            return
        }

        val featureState = plugin.mcpRuntimeClient.callTool("ainpc.feature.state")
        sb.append("Feature state tool: ").append(featureState.status).append(" (").append(featureState.durationMillis).append("ms)\n")
        val toolsObject = runCatching {
            JsonParser.parseString(featureState.contentJson).asJsonObject.getAsJsonObject("tools")
        }.getOrNull()
        if (toolsObject != null) {
            sb.append("Semantic exports: ")
                .append("semantic=").append(toolsObject.get("semanticContextExport")?.asBoolean ?: false)
                .append(", story=").append(toolsObject.get("storySemanticContextExport")?.asBoolean ?: false)
                .append(", mapping=").append(toolsObject.get("mappingSemanticContextExport")?.asBoolean ?: false)
                .append(", quest=").append(toolsObject.get("questSemanticContextExport")?.asBoolean ?: false)
                .append(", routing=").append(toolsObject.get("routingSemanticContextExport")?.asBoolean ?: false)
                .append("\n")
        } else {
            sb.append("Feature state payload unavailable.\n")
        }
    }
}
