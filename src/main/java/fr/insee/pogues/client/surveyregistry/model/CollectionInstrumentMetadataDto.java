package fr.insee.pogues.client.surveyregistry.model;

import fr.insee.pogues.domain.enums.generation.CollectMode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CollectionInstrumentMetadataDto(
        UUID collectionInstrumentId,
        Long version,
        String releaseDescription,
        Instant releaseDate,
        UUID poguesVersionId,
        CollectMode mode,
        List<CollectionInstrumentCodesList> codesLists
) {
}
