package fr.insee.pogues.service.release.steps;

import fr.insee.pogues.domain.enums.release.ReleaseRequestModeStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ReleaseModeWorkflowStepRegistry")
class ReleaseModeWorkflowStepRegistryTest {

    private ReleaseModeWorkflowStep stepOf(ReleaseRequestModeStep type) {
        ReleaseModeWorkflowStep step = mock(ReleaseModeWorkflowStep.class);
        when(step.getStep()).thenReturn(type);
        return step;
    }

    private List<ReleaseModeWorkflowStep> allSteps;

    @BeforeEach
    void setUp() {
        allSteps = List.of(
                stepOf(ReleaseRequestModeStep.GENERATE_PARAMETERS),
                stepOf(ReleaseRequestModeStep.PUBLICATION_PRERELEASE),
                stepOf(ReleaseRequestModeStep.GENERATE_LUNATIC),
                stepOf(ReleaseRequestModeStep.PUBLICATION_LUNATIC)
        );
    }

    @Nested
    @DisplayName("when all required steps are provided")
    class WhenAllStepsProvided {

        @Test
        @DisplayName("should build the registry without error")
        void should_build_registry_without_error() {
            // Given
            List<ReleaseModeWorkflowStep> steps = allSteps;

            // When / Then
            assertThatCode(() -> new ReleaseModeWorkflowStepRegistry(steps)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should return steps in the fixed business execution order")
        void should_return_steps_in_fixed_execution_order() {
            // Given
            ReleaseModeWorkflowStep generateParameters = stepOf(ReleaseRequestModeStep.GENERATE_PARAMETERS);
            ReleaseModeWorkflowStep publishPrerelease = stepOf(ReleaseRequestModeStep.PUBLICATION_PRERELEASE);
            ReleaseModeWorkflowStep generateLunatic = stepOf(ReleaseRequestModeStep.GENERATE_LUNATIC);
            ReleaseModeWorkflowStep publishLunatic = stepOf(ReleaseRequestModeStep.PUBLICATION_LUNATIC);

            // Steps provided in a deliberately different order than the expected execution order
            List<ReleaseModeWorkflowStep> steps = List.of(publishLunatic, generateLunatic, publishPrerelease, generateParameters);
            ReleaseModeWorkflowStepRegistry registry = new ReleaseModeWorkflowStepRegistry(steps);

            // When
            List<ReleaseModeWorkflowStep> orderedSteps = registry.orderedSteps();

            // Then
            assertThat(orderedSteps).containsExactly(generateParameters, publishPrerelease, generateLunatic, publishLunatic);
        }
    }

    @Nested
    @DisplayName("when a required step implementation is missing")
    class WhenStepIsMissing {

        @Test
        @DisplayName("should throw an exception listing the missing step")
        void should_throw_when_a_single_step_is_missing() {
            // Given
            List<ReleaseModeWorkflowStep> steps = List.of(
                    stepOf(ReleaseRequestModeStep.GENERATE_PARAMETERS),
                    stepOf(ReleaseRequestModeStep.PUBLICATION_PRERELEASE),
                    stepOf(ReleaseRequestModeStep.GENERATE_LUNATIC)
                    // PUBLICATION_LUNATIC missing
            );

            // When / Then
            assertThatThrownBy(() -> new ReleaseModeWorkflowStepRegistry(steps))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Missing ReleaseWorkflowStep implementation(s)")
                    .hasMessageContaining(ReleaseRequestModeStep.PUBLICATION_LUNATIC.toString());
        }

        @Test
        @DisplayName("should throw an exception listing all missing steps")
        void should_throw_when_several_steps_are_missing() {
            // Given
            List<ReleaseModeWorkflowStep> steps = List.of(
                    stepOf(ReleaseRequestModeStep.GENERATE_PARAMETERS)
            );

            // When / Then
            assertThatThrownBy(() -> new ReleaseModeWorkflowStepRegistry(steps))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(ReleaseRequestModeStep.PUBLICATION_PRERELEASE.toString())
                    .hasMessageContaining(ReleaseRequestModeStep.GENERATE_LUNATIC.toString())
                    .hasMessageContaining(ReleaseRequestModeStep.PUBLICATION_LUNATIC.toString());
        }

        @Test
        @DisplayName("should throw when no step implementation is provided at all")
        void should_throw_when_no_step_provided() {
            // Given
            List<ReleaseModeWorkflowStep> steps = List.of();

            // When / Then
            assertThatThrownBy(() -> new ReleaseModeWorkflowStepRegistry(steps))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Missing ReleaseWorkflowStep implementation(s)");
        }
    }

    @Nested
    @DisplayName("when a step implementation is duplicated")
    class WhenStepIsDuplicated {

        @Test
        @DisplayName("should throw an exception naming the duplicated step type")
        void should_throw_when_a_step_type_is_duplicated() {
            // Given
            ReleaseModeWorkflowStep firstGenerateParametersImpl = stepOf(ReleaseRequestModeStep.GENERATE_PARAMETERS);
            ReleaseModeWorkflowStep secondGenerateParametersImpl = stepOf(ReleaseRequestModeStep.GENERATE_PARAMETERS);

            List<ReleaseModeWorkflowStep> steps = List.of(
                    firstGenerateParametersImpl,
                    secondGenerateParametersImpl,
                    stepOf(ReleaseRequestModeStep.PUBLICATION_PRERELEASE),
                    stepOf(ReleaseRequestModeStep.GENERATE_LUNATIC),
                    stepOf(ReleaseRequestModeStep.PUBLICATION_LUNATIC)
            );

            // When / Then
            assertThatThrownBy(() -> new ReleaseModeWorkflowStepRegistry(steps))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Duplicate ReleaseWorkflowStep implementation for")
                    .hasMessageContaining(ReleaseRequestModeStep.GENERATE_PARAMETERS.toString());
        }
    }
}