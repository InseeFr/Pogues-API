package fr.insee.pogues.client.metadata.model.magma.fusion;

import java.util.List;

public record SerieMetadata(
        String seriesId,
        String uri,
        List<Label> label,
        List<Label> altLabel,
        List<Operation> operations
) {
}
