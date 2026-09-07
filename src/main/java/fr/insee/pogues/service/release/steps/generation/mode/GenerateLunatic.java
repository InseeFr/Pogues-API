package fr.insee.pogues.service.release.steps.generation.mode;

import fr.insee.pogues.client.generation.EnoClient;
import fr.insee.pogues.domain.enums.generation.parameters.GenerationParameters;
import fr.insee.pogues.domain.enums.release.ReleaseRequestModeStep;
import fr.insee.pogues.exception.release.steps.GenerateLunaticException;
import fr.insee.pogues.model.Questionnaire;
import fr.insee.pogues.service.release.context.ReleaseModeWorkflowContext;
import fr.insee.pogues.service.release.steps.ReleaseModeWorkflowStep;
import fr.insee.pogues.utils.PoguesSerializer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class GenerateLunatic implements ReleaseModeWorkflowStep {

    private final EnoClient enoClient;

    @Override
    public ReleaseRequestModeStep getStep() {
        return ReleaseRequestModeStep.GENERATE_LUNATIC;
    }

    @Override
    public void execute(ReleaseModeWorkflowContext workflowContext) {
        String lunatic = generateLunatic(workflowContext.getReleaseContext().getPoguesQuestionnaire(), workflowContext.getGenerationParameters());
        workflowContext.setLunatic(lunatic);
    }

    private String generateLunatic(Questionnaire questionnaire, GenerationParameters generationParameters) {
        try {
            String jsonPogues = PoguesSerializer.questionnaireJavaToString(questionnaire);
            return enoClient.getPoguesJsonToLunaticJson(jsonPogues, generationParameters);
        } catch (Exception e) {
            throw new GenerateLunaticException(e.getMessage());
        }
    }
}
