package com.waffles.harbor.project;

import com.waffles.harbor.auth.Role;
import com.waffles.harbor.user.User;
import com.waffles.harbor.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
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
public class ProjectRepositoryTest {

    @Container
    @ServiceConnection // uses the give container as a datasource instead of writing config in application.props
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    private User createAndSaveUser(String username) {
        User user = User.builder()
                .username(username)
                .email(username+"@gmail.com")
                .passwordHash("pwd")
                .role(Role.USER)
                .build();
        return userRepository.save(user);
    }

    private Project createProject(User user, String projectName){
        return Project.builder()
                .user(user)
                .projectName(projectName)
                .build();
    }

    @Test
    void shouldFindProjectsByUserOrderedByCreatedAtDesc(){
        User activeUser = createAndSaveUser("testUser");

        Project project1 = createProject(activeUser, "project1");
        projectRepository.save(project1);
        Project project2 = createProject(activeUser, "project2");
        projectRepository.save(project2);
        Project project3 = createProject(activeUser, "project3");
        projectRepository.save(project3);

        List<Project> projectList = projectRepository.findByUserOrderByCreatedAtDesc(activeUser);

        assertEquals(3, projectList.size());
        assertEquals("project3", projectList.get(0).getProjectName());
        assertEquals("project2", projectList.get(1).getProjectName());
        assertEquals("project1", projectList.get(2).getProjectName());
    }

    @Test
    void shouldReturnEmptyWhenUserHasNoProject(){
        User emptyUser = createAndSaveUser("testUser");

        List<Project> projectList = projectRepository.findByUserOrderByCreatedAtDesc(emptyUser);

        assertTrue(projectList.isEmpty());
    }

    @Test
    void shouldFindProjectByUserAndProjectNameIgnoreCase(){

        User user = createAndSaveUser("testUser");
        Project project = createProject(user,"Calculator");
        projectRepository.save(project);

        Optional<Project> result= projectRepository.findByUserAndProjectNameIgnoreCase(user,"calculator");
        assertTrue(result.isPresent());

        Project found = result.get();
        assertEquals("Calculator", found.getProjectName());
    }

    @Test
    void shouldFindProjectByUserAndProjectId(){

        User user = createAndSaveUser("testUser");
        Project project = createProject(user,"name");
        Project saved = projectRepository.save(project);

        assertNotNull(saved.getProjectId());
        UUID projectId = saved.getProjectId();

        Optional<Project> result = projectRepository.findByUserAndProjectId(user, projectId);
        assertTrue(result.isPresent());

        assertEquals(result.get().getProjectId(),projectId);
    }

    @Test
    void shouldReturnTrueWhenProjectExists(){

        User user = createAndSaveUser("testUser");
        Project project = createProject(user,"exists");
        projectRepository.save(project);

        boolean exists = projectRepository.existsByUserAndProjectNameIgnoreCase(user,"exists");
        assertTrue(exists);
    }

    @Test
    void shouldReturnFalseWhenProjectNotExists(){

        User user = createAndSaveUser("testUser");

        boolean exists = projectRepository.existsByUserAndProjectNameIgnoreCase(user,"exists");
        assertFalse(exists);
    }

    @Test
    void shouldThrowExceptionWhenUserHasProjectsWithSameName(){

        User user = createAndSaveUser("testUser");

        Project project1 = createProject(user, "test");
        projectRepository.save(project1);
        Project project2 = createProject(user, "test");

        assertThrows(DataIntegrityViolationException.class,
                ()-> projectRepository.saveAndFlush(project2));
    }

    @Test
    void shouldNotReturnAnotherUsersProject(){

        User user1 = createAndSaveUser("testUser");
        User user2 = createAndSaveUser("testUser2");

        projectRepository.save(createProject(user1,"test1"));
        projectRepository.save(createProject(user2,"test2"));

        Optional<Project> result = projectRepository.findByUserAndProjectNameIgnoreCase(user1,"test1");
        assertTrue(result.isPresent());

        Project found = result.get();
        assertEquals("test1",found.getProjectName());

        Optional<Project> result2 = projectRepository.findByUserAndProjectNameIgnoreCase(user1,"test2");
        assertTrue(result2.isEmpty());
    }

    @Test
    void shouldAllowDifferentUsersToHaveSameProjectName(){

        User user1 = createAndSaveUser("testUser");
        User user2 = createAndSaveUser("testUser2");

        projectRepository.saveAndFlush(createProject(user1, "test1"));

        assertDoesNotThrow(() ->
                projectRepository.saveAndFlush(createProject(user2, "test1")));
    }
}
