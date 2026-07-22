package tn.gov.ancs.audit.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire d'exceptions global pour l'API REST.
 *
 * <p>Sécurité : Empêche la divulgation de traces de pile (stack traces) internes
 * en production, tout en renvoyant des codes HTTP sémantiquement corrects (400, 401, 403, 404)
 * avec une structure JSON standardisée.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Ressource non trouvée: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDetails> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Accès refusé sur {}: {}", request.getDescription(false), ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorDetails> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        log.warn("Échec d'authentification: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDetails> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("Paramètre invalide: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorDetails> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        log.warn("État invalide: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<ErrorDetails> handleAiUnavailableException(AiUnavailableException ex, WebRequest request) {
        log.error("Module d'IA indisponible: {}", ex.getMessage());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation échouée: {}", errors);

        ErrorDetails details = ErrorDetails.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Les données d'entrée sont invalides")
            .validationErrors(errors)
            .path(request.getDescription(false).replace("uri=", ""))
            .timestamp(Instant.now())
            .build();

        return new ResponseEntity<>(details, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDetails> handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException ex, WebRequest request) {
        log.warn("Violation d'intégrité des données: {}", ex.getMessage());
        return buildResponse(
            HttpStatus.CONFLICT,
            "Conflict",
            "Un rapport est déjà en cours de génération pour cette mission, veuillez réessayer dans quelques secondes.",
            request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Exception non gérée survenue lors de l'appel à l'API", ex);
        return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR, 
            "Internal Server Error", 
            "Une erreur interne inattendue s'est produite. Veuillez contacter le support technique.", 
            request
        );
    }

    private ResponseEntity<ErrorDetails> buildResponse(HttpStatus status, String error, String message, WebRequest request) {
        ErrorDetails details = ErrorDetails.builder()
            .status(status.value())
            .error(error)
            .message(message)
            .path(request.getDescription(false).replace("uri=", ""))
            .timestamp(Instant.now())
            .build();
        return new ResponseEntity<>(details, status);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetails {
        private int status;
        private String error;
        private String message;
        private String path;
        private Instant timestamp;
        private Map<String, String> validationErrors;
    }
}
