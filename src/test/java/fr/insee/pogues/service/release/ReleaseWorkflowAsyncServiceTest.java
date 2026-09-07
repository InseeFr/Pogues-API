package fr.insee.pogues.service.release;

import fr.insee.pogues.domain.entity.db.ReleaseRequestDB;
import fr.insee.pogues.domain.entity.db.ReleaseRequestModeDB;
import fr.insee.pogues.domain.enums.generation.CollectMode;
import fr.insee.pogues.domain.enums.release.ReleaseRequestModeStep;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatus;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatusCode;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStep;
import fr.insee.pogues.exception.PoguesDeserializationException;
import fr.insee.pogues.exception.questionnaire.composition.DeReferencingException;
import fr.insee.pogues.exception.questionnaire.composition.NullReferenceException;
import fr.insee.pogues.exception.release.steps.GenerateDDIException;
import fr.insee.pogues.exception.release.steps.PublishPreReleaseException;
import fr.insee.pogues.model.Questionnaire;
import fr.insee.pogues.persistence.service.IQuestionnaireService;
import fr.insee.pogues.persistence.service.VersionService;
import fr.insee.pogues.service.release.context.ReleaseModeWorkflowContext;
import fr.insee.pogues.service.release.context.ReleaseWorkflowContext;
import fr.insee.pogues.service.release.steps.ReleaseModeWorkflowStep;
import fr.insee.pogues.service.release.steps.ReleaseModeWorkflowStepRegistry;
import fr.insee.pogues.service.release.steps.ReleaseWorkflowStep;
import fr.insee.pogues.service.release.steps.ReleaseWorkflowStepRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReleaseWorkflowAsyncService")
class ReleaseWorkflowAsyncServiceTest {

    @Mock
    private ReleasePublicationService releaseRequestService;

    @Mock
    private ReleaseModeService releaseModeService;

    @Mock
    private IQuestionnaireService questionnaireService;

    @Mock
    private VersionService versionService;

    @Mock
    private ReleaseWorkflowStepRegistry workflowStepRegistry;

    @Mock
    private ReleaseModeWorkflowStepRegistry modeWorkflowStepRegistry;

    @Mock
    private ReleaseRequestDB releaseRequestDB;

    @Mock
    private Questionnaire versionQuestionnaireModel;

    @Mock
    private Questionnaire poguesQuestionnaireWithReferences;

    private ReleaseWorkflowAsyncService releaseWorkflowAsyncService;

    private static final Long RELEASE_REQUEST_ID = 42L;
    private static final UUID POGUES_VERSION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() throws PoguesDeserializationException, DeReferencingException, NullReferenceException {
        releaseWorkflowAsyncService = new ReleaseWorkflowAsyncService(
                releaseRequestService, releaseModeService,
                questionnaireService, versionService,
                workflowStepRegistry, modeWorkflowStepRegistry);
    }

    private void stubRequestBasics() throws PoguesDeserializationException, DeReferencingException, NullReferenceException {
        when(releaseRequestDB.getId()).thenReturn(RELEASE_REQUEST_ID);
        when(releaseRequestDB.getPoguesVersionId()).thenReturn(POGUES_VERSION_ID);
        when(releaseRequestDB.getReleaseDescription()).thenReturn("description");
        when(releaseRequestService.getReleaseRequestWithModesById(RELEASE_REQUEST_ID)).thenReturn(releaseRequestDB);
        when(versionService.getVersionDataQuestionnaireModelByVersionId(POGUES_VERSION_ID))
                .thenReturn(versionQuestionnaireModel);
        when(questionnaireService.getQuestionnaireWithItsReferences(versionQuestionnaireModel))
                .thenReturn(poguesQuestionnaireWithReferences);
    }

    private ReleaseWorkflowStep stepOf(ReleaseRequestStep type) {
        ReleaseWorkflowStep step = mock(ReleaseWorkflowStep.class);
        when(step.getStep()).thenReturn(type);
        return step;
    }

    private ReleaseModeWorkflowStep modeStepOf(ReleaseRequestModeStep type) {
        ReleaseModeWorkflowStep step = mock(ReleaseModeWorkflowStep.class);
        when(step.getStep()).thenReturn(type);
        return step;
    }

    @Nested
    @DisplayName("when the workflow runs successfully")
    class WhenWorkflowSucceeds {

        @BeforeEach
        void setUp() throws PoguesDeserializationException, DeReferencingException, NullReferenceException {
            stubRequestBasics();
        }

