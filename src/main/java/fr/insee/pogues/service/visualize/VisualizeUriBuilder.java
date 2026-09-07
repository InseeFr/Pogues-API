package fr.insee.pogues.service.visualize;

import fr.insee.pogues.client.surveyregistry.model.CollectionInstrumentCodesList;
import fr.insee.pogues.client.surveyregistry.model.CollectionInstrumentMetadataDto;
import fr.insee.pogues.configuration.properties.visualize.InterviewerUiVisualizeProperties;
import fr.insee.pogues.configuration.properties.visualize.VisualizeQueryParams;
import fr.insee.pogues.configuration.properties.visualize.WebUiVisualizeProperties;
import fr.insee.pogues.domain.enums.generation.CollectMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static fr.insee.pogues.client.surveyregistry.questionnaire.QuestionnaireRegistryRestClient.COLLECTION_INSTRUMENTS_PATH;

@Component
public class VisualizeUriBuilder {

    private final InterviewerUiVisualizeProperties interviewerUiProperties;
    private final WebUiVisualizeProperties webUiProperties;
    private final String questionnaireRegistryHost;

    public VisualizeUriBuilder(InterviewerUiVisualizeProperties interviewerUiProperties,
                               WebUiVisualizeProperties webUiProperties,
                               @Value("${application.registry.questionnaire.host}") String questionnaireRegistryHost) {
        this.interviewerUiProperties = interviewerUiProperties;
        this.webUiProperties = webUiProperties;
        this.questionnaireRegistryHost = questionnaireRegistryHost;
    }

    private static final ObjectMapper objectMapper = JsonMapper.builder().build();


    public URI buildVisualizeUriFromCollectionInstrument(CollectionInstrumentMetadataDto collectionInstrument) {
        if (CollectMode.CAWI.equals(collectionInstrument.mode())) {
            return buildUri(webUiProperties.host(), webUiProperties.path(),
                    webUiProperties.queryParams(), collectionInstrument);
        }
        return buildUri(interviewerUiProperties.host(), interviewerUiProperties.path(),
                interviewerUiProperties.queryParams(), collectionInstrument);
    }

    private URI buildUri(String host, String path,
                         VisualizeQueryParams visualizeQueryParams,
                         CollectionInstrumentMetadataDto collectionInstrument) {

        String questionnaireQueryParam = encodeQueryParamValue(
                buildQuestionnaireUri(collectionInstrument.collectionInstrumentId()));
        String nomenclaturesQueryParam = encodeQueryParamValue(
                buildNomenclaturesJson(collectionInstrument.codesLists()));


        return UriComponentsBuilder
                .fromUriString(host)
                .path(path)
                .queryParam(visualizeQueryParams.questionnaire(), questionnaireQueryParam)
                .queryParam(visualizeQueryParams.nomenclatures(), nomenclaturesQueryParam)
                .build(true)
                .toUri();
    }

    private String buildQuestionnaireUri(UUID collectionInstrumentId) {
        return UriComponentsBuilder
                .fromUriString("{registryHost}/" + COLLECTION_INSTRUMENTS_PATH + "/{collectionInstrumentId}")
                .buildAndExpand(questionnaireRegistryHost, collectionInstrumentId)
                .encode()
                .toUriString();
    }

    private String buildNomenclaturesJson(List<CollectionInstrumentCodesList> codesLists){
        Map<UUID, URI> result = new HashMap<>();
        for(CollectionInstrumentCodesList codesList : codesLists){
            result.put(codesList.id(), codesList.url());
        }
        return objectMapper.writeValueAsString(result);
    }

    private String encodeQueryParamValue(String rawValue) {
        return URLEncoder.encode(rawValue, StandardCharsets.UTF_8);
    }

}
