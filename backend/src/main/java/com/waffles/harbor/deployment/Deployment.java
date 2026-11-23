package com.waffles.harbor.deployment;

import com.waffles.harbor.deployment.enums.DeploymentStatus;
import com.waffles.harbor.deployment.enums.DeploymentType;
import com.waffles.harbor.deployment.enums.ResourceType;
import com.waffles.harbor.project.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "deployments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID deploymentId;

    @OneToMany(
            mappedBy = "deployment",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<DeploymentResource> deploymentResources = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeploymentType deploymentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeploymentStatus status;

    private String commitHash;

    @CreationTimestamp
    @Column(name="created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name="updated_at",nullable = false)
    private LocalDateTime updatedAt;

    public void addResource(DeploymentResource resource){
        this.deploymentResources.add(resource);
        resource.setDeployment(this);
    }

    public void removeResource(DeploymentResource resource){
        this.deploymentResources.remove(resource);
        resource.setDeployment(null);
    }

    // get the sole container associated with a Dockerfile deployment
    public DeploymentResource getSingleContainerResource(){
        List<DeploymentResource> containers = deploymentResources.stream()
                .filter(r -> r.getResourceType() == ResourceType.CONTAINER)
                .toList();

        if(containers.size() != 1)
            throw new IllegalStateException("Expected only one container, found  " + containers.size());

        return containers.getFirst();
    }
    // get all containers associated docker-compose files
    public List<DeploymentResource> getContainerResources(){
        return deploymentResources.stream()
                .filter(r -> r.getResourceType() == ResourceType.CONTAINER)
                .toList();
    }

    public DeploymentResource getSingleImageResource(){
        List<DeploymentResource> images = deploymentResources.stream()
                .filter(r -> r.getResourceType() == ResourceType.IMAGE)
                .toList();

        if(images.size() != 1)
            throw new IllegalStateException("Expected only one image, found  " + images.size());

        return images.getFirst();
    }
    // get all containers associated docker-compose files
    public List<DeploymentResource> getImageResources(){
        return deploymentResources.stream()
                .filter(r -> r.getResourceType() == ResourceType.IMAGE)
                .toList();
    }

    public DeploymentResource getSingleNetworkResource(){
        List<DeploymentResource> networks = deploymentResources.stream()
                .filter(r -> r.getResourceType() == ResourceType.NETWORK)
                .toList();

        if(networks.size() != 1)
            throw new IllegalStateException("Expected only one network, found " + networks.size());

        return networks.getFirst();
    }
    // get all networks associated docker-compose files
    public List<DeploymentResource> getNetworkResources(){
        return deploymentResources.stream()
                .filter(r -> r.getResourceType() == ResourceType.NETWORK)
                .toList();
    }


}
