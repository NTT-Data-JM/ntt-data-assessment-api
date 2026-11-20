package com.nttdata.assessment.api.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Maps assessment failures to HTTP responses.
 */
@RestControllerAdvice
public class AssessmentExceptionHandler {

    /**
     * @param ex business rule failure
     * @return HTTP 422
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorBody> business(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErrorBody(ex.getMessage()));
    }

    /**
     * @param ex validation failure
     * @return HTTP 400
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorBody> validation(WebExchangeBindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Validation failed");
        return ResponseEntity.badRequest().body(new ErrorBody(message));
    }

    /**
     * @param ex status exception
     * @return mapped status
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorBody> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(new ErrorBody(ex.getReason()));
    }

    /**
     * @param message safe message
     */
    public record ErrorBody(String message) {
    }
}
