package com.example.sakeec.exception;

import com.example.sakeec.dto.ConflictErrorResponse;
import com.example.sakeec.dto.ErrorResponse;
import com.example.sakeec.dto.ValidationErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.logstash.logback.argument.StructuredArguments.kv;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, List<String>> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(fe.getField(), k -> new ArrayList<>()).add(fe.getDefaultMessage());
        }
        log.warn("VALIDATION_ERROR",
                kv("event", "BUSINESS_ERROR"),
                kv("errorCode", "VALIDATION_ERROR"),
                kv("status", 400),
                kv("invalidFields", errors.keySet()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ValidationErrorResponse("VALIDATION_ERROR", "validation error", traceId(), errors));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        log.warn("NOT_FOUND: {}", ex.getMessage(),
                kv("event", "BUSINESS_ERROR"),
                kv("errorCode", ex.getCode()),
                kv("status", 404));
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), traceId()));
    }

    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<ErrorResponse> handleOutOfStock(OutOfStockException ex) {
        log.warn("OUT_OF_STOCK: {}", ex.getMessage(),
                kv("event", "BUSINESS_ERROR"),
                kv("errorCode", ex.getCode()),
                kv("status", 409));
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), traceId()));
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ConflictErrorResponse> handleInvalidTransition(InvalidStatusTransitionException ex) {
        log.warn("INVALID_STATUS_TRANSITION: {}", ex.getMessage(),
                kv("event", "BUSINESS_ERROR"),
                kv("errorCode", ex.getCode()),
                kv("status", 409),
                kv("currentStatus", ex.getCurrentStatus()));
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ConflictErrorResponse(ex.getCode(), ex.getMessage(), ex.getCurrentStatus()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        log.warn("BUSINESS_ERROR: {}", ex.getMessage(),
                kv("event", "BUSINESS_ERROR"),
                kv("errorCode", ex.getCode()),
                kv("status", 400));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), traceId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("UNHANDLED_EXCEPTION: {}",
                ex.getMessage(),
                kv("event", "UNHANDLED_EXCEPTION"),
                kv("errorCode", "INTERNAL_ERROR"),
                kv("status", 500),
                kv("exceptionClass", ex.getClass().getName()),
                ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "internal server error", traceId()));
    }

    private String traceId() {
        return MDC.get("traceId");
    }
}
