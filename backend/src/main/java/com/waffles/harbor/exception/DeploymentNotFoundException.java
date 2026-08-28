package com.waffles.harbor.exception;

public class DeploymentNotFoundException extends RuntimeException{

    public DeploymentNotFoundException() {
        super("Deployment could not be found!");
    }
}
