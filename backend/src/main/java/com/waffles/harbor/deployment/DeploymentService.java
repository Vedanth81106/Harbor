package com.waffles.harbor.deployment;

import com.waffles.harbor.docker.DockerService;
import com.waffles.harbor.git.GitCommit;
import com.waffles.harbor.git.GitService;
import com.waffles.harbor.project.Project;
import com.waffles.harbor.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeploymentService {

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

    public DeploymentResponse createDeployment(UUID projectId){

        Project project = projectRepository.findById(projectId).orElseThrow();
        Deployment deployment = Deployment.builder()
                .project(project)
                .status(DeploymentStatus.QUEUED)
                .build();

        Deployment savedDeployment = deploymentRepository.save(deployment);

        return toResponse(savedDeployment);
    }

    public String deploy(UUID deploymentId) throws Exception {

        Deployment deployment = deploymentRepository
                .findById(deploymentId)
                .orElseThrow();

        try {
            String filePath = createDirectory(deployment);

            deployment.setStatus(DeploymentStatus.BUILDING);
            deploymentRepository.save(deployment);

            gitService.cloneRepository(
                    deployment.getProject().getRepositoryUrl(),
                    filePath
            );

            List<GitCommit> gitCommits = gitService.getCommits(filePath);
            String latestCommitHash = gitCommits.getFirst().getHash();
            deployment.setCommitHash(latestCommitHash);
            deployment.setImageName(deployment.getProject().getProjectName().replace("\\s+","-")+":"+latestCommitHash);
            deploymentRepository.save(deployment);

            dockerService.buildImage(
                    filePath,
                    deployment.getImageName()
            );

            String containerId = dockerService.createContainer(
                    deployment.getImageName(),
                    8081,
                    8080,
                    "harbor-" + deploymentId
            );

            dockerService.startContainer(containerId);

            deployment.setStatus(DeploymentStatus.READY);
            deployment.setContainerId(containerId);
            deploymentRepository.save(deployment);

            return containerId;

        } catch (Exception e) {

            deployment.setStatus(DeploymentStatus.FAILED);
            deploymentRepository.save(deployment);

            throw e;
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

    private String createDirectory(Deployment deployment) {
        Project project = deployment.getProject();

        return Path.of(
                "/home/vedanth/Harbor/tmp",
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
}
