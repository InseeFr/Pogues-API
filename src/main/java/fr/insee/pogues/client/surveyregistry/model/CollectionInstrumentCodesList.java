package fr.insee.pogues.client.surveyregistry.model;

import java.net.URI;
import java.util.UUID;

public record CollectionInstrumentCodesList(
        UUID id,
        URI url
) {
}
