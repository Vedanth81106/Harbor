package com.waffles.harbor.repositories;

import com.waffles.harbor.entities.Project;
import com.waffles.harbor.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByUserOrderByCreatedAtDesc(User user);
    List<Project> findAllByUserAndProjectName(User user, String projectName);
}
