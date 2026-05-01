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

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, List<String>> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(fe.getField(), k -> new ArrayList<>()).add(fe.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ValidationErrorResponse("VALIDATION_ERROR", "validation error", traceId(), errors));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), traceId()));
    }

    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<ErrorResponse> handleOutOfStock(OutOfStockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), traceId()));
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ConflictErrorResponse> handleInvalidTransition(InvalidStatusTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ConflictErrorResponse(ex.getCode(), ex.getMessage(), ex.getCurrentStatus()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "internal server error", traceId()));
    }

    private String traceId() {
        return MDC.get("traceId");
    }
}
