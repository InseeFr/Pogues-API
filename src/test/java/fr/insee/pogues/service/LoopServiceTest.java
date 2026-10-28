package fr.insee.pogues.service;

import fr.insee.pogues.exception.loops.LoopInvalidException;
import fr.insee.pogues.exception.loops.LoopNotFoundException;
import fr.insee.pogues.exception.loops.LoopUsedAsBasedOnException;
import fr.insee.pogues.model.*;
import fr.insee.pogues.model.dto.loops.ExtendedLoopDTO;
import fr.insee.pogues.model.dto.loops.LoopDTO;
import fr.insee.pogues.model.dto.loops.LoopInitialMemberDTO;
import fr.insee.pogues.model.dto.loops.LoopMemberDTO;
import fr.insee.pogues.model.dto.loops.LoopMemberTypeDTOEnum;
import fr.insee.pogues.persistence.service.IQuestionnaireService;
import fr.insee.pogues.persistence.service.VersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static fr.insee.pogues.model.dto.loops.LoopMemberTypeDTOEnum.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class LoopServiceTest {

    @Mock
    IQuestionnaireService questionnaireService;

    @Mock
    VersionService versionService;

    private LoopService loopService;

    @BeforeEach
    void init() {
        loopService = new LoopService(questionnaireService, versionService);
    }

    private static SequenceType sequence(String id, GenericNameEnum genericName, ComponentType... children) {
        SequenceType sequence = new SequenceType();
        sequence.setId(id);
        sequence.setName(id + "_NAME");
        sequence.setGenericName(genericName);
        sequence.getChild().addAll(List.of(children));
        return sequence;
    }

    /** Question collecting the variable {@code <id>_VAR}. */
    private static QuestionType question(String id) {
        QuestionType question = new QuestionType();
        question.setId(id);
        question.setName(id + "_NAME");
        question.setQuestionType(QuestionTypeEnum.SIMPLE);
        ResponseType response = new ResponseType();
        response.setCollectedVariableReference(id + "_VAR");
        question.getResponse().add(response);
        return question;
    }

    private static CollectedVariableType collectedVariable(String id) {
        CollectedVariableType variable = new CollectedVariableType();
        variable.setId(id);
        variable.setName(id);
        return variable;
    }

    private static LoopDTO fixedLoop(String id, String name, String initialMember, String finalMember) {
        return new LoopDTO(id, name, initialMember, finalMember, null, null, true, "2", false, null, null);
    }

    private static LoopDTO basedOnLoop(String id, String name, String initialMember, String finalMember, String basedOn) {
        return new LoopDTO(id, name, initialMember, finalMember, basedOn, null, null, null, null, null, null);
    }

    private static String scopeOf(Questionnaire questionnaire, String variableId) {
        return questionnaire.getVariables().getVariable().stream()
                .filter(variable -> variableId.equals(variable.getId()))
                .findFirst().orElseThrow().getScope();
    }

    private static LoopMemberDTO member(String id, LoopMemberTypeDTOEnum type) {
        return new LoopMemberDTO(id, id + "_NAME", type);
    }

    private static LoopInitialMemberDTO initialMember(String id, LoopMemberTypeDTOEnum type, LoopMemberDTO... finalMembers) {
        return new LoopInitialMemberDTO(id, id + "_NAME", type, List.of(finalMembers));
    }

    /**
     * ├── S1 (sequence)
     * │   ├── SS1 (subsequence)
     * │   │   ├── Q1 (question)
     * │   │   └── QA (question)
     * │   └── SS2 (subsequence)
     * │       └── Q2 (question)
     * ├── E1 (external element)
     * ├── R1 (roundabout)
     * ├── S2 (sequence)
     * │   └── SS3 (subsequence)
     * │       └── Q3 (question)
     * └── idendquest
     */
    private static Questionnaire buildQuestionnaire() {
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setId("q-id");
        questionnaire.setGenericName(GenericNameEnum.QUESTIONNAIRE);
        RoundaboutType roundabout = new RoundaboutType();
        roundabout.setId("R1");
        questionnaire.getChild().addAll(List.of(
                sequence("S1", GenericNameEnum.MODULE,
                        sequence("SS1", GenericNameEnum.SUBMODULE, question("Q1"), question("QA")),
                        sequence("SS2", GenericNameEnum.SUBMODULE, question("Q2"))),
                sequence("E1", GenericNameEnum.EXTERNAL_ELEMENT),
                roundabout,
                sequence("S2", GenericNameEnum.MODULE,
                        sequence("SS3", GenericNameEnum.SUBMODULE, question("Q3"))),
                sequence("idendquest", GenericNameEnum.MODULE)
        ));
        questionnaire.setVariables(new Questionnaire.Variables());
        questionnaire.getVariables().getVariable().addAll(List.of(
                collectedVariable("Q1_VAR"), collectedVariable("QA_VAR"),
                collectedVariable("Q2_VAR"), collectedVariable("Q3_VAR")));
        return questionnaire;
    }

    @Test
    @DisplayName("Should compute loop members of a questionnaire in the questionnaire order")
    void getQuestionnaireLoopMembers_success() throws Exception {
        // Given a questionnaire with sequences, subsequences, questions, an external element and a roundabout
        Mockito.when(questionnaireService.getQuestionnaireModelByID("q-id")).thenReturn(buildQuestionnaire());

        // When we compute the loop members
        List<LoopInitialMemberDTO> res = loopService.getQuestionnaireLoopMembers("q-id");

        // Then sequences, subsequences and external elements are proposed as initial members, with their final members
        List<LoopInitialMemberDTO> expected = List.of(
                initialMember("S1", SEQUENCE, member("S1", SEQUENCE), member("E1", EXTERNAL_ELEMENT), member("S2", SEQUENCE)),
                initialMember("SS1", SUBSEQUENCE, member("SS1", SUBSEQUENCE), member("SS2", SUBSEQUENCE)),
                initialMember("SS2", SUBSEQUENCE, member("SS2", SUBSEQUENCE)),
                initialMember("E1", EXTERNAL_ELEMENT, member("E1", EXTERNAL_ELEMENT), member("S2", SEQUENCE)),
                initialMember("S2", SEQUENCE, member("S2", SEQUENCE)),
                initialMember("SS3", SUBSEQUENCE, member("SS3", SUBSEQUENCE))
        );
        assertThat(res).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should fetch the loop members of a questionnaire's version")
    void getVersionLoopMembers_success() throws Exception {
        // Given a questionnaire's version
        UUID versionId = UUID.randomUUID();
        Mockito.when(versionService.getVersionDataQuestionnaireModelByVersionId(versionId)).thenReturn(buildQuestionnaire());

        // When we compute the loop members of the version
        List<LoopInitialMemberDTO> res = loopService.getVersionLoopMembers(versionId);

        // Then they are the same as the ones of the questionnaire
        assertThat(res).isEqualTo(loopService.getQuestionnaireLoopMembers(buildQuestionnaire()));
        assertThat(res).extracting(LoopInitialMemberDTO::id).containsExactly("S1", "SS1", "SS2", "E1", "S2", "SS3");
    }

    @Test
    @DisplayName("Should not propose the fake last sequence `idendquest` as a loop member")
    void getQuestionnaireLoopMembers_empty() {
        // Given a questionnaire with the fake last sequence `idendquest`
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.getChild().add(sequence("idendquest", GenericNameEnum.MODULE));

        // When we compute the loop members
        List<LoopInitialMemberDTO> res = loopService.getQuestionnaireLoopMembers(questionnaire);

        // Then the fake last sequence is not considered as a possible loop member
        assertThat(res).isEmpty();
    }

    @Test
    @DisplayName("Should create a loop and save the questionnaire")
    void upsertQuestionnaireLoop_create() throws Exception {
        // Given a questionnaire without loop
        Questionnaire questionnaire = buildQuestionnaire();
        Mockito.when(questionnaireService.getQuestionnaireModelByID("q-id")).thenReturn(questionnaire);

        // When we create a loop over S1 and E1
        boolean isCreated = loopService.upsertQuestionnaireLoop("q-id", fixedLoop("L1", "BOUCLE", "S1", "E1"));

        // Then the loop is added, the variables it covers have the loop as scope, and the questionnaire is saved
        assertThat(isCreated).isTrue();
        List<IterationType> iterations = questionnaire.getIterations().getIteration();
        assertThat(iterations).hasSize(1);
        assertThat(iterations.getFirst().getId()).isEqualTo("L1");
        assertThat(iterations.getFirst().getMemberReference()).isEqualTo(List.of("S1", "E1"));
        assertThat(scopeOf(questionnaire, "Q1_VAR")).isEqualTo("L1");
        assertThat(scopeOf(questionnaire, "QA_VAR")).isEqualTo("L1");
        assertThat(scopeOf(questionnaire, "Q2_VAR")).isEqualTo("L1");
        assertThat(scopeOf(questionnaire, "Q3_VAR")).isNull();
        Mockito.verify(questionnaireService).updateQuestionnaire("q-id", questionnaire);
    }

    @Test
    @DisplayName("Should create a loop based on another loop at the end of the iterations")
    void upsertQuestionnaireLoop_createBasedOn() throws Exception {
        // Given a questionnaire with a loop over S1
        Questionnaire questionnaire = buildQuestionnaire();
        loopService.upsertQuestionnaireLoop(questionnaire, fixedLoop("L1", "BOUCLE", "S1", "S1"));

        // When we create a loop over SS2 based on the first loop
        boolean isCreated = loopService.upsertQuestionnaireLoop(questionnaire, basedOnLoop("L2", "BOUCLE2", "SS2", "SS2", "L1"));

        // Then the loop is added at the end, and the variables keep the scope of the first loop
        assertThat(isCreated).isTrue();
        assertThat(questionnaire.getIterations().getIteration()).extracting(IterationType::getId).containsExactly("L1", "L2");
        assertThat(scopeOf(questionnaire, "Q1_VAR")).isEqualTo("L1");
        assertThat(scopeOf(questionnaire, "Q2_VAR")).isEqualTo("L1");
    }

    @Test
    @DisplayName("Should update an existing loop in place")
    void upsertQuestionnaireLoop_update() throws Exception {
        // Given a questionnaire with two loops
        Questionnaire questionnaire = buildQuestionnaire();
        loopService.upsertQuestionnaireLoop(questionnaire, fixedLoop("L1", "BOUCLE", "S1", "S1"));
        loopService.upsertQuestionnaireLoop(questionnaire, fixedLoop("L2", "BOUCLE2", "S2", "S2"));

        // When we update the first loop to go over SS3 only, keeping its name
        boolean isCreated = loopService.upsertQuestionnaireLoop(questionnaire, fixedLoop("L1", "BOUCLE", "SS3", "SS3"));

        // Then the loop is replaced at the same position, and the scopes are computed again
        assertThat(isCreated).isFalse();
        List<IterationType> iterations = questionnaire.getIterations().getIteration();
        assertThat(iterations).extracting(IterationType::getId).containsExactly("L1", "L2");
        assertThat(iterations.getFirst().getMemberReference()).isEqualTo(List.of("SS3", "SS3"));
        assertThat(scopeOf(questionnaire, "Q1_VAR")).isNull();
        assertThat(scopeOf(questionnaire, "Q3_VAR")).isEqualTo("L1");
    }

    @Test
    @DisplayName("Should refuse a loop with fields that do not match its shape")
    void upsertQuestionnaireLoop_invalidModel() {
        Questionnaire questionnaire = buildQuestionnaire();
        // based on a scope but with a size
        LoopDTO loop = new LoopDTO("L1", "BOUCLE", "S1", "S1", "TABLE", null, null, "2", null, null, null);

        LoopInvalidException exception = assertThrows(LoopInvalidException.class,
                () -> loopService.upsertQuestionnaireLoop(questionnaire, loop));
        assertThat(exception.getMessage()).startsWith("The loop must either be based on a scope");
    }

    @Test
    @DisplayName("Should refuse a loop whose name is already used by a component or another loop")
    void upsertQuestionnaireLoop_nameNotUnique() throws Exception {
        Questionnaire questionnaire = buildQuestionnaire();
        loopService.upsertQuestionnaireLoop(questionnaire, fixedLoop("L1", "BOUCLE", "S1", "S1"));

        LoopInvalidException componentName = assertThrows(LoopInvalidException.class,
                () -> loopService.upsertQuestionnaireLoop(questionnaire, fixedLoop("L2", "Q1_NAME", "S2", "S2")));
        LoopInvalidException loopName = assertThrows(LoopInvalidException.class,
                () -> loopService.upsertQuestionnaireLoop(questionnaire, fixedLoop("L2", "BOUCLE", "S2", "S2")));
        assertThat(componentName.getMessage()).isEqualTo("The name Q1_NAME is already used in the questionnaire");
        assertThat(loopName.getMessage()).isEqualTo("The name BOUCLE is already used in the questionnaire");
    }

    @Test
    @DisplayName("Should refuse a loop whose final member is not a following sibling of the initial member")
    void upsertQuestionnaireLoop_invalidMembers() {
        Questionnaire questionnaire = buildQuestionnaire();

        // different parents, wrong order, fake last sequence
        for (LoopDTO loop : List.of(
                fixedLoop("L1", "BOUCLE", "SS1", "SS3"),
                fixedLoop("L1", "BOUCLE", "S2", "S1"),
                fixedLoop("L1", "BOUCLE", "S2", "idendquest"))) {
            LoopInvalidException exception = assertThrows(LoopInvalidException.class,
                    () -> loopService.upsertQuestionnaireLoop(questionnaire, loop));
            assertThat(exception.getMessage()).startsWith("The loop cannot go from");
        }
    }

    @Test
    @DisplayName("Should refuse a loop based on an unknown scope or on itself")
    void upsertQuestionnaireLoop_invalidBasedOn() throws Exception {
        Questionnaire questionnaire = buildQuestionnaire();
        loopService.upsertQuestionnaireLoop(questionnaire, fixedLoop("L1", "BOUCLE", "S1", "S1"));
        Mockito.when(questionnaireService.getQuestionnaireModelByIDWithReferences("q-id")).thenReturn(buildQuestionnaire());

        LoopInvalidException unknown = assertThrows(LoopInvalidException.class,
                () -> loopService.upsertQuestionnaireLoop(questionnaire, basedOnLoop("L2", "BOUCLE2", "S2", "S2", "UNKNOWN")));
        LoopInvalidException itself = assertThrows(LoopInvalidException.class,
                () -> loopService.upsertQuestionnaireLoop(questionnaire, basedOnLoop("L1", "BOUCLE", "S2", "S2", "L1")));
        assertThat(unknown.getMessage()).isEqualTo("The scope UNKNOWN does not exist in the questionnaire");
        assertThat(itself.getMessage()).isEqualTo("A loop cannot be based on itself");
    }

    @Test
    @DisplayName("Should fetch a loop of the questionnaire in the format used to update it")
    void getQuestionnaireLoop_success() throws Exception {
        // Given a questionnaire with a loop
        Questionnaire questionnaire = buildQuestionnaire();
        LoopDTO loop = fixedLoop("L1", "BOUCLE", "S1", "E1");
        loopService.upsertQuestionnaireLoop(questionnaire, loop);
        Mockito.when(questionnaireService.getQuestionnaireModelByID("q-id")).thenReturn(questionnaire);

        // When we fetch the loop
        ExtendedLoopDTO res = loopService.getQuestionnaireLoop("q-id", "L1");

        // Then it is the same as the one which has been saved, without related loops
        assertThat(res).isEqualTo(new ExtendedLoopDTO(loop, List.of()));
    }

    @Test
    @DisplayName("Should fetch a loop of a questionnaire's version in the format used to update it")
    void getVersionLoop_success() throws Exception {
        // Given a questionnaire's version with a loop
        Questionnaire questionnaire = buildQuestionnaire();
        LoopDTO loop = fixedLoop("L1", "BOUCLE", "S1", "E1");
        loopService.upsertQuestionnaireLoop(questionnaire, loop);
        UUID versionId = UUID.randomUUID();
        Mockito.when(versionService.getVersionDataQuestionnaireModelByVersionId(versionId)).thenReturn(questionnaire);

        // When we fetch the loop
        ExtendedLoopDTO res = loopService.getVersionLoop(versionId, "L1");

        // Then it is the same as the one which has been saved, without related loops
        assertThat(res).isEqualTo(new ExtendedLoopDTO(loop, List.of()));
    }

    @Test
    @DisplayName("Should fetch a loop with the names of the loops based on it")
    void getQuestionnaireLoop_withRelatedLoops() throws Exception {
        // Given a loop over S1, two loops based on it, and a loop over S2
        Questionnaire questionnaire = buildQuestionnaire();
        loopService.upsertQuestionnaireLoop(questionnaire, fixedLoop("L1", "BOUCLE", "S1", "S1"));
        loopService.upsertQuestionnaireLoop(questionnaire, basedOnLoop("L2", "BOUCLE2", "SS1", "SS1", "L1"));
        loopService.upsertQuestionnaireLoop(questionnaire, basedOnLoop("L3", "BOUCLE3", "SS2", "SS2", "L1"));
        loopService.upsertQuestionnaireLoop(questionnaire, fixedLoop("L4", "BOUCLE4", "S2", "S2"));

        // When we fetch the loops
        // Then only the first one has related loops, in the questionnaire order
        assertThat(loopService.getQuestionnaireLoop(questionnaire, "L1").relatedLoopNames()).containsExactly("BOUCLE2", "BOUCLE3");
        assertThat(loopService.getQuestionnaireLoop(questionnaire, "L2").relatedLoopNames()).isEmpty();
        assertThat(loopService.getQuestionnaireLoop(questionnaire, "L4").relatedLoopNames()).isEmpty();
    }

    @Test
    @DisplayName("Should throw a 404 when the loop does not exist")
    void getQuestionnaireLoop_notFound() throws Exception {
        Questionnaire questionnaire = buildQuestionnaire();

        // without any loop
        LoopNotFoundException noLoop = assertThrows(LoopNotFoundException.class,
                () -> loopService.getQuestionnaireLoop(questionnaire, "L1"));
        // with another loop
        loopService.upsertQuestionnaireLoop(questionnaire, fixedLoop("L2", "BOUCLE", "S1", "S1"));
        LoopNotFoundException otherLoop = assertThrows(LoopNotFoundException.class,
                () -> loopService.getQuestionnaireLoop(questionnaire, "L1"));

        assertThat(noLoop.getMessage()).isEqualTo("Loop with id L1 not found in questionnaire q-id");
        assertThat(otherLoop.getMessage()).isEqualTo("Loop with id L1 not found in questionnaire q-id");
    }

    @Test
    @DisplayName("Should delete a loop, compute again the scopes and save the questionnaire")
    void deleteQuestionnaireLoop_success() throws Exception {
        // Given a questionnaire with two loops
        Questionnaire questionnaire = buildQuestionnaire();
        loopService.upsertQuestionnaireLoop(questionnaire, fixedLoop("L1", "BOUCLE", "S1", "S1"));
        loopService.upsertQuestionnaireLoop(questionnaire, fixedLoop("L2", "BOUCLE2", "S2", "S2"));
        Mockito.when(questionnaireService.getQuestionnaireModelByID("q-id")).thenReturn(questionnaire);

        // When we delete the first loop
        loopService.deleteQuestionnaireLoop("q-id", "L1");

        // Then only the other loop remains, the variables of the deleted loop have no scope anymore,
        // and the questionnaire is saved
        assertThat(questionnaire.getIterations().getIteration()).extracting(IterationType::getId).containsExactly("L2");
        assertThat(scopeOf(questionnaire, "Q1_VAR")).isNull();
        assertThat(scopeOf(questionnaire, "Q2_VAR")).isNull();
        assertThat(scopeOf(questionnaire, "Q3_VAR")).isEqualTo("L2");
        Mockito.verify(questionnaireService).updateQuestionnaire("q-id", questionnaire);
    }

    @Test
    @DisplayName("Should throw a 404 when deleting a loop which does not exist")
    void deleteQuestionnaireLoop_notFound() throws Exception {
        Questionnaire questionnaire = buildQuestionnaire();

        // without any loop
        LoopNotFoundException noLoop = assertThrows(LoopNotFoundException.class,
                () -> loopService.deleteQuestionnaireLoop(questionnaire, "L1"));
        // with another loop, which is kept
        loopService.upsertQuestionnaireLoop(questionnaire, fixedLoop("L2", "BOUCLE", "S1", "S1"));
        LoopNotFoundException otherLoop = assertThrows(LoopNotFoundException.class,
                () -> loopService.deleteQuestionnaireLoop(questionnaire, "L1"));

        assertThat(noLoop.getMessage()).isEqualTo("Loop with id L1 not found in questionnaire q-id");
        assertThat(otherLoop.getMessage()).isEqualTo("Loop with id L1 not found in questionnaire q-id");
        assertThat(questionnaire.getIterations().getIteration()).extracting(IterationType::getId).containsExactly("L2");
    }

    @Test
    @DisplayName("Should refuse to delete a loop on which other loops are based")
    void deleteQuestionnaireLoop_usedAsBasedOn() throws Exception {
        // Given a loop over S1, and two loops based on it
        Questionnaire questionnaire = buildQuestionnaire();
        loopService.upsertQuestionnaireLoop(questionnaire, fixedLoop("L1", "BOUCLE", "S1", "S1"));
        loopService.upsertQuestionnaireLoop(questionnaire, basedOnLoop("L2", "BOUCLE2", "SS1", "SS1", "L1"));
        loopService.upsertQuestionnaireLoop(questionnaire, basedOnLoop("L3", "BOUCLE3", "SS2", "SS2", "L1"));

        // When we delete the first loop
        LoopUsedAsBasedOnException exception = assertThrows(LoopUsedAsBasedOnException.class,
                () -> loopService.deleteQuestionnaireLoop(questionnaire, "L1"));

        // Then the deletion is refused with the names of the loops based on it, and nothing is deleted
        assertThat(exception.getMessage()).isEqualTo(
                "The loop BOUCLE cannot be deleted because the following loops are based on it: BOUCLE2, BOUCLE3");
        assertThat(questionnaire.getIterations().getIteration()).extracting(IterationType::getId).containsExactly("L1", "L2", "L3");
        assertThat(scopeOf(questionnaire, "Q1_VAR")).isEqualTo("L1");

        // The loops based on it can still be deleted
        loopService.deleteQuestionnaireLoop(questionnaire, "L2");
        assertThat(questionnaire.getIterations().getIteration()).extracting(IterationType::getId).containsExactly("L1", "L3");
    }

}
