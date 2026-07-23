package fr.insee.pogues.service;

import fr.insee.pogues.exception.validation.ModelValidationException;
import fr.insee.pogues.model.*;
import fr.insee.pogues.model.dto.details.QuestionnaireDetailsDto;
import fr.insee.pogues.persistence.service.IQuestionnaireService;
import fr.insee.pogues.persistence.service.VersionService;
import fr.insee.pogues.service.validation.ValidationResult;
import fr.insee.pogues.service.validation.steps.QuestionnaireDDIAgencyCheck;
import fr.insee.pogues.service.validation.steps.QuestionnaireDataCollectionCheck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionnaireDetailsServiceTest {

    @Mock
    private IQuestionnaireService questionnaireService;

    @Mock
    private VersionService versionService;

    @Mock
    private QuestionnaireDDIAgencyCheck questionnaireDDIAgencyCheck;

    @Mock
    private QuestionnaireDataCollectionCheck questionnaireDataCollectionCheck;


    private QuestionnaireDetailsService questionnaireDetailsService;

    @BeforeEach
    void init() {
        questionnaireDetailsService =
                new QuestionnaireDetailsService(
                        questionnaireService, versionService,
                        questionnaireDDIAgencyCheck, questionnaireDataCollectionCheck);
    }

    @Test
    @DisplayName("Should return questionnaire details by questionnaire id")
    void getQuestionnaireDetailsById_success() throws Exception {

        Questionnaire questionnaire = buildQuestionnaire();

        when(questionnaireService.getQuestionnaireModelByID("id"))
                .thenReturn(questionnaire);

        QuestionnaireDetailsDto dto =
                questionnaireDetailsService.getQuestionnaireDetailsById("id");

        assertEquals("id", dto.id());
        assertEquals("Questionnaire", dto.name());
        assertEquals("My label", dto.label());
        assertEquals(FlowLogicEnum.FILTER, dto.flowLogic());
        assertEquals(FormulasLanguageEnum.VTL, dto.formulasLanguage());
        assertEquals(questionnaire.getDataCollection(), dto.dataCollection());
        assertEquals(List.of(SurveyModeEnum.CAPI, SurveyModeEnum.CAWI), dto.targetMode());
        assertEquals("fr.insee", dto.agency());
        assertEquals("owner", dto.owner());

        verify(questionnaireService).getQuestionnaireModelByID("id");
    }

    @Test
    @DisplayName("Should update questionnaire details")
    void updateQuestionnaireDetailsById_success() throws Exception {

        Questionnaire questionnaire = buildQuestionnaire();

        when(questionnaireService.getQuestionnaireModelByID("id"))
                .thenReturn(questionnaire);

        when(questionnaireDataCollectionCheck.validateDataCollection(any())).thenReturn(ValidationResult.valid());
        when(questionnaireDDIAgencyCheck.validateAgency(any())).thenReturn(ValidationResult.valid());

        DataCollection newDataCollection = new DataCollection();
        Serie serie = new Serie();
        serie.setId("s2");
        serie.setUri("uri:new");
        newDataCollection.setSerie(serie);

        QuestionnaireDetailsDto dto = new QuestionnaireDetailsDto(
                "id",
                "New name",
                "New label",
                FlowLogicEnum.REDIRECTION,
                FormulasLanguageEnum.XPATH,
                newDataCollection,
                List.of(SurveyModeEnum.PAPI),
                "new.agency",
                "new.owner"
        );

        questionnaireDetailsService.updateQuestionnaireDetailsById("id", dto);

        assertEquals("New name", questionnaire.getName());
        assertEquals("New label", questionnaire.getLabel().getFirst());
        assertEquals(FlowLogicEnum.REDIRECTION, questionnaire.getFlowLogic());
        assertEquals(FormulasLanguageEnum.XPATH, questionnaire.getFormulasLanguage());
        assertSame(newDataCollection, questionnaire.getDataCollection());

        assertEquals(1, questionnaire.getTargetMode().size());
        assertEquals(SurveyModeEnum.PAPI, questionnaire.getTargetMode().getFirst());

        assertEquals("new.agency", questionnaire.getAgency());
        assertEquals("new.owner", questionnaire.getOwner());

        verify(questionnaireService).updateQuestionnaire("id", questionnaire);
    }

    @Test
    @DisplayName("Should not update questionnaire details when invalid")
    void updateQuestionnaireDetailsById_failed() throws Exception {

        when(questionnaireDataCollectionCheck.validateDataCollection(any()))
                .thenReturn(ValidationResult.invalid("DDI Agency \"new.agency\" doesn't exist"));
        when(questionnaireDDIAgencyCheck.validateAgency(any()))
                .thenReturn(ValidationResult.invalid("Serie of id:\"s2\" doesn't exist in metadata repository"));

        DataCollection newDataCollection = new DataCollection();
        Serie serie = new Serie();
        serie.setId("s2");
        serie.setUri("uri:new");
        newDataCollection.setSerie(serie);

        QuestionnaireDetailsDto dto = new QuestionnaireDetailsDto(
                "id",
                "New name",
                "New label",
                FlowLogicEnum.REDIRECTION,
                FormulasLanguageEnum.XPATH,
                newDataCollection,
                List.of(SurveyModeEnum.PAPI),
                "new.agency",
                "new.owner"
        );

        ModelValidationException ex = assertThrows(ModelValidationException.class,
                () -> questionnaireDetailsService.updateQuestionnaireDetailsById("id", dto));

        assertEquals("Questionnaire details invalid", ex.getMessage());
        assertThat(ex.getDetails()).hasSize(2);
        assertThat(ex.getDetails()).contains("DDI Agency \"new.agency\" doesn't exist");
        assertThat(ex.getDetails()).contains("Serie of id:\"s2\" doesn't exist in metadata repository");

        verifyNoInteractions(questionnaireService);
    }

    @Test
    @DisplayName("Should return questionnaire details by version id")
    void getQuestionnaireDetailsByVersionId_success() throws Exception {

        UUID versionId = UUID.randomUUID();

        Questionnaire questionnaire = buildQuestionnaire();

        when(versionService.getVersionDataQuestionnaireModelByVersionId(versionId))
                .thenReturn(questionnaire);

        QuestionnaireDetailsDto dto =
                questionnaireDetailsService.getQuestionnaireDetailsByVersionId(versionId);

        assertEquals("id", dto.id());
        assertEquals("Questionnaire", dto.name());
        assertEquals("My label", dto.label());
        assertEquals(FlowLogicEnum.FILTER, dto.flowLogic());
        assertEquals(FormulasLanguageEnum.VTL, dto.formulasLanguage());
        assertEquals("s1", dto.dataCollection().getSerie().getId());
        assertThat(dto.targetMode()).contains(SurveyModeEnum.CAPI, SurveyModeEnum.CAWI);
        assertEquals("fr.insee", dto.agency());
        assertEquals("owner", dto.owner());

        verify(versionService)
                .getVersionDataQuestionnaireModelByVersionId(versionId);
    }

    private Questionnaire buildQuestionnaire() {

        Questionnaire questionnaire = new Questionnaire();

        questionnaire.setId("id");
        questionnaire.setName("Questionnaire");

        questionnaire.getLabel().add("My label");

        questionnaire.setFlowLogic(FlowLogicEnum.FILTER);
        questionnaire.setFormulasLanguage(FormulasLanguageEnum.VTL);

        DataCollection dataCollection = new DataCollection();
        Serie serie = new Serie();
        serie.setUri("uri:uri");
        serie.setId("s1");
        dataCollection.setSerie(serie);

        questionnaire.setDataCollection(dataCollection);

        questionnaire.getTargetMode().addAll(List.of(SurveyModeEnum.CAPI, SurveyModeEnum.CAWI));

        questionnaire.setAgency("fr.insee");
        questionnaire.setOwner("owner");

        return questionnaire;
    }
}