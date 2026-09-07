package fr.insee.pogues.model.dto.release;

import fr.insee.pogues.domain.enums.generation.CollectMode;
import fr.insee.pogues.domain.enums.generation.GenerationContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Model for creation of request-release
 * USER should make a POST request with this DTO
 */
public record CreateReleaseRequestDto(
        @NotBlank String poguesId,
        String releaseDescription,
        @NotEmpty List<CollectMode> modes,
        @NotNull GenerationContext context,
        OverrideGenerationParameters overrideGenerationParameters
) {
}
