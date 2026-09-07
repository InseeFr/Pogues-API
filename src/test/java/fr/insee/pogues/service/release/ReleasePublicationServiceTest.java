package fr.insee.pogues.service.release;

import fr.insee.pogues.domain.entity.db.ReleaseRequestDB;
import fr.insee.pogues.domain.entity.db.Version;
import fr.insee.pogues.domain.enums.generation.CollectMode;
import fr.insee.pogues.domain.enums.generation.GenerationContext;
import fr.insee.pogues.domain.enums.generation.GenerationQuestionNumberingMode;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatus;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatusCode;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStep;
import fr.insee.pogues.exception.release.ReleaseRequestNotFoundException;
import fr.insee.pogues.exception.release.ReleaseRequestAlreadyExists;
import fr.insee.pogues.exception.release.ReleaseRequestStatusException;
import fr.insee.pogues.mapper.ReleaseRequestMapper;
import fr.insee.pogues.model.dto.release.*;
import fr.insee.pogues.persistence.repository.jpa.ReleaseRequestRepository;
import fr.insee.pogues.persistence.service.VersionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReleaseService")
class ReleasePublicationServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-27T15:49:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @Mock
    private VersionService versionService;

    @Mock
    private ReleaseRequestMapper mapper;

    @Mock
    private ReleaseRequestRepository repository;


    private ReleasePublicationService releasePublicationService;

    private ReleasePublicationService newService() {
        return new ReleasePublicationService(versionService, mapper, repository, FIXED_CLOCK);
    }

    private CreateReleaseRequestDto aCreateReleaseRequestDto() {
        return new CreateReleaseRequestDto(
                "pogues-id",
                "release description",
                List.of(CollectMode.CAWI),
                GenerationContext.BUSINESS,
                new OverrideGenerationParameters(GenerationQuestionNumberingMode.SEQUENCE, false)
        );
    }

    private Version aVersion(UUID versionId) {
        return new Version(versionId, "pogues-id", ZonedDateTime.now(FIXED_CLOCK), null, null, "author");
    }

    private ReleaseRequestDB aReleaseRequestDB(Long id, ReleaseRequestStatus status) {
        ReleaseRequestDB db = new ReleaseRequestDB();
        db.setId(id);
        db.setStatus(status);
        return db;
    }

    @Nested
    @DisplayName("when initializing a release request")
    class InitReleaseRequest {

        @Test
        @DisplayName("should create the request from the last non-provisional version and return the mapped dto")
        void should_create_release_request_from_last_version() {
            // Given
            releasePublicationService = newService();
            UUID poguesVersionId = UUID.randomUUID();
            CreateReleaseRequestDto createDto = aCreateReleaseRequestDto();
            Version lastVersion = aVersion(poguesVersionId);
            ReleaseRequestDB savedDb = aReleaseRequestDB(1L, ReleaseRequestStatus.CREATED);
            ReleaseRequestDto expectedDto = new ReleaseRequestDto(
                    1L, "author", FIXED_INSTANT, ReleaseRequestStatus.CREATED,
                    ReleaseRequestStatusCode.NOT_STARTED.name(), poguesVersionId, "pogues-id",
                    "release description", List.of(CollectMode.CAWI), GenerationContext.BUSINESS,
                    createDto.overrideGenerationParameters()
            );

            when(versionService.getLastVersionByQuestionnaireId("pogues-id", false)).thenReturn(lastVersion);
            when(repository.existsByPoguesVersionId(poguesVersionId)).thenReturn(false);
            when(repository.save(any(ReleaseRequestDB.class))).thenReturn(savedDb);
            when(mapper.toDto(savedDb)).thenReturn(expectedDto);

            // When
            ReleaseRequestDto result = releasePublicationService.initReleaseRequest(createDto, "author");

            // Then
            assertThat(result).isEqualTo(expectedDto);
        }

        @Test
        @DisplayName("should throw when a release request already exists for the resolved version")
        void should_throw_when_release_request_already_exists() {
            // Given
            releasePublicationService = newService();
            UUID poguesVersionId = UUID.randomUUID();
            CreateReleaseRequestDto createDto = aCreateReleaseRequestDto();

            when(versionService.getLastVersionByQuestionnaireId("pogues-id", false))
                    .thenReturn(aVersion(poguesVersionId));
            when(repository.existsByPoguesVersionId(poguesVersionId)).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> releasePublicationService.initReleaseRequest(createDto, "author"))
                    .isInstanceOf(ReleaseRequestAlreadyExists.class);
        }
    }

    @Nested
    @DisplayName("when creating a release request")
    class CreateReleaseRequest {

        @Test
        @DisplayName("should populate all fields and persist the request with initial status and step")
        void should_populate_and_save_release_request() {
            // Given
            releasePublicationService = newService();
            UUID poguesVersionId = UUID.randomUUID();
            CreateReleaseRequestDto createDto = aCreateReleaseRequestDto();

            when(repository.existsByPoguesVersionId(poguesVersionId)).thenReturn(false);
            when(repository.save(any(ReleaseRequestDB.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ArgumentCaptor<ReleaseRequestDB> captor = ArgumentCaptor.forClass(ReleaseRequestDB.class);

            // When
            ReleaseRequestDB result = releasePublicationService.createReleaseRequest(createDto, poguesVersionId, "author");

            // Then
            verify(repository).save(captor.capture());
            ReleaseRequestDB saved = captor.getValue();

            assertThat(saved.getRequestedAt()).isEqualTo(FIXED_INSTANT);
            assertThat(saved.getPoguesId()).isEqualTo("pogues-id");
            assertThat(saved.getPoguesVersionId()).isEqualTo(poguesVersionId);
            assertThat(saved.getRequestedBy()).isEqualTo("author");
            assertThat(saved.getReleaseDescription()).isEqualTo("release description");
            assertThat(saved.getCurrentStep()).isEqualTo(ReleaseRequestStep.INIT);
            assertThat(saved.getStatus()).isEqualTo(ReleaseRequestStatus.CREATED);
            assertThat(saved.getStatusDescription()).isEqualTo(ReleaseRequestStatusCode.NOT_STARTED);
            assertThat(result).isSameAs(saved);
        }

        @Test
        @DisplayName("should leave generation parameters unset when override generation parameters is null")
        void should_not_set_generation_parameters_when_override_is_null() {
            // Given
            releasePublicationService = newService();
            UUID poguesVersionId = UUID.randomUUID();
            CreateReleaseRequestDto createDto = new CreateReleaseRequestDto(
                    "pogues-id",
                    "release description",
                    List.of(CollectMode.CAWI),
                    GenerationContext.BUSINESS,
                    null
            );

            when(repository.existsByPoguesVersionId(poguesVersionId)).thenReturn(false);
            when(repository.save(any(ReleaseRequestDB.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ArgumentCaptor<ReleaseRequestDB> captor = ArgumentCaptor.forClass(ReleaseRequestDB.class);

            // When
            releasePublicationService.createReleaseRequest(createDto, poguesVersionId, "author");

            // Then
            verify(repository).save(captor.capture());
            ReleaseRequestDB saved = captor.getValue();

            assertThat(saved.getGenerationParamsQuestionNumberingMode()).isNull();
            assertThat(saved.getGenerationParamsResponseTimeQuestion()).isNull();
        }

        @Test
        @DisplayName("should throw when a release request already exists for this pogues version id")
        void should_throw_when_already_exists() {
            // Given
            releasePublicationService = newService();
            UUID poguesVersionId = UUID.randomUUID();
            CreateReleaseRequestDto createDto = aCreateReleaseRequestDto();

            when(repository.existsByPoguesVersionId(poguesVersionId)).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> releasePublicationService.createReleaseRequest(createDto, poguesVersionId, "author"))
                    .isInstanceOf(ReleaseRequestAlreadyExists.class)
                    .hasMessageContaining(poguesVersionId.toString());

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("when retrieving a release request by id")
    class GetReleaseRequestById {

        @Test
        @DisplayName("should return the release request when it exists")
        void should_return_release_request_when_found() {
            // Given
            releasePublicationService = newService();
            ReleaseRequestDB expected = aReleaseRequestDB(1L, ReleaseRequestStatus.CREATED);
            when(repository.findById(1L)).thenReturn(Optional.of(expected));

            // When
            ReleaseRequestDB result = releasePublicationService.getReleaseRequestById(1L);

            // Then
            assertThat(result).isSameAs(expected);
        }

        @Test
        @DisplayName("should throw when the release request does not exist")
        void should_throw_when_not_found() {
            // Given
            releasePublicationService = newService();
            when(repository.findById(1L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> releasePublicationService.getReleaseRequestById(1L))
                    .isInstanceOf(ReleaseRequestNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("when updating the current step")
    class UpdateStep {

        @Test
        @DisplayName("should update the current step and persist the release request")
        void should_update_step() {
            // Given
            releasePublicationService = newService();
            ReleaseRequestDB releaseRequest = aReleaseRequestDB(1L, ReleaseRequestStatus.CREATED);
            when(repository.findById(1L)).thenReturn(Optional.of(releaseRequest));

            // When
            releasePublicationService.updateStep(1L, ReleaseRequestStep.GENERATE_DDI);

            // Then
            assertThat(releaseRequest.getCurrentStep()).isEqualTo(ReleaseRequestStep.GENERATE_DDI);
            verify(repository).save(releaseRequest);
        }

        @Test
        @DisplayName("should throw when the release request does not exist")
        void should_throw_when_not_found() {
            // Given
            releasePublicationService = newService();
            when(repository.findById(1L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> releasePublicationService.updateStep(1L, ReleaseRequestStep.GENERATE_DDI))
                    .isInstanceOf(ReleaseRequestNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("when updating the status")
    class UpdateStatus {

        @Test
        @DisplayName("should update status and set status description to in progress")
        void should_update_status_to_in_progress() {
            // Given
            releasePublicationService = newService();
            ReleaseRequestDB releaseRequest = aReleaseRequestDB(1L, ReleaseRequestStatus.CREATED);
            when(repository.findById(1L)).thenReturn(Optional.of(releaseRequest));

            // When
            releasePublicationService.updateStatus(1L, ReleaseRequestStatus.RUNNING);

            // Then
            assertThat(releaseRequest.getStatus()).isEqualTo(ReleaseRequestStatus.RUNNING);
            assertThat(releaseRequest.getStatusDescription()).isEqualTo(ReleaseRequestStatusCode.IN_PROGRESS);
            verify(repository).save(releaseRequest);
        }
    }

    @Nested
    @DisplayName("when marking a release request as failed")
    class FailReleaseRequest {

        @Test
        @DisplayName("should set status to failed with the given reason code")
        void should_mark_as_failed() {
            // Given
            releasePublicationService = newService();
            ReleaseRequestDB releaseRequest = aReleaseRequestDB(1L, ReleaseRequestStatus.RUNNING);
            when(repository.findById(1L)).thenReturn(Optional.of(releaseRequest));

            // When
            releasePublicationService.failReleaseRequest(1L, ReleaseRequestStatusCode.ERROR_GENERATION_DDI);

            // Then
            assertThat(releaseRequest.getStatus()).isEqualTo(ReleaseRequestStatus.FAILED);
            assertThat(releaseRequest.getStatusDescription()).isEqualTo(ReleaseRequestStatusCode.ERROR_GENERATION_DDI);
            verify(repository).save(releaseRequest);
        }
    }

    @Nested
    @DisplayName("when terminating a release request")
    class TerminateReleaseRequest {

        @Test
        @DisplayName("should mark the request as completed and finished")
        void should_mark_as_completed_and_finished() {
            // Given
            releasePublicationService = newService();
            ReleaseRequestDB releaseRequest = aReleaseRequestDB(1L, ReleaseRequestStatus.RUNNING);
            when(repository.findById(1L)).thenReturn(Optional.of(releaseRequest));

            // When
            releasePublicationService.terminate(1L);

            // Then
            assertThat(releaseRequest.getStatus()).isEqualTo(ReleaseRequestStatus.COMPLETED);
            assertThat(releaseRequest.getStatusDescription()).isEqualTo(ReleaseRequestStatusCode.COMPLETED);
            assertThat(releaseRequest.getCurrentStep()).isEqualTo(ReleaseRequestStep.FINISHED);
            verify(repository).save(releaseRequest);
        }
    }



    @Nested
    @DisplayName("when deleting a failed release request")
    class DeleteFailedReleaseRequest {

        @Test
        @DisplayName("should delete the request when its status is failed")
        void should_delete_when_status_is_failed() {
            // Given
            releasePublicationService = newService();
            ReleaseRequestDB releaseRequest = aReleaseRequestDB(1L, ReleaseRequestStatus.FAILED);
            when(repository.findById(1L)).thenReturn(Optional.of(releaseRequest));

            // When
            releasePublicationService.deleteFailedReleaseRequestsById(1L);

            // Then
            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("should throw when status is not failed")
        void should_throw_when_status_is_not_failed() {
            // Given
            releasePublicationService = newService();
            ReleaseRequestDB releaseRequest = aReleaseRequestDB(1L, ReleaseRequestStatus.RUNNING);
            when(repository.findById(1L)).thenReturn(Optional.of(releaseRequest));

            // When / Then
            assertThatThrownBy(() -> releasePublicationService.deleteFailedReleaseRequestsById(1L))
                    .isInstanceOf(ReleaseRequestStatusException.class)
                    .hasMessageContaining(ReleaseRequestStatus.RUNNING.toString());

            verify(repository, never()).deleteById(any());
        }

        @Test
        @DisplayName("should throw when the release request does not exist")
        void should_throw_when_not_found() {
            // Given
            releasePublicationService = newService();
            when(repository.findById(1L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> releasePublicationService.deleteFailedReleaseRequestsById(1L))
                    .isInstanceOf(ReleaseRequestNotFoundException.class);

            verify(repository, never()).deleteById(any());
        }
    }
}