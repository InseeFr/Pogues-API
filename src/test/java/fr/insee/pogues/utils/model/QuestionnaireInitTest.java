package fr.insee.pogues.utils.model;

import fr.insee.pogues.model.*;
import fr.insee.pogues.utils.PoguesSerializer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.math.BigInteger;
import java.util.List;

import static fr.insee.pogues.utils.json.JSONFunctions.jsonStringtoJsonNode;
import static fr.insee.pogues.utils.model.PoguesModelUtils.FAKE_LAST_ELEMENT_ID;
import static fr.insee.pogues.utils.model.QuestionnaireInit.END_SEQUENCE_NAME;
import static org.junit.jupiter.api.Assertions.*;

class QuestionnaireInitTest {

    private static final List<SurveyModeEnum> TARGET_MODES = List.of(SurveyModeEnum.CAWI, SurveyModeEnum.CAPI);

    @Test
    void initQuestionnaire_fixedValues() {
        Questionnaire questionnaire = QuestionnaireInit.initQuestionnaire(TARGET_MODES);

        assertEquals(GenericNameEnum.QUESTIONNAIRE, questionnaire.getGenericName());
        assertEquals(FlowLogicEnum.FILTER, questionnaire.getFlowLogic());
        assertEquals(FormulasLanguageEnum.VTL, questionnaire.getFormulasLanguage());
        assertFalse(questionnaire.isFinal());
        assertEquals(TARGET_MODES, questionnaire.getTargetMode());
    }

    @Test
    void initQuestionnaire_endSequence() {
        Questionnaire questionnaire = QuestionnaireInit.initQuestionnaire(TARGET_MODES);

        // The end sequence is the only component of the questionnaire
        assertEquals(1, questionnaire.getChild().size());
        SequenceType endSequence = assertInstanceOf(SequenceType.class, questionnaire.getChild().getFirst());
        assertEquals(FAKE_LAST_ELEMENT_ID, endSequence.getId());
        assertEquals(END_SEQUENCE_NAME, endSequence.getName());
        assertEquals(List.of(END_SEQUENCE_NAME), endSequence.getLabel());
        assertEquals(BigInteger.ONE, endSequence.getDepth());
        assertEquals(GenericNameEnum.MODULE, endSequence.getGenericName());
        assertEquals(TARGET_MODES, endSequence.getTargetMode());
        assertTrue(endSequence.getChild().isEmpty());
    }

    @Test
    void initQuestionnaire_emptyLists() {
        Questionnaire questionnaire = QuestionnaireInit.initQuestionnaire(TARGET_MODES);

        assertNotNull(questionnaire.getCodeLists());
        assertTrue(questionnaire.getCodeLists().getCodeList().isEmpty());
        assertNotNull(questionnaire.getVariables());
        assertTrue(questionnaire.getVariables().getVariable().isEmpty());
        assertNotNull(questionnaire.getIterations());
        assertTrue(questionnaire.getIterations().getIteration().isEmpty());
        assertTrue(questionnaire.getChildQuestionnaireRef().isEmpty());
        assertTrue(questionnaire.getControl().isEmpty());
        assertTrue(questionnaire.getFlowControl().isEmpty());
        assertTrue(questionnaire.getComponentGroup().isEmpty());
    }

    @Test
    void initQuestionnaire_emptyListsAreSerialized() throws Exception {
        Questionnaire questionnaire = QuestionnaireInit.initQuestionnaire(TARGET_MODES);
        // Required attribute in the model, set by the service in real use
        questionnaire.setId("q-id");

        JsonNode json = jsonStringtoJsonNode(PoguesSerializer.questionnaireJavaToString(questionnaire));

        assertTrue(json.get("CodeLists").get("CodeList").isArray());
        assertTrue(json.get("CodeLists").get("CodeList").isEmpty());
        assertTrue(json.get("Variables").get("Variable").isArray());
        assertTrue(json.get("Variables").get("Variable").isEmpty());
        assertTrue(json.get("Iterations").get("Iteration").isArray());
        assertTrue(json.get("Iterations").get("Iteration").isEmpty());
        assertTrue(json.get("Control").isArray());
        assertTrue(json.get("FlowControl").isArray());

        JsonNode endSequence = json.get("Child").get(0);
        assertEquals(FAKE_LAST_ELEMENT_ID, endSequence.get("id").asString());
        assertTrue(endSequence.get("Declaration").isArray());
        assertTrue(endSequence.get("Control").isArray());
        assertTrue(endSequence.get("FlowControl").isArray());
        assertTrue(endSequence.get("Child").isArray());
    }

    @Test
    void initQuestionnaire_returnsNewInstances() {
        Questionnaire questionnaire1 = QuestionnaireInit.initQuestionnaire(TARGET_MODES);
        Questionnaire questionnaire2 = QuestionnaireInit.initQuestionnaire(TARGET_MODES);

        assertNotSame(questionnaire1, questionnaire2);
        assertNotSame(questionnaire1.getChild().getFirst(), questionnaire2.getChild().getFirst());
    }
}
