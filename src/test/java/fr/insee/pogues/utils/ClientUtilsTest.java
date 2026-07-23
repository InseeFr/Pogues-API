package fr.insee.pogues.utils;

import fr.insee.pogues.configuration.client.ClientApiConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ClientUtilsTest {

    private ClientApiConfig clientApiConfig;

    @BeforeEach
    void init(){
        clientApiConfig = new ClientApiConfig(null);
    }

    @Test
    void buildBaseUrl_WhenHostIsEmpty_ShouldReturnSlash() {
        // Given
        String host = "";

        // When
        String result = clientApiConfig.buildBaseUrl(host);

        // Then
        assertThat(result).isEqualTo("/");
    }

    @Test
    void buildBaseUrl_WhenHostAlreadyStartsWithSlash_ShouldReturnHostUnchanged() {
        // Given
        String host = "/api";

        // When
        String result = clientApiConfig.buildBaseUrl(host);

        // Then
        assertThat(result).isEqualTo("/api/");
    }

    @Test
    void buildBaseUrl_WhenHostDoesNotStartWithSlash_ShouldAddSlash() {
        // Given
        String host = "api";

        // When
        String result = clientApiConfig.buildBaseUrl(host);

        // Then
        assertThat(result).isEqualTo("api/");
    }

    @Test
    void buildBaseUrl_WhenHostIsNull_ShouldReturnSlash() {
        // Given
        String host = null;

        // When
        String result = clientApiConfig.buildBaseUrl(host);

        // Then
        assertThat(result).isEqualTo("/");
    }

    @Test
    void buildBaseUrl_WhenHostHasNoPath_ShouldAddSlash() {
        // Given
        String host = "http://example.com";

        // When
        String result = clientApiConfig.buildBaseUrl(host);

        // Then
        assertThat(result).isEqualTo("http://example.com/");
    }

    @Test
    void buildBaseUrl_WhenHostHasPath_ShouldPreservePathAndAddSlash() {
        // Given
        String host = "http://example.com/path";

        // When
        String result = clientApiConfig.buildBaseUrl(host);

        // Then
        assertThat(result).isEqualTo("http://example.com/path/");
    }
}
