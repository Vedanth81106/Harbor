package com.waffles.harbor.project;

import com.waffles.harbor.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectResponse createProject(User user, ProjectRequest projectRequest){

        if(projectRepository.existsByUserAndProjectNameIgnoreCase(user, projectRequest.getProjectName()))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Project with the project name " + projectRequest.getProjectName() + " already exists!"
            );

        var project = Project.builder()
                .user(user)
                .projectName(projectRequest.getProjectName())
                .build();

        Project savedProject = projectRepository.save(project);

        return toResponse(savedProject);
    }

    public List<ProjectResponse> getAllProjectsByUser(User user){

        List<Project> projects = projectRepository.findByUserOrderByCreatedAtDesc(user);

        return projects.stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjectResponse getProjectByProjectId(User user, UUID projectId){

        Project project = projectRepository.findByUserAndProjectId(user,projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project could not be found!"));

        return toResponse(project);
    }

    private ProjectResponse toResponse(Project project){

        return ProjectResponse.builder()
                .projectId(project.getProjectId())
                .projectName(project.getProjectName())
                .build();
    }
}
