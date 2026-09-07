package fr.insee.pogues.service.release.steps;

import fr.insee.pogues.domain.enums.release.ReleaseRequestStep;
import fr.insee.pogues.service.release.context.ReleaseWorkflowContext;

public interface ReleaseWorkflowStep {
    ReleaseRequestStep getStep();
    void execute(ReleaseWorkflowContext workflowContext);
    default void afterExecute(ReleaseWorkflowContext workflowContext) {}
}