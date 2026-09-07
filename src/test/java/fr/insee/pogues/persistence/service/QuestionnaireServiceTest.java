package fr.insee.pogues.persistence.service;

import fr.insee.pogues.domain.entity.db.QuestionnaireEntity;
import fr.insee.pogues.exception.PoguesException;
import fr.insee.pogues.exception.questionnaire.QuestionnaireNotFoundException;
import fr.insee.pogues.persistence.repository.QuestionnaireRepository;
import fr.insee.pogues.persistence.repository.jpa.QuestionnaireJpaRepository;
import fr.insee.pogues.service.modelcleaning.ModelCleaningService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionnaireServiceTest {

    @Mock
    QuestionnaireRepository questionnairesServiceQuery;

    @Mock
    QuestionnaireJpaRepository questionnaireJpaRepository;

    @InjectMocks
    QuestionnaireService questionnaireService;

    @Mock
    ModelCleaningService modelCleaningService;

    @Mock
    VersionService versionService;

    @Test
    void getQuestionnaireByOwnerWithNullException() {
        Throwable exception = assertThrows(PoguesException.class,()->questionnaireService.getQuestionnairesByOwner(null));
        assertEquals("Bad Request",exception.getMessage());
    }
    
    @Test
    void getQuestionnaireByOwnerWithEmptyException() {
        Throwable exception = assertThrows(PoguesException.class,()->questionnaireService.getQuestionnairesByOwner(""));
        assertEquals("Bad Request",exception.getMessage());
    }


    @Test
    void questionnaireNotFoundThrowsException() {
        assertThrows(QuestionnaireNotFoundException.class, ()->questionnaireService.getQuestionnaireByID("id"));
    }

    @Test
    void getQuestionnaireById() throws Exception {
        ObjectNode q1 = JsonNodeFactory.instance.objectNode();
        q1.put("id", "foo");
        q1.putArray("Control");
        q1.putArray("Child");
        when(questionnaireJpaRepository.findById("foo"))
                .thenReturn(
                        Optional.of(new QuestionnaireEntity("foo", q1.toPrettyString())));
        when(modelCleaningService.cleanModel(q1)).thenReturn(q1);
        JsonNode q2 = questionnaireService.getQuestionnaireByID("foo");
        assertEquals(q1, q2);
        assertEquals("foo", q2.get("id").asText());

    }

    @Test
    void deleteExceptionPropagate() throws Exception {
        doThrow(new SQLException("Test: Exception should propagate"))
                .when(questionnairesServiceQuery)
                .deleteQuestionnaireByID("1");
        Throwable exception = assertThrows(SQLException.class,()->questionnaireService.deleteQuestionnaireByID("1"));
        assertEquals("Test: Exception should propagate",exception.getMessage());

    }

    @Test
    void deleteQuestionnaireById() throws Exception {
        doAnswer(invocationOnMock -> null).when(questionnairesServiceQuery).deleteQuestionnaireByID("foo");
        doAnswer(invocationOnMock -> null).when(versionService).deleteAllVersionsByQuestionnaireIdExceptLast("foo");
        questionnaireService.deleteQuestionnaireByID("foo");
    }
}

