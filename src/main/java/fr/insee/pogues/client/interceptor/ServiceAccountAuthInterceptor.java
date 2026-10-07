package fr.insee.pogues.client.interceptor;

import fr.insee.pogues.configuration.properties.OidcServiceAccountProperties.OidcServiceAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/** Adds a service account token to http requests, for a specific OIDC service account. */
@Slf4j
@RequiredArgsConstructor
public class ServiceAccountAuthInterceptor implements ClientHttpRequestInterceptor {

    private final ServiceAccountTokenProvider serviceAccountTokenProvider;
    private final OidcServiceAccount serviceAccount;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().setBearerAuth(serviceAccountTokenProvider.getToken(serviceAccount));
        return execution.execute(request, body);
    }
}