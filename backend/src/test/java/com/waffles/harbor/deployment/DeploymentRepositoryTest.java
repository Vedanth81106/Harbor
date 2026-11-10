package com.waffles.harbor.deployment;

import com.waffles.harbor.auth.Role;
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
                .status(status)
                .build();
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
    void shouldReturnTrueWhenImageExists() {
        User user = createAndSaveUser("testUser");
        Project project = createAndSaveProject(user, "project");

        Deployment deployment = createDeployment(project, DeploymentStatus.READY);
        deployment.setImageName("project:1");
        deploymentRepository.save(deployment);

        assertTrue(deploymentRepository.existsByImageName("project:1"));
    }

    @Test
    void shouldReturnFalseWhenImageDoesNotExist() {
        assertFalse(deploymentRepository.existsByImageName("project:1"));
    }

    @Test
    void shouldDetectImageReferencedByAnotherDeployment() {
        User user = createAndSaveUser("testUser");
        Project project = createAndSaveProject(user, "project");

        Deployment first = createDeployment(project, DeploymentStatus.READY);
        first.setImageName("project:1");
        deploymentRepository.save(first);

        Deployment second = createDeployment(project, DeploymentStatus.READY);
        second.setImageName("project:1");
        deploymentRepository.save(second);

        assertTrue(deploymentRepository.existsByImageNameAndDeploymentIdNot(
                "project:1",
                first.getDeploymentId()
        ));
    }

    @Test
    void shouldReturnFalseWhenImageIsOnlyReferencedByGivenDeployment() {
        User user = createAndSaveUser("testUser");
        Project project = createAndSaveProject(user, "project");

        Deployment deployment = createDeployment(project, DeploymentStatus.READY);
        deployment.setImageName("project:1");
        deploymentRepository.save(deployment);

        assertFalse(deploymentRepository.existsByImageNameAndDeploymentIdNot(
                "project:1",
                deployment.getDeploymentId()
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
