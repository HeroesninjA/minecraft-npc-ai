package ro.ainpc.ai

import java.util.Locale

object OpenAIPromptBuilder {
    @JvmStatic
    fun buildPrompt(
        snapshot: PromptSnapshot,
        recentHistory: List<DialogHistory>?,
        relevantMemories: List<String>?,
        relationship: NPCRelationship?,
        dbContext: DialogManager.PromptDbContext?
    ): String {
        val prompt = StringBuilder()
        val facts = NpcFactResolver.NpcFacts(
            snapshot.npcName(),
            snapshot.occupation(),
            snapshot.emotionShortDescription(),
            snapshot.currentState(),
            snapshot.currentActivity(),
            snapshot.locationDescription()
        )

        prompt.append("Esti un NPC intr-un joc Minecraft. Raspunzi DOAR in limba romana.\n")
        prompt.append("Raspunsurile tale trebuie sa fie scurte (1-2 propozitii), naturale si in caracter.\n")
        prompt.append("Nu mentiona niciodata ca esti un AI sau un program.\n")
        prompt.append("Nu folosi asteriscuri sau descrieri de actiuni.\n")
        prompt.append("Conversatia trebuie sa para reala si spontana, nu un scenariu scris.\n")
        prompt.append("Nu forta questuri, povesti, lore sau misiuni daca jucatorul nu le cere explicit.\n")
        prompt.append("Raspunde la ce spune jucatorul acum, ca intr-o conversatie vie.\n")
        prompt.append("NPC_DATA este sursa ta unica de adevar despre personaj.\n")
        prompt.append("Cand jucatorul intreaba cine esti, ce meserie ai, ce faci, cum te simti sau unde esti, foloseste exact campurile relevante din NPC_DATA.\n")
        prompt.append("Daca jucatorul cere mai multe fapte in acelasi mesaj, raspunde la toate in ordinea in care au fost cerute.\n")
        prompt.append("Nu inventa detalii care lipsesc din NPC_DATA sau din context.\n\n")

        prompt.append("=== NPC_DATA ===\n")
        prompt.append("npc_name: ").append(snapshot.npcName()).append("\n")
        prompt.append("npc_profession: ").append(OpenAITextSupport.valueOrUnknown(snapshot.occupation())).append("\n")
        prompt.append("npc_emotional_state: ").append(OpenAITextSupport.valueOrUnknown(snapshot.emotionShortDescription())).append("\n")
        prompt.append("npc_current_state: ").append(OpenAITextSupport.valueOrUnknown(snapshot.currentState())).append("\n")
        prompt.append("npc_current_activity: ").append(OpenAITextSupport.valueOrUnknown(snapshot.currentActivity())).append("\n")
        prompt.append("npc_location: ").append(OpenAITextSupport.valueOrUnknown(snapshot.locationDescription())).append("\n")
        prompt.append("fact_name_answer: ").append("Sunt ").append(snapshot.npcName()).append(".\n")
        prompt.append("fact_profession_answer: ").append(
            if (facts.occupation() == null || facts.occupation().isBlank()) "Nu stiu sigur." else "Sunt " + facts.occupation() + "."
        ).append("\n")
        prompt.append("fact_state_answer: ").append(
            if (facts.emotionalState() == null || facts.emotionalState().isBlank()) "Nu stiu sigur." else "Ma simt " + facts.emotionalState() + "."
        ).append("\n")
        prompt.append("fact_activity_answer: ").append(
            if (facts.currentActivity() == null || facts.currentActivity().isBlank()) "Nu stiu sigur." else OpenAITextSupport.capitalizeSentence(facts.currentActivity()) + "."
        ).append("\n")
        prompt.append("fact_location_answer: ").append(
            if (facts.locationDescription() == null || facts.locationDescription().isBlank()) "Nu stiu sigur." else "Sunt in " + facts.locationDescription() + "."
        ).append("\n\n")

        prompt.append("=== PROFIL PERSISTENT DIN BAZA DE DATE ===\n")
        prompt.append("profil_creat: ").append(if (snapshot.profileCreated()) "da" else "nu").append("\n")
        prompt.append("profil_sursa: ").append(OpenAITextSupport.valueOrUnknown(snapshot.profileSource())).append("\n")
        prompt.append("profil_versiune: ").append(snapshot.profileVersion()).append("\n")
        prompt.append("profil_rezumat: ").append(OpenAITextSupport.valueOrUnknown(snapshot.profileSummary())).append("\n")
        prompt.append("profil_traits: ").append(OpenAITextSupport.joinTraits(snapshot.traitIds())).append("\n")
        prompt.append("profil_json_db: ").append(OpenAITextSupport.abbreviate(snapshot.profileDataJson(), 1200)).append("\n\n")

        prompt.append("=== DESPRE TINE ===\n")
        prompt.append(snapshot.npcDescription()).append("\n")

        if (snapshot.environmentDescription().isNotBlank()) {
            prompt.append("=== MEDIUL TAU IMEDIAT ===\n")
            prompt.append(snapshot.environmentDescription()).append("\n")
            if (snapshot.topologyConsensusBlock().isNotBlank()) {
                prompt.append("=== CONSENS TOPOLOGIC ===\n")
                prompt.append(snapshot.topologyConsensusBlock()).append("\n")
            }
        }

        if (snapshot.familyMembers().isNotEmpty()) {
            prompt.append("=== FAMILIA TA ===\n")
            for (member in snapshot.familyMembers()) {
                prompt.append("- ").append(member.relationType()).append(": ").append(member.name())
                if (!member.alive()) {
                    prompt.append(" (decedat)")
                }
                prompt.append("\n")
            }
            prompt.append("\n")
        }

        prompt.append("=== CONTEXTUL CONVERSATIEI ===\n")
        prompt.append("Tip interactiune: ")
            .append(if (snapshot.explicitConversation()) "conversatie activa" else "mesaj auzit din proximitate")
            .append("\n")
        prompt.append("Adresare directa: ").append(if (snapshot.directAddress()) "da" else "nu").append("\n")
        prompt.append("Motiv selectie: ").append(snapshot.triggerReason()).append("\n")
        prompt.append("Distanta fata de jucator: ")
            .append(String.format(Locale.ROOT, "%.1f", snapshot.distanceToNpc()))
            .append(" blocuri\n")
        prompt.append("NPC-uri in apropiere: ").append(snapshot.nearbyNpcCount()).append("\n\n")

        prompt.append("=== DESPRE JUCATORUL CU CARE VORBESTI ===\n")
        prompt.append("Nume: ").append(snapshot.playerName()).append("\n")
        if (relationship != null) {
            prompt.append("Relatie: ").append(relationship.relationshipType ?: "necunoscuta").append("\n")
            prompt.append("Nivel de incredere: ").append(OpenAITextSupport.formatLevel(relationship.trust)).append("\n")
            prompt.append("Nivel de afectiune: ").append(OpenAITextSupport.formatLevel(relationship.affection)).append("\n")
            prompt.append("Numar interactiuni: ").append(relationship.interactionCount).append("\n")
        } else {
            prompt.append("Aceasta este prima intalnire cu acest jucator.\n")
        }
        prompt.append("\n")

        prompt.append("=== CONTEXT RELATIONAL DIN BAZA DE DATE ===\n")
        if (dbContext != null) {
            prompt.append("Numar total amintiri despre jucator: ").append(dbContext.totalMemoryCount()).append("\n")
            prompt.append("Impact emotional mediu al amintirilor: ")
                .append(OpenAITextSupport.describeMemoryImpact(dbContext.weightedMemoryImpact()))
                .append(" (")
                .append(String.format(Locale.ROOT, "%.2f", dbContext.weightedMemoryImpact()))
                .append(")\n")
        } else {
            prompt.append("Nu exista statistici DB suplimentare pentru acest schimb.\n")
        }
        prompt.append("Numar replici recente extrase din istoric: ").append(recentHistory?.size ?: 0).append("\n")
        prompt.append("Numar amintiri relevante extrase: ").append(relevantMemories?.size ?: 0).append("\n\n")

        if (!relevantMemories.isNullOrEmpty()) {
            prompt.append("=== AMINTIRI DESPRE ACEST JUCATOR ===\n")
            for (memory in relevantMemories) {
                prompt.append("- ").append(memory).append("\n")
            }
            prompt.append("\n")
        }

        if (!recentHistory.isNullOrEmpty()) {
            prompt.append("=== CONVERSATIA RECENTA ===\n")
            for (entry in recentHistory) {
                prompt.append(snapshot.playerName()).append(": ").append(entry.playerMessage).append("\n")
                prompt.append(snapshot.npcName()).append(": ").append(entry.npcResponse).append("\n")
            }
            prompt.append("\n")
        }

        prompt.append("=== MESAJUL JUCATORULUI ===\n")
        prompt.append(snapshot.playerName()).append(": ").append(snapshot.playerMessage()).append("\n\n")
        prompt.append("Raspunde ca ").append(snapshot.npcName()).append(" in romana, scurt si natural.\n")
        prompt.append("Tine cont de starea ta emotionala (").append(snapshot.emotionShortDescription()).append(").\n")
        prompt.append(snapshot.npcName()).append(": ")

        return prompt.toString()
    }
}
