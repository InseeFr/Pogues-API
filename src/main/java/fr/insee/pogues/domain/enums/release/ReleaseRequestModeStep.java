package fr.insee.pogues.domain.enums.release;

public enum ReleaseRequestModeStep {
    INIT,
    GENERATE_PARAMETERS,
    PUBLICATION_PRERELEASE,
    GENERATE_LUNATIC,
    PUBLICATION_LUNATIC,
    FINISHED
}
