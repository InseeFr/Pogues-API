package fr.insee.pogues.exception.loops;

/**
 * Exception that is thrown when one tries to create a loop which is not valid
 * (invalid fields, name already used, invalid bounds or scope).
 */
public class LoopInvalidException extends RuntimeException {

    public LoopInvalidException(String message) {
        super(message);
    }
}