        @Test
        @DisplayName("should execute common steps then mode steps in order and terminate the request")
        void should_execute_common_and_mode_steps_then_terminate()  {
            // Given
            when(releaseRequestDB.getModes()).thenReturn(List.of());
            when(workflowStepRegistry.orderedSteps()).thenReturn(List.of());

            // When
            releaseWorkflowAsyncService.publishRelease(RELEASE_REQUEST_ID);

            // Then
            InOrder inOrder = inOrder(releaseRequestService);
            inOrder.verify(releaseRequestService).updateStatus(RELEASE_REQUEST_ID, ReleaseRequestStatus.RUNNING);
            inOrder.verify(releaseRequestService).terminate(RELEASE_REQUEST_ID);
            verify(releaseRequestService, never()).failReleaseRequest(any(), any());
        }

        @Test
        @DisplayName("should execute each common step and mark it as current step before running it")
        void should_execute_each_common_step() {
            // Given
            when(releaseRequestDB.getModes()).thenReturn(List.of());
            ReleaseWorkflowStep step = stepOf(ReleaseRequestStep.GENERATE_DDI);
            when(workflowStepRegistry.orderedSteps()).thenReturn(List.of(step));

            // When
            releaseWorkflowAsyncService.publishRelease(RELEASE_REQUEST_ID);

            // Then
            InOrder inOrder = inOrder(releaseRequestService, step);
            inOrder.verify(releaseRequestService).updateStep(RELEASE_REQUEST_ID, ReleaseRequestStep.GENERATE_DDI);
            inOrder.verify(step).execute(any(ReleaseWorkflowContext.class));
            inOrder.verify(step).afterExecute(any(ReleaseWorkflowContext.class));
        }

        @Test
        @DisplayName("should run mode steps for every mode of the release request")
        void should_execute_mode_steps_for_every_mode() {
            // Given
            ReleaseRequestModeDB mode = mock(ReleaseRequestModeDB.class);
            when(mode.getId()).thenReturn(7L);
            when(mode.getMode()).thenReturn(CollectMode.CAWI);
            when(releaseRequestDB.getModes()).thenReturn(List.of(mode));
            when(workflowStepRegistry.orderedSteps()).thenReturn(List.of());

            ReleaseModeWorkflowStep modeStep = modeStepOf(ReleaseRequestModeStep.GENERATE_LUNATIC);
            when(modeWorkflowStepRegistry.orderedSteps()).thenReturn(List.of(modeStep));

            // When
            releaseWorkflowAsyncService.publishRelease(RELEASE_REQUEST_ID);

            // Then
            InOrder inOrder = inOrder(releaseModeService, modeStep);
            inOrder.verify(releaseModeService).updateStatus(7L, ReleaseRequestStatus.RUNNING);
            inOrder.verify(releaseModeService).updateStep(7L, ReleaseRequestModeStep.GENERATE_LUNATIC);
            inOrder.verify(modeStep).execute(any(ReleaseModeWorkflowContext.class));
            inOrder.verify(modeStep).afterExecute(any(ReleaseModeWorkflowContext.class));
            inOrder.verify(releaseModeService).terminate(7L);
        }

        @Test
        @DisplayName("should update the collection instrument id after the pre-release publication step")
        void should_update_collection_instrument_id_after_pre_release_step() {
            // Given
            ReleaseRequestModeDB mode = mock(ReleaseRequestModeDB.class);
            when(mode.getId()).thenReturn(7L);
            when(mode.getMode()).thenReturn(CollectMode.CAWI);
            when(releaseRequestDB.getModes()).thenReturn(List.of(mode));
            when(workflowStepRegistry.orderedSteps()).thenReturn(List.of());

            UUID collectionInstrumentId = UUID.randomUUID();
            ReleaseModeWorkflowStep preReleaseStep = modeStepOf(ReleaseRequestModeStep.PUBLICATION_PRERELEASE);
            doAnswer(invocation -> {
                ReleaseModeWorkflowContext context = invocation.getArgument(0);
                context.setCollectionInstrumentId(collectionInstrumentId);
                return null;
            }).when(preReleaseStep).execute(any(ReleaseModeWorkflowContext.class));
            when(modeWorkflowStepRegistry.orderedSteps()).thenReturn(List.of(preReleaseStep));

            // When
            releaseWorkflowAsyncService.publishRelease(RELEASE_REQUEST_ID);

            // Then
            verify(releaseModeService).updateCollectionInstrumentId(7L, collectionInstrumentId);
        }

