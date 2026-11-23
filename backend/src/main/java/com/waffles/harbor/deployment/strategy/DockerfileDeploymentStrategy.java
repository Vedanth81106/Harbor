package com.waffles.harbor.deployment.strategy;

import com.waffles.harbor.deployment.Deployment;
import com.waffles.harbor.deployment.DeploymentResource;
import com.waffles.harbor.deployment.enums.DeploymentType;
import com.waffles.harbor.deployment.enums.ResourceType;
import com.waffles.harbor.docker.DockerService;
import com.waffles.harbor.exception.DeploymentStateException;
import com.waffles.harbor.git.GitCommit;
import com.waffles.harbor.git.GitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class DockerfileDeploymentStrategy implements DeploymentStrategy{

    private final DockerService dockerService;
    private final GitService gitService;

    public record BuiltImage(String commitHash, String imageName, String imageId){}

    @Override
    public DeploymentType getType() {
        return DeploymentType.DOCKERFILE;
    }

    @Override
    public DeploymentResult prepare(Deployment deployment, Path directory) throws Exception {
        String networkId = null;
        String containerId = null;
        String imageName = null;

        try {
            networkId = dockerService.createNetwork(deployment.getDeploymentId());
            String networkName = dockerService.getDeploymentNetworkName(deployment.getDeploymentId());

            BuiltImage builtImage = buildImage(
                    directory.toString(),
                    deployment.getProject().getProjectId()
            );
            imageName = builtImage.imageName();

            containerId = createContainer(
                    deployment.getDeploymentId(),
                    builtImage.imageName(),
                    networkId
            );

            String containerName = "harbor-"
                    + deployment.getDeploymentId().toString().substring(0, 8);

            DeploymentResult.Result network = new DeploymentResult.Result(
                    ResourceType.NETWORK, networkId, networkName, null
            );

            DeploymentResult.Result image = new DeploymentResult.Result(
                    ResourceType.IMAGE, builtImage.imageId(), builtImage.imageName(), null
            );

            DeploymentResult.Result container = new DeploymentResult.Result(
                    ResourceType.CONTAINER, containerId, containerName, null
            );

            return new DeploymentResult(
                    builtImage.commitHash(),
                    List.of(network, image, container)
            );

        } catch (Exception e) {
            try {
                if (networkId != null) {
                    dockerService.tearDownDeployment(containerId, networkId);
                }
            } catch (RuntimeException cleanupException) {
                e.addSuppressed(cleanupException);
            }

            if (imageName != null) {
                try {
                    dockerService.removeImage(imageName);
                } catch (RuntimeException cleanupException) {
                    e.addSuppressed(cleanupException);
                }
            }

            throw e;
        }
    }

    @Override
    public void activate(Deployment deployment) {

        DeploymentResource container = deployment.getSingleContainerResource();
        DeploymentResource network = deployment.getSingleNetworkResource();

        String containerId = container.getExternalId();
        String networkId = network.getExternalId();

        dockerService.startContainer(containerId, networkId);
    }

    @Override
    public void teardown(Deployment deployment) {
        String networkId = deployment.getNetworkResources().stream()
                .map(DeploymentResource::getExternalId)
                .findFirst()
                .orElse(null);
        String containerId = deployment.getContainerResources().stream()
                .map(DeploymentResource::getExternalId)
                .findFirst()
                .orElse(null);

        dockerService.tearDownDeployment(containerId, networkId);
    }

    @Override
    public void recover(Deployment deployment) {
        String containerId = deployment.getSingleContainerResource().getExternalId();
        String networkId = deployment.getSingleNetworkResource().getExternalId();

        if (containerId == null || containerId.isBlank()) {
            throw new DeploymentStateException(
                    "Cannot recover deployment: container ID is missing"
            );
        }

        dockerService.recoverContainer(
                containerId,
                networkId
        );
    }

    private BuiltImage buildImage(String directory, UUID projectId) throws Exception {
        GitCommit latestCommit = gitService.getLatestCommit(directory);
        String imageName = projectId + ":" + latestCommit.getHash();

        String imageId = dockerService.buildImage(directory, imageName);

        return new BuiltImage(latestCommit.getHash(), imageName, imageId);
    }


    private String createContainer(
            UUID deploymentId,
            String imageName,
            String networkId) {

        int containerPort = dockerService.getExposedImagePort(imageName);
        String containerName = "harbor-"
                + deploymentId.toString().substring(0, 8);

        return dockerService.createContainer(
                imageName,
                containerPort,
                containerName,
                networkId
        );
    }
}
