package com.waffles.harbor.deployment;

import com.waffles.harbor.auth.Role;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeploymentServiceTest {

    @Mock
    private DeploymentRepository deploymentRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private GitService gitService;

    @Mock
    private DockerService dockerService;

    @Mock
    private SseService sseService;

    @InjectMocks
    private DeploymentService deploymentService;

    @TempDir
    Path tempDir;

    private UUID userId;
    private UUID projectId;
    private UUID deploymentId;

    private User user;
    private Project project;
    private Deployment deployment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(deploymentService, "path", tempDir.toString());

        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        deploymentId = UUID.randomUUID();

        user = User.builder()
                .userId(userId)
                .username("happy")
                .email("happy@gmail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        project = Project.builder()
                .projectId(projectId)
                .projectName("project")
                .repositoryUrl("https://github.com/user/repo.git")
                .user(user)
                .deployments(new ArrayList<>())
                .build();

        deployment = Deployment.builder()
                .deploymentId(deploymentId)
                .project(project)
                .status(DeploymentStatus.QUEUED)
                .build();
    }

    private String deploymentDirectory() {
        return Path.of(
                tempDir.toString(),
                userId.toString(),
                projectId.toString(),
                deploymentId.toString()
        ).toString();
    }

    // ---------- queries ----------

    @Test
    void shouldRejectListingDeploymentsForMissingProject() {
        when(projectRepository.existsByUserAndProjectId(user, projectId)).thenReturn(false);

        assertThrows(
                ProjectNotFoundException.class,
                () -> deploymentService.getAllDeployments(user, projectId)
        );

        verify(deploymentRepository, never())
                .findByProject_ProjectIdOrderByCreatedAtDesc(any());
    }

    @Test
    void shouldListDeploymentsForOwnedProject() {
        when(projectRepository.existsByUserAndProjectId(user, projectId)).thenReturn(true);
        when(deploymentRepository.findByProject_ProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(List.of(deployment));

        List<DeploymentResponse> responses =
                deploymentService.getAllDeployments(user, projectId);

        assertEquals(1, responses.size());
        DeploymentResponse response = responses.getFirst();
        assertEquals(deploymentId, response.getDeploymentId());
        assertEquals(projectId, response.getProjectId());
        assertEquals(DeploymentStatus.QUEUED, response.getStatus());
    }

    @Test
    void shouldReturnDeploymentById() {
        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.of(deployment));

        DeploymentResponse response = deploymentService.getDeployment(user, deploymentId);

        assertEquals(deploymentId, response.getDeploymentId());
        assertEquals(projectId, response.getProjectId());
    }

    @Test
    void shouldRejectAccessToMissingDeployment() {
        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.empty());

        assertThrows(
                DeploymentNotFoundException.class,
                () -> deploymentService.getDeployment(user, deploymentId)
        );
    }

    @Test
    void shouldRejectLogsForMissingDeployment() {
        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.empty());

        assertThrows(
                DeploymentNotFoundException.class,
                () -> deploymentService.getDeploymentLogs(user, deploymentId)
        );

        verifyNoInteractions(sseService);
    }

    @Test
    void shouldRejectLogsWhenDeploymentHasNoContainer() {
        deployment.setContainerId(null);
        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.of(deployment));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> deploymentService.getDeploymentLogs(user, deploymentId)
        );

        assertTrue(exception.getMessage().contains("container"));
        verifyNoInteractions(sseService);
    }

    @Test
    void shouldStreamLogsFromDeploymentContainer() {
        deployment.setContainerId("container-id");
        SseEmitter emitter = new SseEmitter();
        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.of(deployment));
        when(sseService.streamContainerLogs("container-id")).thenReturn(emitter);

        SseEmitter result = deploymentService.getDeploymentLogs(user, deploymentId);

        assertSame(emitter, result);
    }

    // ---------- create ----------

    @Test
    void shouldRejectCreatingDeploymentForMissingProject() {
        when(projectRepository.findByUserAndProjectId(user, projectId))
                .thenReturn(Optional.empty());

        assertThrows(
                ProjectNotFoundException.class,
                () -> deploymentService.createDeployment(user, projectId)
        );

        verify(deploymentRepository, never()).save(any());
    }

    @Test
    void shouldCreateQueuedDeployment() {
        when(projectRepository.findByUserAndProjectId(user, projectId))
                .thenReturn(Optional.of(project));
        when(deploymentRepository.save(any(Deployment.class)))
                .thenAnswer(invocation -> {
                    Deployment saved = invocation.getArgument(0);
                    saved.setDeploymentId(deploymentId);
                    return saved;
                });

        DeploymentResponse response = deploymentService.createDeployment(user, projectId);

        ArgumentCaptor<Deployment> captor = ArgumentCaptor.forClass(Deployment.class);
        verify(deploymentRepository).save(captor.capture());

        Deployment saved = captor.getValue();
        assertEquals(DeploymentStatus.QUEUED, saved.getStatus());
        assertSame(project, saved.getProject());

        assertEquals(deploymentId, response.getDeploymentId());
        assertEquals(projectId, response.getProjectId());
        assertEquals(DeploymentStatus.QUEUED, response.getStatus());
    }

    // ---------- execute ----------

    @Test
    void shouldRejectExecutingMissingDeployment() {
        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.empty());

        assertThrows(
                DeploymentNotFoundException.class,
                () -> deploymentService.executeDeployment(user, deploymentId)
        );
    }

    @Test
    void shouldRejectExecutingDeploymentThatIsNotQueued() {
        deployment.setStatus(DeploymentStatus.READY);
        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.of(deployment));

        assertThrows(
                DeploymentStateException.class,
                () -> deploymentService.executeDeployment(user, deploymentId)
        );

        verifyNoInteractions(gitService, dockerService);
    }

    @Test
    void shouldExecuteQueuedDeployment() throws Exception {
        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.of(deployment));
        when(gitService.getLatestCommit(anyString()))
                .thenReturn(GitCommit.builder().hash("abc123").build());
        when(dockerService.getExposedImagePort(projectId + ":abc123")).thenReturn(8080);
        when(dockerService.createContainer(
                eq(deploymentId),
                eq(projectId + ":abc123"),
                eq(8080),
                eq("harbor-" + deploymentId.toString().substring(0, 8))
        )).thenReturn("container-id");

        DeploymentResponse response = deploymentService.executeDeployment(user, deploymentId);

        assertEquals(DeploymentStatus.READY, response.getStatus());
        assertEquals("abc123", deployment.getCommitHash());
        assertEquals(projectId + ":abc123", deployment.getImageName());
        assertEquals("container-id", deployment.getContainerId());

        verify(gitService).cloneRepository(
                "https://github.com/user/repo.git",
                deploymentDirectory(),
                60
        );
        verify(dockerService).buildImage(deploymentDirectory(), projectId + ":abc123");
        verify(dockerService).startContainer("container-id", deploymentId);
        verify(deploymentRepository, atLeast(4)).save(deployment);
    }

    @Test
    void shouldRejectExecutionWhenRepositoryIsNotConfigured() throws Exception {
        project.setRepositoryUrl(null);
        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.of(deployment));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> deploymentService.executeDeployment(user, deploymentId)
        );

        assertTrue(exception.getMessage().contains("repository"));
        assertEquals(DeploymentStatus.FAILED, deployment.getStatus());
        verifyNoInteractions(gitService);
        verify(dockerService).tearDownDeployment(null, deploymentId);
        verify(dockerService, never()).removeImage(any());
        // CLONING is persisted before the failure, then FAILED is persisted.
        verify(deploymentRepository, times(2)).save(deployment);
    }

    @Test
    void shouldMarkDeploymentFailedAndCleanUpWhenExecutionFails() throws Exception {
        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.of(deployment));
        doThrow(new RuntimeException("clone failed"))
                .when(gitService).cloneRepository(anyString(), anyString(), eq(60));

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> deploymentService.executeDeployment(user, deploymentId)
        );

        assertEquals("clone failed", thrown.getMessage());
        assertEquals(DeploymentStatus.FAILED, deployment.getStatus());
        verify(dockerService).tearDownDeployment(null, deploymentId);
        // CLONING is persisted before the failure, then FAILED is persisted.
        verify(deploymentRepository, times(2)).save(deployment);
    }

    @Test
    void shouldSuppressCleanupFailureWhenExecutionFails() throws Exception {
        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.of(deployment));

        RuntimeException primary = new RuntimeException("clone failed");
        doThrow(primary).when(gitService).cloneRepository(anyString(), anyString(), eq(60));
        doThrow(new RuntimeException("cleanup failed"))
                .when(dockerService).tearDownDeployment(nullable(String.class), any());

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> deploymentService.executeDeployment(user, deploymentId)
        );

        assertSame(primary, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertEquals("cleanup failed", thrown.getSuppressed()[0].getMessage());
    }

    // ---------- delete ----------

    @Test
    void shouldRejectDeletingMissingDeployment() {
        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.empty());

        assertThrows(
                DeploymentNotFoundException.class,
                () -> deploymentService.deleteDeployment(user, deploymentId)
        );

        verifyNoInteractions(dockerService);
    }

    @Test
    void shouldDeleteDeploymentAndItsResources() throws Exception {
        deployment.setContainerId("container-id");
        deployment.setImageName("project:1");

        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.of(deployment));
        when(deploymentRepository.existsByImageNameAndDeploymentIdNot("project:1", deploymentId))
                .thenReturn(false);

        Path directory = Path.of(deploymentDirectory());
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("file.txt"), "hello");

        deploymentService.deleteDeployment(user, deploymentId);

        verify(dockerService).tearDownDeployment("container-id", deploymentId);
        verify(dockerService).removeImage("project:1");
        verify(deploymentRepository).delete(deployment);
        assertFalse(Files.exists(directory));
    }

    @Test
    void shouldKeepImageWhenAnotherDeploymentReferencesIt() {
        deployment.setContainerId("container-id");
        deployment.setImageName("project:1");

        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.of(deployment));
        when(deploymentRepository.existsByImageNameAndDeploymentIdNot("project:1", deploymentId))
                .thenReturn(true);

        deploymentService.deleteDeployment(user, deploymentId);

        verify(dockerService, never()).removeImage(any());
    }

    @Test
    void shouldRejectDeletingDeploymentsForMissingProject() {
        when(projectRepository.findByUserAndProjectId(user, projectId))
                .thenReturn(Optional.empty());

        assertThrows(
                ProjectNotFoundException.class,
                () -> deploymentService.deleteAllDeploymentsForProject(user, projectId)
        );

        verifyNoInteractions(dockerService);
    }

    @Test
    void shouldDeleteAllDeploymentsForProject() {
        deployment.setContainerId("first-container");
        deployment.setImageName("project:1");

        Deployment second = Deployment.builder()
                .deploymentId(UUID.randomUUID())
                .project(project)
                .status(DeploymentStatus.READY)
                .containerId("second-container")
                .imageName("project:2")
                .build();

        project.getDeployments().add(deployment);
        project.getDeployments().add(second);

        when(projectRepository.findByUserAndProjectId(user, projectId))
                .thenReturn(Optional.of(project));
        when(deploymentRepository.existsByImageName("project:1")).thenReturn(false);
        when(deploymentRepository.existsByImageName("project:2")).thenReturn(false);

        deploymentService.deleteAllDeploymentsForProject(user, projectId);

        verify(dockerService).tearDownDeployment("first-container", deploymentId);
        verify(dockerService).tearDownDeployment("second-container", second.getDeploymentId());
        // Each image is removed during per-deployment cleanup and again by the
        // image de-duplication pass.
        verify(dockerService, times(2)).removeImage("project:1");
        verify(dockerService, times(2)).removeImage("project:2");
        verify(projectRepository).save(project);
        assertTrue(project.getDeployments().isEmpty());
    }

    @Test
    void shouldNotRemoveImageStillReferencedByAnotherDeployment() {
        deployment.setContainerId("container-id");
        deployment.setImageName("project:1");
        project.getDeployments().add(deployment);

        when(projectRepository.findByUserAndProjectId(user, projectId))
                .thenReturn(Optional.of(project));
        when(deploymentRepository.existsByImageNameAndDeploymentIdNot("project:1", deploymentId))
                .thenReturn(true);
        when(deploymentRepository.existsByImageName("project:1")).thenReturn(true);

        deploymentService.deleteAllDeploymentsForProject(user, projectId);

        verify(dockerService, never()).removeImage(any());
    }
}
