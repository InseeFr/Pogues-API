package fr.insee.pogues.exception.loops;

import java.util.List;

/** Exception that is thrown when one tries to delete a loop on which other loops are based. */
public class LoopUsedAsBasedOnException extends RuntimeException {

    public LoopUsedAsBasedOnException(String loopName, List<String> relatedLoopNames) {
        super(String.format("The loop %s cannot be deleted because the following loops are based on it: %s",
                loopName, String.join(", ", relatedLoopNames)));
    }
}
