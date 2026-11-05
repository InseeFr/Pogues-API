package fr.insee.pogues.exception.details;

/**
 * Exception that is thrown when one tries to create or update a questionnaire with invalid details
 * (unknown serie or DDI agency).
 */
public class QuestionnaireDetailsInvalidException extends RuntimeException {

    public QuestionnaireDetailsInvalidException(String message) {
        super(message);
    }
}
