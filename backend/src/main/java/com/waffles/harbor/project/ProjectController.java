package com.waffles.harbor.project;

import com.waffles.harbor.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjectsByUser(@AuthenticationPrincipal User user){

        return ResponseEntity.status(HttpStatus.OK)
                .body(projectService.getAllProjects(user));
    }

    @GetMapping("/{projectName}")
    public ResponseEntity<ProjectResponse> getProjectByName(@AuthenticationPrincipal User user, @PathVariable String projectName){

        return ResponseEntity.status(HttpStatus.OK)
                .body(projectService.getProjectByName(user,projectName));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@AuthenticationPrincipal User user, @RequestBody @Valid ProjectRequest request){

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(user, request));
    }
}
