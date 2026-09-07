package fr.insee.pogues.client.surveyregistry.questionnaire;

import fr.insee.pogues.client.surveyregistry.exceptions.*;
import fr.insee.pogues.client.surveyregistry.model.CollectionInstrumentCreateDto;
import fr.insee.pogues.client.surveyregistry.model.CollectionInstrumentMetadataDto;
import fr.insee.pogues.client.surveyregistry.model.ConceptualModelDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Client which allow to access registry data
 * through REST endpoints calls towards Registry API.
 *
 * @since 5.0
 */
@Slf4j
@Service
public class QuestionnaireRegistryRestClient implements QuestionnaireRegistryClient {

    public static final String EXPAND_PARAM = "expand";
    public static final String CODES_LISTS_EXPAND_PARAM = "CODES_LISTS";
    public static final String CONCEPTUAL_MODELS_PATH = "conceptual-models";
    public static final String COLLECTION_INSTRUMENTS_PATH = "collection-instruments";

    private static final String NOT_FOUND = " not found";

    private final RestClient restClient;

    public QuestionnaireRegistryRestClient(@Qualifier("questionnaireRegistryApiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<ConceptualModelDto> findConceptualModelByPoguesVersionId(UUID poguesVersionId) {
        URI uri = UriComponentsBuilder
                .fromPath(CONCEPTUAL_MODELS_PATH)
                .queryParam("poguesVersionId", poguesVersionId)
                .encode()
                .build()
                .toUri();

        return restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .exchange((request, response) -> {
                    if (response.getStatusCode().value() == 404) {
                        return Optional.empty();
                    }
                    if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
                        throw new SurveyRegistryException("Error when getting conceptual-model: " + response.getStatusCode());
                    }
                    return Optional.ofNullable(response.bodyTo(ConceptualModelDto.class));
                });
    }

    @Override
    public void createConceptualModel(ConceptualModelDto conceptualModel) {
        restClient.post()
                .uri(CONCEPTUAL_MODELS_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .body(conceptualModel)
                .retrieve()
                .body(ConceptualModelDto.class);
    }

    @Override
    public UUID createCollectionInstrumentMetadata(CollectionInstrumentCreateDto collectionInstrumentCreate) {
        return restClient.post()
                .uri(COLLECTION_INSTRUMENTS_PATH + "/metadata")
                .body(collectionInstrumentCreate)
                .retrieve()
                .body(UUID.class);
    }

    @Override
    public void publishLunaticInCollectionInstrument(UUID collectionInstrumentId, String lunatic) {
        restClient.put()
                .uri(COLLECTION_INSTRUMENTS_PATH + "/{collectionInstrumentId}/lunatic", collectionInstrumentId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(lunatic)
                .exchange((request, response) -> {
                    if (response.getStatusCode().value() == 404) {
                        throw new ResourceRegistryNotFound("Collection instrument " + collectionInstrumentId + NOT_FOUND);
                    }
                    if (response.getStatusCode().value() == 409) {
                        throw new CollectionInstrumentConflictException("lunatic in collection instrument " + collectionInstrumentId + " already exists");
                    }
                    if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
                        throw new SurveyRegistryException("Error when publishing lunatic in collection instrument " + collectionInstrumentId + ": " + response.getStatusCode());
                    }
                    return null;
                });
    }

    @Override
    public void publishDDIInConceptualModel(UUID poguesVersionId, String ddi) {
        restClient.put()
                .uri(CONCEPTUAL_MODELS_PATH + "/{poguesVersionId}/ddi", poguesVersionId)
                .contentType(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_JSON)
                .body(ddi)
                .exchange((request, response) -> {
                    if (response.getStatusCode().value() == 404) {
                        throw new ResourceRegistryNotFound("Conceptual model " + poguesVersionId + NOT_FOUND);
                    }
                    if (response.getStatusCode().value() == 409) {
                        throw new CollectionInstrumentConflictException("ddi in conceptual model " + poguesVersionId + " already exists");
                    }
                    if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
                        throw new SurveyRegistryException("Error when publishing ddi in conceptual model " + poguesVersionId + ": " + response.getStatusCode());
                    }
                    return null;
                });
    }

    @Override
    public List<CollectionInstrumentMetadataDto> getCollectionInstrumentByPoguesId(String poguesId, boolean withCodeLists) {

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromPath(COLLECTION_INSTRUMENTS_PATH)
                .queryParam("poguesId", poguesId);

        if (withCodeLists) {
            uriBuilder.queryParam(EXPAND_PARAM, CODES_LISTS_EXPAND_PARAM);
        }

        URI uri = uriBuilder.encode().build().toUri();

        return restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .exchange((request, response) -> {
                    if (response.getStatusCode().value() == 404) {
                        return List.of();
                    }

                    if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
                        throw new SurveyRegistryException(
                                "Error when getting collectionInstruments: "
                                        + response.getStatusCode()
                        );
                    }

                    return response.bodyTo(new ParameterizedTypeReference<>() {});
                });
    }
}
