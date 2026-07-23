package fr.insee.pogues.model.dto.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SerieDto(
        String id,
        String uri,
        String label,
        String altLabel,
        List<OperationDto> operations
) {
}
