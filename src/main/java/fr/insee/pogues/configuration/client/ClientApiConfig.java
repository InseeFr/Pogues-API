package fr.insee.pogues.configuration.client;

import fr.insee.pogues.client.interceptor.AuthInterceptor;
import fr.insee.pogues.configuration.auth.user.AuthenticationHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
@RequiredArgsConstructor
public class ClientApiConfig {

    private final AuthenticationHelper authenticationHelper;
    private final RestClient.Builder restClientBuilder;

    @Bean("questionnaireRegistryApiRestClient")
    public RestClient questionnaireRegistryApiRestClient(
            @Value("${application.registry.questionnaire.host}") String registryHost
    ) {
        return buildRestClient(registryHost, new AuthInterceptor(authenticationHelper));
    }

    @Bean("nomenclatureRegistryApiRestClient")
    public RestClient nomenclatureRegistryApiRestClient(
            @Value("${application.registry.nomenclature.host}") String registryHost
    ) {
        return buildRestClient(registryHost, new AuthInterceptor(authenticationHelper));
    }

    @Bean("magmaFusionApiRestClient")
    public RestClient magmaFusionApiRestClient(
            @Value("${application.metadata.magma-fusion}") String magmaFusionHost
    ) {
        return buildRestClient(magmaFusionHost, null);
    }

    @Bean("enoApiRestClient")
    public RestClient enoApiRestClient(
            @Value("${application.eno.host}") String enoHost
    ) {
        return buildRestClient(enoHost, null);
    }

    private RestClient buildRestClient(String host, ClientHttpRequestInterceptor interceptor) {
        RestClient.Builder builder =  restClientBuilder.clone()
                .baseUrl(buildBaseUrl(host));
        if (interceptor != null) {
            builder.requestInterceptor(interceptor);
        }
        return builder.build();
    }

    /**
     * Builds the base URL for a RestClient.
     * <p>
     * Note: consumers must use <b>relative paths</b> (no leading "/") when calling
     * {@code RestClient.uri(...)} if the host itself contains a base path
     * (e.g. "http://host.test/base-api"). An absolute path starting with "/"
     * replaces the base path entirely (RFC 3986 URI resolution), losing "/base-api".
     */
    static String buildBaseUrl(String host) {
        if (host == null || host.isBlank()) {
            throw new IllegalStateException(
                    "Missing required host configuration for RestClient base URL");
        }
        return host.endsWith("/") ? host : host + "/";
    }
}