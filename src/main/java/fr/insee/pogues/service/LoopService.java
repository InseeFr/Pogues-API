package fr.insee.pogues.service;

import fr.insee.pogues.exception.PoguesException;
import fr.insee.pogues.exception.loops.LoopInvalidException;
import fr.insee.pogues.exception.loops.LoopNotFoundException;
import fr.insee.pogues.exception.loops.LoopUsedAsBasedOnException;
import fr.insee.pogues.mapper.LoopsMapper;
import fr.insee.pogues.model.ComponentType;
import fr.insee.pogues.model.DynamicIterationType;
import fr.insee.pogues.model.IterationType;
import fr.insee.pogues.model.Questionnaire;
import fr.insee.pogues.model.SequenceType;
import fr.insee.pogues.model.dto.loops.ExtendedLoopDTO;
import fr.insee.pogues.model.dto.loops.LoopDTO;
import fr.insee.pogues.model.dto.loops.LoopInitialMemberDTO;
import fr.insee.pogues.model.dto.loops.LoopMemberDTO;
import fr.insee.pogues.model.dto.loops.LoopMemberTypeDTOEnum;
import fr.insee.pogues.persistence.service.IQuestionnaireService;
import fr.insee.pogues.persistence.service.VersionService;
import fr.insee.pogues.utils.model.PoguesModelUtils;
import fr.insee.pogues.utils.model.VariablesScope;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

import static fr.insee.pogues.utils.ListUtils.replaceElementInListAccordingToCondition;
import static fr.insee.pogues.utils.model.PoguesModelUtils.FAKE_LAST_ELEMENT_ID;

/**
 * Service used to compute the components that can be used as bounds of a loop.
 * A loop iterates over sibling components, from its initial member to its final member (both included).
 * The initial member can be a sequence, a subsequence or an external element (children questionnaire). The final member must be
 * a sibling located after (or equal to) the initial member, of a compatible type: sequences and external
 * elements are interchangeable, subsequences can only be paired with subsequences.
 */
@Service
@Slf4j
@AllArgsConstructor
public class LoopService {

    private final IQuestionnaireService questionnaireService;
    private final VersionService versionService;

    /**
     * Fetch the components that can be used as initial member of a loop, each one with the components
     * that can then be used as final member.
     * @param questionnaireId ID of the questionnaire
     * @return Possible initial members, in the questionnaire order
     * @throws Exception Could not read the questionnaire from the DB
     * @throws PoguesException 404 questionnaire not found
     */
    public List<LoopInitialMemberDTO> getQuestionnaireLoopMembers(String questionnaireId) throws Exception {
        Questionnaire questionnaire = questionnaireService.getQuestionnaireModelByID(questionnaireId);
        return getQuestionnaireLoopMembers(questionnaire);
    }

    /**
     * Fetch the components of a questionnaire's version that can be used as initial member of a loop, each one
     * with the components that can then be used as final member.
     * @param versionId ID of the questionnaire's version
     * @return Possible initial members, in the questionnaire order
     * @throws Exception Could not read the version from the DB
     */
    public List<LoopInitialMemberDTO> getVersionLoopMembers(UUID versionId) throws Exception {
        Questionnaire questionnaire = versionService.getVersionDataQuestionnaireModelByVersionId(versionId);
        return getQuestionnaireLoopMembers(questionnaire);
    }

    /**
     * Compute the components that can be used as initial member of a loop, each one with the components
     * that can then be used as final member.
     * @param questionnaire Questionnaire in the Pogues model format
     * @return Possible initial members, in the questionnaire order (depth-first traversal)
     */
    public List<LoopInitialMemberDTO> getQuestionnaireLoopMembers(Questionnaire questionnaire) {
        return computeLoopMembers(questionnaire.getChild());
    }

