package fr.insee.pogues.controller;

import fr.insee.pogues.configuration.properties.ApplicationProperties;
import fr.insee.pogues.exception.validation.QuestionnaireIdentifierException;
import fr.insee.pogues.model.dto.metadata.SerieDto;
import fr.insee.pogues.model.dto.questionnaires.QuestionnaireSearchResultDTO;
import fr.insee.pogues.service.QuestionnaireSearchService;
import fr.insee.pogues.service.validation.ModelValidationService;
import fr.insee.pogues.service.stub.QuestionnaireServiceStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuestionnaireControllerTest {

    private QuestionnaireController questionnaireController;
    private QuestionnaireServiceStub questionnaireServiceStub;
    private QuestionnaireSearchService questionnaireSearchService;

    @BeforeEach
    void beforeEach() {
        ApplicationProperties fooProperties = new ApplicationProperties("localhost", "http", null, null, null, "Pogues", "latest",null);
        questionnaireServiceStub = new QuestionnaireServiceStub();
        questionnaireSearchService = mock(QuestionnaireSearchService.class);
        questionnaireController = new QuestionnaireController(
                fooProperties, questionnaireServiceStub, null, null, null, null,
                new ModelValidationService(null, null), questionnaireSearchService);
    }

    @Test
    void testCreateBadIdQuestionnaire() {
        // Given
        // a questionnaire with a valid id
        ObjectNode fakeQuestionnaire = JsonNodeFactory.instance.objectNode();
        fakeQuestionnaire.put("id","bad-id"); // '-' is not allowed in id

        // When
        // calling the "create questionnaire" controller
        // test if exception is thrown
        QuestionnaireIdentifierException identifierException = assertThrows(
                QuestionnaireIdentifierException.class,
                () -> questionnaireController.createQuestionnaire(fakeQuestionnaire));

        // Then
        assertEquals("Invalid questionnaire identifier: bad-id", identifierException.getMessage());

        // test if the method of create questionnaire is never called
        assertEquals(0, questionnaireServiceStub.getGetCreateQuestionnaireCalls());
    }

    @Test
    void testCreateGoodIdQuestionnaire() {
        // Given
        // a questionnaire with a valid id
        ObjectNode fakeQuestionnaire = JsonNodeFactory.instance.objectNode();
        String goodId = "foo12345";
        fakeQuestionnaire.put("id", goodId);
        // When
        // calling the "create questionnaire" controller
        // test that no exception is thrown
        ResponseEntity<Object> responseEntity = assertDoesNotThrow(() ->
                questionnaireController.createQuestionnaire(fakeQuestionnaire));

        // Then
        // test if the method of create questionnaire is called
        assertEquals(1, questionnaireServiceStub.getGetCreateQuestionnaireCalls());
        // test content of response
        List<String> location = responseEntity.getHeaders().get("Location");
        assertNotNull(location);
        assertEquals(1, location.size());
        assertEquals("http://localhost/api/persistence/questionnaire/foo12345", location.getFirst());
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    }

    @Test
    void testSearchQuestionnairesByOwnerAndSerie() {
        // Given
        // two questionnaires, only the first one matching the owner and the serie id
        QuestionnaireSearchResultDTO q1 = new QuestionnaireSearchResultDTO("q1", "Questionnaire 1", "2026-10-29T10:00:00.000+0100", "own01",
                new SerieDto("s1234", "https://example.fr/serie/s1234", "Serie label", "Serie alt label"));
        when(questionnaireSearchService.searchQuestionnaires("own01", "s1234")).thenReturn(List.of(q1));

        // When
        // calling the "search questionnaires" controller with filters
        ResponseEntity<List<QuestionnaireSearchResultDTO>> responseEntity =
                questionnaireController.searchQuestionnairesByOwnerAndSerie("own01", "s1234");

        // Then
        // only the matching questionnaire is returned
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(List.of(q1), responseEntity.getBody());
    }

    @Test
    void testSearchQuestionnairesWithoutFilter() {
        // Given
        // two questionnaires, the second one without serie
        QuestionnaireSearchResultDTO q1 = new QuestionnaireSearchResultDTO("q1", "Questionnaire 1", "2026-10-29T10:00:00.000+0100", "own01",
                new SerieDto("s1234", "https://example.fr/serie/s1234", "Serie label", "Serie alt label"));
        QuestionnaireSearchResultDTO q2 = new QuestionnaireSearchResultDTO("q2", "Questionnaire 2", "2026-10-29T10:00:00.000+0100", "own02", null);
        when(questionnaireSearchService.searchQuestionnaires(null, null)).thenReturn(List.of(q1, q2));

        // When
        // calling the "search questionnaires" controller without filter
        ResponseEntity<List<QuestionnaireSearchResultDTO>> responseEntity =
                questionnaireController.searchQuestionnairesByOwnerAndSerie(null, null);

        // Then
        // both questionnaires are returned
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(List.of(q1, q2), responseEntity.getBody());
    }

}
