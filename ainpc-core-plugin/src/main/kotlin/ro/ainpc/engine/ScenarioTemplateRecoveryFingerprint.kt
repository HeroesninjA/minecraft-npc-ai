package ro.ainpc.engine

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.HexFormat

internal object ScenarioTemplateRecoveryFingerprint {
    fun compute(template: ScenarioTemplate): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fun add(value: String?) {
            digest.update(if (value == null) 0.toByte() else 1.toByte())
            if (value != null) {
                val bytes = value.toByteArray(StandardCharsets.UTF_8)
                digest.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(bytes.size).array())
                digest.update(bytes)
            }
        }
        fun addList(values: Collection<String>) {
            add(values.size.toString())
            values.forEach(::add)
        }

        add("scenario-template-recovery-v1")
        add(template.templateId)
        add(template.type.name)
        add(template.sourcePackId)
        add(template.displayName)
        add(template.hint)
        addList(template.phases)
        add(template.progressionEnabled.toString())
        add(template.requiresPlayer.toString())
        add(template.questCode)
        add(template.questGiverProfession)
        add(template.minimumNpcCount.toString())
        add(template.roles.size.toString())
        template.roles.toSortedMap().forEach { (roleId, role) ->
            add(roleId)
            add(role.description)
            add(role.playerRole.toString())
            add(role.optional.toString())
            addList(role.requiredProfessions)
            addList(role.preferredProfessions)
            addList(role.requiredTraits)
            addList(role.preferredTraits)
        }
        add(template.actors.size.toString())
        template.actors.toSortedMap().forEach { (actorId, actor) ->
            add(actorId)
            add(actor.id)
            add(actor.name)
            add(actor.lifecycleType.name)
            add(actor.persistenceMode.name)
            add(actor.simulationMode.name)
            add(actor.interactionProfile.name)
            add(actor.entityKind.name)
            add(actor.entityArchetype)
            add(actor.ownerScenarioId)
            add(actor.ownerQuestId)
            add(actor.spawnSource)
            add(actor.spawnPhase)
            add(actor.spawnPolicy.name)
            add(actor.despawnRule)
            add(actor.durationSeconds?.toString())
            addList(actor.temporaryTags.sorted())
        }
        return HexFormat.of().formatHex(digest.digest())
    }
}
