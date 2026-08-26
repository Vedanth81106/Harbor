package com.waffles.harbor.deployment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;

    @GetMapping
    public ResponseEntity<List<DeploymentResponse>> getAllDeployments(
            @RequestParam UUID projectId
    ) {
        return ResponseEntity.ok(
                deploymentService.getAllDeployments(projectId)
        );
    }

    @PostMapping
    public ResponseEntity<DeploymentResponse> createDeployment(
            @RequestParam UUID projectId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deploymentService.createDeployment(projectId));
    }

    @PostMapping("/deploy")
    public ResponseEntity<DeploymentResponse> deploy(
            @RequestParam UUID deploymentId
    ) throws Exception {
        return ResponseEntity.ok(
                deploymentService.deploy(deploymentId)
        );
    }

    @DeleteMapping("/{deploymentId}")
    public ResponseEntity<Void> deleteDeployment(
            @PathVariable UUID deploymentId
    ) {

        System.out.println("DELETE DEPLOYMENT: " + deploymentId);
        deploymentService.deleteDeployment(deploymentId);

        return ResponseEntity.noContent().build();
    }
}
