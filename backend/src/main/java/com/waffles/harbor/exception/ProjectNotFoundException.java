package com.waffles.harbor.exception;

public class ProjectNotFoundException extends RuntimeException{

    public ProjectNotFoundException() {
        super("Project could not be found!");
    }
}
