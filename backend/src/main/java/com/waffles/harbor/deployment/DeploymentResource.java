// This class is the database record of the resource entity.
// What is a resource you ask? Well it is basically a single service that is provided by docker
// for example, a backend container/network/image having a container name,id, and service name like fastapi-backend


package com.waffles.harbor.deployment;

import com.waffles.harbor.deployment.enums.ResourceType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "deployment_resources")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class DeploymentResource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID resourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deployment_id", nullable = false)
    private Deployment deployment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType resourceType;

    // Actual Docker resource ID or image reference
    @Column(nullable = false)
    private String externalId;

    // Resource name given to docker in human readable format
    private String resourceName;

    // Compose service associated with the resource( if applicable i.e. for images and containers)
    private String serviceName;


}
