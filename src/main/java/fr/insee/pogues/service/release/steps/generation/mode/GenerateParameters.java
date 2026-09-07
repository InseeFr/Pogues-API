package fr.insee.pogues.service.release.steps.generation.mode;

import fr.insee.pogues.client.generation.EnoClient;
import fr.insee.pogues.domain.enums.generation.CollectMode;
import fr.insee.pogues.domain.enums.generation.GenerationContext;
import fr.insee.pogues.domain.enums.generation.GenerationFormat;
import fr.insee.pogues.domain.enums.generation.parameters.GenerationParameters;
import fr.insee.pogues.domain.enums.release.ReleaseRequestModeStep;
import fr.insee.pogues.exception.release.steps.GenerateParametersException;
import fr.insee.pogues.service.release.context.ReleaseModeWorkflowContext;
import fr.insee.pogues.service.release.steps.ReleaseModeWorkflowStep;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class GenerateParameters implements ReleaseModeWorkflowStep {

    private final EnoClient enoClient;

    @Override
    public ReleaseRequestModeStep getStep() {
        return ReleaseRequestModeStep.GENERATE_PARAMETERS;
    }

    @Override
    public void execute(ReleaseModeWorkflowContext workflowContext) {
        try {
            GenerationContext context = workflowContext.getReleaseContext().getCommonParameters().getContext();
            GenerationFormat outFormat = GenerationFormat.LUNATIC;
            CollectMode mode = workflowContext.getMode();

            GenerationParameters generationParameters = enoClient.getGenerationParameters(context, outFormat, mode);

            // override only if context is BUSINESS
            if(GenerationContext.BUSINESS.equals(context)){
                if(workflowContext.getReleaseContext().getCommonParameters().getResponseTimeQuestion() != null){
                    generationParameters.setResponseTimeQuestion(workflowContext.getReleaseContext().getCommonParameters().getResponseTimeQuestion());
                }
                if(workflowContext.getReleaseContext().getCommonParameters().getQuestionNumberingMode() != null){
                    generationParameters.setQuestionNumberingMode(workflowContext.getReleaseContext().getCommonParameters().getQuestionNumberingMode());
                }
            }
            workflowContext.setGenerationParameters(generationParameters);
        } catch (Exception e) {
            throw new GenerateParametersException(e.getMessage());
        }
    }
}
