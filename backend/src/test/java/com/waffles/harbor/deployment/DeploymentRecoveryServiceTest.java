package com.waffles.harbor.deployment;

import com.waffles.harbor.deployment.enums.DeploymentStatus;
import com.waffles.harbor.deployment.enums.DeploymentType;
import com.waffles.harbor.deployment.enums.ResourceType;
import com.waffles.harbor.deployment.strategy.DeploymentStrategy;
import com.waffles.harbor.deployment.strategy.DeploymentStrategyFactory;
import com.waffles.harbor.docker.DockerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeploymentRecoveryServiceTest {

    @Mock
    private DeploymentRepository deploymentRepository;

    @Mock
    private DockerService dockerService;

    @Mock
    private DeploymentStrategyFactory deploymentStrategyFactory;

    @Mock
    private DeploymentStrategy strategy;

    @InjectMocks
    private DeploymentRecoveryService recoveryService;

    private Deployment readyDeployment(boolean withResources) {
        Deployment deployment = Deployment.builder()
                .deploymentId(UUID.randomUUID())
                .deploymentType(DeploymentType.DOCKERFILE)
                .status(DeploymentStatus.READY)
                .build();

        if (withResources) {
            deployment.addResource(DeploymentResource.builder()
                    .resourceType(ResourceType.CONTAINER)
                    .externalId("container-id")
                    .resourceName("harbor-12345678")
                    .build());
            deployment.addResource(DeploymentResource.builder()
                    .resourceType(ResourceType.NETWORK)
                    .externalId("network-id")
                    .resourceName("net-12345678")
                    .build());
        }

        return deployment;
    }

    @Test
    void shouldDoNothingWhenNoDeploymentsAreReady() {
        when(deploymentRepository.findByStatus(DeploymentStatus.READY))
                .thenReturn(List.of());

        recoveryService.recoverDeployments();

        verifyNoInteractions(deploymentStrategyFactory, strategy);
    }

    @Test
    void shouldRecoverEveryReadyDeployment() {
        Deployment first = readyDeployment(true);
        Deployment second = readyDeployment(true);

        when(deploymentRepository.findByStatus(DeploymentStatus.READY))
                .thenReturn(List.of(first, second));
        when(deploymentStrategyFactory.getStrategy(DeploymentType.DOCKERFILE))
                .thenReturn(strategy);

        recoveryService.recoverDeployments();

        verify(strategy).recover(first);
        verify(strategy).recover(second);
    }

    @Test
    void shouldContinueRecoveryWhenOneDeploymentFails() {
        Deployment failing = readyDeployment(true);
        Deployment healthy = readyDeployment(true);

        when(deploymentRepository.findByStatus(DeploymentStatus.READY))
                .thenReturn(List.of(failing, healthy));
        when(deploymentStrategyFactory.getStrategy(DeploymentType.DOCKERFILE))
                .thenReturn(strategy);
        doThrow(new RuntimeException("docker unavailable"))
                .when(strategy).recover(failing);

        recoveryService.recoverDeployments();

        verify(strategy).recover(healthy);
    }

    @Test
    void shouldSkipReadyDeploymentWithoutResources() {
        when(deploymentRepository.findByStatus(DeploymentStatus.READY))
                .thenReturn(List.of(readyDeployment(false)));

        recoveryService.recoverDeployments();

        verifyNoInteractions(strategy);
    }

    @Test
    void shouldTearDownStuckDeployments() {
        Deployment stuck = readyDeployment(true);
        stuck.setStatus(DeploymentStatus.STARTING);

        when(deploymentRepository.findByStatusIn(anyCollection()))
                .thenReturn(List.of(stuck));
        when(deploymentStrategyFactory.getStrategy(DeploymentType.DOCKERFILE))
                .thenReturn(strategy);

        recoveryService.cleanupStuckDeployments();

        verify(strategy).teardown(stuck);
        assertEquals(DeploymentStatus.FAILED, stuck.getStatus());
        verify(deploymentRepository).save(stuck);
    }
}
