package com.waffles.harbor.dockerfile;

import com.waffles.harbor.docker.DockerService;
import com.waffles.harbor.git.GitCommit;
import com.waffles.harbor.git.GitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DockerfileService {

    private final DockerService dockerService;
    private final GitService gitService;

    /**
     * Builds the image for a checked-out repository and returns the details
     * the deployment needs to persist.
     */
    public BuiltImage buildImage(
            String directory,
            UUID projectId
    ) throws Exception {
        GitCommit latestCommit = gitService.getLatestCommit(directory);
        String imageName = projectId + ":" + latestCommit.getHash();

        dockerService.buildImage(directory, imageName);

        return new BuiltImage(latestCommit.getHash(), imageName);
    }

    /**
     * Creates (but does not start) the deployment container.
     * The caller persists the returned ID before calling
     * {@link #startContainer(String, UUID)} so cleanup can find the container
     * if starting fails.
     */
    public String createContainer(UUID deploymentId, String imageName) {
        int containerPort = dockerService.getExposedImagePort(imageName);
        String containerName = "harbor-"
                + deploymentId.toString().substring(0, 8);

        return dockerService.createContainer(
                deploymentId,
                imageName,
                containerPort,
                containerName
        );
    }

    public void startContainer(String containerId, UUID deploymentId) {
        dockerService.startContainer(containerId, deploymentId);
    }

    public void deleteContainer(UUID deploymentId, String containerId) {
        dockerService.tearDownDeployment(containerId, deploymentId);
    }

    public record BuiltImage(String commitHash, String imageName) {
    }
}
