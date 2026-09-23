package com.waffles.harbor.deployment;

public enum DeploymentStatus {

    QUEUED,
    CLONING,
    BUILDING,
    STARTING,
    RUNNING,
    FAILED,
    READY,
    STOPPED
}
