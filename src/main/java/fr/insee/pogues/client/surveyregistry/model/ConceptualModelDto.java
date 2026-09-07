package fr.insee.pogues.client.surveyregistry.model;

import java.util.UUID;

public record ConceptualModelDto(
        String poguesId,
        String serieId,
        UUID poguesVersionId) {
}