        @Test
        @DisplayName("should not update the collection instrument id for other steps")
        void should_not_update_collection_instrument_id_for_other_steps() {
            // Given
            ReleaseRequestModeDB mode = mock(ReleaseRequestModeDB.class);
            when(mode.getId()).thenReturn(7L);
            when(mode.getMode()).thenReturn(CollectMode.CAWI);
            when(releaseRequestDB.getModes()).thenReturn(List.of(mode));
            when(workflowStepRegistry.orderedSteps()).thenReturn(List.of());

            ReleaseModeWorkflowStep modeStep = modeStepOf(ReleaseRequestModeStep.GENERATE_LUNATIC);
            when(modeWorkflowStepRegistry.orderedSteps()).thenReturn(List.of(modeStep));

            // When
            releaseWorkflowAsyncService.publishRelease(RELEASE_REQUEST_ID);

            // Then
            verify(releaseModeService, never()).updateCollectionInstrumentId(any(), any());
        }
    }

    @Nested
    @DisplayName("when the workflow fails")
    class WhenWorkflowFails {


        @BeforeEach
        void setUp() throws PoguesDeserializationException, DeReferencingException, NullReferenceException {
            stubRequestBasics();
        }

        @Test
        @DisplayName("should fail the release request with the mapped status code when a known step exception occurs")
        void should_fail_release_request_with_mapped_status_code() {
            // Given
            ReleaseWorkflowStep step = stepOf(ReleaseRequestStep.GENERATE_DDI);
            doThrow(new GenerateDDIException("boom")).when(step).execute(any(ReleaseWorkflowContext.class));
            when(workflowStepRegistry.orderedSteps()).thenReturn(List.of(step));

            // When
            releaseWorkflowAsyncService.publishRelease(RELEASE_REQUEST_ID);

            // Then
            verify(releaseRequestService).failReleaseRequest(RELEASE_REQUEST_ID, ReleaseRequestStatusCode.ERROR_GENERATION_DDI);
            verify(releaseRequestService, never()).terminate(any());
        }

        @Test
        @DisplayName("should fail the release request with default status code when the exception is unknown")
        void should_fail_release_request_with_default_status_code_for_unknown_exception() {
            // Given
            ReleaseWorkflowStep step = stepOf(ReleaseRequestStep.GENERATE_DDI);
            doThrow(new RuntimeException("unexpected")).when(step).execute(any(ReleaseWorkflowContext.class));
            when(workflowStepRegistry.orderedSteps()).thenReturn(List.of(step));

            // When
            releaseWorkflowAsyncService.publishRelease(RELEASE_REQUEST_ID);

            // Then
            verify(releaseRequestService).failReleaseRequest(RELEASE_REQUEST_ID, ReleaseRequestStatusCode.DEFAULT_FAILURE_STATUS_CODE);
        }

        @Test
        @DisplayName("should fail the release request when a mode step exception occurs")
        void should_fail_release_request_when_mode_step_fails() {
            // Given
            ReleaseRequestModeDB mode = mock(ReleaseRequestModeDB.class);
            when(mode.getId()).thenReturn(7L);
            when(mode.getMode()).thenReturn(CollectMode.CAWI);
            when(releaseRequestDB.getModes()).thenReturn(List.of(mode));
            when(workflowStepRegistry.orderedSteps()).thenReturn(List.of());

            ReleaseModeWorkflowStep modeStep = modeStepOf(ReleaseRequestModeStep.PUBLICATION_PRERELEASE);
            doThrow(new PublishPreReleaseException("registry down"))
                    .when(modeStep).execute(any(ReleaseModeWorkflowContext.class));
            when(modeWorkflowStepRegistry.orderedSteps()).thenReturn(List.of(modeStep));

            // When
            releaseWorkflowAsyncService.publishRelease(RELEASE_REQUEST_ID);

            // Then
            verify(releaseRequestService).failReleaseRequest(RELEASE_REQUEST_ID, ReleaseRequestStatusCode.ERROR_PUBLICATION_PRERELEASE);
            verify(releaseModeService, never()).terminate(7L);
        }
    }

    @Nested
    @DisplayName("handleWorkflowFailure")
    class HandleWorkflowFailure {

        @Test
        @DisplayName("should map the exception type to its dedicated failure status code")
        void should_map_known_exception_to_failure_status_code() {
            // When
            releaseWorkflowAsyncService.handleWorkflowFailure(RELEASE_REQUEST_ID, new GenerateDDIException("boom"));

            // Then
            verify(releaseRequestService).failReleaseRequest(RELEASE_REQUEST_ID, ReleaseRequestStatusCode.ERROR_GENERATION_DDI);
        }

        @Test
        @DisplayName("should use the default failure status code for an unmapped exception")
        void should_use_default_status_code_for_unmapped_exception() {
            // When
            releaseWorkflowAsyncService.handleWorkflowFailure(RELEASE_REQUEST_ID, new RuntimeException("unexpected"));

            // Then
            verify(releaseRequestService).failReleaseRequest(RELEASE_REQUEST_ID, ReleaseRequestStatusCode.DEFAULT_FAILURE_STATUS_CODE);
        }
    }
}