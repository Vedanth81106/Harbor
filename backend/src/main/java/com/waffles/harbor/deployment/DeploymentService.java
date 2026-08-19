package com.waffles.harbor.deployment;

import com.waffles.harbor.git.GitService;
import com.waffles.harbor.project.Project;
import com.waffles.harbor.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final ProjectRepository projectRepository;
    private final GitService gitService;

    public List<Deployment> getAllDeployments(UUID projectId){
        return deploymentRepository.findByProject_ProjectIdOrderByCreatedAtDesc(projectId);
    }

    public Deployment createDeployment(UUID projectId, String commitHash){

        Project project = projectRepository.findById(projectId).orElseThrow();
        Deployment deployment = Deployment.builder()
                .project(project)
                .commitHash(commitHash)
                .imageName(project.getProjectName()+":"+commitHash)
                .status(DeploymentStatus.QUEUED)
                .build();

        return deploymentRepository.save(deployment);
    }

//    public Deployment startDeployment(UUID deploymentId){
//
//        String filePath = generateFilePath(deploymentId);
//        Deployment deployment = deploymentRepository.findById(deploymentId).orElseThrow();
//        gitService.cloneRepository(deployment.getProject().getRepositoryUrl(), filePath);
//    }

    private String generateFilePath(UUID deploymentId){
        Deployment deployment = deploymentRepository.findById(deploymentId).orElseThrow();
        Project project = deployment.getProject();

        return Path.of(
                "/Harbor",
                project.getUser().getUserId().toString(),
                project.getProjectId().toString(),
                deploymentId.toString()
        ).toString();
    }
}
