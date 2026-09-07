package fr.insee.pogues.service.release.steps.generation;

import fr.insee.pogues.client.generation.EnoClient;
import fr.insee.pogues.domain.enums.generation.CollectMode;
import fr.insee.pogues.domain.enums.generation.GenerationContext;
import fr.insee.pogues.domain.enums.generation.GenerationFormat;
import fr.insee.pogues.domain.enums.generation.GenerationQuestionNumberingMode;
import fr.insee.pogues.domain.enums.generation.parameters.GenerationParameters;
import fr.insee.pogues.domain.enums.release.ReleaseRequestModeStep;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStep;
import fr.insee.pogues.model.Questionnaire;
import fr.insee.pogues.service.release.context.ReleaseModeWorkflowContext;
import fr.insee.pogues.service.release.context.ReleaseWorkflowContext;
import fr.insee.pogues.service.release.steps.generation.mode.GenerateParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateParametersTest {

    @Mock
    private EnoClient enoClient;

    @InjectMocks
    private GenerateParameters generateParameters;

    @Test
    @DisplayName("should return GENERATE_PARAMETERS as step identifier")
    void should_return_generate_parameters_as_step() {
        assertThat(generateParameters.getStep()).isEqualTo(ReleaseRequestModeStep.GENERATE_PARAMETERS);
    }

    @Nested
    @DisplayName("when context is BUSINESS")
    class WhenContextIsBusiness {

        @Test
        @DisplayName("should enrich generation parameters returned by Eno with response time question and numbering mode from the workflow context")
        void should_enrich_generation_parameters_from_eno_client() {
            // Given
            GenerationContext context = GenerationContext.BUSINESS;
            GenerationFormat outFormat = GenerationFormat.LUNATIC;
            CollectMode mode = CollectMode.CAWI;

            GenerationParameters commonParameters = GenerationParameters.builder()
                    .context(context)
                    .outFormat(outFormat)
                    .modeParameter(mode)
                    .responseTimeQuestion(true)
                    .questionNumberingMode(GenerationQuestionNumberingMode.SEQUENCE).build();

            ReleaseWorkflowContext workflowContext = new ReleaseWorkflowContext(
                    1L,
                    UUID.randomUUID(),
                    "releaseDescription",
                    new Questionnaire(),
                    commonParameters
            );

            ReleaseModeWorkflowContext modeWorkflowContext = new ReleaseModeWorkflowContext(workflowContext, 1L, mode);

            GenerationParameters parametersFromEno = GenerationParameters.builder().build();

            when(enoClient.getGenerationParameters(context, outFormat, mode)).thenReturn(parametersFromEno);

            // When
            generateParameters.execute(modeWorkflowContext);

            // Then
            assertThat(modeWorkflowContext.getGenerationParameters())
                    .isSameAs(parametersFromEno)
                    .satisfies(result -> {
                        assertThat(result.getResponseTimeQuestion()).isTrue();
                        assertThat(result.getQuestionNumberingMode()).isEqualTo(GenerationQuestionNumberingMode.SEQUENCE);
                    });
        }

        @Test
        @DisplayName("should overwrite response time question and numbering mode already present on parameters returned by Eno")
        void should_overwrite_existing_values_on_eno_parameters() {
            // Given
            GenerationContext context = GenerationContext.BUSINESS;
            GenerationFormat outFormat = GenerationFormat.LUNATIC;
            CollectMode mode = CollectMode.CATI;

            GenerationParameters commonParameters = GenerationParameters.builder()
                    .context(context)
                    .outFormat(outFormat)
                    .modeParameter(mode)
                    .responseTimeQuestion(false)
                    .questionNumberingMode(GenerationQuestionNumberingMode.NONE).build();

            ReleaseWorkflowContext workflowContext = new ReleaseWorkflowContext(
                    1L,
                    UUID.randomUUID(),
                    "releaseDescription",
                    new Questionnaire(),
                    commonParameters
            );
            ReleaseModeWorkflowContext modeWorkflowContext = new ReleaseModeWorkflowContext(workflowContext, 1L, mode);

            GenerationParameters parametersFromEno = GenerationParameters.builder().build();
            parametersFromEno.setResponseTimeQuestion(true);
            parametersFromEno.setQuestionNumberingMode(GenerationQuestionNumberingMode.ALL);

            when(enoClient.getGenerationParameters(context, outFormat, mode)).thenReturn(parametersFromEno);

            // When
            generateParameters.execute(modeWorkflowContext);

            // Then
            assertThat(modeWorkflowContext.getGenerationParameters().getResponseTimeQuestion()).isFalse();
            assertThat(modeWorkflowContext.getGenerationParameters().getQuestionNumberingMode())
                    .isEqualTo(GenerationQuestionNumberingMode.NONE);
        }

        @Test
        @DisplayName("should keep Eno values when workflow context parameters are null")
        void should_keep_eno_values_when_workflow_parameters_are_null() {
            // Given
            GenerationContext context = GenerationContext.BUSINESS;
            GenerationFormat outFormat = GenerationFormat.LUNATIC;
            CollectMode mode = CollectMode.CAWI;

            GenerationParameters commonParameters = GenerationParameters.builder()
                    .context(context)
                    .outFormat(outFormat)
                    .modeParameter(mode)
                    .responseTimeQuestion(null)
                    .questionNumberingMode(null).build();

            ReleaseWorkflowContext workflowContext = new ReleaseWorkflowContext(
                    1L,
                    UUID.randomUUID(),
                    "releaseDescription",
                    new Questionnaire(),
                    commonParameters
            );
            ReleaseModeWorkflowContext modeWorkflowContext = new ReleaseModeWorkflowContext(workflowContext, 1L, mode);

            GenerationParameters parametersFromEno = GenerationParameters.builder().build();
            parametersFromEno.setResponseTimeQuestion(true);
            parametersFromEno.setQuestionNumberingMode(GenerationQuestionNumberingMode.ALL);

            when(enoClient.getGenerationParameters(context, outFormat, mode)).thenReturn(parametersFromEno);

            // When
            generateParameters.execute(modeWorkflowContext);

            // Then
            assertThat(modeWorkflowContext.getGenerationParameters().getResponseTimeQuestion()).isTrue();
            assertThat(modeWorkflowContext.getGenerationParameters().getQuestionNumberingMode())
                    .isEqualTo(GenerationQuestionNumberingMode.ALL);
        }
    }

    @Nested
    @DisplayName("when context is not BUSINESS")
    class WhenContextIsNotBusiness {

        @ParameterizedTest
        @EnumSource(value = GenerationContext.class, names = "BUSINESS", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("should not override response time question and numbering mode from workflow context")
        void should_not_override_eno_parameters(GenerationContext context) {
            // Given
            GenerationFormat outFormat = GenerationFormat.LUNATIC;
            CollectMode mode = CollectMode.CAWI;

            GenerationParameters commonParameters = GenerationParameters.builder()
                    .context(context)
                    .outFormat(outFormat)
                    .modeParameter(mode)
                    .responseTimeQuestion(true)
                    .questionNumberingMode(GenerationQuestionNumberingMode.SEQUENCE).build();

            ReleaseWorkflowContext workflowContext = new ReleaseWorkflowContext(
                    1L,
                    UUID.randomUUID(),
                    "releaseDescription",
                    new Questionnaire(),
                    commonParameters
            );
            ReleaseModeWorkflowContext modeWorkflowContext = new ReleaseModeWorkflowContext(workflowContext, 1L, mode);

            GenerationParameters parametersFromEno = GenerationParameters.builder().build();
            parametersFromEno.setResponseTimeQuestion(false);
            parametersFromEno.setQuestionNumberingMode(GenerationQuestionNumberingMode.NONE);

            when(enoClient.getGenerationParameters(context, outFormat, mode)).thenReturn(parametersFromEno);

            // When
            generateParameters.execute(modeWorkflowContext);

            // Then
            assertThat(modeWorkflowContext.getGenerationParameters().getResponseTimeQuestion()).isFalse();
            assertThat(modeWorkflowContext.getGenerationParameters().getQuestionNumberingMode())
                    .isEqualTo(GenerationQuestionNumberingMode.NONE);
        }
    }
}