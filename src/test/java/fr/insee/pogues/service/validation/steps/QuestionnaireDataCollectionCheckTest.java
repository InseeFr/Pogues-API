package fr.insee.pogues.service.validation.steps;

import fr.insee.pogues.model.DataCollection;
import fr.insee.pogues.model.Questionnaire;
import fr.insee.pogues.model.Serie;
import fr.insee.pogues.model.dto.metadata.SerieDto;
import fr.insee.pogues.service.metadata.MetadataService;
import fr.insee.pogues.service.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class QuestionnaireDataCollectionCheckTest {

    @Mock
    private MetadataService metadataService;

    private QuestionnaireDataCollectionCheck dataCollectionCheck;

    @BeforeEach
    void init() {
        dataCollectionCheck =
                new QuestionnaireDataCollectionCheck(metadataService);
    }

    @Test
    @DisplayName("Valid questionnaire Data Collection")
    void validDataCollection() {
        DataCollection dataCollection = new DataCollection();
        Serie serie = new Serie();
        serie.setId("valid-serie-id");
        dataCollection.setSerie(serie);
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setDataCollection(dataCollection);

        when(metadataService.getAllSeries())
                .thenReturn(List.of(new SerieDto("valid-serie-id", null,null, null,null)));

        ValidationResult validationResult = dataCollectionCheck.validate(questionnaire);
        assertTrue(validationResult.isValid());
        assertNull(validationResult.errorMessage());
    }

    @Test
    @DisplayName("InValid questionnaire Data Collection")
    void inValidDataCollection() {
        DataCollection dataCollection = new DataCollection();
        Serie serie = new Serie();
        serie.setId("invalid-serie-id");
        dataCollection.setSerie(serie);
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setDataCollection(dataCollection);

        when(metadataService.getAllSeries())
                .thenReturn(List.of(new SerieDto("valid-serie-id", null,null, null,null)));

        ValidationResult validationResult = dataCollectionCheck.validate(questionnaire);
        assertFalse(validationResult.isValid());
        assertEquals("Serie of id:\"invalid-serie-id\" doesn't exist in metadata repository", validationResult.errorMessage());
    }

    @Test
    @DisplayName("valid dataCollection when not defined")
    void validDataCollection_withNull() {
        Questionnaire noDataCollection = new Questionnaire();


        Questionnaire noSerie = new Questionnaire();
        DataCollection dataCollection = new DataCollection();
        noSerie.setDataCollection(dataCollection);

        when(metadataService.getAllSeries())
                .thenReturn(List.of(new SerieDto("valid-serie-id", null,null, null,null)));

        assertTrue(dataCollectionCheck.validate(noDataCollection).isValid());
        assertTrue(dataCollectionCheck.validate(noSerie).isValid());
    }
}
