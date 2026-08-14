package com.waffles.harbor.project;

import com.waffles.harbor.exception.ProjectAlreadyExistsException;
import com.waffles.harbor.exception.ProjectNotFoundException;
import com.waffles.harbor.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectResponse createProject(User user, ProjectRequest projectRequest){

        if(projectRepository.existsByUserAndProjectNameIgnoreCase(user, projectRequest.getProjectName()))
            throw new ProjectAlreadyExistsException();

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
                .orElseThrow(ProjectNotFoundException::new);

        return toResponse(project);
    }

    private ProjectResponse toResponse(Project project){

        return ProjectResponse.builder()
                .projectId(project.getProjectId())
                .projectName(project.getProjectName())
                .build();
    }
}
