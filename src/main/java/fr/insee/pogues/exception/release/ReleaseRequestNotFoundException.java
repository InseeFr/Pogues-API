package fr.insee.pogues.exception.release;

public class ReleaseRequestNotFoundException extends RuntimeException {
    public ReleaseRequestNotFoundException(Long id) {
        super("ReleaseRequest not found with id: "+ id);
    }
}
