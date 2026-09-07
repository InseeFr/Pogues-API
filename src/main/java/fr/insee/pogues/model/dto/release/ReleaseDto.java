package fr.insee.pogues.model.dto.release;

import fr.insee.pogues.domain.enums.generation.GenerationContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReleaseDto(
        String author,
        Instant releaseDate,
        UUID poguesVersionId,
        String releaseDescription,
        GenerationContext context,
        List<CollectionInstrumentReleaseDto> collectionInstruments
) {
}
