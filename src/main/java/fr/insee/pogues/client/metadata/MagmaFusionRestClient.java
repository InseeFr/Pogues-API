package fr.insee.pogues.client.metadata;

import fr.insee.pogues.client.metadata.exceptions.MetadataRepositoryException;
import fr.insee.pogues.client.metadata.exceptions.SerieNotFoundException;
import fr.insee.pogues.client.metadata.model.magma.fusion.Serie;
import fr.insee.pogues.client.metadata.model.magma.fusion.SerieMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "feature.metadata.magma-client", havingValue = "rest")
public class MagmaFusionRestClient implements MagmaFusionClient {

    private final RestClient restClient;

    public MagmaFusionRestClient(@Qualifier("magmaFusionApiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<Serie> getSeries() throws MetadataRepositoryException, SerieNotFoundException {
        URI uri = UriComponentsBuilder
                .fromPath("operations/series")
                .encode()
                .build()
                .toUri();
        log.info("Get all series from magma with URI {}", uri);
        return restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        (request, response) -> {
                            if (response.getStatusCode().value() == 404) {
                                throw new SerieNotFoundException("Series not found");
                            }
                            throw new MetadataRepositoryException(String.format(
                                    "Error when getting series from metadata repository: %s",
                                    response.getStatusCode()));
                        })
                .body(new ParameterizedTypeReference<>() {});
    }

    @Override
    public SerieMetadata getSerieById(String serieId) throws MetadataRepositoryException, SerieNotFoundException {
        URI uri = UriComponentsBuilder
                .fromPath("operations/serie/")
                .path(serieId)
                .encode()
                .build()
                .toUri();
        log.info("Get one serie from magma with URI {}", uri);
        return restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        (request, response) -> {
                            if (response.getStatusCode().value() == 404) {
                                throw new SerieNotFoundException(String.format("Serie (id:%s) not found", serieId));
                            }
                            throw new MetadataRepositoryException(String.format(
                                    "Error when getting serie (id:%s) from metadata repository: %s",
                                    serieId,
                                    response.getStatusCode()));
                        })
                .body(new ParameterizedTypeReference<>() {});
    }
}
