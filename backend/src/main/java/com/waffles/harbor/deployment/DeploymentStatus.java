package com.waffles.harbor.deployment;

public enum DeploymentStatus {

    QUEUED,
    CLONING,
    BUILDING,
    STARTING,
    FAILED,
    READY,
    STOPPED
}
