package com.waffles.harbor.deployment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/deployment")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;

    @GetMapping
    public ResponseEntity<List<Deployment>> getAllDeployments(
            @RequestParam UUID projectId
    ) {
        return ResponseEntity.ok(
                deploymentService.getAllDeployments(projectId)
        );
    }

    @PostMapping
    public ResponseEntity<Deployment> createDeployment(
            @RequestParam UUID projectId,
            @RequestParam String commitHash
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deploymentService.createDeployment(projectId, commitHash));
    }

    @PostMapping("/deploy")
    public ResponseEntity<String> deploy(
            @RequestParam UUID deploymentId
    ) throws Exception {
        return ResponseEntity.ok(
                deploymentService.deploy(deploymentId)
        );
    }
}
