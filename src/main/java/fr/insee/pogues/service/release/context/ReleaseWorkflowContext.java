package fr.insee.pogues.service.release.context;

import fr.insee.pogues.domain.enums.generation.parameters.GenerationParameters;
import fr.insee.pogues.model.Questionnaire;
import fr.insee.pogues.service.release.steps.generation.mode.GenerateParameters;
import lombok.*;

import java.util.UUID;

@Getter
@ToString
public class ReleaseWorkflowContext {
    private final Long releaseRequestId;
    private final String releaseDescription;
    private Questionnaire poguesQuestionnaire;
    private final UUID poguesVersionId;
    private final GenerationParameters commonParameters;
    @Setter
    private String ddi;

    public ReleaseWorkflowContext(Long releaseRequestId,
                                  UUID poguesVersionId,
                                  String releaseDescription,
                                  Questionnaire poguesQuestionnaire,
                                  GenerationParameters commonParameters) {
        this.releaseRequestId = releaseRequestId;
        this.poguesQuestionnaire = poguesQuestionnaire;
        this.releaseDescription = releaseDescription;
        this.poguesVersionId = poguesVersionId;
        this.commonParameters = commonParameters;
    }


    public void clearDDI(){
        this.ddi = null;
    }
    public void clearPogues(){
        this.poguesQuestionnaire = null;
    }

}
