package fr.insee.pogues.exception.release;

import fr.insee.pogues.controller.ReleaseController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(assignableTypes = { ReleaseController.class })
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReleaseRequestControllerAdvice {

    @ExceptionHandler(ReleaseRequestNotFoundException.class)
    public ProblemDetail handleReleaseRequestNotFoundException(ReleaseRequestNotFoundException ex, HttpServletRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Release request not found");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        return problemDetail;
    }

    @ExceptionHandler(ReleaseRequestAlreadyExists.class)
    public ProblemDetail handleMetadataRepositoryException(ReleaseRequestAlreadyExists ex, HttpServletRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Release request already exists");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        return problemDetail;
    }

    @ExceptionHandler(ReleaseRequestStatusException.class)
    public ProblemDetail handleReleaseRequestStatusException(ReleaseRequestStatusException ex, HttpServletRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Release request can't be deleted");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        return problemDetail;
    }
}
