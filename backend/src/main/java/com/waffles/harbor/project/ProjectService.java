package com.waffles.harbor.project;

import com.waffles.harbor.user.User;
import com.waffles.harbor.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public ProjectResponse createProject(User user, ProjectRequest projectRequest){

        if(projectRepository.findByUserAndProjectNameIgnoreCase(user, projectRequest.getProjectName()).isPresent())
            throw new IllegalArgumentException("Project with the project name " + projectRequest.getProjectName() + " already exists!");

        var project = Project.builder()
                .user(user)
                .projectName(projectRequest.getProjectName())
                .build();

        Project savedProject = projectRepository.save(project);

        return toResponse(savedProject);
    }

    public List<ProjectResponse> getAllProjects(User user){

        List<Project> projects = projectRepository.findByUserOrderByCreatedAtDesc(user);

        return projects.stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjectResponse getProjectByName(User user, String projectName){

        Project project = projectRepository.findByUserAndProjectNameIgnoreCase(user,projectName)
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
