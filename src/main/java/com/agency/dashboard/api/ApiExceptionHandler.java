package com.agency.dashboard.api;

import com.agency.dashboard.api.model.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Keeps failures inside the same envelope as successes, so a client only parses one shape.
 *
 * <p>Spring MVC's own exceptions (404, 405, 415, ...) implement {@link ErrorResponse}; they
 * keep their status here instead of being flattened into a 500. Anything else is logged and
 * reported as a generic {@code internal_error}, so internals never leak to the client.</p>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        if (exception instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            if (status.is5xxServerError()) {
                log.error("Server error while serving a request", exception);
            }
            return ResponseEntity.status(status)
                    .body(ApiResponse.error(typeFor(status), messageFor(status)));
        }

        log.error("Unhandled exception while serving a request", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("internal_error", "Internal server error."));
    }

    private static String typeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "bad_request";
            case 401 -> "unauthorized";
            case 403 -> "forbidden";
            case 404 -> "not_found";
            case 405 -> "method_not_allowed";
            case 415 -> "unsupported_media_type";
            default -> status.is5xxServerError() ? "internal_error" : "request_failed";
        };
    }

    private static String messageFor(HttpStatusCode status) {
        if (status.is5xxServerError() || !(status instanceof HttpStatus known)) {
            return "Internal server error.";
        }
        return known.getReasonPhrase() + ".";
    }
}
