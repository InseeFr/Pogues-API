package fr.insee.pogues.client.surveyregistry.model;

import fr.insee.pogues.domain.enums.generation.CollectMode;
import fr.insee.pogues.domain.enums.generation.parameters.GenerationParameters;

import java.util.UUID;

public record CollectionInstrumentCreateDto(
        UUID poguesVersionId,
        String releaseDescription,
        CollectMode mode,
        GenerationParameters generationParameters
) {
}
