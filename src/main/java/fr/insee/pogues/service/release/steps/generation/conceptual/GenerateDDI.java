package fr.insee.pogues.service.release.steps.generation.conceptual;

import fr.insee.pogues.client.generation.EnoClient;
import fr.insee.pogues.conversion.JSONToXMLTranslator;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStep;
import fr.insee.pogues.exception.release.steps.GenerateDDIException;
import fr.insee.pogues.model.Questionnaire;
import fr.insee.pogues.service.release.context.ReleaseWorkflowContext;
import fr.insee.pogues.service.release.steps.ReleaseWorkflowStep;
import fr.insee.pogues.utils.PoguesSerializer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@AllArgsConstructor
public class GenerateDDI implements ReleaseWorkflowStep {

    private final EnoClient enoClient;
    private final JSONToXMLTranslator jsonToXMLTranslator = new JSONToXMLTranslator();

    @Override
    public ReleaseRequestStep getStep() {
        return ReleaseRequestStep.GENERATE_DDI;
    }

    @Override
    public void execute(ReleaseWorkflowContext workflowContext) {
        workflowContext.setDdi(generateDDI(workflowContext.getPoguesQuestionnaire()));
    }

    private String generateDDI(Questionnaire questionnaire) {
        try {
            String jsonPogues = PoguesSerializer.questionnaireJavaToString(questionnaire);
            String xmlPogues = jsonToXMLTranslator.translate(jsonPogues);
            return enoClient.getPoguesXmlToDDI(xmlPogues);
        } catch (Exception e) {
            throw new GenerateDDIException(e.getMessage());
        }
    }
}