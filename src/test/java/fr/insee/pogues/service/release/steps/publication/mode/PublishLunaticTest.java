package fr.insee.pogues.service.release.steps.publication.mode;

import fr.insee.pogues.client.surveyregistry.questionnaire.QuestionnaireRegistryClient;
import fr.insee.pogues.domain.enums.release.ReleaseRequestModeStep;
import fr.insee.pogues.service.release.context.ReleaseModeWorkflowContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublishLunatic")
class PublishLunaticTest {

    private static final UUID COLLECTION_INSTRUMENT_ID = UUID.randomUUID();
    private static final String LUNATIC = "{\"id\":\"lunatic-questionnaire\"}";

    @Mock
    private QuestionnaireRegistryClient registryClient;

    @Mock
    private ReleaseModeWorkflowContext workflowContext;

    private PublishLunatic publishLunatic;

    @BeforeEach
    void setUp() {
        publishLunatic = new PublishLunatic(registryClient);
    }

    @Test
    @DisplayName("should return PUBLISH_LUNATIC as its step type")
    void should_return_publish_lunatic_step_type() {
        // When
        ReleaseRequestModeStep step = publishLunatic.getStep();

        // Then
        assertThat(step).isEqualTo(ReleaseRequestModeStep.PUBLICATION_LUNATIC);
    }

    @Nested
    @DisplayName("when executing the step")
    class WhenExecuting {

        @Test
        @DisplayName("should publish the lunatic questionnaire in the collection instrument")
        void should_publish_lunatic_in_collection_instrument() {
            // Given
            when(workflowContext.getLunatic()).thenReturn(LUNATIC);
            when(workflowContext.getCollectionInstrumentId()).thenReturn(COLLECTION_INSTRUMENT_ID);

            // When
            publishLunatic.execute(workflowContext);

            // Then
            verify(registryClient).publishLunaticInCollectionInstrument(COLLECTION_INSTRUMENT_ID, LUNATIC);
        }
    }

    @Nested
    @DisplayName("after executing the step")
    class AfterExecuting {

        @Test
        @DisplayName("should clear the lunatic questionnaire from the workflow context")
        void should_clear_lunatic_from_context() {
            // When
            publishLunatic.afterExecute(workflowContext);

            // Then
            verify(workflowContext).clearLunatic();
            verifyNoMoreInteractions(registryClient);
        }
    }
}