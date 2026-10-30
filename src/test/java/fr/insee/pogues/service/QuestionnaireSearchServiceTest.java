package fr.insee.pogues.service;

import fr.insee.pogues.mapper.QuestionnaireSearchResultMapper;
import fr.insee.pogues.model.dto.metadata.SerieDto;
import fr.insee.pogues.model.dto.questionnaires.QuestionnaireSearchResultDTO;
import fr.insee.pogues.persistence.repository.jpa.QuestionnaireJpaRepository;
import fr.insee.pogues.persistence.repository.jpa.QuestionnaireJpaRepository.SearchResultProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuestionnaireSearchServiceTest {

    private static final String OWNER_A = "own01";
    private static final String OWNER_B = "own02";
    private static final String SERIE_1 = "s1234";
    private static final String SERIE_2 = "s5678";

    /**
     * Questionnaires in database:
     * <ul>
     *     <li>q1: owner A, serie 1</li>
     *     <li>q2: owner A, serie 2</li>
     *     <li>q3: owner A, no serie</li>
     *     <li>q4: owner B, serie 1</li>
     *     <li>q5: owner B, no serie</li>
     * </ul>
     */
    private static final List<SearchResultProjection> QUESTIONNAIRES = List.of(
            projection("q1", OWNER_A, SERIE_1),
            projection("q2", OWNER_A, SERIE_2),
            projection("q3", OWNER_A, null),
            projection("q4", OWNER_B, SERIE_1),
            projection("q5", OWNER_B, null)
    );

    private QuestionnaireSearchService questionnaireSearchService;

    @BeforeEach
    void setUp() {
        QuestionnaireJpaRepository questionnaireJpaRepository = mock(QuestionnaireJpaRepository.class);
        // Same filtering as the query: a null parameter is not used as a filter
        when(questionnaireJpaRepository.searchQuestionnaires(any(), any())).thenAnswer(invocation -> {
            String owner = invocation.getArgument(0);
            String serieId = invocation.getArgument(1);
            return QUESTIONNAIRES.stream()
                    .filter(q -> owner == null || owner.equals(q.getOwner()))
                    .filter(q -> serieId == null || serieId.equals(q.getSerieId()))
                    .toList();
        });
        questionnaireSearchService = new QuestionnaireSearchService(questionnaireJpaRepository, new QuestionnaireSearchResultMapper());
    }

    @Test
    @DisplayName("Should return all questionnaires when there is no filter")
    void should_return_all_questionnaires_without_filter() {
        // When
        List<QuestionnaireSearchResultDTO> result = questionnaireSearchService.searchQuestionnaires(null, null);

        // Then
        assertThat(result).extracting(QuestionnaireSearchResultDTO::id)
                .containsExactly("q1", "q2", "q3", "q4", "q5");
    }

    @Test
    @DisplayName("Should search on empty strings when filters are empty")
    void should_search_on_empty_filters() {
        // When
        List<QuestionnaireSearchResultDTO> result = questionnaireSearchService.searchQuestionnaires("", "");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should search questionnaires by owner only")
    void should_search_questionnaires_by_owner() {
        // When
        List<QuestionnaireSearchResultDTO> resultA = questionnaireSearchService.searchQuestionnaires(OWNER_A, null);
        List<QuestionnaireSearchResultDTO> resultB = questionnaireSearchService.searchQuestionnaires(OWNER_B, null);

        // Then
        assertThat(resultA).extracting(QuestionnaireSearchResultDTO::id).containsExactly("q1", "q2", "q3");
        assertThat(resultB).extracting(QuestionnaireSearchResultDTO::id).containsExactly("q4", "q5");
    }

    @Test
    @DisplayName("Should search questionnaires by serie only")
    void should_search_questionnaires_by_serie() {
        // When
        List<QuestionnaireSearchResultDTO> result1 = questionnaireSearchService.searchQuestionnaires(null, SERIE_1);
        List<QuestionnaireSearchResultDTO> result2 = questionnaireSearchService.searchQuestionnaires(null, SERIE_2);

        // Then
        assertThat(result1).extracting(QuestionnaireSearchResultDTO::id).containsExactly("q1", "q4");
        assertThat(result2).extracting(QuestionnaireSearchResultDTO::id).containsExactly("q2");
    }

    @Test
    @DisplayName("Should search questionnaires by owner and serie")
    void should_search_questionnaires_by_owner_and_serie() {
        // When
        List<QuestionnaireSearchResultDTO> resultA1 = questionnaireSearchService.searchQuestionnaires(OWNER_A, SERIE_1);
        List<QuestionnaireSearchResultDTO> resultA2 = questionnaireSearchService.searchQuestionnaires(OWNER_A, SERIE_2);
        List<QuestionnaireSearchResultDTO> resultB1 = questionnaireSearchService.searchQuestionnaires(OWNER_B, SERIE_1);
        List<QuestionnaireSearchResultDTO> resultB2 = questionnaireSearchService.searchQuestionnaires(OWNER_B, SERIE_2);

        // Then
        assertThat(resultA1).extracting(QuestionnaireSearchResultDTO::id).containsExactly("q1");
        assertThat(resultA2).extracting(QuestionnaireSearchResultDTO::id).containsExactly("q2");
        assertThat(resultB1).extracting(QuestionnaireSearchResultDTO::id).containsExactly("q4");
        assertThat(resultB2).isEmpty();
    }

    @Test
    @DisplayName("Should return nothing when no questionnaire matches")
    void should_return_nothing_when_no_match() {
        // When
        List<QuestionnaireSearchResultDTO> unknownOwner = questionnaireSearchService.searchQuestionnaires("UNKNOWN", null);
        List<QuestionnaireSearchResultDTO> unknownSerie = questionnaireSearchService.searchQuestionnaires(null, "s0000");

        // Then
        assertThat(unknownOwner).isEmpty();
        assertThat(unknownSerie).isEmpty();
    }

    @Test
    @DisplayName("Should map the found questionnaires to DTO")
    void should_map_found_questionnaires() {
        // When
        List<QuestionnaireSearchResultDTO> result = questionnaireSearchService.searchQuestionnaires(OWNER_A, null);

        // Then
        assertThat(result).containsExactly(
                new QuestionnaireSearchResultDTO("q1", "Label q1", "2026-10-29T10:00:00.000+0100", OWNER_A,
                        new SerieDto(SERIE_1, "https://example.fr/serie/" + SERIE_1, "Label " + SERIE_1, "Alt label " + SERIE_1)),
                new QuestionnaireSearchResultDTO("q2", "Label q2", "2026-10-29T10:00:00.000+0100", OWNER_A,
                        new SerieDto(SERIE_2, "https://example.fr/serie/" + SERIE_2, "Label " + SERIE_2, "Alt label " + SERIE_2)),
                new QuestionnaireSearchResultDTO("q3", "Label q3", "2026-10-29T10:00:00.000+0100", OWNER_A,null)
        );
    }

    private static SearchResultProjection projection(String id, String owner, String serieId) {
        SearchResultProjection projection = mock(SearchResultProjection.class);
        when(projection.getId()).thenReturn(id);
        when(projection.getLabel()).thenReturn("Label " + id);
        when(projection.getLastUpdatedDate()).thenReturn("2026-10-29T10:00:00.000+0100");
        when(projection.getOwner()).thenReturn(owner);
        if (serieId != null) {
            when(projection.getSerieId()).thenReturn(serieId);
            when(projection.getSerieUri()).thenReturn("https://example.fr/serie/" + serieId);
            when(projection.getSerieLabel()).thenReturn("Label " + serieId);
            when(projection.getSerieAltLabel()).thenReturn("Alt label " + serieId);
        }
        return projection;
    }
}
