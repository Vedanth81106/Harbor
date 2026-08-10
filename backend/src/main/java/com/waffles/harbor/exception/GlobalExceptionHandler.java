package com.waffles.harbor.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Void> handleBadCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Void> handleIllegalArgumentException() {
        return ResponseEntity.badRequest().build();
    }
}