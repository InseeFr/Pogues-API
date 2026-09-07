package fr.insee.pogues.service.release.steps.generation;

import fr.insee.pogues.client.generation.EnoClient;
import fr.insee.pogues.conversion.JSONToXMLTranslator;
import fr.insee.pogues.domain.enums.generation.parameters.GenerationParameters;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStep;
import fr.insee.pogues.exception.PoguesSerializationException;
import fr.insee.pogues.exception.release.steps.GenerateDDIException;
import fr.insee.pogues.model.Questionnaire;
import fr.insee.pogues.service.release.context.ReleaseWorkflowContext;
import fr.insee.pogues.service.release.steps.generation.conceptual.GenerateDDI;
import fr.insee.pogues.utils.PoguesSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateDDITest {

    @Mock
    private EnoClient enoClient;

    @Mock
    private JSONToXMLTranslator jsonToXMLTranslator;

    @InjectMocks
    private GenerateDDI generateDDI;

    @Test
    @DisplayName("should return GENERATE_DDI as step identifier")
    void should_return_generate_ddi_as_step() {
        assertThat(generateDDI.getStep()).isEqualTo(ReleaseRequestStep.GENERATE_DDI);
    }

    @Nested
    @DisplayName("when Pogues serialization and XML translation succeed")
    class WhenSerializationAndTranslationSucceed {

        @Test
        @DisplayName("should set generated DDI into the workflow context")
        void should_set_ddi_into_context() {
            // Given
            Questionnaire questionnaire = new Questionnaire();
            ReleaseWorkflowContext workflowContext = new ReleaseWorkflowContext(
                    1L,
                    UUID.randomUUID(),
                    "releaseDescription",
                    questionnaire,
                    GenerationParameters.builder().build()
            );

            String jsonPogues = "{\"id\":\"q1\",\"label\":\"Questionnaire\"}";
            String expectedDdi = "<ddi>generated</ddi>";

            try (MockedStatic<PoguesSerializer> mockedSerializer = mockStatic(PoguesSerializer.class)) {
                mockedSerializer.when(() -> PoguesSerializer.questionnaireJavaToString(questionnaire))
                        .thenReturn(jsonPogues);
                when(enoClient.getPoguesXmlToDDI(any())).thenReturn(expectedDdi);

                // When
                generateDDI.execute(workflowContext);

                // Then
                assertThat(workflowContext.getDdi()).isEqualTo(expectedDdi);
            }
        }
    }

    @Nested
    @DisplayName("when Pogues serialization fails")
    class WhenSerializationFails {

        @Test
        @DisplayName("should wrap PoguesSerializationException into a GenerateDDIException")
        void should_wrap_serialization_exception() {
            // Given
            Questionnaire questionnaire = new Questionnaire();

            ReleaseWorkflowContext workflowContext = new ReleaseWorkflowContext(
                    1L,
                    UUID.randomUUID(),
                    "releaseDescription",
                    questionnaire,
                    GenerationParameters.builder().build()
            );

            PoguesSerializationException serializationException = new PoguesSerializationException(new Exception("serialization error"));

            try (MockedStatic<PoguesSerializer> mockedSerializer = mockStatic(PoguesSerializer.class)) {
                mockedSerializer.when(() -> PoguesSerializer.questionnaireJavaToString(questionnaire))
                        .thenThrow(serializationException);

                // When / Then
                assertThatThrownBy(() -> generateDDI.execute(workflowContext))
                        .isInstanceOf(GenerateDDIException.class);

                verifyNoInteractions(jsonToXMLTranslator);
                verifyNoInteractions(enoClient);
            }
        }
    }
}