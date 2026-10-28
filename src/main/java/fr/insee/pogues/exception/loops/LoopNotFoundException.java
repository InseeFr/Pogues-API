package fr.insee.pogues.exception.loops;

/** Exception that is thrown when one tries to access a loop which does not exist. */
public class LoopNotFoundException extends RuntimeException {

    public LoopNotFoundException(String loopId, String questionnaireId) {
        super(String.format("Loop with id %s not found in questionnaire %s", loopId, questionnaireId));
    }
}
