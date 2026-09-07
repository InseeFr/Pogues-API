package fr.insee.pogues.service.release.steps.publication.mode;

import fr.insee.pogues.client.surveyregistry.questionnaire.QuestionnaireRegistryClient;
import fr.insee.pogues.client.surveyregistry.model.CollectionInstrumentCreateDto;
import fr.insee.pogues.domain.enums.release.ReleaseRequestModeStep;
import fr.insee.pogues.exception.release.steps.PublishPreReleaseException;
import fr.insee.pogues.service.release.context.ReleaseModeWorkflowContext;
import fr.insee.pogues.service.release.steps.ReleaseModeWorkflowStep;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@AllArgsConstructor
public class PublishPreRelease implements ReleaseModeWorkflowStep {

    private final QuestionnaireRegistryClient registryClient;

    @Override
    public ReleaseRequestModeStep getStep() {
        return ReleaseRequestModeStep.PUBLICATION_PRERELEASE;
    }

    @Override
    public void execute(ReleaseModeWorkflowContext workflowContext) {
        try {
            UUID collectionInstrumentId = initCollectionInstrument(workflowContext);
            workflowContext.setCollectionInstrumentId(collectionInstrumentId);
        } catch (Exception e) {
            throw new PublishPreReleaseException(e.getMessage());
        }
    }

    private UUID initCollectionInstrument(ReleaseModeWorkflowContext workflowContext) {

        CollectionInstrumentCreateDto collectionInstrumentCreateDto = new CollectionInstrumentCreateDto(
                workflowContext.getReleaseContext().getPoguesVersionId(),
                workflowContext.getReleaseContext().getReleaseDescription(),
                workflowContext.getMode(),
                workflowContext.getGenerationParameters()
        );

        return registryClient.createCollectionInstrumentMetadata(collectionInstrumentCreateDto);
    }
}