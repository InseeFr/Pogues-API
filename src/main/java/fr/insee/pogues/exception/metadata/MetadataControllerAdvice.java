package fr.insee.pogues.exception.metadata;


import fr.insee.pogues.client.metadata.exceptions.MetadataRepositoryException;
import fr.insee.pogues.client.metadata.exceptions.SerieNotFoundException;
import fr.insee.pogues.controller.MetadataController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(assignableTypes = { MetadataController.class })
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MetadataControllerAdvice {

    @ExceptionHandler(MetadataRepositoryException.class)
    public ProblemDetail handleMetadataRepositoryException(MetadataRepositoryException ex, HttpServletRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setTitle("Error when calling metadata repository");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        return problemDetail;
    }

    @ExceptionHandler(SerieNotFoundException.class)
    public ProblemDetail handleSerieNotFoundException(SerieNotFoundException ex, HttpServletRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Serie Not found");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        return problemDetail;
    }

    @ExceptionHandler(DDIAgencyAlreadyExists.class)
    public ProblemDetail handleDDIAgencyAlreadyExists(DDIAgencyAlreadyExists ex, HttpServletRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("DDI Agency already exists");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        return problemDetail;
    }

    @ExceptionHandler(DDIAgencyNotFound.class)
    public ProblemDetail handleDDIAgencyNotFound(DDIAgencyNotFound ex, HttpServletRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("DDI Agency not found");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        return problemDetail;
    }
}
