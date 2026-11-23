package com.waffles.harbor.deployment.strategy;

import com.waffles.harbor.deployment.Deployment;
import com.waffles.harbor.deployment.enums.DeploymentType;

import java.nio.file.Path;

public interface DeploymentStrategy {

    DeploymentType getType();
    // prepares all the resources needed for the deployment entity
    DeploymentResult prepare(Deployment deployment, Path directory) throws Exception;
    // activates all the resources available to the entity
    void activate(Deployment deployment);
    void teardown(Deployment deployment);
    void recover(Deployment deployment);
}
