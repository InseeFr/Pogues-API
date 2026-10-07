package fr.insee.pogues.client.interceptor;

import fr.insee.pogues.configuration.properties.OidcServiceAccountProperties.OidcServiceAccount;
import fr.insee.pogues.exception.oidc.TokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceAccountTokenProviderTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private ServiceAccountTokenProvider tokenProvider;

    private OidcServiceAccount serviceAccount;

    @BeforeEach
    void setUp() {
        tokenProvider = new ServiceAccountTokenProvider(restClientBuilder);

        serviceAccount = new OidcServiceAccount(
                30,
                "https://auth.example.test",
                "my-realm",
                "client-id",
                "client-secret");

        when(restClientBuilder.build()).thenReturn(restClient);
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    private static Map<String, Object> validResponse(String accessToken, int expiresIn) {
        Map<String, Object> body = new HashMap<>();
        body.put("access_token", accessToken);
        body.put("expires_in", expiresIn);
        return body;
    }

    @Nested
    @DisplayName("when the cache is empty")
    class CacheMiss {

        @Test
        @DisplayName("should call the auth server and return the retrieved token")
        void should_retrieve_token_on_first_call() {
            doReturn(validResponse("token-1", 3600))
                    .when(responseSpec)
                    .body(ArgumentMatchers.any(ParameterizedTypeReference.class));

            String token = tokenProvider.getToken(serviceAccount);

            assertThat(token).isEqualTo("token-1");
            verify(restClient, times(1)).post();
            verify(requestBodyUriSpec).uri("https://auth.example.test/realms/my-realm/protocol/openid-connect/token");
        }
    }

    @Nested
    @DisplayName("when a valid token is already cached")
    class CacheHit {

        @Test
        @DisplayName("should return the cached token without calling the auth server again")
        void should_return_cached_token_without_new_call() {
            doReturn(validResponse("token-1", 3600))
                    .when(responseSpec)
                    .body(ArgumentMatchers.any(ParameterizedTypeReference.class));

            String firstToken = tokenProvider.getToken(serviceAccount);
            String secondToken = tokenProvider.getToken(serviceAccount);

            assertThat(firstToken).isEqualTo("token-1");
            assertThat(secondToken).isEqualTo("token-1");
            verify(restClient, times(1)).post();
        }
    }

    @Nested
    @DisplayName("when the cached token is expired (or within the refresh margin)")
    class CacheExpired {

        @Test
        @DisplayName("should call the auth server again and return the refreshed token")
        void should_refresh_token_when_expired() {
            doReturn(validResponse("token-expired", 0), validResponse("token-refreshed", 3600))
                    .when(responseSpec)
                    .body(ArgumentMatchers.any(ParameterizedTypeReference.class));

            String firstToken = tokenProvider.getToken(serviceAccount);
            String secondToken = tokenProvider.getToken(serviceAccount);

            assertThat(firstToken).isEqualTo("token-expired");
            assertThat(secondToken).isEqualTo("token-refreshed");
            verify(restClient, times(2)).post();
        }
    }

    @Nested
    @DisplayName("when the auth server call fails")
    class AuthServerFailure {

        @Test
        @DisplayName("should wrap RestClientException into a TokenException")
        void should_wrap_rest_client_exception() {
            RestClientException cause = new RestClientException("authentication server unavailable");
            when(responseSpec.body(ArgumentMatchers.any(ParameterizedTypeReference.class)))
                    .thenThrow(cause);

            assertThatThrownBy(() -> tokenProvider.getToken(serviceAccount))
                    .isInstanceOf(TokenException.class)
                    .hasMessage("Failed to retrieve service account token: authentication server unavailable");
        }
    }

    @Nested
    @DisplayName("when the auth server response is invalid")
    class InvalidResponse {

        @Test
        @DisplayName("should throw TokenException when response body is null")
        void should_throw_when_body_is_null() {
            doReturn(null)
                    .when(responseSpec)
                    .body(ArgumentMatchers.any(ParameterizedTypeReference.class));

            assertThatThrownBy(() -> tokenProvider.getToken(serviceAccount))
                    .isInstanceOf(TokenException.class)
                    .hasMessage("Invalid response: missing or incorrect 'access_token'");
        }

        @Test
        @DisplayName("should throw TokenException when access_token is missing")
        void should_throw_when_access_token_missing() {
            Map<String, Object> body = new HashMap<>();
            body.put("expires_in", 3600);
            doReturn(body)
                    .when(responseSpec)
                    .body(ArgumentMatchers.any(ParameterizedTypeReference.class));

            assertThatThrownBy(() -> tokenProvider.getToken(serviceAccount))
                    .isInstanceOf(TokenException.class)
                    .hasMessage("Invalid response: missing or incorrect 'access_token'");
        }

        @Test
        @DisplayName("should throw TokenException when access_token is not a String")
        void should_throw_when_access_token_wrong_type() {
            Map<String, Object> body = new HashMap<>();
            body.put("access_token", 12345);
            body.put("expires_in", 3600);
            doReturn(body)
                    .when(responseSpec)
                    .body(ArgumentMatchers.any(ParameterizedTypeReference.class));

            assertThatThrownBy(() -> tokenProvider.getToken(serviceAccount))
                    .isInstanceOf(TokenException.class)
                    .hasMessage("Invalid response: missing or incorrect 'access_token'");
        }

        @Test
        @DisplayName("should throw TokenException when expires_in is missing")
        void should_throw_when_expires_in_missing() {
            Map<String, Object> body = new HashMap<>();
            body.put("access_token", "token-without-expiration");
            doReturn(body)
                    .when(responseSpec)
                    .body(ArgumentMatchers.any(ParameterizedTypeReference.class));

            assertThatThrownBy(() -> tokenProvider.getToken(serviceAccount))
                    .isInstanceOf(TokenException.class)
                    .hasMessage("Invalid response: missing 'expires_in'");
        }
    }

    @Nested
    @DisplayName("when different service accounts are used")
    class MultipleServiceAccounts {

        @Test
        @DisplayName("should manage separate token caches per service account")
        void should_cache_tokens_independently_per_account() {
            OidcServiceAccount otherAccount = new OidcServiceAccount(
                    30,
                    "https://auth.example.test",
                    "other-realm",
                    "other-client",
                    "other-secret");

            doReturn(validResponse("token-account-1", 3600), validResponse("token-account-2", 3600))
                    .when(responseSpec)
                    .body(ArgumentMatchers.any(ParameterizedTypeReference.class));

            String tokenForFirstAccount = tokenProvider.getToken(serviceAccount);
            String tokenForSecondAccount = tokenProvider.getToken(otherAccount);

            assertThat(tokenForFirstAccount).isEqualTo("token-account-1");
            assertThat(tokenForSecondAccount).isEqualTo("token-account-2");
            verify(restClient, times(2)).post();
        }
    }
}