package fr.insee.pogues.mapper;

import fr.insee.pogues.model.DynamicIterationType;
import fr.insee.pogues.model.ExpressionType;
import fr.insee.pogues.model.dto.loops.LoopDTO;

import java.math.BigInteger;
import java.util.List;

public class LoopsMapper {

    private LoopsMapper() {}

    /**
     * Compute the loop into its Pogues model.
     * Only the fields relevant to the shape of the loop (based on a scope, fixed or dynamic number of occurrences)
     * are set. The loop is supposed to have been validated beforehand.
     * @param loopDTO Loop to convert
     * @return The loop as a Pogues iteration
     */
    public static DynamicIterationType toModel(LoopDTO loopDTO) {
        DynamicIterationType iteration = new DynamicIterationType();
        iteration.setId(loopDTO.id());
        iteration.setName(loopDTO.name());
        // Always two references, even when the loop iterates over a single component
        iteration.getMemberReference().addAll(List.of(loopDTO.initialMember(), loopDTO.finalMember()));

        if (isFilled(loopDTO.basedOn())) {
            iteration.setIterableReference(loopDTO.basedOn());
            if (isFilled(loopDTO.filter())) iteration.setFilter(toExpression(loopDTO.filter()));
            return iteration;
        }

        iteration.setIsFixedLength(loopDTO.isFixedLength());
        iteration.setStep(BigInteger.ONE);
        if (Boolean.TRUE.equals(loopDTO.isFixedLength())) {
            iteration.setSize(toExpression(loopDTO.size()));
            iteration.setShouldSplitIterations(Boolean.TRUE.equals(loopDTO.shouldSplitIterations()));
        } else {
            iteration.setMinimum(toExpression(loopDTO.minimum()));
            iteration.setMaximum(toExpression(loopDTO.maximum()));
        }
        return iteration;
    }

    /**
     * Compute the Pogues iteration into the loop sent to the frontend form, so that it can be edited.
     * Only the fields relevant to the shape of the loop are set, so that the loop can be posted back as is:
     * a loop with an iterable reference is based on a scope, otherwise it has a fixed number of occurrences
     * when {@code isFixedLength} is true, and a dynamic one in any other case (legacy loops have no
     * {@code isFixedLength}).
     * @param iteration Pogues iteration to convert
     * @return The loop
     */
    public static LoopDTO toDTO(DynamicIterationType iteration) {
        List<String> members = iteration.getMemberReference();
        String initialMember = members.isEmpty() ? null : members.getFirst();
        String finalMember = members.isEmpty() ? null : members.getLast();

        if (isFilled(iteration.getIterableReference())) {
            return new LoopDTO(iteration.getId(), iteration.getName(), initialMember, finalMember,
                    iteration.getIterableReference(), fromExpression(iteration.getFilter()),
                    null, null, null, null, null);
        }
        if (Boolean.TRUE.equals(iteration.isIsFixedLength())) {
            return new LoopDTO(iteration.getId(), iteration.getName(), initialMember, finalMember,
                    null, null,
                    true, fromExpression(iteration.getSize()), Boolean.TRUE.equals(iteration.isShouldSplitIterations()),
                    null, null);
        }
        return new LoopDTO(iteration.getId(), iteration.getName(), initialMember, finalMember,
                null, null,
                false, null, null,
                fromExpression(iteration.getMinimum()), fromExpression(iteration.getMaximum()));
    }

    private static String fromExpression(ExpressionType expression) {
        return expression == null ? null : expression.getValue();
    }

    private static ExpressionType toExpression(String value) {
        ExpressionType expression = new ExpressionType();
        expression.setValue(value);
        return expression;
    }

    private static boolean isFilled(String value) {
        return value != null && !value.isBlank();
    }

}
