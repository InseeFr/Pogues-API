package fr.insee.pogues.controller;

import fr.insee.pogues.configuration.properties.ApplicationProperties;
import fr.insee.pogues.exception.PoguesException;
import fr.insee.pogues.model.SurveyModeEnum;
import fr.insee.pogues.model.dto.details.QuestionnaireDetailsDto;
import fr.insee.pogues.service.QuestionnaireDetailsService;
import fr.insee.pogues.service.stub.QuestionnaireServiceStub;
import fr.insee.pogues.service.validation.ValidationResult;
import fr.insee.pogues.service.validation.steps.QuestionnaireDDIAgencyCheck;
import fr.insee.pogues.service.validation.steps.QuestionnaireDataCollectionCheck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuestionnaireDetailsControllerTest {

    private QuestionnaireDetailsController questionnaireDetailsController;
    private QuestionnaireServiceStub questionnaireServiceStub;

    @BeforeEach
    void beforeEach() {
        ApplicationProperties fooProperties = new ApplicationProperties("localhost", "http", null, null, null, "Pogues", "latest",null);
        questionnaireServiceStub = new QuestionnaireServiceStub();
        QuestionnaireDDIAgencyCheck ddiAgencyCheck = mock(QuestionnaireDDIAgencyCheck.class);
        QuestionnaireDataCollectionCheck dataCollectionCheck = mock(QuestionnaireDataCollectionCheck.class);
        when(ddiAgencyCheck.validateAgency(any())).thenReturn(ValidationResult.valid());
        when(dataCollectionCheck.validateDataCollection(any())).thenReturn(ValidationResult.valid());
        QuestionnaireDetailsService questionnaireDetailsService = new QuestionnaireDetailsService(
                questionnaireServiceStub, null, ddiAgencyCheck, dataCollectionCheck);
        questionnaireDetailsController = new QuestionnaireDetailsController(questionnaireDetailsService, fooProperties);
    }

    private static QuestionnaireDetailsDto buildDetails(String id) {
        return new QuestionnaireDetailsDto(
                id, "ENQLOG2026", "Enquête logement 2026", null,
                List.of(SurveyModeEnum.CAWI, SurveyModeEnum.CAPI), "fr.insee", "DR59-SNDI59");
    }

    @Test
    void testCreateQuestionnaireFromDetails() throws Exception {
        ResponseEntity<QuestionnaireDetailsDto> responseEntity =
                questionnaireDetailsController.createQuestionnaireFromDetails(buildDetails("m5noru0h"));

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertEquals(
                List.of("http://localhost/api/persistence/questionnaire/m5noru0h"),
                responseEntity.getHeaders().get("Location"));
        QuestionnaireDetailsDto body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals("m5noru0h", body.id());
        assertEquals("ENQLOG2026", body.name());
        assertNull(body.dataCollection());
        assertEquals("DR59-SNDI59", body.owner());

        // persisted Pogues model
        assertEquals(1, questionnaireServiceStub.getGetCreateQuestionnaireCalls());
        JsonNode persisted = questionnaireServiceStub.getQuestionnaireByID("m5noru0h");
        assertEquals("QUESTIONNAIRE", persisted.get("genericName").asString());
        assertEquals("FILTER", persisted.get("flowLogic").asString());
        assertEquals("VTL", persisted.get("formulasLanguage").asString());
        assertTrue(persisted.get("CodeLists").get("CodeList").isEmpty());
        assertTrue(persisted.get("Variables").get("Variable").isEmpty());
        assertTrue(persisted.get("childQuestionnaireRef").isEmpty());
        assertTrue(persisted.get("FlowControl").isEmpty());
        assertTrue(persisted.get("ComponentGroup").isEmpty());
        assertEquals(1, persisted.get("Child").size());
        JsonNode endSequence = persisted.get("Child").get(0);
        assertEquals("idendquest", endSequence.get("id").asString());
        assertEquals("QUESTIONNAIRE_END", endSequence.get("Name").asString());
        assertEquals("SequenceType", endSequence.get("type").asString());
        assertEquals(1, endSequence.get("depth").asInt());
        assertEquals("MODULE", endSequence.get("genericName").asString());
    }

    @Test
    void testCreateQuestionnaireFromDetailsConflict() throws Exception {
        questionnaireDetailsController.createQuestionnaireFromDetails(buildDetails("m5noru0h"));

        PoguesException exception = assertThrows(PoguesException.class,
                () -> questionnaireDetailsController.createQuestionnaireFromDetails(buildDetails("m5noru0h")));
        assertEquals(409, exception.getStatus());
    }

}
