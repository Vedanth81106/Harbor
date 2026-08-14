package com.waffles.harbor.integration;

import com.waffles.harbor.auth.Role;
import com.waffles.harbor.user.User;
import com.waffles.harbor.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
public class AuthIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterUserAndPersistToDatabase() throws Exception{

        mockMvc.perform(
                post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username":"integration-happy",
                                    "email":"happy@gmail.com",
                                    "password":"password123"
                                }
                                """)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("integration-happy"))
                .andExpect(jsonPath("$.email").value("happy@gmail.com"))
                .andExpect(jsonPath("$.accessToken").exists());

        User savedUser = userRepository.findByUsername("integration-happy")
                .orElseThrow();

        assertEquals("integration-happy",savedUser.getUsername());
        assertEquals("happy@gmail.com",savedUser.getEmail());
        assertNotNull(savedUser.getUserId());
        assertNotEquals("password123", savedUser.getPassword());
        assertTrue(passwordEncoder.matches(
                "password123",
                savedUser.getPassword()
        ));
        assertEquals(Role.USER,savedUser.getRole());
    }

    @Test
    void shouldRejectWhenNoRegisteredUserExists() throws Exception{

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "usernameOrEmail":"integration-happy",
                                    "password":"password123"
                                }
                                """)
                )
                .andExpect(status().isUnauthorized());

    }

    @Test
    void shouldRejectRegistrationWhenUsernameAlreadyExists() throws Exception{
        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "username":"integration-sad",
                                    "email":"sad@gmail.com",
                                    "password":"password123"
                                }
                                """)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "username":"integration-sad",
                                    "email":"sad2@gmail.com",
                                    "password":"password123"
                                }
                                """)
                )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectRegistrationWhenEmailAlreadyExists() throws Exception{
        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "username":"integration-sad",
                                    "email":"sad@gmail.com",
                                    "password":"password123"
                                }
                                """)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "username":"integration-sad2",
                                    "email":"sad@gmail.com",
                                    "password":"password123"
                                }
                                """)
                )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldLoginSuccessfully() throws Exception{

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "username":"login-happy",
                                    "email":"login-happy@gmail.com",
                                    "password":"password123"
                                }
                                """)
                )
                .andExpect(status().isCreated());


        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "usernameOrEmail":"login-happy",
                                    "password":"password123"
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("login-happy"))
                .andExpect(jsonPath("$.email").value("login-happy@gmail.com"))
                .andExpect(jsonPath("$.accessToken").exists());

        User savedUser = userRepository.findByUsername("login-happy")
                .orElseThrow();

        assertEquals("login-happy",savedUser.getUsername());
        assertEquals("login-happy@gmail.com",savedUser.getEmail());
        assertNotEquals("password123", savedUser.getPassword());
        assertEquals(Role.USER,savedUser.getRole());

    }

    @Test
    void shouldRejectLoginWithWrongPassword() throws Exception{
        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "username":"login-sad",
                                    "email":"login-sad@gmail.com",
                                    "password":"password123"
                                }
                                """)
                )
                .andExpect(status().isCreated());


        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "usernameOrEmail":"login-sad",
                                    "password":"password"
                                }
                                """)
                )
                .andExpect(status().isUnauthorized());

    }
}
