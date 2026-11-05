package fr.insee.pogues.utils.model;

import fr.insee.pogues.model.*;

import java.math.BigInteger;
import java.util.List;

import static fr.insee.pogues.utils.model.PoguesModelUtils.FAKE_LAST_ELEMENT_ID;

/** Helper class to initialize the Pogues-Model of a new questionnaire. */
public class QuestionnaireInit {

    /** Name (and label) of the artificial end sequence. */
    public static final String END_SEQUENCE_NAME = "QUESTIONNAIRE_END";

    private QuestionnaireInit() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Initialize the model of a new questionnaire, with the values that are fixed for every new questionnaire,
     * its technical end sequence and its empty lists.
     * Target modes must be provided for creating the fake end sequence.
     * @param targetModes Target modes of the questionnaire.
     * @return A new Questionnaire object.
     */
    public static Questionnaire initQuestionnaire(List<SurveyModeEnum> targetModes) {
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setGenericName(GenericNameEnum.QUESTIONNAIRE);
        // We still handle `REDIRECTION` in model, but now we only create questionnaires with `FILTER`
        questionnaire.setFlowLogic(FlowLogicEnum.FILTER);
        // We still handle `XPATH` in model, but now we only create questionnaires with `VTL`
        questionnaire.setFormulasLanguage(FormulasLanguageEnum.VTL);
        questionnaire.setFinal(false);
        questionnaire.getTargetMode().addAll(targetModes);
        questionnaire.getChild().add(buildEndSequence(targetModes));
        initEmptyLists(questionnaire);
        return questionnaire;
    }

    /**
     * Build the technical end sequence, always added at the end of a questionnaire.
     * @param targetModes Target modes of the questionnaire.
     * @return The end sequence.
     */
    private static SequenceType buildEndSequence(List<SurveyModeEnum> targetModes) {
        SequenceType endSequence = new SequenceType();
        endSequence.setId(FAKE_LAST_ELEMENT_ID);
        endSequence.setName(END_SEQUENCE_NAME);
        endSequence.getLabel().add(END_SEQUENCE_NAME);
        endSequence.setDepth(BigInteger.ONE);
        endSequence.setGenericName(GenericNameEnum.MODULE);
        endSequence.getTargetMode().addAll(targetModes);
        endSequence.getDeclaration();
        endSequence.getControl();
        endSequence.getFlowControl();
        endSequence.getChild();
        return endSequence;
    }

    /**
     * Lists of the Pogues model are lazily initialized by their getters: an uninitialized list is not serialized,
     * and the serialization fails on a "CodeLists" object without "CodeList" array.
     * Calling the getters ensures empty arrays are serialized.
     * @param questionnaire A Questionnaire object.
     */
    private static void initEmptyLists(Questionnaire questionnaire) {
        CodeLists codeLists = new CodeLists();
        codeLists.getCodeList();
        questionnaire.setCodeLists(codeLists);
        Questionnaire.Variables variables = new Questionnaire.Variables();
        variables.getVariable();
        questionnaire.setVariables(variables);
        Questionnaire.Iterations iterations = new Questionnaire.Iterations();
        iterations.getIteration();
        questionnaire.setIterations(iterations);
        questionnaire.getChildQuestionnaireRef();
        questionnaire.getControl();
        questionnaire.getFlowControl();
        questionnaire.getComponentGroup();
    }
}
