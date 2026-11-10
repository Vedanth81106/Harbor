package com.waffles.harbor.deployment;

import com.waffles.harbor.docker.DockerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeploymentRecoveryServiceTest {

    @Mock
    private DeploymentRepository deploymentRepository;

    @Mock
    private DockerService dockerService;

    @InjectMocks
    private DeploymentRecoveryService recoveryService;

    private Deployment readyDeployment() {
        return Deployment.builder()
                .deploymentId(UUID.randomUUID())
                .containerId("container-id")
                .status(DeploymentStatus.READY)
                .build();
    }

    @Test
    void shouldDoNothingWhenNoDeploymentsAreReady() {
        when(deploymentRepository.findByStatus(DeploymentStatus.READY))
                .thenReturn(List.of());

        recoveryService.recoverDeployments();

        verifyNoInteractions(dockerService);
    }

    @Test
    void shouldRecoverEveryReadyDeployment() {
        Deployment first = readyDeployment();
        Deployment second = readyDeployment();

        when(deploymentRepository.findByStatus(DeploymentStatus.READY))
                .thenReturn(List.of(first, second));

        recoveryService.recoverDeployments();

        verify(dockerService).recoverContainer(
                first.getDeploymentId(),
                first.getContainerId()
        );
        verify(dockerService).recoverContainer(
                second.getDeploymentId(),
                second.getContainerId()
        );
    }

    @Test
    void shouldContinueRecoveryWhenOneDeploymentFails() {
        Deployment failing = readyDeployment();
        Deployment healthy = readyDeployment();

        when(deploymentRepository.findByStatus(DeploymentStatus.READY))
                .thenReturn(List.of(failing, healthy));

        doThrow(new RuntimeException("docker unavailable"))
                .when(dockerService)
                .recoverContainer(failing.getDeploymentId(), failing.getContainerId());

        recoveryService.recoverDeployments();

        verify(dockerService).recoverContainer(
                healthy.getDeploymentId(),
                healthy.getContainerId()
        );
    }
}
