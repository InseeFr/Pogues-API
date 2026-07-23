package fr.insee.pogues.model.dto.details;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.insee.pogues.model.DataCollection;
import fr.insee.pogues.model.FlowLogicEnum;
import fr.insee.pogues.model.FormulasLanguageEnum;
import fr.insee.pogues.model.SurveyModeEnum;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionnaireDetailsDto(
        String id,
        String name,
        String label,
        FlowLogicEnum flowLogic,
        FormulasLanguageEnum formulasLanguage,
        DataCollection dataCollection,
        List<SurveyModeEnum> targetMode,
        String agency,
        String owner
) {
}