    /**
     * Fetch a loop of the questionnaire, in the format used to create or update it, with the names of the loops
     * based on it.
     * @param questionnaireId ID of the questionnaire
     * @param loopId ID of the loop
     * @return The loop
     * @throws Exception Could not read the questionnaire from the DB
     * @throws PoguesException 404 questionnaire not found
     * @throws LoopNotFoundException The loop does not exist in the questionnaire
     */
    public ExtendedLoopDTO getQuestionnaireLoop(String questionnaireId, String loopId) throws Exception {
        Questionnaire questionnaire = questionnaireService.getQuestionnaireModelByID(questionnaireId);
        return getQuestionnaireLoop(questionnaire, loopId);
    }

    /**
     * Fetch a loop of a questionnaire's version, in the format used to create or update it, with the names of the
     * loops based on it.
     * @param versionId ID of the questionnaire's version
     * @param loopId ID of the loop
     * @return The loop
     * @throws Exception Could not read the version from the DB
     * @throws LoopNotFoundException The loop does not exist in the questionnaire's version
     */
    public ExtendedLoopDTO getVersionLoop(UUID versionId, String loopId) throws Exception {
        Questionnaire questionnaire = versionService.getVersionDataQuestionnaireModelByVersionId(versionId);
        return getQuestionnaireLoop(questionnaire, loopId);
    }

    /**
     * Find a loop of the questionnaire, in the format used to create or update it, with the names of the loops
     * based on it (which prevent it from being deleted).
     * @param questionnaire Questionnaire in the Pogues model format
     * @param loopId ID of the loop
     * @return The loop
     * @throws LoopNotFoundException The loop does not exist in the questionnaire
     */
    ExtendedLoopDTO getQuestionnaireLoop(Questionnaire questionnaire, String loopId) {
        if (questionnaire.getIterations() == null) throw loopNotFound(questionnaire, loopId);
        // IterationType only has one implementation: DynamicIterationType
        return questionnaire.getIterations().getIteration().stream()
                .filter(iteration -> loopId.equals(iteration.getId()))
                .filter(DynamicIterationType.class::isInstance)
                .map(DynamicIterationType.class::cast)
                .findFirst()
                .map(LoopsMapper::toDTO)
                .map(loop -> new ExtendedLoopDTO(loop, getRelatedLoopNames(questionnaire, loopId)))
                .orElseThrow(() -> loopNotFound(questionnaire, loopId));
    }

    /**
     * Find the loops based on the given loop.
     * @param questionnaire Questionnaire in the Pogues model format
     * @param loopId ID of the loop
     * @return Names of the loops based on the loop, in the questionnaire order
     */
    private static List<String> getRelatedLoopNames(Questionnaire questionnaire, String loopId) {
        if (questionnaire.getIterations() == null) return List.of();
        return questionnaire.getIterations().getIteration().stream()
                .filter(DynamicIterationType.class::isInstance)
                .map(DynamicIterationType.class::cast)
                .filter(iteration -> loopId.equals(iteration.getIterableReference()))
                .map(IterationType::getName)
                .toList();
    }

    private static LoopNotFoundException loopNotFound(Questionnaire questionnaire, String loopId) {
        return new LoopNotFoundException(loopId, questionnaire.getId());
    }

    /**
     * Update or create a new loop in the questionnaire, then compute again the scope of the collected variables
     * (the variables of the questions covered by the loop have the loop scope).
     * It will update the questionnaire's last updated date.
     * @param questionnaireId ID of the questionnaire to update
     * @param loop Loop to upsert
     * @return Whether the loop has been created
     * @throws Exception Could not read from or write in the DB
     * @throws PoguesException 404 questionnaire not found
     * @throws LoopInvalidException The loop is not valid
     */
    public boolean upsertQuestionnaireLoop(String questionnaireId, LoopDTO loop) throws Exception {
        Questionnaire questionnaire = questionnaireService.getQuestionnaireModelByID(questionnaireId);
        boolean isCreated = upsertQuestionnaireLoop(questionnaire, loop);
        questionnaireService.updateQuestionnaire(questionnaireId, questionnaire);
        return isCreated;
    }

