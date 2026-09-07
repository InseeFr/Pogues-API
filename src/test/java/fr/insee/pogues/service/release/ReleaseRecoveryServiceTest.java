package fr.insee.pogues.service.release;

import fr.insee.pogues.client.surveyregistry.questionnaire.QuestionnaireRegistryClient;
import fr.insee.pogues.client.surveyregistry.model.CollectionInstrumentMetadataDto;
import fr.insee.pogues.domain.entity.db.ReleaseRequestDB;
import fr.insee.pogues.domain.entity.db.ReleaseRequestModeDB;
import fr.insee.pogues.domain.enums.generation.CollectMode;
import fr.insee.pogues.domain.enums.generation.GenerationContext;
import fr.insee.pogues.domain.enums.generation.GenerationQuestionNumberingMode;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatus;
import fr.insee.pogues.exception.questionnaire.QuestionnaireNotFoundException;
import fr.insee.pogues.mapper.ReleaseRequestMapper;
import fr.insee.pogues.model.dto.release.*;
import fr.insee.pogues.persistence.repository.jpa.ReleaseRequestRepository;
import fr.insee.pogues.persistence.service.IQuestionnaireService;
import fr.insee.pogues.service.stub.QuestionnaireServiceStub;
import fr.insee.pogues.service.visualize.VisualizeUriBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("releaseRecoveryService")
class ReleaseRecoveryServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-27T15:49:00Z");

    @Mock
    private ReleaseRequestRepository repository;

    @Mock
    private QuestionnaireRegistryClient registryClient;

    @Mock
    private VisualizeUriBuilder visualizeUriBuilder;

    @Mock
    private ReleaseRequestMapper mapper;

    @Mock
    private IQuestionnaireService questionnaireService;

    private ReleaseRecoveryService releaseRecoveryService;

    private ReleaseRecoveryService newService() {
        return new ReleaseRecoveryService(repository, registryClient, visualizeUriBuilder, mapper, questionnaireService);
    }

    private ReleaseRequestDB aReleaseRequestDB(Long id, ReleaseRequestStatus status) {
        ReleaseRequestDB db = new ReleaseRequestDB();
        db.setId(id);
        db.setStatus(status);
        return db;
    }

    @Nested
    @DisplayName("when retrieving completed releases by pogues id")
    class GetCompletedReleasesByPoguesId {

        private ReleaseRequestModeDB aReleaseRequestMode(CollectMode mode) {
            ReleaseRequestModeDB modeDB = mock(ReleaseRequestModeDB.class);
            when(modeDB.getMode()).thenReturn(mode);
            return modeDB;
        }

        private ReleaseRequestDB aCompletedReleaseRequest(Long id, UUID poguesVersionId, String author,
                                                          GenerationContext context, List<CollectMode> modes) {
            ReleaseRequestDB db = aReleaseRequestDB(id, ReleaseRequestStatus.COMPLETED);
            db.setPoguesVersionId(poguesVersionId);
            db.setRequestedBy(author);
            db.setReleaseDescription("release description");
            db.setContext(context);
            db.setModes(modes.stream().map(this::aReleaseRequestMode).toList());
            return db;
        }

        @Test
        @DisplayName("should return a release dto combining registry metadata and matching release request")
        void should_return_release_combining_registry_and_request_data() {
            // Given
            releaseRecoveryService = newService();
            UUID poguesVersionId = UUID.randomUUID();
            UUID collectionInstrumentId = UUID.randomUUID();

            CollectionInstrumentMetadataDto collectionInstrument = aCollectionInstrument(
                    collectionInstrumentId, poguesVersionId, CollectMode.CAWI, FIXED_INSTANT);

            ReleaseRequestDB releaseRequest = aCompletedReleaseRequest(
                    1L, poguesVersionId, "author", GenerationContext.BUSINESS, List.of(CollectMode.CAWI));
            releaseRequest.setGenerationParamsQuestionNumberingMode(GenerationQuestionNumberingMode.SEQUENCE);
            releaseRequest.setGenerationParamsResponseTimeQuestion(false);

            URI visualizeUri = URI.create("https://example.org/visualize/" + collectionInstrumentId);

            when(registryClient.getCollectionInstrumentByPoguesId("pogues-id", true))
                    .thenReturn(List.of(collectionInstrument));
            when(repository.findWithModesByPoguesIdAndStatus("pogues-id", ReleaseRequestStatus.COMPLETED))
                    .thenReturn(List.of(releaseRequest));
            when(visualizeUriBuilder.buildVisualizeUriFromCollectionInstrument(collectionInstrument))
                    .thenReturn(visualizeUri);

            // When
            List<ReleaseDto> result = releaseRecoveryService.getCompletedReleasesByPoguesId("pogues-id");

            // Then
            assertThat(result).containsExactly(new ReleaseDto(
                    "author",
                    FIXED_INSTANT,
                    poguesVersionId,
                    "release description",
                    GenerationContext.BUSINESS,
                    List.of(new CollectionInstrumentReleaseDto(
                            CollectMode.CAWI,
                            collectionInstrumentId,
                            1L,
                            new OverrideGenerationParameters(GenerationQuestionNumberingMode.SEQUENCE, false),
                            visualizeUri
                    ))
            ));
        }

        @Test
        @DisplayName("should not set override generation parameters when context is not business")
        void should_not_set_override_params_when_context_is_not_business() {
            // Given
            releaseRecoveryService = newService();
            UUID poguesVersionId = UUID.randomUUID();
            UUID collectionInstrumentId = UUID.randomUUID();

            CollectionInstrumentMetadataDto collectionInstrument = aCollectionInstrument(
                    collectionInstrumentId, poguesVersionId, CollectMode.CAWI, FIXED_INSTANT);

            ReleaseRequestDB releaseRequest = aCompletedReleaseRequest(
                    1L, poguesVersionId, "author", GenerationContext.HOUSEHOLD, List.of(CollectMode.CAWI));

            when(registryClient.getCollectionInstrumentByPoguesId("pogues-id", true))
                    .thenReturn(List.of(collectionInstrument));
            when(repository.findWithModesByPoguesIdAndStatus("pogues-id", ReleaseRequestStatus.COMPLETED))
                    .thenReturn(List.of(releaseRequest));
            when(visualizeUriBuilder.buildVisualizeUriFromCollectionInstrument(collectionInstrument))
                    .thenReturn(URI.create("https://example.org/visualize"));

            // When
            List<ReleaseDto> result = releaseRecoveryService.getCompletedReleasesByPoguesId("pogues-id");

            // Then
            assertThat(result)
                    .flatExtracting(ReleaseDto::collectionInstruments)
                    .extracting(CollectionInstrumentReleaseDto::overrideGenerationParameters)
                    .containsOnlyNulls();
        }

        @Test
        @DisplayName("should not set override generation parameters when mode is not CAWI")
        void should_not_set_override_params_when_mode_is_not_cawi() {
            // Given
            releaseRecoveryService = newService();
            UUID poguesVersionId = UUID.randomUUID();
            UUID collectionInstrumentId = UUID.randomUUID();

            CollectionInstrumentMetadataDto collectionInstrument = aCollectionInstrument(
                    collectionInstrumentId, poguesVersionId, CollectMode.CAPI, FIXED_INSTANT);

            ReleaseRequestDB releaseRequest = aCompletedReleaseRequest(
                    1L, poguesVersionId, "author", GenerationContext.BUSINESS, List.of(CollectMode.CAPI));

            when(registryClient.getCollectionInstrumentByPoguesId("pogues-id", true))
                    .thenReturn(List.of(collectionInstrument));
            when(repository.findWithModesByPoguesIdAndStatus("pogues-id", ReleaseRequestStatus.COMPLETED))
                    .thenReturn(List.of(releaseRequest));
            when(visualizeUriBuilder.buildVisualizeUriFromCollectionInstrument(collectionInstrument))
                    .thenReturn(URI.create("https://example.org/visualize"));

            // When
            List<ReleaseDto> result = releaseRecoveryService.getCompletedReleasesByPoguesId("pogues-id");

            // Then
            assertThat(result)
                    .flatExtracting(ReleaseDto::collectionInstruments)
                    .extracting(CollectionInstrumentReleaseDto::overrideGenerationParameters)
                    .containsOnlyNulls();
        }

        @Test
        @DisplayName("should exclude the release request when no matching collection instrument is found")
        void should_exclude_release_request_without_matching_collection_instrument() {
            // Given
            releaseRecoveryService = newService();
            UUID poguesVersionId = UUID.randomUUID();

            ReleaseRequestDB releaseRequest = aCompletedReleaseRequest(
                    1L, poguesVersionId, "author", GenerationContext.BUSINESS, List.of(CollectMode.CAWI));

            when(registryClient.getCollectionInstrumentByPoguesId("pogues-id", true))
                    .thenReturn(List.of());
            when(repository.findWithModesByPoguesIdAndStatus("pogues-id", ReleaseRequestStatus.COMPLETED))
                    .thenReturn(List.of(releaseRequest));

            // When
            List<ReleaseDto> result = releaseRecoveryService.getCompletedReleasesByPoguesId("pogues-id");

            // Then
            assertThat(result).isEmpty();
            verify(visualizeUriBuilder, never()).buildVisualizeUriFromCollectionInstrument(any());
        }

        @Test
        @DisplayName("should return an empty list when there is no completed release request")
        void should_return_empty_list_when_no_completed_request() {
            // Given
            releaseRecoveryService = newService();

            when(registryClient.getCollectionInstrumentByPoguesId("pogues-id", true))
                    .thenReturn(List.of());
            when(repository.findWithModesByPoguesIdAndStatus("pogues-id", ReleaseRequestStatus.COMPLETED))
                    .thenReturn(List.of());

            // When
            List<ReleaseDto> result = releaseRecoveryService.getCompletedReleasesByPoguesId("pogues-id");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should compute the latest release date among several modes of the same request")
        void should_compute_latest_release_date_among_modes() {
            // Given
            releaseRecoveryService = newService();
            UUID poguesVersionId = UUID.randomUUID();
            Instant olderDate = FIXED_INSTANT.minusSeconds(3600);

            CollectionInstrumentMetadataDto cawiInstrument = aCollectionInstrument(
                    UUID.randomUUID(), poguesVersionId, CollectMode.CAWI, olderDate);
            CollectionInstrumentMetadataDto papiInstrument = aCollectionInstrument(
                    UUID.randomUUID(), poguesVersionId, CollectMode.CAPI, FIXED_INSTANT);

            ReleaseRequestDB releaseRequest = aCompletedReleaseRequest(
                    1L, poguesVersionId, "author", GenerationContext.BUSINESS,
                    List.of(CollectMode.CAWI, CollectMode.CAPI));

            when(registryClient.getCollectionInstrumentByPoguesId("pogues-id", true))
                    .thenReturn(List.of(cawiInstrument, papiInstrument));
            when(repository.findWithModesByPoguesIdAndStatus("pogues-id", ReleaseRequestStatus.COMPLETED))
                    .thenReturn(List.of(releaseRequest));
            when(visualizeUriBuilder.buildVisualizeUriFromCollectionInstrument(any()))
                    .thenReturn(URI.create("https://example.org/visualize"));

            // When
            List<ReleaseDto> result = releaseRecoveryService.getCompletedReleasesByPoguesId("pogues-id");

            // Then
            assertThat(result)
                    .extracting(ReleaseDto::releaseDate)
                    .containsExactly(FIXED_INSTANT);
        }

        @Test
        @DisplayName("should match each release request only with its own pogues version id and mode")
        void should_match_correct_release_request_among_several() {
            // Given
            releaseRecoveryService = newService();
            UUID poguesVersionId1 = UUID.randomUUID();
            UUID poguesVersionId2 = UUID.randomUUID();

            CollectionInstrumentMetadataDto instrument1 = aCollectionInstrument(
                    UUID.randomUUID(), poguesVersionId1, CollectMode.CAWI, FIXED_INSTANT);
            CollectionInstrumentMetadataDto instrument2 = aCollectionInstrument(
                    UUID.randomUUID(), poguesVersionId2, CollectMode.CAWI, FIXED_INSTANT);

            ReleaseRequestDB releaseRequest1 = aCompletedReleaseRequest(
                    1L, poguesVersionId1, "author-1", GenerationContext.BUSINESS, List.of(CollectMode.CAWI));
            ReleaseRequestDB releaseRequest2 = aCompletedReleaseRequest(
                    2L, poguesVersionId2, "author-2", GenerationContext.BUSINESS, List.of(CollectMode.CAWI));

            when(registryClient.getCollectionInstrumentByPoguesId("pogues-id", true))
                    .thenReturn(List.of(instrument1, instrument2));
            when(repository.findWithModesByPoguesIdAndStatus("pogues-id", ReleaseRequestStatus.COMPLETED))
                    .thenReturn(List.of(releaseRequest1, releaseRequest2));
            when(visualizeUriBuilder.buildVisualizeUriFromCollectionInstrument(any()))
                    .thenReturn(URI.create("https://example.org/visualize"));

            // When
            List<ReleaseDto> result = releaseRecoveryService.getCompletedReleasesByPoguesId("pogues-id");

            // Then
            assertThat(result)
                    .extracting(ReleaseDto::author)
                    .containsExactly("author-1", "author-2");
        }
    }

    private CollectionInstrumentMetadataDto aCollectionInstrument(
            UUID collectionInstrumentId, UUID poguesVersionId, CollectMode mode, Instant releaseDate) {
        return new CollectionInstrumentMetadataDto(
                collectionInstrumentId, 1L, "releaseDescription",
                releaseDate,
                poguesVersionId, mode, List.of()
        );
    }


    @Nested
    @DisplayName("when retrieving pending release requests by pogues id")
    class GetPendingReleaseRequests {

        @Test
        @DisplayName("should return the mapped dtos for requests not completed")
        void should_return_mapped_pending_requests() {
            // Given
            releaseRecoveryService = newService();
            ReleaseRequestDB pending = aReleaseRequestDB(1L, ReleaseRequestStatus.CREATED);
            ReleaseRequestDto expectedDto = mock(ReleaseRequestDto.class);

            when(repository.findWithModesByPoguesIdAndStatusNot("pogues-id", ReleaseRequestStatus.COMPLETED))
                    .thenReturn(List.of(pending));
            when(mapper.toDto(pending)).thenReturn(expectedDto);

            // When
            List<ReleaseRequestDto> result = releaseRecoveryService.getPendingReleaseRequestsByPoguesId("pogues-id");

            // Then
            assertThat(result).containsExactly(expectedDto);
        }

        @Test
        @DisplayName("should return an empty list when no pending request exists")
        void should_return_empty_list_when_none_pending() {
            // Given
            releaseRecoveryService = newService();
            when(repository.findWithModesByPoguesIdAndStatusNot("pogues-id", ReleaseRequestStatus.COMPLETED))
                    .thenReturn(List.of());

            // When
            List<ReleaseRequestDto> result = releaseRecoveryService.getPendingReleaseRequestsByPoguesId("pogues-id");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("when retrieving pending release requests internally")
    class GetPendingReleaseRequestsInternal {

        @Test
        @DisplayName("should delegate to the repository excluding completed requests")
        void should_delegate_to_repository() {
            // Given
            releaseRecoveryService = newService();
            List<ReleaseRequestDB> expected = List.of(aReleaseRequestDB(1L, ReleaseRequestStatus.CREATED));
            when(repository.findWithModesByPoguesIdAndStatusNot("pogues-id", ReleaseRequestStatus.COMPLETED))
                    .thenReturn(expected);

            // When
            List<ReleaseRequestDB> result = releaseRecoveryService.getPendingReleaseRequestsByPoguesIdInternal("pogues-id");

            // Then
            assertThat(result).isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("should throw QuestionnaireNotFoundException when retrieving pending release requests")
    void should_throw_exception_when_retrieving_pending_requests() {
        // Given
        releaseRecoveryService = newService();
        String poguesId = "unknown-pogues-id";

        doThrow(new QuestionnaireNotFoundException(
                "Questionnaire with id %s does not exist".formatted(poguesId)
        )).when(questionnaireService).ensureExistsById(poguesId);

        // When / Then
        assertThatThrownBy(() ->
                releaseRecoveryService.getPendingReleaseRequestsByPoguesId(poguesId)
        )
                .isInstanceOf(QuestionnaireNotFoundException.class)
                .hasMessage("Questionnaire with id unknown-pogues-id does not exist");

        verify(questionnaireService).ensureExistsById(poguesId);
        verifyNoInteractions(repository, registryClient, mapper, visualizeUriBuilder);
    }

    @Test
    @DisplayName("should throw QuestionnaireNotFoundException when retrieving completed releases")
    void should_throw_exception_when_retrieving_completed_releases() {
        // Given
        releaseRecoveryService = newService();
        String poguesId = "unknown-pogues-id";

        doThrow(new QuestionnaireNotFoundException(
                "Questionnaire with id %s does not exist".formatted(poguesId)
        )).when(questionnaireService).ensureExistsById(poguesId);

        // When / Then
        assertThatThrownBy(() ->
                releaseRecoveryService.getCompletedReleasesByPoguesId(poguesId)
        )
                .isInstanceOf(QuestionnaireNotFoundException.class)
                .hasMessage("Questionnaire with id unknown-pogues-id does not exist");

        verify(questionnaireService).ensureExistsById(poguesId);
        verifyNoInteractions(repository, registryClient, mapper, visualizeUriBuilder);
    }

}