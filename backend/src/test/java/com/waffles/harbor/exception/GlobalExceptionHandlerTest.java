package com.waffles.harbor.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleValidationFailure() {
        MethodArgumentNotValidException exception =
                mock(MethodArgumentNotValidException.class);

        assertError(
                handler.handleValidationException(exception),
                HttpStatus.BAD_REQUEST,
                "Request validation failed"
        );
    }

    @Test
    void shouldHandleBadCredentials() {
        assertError(
                handler.handleBadCredentials(new BadCredentialsException("bad")),
                HttpStatus.UNAUTHORIZED,
                "Invalid credentials"
        );
    }

    @Test
    void shouldHandleMissingProject() {
        assertError(
                handler.handleProjectNotFound(new ProjectNotFoundException()),
                HttpStatus.NOT_FOUND,
                "Project could not be found!"
        );
    }

    @Test
    void shouldHandleMissingDeployment() {
        assertError(
                handler.handleDeploymentNotFound(new DeploymentNotFoundException()),
                HttpStatus.NOT_FOUND,
                "Deployment could not be found!"
        );
    }

    @Test
    void shouldHandleDeploymentStateConflict() {
        assertError(
                handler.handleDeploymentState(
                        new DeploymentStateException("Only queued deployments can be executed")
                ),
                HttpStatus.CONFLICT,
                "Only queued deployments can be executed"
        );
    }

    @Test
    void shouldHandleDuplicateUsername() {
        assertError(
                handler.handleUsernameAlreadyExists(new UsernameAlreadyExistsException()),
                HttpStatus.CONFLICT,
                "Username is already taken!"
        );
    }

    @Test
    void shouldHandleDuplicateEmail() {
        assertError(
                handler.handleEmailAlreadyExists(new EmailAlreadyExistsException()),
                HttpStatus.CONFLICT,
                "Email already exists!"
        );
    }

    @Test
    void shouldHandleDuplicateProject() {
        assertError(
                handler.handleProjectAlreadyExists(new ProjectAlreadyExistsException()),
                HttpStatus.CONFLICT,
                "Project already exists!"
        );
    }

    @Test
    void shouldHandleDataIntegrityViolation() {
        assertError(
                handler.handleDataIntegrityViolation(
                        new DataIntegrityViolationException("constraint")
                ),
                HttpStatus.CONFLICT,
                "The requested operation violates a database constraint"
        );
    }

    private void assertError(
            ResponseEntity<ErrorResponse> response,
            HttpStatus expectedStatus,
            String expectedMessage) {

        assertEquals(expectedStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedStatus.value(), response.getBody().getStatus());
        assertEquals(expectedMessage, response.getBody().getMessage());
    }
}
