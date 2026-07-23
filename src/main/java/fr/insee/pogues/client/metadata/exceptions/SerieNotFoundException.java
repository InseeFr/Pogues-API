package fr.insee.pogues.client.metadata.exceptions;

public class SerieNotFoundException extends RuntimeException {
    public SerieNotFoundException(String message) {
        super(message);
    }
}
