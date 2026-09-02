package com.waffles.harbor.deployment;

import com.waffles.harbor.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeploymentRepository extends JpaRepository<Deployment, UUID> {
    List<Deployment> findByProject_ProjectIdOrderByCreatedAtDesc(UUID projectId);
    Optional<Deployment> findByDeploymentIdAndProject_User(UUID deploymentId, User user);

    // if any deployment exists that has image name and any deployment with deployment id OTHER than give one
    boolean existsByImageNameAndDeploymentIdNot(String imageName, UUID deploymentId);

    boolean existsByImageName(String imageName);
}
