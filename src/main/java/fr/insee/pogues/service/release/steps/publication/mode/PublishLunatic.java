package fr.insee.pogues.service.release.steps.publication.mode;

import fr.insee.pogues.client.surveyregistry.questionnaire.QuestionnaireRegistryClient;
import fr.insee.pogues.domain.enums.release.ReleaseRequestModeStep;
import fr.insee.pogues.exception.release.steps.PublishLunaticException;
import fr.insee.pogues.service.release.context.ReleaseModeWorkflowContext;
import fr.insee.pogues.service.release.steps.ReleaseModeWorkflowStep;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@AllArgsConstructor
public class PublishLunatic implements ReleaseModeWorkflowStep {

    private final QuestionnaireRegistryClient registryClient;

    @Override
    public ReleaseRequestModeStep getStep() {
        return ReleaseRequestModeStep.PUBLICATION_LUNATIC;
    }

    @Override
    public void execute(ReleaseModeWorkflowContext workflowContext) {
        try {
            String lunatic = workflowContext.getLunatic();
            UUID collectionInstrumentId = workflowContext.getCollectionInstrumentId();
            registryClient.publishLunaticInCollectionInstrument(collectionInstrumentId, lunatic);
        } catch (Exception e) {
            throw new PublishLunaticException(e.getMessage());
        }
    }

    @Override
    public void afterExecute(ReleaseModeWorkflowContext workflowContext){
        workflowContext.clearLunatic();
    }
}
