package fr.insee.pogues.domain.enums.release;

public enum ReleaseRequestStatusCode {
    NOT_STARTED("publication:not_started"),
    IN_PROGRESS("publication:in_progress"),
    COMPLETED("publication:completed"),

    ERROR_GENERATION_DDI("error:generation:ddi"),
    ERROR_GENERATION_LUNATIC("error:generation:lunatic"),
    ERROR_GENERATION_PARAMETERS("error:generation:parameters"),

    ERROR_PUBLICATION_DDI("error:publication:ddi"),
    ERROR_PUBLICATION_LUNATIC("error:publication:lunatic"),
    ERROR_PUBLICATION_PRERELEASE("error:publication:prerelease"),

    DEFAULT_FAILURE_STATUS_CODE("error:publication");
    public final String label;

    ReleaseRequestStatusCode(String label){ this.label = label; }
}
