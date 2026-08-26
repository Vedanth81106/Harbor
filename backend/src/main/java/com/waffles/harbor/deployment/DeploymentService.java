package com.waffles.harbor.deployment;

import com.waffles.harbor.docker.DockerService;
import com.waffles.harbor.git.GitCommit;
import com.waffles.harbor.git.GitService;
import com.waffles.harbor.project.Project;
import com.waffles.harbor.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeploymentService {

    @Value("${configuration-path}")
    private String path;

    private final DeploymentRepository deploymentRepository;
    private final ProjectRepository projectRepository;
    private final GitService gitService;
    private final DockerService dockerService;

    public List<DeploymentResponse> getAllDeployments(UUID projectId) {
        return deploymentRepository
                .findByProject_ProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DeploymentResponse deploy(UUID projectId) throws Exception {

        Project project = projectRepository.findById(projectId).orElseThrow();

        Deployment deployment = Deployment.builder()
                .project(project)
                .status(DeploymentStatus.BUILDING)
                .build();

        deploymentRepository.save(deployment);

        try {
            String filePath = createDirectory(deployment);

            cloneRepository(deployment, filePath);
            buildDeploymentImage(deployment, filePath);

            String containerId = startDeploymentContainer(deployment);

            deployment.setContainerId(containerId);
            deployment.setStatus(DeploymentStatus.READY);

            deploymentRepository.save(deployment);

            return toResponse(deployment);

        } catch (Exception e) {

            deployment.setStatus(DeploymentStatus.FAILED);
            deploymentRepository.save(deployment);

            throw e;
        }
    }

    public void deleteDeployment(UUID deploymentId) {

        Deployment deployment = deploymentRepository
                .findById(deploymentId)
                .orElseThrow();

        String containerId = deployment.getContainerId();
        String filePath = createDirectory(deployment);

        File deploymentDirectory = new File(filePath);

        if (containerId != null) {
            dockerService.stopContainer(containerId);
            dockerService.removeContainer(containerId);
        }

        if (deployment.getImageName() != null) {
            dockerService.removeImage(deployment.getImageName());
        }

        if (deploymentDirectory.exists()) {
            deleteDirectory(deploymentDirectory);
        }

        deploymentRepository.delete(deployment);
    }

    private void cloneRepository(
            Deployment deployment,
            String filePath
    ) throws Exception {

        String repositoryUrl = deployment.getProject().getRepositoryUrl();

        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            throw new IllegalStateException("Project does not have a repository configured");
        }

        gitService.cloneRepository(repositoryUrl, filePath);
    }

    private void buildDeploymentImage(
            Deployment deployment,
            String filePath
    ) throws Exception {

        GitCommit latestCommit = gitService.getLatestCommit(filePath);
        deployment.setCommitHash(latestCommit.getHash());

        deployment.setImageName(
                deployment.getProject().getProjectId() + ":" + latestCommit.getHash()
        );

        deploymentRepository.save(deployment);

        dockerService.buildImage(
                filePath,
                deployment.getImageName()
        );
    }

    private String startDeploymentContainer(
            Deployment deployment
    ) throws Exception {

        int containerPort = dockerService.getExposedImagePort(deployment.getImageName());
        String containerId = dockerService.createContainer(
                deployment.getImageName(),
                0,
                containerPort,
                "harbor-" + deployment.getDeploymentId()
        );

        dockerService.startContainer(containerId);

        int assignedHostPort = dockerService.getDockerContainerPort(containerId, containerPort);
        deployment.setAssignedHostPort(assignedHostPort);
        deploymentRepository.save(deployment);

        return containerId;
    }

    private String createDirectory(Deployment deployment) {
        Project project = deployment.getProject();

        return Path.of(
                path,
                project.getUser().getUserId().toString(),
                project.getProjectId().toString(),
                deployment.getDeploymentId().toString()
        ).toString();
    }

    private void deleteDirectory(File directory) {

        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }

        directory.delete();
    }

    private DeploymentResponse toResponse(Deployment deployment) {

        return DeploymentResponse.builder()
                .deploymentId(deployment.getDeploymentId())
                .projectId(deployment.getProject().getProjectId())
                .status(deployment.getStatus())
                .commitHash(deployment.getCommitHash())
                .imageName(deployment.getImageName())
                .hostPort(deployment.getAssignedHostPort())
                .build();
    }

}
