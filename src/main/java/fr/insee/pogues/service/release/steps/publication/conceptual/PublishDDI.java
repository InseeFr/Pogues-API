package fr.insee.pogues.service.release.steps.publication.conceptual;

import fr.insee.pogues.client.surveyregistry.questionnaire.QuestionnaireRegistryClient;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStep;
import fr.insee.pogues.exception.release.steps.PublishDDIException;
import fr.insee.pogues.service.release.context.ReleaseWorkflowContext;
import fr.insee.pogues.service.release.steps.ReleaseWorkflowStep;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@AllArgsConstructor
public class PublishDDI implements ReleaseWorkflowStep {

    private final QuestionnaireRegistryClient registryClient;

    @Override
    public ReleaseRequestStep getStep() {
        return ReleaseRequestStep.PUBLICATION_DDI;
    }

    @Override
    public void execute(ReleaseWorkflowContext workflowContext) {
        try {
            String ddi = workflowContext.getDdi();
            UUID poguesVersionId = workflowContext.getPoguesVersionId();
            registryClient.publishDDIInConceptualModel(poguesVersionId, ddi);
        } catch (Exception e) {
            throw new PublishDDIException(e.getMessage());
        }
    }

    @Override
    public void afterExecute(ReleaseWorkflowContext workflowContext){
        workflowContext.clearDDI();
    }
}
