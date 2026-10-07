package fr.insee.pogues.configuration.client;

import fr.insee.pogues.client.interceptor.ServiceAccountTokenProvider;
import fr.insee.pogues.configuration.auth.user.AuthenticationHelper;
import fr.insee.pogues.configuration.properties.OidcServiceAccountProperties;
import fr.insee.pogues.configuration.properties.RegistryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientApiConfigTest {

    @Mock
    private AuthenticationHelper authenticationHelper;

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RegistryProperties registryProperties;

    @Mock
    private ServiceAccountTokenProvider serviceAccountTokenProvider;
    @Mock
    private OidcServiceAccountProperties oidcServiceAccountProperties;

    @Mock
    private RestClient globalRestClient;

    private ClientApiConfig clientApiConfig;

    @BeforeEach
    void setUp() {
        clientApiConfig = new ClientApiConfig(
                authenticationHelper, serviceAccountTokenProvider, oidcServiceAccountProperties,
                restClientBuilder, registryProperties);
        lenient().when(restClientBuilder.clone()).thenReturn(restClientBuilder);
        lenient().when(restClientBuilder.baseUrl((String) any())).thenReturn(restClientBuilder);
        lenient().when(restClientBuilder.build()).thenReturn(globalRestClient);
    }

    @Nested
    @DisplayName("when building the base URL")
    class BuildBaseUrl {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("should throw when host is null, empty or blank")
        void should_throw_when_host_is_missing(String invalidHost) {
            assertThatThrownBy(() -> ClientApiConfig.buildBaseUrl(invalidHost))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Missing required host configuration");
        }

        @Test
        @DisplayName("should return the host unchanged when it is valid")
        void should_return_host_when_valid() {
            String host = "http://survey-registry.test/base-api/";

            String baseUrl = ClientApiConfig.buildBaseUrl(host);

            assertThat(baseUrl).isEqualTo(host);
        }

        @Test
        @DisplayName("should return the host changed when it is valid")
        void should_return_host_with_slash_when_valid() {
            String host = "http://survey-registry.test/base-api";

            String baseUrl = ClientApiConfig.buildBaseUrl(host);

            assertThat(baseUrl).isEqualTo(host + "/");
        }
    }

    @Nested
    @DisplayName("when creating the questionnaire registry RestClient")
    class QuestionnaireRegistryApiRestClient {

        @Test
        @DisplayName("should build a RestClient configured with the given host")
        void should_build_rest_client_with_host() {

            when(registryProperties.questionnaire()).thenReturn(new RegistryProperties.RegistryEndpoint("http://survey-registry.test/base-api",""));
            RestClient restClient = clientApiConfig.questionnaireRegistryApiRestClient();

            assertThat(restClient).isNotNull();
        }

        @Test
        @DisplayName("should throw when host is missing")
        void should_throw_when_host_is_missing() {
            when(registryProperties.questionnaire()).thenReturn(new RegistryProperties.RegistryEndpoint(null, null));
            assertThatThrownBy(() -> clientApiConfig.questionnaireRegistryApiRestClient())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("when creating the nomenclature registry RestClient")
    class NomenclatureRegistryApiRestClient {

        @Test
        @DisplayName("should build a RestClient configured with the given host")
        void should_build_rest_client_with_host() {
            when(registryProperties.nomenclature()).thenReturn(new RegistryProperties.RegistryEndpoint("http://survey-registry.test/base-api",""));
            RestClient restClient = clientApiConfig.nomenclatureRegistryApiRestClient();

            assertThat(restClient).isNotNull();
        }

        @Test
        @DisplayName("should throw when host is missing")
        void should_throw_when_host_is_missing() {
            when(registryProperties.nomenclature()).thenReturn(new RegistryProperties.RegistryEndpoint(null,null));
            assertThatThrownBy(() -> clientApiConfig.nomenclatureRegistryApiRestClient())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("when creating the magma fusion RestClient")
    class MagmaFusionApiRestClient {

        @Test
        @DisplayName("should build a RestClient configured with the given host")
        void should_build_rest_client_with_host() {
            RestClient restClient = clientApiConfig.magmaFusionApiRestClient("http://magma-fusion.test");

            assertThat(restClient).isNotNull();
        }

        @Test
        @DisplayName("should throw when host is missing")
        void should_throw_when_host_is_missing() {
            assertThatThrownBy(() -> clientApiConfig.magmaFusionApiRestClient(null))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("when creating the DDI-AS RestClient")
    class DdiAsApiRestClient {

        @Test
        @DisplayName("should build a RestClient configured with the given host")
        void should_build_rest_client_with_host() {
            RestClient restClient = clientApiConfig.ddiAsApiRestClient("http://ddi-as.test");

            assertThat(restClient).isNotNull();
        }

        @Test
        @DisplayName("should throw when host is missing")
        void should_throw_when_host_is_missing() {
            assertThatThrownBy(() -> clientApiConfig.ddiAsApiRestClient(null))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("when creating the Eno RestClient")
    class EnoApiRestClient {

        @Test
        @DisplayName("should build a RestClient configured with the given host")
        void should_build_rest_client_with_host() {
            RestClient restClient = clientApiConfig.enoApiRestClient("http://eno.test");

            assertThat(restClient).isNotNull();
        }

        @Test
        @DisplayName("should throw when host is missing")
        void should_throw_when_host_is_missing() {
            assertThatThrownBy(() -> clientApiConfig.enoApiRestClient(null))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("when creating the default RestClient")
    class DefaultRestClient {

        @Test
        @DisplayName("should build a RestClient using the injected builder without a base URL")
        void should_build_rest_client_without_base_url() {
            RestClient restClient = clientApiConfig.restClient();

            assertThat(restClient).isSameAs(globalRestClient);
        }
    }
}