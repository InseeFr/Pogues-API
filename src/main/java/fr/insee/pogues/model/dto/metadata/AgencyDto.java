package fr.insee.pogues.model.dto.metadata;

import lombok.Builder;

@Builder
public record AgencyDto(
        String id,
        String label) {
}
