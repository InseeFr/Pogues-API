package fr.insee.pogues.client.interceptor;

import fr.insee.pogues.configuration.properties.OidcServiceAccountProperties.OidcServiceAccount;
import fr.insee.pogues.exception.oidc.TokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceAccountTokenProvider {

    private static final String ACCESS_TOKEN = "access_token";
    private static final String EXPIRES_IN = "expires_in";

    private final RestClient.Builder restClientBuilder;

    private final Map<OidcServiceAccount, OidcToken> cachedTokens = new ConcurrentHashMap<>();
    private final Map<OidcServiceAccount, Lock> refreshLocks = new ConcurrentHashMap<>();

    /**
     * Returns the current access token for the given service account, refreshing
     * it if necessary. The token is refreshed slightly before its actual
     * expiration, using the account's configured margin.
     */
    public String getToken(OidcServiceAccount serviceAccount) {
        Lock lock = refreshLocks.computeIfAbsent(serviceAccount, key -> new ReentrantLock());
        lock.lock();
        try {
            long marginMillis = serviceAccount.refreshMargin() * 1000L;
            OidcToken currentToken = cachedTokens.get(serviceAccount);
            if (currentToken == null || currentToken.isExpired(marginMillis)) {
                currentToken = retrieveNewToken(serviceAccount);
                cachedTokens.put(serviceAccount, currentToken);
                log.debug("Service account token refreshed for client {} on realm {}",
                        serviceAccount.clientId(), serviceAccount.realm());
            }
            return currentToken.tokenValue();
        } finally {
            lock.unlock();
        }
    }

    private OidcToken retrieveNewToken(OidcServiceAccount serviceAccount) {
        String tokenUrl = "%s/realms/%s/protocol/openid-connect/token".formatted(
                serviceAccount.authServerUrl(), serviceAccount.realm());

        String clientId = serviceAccount.clientId();
        String clientSecret = serviceAccount.clientSecret();
        String body = "grant_type=client_credentials&client_id=%s&client_secret=%s&scope=openid profile roles"
                .formatted(clientId, clientSecret);

        log.debug("Calling auth endpoint {} with body {}", tokenUrl, maskSecret(body, clientSecret));

        try {
            Map<String, Object> responseBody = restClientBuilder.build()
                    .post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return toOidcToken(responseBody);
        } catch (RestClientException e) {
            throw new TokenException("Failed to retrieve service account token: " + e.getMessage());
        }
    }

    private OidcToken toOidcToken(Map<String, Object> responseBody) {
        if (responseBody == null
                || !(responseBody.get(ACCESS_TOKEN) instanceof String accessToken)) {
            throw new TokenException("Invalid response: missing or incorrect 'access_token'");
        }
        Integer expiresIn = (Integer) responseBody.get(EXPIRES_IN);
        if (expiresIn == null) {
            throw new TokenException("Invalid response: missing 'expires_in'");
        }
        return new OidcToken(accessToken, System.currentTimeMillis() + (expiresIn.longValue() * 1000L));
    }

    private String maskSecret(String body, String secret) {
        return secret == null ? body : body.replace(secret, "<SECRET>");
    }
}