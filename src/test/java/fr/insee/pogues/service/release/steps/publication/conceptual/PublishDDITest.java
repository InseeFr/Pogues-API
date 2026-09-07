package fr.insee.pogues.service.release.steps.publication.conceptual;

import fr.insee.pogues.client.surveyregistry.questionnaire.QuestionnaireRegistryClient;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStep;
import fr.insee.pogues.service.release.context.ReleaseWorkflowContext;
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
@DisplayName("PublishDDI")
class PublishDDITest {

    private static final UUID POGUES_VERSION_ID = UUID.randomUUID();
    private static final String DDI = "<DDIInstance>ddi-content</DDIInstance>";

    @Mock
    private QuestionnaireRegistryClient registryClient;

    @Mock
    private ReleaseWorkflowContext workflowContext;

    private PublishDDI publishDDI;

    @BeforeEach
    void setUp() {
        publishDDI = new PublishDDI(registryClient);
    }

    @Test
    @DisplayName("should return PUBLISH_DDI as its step type")
    void should_return_publish_ddi_step_type() {
        // When
        ReleaseRequestStep step = publishDDI.getStep();

        // Then
        assertThat(step).isEqualTo(ReleaseRequestStep.PUBLICATION_DDI);
    }

    @Nested
    @DisplayName("when executing the step")
    class WhenExecuting {

        @Test
        @DisplayName("should publish the DDI in the collection instrument")
        void should_publish_ddi_in_collection_instrument() {
            // Given
            when(workflowContext.getDdi()).thenReturn(DDI);
            when(workflowContext.getPoguesVersionId()).thenReturn(POGUES_VERSION_ID);

            // When
            publishDDI.execute(workflowContext);

            // Then
            verify(registryClient).publishDDIInConceptualModel(POGUES_VERSION_ID, DDI);
        }
    }

    @Nested
    @DisplayName("after executing the step")
    class AfterExecuting {

        @Test
        @DisplayName("should clear the DDI from the workflow context")
        void should_clear_ddi_from_context() {
            // When
            publishDDI.afterExecute(workflowContext);

            // Then
            verify(workflowContext).clearDDI();
            verifyNoMoreInteractions(registryClient);
        }
    }
}