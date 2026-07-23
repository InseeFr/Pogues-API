package fr.insee.pogues.configuration.client;

import fr.insee.pogues.client.interceptor.AuthInterceptor;
import fr.insee.pogues.configuration.auth.user.AuthenticationHelper;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
@AllArgsConstructor
public class ClientApiConfig {

    private AuthenticationHelper authenticationHelper;

    public static String buildBaseUrl(String host){
        if(host == null) return "/";
        return UriComponentsBuilder.fromUriString(host)
                .path("/")
                .build()
                .toUriString();
    }

    @Bean("surveyRegistryApiRestClient")
    public RestClient surveyRegistryApiRestClient(
            @Value("${application.survey-registry.host}") String registryHost
    ) {

        return RestClient.builder()
                .baseUrl(buildBaseUrl(registryHost))
                .requestInterceptor(new AuthInterceptor(authenticationHelper))
                .build();
    }

    @Bean("magmaFusionApiRestClient")
    public RestClient magmaFusionApiRestClient(
            @Value("${application.metadata.magma-fusion}") String magmaFusionHost
    ) {
        return RestClient.builder()
                .baseUrl(buildBaseUrl(magmaFusionHost))
                .build();
    }

}