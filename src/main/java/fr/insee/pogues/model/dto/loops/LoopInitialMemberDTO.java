package fr.insee.pogues.model.dto.loops;

import java.util.List;

/**
 * Component that can be selected as the initial member of a loop, with the components
 * that can then be selected as the final member of the loop.
 * @param id ID of the component
 * @param name Name of the component
 * @param type Kind of the component
 * @param finalMembers Components that can be selected as final member when this component is the initial member,
 *                     in the questionnaire order (the component itself is always the first one)
 */
public record LoopInitialMemberDTO(String id, String name, LoopMemberTypeDTOEnum type, List<LoopMemberDTO> finalMembers) {}
