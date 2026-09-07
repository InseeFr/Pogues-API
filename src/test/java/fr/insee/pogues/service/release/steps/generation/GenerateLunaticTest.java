package fr.insee.pogues.service.release.steps.generation;

import fr.insee.pogues.client.generation.EnoClient;
import fr.insee.pogues.domain.enums.generation.parameters.GenerationParameters;
import fr.insee.pogues.domain.enums.release.ReleaseRequestModeStep;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStep;
import fr.insee.pogues.exception.PoguesSerializationException;
import fr.insee.pogues.exception.release.steps.GenerateLunaticException;
import fr.insee.pogues.model.Questionnaire;
import fr.insee.pogues.service.release.context.ReleaseModeWorkflowContext;
import fr.insee.pogues.service.release.context.ReleaseWorkflowContext;
import fr.insee.pogues.service.release.steps.generation.mode.GenerateLunatic;
import fr.insee.pogues.utils.PoguesSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateLunaticTest {

    @Mock
    private EnoClient enoClient;

    @InjectMocks
    private GenerateLunatic generateLunatic;

    @Test
    @DisplayName("should return GENERATE_LUNATIC as step identifier")
    void should_return_generate_lunatic_as_step() {
        assertThat(generateLunatic.getStep()).isEqualTo(ReleaseRequestModeStep.GENERATE_LUNATIC);
    }

    @Nested
    @DisplayName("when Pogues serialization succeeds")
    class WhenSerializationSucceeds {

        @Test
        @DisplayName("should set generated lunatic questionnaire into the workflow context")
        void should_set_lunatic_into_context() {
            // Given
            Questionnaire questionnaire = new Questionnaire();
            GenerationParameters generationParameters =
                    GenerationParameters.builder().build();

            ReleaseWorkflowContext releaseWorkflowContext = new ReleaseWorkflowContext(
                    1L,
                    null,
                    "",
                    questionnaire,
                    generationParameters
            );

            ReleaseModeWorkflowContext workflowContext = new ReleaseModeWorkflowContext(
                    releaseWorkflowContext,
                    1L,
                    null
            );
            workflowContext.setGenerationParameters(generationParameters);

            String jsonPogues = "{\"pogues\":\"json\"}";
            String expectedLunatic = "{\"lunatic\":\"json\"}";

            try (MockedStatic<PoguesSerializer> mockedSerializer = mockStatic(PoguesSerializer.class)) {
                mockedSerializer.when(() -> PoguesSerializer.questionnaireJavaToString(questionnaire))
                        .thenReturn(jsonPogues);
                when(enoClient.getPoguesJsonToLunaticJson(jsonPogues, generationParameters))
                        .thenReturn(expectedLunatic);

                // When
                generateLunatic.execute(workflowContext);

                // Then
                assertThat(workflowContext.getLunatic()).isEqualTo(expectedLunatic);
            }
        }
    }

    @Nested
    @DisplayName("when Pogues serialization fails")
    class WhenSerializationFails {

        @Test
        @DisplayName("should wrap PoguesSerializationException into a GenerateLunaticException")
        void should_wrap_serialization_exception() {
            // Given
            Questionnaire questionnaire = new Questionnaire();
            GenerationParameters generationParameters =
                    GenerationParameters.builder().build();

            ReleaseModeWorkflowContext workflowContext = new ReleaseModeWorkflowContext(
                    new ReleaseWorkflowContext(1L, UUID.randomUUID(), "",questionnaire, generationParameters),
                    1L,
                    null
            );
            workflowContext.setGenerationParameters(generationParameters);

            PoguesSerializationException serializationException = new PoguesSerializationException(new Exception("serialization error"));

            try (MockedStatic<PoguesSerializer> mockedSerializer = mockStatic(PoguesSerializer.class)) {
                mockedSerializer.when(() -> PoguesSerializer.questionnaireJavaToString(questionnaire))
                        .thenThrow(serializationException);

                // When / Then
                assertThatThrownBy(() -> generateLunatic.execute(workflowContext))
                        .isInstanceOf(GenerateLunaticException.class);

                verifyNoInteractions(enoClient);
            }
        }
    }
}