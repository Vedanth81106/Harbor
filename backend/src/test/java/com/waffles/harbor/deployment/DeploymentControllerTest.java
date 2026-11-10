package com.waffles.harbor.deployment;

import com.waffles.harbor.auth.Role;
import com.waffles.harbor.exception.DeploymentNotFoundException;
import com.waffles.harbor.exception.DeploymentStateException;
import com.waffles.harbor.security.JwtService;
import com.waffles.harbor.user.User;
import com.waffles.harbor.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeploymentController.class)
class DeploymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeploymentService deploymentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private User createUser(String username) {
        return User.builder()
                .userId(UUID.randomUUID())
                .username(username)
                .email(username + "@gmail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();
    }

    private DeploymentResponse createDeploymentResponse(UUID projectId) {
        return DeploymentResponse.builder()
                .deploymentId(UUID.randomUUID())
                .projectId(projectId)
                .status(DeploymentStatus.READY)
                .commitHash("abc123")
                .imageName("project:abc123")
                .build();
    }

    private org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth(User user) {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );
    }

    @Test
    void shouldGetAllDeploymentsForProject() throws Exception {
        User user = createUser("happy");
        UUID projectId = UUID.randomUUID();
        DeploymentResponse first = createDeploymentResponse(projectId);
        DeploymentResponse second = createDeploymentResponse(projectId);

        when(deploymentService.getAllDeployments(user, projectId))
                .thenReturn(List.of(first, second));

        mockMvc.perform(
                        get("/deployments/project/" + projectId)
                                .with(authentication(auth(user)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].deploymentId").value(first.getDeploymentId().toString()));

        verify(deploymentService).getAllDeployments(user, projectId);
    }

    @Test
    void shouldGetDeploymentById() throws Exception {
        User user = createUser("happy");
        DeploymentResponse response = createDeploymentResponse(UUID.randomUUID());

        when(deploymentService.getDeployment(user, response.getDeploymentId()))
                .thenReturn(response);

        mockMvc.perform(
                        get("/deployments/" + response.getDeploymentId())
                                .with(authentication(auth(user)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));

        verify(deploymentService).getDeployment(user, response.getDeploymentId());
    }

    @Test
    void shouldReturnNotFoundForMissingDeployment() throws Exception {
        User user = createUser("sad");
        UUID deploymentId = UUID.randomUUID();

        when(deploymentService.getDeployment(user, deploymentId))
                .thenThrow(new DeploymentNotFoundException());

        mockMvc.perform(
                        get("/deployments/" + deploymentId)
                                .with(authentication(auth(user)))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateDeployment() throws Exception {
        User user = createUser("happy");
        UUID projectId = UUID.randomUUID();
        DeploymentResponse response = createDeploymentResponse(projectId);

        when(deploymentService.createDeployment(user, projectId)).thenReturn(response);

        mockMvc.perform(
                        post("/deployments/" + projectId)
                                .with(csrf())
                                .with(authentication(auth(user)))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deploymentId").value(response.getDeploymentId().toString()));

        verify(deploymentService).createDeployment(user, projectId);
    }

    @Test
    void shouldExecuteDeployment() throws Exception {
        User user = createUser("happy");
        DeploymentResponse response = createDeploymentResponse(UUID.randomUUID());

        when(deploymentService.executeDeployment(user, response.getDeploymentId()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/deployments/" + response.getDeploymentId() + "/execute")
                                .with(csrf())
                                .with(authentication(auth(user)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));

        verify(deploymentService).executeDeployment(user, response.getDeploymentId());
    }

    @Test
    void shouldReturnConflictWhenDeploymentCannotBeExecuted() throws Exception {
        User user = createUser("happy");
        UUID deploymentId = UUID.randomUUID();

        when(deploymentService.executeDeployment(user, deploymentId))
                .thenThrow(new DeploymentStateException("Only queued deployments can be executed"));

        mockMvc.perform(
                        post("/deployments/" + deploymentId + "/execute")
                                .with(csrf())
                                .with(authentication(auth(user)))
                )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldDeleteDeployment() throws Exception {
        User user = createUser("happy");
        UUID deploymentId = UUID.randomUUID();

        mockMvc.perform(
                        delete("/deployments/" + deploymentId)
                                .with(csrf())
                                .with(authentication(auth(user)))
                )
                .andExpect(status().isNoContent());

        verify(deploymentService).deleteDeployment(user, deploymentId);
    }

    @Test
    void shouldStreamDeploymentLogs() throws Exception {
        User user = createUser("happy");
        UUID deploymentId = UUID.randomUUID();

        when(deploymentService.getDeploymentLogs(user, deploymentId))
                .thenReturn(new org.springframework.web.servlet.mvc.method.annotation.SseEmitter());

        mockMvc.perform(
                        get("/deployments/" + deploymentId + "/logs")
                                .with(authentication(auth(user)))
                )
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        verify(deploymentService).getDeploymentLogs(user, deploymentId);
    }

    @Test
    void shouldRejectUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/deployments/project/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(deploymentService);
    }
}
