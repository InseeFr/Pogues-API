package fr.insee.pogues.model.dto.loops;

/**
 * Component that can be selected as the final member of a loop.
 * @param id ID of the component
 * @param name Name of the component
 * @param type Kind of the component
 */
public record LoopMemberDTO(String id, String name, LoopMemberTypeDTOEnum type) {}
