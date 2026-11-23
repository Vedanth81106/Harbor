package com.waffles.harbor.deployment;

import com.waffles.harbor.auth.Role;
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

    @Mock
    private ComposeFileDetector composeFileDetector;

    @Mock
    private DeploymentStrategyFactory deploymentStrategyFactory;

    @Mock
    private DeploymentStrategy strategy;

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
                .deploymentType(DeploymentType.DOCKERFILE)
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

    private void createDockerfile() throws Exception {
        Path directory = Path.of(deploymentDirectory());
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("Dockerfile"), "FROM scratch");
    }

    private void attachResources(Deployment target, String imageName) {
        target.addResource(DeploymentResource.builder()
                .resourceType(ResourceType.NETWORK)
                .externalId("network-id")
                .resourceName("net-12345678")
                .build());
        target.addResource(DeploymentResource.builder()
                .resourceType(ResourceType.IMAGE)
                .externalId("image-id")
                .resourceName(imageName)
                .build());
        target.addResource(DeploymentResource.builder()
                .resourceType(ResourceType.CONTAINER)
                .externalId("container-id")
                .resourceName("harbor-12345678")
                .build());
    }

    private DeploymentResult preparedResult() {
        return new DeploymentResult("abc123", List.of(
                new DeploymentResult.Result(ResourceType.NETWORK, "network-id", "net-12345678", null),
                new DeploymentResult.Result(ResourceType.IMAGE, "image-id", projectId + ":abc123", null),
                new DeploymentResult.Result(ResourceType.CONTAINER, "container-id", "harbor-12345678", null)
        ));
    }

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
        attachResources(deployment, projectId + ":abc123");
        SseEmitter emitter = new SseEmitter();
        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.of(deployment));
        when(sseService.streamContainerLogs("container-id")).thenReturn(emitter);

        SseEmitter result = deploymentService.getDeploymentLogs(user, deploymentId);

        assertSame(emitter, result);
    }

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
        assertEquals(DeploymentType.DOCKERFILE, saved.getDeploymentType());
        assertSame(project, saved.getProject());

        assertEquals(deploymentId, response.getDeploymentId());
        assertEquals(projectId, response.getProjectId());
        assertEquals(DeploymentStatus.QUEUED, response.getStatus());
    }

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

        verifyNoInteractions(gitService, dockerService, deploymentStrategyFactory);
    }

    @Test
    void shouldExecuteQueuedDeployment() throws Exception {
        createDockerfile();
        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.of(deployment));
        when(deploymentStrategyFactory.getStrategy(DeploymentType.DOCKERFILE))
                .thenReturn(strategy);
        when(strategy.prepare(deployment, Path.of(deploymentDirectory())))
                .thenReturn(preparedResult());

        DeploymentResponse response = deploymentService.executeDeployment(user, deploymentId);

        assertEquals(DeploymentStatus.READY, response.getStatus());
        assertEquals("abc123", deployment.getCommitHash());
        assertEquals(List.of(projectId + ":abc123"), response.getImageNames());
        assertEquals(3, deployment.getDeploymentResources().size());
        verify(gitService).cloneRepository(
                "https://github.com/user/repo.git",
                deploymentDirectory(),
                60
        );
        verify(strategy).prepare(deployment, Path.of(deploymentDirectory()));
        verify(strategy).activate(deployment);
    }

    @Test
    void shouldRejectComposeDeploymentUntilComposeIsImplemented() throws Exception {
        Path directory = Path.of(deploymentDirectory());
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("compose.yaml"), "services: {}");

        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.of(deployment));
        when(composeFileDetector.detectComposeFiles(deploymentDirectory()))
                .thenReturn(Optional.of(directory.resolve("compose.yaml")));
        when(deploymentStrategyFactory.getStrategy(DeploymentType.COMPOSE))
                .thenThrow(new DeploymentStateException(
                        "No strategy found for this deployment type: COMPOSE"
                ));

        assertThrows(
                DeploymentStateException.class,
                () -> deploymentService.executeDeployment(user, deploymentId)
        );

        assertEquals(DeploymentStatus.FAILED, deployment.getStatus());
        verify(deploymentStrategyFactory).getStrategy(DeploymentType.COMPOSE);
    }

    @Test
    void shouldPreferDockerfileWhenAComposeFileIsAlsoPresent() throws Exception {
        Path directory = Path.of(deploymentDirectory());
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("Dockerfile"), "FROM scratch");
        Files.writeString(directory.resolve("compose.yaml"), "services: {}");

        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.of(deployment));
        when(deploymentStrategyFactory.getStrategy(DeploymentType.DOCKERFILE))
                .thenReturn(strategy);
        when(strategy.prepare(deployment, Path.of(deploymentDirectory())))
                .thenReturn(preparedResult());

        DeploymentResponse response = deploymentService.executeDeployment(user, deploymentId);

        assertEquals(DeploymentStatus.READY, response.getStatus());
        assertEquals(DeploymentType.DOCKERFILE, deployment.getDeploymentType());
        verify(deploymentStrategyFactory).getStrategy(DeploymentType.DOCKERFILE);
    }

    @Test
    void shouldRejectExecutionWhenRepositoryIsNotConfigured() {
        project.setRepositoryUrl(null);
        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.of(deployment));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> deploymentService.executeDeployment(user, deploymentId)
        );

        assertTrue(exception.getMessage().contains("repository"));
        assertEquals(DeploymentStatus.FAILED, deployment.getStatus());
        verifyNoInteractions(gitService, deploymentStrategyFactory);
        verify(dockerService, never()).removeImage(any());
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
        verifyNoInteractions(deploymentStrategyFactory);
    }

    @Test
    void shouldSuppressCleanupFailureWhenExecutionFails() throws Exception {
        createDockerfile();
        attachResources(deployment, projectId + ":abc123");
        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.of(deployment));
        when(deploymentStrategyFactory.getStrategy(DeploymentType.DOCKERFILE))
                .thenReturn(strategy);
        when(strategy.prepare(any(), any()))
                .thenThrow(new RuntimeException("prepare failed"));
        doThrow(new RuntimeException("cleanup failed")).when(strategy).teardown(deployment);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> deploymentService.executeDeployment(user, deploymentId)
        );

        assertEquals("prepare failed", thrown.getMessage());
        assertEquals(1, thrown.getSuppressed().length);
        assertEquals("cleanup failed", thrown.getSuppressed()[0].getMessage());
    }

    @Test
    void shouldRejectDeletingMissingDeployment() {
        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.empty());

        assertThrows(
                DeploymentNotFoundException.class,
                () -> deploymentService.deleteDeployment(user, deploymentId)
        );

        verifyNoInteractions(dockerService, deploymentStrategyFactory);
    }

    @Test
    void shouldDeleteDeploymentAndItsResources() throws Exception {
        attachResources(deployment, "project:1");
        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.of(deployment));
        when(deploymentStrategyFactory.getStrategy(DeploymentType.DOCKERFILE))
                .thenReturn(strategy);
        when(deploymentRepository.existsImageReference(
                deploymentId, ResourceType.IMAGE, "project:1"
        )).thenReturn(false);

        Path directory = Path.of(deploymentDirectory());
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("file.txt"), "hello");

        deploymentService.deleteDeployment(user, deploymentId);

        verify(strategy).teardown(deployment);
        verify(dockerService).removeImage("project:1");
        verify(deploymentRepository).delete(deployment);
        assertFalse(Files.exists(directory));
    }

    @Test
    void shouldKeepImageWhenAnotherDeploymentReferencesIt() {
        attachResources(deployment, "project:1");
        when(deploymentRepository.findByDeploymentIdAndProject_User(deploymentId, user))
                .thenReturn(Optional.of(deployment));
        when(deploymentStrategyFactory.getStrategy(DeploymentType.DOCKERFILE))
                .thenReturn(strategy);
        when(deploymentRepository.existsImageReference(
                deploymentId, ResourceType.IMAGE, "project:1"
        )).thenReturn(true);

        deploymentService.deleteDeployment(user, deploymentId);

        verify(strategy).teardown(deployment);
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

        verifyNoInteractions(dockerService, deploymentStrategyFactory);
    }

    @Test
    void shouldDeleteAllDeploymentsForProject() {
        attachResources(deployment, "project:1");

        Deployment second = Deployment.builder()
                .deploymentId(UUID.randomUUID())
                .project(project)
                .deploymentType(DeploymentType.DOCKERFILE)
                .status(DeploymentStatus.READY)
                .build();
        attachResources(second, "project:2");

        project.getDeployments().add(deployment);
        project.getDeployments().add(second);

        when(projectRepository.findByUserAndProjectId(user, projectId))
                .thenReturn(Optional.of(project));
        when(deploymentStrategyFactory.getStrategy(DeploymentType.DOCKERFILE))
                .thenReturn(strategy);

        deploymentService.deleteAllDeploymentsForProject(user, projectId);

        verify(strategy).teardown(deployment);
        verify(strategy).teardown(second);
        verify(dockerService).removeImage("project:1");
        verify(dockerService).removeImage("project:2");
        verify(projectRepository).save(project);
        assertTrue(project.getDeployments().isEmpty());
    }

    @Test
    void shouldNotRemoveImageStillReferencedByAnotherDeployment() {
        attachResources(deployment, "project:1");
        project.getDeployments().add(deployment);

        when(projectRepository.findByUserAndProjectId(user, projectId))
                .thenReturn(Optional.of(project));
        when(deploymentStrategyFactory.getStrategy(DeploymentType.DOCKERFILE))
                .thenReturn(strategy);
        when(deploymentRepository.existsImageReferenceOutside(
                anyCollection(), eq(ResourceType.IMAGE), eq("project:1")
        )).thenReturn(true);

        deploymentService.deleteAllDeploymentsForProject(user, projectId);

        verify(strategy).teardown(deployment);
        verify(dockerService, never()).removeImage(any());
    }
}
