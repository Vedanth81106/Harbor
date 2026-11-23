package com.waffles.harbor.deployment;

import com.waffles.harbor.auth.Role;
import com.waffles.harbor.deployment.enums.DeploymentStatus;
import com.waffles.harbor.deployment.enums.DeploymentType;
import com.waffles.harbor.deployment.enums.ResourceType;
import com.waffles.harbor.project.Project;
import com.waffles.harbor.project.ProjectRepository;
import com.waffles.harbor.user.User;
import com.waffles.harbor.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DeploymentRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    private User createAndSaveUser(String username) {
        User user = User.builder()
                .username(username)
                .email(username + "@gmail.com")
                .passwordHash("pwd")
                .role(Role.USER)
                .build();
        return userRepository.save(user);
    }

    private Project createAndSaveProject(User user, String name) {
        Project project = Project.builder()
                .user(user)
                .projectName(name)
                .build();
        return projectRepository.save(project);
    }

    private Deployment createDeployment(Project project, DeploymentStatus status) {
        return Deployment.builder()
                .project(project)
                .deploymentType(DeploymentType.DOCKERFILE)
                .status(status)
                .build();
    }

    private void addImage(Deployment deployment, String imageName) {
        deployment.addResource(DeploymentResource.builder()
                .resourceType(ResourceType.IMAGE)
                .externalId("image-id")
                .resourceName(imageName)
                .build());
    }

    @Test
    void shouldFindDeploymentsByProjectOrderedByCreatedAtDesc() {
        User user = createAndSaveUser("testUser");
        Project project = createAndSaveProject(user, "project");

        Deployment first = deploymentRepository.save(createDeployment(project, DeploymentStatus.QUEUED));
        Deployment second = deploymentRepository.save(createDeployment(project, DeploymentStatus.READY));
        Deployment third = deploymentRepository.save(createDeployment(project, DeploymentStatus.FAILED));

        List<Deployment> deployments =
                deploymentRepository.findByProject_ProjectIdOrderByCreatedAtDesc(project.getProjectId());

        assertEquals(3, deployments.size());
        assertEquals(third.getDeploymentId(), deployments.get(0).getDeploymentId());
        assertEquals(second.getDeploymentId(), deployments.get(1).getDeploymentId());
        assertEquals(first.getDeploymentId(), deployments.get(2).getDeploymentId());
    }

    @Test
    void shouldReturnEmptyWhenProjectHasNoDeployments() {
        User user = createAndSaveUser("testUser");
        Project project = createAndSaveProject(user, "project");

        List<Deployment> deployments =
                deploymentRepository.findByProject_ProjectIdOrderByCreatedAtDesc(project.getProjectId());

        assertTrue(deployments.isEmpty());
    }

    @Test
    void shouldFindDeploymentByUserAndDeploymentId() {
        User user = createAndSaveUser("testUser");
        Project project = createAndSaveProject(user, "project");
        Deployment saved = deploymentRepository.save(createDeployment(project, DeploymentStatus.QUEUED));

        Optional<Deployment> result = deploymentRepository
                .findByDeploymentIdAndProject_User(saved.getDeploymentId(), user);

        assertTrue(result.isPresent());
        assertEquals(saved.getDeploymentId(), result.get().getDeploymentId());
    }

    @Test
    void shouldNotReturnAnotherUsersDeployment() {
        User owner = createAndSaveUser("owner");
        User other = createAndSaveUser("other");

        Project project = createAndSaveProject(owner, "project");
        Deployment saved = deploymentRepository.save(createDeployment(project, DeploymentStatus.QUEUED));

        Optional<Deployment> result = deploymentRepository
                .findByDeploymentIdAndProject_User(saved.getDeploymentId(), other);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnTrueWhenImageReferenceExists() {
        User user = createAndSaveUser("testUser");
        Project project = createAndSaveProject(user, "project");

        Deployment deployment = createDeployment(project, DeploymentStatus.READY);
        addImage(deployment, "project:1");
        deploymentRepository.save(deployment);

        assertTrue(deploymentRepository.existsImageReference(
                UUID.randomUUID(), ResourceType.IMAGE, "project:1"
        ));
    }

    @Test
    void shouldReturnFalseWhenImageDoesNotExist() {
        assertFalse(deploymentRepository.existsImageReference(
                UUID.randomUUID(), ResourceType.IMAGE, "project:1"
        ));
    }

    @Test
    void shouldDetectImageReferencedByAnotherDeployment() {
        User user = createAndSaveUser("testUser");
        Project project = createAndSaveProject(user, "project");

        Deployment first = createDeployment(project, DeploymentStatus.READY);
        addImage(first, "project:1");
        deploymentRepository.save(first);

        Deployment second = createDeployment(project, DeploymentStatus.READY);
        addImage(second, "project:1");
        deploymentRepository.save(second);

        assertTrue(deploymentRepository.existsImageReference(
                first.getDeploymentId(), ResourceType.IMAGE, "project:1"
        ));
    }

    @Test
    void shouldReturnFalseWhenImageIsOnlyReferencedByGivenDeployment() {
        User user = createAndSaveUser("testUser");
        Project project = createAndSaveProject(user, "project");

        Deployment deployment = createDeployment(project, DeploymentStatus.READY);
        addImage(deployment, "project:1");
        deploymentRepository.save(deployment);

        assertFalse(deploymentRepository.existsImageReference(
                deployment.getDeploymentId(), ResourceType.IMAGE, "project:1"
        ));
    }

    @Test
    void shouldDetectImageOutsideGivenDeployments() {
        User user = createAndSaveUser("testUser");
        Project project = createAndSaveProject(user, "project");

        Deployment inside = createDeployment(project, DeploymentStatus.READY);
        addImage(inside, "project:1");
        deploymentRepository.save(inside);

        Deployment outside = createDeployment(project, DeploymentStatus.READY);
        addImage(outside, "project:1");
        deploymentRepository.save(outside);

        assertTrue(deploymentRepository.existsImageReferenceOutside(
                Set.of(inside.getDeploymentId()), ResourceType.IMAGE, "project:1"
        ));
        assertFalse(deploymentRepository.existsImageReferenceOutside(
                Set.of(inside.getDeploymentId(), outside.getDeploymentId()),
                ResourceType.IMAGE,
                "project:1"
        ));
    }

    @Test
    void shouldFindDeploymentsByStatus() {
        User user = createAndSaveUser("testUser");
        Project project = createAndSaveProject(user, "project");

        deploymentRepository.save(createDeployment(project, DeploymentStatus.QUEUED));
        deploymentRepository.save(createDeployment(project, DeploymentStatus.READY));
        deploymentRepository.save(createDeployment(project, DeploymentStatus.READY));

        List<Deployment> ready = deploymentRepository.findByStatus(DeploymentStatus.READY);

        assertEquals(2, ready.size());
        assertTrue(ready.stream().allMatch(d -> d.getStatus() == DeploymentStatus.READY));
    }
}
