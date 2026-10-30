package fr.insee.pogues.model.dto.questionnaires;

import fr.insee.pogues.model.dto.metadata.SerieDto;

/**
 * Light representation of a questionnaire, returned when searching questionnaires.
 */
public record QuestionnaireSearchResultDTO(
        String id,
        String label,
        String lastUpdatedDate,
        String owner,
        SerieDto serie
) {
}
