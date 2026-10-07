package fr.insee.pogues.client.interceptor;

import fr.insee.pogues.configuration.properties.OidcServiceAccountProperties.OidcServiceAccount;
import fr.insee.pogues.exception.oidc.TokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceAccountAuthInterceptorTest {

    @Mock
    private ServiceAccountTokenProvider serviceAccountTokenProvider;

    @Mock
    private HttpRequest request;

    @Mock
    private ClientHttpRequestExecution execution;

    @Mock
    private ClientHttpResponse expectedResponse;

    private HttpHeaders headers;

    private OidcServiceAccount serviceAccount;

    private ServiceAccountAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        headers = new HttpHeaders();
        serviceAccount = new OidcServiceAccount(
                30L,
                "https://auth.example.test",
                "my-realm",
                "client-id",
                "client-secret");

        interceptor = new ServiceAccountAuthInterceptor(serviceAccountTokenProvider, serviceAccount);
    }

    @Nested
    @DisplayName("when the token is retrieved successfully")
    class SuccessfulTokenRetrieval {

        @Test
        @DisplayName("should set the Authorization header with the Bearer token, execute the request and return its response")
        void should_inject_bearer_token_and_forward_execution() throws IOException {
            when(request.getHeaders()).thenReturn(headers);
            when(serviceAccountTokenProvider.getToken(serviceAccount)).thenReturn("the-access-token");
            byte[] body = "some-body".getBytes();
            when(execution.execute(request, body)).thenReturn(expectedResponse);

            ClientHttpResponse actualResponse = interceptor.intercept(request, body, execution);

            assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer the-access-token");
            verify(execution, times(1)).execute(request, body);
            assertThat(actualResponse).isSameAs(expectedResponse);
        }

        @Test
        @DisplayName("should request the token for the exact service account configured on the interceptor")
        void should_request_token_for_correct_service_account() throws IOException {
            when(request.getHeaders()).thenReturn(headers);
            when(serviceAccountTokenProvider.getToken(any(OidcServiceAccount.class))).thenReturn("token-value");
            byte[] body = new byte[0];
            when(execution.execute(request, body)).thenReturn(expectedResponse);

            interceptor.intercept(request, body, execution);

            verify(serviceAccountTokenProvider, times(1)).getToken(eq(serviceAccount));
        }
    }

    @Nested
    @DisplayName("when the token retrieval fails")
    class TokenRetrievalFailure {

        @Test
        @DisplayName("should propagate the exception and never call execution.execute")
        void should_propagate_exception_without_calling_execution() {
            byte[] body = "some-body".getBytes();
            when(request.getHeaders()).thenReturn(headers);
            when(serviceAccountTokenProvider.getToken(serviceAccount))
                    .thenThrow(new TokenException("unable to retrieve token"));

            assertThatThrownBy(() -> interceptor.intercept(request, body, execution))
                    .isInstanceOf(TokenException.class)
                    .hasMessage("unable to retrieve token");

            verifyNoInteractions(execution);
            assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isNull();
        }
    }
}