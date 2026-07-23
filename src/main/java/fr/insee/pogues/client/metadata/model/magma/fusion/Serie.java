package fr.insee.pogues.client.metadata.model.magma.fusion;

import java.util.List;

public record Serie(
        String seriesId,
        String uri,
        List<Label> label) {
}
