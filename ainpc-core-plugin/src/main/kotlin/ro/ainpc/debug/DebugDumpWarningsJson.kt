package ro.ainpc.debug

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin
import ro.ainpc.engine.FeaturePackLoader

object DebugDumpWarningsJson {

    @JvmStatic
    fun buildWarningsJson(plugin: AINPCPlugin): JsonObject {
        val root = JsonObject()
        root.addProperty("version", 1)

        val loader = runCatching { plugin.featurePackLoader }.getOrNull()
        if (loader == null) {
            root.addProperty("total_warnings", 0)
            root.add("warnings", JsonArray())
            return root
        }

        val warningsArray = JsonArray()
        var total = 0
        for (scenario in loader.getAllScenarios()) {
            if (!DebugDumpQuestDefinitionJson.isLoadedQuestDefinitionCandidate(scenario)) {
                continue
            }
            if (scenario.validationWarningDetails.isEmpty()) {
                continue
            }
            for (warning in scenario.validationWarningDetails) {
                val entry = JsonObject()
                entry.addProperty("template_id", DebugDumpSupport.questTemplateId(scenario))
                if (scenario.sourceFile.isNotBlank()) entry.addProperty("source_file", scenario.sourceFile)
                entry.addProperty("type", warning.type)
                entry.addProperty("message", warning.message)
                if (warning.context.isNotBlank()) {
                    entry.addProperty("context", warning.context)
                }
                warningsArray.add(entry)
                total++
            }
        }

        root.addProperty("total_warnings", total)
        root.add("warnings", warningsArray)
        return root
    }
}
