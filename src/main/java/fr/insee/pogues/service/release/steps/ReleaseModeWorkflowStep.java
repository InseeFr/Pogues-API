package fr.insee.pogues.service.release.steps;

import fr.insee.pogues.domain.enums.release.ReleaseRequestModeStep;
import fr.insee.pogues.service.release.context.ReleaseModeWorkflowContext;

public interface ReleaseModeWorkflowStep {
    ReleaseRequestModeStep getStep();
    void execute(ReleaseModeWorkflowContext context);
    default void afterExecute(ReleaseModeWorkflowContext context) {}
}