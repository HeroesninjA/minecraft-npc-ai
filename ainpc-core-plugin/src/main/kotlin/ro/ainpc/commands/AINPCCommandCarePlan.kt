package ro.ainpc.commands

import ro.ainpc.engine.DecisionEngine
import java.util.Locale

internal fun formatDecisionCarePlan(preview: DecisionEngine.ActionDecisionPreview): List<String> {
    val plan = preview.carePlan ?: return emptyList()
    return buildList {
        add("&ePlan ingrijire [SHADOW, fara executie]: &f${plan.status.label}")
        if (plan.goal != null) add("&7Obiectiv propus: ${plan.goal.name}; prag observat necesar: ${plan.targetNeed}/100")
        plan.alternatives.forEachIndexed { index, alternative ->
            val marker = if (index == 0) "preferata" else "alternativa"
            val probability = String.format(Locale.ROOT, "%.1f", (preview.probabilities[alternative.action] ?: 0.0) * 100)
            add("&7$marker: ${alternative.action.name}; cost euristic ${alternative.heuristicCost}; " +
                "Utility acum: $probability%")
            add("&7Pasi propusi: ${alternative.steps.joinToString(" -> ")}")
        }
        if (plan.selected != null) {
            add("&7Costul este ordinal, nu durata. Casa vine din context; ruta, resursele si succesul raman neverificate.")
        }
    }
}
