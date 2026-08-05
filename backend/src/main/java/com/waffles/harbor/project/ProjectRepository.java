package com.waffles.harbor.project;

import com.waffles.harbor.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByUserOrderByCreatedAtDesc(User user);
    Optional<Project> findByUserAndProjectNameIgnoreCase(User user, String projectName);
    Optional<Project> findByUserAndProjectId(User user, UUID projectId);
    boolean existsByUserAndProjectNameIgnoreCase(User user, String projectName);
}
