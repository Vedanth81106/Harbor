package com.waffles.harbor.dockerfile;

import com.waffles.harbor.docker.DockerService;
import com.waffles.harbor.git.GitCommit;
import com.waffles.harbor.git.GitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DockerfileServiceTest {

    @Mock
    private DockerService dockerService;

    @Mock
    private GitService gitService;

    @InjectMocks
    private DockerfileService dockerfileService;

    private final UUID projectId = UUID.randomUUID();
    private final UUID deploymentId = UUID.randomUUID();

    private String containerName() {
        return "harbor-" + deploymentId.toString().substring(0, 8);
    }

    // ---------- buildImage ----------

    @Test
    void shouldBuildImageTaggedWithLatestCommit() throws Exception {
        when(gitService.getLatestCommit("/tmp/repo"))
                .thenReturn(GitCommit.builder().hash("abc123").build());

        DockerfileService.BuiltImage image =
                dockerfileService.buildImage("/tmp/repo", projectId);

        assertEquals("abc123", image.commitHash());
        assertEquals(projectId + ":abc123", image.imageName());
        verify(dockerService).buildImage("/tmp/repo", projectId + ":abc123");
    }

    @Test
    void shouldPropagateBuildFailure() throws Exception {
        when(gitService.getLatestCommit(anyString()))
                .thenReturn(GitCommit.builder().hash("abc123").build());
        doThrow(new RuntimeException("build failed"))
                .when(dockerService).buildImage(anyString(), anyString());

        RuntimeException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> dockerfileService.buildImage("/tmp/repo", projectId)
        );

        assertEquals("build failed", thrown.getMessage());
    }

    // ---------- createContainer ----------

    @Test
    void shouldCreateContainerWithHarborNameAndExposedPort() {
        when(dockerService.getExposedImagePort(projectId + ":abc123"))
                .thenReturn(8080);
        when(dockerService.createContainer(
                deploymentId,
                projectId + ":abc123",
                8080,
                containerName()
        )).thenReturn("container-id");

        String containerId = dockerfileService.createContainer(
                deploymentId,
                projectId + ":abc123"
        );

        assertEquals("container-id", containerId);
        verify(dockerService).createContainer(
                deploymentId,
                projectId + ":abc123",
                8080,
                containerName()
        );
    }

    // ---------- startContainer / deleteContainer ----------

    @Test
    void shouldStartContainer() {
        dockerfileService.startContainer("container-id", deploymentId);

        verify(dockerService).startContainer("container-id", deploymentId);
    }

    @Test
    void shouldTearDownContainer() {
        dockerfileService.deleteContainer(deploymentId, "container-id");

        verify(dockerService).tearDownDeployment("container-id", deploymentId);
    }
}
