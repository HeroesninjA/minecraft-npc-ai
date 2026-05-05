package ro.ainpc.engine;

import java.util.List;
import java.util.function.Predicate;

final class QuestTemplateSelector {

    private QuestTemplateSelector() {
    }

    static ScenarioEngine.ScenarioTemplate selectConfiguredTemplate(
        List<ScenarioEngine.ScenarioTemplate> templates,
        Predicate<ScenarioEngine.ScenarioTemplate> available,
        Predicate<ScenarioEngine.ScenarioTemplate> completed
    ) {
        if (templates == null || templates.isEmpty()) {
            return null;
        }

        Predicate<ScenarioEngine.ScenarioTemplate> safeAvailable = available != null ? available : template -> true;
        Predicate<ScenarioEngine.ScenarioTemplate> safeCompleted = completed != null ? completed : template -> false;
        ScenarioEngine.ScenarioTemplate firstAvailableCompletedTemplate = null;
        ScenarioEngine.ScenarioTemplate firstUnavailableTemplate = null;

        for (ScenarioEngine.ScenarioTemplate template : templates) {
            if (template == null) {
                continue;
            }

            if (!safeAvailable.test(template)) {
                if (firstUnavailableTemplate == null) {
                    firstUnavailableTemplate = template;
                }
                continue;
            }

            if (!safeCompleted.test(template)) {
                return template;
            }

            if (firstAvailableCompletedTemplate == null) {
                firstAvailableCompletedTemplate = template;
            }
        }

        return firstAvailableCompletedTemplate != null
            ? firstAvailableCompletedTemplate
            : firstUnavailableTemplate;
    }
}
