package com.waffles.harbor.project;

import com.waffles.harbor.auth.Role;
import com.waffles.harbor.exception.ProjectNotFoundException;
import com.waffles.harbor.security.JwtService;
import com.waffles.harbor.user.User;
import com.waffles.harbor.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    private User createUser(String username){

        return User.builder()
                .userId(UUID.randomUUID())
                .username(username)
                .email(username+"@gmail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();
    }

    private ProjectResponse createProjectResponse(String projectName){

        return ProjectResponse.builder()
                .projectId(UUID.randomUUID())
                .projectName(projectName)
                .build();
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ProjectService projectService;

    @Test
    void shouldGetAllProjects() throws Exception {

        User user = createUser("happy");

        ProjectResponse project1 = createProjectResponse("project1");
        ProjectResponse project2 = createProjectResponse("project2");
        ProjectResponse project3 = createProjectResponse("project3");

        when(projectService.getAllProjectsByUser(user))
                .thenReturn(List.of(project1,project2,project3));

        mockMvc.perform(
                        get("/projects")
                                .with(authentication(
                                        new UsernamePasswordAuthenticationToken(
                                                user,
                                                null,
                                                user.getAuthorities()
                                        )
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].projectName").value("project1"))
                .andExpect(jsonPath("$[1].projectName").value("project2"));

        verify(projectService).getAllProjectsByUser(user);
    }

    @Test
    void shouldGetProjectByProjectIdSuccessfully() throws Exception{

        User user = createUser("happy");
        ProjectResponse projectResponse = createProjectResponse("hp");
        UUID projectId = projectResponse.getProjectId();

        when(projectService.getProjectByProjectId(user,projectId))
                .thenReturn(projectResponse);

        mockMvc.perform(
                get("/projects/"+projectId)
                        .with(authentication(
                                new UsernamePasswordAuthenticationToken(
                                        user,
                                        null,
                                        user.getAuthorities()
                                )
                        ))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectName").value("hp"));

        verify(projectService).getProjectByProjectId(user,projectId);
    }

    @Test
    void shouldCreateProjectSuccessfully() throws Exception {

        User user = createUser("happy");

        ProjectResponse projectResponse =
                createProjectResponse("happy-project-response");

        when(projectService.createProject(
                eq(user),
                any(ProjectRequest.class)
        )).thenReturn(projectResponse);

        mockMvc.perform(
                        post("/projects")
                                .with(csrf())
                                .with(authentication(
                                        new UsernamePasswordAuthenticationToken(
                                                user,
                                                null,
                                                user.getAuthorities()
                                        )
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "projectName":"happy-project-request"
                            }
                            """)

                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectName")
                        .value("happy-project-response"));

        ArgumentCaptor<ProjectRequest> captor =
                ArgumentCaptor.forClass(ProjectRequest.class);

        verify(projectService).createProject(eq(user), captor.capture());

        assertEquals(
                "happy-project-request",
                captor.getValue().getProjectName()
        );
    }

    @Test
    void shouldRejectWhenProjectDoesNotExist() throws Exception{

        User user = createUser("sad");
        UUID projectId = UUID.randomUUID();

        when(projectService.getProjectByProjectId(user,projectId))
                .thenThrow(new IllegalArgumentException("Project not found!"));

        mockMvc.perform(
                        get("/projects/"+projectId)
                                .with(authentication(
                                        new UsernamePasswordAuthenticationToken(
                                                user,
                                                null,
                                                user.getAuthorities()
                                        )
                                ))
                )
                .andExpect(status().isBadRequest());

        verify(projectService).getProjectByProjectId(user,projectId);
    }

    @Test
    void shouldRejectProjectCreationWithoutProjectName() throws Exception{
        User user = createUser("happy");

        mockMvc.perform(
                        post("/projects")
                                .with(csrf())
                                .with(authentication(
                                        new UsernamePasswordAuthenticationToken(
                                                user,
                                                null,
                                                user.getAuthorities()
                                        )
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "projectName":""
                            }
                            """)

                )
                .andExpect(status().isBadRequest());

        verify(projectService,never()).createProject(any(User.class), any(ProjectRequest.class));
    }

    @Test
    void shouldRejectUnauthenticatedAccess() throws Exception {

        mockMvc.perform(
                        get("/projects")
                )
                .andExpect(status().isUnauthorized());

        verify(projectService, never())
                .getAllProjectsByUser(any(User.class));
    }

    @Test
    void shouldAddRepositoryUrlSuccessfully() throws Exception {

        User user = createUser("happy");

        UUID projectId = UUID.randomUUID();

        ProjectResponse projectResponse =
                ProjectResponse.builder()
                        .projectId(projectId)
                        .projectName("happy-project")
                        .repositoryUrl("https://github.com/Vedanth81106/docs.git")
                        .build();

        when(projectService.addRepositoryUrl(
                user,
                projectId,
                "https://github.com/Vedanth81106/docs.git"
        )).thenReturn(projectResponse);

        mockMvc.perform(
                        put("/projects/" + projectId)
                                .with(csrf())
                                .with(authentication(
                                        new UsernamePasswordAuthenticationToken(
                                                user,
                                                null,
                                                user.getAuthorities()
                                        )
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "repositoryUrl":"https://github.com/Vedanth81106/docs.git"
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectName").value("happy-project"))
                .andExpect(jsonPath("$.repositoryUrl")
                        .value("https://github.com/Vedanth81106/docs.git"));

        verify(projectService).addRepositoryUrl(
                user,
                projectId,
                "https://github.com/Vedanth81106/docs.git"
        );
    }

    @Test
    void shouldRejectBlankRepositoryUrl() throws Exception {

        User user = createUser("happy");
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(
                        put("/projects/" + projectId)
                                .with(csrf())
                                .with(authentication(
                                        new UsernamePasswordAuthenticationToken(
                                                user,
                                                null,
                                                user.getAuthorities()
                                        )
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "repositoryUrl":""
                                }
                                """)
                )
                .andExpect(status().isBadRequest());

        verify(projectService, never())
                .addRepositoryUrl(
                        any(User.class),
                        any(UUID.class),
                        anyString()
                );
    }

    @Test
    void shouldRejectAddingRepositoryUrlWhenProjectDoesNotExist() throws Exception {

        User user = createUser("sad");
        UUID projectId = UUID.randomUUID();

        when(projectService.addRepositoryUrl(
                user,
                projectId,
                "https://github.com/Vedanth81106/docs.git"
        )).thenThrow(new ProjectNotFoundException());

        mockMvc.perform(
                        put("/projects/" + projectId)
                                .with(csrf())
                                .with(authentication(
                                        new UsernamePasswordAuthenticationToken(
                                                user,
                                                null,
                                                user.getAuthorities()
                                        )
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "repositoryUrl":"https://github.com/Vedanth81106/docs.git"
                                }
                                """)
                )
                .andExpect(status().isNotFound());

        verify(projectService).addRepositoryUrl(
                user,
                projectId,
                "https://github.com/Vedanth81106/docs.git"
        );
    }
}