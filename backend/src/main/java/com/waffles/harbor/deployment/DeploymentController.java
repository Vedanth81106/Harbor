package com.waffles.harbor.deployment;

import com.waffles.harbor.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;

    @GetMapping("/{projectId}")
    public ResponseEntity<List<DeploymentResponse>> getAllDeployments(
            @AuthenticationPrincipal User user,
            @PathVariable UUID projectId
    ) {
        return ResponseEntity.ok(
                deploymentService.getAllDeployments(user, projectId)
        );
    }

    @GetMapping(
            value = "/{deploymentId}/logs",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter getDeploymentLogs(
            @AuthenticationPrincipal User user,
            @PathVariable UUID deploymentId
    ){
        return deploymentService.getDeploymentLogs(user,deploymentId);
    }

    @PostMapping("/deploy/{projectId}")
    public ResponseEntity<DeploymentResponse> deploy(
            @AuthenticationPrincipal User user,
            @PathVariable UUID projectId
    ) throws Exception {
        return ResponseEntity.ok(
                deploymentService.deploy(user, projectId)
        );
    }

    @DeleteMapping("/{deploymentId}")
    public ResponseEntity<Void> deleteDeployment(
            @AuthenticationPrincipal User user,
            @PathVariable UUID deploymentId
    ) {

        deploymentService.deleteDeployment(user, deploymentId);

        return ResponseEntity.noContent().build();
    }

}
