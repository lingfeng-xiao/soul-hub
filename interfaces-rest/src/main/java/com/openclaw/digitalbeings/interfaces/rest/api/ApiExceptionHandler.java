package com.openclaw.digitalbeings.interfaces.rest.api;

import com.openclaw.digitalbeings.interfaces.rest.status.RequestEnvelope;
import com.openclaw.digitalbeings.interfaces.rest.status.RequestEnvelopes;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RequestEnvelope<Object>> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(errorEnvelope(request, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RequestEnvelope<Object>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(errorEnvelope(request, "Request body validation failed."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RequestEnvelope<Object>> handleUnhandledException(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RequestEnvelopes.error(new ApiError("GRAPH_UNEXPECTED", exception.getMessage())));
    }

    private static RequestEnvelope<Object> errorEnvelope(HttpServletRequest request, String message) {
        ApiErrorCode code = ApiErrorCode.forRequestPath(request == null ? null : request.getRequestURI());
        return RequestEnvelopes.error(new ApiError(code.name(), message));
    }
}
