
package com.waffles.harbor.deployment;

import com.waffles.harbor.compose.ComposeFileDetector;
import com.waffles.harbor.deployment.enums.DeploymentStatus;
import com.waffles.harbor.deployment.enums.DeploymentType;
import com.waffles.harbor.deployment.enums.ResourceType;
import com.waffles.harbor.deployment.strategy.DeploymentResult;
import com.waffles.harbor.deployment.strategy.DeploymentStrategy;
import com.waffles.harbor.deployment.strategy.DeploymentStrategyFactory;
import com.waffles.harbor.docker.DockerService;
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
import java.util.*;
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
    private final ComposeFileDetector composeFileDetector;
    private final DeploymentStrategyFactory deploymentStrategyFactory;

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

        DeploymentResource container =
                deployment.getSingleContainerResource();

        String containerId = container.getExternalId();

        if (containerId == null || containerId.isBlank()) {
            throw new IllegalStateException(
                    "Deployment does not have a valid container"
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
            deploymentRepository.save(deployment);

            DeploymentStrategy strategy =
                    deploymentStrategyFactory.getStrategy(deployment.getDeploymentType());

            updateStatus(deployment, DeploymentStatus.BUILDING);
            DeploymentResult result = strategy.prepare(deployment,Path.of(directory));
            persistDeploymentResources(deployment,result);

            updateStatus(deployment, DeploymentStatus.STARTING);
            strategy.activate(deployment);
            updateStatus(deployment,DeploymentStatus.READY);

            return toResponse(deployment);

        } catch (Exception deploymentException) {
            try{
                if(!deployment.getDeploymentResources().isEmpty()){
                    DeploymentStrategy strategy = deploymentStrategyFactory.getStrategy(deployment.getDeploymentType());
                    strategy.teardown(deployment);
                }
                removeImageIfUnreferenced(deployment);
            }catch (RuntimeException cleanupException){
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

    private void persistDeploymentResources(Deployment deployment, DeploymentResult result){

        for(DeploymentResult.Result resource : result.resources()){
            DeploymentResource deploymentResource = DeploymentResource.builder()
                    .resourceType(resource.type())
                    .resourceName(resource.resourceName())
                    .externalId(resource.externalId())
                    .serviceName(resource.serviceName())
                    .build();

            deployment.addResource(deploymentResource);
        }
        deployment.setCommitHash(result.commitHash());
        deploymentRepository.save(deployment);
    }

    @Transactional
    public void deleteDeployment(User user, UUID deploymentId) {
        Deployment deployment = findDeployment(user, deploymentId);

        deleteDeploymentResources(deployment);
        removeImageIfUnreferenced(deployment);
        deploymentRepository.delete(deployment);
    }

    private void deleteDeploymentResources(Deployment deployment) {

        if (!deployment.getDeploymentResources().isEmpty()) {
            DeploymentStrategy strategy =
                    deploymentStrategyFactory.getStrategy(
                            deployment.getDeploymentType()
                    );

            strategy.teardown(deployment);
        }

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

        Set<String> imageNames = new HashSet<>();
        Set<UUID> deploymentIds = deployments.stream()
                .map(Deployment::getDeploymentId)
                .collect(Collectors.toSet());
        // collect all image references
        for(Deployment deployment : deployments){
            for(DeploymentResource deploymentResource : deployment.getImageResources()){
                imageNames.add(deploymentResource.getResourceName());
            }
        }

        // Clean up Docker resources and files first.
        for (Deployment deployment : deployments) {
            deleteDeploymentResources(deployment);
        }

        // orphanRemoval = true deletes the child records.
        project.getDeployments().clear();
        projectRepository.save(project);

        for(String imageName : imageNames){
            if (!deploymentRepository.existsImageReferenceOutside(
                    deploymentIds,
                    ResourceType.IMAGE,
                    imageName
            )) {
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

    private DeploymentType detectDeploymentType(String directory) {
        // A Dockerfile wins when a repository has both, so Dockerfile
        // projects keep deploying even if a compose file is also present.
        if (Files.isRegularFile(Path.of(directory, "Dockerfile"))) {
            return DeploymentType.DOCKERFILE;
        }

        if (composeFileDetector.detectComposeFiles(directory).isPresent()) {
            return DeploymentType.COMPOSE;
        }

        throw new DeploymentStateException(
                "No supported deployment configuration found"
        );
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
        List<DeploymentResource> images = deployment.getImageResources();

        for(DeploymentResource image : images){

            String imageName = image.getResourceName();
            boolean isReferenced = deploymentRepository.existsImageReference(
                    deployment.getDeploymentId(),
                    ResourceType.IMAGE,
                    imageName
            );
            if (!isReferenced) {
                dockerService.removeImage(imageName);
            }
        }
    }

    private DeploymentResponse toResponse(Deployment deployment) {
        return DeploymentResponse.builder()
                .deploymentId(deployment.getDeploymentId())
                .projectId(deployment.getProject().getProjectId())
                .status(deployment.getStatus())
                .commitHash(deployment.getCommitHash())
                .imageNames(deployment.getImageResources().stream()
                        .map(DeploymentResource::getResourceName)
                        .toList())
                .build();
    }
}
