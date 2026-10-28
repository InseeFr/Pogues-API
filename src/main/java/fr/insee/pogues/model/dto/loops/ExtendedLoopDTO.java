package fr.insee.pogues.model.dto.loops;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.List;

/**
 * Loop of a questionnaire as sent to the Pogues frontend form, with the information needed to edit it.
 * The fields of the loop are serialized at the same level as the extended ones.
 * @param loop Loop, in the format used to create or update it
 * @param relatedLoopNames Names of the loops based on this loop, which prevent it from being deleted
 */
public record ExtendedLoopDTO(
        @JsonUnwrapped LoopDTO loop,
        List<String> relatedLoopNames
) {}
