package fr.insee.pogues.mapper;

import fr.insee.pogues.model.dto.metadata.SerieDto;
import fr.insee.pogues.model.dto.questionnaires.QuestionnaireSearchResultDTO;
import fr.insee.pogues.persistence.repository.jpa.QuestionnaireJpaRepository.SearchResultProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuestionnaireSearchResultMapperTest {

    private QuestionnaireSearchResultMapper questionnaireSearchResultMapper;

    @BeforeEach
    void setUp() {
        questionnaireSearchResultMapper = new QuestionnaireSearchResultMapper();
    }

    @Test
    @DisplayName("Should return null when the projection is null")
    void should_return_null_when_projection_is_null() {
        // When
        QuestionnaireSearchResultDTO result = questionnaireSearchResultMapper.toDto(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should map a projection with a serie to a DTO")
    void should_map_projection_with_serie_to_dto() {
        // Given
        SearchResultProjection projection = mock(SearchResultProjection.class);
        when(projection.getId()).thenReturn("q-id");
        when(projection.getLabel()).thenReturn("My questionnaire");
        when(projection.getLastUpdatedDate()).thenReturn("2026-10-29T10:00:00.000+0100");
        when(projection.getOwner()).thenReturn("own01");
        when(projection.getSerieId()).thenReturn("s1234");
        when(projection.getSerieUri()).thenReturn("https://example.fr/serie/s1234");
        when(projection.getSerieLabel()).thenReturn("Serie label");
        when(projection.getSerieAltLabel()).thenReturn("Serie alt label");

        // When
        QuestionnaireSearchResultDTO result = questionnaireSearchResultMapper.toDto(projection);

        // Then
        assertThat(result).isEqualTo(new QuestionnaireSearchResultDTO(
                "q-id",
                "My questionnaire",
                "2026-10-29T10:00:00.000+0100",
                "own01",
                new SerieDto("s1234", "https://example.fr/serie/s1234", "Serie label", "Serie alt label")
        ));
    }

    @Test
    @DisplayName("Should map a projection without serie to a DTO with a null serie")
    void should_map_projection_without_serie_to_dto() {
        // Given a projection without serie
        SearchResultProjection projection = mock(SearchResultProjection.class);
        when(projection.getId()).thenReturn("q-id");
        when(projection.getLabel()).thenReturn("My questionnaire");
        when(projection.getLastUpdatedDate()).thenReturn("2026-10-29T10:00:00.000+0100");
        when(projection.getOwner()).thenReturn("own01");

        // When
        QuestionnaireSearchResultDTO result = questionnaireSearchResultMapper.toDto(projection);

        // Then
        assertThat(result.id()).isEqualTo("q-id");
        assertThat(result.serie()).isNull();
    }
}
