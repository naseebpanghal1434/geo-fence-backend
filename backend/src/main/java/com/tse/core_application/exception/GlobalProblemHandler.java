package com.tse.core_application.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalProblemHandler {

    @ExceptionHandler(ProblemException.class)
    public ResponseEntity<Map<String, Object>> handleProblemException(ProblemException ex) {
        Map<String, Object> problem = new HashMap<>();
        problem.put("type", "about:blank");
        problem.put("title", ex.getTitle());
        problem.put("status", ex.getStatus().value());
        problem.put("detail", ex.getDetail());
        problem.put("code", ex.getCode());

        return ResponseEntity.status(ex.getStatus()).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> problem = new HashMap<>();
        problem.put("type", "about:blank");
        problem.put("title", "Validation failed");
        problem.put("status", HttpStatus.BAD_REQUEST.value());

        StringBuilder detail = new StringBuilder();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            if (detail.length() > 0) {
                detail.append("; ");
            }
            detail.append(error.getField()).append(" ").append(error.getDefaultMessage());
        });

        problem.put("detail", detail.toString());
        problem.put("code", "VALIDATION_FAILED");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        Map<String, Object> problem = new HashMap<>();
        problem.put("type", "about:blank");
        problem.put("title", "Database constraint violation");
        problem.put("status", HttpStatus.CONFLICT.value());
        problem.put("detail", "A database constraint was violated");
        problem.put("code", "DB_CONFLICT");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }
}
