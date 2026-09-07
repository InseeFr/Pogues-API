package fr.insee.pogues.exception.release;

public class ReleaseRequestAlreadyExists extends RuntimeException {
    public ReleaseRequestAlreadyExists(String message) {
        super(message);
    }
}
