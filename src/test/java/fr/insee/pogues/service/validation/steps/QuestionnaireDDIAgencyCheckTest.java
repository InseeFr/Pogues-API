package fr.insee.pogues.service.validation.steps;

import fr.insee.pogues.model.Questionnaire;
import fr.insee.pogues.service.metadata.MetadataService;
import fr.insee.pogues.service.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class QuestionnaireDDIAgencyCheckTest {

    @Mock
    private MetadataService metadataService;

    private QuestionnaireDDIAgencyCheck ddiAgencyCheck;

    @BeforeEach
    void init() {
        ddiAgencyCheck =
                new QuestionnaireDDIAgencyCheck(metadataService);
    }

    @Test
    @DisplayName("Valid DDI agency")
    void validDataCollection() {
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setAgency("fr.insee");

        when(metadataService.existAgencyById("fr.insee"))
                .thenReturn(true);

        ValidationResult validationResult = ddiAgencyCheck.validate(questionnaire);
        assertTrue(validationResult.isValid());
        assertNull(validationResult.errorMessage());
    }

    @Test
    @DisplayName("InValid questionnaire Data Collection")
    void inValidDataCollection() {
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setAgency("fr.insee");

        when(metadataService.existAgencyById("fr.insee"))
                .thenReturn(false);

        ValidationResult validationResult = ddiAgencyCheck.validate(questionnaire);
        assertFalse(validationResult.isValid());
        assertEquals("DDI Agency \"fr.insee\" doesn't exist", validationResult.errorMessage());
    }
}
