package fr.insee.pogues.client.metadata;

import fr.insee.pogues.client.metadata.exceptions.MetadataRepositoryException;
import fr.insee.pogues.client.metadata.exceptions.UnitNotFoundException;
import fr.insee.pogues.client.metadata.model.ddias.Unit;
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
@ConditionalOnProperty(name = "feature.metadata.ddias-client", havingValue = "rest")
public class DDIASRestClient implements DDIASClient {

    private final RestClient restClient;

    public DDIASRestClient(@Qualifier("ddiAsApiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<Unit> getUnits() {
        URI uri = UriComponentsBuilder
            .fromPath("meta-data/units")
            .encode()
            .build()
            .toUri();
        log.info("Get all Units from DDIAS with URI {}", uri);
        return restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        (request, response) -> {
                            if (response.getStatusCode().value() == 404) {
                                throw new UnitNotFoundException("Unit not found");
                            }
                            throw new MetadataRepositoryException(String.format(
                                    "Error when getting units from metadata repository (DDIAS): %s",
                                    response.getStatusCode()));
                        })
                .body(new ParameterizedTypeReference<>() {});
    }
}
