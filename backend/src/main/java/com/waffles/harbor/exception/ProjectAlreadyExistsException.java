package com.waffles.harbor.exception;

public class ProjectAlreadyExistsException extends RuntimeException {
    public ProjectAlreadyExistsException() {
        super("Project already exists!");
    }
}
