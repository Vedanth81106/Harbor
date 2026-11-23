
package com.waffles.harbor.deployment;

import com.waffles.harbor.deployment.enums.DeploymentStatus;
import com.waffles.harbor.deployment.strategy.DeploymentStrategy;
import com.waffles.harbor.deployment.strategy.DeploymentStrategyFactory;
import com.waffles.harbor.docker.DockerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeploymentRecoveryService {

    private final DeploymentRepository deploymentRepository;
    private final DockerService dockerService;
    private final DeploymentStrategyFactory deploymentStrategyFactory;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverDeployments() {
        var deployments = deploymentRepository.findByStatus(DeploymentStatus.READY);

        for (Deployment deployment : deployments) {
            if (deployment.getDeploymentResources().isEmpty()) continue;
            try {
                DeploymentStrategy strategy = deploymentStrategyFactory.getStrategy(deployment.getDeploymentType());
                strategy.recover(deployment);
            } catch (Exception e) {
                log.error(
                        "Failed to restore routing for deployment {}",
                        deployment.getDeploymentId(),
                        e
                );
            }
        }
    }

    // cleans up methods that were cloning/building/starting and the application restarted
    @EventListener(ApplicationReadyEvent.class)
    public void cleanupStuckDeployments(){

        List<DeploymentStatus> stuckDeploymentsStatuses = List.of(
                DeploymentStatus.BUILDING, DeploymentStatus.CLONING, DeploymentStatus.FAILED, DeploymentStatus.STARTING);

        List<Deployment> stuckDeployments = deploymentRepository.findByStatusIn(stuckDeploymentsStatuses);

        for(Deployment deployment : stuckDeployments){
            if (deployment.getDeploymentResources().isEmpty()) continue;
            try {
                DeploymentStrategy strategy = deploymentStrategyFactory.getStrategy(deployment.getDeploymentType());
                strategy.teardown(deployment);

                deployment.setStatus(DeploymentStatus.FAILED);
                deploymentRepository.save(deployment);
            } catch (Exception e) {
                log.error(
                        "Failed to restore deployment {}",
                        deployment.getDeploymentId(),
                        e
                );
            }
        }
    }
}
