package fr.insee.pogues.model.dto.release;

import fr.insee.pogues.domain.enums.generation.GenerationQuestionNumberingMode;

public record OverrideGenerationParameters(
        GenerationQuestionNumberingMode questionNumberingMode,
        Boolean responseTimeQuestion
) {
}
