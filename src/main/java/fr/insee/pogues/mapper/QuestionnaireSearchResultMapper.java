package fr.insee.pogues.mapper;

import fr.insee.pogues.model.dto.metadata.SerieDto;
import fr.insee.pogues.model.dto.questionnaires.QuestionnaireSearchResultDTO;
import fr.insee.pogues.persistence.repository.jpa.QuestionnaireJpaRepository.SearchResultProjection;
import org.springframework.stereotype.Component;

@Component
public class QuestionnaireSearchResultMapper {

    public QuestionnaireSearchResultDTO toDto(SearchResultProjection projection) {
        if (projection == null) {
            return null;
        }

        return new QuestionnaireSearchResultDTO(
                projection.getId(),
                projection.getLabel(),
                projection.getLastUpdatedDate(),
                projection.getOwner(),
                computeSerie(projection)
        );
    }

    /** Only return the serie if the questionnaire is linked to one. */
    private SerieDto computeSerie(SearchResultProjection projection) {
        if (projection.getSerieId() == null) {
            return null;
        }
        return new SerieDto(
                projection.getSerieId(),
                projection.getSerieUri(),
                projection.getSerieLabel(),
                projection.getSerieAltLabel()
        );
    }
}
