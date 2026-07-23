package fr.insee.pogues.client.metadata.model.magma.fusion;

import java.util.List;

public record Operation(
        String id,
        String uri,
        List<Label> label
) {
}
