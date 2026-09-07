package fr.insee.pogues.service.release.steps;

import fr.insee.pogues.domain.enums.release.ReleaseRequestStep;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ReleaseWorkflowStepRegistry {

    private static final List<ReleaseRequestStep> EXECUTION_ORDER = List.of(
            ReleaseRequestStep.PUBLICATION_CONCEPTUAL_PRERELEASE,
            ReleaseRequestStep.GENERATE_DDI,
            ReleaseRequestStep.PUBLICATION_DDI
    );

    private final Map<ReleaseRequestStep, ReleaseWorkflowStep> workflowStepsByType;

    public ReleaseWorkflowStepRegistry(List<ReleaseWorkflowStep> steps) {
        this.workflowStepsByType = buildWorkflowSteps(steps);
        validateAllStepsArePresent();
    }

    private Map<ReleaseRequestStep, ReleaseWorkflowStep> buildWorkflowSteps(List<ReleaseWorkflowStep> steps) {
        Map<ReleaseRequestStep, ReleaseWorkflowStep> map = new EnumMap<>(ReleaseRequestStep.class);

        for (ReleaseWorkflowStep step : steps) {
            ReleaseRequestStep stepType = step.getStep();

            if (map.containsKey(stepType)) {
                throw new IllegalStateException("Duplicate ReleaseWorkflowStep implementation for " + stepType);
            }
            map.put(stepType, step);
        }
        return Collections.unmodifiableMap(map);
    }

    private void validateAllStepsArePresent() {
        List<ReleaseRequestStep> missing = new ArrayList<>();

        for (ReleaseRequestStep expectedStep : EXECUTION_ORDER) {
            if (!workflowStepsByType.containsKey(expectedStep)) {
                missing.add(expectedStep);
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing ReleaseWorkflowStep implementation(s) for: " + missing);
        }
    }

    public List<ReleaseWorkflowStep> orderedSteps() {
        return EXECUTION_ORDER.stream().map(workflowStepsByType::get).toList();
    }
}