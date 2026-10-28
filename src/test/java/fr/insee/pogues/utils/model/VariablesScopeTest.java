package fr.insee.pogues.utils.model;

import fr.insee.pogues.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VariablesScopeTest {

    private static SequenceType sequence(String id, GenericNameEnum genericName, ComponentType... children) {
        SequenceType sequence = new SequenceType();
        sequence.setId(id);
        sequence.setGenericName(genericName);
        sequence.getChild().addAll(List.of(children));
        return sequence;
    }

    /** Question collecting the variable {@code <id>_VAR}. */
    private static QuestionType question(String id) {
        QuestionType question = new QuestionType();
        question.setId(id);
        question.setQuestionType(QuestionTypeEnum.SIMPLE);
        ResponseType response = new ResponseType();
        response.setCollectedVariableReference(id + "_VAR");
        question.getResponse().add(response);
        return question;
    }

    private static QuestionType dynamicTable(String id) {
        QuestionType table = question(id);
        table.setQuestionType(QuestionTypeEnum.TABLE);
        DimensionType primary = new DimensionType();
        primary.setDimensionType(DimensionTypeEnum.PRIMARY);
        primary.setDynamic("DYNAMIC");
        table.setResponseStructure(new ResponseStructureType());
        table.getResponseStructure().getDimension().add(primary);
        return table;
    }

    private static DynamicIterationType loop(String id, String initialMember, String finalMember, String basedOn) {
        DynamicIterationType loop = new DynamicIterationType();
        loop.setId(id);
        loop.getMemberReference().addAll(List.of(initialMember, finalMember));
        loop.setIterableReference(basedOn);
        return loop;
    }

    private static CollectedVariableType collectedVariable(String id, String scope) {
        CollectedVariableType variable = new CollectedVariableType();
        variable.setId(id);
        variable.setScope(scope);
        return variable;
    }

    private static Questionnaire questionnaire(List<ComponentType> children, List<IterationType> loops, List<VariableType> variables) {
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.getChild().addAll(children);
        questionnaire.setIterations(new Questionnaire.Iterations());
        questionnaire.getIterations().getIteration().addAll(loops);
        questionnaire.setVariables(new Questionnaire.Variables());
        questionnaire.getVariables().getVariable().addAll(variables);
        return questionnaire;
    }

    private static String scopeOf(Questionnaire questionnaire, String variableId) {
        return questionnaire.getVariables().getVariable().stream()
                .filter(variable -> variableId.equals(variable.getId()))
                .findFirst().orElseThrow().getScope();
    }

    @Test
    @DisplayName("Should give the loop scope to the variables of the questions covered by the loop")
    void updateCollectedVariablesScope_loops() {
        // Given a loop over S1 and S2, a loop over SS1 to SS2 based on another scope, and a variable with an outdated scope
        Questionnaire questionnaire = questionnaire(
                List.of(
                        sequence("S1", GenericNameEnum.MODULE,
                                sequence("SS1", GenericNameEnum.SUBMODULE, question("Q1")),
                                question("Q2"),
                                sequence("SS2", GenericNameEnum.SUBMODULE, question("Q3"))),
                        sequence("S2", GenericNameEnum.MODULE, question("Q4")),
                        sequence("S3", GenericNameEnum.MODULE, question("Q5"))),
                // the inner loop is declared first, it must still prevail over the outer loop
                List.of(loop("INNER", "SS1", "SS2", "PAIRWISE"), loop("OUTER", "S1", "S2", null)),
                List.of(collectedVariable("Q1_VAR", null), collectedVariable("Q2_VAR", null),
                        collectedVariable("Q3_VAR", null), collectedVariable("Q4_VAR", null),
                        collectedVariable("Q5_VAR", "OLD_LOOP")));

        // When we compute the scopes
        VariablesScope.updateCollectedVariablesScope(questionnaire);

        // Then the variables have the scope of their innermost loop, or no scope outside any loop
        // (Q2 is between the subsequences, so it is covered by the inner loop)
        assertThat(scopeOf(questionnaire, "Q1_VAR")).isEqualTo("PAIRWISE");
        assertThat(scopeOf(questionnaire, "Q2_VAR")).isEqualTo("PAIRWISE");
        assertThat(scopeOf(questionnaire, "Q3_VAR")).isEqualTo("PAIRWISE");
        assertThat(scopeOf(questionnaire, "Q4_VAR")).isEqualTo("OUTER");
        assertThat(scopeOf(questionnaire, "Q5_VAR")).isNull();
    }

    @Test
    @DisplayName("Should give the scope of a loop based on another scope to the variables it covers")
    void updateCollectedVariablesScope_basedOn() {
        // Given a loop over SS1 based on a dynamic table, and a main loop over S2
        Questionnaire questionnaire = questionnaire(
                List.of(
                        sequence("S1", GenericNameEnum.MODULE,
                                sequence("SS1", GenericNameEnum.SUBMODULE, question("Q1")),
                                sequence("SS2", GenericNameEnum.SUBMODULE, question("Q2"))),
                        sequence("S2", GenericNameEnum.MODULE, question("Q3"))),
                List.of(loop("L1", "SS1", "SS1", "TABLE"), loop("L2", "S2", "S2", null)),
                List.of(collectedVariable("Q1_VAR", null), collectedVariable("Q2_VAR", null),
                        collectedVariable("Q3_VAR", null)));

        // When we compute the scopes
        VariablesScope.updateCollectedVariablesScope(questionnaire);

        // Then
        assertThat(scopeOf(questionnaire, "Q1_VAR")).isEqualTo("TABLE");
        assertThat(scopeOf(questionnaire, "Q2_VAR")).isNull();
        assertThat(scopeOf(questionnaire, "Q3_VAR")).isEqualTo("L2");
    }

    @Test
    @DisplayName("Should give the table scope to the variables of a dynamic table, even in a loop")
    void updateCollectedVariablesScope_dynamicTable() {
        // Given a dynamic table in a loop
        Questionnaire questionnaire = questionnaire(
                List.of(sequence("S1", GenericNameEnum.MODULE, dynamicTable("TABLE"), question("Q1"))),
                List.of(loop("L1", "S1", "S1", null)),
                List.of(collectedVariable("TABLE_VAR", null), collectedVariable("Q1_VAR", null)));

        // When we compute the scopes
        VariablesScope.updateCollectedVariablesScope(questionnaire);

        // Then
        assertThat(scopeOf(questionnaire, "TABLE_VAR")).isEqualTo("TABLE");
        assertThat(scopeOf(questionnaire, "Q1_VAR")).isEqualTo("L1");
    }

    @Test
    @DisplayName("Should give the roundabout scope to the variables it covers, and leave other variables unchanged")
    void updateCollectedVariablesScope_roundabout() {
        // Given a roundabout over S1, and a calculated variable with a scope chosen by the user
        RoundaboutType roundabout = new RoundaboutType();
        roundabout.setId("R1");
        roundabout.setLoop(loop("R1_LOOP", "S1", "S1", "MAIN_LOOP"));
        CalculatedVariableType calculated = new CalculatedVariableType();
        calculated.setId("CALC");
        calculated.setScope("MAIN_LOOP");
        Questionnaire questionnaire = questionnaire(
                List.of(roundabout, sequence("S1", GenericNameEnum.MODULE, question("Q1"))),
                List.of(),
                List.of(collectedVariable("Q1_VAR", null), calculated));

        // When we compute the scopes
        VariablesScope.updateCollectedVariablesScope(questionnaire);

        // Then
        assertThat(scopeOf(questionnaire, "Q1_VAR")).isEqualTo("MAIN_LOOP");
        assertThat(scopeOf(questionnaire, "CALC")).isEqualTo("MAIN_LOOP");
    }

}
