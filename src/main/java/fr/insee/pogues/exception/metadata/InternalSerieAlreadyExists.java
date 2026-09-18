package fr.insee.pogues.exception.metadata;

public class InternalSerieAlreadyExists extends RuntimeException {
    public InternalSerieAlreadyExists(String message) {
        super(message);
    }
}