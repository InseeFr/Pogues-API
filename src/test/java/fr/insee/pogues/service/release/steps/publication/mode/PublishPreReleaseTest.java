package fr.insee.pogues.service.release.steps.publication.mode;

import fr.insee.pogues.client.surveyregistry.questionnaire.QuestionnaireRegistryClient;
import fr.insee.pogues.client.surveyregistry.model.CollectionInstrumentCreateDto;
import fr.insee.pogues.domain.enums.generation.CollectMode;
import fr.insee.pogues.domain.enums.generation.GenerationContext;
import fr.insee.pogues.domain.enums.generation.GenerationFormat;
import fr.insee.pogues.domain.enums.generation.GenerationQuestionNumberingMode;
import fr.insee.pogues.domain.enums.generation.parameters.GenerationParameters;
import fr.insee.pogues.domain.enums.release.ReleaseRequestModeStep;
import fr.insee.pogues.exception.release.steps.PublishPreReleaseException;
import fr.insee.pogues.model.Questionnaire;
import fr.insee.pogues.service.release.context.ReleaseModeWorkflowContext;
import fr.insee.pogues.service.release.context.ReleaseWorkflowContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublishPreRelease")
class PublishPreReleaseTest {

    @Mock
    private QuestionnaireRegistryClient registryClient;

    private PublishPreRelease publishPreRelease;

    private ReleaseModeWorkflowContext modeWorkflowContext;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        publishPreRelease = new PublishPreRelease(registryClient);

        GenerationParameters commonParameters = GenerationParameters.builder()
                .context(GenerationContext.HOUSEHOLD)
                .outFormat(GenerationFormat.LUNATIC)
                .modeParameter(CollectMode.CAWI)
                .responseTimeQuestion(true)
                .questionNumberingMode(GenerationQuestionNumberingMode.SEQUENCE)
                .build();

        ReleaseWorkflowContext workflowContext = new ReleaseWorkflowContext(
                1L,
                UUID.randomUUID(),
                "releaseDescription",
                new Questionnaire(),
                commonParameters
        );

        modeWorkflowContext = new ReleaseModeWorkflowContext(workflowContext, 1L, CollectMode.CAWI);
    }

    @Test
    @DisplayName("should return the PUBLICATION_PRERELEASE step type")
    void should_return_publication_prerelease_step_type() {
        // When
        ReleaseRequestModeStep step = publishPreRelease.getStep();

        // Then
        assertThat(step).isEqualTo(ReleaseRequestModeStep.PUBLICATION_PRERELEASE);
    }

    @Nested
    @DisplayName("when the registry client successfully creates the collection instrument")
    class WhenCollectionInstrumentCreationSucceeds {

        @Test
        @DisplayName("should set the collection instrument id returned by the registry on the context")
        void should_set_collection_instrument_id_on_context() {
            // Given
            UUID collectionInstrumentId = UUID.randomUUID();
            when(registryClient.createCollectionInstrumentMetadata(any(CollectionInstrumentCreateDto.class)))
                    .thenReturn(collectionInstrumentId);

            // When
            publishPreRelease.execute(modeWorkflowContext);

            // Then
            assertThat(modeWorkflowContext.getCollectionInstrumentId()).isEqualTo(collectionInstrumentId);
        }

        @Test
        @DisplayName("should call the registry client with a request built from the workflow context")
        void should_call_registry_client_with_expected_request() {
            // Given
            UUID collectionInstrumentId = UUID.randomUUID();
            when(registryClient.createCollectionInstrumentMetadata(any(CollectionInstrumentCreateDto.class)))
                    .thenReturn(collectionInstrumentId);

            ArgumentCaptor<CollectionInstrumentCreateDto> captor = ArgumentCaptor.forClass(CollectionInstrumentCreateDto.class);

            // When
            publishPreRelease.execute(modeWorkflowContext);

            // Then
            verify(registryClient).createCollectionInstrumentMetadata(captor.capture());
            CollectionInstrumentCreateDto sentRequest = captor.getValue();

            assertThat(sentRequest.poguesVersionId())
                    .isEqualTo(modeWorkflowContext.getReleaseContext().getPoguesVersionId());
            assertThat(sentRequest.releaseDescription())
                    .isEqualTo(modeWorkflowContext.getReleaseContext().getReleaseDescription());
            assertThat(sentRequest.mode())
                    .isEqualTo(modeWorkflowContext.getMode());
            assertThat(sentRequest.generationParameters())
                    .isEqualTo(modeWorkflowContext.getGenerationParameters());
        }
    }

    @Nested
    @DisplayName("when the registry client fails to create the collection instrument")
    class WhenCollectionInstrumentCreationFails {

        @Test
        @DisplayName("should wrap the failure into a PublishPreReleaseException")
        void should_throw_publish_pre_release_exception_on_failure() {
            // Given
            when(registryClient.createCollectionInstrumentMetadata(any(CollectionInstrumentCreateDto.class)))
                    .thenThrow(new RuntimeException("registry unavailable"));

            // When / Then
            assertThatThrownBy(() -> publishPreRelease.execute(modeWorkflowContext))
                    .isInstanceOf(PublishPreReleaseException.class)
                    .hasMessageContaining("registry unavailable");
        }

        @Test
        @DisplayName("should not set a collection instrument id on the context")
        void should_not_set_collection_instrument_id_on_failure() {
            // Given
            when(registryClient.createCollectionInstrumentMetadata(any(CollectionInstrumentCreateDto.class)))
                    .thenThrow(new RuntimeException("registry unavailable"));

            // When
            try {
                publishPreRelease.execute(modeWorkflowContext);
            } catch (PublishPreReleaseException ignored) {
                // expected, assertion below focuses on the context state
            }

            // Then
            assertThat(modeWorkflowContext.getCollectionInstrumentId()).isNull();
        }
    }
}