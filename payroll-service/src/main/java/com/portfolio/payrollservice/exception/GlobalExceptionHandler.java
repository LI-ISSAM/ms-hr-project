package com.portfolio.payrollservice.exception;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Service distant indisponible (retry épuisé, circuit ouvert, bulkhead plein)
    @ExceptionHandler(RemoteServiceUnavailableException.class)
    public ResponseEntity<Object> handleRemoteUnavailable(RemoteServiceUnavailableException ex,
                                                          WebRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, null, ex.getMessage(), request);
    }

    // Violation de contrainte unique (doublon, ou deux requêtes simultanées avec la même clé)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleConflict(DataIntegrityViolationException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, null, "A resource with the same unique value already exists", request);
    }

    // Filet de sécurité : jamais de trace de pile envoyée au client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, null, "Unexpected error", request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, headers, "Malformed or unreadable request body", request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
                                                        HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, headers, "Invalid value for a request parameter: " + ex.getValue(), request);
    }

    // Toutes les erreurs standard de Spring MVC (400, 404, 405, 415, ResponseStatusException...)
    // passent ici et sont converties au même format
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
                                                             HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        String message = (ex instanceof ErrorResponse er && er.getBody().getDetail() != null)
                ? er.getBody().getDetail()
                : "Request failed";
        return build(statusCode, headers, message, request);
    }

    private ResponseEntity<Object> build(HttpStatusCode status, HttpHeaders headers,
                                         String message, WebRequest request) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        ApiError body = new ApiError(
                Instant.now().toString(),
                status.value(),
                resolved != null ? resolved.getReasonPhrase() : "Error",
                message,
                request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : "");
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
        if (headers != null) {
            response.headers(headers);
        }
        return response.body(body);
    }
}