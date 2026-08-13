package com.waffles.harbor.integration;

import com.waffles.harbor.project.Project;
import com.waffles.harbor.project.ProjectRepository;
import com.waffles.harbor.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
public class ProjectIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @BeforeEach
    void cleanDatabase() {
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerUser(String username, String email) throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "username": "%s",
                                        "email": "%s",
                                        "password": "password123"
                                    }
                                    """.formatted(username, email))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = jsonMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return json.get("accessToken").asText();
    }
    private UUID createProject(String token, String projectName) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/projects")
                                .with(csrf())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "projectName": "%s"
                            }
                            """.formatted(projectName))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = jsonMapper.readTree(
          result.getResponse().getContentAsString()
        );

        return UUID.fromString(json.get("projectId").asString());
    }

    @Test
    void shouldCreateProjectAndPersistToDatabase() throws Exception {

        String token = registerUser("happy","happy@gmail.com");

        mockMvc.perform(
                        post("/projects")
                                .with(csrf())
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "projectName":"happy-project"
                                    }
                                    """)

                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectName").value("happy-project"));

        List<Project> projects = projectRepository.findByUserOrderByCreatedAtDesc(userRepository.findByUsername("happy").orElseThrow());
        assertEquals(1, projects.size());
        Project savedProject = projects.getFirst();

        assertEquals("happy-project",savedProject.getProjectName());
        assertNotNull(savedProject.getProjectId());
    }

    @Test
    void shouldAccessOwnProjects() throws Exception{

        String token1 = registerUser("user1","user1@gmail.com");
        String token2 = registerUser("user2","user2@gmail.com");

        UUID projectId1 = createProject(token1,"hp1");
        UUID projectId2 = createProject(token1,"hp2");
        UUID projectId3 = createProject(token2,"hp3");

        mockMvc.perform(
                get("/projects")
                        .header(
                                "Authorization",
                                "Bearer "+token1
                        )
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].projectName").value("hp2"))
                .andExpect(jsonPath("$[1].projectName").value("hp1"));

        mockMvc.perform(
                        get("/projects")
                                .header(
                                        "Authorization",
                                        "Bearer "+token2
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].projectName").value("hp3"));

        List<Project> projects = projectRepository.findByUserOrderByCreatedAtDesc(userRepository.findByUsername("user1").orElseThrow());
        assertEquals(2, projects.size());

        assertEquals("hp2",projects.getFirst().getProjectName());
        assertEquals(projectId1,projects.get(1).getProjectId());
    }

    @Test
    void shouldAccessProjectWithProjectId() throws Exception{

        String token = registerUser("user","user@gmail.com");
        UUID projectId = createProject(token,"happy-path");

        mockMvc.perform(
                        get("/projects/" + projectId)
                                .header(
                                        "Authorization",
                                        "Bearer "+token
                                )
                )
                .andExpect(status().isOk());

        List<Project> projects = projectRepository.findByUserOrderByCreatedAtDesc(userRepository.findByUsername("user").orElseThrow());
        assertEquals(1, projects.size());

        assertEquals("happy-path",projects.getFirst().getProjectName());
        assertEquals(projectId,projects.get(0).getProjectId());
    }

    @Test
    void shouldRejectAccessToAnotherUsersProject() throws Exception{

        String token1 = registerUser("user1","user1@gmail.com");
        String token2 = registerUser("user2","user2@gmail.com");

        UUID projectId = createProject(token1,"sp1");

        mockMvc.perform(
                get("/projects/"+projectId)
                        .header(
                                "Authorization",
                                "Bearer " + token2
                        )
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDuplicateProjectName() throws Exception{

        String token = registerUser("dupe-user","dupe@gmail.com");
        UUID projectId = createProject(token,"dupe-path");

        mockMvc.perform(
                        post("/projects")
                                .with(csrf())
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "projectName":"dupe-path"
                                    }
                                    """)

                )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectUnauthenticatedProjectAccess() throws Exception {

        mockMvc.perform(
                        get("/projects")
                                .with(csrf())
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInvalidJwt() throws Exception {

        mockMvc.perform(
                        get("/projects")
                                .header("Authorization", "Bearer not-a-real-token")
                )
                .andExpect(status().isUnauthorized());
    }

}
