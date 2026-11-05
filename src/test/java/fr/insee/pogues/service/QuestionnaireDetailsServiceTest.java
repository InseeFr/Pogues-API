package fr.insee.pogues.service;

import fr.insee.pogues.exception.details.QuestionnaireDetailsInvalidException;
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
import java.util.concurrent.atomic.AtomicReference;

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
        assertEquals(questionnaire.getDataCollection(), dto.dataCollection());
        assertEquals(List.of(SurveyModeEnum.CAPI, SurveyModeEnum.CAWI), dto.targetMode());
        assertEquals("fr.insee", dto.agency());
        assertEquals("owner", dto.owner());

        verify(questionnaireService).getQuestionnaireModelByID("id");
    }

    @Test
    @DisplayName("Should update questionnaire details without changing flow logic and formulas language")
    void updateQuestionnaireDetailsById_success() throws Exception {

        Questionnaire questionnaire = buildQuestionnaire();
        questionnaire.setFlowLogic(FlowLogicEnum.REDIRECTION);
        questionnaire.setFormulasLanguage(FormulasLanguageEnum.XPATH);

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
    @DisplayName("Should not update questionnaire details when serie is invalid")
    void updateQuestionnaireDetailsById_failed() throws Exception {

        when(questionnaireDataCollectionCheck.validateDataCollection(any()))
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
                newDataCollection,
                List.of(SurveyModeEnum.PAPI),
                "new.agency",
                "new.owner"
        );

        QuestionnaireDetailsInvalidException ex = assertThrows(QuestionnaireDetailsInvalidException.class,
                () -> questionnaireDetailsService.updateQuestionnaireDetailsById("id", dto));

        assertEquals("Serie of id:\"s2\" doesn't exist in metadata repository", ex.getMessage());
        verifyNoInteractions(questionnaireDDIAgencyCheck);

        verifyNoInteractions(questionnaireService);
    }

    @Test
    @DisplayName("Should create questionnaire from details")
    void createQuestionnaireFromDetails_success() throws Exception {

        when(questionnaireDataCollectionCheck.validateDataCollection(any())).thenReturn(ValidationResult.valid());
        when(questionnaireDDIAgencyCheck.validateAgency(any())).thenReturn(ValidationResult.valid());
        AtomicReference<Questionnaire> saved = new AtomicReference<>();
        doAnswer(invocation -> {
            saved.set(invocation.getArgument(0));
            return null;
        }).when(questionnaireService).createQuestionnaire(any(Questionnaire.class));
        when(questionnaireService.getQuestionnaireModelByID("m5noru0h"))
                .thenAnswer(invocation -> saved.get());

        DataCollection dataCollection = new DataCollection();
        Serie serie = new Serie();
        serie.setId("s1");
        dataCollection.setSerie(serie);

        QuestionnaireDetailsDto dto = new QuestionnaireDetailsDto(
                "m5noru0h",
                "enqlog2026",
                "Enquête logement 2026",
                dataCollection,
                List.of(SurveyModeEnum.CAWI, SurveyModeEnum.CAPI),
                "fr.insee",
                "DR59-SNDI59"
        );

        QuestionnaireDetailsDto created = questionnaireDetailsService.createQuestionnaireFromDetails(dto);

        Questionnaire questionnaire = saved.get();
        assertEquals("m5noru0h", questionnaire.getId());
        assertEquals("enqlog2026", questionnaire.getName());
        assertEquals(List.of("Enquête logement 2026"), questionnaire.getLabel());
        assertEquals(List.of(SurveyModeEnum.CAWI, SurveyModeEnum.CAPI), questionnaire.getTargetMode());
        assertEquals(FlowLogicEnum.FILTER, questionnaire.getFlowLogic());
        assertEquals(FormulasLanguageEnum.VTL, questionnaire.getFormulasLanguage());
        assertSame(dataCollection, questionnaire.getDataCollection());
        assertEquals("fr.insee", questionnaire.getAgency());
        assertEquals("DR59-SNDI59", questionnaire.getOwner());
        assertEquals(GenericNameEnum.QUESTIONNAIRE, questionnaire.getGenericName());
        assertThat(questionnaire.getCodeLists().getCodeList()).isEmpty();
        assertThat(questionnaire.getVariables().getVariable()).isEmpty();

        assertEquals(1, questionnaire.getChild().size());
        SequenceType endSequence = (SequenceType) questionnaire.getChild().getFirst();
        assertEquals("idendquest", endSequence.getId());
        assertEquals("QUESTIONNAIRE_END", endSequence.getName());
        assertEquals(List.of("QUESTIONNAIRE_END"), endSequence.getLabel());
        assertEquals(1, endSequence.getDepth().intValue());
        assertEquals(GenericNameEnum.MODULE, endSequence.getGenericName());
        assertEquals(List.of(SurveyModeEnum.CAWI, SurveyModeEnum.CAPI), endSequence.getTargetMode());

        assertEquals("m5noru0h", created.id());
    }

    @Test
    @DisplayName("Should not create questionnaire when agency is invalid")
    void createQuestionnaireFromDetails_failed() {

        when(questionnaireDataCollectionCheck.validateDataCollection(any())).thenReturn(ValidationResult.valid());
        when(questionnaireDDIAgencyCheck.validateAgency(any()))
                .thenReturn(ValidationResult.invalid("DDI Agency \"new.agency\" doesn't exist"));

        QuestionnaireDetailsDto dto = new QuestionnaireDetailsDto(
                "id", "NAME", "label", null,
                List.of(SurveyModeEnum.CAWI), "new.agency", "owner"
        );

        QuestionnaireDetailsInvalidException ex = assertThrows(QuestionnaireDetailsInvalidException.class,
                () -> questionnaireDetailsService.createQuestionnaireFromDetails(dto));

        assertEquals("DDI Agency \"new.agency\" doesn't exist", ex.getMessage());
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