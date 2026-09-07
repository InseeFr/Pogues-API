package fr.insee.pogues.service.release.steps;

import fr.insee.pogues.domain.enums.release.ReleaseRequestStep;
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

@DisplayName("ReleaseWorkflowStepRegistry")
class ReleaseWorkflowStepRegistryTest {

    private ReleaseWorkflowStep stepOf(ReleaseRequestStep type) {
        ReleaseWorkflowStep step = mock(ReleaseWorkflowStep.class);
        when(step.getStep()).thenReturn(type);
        return step;
    }

    private List<ReleaseWorkflowStep> allSteps;

    @BeforeEach
    void setUp() {
        allSteps = List.of(
                stepOf(ReleaseRequestStep.PUBLICATION_CONCEPTUAL_PRERELEASE),
                stepOf(ReleaseRequestStep.GENERATE_DDI),
                stepOf(ReleaseRequestStep.PUBLICATION_DDI)
        );
    }

    @Nested
    @DisplayName("when all required steps are provided")
    class WhenAllStepsProvided {

        @Test
        @DisplayName("should build the registry without error")
        void should_build_registry_without_error() {
            // Given
            List<ReleaseWorkflowStep> steps = allSteps;

            // When / Then
            assertThatCode(() -> new ReleaseWorkflowStepRegistry(steps)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should return steps in the fixed business execution order")
        void should_return_steps_in_fixed_execution_order() {
            // Given
            ReleaseWorkflowStep publishPrerelease = stepOf(ReleaseRequestStep.PUBLICATION_CONCEPTUAL_PRERELEASE);
            ReleaseWorkflowStep generateDdi = stepOf(ReleaseRequestStep.GENERATE_DDI);
            ReleaseWorkflowStep publishDdi = stepOf(ReleaseRequestStep.PUBLICATION_DDI);

            // Steps provided in a deliberately different order than the expected execution order
            List<ReleaseWorkflowStep> steps = List.of(publishDdi, generateDdi, publishPrerelease);
            ReleaseWorkflowStepRegistry registry = new ReleaseWorkflowStepRegistry(steps);

            // When
            List<ReleaseWorkflowStep> orderedSteps = registry.orderedSteps();

            // Then
            assertThat(orderedSteps).containsExactly(publishPrerelease, generateDdi, publishDdi);
        }
    }

    @Nested
    @DisplayName("when a required step implementation is missing")
    class WhenStepIsMissing {

        @Test
        @DisplayName("should throw an exception listing the missing step")
        void should_throw_when_a_single_step_is_missing() {
            // Given
            List<ReleaseWorkflowStep> steps = List.of(
                    stepOf(ReleaseRequestStep.PUBLICATION_CONCEPTUAL_PRERELEASE),
                    stepOf(ReleaseRequestStep.GENERATE_DDI)
                    // PUBLICATION_DDI missing
            );

            // When / Then
            assertThatThrownBy(() -> new ReleaseWorkflowStepRegistry(steps))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Missing ReleaseWorkflowStep implementation(s)")
                    .hasMessageContaining(ReleaseRequestStep.PUBLICATION_DDI.toString());
        }

        @Test
        @DisplayName("should throw an exception listing all missing steps")
        void should_throw_when_several_steps_are_missing() {
            // Given
            List<ReleaseWorkflowStep> steps = List.of(
                    stepOf(ReleaseRequestStep.PUBLICATION_CONCEPTUAL_PRERELEASE)
            );

            // When / Then
            assertThatThrownBy(() -> new ReleaseWorkflowStepRegistry(steps))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(ReleaseRequestStep.GENERATE_DDI.toString())
                    .hasMessageContaining(ReleaseRequestStep.PUBLICATION_DDI.toString());
        }

        @Test
        @DisplayName("should throw when no step implementation is provided at all")
        void should_throw_when_no_step_provided() {
            // Given
            List<ReleaseWorkflowStep> steps = List.of();

            // When / Then
            assertThatThrownBy(() -> new ReleaseWorkflowStepRegistry(steps))
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
            ReleaseWorkflowStep firstGenerateParametersImpl = stepOf(ReleaseRequestStep.PUBLICATION_CONCEPTUAL_PRERELEASE);
            ReleaseWorkflowStep secondGenerateParametersImpl = stepOf(ReleaseRequestStep.PUBLICATION_CONCEPTUAL_PRERELEASE);

            List<ReleaseWorkflowStep> steps = List.of(
                    firstGenerateParametersImpl,
                    secondGenerateParametersImpl,
                    stepOf(ReleaseRequestStep.GENERATE_DDI),
                    stepOf(ReleaseRequestStep.PUBLICATION_DDI)
            );

            // When / Then
            assertThatThrownBy(() -> new ReleaseWorkflowStepRegistry(steps))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Duplicate ReleaseWorkflowStep implementation for")
                    .hasMessageContaining(ReleaseRequestStep.PUBLICATION_CONCEPTUAL_PRERELEASE.toString());
        }
    }
}