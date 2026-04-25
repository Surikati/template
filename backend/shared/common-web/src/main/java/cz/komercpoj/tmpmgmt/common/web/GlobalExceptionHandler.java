package cz.komercpoj.tmpmgmt.common.web;

import cz.komercpoj.tmpmgmt.common.ConflictException;
import cz.komercpoj.tmpmgmt.common.DomainException;
import cz.komercpoj.tmpmgmt.common.NotFoundException;
import cz.komercpoj.tmpmgmt.common.ValidationException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates domain exceptions to RFC 9457 problem+json responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String PROBLEM_TYPE_BASE = "https://errors.tmpmgmt.komercpoj.cz/";

  @ExceptionHandler(NotFoundException.class)
  ProblemDetail handleNotFound(NotFoundException ex) {
    return problem(HttpStatus.NOT_FOUND, ex);
  }

  @ExceptionHandler(ConflictException.class)
  ProblemDetail handleConflict(ConflictException ex) {
    return problem(HttpStatus.CONFLICT, ex);
  }

  @ExceptionHandler(ValidationException.class)
  ProblemDetail handleValidation(ValidationException ex) {
    ProblemDetail pd = problem(HttpStatus.UNPROCESSABLE_ENTITY, ex);
    pd.setProperty("violations", ex.getViolations());
    return pd;
  }

  @ExceptionHandler(DomainException.class)
  ProblemDetail handleDomain(DomainException ex) {
    return problem(HttpStatus.BAD_REQUEST, ex);
  }

  private ProblemDetail problem(HttpStatus status, DomainException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
    pd.setType(URI.create(PROBLEM_TYPE_BASE + ex.getErrorCode()));
    pd.setProperty("code", ex.getErrorCode());
    return pd;
  }
}
