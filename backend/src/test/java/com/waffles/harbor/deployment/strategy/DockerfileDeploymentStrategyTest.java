package com.waffles.harbor.deployment.strategy;

import com.waffles.harbor.auth.Role;
import com.waffles.harbor.deployment.Deployment;
import com.waffles.harbor.deployment.DeploymentResource;
import com.waffles.harbor.deployment.enums.DeploymentStatus;
import com.waffles.harbor.deployment.enums.DeploymentType;
import com.waffles.harbor.deployment.enums.ResourceType;
import com.waffles.harbor.docker.DockerService;
import com.waffles.harbor.git.GitCommit;
import com.waffles.harbor.git.GitService;
import com.waffles.harbor.project.Project;
import com.waffles.harbor.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DockerfileDeploymentStrategyTest {

    @Mock
    private DockerService dockerService;

    @Mock
    private GitService gitService;

    @InjectMocks
    private DockerfileDeploymentStrategy strategy;

    private final UUID deploymentId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final Path directory = Path.of("/tmp/repo");

    private Deployment deployment;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .userId(UUID.randomUUID())
                .username("happy")
                .email("happy@gmail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        Project project = Project.builder()
                .projectId(projectId)
                .projectName("project")
                .user(user)
                .build();

        deployment = Deployment.builder()
                .deploymentId(deploymentId)
                .project(project)
                .deploymentType(DeploymentType.DOCKERFILE)
                .status(DeploymentStatus.QUEUED)
                .build();
    }

    private String containerName() {
        return "harbor-" + deploymentId.toString().substring(0, 8);
    }

    private void attachResource(Deployment target, ResourceType type, String externalId, String name) {
        target.addResource(DeploymentResource.builder()
                .resourceType(type)
                .externalId(externalId)
                .resourceName(name)
                .build());
    }

    @Test
    void shouldPrepareNetworkImageAndContainerResources() throws Exception {
        when(dockerService.createNetwork(deploymentId)).thenReturn("network-id");
        when(dockerService.getDeploymentNetworkName(deploymentId)).thenReturn("net-12345678");
        when(gitService.getLatestCommit(directory.toString()))
                .thenReturn(GitCommit.builder().hash("abc123").build());
        when(dockerService.buildImage(directory.toString(), projectId + ":abc123"))
                .thenReturn("image-id");
        when(dockerService.getExposedImagePort(projectId + ":abc123")).thenReturn(8080);
        when(dockerService.createContainer(
                projectId + ":abc123", 8080, containerName(), "network-id"
        )).thenReturn("container-id");

        DeploymentResult result = strategy.prepare(deployment, directory);

        assertEquals("abc123", result.commitHash());
        assertEquals(3, result.resources().size());
        assertEquals(ResourceType.NETWORK, result.resources().get(0).type());
        assertEquals("network-id", result.resources().get(0).externalId());
        assertEquals("net-12345678", result.resources().get(0).resourceName());
        assertEquals(ResourceType.IMAGE, result.resources().get(1).type());
        assertEquals("image-id", result.resources().get(1).externalId());
        assertEquals(projectId + ":abc123", result.resources().get(1).resourceName());
        assertEquals(ResourceType.CONTAINER, result.resources().get(2).type());
        assertEquals("container-id", result.resources().get(2).externalId());
        assertEquals(containerName(), result.resources().get(2).resourceName());
        verify(dockerService).createContainer(
                projectId + ":abc123", 8080, containerName(), "network-id"
        );
    }

    @Test
    void shouldTearDownNetworkWhenImageBuildFails() throws Exception {
        when(dockerService.createNetwork(deploymentId)).thenReturn("network-id");
        when(dockerService.getDeploymentNetworkName(deploymentId)).thenReturn("net-12345678");
        when(gitService.getLatestCommit(anyString()))
                .thenReturn(GitCommit.builder().hash("abc123").build());
        doThrow(new RuntimeException("build failed"))
                .when(dockerService).buildImage(anyString(), anyString());

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> strategy.prepare(deployment, directory)
        );

        assertEquals("build failed", thrown.getMessage());
        verify(dockerService).tearDownDeployment(null, "network-id");
        verify(dockerService, never()).removeImage(anyString());
    }

    @Test
    void shouldRemoveBuiltImageWhenContainerCreationFails() throws Exception {
        when(dockerService.createNetwork(deploymentId)).thenReturn("network-id");
        when(dockerService.getDeploymentNetworkName(deploymentId)).thenReturn("net-12345678");
        when(gitService.getLatestCommit(anyString()))
                .thenReturn(GitCommit.builder().hash("abc123").build());
        when(dockerService.buildImage(anyString(), anyString())).thenReturn("image-id");
        when(dockerService.getExposedImagePort(projectId + ":abc123"))
                .thenThrow(new IllegalStateException("no port"));

        assertThrows(
                IllegalStateException.class,
                () -> strategy.prepare(deployment, directory)
        );

        verify(dockerService).tearDownDeployment(null, "network-id");
        verify(dockerService).removeImage(projectId + ":abc123");
    }

    @Test
    void shouldStartContainerWithPersistedIds() {
        attachResource(deployment, ResourceType.CONTAINER, "container-id", containerName());
        attachResource(deployment, ResourceType.NETWORK, "network-id", "net-12345678");

        strategy.activate(deployment);

        verify(dockerService).startContainer("container-id", "network-id");
    }

    @Test
    void shouldTearDownWithOnlyAvailableResources() {
        attachResource(deployment, ResourceType.NETWORK, "network-id", "net-12345678");

        strategy.teardown(deployment);

        verify(dockerService).tearDownDeployment(null, "network-id");
    }

    @Test
    void shouldRecoverContainerWithPersistedIds() {
        attachResource(deployment, ResourceType.CONTAINER, "container-id", containerName());
        attachResource(deployment, ResourceType.NETWORK, "network-id", "net-12345678");

        strategy.recover(deployment);

        verify(dockerService).recoverContainer("container-id", "network-id");
    }
}
