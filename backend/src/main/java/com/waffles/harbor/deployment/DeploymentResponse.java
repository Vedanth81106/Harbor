package com.waffles.harbor.deployment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class DeploymentResponse {

    private UUID deploymentId;
    private UUID projectId;
    private DeploymentStatus status;
    private String commitHash;
    private String imageName;

}
