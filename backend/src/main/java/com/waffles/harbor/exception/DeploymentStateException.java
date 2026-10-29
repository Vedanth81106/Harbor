package com.waffles.harbor.exception;

public class DeploymentStateException extends RuntimeException {
    public DeploymentStateException(String message) {
        super(message);
    }
}
