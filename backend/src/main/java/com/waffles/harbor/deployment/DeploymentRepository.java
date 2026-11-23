package com.waffles.harbor.deployment;

import com.waffles.harbor.deployment.enums.DeploymentStatus;
import com.waffles.harbor.deployment.enums.ResourceType;
import com.waffles.harbor.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeploymentRepository extends JpaRepository<Deployment, UUID> {
    List<Deployment> findByProject_ProjectIdOrderByCreatedAtDesc(UUID projectId);
    Optional<Deployment> findByDeploymentIdAndProject_User(UUID deploymentId, User user);

    // if any deployment exists that has image name and any deployment with deployment id OTHER than give one
    @Query("""
    SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
    FROM DeploymentResource r
    WHERE r.deployment.deploymentId <> :deploymentId
      AND r.resourceType = :resourceType
      AND r.resourceName = :resourceName
""")
    boolean existsImageReference(
            @Param("deploymentId") UUID deploymentId,
            @Param("resourceType") ResourceType resourceType,
            @Param("resourceName") String resourceName
    );

    @Query("""
    SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
    FROM DeploymentResource r
    WHERE r.deployment.deploymentId NOT IN :deploymentIds
      AND r.resourceType = :resourceType
      AND r.resourceName = :resourceName
""")
    boolean existsImageReferenceOutside(
            @Param("deploymentIds") Collection<UUID> deploymentIds,
            @Param("resourceType") ResourceType resourceType,
            @Param("resourceName") String resourceName
    );

    List<Deployment> findByStatus(DeploymentStatus status);
    List<Deployment> findByStatusIn(Collection<DeploymentStatus> statuses);
}
