package com.waffles.harbor.project;

import com.waffles.harbor.auth.Role;
import com.waffles.harbor.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    private User createUser(String username){
        return User.builder()
                .username(username)
                .email(username+"@gmail.com")
                .role(Role.USER)
                .passwordHash("hash")
                .build();
    }

    private Project createProject(String projectName, User user){
        return Project.builder()
                .projectName(projectName)
                .user(user)
                .build();
    }

    @Test
    void shouldCreateProjectSuccessfully(){

        User user = createUser("happy");
        UUID randomId = UUID.randomUUID();
        ProjectRequest projectRequest = new ProjectRequest("hp");
        Project project = createProject(projectRequest.getProjectName(),user);
        project.setProjectId(randomId);

        when(projectRepository.existsByUserAndProjectNameIgnoreCase(user,"hp"))
                .thenReturn(false);
        when(projectRepository.save(any(Project.class)))
                .thenReturn(project);

        ProjectResponse projectResponse = projectService.createProject(user,projectRequest);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        Project savedProject = captor.getValue();

        assertEquals("hp",savedProject.getProjectName());
        assertEquals("happy",savedProject.getUser().getUsername());

        assertNotNull(projectResponse);
        assertEquals("hp", projectResponse.getProjectName());
        assertEquals(randomId,projectResponse.getProjectId());
    }

    @Test
    void shouldRejectDuplicateProjectName(){

        User user = createUser("sad");
        ProjectRequest projectRequest = new ProjectRequest("sp");

        when(projectRepository.existsByUserAndProjectNameIgnoreCase(user,"sp"))
                .thenReturn(true);

        assertThrows(
                ResponseStatusException.class,
                () -> projectService.createProject(user,projectRequest)
        );

        verify(projectRepository,never()).save(any());
        verify(projectRepository).existsByUserAndProjectNameIgnoreCase(user,"sp");
    }

    @Test
    void shouldGetAllProjects() {

        User user = createUser("test");

        Project project1 = createProject("project1", user);
        Project project2 = createProject("project2", user);
        Project project3 = createProject("project3", user);

        when(projectRepository.findByUserOrderByCreatedAtDesc(user))
                .thenReturn(List.of(project3, project2, project1));

        List<ProjectResponse> projects = projectService.getAllProjectsByUser(user);

        verify(projectRepository).findByUserOrderByCreatedAtDesc(user);

        assertEquals(3, projects.size());

        assertEquals("project3", projects.get(0).getProjectName());
        assertEquals("project2", projects.get(1).getProjectName());
        assertEquals("project1", projects.get(2).getProjectName());
    }

    @Test
    void shouldReturnEmptyWhenUserHasNoProjects(){

        User user = createUser("test");

        when(projectRepository.findByUserOrderByCreatedAtDesc(user))
                .thenReturn(List.of());

        List<ProjectResponse> projects = projectService.getAllProjectsByUser(user);

        verify(projectRepository).findByUserOrderByCreatedAtDesc(user);
        assertTrue(projects.isEmpty());
    }

    @Test
    void shouldGetProjectByProjectId(){

        User user = createUser("happy");
        Project project = createProject("hp",user);
        UUID projectId = UUID.randomUUID();
        project.setProjectId(projectId);

        when(projectRepository.findByUserAndProjectId(user,projectId))
                .thenReturn(Optional.of(project));

        ProjectResponse projectResponse = projectService.getProjectByProjectId(user,projectId);

        verify(projectRepository).findByUserAndProjectId(user,projectId);

        assertEquals("hp",projectResponse.getProjectName());
        assertEquals(projectResponse.getProjectId(),projectId);
    }

    @Test
    void shouldRejectWhenProjectDoesNotExist(){

        User user = createUser("sad");
        UUID projectId = UUID.randomUUID();

        when(projectRepository.findByUserAndProjectId(user,projectId))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> projectService.getProjectByProjectId(user,projectId)
        );

        verify(projectRepository).findByUserAndProjectId(user,projectId);
    }

}
