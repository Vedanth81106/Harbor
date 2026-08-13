package com.waffles.harbor.integration;

import com.waffles.harbor.project.Project;
import com.waffles.harbor.project.ProjectRepository;
import com.waffles.harbor.user.User;
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
    void cleanProjectDatabase() {
        projectRepository.deleteAll();
    }

    @BeforeEach
    void cleanUserDatabase() {
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

        List<Project> projects = projectRepository.findAll();
        assertEquals(1, projects.size());
        Project savedProject = projects.getFirst();

        assertEquals("happy-project",savedProject.getProjectName());
        assertNotNull(savedProject.getProjectId());
    }
//
//    @Test
//    void shouldRejectAccessToAnotherUsersProject() throws Exception{
//
//        String token1 = registerUser("user1","user1@gmail.com");
//        String token2 = registerUser("user2","user2@gmail.com");
//
//        mockMvc.perform(
//                        post("/projects")
//                                .with(csrf())
//                                .header(
//                                        "Authorization",
//                                        "Bearer " + token1
//                                )
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content("""
//                                    {
//                                        "projectName":"sad-project1"
//                                    }
//                                    """)
//
//                )
//                .andExpect(status().isCreated());
//
//        mockMvc.perform(
//                        post("/projects")
//                                .with(csrf())
//                                .header(
//                                        "Authorization",
//                                        "Bearer " + token2
//                                )
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content("""
//                                    {
//                                        "projectName":"sad-project2"
//                                    }
//                                    """)
//
//                )
//                .andExpect(status().isCreated());
//
//        List<Project> projects = projectRepository.findByUserOrderByCreatedAtDesc(user1);
//        Project savedProject = projects.getFirst();
//
//        mockMvc.perform(
//                get("/projects")
//        )
//    }
}
