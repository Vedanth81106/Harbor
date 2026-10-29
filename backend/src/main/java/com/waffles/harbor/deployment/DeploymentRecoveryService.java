
package com.waffles.harbor.deployment;

import com.waffles.harbor.docker.DockerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeploymentRecoveryService {

    private final DeploymentRepository deploymentRepository;
    private final DockerService dockerService;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverDeployments() {
        var deployments = deploymentRepository
                .findByStatus(DeploymentStatus.READY);

        for (Deployment deployment : deployments) {
            try {
                dockerService.reconnectTraefik(
                        deployment.getDeploymentId(),
                        deployment.getContainerId()
                );
            } catch (Exception e) {
                log.error(
                        "Failed to restore routing for deployment {}",
                        deployment.getDeploymentId(),
                        e
                );
            }
        }
    }
}
