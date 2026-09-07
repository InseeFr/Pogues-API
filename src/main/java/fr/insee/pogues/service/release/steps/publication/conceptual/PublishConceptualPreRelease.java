package fr.insee.pogues.service.release.steps.publication.conceptual;

import fr.insee.pogues.client.surveyregistry.questionnaire.QuestionnaireRegistryClient;
import fr.insee.pogues.client.surveyregistry.model.ConceptualModelDto;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStep;
import fr.insee.pogues.exception.release.steps.PublishPreReleaseException;
import fr.insee.pogues.model.Questionnaire;
import fr.insee.pogues.service.release.context.ReleaseWorkflowContext;
import fr.insee.pogues.service.release.steps.ReleaseWorkflowStep;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@AllArgsConstructor
public class PublishConceptualPreRelease implements ReleaseWorkflowStep {

    private final QuestionnaireRegistryClient registryClient;

    @Override
    public ReleaseRequestStep getStep() {
        return ReleaseRequestStep.PUBLICATION_CONCEPTUAL_PRERELEASE;
    }

    @Override
    public void execute(ReleaseWorkflowContext workflowContext) {
        try {
            Questionnaire questionnaire = workflowContext.getPoguesQuestionnaire();

            if (conceptualModelNotExistByPoguesVersionId(workflowContext.getPoguesVersionId())) {
                createConceptualModel(
                        questionnaire.getId(),
                        questionnaire.getDataCollection().getSerie().getId(),
                        workflowContext.getPoguesVersionId());
            }
        } catch (Exception e) {
            throw new PublishPreReleaseException(e.getMessage());
        }
    }

    private boolean conceptualModelNotExistByPoguesVersionId(UUID poguesVersionId) {
        return registryClient
                .findConceptualModelByPoguesVersionId(poguesVersionId)
                .isEmpty();
    }

    private void createConceptualModel(String poguesId, String serieId, UUID poguesVersionId) {
        registryClient.createConceptualModel(
                new ConceptualModelDto(poguesId, serieId, poguesVersionId)
        );
    }
}