    /**
     * Update or create a new loop in the questionnaire, then compute again the scope of the collected variables.
     * A new loop is added at the end of the questionnaire iterations, an existing one keeps its position.
     * @param questionnaire Questionnaire to update
     * @param loop Loop to upsert
     * @return Whether the loop has been created
     * @throws Exception Could not read the questionnaire references from the DB
     * @throws LoopInvalidException The loop is not valid
     */
    boolean upsertQuestionnaireLoop(Questionnaire questionnaire, LoopDTO loop) throws Exception {
        validateLoop(questionnaire, loop);

        if (questionnaire.getIterations() == null) questionnaire.setIterations(new Questionnaire.Iterations());
        List<IterationType> iterations = questionnaire.getIterations().getIteration();
        DynamicIterationType iteration = LoopsMapper.toModel(loop);
        boolean isCreated = iterations.stream().noneMatch(existing -> loop.id().equals(existing.getId()));
        if (isCreated) {
            iterations.add(iteration);
        } else {
            replaceElementInListAccordingToCondition(
                    iterations, existing -> loop.id().equals(existing.getId()), iteration, i -> i);
        }

        VariablesScope.updateCollectedVariablesScope(questionnaire);
        return isCreated;
    }

    /**
     * Delete a loop from the questionnaire, then compute again the scope of the collected variables
     * (the variables of the questions covered by the loop lose the loop scope).
     * It will update the questionnaire's last updated date.
     * @param questionnaireId ID of the questionnaire to update
     * @param loopId ID of the loop to delete
     * @throws Exception Could not read from or write in the DB
     * @throws PoguesException 404 questionnaire not found
     * @throws LoopNotFoundException The loop does not exist in the questionnaire
     * @throws LoopUsedAsBasedOnException Other loops are based on the loop
     */
    public void deleteQuestionnaireLoop(String questionnaireId, String loopId) throws Exception {
        Questionnaire questionnaire = questionnaireService.getQuestionnaireModelByID(questionnaireId);
        deleteQuestionnaireLoop(questionnaire, loopId);
        questionnaireService.updateQuestionnaire(questionnaireId, questionnaire);
    }

    /**
     * Delete a loop from the questionnaire, then compute again the scope of the collected variables.
     * The components covered by the loop are not modified.
     * A loop on which other loops are based cannot be deleted, since they would lose their scope.
     * @param questionnaire Questionnaire to update
     * @param loopId ID of the loop to delete
     * @throws LoopNotFoundException The loop does not exist in the questionnaire
     * @throws LoopUsedAsBasedOnException Other loops are based on the loop
     */
    void deleteQuestionnaireLoop(Questionnaire questionnaire, String loopId) {
        if (questionnaire.getIterations() == null) throw loopNotFound(questionnaire, loopId);
        List<IterationType> iterations = questionnaire.getIterations().getIteration();
        IterationType loop = iterations.stream()
                .filter(iteration -> loopId.equals(iteration.getId()))
                .findFirst()
                .orElseThrow(() -> loopNotFound(questionnaire, loopId));

        List<String> relatedLoopNames = getRelatedLoopNames(questionnaire, loopId);
        if (!relatedLoopNames.isEmpty()) {
            throw new LoopUsedAsBasedOnException(loop.getName(), relatedLoopNames);
        }

        iterations.remove(loop);

        VariablesScope.updateCollectedVariablesScope(questionnaire);
    }

    /**
     * Check that the loop can be saved in the questionnaire.
     * @param questionnaire Questionnaire of the loop
     * @param loop Loop to check
     * @throws Exception Could not read the questionnaire references from the DB
     * @throws LoopInvalidException The loop is not valid
     */
    private void validateLoop(Questionnaire questionnaire, LoopDTO loop) throws Exception {
        validateLoopShape(loop);
        validateLoopName(questionnaire, loop);
        validateLoopMembers(questionnaire, loop);
        if (isFilled(loop.basedOn())) validateLoopBasedOn(questionnaire, loop);
    }

