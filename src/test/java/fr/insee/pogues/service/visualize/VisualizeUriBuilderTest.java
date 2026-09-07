package fr.insee.pogues.service.visualize;

import fr.insee.pogues.client.surveyregistry.model.CollectionInstrumentCodesList;
import fr.insee.pogues.client.surveyregistry.model.CollectionInstrumentMetadataDto;
import fr.insee.pogues.configuration.properties.visualize.InterviewerUiVisualizeProperties;
import fr.insee.pogues.configuration.properties.visualize.VisualizeQueryParams;
import fr.insee.pogues.configuration.properties.visualize.WebUiVisualizeProperties;
import fr.insee.pogues.domain.enums.generation.CollectMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VisualizeUriBuilder")
class VisualizeUriBuilderTest {

    private static final UUID COLLECTION_INSTRUMENT_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final InterviewerUiVisualizeProperties interviewerUiProperties =
            new InterviewerUiVisualizeProperties(
                    "https://interviewer.example.com",
                    "/sub-path/interviewer-visualize",
                    new VisualizeQueryParams("questionnaire", "nomenclature")
            );

    private final WebUiVisualizeProperties webUiProperties =
            new WebUiVisualizeProperties(
                    "https://web.example.com",
                    "/visualize",
                    new VisualizeQueryParams("source", "nomenclatures")
            );

    private final VisualizeUriBuilder visualizeUriBuilder =
            new VisualizeUriBuilder(interviewerUiProperties, webUiProperties, "https://registry.example.com");

    @Test
    @DisplayName("should build the URI using the Web UI properties when mode is CAWI")
    void should_build_uri_using_web_ui_properties_when_mode_is_cawi() {
        // Given
        CollectionInstrumentMetadataDto collectionInstrument = aCollectionInstrument(
                CollectMode.CAWI, COLLECTION_INSTRUMENT_ID, List.of());

        // When
        URI result = visualizeUriBuilder.buildVisualizeUriFromCollectionInstrument(collectionInstrument);

        // Then
        assertThat(result)
                .hasScheme("https")
                .hasHost("web.example.com")
                .hasPath("/visualize");
    }

    @Test
    @DisplayName("should use the Web UI query param names when mode is CAWI")
    void should_use_web_ui_query_param_names_when_mode_is_cawi() {
        // Given
        CollectionInstrumentMetadataDto collectionInstrument = aCollectionInstrument(
                CollectMode.CAWI, COLLECTION_INSTRUMENT_ID, List.of());

        // When
        URI result = visualizeUriBuilder.buildVisualizeUriFromCollectionInstrument(collectionInstrument);
        // Then
        assertThat(result.getQuery())
                .contains("source=")
                .contains("nomenclatures=");
    }

    @ParameterizedTest(name = "mode {0} should use the Interviewer UI properties")
    @EnumSource(value = CollectMode.class, names = "CAWI", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("should build the URI using the Interviewer UI properties when mode is not CAWI")
    void should_build_uri_using_interviewer_ui_properties_when_mode_is_not_cawi(CollectMode mode) {
        // Given
        CollectionInstrumentMetadataDto collectionInstrument = aCollectionInstrument(
                mode, COLLECTION_INSTRUMENT_ID, List.of());

        // When
        URI result = visualizeUriBuilder.buildVisualizeUriFromCollectionInstrument(collectionInstrument);

        // Then
        assertThat(result)
                .hasScheme("https")
                .hasHost("interviewer.example.com")
                .hasPath("/sub-path/interviewer-visualize");
    }

    @Test
    @DisplayName("should use the Interviewer UI query param names when mode is not CAWI")
    void should_use_interviewer_ui_query_param_names_when_mode_is_not_cawi() {
        // Given
        CollectionInstrumentMetadataDto collectionInstrument = aCollectionInstrument(
                CollectMode.CAPI, COLLECTION_INSTRUMENT_ID, List.of());

        // When
        URI result = visualizeUriBuilder.buildVisualizeUriFromCollectionInstrument(collectionInstrument);

        // Then
        assertThat(result.getQuery())
                .contains("questionnaire=")
                .contains("nomenclature=");
    }

    @Test
    @DisplayName("should encode the collection instrument id as a collection-instrument path URI")
    void should_encode_collection_instrument_id_as_path_uri() {
        // Given
        CollectionInstrumentMetadataDto collectionInstrument = aCollectionInstrument(
                CollectMode.CAWI, COLLECTION_INSTRUMENT_ID, List.of());

        // When
        URI result = visualizeUriBuilder.buildVisualizeUriFromCollectionInstrument(collectionInstrument);

        // Then
        assertThat(result.getQuery())
                .contains("collection-instruments/" + COLLECTION_INSTRUMENT_ID);
    }

    @Test
    @DisplayName("should include each codes list id and url when codes lists are present")
    void should_include_codes_lists_when_present() {
        // Given
        UUID codesListId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        URI codesListUrl = URI.create("https://nomenclatures.example.com/22222222-2222-2222-2222-222222222222");
        CollectionInstrumentCodesList codesList = new CollectionInstrumentCodesList(codesListId, codesListUrl);
        CollectionInstrumentMetadataDto collectionInstrument = aCollectionInstrument(
                CollectMode.CAWI, COLLECTION_INSTRUMENT_ID, List.of(codesList));

        // When
        URI result = visualizeUriBuilder.buildVisualizeUriFromCollectionInstrument(collectionInstrument);

        // Then
        assertThat(result.toString())
                .contains(codesListId.toString())
                .contains("nomenclatures.example.com")
                .contains("%22") // quotes in JSON encoded
                .doesNotContain("{\"");
    }

    @Test
    @DisplayName("should produce an empty nomenclatures param when there is no codes list")
    void should_produce_empty_param_when_no_codes_list() {
        // Given
        CollectionInstrumentMetadataDto collectionInstrument = aCollectionInstrument(
                CollectMode.CAWI, COLLECTION_INSTRUMENT_ID, List.of());

        // When
        URI result = visualizeUriBuilder.buildVisualizeUriFromCollectionInstrument(collectionInstrument);

        // Then
        assertThat(result.getQuery()).contains("nomenclatures=");
    }

    private CollectionInstrumentMetadataDto aCollectionInstrument(
            CollectMode mode, UUID collectionInstrumentId, List<CollectionInstrumentCodesList> codesLists) {
        return new CollectionInstrumentMetadataDto(
                collectionInstrumentId,
                1L,
                "description",
                Instant.now(),
                UUID.randomUUID(),
                mode, codesLists);
    }
}