package com.waffles.harbor.deployment;

import com.waffles.harbor.docker.DockerService;
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

    public DeploymentResponse createDeployment(UUID projectId, String commitHash){

        Project project = projectRepository.findById(projectId).orElseThrow();
        Deployment deployment = Deployment.builder()
                .project(project)
                .commitHash(commitHash)
                .imageName(project.getProjectName()+":"+commitHash)
                .status(DeploymentStatus.QUEUED)
                .build();

        Deployment savedDeployment =  deploymentRepository.save(deployment);

        return toResponse(savedDeployment);
    }

    public String deploy(UUID deploymentId) throws Exception {

        Deployment deployment = deploymentRepository
                .findById(deploymentId)
                .orElseThrow();

        String filePath = generateFilePath(deploymentId);

        System.out.println("Cloning git repository...");
        gitService.cloneRepository(
                deployment.getProject().getRepositoryUrl(),
                filePath
        );

        System.out.println("Building docker image...");
        dockerService.buildImage(
                filePath,
                deployment.getImageName()
        );

        System.out.println("Creating container...");
        String containerId = dockerService.createContainer(
                deployment.getImageName(),
                8081,
                8080,
                "harbor-" + deploymentId
        );

        System.out.println("Starting container...");
        dockerService.startContainer(containerId);

        return containerId;
    }

    private String generateFilePath(UUID deploymentId){
        Deployment deployment = deploymentRepository.findById(deploymentId).orElseThrow();
        Project project = deployment.getProject();

        return Path.of(
                "/home/vedanth/Harbor/tmp",
                project.getUser().getUserId().toString(),
                project.getProjectId().toString(),
                deploymentId.toString()
        ).toString();
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

        String filePath = generateFilePath(deploymentId);

        File deploymentDirectory = new File(filePath);

        if (deploymentDirectory.exists()) {
            deleteDirectory(deploymentDirectory);
        }

        deploymentRepository.delete(deployment);
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
