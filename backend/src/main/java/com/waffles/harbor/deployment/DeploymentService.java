
package com.waffles.harbor.deployment;

import com.waffles.harbor.docker.DockerService;
import com.waffles.harbor.exception.DeploymentNotFoundException;
import com.waffles.harbor.exception.DeploymentStateException;
import com.waffles.harbor.exception.ProjectNotFoundException;
import com.waffles.harbor.git.GitCommit;
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

            updateStatus(deployment, DeploymentStatus.BUILDING);
            buildDeploymentImage(deployment, directory);

            updateStatus(deployment, DeploymentStatus.STARTING);
            startDeploymentContainer(deployment);

            updateStatus(deployment, DeploymentStatus.READY);
            return toResponse(deployment);

        } catch (Exception deploymentException) {
            try {
                deleteImagesAndContainers(deployment);
                deleteDeploymentDirectory(deployment);
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

        deleteImagesAndContainers(deployment);
        deleteDeploymentDirectory(deployment);
        deploymentRepository.delete(deployment);
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
            deleteImagesAndContainers(deployment);
            deleteDeploymentDirectory(deployment);
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

    private void buildDeploymentImage(
            Deployment deployment,
            String filePath
    ) throws Exception {
        GitCommit latestCommit = gitService.getLatestCommit(filePath);

        deployment.setCommitHash(latestCommit.getHash());
        deployment.setImageName(
                deployment.getProject().getProjectId()
                        + ":" + latestCommit.getHash()
        );

        deploymentRepository.save(deployment);

        dockerService.buildImage(
                filePath,
                deployment.getImageName()
        );
    }

    private void startDeploymentContainer(Deployment deployment) {
        int containerPort = dockerService.getExposedImagePort(
                deployment.getImageName()
        );

        UUID deploymentId = deployment.getDeploymentId();
        String containerName = "harbor-"
                + deploymentId.toString().substring(0, 8);

        String containerId = dockerService.createContainer(
                deploymentId,
                deployment.getImageName(),
                containerPort,
                containerName
        );

        // Persist the ID before starting so failure cleanup can find it.
        deployment.setContainerId(containerId);
        deploymentRepository.save(deployment);

        dockerService.startContainer(containerId, deploymentId);
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

    private void deleteImagesAndContainers(Deployment deployment) {
        // Also cleans up a network when no container ID was saved.
        dockerService.tearDownDeployment(
                deployment.getContainerId(),
                deployment.getDeploymentId()
        );

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
