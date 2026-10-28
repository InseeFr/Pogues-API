package fr.insee.pogues.utils.model;

import fr.insee.pogues.model.*;

import java.util.*;

import static fr.insee.pogues.utils.model.Variables.getQuestionCollectedVariables;
import static fr.insee.pogues.utils.model.question.Table.isDynamicTable;

/**
 * <p>Helper class to compute the scope of the collected variables of a questionnaire.</p>
 * <p>The scope of a collected variable is not chosen by the user: it is deduced from the loops and roundabouts
 * of the questionnaire, and from the dynamic tables. It must therefore be computed again each time a loop or a
 * roundabout is created, updated or deleted, or when a component is moved.</p>
 */
public class VariablesScope {

    private VariablesScope() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * <p>Compute again the scope of every collected variable of the questionnaire:</p>
     * <ol>
     *     <li>the variables of a dynamic table have the table as scope;</li>
     *     <li>otherwise, the variables of a question covered by a loop (or a roundabout) have the loop scope as
     *     scope: the scope the loop is based on if any, the loop itself otherwise. When a question is covered by
     *     several loops, the innermost one is kept;</li>
     *     <li>otherwise, the variables have no scope.</li>
     * </ol>
     * <p>Calculated and external variables are left unchanged, since their scope is chosen by the user.</p>
     * @param questionnaire Questionnaire to update
     */
    public static void updateCollectedVariablesScope(Questionnaire questionnaire) {
        Map<String, String> scopeByVariableId = new HashMap<>();

        // Loops over sequences are applied first, so that loops over subsequences (the innermost ones) prevail
        List<LoopScope> loops = getLoopScopes(questionnaire);
        loops.stream().filter(loop -> !loop.isOverSubsequences(questionnaire))
                .forEach(loop -> setScopeOfCoveredVariables(questionnaire, loop, scopeByVariableId));
        loops.stream().filter(loop -> loop.isOverSubsequences(questionnaire))
                .forEach(loop -> setScopeOfCoveredVariables(questionnaire, loop, scopeByVariableId));

        // Dynamic tables prevail over loops
        getQuestions(questionnaire.getChild()).stream()
                .filter(VariablesScope::isDynamicTableQuestion)
                .forEach(table -> getQuestionCollectedVariables(table)
                        .forEach(variableId -> scopeByVariableId.put(variableId, table.getId())));

        if (questionnaire.getVariables() == null) return;
        questionnaire.getVariables().getVariable().stream()
                .filter(CollectedVariableType.class::isInstance)
                .forEach(variable -> variable.setScope(scopeByVariableId.get(variable.getId())));
    }

    /**
     * Bounds of a loop (or of a roundabout) and the scope given to the variables it covers.
     * @param initialMember ID of the first component covered
     * @param finalMember ID of the last component covered
     * @param scope Scope of the variables collected in the covered components
     */
    private record LoopScope(String initialMember, String finalMember, String scope) {

        /** Whether the loop iterates over subsequences (rather than over sequences or external elements). */
        boolean isOverSubsequences(Questionnaire questionnaire) {
            return questionnaire.getChild().stream().noneMatch(component -> initialMember.equals(component.getId()));
        }
    }

    /**
     * Gather the loops and the roundabouts of the questionnaire, in this order.
     * @param questionnaire Questionnaire
     * @return Bounds and scope of each loop and roundabout having valid bounds
     */
    private static List<LoopScope> getLoopScopes(Questionnaire questionnaire) {
        List<LoopScope> loops = new ArrayList<>();
        if (questionnaire.getIterations() != null) {
            questionnaire.getIterations().getIteration().stream()
                    .filter(DynamicIterationType.class::isInstance).map(DynamicIterationType.class::cast)
                    .forEach(loop -> toLoopScope(loop, loop.getId()).ifPresent(loops::add));
        }
        questionnaire.getChild().stream()
                .filter(RoundaboutType.class::isInstance).map(RoundaboutType.class::cast)
                .filter(roundabout -> roundabout.getLoop() != null)
                .forEach(roundabout -> toLoopScope(roundabout.getLoop(), roundabout.getId()).ifPresent(loops::add));
        return loops;
    }

    /**
     * @param iteration Loop or loop of a roundabout
     * @param defaultScope Scope of the covered variables if the loop is not based on another scope
     * @return The loop bounds and scope, or empty if the loop has no bounds
     */
    private static Optional<LoopScope> toLoopScope(DynamicIterationType iteration, String defaultScope) {
        List<String> members = iteration.getMemberReference();
        if (members.isEmpty()) return Optional.empty();
        // A single reference means that the loop iterates over a single component
        String scope = iteration.getIterableReference() != null ? iteration.getIterableReference() : defaultScope;
        return Optional.of(new LoopScope(members.getFirst(), members.getLast(), scope));
    }

    /**
     * Set the scope of the loop to the variables collected by the questions it covers.
     * @param questionnaire Questionnaire
     * @param loop Loop
     * @param scopeByVariableId Scope of each variable, updated by the method
     */
    private static void setScopeOfCoveredVariables(Questionnaire questionnaire, LoopScope loop, Map<String, String> scopeByVariableId) {
        getCoveredComponents(questionnaire.getChild(), loop).stream()
                .map(component -> getQuestions(List.of(component)))
                .flatMap(List::stream)
                .forEach(question -> getQuestionCollectedVariables(question)
                        .forEach(variableId -> scopeByVariableId.put(variableId, loop.scope())));
    }

    /**
     * Find the components covered by the loop: the siblings from its initial member to its final member
     * (both included).
     * @param siblings Components sharing the same parent, searched recursively
     * @param loop Loop
     * @return The covered components, or an empty list if the bounds are not found
     */
    private static List<ComponentType> getCoveredComponents(List<ComponentType> siblings, LoopScope loop) {
        List<String> siblingIds = siblings.stream().map(ComponentType::getId).toList();
        int initialIndex = siblingIds.indexOf(loop.initialMember());
        if (initialIndex >= 0) {
            int finalIndex = siblingIds.indexOf(loop.finalMember());
            if (finalIndex < initialIndex) return List.of();
            return siblings.subList(initialIndex, finalIndex + 1);
        }
        for (ComponentType component : siblings) {
            if (component instanceof SequenceType sequence) {
                List<ComponentType> covered = getCoveredComponents(sequence.getChild(), loop);
                if (!covered.isEmpty()) return covered;
            }
        }
        return List.of();
    }

    /**
     * @param components Components of the questionnaire
     * @return The questions among the components and their descendants, in the questionnaire order
     */
    private static List<QuestionType> getQuestions(List<ComponentType> components) {
        List<QuestionType> questions = new ArrayList<>();
        for (ComponentType component : components) {
            if (component instanceof QuestionType question) questions.add(question);
            else if (component instanceof SequenceType sequence) questions.addAll(getQuestions(sequence.getChild()));
        }
        return questions;
    }

    private static boolean isDynamicTableQuestion(QuestionType question) {
        return question.getResponseStructure() != null && isDynamicTable(question);
    }

}