    /**
     * Check that the loop matches exactly one shape: based on a scope, with a fixed number of occurrences, or with
     * a dynamic number of occurrences. The mandatory fields of the shape must be filled, and the fields of the
     * other shapes must be absent.
     */
    private static void validateLoopShape(LoopDTO loop) {
        boolean isValid;
        if (isFilled(loop.basedOn())) {
            isValid = allNull(loop.isFixedLength(), loop.size(), loop.shouldSplitIterations(),
                    loop.minimum(), loop.maximum());
        } else if (Boolean.TRUE.equals(loop.isFixedLength())) {
            isValid = isFilled(loop.size()) && allNull(loop.filter(), loop.minimum(), loop.maximum());
        } else if (Boolean.FALSE.equals(loop.isFixedLength())) {
            isValid = isFilled(loop.minimum()) && isFilled(loop.maximum())
                    && allNull(loop.filter(), loop.size(), loop.shouldSplitIterations());
        } else {
            isValid = false;
        }
        if (!isValid) {
            throw new LoopInvalidException(
                    "The loop must either be based on a scope, have a fixed length (size) or a dynamic length (minimum and maximum)");
        }
    }

    private static boolean allNull(Object... values) {
        return Arrays.stream(values).allMatch(Objects::isNull);
    }

    /**
     * Check that the loop name is not already used by a component or another loop (filters are not concerned).
     * When the loop is updated, its current name is not considered as used.
     */
    private static void validateLoopName(Questionnaire questionnaire, LoopDTO loop) {
        Set<String> usedNames = new HashSet<>();
        usedNames.add(questionnaire.getName());
        addComponentNames(questionnaire.getChild(), usedNames);
        if (questionnaire.getIterations() != null) {
            questionnaire.getIterations().getIteration().stream()
                    .filter(iteration -> !loop.id().equals(iteration.getId()))
                    .forEach(iteration -> usedNames.add(iteration.getName()));
        }
        if (usedNames.contains(loop.name())) {
            throw new LoopInvalidException(
                    String.format("The name %s is already used in the questionnaire", loop.name()));
        }
    }

    private static void addComponentNames(List<ComponentType> components, Set<String> names) {
        for (ComponentType component : components) {
            names.add(component.getName());
            if (component instanceof SequenceType sequence) addComponentNames(sequence.getChild(), names);
        }
    }

    /**
     * Check that the final member is one of the possible final members of the initial member: a sibling of a
     * compatible type, located after (or equal to) the initial member.
     */
    private void validateLoopMembers(Questionnaire questionnaire, LoopDTO loop) {
        boolean isValid = getQuestionnaireLoopMembers(questionnaire).stream()
                .filter(initialMember -> initialMember.id().equals(loop.initialMember()))
                .flatMap(initialMember -> initialMember.finalMembers().stream())
                .anyMatch(finalMember -> finalMember.id().equals(loop.finalMember()));
        if (!isValid) {
            throw new LoopInvalidException(
                    String.format("The loop cannot go from %s to %s", loop.initialMember(), loop.finalMember()));
        }
    }

    /**
     * Check that the loop is based on an existing scope of the questionnaire: a loop which is not itself based on
     * a scope, a dynamic table, a pairwise question, or a loop of a referenced questionnaire.
     * A loop cannot be based on itself.
     * @throws Exception Could not read the questionnaire references from the DB
     */
    private void validateLoopBasedOn(Questionnaire questionnaire, LoopDTO loop) throws Exception {
        String basedOn = loop.basedOn();
        if (basedOn.equals(loop.id())) {
            throw new LoopInvalidException("A loop cannot be based on itself");
        }
        if (PoguesModelUtils.getQuestionnaireScopes(questionnaire).containsKey(basedOn)) return;
        // The scope may come from a referenced questionnaire, which is only fetched when needed
        Questionnaire questionnaireWithReferences = questionnaireService.getQuestionnaireModelByIDWithReferences(questionnaire.getId());
        if (PoguesModelUtils.getQuestionnaireScopes(questionnaireWithReferences).containsKey(basedOn)) return;
        throw new LoopInvalidException(
                String.format("The scope %s does not exist in the questionnaire", basedOn));
    }

