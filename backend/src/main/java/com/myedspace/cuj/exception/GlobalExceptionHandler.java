package com.myedspace.cuj.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }

    /**
     * Catches the DB-level unique-constraint rejection from the check-then-act race in
     * OnboardingController (two concurrent requests for the same invitation token or email
     * both pass the pre-check; the loser fails here instead of corrupting data) and maps it
     * to a clean 409 instead of leaking a raw constraint-violation error.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Rejected write due to a data integrity conflict", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("This request conflicts with existing data — it may have already been processed"));
    }

    /**
     * Covers Spring's "couldn't route this request" family — wrong HTTP method, unsupported
     * media type, etc. These already carry the correct status; without this handler the
     * catch-all below would flatten a routing-level 404/405/415 into a raw 500.
     */
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ErrorResponse> handleErrorResponseException(ErrorResponseException ex) {
        return toResponse(ex.getStatusCode(), ex.getBody() != null ? ex.getBody().getDetail() : null);
    }

    /**
     * Unmapped paths (e.g. the H2 console when disabled, or any typo'd URL). Handled separately
     * from ErrorResponseException above because NoResourceFoundException implements the
     * ErrorResponse *interface* directly rather than extending ErrorResponseException.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        return toResponse(ex.getStatusCode(), ex.getBody() != null ? ex.getBody().getDetail() : null);
    }

    private ResponseEntity<ErrorResponse> toResponse(org.springframework.http.HttpStatusCode status, String detail) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(detail != null ? detail : "Request could not be processed"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("An unexpected error occurred"));
    }
}
