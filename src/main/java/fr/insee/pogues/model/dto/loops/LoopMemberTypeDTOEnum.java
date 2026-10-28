package fr.insee.pogues.model.dto.loops;

/** Kind of component that can be a bound (initial or final member) of a loop. */
public enum LoopMemberTypeDTOEnum {
    /** Sequence (depth 1 component, generic name MODULE). */
    SEQUENCE,
    /** Subsequence (depth 2 component, generic name SUBMODULE). */
    SUBSEQUENCE,
    /** Reference to a child questionnaire (depth 1 component). */
    EXTERNAL_ELEMENT
}
