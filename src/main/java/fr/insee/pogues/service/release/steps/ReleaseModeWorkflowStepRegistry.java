package fr.insee.pogues.service.release.steps;

import fr.insee.pogues.domain.enums.release.ReleaseRequestModeStep;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ReleaseModeWorkflowStepRegistry {

    private static final List<ReleaseRequestModeStep> EXECUTION_ORDER = List.of(
            ReleaseRequestModeStep.GENERATE_PARAMETERS,
            ReleaseRequestModeStep.PUBLICATION_PRERELEASE,
            ReleaseRequestModeStep.GENERATE_LUNATIC,
            ReleaseRequestModeStep.PUBLICATION_LUNATIC
    );

    private final Map<ReleaseRequestModeStep, ReleaseModeWorkflowStep> workflowStepsByType;

    public ReleaseModeWorkflowStepRegistry(List<ReleaseModeWorkflowStep> steps) {
        this.workflowStepsByType = buildWorkflowSteps(steps);
        validateAllStepsArePresent();
    }

    private Map<ReleaseRequestModeStep, ReleaseModeWorkflowStep> buildWorkflowSteps(List<ReleaseModeWorkflowStep> steps) {
        Map<ReleaseRequestModeStep, ReleaseModeWorkflowStep> map = new EnumMap<>(ReleaseRequestModeStep.class);

        for (ReleaseModeWorkflowStep step : steps) {
            ReleaseRequestModeStep stepType = step.getStep();

            if (map.containsKey(stepType)) {
                throw new IllegalStateException("Duplicate ReleaseWorkflowStep implementation for " + stepType);
            }
            map.put(stepType, step);
        }
        return Collections.unmodifiableMap(map);
    }

    private void validateAllStepsArePresent() {
        List<ReleaseRequestModeStep> missing = new ArrayList<>();

        for (ReleaseRequestModeStep expectedStep : EXECUTION_ORDER) {
            if (!workflowStepsByType.containsKey(expectedStep)) {
                missing.add(expectedStep);
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing ReleaseWorkflowStep implementation(s) for: " + missing);
        }
    }

    public List<ReleaseModeWorkflowStep> orderedSteps() {
        return EXECUTION_ORDER.stream().map(workflowStepsByType::get).toList();
    }
}