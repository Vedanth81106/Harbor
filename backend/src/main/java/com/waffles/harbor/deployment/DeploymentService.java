
package com.waffles.harbor.deployment;

import com.waffles.harbor.compose.ComposeFileDetector;
import com.waffles.harbor.docker.DockerService;
import com.waffles.harbor.dockerfile.DockerfileService;
import com.waffles.harbor.exception.DeploymentNotFoundException;
import com.waffles.harbor.exception.DeploymentStateException;
import com.waffles.harbor.exception.ProjectNotFoundException;
import com.waffles.harbor.git.GitService;
import com.waffles.harbor.project.Project;
import com.waffles.harbor.project.ProjectRepository;
import com.waffles.harbor.sse.SseService;
import com.waffles.harbor.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeploymentService {

    @Value("${configuration-path}")
    private String path;

    private final DeploymentRepository deploymentRepository;
    private final ProjectRepository projectRepository;
    private final GitService gitService;
    private final DockerService dockerService;
    private final SseService sseService;
    private final DockerfileService dockerfileService;
    private final ComposeFileDetector composeFileDetector;

    public List<DeploymentResponse> getAllDeployments(
            User user,
            UUID projectId
    ) {
        if (!projectRepository.existsByUserAndProjectId(user, projectId)) {
            throw new ProjectNotFoundException();
        }

        return deploymentRepository
                .findByProject_ProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DeploymentResponse getDeployment(User user, UUID deploymentId) {
        Deployment deployment = findDeployment(user, deploymentId);
        return toResponse(deployment);
    }

    public SseEmitter getDeploymentLogs(User user, UUID deploymentId) {
        Deployment deployment = findDeployment(user, deploymentId);
        String containerId = deployment.getContainerId();

        if (containerId == null) {
            throw new IllegalStateException(
                    "Deployment does not have a container"
            );
        }

        return sseService.streamContainerLogs(containerId);
    }

    public DeploymentResponse createDeployment(User user, UUID projectId) {
        Project project = projectRepository
                .findByUserAndProjectId(user, projectId)
                .orElseThrow(ProjectNotFoundException::new);

        Deployment deployment = Deployment.builder()
                .project(project)
                .deploymentType(DeploymentType.DOCKERFILE)
                .status(DeploymentStatus.QUEUED)
                .build();

        return toResponse(deploymentRepository.save(deployment));
    }

    public DeploymentResponse executeDeployment(
            User user,
            UUID deploymentId
    ) throws Exception {
        Deployment deployment = findDeployment(user, deploymentId);

        if (deployment.getStatus() != DeploymentStatus.QUEUED) {
            throw new DeploymentStateException(
                    "Only queued deployments can be executed"
            );
        }

        try {
            String directory = getDeploymentDirectory(deployment);

            updateStatus(deployment, DeploymentStatus.CLONING);
            cloneRepository(deployment, directory);
            deployment.setDeploymentType(detectDeploymentType(directory));

            switch (deployment.getDeploymentType()) {
                case DOCKERFILE -> deployDockerfile(deployment, directory);
                case COMPOSE -> throw new DeploymentStateException(
                        "Compose deployments are not supported yet"
                );
            }
            return toResponse(deployment);

        } catch (Exception deploymentException) {
            try {
                deleteDeploymentResources(deployment);
            } catch (RuntimeException cleanupException) {
                deploymentException.addSuppressed(cleanupException);
                log.error(
                        "Cleanup failed for deployment {}",
                        deploymentId,
                        cleanupException
                );
            }

            deployment.setStatus(DeploymentStatus.FAILED);
            deploymentRepository.save(deployment);
            throw deploymentException;
        }
    }

    public void deleteDeployment(User user, UUID deploymentId) {
        Deployment deployment = findDeployment(user, deploymentId);

        deleteDeploymentResources(deployment);
        deploymentRepository.delete(deployment);
    }

    private void deleteDeploymentResources(Deployment deployment) {
        if (deployment.getDeploymentType() == DeploymentType.DOCKERFILE) {
            dockerfileService.deleteContainer(
                    deployment.getDeploymentId(),
                    deployment.getContainerId()
            );
        }

        // TODO: add docker compose deletion

        removeImageIfUnreferenced(deployment);
        deleteDeploymentDirectory(deployment);
    }

    @Transactional
    public void deleteAllDeploymentsForProject(
            User user,
            UUID projectId
    ) {
        Project project = projectRepository
                .findByUserAndProjectId(user, projectId)
                .orElseThrow(ProjectNotFoundException::new);

        List<Deployment> deployments =
                new ArrayList<>(project.getDeployments());

        Set<String> imageNames = deployments.stream()
                .map(Deployment::getImageName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Clean up Docker resources and files first.
        for (Deployment deployment : deployments) {
            deleteDeploymentResources(deployment);
        }

        // orphanRemoval = true deletes the child records.
        project.getDeployments().clear();
        projectRepository.save(project);

        // Remove images only when no deployment references them.
        for (String imageName : imageNames) {
            if (!deploymentRepository.existsByImageName(imageName)) {
                dockerService.removeImage(imageName);
            }
        }
    }

    private Deployment findDeployment(User user, UUID deploymentId) {
        return deploymentRepository
                .findByDeploymentIdAndProject_User(deploymentId, user)
                .orElseThrow(DeploymentNotFoundException::new);
    }

    private void updateStatus(
            Deployment deployment,
            DeploymentStatus status
    ) {
        deployment.setStatus(status);
        deploymentRepository.save(deployment);
    }

    private void cloneRepository(
            Deployment deployment,
            String filePath
    ) throws Exception {
        String repositoryUrl =
                deployment.getProject().getRepositoryUrl();

        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            throw new IllegalStateException(
                    "Project does not have a repository configured"
            );
        }

        gitService.cloneRepository(repositoryUrl, filePath, 60);
    }

    private void deployDockerfile(
            Deployment deployment,
            String directory
    ) throws Exception {
        updateStatus(deployment, DeploymentStatus.BUILDING);
        DockerfileService.BuiltImage image = dockerfileService.buildImage(
                directory,
                deployment.getProject().getProjectId()
        );

        deployment.setCommitHash(image.commitHash());
        deployment.setImageName(image.imageName());
        deploymentRepository.save(deployment);

        updateStatus(deployment, DeploymentStatus.STARTING);
        String containerId = dockerfileService.createContainer(
                deployment.getDeploymentId(),
                deployment.getImageName()
        );

        // Persist the ID before starting so failure cleanup can find it.
        deployment.setContainerId(containerId);
        deploymentRepository.save(deployment);
        dockerfileService.startContainer(containerId, deployment.getDeploymentId());

        updateStatus(deployment, DeploymentStatus.READY);
    }

    private DeploymentType detectDeploymentType(String directory) {
        // A Dockerfile wins when a repository has both, so Dockerfile
        // projects keep deploying even if a compose file is also present.
        if (Files.isRegularFile(Path.of(directory, "Dockerfile"))) {
            return DeploymentType.DOCKERFILE;
        }

        if (composeFileDetector.detectComposeFiles(directory).isPresent()) {
            return DeploymentType.COMPOSE;
        }

        return DeploymentType.DOCKERFILE;
    }

    private String getDeploymentDirectory(Deployment deployment) {
        Project project = deployment.getProject();

        return Path.of(
                path,
                project.getUser().getUserId().toString(),
                project.getProjectId().toString(),
                deployment.getDeploymentId().toString()
        ).toString();
    }

    private void deleteDeploymentDirectory(Deployment deployment) {
        File directory = new File(getDeploymentDirectory(deployment));

        if (directory.exists()) {
            deleteDirectory(directory);
        }
    }

    private void deleteDirectory(File directory) {
        File[] contents = directory.listFiles();

        if (contents != null) {
            for (File file : contents) {
                deleteDirectory(file);
            }
        }

        if (!directory.delete() && directory.exists()) {
            throw new IllegalStateException(
                    "Could not delete directory: " + directory
            );
        }
    }

    private void removeImageIfUnreferenced(Deployment deployment) {
        String imageName = deployment.getImageName();

        if (imageName != null
                && !deploymentRepository.existsByImageNameAndDeploymentIdNot(
                imageName,
                deployment.getDeploymentId()
        )) {
            dockerService.removeImage(imageName);
        }
    }

    private DeploymentResponse toResponse(Deployment deployment) {
        return DeploymentResponse.builder()
                .deploymentId(deployment.getDeploymentId())
                .projectId(deployment.getProject().getProjectId())
                .status(deployment.getStatus())
                .commitHash(deployment.getCommitHash())
                .imageName(deployment.getImageName())
                .build();
    }
}
