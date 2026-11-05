package fr.insee.pogues.model.dto.details;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.insee.pogues.model.DataCollection;
import fr.insee.pogues.model.SurveyModeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * <p>Details of a questionnaire, as sent by the Pogues frontend form.</p>
 * <p>The same rules apply on creation and on update.</p>
 * @param id ID of the questionnaire, provided by the frontend
 * @param name Technical name of the questionnaire
 * @param label Title of the questionnaire
 * @param dataCollection Serie of the questionnaire (optional)
 * @param targetMode Collection modes of the questionnaire
 * @param agency DDI agency of the questionnaire
 * @param owner Stamp owning the questionnaire
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionnaireDetailsDto(
        @NotBlank String id,
        @NotBlank String name,
        @NotBlank String label,
        DataCollection dataCollection,
        @NotEmpty List<SurveyModeEnum> targetMode,
        @NotBlank String agency,
        @NotBlank String owner
) {
}
