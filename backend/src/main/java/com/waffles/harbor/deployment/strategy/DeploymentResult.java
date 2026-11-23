package com.waffles.harbor.deployment.strategy;

import com.waffles.harbor.deployment.enums.ResourceType;

import java.util.List;

public record DeploymentResult(
        String commitHash,
        List<Result> resources
        ) {

        public record Result(
                ResourceType type,
                String externalId,
                String resourceName,
                String serviceName
        ){}
}

// Not to be confused with DeploymentResource class.
// That class is the actual entity which gets stored in the database whereas this class is the temporary result returned by creating the said resource