package fr.insee.pogues.model.dto.release;

import fr.insee.pogues.domain.enums.generation.CollectMode;

import java.net.URI;
import java.util.UUID;

public record CollectionInstrumentReleaseDto(
        CollectMode mode,
        UUID collectionInstrumentId,
        Long version,
        OverrideGenerationParameters overrideGenerationParameters,
        URI visualizeUrl) {
}
