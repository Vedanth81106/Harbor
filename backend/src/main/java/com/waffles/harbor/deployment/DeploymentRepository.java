package com.waffles.harbor.deployment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeploymentRepository extends JpaRepository<Deployment, UUID> {
    List<Deployment> findByProject_ProjectIdOrderByCreatedAtDesc(UUID projectId);
}
