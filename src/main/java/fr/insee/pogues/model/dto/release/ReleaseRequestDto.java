package fr.insee.pogues.model.dto.release;

import fr.insee.pogues.domain.enums.generation.CollectMode;
import fr.insee.pogues.domain.enums.generation.GenerationContext;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReleaseRequestDto(
        Long releaseRequestId,
        String author,
        Instant requestDate,
        ReleaseRequestStatus status,
        String statusDescription,
        UUID poguesVersionId,
        String poguesId,
        String releaseDescription,
        List<CollectMode> modes,
        GenerationContext context,
        OverrideGenerationParameters overrideGenerationParameters
) {
}
