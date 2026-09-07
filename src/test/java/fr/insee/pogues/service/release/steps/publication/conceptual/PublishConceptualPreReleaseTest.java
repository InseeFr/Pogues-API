package fr.insee.pogues.service.release.steps.publication.conceptual;

import fr.insee.pogues.client.surveyregistry.questionnaire.QuestionnaireRegistryClient;
import fr.insee.pogues.client.surveyregistry.model.ConceptualModelDto;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStep;
import fr.insee.pogues.exception.release.steps.PublishPreReleaseException;
import fr.insee.pogues.model.DataCollection;
import fr.insee.pogues.model.Questionnaire;
import fr.insee.pogues.model.Serie;
import fr.insee.pogues.service.release.context.ReleaseWorkflowContext;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublishConceptualPreRelease")
class PublishConceptualPreReleaseTest {

    @Mock
    private QuestionnaireRegistryClient registryClient;

    private PublishConceptualPreRelease publishConceptualPreRelease;

    private ReleaseWorkflowContext workflowContext;

    private final UUID poguesVersionId = UUID.randomUUID();
    private final String poguesId = "questionnaire-id";
    private final String serieId = "serie-id";

    @BeforeEach
    void setUp() {
        publishConceptualPreRelease = new PublishConceptualPreRelease(registryClient);

        Serie serie = new Serie();
        serie.setId(serieId);

        DataCollection dataCollection = new DataCollection();
        dataCollection.setSerie(serie);

        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setId(poguesId);
        questionnaire.setDataCollection(dataCollection);

        workflowContext = new ReleaseWorkflowContext(
                1L,
                poguesVersionId,
                "releaseDescription",
                questionnaire,
                null
        );
    }

    @Test
    @DisplayName("should return the PUBLICATION_PRERELEASE step type")
    void should_return_publication_prerelease_step_type() {
        // When
        ReleaseRequestStep step = publishConceptualPreRelease.getStep();

        // Then
        assertThat(step).isEqualTo(ReleaseRequestStep.PUBLICATION_CONCEPTUAL_PRERELEASE);
    }

    @Nested
    @DisplayName("when no conceptual model exists for the pogues version")
    class WhenConceptualModelDoesNotExist {

        @Test
        @DisplayName("should create the conceptual model with the questionnaire and serie identifiers")
        void should_create_conceptual_model_with_expected_data() {
            // Given
            when(registryClient.findConceptualModelByPoguesVersionId(poguesVersionId))
                    .thenReturn(Optional.empty());

            ArgumentCaptor<ConceptualModelDto> captor = ArgumentCaptor.forClass(ConceptualModelDto.class);

            // When
            publishConceptualPreRelease.execute(workflowContext);

            // Then
            verify(registryClient).createConceptualModel(captor.capture());
            ConceptualModelDto createdModel = captor.getValue();

            assertThat(createdModel.poguesId()).isEqualTo(poguesId);
            assertThat(createdModel.serieId()).isEqualTo(serieId);
            assertThat(createdModel.poguesVersionId()).isEqualTo(poguesVersionId);
        }
    }

    @Nested
    @DisplayName("when a conceptual model already exists for the pogues version")
    class WhenConceptualModelAlreadyExists {

        @Test
        @DisplayName("should not create a new conceptual model")
        void should_not_create_conceptual_model() {
            // Given
            when(registryClient.findConceptualModelByPoguesVersionId(poguesVersionId))
                    .thenReturn(Optional.of(new ConceptualModelDto(poguesId, serieId, poguesVersionId)));

            // When
            publishConceptualPreRelease.execute(workflowContext);

            // Then
            verify(registryClient, never()).createConceptualModel(any(ConceptualModelDto.class));
        }
    }

    @Nested
    @DisplayName("when the registry client fails")
    class WhenRegistryClientFails {

        @Test
        @DisplayName("should wrap the lookup failure into a PublishPreReleaseException")
        void should_wrap_lookup_failure_into_publish_pre_release_exception() {
            // Given
            when(registryClient.findConceptualModelByPoguesVersionId(poguesVersionId))
                    .thenThrow(new RuntimeException("registry unavailable"));

            // When / Then
            assertThatThrownBy(() -> publishConceptualPreRelease.execute(workflowContext))
                    .isInstanceOf(PublishPreReleaseException.class)
                    .hasMessageContaining("registry unavailable");
        }

        @Test
        @DisplayName("should wrap the creation failure into a PublishPreReleaseException")
        void should_wrap_creation_failure_into_publish_pre_release_exception() {
            // Given
            when(registryClient.findConceptualModelByPoguesVersionId(poguesVersionId))
                    .thenReturn(Optional.empty());
            doThrow(new RuntimeException("creation failed"))
                    .when(registryClient).createConceptualModel(any(ConceptualModelDto.class));

            // When / Then
            assertThatThrownBy(() -> publishConceptualPreRelease.execute(workflowContext))
                    .isInstanceOf(PublishPreReleaseException.class)
                    .hasMessageContaining("creation failed");
        }
    }
}