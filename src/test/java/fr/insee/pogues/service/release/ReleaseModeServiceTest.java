package fr.insee.pogues.service.release;

import fr.insee.pogues.domain.entity.db.ReleaseRequestModeDB;
import fr.insee.pogues.domain.enums.generation.CollectMode;
import fr.insee.pogues.domain.enums.release.ReleaseRequestModeStep;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatus;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatusCode;
import fr.insee.pogues.exception.release.ReleaseRequestNotFoundException;
import fr.insee.pogues.persistence.repository.jpa.ReleaseRequestModeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReleaseModeService")
class ReleaseModeServiceTest {

    @Mock
    private ReleaseRequestModeRepository releaseRequestModeRepository;

    private ReleaseModeService releaseModeService;

    private static final Long MODE_REQUEST_ID = 1L;

    @BeforeEach
    void setUp() {
        releaseModeService = new ReleaseModeService(releaseRequestModeRepository);
    }

    @Nested
    @DisplayName("initModeRequest")
    class InitModeRequest {

        @Test
        @DisplayName("should build a mode request initialized with default status and step")
        void should_build_mode_request_with_default_state() {
            // Given
            CollectMode mode = CollectMode.CAWI;

            // When
            ReleaseRequestModeDB modeDB = ReleaseModeService.initModeRequest(mode);

            // Then
            assertThat(modeDB.getMode()).isEqualTo(mode);
            assertThat(modeDB.getCurrentStep()).isEqualTo(ReleaseRequestModeStep.INIT);
            assertThat(modeDB.getStatus()).isEqualTo(ReleaseRequestStatus.CREATED);
            assertThat(modeDB.getStatusDescription()).isEqualTo(ReleaseRequestStatusCode.NOT_STARTED);
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("should update the status and mark it in progress")
        void should_update_status_and_mark_in_progress() {
            // Given
            ReleaseRequestModeDB requestModeDB = new ReleaseRequestModeDB();
            when(releaseRequestModeRepository.findById(MODE_REQUEST_ID))
                    .thenReturn(Optional.of(requestModeDB));

            // When
            releaseModeService.updateStatus(MODE_REQUEST_ID, ReleaseRequestStatus.CREATED);

            // Then
            ArgumentCaptor<ReleaseRequestModeDB> captor = ArgumentCaptor.forClass(ReleaseRequestModeDB.class);
            verify(releaseRequestModeRepository).save(captor.capture());

            ReleaseRequestModeDB saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(ReleaseRequestStatus.CREATED);
            assertThat(saved.getStatusDescription()).isEqualTo(ReleaseRequestStatusCode.IN_PROGRESS);
        }

        @Test
        @DisplayName("should throw when the release request mode does not exist")
        void should_throw_when_mode_request_not_found() {
            // Given
            when(releaseRequestModeRepository.findById(MODE_REQUEST_ID))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> releaseModeService.updateStatus(MODE_REQUEST_ID, ReleaseRequestStatus.CREATED))
                    .isInstanceOf(ReleaseRequestNotFoundException.class);

            verify(releaseRequestModeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateStep")
    class UpdateStep {

        @Test
        @DisplayName("should update the current step")
        void should_update_current_step() {
            // Given
            ReleaseRequestModeDB requestModeDB = new ReleaseRequestModeDB();
            when(releaseRequestModeRepository.findById(MODE_REQUEST_ID))
                    .thenReturn(Optional.of(requestModeDB));

            // When
            releaseModeService.updateStep(MODE_REQUEST_ID, ReleaseRequestModeStep.GENERATE_LUNATIC);

            // Then
            ArgumentCaptor<ReleaseRequestModeDB> captor = ArgumentCaptor.forClass(ReleaseRequestModeDB.class);
            verify(releaseRequestModeRepository).save(captor.capture());

            assertThat(captor.getValue().getCurrentStep()).isEqualTo(ReleaseRequestModeStep.GENERATE_LUNATIC);
        }

        @Test
        @DisplayName("should throw when the release request mode does not exist")
        void should_throw_when_mode_request_not_found() {
            // Given
            when(releaseRequestModeRepository.findById(MODE_REQUEST_ID))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> releaseModeService.updateStep(MODE_REQUEST_ID, ReleaseRequestModeStep.GENERATE_LUNATIC))
                    .isInstanceOf(ReleaseRequestNotFoundException.class);

            verify(releaseRequestModeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("terminate")
    class Terminate {

        @Test
        @DisplayName("should mark the request as completed with the finished step")
        void should_mark_request_as_completed() {
            // Given
            ReleaseRequestModeDB requestModeDB = new ReleaseRequestModeDB();
            when(releaseRequestModeRepository.findById(MODE_REQUEST_ID))
                    .thenReturn(Optional.of(requestModeDB));

            // When
            releaseModeService.terminate(MODE_REQUEST_ID);

            // Then
            ArgumentCaptor<ReleaseRequestModeDB> captor = ArgumentCaptor.forClass(ReleaseRequestModeDB.class);
            verify(releaseRequestModeRepository).save(captor.capture());

            ReleaseRequestModeDB saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(ReleaseRequestStatus.COMPLETED);
            assertThat(saved.getStatusDescription()).isEqualTo(ReleaseRequestStatusCode.COMPLETED);
            assertThat(saved.getCurrentStep()).isEqualTo(ReleaseRequestModeStep.FINISHED);
        }

        @Test
        @DisplayName("should throw when the release request mode does not exist")
        void should_throw_when_mode_request_not_found() {
            // Given
            when(releaseRequestModeRepository.findById(MODE_REQUEST_ID))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> releaseModeService.terminate(MODE_REQUEST_ID))
                    .isInstanceOf(ReleaseRequestNotFoundException.class);

            verify(releaseRequestModeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateCollectionInstrumentId")
    class UpdateCollectionInstrumentId {

        @Test
        @DisplayName("should update the collection instrument id")
        void should_update_collection_instrument_id() {
            // Given
            UUID collectionInstrumentId = UUID.randomUUID();
            ReleaseRequestModeDB requestModeDB = new ReleaseRequestModeDB();
            when(releaseRequestModeRepository.findById(MODE_REQUEST_ID))
                    .thenReturn(Optional.of(requestModeDB));

            // When
            releaseModeService.updateCollectionInstrumentId(MODE_REQUEST_ID, collectionInstrumentId);

            // Then
            ArgumentCaptor<ReleaseRequestModeDB> captor = ArgumentCaptor.forClass(ReleaseRequestModeDB.class);
            verify(releaseRequestModeRepository).save(captor.capture());

            assertThat(captor.getValue().getCollectionInstrumentId()).isEqualTo(collectionInstrumentId);
        }

        @Test
        @DisplayName("should throw when the release request mode does not exist")
        void should_throw_when_mode_request_not_found() {
            // Given
            when(releaseRequestModeRepository.findById(MODE_REQUEST_ID))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> releaseModeService.updateCollectionInstrumentId(MODE_REQUEST_ID, UUID.randomUUID()))
                    .isInstanceOf(ReleaseRequestNotFoundException.class);

            verify(releaseRequestModeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getReleaseRequestModeDBById")
    class GetReleaseRequestModeDBById {

        @Test
        @DisplayName("should return the release request mode when found")
        void should_return_mode_request_when_found() {
            // Given
            ReleaseRequestModeDB requestModeDB = new ReleaseRequestModeDB();
            when(releaseRequestModeRepository.findById(MODE_REQUEST_ID))
                    .thenReturn(Optional.of(requestModeDB));

            // When
            ReleaseRequestModeDB result = releaseModeService.getReleaseRequestModeDBById(MODE_REQUEST_ID);

            // Then
            assertThat(result).isSameAs(requestModeDB);
        }

        @Test
        @DisplayName("should throw when the release request mode does not exist")
        void should_throw_when_mode_request_not_found() {
            // Given
            when(releaseRequestModeRepository.findById(MODE_REQUEST_ID))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> releaseModeService.getReleaseRequestModeDBById(MODE_REQUEST_ID))
                    .isInstanceOf(ReleaseRequestNotFoundException.class);
        }
    }
}