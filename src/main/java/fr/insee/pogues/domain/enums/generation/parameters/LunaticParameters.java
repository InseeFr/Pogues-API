package fr.insee.pogues.domain.enums.generation.parameters;

public record LunaticParameters(
        boolean controls,
        boolean mandatoryControls,
        boolean toolTip,
        boolean missingVariables,
        boolean filterResult,
        boolean filterDescription,
        LunaticPaginationMode lunaticPaginationMode,
        boolean questionWrapping
) {
    public enum LunaticPaginationMode {
        NONE,
        QUESTION,
        SEQUENCE
    }
}