    private static boolean isFilled(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * <p>Compute the loop members of the given sibling components and of their descendants.</p>
     * <p>A loop can only iterate over sibling components, so the final members of an initial member are
     * searched among its siblings only: the method is called once per list of siblings (children of the
     * questionnaire, then children of each sequence).</p>
     * @param siblings Components sharing the same parent
     * @return Possible initial members, in the questionnaire order (depth-first traversal)
     */
    private List<LoopInitialMemberDTO> computeLoopMembers(List<ComponentType> siblings) {
        List<LoopInitialMemberDTO> initialMembers = new ArrayList<>();
        if (siblings == null) return initialMembers;

        // Siblings that can be a bound of a loop, in the questionnaire order.
        // Other components (questions, roundabouts...) are left out: they can neither start nor end a loop,
        // but a loop can still go over them (e.g. a question between two subsequences).
        List<LoopMemberDTO> siblingMembers = siblings.stream()
                .map(LoopService::componentToLoopMember)
                .flatMap(Optional::stream)
                .toList();

        // Position of the current component in siblingMembers (only incremented for loop members),
        // used to only keep as final members the siblings located after the initial member
        int memberIndex = 0;
        for (ComponentType component : siblings) {
            Optional<LoopMemberDTO> member = componentToLoopMember(component);
            if (member.isPresent()) {
                LoopMemberDTO initialMember = member.get();
                // Final members are the initial member itself (loop over a single component)
                // and the following siblings of a compatible type
                List<LoopMemberDTO> finalMembers = siblingMembers.subList(memberIndex, siblingMembers.size()).stream()
                        .filter(candidate -> isCompatibleLoopMemberType(initialMember.type(), candidate.type()))
                        .toList();
                initialMembers.add(new LoopInitialMemberDTO(initialMember.id(), initialMember.name(), initialMember.type(), finalMembers));
                memberIndex++;
            }
            // Children are added right after their parent, to keep the questionnaire order
            if (component instanceof SequenceType sequence) {
                // add all subSequences
                initialMembers.addAll(computeLoopMembers(sequence.getChild()));
            }
        }
        return initialMembers;
    }

    /**
     * Convert a component to a loop member if it can be a bound of a loop.
     * @param component Component of the questionnaire
     * @return The loop member, or empty if the component cannot be a bound of a loop (question, roundabout,
     * fake last sequence...)
     */
    private static Optional<LoopMemberDTO> componentToLoopMember(ComponentType component) {
        // Sequences, subsequences and external elements are all SequenceType components,
        // distinguished by their generic name. The fake last sequence added by the front is excluded.
        if (!(component instanceof SequenceType sequence)
                || sequence.getGenericName() == null
                || FAKE_LAST_ELEMENT_ID.equals(sequence.getId())) {
            return Optional.empty();
        }
        LoopMemberTypeDTOEnum type = switch (sequence.getGenericName()) {
            case MODULE -> LoopMemberTypeDTOEnum.SEQUENCE;
            case SUBMODULE -> LoopMemberTypeDTOEnum.SUBSEQUENCE;
            case EXTERNAL_ELEMENT -> LoopMemberTypeDTOEnum.EXTERNAL_ELEMENT;
            // only the root questionnaire has this generic name, it cannot be a bound of a loop
            case QUESTIONNAIRE -> null;
        };
        return Optional.ofNullable(type).map(memberType -> new LoopMemberDTO(sequence.getId(), sequence.getName(), memberType));
    }

    /**
     * Check if a candidate can be the final member of a loop starting with a component of the given type.
     * A subsequence can only be paired with subsequences, while sequences and external elements are
     * interchangeable since they are both direct children of the questionnaire.
     * @param initialType Type of the initial member of the loop
     * @param candidateType Type of the candidate final member
     * @return Whether the candidate type is compatible with the initial member type
     */
    private static boolean isCompatibleLoopMemberType(LoopMemberTypeDTOEnum initialType, LoopMemberTypeDTOEnum candidateType) {
        return initialType == candidateType || (isTopLevel(initialType) && isTopLevel(candidateType));
    }

    /** Whether the loop member type is a direct child of the questionnaire (sequence or external element). */
    private static boolean isTopLevel(LoopMemberTypeDTOEnum type) {
        return type == LoopMemberTypeDTOEnum.SEQUENCE || type == LoopMemberTypeDTOEnum.EXTERNAL_ELEMENT;
    }

}
