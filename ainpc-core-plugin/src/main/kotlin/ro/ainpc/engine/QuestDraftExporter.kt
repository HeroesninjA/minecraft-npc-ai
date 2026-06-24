package ro.ainpc.engine

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class QuestDraftExporter {

    fun exportDraft(params: QuestDraftParams): String {
        val root = JsonObject()
        root.addProperty("draftId", params.draftId)
        root.addProperty("title", params.title)
        root.addProperty("description", params.description)
        root.addProperty("mechanicId", params.mechanicId)
        root.addProperty("kind", params.kind)
        root.addProperty("baseType", params.baseType)
        root.addProperty("npcGiver", params.npcGiver)
        root.addProperty("npcGiverPlace", params.npcGiverPlace)
        root.addProperty("createdAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        val objectivesArr = JsonArray()
        for (obj in params.objectives) {
            val o = JsonObject()
            o.addProperty("type", obj.type)
            o.addProperty("target", obj.target)
            o.addProperty("count", obj.count)
            if (obj.stageId.isNotBlank()) o.addProperty("stage", obj.stageId)
            if (obj.dialog.isNotBlank()) o.addProperty("dialog", obj.dialog)
            objectivesArr.add(o)
        }
        root.add("objectives", objectivesArr)

        val stagesArr = JsonArray()
        for (st in params.stages) {
            val s = JsonObject()
            s.addProperty("id", st.id)
            s.addProperty("name", st.name)
            s.addProperty("completionMode", st.completionMode)
            if (st.nextStage.isNotBlank()) s.addProperty("next_stage", st.nextStage)
            stagesArr.add(s)
        }
        root.add("stages", stagesArr)

        val rewardsArr = JsonArray()
        for (rw in params.rewards) {
            val r = JsonObject()
            r.addProperty("type", rw.type)
            r.addProperty("value", rw.value)
            r.addProperty("count", rw.count)
            if ((rw.type == "story_event" || rw.type == "record_story_event") && rw.eventKey.isNotBlank()) {
                r.addProperty("scope", rw.eventScope.ifBlank { "region" })
                r.addProperty("target", rw.eventTarget.ifBlank { "current_region" })
                r.addProperty("event_type", rw.eventType)
                r.addProperty("event_key", rw.eventKey)
                if (rw.eventTitle.isNotBlank()) r.addProperty("title", rw.eventTitle)
                if (rw.eventPayload.isNotEmpty()) {
                    val payload = JsonObject()
                    for ((k, v) in rw.eventPayload) payload.addProperty(k, v)
                    r.add("payload", payload)
                }
            }
            rewardsArr.add(r)
        }
        root.add("rewards", rewardsArr)

        val dialogArr = JsonArray()
        for (msg in params.dialogMessages) {
            val d = JsonObject()
            d.addProperty("type", msg.type)
            d.addProperty("speaker", msg.speaker)
            d.addProperty("message", msg.message)
            if (msg.condition.isNotBlank()) d.addProperty("condition", msg.condition)
            dialogArr.add(d)
        }
        root.add("dialog", dialogArr)

        val systemArr = JsonArray()
        for (msg in params.systemMessages) {
            val m = JsonObject()
            m.addProperty("text", msg)
            systemArr.add(m)
        }
        root.add("systemMessages", systemArr)

        if (params.npcGiver.isNotBlank()) {
            val anchor = JsonObject()
            anchor.addProperty("type", "npc")
            anchor.addProperty("target", params.npcGiver)
            if (params.npcGiverPlace.isNotBlank()) {
                anchor.addProperty("place", params.npcGiverPlace)
            }
            root.add("questAnchor", anchor)
        }

        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(root)
    }

    data class QuestDraftParams(
        val draftId: String,
        val title: String,
        val description: String = "",
        val mechanicId: String,
        val kind: String = "",
        val baseType: String = "QUEST",
        val npcGiver: String = "",
        val npcGiverPlace: String = "",
        val objectives: List<ObjectiveDef> = listOf(),
        val stages: List<StageDef> = listOf(),
        val rewards: List<RewardDef> = listOf(),
        val dialogMessages: List<DialogDef> = listOf(),
        val systemMessages: List<String> = listOf()
    )

    data class ObjectiveDef(
        val type: String,
        val target: String,
        val count: Int = 1,
        val stageId: String = "",
        val dialog: String = ""
    )

    data class StageDef(
        val id: String,
        val name: String = "",
        val completionMode: String = "all",
        val nextStage: String = ""
    )

    data class RewardDef(
        val type: String,
        val value: String,
        val count: Int = 1,
        val eventScope: String = "",
        val eventTarget: String = "",
        val eventType: String = "quest_completed",
        val eventKey: String = "",
        val eventTitle: String = "",
        val eventPayload: Map<String, String> = emptyMap()
    )

    data class DialogDef(
        val type: String,
        val speaker: String,
        val message: String,
        val condition: String = ""
    )
}
