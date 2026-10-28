package fr.insee.pogues.model.dto.loops;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * <p>Loop of a questionnaire, as sent by the Pogues frontend form.</p>
 * <p>A loop iterates over the sibling components from its initial member to its final member (both included).
 * It comes in three shapes, and only the fields of the chosen shape must be filled:</p>
 * <ul>
 *     <li>based on a scope: {@code basedOn} and optionally {@code filter};</li>
 *     <li>with a fixed number of occurrences: {@code isFixedLength = true}, {@code size} and
 *     {@code shouldSplitIterations};</li>
 *     <li>with a dynamic number of occurrences: {@code isFixedLength = false}, {@code minimum} and {@code maximum}.</li>
 * </ul>
 * @param id ID of the loop, provided by the frontend (on creation as well as on update)
 * @param name Name of the loop
 * @param initialMember ID of the first component of the loop
 * @param finalMember ID of the last component of the loop (sibling of the initial member)
 * @param basedOn ID of the scope the loop iterates on (loop, dynamic table, pairwise question...)
 * @param filter VTL formula filtering the occurrences of the scope
 * @param isFixedLength Whether the number of occurrences is fixed (size) or dynamic (minimum / maximum)
 * @param size VTL formula of the number of occurrences
 * @param shouldSplitIterations Whether each occurrence is displayed on its own page
 * @param minimum VTL formula of the minimum number of occurrences
 * @param maximum VTL formula of the maximum number of occurrences
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoopDTO(
        @NotBlank String id,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9\\-_]+$", message = "must only contain letters, digits, '-' and '_'") String name,
        @NotBlank String initialMember,
        @NotBlank String finalMember,
        String basedOn,
        String filter,
        Boolean isFixedLength,
        String size,
        Boolean shouldSplitIterations,
        String minimum,
        String maximum
) {}
