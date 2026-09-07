package fr.insee.pogues.exception;

public class PoguesSerializationException extends RuntimeException {

    public PoguesSerializationException(Exception exception) {
        super(exception);
    }

}
