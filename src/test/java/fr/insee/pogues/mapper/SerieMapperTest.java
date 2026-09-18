package fr.insee.pogues.mapper;

import fr.insee.pogues.client.metadata.model.magma.fusion.Label;
import fr.insee.pogues.client.metadata.model.magma.fusion.Serie;
import fr.insee.pogues.client.metadata.model.magma.fusion.SerieMetadata;
import fr.insee.pogues.domain.entity.db.InternalSerieDB;
import fr.insee.pogues.model.dto.metadata.SerieDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SerieMapperTest {

    private SerieMapper serieMapper;

    @BeforeEach
    void setUp() {
        serieMapper = new SerieMapper();
    }

    @Test
    @DisplayName("Should return null when the internal series is null")
    void should_return_null_when_internal_series_is_null() {
        // Given
        InternalSerieDB internalSeries = null;

        // When
        SerieDto result = serieMapper.toDto(internalSeries);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should map an internal series to a DTO")
    void should_map_internal_series_to_dto() {
        // Given
        InternalSerieDB internalSeries = new InternalSerieDB(
                "series-1",
                "https://example.com/series-1",
                "French label",
                "Alternative label"
        );

        // When
        SerieDto result = serieMapper.toDto(internalSeries);

        // Then
        assertThat(result)
                .isEqualTo(new SerieDto(
                        "series-1",
                        "https://example.com/series-1",
                        "French label",
                        "Alternative label"
                ));
    }

    @Test
    @DisplayName("Should return null when the RMES series is null")
    void should_return_null_when_rmes_series_is_null() {
        // Given
        Serie rmesSeries = null;

        // When
        SerieDto result = serieMapper.toDto(rmesSeries);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should map an RMES series with its French label")
    void should_map_rmes_series_with_french_label() {
        // Given
        Serie rmesSeries = new Serie(
                "series-1",
                "https://example.com/series-1",
                List.of(
                        new Label("English label", "en"),
                        new Label("French label", "fr")
                )
        );

        // When
        SerieDto result = serieMapper.toDto(rmesSeries);

        // Then
        assertThat(result)
                .isEqualTo(new SerieDto(
                        "series-1",
                        "https://example.com/series-1",
                        "French label",
                        null
                ));
    }

    @Test
    @DisplayName("Should return an empty label when the French label is missing")
    void should_return_empty_label_when_french_label_is_missing() {
        // Given
        Serie rmesSeries = new Serie(
                "series-1",
                "https://example.com/series-1",
                List.of(
                        new Label("English label","en"),
                        new Label("Spanish label", "es")
                )
        );

        // When
        SerieDto result = serieMapper.toDto(rmesSeries);

        // Then
        assertThat(result.label()).isEmpty();
        assertThat(result.altLabel()).isNull();
    }

    @Test
    @DisplayName("Should return null when the series metadata is null")
    void should_return_null_when_series_metadata_is_null() {
        // Given
        SerieMetadata seriesMetadata = null;

        // When
        SerieDto result = serieMapper.toDto(seriesMetadata);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should map series metadata with French labels")
    void should_map_series_metadata_with_french_labels() {
        // Given
        SerieMetadata seriesMetadata = new SerieMetadata(
                "series-1",
                "https://example.com/series-1",
                List.of(
                        new Label("French label", "fr")
                ),
                List.of(
                        new Label("French alternative label", "fr")
                )
        );

        // When
        SerieDto result = serieMapper.toDto(seriesMetadata);

        // Then
        assertThat(result)
                .isEqualTo(new SerieDto(
                        "series-1",
                        "https://example.com/series-1",
                        "French label",
                        "French alternative label"
                ));
    }

    @Test
    @DisplayName("Should return empty labels when French labels are missing")
    void should_return_empty_labels_when_french_labels_are_missing() {
        // Given
        SerieMetadata seriesMetadata = new SerieMetadata(
                "series-1",
                "https://example.com/series-1",
                List.of(
                        new Label("en", "English label")
                ),
                List.of(
                        new Label("en", "English alternative label")
                )
        );

        // When
        SerieDto result = serieMapper.toDto(seriesMetadata);

        // Then
        assertThat(result.label()).isEmpty();
        assertThat(result.altLabel()).isEmpty();
    }
}